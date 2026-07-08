# Plano de Resposta a Incidentes (dev solo)

> Cobre PRD Seção 4.6 e o critério de lançamento "plano de resposta a incidentes" (Seção 9,
> `operational-reliability` spec, requirement "Plano de resposta a incidentes para dev solo").
> Este plano é dimensionado para uma operação de desenvolvedor único — sem equipe de plantão,
> sem SOC. Simples e acionável importa mais que exaustivo.

## Responsável

O próprio desenvolvedor/mantenedor do Comanda é o único responsável por identificar, conter e
comunicar qualquer incidente de segurança ou vazamento de dados. Não há rotação de plantão no
MVP.

## Canal de emergência

- E-mail: privacidade@comanda.app (mesmo canal do contato de privacidade — task 8.4).
- WhatsApp pessoal do desenvolvedor (documentado internamente, fora deste repositório público, por
  não ser um dado a versionar em texto plano).

## O que conta como incidente

- Vazamento ou exposição indevida de dados pessoais (de donos de negócio ou de clientes finais):
  credenciais, telefones, endereços, tokens.
- Acesso cross-tenant indevido (falha de isolamento/IDOR) constatado em produção.
- Comprometimento de credenciais de infraestrutura (VPS, banco, secret do JWT).
- Indisponibilidade prolongada que impeça o dono de operar pedidos (fora do escopo de
  privacidade, mas tratado com a mesma disciplina de resposta).

## Passos de resposta

1. **Conter:** isolar a causa imediata — revogar credencial comprometida, aplicar patch, ou
   reverter deploy (rollback, ver `design.md` da `deploy-vps`/`reliability-and-security`: mudanças
   de segurança HTTP são config-driven e reversíveis por deploy anterior).
2. **Avaliar:** determinar quais dados e quantos titulares foram afetados. Consultar o RoPA
   (`docs/ropa.md`) para saber quais categorias de dado cada tabela/fluxo envolve.
3. **Registrar:** anotar timeline (quando começou, quando foi detectado, quando foi contido) —
   necessário para a notificação à ANPD, se aplicável.
4. **Notificar a ANPD, se aplicável:** vazamento de dados pessoais com **risco real aos titulares**
   (ex.: exposição de senha em texto plano, exposição de dados de contato em massa, acesso
   indevido a dados de outro tenant em produção) é notificado à ANPD em **até 72 horas** a partir
   da ciência do incidente, conforme LGPD art. 48.
5. **Notificar os titulares afetados**, quando o risco for real, com linguagem clara sobre o que
   aconteceu e o que fazer.
6. **Corrigir a causa raiz** e, quando fizer sentido, adicionar um teste de regressão que teria
   pego o problema (padrão já seguido pela suíte de segurança desta change — IDOR, rate limiting,
   magic bytes, etc. são todos cobertos por teste, não só por revisão).

## O que não é incidente reportável

Falhas de disponibilidade sem exposição de dados, ou bugs funcionais sem acesso indevido a dado
pessoal, seguem o fluxo normal de correção de bug — não este plano.
