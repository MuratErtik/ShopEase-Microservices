import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { orderService } from '../../services/orderService'

const STATUS_LABEL = {
  PENDING:        'Beklemede',
  STOCK_RESERVED: 'Stok Ayrıldı',
  CONFIRMED:      'Onaylandı',
  CANCELLED:      'İptal Edildi',
}

const STATUS_COLOR = {
  PENDING:        'bg-yellow-100 text-yellow-700',
  STOCK_RESERVED: 'bg-blue-100 text-blue-700',
  CONFIRMED:      'bg-green-100 text-green-700',
  CANCELLED:      'bg-red-100 text-red-500',
}

export default function OrdersPage() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    orderService.getOrders(page)
      .then((res) => {
        setOrders(res.content ?? [])
        setTotalPages(res.totalPages ?? 0)
      })
      .catch(() => setError('Siparişler yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [page])

  if (loading) return (
    <Layout>
      <div className="max-w-2xl mx-auto animate-pulse space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="bg-white border border-gray-200 rounded-xl p-4 h-20" />
        ))}
      </div>
    </Layout>
  )

  return (
    <Layout>
      <div className="max-w-2xl mx-auto">
        <h1 className="text-xl font-semibold text-gray-900 mb-6">Siparişlerim</h1>

        {error && (
          <p className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-4 py-2 mb-4">
            {error}
          </p>
        )}

        {orders.length === 0 ? (
          <div className="text-center py-24 space-y-3">
            <p className="text-gray-400">Henüz siparişiniz yok.</p>
            <Link to="/" className="text-sm text-gray-900 underline hover:no-underline">
              Alışverişe Başla
            </Link>
          </div>
        ) : (
          <>
            <div className="space-y-3">
              {orders.map((order) => (
                <Link
                  key={order.id}
                  to={`/orders/${order.id}`}
                  className="block bg-white border border-gray-200 rounded-xl p-4 hover:border-gray-300 hover:shadow-sm transition-all"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        Sipariş #{order.id.slice(0, 8).toUpperCase()}
                      </p>
                      <p className="text-xs text-gray-400 mt-0.5">
                        {new Date(order.createdAt).toLocaleDateString('tr-TR', {
                          day: 'numeric', month: 'long', year: 'numeric',
                        })}
                        {' · '}
                        {order.items?.length ?? 0} ürün
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className={`text-xs font-medium px-2.5 py-1 rounded-full ${STATUS_COLOR[order.status] ?? 'bg-gray-100 text-gray-600'}`}>
                        {STATUS_LABEL[order.status] ?? order.status}
                      </span>
                      <span className="font-semibold text-gray-900 text-sm">
                        ₺{Number(order.totalAmount).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                      </span>
                    </div>
                  </div>
                </Link>
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
    </Layout>
  )
}
