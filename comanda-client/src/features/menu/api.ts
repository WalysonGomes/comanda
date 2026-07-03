const PAINEL_BASE = '/api/painel'

export class MenuApiError extends Error {
  code: string
  status: number

  constructor(status: number, code: string, message: string) {
    super(message)
    this.code = code
    this.status = status
  }
}

export type Category = {
  id: number
  name: string
  position: number
  active: boolean
}

export type Product = {
  id: number
  categoryId: number
  name: string
  description: string | null
  price: string
  imageUrl: string | null
  available: boolean
  position: number
  availableDays: number[] | null
}

export type SelectionType = 'SINGLE' | 'MULTIPLE'

export type AdditionalItem = {
  id: number
  groupId: number
  name: string
  additionalPrice: string
  available: boolean
}

export type AdditionalGroup = {
  id: number
  productId: number
  name: string
  required: boolean
  selectionType: SelectionType
  minSelections: number
  maxSelections: number | null
  items: AdditionalItem[]
}

async function request<T>(accessToken: string, path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)
  if (init?.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${PAINEL_BASE}${path}`, { ...init, headers })

  if (response.status === 204) {
    return undefined as T
  }
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new MenuApiError(response.status, data?.code ?? 'UNKNOWN_ERROR', data?.message ?? 'Algo deu errado. Tente novamente.')
  }
  return data as T
}

export type CategoryPayload = { name: string; active?: boolean }
export type ProductPayload = {
  name: string
  description: string | null
  price: string
  available?: boolean
  availableDays: number[] | null
  categoryId: number
  imageUrl: string | null
}
export type AdditionalGroupPayload = {
  name: string
  required: boolean
  selectionType: SelectionType
  minSelections: number
  maxSelections: number | null
}
export type AdditionalItemPayload = { name: string; additionalPrice: string; available?: boolean }

export const menuApi = {
  listCategories: (token: string) => request<Category[]>(token, '/categories'),
  createCategory: (token: string, body: CategoryPayload) =>
    request<Category>(token, '/categories', { method: 'POST', body: JSON.stringify(body) }),
  updateCategory: (token: string, id: number, body: CategoryPayload) =>
    request<Category>(token, `/categories/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteCategory: (token: string, id: number) => request<void>(token, `/categories/${id}`, { method: 'DELETE' }),
  reorderCategories: (token: string, orderedIds: number[]) =>
    request<void>(token, '/categories/reorder', { method: 'POST', body: JSON.stringify({ orderedIds }) }),

  listProducts: (token: string) => request<Product[]>(token, '/products'),
  createProduct: (token: string, body: ProductPayload) =>
    request<Product>(token, '/products', { method: 'POST', body: JSON.stringify(body) }),
  updateProduct: (token: string, id: number, body: ProductPayload) =>
    request<Product>(token, `/products/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  setProductAvailability: (token: string, id: number, available: boolean) =>
    request<Product>(token, `/products/${id}/availability`, { method: 'PATCH', body: JSON.stringify({ available }) }),
  deleteProduct: (token: string, id: number) => request<void>(token, `/products/${id}`, { method: 'DELETE' }),

  listAdditionalGroups: (token: string, productId: number) =>
    request<AdditionalGroup[]>(token, `/products/${productId}/additional-groups`),
  createAdditionalGroup: (token: string, productId: number, body: AdditionalGroupPayload) =>
    request<AdditionalGroup>(token, `/products/${productId}/additional-groups`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAdditionalGroup: (token: string, groupId: number, body: AdditionalGroupPayload) =>
    request<AdditionalGroup>(token, `/additional-groups/${groupId}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteAdditionalGroup: (token: string, groupId: number) =>
    request<void>(token, `/additional-groups/${groupId}`, { method: 'DELETE' }),

  createAdditionalItem: (token: string, groupId: number, body: AdditionalItemPayload) =>
    request<AdditionalItem>(token, `/additional-groups/${groupId}/items`, { method: 'POST', body: JSON.stringify(body) }),
  updateAdditionalItem: (token: string, itemId: number, body: AdditionalItemPayload) =>
    request<AdditionalItem>(token, `/additional-items/${itemId}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteAdditionalItem: (token: string, itemId: number) =>
    request<void>(token, `/additional-items/${itemId}`, { method: 'DELETE' }),

  uploadImage: (token: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<{ imageUrl: string }>(token, '/images', { method: 'POST', body: form })
  },
}
