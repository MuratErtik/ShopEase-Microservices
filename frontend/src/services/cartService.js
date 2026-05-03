import { api } from './api'

export const cartService = {
  getCart: () => api.get('/cart'),

  addItem: (productId, quantity) =>
    api.post('/cart/items', { productId, quantity }),

  updateItem: (productId, quantity) =>
    api.patch(`/cart/items/${productId}`, { quantity }),

  removeItem: (productId) => api.delete(`/cart/items/${productId}`),

  clearCart: () => api.delete('/cart'),
}
