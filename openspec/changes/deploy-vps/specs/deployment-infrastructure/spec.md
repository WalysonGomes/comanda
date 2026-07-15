## ADDED Requirements

### Requirement: Artefato único deployável

O sistema SHALL ser empacotado como um único artefato JAR do Spring Boot 4.1 que embarca o build estático da SPA React/Vite, de modo que um único build produza tudo o que é implantado e um único processo sirva tanto a API quanto o frontend.

#### Scenario: Build produz artefato único

- **WHEN** o build de produção é executado
- **THEN** o pipeline Maven gera o build do Vite e o embarca nos recursos estáticos do JAR
- **AND** o resultado é um único arquivo JAR executável

#### Scenario: JAR serve API e SPA no mesmo processo

- **WHEN** o JAR é iniciado no VPS
- **THEN** as rotas de API respondem sob o mesmo processo que serve os assets estáticos da SPA
- **AND** nenhum servidor de frontend separado é necessário

#### Scenario: Deploy substitui uma unidade

- **WHEN** uma nova versão é implantada
- **THEN** a operação substitui o único artefato JAR e reinicia o processo
- **AND** não há múltiplos artefatos a sincronizar

### Requirement: Proxy reverso com HTTPS wildcard para subdomínios de tenant

O sistema SHALL usar Caddy como proxy reverso provendo HTTPS automático com certificado TLS wildcard para `*.${APP_DOMAIN}`, de modo que cada tenant em `nomedonegocio.${APP_DOMAIN}` seja atendido sob HTTPS sem provisionamento manual de certificado por tenant.

#### Scenario: Novo tenant acessível sob HTTPS sem provisionamento manual

- **WHEN** um novo tenant é criado com subdomínio `nomedonegocio`
- **THEN** `https://nomedonegocio.${APP_DOMAIN}` é atendido pelo Caddy com TLS válido
- **AND** nenhuma emissão manual de certificado ou configuração por tenant é necessária

#### Scenario: Certificado wildcard emitido automaticamente

- **WHEN** o Caddy inicia
- **THEN** obtém e renova automaticamente o certificado TLS wildcard para `*.${APP_DOMAIN}`

#### Scenario: HTTP redirecionado para HTTPS

- **WHEN** uma requisição chega em HTTP simples a um subdomínio de tenant
- **THEN** o proxy redireciona para HTTPS

#### Scenario: Proxy encaminha ao backend preservando o host

- **WHEN** uma requisição HTTPS chega para um subdomínio de tenant
- **THEN** o Caddy encaminha ao processo do backend preservando o Host original, para que a resolução de tenant por subdomínio funcione

### Requirement: Banco PostgreSQL no próprio host

O sistema SHALL usar PostgreSQL 17 rodando no mesmo VPS, sem serviço gerenciado, sem pausa por inatividade e sem cold start.

#### Scenario: Aplicação conecta ao Postgres local

- **WHEN** o backend inicia
- **THEN** conecta à instância PostgreSQL 17 no mesmo host usando credenciais lidas de variáveis de ambiente

#### Scenario: Banco não pausa por inatividade

- **WHEN** não há tráfego por um período prolongado
- **THEN** o Postgres permanece disponível e a primeira requisição seguinte não sofre cold start

### Requirement: Storage de fotos em disco local com abstração migrável

O sistema SHALL armazenar as fotos dos produtos em disco local do VPS no MVP, acessadas por meio de uma abstração de storage, de modo que migrar para um bucket S3-compatível no futuro não exija reescrever as regras de domínio.

#### Scenario: Upload persistido em disco local

- **WHEN** uma foto de produto é enviada
- **THEN** o arquivo é gravado no diretório de storage configurado no disco local do VPS
- **AND** o caminho/URL persistido no banco é resolvido pela abstração de storage

#### Scenario: Foto servida a partir do disco local

- **WHEN** o cardápio referencia a foto de um produto
- **THEN** a imagem é servida a partir do storage local

#### Scenario: Backend de storage é substituível por configuração

- **WHEN** a implementação de storage é trocada de disco local para bucket S3-compatível
- **THEN** apenas a implementação da abstração e a configuração mudam
- **AND** as regras de domínio de menu/pedido permanecem inalteradas

### Requirement: Configuração por variáveis de ambiente e segredos

O sistema SHALL ler toda a configuração sensível — credenciais de banco, segredo JWT, credenciais SMTP, `APP_DOMAIN` e caminho de storage — exclusivamente de variáveis de ambiente/segredos, nunca de valores hardcoded no código ou versionados em texto claro.

#### Scenario: Segredos lidos do ambiente

- **WHEN** a aplicação inicia
- **THEN** obtém credenciais de banco, segredo JWT, credenciais SMTP, `APP_DOMAIN` e caminho de storage a partir de variáveis de ambiente

#### Scenario: Boot falha quando segredo obrigatório está ausente

- **WHEN** um segredo obrigatório não está definido no ambiente
- **THEN** a aplicação falha no boot com erro claro
- **AND** NÃO sobe com um default inseguro

#### Scenario: Nenhum segredo em texto claro no repositório

- **WHEN** o repositório é inspecionado
- **THEN** nenhuma credencial de banco, segredo JWT ou chave SMTP aparece hardcoded no código ou versionada em texto claro

### Requirement: Cliente SMTP transacional configurável por ambiente

O sistema SHALL disponibilizar um cliente SMTP transacional (provedor free tier) configurado exclusivamente a partir de variáveis de ambiente, pronto para uso por qualquer fluxo que venha a disparar e-mail.

Nenhuma feature do escopo do MVP (PRD Seção 3 — autoridade de escopo pela regra 1 da Seção 12) dispara e-mail transacional: o login do painel é e-mail + senha, sem recuperação por e-mail, e o onboarding não envia mensagem. A tabela de infraestrutura do PRD (Seção 7.3) prevê SMTP, então esta change entrega a plumbing; a validação de envio ponta a ponta pertence à change que introduzir o primeiro fluxo de e-mail do produto.

#### Scenario: Credenciais SMTP lidas do ambiente

- **WHEN** a aplicação sobe no perfil `prod`
- **THEN** o cliente SMTP é configurado a partir das variáveis de ambiente, sem valor hardcoded no código

#### Scenario: Falha de SMTP não derruba a aplicação

- **WHEN** o provedor SMTP está indisponível no momento do envio
- **THEN** a falha é registrada e tratada sem derrubar a aplicação

### Requirement: Backup diário do banco com cópia off-site

O sistema SHALL realizar um dump diário do PostgreSQL via cron no host e manter uma cópia off-site do backup.

#### Scenario: Dump diário agendado

- **WHEN** o horário diário agendado é atingido
- **THEN** o cron executa um dump completo do PostgreSQL no host

#### Scenario: Cópia off-site do backup

- **WHEN** um dump diário é concluído

- **THEN** uma cópia do dump é enviada para um destino off-site

#### Scenario: Restauração a partir do dump

- **WHEN** o dump mais recente é usado para restaurar o banco
- **THEN** o banco é restaurado a um estado consistente a partir daquele dump

### Requirement: Ambiente reprodutível via Docker Compose

O sistema SHALL prover um Docker Compose que suba app, PostgreSQL e Caddy juntos, de modo a reproduzir o ambiente local e de produção com uma única definição.

#### Scenario: Compose sobe a stack completa

- **WHEN** o Docker Compose é iniciado
- **THEN** os serviços de app, PostgreSQL e Caddy sobem e conseguem se comunicar entre si
- **AND** a aplicação fica acessível através do Caddy
