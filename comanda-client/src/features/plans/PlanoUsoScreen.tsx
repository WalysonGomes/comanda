import { ArrowLeft, Check, TriangleAlert } from 'lucide-react'
import { useNavigate } from 'react-router'

import { usePlanStatus } from '@/features/plans/queries'

function usageColor(count: number, limit: number | null): string {
  if (limit === null) return '#2f7a44'
  const pct = count / limit
  if (pct >= 1) return '#c0392b'
  if (pct >= (limit === 30 ? 25 / 30 : 0.8)) return '#c9881a'
  return '#2f7a44'
}

function usageLabel(count: number, limit: number | null): string {
  return limit === null ? `${count} · ilimitado` : `${count} / ${limit}`
}

function usagePct(count: number, limit: number | null): string {
  if (limit === null) return '100%'
  return `${Math.min(100, Math.round((count / limit) * 100))}%`
}

/** Bloco `PLANO E USO` (`.design/Comanda Painel.dc.html` lines 493-534, task 7). */
export function PlanoUsoScreen() {
  const navigate = useNavigate()
  const statusQuery = usePlanStatus()
  const status = statusQuery.data

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <header className="flex flex-none items-center gap-3 px-4 pt-5 pb-3.5">
        <button
          type="button"
          onClick={() => navigate('/painel/ajustes')}
          aria-label="Voltar"
          className="flex size-9 items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
        >
          <ArrowLeft className="size-5" strokeWidth={2} />
        </button>
        <h1 className="font-display text-xl font-extrabold text-ink">Plano e uso</h1>
      </header>

      <div className="flex-1 px-4 pb-8">
        {statusQuery.isLoading || !status ? (
          <div className="sk h-24 w-full" />
        ) : (
          <>
            <div
              className="flex items-center justify-between rounded-2xl px-4.5 py-4"
              style={{ background: 'linear-gradient(155deg,#2c2520,#3a3028)' }}
            >
              <div>
                <div className="text-[12.5px] text-[#b6a890]">Seu plano</div>
                <div className="mt-1 font-display text-[22px] font-extrabold text-white">
                  {status.plan === 'ESSENCIAL' ? 'Essencial' : 'Gratuito'}
                </div>
              </div>
              <span className="rounded-lg bg-[rgba(255,179,138,.16)] px-2.5 py-1.5 text-[11px] font-bold text-[#ffce9e]">
                {status.plan === 'ESSENCIAL' ? 'R$ 49 / mês' : 'R$ 0 / sempre'}
              </span>
            </div>

            {status.showQuotaWarning && (
              <div className="mt-3.5 flex gap-2.5 rounded-2xl border border-[rgba(214,73,47,.24)] bg-acc-tint p-3.5">
                <TriangleAlert className="mt-0.5 size-4 shrink-0 text-acc" strokeWidth={2} />
                <p className="text-[13px] leading-snug text-[#8a4a3a]">
                  Você já usou <b className="font-bold text-[#7a2417]">{status.orderCountMonth} dos {status.orderLimit} pedidos</b> deste
                  mês. Faltam poucos para o limite do plano gratuito.
                </p>
              </div>
            )}

            <div className="mt-4.5 flex flex-col gap-4">
              <UsageBar label="Pedidos do mês" count={status.orderCountMonth} limit={status.orderLimit} />
              <UsageBar label="Produtos" count={status.productCount} limit={status.productLimit} />
              <UsageBar label="Categorias" count={status.categoryCount} limit={status.categoryLimit} />
            </div>

            {status.plan === 'GRATUITO' && (
              <>
                <p className="mt-6.5 mb-2.5 text-xs font-bold tracking-wide text-ink-3 uppercase">
                  Faça mais com o Essencial
                </p>
                <div className="flex gap-2.5">
                  <PlanCard
                    name="Gratuito"
                    tag="Atual"
                    price="R$ 0"
                    per="/sempre"
                    features={['30 pedidos/mês', '30 produtos, 5 categorias', 'Histórico de 7 dias']}
                    active
                  />
                  <PlanCard
                    name="Essencial"
                    tag="Recomendado"
                    price="R$ 49"
                    per="/mês"
                    features={['Pedidos ilimitados', 'Produtos e categorias ilimitados', 'Histórico de 30 dias']}
                  />
                </div>
                <button
                  type="button"
                  className="mt-4.5 w-full rounded-[14px] bg-acc py-3.5 text-[15px] font-extrabold text-white shadow-[0_10px_22px_-10px_var(--acc)]"
                >
                  Assinar o Essencial — R$ 49/mês
                </button>
                <p className="mt-2.5 text-center text-xs text-ink-3">No MVP, a ativação é combinada pelo WhatsApp.</p>
              </>
            )}
          </>
        )}
      </div>
    </div>
  )
}

function UsageBar({ label, count, limit }: { label: string; count: number; limit: number | null }) {
  const color = usageColor(count, limit)
  return (
    <div>
      <div className="mb-1.5 flex justify-between">
        <span className="text-[13.5px] font-bold text-ink">{label}</span>
        <span className="font-mono text-[12.5px] font-bold" style={{ color }}>
          {usageLabel(count, limit)}
        </span>
      </div>
      <div className="h-[9px] overflow-hidden rounded-[5px] bg-[#ece3d3]">
        <div className="h-full rounded-[5px]" style={{ width: usagePct(count, limit), background: color }} />
      </div>
    </div>
  )
}

function PlanCard({
  name,
  tag,
  price,
  per,
  features,
  active,
}: {
  name: string
  tag: string
  price: string
  per: string
  features: string[]
  active?: boolean
}) {
  return (
    <div
      className="flex-1 rounded-2xl border-[1.5px] p-3.5"
      style={{ background: active ? '#efe6d6' : 'var(--acc-tint)', borderColor: active ? '#d6cab4' : 'var(--acc)' }}
    >
      <div className="flex items-center justify-between">
        <span className="font-display text-[15px] font-extrabold" style={{ color: active ? 'var(--ink)' : 'var(--acc-d)' }}>
          {name}
        </span>
        <span
          className="rounded-md px-1.5 py-0.5 text-[9px] font-bold tracking-wide uppercase"
          style={{ background: active ? '#d6cab4' : 'var(--acc)', color: active ? 'var(--ink-2)' : '#fff' }}
        >
          {tag}
        </span>
      </div>
      <div className="mt-2 font-display text-xl font-extrabold" style={{ color: active ? 'var(--ink)' : 'var(--acc-d)' }}>
        {price}
        <span className="text-[11px] font-semibold text-ink-3">{per}</span>
      </div>
      <div className="mt-3 flex flex-col gap-1.5">
        {features.map((feature) => (
          <div key={feature} className="flex items-start gap-1.5">
            <Check className="mt-0.5 size-3.5 shrink-0" style={{ color: active ? 'var(--ink-3)' : 'var(--acc-d)' }} />
            <span className="text-[12px] leading-snug text-ink-2">{feature}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
