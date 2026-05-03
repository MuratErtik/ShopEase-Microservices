import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import Layout from '../../components/layout/Layout'
import { orderService } from '../../services/orderService'
import { useState } from 'react'

export default function CheckoutPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm()

  const onSubmit = async (data) => {
    setServerError('')
    try {
      const order = await orderService.createOrder({
        shippingAddress: data.shippingAddress,
        cardHolderName: data.cardHolderName,
        cardNumber: data.cardNumber.replace(/\s/g, ''),
        expireMonth: data.expireMonth,
        expireYear: data.expireYear,
        cvc: data.cvc,
      })
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setServerError(err.message || 'Sipariş oluşturulamadı.')
    }
  }

  return (
    <Layout>
      <div className="max-w-lg mx-auto">
        <h1 className="text-xl font-semibold text-gray-900 mb-6">Siparişi Tamamla</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">

          {/* Teslimat adresi */}
          <div className="bg-white border border-gray-200 rounded-xl p-5 space-y-4">
            <h2 className="font-medium text-gray-900">Teslimat Adresi</h2>
            <div>
              <textarea
                rows={3}
                placeholder="Mahalle, Cadde/Sokak No, Daire, İlçe, İl"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
                {...register('shippingAddress', { required: 'Teslimat adresi zorunludur.' })}
              />
              {errors.shippingAddress && (
                <p className="mt-1 text-xs text-red-500">{errors.shippingAddress.message}</p>
              )}
            </div>
          </div>

          {/* Ödeme bilgileri */}
          <div className="bg-white border border-gray-200 rounded-xl p-5 space-y-4">
            <h2 className="font-medium text-gray-900">Ödeme Bilgileri</h2>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Kart Sahibi</label>
              <input
                type="text"
                placeholder="Ad Soyad"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                {...register('cardHolderName', { required: 'Kart sahibi adı zorunludur.' })}
              />
              {errors.cardHolderName && (
                <p className="mt-1 text-xs text-red-500">{errors.cardHolderName.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Kart Numarası</label>
              <input
                type="text"
                placeholder="1234 5678 9012 3456"
                maxLength={19}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent font-mono"
                {...register('cardNumber', {
                  required: 'Kart numarası zorunludur.',
                  pattern: { value: /^\d{15,16}$/, message: '15 veya 16 haneli kart numarası girin.' },
                  setValueAs: (v) => v.replace(/\s/g, ''),
                })}
                onChange={(e) => {
                  const raw = e.target.value.replace(/\D/g, '').slice(0, 16)
                  e.target.value = raw.replace(/(.{4})/g, '$1 ').trim()
                }}
              />
              {errors.cardNumber && (
                <p className="mt-1 text-xs text-red-500">{errors.cardNumber.message}</p>
              )}
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Ay</label>
                <select
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white"
                  {...register('expireMonth', { required: true })}
                >
                  <option value="">Ay</option>
                  {Array.from({ length: 12 }, (_, i) => {
                    const m = String(i + 1).padStart(2, '0')
                    return <option key={m} value={m}>{m}</option>
                  })}
                </select>
                {errors.expireMonth && (
                  <p className="mt-1 text-xs text-red-500">Zorunlu</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Yıl</label>
                <select
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white"
                  {...register('expireYear', { required: true })}
                >
                  <option value="">Yıl</option>
                  {Array.from({ length: 10 }, (_, i) => {
                    const y = String(new Date().getFullYear() + i)
                    return <option key={y} value={y}>{y}</option>
                  })}
                </select>
                {errors.expireYear && (
                  <p className="mt-1 text-xs text-red-500">Zorunlu</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">CVC</label>
                <input
                  type="text"
                  placeholder="123"
                  maxLength={4}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent font-mono"
                  {...register('cvc', {
                    required: 'CVC zorunludur.',
                    pattern: { value: /^\d{3,4}$/, message: '3-4 haneli olmalı.' },
                  })}
                  onChange={(e) => {
                    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 4)
                  }}
                />
                {errors.cvc && (
                  <p className="mt-1 text-xs text-red-500">{errors.cvc.message}</p>
                )}
              </div>
            </div>
          </div>

          {serverError && (
            <p className="text-sm text-red-500 bg-red-50 border border-red-200 rounded-lg px-4 py-2">
              {serverError}
            </p>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-indigo-600 text-white py-3 rounded-xl font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? 'Sipariş veriliyor...' : 'Siparişi Onayla'}
          </button>
        </form>
      </div>
    </Layout>
  )
}
