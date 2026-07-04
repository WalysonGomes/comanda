import { useState } from 'react'
import { ChevronDown, ChevronUp, ImageIcon, Pencil, Plus, UtensilsCrossed } from 'lucide-react'
import { useNavigate } from 'react-router'

import { PainelTabBar } from '@/components/PainelTabBar'
import { CategoryFormSheet } from '@/features/menu/CategoryFormSheet'
import { Skeleton } from '@/features/menu/components/Skeleton'
import { formatCurrency } from '@/features/menu/format'
import { MenuApiError, type Category, type Product } from '@/features/menu/api'
import {
  useCategories,
  useCreateCategory,
  useProducts,
  useReorderCategories,
  useSetProductAvailability,
  useUpdateCategory,
} from '@/features/menu/queries'

const PLACEHOLDER_BG = {
  backgroundImage:
    'repeating-linear-gradient(45deg, #f1e8d6, #f1e8d6 7px, #ece2cd 7px, #ece2cd 14px)',
}

export function MenuScreen() {
  const categoriesQuery = useCategories()
  const productsQuery = useProducts()
  const createCategory = useCreateCategory()
  const updateCategory = useUpdateCategory()
  const reorderCategories = useReorderCategories()
  const setAvailability = useSetProductAvailability()
  const navigate = useNavigate()

  const [sheetCategory, setSheetCategory] = useState<Category | null | 'new'>(null)
  const [sheetError, setSheetError] = useState<string | null>(null)

  const categories = categoriesQuery.data ?? []
  const products = productsQuery.data ?? []

  const groups = [...categories]
    .sort((a, b) => a.position - b.position)
    .map((category) => ({
      category,
      products: products
        .filter((p) => p.categoryId === category.id)
        .sort((a, b) => a.position - b.position),
    }))

  const isLoading = categoriesQuery.isLoading || productsQuery.isLoading

  function moveCategory(index: number, direction: -1 | 1) {
    const orderedIds = [...categories].sort((a, b) => a.position - b.position).map((c) => c.id)
    const target = index + direction
    if (target < 0 || target >= orderedIds.length) return
    ;[orderedIds[index], orderedIds[target]] = [orderedIds[target], orderedIds[index]]
    reorderCategories.mutate(orderedIds)
  }

  function saveCategory(name: string, active: boolean) {
    setSheetError(null)
    const isNew = sheetCategory === 'new'
    const mutation = isNew
      ? createCategory.mutateAsync({ name })
      : updateCategory.mutateAsync({ id: (sheetCategory as Category).id, body: { name, active } })
    mutation
      .then(() => setSheetCategory(null))
      .catch((e) => setSheetError(e instanceof MenuApiError ? e.message : 'Não foi possível salvar. Tente novamente.'))
  }

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <header className="flex-none px-4 pb-3 pt-5">
        <h1 className="font-display text-xl font-extrabold text-ink">Cardápio</h1>
      </header>

      <div className="flex-1 px-4 pb-8">
        {isLoading && <MenuSkeleton />}

        {!isLoading && groups.length === 0 && (
          <div className="flex flex-col items-center px-9 pb-10 pt-14 text-center text-ink-3">
            <div className="flex size-[62px] items-center justify-center rounded-[20px] bg-[#efe6d6] text-[#c2b49a]">
              <UtensilsCrossed className="size-7" strokeWidth={1.75} />
            </div>
            <p className="mt-4 text-base font-bold text-ink">Seu cardápio está vazio</p>
            <p className="mt-1.5 max-w-[240px] text-sm leading-normal">
              Crie categorias e produtos para o cardápio aparecer aqui.
            </p>
          </div>
        )}

        {!isLoading &&
          groups.map(({ category, products: categoryProducts }, index) => (
            <section key={category.id} className="mb-5">
              <div className="mb-2.5 flex items-center gap-2">
                <span className="text-[13px] font-extrabold tracking-wide text-ink-2 uppercase">{category.name}</span>
                <span className="rounded-md bg-[#efe6d6] px-1.5 py-0.5 font-mono text-[11px] font-bold text-ink-3">
                  {categoryProducts.length}
                </span>
                {!category.active && (
                  <span className="rounded-md bg-[#efe6d6] px-1.5 py-0.5 text-[11px] font-bold text-ink-3">Inativa</span>
                )}
                <div className="ml-auto flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => moveCategory(index, -1)}
                    disabled={index === 0}
                    aria-label="Mover categoria para cima"
                    className="flex size-7 items-center justify-center rounded-md text-ink-3 disabled:opacity-30"
                  >
                    <ChevronUp className="size-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => moveCategory(index, 1)}
                    disabled={index === groups.length - 1}
                    aria-label="Mover categoria para baixo"
                    className="flex size-7 items-center justify-center rounded-md text-ink-3 disabled:opacity-30"
                  >
                    <ChevronDown className="size-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setSheetCategory(category)}
                    aria-label="Editar categoria"
                    className="flex size-7 items-center justify-center rounded-md text-ink-3"
                  >
                    <Pencil className="size-[15px]" />
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate(`/painel/cardapio/produtos/novo?categoria=${category.id}`)}
                    aria-label="Novo produto nesta categoria"
                    className="flex size-7 items-center justify-center rounded-md text-acc-d"
                  >
                    <Plus className="size-4" />
                  </button>
                </div>
              </div>

              <div className="flex flex-col gap-2.5">
                {categoryProducts.map((product) => (
                  <ProductRow
                    key={product.id}
                    product={product}
                    onOpen={() => navigate(`/painel/cardapio/produtos/${product.id}`)}
                    onToggle={() => setAvailability.mutate({ id: product.id, available: !product.available })}
                  />
                ))}
                {categoryProducts.length === 0 && (
                  <p className="rounded-2xl border-[1.5px] border-dashed border-[#d3c4ab] px-4 py-4 text-center text-[13px] text-ink-3">
                    Nenhum produto nesta categoria ainda.
                  </p>
                )}
              </div>
            </section>
          ))}

        {!isLoading && (
          <button
            type="button"
            onClick={() => setSheetCategory('new')}
            className="flex w-full items-center justify-center gap-2 rounded-2xl border-[1.5px] border-dashed border-[#d3c4ab] py-3.5 text-[13.5px] font-bold text-ink-2"
          >
            <Plus className="size-[18px]" />
            Nova categoria
          </button>
        )}
      </div>

      <PainelTabBar current="cardapio" />

      {sheetCategory !== null && (
        <CategoryFormSheet
          category={sheetCategory === 'new' ? null : sheetCategory}
          saving={createCategory.isPending || updateCategory.isPending}
          errorMessage={sheetError}
          onSave={saveCategory}
          onClose={() => {
            setSheetCategory(null)
            setSheetError(null)
          }}
        />
      )}
    </div>
  )
}

function ProductRow({
  product,
  onOpen,
  onToggle,
}: {
  product: Product
  onOpen: () => void
  onToggle: () => void
}) {
  return (
    <div
      className="flex items-center gap-3 rounded-[15px] border border-line bg-card p-2.5"
      style={{ opacity: product.available ? 1 : 0.55 }}
    >
      <button
        type="button"
        onClick={onOpen}
        aria-label={`Editar ${product.name}`}
        className="flex size-14 flex-none items-center justify-center overflow-hidden rounded-[11px] text-[#c2b49a]"
        style={product.imageUrl ? undefined : PLACEHOLDER_BG}
      >
        {product.imageUrl ? (
          <img src={product.imageUrl} alt="" className="size-full object-cover" />
        ) : (
          <ImageIcon className="size-5" strokeWidth={1.75} />
        )}
      </button>
      <button type="button" onClick={onOpen} className="min-w-0 flex-1 text-left">
        <div className="truncate text-[15px] font-bold text-ink">{product.name}</div>
        <div className="truncate text-[12.5px] text-ink-3">{product.description}</div>
        <div className="mt-1.5 font-display text-sm font-extrabold text-ink">{formatCurrency(product.price)}</div>
      </button>
      <div className="flex flex-none flex-col items-end gap-1.5">
        <button
          type="button"
          onClick={onToggle}
          aria-label={product.available ? 'Marcar como indisponível' : 'Marcar como disponível'}
          className="relative h-[25px] w-[42px] rounded-full"
          style={{ background: product.available ? 'var(--acc)' : '#d9cdb6' }}
        >
          <span
            className="absolute top-[3px] size-[19px] rounded-full bg-white shadow transition-[left]"
            style={{ left: product.available ? '20px' : '3px' }}
          />
        </button>
        <span
          className="text-[10.5px] font-bold"
          style={{ color: product.available ? '#2f7a44' : '#a05a4c' }}
        >
          {product.available ? 'Disponível' : 'Indisponível'}
        </span>
      </div>
    </div>
  )
}

function MenuSkeleton() {
  return (
    <div className="flex flex-col gap-5">
      {[0, 1].map((section) => (
        <div key={section} className="flex flex-col gap-2.5">
          <Skeleton className="h-4 w-24" />
          {[0, 1, 2].map((row) => (
            <div key={row} className="flex items-center gap-3 rounded-[15px] border border-line bg-card p-2.5">
              <Skeleton className="size-14 flex-none" />
              <div className="flex-1">
                <Skeleton className="h-4 w-2/3" />
                <Skeleton className="mt-2 h-3 w-1/3" />
              </div>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
