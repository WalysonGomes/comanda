## Why

O `public-storefront` monta o pedido e faz o handoff para o WhatsApp, mas nada é **persistido**: o pedido não existe no backend e o dono não tem onde acompanhá-lo. Esta change fecha o loop **pedido → painel** — persiste o pedido de forma idempotente (nunca duplicar) e entrega o **painel de operação** onde o dono acompanha, avança e cancela pedidos em tempo quase real. É o outro lado do contrato declarado por `public-storefront` e o núcleo da promessa "nunca mais perder pedido".

## What Changes

- **Endpoint público de criação de pedido idempotente** (satisfaz o contrato declarado por `public-storefront`): `idempotency_key` único por tenant, validado cross-tenant (mesma key em tenant diferente é rejeitada/isolada); reenvio com a mesma key no mesmo tenant retorna o **mesmo** pedido, sem duplicar. Tenant sempre resolvido do subdomínio, nunca do corpo.
- **Snapshots imutáveis obrigatórios** no momento do pedido: `product_name_snapshot`, `unit_price_snapshot` por item e `additional_name_snapshot` / `additional_price_snapshot` por adicional (Seção 8 e Regra 7 do PRD). Totais (`subtotal`, `delivery_fee`, `total`) e `address_snapshot` recalculados/gravados no servidor.
- **`order_status_history` append-only**: cada mudança grava `from_status`, `to_status`, `changed_by` (usuário OWNER ou `SYSTEM`/null), `created_at`. Nunca atualizar nem deletar registros.
- **Máquina de estados linear** `Recebido → Aceito → Em preparo → Pronto → Entregue`, com endpoint de avanço **idempotente** (request duplicado não gera dois registros de histórico nem pula etapa).
- **Cancelamento com motivo obrigatório** (mínimo 10 caracteres), a partir de qualquer estado não-terminal, gravando `cancellation_reason` e um registro de histórico.
- **Endpoints autenticados do painel**: listar pedidos do tenant (com filtro por status), obter detalhe de um pedido, avançar status, cancelar. Todos isolados por `tenant_id` do JWT; IDOR bloqueado.
- **Painel do dono (bloco `PEDIDOS`)**: lista com card hero de resumo do dia (colapsável), chips de filtro (Todos, Novos, Aceitos, Em preparo, Prontos, Entregues, Cancelados), cards de pedido com cor de acento + rótulo textual de status, skeleton loaders, estado vazio.
- **Polling automático de 15s** com verificação de `document.visibilityState` (não consome recursos com aba em background); **indicador de conectividade** com timestamp da última atualização bem-sucedida quando o polling falha (nunca silêncio) — no header de Pedidos.
- **Detalhe do pedido em bottom sheet (bloco `DETAIL SHEET`)**: stepper de status, dados do cliente, itens com adicionais/observação a partir dos snapshots, totais, **preview da mensagem de WhatsApp** gerada a cada avanço com ação opcional "Enviar no WhatsApp" (o dono avança **com ou sem** notificar), botão de avanço com **estado de loading** (desabilitado durante a requisição, previne duplo toque); em falha, o card **reverte visivelmente** ao estado anterior com erro (nenhum estado ambíguo/silencioso).
- **Cancelamento em bottom sheet (bloco `CANCEL SHEET`)**: textarea de motivo com contador e mínimo de 10 caracteres, confirmação irreversível.
- **Toggle Aberto/Fechado no header (fonte única, uma vez)** e **banner contextual de abertura** quando o dono acessa dentro do horário configurado e o negócio está `is_open=false`.

Fora de escopo (**out**):
- Notificação automática via WhatsApp API (Fase 4) — aqui só o **preview** e o handoff manual `wa.me`.
- Alerta sonoro / push nativo (Fase 2, depende de `pwa`/nativo).
- Enforcement do limite de pedidos do plano (é `plans-and-onboarding`) — aqui apenas **registrar a contagem** (`order_count_month`) na criação; o contador vive junto da criação, o limite de 30/mês é imposto em `plans-and-onboarding`.
- Retenção/filtro de histórico por plano (7/30 dias) → `plans-and-onboarding`.

## Capabilities

### New Capabilities
- `order-operation`: persistência idempotente de pedido com snapshots imutáveis, máquina de estados linear com avanço idempotente e histórico append-only, cancelamento com motivo obrigatório, e o painel de operação do dono (lista com resumo do dia e filtros, detalhe em bottom sheet, avanço/cancelamento com estado de loading e reversão em falha, preview de mensagem de WhatsApp, polling 15s com indicador de conectividade e última atualização, toggle aberto/fechado e banner de abertura).

### Modified Capabilities
<!-- Nenhuma. As specs de foundations/owner-auth/menu-management/public-storefront ainda não estão
     arquivadas em openspec/specs/. Esta change SATISFAZ o contrato do endpoint de criação de pedido
     declarado por public-storefront (não é modificação de spec existente): o contrato é descrito em
     prosa aqui e implementado como capability nova. Dependências sobre multi-tenancy (JWT no painel,
     subdomínio no endpoint público), design-system e o conteúdo do cardápio (menu-management) são
     descritas em prosa e satisfeitas pelas capabilities já definidas nas changes anteriores. -->

## Impact

- **Backend (novo, fatia vertical `orders`):** endpoint **público** (resolvido por subdomínio, sem autenticação) de criação idempotente de pedido; endpoints **autenticados** (JWT) do painel — listar/filtrar, detalhe, avançar status, cancelar. Toda query filtra por `tenant_id` (do subdomínio no público, do JWT no painel). APIs compatíveis com Spring Boot 4.1 / Spring 7 / Jakarta EE 11 / Hibernate 7; execução sobre Virtual Threads.
- **Banco:** escrita em `orders`, `order_items`, `order_item_additionals`, `order_status_history`, `customers`; leitura de `products`/`additional_*` (para snapshot e validação) e `tenants`/`business_hours`. Constraint de unicidade `(tenant_id, idempotency_key)` já prevista em `foundations`; `order_status_history` append-only por convenção (sem update/delete). Incremento de `tenants.order_count_month` na criação.
- **Frontend (painel, mesma SPA):** rotas autenticadas do painel; tela `PEDIDOS` (header com toggle aberto/fechado, banner de abertura, indicador de conectividade + timestamp; hero colapsável; chips de filtro; cards; skeleton; vazio), `DETAIL SHEET` (stepper, itens, totais, preview WhatsApp, avanço com loading, reversão em falha), `CANCEL SHEET` (motivo mín. 10 chars). TanStack Query para polling/cache/estados; montagem da mensagem de WhatsApp no formato da Seção 3.1 do PRD; ícones Lucide, **sem emoji na UI** (emoji só na mensagem de WhatsApp); status sempre cor + rótulo textual.
- **Dependências:** consome `public-storefront` (é o produtor das requisições de criação — satisfaz seu contrato), `menu-management` (produtos/adicionais para snapshot e validação), `foundations` (`multi-tenancy`, `database-schema`, `design-system`), `owner-auth` (JWT do painel). É pré-requisito de `plans-and-onboarding` (enforcement do limite de pedidos usa o contador registrado aqui) e `pwa` (que instala o painel pronto).
- **Confiabilidade (PRD Seção 4):** idempotência de criação (4.1) e de avanço (4.3); `order_status_history` append-only e rastreável (4.4); polling com `visibilityState` e indicador de conectividade sem silêncio (4.2); reversão visível do card em falha de avanço (4.2); nenhum estado silencioso de falha.
- **Segurança (PRD Seção 9):** IDOR bloqueado em todo acesso por ID (painel filtra por `tenant_id` do JWT; público pelo subdomínio); nenhum dado sensível (telefone do cliente) em logs.
