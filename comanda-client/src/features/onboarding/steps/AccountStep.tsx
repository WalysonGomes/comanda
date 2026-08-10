import { useMemo, type ReactNode } from 'react'
import { normalizeTenantLabel, tenantDomainSuffix, tenantMenuHost } from '@/lib/domain'

const INPUT_CLASS =
  'w-full rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3.5 text-[15px] text-ink outline-none focus:border-acc'

export type AccountFields = {
  name: string
  businessName: string
  subdomain: string
  whatsappNumber: string
  email: string
  password: string
}

/**
 * Step 1 (`.design/Comanda Painel.dc.html` `ob1`, lines 144-166): reuses the exact fields/copy of
 * `owner-auth`'s {@code SignupPage} (task 6.3) — just without that page's own chrome/submit, since
 * the wizard's chrome and "Avançar" button own those here.
 */
export function AccountStep({
  fields,
  onChange,
  error,
}: {
  fields: AccountFields
  onChange: (fields: AccountFields) => void
  error: string | null
}) {
  const subdomainPreview = useMemo(
    () => tenantMenuHost(fields.subdomain || 'seunegocio'),
    [fields.subdomain],
  )

  function set<K extends keyof AccountFields>(key: K, value: AccountFields[K]) {
    onChange({ ...fields, [key]: value })
  }

  return (
    <div className="animate-cmd-slide">
      <h1 className="font-display text-[26px] leading-[1.1] font-black text-ink">Crie sua conta</h1>
      <p className="mt-2 text-sm text-ink-2">Leva menos de um minuto.</p>

      <div className="mt-5 flex flex-col gap-3.5">
        <Field label="Seu nome" htmlFor="ob-name">
          <input
            id="ob-name"
            value={fields.name}
            onChange={(e) => set('name', e.target.value)}
            placeholder="Ana Oliveira"
            className={INPUT_CLASS}
          />
        </Field>

        <Field label="Nome do negócio" htmlFor="ob-business">
          <input
            id="ob-business"
            value={fields.businessName}
            onChange={(e) => set('businessName', e.target.value)}
            placeholder="Brasa Burger Caseiro"
            className={INPUT_CLASS}
          />
        </Field>

        <div>
          <label className="mb-2 block text-xs font-bold text-ink-2" htmlFor="ob-subdomain">
            Seu endereço (subdomínio)
          </label>
          <div className="flex items-center rounded-xl border-[1.5px] border-line bg-card px-3.5 focus-within:border-acc">
            <input
              id="ob-subdomain"
              value={fields.subdomain}
              onChange={(e) => set('subdomain', normalizeTenantLabel(e.target.value))}
              placeholder="brasaburger"
              className="w-[42%] flex-none border-none bg-transparent py-3.5 font-mono text-[15px] font-bold text-acc-d outline-none"
            />
            <span className="font-mono text-sm font-semibold text-ink-3">{tenantDomainSuffix}</span>
          </div>
          <p className="mt-1.5 text-xs text-ink-3">
            Seu cardápio ficará em <b className="font-bold text-ink-2">{subdomainPreview}</b>
          </p>
        </div>

        <Field label="WhatsApp do negócio" htmlFor="ob-whatsapp">
          <input
            id="ob-whatsapp"
            value={fields.whatsappNumber}
            onChange={(e) => set('whatsappNumber', e.target.value)}
            placeholder="(85) 99999-0000"
            className={INPUT_CLASS}
          />
        </Field>

        <div className="flex gap-3">
          <Field label="E-mail" htmlFor="ob-email" className="flex-1">
            <input
              id="ob-email"
              type="email"
              autoComplete="email"
              value={fields.email}
              onChange={(e) => set('email', e.target.value)}
              placeholder="ana@email.com"
              className={INPUT_CLASS}
            />
          </Field>
          <Field label="Senha" htmlFor="ob-password" className="flex-1">
            <input
              id="ob-password"
              type="password"
              autoComplete="new-password"
              value={fields.password}
              onChange={(e) => set('password', e.target.value)}
              placeholder="••••••"
              minLength={6}
              className={INPUT_CLASS}
            />
          </Field>
        </div>
      </div>

      {error && <p className="mt-4 text-[13px] font-semibold text-[#a05a4c]">{error}</p>}
    </div>
  )
}

function Field({
  label,
  htmlFor,
  className,
  children,
}: {
  label: string
  htmlFor: string
  className?: string
  children: ReactNode
}) {
  return (
    <div className={className}>
      <label className="mb-2 block text-xs font-bold text-ink-2" htmlFor={htmlFor}>
        {label}
      </label>
      {children}
    </div>
  )
}
