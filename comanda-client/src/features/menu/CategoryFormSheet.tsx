import { useState } from 'react'
import { X } from 'lucide-react'

import { PlanLimitError } from '@/components/PlanLimitError'
import type { Category } from '@/features/menu/api'

/** Create/rename category — a bottom sheet, matching the design's `cmd-up` overlay pattern. */
export function CategoryFormSheet({
  category,
  saving,
  errorMessage,
  errorCode,
  onSave,
  onClose,
}: {
  category: Category | null
  saving: boolean
  errorMessage: string | null
  errorCode?: string | null
  onSave: (name: string, active: boolean) => void
  onClose: () => void
}) {
  const [name, setName] = useState(category?.name ?? '')
  const [active, setActive] = useState(category?.active ?? true)

  return (
    <div className="fixed inset-0 z-20">
      <div className="absolute inset-0 animate-cmd-fade bg-[#1c1813]/42" onClick={onClose} />
      <div className="absolute inset-x-0 bottom-0 animate-cmd-up rounded-t-[26px] bg-cream p-5 pb-7 shadow-[0_-20px_50px_-20px_rgba(28,24,19,.5)]">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-extrabold text-ink">
            {category ? 'Editar categoria' : 'Nova categoria'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            className="flex size-9 items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
          >
            <X className="size-[18px]" strokeWidth={2.2} />
          </button>
        </div>

        <label className="mt-5 block">
          <span className="mb-2 block text-xs font-bold text-ink-2">Nome da categoria</span>
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Ex: Lanches"
            className="w-full rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3.5 text-[15px] text-ink outline-none focus:border-acc"
          />
        </label>

        {category && (
          <button
            type="button"
            onClick={() => setActive((a) => !a)}
            className="mt-4 flex w-full items-center justify-between rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3"
          >
            <span className="text-[13px] font-bold text-ink">Categoria ativa</span>
            <span
              className="relative h-[25px] w-[42px] flex-none rounded-full"
              style={{ background: active ? 'var(--acc)' : '#d9cdb6' }}
            >
              <span
                className="absolute top-[3px] size-[19px] rounded-full bg-white shadow transition-[left]"
                style={{ left: active ? '20px' : '3px' }}
              />
            </span>
          </button>
        )}

        {errorMessage && (
          <div className="mt-3">
            <PlanLimitError code={errorCode ?? null} message={errorMessage} />
          </div>
        )}

        <button
          type="button"
          disabled={saving || name.trim().length === 0}
          onClick={() => onSave(name.trim(), active)}
          className="mt-5 w-full rounded-2xl bg-acc py-3.5 text-[15px] font-extrabold text-white shadow-[0_10px_22px_-10px_var(--acc)] disabled:opacity-60"
        >
          {saving ? 'Salvando…' : 'Salvar'}
        </button>
      </div>
    </div>
  )
}
