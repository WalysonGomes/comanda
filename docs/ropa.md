# Registro de Operações de Tratamento de Dados Pessoais (RoPA)

> Mantido conforme LGPD art. 37. Cobre as operações de tratamento do MVP do Comanda (PRD
> Seção 3). Atualizar sempre que um novo dado pessoal passar a ser coletado ou uma nova
> finalidade surgir.

**Controlador/Operador:** para os dados do dono do negócio, o Comanda é controlador. Para os
dados do cliente final de cada tenant, o dono do negócio é controlador e o Comanda é operador
(cláusula em `/termos`, implementada em `comanda-client/src/features/legal/TermsPage.tsx`).

**Encarregado / contato de privacidade:** privacidade@comanda.app.

## 1. Cadastro do dono do negócio (signup)

- **Dados tratados:** nome, e-mail, senha (hash BCrypt, nunca texto puro), número de WhatsApp do
  negócio, nome do negócio e subdomínio.
- **Finalidade:** criar e autenticar a conta e identificar o negócio no cardápio público.
- **Base legal:** execução de contrato.
- **Categoria de titular:** dono do negócio (usuário contratante).
- **Compartilhamento:** nenhum compartilhamento com terceiros no cadastro. O provedor de e-mail
  transacional recebe o e-mail do titular como destinatário quando houver recuperação de senha.
- **Prazo de retenção:** enquanto a conta estiver ativa; solicitações de exclusão seguem a Seção 5.
- **Local de armazenamento:** PostgreSQL no ambiente de hospedagem da aplicação; a topologia de
  produção e sua política operacional de backup ainda dependem da infraestrutura definitiva.
- **Medidas de segurança:** hash de senha, JWT em memória/cookie httpOnly, isolamento por
  `tenant_id`, headers de segurança HTTP e CORS restrito (ver `application-security` spec).

## 2. Sessão e autenticação (login/refresh)

- **Dados tratados:** e-mail e senha na requisição (nunca persistidos em texto puro ou logados),
  token de acesso JWT em memória no cliente e token de refresh, cujo hash é armazenado e cujo
  valor bruto permanece em cookie httpOnly.
- **Finalidade:** autenticação e manutenção da sessão do dono do negócio.
- **Base legal:** execução de contrato.
- **Prazo de retenção:** o token de refresh expira em `app.jwt.refresh-ttl-days` (padrão de 14
  dias), e o hash é substituído a cada rotação.

## 3. Pedido do cliente final (storefront público)

- **Dados tratados:** nome, telefone, tipo de entrega, endereço quando houver entrega, itens do
  pedido e observações.
- **Finalidade:** registrar o pedido, exibi-lo no painel operacional e montar a mensagem enviada
  ao WhatsApp do dono do negócio.
- **Base legal:** execução de contrato entre o cliente final e o dono do negócio; o Comanda
  processa como operador em nome do dono do negócio, que é o controlador.
- **Categoria de titular:** cliente final, sem cadastro ou login.
- **Compartilhamento:** o texto do pedido é entregue ao WhatsApp do dono via `wa.me`; o handoff
  parte do dispositivo do cliente, não do backend do Comanda.
- **Prazo de retenção:** os dados permanecem em `orders`/`customers` enquanto a conta do tenant
  existir. A visibilidade no painel é limitada pelo plano (7 ou 30 dias, PRD Seção 6), mas esse
  filtro não representa exclusão automática.
- **Medidas de segurança:** isolamento por `tenant_id` e `idempotency_key` única por tenant.

## 4. Rastreabilidade de status do pedido (`order_status_history`)

- **Dados tratados:** transições de status do pedido e identificador do usuário que executou a
  transição, ou `SYSTEM`.
- **Finalidade:** auditoria mínima e confiável da operação (PRD Seção 4.4).
- **Base legal:** execução de contrato e cumprimento da rastreabilidade assumida com o dono.
- **Prazo de retenção:** registro append-only, mantido enquanto o pedido existir.

## 5. Direitos dos titulares

Solicitações de acesso, correção ou exclusão devem ser direcionadas a
privacidade@comanda.app. Para dados de cliente final, o Comanda encaminha a solicitação ao dono
do negócio (controlador), quando aplicável, por ser quem mantém a relação direta com o titular.

## 6. Revisão

Este documento deve ser revisado a cada feature que introduza novo dado pessoal ou nova
finalidade de tratamento.
