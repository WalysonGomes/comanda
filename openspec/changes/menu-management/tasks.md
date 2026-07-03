## 1. Backend — fatia vertical `menu` (base)

- [x] 1.1 Criar o pacote `menu` (fatia vertical) seguindo o padrão de `foundations`, com sub-organização por recurso (categories, products, additionals, images)
- [x] 1.2 Mapear entidades JPA `Category`, `Product`, `AdditionalGroup`, `AdditionalItem` sobre as tabelas existentes, com `tenant_id` e relacionamentos (produto → categoria; grupo → produto; item → grupo)
- [x] 1.3 Criar repositórios que filtram por `tenant_id`; verificar se falta alguma coluna vs. schema de `foundations` e, se faltar, criar migration Flyway nova (não editar migrations aplicadas)
- [x] 1.4 Garantir que a resolução de tenant do JWT (`multi-tenancy`) é aplicada a todos os endpoints da fatia

## 2. Backend — CRUD de categorias

- [x] 2.1 Endpoints criar/listar/editar/remover categoria, isolados por tenant; listagem ordenada por `position`
- [x] 2.2 Validação de nome obrigatório e toggle `is_active` (Bean Validation)
- [x] 2.3 Endpoint de reordenação de categorias (reescreve `position` em transação)
- [x] 2.4 Bloquear remoção de categoria não-vazia com código `CATEGORY_NOT_EMPTY`
- [x] 2.5 Acesso por ID retorna 404 para categoria de outro tenant (proteção IDOR)

## 3. Backend — CRUD de produtos

- [x] 3.1 Endpoints criar/listar/editar/remover produto, isolados por tenant e vinculados a categoria do mesmo tenant
- [x] 3.2 Validação: nome obrigatório, preço não-negativo; rejeitar categoria de outro tenant
- [x] 3.3 Persistir `available_days` (int[], convenção `0=Dom … 6=Sáb`; null = todos) e toggle `is_available` independentes
- [x] 3.4 Endpoint de reordenação de produtos por `position` dentro da categoria
- [x] 3.5 Remoção de produto cascateia grupos e itens de adicionais
- [x] 3.6 Acesso por ID retorna 404 para produto de outro tenant (proteção IDOR)

## 4. Backend — grupos e itens de adicionais

- [x] 4.1 Endpoints CRUD de grupos de adicionais dentro de um produto (nome, `required`, `selection_type` SINGLE/MULTIPLE, `min_selections`, `max_selections`)
- [x] 4.2 Validação de coerência: `min_selections` ≤ `max_selections` e limites coerentes com o tipo de seleção
- [x] 4.3 Endpoints CRUD de itens de adicional (nome, preço adicional não-negativo, `is_available`)
- [x] 4.4 Validar cadeia de ownership item → grupo → produto → tenant; 404 se pertencer a outro tenant

## 5. Backend — upload de imagem

- [x] 5.1 Definir abstração `ImageStorage` (impl. disco local por tenant no MVP; interface preparada para bucket S3-compatível)
- [x] 5.2 Endpoint de upload que valida magic bytes (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF..WEBP`), rejeitando conteúdo real não permitido independentemente da extensão/Content-Type
- [x] 5.3 Aplicar limite de tamanho e gerar nome de arquivo (UUID) com extensão do tipo real detectado; retornar referência para `image_url`
- [x] 5.4 Ao trocar/remover imagem, apagar a anterior best-effort (falha não bloqueia a operação)
- [x] 5.5 Configurar o backend para servir os arquivos de imagem armazenados

## 6. Backend — testes

- [x] 6.1 Testes de isolamento por tenant / IDOR em categorias, produtos, grupos e itens (acesso cross-tenant → 404)
- [x] 6.2 Testes de validação (nome vazio, preço negativo, limites min/max incoerentes)
- [x] 6.3 Testes de upload: magic bytes válidos, extensão falsa rejeitada, produto salvo sem foto quando upload falha
- [x] 6.4 Teste de bloqueio de remoção de categoria não-vazia e de cascata de remoção de produto

## 7. Frontend — infraestrutura

- [x] 7.1 Rotas do cardápio dentro do shell autenticado do painel (`owner-auth`), protegidas por sessão
- [x] 7.2 Camada de acesso à API (TanStack Query) com cache/estados de loading e erro para categorias, produtos e adicionais
- [x] 7.3 Componentes de skeleton loader reutilizando os tokens/`.sk` do design

## 8. Frontend — tela de cardápio (bloco CARDÁPIO)

- [x] 8.1 Lista agrupada por categoria com contagem por categoria e cartões de produto (foto/placeholder, nome, descrição, preço)
- [x] 8.2 Toggle de disponibilidade por produto no cartão, com rótulo textual (nunca só cor) e chamada à API
- [x] 8.3 Ação "Nova categoria" e edição/ordenação de categorias
- [x] 8.4 Estados de carregamento (skeleton) e vazio, sem emoji, ícones Lucide

## 9. Frontend — editor de produto (bloco PRODUTO)

- [x] 9.1 Formulário com foto, nome, descrição, preço e toggle de disponibilidade, com validação inline
- [x] 9.2 Chips dos 7 dias da semana ligados a `available_days` (convenção `0=Dom … 6=Sáb`)
- [x] 9.3 Editor de grupos de adicionais (nome, obrigatoriedade, tipo de seleção, min/max) e itens (nome, preço)
- [x] 9.4 Fluxo de upload de foto: estados de progresso, erro e "salvar sem foto + tentar de novo" (Regra 4.5)
- [x] 9.5 Ação "Salvar produto" com estado de loading; refletir erros de validação do backend

## 10. Validação final

- [x] 10.1 Rodar `openspec validate menu-management --strict` e corrigir apontamentos
- [x] 10.2 Conferir critérios do PRD Seção 9 aplicáveis (IDOR testado, magic bytes, sem dado sensível em log, sem emoji na UI)
- [x] 10.3 Confirmar que nenhuma regra de limite de plano foi implementada aqui (fronteira com `plans-and-onboarding`)
