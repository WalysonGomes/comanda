# database-schema Specification

## Purpose
TBD - created by archiving change foundations. Update Purpose after archive.
## Requirements
### Requirement: Versioned MVP schema via Flyway
O sistema SHALL criar o schema inicial do MVP via migrations Flyway versionadas, contemplando apenas as tabelas do MVP: `tenants`, `users`, `business_hours`, `categories`, `products`, `additional_groups`, `additional_items`, `customers`, `orders`, `order_items`, `order_item_additionals`, `order_status_history`, `subscriptions`. Migrations SHALL ser versionadas e idempotentes de execução (rodar a mesma migration versionada mais de uma vez não altera o schema além do estado esperado).

#### Scenario: Migração cria o schema do MVP
- **WHEN** a aplicação sobe contra um banco vazio
- **THEN** o Flyway aplica as migrations e cria exatamente as 13 tabelas do MVP
- **AND** nenhuma tabela de feature futura (modos de operação, multioperadores, estoque diário) é criada

#### Scenario: Reexecução não reaplica migration já aplicada
- **WHEN** a aplicação sobe novamente contra um banco já migrado
- **THEN** o Flyway não reaplica migrations já registradas
- **AND** o schema permanece no estado esperado

### Requirement: Uniqueness and integrity constraints in the database
O sistema SHALL impor no banco as constraints: `subdomain` único em `tenants`; `idempotency_key` único por tenant em `orders`; chaves estrangeiras carregando `tenant_id` onde aplicável. Essas garantias SHALL viver no banco, não apenas no código.

#### Scenario: Subdomínio duplicado é rejeitado pelo banco
- **WHEN** uma inserção tenta criar um segundo tenant com um `subdomain` já existente
- **THEN** o banco rejeita a operação por violação de unicidade

#### Scenario: idempotency_key única por tenant
- **WHEN** um pedido é inserido com uma `idempotency_key` já usada pelo mesmo tenant
- **THEN** o banco rejeita a operação por violação de unicidade
- **AND** a mesma `idempotency_key` em um tenant diferente é aceita

### Requirement: Append-only order status history
O sistema SHALL tratar `order_status_history` como append-only por convenção: registros são apenas inseridos, nunca atualizados nem deletados. Cada registro SHALL conter `from_status`, `to_status`, `changed_by_user_id` (nullable para origem SYSTEM) e `created_at`.

#### Scenario: Mudança de status insere novo registro
- **WHEN** o status de um pedido muda
- **THEN** um novo registro é inserido em `order_status_history` com `from_status`, `to_status`, `changed_by_user_id` e `created_at`
- **AND** nenhum registro existente é atualizado ou removido

