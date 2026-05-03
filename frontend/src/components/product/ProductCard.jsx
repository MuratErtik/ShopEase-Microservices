import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { cartService } from '../../services/cartService'
import { useState } from 'react'

export default function ProductCard({ product }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)

  const handleAddToCart = async (e) => {
    e.preventDefault()
    if (!user) { navigate('/login'); return }
    setAdding(true)
    try {
      await cartService.addItem(product.id, 1)
      setAdded(true)
      setTimeout(() => setAdded(false), 2000)
    } catch {
      // sessizce geç, detay sayfasında daha iyi hata gösterilir
    } finally {
      setAdding(false)
    }
  }

  return (
    <Link
      to={`/products/${product.id}`}
      className="group bg-white border border-gray-200 rounded-xl overflow-hidden hover:shadow-md hover:border-gray-300 transition-all flex flex-col"
    >
      <div className="aspect-square bg-gray-100 overflow-hidden">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-300">
            <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        )}
      </div>

      <div className="p-4 flex flex-col gap-2 flex-1">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-sm font-medium text-gray-900 line-clamp-2 flex-1">
            {product.name}
          </h3>
          <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full shrink-0">
            {product.category}
          </span>
        </div>

        <p className="text-xs text-gray-400">{product.brand}</p>

        <div className="mt-auto flex items-center justify-between pt-2">
          <span className="font-semibold text-gray-900">
            ₺{Number(product.price).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
          </span>

          {user?.role === 'USER' && (
            <button
              onClick={handleAddToCart}
              disabled={adding || added}
              className="text-xs px-3 py-1.5 rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
            >
              {added ? 'Eklendi ✓' : adding ? '...' : 'Sepete Ekle'}
            </button>
          )}
        </div>
      </div>
    </Link>
  )
}
