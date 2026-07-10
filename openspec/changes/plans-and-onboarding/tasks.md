## 1. Backend — fatia vertical `plans` (política de plano)

- [x] 1.1 Criar o pacote de fatia vertical `plans` seguindo o padrão de `foundations` (feature-based).
- [x] 1.2 Definir o enum de plano (`GRATUITO`, `ESSENCIAL`) e as constantes de limite do Gratuito (30 produtos, 5 categorias, 30 pedidos/mês) num único lugar.
- [x] 1.3 Implementar o serviço de política de plano: `canCreateProduct(tenant)`, `canCreateCategory(tenant)`, `canCreateOrder(tenant)` — lê `tenants.plan`, conta produtos/categorias do tenant e lê `order_count_month`; Essencial nunca bloqueia.
- [x] 1.4 Definir a exceção de limite e o handler que a traduz em **HTTP 402** com `code` (`PRODUCT_LIMIT_REACHED`, `CATEGORY_LIMIT_REACHED`, `PLAN_LIMIT_REACHED`) e mensagem clara.
- [x] 1.5 Reset mensal de `order_count_month`: reset preguiçoso por competência (ano-mês em `America/Fortaleza`) na leitura/incremento; adicionar coluna de competência via migration Flyway mínima só se não existir em `foundations`.

## 2. Backend — ligar enforcement aos pontos de criação

- [x] 2.1 `menu-management` (criação de produto): consultar `canCreateProduct` antes de persistir; 402 `PRODUCT_LIMIT_REACHED` no 31º do Gratuito.
- [x] 2.2 `menu-management` (criação de categoria): consultar `canCreateCategory` antes de persistir; 402 `CATEGORY_LIMIT_REACHED` na 6ª do Gratuito.
- [x] 2.3 Criação de pedido (`order-operation`/público): consultar `canCreateOrder` antes de criar; 402 `PLAN_LIMIT_REACHED` no 31º do mês do Gratuito. A política **lê** `order_count_month`; o incremento permanece em `order-operation` (não reincrementar aqui; sem dupla contagem no retry idempotente).

## 3. Backend — retenção de histórico por plano

- [x] 3.1 Aplicar filtro de leitura na listagem/detalhe de pedidos: Gratuito 7 dias, Essencial 30 dias (janela em `America/Fortaleza`); nunca deletar dados.
- [x] 3.2 Garantir que elevar o plano volte a expor pedidos antigos (filtro só de leitura).

## 4. Backend — ativação manual do Essencial (operador)

- [x] 4.1 Superfície restrita ao operador (endpoint por allowlist/credencial de operador ou tela admin interna simples), **não** exposta ao OWNER do tenant.
- [x] 4.2 Ativar: gravar `subscriptions` (`plan=ESSENCIAL`, status ativo, `current_period_end`) e `tenants` (`plan=ESSENCIAL`, `plan_expires_at`); `external_subscription_id` permanece nulo.
- [x] 4.3 Rebaixar: `tenants.plan=GRATUITO`, `subscriptions.status=CANCELLED`, `cancelled_at`; limites do Gratuito voltam a valer.
- [x] 4.4 Garantir que nenhum endpoint do painel do OWNER permita autoelevação de plano.

## 5. Backend — fatia vertical `onboarding`

- [x] 5.1 Criar o pacote de fatia vertical `onboarding`; catálogo de **seeds de cardápio de demonstração por segmento** (marmiteria, confeitaria, hamburgueria, açaizeria) como dados versionados no backend, calibrados ≤ 30 produtos / ≤ 5 categorias.
- [x] 5.2 Endpoint/serviço que materializa o seed do segmento escolhido como categorias/produtos/adicionais reais e editáveis do tenant (via serviços de `menu-management`), após o cadastro.
- [x] 5.3 Endpoints finos do wizard (horário de funcionamento, confirmar/editar primeiro produto) reusando serviços de config/`menu-management`; nenhuma regra de cardápio reimplementada.
- [x] 5.4 Geração do payload de "Meu link" (URL `subdomain.${APP_DOMAIN}` + dados para QR).

## 6. Frontend — ONBOARDING (bloco `ONBOARDING`, 5 steps)

- [x] 6.1 Chrome do wizard: dots de progresso, botão voltar, rótulo de passo, botão avançar/concluir (tokens do design).
- [x] 6.2 Step 0 — segmento: cards de segmento (ícone Lucide, label, descrição, check) + caixa informando o limite do Gratuito (30 pedidos/mês) **antes** de criar a conta.
- [x] 6.3 Step 1 — conta: reusar o cadastro de `owner-auth` (nome, negócio, subdomínio único com preview, WhatsApp, e-mail, senha).
- [x] 6.4 Step 2 — horário: linhas por dia da semana com toggle aberto/fechado e faixa de horário (`business_hours`).
- [x] 6.5 Step 3 — primeiro produto: formulário pré-preenchido (foto, nome, descrição, preço) editável, salvando via `menu-management`.
- [x] 6.6 Step 4 — pronto: animação `cmd-check`, link do cardápio, copiar, QR Code (gerado no cliente) e botão compartilhar no WhatsApp; cardápio publicado no subdomínio.

## 7. Frontend — PLANO E USO (bloco `PLANO E USO`)

- [x] 7.1 Card de plano atual (badge Gratuito/Essencial + preço).
- [x] 7.2 Banner de aviso de cota quando `order_count_month >= 25` (Gratuito), com quantos pedidos restam.
- [x] 7.3 Barras de uso (pedidos do mês, produtos, categorias) com cor **e** rótulo indicando proximidade do limite.
- [x] 7.4 Comparativo Gratuito × Essencial + CTA "Assinar o Essencial", com nota de que a ativação é combinada pelo WhatsApp (sem checkout no app).

## 8. Frontend — MEU LINK (bloco `MEU LINK`)

- [x] 8.1 QR Code grande (gerado no cliente), endereço do cardápio, copiar e compartilhar no WhatsApp.
- [x] 8.2 Aviso **permanente e visível** de que alterar o subdomínio quebra QR Codes impressos e links compartilhados.

## 9. Frontend — tratamento de 402 (fluxo de erro de primeira classe)

- [x] 9.1 Mapear `PRODUCT_LIMIT_REACHED` / `CATEGORY_LIMIT_REACHED` no painel para mensagem clara com caminho (assinar Essencial); nenhum estado silencioso.
- [x] 9.2 Mapear `PLAN_LIMIT_REACHED` no storefront (finalização do 31º pedido do mês) para mensagem apropriada ao cliente final, sem expor detalhes de plano do dono.

## 10. Testes e validação

- [x] 10.1 Teste: 31º produto e 6ª categoria no Gratuito retornam 402 com o `code` correto; Essencial não é bloqueado.
- [x] 10.2 Teste: 31º pedido do mês no Gratuito retorna 402 `PLAN_LIMIT_REACHED`; retry idempotente do 30º não conta em dobro; Essencial ilimitado.
- [x] 10.3 Teste: reset mensal — primeiro pedido do novo mês (virada em America/Fortaleza) não é bloqueado.
- [x] 10.4 Teste: aviso de cota aparece em 25/30 e não abaixo; só Gratuito.
- [x] 10.5 Teste: retenção — Gratuito vê 7 dias, Essencial 30; elevar plano reexpõe pedidos antigos; nada é deletado.
- [x] 10.6 Teste: ativação/rebaixamento manual do operador reflete em `subscriptions`/`tenants` com `external_subscription_id` nulo; OWNER não consegue autoelevar.
- [x] 10.7 Teste: seed de demonstração por segmento respeita limites (≤ 30 produtos / ≤ 5 categorias) e gera dados editáveis.
- [x] 10.8 Teste (E2E/UI): wizard de onboarding termina com cardápio publicado no subdomínio + link/QR/compartilhar.
- [x] 10.9 Rodar `openspec validate plans-and-onboarding --strict` e corrigir o que apontar.
