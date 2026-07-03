# design-system Specification

## Purpose
TBD - created by archiving change foundations. Update Purpose after archive.
## Requirements
### Requirement: Approved design tokens applied to the theme
O frontend SHALL aplicar os tokens do design system aprovado (derivados do `.cmd-root` de `.design/Comanda Painel.dc.html`) como tema via CSS variables / config do Tailwind: acento `--acc #d6492f`, `--acc-d #b53c25`, `--acc-tint #fbe6df`; texto `--ink #2a2320`, `--ink2 #6f6557`, `--ink3 #9a8f7e`; `--line #e8ddcc`; fundo `--cream #f7f1e6`; superfície `--card #fffdf9`. As três fontes SHALL ser configuradas: Schibsted Grotesk (títulos/marca), Hanken Grotesk (interface), JetBrains Mono (dados). Somente modo claro.

#### Scenario: Tema expõe os tokens do design
- **WHEN** um componente do frontend referencia cor, fonte ou superfície do tema
- **THEN** os valores resolvem para os tokens aprovados (acento `#d6492f`, creme `#f7f1e6`, etc.)
- **AND** as três famílias de fonte estão disponíveis com seus papéis

#### Scenario: Animações nomeadas disponíveis como utilitários
- **WHEN** um componente precisa das microinterações do design
- **THEN** as animações `cmd-up`, `cmd-fade`, `cmd-pop`, `cmd-check`, `cmd-pulse` e `cmd-spin` estão disponíveis como utilitários reutilizáveis

### Requirement: Lucide-only iconography, no emojis in UI
O frontend SHALL usar exclusivamente ícones Lucide React na UI e NÃO SHALL usar emojis na interface. Emojis são permitidos apenas na mensagem de WhatsApp gerada.

#### Scenario: Ícone da UI é Lucide
- **WHEN** a UI exibe um ícone
- **THEN** o ícone provém de Lucide React
- **AND** nenhum emoji é usado como ícone ou elemento da interface

### Requirement: Routing separates public storefront from owner panel
O frontend SHALL, no mesmo bundle SPA, separar por rota o storefront público e o painel do dono, de modo que as duas superfícies coexistam sem servidores distintos.

#### Scenario: Rota pública carrega o storefront
- **WHEN** o usuário acessa uma rota pública do storefront
- **THEN** a SPA renderiza a superfície de storefront

#### Scenario: Rota do painel carrega o shell autenticado
- **WHEN** o usuário acessa uma rota do painel do dono
- **THEN** a SPA renderiza a superfície do painel
- **AND** ambas as superfícies vivem no mesmo bundle

