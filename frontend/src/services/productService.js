import { api } from './api'

export const productService = {
  getAll: (page = 0, size = 12, sort = 'createdAt') =>
    api.get(`/products?page=${page}&size=${size}&sort=${sort}`),

  getById: (id) => api.get(`/products/${id}`),

  search: (name, page = 0, size = 12) =>
    api.get(`/products/search?name=${encodeURIComponent(name)}&page=${page}&size=${size}`),

  getByCategory: (category, page = 0, size = 12) =>
    api.get(`/products/category/${category}?page=${page}&size=${size}`),

  getByBrand: (brand, page = 0, size = 12) =>
    api.get(`/products/brand?brand=${encodeURIComponent(brand)}&page=${page}&size=${size}`),

  getByPriceRange: (minPrice, maxPrice, page = 0, size = 12) =>
    api.get(`/products/price-range?minPrice=${minPrice}&maxPrice=${maxPrice}&page=${page}&size=${size}`),

  filter: (params = {}, page = 0, size = 12) => {
    const query = new URLSearchParams({ page, size })
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== '' && v !== null) query.append(k, v)
    })
    return api.get(`/products/filter?${query}`)
  },

  getBySeller: (sellerId, page = 0, size = 12) =>
    api.get(`/products/sellers/${sellerId}?page=${page}&size=${size}`),

  getMyProducts: (page = 0, size = 12) =>
    api.get(`/products/my?page=${page}&size=${size}`),

  getMyProductById: (id) => api.get(`/products/my/${id}`),

  create: (data) => api.post('/products', data),

  update: (id, data) => api.patch(`/products/${id}`, data),

  delete: (id) => api.delete(`/products/${id}`),
}
