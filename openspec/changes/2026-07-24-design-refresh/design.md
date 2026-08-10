## Context

O mesmo bundle React atende três contextos: landing no domínio raiz, storefront em subdomínio de tenant e painel em `/painel`. O trabalho parcial já contém landing, detecção de host, novos assets, alteração de tokens e build estático regenerado. Ele foi preservado no checkpoint `5e6f78e`, mas não equivale a implementação concluída.

Há uma divergência explícita. `.design/Comanda Landing.dc.html` e `.design/Comanda Painel.dc.html` usam `--acc: #b53c25`, `--acc-d: #9c351f` e `--ink3: #7d7263`; a spec consolidada `design-system` e a implementação em `main` usam, respectivamente, `#d6492f`, `#b53c25` e `#9a8f7e`.

## Goals / Non-Goals

**Goals:** landing fiel às referências aprovadas; seleção segura entre domínio raiz e tenant; tokens e marca consistentes; CTAs funcionais; conteúdo honesto; comportamento responsivo, acessível e compatível com movimento reduzido; validação automatizada e visual.

**Non-Goals:** Stripe, push nativo, recuperação de senha, features pós-MVP, correção funcional do checkout e a correção de PRD `Dados do negócio`.

## Decisions

### D1 — Um conjunto de tokens é autoritativo

Antes da implementação final, a equipe SHALL aprovar uma das duas paletas divergentes. A decisão permanece aberta nesta formalização; nenhuma fonte isolada é implicitamente promovida a definitiva. Depois da aprovação, o conjunto escolhido SHALL ser sincronizado atomicamente em `.design`, `comanda-client/src/index.css`, spec consolidada `design-system`, manifest PWA, tags runtime `theme-color` e ícones/assets de marca relevantes. Valores duplicados em estilos locais devem ser eliminados ou comprovadamente derivados da mesma fonte.

### D2 — Host decide a superfície da rota `/`

No domínio raiz, `www`, `app`, `localhost` e endereços de desenvolvimento explicitamente configurados, `/` renderiza a landing. Em um subdomínio de tenant válido, `/` renderiza o storefront. O backend continua sendo a autoridade para resolver e isolar o tenant; a detecção frontend apenas escolhe a superfície e não concede acesso. O escape de desenvolvimento por query string, se mantido, deve ser limitado ao ambiente de desenvolvimento e testado.

### D3 — Comunicação limitada ao MVP comprovado

Polling deve ser descrito como atualização periódica ou “quase em tempo real” com qualificação, nunca como tempo real. A landing não promete notificação automática. O fluxo só pode afirmar que o pedido foi registrado/chegou ao painel após persistência confirmada; se a criação falhar, deve apresentar erro/fallback coerente. O defeito atual de confiabilidade do checkout é bloqueador de release desta comunicação, embora sua correção pertença a outra change.

### D4 — Assets aprovados e build reproduzível

Logo e screenshots aprovados ficam versionados na fonte pública. Como o repositório intencionalmente entrega o build Vite pelos recursos estáticos do Spring Boot, o build hashado correspondente também é versionado quando regenerado. A aceitação exige build limpo e referências do `index.html`/Service Worker apontando somente para assets presentes.

### D5 — Acessibilidade e consistência visual são requisitos

Todos os CTAs devem usar destinos reais e semântica adequada; navegação por teclado, foco visível, texto alternativo, contraste, reflow responsivo e `prefers-reduced-motion` devem ser verificados. Status continuam combinando cor semântica com rótulo textual, usando a mesma terminologia entre lista, detalhe e stepper.

## Risks / Trade-offs

- Detecção genérica de subdomínio pode classificar hosts de preview como tenant; mitigar com configuração explícita e matriz de testes.
- A landing parcial contém alegações ainda não comprovadas; revisar copy antes de release.
- Duplicar screenshots na fonte e no build aumenta o repositório, mas mantém o artefato Spring Boot reproduzível segundo a convenção atual.
- Trocar tokens afeta landing, painel, PWA e assets; a atualização deve ser atômica para evitar uma marca inconsistente.

## Migration Plan

1. Aprovar a paleta autoritativa e registrar a decisão nesta change.
2. Reconciliar `.design`, specs e implementação; concluir landing e roteamento com testes.
3. Corrigir copy/CTAs, acessibilidade e responsividade.
4. Regenerar o build estático e executar a matriz completa de validação.
5. Liberar somente após tratar o bloqueador de checkout ou remover toda comunicação contraditória.

## Open Questions

- Qual paleta será autoritativa: a atual de `.design` (`#b53c25/#9c351f/#7d7263`) ou a consolidada (`#d6492f/#b53c25/#9a8f7e`)?
- Quais hosts de produção, preview e desenvolvimento pertencem explicitamente ao domínio raiz, e o parâmetro `?storefront` deve existir fora de desenvolvimento?
- Quais screenshots e alegações da landing representam comportamento já verificado do MVP?
- Qual change corrigirá o bloqueador de checkout antes da aprovação de release?
