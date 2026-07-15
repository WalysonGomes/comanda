## Why

Todas as capabilities do MVP estão especificadas, mas não há forma definida de colocar o produto no ar. O PRD (Seção 7.3) decide deliberadamente consolidar tudo em um único VPS — backend, banco e proxy no mesmo host — para minimizar superfície de falha, dependências externas e custo. Esta change fecha o MVP definindo o empacotamento e a implantação: um artefato, um deploy, HTTPS wildcard automático para os subdomínios de tenant.

## What Changes

- **Artefato único deployável**: JAR do Spring Boot 4.1 embarcando o build estático da SPA React/Vite. Um build Maven produz o que sobe ao servidor.
- **Caddy como proxy reverso** com HTTPS automático e certificado TLS wildcard para `*.${APP_DOMAIN}`, de modo que cada tenant em `nomedonegocio.${APP_DOMAIN}` funcione sem provisionamento manual de certificado ou DNS por tenant.
- **PostgreSQL 17 no próprio host**, sem serviço gerenciado, sem pausa por inatividade.
- **Armazenamento de fotos em disco local** do VPS, com o domínio estruturado atrás de uma abstração de storage que permita migrar para bucket S3-compatível no futuro sem reescrever regras de negócio.
- **Configuração 100% por variáveis de ambiente/segredos**: credenciais de banco, segredo JWT, chaves SMTP, domínio da aplicação — nada hardcoded no código nem versionado em texto claro.
- **SMTP transacional (free tier)** com credenciais lidas do ambiente e falha tratada sem derrubar a aplicação. Só a plumbing: nenhuma feature do MVP (PRD Seção 3) dispara e-mail hoje — o painel usa login e-mail + senha, sem recuperação por e-mail. Validar envio ponta a ponta cabe à change que introduzir o primeiro fluxo de e-mail.
- **Backup diário do Postgres** via cron no host, com cópia off-site.
- **Docker Compose opcional** para reproduzir o ambiente (app + Postgres + Caddy) localmente e em produção.

## Capabilities

### New Capabilities
- `deployment-infrastructure`: empacotamento em artefato único, topologia do VPS (app + Postgres + Caddy), proxy reverso com HTTPS/TLS wildcard para subdomínios de tenant, storage de fotos em disco com abstração migrável, configuração por variáveis de ambiente/segredos, e backup diário com cópia off-site.

### Modified Capabilities
<!-- Nenhuma. Esta change é infraestrutura transversal e não altera requisitos de comportamento de capabilities existentes. As dependências (SPA estática servida pelo backend, resolução de tenant por subdomínio, storage de fotos) já foram especificadas em foundations e menu-management; aqui elas são apenas implantadas. -->

## Impact

- **Build**: pipeline Maven passa a empacotar o build do Vite dentro do JAR (dependência de foundations, que define o backend servindo os recursos estáticos da SPA).
- **Operação**: novo host VPS provisionado; Caddy, PostgreSQL 17 e o JAR rodando no mesmo servidor; cron de backup; conta em provedor SMTP transacional; registro DNS wildcard `*.${APP_DOMAIN}` apontando para o VPS.
- **Configuração**: introdução de um conjunto de variáveis de ambiente obrigatórias (DB, JWT, SMTP, APP_DOMAIN, caminho de storage) — a aplicação deve falhar no boot se um segredo obrigatório estiver ausente, em vez de subir com default inseguro.
- **Depende de**: todas as changes anteriores (é a última). Não introduz APIs de produto novas.
- **Fora de escopo**: serviços gerenciados, múltiplos servidores, CDN, orquestração (Kubernetes) — decisão de produto de consolidar em um VPS.
