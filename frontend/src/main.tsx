import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'

import { ProjectProvider } from './context/ProjectContext'
import { AuthProvider } from './context/AuthContext'
import LiveToast from './components/LiveToast'
import AppLayout from './components/AppLayout'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import NgoDashboard from './pages/NgoDashboard'
import ContributorDashboard from './pages/ContributorDashboard'
import NgoRegister from './pages/NgoRegister'
import ContributorRegister from './pages/ContributorRegister'
import UserProfile from './pages/UserProfile'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      <ProjectProvider>
        <BrowserRouter>
          <Routes>
            {/* Full-screen pages — no Navbar */}
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* All other pages share the Navbar layout */}
            <Route element={<AppLayout />}>
              <Route path="/ngo" element={<NgoDashboard />} />
              <Route path="/contributor" element={<ContributorDashboard />} />
              <Route path="/register/ngo" element={<NgoRegister />} />
              <Route path="/register/contributor" element={<ContributorRegister />} />
              <Route path="/profile" element={<UserProfile />} />
            </Route>
          </Routes>

          <LiveToast />
        </BrowserRouter>
      </ProjectProvider>
    </AuthProvider>
  </React.StrictMode>
)
