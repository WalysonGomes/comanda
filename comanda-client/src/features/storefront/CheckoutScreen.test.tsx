import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { StorefrontApiError, storefrontApi, type BusinessInfo, type ValidateCartResult } from '@/features/storefront/api'
import { useCartStore, type CartLine } from '@/features/storefront/cart'
import { CheckoutScreen } from '@/features/storefront/CheckoutScreen'

const validateCartMock = vi.hoisted(() => vi.fn())

vi.mock('@/features/storefront/queries', () => ({
  useValidateCart: () => ({ mutateAsync: validateCartMock }),
}))

const business: BusinessInfo = {
  name: 'Bistrô Teste',
  logoUrl: null,
  whatsappNumber: '5585999990000',
  deliveryFee: '7.00',
  minOrderValue: '0.00',
  open: true,
  hoursLabel: 'Aberto hoje',
  reopensLabel: null,
}

const cartLine: CartLine = {
  lineId: 'line-1',
  productId: 10,
  name: 'Prato feito',
  basePrice: 25,
  quantity: 1,
  note: 'Sem cebola',
  specs: [],
  adds: [{ name: 'Ovo', price: 2 }],
  selections: { 1: [2] },
}

const validCartResult: ValidateCartResult = {
  valid: true,
  lines: [{ lineId: 'line-1', available: true, unitPrice: '27.00', lineTotal: '27.00' }],
  subtotal: '27.00',
  deliveryFee: '7.00',
  total: '34.00',
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
}

function renderCheckout(onSent = vi.fn()) {
  render(<CheckoutScreen business={business} onBack={vi.fn()} onSent={onSent} />)
  fireEvent.change(screen.getByLabelText('Seu nome'), { target: { value: 'Cliente Teste' } })
  fireEvent.change(screen.getByLabelText('WhatsApp / telefone'), { target: { value: '(85) 98888-7777' } })
  fireEvent.change(screen.getByLabelText('Endereço de entrega'), { target: { value: 'Rua Um, 123' } })
  return onSent
}

async function submitOrder() {
  fireEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }))
}

describe('CheckoutScreen order persistence handoff', () => {
  beforeEach(() => {
    cleanup()
    localStorage.clear()
    validateCartMock.mockReset()
    validateCartMock.mockResolvedValue(validCartResult)
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'idem-checkout-1') })
    vi.spyOn(storefrontApi, 'createOrder').mockResolvedValue({ id: 'order-1', total: '34.00' })
    useCartStore.setState({ lines: [cartLine], deliveryType: 'ENTREGA' })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    cleanup()
  })

  it('transitions to sent only after order creation succeeds', async () => {
    const onSent = renderCheckout()

    await submitOrder()

    await waitFor(() => expect(storefrontApi.createOrder).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(onSent).toHaveBeenCalledTimes(1))
    expect(onSent.mock.calls[0][1]).toContain('wa.me/5585999990000')
  })

  it('keeps checkout open and shows retry when a network failure prevents confirmation', async () => {
    vi.mocked(storefrontApi.createOrder).mockRejectedValueOnce(new Error('Failed to fetch'))
    const onSent = renderCheckout()

    await submitOrder()

    expect(await screen.findByRole('alert')).toHaveTextContent('Pedido ainda não confirmado')
    expect(screen.getByRole('alert')).toHaveTextContent('Não conseguimos confirmar o pedido no sistema')
    expect(screen.getAllByRole('button', { name: 'Tentar novamente' }).length).toBeGreaterThan(0)
    expect(onSent).not.toHaveBeenCalled()
  })

  it('keeps checkout open and shows retry when HTTP 5xx prevents confirmation', async () => {
    vi.mocked(storefrontApi.createOrder).mockRejectedValueOnce(
      new StorefrontApiError(500, 'UNKNOWN_ERROR', 'Erro temporário.'),
    )
    const onSent = renderCheckout()

    await submitOrder()

    expect(await screen.findByRole('alert')).toHaveTextContent('Pedido ainda não confirmado')
    expect(onSent).not.toHaveBeenCalled()
  })

  it('retries with the same idempotencyKey and succeeds as the same logical attempt', async () => {
    vi.mocked(storefrontApi.createOrder)
      .mockRejectedValueOnce(new Error('timeout'))
      .mockResolvedValueOnce({ id: 'order-1', total: '34.00' })
    const onSent = renderCheckout()

    await submitOrder()
    await screen.findByRole('alert')
    fireEvent.click(screen.getAllByRole('button', { name: 'Tentar novamente' })[0])

    await waitFor(() => expect(onSent).toHaveBeenCalledTimes(1))
    const idempotencyKeys = vi.mocked(storefrontApi.createOrder).mock.calls.map(([payload]) => payload.idempotencyKey)
    expect(idempotencyKeys).toEqual(['idem-checkout-1', 'idem-checkout-1'])
    expect(new Set(idempotencyKeys).size).toBe(1)
  })

  it('rapid double-click starts only one in-flight request', async () => {
    const pendingCreate = deferred<{ id: string; total: string }>()
    vi.mocked(storefrontApi.createOrder).mockReturnValueOnce(pendingCreate.promise)
    const onSent = renderCheckout()
    const submit = screen.getByRole('button', { name: 'Enviar pedido' })

    fireEvent.click(submit)
    fireEvent.click(submit)

    await waitFor(() => expect(storefrontApi.createOrder).toHaveBeenCalledTimes(1))
    expect(onSent).not.toHaveBeenCalled()
    pendingCreate.resolve({ id: 'order-1', total: '34.00' })
    await waitFor(() => expect(onSent).toHaveBeenCalledTimes(1))
  })

  it('keeps PLAN_LIMIT_REACHED distinct with its specific blocked message', async () => {
    vi.mocked(storefrontApi.createOrder).mockRejectedValueOnce(
      new StorefrontApiError(402, 'PLAN_LIMIT_REACHED', 'Limite do plano atingido.'),
    )
    const onSent = renderCheckout()

    await submitOrder()

    expect(await screen.findByText('Loja indisponível para novos pedidos')).toBeInTheDocument()
    expect(screen.getByText(/atingiu o limite de pedidos deste mês/i)).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(onSent).not.toHaveBeenCalled()
  })

  it('still identifies unavailable cart items before creating an order', async () => {
    validateCartMock.mockResolvedValueOnce({
      ...validCartResult,
      valid: false,
      lines: [{ lineId: 'line-1', available: false, unitPrice: null, lineTotal: null }],
    })
    const onSent = renderCheckout()

    await submitOrder()

    expect(await screen.findByText('Prato feito não está mais disponível')).toBeInTheDocument()
    expect(storefrontApi.createOrder).not.toHaveBeenCalled()
    expect(onSent).not.toHaveBeenCalled()
  })

  it('does not offer WhatsApp handoff before persistence confirmation', async () => {
    const pendingCreate = deferred<{ id: string; total: string }>()
    vi.mocked(storefrontApi.createOrder).mockReturnValueOnce(pendingCreate.promise)
    const onSent = renderCheckout()

    await submitOrder()

    await waitFor(() => expect(storefrontApi.createOrder).toHaveBeenCalledTimes(1))
    expect(onSent).not.toHaveBeenCalled()
    expect(screen.queryByText('Abrir no WhatsApp')).not.toBeInTheDocument()
    pendingCreate.resolve({ id: 'order-1', total: '34.00' })
    await waitFor(() => expect(onSent).toHaveBeenCalledTimes(1))
  })
})
