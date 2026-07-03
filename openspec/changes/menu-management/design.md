## Context

`foundations` criou o schema (`categories`, `products`, `additional_groups`, `additional_items`) e o padrão de isolamento por `tenant_id`; `owner-auth` entrega a sessão do OWNER e o shell do painel. Falta o conteúdo: gerir o cardápio. O cardápio é hierárquico — categoria → produto → grupo de adicional → item de adicional — e é lido por três consumidores futuros: a vitrine pública (`public-storefront`), o pedido (`order-operation`, via snapshots) e o próprio painel.

Restrições herdadas do PRD:
- Um único VPS; fotos no **disco local** no MVP, com o domínio estruturado para migrar a bucket S3-compatível sem reescrita (Seção 7.3).
- Multi-tenancy por coluna `tenant_id`; IDOR validado em todo acesso por ID (Seção 7.4 / Regra 9).
- Timezone `America/Fortaleza` hardcoded — relevante para interpretar `available_days` (dia da semana) de forma consistente entre back e front (Limitação Seção 13, convenção `0=Dom`).
- Sem emoji na UI; Lucide React; skeleton loaders; mobile-first (Seção 9 / Regra 12–13).

## Goals / Non-Goals

**Goals:**
- CRUD completo e isolado por tenant de categorias, produtos e adicionais.
- Upload de imagem robusto: valida magic bytes, e **nunca** bloqueia o salvamento do produto quando falha.
- Persistir disponibilidade por dia da semana (`available_days`) e toggle manual (`is_available`) sem inventar estoque diário.
- Reordenação estável de categorias e produtos via `position`.
- Telas fiéis ao design (`CARDÁPIO`, `PRODUTO (editor)`).

**Non-Goals:**
- Leitura pública / filtragem por disponibilidade no dia (fica em `public-storefront`).
- Enforcement de limites de plano (produtos/categorias) — só persistência aqui; limite em `plans-and-onboarding`.
- Estoque diário, galeria de fotos, versão/publicação de cardápio.

## Decisions

### 1. Modelo de escrita hierárquico com ownership por produto
`additional_groups` e `additional_items` pertencem a um produto (grupo → `product_id`; item → `additional_group_id`). Grupos e itens **não** são reutilizáveis entre produtos no MVP — casa com o schema de `foundations` (grupo referencia `product_id`) e com a UI (editor de grupos dentro da tela de produto). Escrita do produto e seus grupos/itens numa transação por operação.
- *Alternativa descartada:* biblioteca global de adicionais reutilizáveis — mais tabelas de junção, fora do escopo do MVP.

### 2. Upload de imagem desacoplado do salvamento do produto
Upload é um passo **separado** que retorna uma referência de imagem; o produto guarda `image_url`. O front tenta o upload; em falha, salva o produto com `image_url = null`, mostra indicador de "foto não enviada" e botão "tentar de novo". Isso satisfaz a Regra 4.5 (falha de foto ≠ falha de produto) sem acoplar dois recursos numa transação frágil.
- **Validação de magic bytes:** o backend inspeciona os bytes iniciais do arquivo (assinaturas JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF....WEBP`) e rejeita qualquer coisa cujo conteúdo real não bata, independentemente da extensão/Content-Type declarado. Limite de tamanho e content-type permitido (`image/jpeg`, `image/png`, `image/webp`).
- **Armazenamento:** disco local sob um diretório por tenant; nome de arquivo gerado (UUID), extensão derivada do tipo real detectado. `image_url` é um caminho relativo servido pelo backend — abstraído atrás de um `ImageStorage` para trocar por bucket depois sem tocar o domínio.
- *Alternativa descartada:* upload embutido no multipart de criação do produto — quando a imagem falha, o produto também falharia ou exigiria rollback parcial; contraria 4.5.

### 3. `available_days` como array de inteiros, convenção `0=Dom … 6=Sáb`
Persistido como `int[]` (schema de `foundations`); `null`/vazio = disponível todos os dias. Back e front **compartilham a mesma convenção** (Limitação Seção 13). A UI mostra 7 chips (D S T Q Q S S). Nesta change só persistimos; a regra "some do cardápio no dia X" é aplicada na leitura pública.

### 4. Isolamento e IDOR
Toda query de leitura/escrita inclui `tenant_id` do JWT (padrão de `foundations`/`multi-tenancy`). Endpoints acessados por ID (`GET/PUT/DELETE /products/{id}` etc.) carregam o recurso **filtrando por tenant** — recurso de outro tenant retorna 404 (não 403, para não vazar existência). Grupos/itens validam a cadeia de ownership até o tenant (item → grupo → produto → tenant).

### 5. Reordenação por `position`
`position` inteiro por categoria (dentro do tenant) e por produto (dentro da categoria). Endpoint de reordenação recebe a nova ordem e reescreve as posições numa transação. Ordenação de leitura sempre por `position` asc.

### 6. Remoção
`DELETE` de categoria com produtos: decidir entre bloquear ou cascatear. **Decisão:** bloquear remoção de categoria não-vazia com erro claro (`CATEGORY_NOT_EMPTY`) — evita apagar produtos por engano; o dono move/remove produtos antes. Remoção de produto cascateia seus grupos/itens (pertencem ao produto). Pedidos já usam **snapshots** (nome/preço no momento), então remover produto não corrompe histórico.

## Risks / Trade-offs

- **Foto órfã no disco** quando o produto é removido ou a imagem é trocada → job/rotina simples de limpeza fica fora do MVP; aceitar acúmulo pequeno e documentar. Mitigação mínima: ao trocar imagem, apagar a anterior no mesmo fluxo (best-effort, falha não bloqueia).
- **Magic bytes não cobrem todo formato** (ex.: WebP tem variações) → restringir a JPEG/PNG/WebP e rejeitar o resto com mensagem clara; ampliar depois se necessário.
- **Bloquear remoção de categoria não-vazia** pode irritar quem quer limpar rápido → mensagem explica o porquê e quantos produtos há; alternativa de mover em lote fica para depois.
- **Sem enforcement de limite aqui** → é possível criar >30 produtos nesta change isolada; o limite entra em `plans-and-onboarding` e a fronteira está registrada. Não duplicar a regra.
- **Ausência de estoque diário** → coberto por `is_available` + `available_days`, decisão de produto (backlog).
