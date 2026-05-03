import { api } from './api'

export const inventoryService = {
  getByProductId: (productId) => api.get(`/inventories/${productId}`),

  updateStock: (productId, quantity) =>
    api.patch(`/inventories/${productId}/stock`, { quantity }),
}
