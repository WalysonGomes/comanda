# multi-tenancy Specification

## Purpose
TBD - created by archiving change foundations. Update Purpose after archive.
## Requirements
### Requirement: Tenant isolation on all persistence and reads
O sistema SHALL isolar dados por `tenant_id` em toda persistência e leitura. Nenhuma query de dados de tenant PODE retornar registros de um tenant diferente do tenant atual da requisição. O `tenant_id` SHALL ser aplicado como discriminador por coluna em todas as tabelas de tenant.

#### Scenario: Leitura só retorna dados do tenant atual
- **WHEN** uma requisição no contexto do tenant A consulta qualquer recurso de tenant
- **THEN** o sistema retorna apenas registros cujo `tenant_id` é o do tenant A
- **AND** registros de outros tenants nunca aparecem no resultado

#### Scenario: Escrita atribui o tenant atual
- **WHEN** uma requisição no contexto do tenant A persiste um novo registro de tenant
- **THEN** o registro é gravado com `tenant_id` do tenant A
- **AND** não é possível gravar um registro com `tenant_id` de outro tenant

#### Scenario: Acesso a recurso de outro tenant por ID é negado
- **WHEN** uma requisição no contexto do tenant A tenta ler ou modificar um recurso por ID que pertence ao tenant B
- **THEN** o sistema trata o recurso como inexistente para o tenant A (sem vazar existência)
- **AND** a operação falha sem expor dados do tenant B

### Requirement: Tenant resolution by subdomain on public routes
O sistema SHALL resolver o tenant atual pelo subdomínio (`nomedonegocio.${APP_DOMAIN}`) nas rotas públicas do storefront e injetá-lo no contexto da requisição.

Os rótulos `www`, `app`, `api`, `docs`, `status`, `admin`, `demo` e `signal` SHALL ser permanentemente reservados para superfícies oficiais. O backend SHALL ser a fronteira autoritativa, apó normalização case-insensitive; o frontend MAY espelhar a lista somente para feedback imediato. Hosts reservados SHALL resolver para a superfície raiz, nunca para storefront.

#### Scenario: Rótulo oficial não vira tenant
- **WHEN** cadastro ou alteração de dados do negócio solicita um rótulo reservado, com qualquer caixa ou espaços externos
- **THEN** o backend rejeita o subdomínio e nenhuma alteração parcial é persistida

#### Scenario: Subdomínio conhecido resolve tenant
- **WHEN** uma requisição pública chega em `nomedonegocio.${APP_DOMAIN}` e existe um tenant com `subdomain` = `nomedonegocio`
- **THEN** o sistema injeta esse tenant no contexto da requisição
- **AND** as leituras subsequentes ficam filtradas por esse `tenant_id`

#### Scenario: Subdomínio inexistente é rejeitado
- **WHEN** uma requisição pública chega em um subdomínio sem tenant correspondente
- **THEN** o sistema não injeta nenhum tenant e a requisição é tratada como não resolvida (sem cair em outro tenant)

### Requirement: Tenant resolution by JWT on authenticated routes
O sistema SHALL resolver o tenant atual pelo JWT nas rotas autenticadas do painel e injetá-lo no contexto da requisição, ignorando o subdomínio como fonte de tenant nesse contexto.

#### Scenario: JWT válido determina o tenant
- **WHEN** uma requisição autenticada do painel apresenta um JWT válido que carrega o tenant
- **THEN** o sistema injeta o tenant do JWT no contexto da requisição
- **AND** todas as queries ficam filtradas por esse `tenant_id`
