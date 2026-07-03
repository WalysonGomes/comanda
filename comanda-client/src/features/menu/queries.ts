import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuth } from '@/features/auth/auth-context'
import {
  menuApi,
  type AdditionalGroupPayload,
  type AdditionalItemPayload,
  type CategoryPayload,
  type ProductPayload,
} from '@/features/menu/api'

const categoriesKey = ['menu', 'categories'] as const
const productsKey = ['menu', 'products'] as const
const groupsKey = (productId: number) => ['menu', 'products', productId, 'additional-groups'] as const

export function useCategories() {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: categoriesKey,
    queryFn: () => menuApi.listCategories(accessToken as string),
    enabled: !!accessToken,
  })
}

export function useProducts() {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: productsKey,
    queryFn: () => menuApi.listProducts(accessToken as string),
    enabled: !!accessToken,
  })
}

export function useAdditionalGroups(productId: number | undefined) {
  const { accessToken } = useAuth()
  return useQuery({
    queryKey: groupsKey(productId as number),
    queryFn: () => menuApi.listAdditionalGroups(accessToken as string, productId as number),
    enabled: !!accessToken && !!productId,
  })
}

export function useCreateCategory() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CategoryPayload) => menuApi.createCategory(accessToken as string, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: categoriesKey }),
  })
}

export function useUpdateCategory() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: CategoryPayload }) =>
      menuApi.updateCategory(accessToken as string, id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: categoriesKey }),
  })
}

export function useDeleteCategory() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => menuApi.deleteCategory(accessToken as string, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: categoriesKey }),
  })
}

export function useReorderCategories() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (orderedIds: number[]) => menuApi.reorderCategories(accessToken as string, orderedIds),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: categoriesKey }),
  })
}

export function useCreateProduct() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ProductPayload) => menuApi.createProduct(accessToken as string, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: productsKey }),
  })
}

export function useUpdateProduct() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ProductPayload }) =>
      menuApi.updateProduct(accessToken as string, id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: productsKey }),
  })
}

export function useSetProductAvailability() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, available }: { id: number; available: boolean }) =>
      menuApi.setProductAvailability(accessToken as string, id, available),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: productsKey }),
  })
}

export function useDeleteProduct() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => menuApi.deleteProduct(accessToken as string, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: productsKey }),
  })
}

export function useCreateAdditionalGroup(productId: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: AdditionalGroupPayload) => menuApi.createAdditionalGroup(accessToken as string, productId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsKey(productId) }),
  })
}

export function useUpdateAdditionalGroup(productId: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ groupId, body }: { groupId: number; body: AdditionalGroupPayload }) =>
      menuApi.updateAdditionalGroup(accessToken as string, groupId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsKey(productId) }),
  })
}

export function useDeleteAdditionalGroup(productId: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (groupId: number) => menuApi.deleteAdditionalGroup(accessToken as string, groupId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsKey(productId) }),
  })
}

export function useCreateAdditionalItem(productId: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ groupId, body }: { groupId: number; body: AdditionalItemPayload }) =>
      menuApi.createAdditionalItem(accessToken as string, groupId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsKey(productId) }),
  })
}

export function useDeleteAdditionalItem(productId: number) {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (itemId: number) => menuApi.deleteAdditionalItem(accessToken as string, itemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsKey(productId) }),
  })
}

export function useUploadImage() {
  const { accessToken } = useAuth()
  return useMutation({
    mutationFn: (file: File) => menuApi.uploadImage(accessToken as string, file),
  })
}
