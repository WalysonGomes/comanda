## 1. Inventário e auditoria (pré-requisito)

- [ ] 1.1 Levantar o inventário de todos os endpoints que acessam recurso por ID (pedido, produto, categoria, adicional, upload, subscription/admin) — base para os testes de IDOR.
- [ ] 1.2 Catalogar todas as superfícies de falha da UI (storefront: carregar cardápio, adicionar item, checkout, handoff; painel: login, listar, detalhe, avançar, cancelar, toggle, polling) e marcar quais já têm mensagem + recuperação.
- [ ] 1.3 Auditar pontos de logging dos fluxos sensíveis (login, refresh, criação de pedido) e marcar onde senha/token/telefone poderiam vazar.
- [ ] 1.4 Confirmar que os três fallbacks (WhatsApp, upload, polling) estão implementados pelas changes de feature e identificar lacunas.

## 2. Segurança HTTP compartilhada (backend)

- [ ] 2.1 Configurar `SecurityFilterChain` (Spring Security 7) com headers de segurança: HSTS, X-Content-Type-Options, proteção contra clickjacking (frame-ancestors/X-Frame-Options), Referrer-Policy e CSP básica.
- [ ] 2.2 Validar a CSP contra a SPA buildada (storefront e painel) e ajustar diretivas até carregar sem quebra.
- [ ] 2.3 Configurar CORS restrito às origens conhecidas (domínio da app + subdomínios de tenant); rejeitar origens não autorizadas.
- [ ] 2.4 Implementar rate limiting em memória (token-bucket por IP+rota) nos endpoints de login e refresh; resposta 429 genérica.

## 3. Autenticação — não-enumeração (backend)

- [ ] 3.1 Padronizar resposta de falha de login para mensagem genérica de credencial inválida (e-mail inexistente e senha errada respondem igual).
- [ ] 3.2 Executar hash de senha (dummy hash) quando o e-mail não existe, para eliminar vazamento por timing.
- [ ] 3.3 Teste: e-mail inexistente vs. e-mail existente com senha errada produzem resposta idêntica (corpo/status) sem diferença grosseira de timing.

## 4. IDOR — isolamento por tenant (backend + testes)

- [ ] 4.1 Revisar cada endpoint do inventário (1.1) garantindo que o `tenant_id` é derivado do contexto de segurança (JWT no painel, subdomínio no público), nunca do corpo/path.
- [ ] 4.2 Corrigir qualquer endpoint que confie em ID/tenant vindo do request.
- [ ] 4.3 Suíte de testes de isolamento cross-tenant parametrizada por endpoint: criar recurso no tenant A, acessar autenticado como tenant B, esperar 404/403.
- [ ] 4.4 Checklist: confirmar que todo endpoint por ID do inventário está coberto pela suíte.

## 5. Upload — validação de magic bytes (backend)

- [ ] 5.1 Validar magic bytes (JPEG/PNG/WebP) no serviço de upload de foto; rejeitar quando o conteúdo real diverge da extensão/Content-Type.
- [ ] 5.2 Garantir que a rejeição por magic bytes é tratada como falha de upload (produto salvo sem foto + opção de tentar novamente).
- [ ] 5.3 Teste: arquivo com extensão de imagem e conteúdo inválido é rejeitado e não persistido.

## 6. Logs sem dado sensível (backend)

- [ ] 6.1 Mascarar/omitir senha, token (access/refresh) e telefone na origem dos logs dos fluxos sensíveis.
- [ ] 6.2 Teste: executar login/refresh/criação de pedido e afirmar que a saída de log não contém senha, token nem telefone.

## 7. Confiabilidade — auditoria e verificação (frontend + testes)

- [ ] 7.1 Garantir mensagem clara + caminho de recuperação em cada superfície de falha catalogada (1.2); nenhum spinner infinito/estado ambíguo.
- [ ] 7.2 Verificar fallback de handoff do WhatsApp: `wa.me` não abre → tela de copiar mensagem (primeira classe) com confirmação de cópia.
- [ ] 7.3 Verificar fallback de upload: falha → produto sem foto + indicador + retry.
- [ ] 7.4 Verificar fallback de polling: falha → indicador de conectividade + timestamp da última atualização bem-sucedida.
- [ ] 7.5 Teste de regressão de idempotência de criação de pedido (mesma key não duplica; cross-tenant isolada).
- [ ] 7.6 Teste de regressão de idempotência de avanço de status (request duplicado não gera dois registros de histórico).
- [ ] 7.7 Verificar/testar `order_status_history` append-only (from/to/changed_by/created_at; sem update/delete).

## 8. LGPD e conformidade (conteúdo + frontend)

- [ ] 8.1 Redigir e publicar a política de privacidade (dados tratados, finalidade, base legal); página estática na SPA.
- [ ] 8.2 Redigir termos de serviço com cláusula controlador-operador (tenant = controlador, Comanda = operador); página estática na SPA.
- [ ] 8.3 Linkar política e termos no footer do storefront.
- [ ] 8.4 Provisionar e-mail de contato de privacidade ativo e monitorado; publicá-lo na política.
- [ ] 8.5 Criar o registro interno de atividades de tratamento (RoPA) cobrindo os tratamentos do MVP.

## 9. Resposta a incidentes (documentação)

- [ ] 9.1 Documentar o plano de resposta a incidentes: responsável, canal de emergência, critério de notificação à ANPD em até 72h para vazamento com risco real.

## 10. Fechamento

- [ ] 10.1 Rodar toda a suíte de segurança/confiabilidade e confirmar verde (IDOR, rate limiting, não-enumeração, magic bytes, logs, idempotência, append-only).
- [ ] 10.2 Conferir os critérios de lançamento das Seções 4 e 9 do PRD item a item.
- [ ] 10.3 Rodar `openspec validate reliability-and-security --strict` e corrigir o que apontar.
