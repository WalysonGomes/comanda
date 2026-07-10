import { useState, type ChangeEvent, type ReactNode } from 'react'
import { ArrowLeft, ImageIcon, Loader2 } from 'lucide-react'
import { useNavigate, useParams, useSearchParams } from 'react-router'

import { cn } from '@/lib/utils'
import { PlanLimitError } from '@/components/PlanLimitError'
import { AdditionalGroupsEditor } from '@/features/menu/AdditionalGroupsEditor'
import { DayChips } from '@/features/menu/components/DayChips'
import { Skeleton } from '@/features/menu/components/Skeleton'
import { MenuApiError, type Product, type ProductPayload } from '@/features/menu/api'
import { useCreateProduct, useProducts, useUpdateProduct, useUploadImage } from '@/features/menu/queries'

const inputClass =
  'w-full rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3.5 text-[15px] text-ink outline-none focus:border-acc'

/**
 * Thin routing wrapper: resolves which product (if any) is being edited and waits for it to
 * load, then mounts {@link ProductEditorForm} keyed by product id so its state initializes
 * straight from props — no effect needed to "hydrate" state after an async fetch resolves.
 */
export function ProductEditorScreen() {
  const { productId } = useParams()
  const [searchParams] = useSearchParams()
  const categoriaParam = searchParams.get('categoria')
  const isNew = !productId

  const productsQuery = useProducts()
  const existingProduct = !isNew ? productsQuery.data?.find((p) => String(p.id) === productId) : undefined

  if (!isNew && productsQuery.isLoading) {
    return <EditorSkeleton />
  }

  return (
    <ProductEditorForm
      key={productId ?? 'new'}
      product={existingProduct ?? null}
      categoryIdFromQuery={categoriaParam ? Number(categoriaParam) : undefined}
    />
  )
}

function ProductEditorForm({
  product,
  categoryIdFromQuery,
}: {
  product: Product | null
  categoryIdFromQuery: number | undefined
}) {
  const navigate = useNavigate()
  const isNew = product === null

  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()
  const uploadImage = useUploadImage()

  const [name, setName] = useState(product?.name ?? '')
  const [description, setDescription] = useState(product?.description ?? '')
  const [price, setPrice] = useState(product?.price ?? '')
  const [available, setAvailable] = useState(product?.available ?? true)
  const [availableDays, setAvailableDays] = useState<number[] | null>(product?.availableDays ?? null)
  const [imageUrl, setImageUrl] = useState<string | null>(product?.imageUrl ?? null)
  const [uploadState, setUploadState] = useState<'idle' | 'uploading' | 'error'>('idle')
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [formErrorCode, setFormErrorCode] = useState<string | null>(null)
  const [nameError, setNameError] = useState<string | null>(null)
  const [priceError, setPriceError] = useState<string | null>(null)

  const categoryId = product ? product.categoryId : categoryIdFromQuery

  function doUpload(file: File) {
    setUploadState('uploading')
    uploadImage.mutate(file, {
      onSuccess: (res) => {
        setImageUrl(res.imageUrl)
        setUploadState('idle')
      },
      onError: () => setUploadState('error'),
    })
  }

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setPendingFile(file)
    doUpload(file)
  }

  function validate(): boolean {
    let ok = true
    setNameError(null)
    setPriceError(null)
    if (name.trim().length === 0) {
      setNameError('Nome é obrigatório.')
      ok = false
    }
    const priceNumber = Number(price.replace(',', '.'))
    if (price.trim().length === 0 || Number.isNaN(priceNumber) || priceNumber < 0) {
      setPriceError('Informe um preço válido.')
      ok = false
    }
    return ok
  }

  function handleSave() {
    setFormError(null)
    setFormErrorCode(null)
    if (!validate()) return
    if (!categoryId) {
      setFormError('Categoria não especificada. Volte ao cardápio e use o botão "+" de uma categoria.')
      return
    }

    const body: ProductPayload = {
      name: name.trim(),
      description: description.trim() || null,
      price: price.replace(',', '.'),
      available,
      availableDays,
      categoryId,
      imageUrl,
    }

    const mutation = isNew ? createProduct.mutateAsync(body) : updateProduct.mutateAsync({ id: product!.id, body })

    mutation
      .then((saved) => {
        if (isNew) {
          navigate(`/painel/cardapio/produtos/${saved.id}`, { replace: true })
        }
      })
      .catch((e) => {
        if (e instanceof MenuApiError) {
          setFormError(e.message)
          setFormErrorCode(e.code)
        } else {
          setFormError('Não foi possível salvar. Tente novamente.')
        }
      })
  }

  const saving = createProduct.isPending || updateProduct.isPending

  return (
    <div className="flex min-h-svh flex-col bg-cream">
      <header className="flex flex-none items-center gap-3 px-4 pb-3 pt-5">
        <button
          type="button"
          onClick={() => navigate('/painel/cardapio')}
          aria-label="Voltar"
          className="flex size-9 items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
        >
          <ArrowLeft className="size-5" strokeWidth={2} />
        </button>
        <h1 className="font-display text-lg font-extrabold text-ink">{isNew ? 'Novo produto' : 'Editar produto'}</h1>
      </header>

      <div className="flex-1 px-4 pb-8">
        <label
          className="relative flex h-[140px] cursor-pointer flex-col items-center justify-center gap-2 overflow-hidden rounded-2xl border-2 border-dashed border-[#d8c9b1] text-[#b3a691]"
          style={
            !imageUrl
              ? { backgroundImage: 'repeating-linear-gradient(45deg,#f6efe2,#f6efe2 9px,#f2ead9 9px,#f2ead9 18px)' }
              : undefined
          }
        >
          <input type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={handleFileChange} />
          {imageUrl && <img src={imageUrl} alt="" className="absolute inset-0 size-full object-cover" />}
          {uploadState === 'uploading' ? (
            <>
              <Loader2 className="size-5 animate-cmd-spin" />
              <span className="font-mono text-xs font-semibold">enviando…</span>
            </>
          ) : (
            !imageUrl && (
              <>
                <ImageIcon className="size-5" />
                <span className="font-mono text-xs font-semibold">adicionar foto</span>
              </>
            )
          )}
        </label>

        {uploadState === 'error' && (
          <div className="mt-2 flex items-center justify-between gap-3 rounded-xl bg-[#f0e1dc] px-3 py-2.5 text-[12.5px] text-[#8a4a3d]">
            <span>Falha ao enviar a foto. O produto pode ser salvo sem foto.</span>
            <button
              type="button"
              onClick={() => pendingFile && doUpload(pendingFile)}
              className="flex-none font-bold underline"
            >
              Tentar de novo
            </button>
          </div>
        )}

        <div className="mt-4 flex flex-col gap-3.5">
          <Field label="Nome" error={nameError}>
            <input value={name} onChange={(e) => setName(e.target.value)} className={inputClass} />
          </Field>

          <Field label="Descrição">
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={cn(inputClass, 'h-16 resize-none')}
            />
          </Field>

          <div className="flex items-end gap-3">
            <Field label="Preço" error={priceError} className="flex-1">
              <input
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="0,00"
                className={cn(inputClass, 'font-bold')}
              />
            </Field>
            <button
              type="button"
              onClick={() => setAvailable((a) => !a)}
              className="flex flex-1 items-center justify-between gap-2 rounded-xl border-[1.5px] border-line bg-card px-3.5 py-3"
            >
              <span className="text-[13px] font-bold" style={{ color: available ? '#2f7a44' : '#a05a4c' }}>
                {available ? 'Disponível' : 'Indisponível'}
              </span>
              <span
                className="relative h-[25px] w-[42px] flex-none rounded-full"
                style={{ background: available ? 'var(--acc)' : '#d9cdb6' }}
              >
                <span
                  className="absolute top-[3px] size-[19px] rounded-full bg-white shadow transition-[left]"
                  style={{ left: available ? '20px' : '3px' }}
                />
              </span>
            </button>
          </div>
        </div>

        <p className="mt-5 mb-2 text-xs font-bold text-ink-2">Disponível nos dias</p>
        <DayChips value={availableDays} onChange={setAvailableDays} />

        <AdditionalGroupsEditor productId={product?.id} />

        {formError && (
          <div className="mt-4">
            <PlanLimitError code={formErrorCode} message={formError} />
          </div>
        )}

        <button
          type="button"
          disabled={saving}
          onClick={handleSave}
          className="mt-6 w-full rounded-2xl bg-acc py-3.5 text-[15px] font-extrabold text-white shadow-[0_10px_22px_-10px_var(--acc)] disabled:opacity-60"
        >
          {saving ? 'Salvando…' : 'Salvar produto'}
        </button>
      </div>
    </div>
  )
}

function Field({
  label,
  error,
  className,
  children,
}: {
  label: string
  error?: string | null
  className?: string
  children: ReactNode
}) {
  return (
    <label className={cn('block', className)}>
      <span className="mb-1.5 block text-xs font-bold text-ink-2">{label}</span>
      {children}
      {error && <span className="mt-1.5 block text-[11.5px] font-semibold text-[#a05a4c]">{error}</span>}
    </label>
  )
}

function EditorSkeleton() {
  return (
    <div className="flex min-h-svh flex-col gap-4 bg-cream px-4 pt-5">
      <Skeleton className="h-9 w-9 rounded-[11px]" />
      <Skeleton className="h-[140px] w-full rounded-2xl" />
      <Skeleton className="h-12 w-full rounded-xl" />
      <Skeleton className="h-16 w-full rounded-xl" />
      <Skeleton className="h-12 w-full rounded-xl" />
    </div>
  )
}
