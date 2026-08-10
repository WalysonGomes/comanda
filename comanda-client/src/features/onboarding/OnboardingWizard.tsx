import { ArrowLeft } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/auth-context'
import { ApiError, type SignupPayload } from '@/lib/api'
import { MenuApiError } from '@/features/menu/api'
import { useProducts, useUpdateProduct } from '@/features/menu/queries'
import { OnboardingApiError, defaultBusinessHours, onboardingApi, type Segment } from '@/features/onboarding/api'
import { AccountStep, type AccountFields } from '@/features/onboarding/steps/AccountStep'
import { HoursStep } from '@/features/onboarding/steps/HoursStep'
import { DoneStep } from '@/features/onboarding/steps/DoneStep'
import { FirstProductStep, type FirstProductFields } from '@/features/onboarding/steps/FirstProductStep'
import { SegmentStep } from '@/features/onboarding/steps/SegmentStep'
import { isReservedTenantLabel, tenantMenuHost } from '@/lib/domain'

const STEP_COUNT = 5

const EMPTY_ACCOUNT: AccountFields = { name: '', businessName: '', subdomain: '', whatsappNumber: '', email: '', password: '' }
const EMPTY_PRODUCT: FirstProductFields = { name: '', description: '', price: '', imageUrl: null }

/**
 * Onboarding wizard (`.design/Comanda Painel.dc.html` `isOnb` block, tasks 6.1-6.6): segmento →
 * conta → horário → primeiro produto → pronto. Step 1 creates the tenant (owner-auth's signup,
 * reused as-is); steps 2-3 write through the onboarding/business-hours and menu-management APIs
 * using the token signup just returned — not context's `accessToken`, which only catches up after
 * a re-render, to keep each step's own submit deterministic instead of racing that render.
 */
export function OnboardingWizard() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  const [step, setStep] = useState(0)
  const [segment, setSegment] = useState<Segment | null>(null)
  const [account, setAccount] = useState<AccountFields>(EMPTY_ACCOUNT)
  const [accountError, setAccountError] = useState<string | null>(null)
  const [hours, setHours] = useState(defaultBusinessHours())
  const [productEdits, setProductEdits] = useState<FirstProductFields | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [stepError, setStepError] = useState<string | null>(null)

  const productsQuery = useProducts()
  const updateProduct = useUpdateProduct()

  // The first item of the just-materialized demo seed (task 6.5) — derived straight from the
  // query result rather than copied into state via an effect, so there's nothing to keep in sync.
  const firstSeededProduct = useMemo(() => {
    if (!productsQuery.data || productsQuery.data.length === 0) return null
    return [...productsQuery.data].sort((a, b) => a.position - b.position)[0]
  }, [productsQuery.data])
  const firstProductId = firstSeededProduct?.id ?? null
  const product: FirstProductFields =
    productEdits ??
    (firstSeededProduct
      ? {
          name: firstSeededProduct.name,
          description: firstSeededProduct.description ?? '',
          price: firstSeededProduct.price,
          imageUrl: firstSeededProduct.imageUrl,
        }
      : EMPTY_PRODUCT)

  async function handleNext() {
    setStepError(null)

    if (step === 0) {
      if (!segment) return
      setStep(1)
      return
    }

    if (step === 1) {
      if (!account.name || !account.businessName || !account.subdomain || !account.whatsappNumber || !account.email || !account.password) {
        setAccountError('Preencha todos os campos para continuar.')
        return
      }
      if (isReservedTenantLabel(account.subdomain)) { setAccountError('Este subdomínio é reservado.'); return }
      setAccountError(null)
      setSubmitting(true)
      try {
        const payload: SignupPayload = { ...account }
        const response = await signup(payload)
        setToken(response.accessToken)
        await onboardingApi.seed(response.accessToken, segment as Segment)
        setStep(2)
      } catch (err) {
        setAccountError(err instanceof ApiError ? err.message : 'Não foi possível criar sua conta. Tente novamente.')
      } finally {
        setSubmitting(false)
      }
      return
    }

    if (step === 2) {
      if (!token) return
      setSubmitting(true)
      try {
        await onboardingApi.saveBusinessHours(token, hours)
        setStep(3)
      } catch (err) {
        setStepError(err instanceof OnboardingApiError ? err.message : 'Não foi possível salvar o horário. Tente novamente.')
      } finally {
        setSubmitting(false)
      }
      return
    }

    if (step === 3) {
      if (!firstProductId) {
        setStep(4)
        return
      }
      const priceNumber = Number(product.price.replace(',', '.'))
      if (product.name.trim().length === 0 || Number.isNaN(priceNumber) || priceNumber < 0) {
        setStepError('Informe nome e preço válidos.')
        return
      }
      setSubmitting(true)
      try {
        await updateProduct.mutateAsync({
          id: firstProductId,
          body: {
            name: product.name.trim(),
            description: product.description.trim() || null,
            price: product.price.replace(',', '.'),
            availableDays: null,
            categoryId: productsQuery.data?.find((p) => p.id === firstProductId)?.categoryId as number,
            imageUrl: product.imageUrl,
          },
        })
        setStep(4)
      } catch (err) {
        setStepError(err instanceof MenuApiError ? err.message : 'Não foi possível salvar o produto. Tente novamente.')
      } finally {
        setSubmitting(false)
      }
      return
    }

    navigate('/painel/pedidos', { replace: true })
  }

  function handleBack() {
    if (step === 0) {
      navigate('/')
      return
    }
    setStep((s) => Math.max(0, s - 1))
  }

  const nextLabel =
    step === 1 || step === 2 || step === 3
      ? submitting
        ? 'Salvando…'
        : 'Continuar'
      : step === 4
        ? 'Ir para o painel'
        : 'Continuar'
  const nextDisabled = (step === 0 && !segment) || submitting

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <div className="flex flex-none items-center gap-3 px-4 pt-3 pb-3.5">
        <button
          type="button"
          onClick={handleBack}
          aria-label="Voltar"
          className="flex size-9 items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
        >
          <ArrowLeft className="size-5" strokeWidth={2} />
        </button>
        <div className="flex flex-1 items-center gap-1.5">
          {Array.from({ length: STEP_COUNT }, (_, i) => (
            <div
              key={i}
              className="h-[5px] flex-1 rounded-[3px]"
              style={{ background: i <= step ? 'var(--acc)' : '#e2d8c6' }}
            />
          ))}
        </div>
        <span className="font-mono text-xs font-bold text-ink-3">
          {step + 1}/{STEP_COUNT}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto px-5 pt-1.5 pb-4">
        {step === 0 && <SegmentStep value={segment} onChange={setSegment} />}
        {step === 1 && <AccountStep fields={account} onChange={setAccount} error={accountError} />}
        {step === 2 && <HoursStep rows={hours} onChange={setHours} />}
        {step === 3 && <FirstProductStep fields={product} onChange={setProductEdits} />}
        {step === 4 && <DoneStep menuUrl={tenantMenuHost(account.subdomain)} businessName={account.businessName} />}
        {stepError && step !== 1 && <p className="mt-4 text-[13px] font-semibold text-[#a05a4c]">{stepError}</p>}
      </div>

      <div className="flex-none px-5 pt-3 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <button
          type="button"
          onClick={handleNext}
          disabled={nextDisabled}
          className="w-full rounded-[14px] bg-acc py-4 text-[15.5px] font-extrabold text-white shadow-[0_12px_24px_-10px_var(--acc)] disabled:opacity-60"
        >
          {nextLabel}
        </button>
      </div>
    </div>
  )
}
