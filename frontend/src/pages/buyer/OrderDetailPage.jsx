import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { orderService } from '../../services/orderService'
import { paymentService } from '../../services/paymentService'

const STATUS_STEPS = ['PENDING', 'STOCK_RESERVED', 'CONFIRMED']

const STATUS_LABEL = {
  PENDING:        'Beklemede',
  STOCK_RESERVED: 'Stok Ayrıldı',
  CONFIRMED:      'Onaylandı',
  CANCELLED:      'İptal Edildi',
}

const PAYMENT_STATUS_LABEL = {
  PENDING:   'Beklemede',
  COMPLETED: 'Ödendi',
  FAILED:    'Başarısız',
}

const PAYMENT_STATUS_COLOR = {
  PENDING:   'text-yellow-600',
  COMPLETED: 'text-green-600',
  FAILED:    'text-red-500',
}

export default function OrderDetailPage() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [payment, setPayment] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    orderService.getOrderById(id)
      .then((o) => {
        setOrder(o)
        return paymentService.getByOrderId(o.id).catch(() => null)
      })
      .then(setPayment)
      .catch(() => setError('Sipariş yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <Layout>
      <div className="max-w-2xl mx-auto animate-pulse space-y-4">
        <div className="h-8 bg-gray-100 rounded w-1/3" />
        <div className="h-32 bg-gray-100 rounded-xl" />
        <div className="h-48 bg-gray-100 rounded-xl" />
      </div>
    </Layout>
  )

  if (error || !order) return (
    <Layout>
      <div className="text-center py-24 text-gray-400">{error || 'Sipariş bulunamadı.'}</div>
    </Layout>
  )

  const isCancelled = order.status === 'CANCELLED'
  const currentStep = STATUS_STEPS.indexOf(order.status)

  return (
    <Layout>
      <div className="max-w-2xl mx-auto">
        {/* Breadcrumb */}
        <nav className="text-sm text-gray-400 mb-6 flex items-center gap-2">
          <Link to="/orders" className="hover:text-gray-600">Siparişlerim</Link>
          <span>/</span>
          <span className="text-gray-600">#{order.id.slice(0, 8).toUpperCase()}</span>
        </nav>

        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-gray-900">
            Sipariş #{order.id.slice(0, 8).toUpperCase()}
          </h1>
          <span className="text-sm text-gray-400">
            {new Date(order.createdAt).toLocaleDateString('tr-TR', {
              day: 'numeric', month: 'long', year: 'numeric',
            })}
          </span>
        </div>

        {/* Durum zaman çizelgesi */}
        {!isCancelled ? (
          <div className="bg-white border border-gray-200 rounded-xl p-5 mb-4">
            <div className="flex items-center justify-between relative">
              <div className="absolute left-0 right-0 top-4 h-0.5 bg-gray-100 mx-8" />
              <div
                className="absolute left-0 top-4 h-0.5 bg-gray-900 mx-8 transition-all"
                style={{ right: `${100 - (currentStep / (STATUS_STEPS.length - 1)) * 100}%` }}
              />
              {STATUS_STEPS.map((step, i) => {
                const done = i <= currentStep
                return (
                  <div key={step} className="flex flex-col items-center gap-2 relative z-10">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-medium transition-colors ${
                      done ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-400'
                    }`}>
                      {done && i < currentStep ? '✓' : i + 1}
                    </div>
                    <span className={`text-xs ${done ? 'text-gray-900 font-medium' : 'text-gray-400'}`}>
                      {STATUS_LABEL[step]}
                    </span>
                  </div>
                )
              })}
            </div>
          </div>
        ) : (
          <div className="bg-red-50 border border-red-200 rounded-xl px-5 py-4 mb-4">
            <p className="text-sm text-red-500 font-medium">Sipariş iptal edildi.</p>
          </div>
        )}

        {/* Sipariş kalemleri */}
        <div className="bg-white border border-gray-200 rounded-xl p-5 mb-4">
          <h2 className="font-medium text-gray-900 mb-4">Ürünler</h2>
          <div className="space-y-3">
            {(order.items ?? []).map((item, i) => (
              <div key={i} className="flex items-center justify-between text-sm">
                <div>
                  <Link to={`/products/${item.productId}`} className="text-gray-900 hover:underline">
                    {item.productName ?? `Ürün #${String(item.productId).slice(0, 8).toUpperCase()}`}
                  </Link>
                  <span className="text-gray-400 ml-2">× {item.quantity}</span>
                  <p className="text-xs text-gray-400 mt-0.5">
                    ₺{Number(item.unitPrice).toLocaleString('tr-TR', { minimumFractionDigits: 2 })} / adet
                  </p>
                </div>
                <span className="font-medium text-gray-900">
                  ₺{Number(item.totalPrice).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                </span>
              </div>
            ))}
          </div>
          <div className="border-t border-gray-100 mt-4 pt-4 flex items-center justify-between">
            <span className="font-medium text-gray-900">Toplam</span>
            <span className="text-xl font-bold text-gray-900">
              ₺{Number(order.totalAmount).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
            </span>
          </div>
        </div>

        {/* Teslimat adresi */}
        <div className="bg-white border border-gray-200 rounded-xl p-5 mb-4">
          <h2 className="font-medium text-gray-900 mb-2">Teslimat Adresi</h2>
          <p className="text-sm text-gray-600">{order.shippingAddress}</p>
        </div>

        {/* Ödeme bilgisi */}
        {payment && (
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <h2 className="font-medium text-gray-900 mb-3">Ödeme</h2>
            <div className="space-y-1.5 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-400">Durum</span>
                <span className={`font-medium ${PAYMENT_STATUS_COLOR[payment.status] ?? 'text-gray-600'}`}>
                  {PAYMENT_STATUS_LABEL[payment.status] ?? payment.status}
                </span>
              </div>
              {payment.cardLastFour && (
                <div className="flex justify-between">
                  <span className="text-gray-400">Kart</span>
                  <span className="text-gray-900 font-mono">•••• {payment.cardLastFour}</span>
                </div>
              )}
              {payment.failureReason && (
                <div className="flex justify-between">
                  <span className="text-gray-400">Hata</span>
                  <span className="text-red-500">{payment.failureReason}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}
