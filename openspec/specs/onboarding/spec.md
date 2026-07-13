# onboarding Specification

## Purpose
TBD - created by archiving change plans-and-onboarding. Update Purpose after archive.

## Requirements

### Requirement: Seleção de segmento com cardápio de demonstração
O sistema SHALL, no primeiro passo do onboarding (bloco `ONBOARDING`, step 0), oferecer a seleção de um **segmento** de negócio (ex.: marmiteria, confeitaria, hamburgueria, açaizeria). Ao concluir o cadastro para o segmento escolhido, o sistema SHALL **pré-popular um cardápio de demonstração** correspondente — categorias, produtos (nome, descrição, preço) e grupos de adicionais típicos — como dados **reais e editáveis** do tenant (não placeholders), de modo que o dono edite algo pronto em vez de partir de uma tela vazia. O cardápio de demonstração SHALL respeitar os limites do plano Gratuito (não exceder 30 produtos nem 5 categorias).

#### Scenario: Segmento gera cardápio de demonstração editável
- **WHEN** o dono escolhe um segmento no onboarding e conclui o cadastro
- **THEN** o tenant recebe um cardápio de demonstração daquele segmento como categorias/produtos/adicionais reais e editáveis, dentro dos limites do plano Gratuito

#### Scenario: Demonstração é editável, não fixa
- **WHEN** o dono altera um produto do cardápio de demonstração
- **THEN** a alteração persiste como dado do tenant, como qualquer edição de cardápio

### Requirement: Comunicação do limite do plano Gratuito antes de criar a conta
O sistema SHALL comunicar o limite do plano Gratuito (até 30 pedidos/mês) ao usuário **antes** da criação da conta, durante o onboarding (no passo de seleção de segmento, anterior ao passo de conta). A informação SHALL ser clara e não bloquear o prosseguimento.

#### Scenario: Limite informado antes do cadastro
- **WHEN** o dono está no passo de seleção de segmento, antes de criar a conta
- **THEN** o sistema informa que o plano Gratuito oferece até 30 pedidos/mês, de forma clara, antes de qualquer criação de conta

### Requirement: Wizard de onboarding curto até o cardápio publicado
O sistema SHALL conduzir o dono por um wizard curto após o cadastro (bloco `ONBOARDING`): definir o **horário de funcionamento**, **confirmar/editar o primeiro produto**, e concluir numa tela **"Pronto! Seu cardápio está no ar"**. O wizard SHALL exibir um indicador de progresso e permitir voltar/avançar. A tela final SHALL apresentar o **link** do cardápio, com ações de **copiar**, **compartilhar no WhatsApp** e um **QR Code**. Ao final, o cardápio SHALL estar publicado e acessível em `subdomain.${APP_DOMAIN}`.

#### Scenario: Concluir o wizard publica o cardápio
- **WHEN** o dono percorre horário → primeiro produto → tela final
- **THEN** vê a tela "Pronto! Seu cardápio está no ar" com link, copiar, compartilhar no WhatsApp e QR Code, e o cardápio está acessível no subdomínio do tenant

#### Scenario: Navegação do wizard
- **WHEN** o dono está num passo intermediário do wizard
- **THEN** pode voltar ao passo anterior e avançar, com indicador de progresso refletindo o passo atual

### Requirement: Tela "Meu link" com aviso permanente sobre subdomínio
O sistema SHALL prover uma tela "Meu link" (bloco `MEU LINK`) exibindo o endereço do cardápio (`subdomain.${APP_DOMAIN}`), um **QR Code**, e ações de **copiar** e **compartilhar no WhatsApp**. A tela SHALL exibir de forma **permanente e visível** (não um aviso transitório) que **alterar o subdomínio quebra os QR Codes impressos e os links já compartilhados**.

#### Scenario: Meu link mostra endereço, QR e ações
- **WHEN** o dono abre a tela "Meu link"
- **THEN** vê o endereço do cardápio, o QR Code e os botões de copiar e compartilhar no WhatsApp

#### Scenario: Aviso de quebra de subdomínio é permanente
- **WHEN** o dono visualiza a tela "Meu link"
- **THEN** um aviso permanente informa que mudar o subdomínio quebra QR Codes impressos e links compartilhados
