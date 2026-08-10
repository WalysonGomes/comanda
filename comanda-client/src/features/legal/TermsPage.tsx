import { LegalPageShell } from '@/features/legal/LegalPageShell'
import { canonicalTenantDomain } from '@/lib/domain'

/**
 * Static terms of service (task 8.2, PRD Seção 9/LGPD): carries the controller-operator clause
 * required by LGPD — the tenant (business owner) is the controller of their end-customers' data,
 * Comanda is the operator that processes it on the tenant's behalf.
 */
export function TermsPage() {
  return (
    <LegalPageShell title="Termos de Serviço">
      <p>Última atualização: julho de 2026.</p>

      <p>
        Estes termos regem o uso da plataforma <strong>Comanda</strong> pelo dono do negócio (o "usuário
        contratante") que cria e opera um cardápio digital.
      </p>

      <h2>O que é o Comanda</h2>
      <p>
        O Comanda é uma ferramenta de cardápio digital e organização de pedidos. Pedidos são combinados e pagos pelo
        WhatsApp, fora da plataforma — o Comanda não processa pagamento nem faz a entrega.
      </p>

      <h2>Relação controlador-operador (LGPD)</h2>
      <p>
        Para os dados pessoais dos clientes finais que fazem pedidos pelo cardápio de um negócio (nome, telefone,
        endereço), o <strong>dono do negócio é o controlador</strong>: é ele quem decide coletar esses dados e usá-los
        para atender o pedido. O <strong>Comanda atua como operador</strong>, processando esses dados exclusivamente
        para viabilizar a operação da plataforma contratada pelo dono do negócio (armazenar o pedido, gerar a
        mensagem de WhatsApp, exibir o painel), e não para finalidade própria.
      </p>
      <p>
        Cabe ao dono do negócio garantir que a coleta dos dados de seus clientes finais está de acordo com a LGPD,
        incluindo atender, quando aplicável, a solicitações desses titulares.
      </p>

      <h2>Conta e responsabilidade</h2>
      <p>
        O dono do negócio é responsável pela veracidade dos dados cadastrados, pela guarda de sua senha e pelo
        conteúdo do cardápio publicado (preços, disponibilidade, descrições).
      </p>

      <h2>Planos e limites</h2>
      <p>
        O uso do plano Gratuito está sujeito aos limites de pedidos, produtos e categorias descritos na tela de
        planos. O plano Essencial remove esses limites mediante pagamento combinado com o Comanda.
      </p>

      <h2>Contato</h2>
      <p>
        Para dúvidas sobre estes termos ou sobre tratamento de dados pessoais:{' '}
        <a href={`mailto:privacidade@${canonicalTenantDomain}`} className="font-semibold text-acc-d underline">
          privacidade@{canonicalTenantDomain}
        </a>
        .
      </p>
    </LegalPageShell>
  )
}
