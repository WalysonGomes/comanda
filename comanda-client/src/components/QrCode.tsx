import { useEffect, useState } from 'react'
import { toDataURL } from 'qrcode'

/**
 * Generated client-side (design.md Decision 9 / task 6.6, 8.1) — no dependency on an external QR
 * service. `qrcode` is a small, dependency-free, widely-used encoder (Regra 6: boring, justified
 * by the spec explicitly requiring client-side generation).
 */
export function QrCode({ value, size = 148 }: { value: string; size?: number }) {
  const [dataUrl, setDataUrl] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    toDataURL(value, { width: size, margin: 0, color: { dark: '#2a2320', light: '#00000000' } })
      .then((url) => {
        if (!cancelled) setDataUrl(url)
      })
      .catch(() => {
        if (!cancelled) setDataUrl(null)
      })
    return () => {
      cancelled = true
    }
  }, [value, size])

  if (!dataUrl) {
    return <div className="sk" style={{ width: size, height: size }} />
  }
  return <img src={dataUrl} alt="QR Code do cardápio" width={size} height={size} />
}
