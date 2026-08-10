import { LegalPageShell } from '@/features/legal/LegalPageShell'
import { canonicalTenantDomain } from '@/lib/domain'

/**
 * Static privacy policy (task 8.1, PRD Seção 9/LGPD): describes what personal data the product
 * processes (owner and end customer), why, and under which legal basis — plus the privacy
 * contact channel (task 8.4). Content lives in the SPA build, not fetched from the API — there's
 * nothing tenant-specific here.
 */
export function PrivacyPolicyPage() {
  return (
    <LegalPageShell title="Política de Privacidade">
      <p>Última atualização: julho de 2026.</p>

      <p>
        Esta política descreve como o <strong>Comanda</strong> trata dados pessoais no uso da plataforma pelos dois
        perfis de usuário: o <strong>dono do negócio</strong> (quem cria e opera o cardápio) e o{' '}
        <strong>cliente final</strong> (quem faz um pedido pelo cardápio público).
      </p>

      <h2>Quais dados tratamos</h2>
      <ul>
        <li>
          <strong>Do dono do negócio:</strong> nome, e-mail, senha (armazenada com hash, nunca em texto puro), número
          de WhatsApp do negócio, nome e dados do negócio (nome, logo, horários, cardápio).
        </li>
        <li>
          <strong>Do cliente final:</strong> nome, telefone e, quando o pedido é para entrega, endereço — informados
          na finalização do pedido e usados apenas para montar a mensagem enviada ao WhatsApp do negócio.
        </li>
      </ul>

      <h2>Finalidade</h2>
      <p>
        Os dados do dono viabilizam login, identificação do negócio e envio de mensagens transacionais (ex.:
        recuperação de senha). Os dados do cliente final existem para que o pedido chegue organizado ao dono do
        negócio via WhatsApp — o Comanda não processa pagamento nem entrega.
      </p>

      <h2>Base legal</h2>
      <p>
        O tratamento se apoia na <strong>execução de contrato</strong> (viabilizar o cadastro do dono e o pedido do
        cliente final) e no <strong>legítimo interesse</strong> do dono do negócio em organizar seus próprios
        pedidos. Veja também os{' '}
        <a href="/termos" className="font-semibold text-acc-d underline">
          Termos de Serviço
        </a>{' '}
        quanto à relação entre o dono do negócio (controlador dos dados de seus clientes) e o Comanda (operador).
      </p>

      <h2>Retenção</h2>
      <p>
        Pedidos e seus dados associados ficam armazenados enquanto a conta do dono estiver ativa; o histórico visível
        no painel varia por plano (7 ou 30 dias), mas os registros não são apagados automaticamente ao expirar essa
        janela de visualização.
      </p>

      <h2>Seus direitos</h2>
      <p>
        Titulares de dados podem solicitar acesso, correção ou exclusão de seus dados pessoais entrando em contato
        pelo canal abaixo.
      </p>

      <h2>Contato de privacidade</h2>
      <p>
        Dúvidas, solicitações ou incidentes envolvendo dados pessoais:{' '}
        <a href={`mailto:privacidade@${canonicalTenantDomain}`} className="font-semibold text-acc-d underline">
          privacidade@{canonicalTenantDomain}
        </a>
        .
      </p>
    </LegalPageShell>
  )
}
