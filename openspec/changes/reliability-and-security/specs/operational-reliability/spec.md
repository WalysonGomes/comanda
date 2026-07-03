## ADDED Requirements

### Requirement: Nenhum estado silencioso de falha na UI
O sistema NÃO SHALL apresentar estado silencioso de falha em nenhuma superfície (storefront público e painel do dono). Toda operação que possa falhar (rede, validação, servidor) SHALL exibir mensagem clara ao usuário e oferecer um caminho de recuperação (repetir, corrigir, ou instrução explícita). Nenhuma requisição SHALL terminar em estado ambíguo, spinner infinito ou tela sem feedback.

#### Scenario: Falha de requisição em qualquer tela exibe erro recuperável
- **WHEN** uma requisição de rede ou do servidor falha em qualquer superfície do storefront ou do painel
- **THEN** o sistema exibe uma mensagem de erro legível descrevendo o que falhou
- **AND** oferece uma ação de recuperação (tentar novamente, corrigir dado ou instrução clara)
- **AND** não deixa a UI em spinner infinito, em branco ou em estado ambíguo

#### Scenario: Auditoria cobre todas as superfícies de erro
- **WHEN** a auditoria de confiabilidade é executada sobre storefront e painel
- **THEN** cada ponto de falha catalogado possui mensagem clara e caminho de recuperação verificados por teste
- **AND** nenhum ponto de falha permanece sem tratamento

### Requirement: Fallback de handoff do WhatsApp
Quando o link `wa.me` não abrir no dispositivo do cliente, o sistema SHALL exibir a mensagem de pedido pré-formatada na própria tela, com um botão "Copiar mensagem", tratada como tela de primeira classe. O cliente NÃO SHALL ficar sem meio de enviar o pedido.

#### Scenario: WhatsApp não abre no checkout
- **WHEN** o cliente finaliza o pedido e o `wa.me` não é aberto pelo dispositivo
- **THEN** o sistema exibe a mensagem formatada na tela (formato da Seção 3.1 do PRD)
- **AND** apresenta um botão "Copiar mensagem" que copia o texto completo para a área de transferência
- **AND** confirma visualmente que a cópia foi realizada

### Requirement: Fallback de upload de foto
Quando o upload da foto de um produto falhar, o sistema SHALL salvar o produto sem foto, sinalizar a falha de forma clara e oferecer a opção de tentar o upload novamente. A falha de upload NÃO SHALL impedir a persistência do produto.

#### Scenario: Upload de foto falha ao salvar produto
- **WHEN** o dono salva um produto e o upload da foto falha
- **THEN** o produto é persistido sem foto
- **AND** o sistema exibe um indicador claro de que a foto não foi enviada
- **AND** oferece a ação de tentar o upload novamente

### Requirement: Fallback de polling do painel
Quando o polling automático da lista de pedidos falhar, o sistema SHALL exibir um indicador visual de conectividade com o timestamp da última atualização bem-sucedida. O painel NUNCA SHALL falhar silenciosamente ao atualizar.

#### Scenario: Polling falha no painel de pedidos
- **WHEN** uma iteração do polling de 15s da lista de pedidos falha
- **THEN** o header de Pedidos exibe um indicador de conectividade perdida
- **AND** mostra o timestamp da última atualização bem-sucedida
- **AND** volta ao estado normal quando o polling é restabelecido

### Requirement: Idempotência de criação de pedido garantida
O sistema SHALL garantir que a criação de pedido é idempotente por `idempotency_key` único por tenant, validada cross-tenant. Reenvio com a mesma chave no mesmo tenant retorna o mesmo pedido sem duplicar; a mesma chave em tenant diferente é rejeitada/isolada. Esta garantia SHALL ser coberta por teste de regressão nesta change.

#### Scenario: Reenvio com a mesma idempotency_key não duplica
- **WHEN** duas requisições de criação de pedido chegam com a mesma `idempotency_key` no mesmo tenant
- **THEN** o sistema cria exatamente um pedido
- **AND** a segunda requisição retorna o mesmo pedido já criado

#### Scenario: Mesma idempotency_key em tenant diferente é isolada
- **WHEN** uma `idempotency_key` já usada no tenant A chega numa requisição do tenant B
- **THEN** o sistema não vincula o pedido ao tenant A
- **AND** trata a requisição isolada no tenant B (rejeita ou cria no escopo de B, nunca cross-tenant)

### Requirement: Idempotência de avanço de status garantida
O sistema SHALL garantir que o avanço de status é idempotente: um request de avanço duplicado NÃO SHALL gerar dois registros em `order_status_history` nem pular etapa. Esta garantia SHALL ser coberta por teste de regressão nesta change.

#### Scenario: Avanço de status duplicado não duplica histórico
- **WHEN** dois requests de avanço para o mesmo próximo status chegam para o mesmo pedido
- **THEN** o status avança uma única vez
- **AND** apenas um registro é gravado em `order_status_history`

### Requirement: Histórico append-only e rastreável
O sistema SHALL manter `order_status_history` append-only: registros nunca são atualizados nem deletados. Cada registro SHALL conter `from_status`, `to_status`, `changed_by` (usuário OWNER ou SYSTEM/null) e `created_at`, garantindo rastreabilidade mínima de toda mudança de status.

#### Scenario: Cada mudança de status grava registro completo
- **WHEN** o status de um pedido muda (avanço ou cancelamento)
- **THEN** um novo registro é anexado a `order_status_history` com `from_status`, `to_status`, `changed_by` e `created_at`
- **AND** nenhum registro existente é atualizado ou removido

#### Scenario: Tentativa de update/delete no histórico é impedida
- **WHEN** o código de operação de pedido tenta modificar ou remover um registro de `order_status_history`
- **THEN** a operação não é permitida (append-only por convenção verificada em revisão/teste)

### Requirement: Plano de resposta a incidentes para dev solo
O produto SHALL manter um plano documentado de resposta a incidentes adequado a um dev solo, definindo o responsável, o canal de emergência e o critério de notificação à ANPD em até 72h para vazamento de dados com risco real aos titulares.

#### Scenario: Plano de incidentes existe e é acionável
- **WHEN** um incidente de segurança ou vazamento é identificado
- **THEN** o plano documentado indica o responsável e o canal de emergência
- **AND** define que vazamento com risco real aos titulares é notificado à ANPD em até 72h
