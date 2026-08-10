# owner-pwa Specification

## Purpose
TBD - created by archiving change pwa. Update Purpose after archive.
## Requirements
### Requirement: Painel do dono instalável como PWA

O painel do dono SHALL ser instalável como Progressive Web App em Android (obrigatório) e SHALL prover suporte de instalação em iOS quando o sistema operacional permitir (desejável). O sistema SHALL servir um Web App Manifest válido, referenciado pelas páginas do painel, contendo no mínimo: `name` "Comanda", `short_name` "Comanda", `start_url` e `scope` restritos às rotas do painel, `display: standalone`, `orientation: portrait`, `theme_color` e `background_color`, e um conjunto de ícones cobrindo os propósitos `any` e `maskable` nos tamanhos 192×192 e 512×512 (mais `apple-touch-icon` 180×180 para iOS).

#### Scenario: Manifest válido servido no painel

- **WHEN** o navegador carrega qualquer rota autenticada do painel do dono
- **THEN** um `manifest.webmanifest` válido é referenciado e servido com `Content-Type: application/manifest+json`
- **AND** o manifest declara `name` "Comanda", `display` `standalone`, `orientation` `portrait`, e ícones `192×192` e `512×512` incluindo ao menos um com `purpose` `maskable`

#### Scenario: Instalação disponível em Android

- **WHEN** o dono acessa o painel em um navegador Android compatível que dispara `beforeinstallprompt` e os critérios de instalabilidade estão satisfeitos (manifest válido + Service Worker registrado + HTTPS)
- **THEN** o painel exibe uma ação de "instalar" (CTA) que dispara o prompt de instalação nativo
- **AND** ao concluir a instalação o painel abre em modo `standalone` (sem barra de navegador) a partir do ícone na tela inicial

#### Scenario: Instrução de instalação em iOS

- **WHEN** o dono acessa o painel em iOS Safari (que não expõe `beforeinstallprompt`)
- **THEN** o painel oferece instruções manuais de "Adicionar à Tela de Início" via menu de compartilhamento
- **AND** a ausência de instalação automática não impede o uso normal do painel no navegador

### Requirement: Aparência do PWA derivada do design aprovado
O manifest, as tags runtime `theme-color` e os ícones do PWA SHALL usar a identidade visual e o conjunto autoritativo aprovado: `theme_color`, cor de destaque dos ícones/assets e `--acc` SHALL usar `#b53c25`; `background_color` SHALL usar `--cream #f7f1e6`. Os ícones NÃO SHALL conter emojis. A geração SHALL ser reproduzível sem a referência local não rastreada `.design`.

#### Scenario: Cores e marca do manifest estão sincronizadas
- **WHEN** o PWA é instalado e a splash/ícone são exibidos pelo sistema
- **THEN** `theme_color`, tags runtime e cor de destaque da marca correspondem ao `--acc` autoritativo
- **AND** a `background_color` corresponde ao `--cream` autoritativo
- **AND** o ícone exibe a marca Comanda sem emojis

#### Scenario: Cores e marca do manifest
- **WHEN** o PWA é instalado e a splash/ícone são exibidos pelo sistema
- **THEN** a `background_color` da splash corresponde ao `--cream` autoritativo e o `theme_color` corresponde ao `--acc` autoritativo
- **AND** o ícone exibe a marca "Comanda" sobre a cor de destaque, sem emojis

### Requirement: Service Worker com precache e uso offline de telas carregadas

O sistema SHALL registrar um Service Worker (via `vite-plugin-pwa` / Workbox) que faz precache dos assets estáticos do build (JS, CSS, fontes e ícones) e SHALL permitir que telas do painel já carregadas continuem abrindo quando o dispositivo está offline. O registro do Service Worker SHALL ocorrer apenas para o escopo do painel.

#### Scenario: Assets estáticos em precache

- **WHEN** o painel é carregado pela primeira vez com conexão
- **THEN** o Service Worker é instalado e faz precache dos assets estáticos do build
- **AND** os assets subsequentes são servidos a partir do cache do Service Worker

#### Scenario: Tela já carregada abre offline

- **WHEN** o dono já carregou o shell do painel e o dispositivo perde conexão
- **THEN** navegar de volta ao painel (ou entre telas já visitadas) ainda renderiza a interface a partir do cache
- **AND** operações que dependem da rede exibem o estado de falha/conectividade já definido pelo painel (nunca falha silenciosa)

### Requirement: Atualização de versão sem estado silencioso

Quando uma nova versão do painel for publicada, o sistema SHALL detectar a atualização do Service Worker e SHALL informar o dono com uma ação explícita para recarregar/atualizar, em vez de recarregar silenciosamente ou servir versão obsoleta sem aviso (regra 11 do PRD — nenhum estado silencioso de falha).

#### Scenario: Nova versão disponível

- **WHEN** um novo Service Worker é detectado (nova build publicada) enquanto o dono usa o painel
- **THEN** o painel exibe um aviso de "nova versão disponível" com ação para atualizar
- **AND** a atualização só é aplicada quando o dono confirma (sem reload silencioso que perca estado em andamento)

### Requirement: Storefront do cliente permanece sem PWA

O storefront público do cliente NÃO SHALL registrar Service Worker, NÃO SHALL referenciar o manifest do painel e NÃO SHALL oferecer instalação. O PWA é exclusivo do painel do dono; o cliente usa o cardápio no navegador sem instalação.

#### Scenario: Storefront não registra Service Worker

- **WHEN** um cliente acessa o cardápio público em `nomedonegocio.${APP_DOMAIN}`
- **THEN** nenhum Service Worker do painel é registrado e nenhum prompt/CTA de instalação é exibido
- **AND** o `scope` do PWA restringe o Service Worker às rotas do painel, não alcançando o storefront

### Requirement: Base estruturada para empacotamento nativo futuro

A estrutura do PWA (registro do Service Worker, manifest e detecção de ambiente de execução) SHALL ser organizada de forma a não exigir reescrita quando o empacotamento nativo (Capacitor) entrar na Fase 2, mantendo o registro do Service Worker isolado e condicional.

#### Scenario: Registro isolado e condicional

- **WHEN** o código de bootstrap do frontend inicializa
- **THEN** o registro do Service Worker está isolado em um módulo dedicado e é condicional ao ambiente (painel web)
- **AND** desabilitar o registro (ex.: em contexto de empacotamento nativo) não requer alterar as telas do painel
