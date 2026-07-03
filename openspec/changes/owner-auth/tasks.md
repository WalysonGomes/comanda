## 1. Backend — fatia vertical `auth` e persistência

- [ ] 1.1 Criar o pacote `auth` (fatia vertical) sobre o pacote-base de `foundations`; definir `User` (entidade sobre a tabela `users`) e `UserRepository` com busca por e-mail
- [ ] 1.2 Confirmar que as tabelas `tenants` e `users` de `foundations` cobrem o cadastro; se a rotação de refresh exigir estado persistido, criar migration Flyway **nova** (nunca editar `V1`)
- [ ] 1.3 Implementar serviço de hash de senha (bcrypt/argon2) — nunca persistir senha em texto claro

## 2. Backend — cadastro self-service

- [ ] 2.1 Implementar serviço de cadastro **transacional**: cria `tenant` + `user` OWNER + dados mínimos do negócio (nome do negócio, WhatsApp) em uma única transação; falha parcial faz rollback total
- [ ] 2.2 Normalizar subdomínio (minúsculas, trim, slug) e checar disponibilidade antes; tratar violação de `unique(subdomain)` como erro "endereço já em uso"
- [ ] 2.3 Rejeitar e-mail já cadastrado com erro claro, sem criar registro
- [ ] 2.4 Expor `POST` de signup que, no sucesso, já inicia sessão autenticada (emite tokens)

## 3. Backend — login e sessão JWT

- [ ] 3.1 Implementar serviço de emissão de JWT: access token curto (15–30 min) + claim de tenant no formato consumido por `multi-tenancy`
- [ ] 3.2 Implementar refresh token longo (7–30 dias) em cookie `httpOnly` + `Secure` + `SameSite`, com **rotação** a cada uso e detecção de reuso (refresh anterior rejeitado)
- [ ] 3.3 Implementar `POST` de login: comparação de hash **sempre** (hash dummy quando o e-mail não existe); mesma mensagem/código genérico para e-mail inexistente, senha errada e conta inativa (anti-enumeração)
- [ ] 3.4 Implementar `POST` de refresh: valida cookie, rotaciona, emite novo access + novo refresh; rejeita refresh já rotacionado
- [ ] 3.5 Garantir que senha e token **nunca** apareçam em logs (cadastro, login, refresh)

## 4. Backend — Spring Security (Spring Boot 4.1 / Spring 7)

- [ ] 4.1 Configurar `SecurityFilterChain` (bean) separando rotas públicas (signup, login, refresh, storefront) das autenticadas do painel (`/api/**`)
- [ ] 4.2 Filtro de autenticação JWT que valida o access token e popula o `TenantContext` via o resolvedor JWT de `foundations`; rota autenticada sem token válido não segue para acesso a dados
- [ ] 4.3 Deixar ponto de extensão claro no fluxo de login para rate limiting (implementado em `reliability-and-security`)

## 5. Frontend — telas e guarda de sessão

- [ ] 5.1 Reproduzir a tela de LOGIN (bloco LOGIN do design) em React/TSX com tokens e animações do `design-system`; erro genérico de credencial inválida; link "Esqueceu a senha?" inerte/oculto (fora do MVP)
- [ ] 5.2 Reproduzir a tela de cadastro (bloco "Crie sua conta" / `ob1`): nome, nome do negócio, subdomínio com preview e `.${APP_DOMAIN}`, WhatsApp, e-mail, senha; **comunicar o limite do plano Gratuito (até 30 pedidos/mês) antes de concluir**
- [ ] 5.3 Store do access token **apenas em memória** (nunca `localStorage`/`sessionStorage`); refresh silencioso via cookie no boot para restaurar sessão
- [ ] 5.4 Guarda de rota do shell autenticado do painel (vazio nesta change): sem sessão válida, redireciona para o login
- [ ] 5.5 Tratar erros de cadastro na UI: subdomínio em uso, e-mail já cadastrado (mensagens claras, sem estado silencioso)

## 6. Testes e validação

- [ ] 6.1 Teste: cadastro atômico (falha parcial não persiste nada); subdomínio duplicado rejeitado; e-mail duplicado rejeitado
- [ ] 6.2 Teste: login válido emite tokens; e-mail inexistente, senha errada e conta inativa retornam resposta indistinguível
- [ ] 6.3 Teste: rotação de refresh emite novo par e invalida o anterior; reuso de refresh rotacionado é rejeitado
- [ ] 6.4 Teste: access token carrega o claim de tenant e resolve para o tenant correto, isolando do tenant B (IDOR mínimo)
- [ ] 6.5 Rodar `openspec validate owner-auth --strict` e corrigir o que apontar
