import { ArrowLeft, TriangleAlert } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { HoursStep } from '@/features/onboarding/steps/HoursStep'
import { isReservedTenantLabel, normalizeTenantLabel, tenantMenuHost } from '@/lib/domain'
import { BusinessSettingsApiError, type BusinessSettings } from './api'
import { useBusinessSettings, useSaveBusinessSettings, useUploadBusinessLogo } from './queries'

const input = 'w-full rounded-xl border border-line bg-card px-3 py-3 outline-none focus:border-acc focus:ring-2 focus:ring-acc/20'
export function BusinessSettingsScreen() {
  const query = useBusinessSettings()
  if (query.isLoading || !query.data) return <p className="p-6">Carregando dados do negócio…</p>
  return <BusinessSettingsForm initial={query.data} />
}

function BusinessSettingsForm({ initial }: { initial: BusinessSettings }) {
  const navigate = useNavigate(); const save = useSaveBusinessSettings(); const logo = useUploadBusinessLogo()
  const [form, setForm] = useState<BusinessSettings>(initial); const [message, setMessage] = useState<string | null>(null)
  function set<K extends keyof BusinessSettings>(key: K, value: BusinessSettings[K]) { setForm((f) => f ? { ...f, [key]: value } : f) }
  async function submit(e: FormEvent) { e.preventDefault(); if (!form || save.isPending) return; setMessage(null); if (isReservedTenantLabel(form.subdomain)) { setMessage('Este subdomínio é reservado.'); return } try { const data = await save.mutateAsync(form); setForm(data); setMessage('Dados salvos com sucesso.') } catch (err) { setMessage(err instanceof BusinessSettingsApiError ? err.message : 'Não foi possível salvar. Tente novamente.') } }
  async function upload(file?: File) { if (!file || logo.isPending) return; setMessage(null); try { const data = await logo.mutateAsync(file); setForm(data); setMessage('Logo atualizado com sucesso.') } catch (err) { setMessage(err instanceof Error ? err.message : 'Não foi possível enviar o logo.') } }
  return <div className="min-h-svh bg-cream pb-8"><header className="flex items-center gap-3 px-4 py-5"><button type="button" aria-label="Voltar" onClick={() => navigate('/painel/ajustes')}><ArrowLeft /></button><h1 className="font-display text-xl font-extrabold">Dados do negócio</h1></header>
    <form onSubmit={submit} className="mx-auto flex max-w-xl flex-col gap-4 px-4">
      <label className="font-bold">Nome do negócio<input className={input} value={form.businessName} onChange={e => set('businessName', e.target.value)} required /></label>
      {form.logoUrl && <img src={form.logoUrl} alt="Logo atual do negócio" className="size-24 rounded-xl object-cover" />}
      <label className="font-bold">Logo<input className={input} type="file" accept="image/jpeg,image/png,image/webp" onChange={e => upload(e.target.files?.[0])} disabled={logo.isPending} /></label>
      <label className="font-bold">WhatsApp<input className={input} value={form.whatsappNumber} onChange={e => set('whatsappNumber', e.target.value)} required aria-describedby={message ? 'settings-message' : undefined} /></label>
      <div className="grid grid-cols-2 gap-3"><label className="font-bold">Taxa de entrega<input className={input} type="number" min="0" step="0.01" value={form.deliveryFee} onChange={e => set('deliveryFee', e.target.value)} required /></label><label className="font-bold">Pedido mínimo<input className={input} type="number" min="0" step="0.01" value={form.minOrderValue} onChange={e => set('minOrderValue', e.target.value)} required /></label></div>
      <label className="font-bold">Subdomínio<input className={input} value={form.subdomain} onChange={e => set('subdomain', normalizeTenantLabel(e.target.value))} required aria-describedby="subdomain-warning settings-url" /></label>
      <p id="settings-url" className="break-all font-mono text-sm text-acc-d">https://{tenantMenuHost(form.subdomain)}</p>
      <div id="subdomain-warning" className="flex gap-2 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900"><TriangleAlert className="shrink-0" />Alterar o subdomínio quebra permanentemente QR Codes impressos e links já compartilhados.</div>
      <HoursStep rows={form.businessHours} onChange={v => set('businessHours', v)} />
      {message && <p id="settings-message" role="status" className="font-semibold text-[#a05a4c]">{message}</p>}
      <button type="submit" disabled={save.isPending || logo.isPending} className="rounded-xl bg-acc py-4 font-bold text-white disabled:opacity-60">{save.isPending ? 'Salvando…' : 'Salvar alterações'}</button>
    </form></div>
}
