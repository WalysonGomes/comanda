# application-security Specification

## Purpose
TBD - created by syncing change reliability-and-security. Update Purpose after archive.

## Requirements
### Requirement: Isolamento por tenant resistente a IDOR

O sistema SHALL isolar todo recurso por `tenant_id` e resistir a IDOR em todo acesso por ID. Nenhum tenant SHALL conseguir ler, alterar ou remover recurso pertencente a outro tenant, ainda que informe um ID válido de outro tenant. O tenant SHALL ser sempre derivado do contexto de segurança (JWT no painel, subdomínio no público), nunca do corpo ou do path da requisição. Todo endpoint que acessa recurso por ID SHALL ter teste de isolamento cross-tenant.

#### Scenario: Acesso a recurso de outro tenant é negado
- **WHEN** um usuário autenticado no tenant A requisita, por ID, um recurso (pedido, produto, categoria, adicional) pertencente ao tenant B
- **THEN** o sistema responde como não encontrado/negado
- **AND** não retorna nem modifica o recurso do tenant B

#### Scenario: Tenant vem do contexto de segurança, não do request
- **WHEN** uma requisição do painel inclui um `tenant_id` no corpo ou path diferente do tenant do JWT
- **THEN** o sistema ignora o valor fornecido e usa o tenant do JWT
- **AND** a operação permanece isolada ao tenant do contexto de segurança

#### Scenario: Todo endpoint por ID tem teste de isolamento
- **WHEN** a suíte de testes de segurança é executada
- **THEN** cada endpoint que acessa recurso por ID possui um teste de isolamento cross-tenant que confirma a negação de acesso entre tenants

### Requirement: Rate limiting na autenticação

O sistema SHALL aplicar rate limiting nos endpoints de autenticação (login e refresh) para conter tentativas de força bruta. Ao exceder o limite, o sistema SHALL responder com bloqueio temporário sem revelar informação sensível.

#### Scenario: Tentativas de login excessivas são limitadas
- **WHEN** um cliente excede o número permitido de tentativas de login numa janela de tempo
- **THEN** o sistema passa a rejeitar novas tentativas com resposta de limite excedido
- **AND** não revela se as credenciais estavam corretas nem se o e-mail existe

### Requirement: Enumeração de usuário impossível via login

O sistema NÃO SHALL permitir enumeração de usuários pelo fluxo de login. Falha de login SHALL retornar mensagem genérica de credencial inválida, independentemente de o e-mail existir, sem diferença observável de resposta ou de timing que revele a existência da conta.

#### Scenario: E-mail inexistente e senha errada respondem igual
- **WHEN** ocorre um login com e-mail inexistente e outro com e-mail existente porém senha incorreta
- **THEN** ambas as respostas são a mesma mensagem genérica de credencial inválida
- **AND** não há diferença observável (corpo, status ou timing) que revele qual e-mail existe

### Requirement: Headers de segurança HTTP

O sistema SHALL responder com headers de segurança HTTP em todas as respostas: HSTS, X-Content-Type-Options, proteção contra clickjacking (X-Frame-Options ou frame-ancestors), Referrer-Policy e uma Content-Security-Policy básica.

#### Scenario: Respostas incluem headers de segurança
- **WHEN** o cliente recebe qualquer resposta HTTP da aplicação
- **THEN** a resposta inclui HSTS, X-Content-Type-Options, proteção contra clickjacking, Referrer-Policy e CSP básica

### Requirement: CORS restrito a origens conhecidas

O sistema SHALL restringir CORS a origens conhecidas (o domínio da aplicação e os subdomínios de tenant). Requisições cross-origin de origens não autorizadas NÃO SHALL receber cabeçalhos CORS permissivos.

#### Scenario: Origem não autorizada é rejeitada pelo CORS
- **WHEN** uma requisição cross-origin chega de uma origem fora da lista de origens conhecidas
- **THEN** o sistema não responde com cabeçalhos CORS que autorizem a origem
- **AND** origens conhecidas (app e subdomínios de tenant) continuam autorizadas

### Requirement: Validação de magic bytes em uploads

O sistema SHALL validar o tipo real do arquivo enviado em uploads por magic bytes (assinatura do conteúdo), não apenas pela extensão ou pelo Content-Type declarado. Arquivos cujo conteúdo real não corresponde a um tipo de imagem permitido SHALL ser rejeitados.

#### Scenario: Arquivo com extensão de imagem mas conteúdo inválido é rejeitado
- **WHEN** um upload chega com extensão/Content-Type de imagem porém os magic bytes não correspondem a um tipo de imagem permitido
- **THEN** o sistema rejeita o upload
- **AND** não persiste o arquivo

### Requirement: Nenhum dado sensível em logs

O sistema NÃO SHALL registrar dados sensíveis em nenhum log. Senha, token (access ou refresh) e telefone do cliente NUNCA SHALL aparecer em logs, em nenhum nível, incluindo logs de erro e de requisição.

#### Scenario: Fluxos com dado sensível não vazam para o log
- **WHEN** um fluxo que manipula senha, token ou telefone é executado (login, refresh, criação de pedido)
- **THEN** nenhum log emitido contém a senha, o token ou o telefone em texto claro
- **AND** a auditoria de logs confirma a ausência desses dados
