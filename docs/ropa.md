# Registro de Operações de Tratamento de Dados Pessoais (RoPA)

> Mantido conforme LGPD art. 37. Cobre as operações de tratamento do MVP do Comanda (PRD Seção 3).
> Atualizar sempre que um novo dado pessoal passar a ser coletado ou uma nova finalidade surgir.

**Controlador/Operador:** para os dados do dono do negócio, o Comanda é controlador. Para os dados
do cliente final de cada tenant, o dono do negócio é controlador e o Comanda é operador (cláusula
em `/termos`, ver `comanda-client/src/features/legal/TermsPage.tsx`).

**Encarregado / contato de privacidade:** privacidade@comanda.app (task 8.4).

---

## 1. Cadastro do dono do negócio (signup)

- **Dados tratados:** nome, e-mail, senha (hash BCrypt, nunca texto puro), número de WhatsApp do
  negócio, nome do negócio, subdomínio.
- **Finalidade:** criar e autenticar a conta; identificar o negócio no cardápio público.
- **Base legal:** execução de contrato.
- **Categoria de titular:** dono do negócio (usuário contratante).
- **Compartilhamento:** nenhum compartilhamento com terceiros. Provedor de e-mail transacional
  (recuperação de senha, Seção 7.3 do PRD) recebe o e-mail do titular como destinatário da
  mensagem.
- **Prazo de retenção:** enquanto a conta estiver ativa; ver Seção 5 (exclusão).
- **Local de armazenamento:** PostgreSQL no VPS único (PRD 7.3), sem replicação externa além do
  backup.
- **Medidas de segurança:** hash de senha, JWT em memória/cookie httpOnly, isolamento por
  `tenant_id`, headers de segurança HTTP, CORS restrito (ver `application-security` spec).

## 2. Sessão e autenticação (login/refresh)

- **Dados tratados:** e-mail e senha (na requisição, nunca persistidos em texto puro ou logados —
  ver `LogSafetyTest`), token de acesso (JWT, em memória no cliente) e token de refresh (hash
  armazenado, valor bruto em cookie httpOnly).
- **Finalidade:** autenticação e manutenção de sessão do dono do negócio.
- **Base legal:** execução de contrato.
- **Prazo de retenção:** token de refresh expira em `app.jwt.refresh-ttl-days` (padrão 14 dias);
  hash é substituído a cada rotação.

## 3. Pedido do cliente final (storefront público)

- **Dados tratados:** nome, telefone, tipo de entrega e endereço (quando entrega), itens do
  pedido, observações.
- **Finalidade:** montar a mensagem de pedido enviada ao WhatsApp do dono do negócio; exibir o
  pedido no painel de operação.
- **Base legal:** execução de contrato (entre o cliente final e o dono do negócio) — o Comanda
  processa como operador em nome do dono do negócio (controlador).
- **Categoria de titular:** cliente final (não cadastrado, sem login).
- **Compartilhamento:** o texto do pedido é enviado ao WhatsApp do dono via `wa.me` — esse envio
  acontece no dispositivo do próprio cliente, não pelo backend do Comanda.
- **Prazo de retenção:** dado permanece na tabela `orders`/`customers` enquanto a conta do tenant
  existir; a *visibilidade* no painel é limitada por plano (7 ou 30 dias, PRD Seção 6), mas isso é
  um filtro de exibição, não uma exclusão automática dos dados.
- **Medidas de segurança:** isolamento por `tenant_id` (testado por IDOR — ver
  `MenuManagementFlowTest`, `OrderOperationFlowTest`), `idempotency_key` único por tenant.

## 4. Rastreabilidade de status do pedido (`order_status_history`)

- **Dados tratados:** transições de status do pedido, id do usuário (dono) que executou a
  transição ou `SYSTEM`.
- **Finalidade:** auditoria mínima e confiável da operação (PRD 4.4).
- **Base legal:** execução de contrato / cumprimento de obrigação de rastreabilidade assumida com
  o dono do negócio.
- **Prazo de retenção:** append-only, nunca deletado enquanto o pedido existir.

## 5. Direitos dos titulares

Solicitações de acesso, correção ou exclusão devem ser direcionadas a
privacidade@comanda.app. Para dados de cliente final, o Comanda encaminha a solicitação ao dono
do negócio (controlador) quando aplicável, por ser quem detém a relação direta com o titular.

## 6. Revisão

Este documento deve ser revisado a cada nova feature que introduza um novo dado pessoal ou uma
nova finalidade de tratamento (checklist de PR: "esta mudança adiciona um campo de dado pessoal
novo? Se sim, atualizar este RoPA").
