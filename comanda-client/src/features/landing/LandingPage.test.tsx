import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { LandingPage } from './LandingPage'

describe('LandingPage', () => {
  afterEach(cleanup)
  beforeEach(() => {
    vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: true }))
    vi.stubGlobal('scrollTo', vi.fn())
  })

  it('renders accessible landmarks, real destinations, and MVP-safe copy', () => {
    render(<MemoryRouter><LandingPage /></MemoryRouter>)

    expect(screen.getByRole('navigation', { name: 'Navegação principal' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1)
    expect(screen.getAllByRole('link', { name: 'Entrar' })[0]).toHaveAttribute('href', '/login')
    expect(screen.getAllByRole('link', { name: /Criar meu cardápio|Começar grátis|Começar no Essencial/ })[0]).toHaveAttribute('href', '/onboarding')
    expect(screen.getByRole('link', { name: 'Política de privacidade' })).toHaveAttribute('href', '/privacidade')
    expect(screen.getByRole('link', { name: 'Termos de serviço' })).toHaveAttribute('href', '/termos')
    expect(screen.getByText(/atualizada automaticamente a cada poucos segundos/i)).toBeInTheDocument()
    expect(document.body).not.toHaveTextContent(/notificação automática|em tempo real|calculated|stripe|recuperação de senha/i)
  })

  it('opens and closes the mobile navigation from its keyboard-operable button', () => {
    render(<MemoryRouter><LandingPage /></MemoryRouter>)
    const menu = screen.getByRole('button', { name: 'Menu' })
    expect(menu).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(menu)
    expect(menu).toHaveAttribute('aria-expanded', 'true')
    expect(document.getElementById('lp-nav-links')).toHaveClass('is-open')
  })
})
