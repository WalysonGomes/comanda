# business-settings Specification

## Purpose
Permitir que o dono mantenha os dados públicos e operacionais do próprio negócio sem comprometer isolamento multi-tenant.

## Requirements

### Requirement: Leitura e atualização isoladas
O painel SHALL ler e atualizar nome, logo, WhatsApp, horários, taxa fixa de entrega, pedido mínimo e subdomínio usando exclusivamente o `tenant_id` do JWT. O corpo SHALL NOT selecionar tenant, plano, quotas ou dono. Toda alteração multi-tabela SHALL ser atômica.

#### Scenario: Dono atualiza seus dados
- **WHEN** um dono autenticado salva dados válidos
- **THEN** somente seu tenant é alterado e a vitrine, status do painel, Meu link e URLs passam a refletir os novos dados

### Requirement: Subdomínio canônico e seguro
O subdomínio SHALL usar o normalizador e a política reservada canônica do backend, manter sintaxe e unicidade e produzir `<tenant>.${APP_DOMAIN}`. Conflitos SHALL retornar erro estável e claro. A UI SHALL manter visível o aviso de que a troca invalida QR Codes e links existentes.

#### Scenario: Troca válida de subdomínio
- **WHEN** o dono salva um rótulo válido e único
- **THEN** a nova URL passa a resolver o tenant e a antiga deixa de resolver

### Requirement: Logo validado por conteúdo
O logo SHALL reutilizar a abstração segura de storage, limitar tamanho e aceitar somente JPEG, PNG ou WebP identificados por magic bytes, expondo URL utilizável no painel e storefront.

#### Scenario: Extensão forjada é rejeitada
- **WHEN** um arquivo declara imagem mas seus magic bytes não correspondem a tipo suportado
- **THEN** o upload é rejeitado sem substituir o logo atual

### Requirement: Horários e valores válidos
Horários SHALL reutilizar a fonte de verdade de `business_hours`, cobrir os sete dias e ser persistidos atomicamente. Taxa e pedido mínimo SHALL ter no máximo duas casas decimais e nunca ser negativos.

#### Scenario: Falha de validação preserva estado
- **WHEN** qualquer horário ou valor monetário da atualização é inválido
- **THEN** nenhuma parte dos dados ou horários é alterada
