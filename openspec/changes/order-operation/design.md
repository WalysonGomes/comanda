## Context

`public-storefront` deixou o pedido pronto no navegador do cliente e declarou o **contrato** de um endpoint de criação idempotente; nada persiste ainda. Esta change implementa esse contrato e o painel de operação — o outro lado da promessa "nunca mais perder pedido". O ponto sensível é confiabilidade: nada pode duplicar (pedido nem avanço) e nada pode falhar em silêncio (Seção 4 do PRD).

Restrições herdadas: Spring Boot 4.1 / Spring 7 / Jakarta EE 11 / Hibernate 7, Java 21 com Virtual Threads, PostgreSQL 17, timezone hardcoded `America/Fortaleza`. Multi-tenancy por `tenant_id` (subdomínio no público, JWT no painel). Frontend React/Vite SPA, TanStack Query, Zustand, Lucide, tokens do `.design/Comanda Painel.dc.html`. Sem SSE/WebSocket no MVP — polling 15s (Seção 3.2).

O schema (`orders`, `order_items`, `order_item_additionals`, `order_status_history`, `customers`, `subscriptions`) e a constraint `(tenant_id, idempotency_key)` únicos vêm de `foundations`. Blocos de design consumidos: `PEDIDOS` (header + hero + chips + cards + skeleton + vazio), `DETAIL SHEET`, `CANCEL SHEET`.

## Goals / Non-Goals

**Goals:**
- Criação de pedido idempotente por `(tenant_id, idempotency_key)`, com snapshots imutáveis e totais calculados no servidor.
- Máquina de estados linear com avanço idempotente e histórico append-only.
- Painel confiável: polling 15s com `visibilityState`, indicador de conectividade + última atualização, avanço com loading e reversão em falha.
- Cancelamento com motivo ≥ 10 caracteres.
- Satisfazer o contrato declarado por `public-storefront` sem alterar seu requisito (confirmar antes do handoff + fallback sempre disponível).

**Non-Goals:**
- Notificação automática via WhatsApp API (Fase 4) — só preview + handoff manual.
- Alerta sonoro / push nativo (Fase 2).
- Enforcement do limite de 30 pedidos/mês (é `plans-and-onboarding`) — aqui só incrementar `order_count_month`.
- Filtro de retenção de histórico por plano 7/30 dias (é `plans-and-onboarding`).
- SSE/WebSocket, real-time abaixo de 15s.

## Decisions

### 1. Idempotência de criação: unique constraint + tratamento de conflito
A garantia final é a constraint de banco `UNIQUE (tenant_id, idempotency_key)` (Regra 8: constraint no banco, não só no código). Fluxo: tentar inserir; em violação de unicidade, **buscar e retornar o pedido existente** daquela `(tenant_id, key)` com 200 (mesmo id/total). Nunca duplica mesmo sob corrida de dois requests simultâneos (retry de rede). O incremento de `order_count_month` acontece **na mesma transação** da criação, então o reenvio (que não cria) também não reconta.
- *Por que não checar-então-inserir sem constraint:* janela de corrida entre check e insert; a constraint é a única defesa correta sob concorrência com Virtual Threads.
- *Alternativa considerada (tabela de idempotência separada):* overkill para o MVP; a própria coluna em `orders` já carrega a key.

### 2. Tenant sempre do servidor, `idempotency_key` do cliente
A criação é pública, resolvida por subdomínio; `tenant_id` nunca vem do corpo (Regra 9 / IDOR). A `idempotency_key` é um UUID gerado no cliente **por tentativa de finalização** (não por sessão), então retries do mesmo "Finalizar" reusam a key e não duplicam, mas um novo pedido intencional gera key nova.

### 3. Totais e snapshots calculados no servidor
Preços vêm do cardápio atual no servidor, não do corpo do cliente (evita adulteração). Na criação o servidor: resolve cada produto/adicional por ID no tenant, revalida disponibilidade do dia (mesma regra do storefront), grava `*_snapshot` e calcula `subtotal`/`delivery_fee`/`total`. Item indisponível na criação → rejeição por item (o storefront já validou antes via endpoint de disponibilidade, mas a criação revalida como fonte da verdade). Snapshots tornam o pedido imune a edições futuras do cardápio (Regra 7).

### 4. Máquina de estados como enum linear + avanço parametrizado pela origem
Status: `RECEBIDO, ACEITO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO`. Avanço = índice+1 na sequência linear (CANCELADO fora da linha). O endpoint de avanço recebe o **status de origem esperado** (o que o painel vê); o servidor só avança se o status atual == origem esperada. Isso torna o avanço idempotente **e** seguro contra duplo toque: a segunda requisição chega com origem já superada → reconhecida como no-op (retorna o estado atual, sem novo registro de histórico). Transições ilegais (pular, retroceder, avançar terminal) → rejeitadas.
- *Alternativa (idempotency-key por avanço):* mais peso; a comparação de origem resolve o caso real (duplo toque) com menos maquinário.

### 5. Histórico append-only por convenção + serviço único de transição
Nenhum update/delete em `order_status_history`. Todas as transições (criação, avanço, cancelamento) passam por **um** método de domínio que grava o registro — garante `from_status`/`to_status`/`changed_by`/`created_at` sempre consistentes. `changed_by_user_id` nulo = `SYSTEM` (registro inicial da criação).

### 6. Polling no cliente com TanStack Query
`refetchInterval: 15000` + `refetchIntervalInBackground: false` cobre o `visibilityState` nativamente (TanStack pausa quando a aba não está visível). Indicador de conectividade: derivar de `query.isError` / `errorUpdatedAt` vs `dataUpdatedAt`; exibir banner com timestamp de `dataUpdatedAt` (última atualização bem-sucedida). O header de `PEDIDOS` já tem os placeholders `connBg/connFg/connIcon/connText/connStamp` — mapeados para esse estado.
- *Por que não `navigator.onLine`:* mede o rádio, não a saúde da API; o que importa é se o **polling** teve sucesso. Usar o resultado real da query.

### 7. Avanço otimista com reversão visível
Ao avançar, atualizar o card otimisticamente + botão em loading/disabled (bloco `DETAIL SHEET`: `advancing` → spinner `cmd-spin`, `advBtnStyle` desabilitado). `onError`: reverter para o status anterior (rollback do cache do TanStack) e mostrar erro. Nunca deixar o card em estado ambíguo (Seção 4.2).

### 8. Fronteira com `public-storefront` (contrato do pedido)
Confirma o contrato declarado lá: request público resolvido por subdomínio, `idempotency_key` no corpo, sem `tenant_id`; resposta = pedido (id curto + total), idempotente. Mantém-se o requisito do storefront intacto: ele confirma "Pedido enviado" antes do handoff e **sempre** oferece o fallback de cópia. A ordem storefront usa — chamar criação → em sucesso confirmar e abrir WhatsApp; em falha de rede após timeout, o retry reusa a key (idempotente) — sem alterar o requisito do storefront.

### 9. Toggle aberto/fechado e banner — fonte única
`is_open` no `tenants`; o toggle no header de `PEDIDOS` é a única superfície de escrita (placeholders `openSegStyle/closedSegStyle`). Banner de abertura derivado no cliente: dentro do horário de `business_hours` de hoje **e** `is_open=false` → exibir sugestão de abrir. A avaliação de "dentro do horário" usa `America/Fortaleza`.

## Risks / Trade-offs

- **[Corrida de criação duplicada sob retry simultâneo]** → a `UNIQUE (tenant_id, idempotency_key)` é a defesa; o caminho de conflito retorna o pedido existente em vez de erro. Testar dois inserts concorrentes com a mesma key.
- **[Duplo toque no avanço gerando dois registros]** → avanço parametrizado pela origem esperada torna a 2ª chamada um no-op; botão desabilitado durante a requisição é a segunda barreira. Testar avanço duplo.
- **[Polling silencioso em falha]** → indicador derivado do estado real da query + timestamp de última atualização; nunca `catch` vazio. Testar queda de rede.
- **[Delay de até 15s para novo pedido]** → limitação aceita e documentada (Seção 13); SSE/WebSocket no backlog. Sem alerta sonoro no MVP (Fase 2) — dono que cozinha pode não ver na hora (limitação conhecida).
- **[Snapshot vs. cardápio vivo]** → snapshots gravados na criação; edição posterior do cardápio não toca pedidos antigos. Testar edição de produto após pedido.
- **[Contagem de pedidos do mês vs. enforcement do limite]** → o contador é incrementado aqui (na criação, mesma transação), mas o limite de 30/mês é imposto em `plans-and-onboarding`; alinhar para não duplicar a regra nem contar em dobro no retry idempotente.
- **[Fuso hardcoded `America/Fortaleza`]** → resumo do dia e "dentro do horário" usam esse fuso; negócios em outro fuso ficam deslocados (Seção 13, Fase 4).

## Migration Plan

Sem migration nova esperada: as tabelas e a constraint `(tenant_id, idempotency_key)` vêm de `foundations`. Se algum índice de leitura do painel faltar (ex.: `orders(tenant_id, status, created_at)` para lista filtrada), adicionar via migration Flyway incremental dentro desta change. Deploy junto do artefato único; rollback = reverter o build (dados de pedido permanecem, histórico é append-only e não destrutivo).

## Open Questions

- Índice de suporte à listagem filtrada do painel (`orders(tenant_id, status, created_at desc)`) — adicionar via Flyway se o plano de query justificar; decidir na implementação.
- Onde exatamente mora o incremento de `order_count_month` vs. o enforcement do limite: registrado aqui na criação; o **bloqueio** em `plans-and-onboarding`. Confirmar na revisão cruzada que não há dupla contagem no caminho idempotente.
- Reset mensal de `order_count_month` (virada de mês) — provavelmente responsabilidade de `plans-and-onboarding` junto do enforcement; não implementar aqui além de incrementar.
