# owner-auth Specification

## Purpose
TBD - created by syncing change owner-auth. Update Purpose after archive.

## Requirements
### Requirement: Cadastro self-service do dono

O sistema SHALL permitir que um dono crie sua própria conta em uma única operação atômica que cria: o `tenant` (com subdomínio único), o `user` OWNER (com senha) e os dados iniciais mínimos do negócio (nome do negócio e número de WhatsApp). Se qualquer parte falhar, NENHUMA parte SHALL ser persistida (tudo ou nada).

#### Scenario: Cadastro bem-sucedido cria tenant + OWNER + subdomínio
- **WHEN** um visitante envia nome, nome do negócio, subdomínio disponível, WhatsApp, e-mail não cadastrado e senha válida
- **THEN** o sistema cria o `tenant`, o `user` com papel OWNER vinculado a esse tenant, persiste os dados mínimos do negócio, e retorna sucesso com uma sessão autenticada iniciada

#### Scenario: Falha parcial não persiste nada
- **WHEN** o cadastro falha depois de criar o tenant mas antes de criar o usuário OWNER
- **THEN** o sistema reverte a operação inteira e nenhum tenant, usuário ou dado de negócio permanece no banco

### Requirement: Subdomínio único e válido

O sistema SHALL garantir que cada subdomínio de tenant seja único, validado por constraint no banco (`subdomain` único) além da checagem em código. Um cadastro com subdomínio já em uso SHALL ser rejeitado com erro claro, sem criar o tenant.

#### Scenario: Subdomínio já em uso
- **WHEN** um visitante tenta se cadastrar com um subdomínio que já pertence a outro tenant
- **THEN** o sistema rejeita o cadastro com uma mensagem indicando que o endereço já está em uso e não cria nenhum registro

#### Scenario: Subdomínio normalizado antes da validação
- **WHEN** o subdomínio informado contém maiúsculas ou espaços
- **THEN** o sistema normaliza (minúsculas, sem espaços) antes de checar unicidade e persiste a forma normalizada

### Requirement: Comunicação do limite do plano Gratuito antes do cadastro

O sistema SHALL comunicar o limite do plano Gratuito (até 30 pedidos/mês) ao usuário ANTES da conclusão do cadastro, na própria interface de criação de conta.

#### Scenario: Limite exibido antes de criar conta
- **WHEN** o visitante está na tela de criação de conta, antes de concluir o cadastro
- **THEN** a interface exibe que o plano Gratuito oferece até 30 pedidos/mês

### Requirement: E-mail único por conta

O sistema SHALL rejeitar cadastro cujo e-mail já esteja registrado, com mensagem clara, sem criar o tenant nem o usuário.

#### Scenario: E-mail já cadastrado
- **WHEN** um visitante tenta se cadastrar com um e-mail que já existe no sistema
- **THEN** o sistema rejeita o cadastro informando que o e-mail já está cadastrado e não cria nenhum registro

### Requirement: Login com e-mail e senha

O sistema SHALL autenticar o dono por e-mail e senha, iniciando uma sessão autenticada quando as credenciais forem válidas.

#### Scenario: Login com credenciais válidas
- **WHEN** o dono envia e-mail e senha corretos de uma conta ativa
- **THEN** o sistema inicia a sessão, emitindo access token e refresh token (ver requisito de sessão JWT)

#### Scenario: Login em conta inativa
- **WHEN** o dono envia credenciais corretas de uma conta com `is_active` falso
- **THEN** o sistema recusa o login com a mesma mensagem genérica de credencial inválida

### Requirement: Falha de login não permite enumeração de usuários

O sistema SHALL responder a qualquer falha de login (e-mail inexistente, senha incorreta, conta inativa) com a MESMA mensagem genérica de credencial inválida, sem revelar se o e-mail existe. O tempo de resposta NÃO SHALL revelar a existência do e-mail (comparação de hash mesmo quando o usuário não existe).

#### Scenario: E-mail inexistente e senha incorreta indistinguíveis
- **WHEN** ocorre uma falha de login por e-mail inexistente e, separadamente, por senha incorreta de e-mail existente
- **THEN** ambas retornam a mesma mensagem genérica ("e-mail ou senha incorretos") e o mesmo código de erro, sem diferença observável que indique qual e-mail existe

### Requirement: Armazenamento seguro de senha

O sistema SHALL armazenar senhas apenas como hash forte (algoritmo de derivação lento, ex.: bcrypt/argon2), nunca em texto claro. Senha e token NUNCA SHALL aparecer em logs.

#### Scenario: Senha persistida como hash
- **WHEN** uma conta é criada com uma senha
- **THEN** o valor persistido em `password_hash` é um hash forte irreversível e a senha em texto claro não é gravada em lugar nenhum

#### Scenario: Senha e token ausentes dos logs
- **WHEN** ocorre qualquer operação de cadastro, login ou refresh
- **THEN** nenhum log emitido contém a senha em texto claro nem o valor de nenhum token

### Requirement: Sessão JWT stateless com refresh rotacionado

O sistema SHALL usar JWT stateless para a sessão do painel: um access token de curta duração (15–30 min) devolvido ao cliente para manter em memória, e um refresh token de longa duração (7–30 dias) entregue em cookie `httpOnly`. A cada uso do refresh token, o sistema SHALL rotacionar (emitir um novo refresh token e invalidar o anterior). O sistema NÃO SHALL depender de armazenamento do JWT em `localStorage`.

#### Scenario: Emissão na autenticação
- **WHEN** um login ou cadastro é bem-sucedido
- **THEN** o sistema devolve um access token curto no corpo da resposta e define o refresh token em um cookie `httpOnly`

#### Scenario: Refresh rotaciona o token
- **WHEN** o cliente usa um refresh token válido para obter novo access token
- **THEN** o sistema emite um novo access token e um novo refresh token, e o refresh token anterior deixa de ser aceito

#### Scenario: Refresh token reutilizado é rejeitado
- **WHEN** um refresh token já rotacionado (anterior) é apresentado novamente
- **THEN** o sistema rejeita a requisição e não emite novos tokens

### Requirement: Claim de tenant no JWT

O sistema SHALL incluir o identificador do tenant como claim no access token, no formato consumido pela capability `multi-tenancy` para resolver o tenant atual no contexto autenticado do painel.

#### Scenario: Access token carrega o tenant
- **WHEN** o sistema emite um access token para um dono autenticado
- **THEN** o token contém um claim com o identificador do tenant desse dono, e requisições autenticadas com esse token resolvem para o tenant correto

#### Scenario: Requisição do painel sem token não acessa dados
- **WHEN** uma rota autenticada do painel é acessada sem access token válido
- **THEN** a requisição é recusada antes de qualquer acesso a dados de tenant

### Requirement: Shell autenticado do painel protegido por sessão

O frontend SHALL prover o shell do painel do dono como área protegida: sem sessão válida, o acesso a qualquer rota do painel redireciona para o login. Nesta change o conteúdo do shell é vazio (apenas a casca autenticada). O access token SHALL ser mantido apenas em memória no cliente, nunca em `localStorage`.

#### Scenario: Acesso ao painel sem sessão redireciona para login
- **WHEN** um usuário sem sessão válida tenta abrir uma rota do painel
- **THEN** o frontend redireciona para a tela de login

#### Scenario: Access token apenas em memória
- **WHEN** o cliente recebe um access token após autenticar
- **THEN** o token é mantido em memória da aplicação e não é gravado em `localStorage` nem `sessionStorage`
