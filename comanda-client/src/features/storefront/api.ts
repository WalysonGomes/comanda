const STOREFRONT_BASE = '/api/loja'

export class StorefrontApiError extends Error {
  code: string
  status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = init?.body ? { 'Content-Type': 'application/json' } : undefined
  const response = await fetch(`${STOREFRONT_BASE}${path}`, { ...init, headers })

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new StorefrontApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Algo deu errado. Tente novamente.')
  }
  return data as T
}

export type BusinessInfo = {
  name: string
  logoUrl: string | null
  whatsappNumber: string | null
  deliveryFee: string
  minOrderValue: string
  open: boolean
  hoursLabel: string
  reopensLabel: string | null
}

export type SelectionType = 'SINGLE' | 'MULTIPLE'

export type StorefrontAdditionalItem = {
  id: number
  groupId: number
  name: string
  additionalPrice: string
  available: boolean
}

export type StorefrontAdditionalGroup = {
  id: number
  productId: number
  name: string
  required: boolean
  selectionType: SelectionType
  minSelections: number
  maxSelections: number | null
  items: StorefrontAdditionalItem[]
}

export type StorefrontProduct = {
  id: number
  name: string
  description: string | null
  price: string
  imageUrl: string | null
  additionalGroups: StorefrontAdditionalGroup[]
}

export type StorefrontCategory = {
  id: number
  name: string
  products: StorefrontProduct[]
}

export type DeliveryType = 'ENTREGA' | 'RETIRADA'

export type ValidateCartLinePayload = {
  lineId: string
  productId: number
  quantity: number
  additionalItemIds: number[]
}

export type ValidateCartPayload = {
  lines: ValidateCartLinePayload[]
  deliveryType: DeliveryType
}

export type ValidateCartLineResult = {
  lineId: string
  available: boolean
  unitPrice: string | null
  lineTotal: string | null
}

export type ValidateCartResult = {
  valid: boolean
  lines: ValidateCartLineResult[]
  subtotal: string
  deliveryFee: string
  total: string
}

/**
 * Contract declared by this change, implemented by `order-operation` (proposal.md Decision 7):
 * public, idempotent order creation. `POST /api/loja/pedidos` doesn't exist yet in this change's
 * backend — calling it is expected to fail until `order-operation` ships; the storefront's own
 * requirement (confirm "Pedido enviado" + always offer the WhatsApp copy fallback) doesn't depend
 * on it succeeding, so callers treat failures as non-blocking (see `CheckoutScreen.finalize`).
 */
export type CreateOrderPayload = {
  idempotencyKey: string
  customerName: string
  customerPhone: string
  deliveryType: DeliveryType
  address: string | null
  notes: string | null
  lines: ValidateCartLinePayload[]
}

export type CreateOrderResult = {
  id: string
  total: string
}

export const storefrontApi = {
  getBusiness: () => request<BusinessInfo>('/negocio'),
  getMenu: () => request<StorefrontCategory[]>('/cardapio'),
  validateCart: (payload: ValidateCartPayload) =>
    request<ValidateCartResult>('/carrinho/validar', { method: 'POST', body: JSON.stringify(payload) }),
  createOrder: (payload: CreateOrderPayload) =>
    request<CreateOrderResult>('/pedidos', { method: 'POST', body: JSON.stringify(payload) }),
}
