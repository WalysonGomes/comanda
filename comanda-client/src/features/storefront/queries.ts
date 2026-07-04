import { useMutation, useQuery } from '@tanstack/react-query'

import { storefrontApi, type ValidateCartPayload } from '@/features/storefront/api'

const businessKey = ['storefront', 'negocio'] as const
const menuKey = ['storefront', 'cardapio'] as const

/** No `document.visibilityState` polling here (that's the owner panel's job) — the storefront
 * only needs a fresh read on load; availability/open-closed can't change meaningfully within a
 * single browsing session in a way that needs live updates before checkout re-validates anyway. */
export function useBusiness() {
  return useQuery({ queryKey: businessKey, queryFn: storefrontApi.getBusiness })
}

export function useMenu() {
  return useQuery({ queryKey: menuKey, queryFn: storefrontApi.getMenu })
}

export function useValidateCart() {
  return useMutation({
    mutationFn: (payload: ValidateCartPayload) => storefrontApi.validateCart(payload),
  })
}
