import { useCallback, useEffect, useRef } from 'react'
import { Link } from 'react-router'

import './landing.css'

/* ─── Tiny check SVG (reused in feature & pricing lists) ─── */
const Check13 = ({ className }: { className?: string }) => (
  <svg width="13" height="13" viewBox="0 0 12 12" fill="none" className={className}>
    <path d="M2 6.2 4.8 9 10 3.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

/* ─── Logo (img tag referencing the public SVG) ─── */
const LOGO_SIZES: Record<string, number> = { xs: 20, sm: 28, md: 28, lg: 64, xl: 104 }
function ComandaLogo({ size = 'md' }: { size?: string }) {
  const px = LOGO_SIZES[size] ?? 28
  return <img src="/comanda-logo.svg" alt="Comanda" width={px} height={px} style={{ display: 'block' }} />
}

/**
 * Marketing landing page — 1:1 translation of `.design/Comanda Landing.dc.html`.
 * Rendered on the main domain (no tenant subdomain).
 */
export function LandingPage() {
  const rootRef = useRef<HTMLDivElement>(null)

  /* ─── Smooth scroll ─── */
  const scrollTo = useCallback((id: string) => (e: React.MouseEvent) => {
    e.preventDefault()
    const el = document.getElementById(id)
    if (el) window.scrollTo({ top: el.getBoundingClientRect().top + window.scrollY - 76, behavior: 'smooth' })
  }, [])

  const goTop = useCallback((e: React.MouseEvent) => {
    e.preventDefault()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  /* ─── Scroll reveal (matches design's componentDidMount) ─── */
  useEffect(() => {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const root = rootRef.current
    if (!root) return

    const t = setTimeout(() => {
      let pending = Array.from(root.querySelectorAll('.rv'))
      if (!pending.length) return
      root.classList.add('lp-anim')

      let ticking = false
      const check = () => {
        ticking = false
        const vh = window.innerHeight
        pending = pending.filter((el) => {
          const r = el.getBoundingClientRect()
          if (r.top < vh * 0.92 && r.bottom > 0) {
            el.classList.add('on')
            return false
          }
          return true
        })
        if (!pending.length) {
          window.removeEventListener('scroll', onScroll)
          window.removeEventListener('resize', onScroll)
        }
      }
      const onScroll = () => {
        if (!ticking) {
          ticking = true
          requestAnimationFrame(check)
        }
      }
      window.addEventListener('scroll', onScroll, { passive: true })
      window.addEventListener('resize', onScroll)
      check()

      return () => {
        window.removeEventListener('scroll', onScroll)
        window.removeEventListener('resize', onScroll)
      }
    }, 60)

    return () => clearTimeout(t)
  }, [])

  return (
    <div
      ref={rootRef}
      className="lp-root"
      data-accent="brick"
      style={{ minHeight: '100vh', fontFamily: "'Hanken Grotesk',system-ui,sans-serif", color: '#2a2320', background: 'radial-gradient(140% 60% at 50% 0%,#f9f4ea 0%,#f2ead9 100%)' }}
    >
      {/* ── NAV ── */}
      <div style={{ position: 'sticky', top: 0, zIndex: 50, background: 'rgba(247,241,230,.86)', backdropFilter: 'blur(12px)', borderBottom: '1px solid rgba(232,221,204,.8)' }}>
        <div style={{ maxWidth: 1120, margin: '0 auto', padding: '0 24px', height: 66, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
          <a href="#topo" onClick={goTop} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <ComandaLogo size="md" />
            <span style={{ font: "800 14px/1 'Schibsted Grotesk',sans-serif", letterSpacing: '.14em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Comanda</span>
          </a>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <a href="#como-funciona" onClick={scrollTo('como-funciona')} style={{ padding: '9px 13px', borderRadius: 10, font: "600 14px/1 'Hanken Grotesk',sans-serif", color: 'var(--ink2)' }}>Como funciona</a>
            <a href="#recursos" onClick={scrollTo('recursos')} style={{ padding: '9px 13px', borderRadius: 10, font: "600 14px/1 'Hanken Grotesk',sans-serif", color: 'var(--ink2)' }}>Recursos</a>
            <a href="#planos" onClick={scrollTo('planos')} style={{ padding: '9px 13px', borderRadius: 10, font: "600 14px/1 'Hanken Grotesk',sans-serif", color: 'var(--ink2)' }}>Planos</a>
            <Link to="/onboarding">
              <button style={{ marginLeft: 8, border: 'none', cursor: 'pointer', padding: '11px 18px', borderRadius: 12, background: 'var(--acc)', color: '#fff', font: "800 14px/1 'Hanken Grotesk',sans-serif", boxShadow: '0 10px 20px -10px var(--acc)' }}>
                Criar meu cardápio
              </button>
            </Link>
          </div>
        </div>
      </div>

      {/* ── HERO ── */}
      <div id="topo" style={{ maxWidth: 1120, margin: '0 auto', padding: 'clamp(48px,7vw,88px) 24px clamp(40px,6vw,72px)', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 'clamp(32px,5vw,64px)', overflowX: 'clip' }}>
        <div style={{ flex: '1 1 420px', minWidth: 'min(420px,100%)' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '7px 13px', borderRadius: 999, background: 'var(--acc-tint)', border: '1px solid #f0d5cc', color: 'var(--acc-d)', font: "700 12.5px/1 'Hanken Grotesk',sans-serif" }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: 'var(--acc)' }} />
            Feito para quem vende pelo WhatsApp
          </div>
          <h1 style={{ margin: '18px 0 0', font: "900 clamp(38px,5vw,58px)/1.04 'Schibsted Grotesk',sans-serif", letterSpacing: '-.015em', color: 'var(--ink)', textWrap: 'balance' as never }}>
            Seu cardápio no ar. Seus pedidos <span style={{ color: 'var(--acc)' }}>organizados</span>.
          </h1>
          <p style={{ margin: '18px 0 0', fontSize: 'clamp(16px,1.6vw,18.5px)', lineHeight: 1.55, color: 'var(--ink2)', maxWidth: 460, textWrap: 'pretty' as never }}>
            O pedido continua chegando no seu WhatsApp — só que pronto, formatado e sem se perder no meio da conversa. Cardápio digital com link próprio e um painel simples de acompanhar.
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, marginTop: 28 }}>
            <Link to="/onboarding">
              <button style={{ border: 'none', cursor: 'pointer', padding: '16px 26px', borderRadius: 14, background: 'var(--acc)', color: '#fff', font: "800 16px/1 'Hanken Grotesk',sans-serif", boxShadow: '0 14px 28px -12px var(--acc)' }}>
                Criar meu cardápio grátis
              </button>
            </Link>
            <a href="#como-funciona" onClick={scrollTo('como-funciona')} style={{ padding: '16px 20px', borderRadius: 14, border: '1.5px solid var(--line)', background: 'var(--card)', color: 'var(--ink)', font: "700 15px/1 'Hanken Grotesk',sans-serif", textDecoration: 'none' }}>
              Ver como funciona
            </a>
          </div>
          <div style={{ marginTop: 16, font: "600 13.5px/1.5 'Hanken Grotesk',sans-serif", color: 'var(--ink3)' }}>
            Grátis até 30 pedidos por mês · Sem cartão · No ar em menos de 10 minutos
          </div>
        </div>
        <div style={{ flex: '1 1 380px', minWidth: 'min(380px,100%)', display: 'flex', justifyContent: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start' }}>
            <img src="/shots/storefront.png" alt="Cardápio digital do Comanda no celular" style={{ width: 'clamp(200px,26vw,290px)', height: 'auto', filter: 'drop-shadow(0 30px 50px rgba(60,42,24,.3))', animation: 'lp-float 7s ease-in-out infinite alternate' }} />
            <img src="/shots/pedidos.png" alt="Painel de pedidos do Comanda" style={{ width: 'clamp(200px,26vw,290px)', height: 'auto', marginLeft: -64, marginTop: 54, filter: 'drop-shadow(0 30px 50px rgba(60,42,24,.3))', animation: 'lp-float2 7s ease-in-out infinite alternate' }} />
          </div>
        </div>
      </div>

      {/* ── ANTES / DEPOIS ── */}
      <div style={{ background: '#ece3d3', borderTop: '1px solid #e4d9c5', borderBottom: '1px solid #e4d9c5' }}>
        <div className="rv" style={{ maxWidth: 1120, margin: '0 auto', padding: 'clamp(56px,8vw,96px) 24px' }}>
          <div style={{ maxWidth: 560, margin: '0 auto', textAlign: 'center' }}>
            <div style={{ font: "700 12.5px/1 'JetBrains Mono',monospace", letterSpacing: '.16em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Por que não só o WhatsApp?</div>
            <h2 style={{ margin: '14px 0 0', font: "900 clamp(28px,3.4vw,38px)/1.12 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: 'var(--ink)', textWrap: 'balance' as never }}>O pedido chega no mesmo lugar. Só que inteiro.</h2>
            <p style={{ margin: '14px 0 0', fontSize: '16.5px', lineHeight: 1.55, color: 'var(--ink2)', textWrap: 'pretty' as never }}>Nada de montar o pedido em dez mensagens, no meio de outras quarenta conversas.</p>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(300px,1fr))', gap: 22, marginTop: 44, maxWidth: 860, marginLeft: 'auto', marginRight: 'auto' }}>
            {/* Antes */}
            <div style={{ background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 20, padding: 24, boxShadow: '0 14px 34px -22px rgba(60,42,24,.3)' }}>
              <div style={{ font: "700 11.5px/1 'JetBrains Mono',monospace", letterSpacing: '.14em', textTransform: 'uppercase', color: 'var(--ink3)', marginBottom: 18 }}>Antes · pedido solto no chat</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 9, fontSize: '14.5px', lineHeight: 1.45, color: 'var(--ink)' }}>
                <div style={{ alignSelf: 'flex-start', maxWidth: '85%', background: '#f1e9da', borderRadius: '4px 14px 14px 14px', padding: '10px 13px' }}>oi, tem hambúrguer hoje?</div>
                <div style={{ alignSelf: 'flex-start', maxWidth: '85%', background: '#f1e9da', borderRadius: '4px 14px 14px 14px', padding: '10px 13px' }}>quero 1 x-tudo e 2 guaraná</div>
                <div style={{ alignSelf: 'flex-start', maxWidth: '85%', background: '#f1e9da', borderRadius: '4px 14px 14px 14px', padding: '10px 13px' }}>ah, sem cebola! quanto fica com a entrega?</div>
                <div style={{ alignSelf: 'flex-start', maxWidth: '85%', background: '#f1e9da', borderRadius: '4px 14px 14px 14px', padding: '10px 13px' }}>alô?? viu meu pedido?</div>
              </div>
              <div style={{ marginTop: 16, paddingTop: 14, borderTop: '1px dashed var(--line)', font: "600 13px/1.45 'Hanken Grotesk',sans-serif", color: '#a05a4c' }}>
                Endereço não veio. Preço foi calculated de cabeça. E no pico, mensagem some.
              </div>
            </div>
            {/* Depois */}
            <div style={{ background: 'var(--card)', border: '1.5px solid #cfe3d2', borderRadius: 20, padding: 24, boxShadow: '0 14px 34px -22px rgba(31,138,82,.35)' }}>
              <div style={{ font: "700 11.5px/1 'JetBrains Mono',monospace", letterSpacing: '.14em', textTransform: 'uppercase', color: 'var(--green)', marginBottom: 18 }}>Depois · pedido pelo Comanda</div>
              <div style={{ background: '#f4f9f1', borderRadius: 14, padding: '15px 16px', fontSize: '13.5px', lineHeight: 1.55, color: 'var(--ink)', fontFamily: "'JetBrains Mono',monospace" }}>
                <div style={{ fontWeight: 700, marginBottom: 6 }}>📋 Pedido #A4F2</div>
                <div>1× X-Tudo sem cebola</div>
                <div>2× Guaraná lata</div>
                <div style={{ marginTop: 8, borderTop: '1px dashed #d6e9d0', paddingTop: 8, fontWeight: 700 }}>
                  Total: R$ 48,00 · Entrega<br />
                  Rua das Acácias, 142
                </div>
              </div>
              <div style={{ marginTop: 16, paddingTop: 14, borderTop: '1px dashed #cfe3d2', font: "600 13px/1.45 'Hanken Grotesk',sans-serif", color: 'var(--green)' }}>
                Preço já calculado. Endereço já veio. Tudo num pedido só, pronto para aceitar.
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── COMO FUNCIONA ── */}
      <div id="como-funciona" style={{ maxWidth: 1120, margin: '0 auto', padding: 'clamp(56px,8vw,96px) 24px' }}>
        <div className="rv" style={{ maxWidth: 560, margin: '0 auto', textAlign: 'center' }}>
          <div style={{ font: "700 12.5px/1 'JetBrains Mono',monospace", letterSpacing: '.16em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Como funciona</div>
          <h2 style={{ margin: '14px 0 0', font: "900 clamp(28px,3.4vw,38px)/1.12 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: 'var(--ink)', textWrap: 'balance' as never }}>
            Três passos. Sem técnico, sem setup complicado.
          </h2>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 22, marginTop: 48 }}>
          {[
            { step: '01', title: 'Monte seu cardápio', desc: 'Adicione seus produtos, preços e adicionais direto do celular. A gente pré-monta um cardápio de exemplo para o seu segmento.' },
            { step: '02', title: 'Compartilhe o link', desc: 'Cada negócio ganha um link próprio e um QR Code. Cole no seu Instagram, no grupo do bairro ou imprima para o balcão.' },
            { step: '03', title: 'Receba os pedidos', desc: 'O cliente escolhe, monta o pedido e te manda no WhatsApp — pronto, organizado e com o total já calculado.' },
          ].map((s) => (
            <div key={s.step} className="lp-card rv" style={{ background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 22, padding: '30px 26px', boxShadow: '0 14px 34px -24px rgba(60,42,24,.3)' }}>
              <div style={{ font: "800 13px/1 'JetBrains Mono',monospace", letterSpacing: '.12em', color: 'var(--acc)', marginBottom: 16 }}>{s.step}</div>
              <div style={{ font: "800 18px/1.2 'Schibsted Grotesk',sans-serif", color: 'var(--ink)' }}>{s.title}</div>
              <p style={{ margin: '12px 0 0', fontSize: '15px', lineHeight: 1.55, color: 'var(--ink2)' }}>{s.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* ── RECURSOS ── */}
      <div id="recursos" style={{ maxWidth: 1120, margin: '0 auto', padding: 'clamp(32px,5vw,56px) 24px clamp(56px,8vw,96px)' }}>
        <div className="rv" style={{ maxWidth: 560, margin: '0 auto', textAlign: 'center' }}>
          <div style={{ font: "700 12.5px/1 'JetBrains Mono',monospace", letterSpacing: '.16em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Recursos</div>
          <h2 style={{ margin: '14px 0 0', font: "900 clamp(28px,3.4vw,38px)/1.12 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: 'var(--ink)', textWrap: 'balance' as never }}>
            Tudo que você precisa para vender, em uma tela.
          </h2>
        </div>

        {/* Feature row 1: Painel de pedidos */}
        <div className="rv" style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 'clamp(32px,5vw,72px)', padding: 'clamp(32px,5vw,56px) 0' }}>
          <div style={{ flex: '1 1 320px', minWidth: 'min(320px,100%)', display: 'flex', justifyContent: 'center' }}>
            <img src="/shots/pedidos.png" alt="Painel de pedidos" style={{ width: 'min(310px,80vw)', height: 'auto', filter: 'drop-shadow(0 26px 44px rgba(60,42,24,.28))' }} />
          </div>
          <div style={{ flex: '1 1 380px', minWidth: 'min(380px,100%)' }}>
            <div style={{ font: "700 12.5px/1 'JetBrains Mono',monospace", letterSpacing: '.16em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Painel de pedidos</div>
            <h3 style={{ margin: '14px 0 0', font: "900 clamp(26px,3vw,34px)/1.15 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: 'var(--ink)', textWrap: 'balance' as never }}>
              O pico virou uma lista organizada
            </h3>
            <p style={{ margin: '14px 0 0', fontSize: 16, lineHeight: 1.55, color: 'var(--ink2)', maxWidth: 440, textWrap: 'pretty' as never }}>
              Cada pedido vira um cartão com status, tempo e total. Aceite, prepare e marque como entregue com um toque.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 11, marginTop: 20 }}>
              {['Filtro por status: novos, em preparo, prontos', 'Notificação de pedido novo em tempo real', 'Resumo do dia: faturamento e pedidos de um olhar'].map((t) => (
                <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 10, font: "600 15px/1.4 'Hanken Grotesk',sans-serif", color: 'var(--ink)' }}>
                  <span style={{ width: 22, height: 22, flex: 'none', borderRadius: '50%', background: 'var(--green-tint)', color: 'var(--green)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Check13 />
                  </span>
                  {t}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Feature row 2: Cardápio digital */}
        <div className="rv" style={{ display: 'flex', flexWrap: 'wrap-reverse', alignItems: 'center', gap: 'clamp(32px,5vw,72px)', padding: 'clamp(32px,5vw,56px) 0' }}>
          <div style={{ flex: '1 1 380px', minWidth: 'min(380px,100%)' }}>
            <div style={{ font: "700 12.5px/1 'JetBrains Mono',monospace", letterSpacing: '.16em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Cardápio digital</div>
            <h3 style={{ margin: '14px 0 0', font: "900 clamp(26px,3vw,34px)/1.15 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: 'var(--ink)', textWrap: 'balance' as never }}>
              O cardápio se adapta ao seu dia, não o contrário
            </h3>
            <p style={{ margin: '14px 0 0', fontSize: 16, lineHeight: 1.55, color: 'var(--ink2)', maxWidth: 440, textWrap: 'pretty' as never }}>
              Acabou a batata? Um toque e ela some do cardápio. Marmita só de segunda a sexta? Configure uma vez e esqueça.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 11, marginTop: 20 }}>
              {['Adicionais com preço, obrigatórios ou opcionais', 'Disponibilidade por dia da semana', 'Aberto e fechado com um toque, no horário que você definir'].map((t) => (
                <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 10, font: "600 15px/1.4 'Hanken Grotesk',sans-serif", color: 'var(--ink)' }}>
                  <span style={{ width: 22, height: 22, flex: 'none', borderRadius: '50%', background: 'var(--green-tint)', color: 'var(--green)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Check13 />
                  </span>
                  {t}
                </div>
              ))}
            </div>
          </div>
          <div style={{ flex: '1 1 320px', minWidth: 'min(320px,100%)', display: 'flex', justifyContent: 'center' }}>
            <img src="/shots/cardapio.png" alt="Gestão do cardápio" style={{ width: 'min(310px,80vw)', height: 'auto', filter: 'drop-shadow(0 26px 44px rgba(60,42,24,.28))' }} />
          </div>
        </div>

        {/* Trust bar */}
        <div className="rv" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(220px,1fr))', gap: 16, marginTop: 'clamp(24px,4vw,48px)', padding: 26, background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 22, boxShadow: '0 14px 34px -24px rgba(60,42,24,.3)' }}>
          {[
            { title: 'Cliente não baixa nada', desc: 'O cardápio abre direto no navegador, pelo link.' },
            { title: 'Instala como app', desc: 'O painel vira um ícone na tela do seu celular.' },
            { title: 'Nada falha em silêncio', desc: 'Se algo der errado, a tela avisa — pedido não some.' },
            { title: 'Seus dados, protegidos', desc: 'Política de privacidade clara, de acordo com a LGPD.' },
          ].map((i) => (
            <div key={i.title} style={{ padding: '6px 4px' }}>
              <div style={{ font: "800 16px/1.25 'Schibsted Grotesk',sans-serif", color: 'var(--ink)' }}>{i.title}</div>
              <div style={{ marginTop: 6, fontSize: 14, lineHeight: 1.5, color: 'var(--ink2)' }}>{i.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* ── PLANOS ── */}
      <div id="planos" style={{ background: '#ece3d3', borderTop: '1px solid #e4d9c5', borderBottom: '1px solid #e4d9c5' }}>
        <div className="rv" style={{ maxWidth: 1120, margin: '0 auto', padding: 'clamp(56px,8vw,96px) 24px' }}>
          <div style={{ maxWidth: 520, margin: '0 auto', textAlign: 'center' }}>
            <div style={{ font: "700 12.5px/1 'JetBrains Mono',monospace", letterSpacing: '.16em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Planos</div>
            <h2 style={{ margin: '14px 0 0', font: "900 clamp(28px,3.4vw,38px)/1.12 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: 'var(--ink)' }}>Comece de graça. Cresça quando fizer sentido.</h2>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(280px,1fr))', gap: 22, marginTop: 44, maxWidth: 780, marginLeft: 'auto', marginRight: 'auto' }}>
            {/* Gratuito */}
            <div className="lp-card" style={{ background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 22, padding: 30, boxShadow: '0 14px 34px -24px rgba(60,42,24,.3)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ font: "800 17px/1 'Schibsted Grotesk',sans-serif", color: 'var(--ink)' }}>Gratuito</div>
              <div style={{ marginTop: 4, font: "600 13.5px/1.4 'Hanken Grotesk',sans-serif", color: 'var(--ink3)' }}>Comece agora</div>
              <div style={{ marginTop: 18, display: 'flex', alignItems: 'baseline', gap: 6 }}>
                <span style={{ font: "900 40px/1 'Schibsted Grotesk',sans-serif", color: 'var(--ink)' }}>R$ 0</span>
                <span style={{ font: "600 14px/1 'Hanken Grotesk',sans-serif", color: 'var(--ink3)' }}>/ sempre</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 22, flex: 1 }}>
                {['Até 30 pedidos por mês', 'Cardápio digital completo, com adicionais', 'Painel de pedidos com resumo do dia', 'Link próprio e QR Code'].map((f) => (
                  <div key={f} style={{ display: 'flex', gap: 9, fontSize: '14.5px', lineHeight: 1.45, color: 'var(--ink)' }}>
                    <span style={{ color: 'var(--green)', flex: 'none', marginTop: 2 }}><Check13 /></span>
                    {f}
                  </div>
                ))}
              </div>
              <Link to="/onboarding">
                <button style={{ marginTop: 26, border: '1.5px solid var(--line)', background: 'var(--cream)', cursor: 'pointer', padding: 15, borderRadius: 13, color: 'var(--ink)', font: "800 15px/1 'Hanken Grotesk',sans-serif", width: '100%' }}>
                  Começar grátis
                </button>
              </Link>
            </div>
            {/* Essencial */}
            <div className="lp-card" style={{ position: 'relative', background: 'var(--card)', border: '2px solid var(--acc)', borderRadius: 22, padding: 30, boxShadow: '0 22px 48px -22px rgba(181,60,37,.4)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ position: 'absolute', top: -13, left: 28, background: 'var(--acc)', color: '#fff', font: "800 11.5px/1 'Hanken Grotesk',sans-serif", letterSpacing: '.08em', textTransform: 'uppercase', padding: '7px 12px', borderRadius: 999 }}>
                Para operar todo dia
              </div>
              <div style={{ font: "800 17px/1 'Schibsted Grotesk',sans-serif", color: 'var(--ink)' }}>Essencial</div>
              <div style={{ marginTop: 4, font: "600 13.5px/1.4 'Hanken Grotesk',sans-serif", color: 'var(--ink3)' }}>Meu negócio organizado</div>
              <div style={{ marginTop: 18, display: 'flex', alignItems: 'baseline', gap: 6 }}>
                <span style={{ font: "900 40px/1 'Schibsted Grotesk',sans-serif", color: 'var(--ink)' }}>R$ 49</span>
                <span style={{ font: "600 14px/1 'Hanken Grotesk',sans-serif", color: 'var(--ink3)' }}>/ mês</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 22, flex: 1 }}>
                {[
                  { text: 'Pedidos ilimitados', bold: true, suffix: '\u00a0por mês' },
                  { text: 'Produtos e categorias ilimitados' },
                  { text: 'Histórico de pedidos por 30 dias' },
                  { text: 'Tudo do plano Gratuito' },
                ].map((f) => (
                  <div key={f.text} style={{ display: 'flex', gap: 9, fontSize: '14.5px', lineHeight: 1.45, color: 'var(--ink)' }}>
                    <span style={{ color: 'var(--acc)', flex: 'none', marginTop: 2 }}><Check13 /></span>
                    {f.bold ? <><b style={{ fontWeight: 700 }}>{f.text}</b>{f.suffix}</> : f.text}
                  </div>
                ))}
              </div>
              <Link to="/onboarding">
                <button style={{ marginTop: 26, border: 'none', cursor: 'pointer', padding: 16, borderRadius: 13, background: 'var(--acc)', color: '#fff', font: "800 15px/1 'Hanken Grotesk',sans-serif", boxShadow: '0 12px 24px -10px var(--acc)', width: '100%' }}>
                  Começar no Essencial
                </button>
              </Link>
            </div>
          </div>
          <div style={{ textAlign: 'center', marginTop: 22, font: "600 13.5px/1.5 'Hanken Grotesk',sans-serif", color: 'var(--ink3)' }}>
            O plano gratuito não é demonstração — é o produto completo, no seu ritmo.
          </div>
        </div>
      </div>

      {/* ── CTA FINAL ── */}
      <div style={{ maxWidth: 1120, margin: '0 auto', padding: 'clamp(56px,8vw,96px) 24px' }}>
        <div className="rv" style={{ background: 'linear-gradient(135deg,var(--acc) 0%,var(--acc-d) 100%)', borderRadius: 26, padding: 'clamp(40px,6vw,64px) clamp(28px,5vw,64px)', textAlign: 'center', boxShadow: '0 30px 60px -28px rgba(156,53,31,.55)' }}>
          <div style={{ display: 'flex', justifyContent: 'center', filter: 'drop-shadow(0 6px 14px rgba(0,0,0,.2))' }}>
            <ComandaLogo size="lg" />
          </div>
          <h2 style={{ margin: '20px auto 0', font: "900 clamp(28px,3.6vw,42px)/1.1 'Schibsted Grotesk',sans-serif", letterSpacing: '-.01em', color: '#fff', maxWidth: 560, textWrap: 'balance' as never }}>
            Seu cardápio no ar hoje. Antes do próximo pico.
          </h2>
          <p style={{ margin: '14px auto 0', fontSize: '16.5px', lineHeight: 1.55, color: 'rgba(255,255,255,.85)', maxWidth: 440, textWrap: 'pretty' as never }}>
            Cadastro direto do celular, em menos de 10 minutos. Sem cartão, sem instalação, sem fidelidade.
          </p>
          <Link to="/onboarding">
            <button style={{ marginTop: 28, border: 'none', cursor: 'pointer', padding: '17px 30px', borderRadius: 14, background: '#fffdf9', color: 'var(--acc-d)', font: "800 16px/1 'Hanken Grotesk',sans-serif", boxShadow: '0 14px 30px -12px rgba(0,0,0,.35)' }}>
              Criar meu cardápio grátis
            </button>
          </Link>
        </div>
      </div>

      {/* ── FOOTER ── */}
      <div style={{ borderTop: '1px solid var(--line)' }}>
        <div style={{ maxWidth: 1120, margin: '0 auto', padding: '28px 24px 36px', display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 18 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
            <ComandaLogo size="xs" />
            <span style={{ font: "800 12.5px/1 'Schibsted Grotesk',sans-serif", letterSpacing: '.14em', textTransform: 'uppercase', color: 'var(--acc-d)' }}>Comanda</span>
            <span style={{ font: "600 13px/1 'Hanken Grotesk',sans-serif", color: 'var(--ink3)', marginLeft: 6 }}>· feito no Brasil</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px 20px', font: "600 13.5px/1 'Hanken Grotesk',sans-serif" }}>
            <Link to="/privacidade" style={{ color: 'var(--ink2)', textDecoration: 'none' }}>Política de privacidade</Link>
            <Link to="/termos" style={{ color: 'var(--ink2)', textDecoration: 'none' }}>Termos de serviço</Link>
            <a href="mailto:contato@comanda.local" style={{ color: 'var(--ink2)', textDecoration: 'none' }}>Fale com a gente</a>
          </div>
        </div>
      </div>
    </div>
  )
}
