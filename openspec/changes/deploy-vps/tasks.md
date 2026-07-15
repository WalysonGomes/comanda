## 1. Artefato único (build)

- [x] 1.1 Configurar o build Maven para acionar o build do Vite (ex.: frontend-maven-plugin) e copiar `dist/` para `classpath:/static` antes do `package`
- [x] 1.2 Configurar o Spring para servir a SPA estática e devolver `index.html` como fallback das rotas client-side, sem colidir com o prefixo de API (`/api/**`)
- [x] 1.3 Verificar que `mvn package` produz um único JAR executável que serve API + SPA no mesmo processo
- [x] 1.4 Rodar o JAR localmente e confirmar que API e frontend respondem sob o mesmo processo

## 2. Configuração por ambiente e segredos

- [x] 2.1 Externalizar toda config sensível (DB, `JWT_SECRET`, SMTP, `APP_DOMAIN`, `STORAGE_PATH`, credencial DNS) para variáveis de ambiente
- [x] 2.2 Implementar validação fail-fast no startup: boot falha com erro claro se um segredo obrigatório estiver ausente (sem default inseguro)
- [x] 2.3 Criar `.env.example` documentando todas as variáveis obrigatórias; garantir que `.env` real está no `.gitignore`
- [x] 2.4 Auditar o repositório: nenhuma credencial/segredo hardcoded ou versionado em texto claro

## 3. PostgreSQL 17 no host

- [x] 3.1 Provisionar PostgreSQL 17 escutando apenas em loopback
- [x] 3.2 Configurar o backend para conectar via credenciais do ambiente; confirmar execução das migrations Flyway no boot
- [x] 3.3 Confirmar que o banco não pausa por inatividade e não sofre cold start

## 4. Storage de fotos com abstração migrável

- [x] 4.1 Definir a interface de storage (`store` / `resolveUrl`) consumida pelo domínio de menu
- [x] 4.2 Implementar `LocalDiskPhotoStorage` gravando em `STORAGE_PATH` no disco local
- [x] 4.3 Confirmar upload persistido em disco e foto servida a partir do storage local
- [x] 4.4 Garantir que o domínio só depende da interface (troca futura para bucket = nova impl + config, sem mudança de domínio)

## 5. Caddy — proxy reverso e TLS wildcard

- [x] 5.1 Escrever o Caddyfile: proxy para o backend em porta local preservando o `Host` original (para resolução de tenant por subdomínio)
- [x] 5.2 Configurar TLS wildcard `*.${APP_DOMAIN}` via desafio DNS-01 com o plugin de DNS do provedor e credencial do ambiente
- [x] 5.3 Garantir redirecionamento de HTTP para HTTPS
- [x] 5.4 Validar que um novo subdomínio de tenant é atendido sob HTTPS sem provisionamento manual de certificado

## 6. E-mail transacional (SMTP)

- [x] 6.1 Configurar o cliente SMTP transacional (free tier) com credenciais do ambiente
- [x] 6.2 Garantir que falha de SMTP é registrada e tratada sem derrubar a aplicação

> Confirmar envio ponta a ponta ficou fora desta change: nenhuma feature do MVP (PRD Seção 3) dispara e-mail transacional — o painel usa login e-mail + senha, sem recuperação por e-mail. Esta change entrega a plumbing (`TransactionalMailService`, configurável por ambiente); a validação de envio pertence à change que introduzir o primeiro fluxo de e-mail do produto.

## 7. Backup diário com cópia off-site

- [x] 7.1 Criar script de `pg_dump` diário gerando arquivo datado
- [x] 7.2 Agendar o dump via cron no host
- [x] 7.3 Enviar cópia do dump para destino off-site (credencial do ambiente); definir retenção/rotação
- [x] 7.4 Testar restauração do dump em um banco descartável e confirmar estado consistente

## 8. Ambiente reprodutível (Docker Compose)

- [x] 8.1 Escrever `docker-compose.yml` com serviços `app`, `db` (Postgres 17) e `caddy`, redes e volumes (dados do Postgres, uploads, certs/dados do Caddy)
- [x] 8.2 Confirmar que `compose up` sobe a stack completa e a aplicação fica acessível através do Caddy

## 9. Deploy e verificação end-to-end

- [x] 9.1 Provisionar o VPS e registrar DNS: `${APP_DOMAIN}` e wildcard `*.${APP_DOMAIN}` apontando para o IP
- [x] 9.2 Definir o `.env` de produção com todos os segredos obrigatórios
- [x] 9.3 Implantar o artefato único e validar `https://<tenant>.${APP_DOMAIN}` (storefront) e o painel do dono ponta a ponta
- [x] 9.4 Validar HTTPS/certificado wildcard, backup rodando e procedimento de rollback (reimplantar JAR anterior)
- [x] 9.5 Rodar `openspec validate deploy-vps --strict` e corrigir pendências
