# Rollback / Kill-Switch do PWA do Painel

> Cobre a change `pwa` (spec `owner-pwa`), task 7.7 e o risco "cache preso numa versão quebrada"
> descrito em `openspec/changes/pwa/design.md`. Procedimento operacional para desativar o
> Service Worker/manifest do painel sem depender de reinstalação manual em cada dispositivo.

## Quando usar

- Um build do painel ficou preso oferecendo uma versão quebrada mesmo depois de novos deploys
  (o Service Worker antigo continua servindo o app shell antigo do precache).
- É preciso desativar o PWA do painel temporariamente (ex.: bug no próprio Service Worker) sem
  esperar o dono desinstalar o app manualmente em cada celular.

## Por que funciona sem acesso ao dispositivo

`/sw.js` é servido com `Cache-Control: no-cache` (`WebMvcConfig`, D6) — o navegador **sempre**
revalida esse arquivo no servidor antes de usar a versão em cache. Publicar um novo `sw.js` no
próximo deploy é suficiente para que todo dispositivo que abrir o painel (mesmo offline-first)
receba o novo Service Worker na próxima vez em que houver conexão.

## Passo a passo

1. Substituir o conteúdo gerado pelo Workbox por um Service Worker "auto-destrutivo", que limpa
   os caches do painel e se desregistra:

   ```js
   // comanda-client/public/sw-killswitch.js (copiar para sw.js no build, ou apontar o build
   // para este arquivo temporariamente)
   self.addEventListener('install', () => {
     self.skipWaiting()
   })

   self.addEventListener('activate', (event) => {
     event.waitUntil(
       caches.keys()
         .then((keys) => Promise.all(keys.map((key) => caches.delete(key))))
         .then(() => self.registration.unregister())
         .then(() => self.clients.matchAll())
         .then((clients) => clients.forEach((client) => client.navigate(client.url))),
     )
   })
   ```

2. Fazer deploy normalmente (o arquivo cai no lugar de `/sw.js`, mesmo escopo `/painel/`).
3. Cada dispositivo que abrir o painel com conexão recebe esse SW, limpa o cache antigo, se
   desregistra e recarrega — o painel volta a rodar 100% de rede, sem Service Worker, até o
   próximo deploy normal (com o Workbox de novo) reativar o PWA.
4. Confirmar em produção: abrir o painel, checar em DevTools → Application → Service Workers que
   nenhum worker ativo permanece registrado para `/painel/`.

## Desativar o registro na origem (sem esperar o próximo deploy alcançar todo mundo)

Se o problema for grave o suficiente para não esperar a propagação do kill-switch, comentar a
chamada a `registerPainelServiceWorker()` em `comanda-client/src/main.tsx` e publicar — novos
carregamentos do painel deixam de registrar SW novo, mas dispositivos que já têm um Service
Worker ativo só o perdem depois de passar pelo kill-switch acima (o registro comentado apenas
impede *novos* registros).
