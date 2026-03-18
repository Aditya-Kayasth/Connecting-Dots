import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'

import { ProjectProvider } from './context/ProjectContext'
import LiveToast from './components/LiveToast'
import AppLayout from './components/AppLayout'
import Landing from './pages/Landing'
import NgoDashboard from './pages/NgoDashboard'
import ContributorDashboard from './pages/ContributorDashboard'
import NgoRegister from './pages/NgoRegister'
import ContributorRegister from './pages/ContributorRegister'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ProjectProvider>
      <BrowserRouter>
        <Routes>
          {/* Full-screen landing — no Navbar */}
          <Route path="/" element={<Landing />} />

          {/* All other pages share the Navbar layout */}
          <Route element={<AppLayout />}>
            <Route path="/ngo" element={<NgoDashboard />} />
            <Route path="/contributor" element={<ContributorDashboard />} />
            <Route path="/register/ngo" element={<NgoRegister />} />
            <Route path="/register/contributor" element={<ContributorRegister />} />
          </Route>
        </Routes>

        {/* Global floating toast — rendered outside route tree so it persists across navigation */}
        <LiveToast />
      </BrowserRouter>
    </ProjectProvider>
  </React.StrictMode>
)
