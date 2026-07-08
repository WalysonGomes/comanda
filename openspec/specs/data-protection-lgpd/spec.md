# data-protection-lgpd Specification

## Purpose
TBD - created by syncing change reliability-and-security. Update Purpose after archive.

## Requirements
### Requirement: Política de privacidade publicada

O produto SHALL publicar uma política de privacidade acessível publicamente e linká-la no footer do storefront. A política SHALL descrever quais dados pessoais são tratados (dono e cliente final), finalidade e base legal.

#### Scenario: Política de privacidade acessível pelo storefront
- **WHEN** um visitante acessa o link de política de privacidade no footer do storefront
- **THEN** o sistema exibe a política de privacidade publicada
- **AND** o conteúdo descreve dados tratados, finalidade e base legal

### Requirement: Termos com cláusula controlador-operador

O produto SHALL publicar termos de serviço contendo cláusula que estabeleça a relação controlador-operador da LGPD: o tenant (dono do negócio) é o controlador dos dados dos seus clientes e o Comanda atua como operador.

#### Scenario: Termos definem controlador e operador
- **WHEN** o dono acessa os termos de serviço
- **THEN** os termos contêm cláusula que define o tenant como controlador e o Comanda como operador dos dados dos clientes finais

### Requirement: E-mail de contato de privacidade monitorado

O produto SHALL manter um e-mail de contato de privacidade ativo e monitorado, publicado na política de privacidade, por onde titulares possam exercer seus direitos.

#### Scenario: Canal de privacidade publicado e monitorado
- **WHEN** um titular consulta a política de privacidade para exercer um direito
- **THEN** encontra um e-mail de contato de privacidade ativo
- **AND** esse canal é monitorado para responder às solicitações

### Requirement: Registro interno de atividades de tratamento

O produto SHALL manter um registro interno de atividades de tratamento (RoPA) documentando as operações de tratamento de dados pessoais realizadas pelo sistema.

#### Scenario: RoPA existe e cobre os tratamentos do MVP
- **WHEN** o registro de atividades de tratamento é consultado
- **THEN** documenta as operações de tratamento de dados pessoais do MVP (cadastro do dono, dados do cliente no pedido, telefone, endereço)
