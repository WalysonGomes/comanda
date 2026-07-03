# menu-management Specification

## Purpose
TBD - created by syncing change menu-management. Update Purpose after archive.

## Requirements
### Requirement: Gestão de categorias pelo OWNER
O sistema SHALL permitir ao usuário OWNER autenticado criar, editar, listar, reordenar e remover categorias de cardápio, sempre isoladas por `tenant_id`. Cada categoria SHALL ter nome, `position` (ordenação) e estado ativa/inativa (`is_active`). A listagem SHALL retornar as categorias do tenant ordenadas por `position` ascendente.

#### Scenario: Criar categoria
- **WHEN** o OWNER envia uma nova categoria com nome válido
- **THEN** o sistema cria a categoria vinculada ao seu `tenant_id`, atribui uma `position` ao final da lista e a retorna como ativa por padrão

#### Scenario: Editar nome e estado ativo/inativo
- **WHEN** o OWNER edita o nome ou alterna `is_active` de uma categoria do seu tenant
- **THEN** o sistema persiste a alteração e retorna a categoria atualizada

#### Scenario: Reordenar categorias
- **WHEN** o OWNER envia uma nova ordem para suas categorias
- **THEN** o sistema reescreve os valores de `position` numa única transação e a listagem passa a refletir a nova ordem

#### Scenario: Nome de categoria vazio é rejeitado
- **WHEN** o OWNER tenta criar ou editar uma categoria com nome em branco
- **THEN** o sistema rejeita com erro de validação de campo e não persiste

#### Scenario: Remover categoria sem produtos
- **WHEN** o OWNER remove uma categoria do seu tenant que não tem produtos
- **THEN** o sistema remove a categoria

#### Scenario: Remoção de categoria com produtos é bloqueada
- **WHEN** o OWNER tenta remover uma categoria que ainda contém produtos
- **THEN** o sistema rejeita com código `CATEGORY_NOT_EMPTY` e mensagem clara, sem remover nada

#### Scenario: Categoria de outro tenant é inacessível
- **WHEN** o OWNER referencia por ID uma categoria pertencente a outro tenant
- **THEN** o sistema responde como recurso inexistente (404) e não expõe seus dados

### Requirement: Gestão de produtos pelo OWNER
O sistema SHALL permitir ao OWNER criar, editar, listar, reordenar e remover produtos, isolados por `tenant_id` e vinculados a uma categoria do mesmo tenant. Cada produto SHALL ter nome, descrição, preço, `image_url` (opcional), `position`, `available_days` e `is_available`. Preço SHALL ser não-negativo e nome obrigatório. Remover um produto SHALL remover em cascata seus grupos e itens de adicionais.

#### Scenario: Criar produto em uma categoria
- **WHEN** o OWNER cria um produto com nome, preço válido e categoria do seu tenant
- **THEN** o sistema persiste o produto vinculado ao tenant e à categoria, disponível por padrão, e o retorna

#### Scenario: Preço inválido é rejeitado
- **WHEN** o OWNER tenta salvar um produto com preço negativo ou ausente
- **THEN** o sistema rejeita com erro de validação de campo e não persiste

#### Scenario: Produto em categoria de outro tenant é rejeitado
- **WHEN** o OWNER tenta vincular um produto a uma categoria que não pertence ao seu tenant
- **THEN** o sistema rejeita a operação como recurso inexistente e não persiste

#### Scenario: Remover produto cascateia adicionais
- **WHEN** o OWNER remove um produto do seu tenant que possui grupos e itens de adicionais
- **THEN** o sistema remove o produto e todos os seus grupos e itens de adicionais

#### Scenario: Produto de outro tenant é inacessível
- **WHEN** o OWNER referencia por ID um produto de outro tenant
- **THEN** o sistema responde como recurso inexistente (404)

### Requirement: Disponibilidade de produto por dia e toggle manual
Produtos SHALL suportar disponibilidade por dia da semana via `available_days` (array de inteiros na convenção `0=Domingo … 6=Sábado`; `null` ou vazio significa disponível todos os dias) e um toggle manual `is_available`. O sistema SHALL persistir ambos de forma independente. A aplicação da regra de exibição por dia é responsabilidade da leitura pública (fora desta change); aqui SHALL apenas persistir os valores.

#### Scenario: Definir dias disponíveis
- **WHEN** o OWNER seleciona um subconjunto de dias da semana para um produto
- **THEN** o sistema persiste esses dias em `available_days` na convenção `0=Dom … 6=Sáb`

#### Scenario: Marcar produto como disponível todos os dias
- **WHEN** o OWNER não seleciona nenhum dia específico
- **THEN** o sistema persiste `available_days` como null/vazio, significando disponível todos os dias

#### Scenario: Alternar disponibilidade manual
- **WHEN** o OWNER alterna o toggle de disponível/indisponível de um produto
- **THEN** o sistema atualiza `is_available` de forma independente de `available_days`

### Requirement: Gestão de grupos e itens de adicionais por produto
O sistema SHALL permitir ao OWNER gerir grupos de adicionais dentro de um produto. Cada grupo SHALL ter nome, obrigatoriedade (`required`), tipo de seleção (`SINGLE` ou `MULTIPLE`) e limites `min_selections`/`max_selections`. Cada item de adicional SHALL ter nome, preço adicional (não-negativo) e disponibilidade (`is_available`). O sistema SHALL validar que `min_selections` ≤ `max_selections` e que os limites são coerentes com o tipo de seleção. Grupos e itens SHALL pertencer a um produto do tenant do OWNER.

#### Scenario: Criar grupo obrigatório de seleção única
- **WHEN** o OWNER cria um grupo `required=true`, `SINGLE` num produto do seu tenant
- **THEN** o sistema persiste o grupo vinculado ao produto e o retorna

#### Scenario: Criar grupo de seleção múltipla com limites
- **WHEN** o OWNER cria um grupo `MULTIPLE` com `min_selections` e `max_selections` válidos
- **THEN** o sistema persiste os limites do grupo

#### Scenario: Limites de seleção incoerentes são rejeitados
- **WHEN** o OWNER envia um grupo com `min_selections` maior que `max_selections`
- **THEN** o sistema rejeita com erro de validação e não persiste

#### Scenario: Adicionar item com preço adicional
- **WHEN** o OWNER adiciona um item com nome e preço adicional não-negativo a um grupo
- **THEN** o sistema persiste o item vinculado ao grupo

#### Scenario: Preço adicional negativo é rejeitado
- **WHEN** o OWNER tenta salvar um item de adicional com preço negativo
- **THEN** o sistema rejeita com erro de validação e não persiste

#### Scenario: Grupo/item de produto de outro tenant é inacessível
- **WHEN** o OWNER tenta operar sobre um grupo ou item cuja cadeia de ownership (item → grupo → produto) pertence a outro tenant
- **THEN** o sistema responde como recurso inexistente (404)

### Requirement: Upload de foto validado por magic bytes com fallback
O sistema SHALL aceitar upload de uma foto por produto validando o **tipo real do arquivo por magic bytes** (não pela extensão nem pelo Content-Type declarado), aceitando apenas JPEG, PNG e WebP, e rejeitando qualquer arquivo cujo conteúdo real não corresponda. A falha de upload NÃO SHALL impedir salvar o produto: o produto SHALL poder ser salvo sem foto (`image_url = null`), com sinalização clara e possibilidade de nova tentativa. O armazenamento SHALL ser abstraído para permitir migração futura de disco local para bucket sem alterar o domínio.

#### Scenario: Upload de imagem válida
- **WHEN** o OWNER envia um arquivo cujos magic bytes correspondem a JPEG, PNG ou WebP
- **THEN** o sistema armazena a imagem, gera um nome de arquivo próprio com a extensão do tipo real detectado e associa `image_url` ao produto

#### Scenario: Arquivo com extensão falsa é rejeitado
- **WHEN** o OWNER envia um arquivo com extensão de imagem mas cujos magic bytes não são de um formato permitido
- **THEN** o sistema rejeita o upload com mensagem clara e não armazena o arquivo

#### Scenario: Falha de upload não impede salvar o produto
- **WHEN** o upload da foto falha durante o cadastro/edição de um produto
- **THEN** o sistema salva o produto com `image_url` nulo, sinaliza claramente que a foto não foi enviada e oferece a opção de tentar novamente

#### Scenario: Reenvio da foto após falha
- **WHEN** o OWNER tenta novamente o upload de foto de um produto salvo sem imagem
- **THEN** o sistema armazena a nova imagem e atualiza `image_url` do produto

### Requirement: Telas do painel de cardápio conforme design aprovado
O sistema SHALL apresentar as telas de gestão de cardápio reproduzindo o design aprovado (`.design/Comanda Painel.dc.html`, blocos `CARDÁPIO` e `PRODUTO (editor)`): mobile-first, ações na thumb zone, skeleton loaders durante carregamento, ícones Lucide React e **nenhum emoji na UI**. A lista de cardápio SHALL agrupar produtos por categoria com toggle de disponibilidade por produto; o editor de produto SHALL conter foto, campos, chips de dias da semana e o editor de grupos de adicionais.

#### Scenario: Lista de cardápio agrupada por categoria
- **WHEN** o OWNER abre a tela de cardápio
- **THEN** o sistema exibe os produtos agrupados por categoria, com contagem por categoria e toggle de disponibilidade por produto, exibindo skeleton loaders enquanto carrega

#### Scenario: Editor de produto com dias e adicionais
- **WHEN** o OWNER abre o editor de um produto
- **THEN** o sistema exibe foto, nome, descrição, preço, toggle de disponibilidade, os 7 chips de dias da semana e a lista editável de grupos de adicionais

#### Scenario: UI sem emoji e com ícones Lucide
- **WHEN** qualquer tela de cardápio é renderizada
- **THEN** todos os ícones são da biblioteca Lucide React e nenhum emoji aparece na interface
