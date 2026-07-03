## Context

Change transversal de hardening. As changes 1–6 entregam as features do MVP; esta consolida as garantias das Seções 4 (Confiabilidade) e 9 (Segurança/LGPD) do PRD, que atravessam todas elas e viram critérios de lançamento verificáveis por teste.

Restrições vindas do PRD e do foundations:
- Backend Spring Boot 4.1 / Spring Security 7 / Jakarta EE 11 / Hibernate 7, Java 21 sobre Virtual Threads. Atenção: APIs de segurança do Spring Security 6/7 divergem de material de Spring Boot 3.x (config via lambda DSL, `SecurityFilterChain` bean).
- Multi-tenancy por discriminador `tenant_id` (foundations): tenant resolvido por subdomínio no público e por JWT no painel — o mecanismo já existe; esta change o **audita** e o **testa** para IDOR, não o reimplementa.
- Um único VPS atrás do Caddy (HTTPS/TLS wildcard). HSTS/TLS terminam no Caddy; a app emite os demais headers e a CSP.
- Dev solo: o plano de incidentes, o RoPA e os textos legais são entregáveis operacionais/documentais, não só código.

Estado no propose: nenhuma spec de feature está arquivada em `openspec/specs/`. Por isso esta change usa **capabilities novas de verificação** em vez de deltas MODIFIED (que quebrariam `openspec validate --strict` sem baseline). No apply (change 7, após arquivar 1–6) estes requisitos auditam as capabilities já arquivadas.

## Goals / Non-Goals

**Goals:**
- Tornar cada critério das Seções 4 e 9 do PRD um requisito SHALL com cenário testável.
- Centralizar a config de segurança HTTP (headers, CORS, rate limiting) numa camada compartilhada do backend.
- Cobrir IDOR com testes de isolamento cross-tenant em todo endpoint por ID.
- Garantir os três fallbacks de UI e a ausência de estado silencioso por auditoria + teste.
- Verificar idempotência (criação e avanço) e append-only do histórico por teste de regressão.
- Entregar os artefatos LGPD (política, termos com cláusula controlador-operador, e-mail de privacidade, RoPA) e o plano de incidentes.

**Non-Goals:**
- Nenhuma feature nova de produto e nenhuma UI nova além das páginas estáticas legais (política/termos).
- Não reimplementar auth, orders ou menu — apenas endurecer e testar o que existe.
- Sem WAF, sem IDS, sem SIEM, sem secret manager externo (deploy-vps trata segredos de ambiente).
- Rate limiting distribuído/Redis fora de escopo (dev solo, instância única — limiter em memória basta; Regra 3 do PRD: sem otimização prematura).

## Decisions

**1. Camada de segurança HTTP única via `SecurityFilterChain` (Spring Security 7).**
Headers, CORS e as regras de auth num único ponto de configuração, em vez de espalhar por controllers/filtros. Alternativa considerada: configurar headers no Caddy — rejeitada porque acopla política de segurança da app à infra e dificulta teste automatizado; HSTS/TLS ficam no Caddy (borda), CSP/frame/referrer/content-type ficam na app (onde são testáveis). CSP básica (sem `unsafe-inline` além do necessário para a SPA) — endurecer é iterativo.

**2. Rate limiting em memória por IP+rota nos endpoints de auth.**
Bucket em memória (ex.: token-bucket) no filtro de auth, chaveado por IP e rota (login/refresh). Instância única no VPS torna estado em memória suficiente. Alternativa: Redis/bucket distribuído — rejeitada por Regra 3 (sem otimização prematura); reavaliar quando houver múltiplos nós. Resposta 429 genérica, sem revelar validade de credencial (cruza com não-enumeração).

**3. IDOR tratado por teste, não por novo código.**
O isolamento já é responsabilidade do mecanismo de tenant do foundations (toda query filtra por `tenant_id` do contexto). Esta change adiciona uma **suíte de testes de isolamento cross-tenant** parametrizada por endpoint que acessa recurso por ID: cria recurso no tenant A, tenta acessá-lo autenticado como tenant B, espera negação (404/403). Onde a auditoria achar um endpoint que confia em ID do request, corrige para derivar tenant do contexto. Regra: tenant **sempre** do contexto de segurança, nunca do corpo/path.

**4. Não-enumeração = resposta genérica + timing constante.**
Login com e-mail inexistente e com senha errada retornam resposta idêntica. Para não vazar por timing, executar o hash de senha (BCrypt/Argon2) mesmo quando o e-mail não existe (dummy hash), evitando o atalho que revelaria inexistência pela resposta rápida. Alternativa: só padronizar o corpo — insuficiente, timing vazaria.

**5. Validação de magic bytes no serviço de upload.**
Ler os primeiros bytes do arquivo e conferir contra as assinaturas dos tipos de imagem permitidos (JPEG/PNG/WebP), rejeitando quando o conteúdo real diverge, independentemente de extensão/Content-Type. Fica na fatia `menu` (único ponto de upload no MVP). Cruza com o fallback de upload: rejeição por magic bytes é uma falha de upload tratada (produto salvo sem foto + retry).

**6. Log seguro por mascaramento na origem + revisão.**
Nunca logar objetos crus de request de auth/pedido. Campos sensíveis (senha, token, telefone) mascarados/omitidos onde entram no log; teste que executa os fluxos e afirma que a saída de log não contém esses valores. Alternativa: filtro global de log por regex — frágil; preferir não colocar o dado no log na origem.

**7. Auditoria de estado silencioso como checklist + testes de UI.**
Catalogar cada superfície de falha (storefront: carregar cardápio, adicionar item, checkout, handoff; painel: login, listar, avançar, cancelar, toggle, polling) e garantir para cada uma mensagem + recuperação. Os três fallbacks (WhatsApp/upload/polling) já são especificados nas changes de feature; aqui são reconferidos e testados de ponta a ponta.

**8. Entregáveis LGPD/incidentes como parte do "pronto".**
Política e termos como páginas estáticas na SPA (rota pública, linkadas no footer do storefront). E-mail de privacidade provisionado (depende de SMTP do deploy-vps para monitorar, mas o endereço e o processo são definidos aqui). RoPA e plano de incidentes como documentos versionados no repositório (ex.: `docs/`).

## Risks / Trade-offs

- **CSP quebrar a SPA (assets inline do Vite/shadcn)** → começar com política que permita o necessário para a SPA carregar e endurecer iterativamente; validar em storefront e painel antes de considerar pronto.
- **Rate limiting em memória zera no restart / não cobre múltiplos nós** → aceitável no MVP (instância única, Regra 3); documentado como limitação; migrar para store compartilhado só quando escalar horizontalmente.
- **Timing attack residual na não-enumeração** → mitigado pelo dummy-hash; não se busca timing perfeitamente constante (fora do modelo de ameaça do MVP), apenas remover o vazamento grosseiro do atalho de e-mail inexistente.
- **Cobertura de IDOR incompleta se um endpoint novo escapar da suíte** → a suíte é parametrizada/enumerada a partir do inventário de endpoints por ID; incluir no checklist de auditoria a conferência de que todo endpoint por ID está na suíte.
- **Append-only por convenção, não por constraint física** → o PRD trata como convenção; risco de update/delete acidental mitigado por revisão de código e teste que afirma imutabilidade; hardening físico (revogar UPDATE/DELETE no grant, ou trigger) fica como opção futura, fora do MVP.
- **Entregáveis legais (texto de política/termos) exigem revisão de conteúdo** → risco não-técnico; o requisito garante existência e cláusulas mínimas; a redação jurídica é responsabilidade do dev solo/consultoria, rastreada no checklist.

## Migration Plan

Sem migration de banco. Passos de deploy:
1. Adicionar/ajustar `SecurityFilterChain`: headers, CORS restrito, rate limiter de auth.
2. Ajustar login para dummy-hash (não-enumeração) e validar mensagens genéricas.
3. Adicionar validação de magic bytes no upload; garantir fallback de upload.
4. Revisar logging dos fluxos sensíveis (mascarar senha/token/telefone).
5. Adicionar páginas estáticas de política e termos; link no footer do storefront.
6. Escrever a suíte de testes: IDOR por endpoint, rate limiting, não-enumeração, magic bytes, idempotência (criação/avanço), append-only, ausência de dado sensível em log.
7. Produzir docs: RoPA e plano de resposta a incidentes; provisionar e-mail de privacidade.

Rollback: mudanças de segurança HTTP são config-driven e reversíveis por deploy anterior; nenhuma alteração destrutiva de dados. Se a CSP quebrar a SPA em produção, afrouxar a diretiva problemática e redeployar.

## Open Questions

- Valores exatos do rate limit (tentativas/janela) para login e refresh — definir a partir de uma folga que não atrapalhe usuário legítimo em rede compartilhada.
- Diretivas finais da CSP após validar os assets reais da SPA buildada.
- Endereço definitivo do e-mail de privacidade e ferramenta de monitoramento (depende do SMTP provisionado em deploy-vps).
