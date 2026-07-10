import { useQuery } from '@tanstack/react-query'

import { useAuth } from '@/features/auth/auth-context'
import { plansApi } from '@/features/plans/api'

export function usePlanStatus() {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: ['plans', 'status'],
    queryFn: () => plansApi.status(accessToken as string),
    enabled: !!accessToken,
  })
}
