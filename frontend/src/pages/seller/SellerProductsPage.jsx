import { useEffect, useState, useMemo } from 'react'
import { Link, useLocation } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { productService } from '../../services/productService'
import { inventoryService } from '../../services/inventoryService'

export default function SellerProductsPage() {
  const location = useLocation()
  const [products, setProducts] = useState([])
  const [inventories, setInventories] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [deletingId, setDeletingId] = useState(null)
  const [deletedIds, setDeletedIds] = useState(new Set())
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  // Elasticsearch indexlemesi async olduğu için create/edit'ten gelen ürünü
  // optimistic olarak listeye ekle — yenileme gerekmeden görünür olur
  const displayProducts = useMemo(() => {
    const { justCreated, justUpdated } = location.state ?? {}
    let list = [...products]
    if (justUpdated) {
      const idx = list.findIndex((p) => p.id === justUpdated.id)
      if (idx !== -1) list[idx] = justUpdated
    }
    if (justCreated && !list.some((p) => p.id === justCreated.id)) {
      list = [justCreated, ...list]
    }
    return list.filter((p) => !deletedIds.has(p.id))
  }, [products, location.state, deletedIds])

  
  const fetchProducts = async () => {
    setLoading(true)
    try {
      const res = await productService.getMyProducts(page)
      const list = res.content ?? []
      setProducts(list)
      setTotalPages(res.totalPages ?? 0)

      // Stok bilgilerini paralel çek
      const invEntries = await Promise.all(
        list.map((p) =>
          inventoryService.getByProductId(p.id)
            .then((inv) => [p.id, inv])
            .catch(() => [p.id, null])
        )
      )
      setInventories(Object.fromEntries(invEntries))
    } catch {
      setError('Ürünler yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProducts() }, [page])

  useEffect(() => {
    const { justCreated, justCreatedStock } = location.state ?? {}
    if (!justCreated) return

    if (inventories[justCreated.id] === undefined) {
      inventoryService.getByProductId(justCreated.id)
        .then((inv) => setInventories((prev) => ({ ...prev, [justCreated.id]: inv })))
        .catch(() => {
          const qty = justCreatedStock ?? 0
          setInventories((prev) => ({
            ...prev,
            [justCreated.id]: { availableQuantity: qty, reservedQuantity: 0, totalQuantity: qty },
          }))
        })
    }

    const timer = setTimeout(() => { fetchProducts() }, 500)
    return () => clearTimeout(timer)
  }, [location.state])

  const handleDelete = async (id) => {
    if (!confirm('Bu ürünü silmek istediğinizden emin misiniz?')) return
    setDeletingId(id)
    try {
      await productService.delete(id)
      setProducts((prev) => prev.filter((p) => p.id !== id))
      setDeletedIds((prev) => new Set([...prev, id]))
    } catch {
      setError('Ürün silinemedi.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-900">Ürünlerim</h1>
        <Link
          to="/seller/products/new"
          className="bg-indigo-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-indigo-700 transition-colors"
        >
          + Yeni Ürün
        </Link>
      </div>

      {error && (
        <p className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-4 py-2 mb-4">
          {error}
        </p>
      )}

      {loading ? (
        <div className="bg-white border border-gray-200 rounded-xl divide-y divide-gray-100 animate-pulse">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="px-5 py-4 flex items-center gap-4">
              <div className="w-12 h-12 bg-gray-100 rounded-lg shrink-0" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-gray-100 rounded w-1/3" />
                <div className="h-3 bg-gray-100 rounded w-1/4" />
              </div>
            </div>
          ))}
        </div>
      ) : displayProducts.length === 0 ? (
        <div className="text-center py-24 space-y-3">
          <p className="text-gray-400">Henüz ürün eklemediniz.</p>
          <Link to="/seller/products/new" className="text-sm text-gray-900 underline">
            İlk ürününüzü ekleyin
          </Link>
        </div>
      ) : (
        <>
          {/* Desktop table */}
          <div className="hidden md:block bg-white border border-gray-200 rounded-xl overflow-hidden">
            <div className="grid grid-cols-12 px-5 py-2.5 text-xs font-semibold text-gray-400 uppercase tracking-wider border-b border-gray-100 bg-gray-50">
              <div className="col-span-5">Ürün</div>
              <div className="col-span-2 text-right">Fiyat</div>
              <div className="col-span-2 text-right">Stok</div>
              <div className="col-span-3 text-right">İşlemler</div>
            </div>

            <div className="divide-y divide-gray-100">
              {displayProducts.map((p) => {
                const inv = inventories[p.id]
                const stock = inv?.availableQuantity ?? '—'
                const isDeleting = deletingId === p.id

                return (
                  <div
                    key={p.id}
                    className={`grid grid-cols-12 items-center px-5 py-3.5 transition-opacity ${isDeleting ? 'opacity-40' : ''}`}
                  >
                    <div className="col-span-5 flex items-center gap-3 min-w-0">
                      <div className="w-12 h-12 bg-gray-100 rounded-lg shrink-0 overflow-hidden">
                        {p.imageUrl ? (
                          <img src={p.imageUrl} alt={p.name} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-gray-300 text-xs">?</div>
                        )}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">{p.name}</p>
                        <p className="text-xs text-gray-400">{p.category} · {p.brand}</p>
                      </div>
                    </div>
                    <div className="col-span-2 text-right text-sm font-medium text-gray-900">
                      ₺{Number(p.price).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                    </div>
                    <div className="col-span-2 text-right">
                      <span className={`text-sm font-medium ${stock === 0 ? 'text-red-500' : 'text-gray-900'}`}>
                        {stock}
                      </span>
                    </div>
                    <div className="col-span-3 flex items-center justify-end gap-2">
                      <Link
                        to={`/seller/products/${p.id}/stock`}
                        className="text-xs border border-gray-200 text-gray-500 px-2.5 py-1 rounded-lg hover:bg-gray-50 transition-colors"
                      >
                        Stok
                      </Link>
                      <Link
                        to={`/seller/products/${p.id}/edit`}
                        className="text-xs border border-gray-200 text-gray-500 px-2.5 py-1 rounded-lg hover:bg-gray-50 transition-colors"
                      >
                        Düzenle
                      </Link>
                      <button
                        onClick={() => handleDelete(p.id)}
                        disabled={isDeleting}
                        className="text-xs border border-red-200 text-red-400 px-2.5 py-1 rounded-lg hover:bg-red-50 transition-colors disabled:opacity-40"
                      >
                        Sil
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Mobile card list */}
          <div className="md:hidden space-y-3">
            {displayProducts.map((p) => {
              const inv = inventories[p.id]
              const stock = inv?.availableQuantity ?? '—'
              const isDeleting = deletingId === p.id

              return (
                <div
                  key={p.id}
                  className={`bg-white border border-gray-200 rounded-xl p-4 transition-opacity ${isDeleting ? 'opacity-40' : ''}`}
                >
                  <div className="flex items-center gap-3 mb-3">
                    <div className="w-14 h-14 bg-gray-100 rounded-lg shrink-0 overflow-hidden">
                      {p.imageUrl ? (
                        <img src={p.imageUrl} alt={p.name} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-gray-300 text-xs">?</div>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">{p.name}</p>
                      <p className="text-xs text-gray-400">{p.category} · {p.brand}</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between mb-3 text-sm">
                    <span className="text-gray-500">Fiyat</span>
                    <span className="font-semibold text-gray-900">
                      ₺{Number(p.price).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                  <div className="flex items-center justify-between mb-3 text-sm">
                    <span className="text-gray-500">Stok</span>
                    <span className={`font-semibold ${stock === 0 ? 'text-red-500' : 'text-gray-900'}`}>
                      {stock}
                    </span>
                  </div>

                  <div className="flex gap-2 pt-1 border-t border-gray-100">
                    <Link
                      to={`/seller/products/${p.id}/stock`}
                      className="flex-1 text-center text-xs border border-gray-200 text-gray-500 py-1.5 rounded-lg hover:bg-gray-50 transition-colors"
                    >
                      Stok
                    </Link>
                    <Link
                      to={`/seller/products/${p.id}/edit`}
                      className="flex-1 text-center text-xs border border-gray-200 text-gray-500 py-1.5 rounded-lg hover:bg-gray-50 transition-colors"
                    >
                      Düzenle
                    </Link>
                    <button
                      onClick={() => handleDelete(p.id)}
                      disabled={isDeleting}
                      className="flex-1 text-xs border border-red-200 text-red-400 py-1.5 rounded-lg hover:bg-red-50 transition-colors disabled:opacity-40"
                    >
                      Sil
                    </button>
                  </div>
                </div>
              )
            })}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-6">
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
    </Layout>
  )
}
