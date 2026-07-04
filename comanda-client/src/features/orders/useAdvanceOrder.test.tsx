import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

import type { OrderDetail, OrdersBoard } from '@/features/orders/api'
import { useAdvanceOrder } from '@/features/orders/queries'

/**
 * Task 9.7: "falha de avanço reverte o card" (PRD 4.2 / spec `order-operation`: "nenhum estado
 * ambíguo"). Exercises the real optimistic-update + rollback logic in `useAdvanceOrder` (not a
 * fake) — only the network boundary (`ordersApi.advance`) is mocked, using a controllable promise
 * so the test can assert the optimistic value mid-flight, before asserting it rolls back on
 * failure.
 */
vi.mock('@/features/orders/api', () => ({
  ordersApi: {
    advance: vi.fn(),
    board: vi.fn(),
    detail: vi.fn(),
    cancel: vi.fn(),
    status: vi.fn(),
    setOpen: vi.fn(),
  },
}))
vi.mock('@/features/auth/auth-context', () => ({
  useAuth: () => ({ accessToken: 'test-token' }),
}))

import { ordersApi } from '@/features/orders/api'

const ORDER_ID = 1

function detailFixture(status: OrderDetail['status']): OrderDetail {
  return {
    id: ORDER_ID,
    shortCode: '#1',
    status,
    customerName: 'Cliente Teste',
    customerPhone: '85999999999',
    deliveryType: 'RETIRADA',
    address: null,
    notes: null,
    cancellationReason: null,
    subtotal: '10.00',
    deliveryFee: '0.00',
    total: '10.00',
    createdAt: new Date().toISOString(),
    items: [],
  }
}

function boardFixture(status: OrderDetail['status']): OrdersBoard {
  return {
    summary: { count: 1, revenue: '10.00', openCount: 1 },
    filters: [],
    orders: [
      {
        id: ORDER_ID,
        shortCode: '#1',
        status,
        customerName: 'Cliente Teste',
        itemsSummary: '1x Item',
        deliveryType: 'RETIRADA',
        total: '10.00',
        createdAt: new Date().toISOString(),
        isNew: status === 'RECEBIDO',
      },
    ],
  }
}

function wrapperFor(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

describe('useAdvanceOrder', () => {
  it('optimistically advances the card, then reverts it with a visible error when the server call fails', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    queryClient.setQueryData(['orders', 'detail', ORDER_ID], detailFixture('RECEBIDO'))
    queryClient.setQueryData(['orders', 'board', 'TODOS'], boardFixture('RECEBIDO'))

    let rejectAdvance!: (error: Error) => void
    vi.mocked(ordersApi.advance).mockReturnValue(
      new Promise((_resolve, reject) => {
        rejectAdvance = reject
      }),
    )

    const { result } = renderHook(() => useAdvanceOrder(ORDER_ID), { wrapper: wrapperFor(queryClient) })

    act(() => {
      result.current.mutate('RECEBIDO')
    })

    // Optimistic update applied immediately, before the server responds.
    await waitFor(() => {
      expect(queryClient.getQueryData<OrderDetail>(['orders', 'detail', ORDER_ID])?.status).toBe('ACEITO')
    })
    expect(queryClient.getQueryData<OrdersBoard>(['orders', 'board', 'TODOS'])?.orders[0].status).toBe('ACEITO')

    act(() => {
      rejectAdvance(new Error('network down'))
    })

    // On failure: rollback to the pre-mutation status everywhere, and the error is visible.
    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(queryClient.getQueryData<OrderDetail>(['orders', 'detail', ORDER_ID])?.status).toBe('RECEBIDO')
    expect(queryClient.getQueryData<OrdersBoard>(['orders', 'board', 'TODOS'])?.orders[0].status).toBe('RECEBIDO')
  })
})
