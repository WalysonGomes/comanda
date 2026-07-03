## Context

As features estão prontas (cardápio, storefront, operação), mas sem o modelo de SaaS: falta o **freio econômico** (limites de plano) e a **porta de entrada** (onboarding). Esta change amarra as duas. O ponto sensível é onde cada regra mora: a **contagem** de pedidos é produzida por `order-operation` (incrementa `order_count_month` na criação), mas o **limite** de 30/mês é definido **aqui** — fonte única, sem dupla contagem no caminho idempotente. Os limites de produto/categoria atuam nos endpoints de criação de `menu-management`. A lógica de planos é **código desde o MVP** (PRD Seção 6/10, Regra 16): só o processamento de pagamento muda na Fase 2 (manual → Stripe).

Restrições herdadas: Spring Boot 4.1 / Spring 7 / Jakarta EE 11 / Hibernate 7, Java 21 + Virtual Threads, PostgreSQL 17, timezone hardcoded `America/Fortaleza`. Multi-tenancy por `tenant_id` (JWT no painel). Frontend React/Vite SPA, TanStack Query, Zustand, Lucide, tokens do `.design/Comanda Painel.dc.html`. Sem gateway de pagamento no MVP.

Schema reusado de `foundations` (sem migration nova esperada): `subscriptions` (`plan`, `status`, `current_period_end`, `cancelled_at`, `external_subscription_id` nullable), `tenants` (`plan`, `plan_expires_at`, `order_count_month`), e as tabelas de cardápio (`categories`, `products`, `additional_*`, `business_hours`). Blocos de design consumidos: `ONBOARDING` (5 steps), `PLANO E USO`, `MEU LINK`.

## Goals / Non-Goals

**Goals:**
- Enforcement em código dos limites do Gratuito nos pontos de criação: produtos ≤ 30, categorias ≤ 5, pedidos/mês ≤ 30 → **402** com `code` claro; Essencial ilimitado.
- Aviso de cota em 25/30 pedidos do mês.
- Retenção de histórico por plano como **filtro de leitura** (7/30 dias), nunca deletar dados.
- Ativação/rebaixamento **manual** do Essencial, restrito ao operador, independente de gateway.
- Onboarding que termina com cardápio publicado e compartilhável, partindo de um cardápio de demonstração por segmento; comunicar o limite antes de criar a conta.
- Tela "Meu link" com aviso permanente de quebra de subdomínio.

**Non-Goals:**
- Integração Stripe (checkout/recorrência/webhook/downgrade automático) — Fase 2 (Seção 7.5/10).
- Plano Profissional.
- Reescrever o CRUD de `menu-management` ou a criação de pedido de `order-operation` — aqui só o freio de limite em cima deles.
- Persistência de novas tabelas — usa o schema de `foundations`.

## Decisions

### 1. Fonte única da regra de limite: um componente de política de plano
Um serviço de política (`plans`) responde "este tenant pode criar mais um X?" para X ∈ {produto, categoria, pedido}. Os endpoints de criação (`menu-management` para produto/categoria, `order-operation`/`public-storefront` para pedido) chamam a política **antes** de persistir. Se o limite foi atingido e o plano é Gratuito, a política sinaliza e o endpoint responde **402** com o `code` correto e mensagem. Essencial nunca é bloqueado. Assim a regra vive em **um** lugar (Regra: não duplicar a regra de limite), mesmo que os pontos de chamada sejam vários.
- *Por que 402 (Payment Required):* é o código escolhido pelo PRD (Seção 6) — semanticamente "pague para continuar", distinto de 403 (proibido) e 409 (conflito).

### 2. Contagem de pedidos: produzida por `order-operation`, limitada aqui
`order-operation` incrementa `tenants.order_count_month` na **mesma transação** da criação (e o reenvio idempotente **não** reconta). O limite de 30/mês é avaliado por `plans` **antes** de criar: se `order_count_month >= 30` e Gratuito → 402 `PLAN_LIMIT_REACHED`, sem criar o pedido. O aviso de cota é derivado: `order_count_month >= 25` → banner. Evita dupla contagem: a política **lê** o contador, quem **escreve** é a criação.
- *Alternativa (contar linhas de `orders` por mês):* mais caro por request e sensível a fuso; o contador denormalizado em `tenants` é a fonte, resetado na virada de mês.

### 3. Reset mensal de `order_count_month`
Na virada de mês em `America/Fortaleza`, `order_count_month` volta a 0. Estratégia do MVP: **reset preguiçoso** — guardar a competência (ano-mês) do contador; ao ler/incrementar, se a competência mudou, zerar antes de usar. Evita depender de um job cron para correção e é resiliente a downtime na virada. (Se um marcador de competência não existir no schema de `foundations`, adicionar via migration Flyway incremental mínima nesta change; senão, derivar de `updated_at`/uma coluna dedicada — decidir na implementação.)
- *Por que não só cron à meia-noite:* cron pode falhar/reiniciar; o reset preguiçoso é correto mesmo sem execução pontual.

### 4. Retenção de histórico = filtro de leitura, nunca delete
Gratuito: listagem/detalhe de pedidos filtra `created_at >= now - 7 dias`; Essencial: 30 dias. Os dados permanecem no banco (Regra: dados nunca deletados; PRD Seção 8). O filtro é aplicado na query de leitura de `order-operation`, parametrizado pelo plano do tenant resolvido por `plans`.

### 5. Ativação manual do Essencial, restrita ao operador
No MVP não há gateway. A elevação Gratuito → Essencial é uma operação **controlada**: um endpoint administrativo restrito ao operador (autorização fora do fluxo normal de OWNER — ex.: credencial/allowlist de operador, não exposto no painel do tenant) ou operação direta sobre `subscriptions`/`tenants`. Ativar: cria/atualiza `subscriptions` (`plan=ESSENCIAL`, `status=ACTIVE`, `current_period_end`), seta `tenants.plan=ESSENCIAL` e `plan_expires_at`. Rebaixar (falha/cancelamento manual): `plan=GRATUITO`, `subscriptions.status=CANCELLED`, `cancelled_at`. `external_subscription_id` permanece **nulo** (Stripe preenche na Fase 2, sem migration disruptiva).
- *Por que não expor no painel do tenant:* o tenant não deve autoelevar seu plano sem pagamento; a ativação é do operador após o PIX combinado (Seção 10).

### 6. Máquina de planos independente do pagamento
`plans` decide limites/retenção só a partir de `tenants.plan` (+ `plan_expires_at`). Nenhum código de enforcement conhece "como" o pagamento acontece. Na Fase 2, o Stripe apenas passa a **escrever** `plan`/`subscriptions` via webhook no lugar da ativação manual — o enforcement não muda (Regra 16). CTA de assinatura no painel apenas informa "ativação combinada pelo WhatsApp".

### 7. Cardápio de demonstração por segmento (seed no cliente do tenant)
Cada segmento (marmiteria, confeitaria, hamburgueria, açaizeria) tem um conjunto semente de categorias + produtos (nome, descrição, preço) + grupos de adicionais típicos. Ao escolher o segmento no onboarding, o sistema materializa esse seed como dados **reais e editáveis** do tenant (via os serviços de `menu-management`), não placeholders — o dono edita algo pronto (PRD Seção 5.1). O seed respeita os limites do Gratuito (não semear >30 produtos/>5 categorias). Os dados de seed vivem no backend (fatia `onboarding`) como fonte versionada.
- *Por que materializar de verdade:* a promessa é "editar algo pronto"; um preview fake exigiria migração posterior. Dados reais desde o início.

### 8. Onboarding reusa cadastro (owner-auth) e cardápio (menu-management)
O step 1 (conta) é o cadastro de `owner-auth` (tenant + OWNER + subdomínio único), com o **limite do Gratuito comunicado no step 0**, antes de criar a conta (PRD Seção 6). Steps 2–3 (horário, primeiro produto) escrevem via serviços de `menu-management`/config já existentes. O wizard é orquestração no frontend + endpoints finos; nenhuma regra de cardápio é reimplementada.

### 9. "Meu link" e QR no cliente; aviso permanente de subdomínio
A URL é `subdomain.${APP_DOMAIN}`. O QR é gerado no **cliente** (sem dependência de serviço externo). Copiar/compartilhar usam Web Share/`wa.me`. O aviso de que mudar o subdomínio quebra QR Codes impressos e links (bloco `MEU LINK` e onboarding) é **permanente e visível**, não um toast — é regra de UX do PRD (Seção 3.1).

### 10. Tratamento de 402 na UI — fluxo de erro de primeira classe
Todo ponto que pode receber 402 (criar produto/categoria no painel, finalizar pedido no storefront no 31º do mês) mapeia o `code` para uma mensagem clara com caminho (ex.: "Você atingiu o limite de 30 produtos do plano Gratuito — assine o Essencial para adicionar mais"), com a mesma linguagem visual dos tokens. Nenhum 402 silencioso (Seção 4/11). No storefront o cliente final vê uma mensagem apropriada (a loja atingiu o limite), sem expor detalhes de plano do dono.

## Risks / Trade-offs

- **[Dupla contagem de pedidos no retry idempotente]** → a política **lê** `order_count_month`; quem incrementa é `order-operation` (uma vez, mesma transação, reenvio não reconta). Testar: 31º pedido bloqueia; retry do 30º não conta em dobro.
- **[Reset mensal na virada / downtime]** → reset preguiçoso por competência (ano-mês) em vez de depender só de cron; correto mesmo sem execução pontual. Testar virada de mês em `America/Fortaleza`.
- **[Seed de demonstração estourar o limite do Gratuito]** → seeds calibrados ≤ 30 produtos / ≤ 5 categorias por segmento. Testar contagem pós-seed.
- **[Tenant autoelevar plano sem pagar]** → ativação restrita ao operador, fora do painel do tenant; o CTA do painel só informa "combine pelo WhatsApp". Verificar que nenhum endpoint de OWNER eleva o próprio plano.
- **[Fuso hardcoded `America/Fortaleza`]** → competência do contador e janelas de retenção usam esse fuso (Seção 13, Fase 4).
- **[Acoplamento do enforcement a vários pontos de criação]** → mitigado pela política única (`plans`) chamada pelos endpoints; a regra não é copiada. Quando as specs anteriores forem arquivadas, vira delta MODIFIED de menu/orders.
- **[Mudança de subdomínio quebrando links]** → não é bug, é consequência; mitigado por aviso permanente e por manter o subdomínio imutável na prática no MVP (a tela alerta fortemente).

## Migration Plan

Sem migration nova esperada: usa `subscriptions`, `tenants` (`plan`/`plan_expires_at`/`order_count_month`) e as tabelas de cardápio de `foundations`. Se o **reset preguiçoso** exigir um marcador de competência (ano-mês) não presente no schema, adicionar via migration Flyway incremental mínima dentro desta change. `external_subscription_id` permanece nulo (Stripe na Fase 2, sem migration disruptiva). Deploy junto do artefato único; rollback = reverter o build (dados de plano/assinatura preservados; enforcement é lógica, não destrutiva).

## Open Questions

- Marcador de competência do `order_count_month` (coluna ano-mês) — existe em `foundations` ou precisa de migration mínima? Decidir na implementação; preferir reset preguiçoso a cron.
- Superfície exata da ativação manual do operador: tela admin interna simples vs. endpoint restrito por allowlist/credencial de operador. Escolher a mais barata que **não** exponha autoelevação ao tenant.
- Conteúdo dos seeds por segmento (quais produtos/adicionais típicos) — calibrar com exemplos do PRD (marmiteria/confeitaria/hamburgueria/açaizeria), respeitando limites do Gratuito.
- `plan_expires_at`/`current_period_end` no MVP manual: como marcar vencimento sem cobrança automática (provavelmente informativo, com rebaixamento manual). Confirmar na revisão.
