import { ArrowLeft, RefreshCw, TriangleAlert } from 'lucide-react'
import { useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { HoursStep } from '@/features/onboarding/steps/HoursStep'
import { isReservedTenantLabel, normalizeTenantLabel, tenantMenuHost } from '@/lib/domain'
import { BusinessSettingsApiError, type BusinessSettings } from './api'
import { useBusinessSettings, useSaveBusinessSettings, useUploadBusinessLogo } from './queries'

const input = 'w-full rounded-xl border border-line bg-card px-3 py-3 outline-none focus:border-acc focus:ring-2 focus:ring-acc/20'
export function BusinessSettingsScreen() {
  const query = useBusinessSettings()
  if (query.isLoading || (query.isFetching && !query.data)) return <BusinessSettingsSkeleton />
  if (query.isError || !query.data) {
    return (
      <div className="flex min-h-svh flex-col items-center justify-center gap-4 bg-cream px-6 text-center">
        <TriangleAlert className="size-9 text-[#a05a4c]" aria-hidden="true" />
        <div role="alert">
          <h1 className="font-display text-xl font-extrabold text-ink">Não foi possível carregar os dados</h1>
          <p className="mt-2 text-sm text-ink-2">Confira sua conexão e tente novamente.</p>
        </div>
        <button
          type="button"
          onClick={() => query.refetch()}
          className="flex items-center gap-2 rounded-xl bg-acc px-5 py-3 font-bold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-acc"
        >
          <RefreshCw className="size-4" aria-hidden="true" />
          Tentar novamente
        </button>
      </div>
    )
  }
  return <BusinessSettingsForm initial={query.data} />
}

function BusinessSettingsSkeleton() {
  return (
    <div className="min-h-svh bg-cream px-4 py-5" role="status" aria-label="Obtendo dados do negócio">
      <div className="sk h-7 w-52 rounded-lg" />
      <div className="mx-auto mt-8 flex max-w-xl flex-col gap-4" aria-hidden="true">
        <div className="sk h-16 rounded-xl" />
        <div className="sk size-24 rounded-xl" />
        <div className="sk h-16 rounded-xl" />
        <div className="grid grid-cols-2 gap-3"><div className="sk h-16 rounded-xl" /><div className="sk h-16 rounded-xl" /></div>
        <div className="sk h-16 rounded-xl" />
        {Array.from({ length: 4 }, (_, index) => <div key={index} className="sk h-14 rounded-xl" />)}
      </div>
    </div>
  )
}

function BusinessSettingsForm({ initial }: { initial: BusinessSettings }) {
  const navigate = useNavigate(); const save = useSaveBusinessSettings(); const logo = useUploadBusinessLogo()
  const [form, setForm] = useState<BusinessSettings>(initial); const [message, setMessage] = useState<string | null>(null)
  const [saving, setSaving] = useState(false); const [uploadingLogo, setUploadingLogo] = useState(false)
  const saveLock = useRef(false); const logoLock = useRef(false)
  function set<K extends keyof BusinessSettings>(key: K, value: BusinessSettings[K]) { setForm((f) => f ? { ...f, [key]: value } : f) }
  async function submit(e: FormEvent) {
    e.preventDefault()
    if (saveLock.current || logoLock.current) return
    setMessage(null)
    if (isReservedTenantLabel(form.subdomain)) { setMessage('Este subdomínio é reservado.'); return }
    saveLock.current = true; setSaving(true)
    try { const data = await save.mutateAsync(form); setForm(data); setMessage('Dados salvos com sucesso.') }
    catch (err) { setMessage(err instanceof BusinessSettingsApiError ? err.message : 'Não foi possível salvar. Tente novamente.') }
    finally { saveLock.current = false; setSaving(false) }
  }
  async function upload(file?: File) {
    if (!file || logoLock.current || saveLock.current) return
    logoLock.current = true; setUploadingLogo(true); setMessage(null)
    try { const data = await logo.mutateAsync(file); setForm(data); setMessage('Logo atualizado com sucesso.') }
    catch (err) { setMessage(err instanceof BusinessSettingsApiError ? err.message : 'Não foi possível enviar o logo. Tente novamente.') }
    finally { logoLock.current = false; setUploadingLogo(false) }
  }
  return <div className="min-h-svh bg-cream pb-8"><header className="flex items-center gap-3 px-4 py-5"><button type="button" aria-label="Voltar" onClick={() => navigate('/painel/ajustes')}><ArrowLeft /></button><h1 className="font-display text-xl font-extrabold">Dados do negócio</h1></header>
    <form onSubmit={submit} className="mx-auto flex max-w-xl flex-col gap-4 px-4">
      <label className="font-bold">Nome do negócio<input className={input} value={form.businessName} onChange={e => set('businessName', e.target.value)} required /></label>
      {form.logoUrl && <img src={form.logoUrl} alt="Logo atual do negócio" className="size-24 rounded-xl object-cover" />}
      <label className="font-bold">Logo<input className={input} type="file" accept="image/jpeg,image/png,image/webp" onChange={e => upload(e.target.files?.[0])} disabled={uploadingLogo || saving} /></label>
      <label className="font-bold">WhatsApp<input className={input} value={form.whatsappNumber} onChange={e => set('whatsappNumber', e.target.value)} required aria-describedby={message ? 'settings-message' : undefined} /></label>
      <div className="grid grid-cols-2 gap-3"><label className="font-bold">Taxa de entrega<input className={input} type="number" min="0" step="0.01" value={form.deliveryFee} onChange={e => set('deliveryFee', e.target.value)} required /></label><label className="font-bold">Pedido mínimo<input className={input} type="number" min="0" step="0.01" value={form.minOrderValue} onChange={e => set('minOrderValue', e.target.value)} required /></label></div>
      <label className="font-bold">Subdomínio<input className={input} value={form.subdomain} onChange={e => set('subdomain', normalizeTenantLabel(e.target.value))} required aria-describedby="subdomain-warning settings-url" /></label>
      <p id="settings-url" className="break-all font-mono text-sm text-acc-d">https://{tenantMenuHost(form.subdomain)}</p>
      <div id="subdomain-warning" className="flex gap-2 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900"><TriangleAlert className="shrink-0" />Alterar o subdomínio quebra permanentemente QR Codes impressos e links já compartilhados.</div>
      <HoursStep rows={form.businessHours} onChange={v => set('businessHours', v)} />
      {message && <p id="settings-message" role="status" className="font-semibold text-[#a05a4c]">{message}</p>}
      <button type="submit" disabled={saving || uploadingLogo} className="rounded-xl bg-acc py-4 font-bold text-white disabled:opacity-60">{saving ? 'Salvando…' : 'Salvar alterações'}</button>
    </form></div>
}
