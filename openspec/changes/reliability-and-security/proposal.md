## Why

O Comanda promete "nunca mais perder pedido" e opera dados pessoais de donos e clientes num SaaS multi-tenant. Duas categorias de garantia atravessam **todas** as features já propostas e não pertencem a nenhuma delas isoladamente: **confiabilidade operacional** (Seção 4 do PRD — nenhuma falha silenciosa, fallbacks, idempotência, rastreabilidade) e **segurança/LGPD** (Seção 9 — isolamento por tenant sem IDOR, rate limiting, sem enumeração de usuário, headers, CORS, uploads, sem dado sensível em log, política de privacidade). Esta change consolida essas garantias como **hardening transversal**: audita o que as changes 1–6 implementaram, fecha lacunas e torna cada critério de lançamento das Seções 4 e 9 um requisito **verificável por teste**. Sem ela, os critérios de lançamento do PRD ficam dispersos e não comprováveis.

## What Changes

Nenhuma feature nova de produto e **nenhuma UI nova** — esta change endurece e verifica o que já existe.

**Confiabilidade (PRD Seção 4):**
- **Auditoria de estado silencioso**: garantir que toda superfície (storefront e painel) trata cada falha com mensagem clara + caminho de recuperação; nenhum estado ambíguo/silencioso.
- **Fallbacks de interface verificados**: `wa.me` não abre → tela de copiar mensagem; upload de foto falha → produto salvo sem foto + tentar de novo; polling falha → banner com timestamp da última atualização bem-sucedida.
- **Idempotência revisada cross-change**: criação de pedido (`idempotency_key` único por tenant, cross-tenant rejeitado) e avanço de status (request duplicado não gera dois registros de histórico) — revisão e teste de regressão.
- **Rastreabilidade mínima**: `order_status_history` append-only, cada registro com `from_status`, `to_status`, `changed_by`, `created_at`; nunca update/delete.
- **Plano de resposta a incidentes (dev solo)**: responsável, canal de emergência, e critério de notificação à ANPD em até 72h para vazamento com risco real aos titulares.

**Segurança (PRD Seção 9):**
- **IDOR**: todo endpoint que acessa recurso por ID é isolado por tenant e coberto por teste de isolamento cross-tenant (um tenant nunca lê/escreve recurso de outro).
- **Rate limiting** ativo nos endpoints de autenticação (login/refresh).
- **Enumeração de usuário impossível** via login (mensagem genérica, timing não revelador).
- **Headers de segurança HTTP** configurados (HSTS, X-Content-Type-Options, X-Frame-Options/frame-ancestors, Referrer-Policy, CSP básica) e **CORS restrito** a origens conhecidas (domínio da app + subdomínios de tenant).
- **Upload valida magic bytes** (tipo real do arquivo), não só a extensão.
- **Nenhum dado sensível em log**: senha, token e telefone nunca aparecem em nenhum log.

**LGPD (PRD Seção 9):**
- Política de privacidade publicada e linkada no footer do storefront.
- Termos de serviço com **cláusula controlador-operador** (tenant = controlador; Comanda = operador).
- E-mail de contato de privacidade ativo e monitorado.
- Registro interno de atividades de tratamento (RoPA) criado.

## Capabilities

### New Capabilities
- `operational-reliability`: garantias transversais de confiabilidade verificáveis por teste — ausência de estado silencioso de falha em toda a UI, fallbacks de interface (WhatsApp/upload/polling), idempotência de criação de pedido e de avanço de status, histórico append-only e rastreável, e plano de resposta a incidentes para dev solo.
- `application-security`: garantias transversais de segurança verificáveis por teste — isolamento por tenant resistente a IDOR em todo acesso por ID, rate limiting na autenticação, ausência de enumeração de usuário, headers de segurança HTTP, CORS restrito, validação de magic bytes em uploads, e ausência de dados sensíveis em logs.
- `data-protection-lgpd`: obrigações de conformidade LGPD — política de privacidade publicada, termos com cláusula controlador-operador, e-mail de contato de privacidade monitorado, e registro interno de atividades de tratamento.

### Modified Capabilities
<!-- Nenhuma delta MODIFIED. As specs de foundations/owner-auth/menu-management/public-storefront/
     order-operation/plans-and-onboarding ainda não estão arquivadas em openspec/specs/ no momento
     do propose (o workflow propõe todas as 9 changes com baseline vazio e só então implementa+arquiva
     na ordem 1→9). Modificar um requisito sem baseline arquivado quebraria `openspec validate --strict`.
     Seguindo a precedência já adotada por order-operation e plans-and-onboarding, os requisitos
     transversais entram como capabilities NOVAS de verificação (SHALL testáveis que não existem como
     requisito nas specs de feature). As dependências sobre auth/orders/menu/storefront são descritas
     em prosa e satisfeitas pelas capabilities já definidas nas changes anteriores; no apply (change 7,
     após arquivar 1–6) estes requisitos auditam e cruzam as capabilities existentes. -->

## Impact

- **Backend:** camada de segurança HTTP compartilhada — filtro/config de CORS restrito a origens conhecidas, headers de segurança (via Spring Security), rate limiter nos endpoints de auth (login/refresh). Revisão de todo endpoint com `{id}` para garantir filtro por `tenant_id` (do JWT no painel; do subdomínio no público) + testes de isolamento cross-tenant. Validação de magic bytes no serviço de upload (fatia `menu`). Auditoria/ajuste de logging para mascarar/omitir senha, token e telefone. APIs compatíveis com Spring Boot 4.1 / Spring Security 7 / Jakarta EE 11.
- **Frontend:** auditoria de superfícies de erro no storefront e no painel para garantir mensagem + recuperação em cada falha (sem estado silencioso); confirmação dos três fallbacks (WhatsApp/upload/polling) já previstos nas changes de feature. Página/rota estática de **política de privacidade** e **termos de serviço**; link no footer do storefront.
- **Testes (critério de lançamento):** suíte de testes de isolamento IDOR por endpoint; teste de rate limiting na autenticação; teste de não-enumeração de usuário; teste de rejeição de upload com magic bytes inválidos; teste de idempotência (criação e avanço); asserção de que logs não contêm dado sensível.
- **Documentação/operacional (não-código):** plano de resposta a incidentes; RoPA (registro de atividades de tratamento); e-mail de privacidade provisionado e monitorado; conteúdo legal da política e dos termos com cláusula controlador-operador.
- **Dependências:** depende de todas as changes de feature (`foundations`, `owner-auth`, `menu-management`, `public-storefront`, `order-operation`, `plans-and-onboarding`) — é a última camada de hardening antes de `pwa` e `deploy-vps`. Cruza especialmente `owner-auth` (rate limiting, enumeração, headers/CORS), `order-operation` (idempotência, append-only), `menu-management` (magic bytes, fallback de upload) e `multi-tenancy` de `foundations` (IDOR).
