## 1. Dependência e configuração do plugin

- [x] 1.1 Adicionar `vite-plugin-pwa` como devDependency do frontend
- [x] 1.2 Configurar o plugin no `vite.config.ts`: estratégia `generateSW`, `registerType: 'prompt'`, `injectRegister: null` (registro manual isolado)
- [x] 1.3 Definir `scope` e `start_url` do manifest para a raiz das rotas do painel (não o storefront); confirmar host/path com `foundations`/`owner-auth`
- [x] 1.4 Configurar `workbox.navigateFallback` para o app shell do painel e excluir (`navigateFallbackDenylist`) as rotas/host do storefront
- [x] 1.5 Configurar `globPatterns` de precache para incluir JS, CSS, fontes e ícones do build

## 2. Manifest e ícones (design)

- [x] 2.1 Gerar ícones do PWA a partir da marca "Comanda": fundo `--acc` `#d6492f`, sem emoji — tamanhos 192×192 e 512×512 em `purpose: any`
- [x] 2.2 Gerar variantes `maskable` (192/512) com safe-zone/padding para ícones circulares do Android
- [x] 2.3 Gerar `apple-touch-icon` 180×180 para iOS
- [x] 2.4 Preencher o `manifest` no plugin: `name`/`short_name` "Comanda", `display: standalone`, `orientation: portrait`, `theme_color: #d6492f`, `background_color: #f7f1e6`, lista de ícones (any + maskable + apple-touch)

## 3. Registro do Service Worker (isolado e condicional)

- [x] 3.1 Criar módulo dedicado `pwa/registerSW.ts` que encapsula `registerSW` do virtual module do plugin
- [x] 3.2 No bootstrap (`main.tsx`), detectar contexto (painel × storefront) usando o mesmo discriminador de rota/host de `foundations`
- [x] 3.3 Invocar o registro do SW **apenas** no branch do painel; garantir que o storefront nunca registra SW
- [x] 3.4 Expor o registro de forma que o desabilitar (contexto Capacitor futuro) não exija tocar telas do painel

## 4. Fluxo de instalação

- [x] 4.1 Capturar o evento `beforeinstallprompt` (Android) e guardar o deferred prompt
- [x] 4.2 Exibir CTA de "instalar app" no painel quando o prompt estiver disponível; disparar o prompt no clique e limpar o estado após resultado
- [x] 4.3 Detectar iOS Safari e exibir instrução manual de "Adicionar à Tela de Início" (sem CTA programático)
- [x] 4.4 Ocultar CTA quando o app já está em modo `standalone`/instalado (`display-mode: standalone`)

## 5. Atualização de versão (sem estado silencioso)

- [x] 5.1 Ligar o callback `onNeedRefresh` do `registerSW` a um componente de aviso "nova versão disponível" com ação de atualizar
- [x] 5.2 Aplicar a atualização (`updateSW`) apenas no clique do dono — sem reload silencioso
- [x] 5.3 Tratar `onOfflineReady` com feedback discreto (opcional), sem bloquear a UI

## 6. Serving pelo backend

- [x] 6.1 Servir `manifest.webmanifest` com `Content-Type: application/manifest+json`
- [x] 6.2 Servir `sw.js`/workbox com `Cache-Control: no-cache` para revalidação; assets hash-nomeados com cache longo/imutável
- [x] 6.3 Garantir que o SW seja servido a partir do escopo do painel (path/host correto)

## 7. Verificação (critérios de lançamento)

- [x] 7.1 Validar manifest + instalabilidade no Android — verificado via build real + Chromium automatizado (Playwright): manifest válido servido com `application/manifest+json`, SW registrado e `active`, `start_url`/`scope` restritos a `/painel/`. Lighthouse completo/dispositivo Android real não disponível neste ambiente.
- [ ] 7.2 Instalar no Android e confirmar abertura em modo `standalone` a partir do ícone — requer dispositivo/navegador Android real, não disponível neste ambiente de execução.
- [x] 7.3 Carregar o painel, ficar offline e confirmar que uma tela já visitada ainda renderiza; chamadas de API caem no indicador de conectividade (nunca falha silenciosa) — verificado automaticamente (Chromium headless: revisita a tela, offline, reload, root renderiza a partir do precache).
- [x] 7.4 Confirmar que o storefront (`nomedonegocio.${APP_DOMAIN}`) NÃO registra SW e NÃO oferece instalação — verificado automaticamente (Chromium: zero registrations, nenhum `<link rel="manifest">` na storefront).
- [ ] 7.5 Confirmar prompt de "nova versão" ao publicar novo build (sem reload silencioso) — fluxo implementado e coberto por D3/`onNeedRefresh` → `UpdateBanner`; ciclo completo de "publicar 2º build e observar o prompt" não executado neste ambiente.
- [ ] 7.6 (Desejável) Confirmar "Adicionar à Tela de Início" em iOS Safari — requer dispositivo iOS real, não disponível neste ambiente.
- [x] 7.7 Documentar procedimento de rollback/kill-switch (SW vazio que limpa caches) no README/operacional — `docs/pwa-rollback.md`
