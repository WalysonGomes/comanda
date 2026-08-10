## MODIFIED Requirements

### Requirement: Aparência do PWA derivada do design aprovado
O manifest, as tags runtime `theme-color` e os ícones do PWA SHALL usar a identidade visual e o conjunto autoritativo de tokens aprovados pela change `2026-07-24-design-refresh`. Enquanto a divergência entre `.design` (`--acc #b53c25`) e a baseline consolidada (`--acc #d6492f`) não for resolvida, a implementação NÃO SHALL ser considerada visualmente concluída. Depois da decisão, `theme_color`, cor de destaque dos ícones/assets e `--acc` SHALL usar o mesmo valor; `background_color` SHALL permanecer derivado de `--cream #f7f1e6`, salvo decisão explícita. Os ícones NÃO SHALL conter emojis.

#### Scenario: Cores e marca do manifest estão sincronizadas
- **WHEN** o PWA é instalado e a splash/ícone são exibidos pelo sistema
- **THEN** `theme_color`, tags runtime e cor de destaque da marca correspondem ao `--acc` autoritativo
- **AND** a `background_color` corresponde ao `--cream` autoritativo
- **AND** o ícone exibe a marca Comanda sem emojis
