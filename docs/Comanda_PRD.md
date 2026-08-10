# Comanda

## Product Requirements Document

> **FONTE DA VERDADE:** Este é o primeiro e único documento de produto do Comanda. É a autoridade ativa para o produto, interface e regras de negócio.

**Status:** Escopo congelado para desenvolvimento
**Contexto:** Projeto desenvolvido por dev solo. MVP enxuto, com foco em colocar um produto real no ar rápido, sem ficar preso a dependências externas. Infraestrutura consolidada em um único VPS.

---

> **Regra fundamental**
> Nenhuma feature entra sem estar na Seção 3. Novas ideias vão para o Roadmap (Seção 11). A pergunta "mas não seria melhor se..." tem sempre a mesma resposta: vai para o roadmap.

---

## 1. Visão do Produto

> _"Seu cardápio no ar. Seus pedidos organizados. Em menos de 10 minutos."_

**Comanda** é uma plataforma de operação de pedidos para negócios alimentícios informais brasileiros. O empreendedor cria um cardápio digital público, compartilha o link com clientes, e recebe pedidos organizados — sem perder pedido no meio de conversa pessoal, sem digitar o mesmo resumo de pedido toda vez, sem ficar preso no celular durante o horário de pico.

O Comanda **não é apenas um cardápio digital** — é uma ferramenta de operação de pedidos, simples na experiência e suficientemente completa para o dia a dia de um negócio alimentício informal.

O produto **não compete** com iFood ou Anota AI. Compete com **não ter nada** — que é a realidade da maioria dos negócios alimentícios informais hoje. O cliente que usa Comanda ainda recebe pagamento pelo WhatsApp, ainda combina entrega pelo WhatsApp. O que muda é que o pedido chega organizado, formatado, e o dono tem um painel para acompanhar tudo.

**O produto se adapta ao modelo do negócio. O negócio não se adapta ao produto.**

### O que é um bom MVP aqui

O menor produto funcional que entrega o valor central para um usuário real, pode ser apresentado com clareza, e é sólido o suficiente para não envergonhar — mas simples o suficiente para ser construído em poucas semanas por um dev solo. Seu único objetivo é validar se empreendedores alimentícios vão usar e pagar por isso antes de construir qualquer coisa além.

**Um produto real, mesmo enxuto.** Velocidade não é desculpa para entregar um protótipo descartável. Cada decisão de corte do MVP é uma decisão sobre _quando_ construir, não sobre _abrir mão da qualidade_ do que já está no ar. O que entra no MVP entra pronto para um usuário pagante de verdade.

## 2. Público-Alvo

**Perfil primário:** qualquer pessoa que vende comida de forma informal e recebe pedidos pelo WhatsApp.

Exemplos concretos: marmiteira que vende para vizinhos e colegas de trabalho, confeiteira que faz bolos e doces sob encomenda, hamburgueria caseira que entrega no bairro, açaizeiro que tem delivery próprio, quem vende salgados e marmitas em grupos de WhatsApp.

**Características que definem esse público:**

- Celular como principal ferramenta de trabalho — não computador
- WhatsApp como canal exclusivo de vendas hoje
- Não tem tempo nem disposição para sistemas complexos
- Já perdeu pedido, já errou preparo por pedido confuso
- Paga por ferramenta quando sente que economiza tempo ou dinheiro
- Opera em diferentes contextos: em casa, em feira, com cardápio que varia por dia

**Quem o produto não atende (e não deve tentar atender):**

- Restaurantes estabelecidos com salão, mesa e garçom
- Negócios com iFood/Rappi como canal principal
- Operações que precisam de integração com sistemas de estoque
- Fornecimento B2B

---

## 3. Escopo do MVP

### 3.1 O que está dentro

#### Cardápio digital público (Storefront)

- Página pública acessada por link único: `nomedonegocio.${APP_DOMAIN}`
- Listagem de produtos com nome, foto (1 por produto) e preço
- Organização por categorias definidas pelo dono
- Produtos com disponibilidade por dia da semana (atributo do produto)
- Página de produto (bottom sheet) com foto, descrição e adicionais
- Adicionais com tipo (seleção única / múltipla) e obrigatoriedade (obrigatório / opcional)
- Carrinho persistente no navegador do cliente
- Tela de finalização: cliente informa nome, telefone, tipo de entrega (Entrega/Retirada) e endereço (se entrega)
- **Validação de disponibilidade na finalização** — se produto foi marcado como indisponível mid-session, erro claro por item
- Ao finalizar, WhatsApp do negócio abre com mensagem pré-formatada
- **Fallback de mensagem** — se `wa.me` não abrir, mensagem exibida na tela para copiar manualmente
- Pedido mínimo: bloqueio visual no carrinho com mensagem clara
- Tela de restaurante fechado quando fora do horário configurado
- Footer com link para política de privacidade

A mensagem gerada tem este formato:

```
Olá! Gostaria de fazer um pedido:

🛒 Meu Pedido
• 2x Marmita média (Frango grelhado) — R$ 15,00
  - Adicional: Ovo frito (+R$ 2,00)
• 1x Suco de laranja — R$ 5,00

📍 Entrega em: Rua das Flores, 42

💰 Total: R$ 25,00 (inclui R$ 5,00 de entrega)

📝 Obs: Sem cebola no frango, por favor.

Pode confirmar disponibilidade e forma de pagamento?
```

#### Painel do dono (operação)

- Login com e-mail e senha
- **Banner contextual de abertura** — quando dono acessa o painel dentro do horário configurado e negócio está fechado
- **Lista de pedidos com atualização automática (polling 15s)** com `document.visibilityState` check
- **Indicador visual de conectividade** — quando polling falha, banner discreto sem silêncio
- Cada pedido com: ID curto, nome do cliente, resumo de itens truncado, valor, status, tipo de entrega
- Avanço manual de status: Recebido → Aceito → Em preparo → Pronto → Entregue
- **Botão de avanço com estado de loading** — desabilitado durante requisição, previne duplo toque
- Mensagem pronta para WhatsApp a cada avanço de status
- Cancelamento com motivo obrigatório (mínimo 10 caracteres)
- Card hero com resumo do dia — colapsável, visível quando há pedidos
- Toggle Aberto / Fechado **visível UMA VEZ no header**
- Filtros: Todos, Novos, Aceitos, Em preparo, Prontos, Entregues, Cancelados
- Skeleton loaders durante carregamento

#### Configuração

- Nome do negócio, logo, horário de funcionamento
- CRUD de categorias e produtos
  - Produto: foto, nome, descrição, preço, disponibilidade por dia da semana, toggle de disponibilidade manual
  - Adicionais por produto: grupos com nome, obrigatoriedade, tipo de seleção, itens com preço adicional
- Número de WhatsApp do negócio
- Taxa de entrega (valor fixo) e valor mínimo de pedido
- "Meu link" com copiar, QR Code e compartilhar no WhatsApp
  - **Aviso permanente**: alterar subdomínio quebra todos os QR Codes impressos e links compartilhados
- Badge do plano atual + barra de progresso de uso

#### Onboarding e SaaS

- Cadastro self-service — meta: cardápio no ar em menos de 10 minutos
- Subdomínio automático gerado no cadastro
- Cardápio de demonstração pré-populado por categoria do negócio
- Wizard de onboarding: horários → primeiro produto → pronto
- Plano Gratuito: até 30 pedidos/mês, 30 produtos, 5 categorias
- Plano Essencial (R$ 49/mês): pedidos, produtos e categorias ilimitados + histórico de 30 dias
- Enforcement de limites por plano com mensagens claras
- Comunicação do limite de pedidos **antes** do cadastro
- Tela de planos com comparativo visual
- **Cobrança no MVP:** ativação manual do plano Essencial (ver Seção 10). A integração com gateway (Stripe) está planejada e tem lugar definido no roadmap, posicionada para não bloquear nenhuma outra entrega.

#### PWA — painel do dono

O painel do dono é entregue como **Progressive Web App instalável**:

- Manifest configurado: ícone, nome, cores, orientação
- Service Worker básico: cache de assets estáticos, funcionamento offline para telas já carregadas
- "Adicionar à tela inicial" funciona em Android e iOS
- Experiência de app instalável para o dono sem publicação nas lojas

O PWA resolve 100% da necessidade de "app instalável" do MVP. Empacotamento nativo via lojas é roadmap (Seção 11), e o frontend é estruturado de forma a não criar retrabalho quando esse momento chegar.

---

### 3.2 O que está fora do MVP

| Feature                                          | Decisão                        | Motivo                                                                                                                                                                                                                                                   |
| ------------------------------------------------ | ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Empacotamento nativo (Capacitor / lojas)         | **Fase 2**                     | PWA instalável resolve o MVP por completo. Empacotamento nativo introduz atrito de build (output estático vs. SPA com subdomínio) que não se paga antes da validação. Entra quando houver usuários ativos pedindo notificação push/alerta sonoro nativo. |
| Integração de gateway de pagamento (Stripe)      | **Fase 2 — sem bloquear nada** | Cobrança manual valida disposição de pagar com zero código de integração. Stripe entra num ponto do roadmap escolhido para não travar nenhuma outra implementação (ver Seção 7.5 e Seção 10).                                                            |
| Modos de Operação (Cardápio Adaptativo)          | **Fase 2**                     | Resolve problema de um subconjunto do público; a maioria dos primeiros usuários não usa. Schema **não** é pré-criado no MVP — adicionado via migration quando a feature entrar.                                                                          |
| Estoque diário por produto (`daily_stock`)       | **Backlog**                    | Decremento por pedido + reset à meia-noite introduz concorrência e dependência de timezone. Toggle de disponibilidade manual + `available_days` cobre o caso real do MVP.                                                                                |
| Multioperadores (UI + permissões)                | **Fase 3**                     | Um OWNER por tenant no MVP. Tabelas de usuários e permissões não são pré-criadas — modeladas quando a feature entrar.                                                                                                                                    |
| Push notifications / alerta sonoro nativo        | **Fase 2**                     | Viabilizado pelo empacotamento nativo.                                                                                                                                                                                                                   |
| Notificações automáticas de WhatsApp             | **Fase 4**                     | Requer integração com API de mensageria.                                                                                                                                                                                                                 |
| Histórico de clientes                            | **Fase 3**                     |                                                                                                                                                                                                                                                          |
| Relatório diário/semanal                         | **Fase 3**                     |                                                                                                                                                                                                                                                          |
| Domínio próprio do tenant                        | **Fase 3**                     |                                                                                                                                                                                                                                                          |
| Agendamento de pedidos                           | **Fase 3**                     | Spec próprio necessário.                                                                                                                                                                                                                                 |
| Status do pedido para o cliente (página pública) | **Fase 3**                     | Requer SSE/WebSocket.                                                                                                                                                                                                                                    |
| Pedido recorrente                                | **Fase 3**                     | Requer histórico de clientes.                                                                                                                                                                                                                            |
| Links promocionais / eventos                     | **Fase 2**                     |                                                                                                                                                                                                                                                          |
| SSE/WebSocket                                    | **Backlog**                    | Polling 15s suficiente para o MVP.                                                                                                                                                                                                                       |
| Timezone configurável por tenant                 | **Fase 4**                     | Hardcode `America/Fortaleza` documentado (Seção 13).                                                                                                                                                                                                     |
| Fotos múltiplas por produto                      | **Backlog**                    | Exige UI de galeria.                                                                                                                                                                                                                                     |
| Dark mode                                        | **Backlog**                    |                                                                                                                                                                                                                                                          |
| Marketing, ads, vídeos, marketplace, IA          | **Descartado**                 | Fora de escopo indefinidamente.                                                                                                                                                                                                                          |
| Painel offline-first completo                    | **Backlog**                    |                                                                                                                                                                                                                                                          |

---

## 4. Confiabilidade Operacional Mínima — Não Negociável

> Para um produto que promete "nunca mais perder pedido", falhar silenciosamente é falhar na proposta de valor central. Esses itens têm o mesmo peso dos critérios de lançamento.

### 4.1 Prevenção de pedido duplicado

- `idempotency_key` único por pedido no backend, validado cross-tenant
- Botão "Finalizar pedido" com estado de loading que bloqueia novo toque enquanto requisição está em curso
- Feedback explícito de "Pedido enviado" antes de abrir WhatsApp
- Endpoint de criação de pedido idempotente

### 4.2 Prevenção de pedido perdido

- Polling 15s com `document.visibilityState` — não consome recursos com aba em background
- Indicador visual de conectividade — nunca silêncio quando polling falha
- Nenhum pedido em estado ambíguo na UI — se avanço falhar, card retorna ao estado anterior com erro visível

### 4.3 Prevenção de ação duplicada no painel

- Botão de avanço com estado de loading — desabilitado durante requisição
- Endpoint de avanço idempotente — request duplicado não gera dois registros em `order_status_history`

### 4.4 Rastreabilidade mínima

- `order_status_history` append-only — nunca atualizar, nunca deletar
- Cada registro: `from_status`, `to_status`, `changed_by`, `created_at`

### 4.5 Fallbacks de interface

- Se `wa.me` não abrir: mensagem formatada na tela com botão "Copiar mensagem"
- Se upload de foto falhar: produto salvo sem foto, indicador claro, opção de tentar novamente
- Se polling retornar erro: banner discreto com timestamp da última atualização bem-sucedida

### 4.6 Plano de resposta a incidentes (dev solo)

- Responsável: o próprio desenvolvedor
- Canal de emergência: e-mail + WhatsApp pessoal documentados internamente
- Critério de notificação à ANPD: vazamento de dados com risco real aos titulares → notificar em até 72h

---

## 5. Jornadas

### 5.1 Jornada do dono — onboarding

```
1. Acessa o domínio da aplicação e clica em "Criar meu cardápio"
2. Seleciona categoria do negócio (marmiteria, confeitaria, hamburgueria, açaizeria)
3. Preenche: nome, nome do negócio, subdomínio, e-mail, senha, WhatsApp
   — Limite do plano gratuito comunicado antes de criar conta
4. Cardápio de demonstração pré-populado com produtos típicos da categoria
5. Wizard:
   → Horário de funcionamento
   → Editar/confirmar primeiro produto
   → Tela "Pronto!": link + copiar + compartilhar no WhatsApp + QR Code
6. Cardápio publicado: nomedonegocio.${APP_DOMAIN}
```

### 5.2 Jornada do cliente final

```
1. Recebe link pelo WhatsApp ou escaneia QR Code
2. Acessa o cardápio — sem login, mobile-first
3. Navega por categorias; produtos indisponíveis no dia não aparecem
4. Toca no produto → bottom sheet com adicionais
   — Adicionais obrigatórios bloqueiam "Adicionar" se não selecionados
5. Adiciona ao carrinho
6. Vai ao checkout: nome, telefone, tipo de entrega, endereço
7. Sistema valida disponibilidade — erro claro se algo mudou mid-session
8. WhatsApp abre com mensagem pré-formatada
   — Fallback: mensagem na tela para copiar se WhatsApp não abrir
```

### 5.3 Jornada do dono — operação

```
1. Abre o painel — banner de abertura se negócio fechado no horário configurado
2. Novo pedido aparece automaticamente (polling 15s)
3. Toca no card → bottom sheet com detalhe e ações
   — Preview da mensagem WhatsApp gerada visível
4. Avança status com ou sem notificação ao cliente
5. Cancela com motivo obrigatório se necessário
6. Card hero mostra resumo do dia em tempo real
```

---

## 6. Modelo de Planos

### Princípios

- O plano gratuito entrega valor real — não é demo bloqueada
- O limite de 30 pedidos não frustra: suficiente para validar, insuficiente para operar em volume
- O upgrade deve ser sentido como inevitável, não persuadido
- O limite é comunicado **antes** do cadastro

### Gratuito — _"Comece agora"_

**R$ 0 / sempre**

| Feature                                 | Detalhe            | Enforcement                                          |
| --------------------------------------- | ------------------ | ---------------------------------------------------- |
| Pedidos por mês                         | Até **30**         | `402` com `code: PLAN_LIMIT_REACHED` no 31º          |
| Produtos                                | Até **30**         | `402` com `code: PRODUCT_LIMIT_REACHED` ao criar 31º |
| Categorias                              | Até **5**          | `402` com `code: CATEGORY_LIMIT_REACHED` ao criar 6ª |
| Cardápio digital público                | ✅                 | —                                                    |
| Subdomínio automático                   | ✅                 | —                                                    |
| Carrinho e finalização via WhatsApp     | ✅                 | —                                                    |
| Painel de pedidos                       | ✅                 | —                                                    |
| Avanço de status com mensagem assistida | ✅                 | —                                                    |
| Adicionais por produto                  | ✅                 | —                                                    |
| Disponibilidade por dia da semana       | ✅                 | —                                                    |
| Resumo do dia                           | ✅                 | —                                                    |
| Toggle aberto/fechado                   | ✅                 | —                                                    |
| Histórico de pedidos                    | **Visível 7 dias** | Filtro na query; dados permanecem no banco           |
| Aviso ao atingir 25/30 pedidos          | ✅                 | Banner quando `orderCountThisMonth >= 25`            |
| PWA instalável                          | ✅                 | —                                                    |

### Essencial — _"Meu negócio organizado"_

**R$ 49/mês**

Tudo do Gratuito, mais:

| Feature              | Detalhe         |
| -------------------- | --------------- |
| Pedidos por mês      | Ilimitados      |
| Produtos             | Ilimitados      |
| Categorias           | Ilimitadas      |
| Histórico de pedidos | Visível 30 dias |

> **Plano Profissional** entra quando houver usuários no Essencial pedindo por mais. Não antes.

A **lógica de planos é código desde o MVP** — o enforcement de limites cria a pressão de upgrade independentemente de como o pagamento é processado. O que muda entre MVP e Fase 2 é apenas o _processamento do pagamento_ (manual → Stripe), não a máquina de planos.

---

## 7. Stack Técnica

A stack será consolidada em **um VPS compartilhado**, com ambientes de produção e demonstração isolados. Nada de orquestrar quatro serviços gratuitos com limitações distintas: um servidor e um ingress compartilhado, com bancos, segredos e volumes separados por ambiente.

### 7.1 Backend

| Componente      | Tecnologia                | Justificativa                                                                                        |
| --------------- | ------------------------- | ---------------------------------------------------------------------------------------------------- |
| Linguagem       | Java 21                   | Virtual Threads — concorrência sem reatividade                                                       |
| Framework       | Spring Boot 4.1.0         | LTS atual(<https://docs.spring.io/spring-boot/index.html>), ecossistema maduro, zona de força do dev |
| Banco de dados  | PostgreSQL 17             | Um banco resolve o MVP e os próximos 18 meses                                                        |
| ORM             | Spring Data JPA           | Padrão de mercado                                                                                    |
| Autenticação    | Spring Security + JWT     | Stateless, simples                                                                                   |
| Migrations      | Flyway                    | Schema versionado desde o dia 1                                                                      |
| Build           | Maven                     | Estável e amplamente suportado                                                                       |
| Servir frontend | Spring (static resources) | O backend serve o build estático da SPA — um único artefato deployável                               |

**JWT:** access token 15-30 min em memória, refresh token 7-30 dias em httpOnly cookie com rotação. Sem JWT em localStorage.

### 7.2 Frontend

O frontend é uma **SPA React servida estaticamente pelo backend** — sem framework SSR, sem servidor de frontend separado, sem conflito de build target. O storefront público lê o subdomínio no client e busca o cardápio via API. SEO não é crítico: o link do cardápio chega pronto pelo WhatsApp, não via busca orgânica — então abrir mão de SSR não custa praticamente nada e elimina uma camada inteira de complexidade.

| Componente        | Tecnologia      | Justificativa                                                                      |
| ----------------- | --------------- | ---------------------------------------------------------------------------------- |
| Build / framework | React + Vite    | SPA leve, build estático, dev server rápido                                        |
| Estilização       | Tailwind CSS    | Produtividade para dev solo                                                        |
| Componentes       | shadcn/ui       | Prontos, acessíveis, sem vendor lock-in — reduz a superfície de frontend ao mínimo |
| Estado global     | Zustand         | Carrinho — simples e leve                                                          |
| Data fetching     | TanStack Query  | Polling, cache e estados de loading/erro com pouco código                          |
| Roteamento        | React Router    | Storefront + painel no mesmo bundle, separados por rota                            |
| Ícones            | Lucide React    | Sem emojis na UI                                                                   |
| PWA               | Vite PWA plugin | Manifest + Service Worker básico                                                   |

### 7.3 Infraestrutura — VPS compartilhado

| Componente             | Decisão                                            | Observação                                                                           |
| ---------------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Host                   | VPS (ex.: Hetzner CAX11 ARM, ~€4/mês)              | Backend + Postgres + proxy no mesmo servidor                                         |
| Proxy reverso / TLS    | Caddy                                              | HTTPS automático; wildcard de subdomínio (`*.${APP_DOMAIN}`) com configuração mínima |
| Banco                  | PostgreSQL no próprio host                         | Sem serviço gerenciado, sem pausa por inatividade, sem keep-alive                    |
| Armazenamento de fotos | Disco local do VPS (ou bucket S3-compatível)       | Local resolve o MVP; migrar para bucket quando o volume justificar                   |
| Deploy                 | Docker Compose com JAR + frontend estático embarcado | O JAR permanece o artefato da aplicação dentro da imagem                       |
| E-mails transacionais  | Provedor SMTP transacional (free tier)             | Onboarding e recuperação de senha                                                    |
| Backup                 | Dump diário do Postgres                            | Cron no host + cópia off-site                                                        |

**Domínio:** ~R$ 40/ano no Registro.br — único custo fixo relevante antes da validação, somado ao VPS (~€4/mês). Sem dependência de free tiers com regras de pausa ou cold start.

**Wildcard de subdomínio:** o Caddy resolve TLS para `*.${APP_DOMAIN}` automaticamente, então cada novo tenant ganha `nomedonegocio.${APP_DOMAIN}` sem provisionamento manual.

**Política de domínio:** a raiz oficial é `${APP_DOMAIN}` e cada vitrine usa `<tenant>.${APP_DOMAIN}`. `www`, `app`, `api`, `docs`, `status`, `admin`, `demo` e `signal` são reservados e não podem ser registrados, com enforcement autoritativo no backend. `.design` é material local e permanece sem rastreamento no Git.

**Infraestrutura futura (deferred):** o VPS será compartilhado por ambientes isolados de produção e demonstração. Compose concreto, ingress, redes, limites, DNS/TLS, bancos, segredos, volumes, Signal, backups e reset da demo somente serão definidos e validados quando o VPS existir. As regras de produto e multi-tenancy não mudam.

### 7.4 Multi-tenancy

- Isolamento por `tenant_id` em todas as tabelas e em todos os endpoints
- Resolução de tenant pelo subdomínio no storefront público; pelo JWT no painel
- IDOR validado em todo recurso acessado por ID
- Constraints de unicidade no banco: `subdomain` único, `idempotency_key` único por tenant

### 7.5 Gateway de Pagamento — Stripe

**Decisão: Stripe é o gateway do produto.** A integração é planejada e tem lugar definido no roadmap (Fase 2), posicionada num ponto em que **não bloqueia nenhuma outra implementação**.

**Por que não no MVP:** integrar gateway de assinatura (checkout, webhook, máquina de estados de assinatura, falha de pagamento, downgrade) é um produto dentro do produto. Para validar a disposição de pagar, basta haver gente querendo pagar. No MVP a cobrança do Essencial é **manual** (ver Seção 10), com a lógica de planos já em código.

**Considerações conhecidas de Stripe no Brasil** (a tratar na implementação da Fase 2):

- PIX no Stripe é processado localmente em BRL (via parceiro), mas **recorrência nativa via PIX é limitada**. Para assinatura recorrente, o caminho prático tende a ser cartão para recorrência automática e/ou PIX em modelo pré-pago/renovação assistida.
- Elegibilidade de PIX exige processamento prévio na conta; planejar ativação com antecedência.
- Atenção a IOF/estrutura da conta (BR vs. cross-border) para que tarifas não apareçam de forma indevida no extrato do cliente final.

**Critério para integrar:** quando o trabalho manual de cobrança começar a doer (aproximadamente 5-10 assinantes ativos) **e** num momento do plano em que a integração não atrase nenhuma feature do caminho crítico. A decisão de _quando_ integrar é uma decisão de planejamento, registrada explicitamente no plano de desenvolvimento.

---

## 8. Modelo de Dados

> Schema enxuto: apenas o que o MVP usa. Tabelas de features futuras (modos de operação, multioperadores, estoque diário) **não** são pré-criadas — entram via migration quando a feature entrar. Flyway torna isso barato.

```sql
-- TENANCY
tenants
  id, subdomain, name, logo_url,
  whatsapp_number,
  delivery_fee, min_order_value,
  is_open, plan, plan_expires_at,
  order_count_month,
  created_at, updated_at

-- USUÁRIO DONO (um por tenant no MVP)
users
  id, tenant_id, name, email,
  password_hash,
  is_active, created_at

business_hours
  id, tenant_id, day_of_week (0=Dom, 6=Sab),
  opens_at, closes_at, is_closed

-- CARDÁPIO
categories
  id, tenant_id, name, position, is_active

products
  id, tenant_id, category_id,
  name, description, price,
  image_url, is_available, position,
  available_days (int[])   -- dias da semana disponíveis; null = todos

-- ADICIONAIS
additional_groups
  id, product_id, name,
  required (boolean),
  selection_type (SINGLE | MULTIPLE),
  min_selections, max_selections

additional_items
  id, additional_group_id,
  name, additional_price, is_available

-- CLIENTES
customers
  id, tenant_id, name, phone, created_at

-- PEDIDOS
orders
  id, tenant_id, customer_id,
  status, delivery_type,
  subtotal, delivery_fee, total,
  address_snapshot, notes, cancellation_reason,
  idempotency_key,
  created_at, updated_at

order_items
  id, order_id,
  product_name_snapshot, unit_price_snapshot,
  quantity, subtotal

order_item_additionals
  id, order_item_id,
  additional_name_snapshot,
  additional_price_snapshot

order_status_history  -- append-only, nunca deletar
  id, order_id,
  from_status, to_status,
  changed_by_user_id (FK users, nullable para SYSTEM),
  notes, created_at

-- ASSINATURA
subscriptions
  id, tenant_id,
  plan, status, current_period_end, cancelled_at,
  external_subscription_id (nullable)  -- preenchido quando Stripe integrar
```

### Decisões de modelo

- `available_days`: array de inteiros (0=Dom...6=Sab). Null = disponível todos os dias.
- Disponibilidade de produto no MVP é controlada por `is_available` (toggle manual) + `available_days`. Estoque diário fica no backlog.
- `idempotency_key`: único por tenant. Validação cross-tenant (mesma key, tenant diferente → rejeitar).
- `order_status_history`: append-only. `changed_by_user_id` null quando origem é SYSTEM.
- Retenção de histórico: filtro na UI (7 ou 30 dias conforme plano); dados nunca são deletados.
- `subscriptions.external_subscription_id`: nullable no MVP (cobrança manual); recebe o ID do Stripe quando a integração entrar, sem migration disruptiva.

---

## 9. Critérios de Lançamento

**Funcional:**

- Dono se cadastra sozinho em menos de 10 minutos
- Cliente faz pedido do zero em menos de 2 minutos
- WhatsApp abre com mensagem formatada em iOS e Android
- Fallback de mensagem funciona quando WhatsApp não abre
- Dono avança status e mensagem para cliente é gerada corretamente
- Toggle aberto/fechado funciona
- Banner contextual de abertura funciona
- Pedido abaixo do mínimo bloqueado
- Adicionais obrigatórios bloqueiam "Adicionar" sem seleção
- Produto com `available_days` some do cardápio nos dias não configurados
- Produto marcado como indisponível some automaticamente
- Validação mid-session retorna erro por item com instrução clara
- Limites do plano gratuito enforçados e comunicados
- Ativação manual do plano Essencial funciona (Seção 10)
- Dados de um tenant não aparecem em query de outro
- Lista de pedidos atualiza automaticamente
- Indicador de conectividade aparece quando polling falha
- Botão de avanço não dispara ação dupla
- Pedido duplicado não é criado
- **PWA instalável funciona no celular do dono (Android obrigatório; iOS desejável)**

**Confiabilidade (Seção 4):**

- Nenhum estado silencioso de falha na UI
- `order_status_history` registra toda mudança
- Fallback de mensagem funcional

**Segurança:**

- IDOR testado em todos os endpoints com ID
- Rate limiting ativo em autenticação
- Enumeração de usuários não possível via login
- Headers de segurança HTTP configurados
- CORS restrito a origens conhecidas
- Upload valida magic bytes
- Nenhum dado sensível em logs

**LGPD:**

- Política de privacidade publicada
- Termos de serviço com cláusula controlador-operador
- E-mail de contato de privacidade ativo e monitorado
- Registro interno de atividades de tratamento criado

**Visual:**

- Tipografia, sombras, animações e hierarquia visual consistentes com o design system
- Skeleton loaders em vez de texto "Carregando..."
- Ícones Lucide React exclusivamente — sem emojis na UI

---

## 10. Monetização

### Matemática do negócio

| Clientes | Plano médio | MRR       | Situação                             |
| -------- | ----------- | --------- | ------------------------------------ |
| 1        | Essencial   | R$ 49     | Cobre o VPS no mês                   |
| 3        | Essencial   | R$ 147    | Cobre VPS + domínio no ano com folga |
| 20       | Essencial   | R$ 980    | Renda complementar real              |
| 50       | Essencial   | R$ 2.450  | Negócio consolidando                 |
| 100      | Mix         | ~R$ 5.500 | Negócio saudável                     |

### Cobrança no MVP — manual

- Quando um usuário quer assinar o Essencial, o pagamento é combinado e recebido manualmente (PIX).
- A liberação do plano é feita por ativação manual (tela de admin interna ou toggle controlado no `subscriptions`/`tenants`).
- O enforcement de limites do plano gratuito **é código** desde o dia 1 — é ele que cria a pressão de upgrade.
- Falha/cancelamento de pagamento no MVP é tratado manualmente (rebaixar para Gratuito na mão).

### Cobrança na Fase 2 — Stripe integrado

- Checkout, recorrência e webhooks via Stripe (ver Seção 7.5).
- Downgrade automático: falha de pagamento → grace period → rebaixa para Gratuito.
- A integração é agendada num ponto do plano que **não bloqueia** nenhuma outra feature do caminho crítico.

---

## 11. Roadmap Pós-MVP

### Fase 2 — Consolidação e monetização automática (após primeiros usuários reais)

- **Integração com Stripe** — checkout, recorrência, webhooks, downgrade automático
- Empacotamento nativo (Capacitor) + publicação na Play Store
- Alerta sonoro + push notification nativa de novo pedido
- Busca e filtro mais robustos no painel
- Destaque visual para pedidos com tempo de espera elevado
- **Modos de Operação** — presets de produtos, taxas e horários; ativação manual; QR Code por preset
- Estoque diário por produto
- Links promocionais com desconto
- Imagem estática gerada para compartilhamento

### Fase 3 — Expansão de operação (após ~20 usuários pagantes)

- **Multioperadores** — UI de convite, permissões por ação, acesso temporário
- Fila de preparo (visão cozinha)
- Agendamento de pedido
- Status do pedido para o cliente (página pública)
- Histórico de clientes com pedido recorrente
- Domínio próprio do tenant
- Publicação na App Store

### Fase 4 — Crescimento e plataforma

- PIX e cartão integrados no checkout do cliente final
- Notificações automáticas via WhatsApp API
- Plano Profissional: relatórios, cupons
- Integração com Google Meu Negócio
- Timezone configurável por tenant

### Backlog sem prazo

- SSE/WebSocket (substituir polling)
- Galeria de fotos por produto
- Programa de fidelidade
- Rastreamento de entregador
- Impressora térmica
- Painel offline-first completo
- Dark mode

---

## 12. Regras de Desenvolvimento

**1. Escopo congelado.** Nenhuma feature entra sem estar na Seção 3.

**2. Velocidade não compra qualidade.** O MVP é enxuto, mas é um produto real. Cortes são decisões de _quando_, não de _se com qualidade_.

**3. Sem otimização prematura.** Sem Redis, sem filas, sem microsserviços, sem schema de feature futura antes de dados reais.

**4. Entregável incremental é sagrado.** Cada etapa do plano tem critério verificável.

**5. Dev solo — uma coisa de cada vez.** Sem paralelizar frentes antes de cada critério estar verde.

**6. Boring é bom.** Nenhuma biblioteca nova sem justificativa clara.

**7. Snapshots são obrigatórios.** Produto, preço e adicionais sempre em snapshot no pedido.

**8. Constraints no banco, não só no código.** `idempotency_key` único, `subdomain` único.

**9. Segurança multi-tenant não é opcional.** Todo endpoint filtra por `tenant_id`. IDOR validado em todo recurso acessado por ID. Teste de isolamento desde o dia 1.

**10. Fluxos de erro são features.** Restaurante fechado, pedido abaixo do mínimo, produto indisponível, limite de plano, adicional obrigatório não selecionado, WhatsApp não abre, conectividade perdida — todos têm tela e tratamento.

**11. Nenhum estado silencioso de falha.** Se algo quebrou, o usuário sabe.

**12. Visual é feature.** Tipografia, sombras, animações e hierarquia fazem parte do critério de "pronto".

**13. Nunca emojis na UI.** Emojis apenas em mensagens de WhatsApp.

**14. Dados sensíveis nunca em logs.** Senha, token, telefone — nunca aparecem em nenhum log.

**15. O produto se adapta ao negócio.** Toda decisão de UX começa em: "funciona para quem opera sozinho E para quem tem um ajudante?"

**16. Integração de pagamento é agendada, não improvisada.** Stripe entra num ponto do plano definido para não bloquear o caminho crítico. Até lá, cobrança é manual e a lógica de planos é código.

---

## 13. Limitações Conhecidas

| Limitação                              | Impacto                                                     | Quando resolver                    |
| -------------------------------------- | ----------------------------------------------------------- | ---------------------------------- |
| Timezone hardcoded `America/Fortaleza` | Negócios em outro fuso terão horários deslocados            | Fase 4                             |
| 1 foto por produto                     | Não afeta MVP                                               | Backlog                            |
| Endereço como TEXT único               | Sem taxa variável por região                                | Fase 3                             |
| Polling 15s em vez de real-time        | Delay máximo de 15s para novo pedido                        | SSE/WebSocket em backlog           |
| `day_of_week` convenção (0=Dom)        | Frontend e backend devem usar mesma convenção               | Teste E2E que valida               |
| Cobrança manual no MVP                 | Trabalho operacional por assinante; insustentável em escala | Stripe na Fase 2, antes de escalar |
| Multioperadores — UI adiada            | Dono com ajudante usa mesmo login temporariamente           | Fase 3                             |
| Alerta sonoro / push adiado            | Dono que cozinha pode não ver pedido na hora                | Fase 2                             |
| Empacotamento nativo adiado            | Usuários instalam via PWA (browser)                         | Fase 2                             |
| Pagamento do cliente final fora do app | Pagamento combinado pelo WhatsApp                           | Fase 4                             |

---

_Comanda — escopo congelado para desenvolvimento._
_Dev solo · MVP enxuto e real · Stack: Spring Boot 4.1.0 + PostgreSQL 17 + React (Vite) SPA, tudo em um VPS · Proxy: Caddy · Gateway: Stripe (Fase 2) · Cobrança no MVP: manual._
