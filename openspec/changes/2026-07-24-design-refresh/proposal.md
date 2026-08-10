## Why

As referências aprovadas em `.design` e a implementação consolidada divergiram em cores, marca e comportamento da raiz do domínio. A change formaliza o trabalho parcial já existente para que a landing institucional, o storefront por tenant e o PWA evoluam a partir de uma única decisão verificável, sem promessas que o MVP ainda não sustenta.

## What Changes

- Adiciona uma landing page responsiva e acessível na raiz do domínio da aplicação.
- Mantém o storefront na raiz quando o host representa um subdomínio de tenant e documenta a detecção de domínio/host.
- Versiona os arquivos de marca e as referências visuais aprovadas usados pela landing.
- Escolhe um conjunto autoritativo de tokens e exige sincronização entre `.design`, CSS, specs, manifest, tags `theme-color`, ícones e demais assets de marca.
- Alinha cores/rótulos de status já tocados, mantendo texto além de cor.
- Requer consistência do PWA, testes de roteamento/landing, build, lint, testes e aceitação visual.
- Corrige a comunicação da landing para descrever polling como atualização periódica, não prometer notificações inexistentes e não afirmar persistência quando a criação do pedido falhar.

## Capabilities

### New Capabilities
- `marketing-landing`: landing institucional no domínio raiz, seus CTAs, conteúdo, responsividade, acessibilidade e aceitação visual.

### Modified Capabilities
- `design-system`: define tokens autoritativos, sincronização das fontes de design/implementação e separação por host entre landing e storefront.
- `owner-pwa`: deriva manifest, tema e assets do mesmo conjunto autoritativo de tokens.

## Impact

- Frontend React/Vite: landing, roteamento por host, estilos/tokens, rótulos já tocados e testes.
- Build estático servido pelo Spring Boot: assets hashados, logo, screenshots, manifest, Service Worker e tags de tema coerentes.
- Design e especificação: reconciliação de `.design`, specs consolidadas e implementação.
- Fora do escopo: Stripe, push nativo, recuperação de senha, recursos pós-MVP e novas features de negócio.

`Dados do negócio` continua sendo uma correção separada do PRD e não será declarado concluído por esta change. O defeito de confiabilidade do checkout permanece fora da implementação da landing, mas é bloqueador de release: a comunicação não pode dizer ou insinuar que um pedido chegou ao painel quando a persistência falhar.
