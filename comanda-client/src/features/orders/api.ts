const PAINEL_BASE = '/api/painel'

export class OrdersApiError extends Error {
  code: string
  status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

export type OrderStatus = 'RECEBIDO' | 'ACEITO' | 'EM_PREPARO' | 'PRONTO' | 'ENTREGUE' | 'CANCELADO'
export type DeliveryType = 'ENTREGA' | 'RETIRADA'

export type OrderSummary = {
  id: number
  shortCode: string
  status: OrderStatus
  customerName: string
  itemsSummary: string
  deliveryType: DeliveryType
  total: string
  createdAt: string
  isNew: boolean
}

export type OrderAdditionalView = { name: string; price: string }
export type OrderItemView = {
  productName: string
  unitPrice: string
  quantity: number
  subtotal: string
  additionals: OrderAdditionalView[]
}

export type OrderDetail = {
  id: number
  shortCode: string
  status: OrderStatus
  customerName: string
  customerPhone: string
  deliveryType: DeliveryType
  address: string | null
  notes: string | null
  cancellationReason: string | null
  subtotal: string
  deliveryFee: string
  total: string
  createdAt: string
  items: OrderItemView[]
}

export type StatusCount = { status: OrderStatus; count: number }
export type DaySummary = { count: number; revenue: string; openCount: number }
export type OrdersBoard = {
  summary: DaySummary
  filters: StatusCount[]
  orders: OrderSummary[]
}

export type TenantStatus = {
  businessName: string
  open: boolean
  closedToday: boolean
  opensAt: string | null
  closesAt: string | null
}

async function request<T>(accessToken: string, path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)
  if (init?.body) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${PAINEL_BASE}${path}`, { ...init, headers })

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new OrdersApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Algo deu errado. Tente novamente.')
  }
  return data as T
}

export const ordersApi = {
  board: (token: string, status?: OrderStatus) =>
    request<OrdersBoard>(token, status ? `/orders?status=${status}` : '/orders'),
  detail: (token: string, id: number) => request<OrderDetail>(token, `/orders/${id}`),
  advance: (token: string, id: number, fromStatus: OrderStatus) =>
    request<OrderDetail>(token, `/orders/${id}/advance`, { method: 'POST', body: JSON.stringify({ fromStatus }) }),
  cancel: (token: string, id: number, reason: string) =>
    request<OrderDetail>(token, `/orders/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }),

  status: (token: string) => request<TenantStatus>(token, '/loja/status'),
  setOpen: (token: string, open: boolean) =>
    request<TenantStatus>(token, '/loja/status', { method: 'PATCH', body: JSON.stringify({ open }) }),
}
