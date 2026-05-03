import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate('/login')
    setMenuOpen(false)
  }

  const close = () => setMenuOpen(false)

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">

        <Link to="/" className="text-xl font-bold text-gray-900 tracking-tight" onClick={close}>
          Vitrin
        </Link>

        {/* Desktop nav */}
        <nav className="hidden md:flex items-center gap-5 text-sm">
          {!user && (
            <>
              <Link to="/login" className="text-gray-500 hover:text-gray-900 transition-colors">
                Giriş Yap
              </Link>
              <Link
                to="/register"
                className="bg-indigo-600 text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors"
              >
                Kayıt Ol
              </Link>
            </>
          )}

          {user?.role === 'USER' && (
            <>
              <Link to="/orders" className="text-gray-500 hover:text-gray-900 transition-colors">
                Siparişlerim
              </Link>
              <Link
                to="/cart"
                className="flex items-center gap-1.5 bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-lg font-medium hover:bg-indigo-100 transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                Sepet
              </Link>
              <button
                onClick={handleLogout}
                className="text-gray-400 hover:text-gray-700 transition-colors"
              >
                Çıkış
              </button>
            </>
          )}

          {user?.role === 'SELLER' && (
            <>
              <Link to="/seller/products" className="text-gray-500 hover:text-gray-900 transition-colors">
                Ürünlerim
              </Link>
              <Link to="/seller/orders" className="text-gray-500 hover:text-gray-900 transition-colors">
                Siparişler
              </Link>
              <Link
                to="/seller"
                className="bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-lg font-medium hover:bg-indigo-100 transition-colors"
              >
                Pano
              </Link>
              <button
                onClick={handleLogout}
                className="text-gray-400 hover:text-gray-700 transition-colors"
              >
                Çıkış
              </button>
            </>
          )}
        </nav>

        {/* Hamburger button - mobile only */}
        <button
          className="md:hidden p-2 -mr-2 text-gray-500 hover:text-gray-700 transition-colors"
          onClick={() => setMenuOpen((o) => !o)}
          aria-label="Menüyü aç"
        >
          {menuOpen ? (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          )}
        </button>
      </div>

      {/* Mobile dropdown */}
      {menuOpen && (
        <div className="md:hidden border-t border-gray-100 bg-white px-4 py-3 space-y-1">
          {!user && (
            <>
              <Link to="/login" onClick={close} className="block py-2 text-sm text-gray-600 hover:text-gray-900">
                Giriş Yap
              </Link>
              <Link
                to="/register"
                onClick={close}
                className="block mt-2 text-center bg-indigo-600 text-white px-3 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors"
              >
                Kayıt Ol
              </Link>
            </>
          )}

          {user?.role === 'USER' && (
            <>
              <Link to="/orders" onClick={close} className="block py-2 text-sm text-gray-600 hover:text-gray-900">
                Siparişlerim
              </Link>
              <Link
                to="/cart"
                onClick={close}
                className="flex items-center gap-2 py-2 text-sm text-indigo-600 font-medium"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                Sepet
              </Link>
              <button
                onClick={handleLogout}
                className="block w-full text-left py-2 text-sm text-gray-400 hover:text-gray-700"
              >
                Çıkış Yap
              </button>
            </>
          )}

          {user?.role === 'SELLER' && (
            <>
              <Link to="/seller/products" onClick={close} className="block py-2 text-sm text-gray-600 hover:text-gray-900">
                Ürünlerim
              </Link>
              <Link to="/seller/orders" onClick={close} className="block py-2 text-sm text-gray-600 hover:text-gray-900">
                Siparişler
              </Link>
              <Link to="/seller" onClick={close} className="block py-2 text-sm text-indigo-600 font-medium">
                Pano
              </Link>
              <button
                onClick={handleLogout}
                className="block w-full text-left py-2 text-sm text-gray-400 hover:text-gray-700"
              >
                Çıkış Yap
              </button>
            </>
          )}
        </div>
      )}
    </header>
  )
}
