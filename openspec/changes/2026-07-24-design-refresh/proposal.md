## Why

As referências aprovadas em `.design` e a implementação consolidada divergiram em cores, marca e comportamento da raiz do domínio. A change formaliza o trabalho parcial já existente para que a landing institucional, o storefront por tenant e o PWA evoluam a partir de uma única decisão verificável, sem promessas que o MVP ainda não sustenta.

## What Changes

- Adiciona uma landing page responsiva e acessível na raiz do domínio da aplicação.
- Mantém o storefront na raiz quando o host representa um subdomínio de tenant e documenta a detecção de domínio/host.
- Mantém `.design` como referência visual local e não rastreada, versionando somente decisões, arquivos de marca e assets derivados aprovados em caminhos pertencentes à aplicação.
- Adota a paleta local aprovada e exige sincronização entre CSS, specs, manifest, tags `theme-color`, ícones e demais assets de marca.
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
- Design e especificação: transferência das decisões aprovadas da referência local `.design` para specs e implementação reproduzíveis sem essa pasta.
- Fora do escopo: Stripe, push nativo, recuperação de senha, recursos pós-MVP e novas features de negócio.

`Dados do negócio` continua fora do escopo desta implementação e será concluído diretamente como correção do PRD/spec consolidada após o design refresh. O defeito de confiabilidade do checkout também será corrigido diretamente depois, usando o PRD e as specs consolidadas como autoridade, e permanece bloqueador de release: a comunicação não pode dizer ou insinuar que um pedido chegou ao painel quando a persistência falhar.
