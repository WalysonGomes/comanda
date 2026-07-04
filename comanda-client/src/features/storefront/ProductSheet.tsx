import { useState } from 'react'
import { Check, ImageIcon, Minus, Plus, X } from 'lucide-react'

import { cn } from '@/lib/utils'
import { formatCurrency } from '@/features/menu/format'
import type { StorefrontAdditionalGroup, StorefrontProduct } from '@/features/storefront/api'
import type { CartLine } from '@/features/storefront/cart'

const PLACEHOLDER_BG = {
  backgroundImage: 'repeating-linear-gradient(45deg, #efe4d0, #efe4d0 10px, #e9dcc6 10px, #e9dcc6 20px)',
}

type UnmetGroup = { group: StorefrontAdditionalGroup; index: number }

/**
 * Bottom sheet for a product's detail: additionals, quantity, note (blocks STOREFRONT tasks
 * 8.1-8.4). Reopening an existing cart line (`editingLine`) pre-loads its selections and updates
 * the same line instead of creating a duplicate (task 8.4/9.2).
 */
export function ProductSheet({
  product,
  editingLine,
  onClose,
  onSave,
}: {
  product: StorefrontProduct
  editingLine: CartLine | null
  onClose: () => void
  onSave: (line: CartLine, isEdit: boolean) => void
}) {
  const [selections, setSelections] = useState<Record<number, number[]>>(editingLine?.selections ?? {})
  const [quantity, setQuantity] = useState(editingLine?.quantity ?? 1)
  const [note, setNote] = useState(editingLine?.note ?? '')

  const groups = product.additionalGroups

  function isSelected(groupId: number, itemId: number) {
    return (selections[groupId] ?? []).includes(itemId)
  }

  function pickSingle(groupId: number, itemId: number) {
    setSelections((s) => ({ ...s, [groupId]: [itemId] }))
  }

  function toggleMulti(group: StorefrontAdditionalGroup, itemId: number) {
    setSelections((s) => {
      const current = s[group.id] ?? []
      if (current.includes(itemId)) {
        return { ...s, [group.id]: current.filter((id) => id !== itemId) }
      }
      if (group.maxSelections != null && current.length >= group.maxSelections) {
        return s
      }
      return { ...s, [group.id]: [...current, itemId] }
    })
  }

  const unmet: UnmetGroup | null = groups
    .map((group, index) => ({ group, index }))
    .find(({ group }) => {
      const count = (selections[group.id] ?? []).length
      const requiredMin = group.required ? Math.max(1, group.minSelections) : 0
      const overMax = group.maxSelections != null && count > group.maxSelections
      return count < requiredMin || overMax
    }) ?? null

  const addsTotal = groups.reduce((sum, group) => {
    const ids = selections[group.id] ?? []
    return (
      sum +
      group.items
        .filter((item) => ids.includes(item.id))
        .reduce((s, item) => s + Number(item.additionalPrice), 0)
    )
  }, 0)
  const unitPricePreview = Number(product.price) + addsTotal
  const totalPreview = unitPricePreview * quantity

  function handleSave() {
    if (unmet) return
    const specs: string[] = []
    const adds: { name: string; price: number }[] = []
    for (const group of groups) {
      const ids = selections[group.id] ?? []
      for (const item of group.items) {
        if (!ids.includes(item.id)) continue
        const price = Number(item.additionalPrice)
        if (group.selectionType === 'SINGLE' && price === 0) {
          specs.push(item.name)
        } else {
          adds.push({ name: item.name, price })
        }
      }
    }
    const line: CartLine = {
      lineId: editingLine?.lineId ?? `l${Date.now()}`,
      productId: product.id,
      name: product.name,
      basePrice: Number(product.price),
      quantity,
      note: note.trim(),
      specs,
      adds,
      selections,
    }
    onSave(line, !!editingLine)
  }

  return (
    <div className="fixed inset-0 z-30">
      <div className="absolute inset-0 animate-cmd-fade bg-[#1c1813]/42" onClick={onClose} />
      <div className="absolute inset-x-0 bottom-0 flex max-h-[94%] flex-col rounded-t-[26px] bg-cream shadow-[0_-20px_50px_-20px_rgba(28,24,19,.5)] animate-cmd-up">
        <div
          className="relative flex h-[150px] flex-none items-center justify-center rounded-t-[26px] text-[#c2b49a]"
          style={product.imageUrl ? undefined : PLACEHOLDER_BG}
        >
          {product.imageUrl ? (
            <img src={product.imageUrl} alt="" className="size-full rounded-t-[26px] object-cover" />
          ) : (
            <ImageIcon className="size-8" strokeWidth={1.75} />
          )}
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            className="absolute top-3 right-3 flex size-[34px] items-center justify-center rounded-full bg-[#1c1813]/50 text-white"
          >
            <X className="size-[18px]" strokeWidth={2.2} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-[18px] pt-4 pb-2">
          <div className="flex items-start justify-between gap-3">
            <h2 className="font-display text-xl font-extrabold text-ink">{product.name}</h2>
            <span className="font-display text-lg font-extrabold whitespace-nowrap text-acc-d">
              {formatCurrency(product.price)}
            </span>
          </div>
          {product.description && <p className="mt-2 text-[13.5px] leading-normal text-ink-2">{product.description}</p>}

          <div className="mt-5 flex flex-col gap-4">
            {groups.map((group, groupIndex) => (
              <div key={group.id}>
                <div className="mb-2.5 flex items-center gap-2">
                  <span className="text-[14.5px] font-bold text-ink">{group.name}</span>
                  <span
                    className={cn(
                      'rounded-md px-1.5 py-1 text-[9.5px] font-bold tracking-wide uppercase',
                      group.required ? 'bg-acc-tint text-acc-d' : 'bg-[#eef4ec] text-[#2f7a44]',
                    )}
                  >
                    {group.required ? 'Obrigatório' : 'Opcional'}
                  </span>
                  <span className="ml-auto text-[11.5px] font-semibold text-ink-3">
                    {group.selectionType === 'SINGLE' ? 'Escolha 1' : 'Escolha à vontade'}
                  </span>
                </div>
                <div className="flex flex-col gap-2">
                  {group.items
                    .filter((item) => item.available)
                    .map((item) => {
                      const selected = isSelected(group.id, item.id)
                      const price = Number(item.additionalPrice)
                      return (
                        <button
                          type="button"
                          key={item.id}
                          onClick={() =>
                            group.selectionType === 'SINGLE' ? pickSingle(group.id, item.id) : toggleMulti(group, item.id)
                          }
                          className="flex items-center gap-2.5 rounded-xl border border-line bg-card p-3"
                        >
                          <span
                            className={cn(
                              'flex size-[21px] flex-none items-center justify-center border-2 text-white',
                              group.selectionType === 'SINGLE' ? 'rounded-full' : 'rounded-[7px]',
                            )}
                            style={{
                              borderColor: selected ? 'var(--acc)' : '#cdbfa6',
                              background: selected ? 'var(--acc)' : 'var(--card)',
                            }}
                          >
                            {selected && <Check className="size-[13px]" strokeWidth={3} />}
                          </span>
                          <span className="flex-1 text-left text-[14px] font-semibold text-ink">{item.name}</span>
                          <span className={cn('text-[12.5px] font-semibold', price > 0 ? 'text-ink-2' : 'text-ink-3')}>
                            {price > 0 ? `+ ${formatCurrency(price)}` : 'Grátis'}
                          </span>
                        </button>
                      )
                    })}
                </div>
                {unmet?.index === groupIndex && (
                  <p className="mt-2 text-[12px] font-semibold text-[#a05a4c]">
                    Escolha {group.selectionType === 'SINGLE' ? 'uma opção' : `até ${group.maxSelections ?? 'algumas opções'}`} em{' '}
                    {group.name}.
                  </p>
                )}
              </div>
            ))}
          </div>

          <div className="mt-5 flex items-center justify-between">
            <span className="text-[13.5px] font-bold text-ink-2">Quantidade</span>
            <div className="flex items-center gap-4 rounded-xl border border-line bg-card px-2 py-1.5">
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                disabled={quantity <= 1}
                aria-label="Diminuir quantidade"
                className="flex size-8 items-center justify-center rounded-[9px] bg-[#efe6d6] text-ink disabled:opacity-35"
              >
                <Minus className="size-[18px]" strokeWidth={2.4} />
              </button>
              <span className="min-w-5 text-center font-display text-[17px] font-extrabold text-ink">{quantity}</span>
              <button
                type="button"
                onClick={() => setQuantity((q) => q + 1)}
                aria-label="Aumentar quantidade"
                className="flex size-8 items-center justify-center rounded-[9px] bg-acc-tint text-acc-d"
              >
                <Plus className="size-[18px]" strokeWidth={2.4} />
              </button>
            </div>
          </div>

          <div className="mt-4">
            <p className="mb-2 text-xs font-bold text-ink-2">Alguma observação?</p>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Ex.: sem cebola, ponto da carne, capricha no molho…"
              className="h-16 w-full resize-none rounded-xl border-[1.5px] border-line bg-card p-3 text-sm text-ink outline-none focus:border-acc"
            />
          </div>
        </div>

        <div className="flex-none border-t border-line bg-cream px-[18px] pt-3 pb-[calc(12px+env(safe-area-inset-bottom))]">
          <button
            type="button"
            disabled={!!unmet}
            onClick={handleSave}
            className={cn(
              'w-full rounded-2xl py-4 text-[15.5px] font-extrabold text-white shadow-[0_10px_22px_-10px_var(--acc)]',
              unmet ? 'cursor-not-allowed bg-[#d8c3bb] shadow-none' : 'bg-acc',
            )}
          >
            {unmet ? `Escolha: ${unmet.group.name}` : `${editingLine ? 'Atualizar' : 'Adicionar'}  ·  ${formatCurrency(totalPreview)}`}
          </button>
        </div>
      </div>
    </div>
  )
}
