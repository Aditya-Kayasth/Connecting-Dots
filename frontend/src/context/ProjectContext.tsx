import { createContext, useContext, useState, ReactNode } from 'react'

export type TechCategory =
  | 'SOFTWARE_WEB'
  | 'DATA_SCIENCE_ML'
  | 'IOT_HARDWARE'
  | 'PROCESS_AUTOMATION'

export type ProjectStatus = 'OPEN' | 'SOLVED'

export interface Project {
  id: string
  ngoName: string
  state: string
  structuredProblem: string
  techCategory: TechCategory
  status: ProjectStatus
}

const MOCK_PROJECTS: Project[] = [
  {
    id: 'p-001',
    ngoName: 'Vidarbha Water Initiative',
    state: 'Maharashtra',
    structuredProblem:
      'Build a real-time IoT sensor dashboard to monitor groundwater levels across 40 villages in Vidarbha, enabling early drought alerts for local administrators.',
    techCategory: 'IOT_HARDWARE',
    status: 'OPEN',
  },
  {
    id: 'p-002',
    ngoName: 'Bangalore Tech Literacy',
    state: 'Karnataka',
    structuredProblem:
      'Develop a multilingual (Kannada + English) web platform to deliver free coding tutorials to government school students in Bengaluru North.',
    techCategory: 'SOFTWARE_WEB',
    status: 'OPEN',
  },
  {
    id: 'p-003',
    ngoName: 'Delhi Air Watch',
    state: 'Delhi',
    structuredProblem:
      'Create a machine-learning model that predicts AQI spikes 48 hours in advance using historical pollution and weather data from CPCB sensors.',
    techCategory: 'DATA_SCIENCE_ML',
    status: 'OPEN',
  },
  {
    id: 'p-004',
    ngoName: 'Surat Textile Workers Union',
    state: 'Gujarat',
    structuredProblem:
      'Automate the monthly wage-slip generation and grievance-tracking workflow for 3,000+ textile workers using a low-code process automation tool.',
    techCategory: 'PROCESS_AUTOMATION',
    status: 'OPEN',
  },
  {
    id: 'p-005',
    ngoName: 'Kerala Flood Relief Network',
    state: 'Kerala',
    structuredProblem:
      'Build a volunteer coordination web app with real-time map overlays showing flood-affected zones, resource depots, and available rescue teams.',
    techCategory: 'SOFTWARE_WEB',
    status: 'OPEN',
  },
  {
    id: 'p-006',
    ngoName: 'Rajasthan Solar Cooperative',
    state: 'Rajasthan',
    structuredProblem:
      'Deploy IoT energy meters on 200 solar micro-grids and build a data pipeline to forecast energy surplus for peer-to-peer trading between villages.',
    techCategory: 'IOT_HARDWARE',
    status: 'OPEN',
  },
  {
    id: 'p-007',
    ngoName: 'Chennai Coastal Fishers Aid',
    state: 'Tamil Nadu',
    structuredProblem:
      'Train a computer-vision model to detect illegal trawling activity from satellite imagery and alert the Tamil Nadu Fisheries Department automatically.',
    techCategory: 'DATA_SCIENCE_ML',
    status: 'OPEN',
  },
  {
    id: 'p-008',
    ngoName: 'Odisha Tribal Health Connect',
    state: 'Odisha',
    structuredProblem:
      'Automate patient record transfers between 15 remote ASHA health posts and the district hospital using a secure, offline-first sync process.',
    techCategory: 'PROCESS_AUTOMATION',
    status: 'OPEN',
  },
]

// ── Context shape ──────────────────────────────────────────────────────────────

interface ProjectContextValue {
  projects: Project[]
  markAsSolved: (projectId: string) => void
}

const ProjectContext = createContext<ProjectContextValue | null>(null)

// ── Provider ───────────────────────────────────────────────────────────────────

export function ProjectProvider({ children }: { children: ReactNode }) {
  const [projects, setProjects] = useState<Project[]>(MOCK_PROJECTS)

  const markAsSolved = (projectId: string) => {
    setProjects(prev =>
      prev.map(p => (p.id === projectId ? { ...p, status: 'SOLVED' } : p))
    )
  }

  return (
    <ProjectContext.Provider value={{ projects, markAsSolved }}>
      {children}
    </ProjectContext.Provider>
  )
}

// ── Hook ───────────────────────────────────────────────────────────────────────

export function useProjects(): ProjectContextValue {
  const ctx = useContext(ProjectContext)
  if (!ctx) throw new Error('useProjects must be used inside <ProjectProvider>')
  return ctx
}
