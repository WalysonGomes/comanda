import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/features/auth/auth-context'
import { businessSettingsApi, type BusinessSettings } from './api'

export const businessSettingsKey = ['business-settings'] as const
export function useBusinessSettings() { const { accessToken } = useAuth(); return useQuery({ queryKey: businessSettingsKey, queryFn: () => businessSettingsApi.get(accessToken!), enabled: !!accessToken }) }
export function useSaveBusinessSettings() { const { accessToken } = useAuth(); const qc = useQueryClient(); return useMutation({ mutationFn: (v: BusinessSettings) => businessSettingsApi.update(accessToken!, v), onSuccess: (data) => { qc.setQueryData(businessSettingsKey, data); qc.invalidateQueries({ queryKey: ['plans', 'status'] }); qc.invalidateQueries({ queryKey: ['storefront', 'negocio'] }) } }) }
export function useUploadBusinessLogo() { const { accessToken } = useAuth(); const qc = useQueryClient(); return useMutation({ mutationFn: (f: File) => businessSettingsApi.logo(accessToken!, f), onSuccess: (data) => { qc.setQueryData(businessSettingsKey, data); qc.invalidateQueries({ queryKey: ['plans', 'status'] }); qc.invalidateQueries({ queryKey: ['storefront', 'negocio'] }) } }) }
