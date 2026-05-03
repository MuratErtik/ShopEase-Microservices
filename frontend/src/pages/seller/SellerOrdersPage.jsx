import { useEffect, useState } from 'react'
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

export default function SellerOrdersPage() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    orderService.getSellerOrders(page)
      .then((res) => {
        setOrders(res.content ?? [])
        setTotalPages(res.totalPages ?? 0)
      })
      .catch(() => setError('Siparişler yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [page])

  return (
    <Layout>
      <h1 className="text-xl font-semibold text-gray-900 mb-6">Gelen Siparişler</h1>

      {error && (
        <p className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-4 py-2 mb-4">
          {error}
        </p>
      )}

      {loading ? (
        <div className="space-y-3 animate-pulse">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="bg-white border border-gray-200 rounded-xl p-4 h-20" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-24 text-gray-400">
          Henüz sipariş gelmedi.
        </div>
      ) : (
        <>
          <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
            <div className="grid grid-cols-12 px-5 py-2.5 text-xs font-semibold text-gray-400 uppercase tracking-wider border-b border-gray-100 bg-gray-50">
              <div className="col-span-3">Sipariş</div>
              <div className="col-span-3">Ürünler</div>
              <div className="col-span-2">Tarih</div>
              <div className="col-span-2 text-right">Tutar</div>
              <div className="col-span-2 text-right">Durum</div>
            </div>

            <div className="divide-y divide-gray-100">
              {orders.map((order) => (
                <div key={order.id} className="grid grid-cols-12 items-start px-5 py-4 gap-2">
                  <div className="col-span-3">
                    <p className="text-sm font-medium text-gray-900 font-mono">
                      #{order.id.slice(0, 8).toUpperCase()}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {order.items?.length ?? 0} kalem
                    </p>
                  </div>

                  <div className="col-span-3">
                    {(order.items ?? []).slice(0, 2).map((item) => (
                      <p key={item.id} className="text-xs text-gray-600 truncate">
                        {item.quantity}× {item.productName}
                      </p>
                    ))}
                    {(order.items?.length ?? 0) > 2 && (
                      <p className="text-xs text-gray-400">+{order.items.length - 2} daha</p>
                    )}
                  </div>

                  <div className="col-span-2">
                    <p className="text-xs text-gray-500">
                      {new Date(order.createdAt).toLocaleDateString('tr-TR', {
                        day: 'numeric', month: 'short', year: 'numeric',
                      })}
                    </p>
                  </div>

                  <div className="col-span-2 text-right">
                    <p className="text-sm font-semibold text-gray-900">
                      ₺{Number(order.totalAmount).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                    </p>
                  </div>

                  <div className="col-span-2 text-right">
                    <span className={`text-xs font-medium px-2.5 py-1 rounded-full ${STATUS_COLOR[order.status] ?? 'bg-gray-100 text-gray-600'}`}>
                      {STATUS_LABEL[order.status] ?? order.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
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
