import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BusinessSettingsScreen } from './BusinessSettingsScreen'

const mutateAsync = vi.fn()
const logoMutateAsync = vi.fn()
const settings = { businessName: 'Loja Ana', logoUrl: null, whatsappNumber: '85999990000', deliveryFee: '5.00', minOrderValue: '20.00', subdomain: 'loja-ana', businessHours: Array.from({ length: 7 }, (_, dayOfWeek) => ({ dayOfWeek, opensAt: '08:00', closesAt: '18:00', closed: false })) }
vi.mock('./queries', () => ({
  useBusinessSettings: () => ({ isLoading: false, data: settings }),
  useSaveBusinessSettings: () => ({ mutateAsync, isPending: false }),
  useUploadBusinessLogo: () => ({ mutateAsync: logoMutateAsync, isPending: false }),
}))

describe('BusinessSettingsScreen', () => {
  afterEach(cleanup)
  beforeEach(() => { mutateAsync.mockReset(); logoMutateAsync.mockReset() })
  it('loads every field, hours, configured URL and permanent warning', () => {
    render(<MemoryRouter><BusinessSettingsScreen /></MemoryRouter>)
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Loja Ana')
    expect(screen.getByLabelText('WhatsApp')).toHaveValue('85999990000')
    expect(screen.getByText(/quebra permanentemente QR Codes/)).toBeVisible()
    expect(screen.getAllByLabelText('Marcar como fechado')).toHaveLength(7)
    expect(screen.getByText(/https:\/\/loja-ana\./)).toBeVisible()
  })
  it('keeps edits and shows reserved-label feedback without submitting', async () => {
    render(<MemoryRouter><BusinessSettingsScreen /></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Nome do negócio'), { target: { value: 'Novo Nome' } })
    fireEvent.change(screen.getByLabelText('Subdomínio'), { target: { value: ' API ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))
    expect(await screen.findByText('Este subdomínio é reservado.')).toBeVisible()
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Novo Nome')
    expect(mutateAsync).not.toHaveBeenCalled()
  })
  it('saves edits and reports recoverable API failure', async () => {
    mutateAsync.mockRejectedValueOnce(new Error('offline'))
    render(<MemoryRouter><BusinessSettingsScreen /></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Nome do negócio'), { target: { value: 'Novo Nome' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))
    await waitFor(() => expect(mutateAsync).toHaveBeenCalled())
    expect(await screen.findByText('Não foi possível salvar. Tente novamente.')).toBeVisible()
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Novo Nome')
  })
})
