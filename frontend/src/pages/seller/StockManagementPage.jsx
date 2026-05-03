import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { inventoryService } from '../../services/inventoryService'
import { productService } from '../../services/productService'

export default function StockManagementPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [inventory, setInventory] = useState(null)
  const [quantity, setQuantity] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      productService.getMyProductById(id),
      inventoryService.getByProductId(id),
    ])
      .then(([p, inv]) => {
        setProduct(p)
        setInventory(inv)
        setQuantity(String(inv.availableQuantity ?? 0))
      })
      .catch(() => setError('Stok bilgisi yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleSave = async (e) => {
    e.preventDefault()
    const qty = parseInt(quantity, 10)
    if (isNaN(qty) || qty < 0) { setError('Geçerli bir miktar girin.'); return }
    setSaving(true)
    setError('')
    setSuccess(false)
    try {
      await inventoryService.updateStock(id, qty)
      const fresh = await inventoryService.getByProductId(id)
      setInventory(fresh)
      setQuantity(String(fresh.availableQuantity ?? qty))
      setSuccess(true)
      setTimeout(() => setSuccess(false), 2500)
    } catch (err) {
      setError(err.message || 'Stok güncellenemedi.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Layout>
      <div className="max-w-lg mx-auto">
        <nav className="text-sm text-gray-400 mb-6 flex items-center gap-2">
          <Link to="/seller/products" className="hover:text-gray-600">Ürünlerim</Link>
          <span>/</span>
          <span className="text-gray-600">Stok Yönetimi</span>
        </nav>

        <h1 className="text-xl font-semibold text-gray-900 mb-6">Stok Yönetimi</h1>

        {loading ? (
          <div className="animate-pulse space-y-4">
            <div className="h-24 bg-gray-100 rounded-xl" />
            <div className="h-32 bg-gray-100 rounded-xl" />
          </div>
        ) : error && !inventory ? (
          <p className="text-center py-12 text-gray-400">{error}</p>
        ) : (
          <>
            {/* Ürün başlık */}
            {product && (
              <div className="bg-white border border-gray-200 rounded-xl p-5 mb-4">
                <p className="text-sm font-medium text-gray-900">{product.name}</p>
                <p className="text-xs text-gray-400 mt-0.5">{product.category} · {product.brand}</p>
              </div>
            )}

            {/* Stok detayları */}
            {inventory && (
              <div className="bg-white border border-gray-200 rounded-xl p-5 mb-4">
                <h2 className="font-medium text-gray-900 mb-4">Mevcut Stok</h2>
                <div className="grid grid-cols-3 gap-4 text-center">
                  <div>
                    <p className="text-2xl font-bold text-green-600">{inventory.availableQuantity}</p>
                    <p className="text-xs text-gray-400 mt-1">Mevcut</p>
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-yellow-600">{inventory.reservedQuantity}</p>
                    <p className="text-xs text-gray-400 mt-1">Rezerve</p>
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{inventory.totalQuantity}</p>
                    <p className="text-xs text-gray-400 mt-1">Toplam</p>
                  </div>
                </div>
              </div>
            )}

            {/* Stok güncelleme formu */}
            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <h2 className="font-medium text-gray-900 mb-4">Stok Güncelle</h2>
              <form onSubmit={handleSave} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Yeni Mevcut Miktar
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  />
                </div>

                {error && (
                  <p className="text-xs text-red-500 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                    {error}
                  </p>
                )}

                {success && (
                  <p className="text-xs text-green-600 bg-green-50 border border-green-200 rounded-lg px-3 py-2">
                    Stok başarıyla güncellendi.
                  </p>
                )}

                <button
                  type="submit"
                  disabled={saving}
                  className="w-full bg-indigo-600 text-white py-2.5 rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {saving ? 'Güncelleniyor...' : 'Güncelle'}
                </button>
              </form>
            </div>
          </>
        )}
      </div>
    </Layout>
  )
}
