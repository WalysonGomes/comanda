## Why

O produto promete "cardápio no ar em menos de 10 minutos", mas hoje o dono já consegue criar conta e entrar no painel (`owner-auth`) e não tem o que gerenciar — o painel é uma casca vazia. Sem gestão de cardápio não há o que a vitrine pública exiba nem o que um pedido referencie. Esta change entrega o CRUD de cardápio (categorias, produtos, adicionais) no painel do dono: o conteúdo que sustenta todas as changes seguintes (`public-storefront`, `order-operation`).

## What Changes

- **CRUD de categorias**: nome, posição/ordenação, ativa/inativa. Isolado por tenant.
- **CRUD de produtos**: foto (1 por produto), nome, descrição, preço, `available_days` (disponibilidade por dia da semana), toggle manual de disponível/indisponível, posição, categoria.
- **CRUD de grupos de adicionais por produto**: nome, obrigatoriedade (obrigatório/opcional), tipo de seleção (`SINGLE`/`MULTIPLE`), `min_selections`/`max_selections`; e itens de adicional (nome, preço adicional, disponibilidade).
- **Upload de foto com fallback de falha** (PRD Seção 4.5): se o upload falhar, o produto é salvo **sem foto**, com indicador claro e opção de tentar novamente. Falha de imagem nunca bloqueia salvar o produto.
- **Validação de tipo real do arquivo por magic bytes** no upload (não confiar na extensão).
- **Telas do painel** para todas essas operações, reproduzindo o design aprovado (mobile-first, thumb zone, skeleton loaders, sem emoji na UI): blocos `CARDÁPIO` e `PRODUTO (editor)` de `.design/Comanda Painel.dc.html`.

Fora de escopo (**out**):
- **Leitura pública do cardápio** pelo cliente → `public-storefront`.
- **Enforcement de limite** de quantidade de produtos (30) / categorias (5) por plano → `plans-and-onboarding`. Aqui apenas persistir; **não** duplicar a regra de limite (fronteira registrada no doc de prompts).
- **Estoque diário** (`daily_stock`) → fora do MVP (backlog). Disponibilidade só por `is_available` + `available_days`.

## Capabilities

### New Capabilities
- `menu-management`: gestão de cardápio no painel do OWNER — CRUD de categorias, produtos e grupos/itens de adicionais, sempre isolado por tenant; disponibilidade de produto por dia da semana e toggle manual; grupos de adicionais com obrigatoriedade e seleção única/múltipla com limites min/max; upload de foto validado por magic bytes com fallback de "salvar sem foto + tentar de novo".

### Modified Capabilities
<!-- Nenhuma. As specs de foundations/owner-auth ainda não estão arquivadas em openspec/specs/;
     a dependência sobre multi-tenancy (isolamento por tenant) e database-schema (tabelas
     categories, products, additional_groups, additional_items) é descrita em prosa e
     satisfeita pelas capabilities já definidas em foundations. -->

## Impact

- **Backend (novo, fatia vertical `menu`):** endpoints CRUD de categorias, produtos, grupos e itens de adicionais, todos filtrados por `tenant_id`; endpoint de upload de imagem com validação de magic bytes e armazenamento no disco local do VPS (estruturado para migração futura a bucket S3-compatível); reordenação por `position`. APIs compatíveis com Spring Boot 4.1 / Spring 7 / Jakarta EE 11 / Bean Validation 3.1.
- **Banco:** usa as tabelas `categories`, `products`, `additional_groups`, `additional_items` já criadas em `foundations` (nenhuma migration nova esperada; se faltar coluna, adicionar via nova migration Flyway).
- **Frontend (novo):** telas de cardápio (lista de categorias + produtos com toggle de disponibilidade) e editor de produto (foto, campos, chips de dias, editor de grupos de adicionais), com os tokens e microinterações do design; skeleton loaders; upload com estados de progresso/erro/retry; ícones Lucide, sem emoji.
- **Dependência:** consome `owner-auth` (sessão/JWT do OWNER + shell do painel) e `multi-tenancy`, `database-schema`, `design-system` de `foundations`. É pré-requisito de `public-storefront` (leitura pública) e `order-operation` (snapshots referenciam produtos/adicionais).
- **Segurança (PRD Seção 9 / Regra 9):** todo acesso a categoria/produto/adicional por ID validado contra IDOR (isolamento por tenant); upload valida magic bytes; nomes de arquivo sanitizados. Limite de produtos/categorias por plano fica em `plans-and-onboarding`.
