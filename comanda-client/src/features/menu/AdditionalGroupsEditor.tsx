import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'

import { formatCurrency } from '@/features/menu/format'
import type { AdditionalGroup, SelectionType } from '@/features/menu/api'
import {
  useAdditionalGroups,
  useCreateAdditionalGroup,
  useCreateAdditionalItem,
  useDeleteAdditionalGroup,
  useDeleteAdditionalItem,
} from '@/features/menu/queries'

const inputClass =
  'w-full rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3 text-[15px] text-ink outline-none focus:border-acc'

/** Groups/items only exist once the product itself exists (backend paths hang off `productId`). */
export function AdditionalGroupsEditor({ productId }: { productId?: number }) {
  const groupsQuery = useAdditionalGroups(productId)
  const createGroup = useCreateAdditionalGroup(productId ?? 0)
  const deleteGroup = useDeleteAdditionalGroup(productId ?? 0)
  const createItem = useCreateAdditionalItem(productId ?? 0)
  const deleteItem = useDeleteAdditionalItem(productId ?? 0)

  const [showNewGroup, setShowNewGroup] = useState(false)
  const [groupError, setGroupError] = useState<string | null>(null)

  return (
    <div className="mt-6">
      <div className="mb-2.5 flex items-center justify-between">
        <span className="text-xs font-extrabold tracking-wide text-ink-2 uppercase">Grupos de adicionais</span>
        {productId && (
          <button
            type="button"
            onClick={() => setShowNewGroup(true)}
            className="flex items-center gap-1 text-[12.5px] font-bold text-acc-d"
          >
            <Plus className="size-4" /> Grupo
          </button>
        )}
      </div>

      {!productId && (
        <p className="rounded-2xl border-[1.5px] border-dashed border-[#d3c4ab] px-4 py-5 text-center text-[13px] text-ink-3">
          Salve o produto primeiro para adicionar grupos de adicionais.
        </p>
      )}

      {productId && (
        <div className="flex flex-col gap-3">
          {(groupsQuery.data ?? []).map((group) => (
            <GroupCard
              key={group.id}
              group={group}
              onDelete={() => deleteGroup.mutate(group.id)}
              onAddItem={(name, price) =>
                createItem.mutate({ groupId: group.id, body: { name, additionalPrice: price } })
              }
              onDeleteItem={(itemId) => deleteItem.mutate(itemId)}
            />
          ))}
          {(groupsQuery.data ?? []).length === 0 && !showNewGroup && (
            <p className="rounded-2xl border-[1.5px] border-dashed border-[#d3c4ab] px-4 py-5 text-center text-[13px] text-ink-3">
              Nenhum adicional ainda. Toque em &quot;Grupo&quot; para criar.
            </p>
          )}

          {showNewGroup && (
            <NewGroupForm
              saving={createGroup.isPending}
              errorMessage={groupError}
              onCancel={() => {
                setShowNewGroup(false)
                setGroupError(null)
              }}
              onCreate={(body) => {
                setGroupError(null)
                createGroup.mutate(body, {
                  onSuccess: () => setShowNewGroup(false),
                  onError: (e: unknown) =>
                    setGroupError(e instanceof Error ? e.message : 'Não foi possível criar o grupo.'),
                })
              }}
            />
          )}
        </div>
      )}
    </div>
  )
}

function GroupCard({
  group,
  onDelete,
  onAddItem,
  onDeleteItem,
}: {
  group: AdditionalGroup
  onDelete: () => void
  onAddItem: (name: string, price: string) => void
  onDeleteItem: (itemId: number) => void
}) {
  const [showNewItem, setShowNewItem] = useState(false)
  const [itemName, setItemName] = useState('')
  const [itemPrice, setItemPrice] = useState('')

  return (
    <div className="rounded-[15px] border border-line bg-card p-3.5">
      <div className="flex items-center gap-2">
        <span className="text-[14.5px] font-bold text-ink">{group.name}</span>
        <span
          className="rounded-md px-1.5 py-0.5 text-[10px] font-bold tracking-wide uppercase"
          style={
            group.required ? { background: '#fbe6df', color: 'var(--acc-d)' } : { background: '#efe6d6', color: '#8a7c66' }
          }
        >
          {group.required ? 'Obrigatório' : 'Opcional'}
        </span>
        <span className="ml-auto text-[11.5px] font-semibold text-ink-3">
          {group.selectionType === 'SINGLE' ? 'Seleção única' : 'Seleção múltipla'}
        </span>
        <button type="button" onClick={onDelete} aria-label="Remover grupo" className="text-ink-3">
          <Trash2 className="size-4" />
        </button>
      </div>

      <div className="my-2.5 h-px bg-line" />

      <div className="flex flex-col gap-2">
        {group.items.map((item) => (
          <div key={item.id} className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-[13.5px] text-ink-2">
              <span className="size-[5px] rounded-full bg-[#c9b89a]" />
              {item.name}
            </span>
            <div className="flex items-center gap-2">
              <span className="text-[12.5px] font-semibold text-ink-2">{formatCurrency(item.additionalPrice)}</span>
              <button type="button" onClick={() => onDeleteItem(item.id)} aria-label={`Remover ${item.name}`} className="text-ink-3">
                <Trash2 className="size-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {showNewItem ? (
        <div className="mt-2.5 flex items-center gap-2">
          <input
            value={itemName}
            onChange={(e) => setItemName(e.target.value)}
            placeholder="Nome do item"
            className="min-w-0 flex-1 rounded-lg border border-line bg-cream px-2.5 py-2 text-[13px]"
          />
          <input
            value={itemPrice}
            onChange={(e) => setItemPrice(e.target.value)}
            placeholder="0,00"
            className="w-20 rounded-lg border border-line bg-cream px-2.5 py-2 text-[13px]"
          />
          <button
            type="button"
            onClick={() => {
              if (itemName.trim().length === 0) return
              onAddItem(itemName.trim(), itemPrice.replace(',', '.') || '0')
              setItemName('')
              setItemPrice('')
              setShowNewItem(false)
            }}
            className="rounded-lg bg-acc px-3 py-2 text-[12.5px] font-bold text-white"
          >
            Add
          </button>
        </div>
      ) : (
        <button type="button" onClick={() => setShowNewItem(true)} className="mt-2.5 text-[12.5px] font-bold text-acc-d">
          + item
        </button>
      )}
    </div>
  )
}

function NewGroupForm({
  saving,
  errorMessage,
  onCreate,
  onCancel,
}: {
  saving: boolean
  errorMessage: string | null
  onCreate: (body: {
    name: string
    required: boolean
    selectionType: SelectionType
    minSelections: number
    maxSelections: number | null
  }) => void
  onCancel: () => void
}) {
  const [name, setName] = useState('')
  const [required, setRequired] = useState(false)
  const [selectionType, setSelectionType] = useState<SelectionType>('SINGLE')
  const [minSelections, setMinSelections] = useState('0')
  const [maxSelections, setMaxSelections] = useState('')

  return (
    <div className="rounded-[15px] border border-line bg-card p-3.5">
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Nome do grupo (ex: Ponto da carne)"
        className={inputClass}
      />
      <div className="mt-2.5 flex items-center gap-3">
        <label className="flex items-center gap-1.5 text-[13px] font-semibold text-ink-2">
          <input type="checkbox" checked={required} onChange={(e) => setRequired(e.target.checked)} />
          Obrigatório
        </label>
        <select
          value={selectionType}
          onChange={(e) => setSelectionType(e.target.value as SelectionType)}
          className="rounded-lg border border-line bg-cream px-2 py-1.5 text-[13px]"
        >
          <option value="SINGLE">Seleção única</option>
          <option value="MULTIPLE">Seleção múltipla</option>
        </select>
      </div>
      <div className="mt-2.5 flex items-center gap-2">
        <input
          type="number"
          min={0}
          value={minSelections}
          onChange={(e) => setMinSelections(e.target.value)}
          placeholder="Mín."
          className="w-20 rounded-lg border border-line bg-cream px-2.5 py-2 text-[13px]"
        />
        <input
          type="number"
          min={0}
          value={maxSelections}
          onChange={(e) => setMaxSelections(e.target.value)}
          placeholder="Máx. (opcional)"
          className="w-32 rounded-lg border border-line bg-cream px-2.5 py-2 text-[13px]"
        />
      </div>
      {errorMessage && <p className="mt-2 text-[12.5px] font-semibold text-[#a05a4c]">{errorMessage}</p>}
      <div className="mt-3 flex gap-2">
        <button
          type="button"
          disabled={saving || name.trim().length === 0}
          onClick={() =>
            onCreate({
              name: name.trim(),
              required,
              selectionType,
              minSelections: Number(minSelections) || 0,
              maxSelections: maxSelections.trim() === '' ? null : Number(maxSelections),
            })
          }
          className="rounded-lg bg-acc px-3.5 py-2 text-[13px] font-bold text-white disabled:opacity-60"
        >
          {saving ? 'Salvando…' : 'Salvar grupo'}
        </button>
        <button type="button" onClick={onCancel} className="rounded-lg border border-line px-3.5 py-2 text-[13px] font-bold text-ink-2">
          Cancelar
        </button>
      </div>
    </div>
  )
}
