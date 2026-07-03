## Context

`menu-management` entregou o conteúdo do cardápio no painel; `foundations` estabeleceu o padrão de multi-tenancy (resolução por subdomínio no contexto público, por JWT no painel) e o design-system. Falta a superfície que o cliente final usa: a vitrine pública. É a primeira superfície do produto **sem autenticação** — o tenant é resolvido pelo **subdomínio**, e nenhum dado do tenant pode vazar por parâmetro controlado pelo cliente.

O fluxo do cliente é: cardápio → produto (sheet) → carrinho → checkout → "Pedido enviado" → WhatsApp. O carrinho vive no **navegador** (sem conta). O storefront **monta** o pedido e faz **handoff** para o WhatsApp; a **persistência** do pedido é responsabilidade de `order-operation`. Esta fronteira é o ponto de design mais sensível da change.

Restrições herdadas do PRD:
- Timezone `America/Fortaleza` hardcoded — determina "que dia é hoje" para `available_days` e "está aberto?" para `business_hours` (convenção `0=Dom … 6=Sáb`, Seção 13).
- Multi-tenancy por `tenant_id`; endpoints públicos filtram pelo tenant do subdomínio, nunca de input (Seção 7.4 / Regra 9).
- Nenhum estado silencioso de falha; fluxos de erro (fechado, mínimo, indisponível, WhatsApp não abre) são features (Regras 10–11).
- Sem emoji na UI; emoji **só** na mensagem de WhatsApp (Regra 13). Skeleton loaders, mobile-first.
- Mensagem de WhatsApp no formato **exato** da Seção 3.1.

## Goals / Non-Goals

**Goals:**
- Resolver o tenant por subdomínio e servir o cardápio público filtrado por disponibilidade do dia/estado.
- Carrinho persistente no navegador, editável (quantidade e adicionais) sem recriar itens.
- Transparência total de preço e bloqueio de pedido mínimo com "quanto falta".
- Bloquear "Adicionar" quando grupos obrigatórios não satisfeitos, com motivo visível.
- Validar disponibilidade na finalização e sinalizar por item.
- Confirmar envio **antes** do handoff; fallback de cópia como tela de primeira classe.
- Estado de loja fechada que deixa navegar/montar mas bloqueia finalizar.
- Definir o **contrato** do endpoint de criação idempotente de pedido (satisfeito por `order-operation`).

**Non-Goals:**
- Persistir o pedido, `order_status_history`, painel de operação (→ `order-operation`).
- Qualquer login/conta de cliente.
- Pagamento no app.
- Status do pedido para o cliente na página pública (Fase 3, requer SSE).

## Decisions

### 1. Resolução de tenant por subdomínio, no servidor
O storefront público é resolvido por host (`nomedonegocio.${APP_DOMAIN}`). O backend extrai o subdomínio do `Host` header (padrão de `foundations`/`multi-tenancy`) e injeta o `tenant_id` no contexto da requisição pública. **O cliente nunca envia `tenant_id`**; qualquer id de produto/adicional recebido é validado contra o tenant resolvido. Subdomínio inexistente → 404.
- *Alternativa descartada:* passar o slug do negócio como parâmetro de query — abriria vetor de acesso cross-tenant e quebraria o modelo de subdomínio do PRD.

### 2. Filtragem de disponibilidade na leitura (não no cliente)
O endpoint de cardápio público retorna **apenas** produtos que hoje estão disponíveis: `is_available = true` **e** (`available_days` é null/vazio **ou** contém o dia da semana atual em `America/Fortaleza`). Produtos indisponíveis simplesmente **não são retornados** — não são enviados como "desabilitado" (Regra da Seção 3.1 / Jornada 5.2). Categorias sem nenhum produto disponível não aparecem.
- *Por que no servidor:* evita expor produtos ocultos ao cliente e mantém a convenção de dia num único lugar (o back), reduzindo risco de divergência de timezone.
- *Alternativa descartada:* enviar tudo e filtrar no front — vaza catálogo indisponível e duplica a regra de dia da semana.

### 3. Estado "aberto/fechado" derivado de `business_hours` no servidor
"Aberto agora?" é calculado no back a partir de `tenants.is_open` (toggle manual do dono) **e** de `business_hours` do dia no fuso `America/Fortaleza`. O storefront recebe um flag `isOpen` + rótulo de horário/reabertura. Fechado **não** bloqueia navegar nem montar o carrinho; bloqueia **apenas** a finalização (botão desabilitado + aviso explicando quando reabre).
- *Alternativa descartada:* calcular no cliente a partir do relógio do dispositivo — relógio do cliente não é confiável e o fuso é fixo do negócio.

### 4. Carrinho persistente no navegador (Zustand + localStorage)
O carrinho é estado client-side em Zustand, persistido em `localStorage` por subdomínio (chave inclui o slug para não misturar carrinhos de negócios diferentes). Cada **linha** de carrinho guarda: `productId`, snapshot de nome/preço para exibição, quantidade, seleções de adicionais (single/multi por grupo), observação, e um `lineId` estável. Editar uma linha reabre o sheet do produto com as seleções pré-carregadas e **atualiza a mesma linha** (não cria outra).
- *Por que snapshot no cliente também:* o preço mostrado no carrinho não deve "pular" se o dono editar o preço no meio da sessão; a verdade de preço para o pedido é reconciliada na validação de finalização (Decisão 6).
- *Alternativa descartada:* carrinho no servidor por sessão anônima — exigiria estado server-side sem conta, contra o "sem login".

### 5. Validação de adicionais obrigatórios no cliente, com motivo visível
Ao abrir o produto, o botão "Adicionar" fica **bloqueado** enquanto algum grupo `required` não tiver seleção válida (respeitando `SINGLE`/`MULTIPLE` e `min/max`). O motivo aparece visível (ex.: "Escolha uma opção em <grupo>") até a seleção ficar válida. Grupos `SINGLE` usam radio; `MULTIPLE` usam checkbox com limite `max`.
- *Alternativa descartada:* deixar adicionar e validar só no checkout — atrito tardio, contraria a Jornada 5.2.

### 6. Validação de disponibilidade na finalização (fonte da verdade no servidor)
Antes do handoff, o storefront chama um endpoint público de **validação de carrinho**: envia os `productId`/adicionais e quantidades; o servidor confere, no tenant do subdomínio, que cada produto/adicional ainda existe e está disponível hoje, e retorna a lista de itens que ficaram **indisponíveis** (por item) além dos preços atuais. Se houver item indisponível, o checkout mostra erro **por item** com instrução de remover (bloco de "não está mais disponível"). Só com o carrinho válido o fluxo avança para "Pedido enviado".
- *Por que endpoint separado da criação:* a criação idempotente do pedido é de `order-operation`; a validação de disponibilidade é leitura pura de storefront e pode rodar antes, dando feedback imediato.
- *Reconciliação de preço:* o total exibido no checkout usa os preços retornados por esta validação (autoridade do servidor), não os snapshots antigos do carrinho.

### 7. Fronteira storefront ↔ order-operation: contrato do pedido idempotente
Esta change **declara** o contrato; `order-operation` **implementa**. Contrato (a ser satisfeito por `order-operation`):
- **Request** (público, resolvido por subdomínio): `idempotency_key` gerado no cliente (UUID por tentativa de finalização), dados do cliente (nome, telefone), `delivery_type` (ENTREGA/RETIRADA), `address` (se ENTREGA), `notes`, e as linhas do pedido (produto + adicionais + quantidade). **Sem** `tenant_id` no corpo (vem do subdomínio).
- **Resposta:** o pedido criado (id curto, total) — idempotente: reenvio com a mesma `idempotency_key` no mesmo tenant retorna o **mesmo** pedido, sem duplicar.
- **Snapshots:** a persistência de snapshots (nome/preço/adicionais no momento) é responsabilidade de `order-operation`.
- No MVP desta change, o handoff ao WhatsApp acontece **após** a confirmação "Pedido enviado". A ordem exata entre "persistir o pedido" e "abrir o WhatsApp" e o tratamento de falha da persistência são detalhados junto de `order-operation`; do lado do storefront, o requisito é: **confirmar o envio ao usuário antes de abrir o WhatsApp** e **sempre** oferecer o fallback de cópia.

### 8. Montagem e handoff da mensagem de WhatsApp
A mensagem é montada no **cliente** no formato **exato** da Seção 3.1 (com emojis — única exceção à regra "sem emoji"). O handoff usa `wa.me/<whatsapp_do_negocio>?text=<mensagem_url_encoded>`. Como abrir `wa.me` pode falhar (bloqueio de popup, sem app), a tela Sent/Handoff **sempre** mostra a mensagem renderizada com botão "Copiar mensagem" e um aviso explicando o fallback — não é um estado de erro, é parte permanente da tela (Regra 4.5).
- *Alternativa descartada:* montar a mensagem no servidor — exigiria roundtrip e o número/itens já estão no cliente; montar no cliente é imediato e resiliente.

### 8b. Endereço só quando Entrega; validação inline
Checkout em tela única: nome (obrigatório), telefone (obrigatório, DDD válido), tipo de entrega. Campo de endereço só é exibido/exigido quando `delivery_type = ENTREGA`. Validação inline em tempo real (borda + mensagem por campo); botão "Finalizar" só habilita com o formulário válido **e** loja aberta **e** carrinho válido (sem itens indisponíveis, acima do mínimo).

## Risks / Trade-offs

- **Divergência de preço entre carrinho e servidor** (dono edita preço mid-session) → o checkout reconcilia com os preços da validação de finalização (Decisão 6); o total final sempre reflete a autoridade do servidor antes do handoff.
- **`wa.me` não abre** (popup bloqueado, sem WhatsApp instalado) → fallback de cópia como tela de primeira classe, sempre presente (Decisão 8).
- **Relógio/fuso do cliente** poderia mascarar aberto/fechado e disponibilidade do dia → ambos calculados no servidor no fuso fixo `America/Fortaleza` (Decisões 2 e 3).
- **Acesso cross-tenant via id de produto** forjado no request → todo id é validado contra o tenant do subdomínio; produto de outro tenant é tratado como inexistente (Decisão 1).
- **Carrinho preso a um subdomínio** que muda de nome (o dono renomeia) → risco aceito no MVP; a chave de localStorage inclui o slug, então o carrinho antigo simplesmente não é reaproveitado (e o PRD já avisa que trocar subdomínio quebra links).
- **Handoff antes/depois de persistir o pedido** é a fronteira com `order-operation` e fica em aberto quanto à ordem exata → do lado do storefront o contrato mínimo é fixo (confirmar antes de abrir WhatsApp + fallback sempre); o detalhe transacional é resolvido em `order-operation`.

## Open Questions

- Ordem exata entre criar o pedido (persistência idempotente em `order-operation`) e abrir o WhatsApp, e o comportamento se a persistência falhar após o usuário já ter visto "Pedido enviado" — a resolver junto com `order-operation` sem alterar o requisito do storefront (confirmar antes do handoff + fallback sempre disponível).
- Se a validação de disponibilidade da finalização e a criação do pedido devem ser um único endpoint em `order-operation` ou permanecer separadas (leitura em storefront + escrita em order-operation). Preferência atual: separadas.
