## MODIFIED Requirements

### Requirement: Aparência do PWA derivada do design aprovado
O manifest, as tags runtime `theme-color` e os ícones do PWA SHALL usar a identidade visual e o conjunto autoritativo aprovado: `theme_color`, cor de destaque dos ícones/assets e `--acc` SHALL usar `#b53c25`; `background_color` SHALL usar `--cream #f7f1e6`. Os ícones NÃO SHALL conter emojis. A geração SHALL ser reproduzível sem a referência local não rastreada `.design`.

#### Scenario: Cores e marca do manifest estão sincronizadas
- **WHEN** o PWA é instalado e a splash/ícone são exibidos pelo sistema
- **THEN** `theme_color`, tags runtime e cor de destaque da marca correspondem ao `--acc` autoritativo
- **AND** a `background_color` corresponde ao `--cream` autoritativo
- **AND** o ícone exibe a marca Comanda sem emojis
