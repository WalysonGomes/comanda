import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuth } from '@/features/auth/auth-context'
import { ordersApi, type OrderDetail, type OrderStatus, type OrdersBoard } from '@/features/orders/api'

const boardKey = (status: OrderStatus | 'TODOS') => ['orders', 'board', status] as const
const detailKey = (id: number) => ['orders', 'detail', id] as const
const statusKey = ['orders', 'tenant-status'] as const

/**
 * Polling (task 5.1/PRD 4.2): `refetchInterval` fires every 15s; `refetchIntervalInBackground:
 * false` makes TanStack Query itself respect `document.visibilityState` — no manual listener
 * needed. The query's own `isError`/`dataUpdatedAt` drive the connectivity indicator (task 5.2).
 */
export function useOrdersBoard(status: OrderStatus | 'TODOS') {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: boardKey(status),
    queryFn: () => ordersApi.board(accessToken as string, status === 'TODOS' ? undefined : status),
    enabled: !!accessToken,
    refetchInterval: 15000,
    refetchIntervalInBackground: false,
  })
}

export function useOrderDetail(id: number | null) {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: detailKey(id as number),
    queryFn: () => ordersApi.detail(accessToken as string, id as number),
    enabled: !!accessToken && id !== null,
  })
}

export function useAdvanceOrder(id: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (fromStatus: OrderStatus) => ordersApi.advance(accessToken as string, id, fromStatus),
    onMutate: async (fromStatus) => {
      await queryClient.cancelQueries({ queryKey: detailKey(id) })
      const previousDetail = queryClient.getQueryData<OrderDetail>(detailKey(id))
      const nextStatus = OPTIMISTIC_NEXT[fromStatus]
      if (previousDetail && nextStatus) {
        queryClient.setQueryData<OrderDetail>(detailKey(id), { ...previousDetail, status: nextStatus })
      }
      const previousBoards = queryClient.getQueriesData<OrdersBoard>({ queryKey: ['orders', 'board'] })
      previousBoards.forEach(([key, board]) => {
        if (!board || !nextStatus) return
        queryClient.setQueryData<OrdersBoard>(key, {
          ...board,
          orders: board.orders.map((o) => (o.id === id ? { ...o, status: nextStatus, isNew: false } : o)),
        })
      })
      return { previousDetail, previousBoards }
    },
    onError: (_err, _fromStatus, context) => {
      if (context?.previousDetail) {
        queryClient.setQueryData(detailKey(id), context.previousDetail)
      }
      context?.previousBoards?.forEach(([key, board]) => {
        queryClient.setQueryData(key, board)
      })
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: detailKey(id) })
      queryClient.invalidateQueries({ queryKey: ['orders', 'board'] })
    },
  })
}

const OPTIMISTIC_NEXT: Partial<Record<OrderStatus, OrderStatus>> = {
  RECEBIDO: 'ACEITO',
  ACEITO: 'EM_PREPARO',
  EM_PREPARO: 'PRONTO',
  PRONTO: 'ENTREGUE',
}

export function useCancelOrder(id: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reason: string) => ordersApi.cancel(accessToken as string, id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: detailKey(id) })
      queryClient.invalidateQueries({ queryKey: ['orders', 'board'] })
    },
  })
}

export function useTenantStatus() {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: statusKey,
    queryFn: () => ordersApi.status(accessToken as string),
    enabled: !!accessToken,
  })
}

export function useSetTenantOpen() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (open: boolean) => ordersApi.setOpen(accessToken as string, open),
    onSuccess: (data) => queryClient.setQueryData(statusKey, data),
  })
}
