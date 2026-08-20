# Plano de Resposta a Incidentes (dev solo)

> Cobre a Seção 4.6 do PRD e o requisito "Plano de resposta a incidentes para dev solo" da
> especificação consolidada `operational-reliability`. Este plano é dimensionado para uma
> operação de desenvolvedor único, sem equipe de plantão ou SOC.

## Responsável

O desenvolvedor/mantenedor do Comanda é o responsável por identificar, conter, registrar,
remediar e comunicar incidentes de segurança ou vazamentos de dados.

## Canal de emergência e privacidade

- E-mail: privacidade@comanda.app.
- WhatsApp pessoal do desenvolvedor, documentado internamente fora deste repositório público.

## Classificação inicial

- **Privacidade/segurança:** exposição de credenciais, telefones, endereços ou tokens; acesso
  cross-tenant/IDOR; comprometimento de credenciais de infraestrutura.
- **Disponibilidade:** interrupção prolongada que impeça o processamento de pedidos.
- **Severidade:** considerar sensibilidade e volume dos dados, quantidade de titulares, duração,
  possibilidade de uso indevido e se a exposição continua ativa.

## Procedimento de resposta

1. **Conter:** limitar imediatamente a exposição. Revogar credenciais, desabilitar o componente
   afetado, isolar acesso ou reverter a versão, escolhendo a medida proporcional ao incidente.
2. **Preservar evidências e avaliar impacto:** registrar logs e horários relevantes sem ampliar a
   exposição; determinar causa provável, dados, tenants e titulares afetados. Consultar
   `docs/ropa.md` para as categorias e responsabilidades de tratamento.
3. **Manter registro interno:** anotar descoberta, início estimado, contenção, sistemas e dados
   envolvidos, decisões, responsável, comunicações e evidências de encerramento, inclusive quando
   se concluir que não houve incidente reportável.
4. **Aplicar o critério de notificação vigente no PRD/spec:** vazamento de dados pessoais com
   **risco real aos titulares** deve ser notificado à ANPD em **até 72 horas** a partir da ciência
   do incidente. Registrar a justificativa da decisão e as informações comunicadas.
5. **Comunicar titulares afetados:** quando houver risco real, usar linguagem clara para explicar
   o que ocorreu, quais dados foram afetados, riscos, medidas tomadas, ações recomendadas e canal
   de contato. Coordenar com o tenant controlador quando os titulares forem seus clientes.
6. **Remediar:** corrigir a causa raiz, rotacionar segredos e invalidar sessões quando necessário,
   revisar configurações e eliminar acessos indevidos persistentes.
7. **Testar e encerrar:** executar testes de regressão de segurança e do fluxo afetado, verificar
   que a contenção e a correção permanecem efetivas e registrar pendências e acompanhamento.

Falhas funcionais ou de disponibilidade sem exposição indevida seguem o fluxo normal de
correção, embora devam ser escaladas por este procedimento se a investigação revelar impacto de
segurança ou privacidade.
