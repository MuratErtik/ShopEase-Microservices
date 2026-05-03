import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import ProductCard from '../../components/product/ProductCard'
import { authService } from '../../services/authService'
import { productService } from '../../services/productService'

export default function SellerStorefrontPage() {
  const { sellerId } = useParams()

  const [seller, setSeller] = useState(null)
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    setError('')

    Promise.all([
      authService.getUserById(sellerId),
      productService.getBySeller(sellerId, page),
    ])
      .then(([sellerData, productsData]) => {
        setSeller(sellerData)
        setProducts(productsData.content ?? [])
        setTotalPages(productsData.totalPages ?? 0)
      })
      .catch(() => setError('Mağaza yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [sellerId, page])

  if (loading) return (
    <Layout>
      <div className="animate-pulse space-y-6">
        <div className="h-32 bg-gray-100 rounded-xl" />
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="bg-white border border-gray-200 rounded-xl overflow-hidden">
              <div className="aspect-square bg-gray-100" />
              <div className="p-4 space-y-2">
                <div className="h-4 bg-gray-100 rounded w-3/4" />
                <div className="h-3 bg-gray-100 rounded w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </Layout>
  )

  if (error) return (
    <Layout>
      <div className="text-center py-24 text-gray-400">{error}</div>
    </Layout>
  )

  return (
    <Layout>
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-400 mb-6 flex items-center gap-2">
        <Link to="/" className="hover:text-gray-600">Ürünler</Link>
        <span>/</span>
        <span className="text-gray-600">{seller?.firstName} {seller?.lastName}</span>
      </nav>

      {/* Satıcı başlık */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 mb-8">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-gray-900 flex items-center justify-center text-white text-xl font-semibold shrink-0">
            {seller?.firstName?.[0]?.toUpperCase()}
          </div>
          <div>
            <h1 className="text-xl font-semibold text-gray-900">
              {seller?.firstName} {seller?.lastName}
            </h1>
            <p className="text-sm text-gray-400">{seller?.email}</p>
            <p className="text-xs text-gray-400 mt-0.5">
              {seller?.createdAt
                ? `${new Date(seller.createdAt).toLocaleDateString('tr-TR', { year: 'numeric', month: 'long' })} tarihinden beri satıcı`
                : ''}
            </p>
          </div>
        </div>
      </div>

      {/* Ürünler */}
      {products.length === 0 ? (
        <div className="text-center py-16 text-gray-400">Bu satıcının henüz ürünü yok.</div>
      ) : (
        <>
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-900">Ürünler</h2>
            <span className="text-sm text-gray-400">{totalPages * 12} ürün</span>
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
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
    </Layout>
  )
}
