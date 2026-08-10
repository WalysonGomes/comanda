## MODIFIED Requirements

### Requirement: Approved design tokens applied to the theme
O frontend SHALL aplicar um único conjunto de tokens aprovado como tema via CSS variables/config do Tailwind: `--acc #b53c25`, `--acc-d #9c351f`, `--ink3 #7d7263`, `--acc-tint #fbe6df`, `--ink #2a2320`, `--ink2 #6f6557`, `--line #e8ddcc`, `--cream #f7f1e6`, `--card #fffdf9`, `--green #1f8a52` e `--green-tint #e2f0e4`. O conjunto SHALL ser sincronizado em `index.css`, nesta spec consolidada quando a change for arquivada, manifest PWA, tags runtime `theme-color` e ícones/assets de marca relevantes. `.design` SHALL permanecer uma referência local não rastreada; CI e builds limpos SHALL funcionar sem ela. Schibsted Grotesk, Hanken Grotesk e JetBrains Mono SHALL manter seus papéis; somente modo claro.

#### Scenario: Tema expõe um conjunto autoritativo
- **WHEN** um componente, manifest, tag de tema ou asset de marca referencia os tokens
- **THEN** os valores resolvem para o mesmo conjunto explicitamente aprovado
- **AND** as três famílias de fonte estão disponíveis com seus papéis

#### Scenario: Animações nomeadas disponíveis como utilitários
- **WHEN** um componente precisa das microinterações do design
- **THEN** as animações `cmd-up`, `cmd-fade`, `cmd-pop`, `cmd-check`, `cmd-pulse` e `cmd-spin` estão disponíveis como utilitários reutilizáveis

### Requirement: Routing separates public storefront from owner panel
O frontend SHALL, no mesmo bundle SPA, separar por host e rota a landing institucional no domínio raiz, o storefront público em subdomínio de tenant e o painel do dono em suas rotas autenticadas. A seleção frontend de superfície NÃO SHALL substituir a resolução e o isolamento de tenant feitos pelo servidor.

#### Scenario: Domínio raiz carrega a landing
- **WHEN** o usuário acessa `/` no domínio raiz ou alias reservado configurado
- **THEN** a SPA renderiza a landing institucional

#### Scenario: Subdomínio de tenant carrega o storefront
- **WHEN** o usuário acessa `/` em um subdomínio de tenant
- **THEN** a SPA renderiza a superfície de storefront

#### Scenario: Rota do painel carrega o shell autenticado
- **WHEN** o usuário acessa uma rota do painel do dono
- **THEN** a SPA renderiza a superfície do painel
- **AND** as três superfícies vivem no mesmo bundle

## ADDED Requirements

### Requirement: Status labels remain visually and semantically consistent
Estados de pedido já tocados por esta change SHALL usar terminologia consistente entre filtros, cards, detalhe e stepper, com cor semântica acompanhada de rótulo textual e contraste adequado.

#### Scenario: Status aparece em superfícies diferentes
- **WHEN** o mesmo status é exibido na lista, detalhe ou stepper
- **THEN** o rótulo e a cor semântica são consistentes
- **AND** o significado nunca depende somente de cor
