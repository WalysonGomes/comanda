# Rollback / Kill-Switch do PWA do Painel

> Procedimento operacional correspondente à especificação consolidada `owner-pwa`. Ele reflete
> a implementação atual, mas ainda requer validação em produção depois que a infraestrutura
> definitiva existir; não constitui evidência de um rollback de produção já testado.

## Implementação atual

- O Workbox gerado por `vite-plugin-pwa` é publicado em `/sw.js`.
- O registro usa escopo `/painel/`, e o fallback de navegação aceita apenas `/painel/**`.
- O módulo dedicado é `comanda-client/src/pwa/registerSW.ts`; `main.tsx` chama
  `registerPainelServiceWorker()` somente para rotas do painel.
- O Workbox precacheia JS, CSS, HTML, imagens, manifestos e fontes do build. A estratégia
  `registerType: 'prompt'` informa que existe atualização e só a aplica após a ação do dono.

## Quando usar

- Um build continua servindo um app shell quebrado a partir do precache.
- O Service Worker precisa ser desativado temporariamente sem depender da desinstalação manual
  em cada dispositivo.

## Procedimento de kill-switch

1. Preparar um `sw.js` auto-destrutivo para substituir temporariamente o arquivo gerado:

   ```js
   self.addEventListener('install', () => self.skipWaiting())

   self.addEventListener('activate', (event) => {
     event.waitUntil(
       caches.keys()
         .then((keys) => Promise.all(keys.map((key) => caches.delete(key))))
         .then(() => self.registration.unregister())
         .then(() => self.clients.matchAll({ type: 'window' }))
         .then((clients) => clients.forEach((client) => client.navigate(client.url))),
     )
   })
   ```

2. Publicar esse arquivo exatamente em `/sw.js`, preservando a possibilidade de atualização do
   registro existente. Confirmar que a resposta não fica presa em cache HTTP.
3. Para impedir novos registros, desabilitar temporariamente a chamada condicional a
   `registerPainelServiceWorker()` em `comanda-client/src/main.tsx`. Isso, isoladamente, não remove
   workers já instalados; esses dispositivos ainda precisam receber o kill-switch.
4. Quando um dispositivo com conexão atualizar o worker, ele deve limpar os caches, cancelar seu
   próprio registro e recarregar os clientes. O painel passa a depender da rede.

## Limpeza manual de recuperação

Se um dispositivo não receber o kill-switch, usar DevTools → Application → Service Workers para
cancelar o registro de `/painel/` e Application → Storage para limpar Cache Storage e dados do
site. Recarregar com a rede ativa.

## Verificação de recuperação

1. Confirmar que não existe worker ativo para o escopo `/painel/` e que os caches Workbox foram
   removidos.
2. Abrir diretamente mais de uma rota do painel com rede ativa e confirmar que HTML, JS e CSS
   atuais vieram do servidor, sem resposta de um Service Worker.
3. Confirmar que login, carregamento do painel e um fluxo representativo funcionam.
4. Depois de corrigida a causa, restaurar o build normal do Workbox e a chamada de registro,
   verificar a instalação no escopo `/painel/`, o precache, o aviso de nova versão e o
   comportamento offline esperado.
5. Registrar o resultado da validação de produção quando ela puder ser executada.
