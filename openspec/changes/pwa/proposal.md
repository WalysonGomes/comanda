## Why

O PRD (Seção 3.1 → "PWA — painel do dono" e Seção 6 → "PWA instalável" no plano Gratuito) exige que o **painel do dono** seja entregue como Progressive Web App instalável, sem publicação em lojas. O dono opera do celular durante o pico; um app na tela inicial (ícone, tela cheia, telas já carregadas funcionando offline) é o que resolve 100% da necessidade de "app instalável" do MVP e é critério de lançamento explícito ("PWA instalável funciona no celular do dono — Android obrigatório; iOS desejável", Seção 9). O empacotamento nativo (Capacitor) é Fase 2 (Seção 11) — esta change deve deixar o terreno pronto para ele sem criar retrabalho.

## What Changes

Nenhuma feature de produto nova e nenhuma tela de negócio nova — esta change torna o **painel do dono** (rotas autenticadas já construídas em `order-operation`) instalável e resiliente.

- **Web App Manifest** do painel: `name` "Comanda", `short_name` "Comanda", ícones (maskable + any) nos tamanhos exigidos, `theme_color`/`background_color` derivados dos tokens do design (`--acc` `#d6492f`, `--cream` `#f7f1e6`), `display: standalone`, `orientation: portrait`, `start_url`/`scope` restritos ao painel (não ao storefront).
- **Service Worker básico** via Vite PWA plugin (`vite-plugin-pwa` + Workbox): precache dos assets estáticos do build (JS/CSS/fontes/ícones) e cache de navegação para as **telas já carregadas** funcionarem offline. Estratégia de update controlada (prompt de "nova versão disponível", sem auto-reload silencioso — coerente com a regra "nenhum estado silencioso").
- **"Adicionar à tela inicial"**: funcional em Android (obrigatório) — captura do evento `beforeinstallprompt` com CTA de instalação no painel; instruções manuais para iOS (desejável), já que iOS não expõe prompt programático.
- **Escopo do PWA restrito ao painel**: o storefront do cliente **não** é PWA e **não** deve registrar Service Worker nem oferecer instalação (o cliente usa no navegador, sem instalação).
- **Base para Capacitor (Fase 2)**: estruturar registro do SW, manifest e detecção de ambiente de forma que o empacotamento nativo futuro não exija reescrita (documentado em design.md).

## Capabilities

### New Capabilities
- `owner-pwa`: o painel do dono como Progressive Web App instalável — Web App Manifest (ícone/nome/cores/orientação), Service Worker com precache de assets e uso offline de telas já carregadas, fluxo de instalação em Android (iOS desejável), atualização de versão sem estado silencioso, e a garantia de que o storefront do cliente permanece sem PWA/instalação. Estruturado para não gerar retrabalho no empacotamento nativo (Capacitor) da Fase 2.

### Modified Capabilities
<!-- Nenhuma delta MODIFIED. As specs de order-operation e das demais changes de feature ainda não
     estão arquivadas em openspec/specs/ no momento do propose (o workflow propõe todas as 9 changes
     com baseline vazio e só implementa+arquiva na ordem 1→9). Modificar um requisito sem baseline
     arquivado quebraria `openspec validate --strict`. Seguindo a precedência de order-operation,
     plans-and-onboarding e reliability-and-security, os requisitos de PWA entram como capability NOVA.
     A dependência sobre o painel pronto (rotas autenticadas de order-operation) é descrita em prosa e
     satisfeita no apply, que ocorre após arquivar as changes anteriores. -->

## Impact

- **Frontend (React + Vite):** adição de `vite-plugin-pwa` (já previsto na Seção 7.2 do PRD como "Vite PWA plugin"); geração do manifest e do Service Worker no build; registro condicional do SW **apenas** nas rotas do painel (não no storefront); componente de CTA de instalação (`beforeinstallprompt`) e componente de prompt de atualização de versão; conjunto de ícones PWA (maskable + any: 192, 512 e apple-touch-icon 180) derivados da marca (quadrado com `--acc` de fundo e as iniciais/marca "Comanda"). Nenhuma dependência nova além do plugin PWA.
- **Backend (Spring Boot):** servir `manifest.webmanifest`, `sw.js`/workbox e ícones como parte do build estático da SPA, com `Content-Type` e cache corretos; garantir que o Service Worker seja servido do escopo do painel. Sem mudança de domínio/negócio.
- **Design:** ícone/nome/cores do manifest derivados de `.design/Comanda Painel.dc.html` (`--acc` `#d6492f`, `--cream` `#f7f1e6`, marca "Comanda"); sem nova tela de negócio.
- **Testes (critério de lançamento):** verificação de instalabilidade no Android (manifest válido + SW registrado + `start_url` no escopo do painel); verificação de que telas já carregadas do painel abrem offline; verificação de que o storefront **não** registra SW nem oferece instalação.
- **Dependências:** depende do **painel de operação** (`order-operation`) já implementado — é ele que define as rotas autenticadas do painel que o PWA envolve. Escopo alinhado com `foundations` (separação storefront/painel no mesmo bundle por rota). Fora de escopo: empacotamento nativo/lojas, push notifications e alerta sonoro (Fase 2), e transformar o storefront em PWA.
