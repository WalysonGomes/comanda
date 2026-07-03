## ADDED Requirements

### Requirement: Enforcement do limite de produtos do plano Gratuito
O sistema SHALL impedir que um tenant no plano **Gratuito** possua mais de **30 produtos**. Ao tentar criar o 31º produto, o sistema SHALL responder **HTTP 402** com `code: PRODUCT_LIMIT_REACHED` e uma mensagem clara, sem persistir o produto. Tenants no plano **Essencial** NÃO SHALL sofrer esse limite (produtos ilimitados). A verificação SHALL considerar apenas os produtos do próprio tenant.

#### Scenario: Criar o 31º produto no Gratuito é bloqueado
- **WHEN** um tenant Gratuito com 30 produtos tenta criar mais um
- **THEN** o sistema responde 402 com `code: PRODUCT_LIMIT_REACHED` e mensagem clara, e o produto não é criado

#### Scenario: Dentro do limite cria normalmente
- **WHEN** um tenant Gratuito com menos de 30 produtos cria um produto
- **THEN** o produto é criado normalmente, sem 402

#### Scenario: Essencial não tem limite de produtos
- **WHEN** um tenant Essencial cria seu 31º (ou 100º) produto
- **THEN** o produto é criado normalmente, sem 402

### Requirement: Enforcement do limite de categorias do plano Gratuito
O sistema SHALL impedir que um tenant no plano **Gratuito** possua mais de **5 categorias**. Ao tentar criar a 6ª categoria, o sistema SHALL responder **HTTP 402** com `code: CATEGORY_LIMIT_REACHED` e uma mensagem clara, sem persistir a categoria. Tenants no plano **Essencial** NÃO SHALL sofrer esse limite (categorias ilimitadas).

#### Scenario: Criar a 6ª categoria no Gratuito é bloqueada
- **WHEN** um tenant Gratuito com 5 categorias tenta criar mais uma
- **THEN** o sistema responde 402 com `code: CATEGORY_LIMIT_REACHED` e mensagem clara, e a categoria não é criada

#### Scenario: Essencial não tem limite de categorias
- **WHEN** um tenant Essencial cria sua 6ª categoria
- **THEN** a categoria é criada normalmente, sem 402

### Requirement: Enforcement do limite de pedidos mensais do plano Gratuito
O sistema SHALL impedir que um tenant no plano **Gratuito** ultrapasse **30 pedidos no mês corrente**. A regra de limite é definida por esta capability (fonte única): antes de criar um pedido, se `tenants.order_count_month >= 30` e o plano é Gratuito, o sistema SHALL responder **HTTP 402** com `code: PLAN_LIMIT_REACHED` e mensagem clara, sem criar o pedido. O contador `order_count_month` é **incrementado por `order-operation` na criação** (uma vez por pedido; reenvio idempotente não reconta) — esta capability apenas **lê** o contador para decidir o bloqueio, sem incrementá-lo. Tenants **Essencial** NÃO SHALL sofrer esse limite (pedidos ilimitados).

#### Scenario: 31º pedido do mês no Gratuito é bloqueado
- **WHEN** um tenant Gratuito já tem 30 pedidos no mês e um novo pedido é submetido
- **THEN** o sistema responde 402 com `code: PLAN_LIMIT_REACHED` e mensagem clara, e nenhum pedido é criado

#### Scenario: Retry idempotente do último pedido não conta em dobro
- **WHEN** o 30º pedido do mês é reenviado com a mesma `idempotency_key` (retry de rede)
- **THEN** o contador permanece em 30, o pedido não é duplicado, e um pedido novo e distinto ainda seria o 31º (bloqueado)

#### Scenario: Essencial não tem limite de pedidos mensais
- **WHEN** um tenant Essencial cria seu 31º pedido do mês
- **THEN** o pedido é criado normalmente, sem 402

### Requirement: Reset mensal do contador de pedidos
O sistema SHALL reiniciar a contagem de pedidos do mês (`order_count_month`) na virada de mês no fuso `America/Fortaleza`, de forma que os limites do plano Gratuito sejam avaliados sobre o mês corrente. O reset SHALL ser resiliente à ausência de execução pontual (ex.: reset preguiçoso por competência ano-mês na leitura/incremento), de modo que a contagem do novo mês comece em zero mesmo que nenhum job tenha rodado exatamente à meia-noite.

#### Scenario: Contagem zera no novo mês
- **WHEN** um tenant chega ao fim do mês com 30 pedidos e o mês vira em America/Fortaleza
- **THEN** no primeiro pedido do novo mês a contagem é tratada como 1 (não 31) e o pedido não é bloqueado

### Requirement: Aviso de cota de pedidos ao aproximar do limite
O sistema SHALL sinalizar ao dono, no painel, quando o tenant Gratuito atingir **25 ou mais** pedidos no mês (`order_count_month >= 25`), antes de bater o limite de 30. O aviso SHALL ser exibido no bloco de plano/uso com mensagem clara de quantos pedidos restam. Tenants Essencial NÃO SHALL receber esse aviso.

#### Scenario: Banner de aviso em 25/30
- **WHEN** o dono de um tenant Gratuito com 26 pedidos no mês abre a tela de plano/uso
- **THEN** um banner informa que ele usou 26 dos 30 pedidos do mês e que faltam poucos para o limite

#### Scenario: Abaixo de 25 não mostra aviso
- **WHEN** o tenant Gratuito tem menos de 25 pedidos no mês
- **THEN** nenhum aviso de cota é exibido

### Requirement: Retenção de histórico de pedidos por plano
O sistema SHALL limitar a **visibilidade** do histórico de pedidos conforme o plano do tenant, aplicando um **filtro de leitura**: Gratuito enxerga pedidos dos últimos **7 dias**; Essencial, dos últimos **30 dias**. O sistema NUNCA SHALL deletar dados de pedido para aplicar essa retenção — os registros permanecem no banco e voltam a ser visíveis se o plano for elevado. A janela SHALL ser calculada no fuso `America/Fortaleza`.

#### Scenario: Gratuito vê 7 dias
- **WHEN** um tenant Gratuito lista o histórico de pedidos
- **THEN** o sistema retorna apenas pedidos dos últimos 7 dias, sem deletar os mais antigos

#### Scenario: Essencial vê 30 dias
- **WHEN** um tenant Essencial lista o histórico de pedidos
- **THEN** o sistema retorna pedidos dos últimos 30 dias

#### Scenario: Elevar plano volta a mostrar histórico antigo
- **WHEN** um tenant Gratuito com pedidos de 20 dias atrás (ocultos) é elevado a Essencial
- **THEN** os pedidos de até 30 dias voltam a aparecer, pois nunca foram deletados

### Requirement: Ativação manual do plano Essencial pelo operador
O sistema SHALL permitir uma ativação **manual e controlada**, restrita ao **operador** da plataforma (não ao OWNER do tenant), que eleva um tenant de Gratuito para Essencial e o rebaixa de volta. A ativação SHALL registrar/atualizar `subscriptions` (`plan=ESSENCIAL`, `status` ativo, `current_period_end`) e refletir em `tenants` (`plan=ESSENCIAL`, `plan_expires_at`). O rebaixamento SHALL marcar o plano como Gratuito e a assinatura como cancelada (`cancelled_at`). O campo `subscriptions.external_subscription_id` SHALL permanecer **nulo** no MVP. Nenhum endpoint acessível ao OWNER do tenant SHALL permitir que ele eleve o próprio plano sem essa ativação do operador.

#### Scenario: Operador ativa Essencial
- **WHEN** o operador ativa o Essencial para um tenant Gratuito
- **THEN** o tenant passa a Essencial, `subscriptions` reflete o plano ativo, `external_subscription_id` permanece nulo, e os limites do Gratuito deixam de se aplicar

#### Scenario: Operador rebaixa para Gratuito
- **WHEN** o operador rebaixa um tenant Essencial (falha/cancelamento de pagamento tratado manualmente)
- **THEN** o tenant volta a Gratuito, a assinatura é marcada como cancelada, e os limites do Gratuito voltam a valer

#### Scenario: OWNER não pode autoelevar o plano
- **WHEN** o OWNER de um tenant Gratuito tenta, por qualquer endpoint do painel, elevar seu próprio plano para Essencial
- **THEN** o sistema não eleva o plano; a mudança só ocorre por ativação do operador

### Requirement: Máquina de planos independente do processamento de pagamento
A lógica de planos (limites, retenção, avaliação de plano) SHALL depender apenas de `tenants.plan` (e `plan_expires_at`), sem conhecer o meio de processamento do pagamento. A troca futura de cobrança manual para gateway (Stripe, Fase 2) NÃO SHALL exigir alteração das regras de enforcement — apenas de quem escreve o plano/assinatura.

#### Scenario: Enforcement não depende de gateway
- **WHEN** o plano de um tenant é definido (seja por ativação manual, seja futuramente por webhook de gateway)
- **THEN** os limites e a retenção são avaliados unicamente a partir do plano registrado, sem depender de como o pagamento foi processado

### Requirement: Plano e uso visíveis no painel
O sistema SHALL exibir no painel do dono (bloco `PLANO E USO`) o **plano atual** (Gratuito ou Essencial) como badge, e o **uso** do tenant em barras de progresso para pedidos do mês, produtos e categorias, com indicação visual da proximidade do limite (cor + rótulo textual, nunca cor sozinha). Para o Gratuito, SHALL apresentar o comparativo com o Essencial e um CTA de assinatura cuja ativação, no MVP, é comunicada como **combinada pelo WhatsApp** (sem checkout embutido).

#### Scenario: Uso e plano exibidos
- **WHEN** o dono abre a tela de plano e uso
- **THEN** vê o badge do plano atual e barras de uso (pedidos do mês, produtos, categorias) com valores atuais e limites, cada uma com cor e rótulo

#### Scenario: CTA de assinatura no MVP
- **WHEN** um tenant Gratuito visualiza a oferta do Essencial
- **THEN** o CTA informa que a ativação é combinada pelo WhatsApp, sem abrir um checkout de pagamento no app
