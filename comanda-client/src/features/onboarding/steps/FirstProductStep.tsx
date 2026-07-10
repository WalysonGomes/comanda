import { ImageIcon, Loader2 } from 'lucide-react'
import { useState, type ChangeEvent } from 'react'

import { useUploadImage } from '@/features/menu/queries'

export type FirstProductFields = {
  name: string
  description: string
  price: string
  imageUrl: string | null
}

/**
 * Step 3 (`.design/Comanda Painel.dc.html` `ob3`, lines 189-202): edits the first product from
 * the demo cardápio that `onboarding/seed` already materialized (task 6.5) — the dono edits
 * something real and pre-filled, not a blank form.
 */
export function FirstProductStep({
  fields,
  onChange,
}: {
  fields: FirstProductFields
  onChange: (fields: FirstProductFields) => void
}) {
  const uploadImage = useUploadImage()
  const [uploadState, setUploadState] = useState<'idle' | 'uploading' | 'error'>('idle')

  function set<K extends keyof FirstProductFields>(key: K, value: FirstProductFields[K]) {
    onChange({ ...fields, [key]: value })
  }

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploadState('uploading')
    uploadImage.mutate(file, {
      onSuccess: (res) => {
        set('imageUrl', res.imageUrl)
        setUploadState('idle')
      },
      onError: () => setUploadState('error'),
    })
  }

  return (
    <div className="animate-cmd-slide">
      <h1 className="font-display text-[26px] leading-[1.1] font-black text-ink">Seu primeiro produto</h1>
      <p className="mt-2 text-sm text-ink-2">Já preenchemos um exemplo. Edite à vontade.</p>

      <label
        className="relative mt-5 flex h-[150px] cursor-pointer flex-col items-center justify-center gap-2 overflow-hidden rounded-2xl border-2 border-dashed border-[#d8c9b1] text-[#b3a691]"
        style={
          !fields.imageUrl
            ? { backgroundImage: 'repeating-linear-gradient(45deg,#f6efe2,#f6efe2 9px,#f2ead9 9px,#f2ead9 18px)' }
            : undefined
        }
      >
        <input type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={handleFileChange} />
        {fields.imageUrl && <img src={fields.imageUrl} alt="" className="absolute inset-0 size-full object-cover" />}
        {uploadState === 'uploading' ? (
          <>
            <Loader2 className="size-5 animate-cmd-spin" />
            <span className="font-mono text-xs font-semibold">enviando…</span>
          </>
        ) : (
          !fields.imageUrl && (
            <>
              <ImageIcon className="size-5" />
              <span className="font-mono text-xs font-semibold">foto do produto</span>
            </>
          )
        )}
      </label>
      {uploadState === 'error' && (
        <p className="mt-2 text-[12.5px] text-[#a05a4c]">Falha ao enviar a foto. O produto pode ser salvo sem foto.</p>
      )}

      <div className="mt-4 flex flex-col gap-3.5">
        <div>
          <span className="mb-1.5 block text-xs font-bold text-ink-2">Nome</span>
          <input
            value={fields.name}
            onChange={(e) => set('name', e.target.value)}
            className="w-full rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3.5 text-[15px] text-ink outline-none focus:border-acc"
          />
        </div>
        <div>
          <span className="mb-1.5 block text-xs font-bold text-ink-2">Descrição</span>
          <textarea
            value={fields.description}
            onChange={(e) => set('description', e.target.value)}
            className="h-16 w-full resize-none rounded-xl border-[1.5px] border-line bg-card p-3 text-sm leading-snug text-ink outline-none focus:border-acc"
          />
        </div>
        <div>
          <span className="mb-1.5 block text-xs font-bold text-ink-2">Preço</span>
          <input
            value={fields.price}
            onChange={(e) => set('price', e.target.value)}
            placeholder="0,00"
            className="w-[55%] rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3.5 font-bold text-[15px] text-ink outline-none focus:border-acc"
          />
        </div>
      </div>
    </div>
  )
}
