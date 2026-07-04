import { useState } from 'react'
import { Bike, Clock, ShoppingBag, Wifi, WifiOff } from 'lucide-react'

import { PainelTabBar } from '@/components/PainelTabBar'
import { Skeleton } from '@/features/menu/components/Skeleton'
import { OrderDetailSheet } from '@/features/orders/OrderDetailSheet'
import type { OrderStatus, OrderSummary } from '@/features/orders/api'
import { EMPTY_STATE, FILTER_DEFS, STATUS_META, formatCurrency, formatRelativeTime } from '@/features/orders/format'
import { useOrdersBoard, useSetTenantOpen, useTenantStatus } from '@/features/orders/queries'
import { cn } from '@/lib/utils'

export function OrdersScreen() {
  const [filter, setFilter] = useState<OrderStatus | 'TODOS'>('TODOS')
  const [heroCollapsed, setHeroCollapsed] = useState(false)
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null)
  const board = useOrdersBoard(filter)
  const tenantStatus = useTenantStatus()
  const setOpen = useSetTenantOpen()

  const orders = board.data?.orders ?? []
  const isLoading = board.isPending
  const emptyKey = filter as keyof typeof EMPTY_STATE
  const [emptyTitle, emptyText] = EMPTY_STATE[emptyKey] ?? EMPTY_STATE.TODOS

  const online = !board.isError
  const lastUpdatedLabel = board.dataUpdatedAt ? formatRelativeTime(new Date(board.dataUpdatedAt).toISOString()) : '—'

  const status = tenantStatus.data
  const initials = (status?.businessName ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase()

  const showBanner = !!status && !status.open && !status.closedToday && isWithinHours(status.opensAt, status.closesAt)

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <header className="relative z-[4] flex-none border-b border-line" style={{ background: online ? 'transparent' : '#f7ecd2' }}>
        <div className="flex items-center gap-[11px] px-4 pt-[11px] pb-2">
          <div className="flex size-10 flex-none items-center justify-center rounded-xl bg-acc font-display text-[15px] font-extrabold text-white">
            {initials || '—'}
          </div>
          <div className="min-w-0 flex-1">
            <div className="truncate font-display text-[17px] font-extrabold text-ink">{status?.businessName ?? ''}</div>
          </div>
          <div
            className="flex flex-none items-center gap-0.5 rounded-[11px] border border-line bg-[#efe6d6] p-[3px] select-none"
            role="group"
            aria-label="Alternar loja aberta ou fechada"
          >
            <button
              type="button"
              onClick={() => setOpen.mutate(true)}
              className="flex items-center gap-[5px] rounded-lg px-2.5 py-1.5 text-xs font-bold"
              style={status?.open ? { background: '#1f8a52', color: '#fff' } : { color: 'var(--ink3)' }}
            >
              <span className="size-1.5 rounded-full" style={{ background: status?.open ? '#fff' : '#c2b49a' }} />
              Aberto
            </button>
            <button
              type="button"
              onClick={() => setOpen.mutate(false)}
              className="rounded-lg px-2.5 py-1.5 text-xs font-bold"
              style={!status?.open ? { background: '#5a5048', color: '#fff' } : { color: 'var(--ink3)' }}
            >
              Fechado
            </button>
          </div>
        </div>
        <div className="flex items-center gap-[7px] px-4 pb-[9px]" style={{ color: online ? '#6a8f5a' : '#9a6510' }}>
          {online ? <Wifi className="size-[15px] flex-none" /> : <WifiOff className="size-[15px] flex-none" />}
          <span className="truncate text-[11.5px] font-semibold">
            {online ? 'Atualização automática ativa' : 'Sem conexão — tentando reconectar'}
          </span>
          <span className="ml-auto flex-none font-mono text-[10.5px] font-semibold opacity-70">
            {online ? `Atualizado ${lastUpdatedLabel}` : `última ${lastUpdatedLabel}`}
          </span>
        </div>
      </header>

      {showBanner && (
        <div className="mx-3.5 mt-3 flex items-center gap-[11px] rounded-2xl bg-[#2c2520] p-3 shadow-lg">
          <Clock className="size-5 flex-none text-[#ffb38a]" />
          <div className="min-w-0 flex-1">
            <div className="text-[13px] font-bold leading-tight text-[#f4ece0]">Cardápio fechado</div>
            <div className="mt-0.5 text-[11.5px] leading-snug text-[#b6a890]">
              Você está no horário de hoje ({status?.opensAt?.slice(0, 5)}–{status?.closesAt?.slice(0, 5)})
            </div>
          </div>
          <button
            type="button"
            onClick={() => setOpen.mutate(true)}
            className="flex-none rounded-[10px] bg-acc px-[13px] py-2.5 text-[12.5px] font-bold text-white"
          >
            Abrir agora
          </button>
        </div>
      )}

      <div className="scr flex-1 overflow-y-auto overflow-x-hidden">
        {board.data && (
          <button
            type="button"
            onClick={() => setHeroCollapsed((v) => !v)}
            className="mx-[18px] mt-[15px] flex items-center gap-2 whitespace-nowrap text-[13px] font-semibold text-ink-2"
          >
            <span className="font-mono text-[10.5px] font-bold uppercase tracking-wider text-ink-3">Hoje</span>
            {!heroCollapsed && (
              <>
                <span>{board.data.summary.count} pedidos</span>
                <Dot />
                <span className="font-bold text-ink">{formatCurrency(board.data.summary.revenue)}</span>
                <Dot />
                <span className="font-bold text-acc-d">{board.data.summary.openCount} em aberto</span>
              </>
            )}
          </button>
        )}

        <div className="chips flex gap-2 overflow-x-auto px-4 pt-[13px] pb-1">
          {FILTER_DEFS.map((f) => {
            const active = filter === f.key
            const count = f.key === 'TODOS' ? board.data?.summary.count ?? 0 : board.data?.filters.find((s) => s.status === f.key)?.count ?? 0
            return (
              <button
                key={f.key}
                type="button"
                onClick={() => setFilter(f.key)}
                className="flex flex-none items-center gap-1.5 rounded-[11px] px-[13px] py-2 text-[13px] font-bold"
                style={active ? { background: 'var(--acc)', color: '#fff' } : { background: 'var(--card)', color: 'var(--ink-2)', border: '1px solid var(--line)' }}
              >
                <span>{f.label}</span>
                <span
                  className="rounded-md px-[5px] py-0.5 font-mono text-[11px] font-bold"
                  style={active ? { background: 'rgba(255,255,255,.24)', color: '#fff' } : { background: '#efe6d6', color: 'var(--ink-3)' }}
                >
                  {count}
                </span>
              </button>
            )
          })}
        </div>

        {isLoading && <OrdersSkeleton />}

        {!isLoading && orders.length === 0 && (
          <div className="flex flex-col items-center px-9 pb-10 pt-[54px] text-center text-ink-3">
            <div className="flex size-[62px] items-center justify-center rounded-[20px] bg-[#efe6d6] text-[#c2b49a]">
              <ShoppingBag className="size-[30px]" strokeWidth={1.75} />
            </div>
            <p className="mt-4 text-base font-bold text-ink">{emptyTitle}</p>
            <p className="mt-1.5 max-w-[240px] text-[13.5px] leading-normal">{emptyText}</p>
          </div>
        )}

        {!isLoading && orders.length > 0 && (
          <div className="flex flex-col gap-3 px-4 pb-[22px] pt-2">
            {orders.map((order) => (
              <OrderCard key={order.id} order={order} onOpen={() => setSelectedOrderId(order.id)} />
            ))}
          </div>
        )}
      </div>

      <PainelTabBar current="pedidos" />

      {selectedOrderId !== null && (
        <OrderDetailSheet orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} />
      )}
    </div>
  )
}

function Dot() {
  return <span className="size-[3px] flex-none rounded-full bg-ink-3 opacity-45" />
}

function OrderCard({ order, onOpen }: { order: OrderSummary; onOpen: () => void }) {
  const meta = STATUS_META[order.status]
  const isDelivery = order.deliveryType === 'ENTREGA'
  return (
    <button
      type="button"
      onClick={onOpen}
      className={cn(
        'relative overflow-hidden rounded-2xl border bg-card p-[15px] pl-[18px] text-left shadow-sm',
        order.isNew && 'animate-cmd-pulse',
      )}
      style={{ borderColor: order.isNew ? 'rgba(214,73,47,.35)' : 'var(--line)' }}
    >
      <span className="absolute inset-y-0 left-0 w-[5px]" style={{ background: meta.accent }} />
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="font-mono text-[13px] font-bold text-ink-2">{order.shortCode}</span>
          {order.isNew && (
            <span className="rounded-[5px] bg-acc px-1.5 py-0.5 text-[10px] font-extrabold tracking-wide text-white">NOVO</span>
          )}
        </div>
        <span
          className="inline-flex items-center gap-[5px] rounded-lg px-[9px] py-[5px] text-[11.5px] font-bold"
          style={{ background: meta.bg, color: meta.fg }}
        >
          <span className="size-1.5 rounded-full" style={{ background: meta.accent }} />
          {meta.label}
        </span>
      </div>
      <div className="mt-[11px] text-[16.5px] font-bold text-ink">{order.customerName}</div>
      <div className="mt-[3px] truncate text-[13px] text-ink-2">{order.itemsSummary}</div>
      <div className="mt-[13px] flex items-center justify-between">
        <span className="inline-flex items-center gap-1.5 text-[12.5px] font-semibold text-ink-2">
          {isDelivery ? <Bike className="size-4 text-ink-3" /> : <ShoppingBag className="size-4 text-ink-3" />}
          {isDelivery ? 'Entrega' : 'Retirada'} · {formatRelativeTime(order.createdAt)}
        </span>
        <span className="font-display text-base font-extrabold text-ink">{formatCurrency(order.total)}</span>
      </div>
    </button>
  )
}

function OrdersSkeleton() {
  return (
    <div className="flex flex-col gap-3 px-4 pb-4 pt-2">
      {[0, 1].map((i) => (
        <div key={i} className="rounded-2xl border border-line bg-card p-[15px]">
          <div className="flex justify-between">
            <Skeleton className="h-3.5 w-16" />
            <Skeleton className="h-[22px] w-[58px] rounded-lg" />
          </div>
          <Skeleton className="mt-[13px] h-[17px] w-[55%]" />
          <Skeleton className="mt-2.5 h-[13px] w-4/5" />
          <div className="mt-3.5 flex justify-between">
            <Skeleton className="h-[13px] w-20" />
            <Skeleton className="h-[18px] w-[70px]" />
          </div>
        </div>
      ))}
    </div>
  )
}

/** Fallback client-side clock (PRD Seção 13: fuso hardcoded, sem correção de dispositivo no MVP). */
function isWithinHours(opensAt: string | null, closesAt: string | null): boolean {
  if (!opensAt || !closesAt) return false
  const now = new Date().toTimeString().slice(0, 8)
  return now >= opensAt && now <= closesAt
}
