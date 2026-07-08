/**
 * Tags de PWA (manifest, theme-color, apple-touch-icon) injetadas em runtime, nunca embutidas no
 * `index.html` único do bundle — esse HTML é compartilhado com o storefront, e a spec `owner-pwa`
 * exige que o storefront NÃO referencie o manifest do painel. Chamada apenas no branch do painel
 * (`main.tsx`, ao lado de `registerPainelServiceWorker`).
 */
export function injectPainelHeadTags(): void {
  const head = document.head

  const manifestLink = document.createElement('link')
  manifestLink.rel = 'manifest'
  manifestLink.href = '/manifest.webmanifest'
  head.appendChild(manifestLink)

  const themeColor = document.createElement('meta')
  themeColor.name = 'theme-color'
  themeColor.content = '#d6492f'
  head.appendChild(themeColor)

  const appleTouchIcon = document.createElement('link')
  appleTouchIcon.rel = 'apple-touch-icon'
  appleTouchIcon.href = '/apple-touch-icon.png'
  head.appendChild(appleTouchIcon)

  const appleCapable = document.createElement('meta')
  appleCapable.name = 'apple-mobile-web-app-capable'
  appleCapable.content = 'yes'
  head.appendChild(appleCapable)

  const appleTitle = document.createElement('meta')
  appleTitle.name = 'apple-mobile-web-app-title'
  appleTitle.content = 'Comanda'
  head.appendChild(appleTitle)
}
