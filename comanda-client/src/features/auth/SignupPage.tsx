import { useMemo, useState, type ReactNode, type SubmitEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { Info } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/auth-context'
import { ApiError, type SignupPayload } from '@/lib/api'

const SUBDOMAIN_SUFFIX = '.comanda.app'
const INPUT_CLASS =
  'w-full rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3.5 text-[15px] text-ink outline-none focus:border-acc'

function normalizeSubdomainInput(value: string) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, '-')
    .replace(/-{2,}/g, '-')
}

/**
 * Reaproveita só a criação de conta do bloco "Crie sua conta" / `ob1` de
 * `.design/Comanda Painel.dc.html` (linhas 144-166) — sem o chrome do wizard de onboarding
 * (segmento, horário, primeiro produto, "pronto"), que é escopo de `plans-and-onboarding`.
 */
export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [businessName, setBusinessName] = useState('')
  const [subdomain, setSubdomain] = useState('')
  const [whatsappNumber, setWhatsappNumber] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const subdomainPreview = useMemo(
    () => `${subdomain || 'seunegocio'}${SUBDOMAIN_SUFFIX}`,
    [subdomain],
  )

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    const payload: SignupPayload = { name, businessName, subdomain, whatsappNumber, email, password }
    try {
      await signup(payload)
      navigate('/painel', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível criar sua conta. Tente novamente.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-svh flex-col bg-cream px-6 py-8">
      <form onSubmit={handleSubmit} className="mx-auto flex w-full max-w-sm flex-1 flex-col gap-5">
        <div>
          <h1 className="font-display text-[26px] leading-tight font-black text-ink">Crie sua conta</h1>
          <p className="mt-2 text-sm text-ink-2">Leva menos de um minuto.</p>
        </div>

        <Field label="Seu nome" htmlFor="signup-name">
          <input
            id="signup-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Ana Oliveira"
            className={INPUT_CLASS}
            required
          />
        </Field>

        <Field label="Nome do negócio" htmlFor="signup-business">
          <input
            id="signup-business"
            value={businessName}
            onChange={(event) => setBusinessName(event.target.value)}
            placeholder="Brasa Burger Caseiro"
            className={INPUT_CLASS}
            required
          />
        </Field>

        <div>
          <label className="mb-2 block text-xs font-bold text-ink-2" htmlFor="signup-subdomain">
            Seu endereço (subdomínio)
          </label>
          <div className="flex items-center rounded-xl border-[1.5px] border-line bg-card px-3.5 focus-within:border-acc">
            <input
              id="signup-subdomain"
              value={subdomain}
              onChange={(event) => setSubdomain(normalizeSubdomainInput(event.target.value))}
              placeholder="brasaburger"
              className="w-[42%] flex-none border-none bg-transparent py-3.5 font-mono text-[15px] font-bold text-acc-d outline-none"
              required
            />
            <span className="font-mono text-sm font-semibold text-ink-3">{SUBDOMAIN_SUFFIX}</span>
          </div>
          <p className="mt-1.5 text-xs text-ink-3">
            Seu cardápio ficará em <b className="font-bold text-ink-2">{subdomainPreview}</b>
          </p>
        </div>

        <Field label="WhatsApp do negócio" htmlFor="signup-whatsapp">
          <input
            id="signup-whatsapp"
            value={whatsappNumber}
            onChange={(event) => setWhatsappNumber(event.target.value)}
            placeholder="(85) 99999-0000"
            className={INPUT_CLASS}
            required
          />
        </Field>

        <div className="flex gap-3">
          <Field label="E-mail" htmlFor="signup-email" className="flex-1">
            <input
              id="signup-email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="ana@email.com"
              className={INPUT_CLASS}
              required
            />
          </Field>
          <Field label="Senha" htmlFor="signup-password" className="flex-1">
            <input
              id="signup-password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="••••••"
              className={INPUT_CLASS}
              minLength={6}
              required
            />
          </Field>
        </div>

        <div className="flex gap-2 rounded-[13px] border border-[#cfe3c8] bg-[#eef4ec] p-3.5">
          <Info className="mt-0.5 size-4 shrink-0 text-[#2f7a44]" strokeWidth={2} />
          <p className="text-[12.5px] leading-snug text-[#356b3f]">
            No plano <b className="font-bold">Gratuito</b> você recebe até <b className="font-bold">30 pedidos/mês</b>{' '}
            — suficiente para começar sem pagar nada.
          </p>
        </div>

        {error && <p className="text-[13px] font-semibold text-[#a05a4c]">{error}</p>}

        <Button
          type="submit"
          disabled={submitting}
          className="h-auto w-full rounded-[14px] py-4 text-[15.5px] font-extrabold shadow-[0_12px_24px_-10px_var(--acc)]"
        >
          {submitting ? 'Criando conta...' : 'Criar minha conta'}
        </Button>

        <div className="text-center text-[13.5px] text-ink-2">
          Já tem conta?{' '}
          <Link to="/login" className="font-extrabold text-acc-d">
            Entrar
          </Link>
        </div>
      </form>
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
