import { cn } from '@/lib/utils'

const DAY_LABELS = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S']

/** `available_days` convention: 0=Dom … 6=Sáb; null/empty = every day (PRD Seção 13). */
export function DayChips({
  value,
  onChange,
}: {
  value: number[] | null
  onChange: (days: number[] | null) => void
}) {
  const selected = new Set(value ?? [])
  const allDays = selected.size === 0

  function toggle(day: number) {
    const next = new Set(selected)
    if (next.has(day)) {
      next.delete(day)
    } else {
      next.add(day)
    }
    onChange(next.size === 0 ? null : Array.from(next).sort((a, b) => a - b))
  }

  return (
    <div className="flex gap-1.5">
      {DAY_LABELS.map((label, day) => {
        const active = allDays || selected.has(day)
        return (
          <button
            key={day}
            type="button"
            onClick={() => toggle(day)}
            aria-pressed={active}
            aria-label={`Disponível ${label === 'D' ? 'domingo' : label}, dia ${day}`}
            className={cn(
              'flex-1 rounded-[10px] py-2.5 text-[11.5px] font-bold transition-colors',
              active ? 'bg-acc text-white' : 'border border-line bg-card text-ink-2',
            )}
          >
            {label}
          </button>
        )
      })}
    </div>
  )
}
