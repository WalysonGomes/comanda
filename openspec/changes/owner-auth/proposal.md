## Why

Para chegar à meta de produto — cardápio no ar em menos de 10 minutos — o dono precisa conseguir criar a própria conta e voltar a ela com segurança. Hoje a fundação (`foundations`) já isola dados por tenant e sabe injetar o tenant do JWT, mas ninguém cria tenants, cria o usuário OWNER nem emite o JWT. Esta change entrega o cadastro self-service e a autenticação — o portão de entrada de todo o painel — sem o qual nenhuma feature autenticada (cardápio, pedidos, planos) tem por onde começar.

## What Changes

- **Cadastro self-service do dono** em uma única operação atômica: cria o `tenant` (com subdomínio gerado e validado como único), o `user` OWNER e os dados iniciais mínimos do negócio (nome, WhatsApp). É o começo do fluxo de "cardápio no ar em <10 min".
- **Comunicação do limite do plano Gratuito ANTES da criação da conta** (PRD Seção 6): o usuário vê "até 30 pedidos/mês" antes de concluir o cadastro.
- **Login com e-mail e senha**, com hash forte de senha e mensagem de erro genérica que **não permite enumeração de usuários**.
- **Sessão JWT stateless**: access token curto (15–30 min) devolvido ao cliente para manter em memória; refresh token (7–30 dias) em cookie `httpOnly` com **rotação** a cada uso. Nunca em `localStorage`.
- **Emissão do JWT com o claim de tenant** que a capability `multi-tenancy` (de `foundations`) já consome para resolver o tenant no contexto autenticado — esta change define o formato e a emissão desse claim.
- **Telas do frontend**: cadastro (formulário de conta), login, e o **shell autenticado do painel** (vazio nesta change — só a casca protegida por sessão).
- Cenários de erro como requisitos de primeira classe: credencial inválida, subdomínio já em uso, e-mail já cadastrado.

Fora de escopo (**out**): onboarding wizard e cardápio de demonstração (ficam em `plans-and-onboarding`); qualquer CRUD de cardápio; enforcement dos limites de plano em runtime (só a **comunicação** do limite entra aqui). Recuperação de senha por e-mail: o PRD não a exige no MVP (Seção 3 não lista, Seção 13 não a cita como limitação) — fica **out**; o link "Esqueceu a senha?" do design pode ficar inerte ou oculto no MVP.

## Capabilities

### New Capabilities
- `owner-auth`: cadastro self-service que cria atomicamente tenant + usuário OWNER + subdomínio único + dados mínimos do negócio; login por e-mail/senha resistente a enumeração; sessão JWT stateless com access token curto e refresh token em cookie `httpOnly` com rotação; emissão do claim de tenant consumido por `multi-tenancy`.

### Modified Capabilities
<!-- Nenhuma. As specs de foundations ainda não estão arquivadas em openspec/specs/;
     a dependência sobre a capability multi-tenancy (ponto de injeção do JWT) é
     descrita em prosa e satisfeita pela emissão do claim definida aqui. -->

## Impact

- **Backend (novo, fatia vertical `auth`):** endpoints de signup, login e refresh; serviço de emissão/validação de JWT (access + refresh com rotação); repositório de `users`; criação transacional de `tenant` + `user` + registro inicial. Spring Security configurado para as rotas do painel. APIs compatíveis com Spring Boot 4.1 / Spring 7 / Jakarta EE 11.
- **Banco:** usa as tabelas `tenants` e `users` já criadas em `foundations` (nenhuma migration nova esperada; se faltar coluna de sessão/rotação, adicionar via nova migration Flyway).
- **Frontend (novo):** telas de cadastro e login com os tokens do design (`.design/Comanda Painel.dc.html` blocos LOGIN e "Crie sua conta"); guarda de rota que protege o shell do painel; armazenamento do access token só em memória; refresh via cookie `httpOnly`.
- **Dependência:** consome `multi-tenancy`, `database-schema`, `platform-runtime` e `design-system` de `foundations`. É pré-requisito de `menu-management`, `order-operation` e `plans-and-onboarding` (todo o painel autenticado).
- **Segurança (PRD Seção 9 / Regras 14):** senha com hash forte; senha e token nunca em logs; erro de login genérico. Rate limiting de autenticação é endurecido transversalmente em `reliability-and-security`.
