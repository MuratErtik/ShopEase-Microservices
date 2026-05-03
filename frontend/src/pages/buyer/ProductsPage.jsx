import { useState, useEffect, useCallback } from 'react'
import Layout from '../../components/layout/Layout'
import ProductCard from '../../components/product/ProductCard'
import { productService } from '../../services/productService'

const CATEGORIES = ['ELECTRONICS', 'FASHION', 'HOMELIVING', 'PERSONALCARE', 'AUTOMOTIVE']

const CATEGORY_LABELS = {
  ELECTRONICS:  'Elektronik',
  FASHION:      'Moda',
  HOMELIVING:   'Ev & Yaşam',
  PERSONALCARE: 'Kişisel Bakım',
  AUTOMOTIVE:   'Otomotiv',
}

const PRICE_PRESETS = [
  { label: '₺0 – ₺50',      min: 0,    max: 50   },
  { label: '₺50 – ₺200',    min: 50,   max: 200  },
  { label: '₺200 – ₺500',   min: 200,  max: 500  },
  { label: '₺500 – ₺1.000', min: 500,  max: 1000 },
  { label: '₺1.000+',       min: 1000, max: null },
  { label: 'Özel',          min: null, max: null, custom: true },
]

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'En Yeni'  },
  { value: 'price,asc',      label: 'Fiyat ↑'  },
  { value: 'price,desc',     label: 'Fiyat ↓'  },
  { value: 'name,asc',       label: 'A → Z'    },
  { value: 'name,desc',      label: 'Z → A'    },
]

export default function ProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState('createdAt,desc')

  const [pricePreset, setPricePreset] = useState(null)
  const [customMin, setCustomMin] = useState('')
  const [customMax, setCustomMax] = useState('')

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  // mobile: only price filter panel toggles
  const [priceOpen, setPriceOpen] = useState(false)

  const isSearchActive = search.trim().length > 0
  const activePreset = pricePreset !== null ? PRICE_PRESETS[pricePreset] : null
  const isCustom = activePreset?.custom === true
  const resolvedMin = isCustom ? customMin : (activePreset?.min ?? '')
  const resolvedMax = isCustom ? customMax : (activePreset?.max ?? '')

  const fetchProducts = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      let res
      if (isSearchActive) {
        res = await productService.search(search, page)
      } else if (category || resolvedMin !== '' || resolvedMax !== '') {
        res = await productService.filter(
          {
            ...(category && { category }),
            ...(resolvedMin !== '' && resolvedMin !== null && { minPrice: resolvedMin }),
            ...(resolvedMax !== '' && resolvedMax !== null && { maxPrice: resolvedMax }),
            sort,
          },
          page
        )
      } else {
        res = await productService.getAll(page, 12, sort)
      }
      setProducts(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
    } catch {
      setError('Ürünler yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }, [search, category, resolvedMin, resolvedMax, sort, page])

  useEffect(() => { fetchProducts() }, [fetchProducts])

  const handleSearch = (e) => {
    e.preventDefault()
    setCategory('')
    setPricePreset(null)
    setCustomMin('')
    setCustomMax('')
    setPage(0)
    setSearch(searchInput.trim())
  }

  const handleCategoryClick = (cat) => {
    setSearch('')
    setSearchInput('')
    setPage(0)
    setCategory((prev) => (prev === cat ? '' : cat))
  }

  const handlePresetClick = (i) => {
    setSearch('')
    setSearchInput('')
    setPage(0)
    setPricePreset((prev) => (prev === i ? null : i))
    setCustomMin('')
    setCustomMax('')
  }

  const handleCustomApply = (e) => {
    e.preventDefault()
    setSearch('')
    setSearchInput('')
    setPage(0)
  }

  const handleReset = () => {
    setSearch('')
    setSearchInput('')
    setCategory('')
    setPricePreset(null)
    setCustomMin('')
    setCustomMax('')
    setSort('createdAt,desc')
    setPage(0)
    setPriceOpen(false)
  }

  const hasActiveFilters = search || category || pricePreset !== null

  return (
    <Layout>
      {/* Search bar */}
      <div className="flex gap-2 mb-4">
        <form onSubmit={handleSearch} className="flex gap-2 flex-1">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Ürün ara..."
            className="flex-1 border border-gray-300 rounded-lg px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          />
          <button
            type="submit"
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700 transition-colors"
          >
            Ara
          </button>
        </form>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={handleReset}
            className="border border-gray-300 text-gray-600 px-3 py-2 rounded-lg text-sm hover:bg-gray-50 transition-colors whitespace-nowrap"
          >
            Temizle
          </button>
        )}
      </div>

      {/* Mobile: category chips — always visible */}
      <div className="md:hidden flex gap-2 overflow-x-auto pb-2 mb-3 -mx-4 px-4 scrollbar-hide">
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            onClick={() => handleCategoryClick(cat)}
            disabled={isSearchActive}
            className={`shrink-0 text-sm px-3 py-1.5 rounded-full border transition-colors disabled:opacity-40 disabled:cursor-not-allowed ${
              category === cat
                ? 'bg-indigo-600 text-white border-indigo-600'
                : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'
            }`}
          >
            {CATEGORY_LABELS[cat]}
          </button>
        ))}
      </div>

      {/* Mobile: sort + price filter toggle row */}
      <div className="md:hidden flex items-center gap-2 mb-4">
        <select
          value={sort}
          onChange={(e) => { setSort(e.target.value); setPage(0) }}
          className="flex-1 border border-gray-200 rounded-lg px-2 py-1.5 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500"
        >
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
        <button
          type="button"
          onClick={() => setPriceOpen((o) => !o)}
          className={`flex items-center gap-1.5 border px-3 py-1.5 rounded-lg text-sm transition-colors ${
            pricePreset !== null
              ? 'border-indigo-600 text-indigo-600 bg-indigo-50'
              : 'border-gray-200 text-gray-600 hover:bg-gray-50'
          }`}
        >
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Fiyat
        </button>
      </div>

      {/* Mobile: collapsible price panel */}
      {priceOpen && (
        <div className="md:hidden bg-white border border-gray-200 rounded-xl p-4 mb-4 space-y-2">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Fiyat Aralığı</p>
          <div className="grid grid-cols-2 gap-2">
            {PRICE_PRESETS.map((preset, i) => (
              <button
                key={i}
                onClick={() => { handlePresetClick(i); if (!preset.custom) setPriceOpen(false) }}
                disabled={isSearchActive}
                className={`text-sm px-3 py-2 rounded-lg border transition-colors disabled:opacity-40 ${
                  pricePreset === i
                    ? 'bg-indigo-600 text-white border-indigo-600'
                    : 'text-gray-600 border-gray-200 hover:bg-gray-50'
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>
          {isCustom && (
            <form onSubmit={(e) => { handleCustomApply(e); setPriceOpen(false) }} className="flex gap-2 pt-1">
              <input
                type="number" min="0" placeholder="Min ₺" value={customMin}
                onChange={(e) => setCustomMin(e.target.value)}
                className="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <input
                type="number" min="0" placeholder="Max ₺" value={customMax}
                onChange={(e) => setCustomMax(e.target.value)}
                className="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <button type="submit" className="bg-indigo-600 text-white px-3 py-1.5 rounded-lg text-sm hover:bg-indigo-700">
                Uygula
              </button>
            </form>
          )}
        </div>
      )}

      <div className="flex gap-6">
        {/* Desktop sidebar */}
        <aside className="hidden md:block w-48 shrink-0 space-y-6">
          {isSearchActive && (
            <p className="text-xs text-gray-400 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2">
              Arama aktifken filtreler devre dışı.
            </p>
          )}

          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Kategori</p>
            <div className="space-y-1">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat}
                  onClick={() => handleCategoryClick(cat)}
                  disabled={isSearchActive}
                  className={`w-full text-left text-sm px-3 py-1.5 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed ${
                    category === cat ? 'bg-indigo-600 text-white' : 'text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {CATEGORY_LABELS[cat]}
                </button>
              ))}
            </div>
          </div>

          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Fiyat (₺)</p>
            <div className="space-y-1">
              {PRICE_PRESETS.map((preset, i) => (
                <button
                  key={i}
                  onClick={() => handlePresetClick(i)}
                  disabled={isSearchActive}
                  className={`w-full text-left text-sm px-3 py-1.5 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed ${
                    pricePreset === i ? 'bg-indigo-600 text-white' : 'text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>
            {isCustom && (
              <form onSubmit={handleCustomApply} className="mt-3 space-y-2">
                <input
                  type="number" min="0" placeholder="Min ₺" value={customMin}
                  onChange={(e) => setCustomMin(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <input
                  type="number" min="0" placeholder="Max ₺" value={customMax}
                  onChange={(e) => setCustomMax(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <button
                  type="submit"
                  className="w-full bg-gray-100 text-gray-700 text-sm py-1.5 rounded-lg hover:bg-gray-200 transition-colors"
                >
                  Uygula
                </button>
              </form>
            )}
          </div>
        </aside>

        {/* Product grid */}
        <div className="flex-1 min-w-0">
          {/* Desktop sort row */}
          <div className="hidden md:flex items-center justify-end mb-4 gap-2">
            <label className="text-xs text-gray-500">Sırala:</label>
            <select
              value={sort}
              onChange={(e) => { setSort(e.target.value); setPage(0) }}
              className="border border-gray-200 rounded-lg px-2 py-1.5 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="bg-white border border-gray-200 rounded-xl overflow-hidden animate-pulse">
                  <div className="aspect-square bg-gray-100" />
                  <div className="p-4 space-y-2">
                    <div className="h-4 bg-gray-100 rounded w-3/4" />
                    <div className="h-3 bg-gray-100 rounded w-1/2" />
                    <div className="h-4 bg-gray-100 rounded w-1/3 mt-2" />
                  </div>
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="text-center py-16 text-gray-400">{error}</div>
          ) : products.length === 0 ? (
            <div className="text-center py-16 text-gray-400">Ürün bulunamadı.</div>
          ) : (
            <>
              <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-8">
                  <button
                    onClick={() => setPage((p) => p - 1)}
                    disabled={page === 0}
                    className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    ← Önceki
                  </button>
                  <span className="text-sm text-gray-500">{page + 1} / {totalPages}</span>
                  <button
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1}
                    className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    Sonraki →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </Layout>
  )
}
