## 1. Dependência e configuração do plugin

- [ ] 1.1 Adicionar `vite-plugin-pwa` como devDependency do frontend
- [ ] 1.2 Configurar o plugin no `vite.config.ts`: estratégia `generateSW`, `registerType: 'prompt'`, `injectRegister: null` (registro manual isolado)
- [ ] 1.3 Definir `scope` e `start_url` do manifest para a raiz das rotas do painel (não o storefront); confirmar host/path com `foundations`/`owner-auth`
- [ ] 1.4 Configurar `workbox.navigateFallback` para o app shell do painel e excluir (`navigateFallbackDenylist`) as rotas/host do storefront
- [ ] 1.5 Configurar `globPatterns` de precache para incluir JS, CSS, fontes e ícones do build

## 2. Manifest e ícones (design)

- [ ] 2.1 Gerar ícones do PWA a partir da marca "Comanda": fundo `--acc` `#d6492f`, sem emoji — tamanhos 192×192 e 512×512 em `purpose: any`
- [ ] 2.2 Gerar variantes `maskable` (192/512) com safe-zone/padding para ícones circulares do Android
- [ ] 2.3 Gerar `apple-touch-icon` 180×180 para iOS
- [ ] 2.4 Preencher o `manifest` no plugin: `name`/`short_name` "Comanda", `display: standalone`, `orientation: portrait`, `theme_color: #d6492f`, `background_color: #f7f1e6`, lista de ícones (any + maskable + apple-touch)

## 3. Registro do Service Worker (isolado e condicional)

- [ ] 3.1 Criar módulo dedicado `pwa/registerSW.ts` que encapsula `registerSW` do virtual module do plugin
- [ ] 3.2 No bootstrap (`main.tsx`), detectar contexto (painel × storefront) usando o mesmo discriminador de rota/host de `foundations`
- [ ] 3.3 Invocar o registro do SW **apenas** no branch do painel; garantir que o storefront nunca registra SW
- [ ] 3.4 Expor o registro de forma que o desabilitar (contexto Capacitor futuro) não exija tocar telas do painel

## 4. Fluxo de instalação

- [ ] 4.1 Capturar o evento `beforeinstallprompt` (Android) e guardar o deferred prompt
- [ ] 4.2 Exibir CTA de "instalar app" no painel quando o prompt estiver disponível; disparar o prompt no clique e limpar o estado após resultado
- [ ] 4.3 Detectar iOS Safari e exibir instrução manual de "Adicionar à Tela de Início" (sem CTA programático)
- [ ] 4.4 Ocultar CTA quando o app já está em modo `standalone`/instalado (`display-mode: standalone`)

## 5. Atualização de versão (sem estado silencioso)

- [ ] 5.1 Ligar o callback `onNeedRefresh` do `registerSW` a um componente de aviso "nova versão disponível" com ação de atualizar
- [ ] 5.2 Aplicar a atualização (`updateSW`) apenas no clique do dono — sem reload silencioso
- [ ] 5.3 Tratar `onOfflineReady` com feedback discreto (opcional), sem bloquear a UI

## 6. Serving pelo backend

- [ ] 6.1 Servir `manifest.webmanifest` com `Content-Type: application/manifest+json`
- [ ] 6.2 Servir `sw.js`/workbox com `Cache-Control: no-cache` para revalidação; assets hash-nomeados com cache longo/imutável
- [ ] 6.3 Garantir que o SW seja servido a partir do escopo do painel (path/host correto)

## 7. Verificação (critérios de lançamento)

- [ ] 7.1 Validar manifest + instalabilidade no Android (Lighthouse PWA / DevTools Application: manifest válido, SW registrado, `start_url` no escopo do painel)
- [ ] 7.2 Instalar no Android e confirmar abertura em modo `standalone` a partir do ícone
- [ ] 7.3 Carregar o painel, ficar offline e confirmar que uma tela já visitada ainda renderiza; chamadas de API caem no indicador de conectividade (nunca falha silenciosa)
- [ ] 7.4 Confirmar que o storefront (`nomedonegocio.${APP_DOMAIN}`) NÃO registra SW e NÃO oferece instalação
- [ ] 7.5 Confirmar prompt de "nova versão" ao publicar novo build (sem reload silencioso)
- [ ] 7.6 (Desejável) Confirmar "Adicionar à Tela de Início" em iOS Safari
- [ ] 7.7 Documentar procedimento de rollback/kill-switch (SW vazio que limpa caches) no README/operacional
