## Why

O dono já consegue montar o cardápio no painel (`menu-management`), mas nada disso chega ao cliente final: não existe a página pública que transforma o cardápio em pedido. Esta change entrega a **vitrine pública** (`nomedonegocio.${APP_DOMAIN}`) — o cliente navega o cardápio sem login, monta o pedido no carrinho e é entregue no WhatsApp do negócio com a mensagem pré-formatada. É o coração da proposta de valor ("seu cardápio no ar, seus pedidos organizados") e o gatilho de tudo que vem depois (`order-operation`).

## What Changes

- **Resolução de tenant por subdomínio** no contexto público (sem JWT) e carregamento do cardápio público (negócio, horários, categorias e produtos disponíveis).
- **Regra de disponibilidade na leitura**: produto fora do `available_days` de hoje ou com `is_available=false` **não aparece** no cardápio (não é listado como desabilitado). Aplica a convenção `0=Dom … 6=Sáb` no timezone `America/Fortaleza`.
- **Home do cardápio** (bloco `STOREFRONT`): mobile-first, coluna única, chips de categoria sticky, cartão do negócio com status aberto/fechado, taxa de entrega e mínimo.
- **Detalhe do produto** em bottom sheet (bloco `PRODUCT SHEET`): grupos de adicionais (obrigatórios bloqueiam "Adicionar" com motivo visível até seleção válida), seletor de quantidade, observação.
- **Carrinho persistente no navegador** (bloco `CART SHEET`): edição de quantidade e de adicionais no próprio carrinho (não forçar remover e recriar); transparência de preço (subtotal, taxa, total sempre discriminados); bloqueio de pedido mínimo mostrando quanto falta.
- **Checkout em tela única** (bloco `CHECKOUT`): nome, telefone, tipo de entrega (Entrega/Retirada), endereço só quando Entrega; validação inline em tempo real.
- **Validação de disponibilidade na finalização**: item que ficou indisponível durante a sessão gera erro claro por item, com instrução para remover.
- **Confirmação "Pedido enviado" antes do handoff** e **handoff para o WhatsApp** do negócio com a mensagem pré-formatada no formato exato da Seção 3.1 do PRD (emojis permitidos **apenas** nessa mensagem).
- **Fallback obrigatório de cópia de mensagem** (bloco `SENT/HANDOFF`): se o `wa.me` não abrir, a mensagem aparece na tela com botão "Copiar mensagem" — tratado como tela de primeira classe.
- **Estado de restaurante fechado** (avisos flutuantes de loja fechada): permite navegar e montar o carrinho, **bloqueia a finalização**, explica o porquê e quando reabre.

Fronteira / dependência explícita (**order-operation**):
- Nesta change o storefront **monta** o pedido e faz o **handoff** para o WhatsApp. A **persistência** do pedido (criação idempotente por `idempotency_key`, snapshots, `order_status_history`) e o painel de operação ficam em `order-operation`. Esta change **declara o contrato** do endpoint de criação idempotente de pedido como dependência a ser satisfeita por `order-operation`; a implementação de storefront consome esse contrato.

Fora de escopo (**out**):
- Persistência/estado do pedido e painel de operação → `order-operation`.
- Login/cadastro do cliente (o cliente **nunca** faz login).
- Pagamento no app (pagamento acontece no WhatsApp, fora do produto).
- CRUD de cardápio (é `menu-management`); enforcement de limite de plano (é `plans-and-onboarding`).

## Capabilities

### New Capabilities
- `public-storefront`: vitrine pública sem login — resolução de tenant por subdomínio, leitura do cardápio filtrada por disponibilidade do dia/estado, detalhe de produto com adicionais e validação de obrigatórios, carrinho persistente com transparência de preço e pedido mínimo, checkout em tela única com validação inline, validação de disponibilidade na finalização, confirmação de envio e handoff para o WhatsApp com mensagem pré-formatada, fallback de cópia da mensagem, e estado de loja fechada que bloqueia a finalização.

### Modified Capabilities
<!-- Nenhuma. As specs de foundations/owner-auth/menu-management ainda não estão arquivadas em
     openspec/specs/. A dependência sobre multi-tenancy (resolução por subdomínio), database-schema
     e a leitura do cardápio (categories/products/additionals de menu-management) é descrita em prosa
     e satisfeita pelas capabilities já definidas nas changes anteriores.
     O contrato do endpoint de criação de pedido é declarado aqui como dependência a ser
     satisfeita por order-operation (não é uma modificação de spec existente). -->

## Impact

- **Backend (novo, fatia vertical `storefront`):** endpoints **públicos** (sem autenticação, resolvidos por subdomínio) para carregar dados do negócio + cardápio disponível e para **validar disponibilidade** de um carrinho na finalização. O endpoint de **criação de pedido idempotente** é definido como contrato aqui e implementado em `order-operation`. Todas as queries filtram por `tenant_id` resolvido do subdomínio (nunca de input do cliente). APIs compatíveis com Spring Boot 4.1 / Spring 7 / Jakarta EE 11.
- **Banco:** somente leitura das tabelas de `menu-management` (`categories`, `products`, `additional_groups`, `additional_items`) e do `tenants`/`business_hours`; nenhuma migration nova esperada nesta change (a escrita de `orders` mora em `order-operation`).
- **Frontend (novo bundle de storefront, mesma SPA):** rotas públicas separadas do painel; home do cardápio, bottom sheets de produto e carrinho, checkout, tela Sent/Handoff com fallback de cópia, avisos de loja fechada; carrinho em Zustand com persistência no `localStorage`; montagem da mensagem de WhatsApp no formato exato do PRD; ícones Lucide, **sem emoji na UI** (emoji só na mensagem de WhatsApp).
- **Dependências:** consome `menu-management` (conteúdo do cardápio), `foundations` (`multi-tenancy` por subdomínio, `design-system`); declara dependência de contrato sobre `order-operation` (criação idempotente do pedido). É pré-requisito de `order-operation` (que fecha o loop pedido → painel).
- **Segurança (PRD Seção 9):** endpoints públicos não expõem dados de outro tenant; o tenant vem sempre do subdomínio resolvido no servidor, nunca de parâmetro controlado pelo cliente; nenhum dado sensível (telefone) em logs.
