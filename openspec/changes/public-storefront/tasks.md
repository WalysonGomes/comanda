## 1. Backend — fatia vertical `storefront` (base pública)

- [ ] 1.1 Criar o pacote `storefront` (fatia vertical) seguindo o padrão de `foundations`, para endpoints públicos (sem JWT)
- [ ] 1.2 Aplicar a resolução de tenant por subdomínio (padrão `multi-tenancy` de `foundations`) ao contexto público; subdomínio inexistente → 404
- [ ] 1.3 Garantir que todo endpoint público filtra pelo `tenant_id` resolvido do subdomínio e nunca por identificador enviado no request
- [ ] 1.4 Definir um utilitário de "agora" no timezone `America/Fortaleza` (dia da semana `0=Dom…6=Sáb` e hora atual) compartilhado pelos cálculos de disponibilidade e abertura

## 2. Backend — cardápio público

- [ ] 2.1 Endpoint público de dados do negócio: nome, logo, `whatsapp_number`, `delivery_fee`, `min_order_value` e status aberto/fechado + rótulo de horário
- [ ] 2.2 Endpoint público de cardápio: categorias e produtos disponíveis, agrupados por categoria e ordenados por `position`
- [ ] 2.3 Filtro de disponibilidade na query: `is_available = true` E (`available_days` nulo/vazio OU contém o dia atual em `America/Fortaleza`); produto indisponível não é retornado
- [ ] 2.4 Omitir categorias sem nenhum produto disponível
- [ ] 2.5 Incluir, por produto, os grupos e itens de adicionais (nome, `required`, `selection_type`, `min/max`, itens disponíveis com preço adicional)
- [ ] 2.6 Cálculo de aberto/fechado no servidor a partir de `tenants.is_open` e `business_hours` do dia (fuso fixo), com rótulo de quando reabre

## 3. Backend — validação de disponibilidade na finalização

- [ ] 3.1 Endpoint público de validação de carrinho: recebe linhas (produto + adicionais + quantidade) e retorna itens indisponíveis por item e os preços autoritativos atuais
- [ ] 3.2 Validar cada produto/adicional contra o tenant do subdomínio (id de outro tenant → tratado como inexistente/indisponível)
- [ ] 3.3 Recalcular subtotal, taxa e total com os preços do servidor para a tela de finalização

## 4. Backend — contrato de criação de pedido (fronteira com order-operation)

- [ ] 4.1 Documentar no proposal/design o contrato do endpoint de criação idempotente de pedido (request público, `idempotency_key` do cliente, dados do cliente, `delivery_type`, `address` se Entrega, `notes`, linhas; sem `tenant_id` no corpo)
- [ ] 4.2 Marcar explicitamente que a implementação (persistência idempotente, snapshots, `order_status_history`) é de `order-operation`; storefront apenas consome o contrato
- [ ] 4.3 Alinhar a resposta esperada (id curto + total) usada pela tela de confirmação/handoff

## 5. Backend — testes

- [ ] 5.1 Teste de isolamento: cardápio de um tenant não retorna produtos de outro; id de produto forjado de outro tenant é inacessível
- [ ] 5.2 Teste de disponibilidade: produto fora do `available_days` do dia e produto `is_available=false` não aparecem; borda de virada de dia no fuso `America/Fortaleza`
- [ ] 5.3 Teste de aberto/fechado por `business_hours` + toggle `is_open`
- [ ] 5.4 Teste de validação de finalização: item que ficou indisponível é sinalizado; carrinho válido retorna preços atuais
- [ ] 5.5 Garantir que telefone do cliente não aparece em logs (Regra 14)

## 6. Frontend — infraestrutura do storefront

- [ ] 6.1 Rotas públicas do storefront separadas do painel, no mesmo bundle (padrão de roteamento de `foundations`); resolução do slug do subdomínio no cliente
- [ ] 6.2 Camada de acesso à API pública (TanStack Query) com estados de loading/erro e skeleton loaders (tokens `.sk`)
- [ ] 6.3 Store do carrinho (Zustand) persistido em `localStorage` por slug de subdomínio, com `lineId` estável por linha
- [ ] 6.4 Utilitário de montagem da mensagem de WhatsApp no formato exato da Seção 3.1 (única superfície com emojis)

## 7. Frontend — home do cardápio (bloco STOREFRONT)

- [ ] 7.1 Cabeçalho do negócio (capa, iniciais/logo, nome), status aberto/fechado com cor + rótulo textual e linha de taxa/retirada/mínimo
- [ ] 7.2 Chips de categoria sticky (scroll horizontal) e lista de produtos por categoria em coluna única
- [ ] 7.3 Cartão de produto (foto/placeholder, nome, descrição truncada, preço, indicador "no carrinho") abrindo o product sheet
- [ ] 7.4 Barra flutuante de carrinho (contagem + total) quando há itens
- [ ] 7.5 Estados de loading (skeleton) e vazio, sem emoji, ícones Lucide

## 8. Frontend — product sheet (bloco PRODUCT SHEET)

- [ ] 8.1 Bottom sheet com foto, nome, preço, descrição e grupos de adicionais (radio para SINGLE, checkbox com limite para MULTIPLE)
- [ ] 8.2 Bloqueio de "Adicionar" com motivo visível enquanto grupos obrigatórios não estiverem válidos (respeitando min/max)
- [ ] 8.3 Seletor de quantidade e campo de observação
- [ ] 8.4 Adicionar ao carrinho e reabrir em modo edição (pré-carregar seleções, atualizar a mesma linha)

## 9. Frontend — carrinho (bloco CART SHEET)

- [ ] 9.1 Lista de linhas com detalhe de adicionais/observação, ajuste de quantidade e remoção
- [ ] 9.2 Ação "Editar" que reabre o product sheet da linha sem duplicar
- [ ] 9.3 Totais discriminados (subtotal, taxa, total) sempre visíveis
- [ ] 9.4 Bloqueio de pedido mínimo mostrando "quanto falta"; botão de avançar habilitado só quando válido

## 10. Frontend — checkout (bloco CHECKOUT)

- [ ] 10.1 Tela única: seleção Entrega/Retirada, nome, telefone; endereço só quando Entrega
- [ ] 10.2 Validação inline em tempo real (borda + mensagem por campo); telefone com DDD
- [ ] 10.3 Chamar a validação de disponibilidade antes de confirmar; bloco de erro por item indisponível com ação "Remover item indisponível"
- [ ] 10.4 Total refletindo os preços autoritativos do servidor; botão "Finalizar" habilitado só com formulário válido + loja aberta + carrinho válido

## 11. Frontend — envio e handoff (bloco SENT/HANDOFF)

- [ ] 11.1 Confirmação "Pedido enviado" exibida antes de qualquer tentativa de abrir o WhatsApp
- [ ] 11.2 Enviar o pedido ao endpoint de criação idempotente (contrato de `order-operation`) com `idempotency_key` gerada na tentativa; botão de finalizar com loading que previne duplo toque
- [ ] 11.3 Handoff via `wa.me/<numero>?text=<mensagem>` com a mensagem no formato do PRD
- [ ] 11.4 Fallback de primeira classe: mensagem sempre visível na tela + botão "Copiar mensagem" com confirmação de cópia
- [ ] 11.5 Ação "Fazer novo pedido" que limpa o carrinho

## 12. Frontend — estado de loja fechada

- [ ] 12.1 Aviso flutuante de "fechado agora · abre às HH" permitindo navegar e montar o carrinho
- [ ] 12.2 Bloquear a finalização quando fechado, com o motivo visível, sem estado silencioso

## 13. Validação final

- [ ] 13.1 Rodar `openspec validate public-storefront --strict` e corrigir apontamentos
- [ ] 13.2 Conferir critérios do PRD Seção 9 (isolamento por tenant nos endpoints públicos, sem dado sensível em log) e Seção 4 (nenhum estado silencioso, fallback de WhatsApp)
- [ ] 13.3 Conferir que nenhuma persistência de pedido / `order_status_history` foi implementada aqui (fronteira com `order-operation`) e que o contrato do pedido está descrito
- [ ] 13.4 Conferir ausência de emoji na UI (emoji só na mensagem de WhatsApp) e uso exclusivo de ícones Lucide
