import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { cartService } from '../../services/cartService'

export default function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState(null)

  const fetchCart = async () => {
    setError('')
    try {
      const data = await cartService.getCart()
      setCart(data)
    } catch {
      setError('Sepet yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchCart() }, [])

  const handleQuantityChange = async (productId, newQty) => {
    if (newQty < 1) return
    setUpdatingId(productId)
    try {
      const updated = await cartService.updateItem(productId, newQty)
      setCart(updated)
    } catch {
      setError('Güncelleme başarısız.')
    } finally {
      setUpdatingId(null)
    }
  }

  const handleRemove = async (productId) => {
    setUpdatingId(productId)
    try {
      await cartService.removeItem(productId)
      await fetchCart()
    } catch {
      setError('Ürün silinemedi.')
    } finally {
      setUpdatingId(null)
    }
  }

  const handleClear = async () => {
    try {
      await cartService.clearCart()
      await fetchCart()
    } catch {
      setError('Sepet temizlenemedi.')
    }
  }

  if (loading) return (
    <Layout>
      <div className="animate-pulse space-y-4 max-w-2xl mx-auto">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="bg-white border border-gray-200 rounded-xl p-4 flex gap-4">
            <div className="w-20 h-20 bg-gray-100 rounded-lg shrink-0" />
            <div className="flex-1 space-y-2">
              <div className="h-4 bg-gray-100 rounded w-2/3" />
              <div className="h-3 bg-gray-100 rounded w-1/3" />
            </div>
          </div>
        ))}
      </div>
    </Layout>
  )

  const items = cart?.items ?? []
  const isEmpty = items.length === 0

  return (
    <Layout>
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-gray-900">Sepetim</h1>
          {!isEmpty && (
            <button
              onClick={handleClear}
              className="text-sm text-gray-400 hover:text-red-500 transition-colors"
            >
              Sepeti Temizle
            </button>
          )}
        </div>

        {error && (
          <p className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-4 py-2 mb-4">
            {error}
          </p>
        )}

        {isEmpty ? (
          <div className="text-center py-24 space-y-3">
            <p className="text-gray-400">Sepetiniz boş.</p>
            <Link to="/" className="text-sm text-gray-900 underline hover:no-underline">
              Alışverişe Başla
            </Link>
          </div>
        ) : (
          <>
            <div className="space-y-3 mb-6">
              {items.map((item) => {
                const isUpdating = updatingId === item.productId
                const price = Number(item.price ?? 0)

                return (
                  <div
                    key={item.productId}
                    className={`bg-white border border-gray-200 rounded-xl p-4 flex gap-4 transition-opacity ${isUpdating ? 'opacity-50' : ''}`}
                  >
                    {/* Resim */}
                    <Link to={`/products/${item.productId}`} className="shrink-0">
                      <div className="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-gray-300">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14" />
                            </svg>
                          </div>
                        )}
                      </div>
                    </Link>

                    {/* Bilgi */}
                    <div className="flex-1 min-w-0">
                      <Link to={`/products/${item.productId}`}>
                        <p className="text-sm font-medium text-gray-900 truncate hover:underline">
                          {item.name ?? 'Ürün'}
                        </p>
                      </Link>
                      <p className="text-xs text-gray-400 mt-0.5">
                        ₺{price.toLocaleString('tr-TR', { minimumFractionDigits: 2 })} / adet
                      </p>
                      <p className="text-sm font-semibold text-gray-900 mt-2">
                        ₺{Number(item.subtotal ?? price * item.quantity).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                      </p>
                    </div>

                    {/* Adet + Sil */}
                    <div className="flex flex-col items-end justify-between shrink-0">
                      <button
                        onClick={() => handleRemove(item.productId)}
                        disabled={isUpdating}
                        className="text-gray-300 hover:text-red-400 transition-colors"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                      <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden">
                        <button
                          onClick={() => handleQuantityChange(item.productId, item.quantity - 1)}
                          disabled={isUpdating || item.quantity <= 1}
                          className="px-2.5 py-1 text-gray-500 hover:bg-gray-50 disabled:opacity-40 text-sm"
                        >
                          −
                        </button>
                        <span className="px-3 py-1 text-sm font-medium border-x border-gray-200">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => handleQuantityChange(item.productId, item.quantity + 1)}
                          disabled={isUpdating}
                          className="px-2.5 py-1 text-gray-500 hover:bg-gray-50 disabled:opacity-40 text-sm"
                        >
                          +
                        </button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Özet */}
            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <div className="flex items-center justify-between mb-4">
                <span className="text-gray-600">Toplam</span>
                <span className="text-xl font-bold text-gray-900">
                  ₺{Number(cart.totalPrice ?? 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                </span>
              </div>
              <button
                onClick={() => navigate('/checkout')}
                className="w-full bg-indigo-600 text-white py-3 rounded-xl font-medium hover:bg-indigo-700 transition-colors"
              >
                Siparişi Tamamla
              </button>
            </div>
          </>
        )}
      </div>
    </Layout>
  )
}
