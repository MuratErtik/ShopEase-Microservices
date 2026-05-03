import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import ProductCard from '../../components/product/ProductCard'
import { productService } from '../../services/productService'
import { inventoryService } from '../../services/inventoryService'
import { cartService } from '../../services/cartService'
import { useAuth } from '../../context/AuthContext'

export default function ProductDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [product, setProduct] = useState(null)
  const [inventory, setInventory] = useState(null)
  const [sellerProducts, setSellerProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [quantity, setQuantity] = useState(1)
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)
  const [cartError, setCartError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    setQuantity(1)
    setAdded(false)

    productService.getById(id)
      .then((p) => {
        setProduct(p)
        // Stok ve diğer ürünleri paralel çek
        return Promise.all([
          inventoryService.getByProductId(p.id).catch(() => null),
          productService.getBySeller(p.sellerId, 0, 5).catch(() => ({ content: [] })),
        ])
      })
      .then(([inv, others]) => {
        setInventory(inv)
        // Mevcut ürünü diğer ürünler listesinden çıkar
        setSellerProducts((others.content ?? []).filter((p) => p.id !== id))
      })
      .catch(() => setError('Ürün yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleAddToCart = async () => {
    if (!user) { navigate('/login'); return }
    setCartError('')
    setAdding(true)
    try {
      await cartService.addItem(product.id, quantity)
      setAdded(true)
      setTimeout(() => setAdded(false), 2500)
    } catch (err) {
      setCartError(err.message || 'Sepete eklenemedi.')
    } finally {
      setAdding(false)
    }
  }

  const available = inventory?.availableQuantity ?? 0
  const inStock = available > 0

  if (loading) return (
    <Layout>
      <div className="animate-pulse space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="aspect-square bg-gray-100 rounded-xl" />
          <div className="space-y-4">
            <div className="h-6 bg-gray-100 rounded w-3/4" />
            <div className="h-4 bg-gray-100 rounded w-1/2" />
            <div className="h-8 bg-gray-100 rounded w-1/3" />
          </div>
        </div>
      </div>
    </Layout>
  )

  if (error || !product) return (
    <Layout>
      <div className="text-center py-24 text-gray-400">{error || 'Ürün bulunamadı.'}</div>
    </Layout>
  )

  return (
    <Layout>
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-400 mb-6 flex items-center gap-2">
        <Link to="/" className="hover:text-gray-600">Ürünler</Link>
        <span>/</span>
        <span className="text-gray-600 truncate">{product.name}</span>
      </nav>

      {/* Üst bölüm — resim + detay */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">

        {/* Resim */}
        <div className="aspect-square bg-gray-100 rounded-xl overflow-hidden">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-300">
              <svg className="w-24 h-24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          )}
        </div>

        {/* Bilgiler */}
        <div className="flex flex-col gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">
                {product.category}
              </span>
              <span className="text-xs text-gray-400">{product.brand}</span>
              {product.color && (
                <span className="text-xs text-gray-400">· {product.color}</span>
              )}
            </div>
            <h1 className="text-2xl font-semibold text-gray-900">{product.name}</h1>
          </div>

          {product.description && (
            <p className="text-sm text-gray-500 leading-relaxed">{product.description}</p>
          )}

          <p className="text-3xl font-bold text-gray-900">
            ₺{Number(product.price).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
          </p>

          {/* Stok durumu */}
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center gap-1.5 text-sm font-medium ${inStock ? 'text-green-600' : 'text-red-500'}`}>
              <span className={`w-2 h-2 rounded-full ${inStock ? 'bg-green-500' : 'bg-red-400'}`} />
              {inStock ? `Stokta (${available} adet)` : 'Stok tükendi'}
            </span>
          </div>

          {/* Sepet aksiyonu */}
          {user?.role === 'USER' && (
            <div className="space-y-3">
              {inStock && (
                <div className="flex items-center gap-3">
                  <label className="text-sm text-gray-600">Adet</label>
                  <div className="flex items-center border border-gray-300 rounded-lg overflow-hidden">
                    <button
                      onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                      className="px-3 py-1.5 text-gray-600 hover:bg-gray-50 transition-colors"
                    >
                      −
                    </button>
                    <span className="px-4 py-1.5 text-sm font-medium border-x border-gray-300 min-w-12 text-center">
                      {quantity}
                    </span>
                    <button
                      onClick={() => setQuantity((q) => Math.min(available, q + 1))}
                      className="px-3 py-1.5 text-gray-600 hover:bg-gray-50 transition-colors"
                    >
                      +
                    </button>
                  </div>
                </div>
              )}

              <button
                onClick={handleAddToCart}
                disabled={!inStock || adding || added}
                className="w-full bg-indigo-600 text-white py-3 rounded-xl font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {added ? 'Sepete Eklendi ✓' : adding ? 'Ekleniyor...' : !inStock ? 'Stok Tükendi' : 'Sepete Ekle'}
              </button>

              {cartError && (
                <p className="text-xs text-red-500">{cartError}</p>
              )}
            </div>
          )}

          {!user && inStock && (
            <Link
              to="/login"
              className="block w-full text-center bg-indigo-600 text-white py-3 rounded-xl font-medium hover:bg-indigo-700 transition-colors"
            >
              Satın almak için giriş yap
            </Link>
          )}
        </div>
      </div>

      {/* Satıcı kartı */}
      <div className="border border-gray-200 rounded-xl p-5 mb-12 bg-white">
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Satıcı</p>
        <div className="flex items-center justify-between">
          <div>
            <p className="font-medium text-gray-900">{product.seller?.fullName}</p>
            <p className="text-sm text-gray-400">{product.seller?.email}</p>
          </div>
          <Link
            to={`/sellers/${product.sellerId}`}
            className="text-sm text-gray-900 border border-gray-300 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Mağazaya Git →
          </Link>
        </div>
      </div>

      {/* Satıcının diğer ürünleri */}
      {sellerProducts.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-900">Bu Satıcının Diğer Ürünleri</h2>
            <Link
              to={`/sellers/${product.sellerId}`}
              className="text-sm text-gray-400 hover:text-gray-600"
            >
              Tümünü gör →
            </Link>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {sellerProducts.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </div>
      )}
    </Layout>
  )
}
