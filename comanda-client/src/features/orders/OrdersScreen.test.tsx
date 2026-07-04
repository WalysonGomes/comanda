import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { OrdersScreen } from '@/features/orders/OrdersScreen'
import { useOrdersBoard, useSetTenantOpen, useTenantStatus } from '@/features/orders/queries'

/**
 * Task 9.7: "falha de polling mostra conectividade + última atualização". Mocks the query hooks
 * directly (rather than fetch/timers) to assert the connectivity indicator is derived from the
 * query's own `isError`/`dataUpdatedAt` — never `navigator.onLine` (design.md Decision 6) — and
 * never goes silent on failure (PRD 4.2).
 */
vi.mock('@/features/orders/queries', () => ({
  useOrdersBoard: vi.fn(),
  useTenantStatus: vi.fn(),
  useSetTenantOpen: vi.fn(),
}))

function renderScreen() {
  render(
    <MemoryRouter>
      <OrdersScreen />
    </MemoryRouter>,
  )
}

describe('OrdersScreen connectivity indicator', () => {
  beforeEach(() => {
    vi.mocked(useTenantStatus).mockReturnValue({
      data: { businessName: 'Loja Teste', open: true, closedToday: false, opensAt: null, closesAt: null },
    } as ReturnType<typeof useTenantStatus>)
    vi.mocked(useSetTenantOpen).mockReturnValue({ mutate: vi.fn() } as unknown as ReturnType<typeof useSetTenantOpen>)
  })

  it('shows the online state while polling succeeds', () => {
    vi.mocked(useOrdersBoard).mockReturnValue({
      data: { summary: { count: 0, revenue: '0', openCount: 0 }, filters: [], orders: [] },
      isPending: false,
      isError: false,
      dataUpdatedAt: Date.now(),
    } as unknown as ReturnType<typeof useOrdersBoard>)

    renderScreen()

    expect(screen.getByText('Atualização automática ativa')).toBeInTheDocument()
    expect(screen.getByText(/^Atualizado/)).toBeInTheDocument()
  })

  it('shows the offline state and the last successful update timestamp when polling fails', () => {
    const lastSuccessfulFetch = Date.now() - 60_000

    vi.mocked(useOrdersBoard).mockReturnValue({
      data: { summary: { count: 0, revenue: '0', openCount: 0 }, filters: [], orders: [] },
      isPending: false,
      isError: true,
      dataUpdatedAt: lastSuccessfulFetch,
    } as unknown as ReturnType<typeof useOrdersBoard>)

    renderScreen()

    expect(screen.getByText('Sem conexão — tentando reconectar')).toBeInTheDocument()
    expect(screen.getByText(/^última/)).toBeInTheDocument()
  })
})
