## Context

Esta é a última change do MVP do Comanda. Todas as capabilities de produto (auth, menu, storefront, orders, plans, onboarding, hardening, PWA) já estão especificadas; falta a implantação. O PRD (Seção 7.3) fixa a decisão de arquitetura: **um único VPS** roda backend + PostgreSQL + proxy reverso. Sem serviços gerenciados, sem orquestração, sem múltiplos hosts, sem CDN — menos superfície de falha, custo fixo baixo e previsível (VPS ~€4/mês, domínio ~R$40/ano).

Restrições herdadas:
- Backend Spring Boot 4.1 (Java 21, Virtual Threads) serve a SPA React/Vite como recursos estáticos (definido em `foundations`).
- Resolução de tenant por subdomínio no storefront público (`nomedonegocio.${APP_DOMAIN}`) e por JWT no painel.
- Storage de fotos abstraído para migração futura a bucket S3-compatível.
- Constraints/segredos nunca hardcoded (Regra 8 e Seção 9 do PRD).

## Goals / Non-Goals

**Goals:**
- Um build → um artefato (JAR com SPA embarcada) → um deploy.
- HTTPS automático e TLS wildcard para `*.${APP_DOMAIN}` sem provisionamento manual por tenant.
- Toda configuração sensível por ambiente; boot falha sem segredo obrigatório.
- Backup diário do Postgres com cópia off-site.
- Ambiente reprodutível (Docker Compose) para local e produção.

**Non-Goals:**
- Serviços gerenciados, múltiplos servidores, CDN, Kubernetes.
- Integração Stripe (Fase 2), empacotamento nativo, migração real para bucket S3 (apenas deixar a porta aberta).
- Pipeline CI/CD elaborado — deploy pode ser um script/comando; automação avançada fica fora.

## Decisions

### Topologia: tudo em um host
App (JAR), PostgreSQL 17 e Caddy no mesmo VPS. Caddy escuta 80/443, termina TLS e faz proxy para o backend em porta local (ex.: 8080). Postgres escuta apenas em loopback.

```
Internet ──443──> Caddy (TLS wildcard *.${APP_DOMAIN})
                     │ proxy_pass, preserva Host
                     ▼
              Spring Boot JAR :8080  ── serve API + SPA estática
                     │ JDBC (localhost:5432)
                     ▼
              PostgreSQL 17 (loopback) ── volume/disco
                     ▲
              disco local: /var/comanda/uploads (fotos)
```

**Por quê:** o PRD manda consolidar. Alternativa (serviços gerenciados / múltiplos hosts) foi explicitamente rejeitada por custo e complexidade antes da validação.

### Caddy para TLS wildcard
Wildcard `*.${APP_DOMAIN}` exige desafio DNS-01 (não HTTP-01). Caddy resolve com plugin de DNS do provedor + credenciais de API por ambiente. Um único certificado cobre todos os tenants; novo subdomínio não dispara nova emissão.

**Alternativa considerada:** Nginx + certbot. Rejeitado — Caddy faz emissão/renovação automática com config mínima (decisão do PRD 7.3) e menos partes móveis para dev solo.

**Trade-off:** DNS-01 acopla o deploy à API DNS do provedor; a credencial vira um segredo obrigatório.

### Artefato único: SPA embarcada no JAR
Maven aciona o build do Vite (via plugin, ex. frontend-maven-plugin) e copia `dist/` para `src/main/resources/static` (ou `classpath:/static`) antes do package. Spring serve a SPA; um fallback de rota devolve `index.html` para rotas client-side do React Router, sem colidir com prefixo de API (ex.: tudo sob `/api/**` é backend).

**Por quê:** um build/deploy, sem servidor de frontend, sem conflito de build target (PRD 7.2).

### Storage de fotos atrás de abstração
Interface de storage (ex.: `PhotoStorage` com `store`/`resolveUrl`) com implementação `LocalDiskPhotoStorage` no MVP gravando em diretório configurável. Domínio (menu/pedido) só conhece a interface. Migração futura = nova impl + config, zero mudança de domínio.

**Por quê:** PRD pede migrar a bucket sem reescrever domínio. Boring: sem SDK S3 no MVP.

### Configuração e segredos por ambiente
Variáveis obrigatórias: `DB_URL`/`DB_USER`/`DB_PASSWORD`, `JWT_SECRET`, `SMTP_HOST`/`SMTP_USER`/`SMTP_PASSWORD`, `APP_DOMAIN`, `STORAGE_PATH`, credencial da API DNS do Caddy. Validação de presença no startup (ex.: `@ConfigurationProperties` + validação / fail-fast) — sem default inseguro. Segredos em arquivo `.env` fora do versionamento (ou secret store), nunca no repositório.

### Backup diário + off-site
`cron` no host roda `pg_dump` diário para arquivo datado; passo seguinte envia a cópia para destino off-site (bucket/host remoto por credencial de ambiente). Retenção simples por rotação de arquivos.

### Docker Compose opcional
Compose com serviços `app`, `db` (Postgres 17), `caddy`, redes e volumes (dados do Postgres, uploads, dados/certs do Caddy). Mesma definição local e produção para paridade de ambiente.

## Risks / Trade-offs

- **[Ponto único de falha — um VPS]** → aceito por decisão de produto no MVP; mitigado por backup diário off-site e restauração testada. Escalar horizontalmente é problema pós-validação.
- **[Disco local para fotos não escala / não é redundante]** → abstração de storage permite trocar por bucket sem reescrever domínio; incluir uploads no backup ou aceitar perda de imagens como risco documentado.
- **[TLS wildcard depende da API DNS do provedor]** → credencial como segredo obrigatório; se a API DNS cair, renovação falha — monitorar validade do certificado.
- **[Segredo obrigatório ausente derruba o boot]** → comportamento desejado (fail-fast > default inseguro); mitigado por `.env.example` documentando todas as variáveis exigidas.
- **[Deploy reinicia o processo → breve indisponibilidade]** → aceitável para MVP; janela curta. Zero-downtime fica fora de escopo.
- **[pg_dump em host com pouca RAM (Hetzner CAX11)]** → dump é leve no volume do MVP; revisitar se o banco crescer.

## Migration Plan

1. Provisionar VPS; instalar Docker/Docker Compose (ou runtime nativo do JAR + Postgres + Caddy).
2. Registrar DNS: `A`/`AAAA` de `${APP_DOMAIN}` e wildcard `*.${APP_DOMAIN}` → IP do VPS. Configurar credencial da API DNS para DNS-01.
3. Definir `.env` de produção com todos os segredos obrigatórios.
4. Build do artefato único (Maven + Vite) e subida (Compose up ou `java -jar`).
5. Caddy emite o certificado wildcard; validar `https://<tenant>.${APP_DOMAIN}`.
6. Instalar cron de backup; validar dump e cópia off-site; testar restauração num banco descartável.

**Rollback:** manter o JAR da versão anterior; em falha, reimplantar o artefato anterior e reiniciar. Banco: restaurar do último dump se uma migration quebrar (Flyway é versionado; evitar migrations destrutivas).

## Open Questions

- Provedor DNS definitivo (define qual plugin de DNS do Caddy) — decisão operacional, não bloqueia a spec.
- Destino off-site do backup (bucket S3-compat barato vs. outro host) — escolher no apply.
- Provedor SMTP free tier específico — escolher no apply; não altera a interface de envio.
