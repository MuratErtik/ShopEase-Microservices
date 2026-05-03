import { api } from './api'

export const orderService = {
  createOrder: (data) => api.post('/orders', data),

  getOrders: (page = 0, size = 10) =>
    api.get(`/orders?page=${page}&size=${size}`),

  getOrderById: (orderId) => api.get(`/orders/${orderId}`),

  getSellerOrders: (page = 0, size = 10) =>
    api.get(`/orders/seller?page=${page}&size=${size}`),
}
