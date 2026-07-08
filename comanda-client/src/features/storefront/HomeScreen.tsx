import { useState } from 'react'
import { Bike, ImageIcon, ShoppingBag, UtensilsCrossed } from 'lucide-react'

import { cn } from '@/lib/utils'
import { formatCurrency } from '@/features/menu/format'
import type { BusinessInfo, StorefrontCategory, StorefrontProduct } from '@/features/storefront/api'
import { Skeleton } from '@/features/storefront/components/Skeleton'
import { Footer } from '@/features/storefront/Footer'

const PLACEHOLDER_BG = {
  backgroundImage: 'repeating-linear-gradient(45deg, #f1e8d6, #f1e8d6 8px, #ece2cd 8px, #ece2cd 16px)',
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0])
    .join('')
    .toUpperCase()
}

/** Storefront home (block STOREFRONT, tasks 7.1-7.5): business header + status, sticky category
 * chips, and the product list grouped by category. */
export function HomeScreen({
  business,
  menu,
  isLoading,
  cartQuantityByProduct,
  onOpenProduct,
}: {
  business: BusinessInfo | undefined
  menu: StorefrontCategory[] | undefined
  isLoading: boolean
  cartQuantityByProduct: Record<number, number>
  onOpenProduct: (product: StorefrontProduct) => void
}) {
  const [activeCategory, setActiveCategory] = useState<'all' | number>('all')

  if (isLoading || !business || !menu) {
    return <HomeSkeleton />
  }

  const visibleCategories = activeCategory === 'all' ? menu : menu.filter((c) => c.id === activeCategory)

  return (
    <div className="flex-1 overflow-x-hidden overflow-y-auto bg-cream">
      <div className="relative h-[120px] overflow-hidden bg-gradient-to-br from-[#3a3028] to-[#241d18]">
        <div className="absolute inset-0 bg-gradient-to-b from-[rgba(247,241,230,0)] via-[rgba(247,241,230,.5)] to-cream" />
      </div>

      <div className="px-4">
        <div className="relative -mt-10 flex items-end gap-3.5">
          <div className="flex size-[72px] flex-none items-center justify-center rounded-[20px] border-[3px] border-cream bg-acc font-display text-2xl font-extrabold text-white shadow-[0_10px_22px_-8px_rgba(60,42,24,.55)]">
            {business.logoUrl ? (
              <img src={business.logoUrl} alt="" className="size-full rounded-[17px] object-cover" />
            ) : (
              initials(business.name)
            )}
          </div>
          <div className="min-w-0 flex-1 pb-1">
            <div className="truncate font-display text-xl font-black text-ink">{business.name}</div>
            <div className="mt-1.5 flex items-center gap-2">
              <span
                className="inline-flex items-center gap-1.5 rounded-[7px] px-2.5 py-1 text-[11px] font-bold whitespace-nowrap"
                style={{
                  background: business.open ? '#dcefe1' : '#f0e1dc',
                  color: business.open ? '#176b41' : '#8a4a3d',
                }}
              >
                <span className="size-1.5 rounded-full" style={{ background: business.open ? '#1f8a52' : '#a05a4c' }} />
                {business.open ? 'Aberto agora' : 'Fechado agora'}
              </span>
              <span className="truncate text-xs text-ink-3">{business.hoursLabel}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-x-3.5 gap-y-1.5 border-t border-b border-line px-4 py-2.5 text-xs font-semibold text-ink-2">
        <span className="inline-flex items-center gap-1.5">
          <Bike className="size-[15px] text-ink-3" />
          Entrega {formatCurrency(business.deliveryFee)}
        </span>
        <span className="size-[3px] flex-none rounded-full bg-ink-3 opacity-45" />
        <span className="inline-flex items-center gap-1.5">
          <ShoppingBag className="size-[15px] text-ink-3" />
          Retirada
        </span>
        <span className="size-[3px] flex-none rounded-full bg-ink-3 opacity-45" />
        <span>Mín. {formatCurrency(business.minOrderValue)}</span>
      </div>

      <div className="sticky top-0 z-10 flex gap-2 overflow-x-auto bg-cream/95 px-4 pt-4 pb-1.5 backdrop-blur-sm">
        <button
          type="button"
          onClick={() => setActiveCategory('all')}
          className={cn(
            'flex-none rounded-[11px] px-3.5 py-2 text-[13px] font-bold',
            activeCategory === 'all' ? 'border border-acc bg-acc text-white' : 'border border-line bg-card text-ink-2',
          )}
        >
          Todos
        </button>
        {menu.map((category) => (
          <button
            type="button"
            key={category.id}
            onClick={() => setActiveCategory(category.id)}
            className={cn(
              'flex-none rounded-[11px] px-3.5 py-2 text-[13px] font-bold',
              activeCategory === category.id ? 'border border-acc bg-acc text-white' : 'border border-line bg-card text-ink-2',
            )}
          >
            {category.name}
          </button>
        ))}
      </div>

      <div className="px-4 pt-1.5">
        {menu.length === 0 && (
          <div className="flex flex-col items-center px-9 pt-14 pb-10 text-center text-ink-3">
            <div className="flex size-[62px] items-center justify-center rounded-[20px] bg-[#efe6d6] text-[#c2b49a]">
              <UtensilsCrossed className="size-7" strokeWidth={1.75} />
            </div>
            <p className="mt-4 text-base font-bold text-ink">Cardápio indisponível no momento</p>
            <p className="mt-1.5 max-w-[240px] text-sm leading-normal">Volte mais tarde para ver os produtos disponíveis hoje.</p>
          </div>
        )}

        {visibleCategories.map((category) => (
          <div key={category.id} className="mb-5">
            <div className="mt-1.5 mb-2.5 text-[13px] font-extrabold tracking-wide text-ink-2 uppercase">{category.name}</div>
            <div className="flex flex-col gap-2.5">
              {category.products.map((product) => {
                const inCart = cartQuantityByProduct[product.id] ?? 0
                return (
                  <button
                    type="button"
                    key={product.id}
                    onClick={() => onOpenProduct(product)}
                    className="flex gap-3.5 rounded-2xl border border-line bg-card p-3 text-left shadow-[0_2px_10px_-7px_rgba(60,42,24,.35)]"
                  >
                    <div
                      className="flex size-[84px] flex-none items-center justify-center rounded-[13px] text-[#c2b49a]"
                      style={product.imageUrl ? undefined : PLACEHOLDER_BG}
                    >
                      {product.imageUrl ? (
                        <img src={product.imageUrl} alt="" className="size-full rounded-[13px] object-cover" />
                      ) : (
                        <ImageIcon className="size-6" strokeWidth={1.75} />
                      )}
                    </div>
                    <div className="flex min-w-0 flex-1 flex-col">
                      <div className="text-[15.5px] font-bold text-ink">{product.name}</div>
                      {product.description && (
                        <div className="mt-0.5 line-clamp-2 text-[12.5px] leading-normal text-ink-3">{product.description}</div>
                      )}
                      <div className="mt-auto flex items-center justify-between pt-2">
                        <span className="font-display text-[15px] font-extrabold text-ink">{formatCurrency(product.price)}</span>
                        <div className="flex items-center gap-2">
                          {inCart > 0 && (
                            <span className="text-[11px] font-bold text-acc-d">
                              {inCart} no carrinho
                            </span>
                          )}
                          <span className="flex size-[30px] items-center justify-center rounded-[9px] bg-acc-tint text-acc-d">+</span>
                        </div>
                      </div>
                    </div>
                  </button>
                )
              })}
            </div>
          </div>
        ))}
      </div>

      <Footer />
      <div className="h-24" />
    </div>
  )
}

function HomeSkeleton() {
  return (
    <div className="flex-1 overflow-y-auto bg-cream px-4 pt-4">
      <div className="flex items-end gap-3.5">
        <Skeleton className="size-[72px] flex-none rounded-[20px]" />
        <div className="flex-1">
          <Skeleton className="h-5 w-2/3" />
          <Skeleton className="mt-2 h-3 w-1/3" />
        </div>
      </div>
      <div className="mt-6 flex flex-col gap-5">
        {[0, 1].map((section) => (
          <div key={section} className="flex flex-col gap-2.5">
            <Skeleton className="h-4 w-24" />
            {[0, 1].map((row) => (
              <div key={row} className="flex items-center gap-3 rounded-2xl border border-line bg-card p-3">
                <Skeleton className="size-[84px] flex-none" />
                <div className="flex-1">
                  <Skeleton className="h-4 w-2/3" />
                  <Skeleton className="mt-2 h-3 w-1/3" />
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}
