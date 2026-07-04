# order-operation Specification

## Purpose
TBD - created by syncing change order-operation. Update Purpose after archive.

## Requirements
### Requirement: Criação idempotente de pedido
O sistema SHALL expor um endpoint **público** (resolvido pelo subdomínio, sem autenticação) que cria um pedido para o tenant do subdomínio. A criação SHALL ser idempotente por `idempotency_key`: a constraint de unicidade é `(tenant_id, idempotency_key)` e o reenvio com a **mesma** key no **mesmo** tenant SHALL retornar o pedido já existente (mesmo `id`, mesmo total), sem criar um segundo pedido nem um segundo registro de histórico. O `tenant_id` SHALL vir sempre do subdomínio resolvido no servidor, nunca do corpo da requisição. O pedido é criado no status inicial `RECEBIDO`, o que grava o primeiro registro em `order_status_history` (`from_status` nulo, `to_status = RECEBIDO`, `changed_by = SYSTEM`), e SHALL incrementar `tenants.order_count_month`.

#### Scenario: Criar pedido novo
- **WHEN** o storefront envia uma requisição de criação com uma `idempotency_key` inédita no tenant, dados do cliente, `delivery_type`, endereço (se ENTREGA), observação e as linhas do pedido
- **THEN** o sistema cria o pedido no status `RECEBIDO` vinculado ao `tenant_id` do subdomínio, grava o registro inicial de histórico, incrementa o contador de pedidos do mês e retorna o pedido criado (id curto e total)

#### Scenario: Reenvio com a mesma idempotency_key não duplica
- **WHEN** o storefront reenvia a criação com uma `idempotency_key` já usada no mesmo tenant (ex.: retry por timeout de rede)
- **THEN** o sistema retorna o **mesmo** pedido já criado, sem criar um segundo pedido, sem novo registro de histórico e sem incrementar o contador novamente

#### Scenario: Mesma idempotency_key em tenant diferente é isolada
- **WHEN** dois tenants distintos criam pedidos com o mesmo valor de `idempotency_key`
- **THEN** cada tenant obtém seu próprio pedido (a unicidade é por `(tenant_id, idempotency_key)`), sem colisão nem vazamento entre tenants

#### Scenario: tenant_id no corpo é ignorado
- **WHEN** a requisição de criação inclui um `tenant_id` (ou identificador de tenant) no corpo diferente do subdomínio
- **THEN** o sistema ignora o valor do corpo e usa exclusivamente o tenant resolvido do subdomínio

### Requirement: Snapshots imutáveis no pedido
Todo pedido criado SHALL gravar snapshots imutáveis do estado do cardápio no momento do pedido: por item, `product_name_snapshot`, `unit_price_snapshot`, `quantity` e `subtotal`; por adicional escolhido, `additional_name_snapshot` e `additional_price_snapshot`. O endereço (quando ENTREGA) SHALL ser gravado como `address_snapshot`. `subtotal`, `delivery_fee` e `total` SHALL ser calculados e gravados pelo servidor a partir dos preços atuais dos produtos/adicionais e da `delivery_fee` do tenant, **não** a partir de valores enviados pelo cliente. Alterações posteriores no cardápio (nome, preço, remoção) NÃO SHALL alterar os snapshots de pedidos já criados.

#### Scenario: Snapshots gravados na criação
- **WHEN** um pedido é criado com itens e adicionais
- **THEN** o sistema grava nome e preço de cada produto e adicional como snapshot no pedido, calcula subtotal/taxa/total no servidor e não confia em preços vindos do cliente

#### Scenario: Mudança de cardápio não altera pedido existente
- **WHEN** o OWNER edita o preço ou o nome de um produto (ou o remove) depois de um pedido já ter sido criado com aquele produto
- **THEN** o pedido já existente continua exibindo o nome e o preço do momento em que foi feito (snapshot inalterado)

### Requirement: Máquina de estados linear com avanço idempotente
O sistema SHALL progredir o status do pedido de forma **linear**: `RECEBIDO → ACEITO → EM_PREPARO → PRONTO → ENTREGUE`. O endpoint de avanço (autenticado, painel) SHALL mover o pedido para o próximo status na sequência e gravar um registro em `order_status_history` (`from_status`, `to_status`, `changed_by` = usuário OWNER, `created_at`). O avanço SHALL ser idempotente: para prevenir duplo toque, a operação é parametrizada pelo status atual esperado — uma segunda requisição idêntica (mesmo pedido, mesmo status de origem já superado) NÃO SHALL gerar um segundo registro de histórico nem pular uma etapa. O sistema NÃO SHALL permitir pular estados nem retroceder. `ENTREGUE` é terminal (sem próximo avanço).

#### Scenario: Avançar um estado
- **WHEN** o OWNER avança um pedido que está em `RECEBIDO`
- **THEN** o pedido passa a `ACEITO`, um registro de histórico é gravado com `from_status=RECEBIDO`, `to_status=ACEITO` e `changed_by` do OWNER

#### Scenario: Avanço duplicado não gera dois registros
- **WHEN** o painel envia duas vezes o avanço a partir do mesmo status de origem (ex.: duplo toque ou retry) para o mesmo pedido
- **THEN** o pedido avança uma única vez e apenas um registro de histórico é gravado; a segunda requisição é reconhecida como duplicada e não produz efeito adicional

#### Scenario: Pular estado é rejeitado
- **WHEN** o painel tenta avançar um pedido diretamente de `RECEBIDO` para `PRONTO` (pulando etapas)
- **THEN** o sistema rejeita a operação e o status permanece inalterado

#### Scenario: Avançar pedido entregue é rejeitado
- **WHEN** o painel tenta avançar um pedido que já está em `ENTREGUE`
- **THEN** o sistema rejeita a operação por ser um estado terminal e nenhum registro é gravado

### Requirement: Histórico de status append-only
`order_status_history` SHALL ser append-only: o sistema NUNCA SHALL atualizar nem deletar registros existentes. Cada mudança de status (criação, cada avanço, cancelamento) SHALL gerar exatamente um novo registro contendo `from_status`, `to_status`, `changed_by_user_id` (nulo quando a origem é `SYSTEM`) e `created_at`.

#### Scenario: Cada transição adiciona um registro
- **WHEN** um pedido passa por criação e avanços sucessivos
- **THEN** cada transição adiciona um novo registro ao histórico, na ordem em que ocorreram, sem sobrescrever os anteriores

#### Scenario: Mudança de origem SYSTEM registra changed_by nulo
- **WHEN** uma transição é originada pelo sistema (ex.: registro inicial na criação do pedido)
- **THEN** o registro grava `changed_by_user_id` nulo, indicando origem `SYSTEM`

### Requirement: Cancelamento de pedido com motivo obrigatório
O sistema SHALL permitir ao OWNER cancelar um pedido em qualquer estado não-terminal (não `ENTREGUE` e não já cancelado). O cancelamento SHALL exigir um motivo com no mínimo 10 caracteres; motivo ausente ou mais curto SHALL ser rejeitado sem alterar o pedido. Ao cancelar, o sistema SHALL gravar `cancellation_reason`, marcar o pedido como `CANCELADO` e adicionar um registro em `order_status_history` (`to_status=CANCELADO`, `changed_by` do OWNER).

#### Scenario: Cancelar com motivo válido
- **WHEN** o OWNER cancela um pedido não-terminal informando um motivo com 10 ou mais caracteres
- **THEN** o pedido passa a `CANCELADO`, o motivo é persistido em `cancellation_reason` e um registro de histórico é gravado

#### Scenario: Motivo com menos de 10 caracteres é rejeitado
- **WHEN** o OWNER tenta cancelar informando um motivo com menos de 10 caracteres (ou em branco)
- **THEN** o sistema rejeita com erro de validação, o pedido não é cancelado e nenhum registro é gravado

#### Scenario: Cancelar pedido entregue é rejeitado
- **WHEN** o OWNER tenta cancelar um pedido já `ENTREGUE`
- **THEN** o sistema rejeita a operação e o pedido permanece `ENTREGUE`

### Requirement: Isolamento por tenant no painel de pedidos
Todos os endpoints autenticados de pedido (listar, detalhe, avançar, cancelar) SHALL filtrar por `tenant_id` derivado do JWT, nunca de parâmetro controlado pelo cliente. Um pedido referenciado por ID que pertença a outro tenant SHALL responder como recurso inexistente (404) e NÃO SHALL expor seus dados. Nenhum dado sensível do cliente (telefone) SHALL aparecer em logs.

#### Scenario: Pedido de outro tenant é inacessível
- **WHEN** um OWNER referencia por ID (detalhe, avanço ou cancelamento) um pedido pertencente a outro tenant
- **THEN** o sistema responde 404 e não realiza a ação nem expõe os dados do pedido

#### Scenario: Listagem só retorna pedidos do próprio tenant
- **WHEN** um OWNER lista os pedidos
- **THEN** o sistema retorna apenas pedidos cujo `tenant_id` corresponde ao do JWT

### Requirement: Lista de pedidos com resumo do dia e filtros
O painel SHALL exibir a lista de pedidos do tenant com um card hero de **resumo do dia** (contagem de pedidos, receita e quantos estão em aberto), colapsável e visível quando há pedidos. A lista SHALL oferecer filtros por status: Todos, Novos, Aceitos, Em preparo, Prontos, Entregues, Cancelados, cada um com contagem. Cada card SHALL mostrar ID curto, nome do cliente, resumo truncado de itens, valor, tipo de entrega, horário e o status com **cor de acento acompanhada de rótulo textual** (nunca cor sozinha). Durante o carregamento inicial o painel SHALL exibir **skeleton loaders** (não texto "Carregando..."); sem pedidos, um **estado vazio** explicativo.

#### Scenario: Resumo do dia reflete os pedidos
- **WHEN** há pedidos do dia
- **THEN** o card hero mostra a contagem, a receita e quantos pedidos estão em aberto, e pode ser colapsado

#### Scenario: Filtrar por status
- **WHEN** o OWNER seleciona um filtro (ex.: "Novos")
- **THEN** a lista passa a exibir apenas os pedidos naquele status e cada chip mostra sua contagem

#### Scenario: Status exibido com cor e rótulo
- **WHEN** um pedido aparece na lista ou no detalhe
- **THEN** seu status é apresentado com cor semântica **e** rótulo textual, nunca apenas por cor

#### Scenario: Carregamento mostra skeleton
- **WHEN** a lista de pedidos está carregando pela primeira vez
- **THEN** o painel mostra skeleton loaders em vez de um texto "Carregando..."

### Requirement: Atualização automática por polling sem falha silenciosa
O painel SHALL atualizar a lista de pedidos automaticamente por polling a cada 15 segundos, verificando `document.visibilityState` para **não** disparar requisições enquanto a aba está em background. Quando o polling **falha**, o painel SHALL exibir um indicador visual de conectividade e o **timestamp da última atualização bem-sucedida** — nunca ficar em silêncio. Quando o polling volta a ter sucesso, o indicador SHALL retornar ao estado normal e o timestamp SHALL avançar.

#### Scenario: Polling pausa com aba em background
- **WHEN** a aba do painel está em background (`document.visibilityState !== "visible"`)
- **THEN** o polling de 15s não dispara requisições enquanto a aba não estiver visível

#### Scenario: Falha de polling mostra conectividade e última atualização
- **WHEN** uma rodada de polling falha (erro de rede/servidor)
- **THEN** o painel exibe um indicador de conectividade e o timestamp da última atualização bem-sucedida, sem falhar em silêncio

#### Scenario: Recuperação limpa o indicador
- **WHEN** o polling volta a ter sucesso após uma falha
- **THEN** o indicador de conectividade volta ao normal e o timestamp de última atualização é avançado

### Requirement: Detalhe do pedido e avanço com estado de loading e reversão
O painel SHALL abrir o detalhe do pedido em bottom sheet com stepper de status, dados do cliente, itens (com adicionais e observação a partir dos snapshots) e totais discriminados. O botão de avanço de status SHALL ter **estado de loading** e ficar **desabilitado durante a requisição** para prevenir duplo toque/ação dupla. Em caso de **falha** no avanço, o card/detalhe SHALL **reverter visivelmente** ao estado anterior com erro visível — nenhum estado ambíguo ou silencioso.

#### Scenario: Botão de avanço bloqueia duplo toque
- **WHEN** o OWNER toca em avançar status
- **THEN** o botão entra em estado de loading e fica desabilitado até a resposta, impedindo uma segunda ação enquanto a requisição está em curso

#### Scenario: Falha no avanço reverte o card
- **WHEN** a requisição de avanço falha
- **THEN** o pedido volta a exibir o status anterior e um erro é mostrado ao OWNER, sem deixar o card em estado ambíguo

#### Scenario: Sucesso reflete o novo status
- **WHEN** o avanço é bem-sucedido
- **THEN** o detalhe e o card na lista passam a exibir o novo status (com cor e rótulo) e o stepper avança

### Requirement: Preview e envio opcional da mensagem de WhatsApp por avanço
A cada avanço de status, o painel SHALL gerar e exibir um **preview** da mensagem de WhatsApp para o cliente, com uma ação **opcional** "Enviar no WhatsApp". O OWNER SHALL poder avançar o status **com ou sem** enviar a mensagem — o envio nunca é obrigatório para avançar. Emojis SHALL aparecer **apenas** na mensagem de WhatsApp, nunca na UI do painel.

#### Scenario: Preview visível a cada avanço
- **WHEN** um pedido tem um próximo status disponível
- **THEN** o detalhe mostra o preview da mensagem de WhatsApp correspondente com uma ação opcional de envio

#### Scenario: Avançar sem enviar mensagem
- **WHEN** o OWNER avança o status sem acionar "Enviar no WhatsApp"
- **THEN** o status avança normalmente e nenhuma mensagem é enviada

#### Scenario: Enviar mensagem ao cliente
- **WHEN** o OWNER aciona "Enviar no WhatsApp"
- **THEN** o WhatsApp do cliente é aberto com a mensagem pré-formatada (emojis permitidos apenas nessa mensagem)

### Requirement: Toggle aberto/fechado e banner contextual de abertura
O painel SHALL exibir o toggle Aberto/Fechado **uma única vez**, no header (fonte única de verdade para `tenants.is_open`). Quando o OWNER acessa o painel **dentro** do horário configurado e o negócio está `is_open=false`, o painel SHALL exibir um **banner contextual** sugerindo abrir. Alternar o toggle SHALL persistir `is_open` e refletir imediatamente no header.

#### Scenario: Alternar aberto/fechado
- **WHEN** o OWNER alterna o toggle no header
- **THEN** o sistema persiste `is_open` do tenant e o header reflete o novo estado imediatamente

#### Scenario: Banner de abertura dentro do horário
- **WHEN** o OWNER acessa o painel dentro do horário configurado enquanto o negócio está fechado (`is_open=false`)
- **THEN** o painel exibe um banner contextual sugerindo abrir o negócio

#### Scenario: Toggle aparece uma única vez
- **WHEN** o OWNER está no painel
- **THEN** o controle de aberto/fechado aparece apenas no header, sem duplicação em outras superfícies
