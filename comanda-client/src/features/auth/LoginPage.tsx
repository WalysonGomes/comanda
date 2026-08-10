import { useState, type SubmitEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { Lock, Mail, TriangleAlert } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/auth-context'

/**
 * Bloco LOGIN de `.design/Comanda Painel.dc.html` (linhas 69-107): avatar, boas-vindas, e-mail,
 * senha, erro genérico e atalho para cadastro. Recuperação de senha permanece fora do MVP.
 */
export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault()
    setError(false)
    setSubmitting(true)
    try {
      await login({ email, password })
      navigate('/painel', { replace: true })
    } catch {
      setError(true)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-svh flex-col bg-cream px-6">
      <form onSubmit={handleSubmit} className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-8">
        <div className="flex flex-col items-center text-center">
          <div className="flex size-[72px] items-center justify-center rounded-[22px] bg-acc font-display text-[27px] font-extrabold text-white shadow-[0_14px_30px_-10px_var(--acc)]">
            C
          </div>
          <h1 className="mt-5 font-display text-[29px] leading-[1.05] font-black tracking-tight text-ink">
            Bem-vindo de volta
          </h1>
          <p className="mt-2.5 max-w-[280px] text-[14.5px] leading-snug text-ink-2">
            Entre para acompanhar seus pedidos, com atualizações automáticas a cada poucos segundos.
          </p>
        </div>

        <div className="mt-7">
          <label className="mb-2 block text-xs font-bold text-ink-2" htmlFor="login-email">
            E-mail
          </label>
          <div className="flex items-center gap-2 rounded-[13px] border-[1.5px] border-line bg-card px-3.5 focus-within:border-acc">
            <Mail className="size-4 shrink-0 text-ink-3" strokeWidth={2} />
            <input
              id="login-email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="voce@email.com"
              className="w-full border-none bg-transparent py-3.5 text-[15px] text-ink outline-none placeholder:text-[#b3a691]"
              required
            />
          </div>
        </div>

        <div className="mt-3.5">
          <label className="mb-2 block text-xs font-bold text-ink-2" htmlFor="login-password">
            Senha
          </label>
          <div className="flex items-center gap-2 rounded-[13px] border-[1.5px] border-line bg-card px-3.5 focus-within:border-acc">
            <Lock className="size-4 shrink-0 text-ink-3" strokeWidth={2} />
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="••••••••"
              className="w-full border-none bg-transparent py-3.5 text-[15px] text-ink outline-none placeholder:text-[#b3a691]"
              required
            />
          </div>
        </div>

        {error && (
          <div className="mt-3.5 flex items-center gap-2 text-[13px] font-semibold text-[#a05a4c]">
            <TriangleAlert className="size-4 shrink-0" strokeWidth={2} />
            E-mail ou senha incorretos. Tente novamente.
          </div>
        )}

        <Button
          type="submit"
          disabled={submitting}
          className="mt-6 h-auto w-full rounded-[14px] py-4 text-[15.5px] font-extrabold shadow-[0_12px_24px_-10px_var(--acc)]"
        >
          {submitting ? 'Entrando...' : 'Entrar'}
        </Button>
      </form>

      <div className="border-t border-line py-5 text-center">
        <span className="text-sm text-ink-2">Ainda não tem cardápio? </span>
        <Link to="/onboarding" className="text-sm font-extrabold text-acc-d">
          Criar meu cardápio
        </Link>
      </div>
    </div>
  )
}
