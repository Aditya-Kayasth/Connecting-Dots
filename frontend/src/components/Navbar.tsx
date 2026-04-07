import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-50 w-full bg-white/80 backdrop-blur-md border-b border-gray-100 shadow-sm">
      <div className="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">

        {/* Logo */}
        <button onClick={() => navigate('/')} className="flex items-center gap-2 group">
          <span className="w-6 h-6 rounded-full bg-amber-400 group-hover:scale-110 transition-transform" />
          <span className="w-6 h-6 rounded-full bg-indigo-500 -ml-3 group-hover:scale-110 transition-transform" />
          <span className="ml-1 font-bold text-gray-800 tracking-tight text-base">
            Connecting<span className="text-indigo-500">-</span>Dots
          </span>
        </button>

        {/* Nav links */}
        <nav className="flex items-center gap-1">
          <NavLink to="/" end className={({ isActive }) =>
            `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-gray-100 text-gray-900' : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'}`
          }>
            Home
          </NavLink>

          {user ? (
            <>
              <NavLink to={user.role === 'NGO' ? '/ngo' : '/contributor'} className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-amber-50 text-amber-700' : 'text-gray-500 hover:text-amber-700 hover:bg-amber-50'}`
              }>
                Dashboard
              </NavLink>
              <NavLink to="/profile" className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-gray-100 text-gray-900' : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'}`
              }>
                {user.name.split(' ')[0]}
              </NavLink>
              <button
                onClick={handleLogout}
                className="px-3 py-1.5 rounded-lg text-sm font-medium text-red-500 hover:bg-red-50 transition-colors"
              >
                Sign Out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-gray-100 text-gray-900' : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'}`
              }>
                Sign In
              </NavLink>
              <NavLink to="/register" className={({ isActive }) =>
                `px-4 py-1.5 rounded-lg text-sm font-semibold transition-colors ${isActive ? 'bg-indigo-600 text-white' : 'bg-indigo-500 text-white hover:bg-indigo-600'}`
              }>
                Register
              </NavLink>
            </>
          )}
        </nav>

      </div>
    </header>
  )
}
