import { Check, Info } from 'lucide-react'

import { cn } from '@/lib/utils'
import type { Segment } from '@/features/onboarding/api'
import { SEGMENTS } from '@/features/onboarding/segments'

/**
 * Step 0 (`.design/Comanda Painel.dc.html` `ob0`, lines 124-142): segment picker + the Gratuito
 * limit communicated before any account exists (PRD Seção 6 — "comunicado antes do cadastro").
 */
export function SegmentStep({ value, onChange }: { value: Segment | null; onChange: (segment: Segment) => void }) {
  return (
    <div className="animate-cmd-slide">
      <h1 className="font-display text-[26px] leading-[1.1] font-black tracking-tight text-ink">
        Que tipo de negócio
        <br />é o seu?
      </h1>
      <p className="mt-2 text-sm leading-snug text-ink-2">Vamos montar um cardápio de exemplo pra você só editar.</p>

      <div className="mt-5 flex flex-col gap-2.5">
        {SEGMENTS.map((segment) => {
          const Icon = segment.icon
          const selected = segment.value === value
          return (
            <button
              key={segment.value}
              type="button"
              onClick={() => onChange(segment.value)}
              className={cn(
                'flex items-center gap-3.5 rounded-2xl border-[1.5px] bg-card p-4 text-left',
                selected ? 'border-acc shadow-[0_2px_10px_-6px_rgba(60,42,24,.3)]' : 'border-line',
              )}
            >
              <span
                className="flex size-[46px] flex-none items-center justify-center rounded-[13px]"
                style={{ background: selected ? 'var(--acc)' : '#efe6d6', color: selected ? '#fff' : 'var(--ink-2)' }}
              >
                <Icon className="size-[22px]" strokeWidth={2} />
              </span>
              <span className="flex-1">
                <span className="block font-bold text-[15.5px] leading-tight text-ink">{segment.label}</span>
                <span className="mt-0.5 block text-[12.5px] text-ink-2">{segment.desc}</span>
              </span>
              {selected && <Check className="size-5 flex-none text-acc-d" strokeWidth={2.5} />}
            </button>
          )
        })}
      </div>

      <div className="mt-4.5 flex gap-2 rounded-[13px] border border-[#cfe3c8] bg-[#eef4ec] p-3.5">
        <Info className="mt-0.5 size-4 shrink-0 text-[#2f7a44]" strokeWidth={2} />
        <p className="text-[12.5px] leading-snug text-[#356b3f]">
          No plano <b className="font-bold">Gratuito</b> você recebe até <b className="font-bold">30 pedidos/mês</b> —
          suficiente para começar sem pagar nada.
        </p>
      </div>
    </div>
  )
}
