import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * No `@vitejs/plugin-react` here: the app's `vite.config.ts` uses a rolldown-based Vite (v8),
 * whose Plugin type is incompatible with vitest's own nested (classic) Vite dependency — passing
 * the plugin here is a type error. Not needed for tests: esbuild's `jsx: 'automatic'` (set from
 * `tsconfig.app.json`'s `"jsx": "react-jsx"`) already compiles JSX; only Fast Refresh and the
 * react-compiler babel transform are lost, neither of which matters for headless test runs.
 */
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
