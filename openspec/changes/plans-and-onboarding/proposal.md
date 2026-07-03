## Why

As changes de feature anteriores entregaram cardápio, storefront e operação de pedidos — mas **sem freio econômico e sem porta de entrada**. Falta o que transforma o Comanda num SaaS: (1) o **modelo de planos com enforcement em código**, que cria a pressão de upgrade independentemente de como o pagamento é processado (Seção 6 e 10 do PRD), e (2) o **onboarding** que cumpre a promessa "cardápio no ar em menos de 10 minutos" (Seção 1 e 5.1) — o dono edita um cardápio de demonstração pronto em vez de encarar uma tela vazia. Esta change amarra as duas pontas: impõe os limites do Gratuito nos pontos onde recursos são criados (produtos, categorias, pedidos), reflete plano e uso no painel, permite ativação **manual** do Essencial no MVP (sem gateway), e entrega o wizard de onboarding que termina com o cardápio publicado e compartilhável.

## What Changes

- **Enforcement dos limites do plano Gratuito em código** (PRD Seção 6), nos pontos de criação já existentes:
  - Até **30 produtos** por tenant → 31º retorna **HTTP 402** com `code: PRODUCT_LIMIT_REACHED`.
  - Até **5 categorias** por tenant → 6ª retorna **402** com `code: CATEGORY_LIMIT_REACHED`.
  - Até **30 pedidos/mês** por tenant → 31º do mês retorna **402** com `code: PLAN_LIMIT_REACHED`. O contador (`tenants.order_count_month`) é **incrementado por `order-operation` na criação**; o **bloqueio** mora aqui (fonte única da regra de limite). Reset na virada de mês em `America/Fortaleza`.
  - Cada 402 carrega mensagem clara para a UI; o tenant Essencial não sofre nenhum desses limites (ilimitado).
- **Aviso de cota de pedidos** ao atingir **25/30** no mês (`order_count_month >= 25`): banner no painel (bloco `PLANO E USO`) antes de bater o teto.
- **Retenção de histórico por plano via filtro de leitura, sem deletar dados** (PRD Seção 6 e decisão de modelo): Gratuito enxerga pedidos dos últimos **7 dias**, Essencial **30 dias**. É filtro na query de listagem/detalhe — os dados **nunca** são removidos do banco.
- **Ativação manual do plano Essencial** no MVP (PRD Seção 10): uma operação **controlada e restrita ao operador** (você) que eleva um tenant Gratuito → Essencial e o rebaixa de volta, refletindo em `subscriptions` e no `plan`/`plan_expires_at` do tenant. Rebaixamento por falha/cancelamento de pagamento é manual. `subscriptions.external_subscription_id` permanece **nulo** no MVP (preenchido quando Stripe integrar, sem migration disruptiva).
- **Plano e uso no painel (bloco `PLANO E USO`)**: badge do plano atual (Gratuito/Essencial), barras de progresso de uso (pedidos do mês, produtos, categorias) com cor que reflete proximidade do limite, comparativo Gratuito × Essencial, e CTA de assinatura cuja ativação é **combinada pelo WhatsApp** no MVP (sem checkout embutido).
- **Onboarding — seleção de segmento + cardápio de demonstração** (bloco `ONBOARDING`, step 0): o dono escolhe o segmento do negócio (ex.: marmiteria, confeitaria, hamburgueria, açaizeria) e o sistema **pré-popula um cardápio de demonstração** por segmento (categorias, produtos com preço/descrição, grupos de adicionais típicos) — o dono edita algo pronto. O limite do plano Gratuito é **comunicado neste passo, antes de criar a conta** (PRD Seção 6). O passo de conta (step 1) reusa o cadastro de `owner-auth`.
- **Onboarding — wizard curto** (bloco `ONBOARDING`, steps 2–4): horário de funcionamento → confirmar/editar o primeiro produto → tela **"Pronto! Seu cardápio está no ar"** com link, copiar, compartilhar no WhatsApp e **QR Code**. Indicador de progresso (dots) e navegação voltar/avançar.
- **Tela "Meu link" (bloco `MEU LINK`)**: QR Code grande, endereço do cardápio, copiar, compartilhar no WhatsApp, e **aviso permanente** de que alterar o subdomínio **quebra os QR Codes impressos e os links já compartilhados** (PRD Seção 3.1 / 3.1 "Meu link").

Fora de escopo (**out**):
- **Integração com Stripe** (checkout, recorrência, webhooks, downgrade automático) — Fase 2, deliberadamente adiada para não bloquear o caminho crítico (PRD Seção 7.5 e 10). No MVP a cobrança é manual.
- **Plano Profissional** — só entra quando houver usuários no Essencial pedindo mais (PRD Seção 6).
- **Persistência básica** de produtos/categorias/pedidos e suas telas — já entregues por `menu-management` e `order-operation`. Aqui só entra o **freio de limite** em cima delas (não duplicar a regra de CRUD).
- **Recuperação/reset mensal automatizado sofisticado** além de zerar `order_count_month` na virada de mês.

## Capabilities

### New Capabilities
- `plans`: modelo de planos (Gratuito/Essencial) com **enforcement em código** dos limites do Gratuito (produtos ≤ 30, categorias ≤ 5, pedidos/mês ≤ 30) retornando 402 com códigos claros; aviso de cota em 25/30 pedidos; retenção de histórico por plano via filtro de leitura (7/30 dias) sem deletar dados; ativação/rebaixamento **manual** do Essencial restrito ao operador, refletido em `subscriptions`/`tenants`, independente de processamento de pagamento; badge de plano e barras de uso no painel.
- `onboarding`: seleção de segmento com **cardápio de demonstração** pré-populado por segmento; comunicação do limite do Gratuito antes de criar a conta; wizard curto (horário → primeiro produto → "pronto") que termina com o cardápio **publicado e compartilhável** (link, copiar, compartilhar no WhatsApp, QR Code); tela "Meu link" com aviso permanente sobre a quebra de QR Codes/links ao mudar o subdomínio.

### Modified Capabilities
<!-- Nenhuma spec MODIFIED formal: as specs de menu-management, order-operation e owner-auth ainda
     não estão arquivadas em openspec/specs/. O enforcement de limite atua nos endpoints de criação
     de produto/categoria (menu-management) e de pedido (order-operation), e o onboarding reusa o
     cadastro de owner-auth — essas fronteiras são descritas em prosa e satisfeitas pelas
     capabilities já definidas nas changes anteriores. O contador order_count_month é PRODUZIDO por
     order-operation na criação; a regra de LIMITE é definida aqui (fonte única), sem dupla contagem
     no caminho idempotente. Quando as specs anteriores forem arquivadas, o enforcement pode virar
     delta MODIFIED de menu/orders na revisão de reliability-and-security. -->

## Impact

- **Backend (fatia vertical `plans`):** um componente de política de plano consultável pelos pontos de criação (`menu-management`: produto/categoria; `order-operation`: pedido) que decide se o limite foi atingido e lança o erro traduzido em **402** com o `code` correto; leitura de `tenants.plan`/`plan_expires_at`, contagem de produtos/categorias por tenant e de `order_count_month`; aviso derivado em 25/30. Filtro de retenção (7/30 dias por plano) aplicado na leitura de pedidos. Operação **manual** de ativação/rebaixamento do Essencial (endpoint restrito ao operador ou tela admin interna simples) escrevendo em `subscriptions` e `tenants`. Reset de `order_count_month` na virada de mês (`America/Fortaleza`). APIs compatíveis com Spring Boot 4.1 / Spring 7 / Jakarta EE 11 / Hibernate 7; Virtual Threads.
- **Backend (fatia vertical `onboarding`):** catálogo de **cardápios de demonstração por segmento** (dados semente aplicados ao tenant recém-criado); endpoint(s) do wizard (horário, primeiro produto) reusando os serviços de `menu-management` e `owner-auth`; geração do payload de "Meu link" (URL do subdomínio + dados para QR). Nenhuma tabela nova (usa `categories`/`products`/`additional_*`/`business_hours` já existentes).
- **Banco:** sem migration nova esperada (usa `subscriptions`, `tenants.plan`/`plan_expires_at`/`order_count_month`, `categories`, `products`, `business_hours` de `foundations`). `subscriptions.external_subscription_id` permanece nulo. Adicionar índice de contagem só se o plano de query justificar.
- **Frontend (painel + onboarding, mesma SPA):** bloco `PLANO E USO` (badge, barras de uso, comparativo, CTA "combinar pelo WhatsApp", banner 25/30); bloco `MEU LINK` (QR grande, copiar/compartilhar, aviso permanente de quebra de subdomínio); bloco `ONBOARDING` completo (5 steps: segmento+aviso de limite, conta, horário, primeiro produto, pronto com link+QR+compartilhar). Tratamento de **402** em todas as telas de criação (produto, categoria, e no storefront ao finalizar pedido no 31º do mês) com mensagem clara e caminho — nenhum estado silencioso. QR gerado no cliente. Ícones Lucide, sem emoji na UI (emoji só na mensagem/compartilhamento de WhatsApp); status/uso sempre com cor **e** rótulo.
- **Dependências:** depende de **todas** as changes de feature anteriores — `foundations` (`multi-tenancy`, `database-schema`: `subscriptions`/`tenants`/menu, `design-system`), `owner-auth` (cadastro reusado no step de conta, JWT do painel), `menu-management` (criação de produto/categoria onde o limite atua; serviços reusados no demo/wizard), `public-storefront` (o 402 de pedido do mês aparece na finalização), `order-operation` (produz `order_count_month`; a retenção filtra sua listagem). É pré-requisito lógico de `pwa` (painel completo) e alinhado com `reliability-and-security` (o 402 e o aviso são fluxos de erro de primeira classe).
- **Regra de negócio (PRD Seção 6/10, Regra 16):** a lógica de planos é **código desde o MVP**; o que muda na Fase 2 é apenas o *processamento do pagamento* (manual → Stripe), não a máquina de planos. Enforcement e ativação são independentes de gateway.
- **Confiabilidade/UX (PRD Seção 4/10):** limite atingido, aviso de cota e mudança de subdomínio são fluxos com tela e mensagem clara; nenhum 402 silencioso.
