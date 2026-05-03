import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import ProductForm from '../../components/product/ProductForm'
import { productService } from '../../services/productService'

export default function CreateProductPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState('')

  const handleSubmit = async (data) => {
    setServerError('')
    try {
      const created = await productService.create(data)
      navigate('/seller/products', {
        state: { justCreated: created, justCreatedStock: data.initialQuantity ?? 0 },
      })
    } catch (err) {
      setServerError(err.message || 'Ürün oluşturulamadı.')
    }
  }

  return (
    <Layout>
      <div className="max-w-xl mx-auto">
        <nav className="text-sm text-gray-400 mb-6 flex items-center gap-2">
          <Link to="/seller/products" className="hover:text-gray-600">Ürünlerim</Link>
          <span>/</span>
          <span className="text-gray-600">Yeni Ürün</span>
        </nav>

        <h1 className="text-xl font-semibold text-gray-900 mb-6">Yeni Ürün Ekle</h1>

        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <ProductForm
            defaultValues={{ initialQuantity: 0 }}
            onSubmit={handleSubmit}
            submitLabel="Ürünü Oluştur"
            serverError={serverError}
          />
        </div>
      </div>
    </Layout>
  )
}
