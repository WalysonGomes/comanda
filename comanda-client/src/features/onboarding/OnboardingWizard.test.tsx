import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { OnboardingWizard } from '@/features/onboarding/OnboardingWizard'
import { tenantMenuHost } from '@/lib/domain'

/**
 * Task 10.8: the wizard's happy path end to end (segmento → conta → horário → primeiro produto →
 * pronto), asserting the cardápio ends up "publicado" — the done step shows the link built from
 * the subdomain the dono typed, with copy/share actions present. Network boundaries are mocked
 * (signup, onboarding seed/business-hours, product update); the state machine itself is real.
 */
const signup = vi.fn()
vi.mock('@/features/auth/auth-context', () => ({
  useAuth: () => ({ signup }),
}))

const seed = vi.fn()
const saveBusinessHours = vi.fn()
vi.mock('@/features/onboarding/api', async () => {
  const actual = await vi.importActual<typeof import('@/features/onboarding/api')>('@/features/onboarding/api')
  return {
    ...actual,
    onboardingApi: { seed: (...args: unknown[]) => seed(...args), saveBusinessHours: (...args: unknown[]) => saveBusinessHours(...args) },
  }
})

const updateProductMutateAsync = vi.fn()
vi.mock('@/features/menu/queries', () => ({
  useProducts: () => ({
    data: [
      { id: 42, categoryId: 7, name: 'Marmita de Frango Grelhado', description: 'Frango grelhado.', price: '18.00', imageUrl: null, available: true, position: 0, availableDays: null },
    ],
  }),
  useUpdateProduct: () => ({ mutateAsync: updateProductMutateAsync }),
  useUploadImage: () => ({ mutate: vi.fn() }),
}))

vi.mock('@/components/QrCode', () => ({ QrCode: () => null }))
vi.mock('@/lib/share', () => ({ shareMenuLink: vi.fn(), copyText: vi.fn().mockResolvedValue(true) }))

function renderWizard() {
  render(
    <MemoryRouter>
      <OnboardingWizard />
    </MemoryRouter>,
  )
}

describe('OnboardingWizard', () => {
  beforeEach(() => {
    signup.mockReset().mockResolvedValue({ accessToken: 'token-123', user: { id: 1, name: 'Ana', email: 'ana@example.com', tenantId: 9 } })
    seed.mockReset().mockResolvedValue(undefined)
    saveBusinessHours.mockReset().mockResolvedValue(undefined)
    updateProductMutateAsync.mockReset().mockResolvedValue({ id: 42 })
  })

  it('walks segmento → conta → horário → primeiro produto → pronto, publishing the cardápio at the typed subdomain', async () => {
    renderWizard()

    // Step 0: segmento — communicates the Gratuito limit before any account exists.
    expect(screen.getByText(/30 pedidos\/mês/)).toBeInTheDocument()
    fireEvent.click(screen.getByText('Marmitaria'))
    fireEvent.click(screen.getByText('Continuar'))

    // Step 1: conta — reuses owner-auth's signup, then seeds the demo cardápio for the segment.
    fireEvent.change(screen.getByLabelText('Seu nome'), { target: { value: 'Ana Oliveira' } })
    fireEvent.change(screen.getByLabelText('Nome do negócio'), { target: { value: 'Marmitas da Ana' } })
    fireEvent.change(screen.getByLabelText('Seu endereço (subdomínio)'), { target: { value: 'marmitasdaana' } })
    fireEvent.change(screen.getByLabelText('WhatsApp do negócio'), { target: { value: '85999990000' } })
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@example.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'senha123' } })
    fireEvent.click(screen.getByText('Continuar'))

    await waitFor(() => expect(signup).toHaveBeenCalledWith(expect.objectContaining({ subdomain: 'marmitasdaana' })))
    await waitFor(() => expect(seed).toHaveBeenCalledWith('token-123', 'MARMITARIA'))

    // Step 2: horário — defaults are pre-filled; just confirm.
    await waitFor(() => expect(screen.getByText('Quando você atende?')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Continuar'))
    await waitFor(() => expect(saveBusinessHours).toHaveBeenCalledWith('token-123', expect.any(Array)))

    // Step 3: primeiro produto — pre-filled from the seed, editable and saved via menu-management.
    await waitFor(() => expect(screen.getByDisplayValue('Marmita de Frango Grelhado')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Continuar'))
    await waitFor(() =>
      expect(updateProductMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({ id: 42, body: expect.objectContaining({ name: 'Marmita de Frango Grelhado', categoryId: 7 }) }),
      ),
    )

    // Step 4: pronto — cardápio published at the subdomain typed in step 1.
    await waitFor(() => expect(screen.getByText(/está no ar/)).toBeInTheDocument())
    expect(screen.getByText(tenantMenuHost('marmitasdaana'))).toBeInTheDocument()
    expect(screen.getByText('Compartilhar no WhatsApp')).toBeInTheDocument()
  })
})
