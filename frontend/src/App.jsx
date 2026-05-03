import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './components/layout/ProtectedRoute'

import LoginPage from './pages/public/LoginPage'
import RegisterPage from './pages/public/RegisterPage'

import ProductsPage from './pages/buyer/ProductsPage'
import ProductDetailPage from './pages/buyer/ProductDetailPage'
import CartPage from './pages/buyer/CartPage'
import CheckoutPage from './pages/buyer/CheckoutPage'
import OrdersPage from './pages/buyer/OrdersPage'
import OrderDetailPage from './pages/buyer/OrderDetailPage'
import SellerStorefrontPage from './pages/buyer/SellerStorefrontPage'

import SellerDashboardPage from './pages/seller/SellerDashboardPage'
import SellerProductsPage from './pages/seller/SellerProductsPage'
import SellerOrdersPage from './pages/seller/SellerOrdersPage'
import CreateProductPage from './pages/seller/CreateProductPage'
import EditProductPage from './pages/seller/EditProductPage'
import StockManagementPage from './pages/seller/StockManagementPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/" element={<ProductsPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/sellers/:sellerId" element={<SellerStorefrontPage />} />

        {/* User (buyer) */}
        <Route path="/cart" element={
          <ProtectedRoute role="USER"><CartPage /></ProtectedRoute>
        } />
        <Route path="/checkout" element={
          <ProtectedRoute role="USER"><CheckoutPage /></ProtectedRoute>
        } />
        <Route path="/orders" element={
          <ProtectedRoute role="USER"><OrdersPage /></ProtectedRoute>
        } />
        <Route path="/orders/:id" element={
          <ProtectedRoute role="USER"><OrderDetailPage /></ProtectedRoute>
        } />

        {/* Seller */}
        <Route path="/seller" element={
          <ProtectedRoute role="SELLER"><SellerDashboardPage /></ProtectedRoute>
        } />
        <Route path="/seller/products" element={
          <ProtectedRoute role="SELLER"><SellerProductsPage /></ProtectedRoute>
        } />
        <Route path="/seller/orders" element={
          <ProtectedRoute role="SELLER"><SellerOrdersPage /></ProtectedRoute>
        } />
        <Route path="/seller/products/new" element={
          <ProtectedRoute role="SELLER"><CreateProductPage /></ProtectedRoute>
        } />
        <Route path="/seller/products/:id/edit" element={
          <ProtectedRoute role="SELLER"><EditProductPage /></ProtectedRoute>
        } />
        <Route path="/seller/products/:id/stock" element={
          <ProtectedRoute role="SELLER"><StockManagementPage /></ProtectedRoute>
        } />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
