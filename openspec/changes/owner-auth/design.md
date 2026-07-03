## Context

`owner-auth` é o portão de entrada do painel. Depende de `foundations`, que já entregou: isolamento por `tenant_id`, resolução de tenant por JWT no contexto autenticado (mas sem emitir o JWT), tabelas `tenants`/`users`, runtime (Virtual Threads, timezone `America/Fortaleza`) e o tema do frontend. Esta change fecha a lacuna: cria tenants e usuários OWNER, autentica, e emite o JWT cujo claim de tenant a capability `multi-tenancy` já sabe consumir.

Stack fixada (PRD 7.1): Spring Boot 4.1.0 / Java 21 / Spring Framework 7 / Jakarta EE 11 / Hibernate 7.x, PostgreSQL 17, Spring Security + JWT. Restrição crítica: usar APIs de Spring Boot 4.1 / Spring 7 — material de Spring Boot 3.x diverge (imports e config de Security mudam). Frontend React + Vite (TS), telas derivadas dos blocos LOGIN e "Crie sua conta" de `.design/Comanda Painel.dc.html`.

## Goals / Non-Goals

**Goals:**
- Cadastro self-service atômico: tenant + OWNER + dados mínimos do negócio + subdomínio único, em uma transação.
- Login e-mail/senha resistente a enumeração de usuários.
- Sessão JWT stateless: access token curto em memória, refresh token em cookie `httpOnly` com rotação.
- Emitir o claim de tenant no formato que `multi-tenancy` consome.
- Telas de cadastro e login + shell autenticado (vazio) com guarda de rota.

**Non-Goals:**
- Onboarding wizard, segmento de negócio e cardápio de demonstração (→ `plans-and-onboarding`).
- Enforcement em runtime dos limites de plano (só a **comunicação** do limite entra aqui) (→ `plans-and-onboarding`).
- Rate limiting de autenticação e headers de segurança (→ `reliability-and-security`, hardening transversal).
- Recuperação de senha por e-mail (o PRD não a exige no MVP).
- Qualquer CRUD de cardápio ou conteúdo do painel.

## Decisions

### D1 — Cadastro em uma transação única
Signup cria `tenant`, `user` OWNER e dados mínimos do negócio dentro de uma única transação de banco. Falha em qualquer passo faz rollback total.
- **Por quê:** meta de "cardápio no ar em <10 min" começa aqui; um estado parcial (tenant sem OWNER, ou OWNER sem tenant) seria irreparável pelo próprio usuário. PRD Regra 11 (nenhum estado silencioso).
- **Alternativa considerada:** criar tenant e depois usuário em chamadas separadas — rejeitado: janela de inconsistência e complexidade de compensação.

### D2 — Unicidade de subdomínio no banco + normalização
`subdomain` é `unique` no banco (constraint de `foundations`). O código normaliza (minúsculas, trim, slug) e checa disponibilidade antes; a constraint é a garantia final contra corrida entre dois cadastros concorrentes.
- **Por quê:** PRD Regra 8 (constraints no banco, não só no código). A checagem em código dá boa UX; a constraint fecha a corrida.
- **Como tratar a corrida:** violação de unique na transação → traduzir para o mesmo erro "endereço já em uso" do caminho de checagem prévia.

### D3 — Login resistente a enumeração
Falha de login (e-mail inexistente, senha errada, conta inativa) retorna sempre a mesma mensagem e o mesmo código genérico. Para não vazar existência por timing, sempre executar a comparação de hash — quando o usuário não existe, comparar contra um hash dummy de custo equivalente.
- **Por quê:** PRD Seção 9 e critério de lançamento "enumeração de usuários não possível via login".
- **Alternativa considerada:** retornar 404 para e-mail inexistente e 401 para senha errada — rejeitado: vaza quais e-mails existem.

### D4 — JWT: access em memória, refresh em cookie httpOnly com rotação
Access token curto (15–30 min) devolvido no corpo da resposta; o cliente o mantém **em memória** (nunca `localStorage`/`sessionStorage`). Refresh token longo (7–30 dias) em cookie `httpOnly` (mais `Secure` e `SameSite`), rotacionado a cada uso: cada refresh emite um novo par e invalida o refresh anterior; reapresentar um refresh já rotacionado é rejeitado (detecção de reuso).
- **Por quê:** PRD 7.1 — stateless, sem JWT em `localStorage` (mitiga XSS). Rotação limita a janela de um refresh token vazado.
- **Estado da rotação:** a invalidação do refresh anterior exige rastrear o refresh vigente por sessão. Preferir um identificador de refresh (jti/família) verificável; se exigir coluna nova (ex.: em `users` ou tabela de sessão leve), adicionar via migration Flyway nova — o schema base de `foundations` não previu isso explicitamente. Decisão de armazenamento detalhada na implementação (ver Open Questions).
- **Alternativa considerada:** sessão server-side com cookie opaco — rejeitado: PRD pede JWT stateless.

### D5 — Claim de tenant no access token
O access token carrega o identificador do tenant como claim, no formato que o resolvedor JWT de `multi-tenancy` (de `foundations`) já lê para popular o `TenantContext`. Esta change é a dona do formato do claim; `foundations` deixou só o ponto de injeção.
- **Por quê:** fecha o contrato aberto em `foundations` (Open Question "formato do claim de tenant no JWT — detalhado em owner-auth").

### D6 — Spring Security para as rotas do painel
Configurar o filtro de autenticação JWT para as rotas autenticadas (`/api/**` do painel), deixando públicas as rotas de signup/login/refresh e as rotas do storefront. Usar a config de Security do Spring Boot 4.1 / Spring 7 (SecurityFilterChain como bean), atento a divergências de API do 3.x.
- **Por quê:** separa contexto público (storefront, resolvido por subdomínio) do autenticado (painel, resolvido por JWT), como `foundations` definiu.

### D7 — Frontend: telas do design + guarda de rota
Reproduzir os blocos LOGIN e "Crie sua conta" (`ob1`) do `.design/Comanda Painel.dc.html` com tokens/animações do `design-system`. Access token em memória (store leve, ex.: contexto/estado global); no boot, tentar refresh silencioso via cookie para restaurar sessão. Guarda de rota redireciona ao login sem sessão. O link "Esqueceu a senha?" fica inerte/oculto (fora do MVP).
- **Nota de escopo do design:** o formulário "Crie sua conta" do protótipo vive dentro do fluxo de onboarding; aqui reaproveitamos só a criação de conta (tenant + OWNER). O wizard (segmento, horário, primeiro produto, "pronto!") é de `plans-and-onboarding`.

## Risks / Trade-offs

- **Corrida de subdomínio entre dois cadastros simultâneos** → constraint `unique` no banco é a fonte da verdade; violação vira o erro "endereço já em uso". (D2)
- **Enumeração de usuários por timing** → sempre comparar hash, com hash dummy quando o usuário não existe; mesma resposta/código para toda falha. (D3)
- **Refresh token vazado** → cookie `httpOnly`+`Secure`+`SameSite`, rotação com detecção de reuso invalida a família ao detectar reapresentação. (D4)
- **API de Spring Security 3.x aplicada por engano no 4.1/Spring 7** → seguir docs do Spring Boot 4.1; validar imports Jakarta EE 11 e a montagem do `SecurityFilterChain`.
- **Rotação de refresh pode exigir coluna/tabela nova não prevista em `foundations`** → adicionar via migration Flyway nova, sem alterar `V1`. (D4)
- **Rate limiting ausente nesta change** → aceito: hardening transversal de autenticação vive em `reliability-and-security`; deixar o ponto de extensão claro no filtro de login.

## Migration Plan

1. Backend: pacote `auth` (fatia vertical) — `AuthController` (signup, login, refresh), serviço de emissão/validação de JWT, `UserRepository`, serviço de cadastro transacional. Config de Spring Security (SecurityFilterChain) separando rotas públicas de autenticadas.
2. Se a rotação de refresh exigir estado persistido, criar migration Flyway nova (nunca editar `V1`).
3. Frontend: telas de cadastro e login (blocos do design), store do access token em memória, refresh silencioso no boot, guarda de rota do shell do painel.
4. Testes: cadastro atômico (rollback em falha parcial), subdomínio duplicado, e-mail duplicado, login válido/ inválido/ inativo indistinguíveis, rotação de refresh e rejeição de reuso, claim de tenant resolve para o tenant correto e isola do outro (IDOR mínimo reaproveitando o teste de `foundations`).
- **Rollback:** sem dados de produção ainda; rollback = reverter branch. Migration nova (se houver) só roda em banco que ainda não a aplicou.

## Open Questions

- **Armazenamento do estado de rotação do refresh:** coluna em `users`, tabela de sessão leve, ou jti/família assinado sem estado? Resolver na implementação, preferindo o mínimo que permita detecção de reuso.
- **TTLs exatos:** access 15 ou 30 min; refresh 7 ou 30 dias — escolher dentro das faixas do PRD na configuração por ambiente.
- **`is_active` no cadastro:** conta nasce ativa no MVP (sem verificação de e-mail, já que recuperação/verificação por e-mail está fora) — confirmar na implementação.
