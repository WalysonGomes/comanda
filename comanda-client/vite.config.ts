import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const rootDomains = process.env.VITE_ROOT_DOMAINS || process.env.APP_DOMAIN || env.VITE_ROOT_DOMAINS || ''
  if (mode === 'production' && !rootDomains.trim()) {
    throw new Error('VITE_ROOT_DOMAINS or APP_DOMAIN is required for a production build')
  }
  return {
  define: {
    'import.meta.env.VITE_ROOT_DOMAINS': JSON.stringify(rootDomains),
    'import.meta.env.VITE_ROOT_HOST_ALIASES': JSON.stringify(process.env.VITE_ROOT_HOST_ALIASES || env.VITE_ROOT_HOST_ALIASES || ''),
  },
  plugins: [
    react(),
    babel({ presets: [reactCompilerPreset()] }),
    tailwindcss(),
    // owner-pwa: manifest gerenciado a mao (manifest: false) — o registro automatico de
    // `<link rel="manifest">` do plugin roda no unico index.html do bundle e vazaria para o
    // storefront (spec `owner-pwa`, "Storefront do cliente permanece sem PWA"). O link real e
    // injetado em runtime, apenas no branch do painel (`pwa/registerSW.ts`).
    VitePWA({
      strategies: 'generateSW',
      registerType: 'prompt',
      injectRegister: null,
      manifest: false,
      // D2: o registro (pwa/registerSW.ts) usa este scope — restringe o SW às rotas do painel,
      // nunca ao storefront, mesmo que ambos um dia compartilhem host/subdomínio.
      scope: '/painel/',
      workbox: {
        // Escopo do painel (D2): fallback de navegacao só serve o shell para rotas de
        // `/painel/**` — o `scope: '/painel/'` do registro (registerSW.ts) já restringe a
        // interceptação do SW, isso é a segunda barreira independente (design.md Risks).
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/(?!painel\/).*/],
        globPatterns: ['**/*.{js,css,html,svg,png,ico,webmanifest,woff,woff2}'],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    // Backend serve a SPA como artefato unico (PRD 7.2/7.3): o build cai direto nos
    // static resources do Spring Boot.
    outDir: '../comanda-api/src/main/resources/static',
    emptyOutDir: true,
  },
  }
})
