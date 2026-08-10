import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BusinessSettingsScreen } from './BusinessSettingsScreen'

const mutateAsync = vi.fn()
const logoMutateAsync = vi.fn()
const refetch = vi.fn()
const settings = {
  businessName: 'Loja Ana', logoUrl: null, whatsappNumber: '85999990000', deliveryFee: '5.00',
  minOrderValue: '20.00', subdomain: 'loja-ana',
  businessHours: Array.from({ length: 7 }, (_, dayOfWeek) => ({ dayOfWeek, opensAt: '08:00', closesAt: '18:00', closed: false })),
}
let queryState: { isLoading: boolean; isFetching: boolean; isError: boolean; data?: typeof settings; refetch: typeof refetch }

vi.mock('./queries', () => ({
  useBusinessSettings: () => queryState,
  useSaveBusinessSettings: () => ({ mutateAsync, isPending: false }),
  useUploadBusinessLogo: () => ({ mutateAsync: logoMutateAsync, isPending: false }),
}))

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

function renderScreen() {
  return render(<MemoryRouter><BusinessSettingsScreen /></MemoryRouter>)
}

describe('BusinessSettingsScreen', () => {
  afterEach(cleanup)
  beforeEach(() => {
    mutateAsync.mockReset(); logoMutateAsync.mockReset(); refetch.mockReset()
    queryState = { isLoading: false, isFetching: false, isError: false, data: settings, refetch }
  })

  it('renders skeletons during initial loading without loading copy', () => {
    queryState = { isLoading: true, isFetching: true, isError: false, refetch }
    renderScreen()
    expect(screen.getByRole('status', { name: 'Obtendo dados do negócio' })).toBeVisible()
    expect(document.querySelectorAll('.sk').length).toBeGreaterThan(1)
    expect(screen.queryByText(/Carregando/i)).not.toBeInTheDocument()
  })

  it('shows an accessible initial error and retry uses refetch', () => {
    queryState = { isLoading: false, isFetching: false, isError: true, refetch }
    renderScreen()
    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível carregar os dados')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(refetch).toHaveBeenCalledTimes(1)
  })

  it('returns to skeletons while retrying and renders the form after success', () => {
    queryState = { isLoading: false, isFetching: false, isError: true, refetch }
    const view = renderScreen()
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(refetch).toHaveBeenCalledTimes(1)
    queryState = { isLoading: false, isFetching: true, isError: true, refetch }
    view.rerender(<MemoryRouter><BusinessSettingsScreen /></MemoryRouter>)
    expect(screen.getByRole('status')).toBeVisible()
    queryState = { isLoading: false, isFetching: false, isError: false, data: settings, refetch }
    view.rerender(<MemoryRouter><BusinessSettingsScreen /></MemoryRouter>)
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Loja Ana')
  })

  it('loads every field, hours, configured URL and permanent warning', () => {
    renderScreen()
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Loja Ana')
    expect(screen.getByText(/quebra permanentemente QR Codes/)).toBeVisible()
    expect(screen.getAllByLabelText('Marcar como fechado')).toHaveLength(7)
    expect(screen.getByText(/https:\/\/loja-ana\./)).toBeVisible()
  })

  it('shows confirmation after a successful settings update', async () => {
    mutateAsync.mockResolvedValueOnce({ ...settings, businessName: 'Novo Nome' })
    renderScreen()
    fireEvent.change(screen.getByLabelText('Nome do negócio'), { target: { value: 'Novo Nome' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))
    expect(await screen.findByText('Dados salvos com sucesso.')).toBeVisible()
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Novo Nome')
  })

  it('preserves edits after failure, releases the lock, and permits retry', async () => {
    mutateAsync.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ ...settings, businessName: 'Novo Nome' })
    renderScreen()
    fireEvent.change(screen.getByLabelText('Nome do negócio'), { target: { value: 'Novo Nome' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))
    expect(await screen.findByText('Não foi possível salvar. Tente novamente.')).toBeVisible()
    expect(screen.getByLabelText('Nome do negócio')).toHaveValue('Novo Nome')
    const button = screen.getByRole('button', { name: 'Salvar alterações' })
    expect(button).toBeEnabled()
    fireEvent.click(button)
    expect(await screen.findByText('Dados salvos com sucesso.')).toBeVisible()
    expect(mutateAsync).toHaveBeenCalledTimes(2)
  })

  it('updates the displayed logo and confirms a successful upload', async () => {
    logoMutateAsync.mockResolvedValueOnce({ ...settings, logoUrl: '/media/1/logo.png' })
    renderScreen()
    fireEvent.change(screen.getByLabelText('Logo'), { target: { files: [new File(['png'], 'logo.png', { type: 'image/png' })] } })
    expect(await screen.findByText('Logo atualizado com sucesso.')).toBeVisible()
    expect(screen.getByRole('img', { name: 'Logo atual do negócio' })).toHaveAttribute('src', '/media/1/logo.png')
  })

  it('shows a recoverable logo error and releases the upload lock', async () => {
    logoMutateAsync.mockRejectedValueOnce(new Error('invalid')).mockResolvedValueOnce({ ...settings, logoUrl: '/media/1/retry.png' })
    renderScreen()
    const input = screen.getByLabelText('Logo')
    fireEvent.change(input, { target: { files: [new File(['bad'], 'bad.png')] } })
    expect(await screen.findByText('Não foi possível enviar o logo. Tente novamente.')).toBeVisible()
    expect(input).toBeEnabled()
    fireEvent.change(input, { target: { files: [new File(['png'], 'retry.png')] } })
    expect(await screen.findByText('Logo atualizado com sucesso.')).toBeVisible()
    expect(logoMutateAsync).toHaveBeenCalledTimes(2)
  })

  it('allows only one rapid settings submission and releases the lock after success', async () => {
    const pending = deferred<typeof settings>(); mutateAsync.mockReturnValueOnce(pending.promise)
    renderScreen()
    const form = screen.getByRole('button', { name: 'Salvar alterações' }).closest('form')!
    fireEvent.submit(form); fireEvent.submit(form)
    expect(mutateAsync).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Salvando…' })).toBeDisabled()
    pending.resolve(settings)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Salvar alterações' })).toBeEnabled())
  })

  it('allows only one rapid logo submission and releases the lock after success', async () => {
    const pending = deferred<typeof settings>(); logoMutateAsync.mockReturnValueOnce(pending.promise)
    renderScreen()
    const input = screen.getByLabelText('Logo')
    const file = new File(['png'], 'logo.png', { type: 'image/png' })
    fireEvent.change(input, { target: { files: [file] } }); fireEvent.change(input, { target: { files: [file] } })
    expect(logoMutateAsync).toHaveBeenCalledTimes(1)
    expect(input).toBeDisabled()
    pending.resolve(settings)
    await waitFor(() => expect(screen.getByLabelText('Logo')).toBeEnabled())
  })
})
