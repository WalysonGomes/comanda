## 1. Backend — fatia vertical `orders` (domínio e persistência)

- [x] 1.1 Criar o pacote de fatia vertical `orders` seguindo o padrão de `foundations` (feature-based).
- [x] 1.2 Definir o enum de status `RECEBIDO, ACEITO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO` e a sequência linear de avanço (CANCELADO fora da linha).
- [x] 1.3 Mapear entidades JPA (Hibernate 7): `Order`, `OrderItem`, `OrderItemAdditional`, `OrderStatusHistory`, `Customer`, todas com `tenant_id`.
- [x] 1.4 Confirmar/garantir a constraint `UNIQUE (tenant_id, idempotency_key)` em `orders` (vem de `foundations`); adicionar migration Flyway incremental só se faltar índice de leitura do painel (`orders(tenant_id, status, created_at desc)`).
- [x] 1.5 Implementar o serviço único de transição de status que grava `order_status_history` (from/to/changed_by/created_at) — usado por criação, avanço e cancelamento; append-only (nunca update/delete).

## 2. Backend — criação idempotente de pedido (endpoint público)

- [x] 2.1 Endpoint público (resolvido por subdomínio, sem JWT) de criação de pedido; `tenant_id` sempre do subdomínio, ignorar qualquer tenant no corpo.
- [x] 2.2 Resolver produtos/adicionais por ID no tenant, revalidar disponibilidade do dia (`is_available` + `available_days` em `America/Fortaleza`); rejeitar por item se indisponível.
- [x] 2.3 Gravar snapshots imutáveis (`product_name_snapshot`, `unit_price_snapshot`, `additional_name_snapshot`, `additional_price_snapshot`, `address_snapshot`) e calcular `subtotal`/`delivery_fee`/`total` no servidor (não confiar em preços do cliente).
- [x] 2.4 Criar pedido em `RECEBIDO`, gravar registro inicial de histórico (`changed_by=SYSTEM`) e incrementar `tenants.order_count_month` na mesma transação.
- [x] 2.5 Tratar violação de `UNIQUE (tenant_id, idempotency_key)`: buscar e retornar o pedido existente (mesmo id/total), sem duplicar, sem novo histórico, sem reincrementar contador.
- [x] 2.6 Upsert de `customer` (nome/telefone) por tenant conforme necessário para o pedido.

## 3. Backend — painel (endpoints autenticados)

- [x] 3.1 Endpoint de listagem de pedidos do tenant (JWT), com filtro por status e dados para o resumo do dia (contagem, receita, em aberto).
- [x] 3.2 Endpoint de detalhe de pedido por ID, filtrado por `tenant_id` do JWT (404 para pedido de outro tenant — IDOR).
- [x] 3.3 Endpoint de avanço de status idempotente: parametrizado pelo status de origem esperado; avança um passo, grava histórico; origem já superada → no-op sem novo registro; rejeitar pular/retroceder/avançar terminal.
- [x] 3.4 Endpoint de cancelamento: motivo obrigatório ≥ 10 caracteres; grava `cancellation_reason`, marca `CANCELADO`, grava histórico; rejeitar em pedido terminal ou já cancelado.
- [x] 3.5 Endpoint de toggle `is_open` do tenant (fonte única) — atualiza `tenants.is_open`.
- [x] 3.6 Garantir que telefone do cliente nunca apareça em logs.

## 4. Frontend — tela PEDIDOS (bloco `PEDIDOS`)

- [x] 4.1 Rota autenticada do painel de pedidos no bundle da SPA (React Router), reutilizando o shell do painel.
- [x] 4.2 Header de Pedidos: nome/inicial do negócio, toggle Aberto/Fechado (fonte única) ligado a `is_open`, e banner contextual de abertura (dentro do horário + `is_open=false`, em `America/Fortaleza`).
- [x] 4.3 Card hero de resumo do dia (contagem, receita, em aberto), colapsável, visível quando há pedidos.
- [x] 4.4 Chips de filtro (Todos, Novos, Aceitos, Em preparo, Prontos, Entregues, Cancelados) com contagem por chip.
- [x] 4.5 Lista de cards de pedido (ID curto, cliente, resumo truncado, tipo de entrega + horário, total) com status por cor de acento **e** rótulo textual; badge "NOVO".
- [x] 4.6 Skeleton loaders no carregamento inicial e estado vazio explicativo.

## 5. Frontend — polling e conectividade

- [x] 5.1 TanStack Query com `refetchInterval: 15000` e `refetchIntervalInBackground: false` (respeita `document.visibilityState`).
- [x] 5.2 Indicador de conectividade derivado do estado real da query (erro vs. sucesso) + timestamp da última atualização bem-sucedida (`dataUpdatedAt`); mapear para `connBg/connFg/connIcon/connText/connStamp` do header.
- [x] 5.3 Recuperação: ao voltar a ter sucesso, limpar o indicador e avançar o timestamp.

## 6. Frontend — DETAIL SHEET e ações

- [x] 6.1 Bottom sheet de detalhe (bloco `DETAIL SHEET`): stepper de status, dados do cliente, itens com adicionais/observação a partir dos snapshots, totais discriminados.
- [x] 6.2 Botão de avanço com estado de loading (`cmd-spin`) e desabilitado durante a requisição (previne duplo toque); atualização otimista do card.
- [x] 6.3 Reversão visível em falha de avanço (rollback do cache do TanStack) com erro visível — nenhum estado ambíguo.
- [x] 6.4 Preview da mensagem de WhatsApp por avanço (bloco de "Mensagem para o cliente") com ação opcional "Enviar no WhatsApp" (abre `wa.me` do cliente); avançar funciona com ou sem enviar. Emojis só na mensagem.
- [x] 6.5 Estado terminal: "Pedido concluído" quando `ENTREGUE`; bloco de pedido cancelado com motivo quando `CANCELADO`.

## 7. Frontend — CANCEL SHEET

- [x] 7.1 Bottom sheet de cancelamento (bloco `CANCEL SHEET`): textarea de motivo com contador e validação de mínimo 10 caracteres; confirmar desabilitado até válido.
- [x] 7.2 Confirmar cancelamento chama o endpoint, fecha o sheet e reflete `CANCELADO` na lista e no detalhe.

## 8. Fronteira storefront ↔ order-operation

> **Resolvido:** `public-storefront` foi implementado (53/53 tasks) num worktree paralelo, ainda não mesclado a `main` no momento em que esta change foi construída — por isso o contrato foi reconciliado depois, não durante. O storefront já chamava `POST /api/loja/pedidos` com o corpo `{..., lines: [...]}`; esta change originalmente expunha `POST /api/loja/orders` com `{..., items: [...]}`. Como o lado do storefront já estava pronto/testado e a mudança de nome é puramente cosmética deste lado, `order-operation` foi ajustado para casar exatamente com o que o storefront envia (endpoint renomeado para `/pedidos`, campo para `lines`) em vez do inverso. Verificado ponta a ponta com o payload literal que `CheckoutScreen.finalize()` gera (incluindo o campo extra `lineId` por linha, ignorado pela deserialização padrão do Jackson): pedido criado e visível no board do painel.
>
> De quebra, a revisão encontrou e corrigiu um bug real do lado do storefront: `idempotencyKey` era gerado com `crypto.randomUUID()` **dentro** de `finalize()`, então um duplo-toque antes do re-render do estado `disabled` (ou qualquer retry futuro) enviaria duas keys diferentes — exatamente o duplo-pedido que o contrato existe para prevenir. Corrigido para gerar a key uma vez por tentativa de checkout (`useState` lazy init, estável durante o tempo de vida do componente) com uma guarda síncrona por `ref` contra invocação concorrente.

- [x] 8.1 Ligar o storefront ao endpoint real de criação idempotente (substituir o contrato declarado pela chamada real), mantendo "Pedido enviado" antes do handoff e o fallback de cópia sempre disponível. O storefront já fazia a chamada real (não um mock); o ajuste foi de nomenclatura (`order-operation` passou a casar com o que o storefront já enviava), não de comportamento.
- [x] 8.2 Garantir que a `idempotency_key` gerada por tentativa de finalização seja reusada em retry (idempotência ponta a ponta). Bug corrigido no storefront (ver nota acima); lado servidor já idempotente e testado (9.1/9.2).

## 9. Testes e validação

- [x] 9.1 Teste: criação duplicada com a mesma `idempotency_key` (inclusive concorrente) não cria segundo pedido nem reconta o contador.
- [x] 9.2 Teste: mesma `idempotency_key` em tenants diferentes é isolada (sem colisão/vazamento).
- [x] 9.3 Teste: avanço duplicado (mesma origem) gera apenas um registro de histórico; pular/retroceder/avançar terminal são rejeitados.
- [x] 9.4 Teste: snapshots imutáveis — editar/remover produto após o pedido não altera o pedido existente.
- [x] 9.5 Teste: cancelamento exige motivo ≥ 10 caracteres; rejeita curto/vazio e pedido terminal.
- [x] 9.6 Teste: IDOR — pedido de outro tenant responde 404 em detalhe/avanço/cancelamento; listagem só retorna pedidos do próprio tenant.
- [x] 9.7 Teste de UI: falha de polling mostra conectividade + última atualização; falha de avanço reverte o card. Vitest + Testing Library introduzidos no projeto (nenhuma change anterior tinha test runner de frontend) — `comanda-client/src/features/orders/OrdersScreen.test.tsx` (indicador online/offline + timestamp, mockando os hooks de query) e `useAdvanceOrder.test.tsx` (rollback real do cache do TanStack Query num avanço que falha, com o mock apenas na fronteira de rede).
- [x] 9.8 Rodar `openspec validate order-operation --strict` e corrigir o que apontar.
