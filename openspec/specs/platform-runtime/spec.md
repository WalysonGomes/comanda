# platform-runtime Specification

## Purpose
TBD - created by archiving change foundations. Update Purpose after archive.
## Requirements
### Requirement: Vertical-slice package organization
O backend SHALL organizar seus pacotes por fatia vertical (feature-based), não por camada técnica. Um pacote-base SHALL ser definido e cada capability futura (auth, menu, storefront, orders, plans) SHALL residir em seu próprio pacote de feature.

#### Scenario: Nova feature ganha pacote próprio
- **WHEN** uma nova capability é adicionada ao backend
- **THEN** seu código (controller, serviço, persistência) vive sob um único pacote de feature
- **AND** não é espalhado por pacotes de camada técnica compartilhados

### Requirement: Virtual Threads enabled
O sistema SHALL rodar sobre Virtual Threads (Java 21 / Spring Boot 4.1) para tratamento de requisições, usando APIs compatíveis com Spring Boot 4.1 / Spring Framework 7.

#### Scenario: Requisições servidas por Virtual Threads
- **WHEN** a aplicação processa requisições HTTP
- **THEN** o processamento ocorre sobre Virtual Threads

### Requirement: Fixed application timezone
O sistema SHALL fixar o timezone da aplicação em `America/Fortaleza`, documentado como limitação conhecida. Toda lógica dependente de data/hora (horário de funcionamento, contagem mensal, timestamps) SHALL usar esse timezone.

#### Scenario: Operações de data usam America/Fortaleza
- **WHEN** o sistema calcula ou registra data/hora
- **THEN** o cálculo usa o timezone `America/Fortaleza`

### Requirement: Backend serves the static SPA
O backend SHALL servir o build estático da SPA React/Vite como parte do artefato único deployável, entregando o `index.html` para rotas de aplicação que não sejam de API ou de asset estático.

#### Scenario: Rota da SPA retorna o app
- **WHEN** um navegador requisita uma rota de aplicação que não é endpoint de API nem asset estático
- **THEN** o backend retorna o `index.html` do build da SPA
- **AND** a SPA assume o roteamento no cliente

#### Scenario: Asset estático é servido diretamente
- **WHEN** o navegador requisita um asset do build (JS, CSS, imagem, fonte)
- **THEN** o backend serve o arquivo estático correspondente

