# public-storefront Specification

## Purpose
TBD - created by syncing change public-storefront. Update Purpose after archive.

## Requirements
### Requirement: Resolução de tenant por subdomínio na vitrine pública
O sistema SHALL resolver o tenant da vitrine pública a partir do subdomínio da requisição (`nomedonegocio.${APP_DOMAIN}`), sem exigir autenticação. Todas as leituras públicas SHALL ser filtradas pelo `tenant_id` resolvido do subdomínio, e NUNCA por identificador de tenant enviado pelo cliente. Um subdomínio que não corresponde a nenhum tenant SHALL resultar em recurso inexistente (404).

#### Scenario: Subdomínio válido carrega o negócio
- **WHEN** o cliente acessa a vitrine em um subdomínio que corresponde a um tenant existente
- **THEN** o sistema resolve o `tenant_id` pelo subdomínio e carrega os dados públicos do negócio (nome, logo, status, taxa de entrega, mínimo)

#### Scenario: Subdomínio inexistente
- **WHEN** o cliente acessa um subdomínio que não corresponde a nenhum tenant
- **THEN** o sistema responde como recurso inexistente (404) e não expõe dados de nenhum negócio

#### Scenario: Identificador de produto de outro tenant é inacessível
- **WHEN** o cliente envia, em qualquer operação pública, o id de um produto ou adicional que não pertence ao tenant do subdomínio
- **THEN** o sistema trata o recurso como inexistente e não expõe nem opera sobre dados de outro tenant

### Requirement: Cardápio público filtrado por disponibilidade
O sistema SHALL retornar no cardápio público apenas os produtos disponíveis no momento: produtos com `is_available = true` E cujo `available_days` seja nulo/vazio OU contenha o dia da semana atual no timezone `America/Fortaleza` (convenção `0=Domingo … 6=Sábado`). Produtos indisponíveis no dia ou marcados como indisponíveis SHALL NÃO ser listados (nem como itens desabilitados). Categorias sem nenhum produto disponível SHALL NÃO aparecer. Os produtos SHALL ser agrupados por categoria e ordenados por `position`.

#### Scenario: Produto disponível no dia aparece
- **WHEN** um produto está com `is_available = true` e o dia atual em `America/Fortaleza` está em seu `available_days` (ou `available_days` é nulo/vazio)
- **THEN** o produto aparece no cardápio, em sua categoria, com nome, foto (ou placeholder) e preço

#### Scenario: Produto indisponível no dia não aparece
- **WHEN** o dia atual não está no `available_days` de um produto
- **THEN** o produto não é listado no cardápio, nem como item desabilitado

#### Scenario: Produto marcado indisponível não aparece
- **WHEN** um produto está com `is_available = false`
- **THEN** o produto não é listado no cardápio

#### Scenario: Categoria sem produtos disponíveis é omitida
- **WHEN** uma categoria não tem nenhum produto disponível no momento
- **THEN** a categoria não aparece no cardápio público

### Requirement: Estado de loja aberta ou fechada
O sistema SHALL determinar, no servidor e no timezone `America/Fortaleza`, se o negócio está aberto no momento, a partir do toggle manual `tenants.is_open` e do `business_hours` do dia. A vitrine SHALL exibir o status (aberto/fechado) com cor e rótulo textual e, quando fechada, informar quando reabre. O estado fechado SHALL permitir navegar o cardápio e montar o carrinho, mas SHALL bloquear a finalização do pedido, explicando o motivo.

#### Scenario: Negócio aberto permite finalizar
- **WHEN** o negócio está aberto (dentro do horário do dia e `is_open = true`)
- **THEN** a vitrine exibe status "aberto" e permite prosseguir até a finalização do pedido

#### Scenario: Negócio fechado permite navegar mas bloqueia finalizar
- **WHEN** o negócio está fechado (fora do horário do dia ou `is_open = false`)
- **THEN** a vitrine exibe status "fechado" com aviso de quando reabre, permite navegar e montar o carrinho, e desabilita a finalização com o motivo visível

### Requirement: Detalhe do produto com adicionais e bloqueio de obrigatórios
O sistema SHALL exibir o detalhe do produto em bottom sheet com descrição, grupos de adicionais, seletor de quantidade e campo de observação. Grupos `SINGLE` SHALL permitir uma única seleção e grupos `MULTIPLE` SHALL respeitar `min_selections`/`max_selections`. O sistema SHALL bloquear a ação "Adicionar" enquanto qualquer grupo obrigatório (`required`) não tiver uma seleção válida, exibindo o motivo do bloqueio de forma visível até que a seleção seja válida.

#### Scenario: Adicionar bloqueado sem obrigatório selecionado
- **WHEN** o produto tem um grupo obrigatório e o cliente ainda não fez uma seleção válida nesse grupo
- **THEN** o botão "Adicionar" fica bloqueado e o sistema mostra o motivo (qual escolha falta) de forma visível

#### Scenario: Adicionar liberado após satisfazer obrigatórios
- **WHEN** o cliente satisfaz todos os grupos obrigatórios com seleções válidas (respeitando único/múltiplo e min/max)
- **THEN** o botão "Adicionar" é liberado

#### Scenario: Seleção múltipla respeita o máximo
- **WHEN** o cliente tenta selecionar mais itens que o `max_selections` de um grupo `MULTIPLE`
- **THEN** o sistema impede exceder o máximo do grupo

#### Scenario: Ajustar quantidade e observação
- **WHEN** o cliente ajusta a quantidade e escreve uma observação no detalhe do produto
- **THEN** o sistema reflete a quantidade e guarda a observação para a linha do carrinho

### Requirement: Carrinho persistente e editável no navegador
O sistema SHALL manter o carrinho no navegador do cliente, persistido entre recarregamentos, sem exigir login. O cliente SHALL poder editar a quantidade e os adicionais de uma linha existente sem precisar removê-la e recriá-la. Cada linha SHALL preservar o produto, os adicionais selecionados, a quantidade e a observação.

#### Scenario: Carrinho sobrevive a recarregamento
- **WHEN** o cliente adiciona itens e recarrega a página da vitrine
- **THEN** o carrinho mantém os itens, quantidades, adicionais e observações anteriores

#### Scenario: Editar adicionais de uma linha existente
- **WHEN** o cliente edita os adicionais ou a quantidade de uma linha do carrinho
- **THEN** o sistema atualiza a mesma linha com as novas seleções, sem criar uma linha duplicada

#### Scenario: Remover uma linha do carrinho
- **WHEN** o cliente remove uma linha do carrinho
- **THEN** o sistema retira o item e recalcula os totais

### Requirement: Transparência de preço e pedido mínimo
O sistema SHALL exibir, de forma discriminada e sempre visível, o subtotal, a taxa de entrega e o total do pedido. Quando o subtotal estiver abaixo do valor mínimo do negócio, o sistema SHALL bloquear o avanço para o checkout e informar exatamente quanto falta para atingir o mínimo.

#### Scenario: Totais discriminados no carrinho
- **WHEN** o cliente abre o carrinho com itens
- **THEN** o sistema mostra subtotal, taxa de entrega e total separadamente

#### Scenario: Pedido abaixo do mínimo é bloqueado
- **WHEN** o subtotal do carrinho está abaixo do valor mínimo do negócio
- **THEN** o sistema bloqueia o avanço e mostra quanto falta para atingir o mínimo

#### Scenario: Pedido atinge o mínimo libera avanço
- **WHEN** o subtotal atinge ou ultrapassa o valor mínimo
- **THEN** o sistema libera o avanço para o checkout

### Requirement: Checkout em tela única com validação inline
O sistema SHALL apresentar o checkout em tela única solicitando nome, telefone e tipo de entrega (Entrega ou Retirada). O campo de endereço SHALL ser exibido e exigido apenas quando o tipo for Entrega. O sistema SHALL validar os campos inline em tempo real e SHALL habilitar a finalização apenas quando o formulário for válido, o negócio estiver aberto e o carrinho for válido (acima do mínimo e sem itens indisponíveis).

#### Scenario: Endereço exigido apenas para Entrega
- **WHEN** o cliente seleciona o tipo "Entrega"
- **THEN** o sistema exibe e exige o campo de endereço

#### Scenario: Retirada não exige endereço
- **WHEN** o cliente seleciona o tipo "Retirada"
- **THEN** o sistema não exibe nem exige o campo de endereço

#### Scenario: Campos inválidos bloqueiam a finalização
- **WHEN** o cliente deixa o nome vazio ou o telefone sem DDD válido
- **THEN** o sistema mostra o erro inline por campo e mantém a finalização desabilitada

### Requirement: Validação de disponibilidade na finalização
Antes de confirmar o envio, o sistema SHALL validar no servidor, contra o tenant do subdomínio, que cada produto e adicional do carrinho ainda existe e está disponível no momento. Quando um ou mais itens ficarem indisponíveis durante a sessão, o sistema SHALL sinalizar o erro por item, com instrução clara para remover o item indisponível, e SHALL impedir a finalização até a resolução. O total apresentado na finalização SHALL refletir os preços autoritativos retornados pela validação do servidor.

#### Scenario: Item ficou indisponível durante a sessão
- **WHEN** um item do carrinho ficou indisponível enquanto o cliente montava o pedido
- **THEN** o sistema mostra, por item, que ele não está mais disponível, instrui a removê-lo e bloqueia a finalização

#### Scenario: Carrinho válido prossegue
- **WHEN** todos os itens do carrinho continuam disponíveis na validação
- **THEN** o sistema permite prosseguir para a confirmação de envio com os preços atuais do servidor

### Requirement: Confirmação de envio antes do handoff ao WhatsApp
O sistema SHALL exibir uma confirmação explícita de "Pedido enviado" ao cliente ANTES de abrir o WhatsApp do negócio. O handoff SHALL abrir o WhatsApp do número do negócio com a mensagem pré-formatada no formato exato definido na Seção 3.1 do PRD. Emojis SHALL ser usados apenas nessa mensagem de WhatsApp, nunca na interface.

#### Scenario: Confirmação precede o WhatsApp
- **WHEN** o cliente finaliza um pedido válido
- **THEN** o sistema mostra a confirmação "Pedido enviado" antes de qualquer tentativa de abrir o WhatsApp

#### Scenario: Mensagem no formato do PRD
- **WHEN** o handoff para o WhatsApp é acionado
- **THEN** a mensagem gerada segue o formato exato da Seção 3.1 (itens com quantidade, adicionais, tipo/endereço de entrega, total com taxa e observação), com emojis apenas nessa mensagem

### Requirement: Fallback de cópia da mensagem
O sistema SHALL tratar o fallback de mensagem como tela de primeira classe: a mensagem formatada do pedido SHALL estar sempre visível na tela de envio, com um botão "Copiar mensagem", de modo que, se o WhatsApp não abrir, o cliente possa copiar a mensagem e colá-la manualmente na conversa do negócio.

#### Scenario: WhatsApp não abre
- **WHEN** a tentativa de abrir o `wa.me` do negócio não funciona
- **THEN** o cliente ainda vê a mensagem completa na tela e pode copiá-la com o botão "Copiar mensagem"

#### Scenario: Copiar mensagem
- **WHEN** o cliente aciona "Copiar mensagem"
- **THEN** o sistema copia a mensagem formatada para a área de transferência e confirma a cópia

### Requirement: Cliente sem cadastro ou login
O sistema SHALL permitir todo o fluxo da vitrine — navegar o cardápio, montar o carrinho, finalizar e fazer o handoff ao WhatsApp — sem exigir cadastro ou login do cliente.

#### Scenario: Pedido completo sem conta
- **WHEN** um cliente novo acessa a vitrine e realiza um pedido do início ao handoff
- **THEN** o sistema não solicita cadastro nem login em nenhuma etapa

### Requirement: Contrato de criação idempotente de pedido (dependência de order-operation)
O storefront SHALL montar o pedido e enviá-lo à criação idempotente de pedido cujo contrato é declarado por esta change e implementado por `order-operation`. A requisição SHALL ser pública (resolvida por subdomínio), incluir uma `idempotency_key` gerada no cliente por tentativa de finalização, os dados do cliente (nome, telefone), o tipo de entrega, o endereço quando for Entrega, a observação e as linhas do pedido (produto, adicionais e quantidade), e NÃO SHALL conter `tenant_id` no corpo (o tenant vem do subdomínio). O reenvio com a mesma `idempotency_key` no mesmo tenant NÃO SHALL criar um pedido duplicado.

#### Scenario: Envio do pedido com chave de idempotência
- **WHEN** o storefront finaliza um pedido válido
- **THEN** ele envia à criação de pedido a `idempotency_key` da tentativa, os dados do cliente e as linhas do pedido, sem `tenant_id` no corpo

#### Scenario: Reenvio não duplica
- **WHEN** o mesmo pedido é reenviado com a mesma `idempotency_key` no mesmo tenant (ex.: toque duplo ou reprocesso)
- **THEN** o sistema não cria um pedido duplicado (garantia satisfeita por `order-operation`)
