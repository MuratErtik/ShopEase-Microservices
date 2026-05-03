import { useForm } from 'react-hook-form'
import { useEffect } from 'react'

const CATEGORIES = ['ELECTRONICS', 'FASHION', 'HOMELIVING', 'PERSONALCARE', 'AUTOMOTIVE']
const CATEGORY_LABELS = {
  ELECTRONICS:  'Elektronik',
  FASHION:      'Moda',
  HOMELIVING:   'Ev & Yaşam',
  PERSONALCARE: 'Kişisel Bakım',
  AUTOMOTIVE:   'Otomotiv',
}

export default function ProductForm({ defaultValues, onSubmit, submitLabel, serverError }) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues })

  useEffect(() => { if (defaultValues) reset(defaultValues) }, [defaultValues])

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="sm:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Ürün Adı</label>
          <input
            type="text"
            placeholder="Örn: Apple iPhone 15"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            {...register('name', { required: 'Ürün adı zorunludur.', maxLength: { value: 255, message: 'En fazla 255 karakter.' } })}
          />
          {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
        </div>

        <div className="sm:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
          <textarea
            rows={3}
            placeholder="Ürün hakkında kısa açıklama..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
            {...register('description', { maxLength: { value: 512, message: 'En fazla 512 karakter.' } })}
          />
          {errors.description && <p className="mt-1 text-xs text-red-500">{errors.description.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Fiyat (₺)</label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            {...register('price', {
              required: 'Fiyat zorunludur.',
              min: { value: 0.01, message: '0\'dan büyük olmalı.' },
              valueAsNumber: true,
            })}
          />
          {errors.price && <p className="mt-1 text-xs text-red-500">{errors.price.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Kategori</label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white"
            {...register('category', { required: 'Kategori seçiniz.' })}
          >
            <option value="">Seçin...</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>{CATEGORY_LABELS[c]}</option>
            ))}
          </select>
          {errors.category && <p className="mt-1 text-xs text-red-500">{errors.category.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Marka</label>
          <input
            type="text"
            placeholder="Örn: Apple"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            {...register('brand', { required: 'Marka zorunludur.', maxLength: { value: 255, message: 'En fazla 255 karakter.' } })}
          />
          {errors.brand && <p className="mt-1 text-xs text-red-500">{errors.brand.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Renk</label>
          <input
            type="text"
            placeholder="Örn: Siyah"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            {...register('color', { required: 'Renk zorunludur.', maxLength: { value: 30, message: 'En fazla 30 karakter.' } })}
          />
          {errors.color && <p className="mt-1 text-xs text-red-500">{errors.color.message}</p>}
        </div>

        <div className="sm:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Resim URL (opsiyonel)</label>
          <input
            type="url"
            placeholder="https://..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            {...register('imageUrl', { maxLength: { value: 500, message: 'En fazla 500 karakter.' } })}
          />
          {errors.imageUrl && <p className="mt-1 text-xs text-red-500">{errors.imageUrl.message}</p>}
        </div>

        {defaultValues?.initialQuantity !== undefined && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Başlangıç Stok</label>
            <input
              type="number"
              min="0"
              placeholder="0"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              {...register('initialQuantity', {
                required: 'Stok miktarı zorunludur.',
                min: { value: 0, message: '0 veya daha fazla olmalı.' },
                valueAsNumber: true,
              })}
            />
            {errors.initialQuantity && <p className="mt-1 text-xs text-red-500">{errors.initialQuantity.message}</p>}
          </div>
        )}
      </div>

      {serverError && (
        <p className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-4 py-2">
          {serverError}
        </p>
      )}

      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full bg-indigo-600 text-white py-2.5 rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        {isSubmitting ? 'Kaydediliyor...' : submitLabel}
      </button>
    </form>
  )
}
