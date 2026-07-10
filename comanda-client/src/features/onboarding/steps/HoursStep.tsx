import type { BusinessHoursRow } from '@/features/onboarding/api'

const DAY_LABELS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado']

/** Step 2 (`.design/Comanda Painel.dc.html` `ob2`, lines 170-186): one row per day of the week
 * (PRD convention 0=Dom…6=Sáb), toggle aberto/fechado plus the time window when open. */
export function HoursStep({ rows, onChange }: { rows: BusinessHoursRow[]; onChange: (rows: BusinessHoursRow[]) => void }) {
  function update(dayOfWeek: number, patch: Partial<BusinessHoursRow>) {
    onChange(rows.map((row) => (row.dayOfWeek === dayOfWeek ? { ...row, ...patch } : row)))
  }

  return (
    <div className="animate-cmd-slide">
      <h1 className="font-display text-[26px] leading-[1.1] font-black text-ink">Quando você atende?</h1>
      <p className="mt-2 text-sm text-ink-2">
        Fora desse horário, o cardápio mostra "fechado". Dá pra mudar depois.
      </p>

      <div className="mt-5 flex flex-col gap-2.5">
        {rows
          .slice()
          .sort((a, b) => a.dayOfWeek - b.dayOfWeek)
          .map((row) => {
            const open = !row.closed
            return (
              <div key={row.dayOfWeek} className="rounded-[13px] border border-line bg-card p-3">
                <div className="flex items-center gap-3">
                  <span className="flex-1 text-[14px] font-bold" style={{ color: open ? 'var(--ink)' : 'var(--ink-3)' }}>
                    {DAY_LABELS[row.dayOfWeek]}
                  </span>
                  <button
                    type="button"
                    onClick={() => update(row.dayOfWeek, { closed: !row.closed })}
                    aria-label={open ? 'Marcar como fechado' : 'Marcar como aberto'}
                    className="relative h-[25px] w-[42px] flex-none rounded-full"
                    style={{ background: open ? 'var(--acc)' : '#d9cdb6' }}
                  >
                    <span
                      className="absolute top-[3px] size-[19px] rounded-full bg-white shadow transition-[left]"
                      style={{ left: open ? '20px' : '3px' }}
                    />
                  </button>
                </div>
                {open && (
                  <div className="mt-2.5 flex items-center gap-2">
                    <input
                      type="time"
                      value={row.opensAt ?? '08:00'}
                      onChange={(e) => update(row.dayOfWeek, { opensAt: e.target.value })}
                      className="flex-1 rounded-lg border border-line bg-cream px-2.5 py-2 font-mono text-[13px] text-ink"
                    />
                    <span className="text-ink-3">–</span>
                    <input
                      type="time"
                      value={row.closesAt ?? '18:00'}
                      onChange={(e) => update(row.dayOfWeek, { closesAt: e.target.value })}
                      className="flex-1 rounded-lg border border-line bg-cream px-2.5 py-2 font-mono text-[13px] text-ink"
                    />
                  </div>
                )}
              </div>
            )
          })}
      </div>
    </div>
  )
}
