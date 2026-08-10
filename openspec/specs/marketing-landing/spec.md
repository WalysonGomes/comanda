# marketing-landing Specification

## Purpose
TBD - created by archiving change 2026-07-24-design-refresh. Update Purpose after archive.
## Requirements
### Requirement: Landing institucional no domínio raiz
O sistema SHALL renderizar uma landing institucional na rota `/` quando o host for o domínio raiz aprovado ou um alias reservado configurado, sem substituir o storefront servido em subdomínios de tenant.

#### Scenario: Domínio raiz abre a landing
- **WHEN** uma pessoa acessa `/` no domínio raiz ou alias reservado configurado
- **THEN** a SPA renderiza a landing institucional

#### Scenario: Tenant abre o storefront
- **WHEN** um cliente acessa `/` em um subdomínio de tenant
- **THEN** a SPA renderiza o storefront e não a landing

### Requirement: Conteúdo limitado às capacidades comprovadas do MVP
A landing SHALL usar comunicação verificável: polling SHALL ser qualificado como atualização periódica ou quase em tempo real; notificações automáticas inexistentes NÃO SHALL ser prometidas; e o sistema NÃO SHALL afirmar que um pedido foi registrado no painel quando a persistência falhar. Stripe, push nativo, recuperação de senha e recursos pós-MVP NÃO SHALL ser promovidos como disponíveis por esta change.

#### Scenario: Copy descreve atualização corretamente
- **WHEN** a landing descreve a atualização da lista de pedidos
- **THEN** o texto qualifica o polling e não o apresenta como tempo real irrestrito ou notificação automática

#### Scenario: Persistência falha
- **WHEN** a criação do pedido não é confirmada pelo servidor
- **THEN** nenhuma confirmação afirma que o pedido chegou ao painel

### Requirement: CTAs completos e verificáveis
Cada CTA e link da landing SHALL apontar para uma rota, seção ou contato real e SHALL ser utilizável por teclado e tecnologias assistivas. Destinos indisponíveis ou fora do MVP SHALL ser removidos ou apresentados sem promessa de disponibilidade.

#### Scenario: CTA principal é acionado
- **WHEN** a pessoa aciona o CTA principal por ponteiro ou teclado
- **THEN** ela navega para o fluxo real de onboarding sem erro ou destino fictício

### Requirement: Landing responsiva, acessível e com movimento reduzido
A landing SHALL preservar leitura e operação em viewports móveis e desktop, sem overflow horizontal, SHALL manter contraste, hierarquia de headings, foco visível, nomes acessíveis e textos alternativos adequados, e SHALL respeitar `prefers-reduced-motion` sem ocultar conteúdo.

#### Scenario: Movimento reduzido está ativo
- **WHEN** o sistema operacional solicita movimento reduzido
- **THEN** animações não essenciais são desativadas e todo o conteúdo permanece visível e operável

#### Scenario: Viewport móvel
- **WHEN** a landing é exibida em largura móvel suportada
- **THEN** navegação, conteúdo, imagens e CTAs permanecem legíveis e operáveis sem rolagem horizontal

### Requirement: Aceitação automatizada e visual
A landing e o roteamento por host SHALL possuir testes automatizados; o frontend SHALL passar build, lint e testes; o serving estático relevante SHALL passar testes de backend; e a aceitação SHALL incluir comparação visual com as referências aprovadas em viewport móvel e desktop.

#### Scenario: Candidato a release é avaliado
- **WHEN** a implementação é proposta para release
- **THEN** build, lint, testes frontend, testes relevantes de serving estático e comparação visual documentada estão aprovados
- **AND** o bloqueador conhecido de checkout foi resolvido ou impede a aprovação
