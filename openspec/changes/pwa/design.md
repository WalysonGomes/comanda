## Context

O painel do dono (`order-operation`) é uma SPA React + Vite, servida estaticamente pelo Spring Boot, compartilhando o mesmo bundle do storefront público, separados por rota (React Router, definido em `foundations`). O PRD (Seção 3.1, 6, 7.2 e critério de lançamento da Seção 9) exige que **apenas o painel** seja um PWA instalável: manifest, Service Worker básico (precache + offline de telas carregadas) e "Adicionar à tela inicial" em Android (obrigatório) / iOS (desejável). O storefront do cliente permanece um site de navegador comum.

O desafio de design vem de dois fatos do MVP:
1. **Um único bundle serve dois contextos** (storefront público em subdomínio de tenant + painel do dono), mas só um deve ser PWA. O `scope`/`start_url` e o registro do Service Worker precisam ser cirúrgicos.
2. **Multi-tenancy por subdomínio**: o storefront vive em `nomedonegocio.${APP_DOMAIN}`. O painel do dono vive em um host/rota estável (definido por `foundations`/`owner-auth`). O Service Worker é escopado por origem + path — o design precisa garantir que o SW não vaze para os subdomínios de storefront.

Fase 2 traz Capacitor (empacotamento nativo). O design deve evitar retrabalho: registro do SW isolado e condicional.

## Goals / Non-Goals

**Goals:**
- Painel instalável em Android via `beforeinstallprompt` + CTA; instruções manuais em iOS.
- Manifest válido com marca/cores do design (`--acc` `#d6492f`, `--cream` `#f7f1e6`, "Comanda").
- Service Worker com precache de assets estáticos e navegação offline para telas já carregadas.
- Atualização de versão explícita (prompt), sem reload silencioso — coerente com "nenhum estado silencioso".
- Storefront garantidamente **sem** SW e sem instalação.
- Registro do SW isolado/condicional para não travar Capacitor na Fase 2.

**Non-Goals:**
- Empacotamento nativo, publicação em lojas (Fase 2).
- Push notifications, alerta sonoro nativo (Fase 2, dependem do nativo).
- Offline-first completo / sincronização offline de mutações (backlog — PRD Seção 3.2 "Painel offline-first completo").
- Tornar o storefront um PWA.
- Cache de respostas de API do painel (dados de pedido são polling em tempo quase real; precache de dados criaria estado obsoleto/ambíguo — fora de escopo).

## Decisions

### D1 — `vite-plugin-pwa` com Workbox, `injectManifest` vs `generateSW`
Usar `vite-plugin-pwa` (já nomeado na Seção 7.2 do PRD como "Vite PWA plugin"), estratégia **`generateSW`** (Workbox gera o SW a partir de config declarativa). Precache automático do `manifest` de build (JS/CSS/fontes/ícones) + runtime caching de navegação.
- **Por quê `generateSW` e não `injectManifest`:** o MVP precisa de precache + fallback de navegação offline, ambos cobertos pela config declarativa do Workbox. `injectManifest` (SW escrito à mão) só se paga quando há lógica de cache customizada — não é o caso agora. Menos código = menos superfície de bug (regra 3 e 6 do PRD).
- **Alternativa considerada:** SW artesanal sem plugin — rejeitado: reinventa precache/versionamento que o Workbox já resolve, e o PRD já elege o plugin.

### D2 — Escopo restrito ao painel (`scope`/`start_url`) + registro condicional
O manifest declara `scope` e `start_url` apontando para a **raiz das rotas do painel** (ex.: `/app/` ou o host do painel, conforme `foundations`/`owner-auth` fixarem). O registro do Service Worker no bootstrap do frontend é **condicional**: só executa quando a aplicação está rodando no contexto do painel do dono, nunca no storefront.
- **Por quê:** um único bundle serve os dois contextos; sem essa condicional, o SW registraria também nos subdomínios de storefront, violando o requisito "storefront sem PWA" e criando cache indevido no dispositivo do cliente.
- **Mecânica:** detectar contexto por rota/host no `main.tsx` (o mesmo discriminador que `foundations` usa para separar storefront × painel). Registrar o SW dentro de um módulo dedicado `pwa/registerSW.ts`, chamado apenas no branch do painel.
- **Alternativa considerada:** dois entrypoints/bundles Vite separados (um por contexto) — rejeitado no MVP: aumenta complexidade de build e o PRD fixa "mesmo bundle, separado por rota". A condicional resolve com custo mínimo.

### D3 — Estratégia de atualização: `registerType: 'prompt'`
Configurar o plugin com `registerType: 'prompt'` (não `autoUpdate`). Quando um novo SW é detectado, o app mostra um aviso "nova versão disponível" com ação de atualizar; o `skipWaiting`/reload só ocorre no clique do dono.
- **Por quê:** `autoUpdate` recarrega a página sozinho ao detectar nova versão — reload silencioso que pode descartar estado em andamento (ex.: motivo de cancelamento sendo digitado). Viola a regra 11 ("nenhum estado silencioso de falha") e a 4.2 ("nenhum estado ambíguo"). O prompt explícito respeita o dono.
- **Alternativa considerada:** `autoUpdate` — rejeitado pela regra de estado silencioso.

### D4 — O que cachear: assets estáticos sim, dados de API não
Precache: todo o build estático (JS, CSS, fontes Schibsted/Hanken/JetBrains, ícones). Runtime: fallback de navegação (`NavigationRoute`) para o app shell, permitindo abrir telas já visitadas offline. **Não** cachear respostas de API do painel (`/api/**`).
- **Por quê:** o painel de pedidos é polling 15s (PRD 4.2); cache de dados serviria pedidos obsoletos e criaria ambiguidade ("isso é o estado real?"). Offline aqui = "a interface abre", não "os dados estão frescos". Ao ficar offline, as chamadas de API falham e caem no indicador de conectividade + timestamp da última atualização já definido em `order-operation` — o caminho de erro correto, não uma tela em branco.
- **Alternativa considerada:** StaleWhileRevalidate em endpoints de leitura — rejeitado: risco de exibir pedido/estado desatualizado sem o dono perceber.

### D5 — Ícones derivados da marca
Gerar o conjunto de ícones (192, 512 em `any` + `maskable`, e `apple-touch-icon` 180) a partir da identidade do design: quadrado com fundo `--acc` `#d6492f` e a marca "Comanda" (as iniciais/marca já usadas no protótipo, ex.: bloco de acento + wordmark), splash `background_color` `--cream` `#f7f1e6`. Versão `maskable` com safe-zone (padding) para não ser cortada em ícones circulares do Android. Sem emoji (regra 13).
- **Por quê:** consistência com o design system aprovado e requisito de instalabilidade Android (maskable exigido para boa aparência).

### D6 — Servir manifest/SW pelo Spring Boot com headers corretos
O backend (que já serve o build estático) SHALL servir `manifest.webmanifest` com `Content-Type: application/manifest+json`, o `sw.js` com `Cache-Control: no-cache` (para o browser sempre revalidar o SW e detectar novas versões) e os assets hash-nomeados com cache longo. O SW SHALL ser servido do escopo do painel.
- **Por quê:** SW cacheado agressivamente nunca atualiza; assets com hash podem ser imutáveis. Regra padrão de PWA.

## Risks / Trade-offs

- **[SW vaza para storefront se a condicional falhar]** → registro isolado em módulo único chamado só no branch do painel (D2) + `scope` restrito no manifest (D2) + teste de aceitação que verifica ausência de SW no storefront. Duas barreiras independentes.
- **[iOS não suporta `beforeinstallprompt` nem instalação programática]** → iOS é "desejável", não obrigatório (PRD Seção 9). Entregar instrução manual de "Adicionar à Tela de Início"; não bloquear uso no navegador.
- **[Reload de atualização perde estado em andamento]** → mitigado por `registerType: 'prompt'` (D3): atualização só no clique do dono.
- **[Cache obsoleto de dados]** → evitado por design: só assets estáticos em cache, nunca API (D4).
- **[Retrabalho no Capacitor da Fase 2]** → registro do SW isolado/condicional (D2); em contexto nativo o registro é simplesmente não-invocado, sem tocar telas.
- **[Cache "preso" numa versão quebrada]** → `sw.js` servido com `no-cache` (D6) garante que o browser sempre busque o SW novo; Workbox versiona o precache por revisão de build.

## Migration Plan

1. Adicionar `vite-plugin-pwa` ao projeto frontend e configurar (`generateSW`, `registerType: 'prompt'`, `scope`/`start_url` do painel, `manifest` com cores/ícones do design, `navigateFallback` para o shell do painel, `denylist`/exclusão das rotas de storefront).
2. Gerar o conjunto de ícones (192/512 any+maskable, apple-touch-icon 180) a partir da marca.
3. Criar módulo `pwa/registerSW.ts` (registro isolado) + componente de prompt de atualização e componente de CTA de instalação (`beforeinstallprompt`); invocar registro **apenas** no branch do painel no bootstrap.
4. Ajustar o serving do backend (headers de `manifest`/`sw.js`).
5. Verificar: instalabilidade Android (Lighthouse/manifest válido + SW), abrir tela já carregada offline, storefront sem SW.
- **Rollback:** desabilitar o registro do SW (não invocar o módulo) e remover a referência ao manifest; um SW já instalado é neutralizado publicando um SW vazio (`self.skipWaiting()` + limpar caches) — considerar deixar preparado caso precise "desinstalar" o PWA de um dispositivo.

## Open Questions

- Host/path exato do painel (`/app` vs. host dedicado) — depende do que `foundations`/`owner-auth` fixarem no apply; o `scope`/`start_url` seguirão essa decisão.
- Preparar já um "kill-switch" SW (SW vazio) para rollback de emergência, ou deixar para quando/se necessário? (Recomendação: documentar o procedimento agora, implementar só se preciso — regra 3, sem otimização prematura.)
