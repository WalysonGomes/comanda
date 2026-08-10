import { Building2, ChevronRight, CreditCard, Link2 } from 'lucide-react'
import { useNavigate } from 'react-router'

import { PainelTabBar } from '@/components/PainelTabBar'
import { usePlanStatus } from '@/features/plans/queries'

export function AjustesScreen() {
  const navigate = useNavigate()
  const statusQuery = usePlanStatus()
  const status = statusQuery.data

  const rows = [
    { key: 'business', label: 'Dados do negócio', sub: 'Nome, logo, horários e endereço', icon: Building2, path: '/painel/ajustes/dados-do-negocio' },
    {
      key: 'meu-link',
      label: 'Meu link',
      sub: 'QR Code, copiar e compartilhar',
      icon: Link2,
      path: '/painel/ajustes/meu-link',
    },
    {
      key: 'plano',
      label: 'Plano e uso',
      sub: 'Limites, uso e assinatura',
      icon: CreditCard,
      path: '/painel/ajustes/plano',
    },
  ]

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <header className="flex-none px-4 pb-3 pt-5">
        <h1 className="font-display text-xl font-extrabold text-ink">Ajustes</h1>
      </header>

      <div className="flex-1 px-4 pb-8">
        <div
          className="flex items-center gap-3.5 rounded-2xl p-4"
          style={{ background: 'linear-gradient(155deg,#2c2520,#3a3028)' }}
        >
          <div className="flex size-[50px] flex-none items-center justify-center rounded-[14px] bg-acc font-display text-lg font-extrabold text-white">
            {(status?.businessName ?? '·').charAt(0).toUpperCase()}
          </div>
          <div className="flex-1">
            <div className="font-display text-[17px] font-extrabold text-white">{status?.businessName ?? '…'}</div>
            <div className="mt-0.5 text-[12.5px] text-[#b6a890]">{status?.menuUrl ?? ''}</div>
          </div>
          <span className="rounded-[7px] bg-[rgba(255,179,138,.16)] px-2.5 py-1 text-[10.5px] font-bold text-[#ffce9e]">
            {status?.plan === 'ESSENCIAL' ? 'Essencial' : 'Gratuito'}
          </span>
        </div>

        <div className="mt-4.5 flex flex-col gap-2.5">
          {rows.map((row) => {
            const Icon = row.icon
            return (
              <button
                key={row.key}
                type="button"
                onClick={() => navigate(row.path)}
                className="flex items-center gap-3.5 rounded-[15px] border border-line bg-card p-3.5 text-left"
              >
                <span className="flex size-10 flex-none items-center justify-center rounded-[11px] bg-acc-tint text-acc-d">
                  <Icon className="size-[18px]" strokeWidth={2} />
                </span>
                <span className="flex-1">
                  <span className="block text-[15px] font-bold text-ink">{row.label}</span>
                  <span className="mt-0.5 block text-[12.5px] text-ink-3">{row.sub}</span>
                </span>
                <ChevronRight className="size-[18px] text-[#c2b49a]" />
              </button>
            )
          })}
        </div>
      </div>

      <PainelTabBar current="ajustes" />
    </div>
  )
}
