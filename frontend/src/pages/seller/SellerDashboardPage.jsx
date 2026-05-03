import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { productService } from '../../services/productService'
import { orderService } from '../../services/orderService'
import { useAuth } from '../../context/AuthContext'

export default function SellerDashboardPage() {
  const { user } = useAuth()
  const [recentProducts, setRecentProducts] = useState([])
  const [totalProducts, setTotalProducts] = useState(0)
  const [pendingOrders, setPendingOrders] = useState(0)
  const [totalSales, setTotalSales] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      productService.getMyProducts(0, 5),
      orderService.getSellerOrders(0, 1000),
    ])
      .then(([products, orders]) => {
        setRecentProducts(products.content ?? [])
        setTotalProducts(products.totalElements ?? 0)
        const allOrders = orders.content ?? []
        const pending = allOrders.filter(
          (o) => o.status === 'PENDING' || o.status === 'STOCK_RESERVED'
        ).length
        setPendingOrders(pending)
        const sales = allOrders
          .filter((o) => o.status === 'CONFIRMED')
          .reduce((sum, o) => sum + Number(o.totalAmount), 0)
        setTotalSales(sales)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  return (
    <Layout>
      <div className="mb-8">
        <h1 className="text-xl font-semibold text-gray-900">
          Merhaba, {user?.firstName} 👋
        </h1>
        <p className="text-sm text-gray-400 mt-1">Satıcı panosu</p>
      </div>

      {/* Özet kart */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="bg-white border border-gray-200 rounded-xl p-5">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1">Toplam Ürün</p>
          <p className="text-3xl font-bold text-gray-900">{loading ? '—' : totalProducts}</p>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-5">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1">Toplam Satış</p>
          <p className="text-3xl font-bold text-gray-900">
            {loading ? '—' : `₺${totalSales.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`}
          </p>
        </div>
        <Link to="/seller/orders" className="bg-white border border-gray-200 rounded-xl p-5 hover:border-gray-300 transition-colors block">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1">Bekleyen Sipariş</p>
          <p className="text-3xl font-bold text-gray-900">{loading ? '—' : pendingOrders}</p>
        </Link>
      </div>

      {/* Son ürünler */}
      <div className="bg-white border border-gray-200 rounded-xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h2 className="font-medium text-gray-900">Son Ürünler</h2>
          <Link to="/seller/products" className="text-sm text-gray-400 hover:text-gray-600">
            Tümünü Gör →
          </Link>
        </div>

        {loading ? (
          <div className="divide-y divide-gray-100">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="px-5 py-3 animate-pulse">
                <div className="h-4 bg-gray-100 rounded w-1/2" />
              </div>
            ))}
          </div>
        ) : recentProducts.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-gray-400">
            Henüz ürün eklemediniz.{' '}
            <Link to="/seller/products/new" className="text-gray-900 underline">
              İlk ürününüzü ekleyin
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {recentProducts.map((p) => (
              <div key={p.id} className="flex items-center justify-between px-5 py-3">
                <div>
                  <p className="text-sm font-medium text-gray-900">{p.name}</p>
                  <p className="text-xs text-gray-400">{p.category} · {p.brand}</p>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm font-medium text-gray-900">
                    ₺{Number(p.price).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                  </span>
                  <Link
                    to={`/seller/products/${p.id}/edit`}
                    className="text-xs text-gray-400 hover:text-gray-600 border border-gray-200 px-2.5 py-1 rounded-lg"
                  >
                    Düzenle
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Hızlı aksiyonlar */}
      <div className="mt-6 flex gap-3">
        <Link
          to="/seller/products/new"
          className="bg-indigo-600 text-white text-sm px-4 py-2.5 rounded-lg hover:bg-indigo-700 transition-colors"
        >
          + Yeni Ürün Ekle
        </Link>
        <Link
          to="/seller/products"
          className="border border-gray-300 text-gray-700 text-sm px-4 py-2.5 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Tüm Ürünleri Yönet
        </Link>
        <Link
          to="/seller/orders"
          className="border border-gray-300 text-gray-700 text-sm px-4 py-2.5 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Gelen Siparişler
        </Link>
      </div>
    </Layout>
  )
}
