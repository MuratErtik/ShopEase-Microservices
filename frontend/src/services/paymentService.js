import { api } from './api'

export const paymentService = {
  getByOrderId: (orderId) => api.get(`/payments/orders/${orderId}`),
}
