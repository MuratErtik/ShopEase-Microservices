import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import ProductForm from '../../components/product/ProductForm'
import { productService } from '../../services/productService'

export default function EditProductPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [defaultValues, setDefaultValues] = useState(null)
  const [loading, setLoading] = useState(true)
  const [serverError, setServerError] = useState('')

  useEffect(() => {
    productService.getMyProductById(id)
      .then((p) => setDefaultValues({
        name: p.name,
        description: p.description ?? '',
        price: p.price,
        category: p.category,
        brand: p.brand,
        color: p.color,
        imageUrl: p.imageUrl ?? '',
      }))
      .catch(() => setServerError('Ürün yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleSubmit = async (data) => {
    setServerError('')
    // Boş string alanları gönderme
    const payload = Object.fromEntries(
      Object.entries(data).filter(([, v]) => v !== '' && v !== null && v !== undefined)
    )
    try {
      const updated = await productService.update(id, payload)
      navigate('/seller/products', { state: { justUpdated: updated } })
    } catch (err) {
      setServerError(err.message || 'Ürün güncellenemedi.')
    }
  }

  return (
    <Layout>
      <div className="max-w-xl mx-auto">
        <nav className="text-sm text-gray-400 mb-6 flex items-center gap-2">
          <Link to="/seller/products" className="hover:text-gray-600">Ürünlerim</Link>
          <span>/</span>
          <span className="text-gray-600">Düzenle</span>
        </nav>

        <h1 className="text-xl font-semibold text-gray-900 mb-6">Ürünü Düzenle</h1>

        <div className="bg-white border border-gray-200 rounded-xl p-6">
          {loading ? (
            <div className="animate-pulse space-y-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="h-10 bg-gray-100 rounded-lg" />
              ))}
            </div>
          ) : (
            <ProductForm
              defaultValues={defaultValues}
              onSubmit={handleSubmit}
              submitLabel="Değişiklikleri Kaydet"
              serverError={serverError}
            />
          )}
        </div>
      </div>
    </Layout>
  )
}
