import { useState } from 'react'
import { ShoppingBag, UtensilsCrossed } from 'lucide-react'

import { formatCurrency } from '@/features/menu/format'
import type { StorefrontCategory, StorefrontProduct } from '@/features/storefront/api'
import { CartSheet } from '@/features/storefront/CartSheet'
import { CheckoutScreen } from '@/features/storefront/CheckoutScreen'
import { ClosedNotice } from '@/features/storefront/ClosedNotice'
import { HomeScreen } from '@/features/storefront/HomeScreen'
import { ProductSheet } from '@/features/storefront/ProductSheet'
import { lineTotal, useCartStore, type CartLine } from '@/features/storefront/cart'
import { useBusiness, useMenu } from '@/features/storefront/queries'
import { SentScreen } from '@/features/storefront/SentScreen'

type View = 'menu' | 'checkout' | 'sent'
type ProductSheetState = { product: StorefrontProduct; editingLine: CartLine | null }
type SentState = { message: string; whatsappUrl: string | null }

function findProduct(menu: StorefrontCategory[] | undefined, productId: number): StorefrontProduct | undefined {
  for (const category of menu ?? []) {
    const product = category.products.find((p) => p.id === productId)
    if (product) return product
  }
  return undefined
}

/**
 * Root of the public storefront (`nomedonegocio.$APP_DOMAIN`): orchestrates the menu → product
 * sheet → cart → checkout → sent flow as view/overlay state within a single page (the design's
 * blocks are sheets over the home, not separate deep-linkable routes — nothing in the spec
 * requires otherwise, and it keeps checkout/cart state trivially in sync).
 */
export function StorefrontShell() {
  const businessQuery = useBusiness()
  const menuQuery = useMenu()
  const business = businessQuery.data
  const menu = menuQuery.data

  const [view, setView] = useState<View>('menu')
  const [productSheet, setProductSheet] = useState<ProductSheetState | null>(null)
  const [cartOpen, setCartOpen] = useState(false)
  const [closedNoticeDismissed, setClosedNoticeDismissed] = useState(false)
  const [sent, setSent] = useState<SentState | null>(null)

  const lines = useCartStore((s) => s.lines)
  const deliveryType = useCartStore((s) => s.deliveryType)
  const addOrUpdateLine = useCartStore((s) => s.addOrUpdateLine)
  const clearCart = useCartStore((s) => s.clear)

  if (businessQuery.isError) {
    return (
      <div className="flex min-h-svh flex-col items-center justify-center gap-3 bg-cream px-6 text-center">
        <UtensilsCrossed className="size-9 text-ink-3" strokeWidth={1.75} />
        <h1 className="font-display text-xl font-extrabold text-ink">Cardápio não encontrado</h1>
        <p className="max-w-xs text-sm text-ink-2">Confira o link e tente novamente.</p>
      </div>
    )
  }

  const cartCount = lines.reduce((sum, line) => sum + line.quantity, 0)
  const cartQuantityByProduct = lines.reduce<Record<number, number>>((acc, line) => {
    acc[line.productId] = (acc[line.productId] ?? 0) + line.quantity
    return acc
  }, {})
  const deliveryFee = business && deliveryType === 'ENTREGA' ? Number(business.deliveryFee) : 0
  const cartTotal = lines.reduce((sum, line) => sum + lineTotal(line), 0) + deliveryFee

  const showCartBar = view === 'menu' && cartCount > 0 && !productSheet && !cartOpen
  const showClosedNotice = view === 'menu' && !!business && !business.open && !closedNoticeDismissed

  function editLine(line: CartLine) {
    const product = findProduct(menu, line.productId)
    if (!product) return
    setProductSheet({ product, editingLine: line })
    setCartOpen(false)
  }

  function saveLine(line: CartLine) {
    addOrUpdateLine(line)
    setProductSheet(null)
  }

  function handleSent(message: string, whatsappUrl: string | null) {
    setSent({ message, whatsappUrl })
    setView('sent')
  }

  function handleNewOrder() {
    clearCart()
    setSent(null)
    setClosedNoticeDismissed(false)
    setView('menu')
  }

  return (
    <div className="relative mx-auto flex min-h-svh max-w-md flex-col overflow-hidden bg-cream">
      {view === 'menu' && (
        <HomeScreen
          business={business}
          menu={menu}
          isLoading={businessQuery.isLoading || menuQuery.isLoading}
          cartQuantityByProduct={cartQuantityByProduct}
          onOpenProduct={(product) => setProductSheet({ product, editingLine: null })}
        />
      )}

      {view === 'checkout' && business && (
        <CheckoutScreen
          business={business}
          onBack={() => {
            setView('menu')
            setCartOpen(true)
          }}
          onSent={handleSent}
        />
      )}

      {view === 'sent' && sent && <SentScreen message={sent.message} whatsappUrl={sent.whatsappUrl} onNewOrder={handleNewOrder} />}

      {showCartBar && (
        <button
          type="button"
          onClick={() => setCartOpen(true)}
          className="absolute right-3.5 bottom-8 left-3.5 z-[15] flex items-center justify-between rounded-2xl bg-acc px-4 py-3.5 text-white shadow-[0_16px_30px_-12px_var(--acc)] animate-cmd-pop"
        >
          <span className="flex items-center gap-2.5">
            <span className="relative flex">
              <ShoppingBag className="size-[21px]" />
              <span className="absolute -top-1.5 -right-2 flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-white px-1 text-[10px] font-extrabold text-acc-d">
                {cartCount}
              </span>
            </span>
            <span className="text-sm font-bold">Ver carrinho · {cartCount} {cartCount === 1 ? 'item' : 'itens'}</span>
          </span>
          <span className="font-display text-base font-extrabold">{formatCurrency(cartTotal)}</span>
        </button>
      )}

      {showClosedNotice && (
        <ClosedNotice
          reopensLabel={business!.reopensLabel}
          bottom={showCartBar ? 104 : 36}
          onDismiss={() => setClosedNoticeDismissed(true)}
        />
      )}

      {productSheet && (
        <ProductSheet
          product={productSheet.product}
          editingLine={productSheet.editingLine}
          onClose={() => setProductSheet(null)}
          onSave={saveLine}
        />
      )}

      {cartOpen && business && (
        <CartSheet
          businessOpen={business.open}
          minOrderValue={Number(business.minOrderValue)}
          deliveryFee={Number(business.deliveryFee)}
          onClose={() => setCartOpen(false)}
          onEditLine={editLine}
          onCheckout={() => {
            setCartOpen(false)
            setView('checkout')
          }}
        />
      )}
    </div>
  )
}
