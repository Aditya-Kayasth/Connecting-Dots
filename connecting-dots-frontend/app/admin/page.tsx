"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { apiRequest, getSavedUser } from "@/lib/api-client"

type NGO = { id: string; organizationName: string; domain: string; isVerified: boolean }
type User = { id: string; email: string; role: string; status: string }
type Problem = { id: string; title: string; organizationName: string; status: string }

export default function AdminPage() {
  const router = useRouter()
  const [authorized, setAuthorized] = useState(false)
  const [data, setData] = useState({
    stats: { totalUsers: 0, totalNGOs: 0, totalProblems: 0, totalApplications: 0 },
    ngos: [] as NGO[],
    users: [] as User[],
    problems: [] as Problem[],
  })
  const [busy, setBusy] = useState("")

  useEffect(() => {
    const user = getSavedUser()
    if (!user || (user.role !== "ROLE_ADMIN" && user.role !== "ADMIN" && user.email !== "admin@connectingdots.org")) {
      router.push("/")
      return
    }
    setAuthorized(true)

    Promise.all([
      apiRequest<typeof data.stats>("/api/v1/core/admin/stats"),
      apiRequest<NGO[]>("/api/v1/core/admin/ngos"),
      apiRequest<User[]>("/api/v1/core/admin/users"),
    ])
      .then(([stats, ngos, users]) => setData((d) => ({ ...d, stats, ngos, users })))
      .catch(() => {})
  }, [router])

  if (!authorized) {
    return (
      <main className="admin-shell flex items-center justify-center min-h-screen">
        <p className="muted">Verifying administrator credentials...</p>
      </main>
    )
  }

  const mutate = async (path: string, method: "PUT" | "DELETE", id: string, action: () => void) => {
    setBusy(id)
    try {
      await apiRequest(path, { method })
      action()
    } catch {} finally {
      setBusy("")
    }
  }

  return (
    <main className="admin-shell">
      <header className="topbar">
        <a className="brand" href="/">
          connecting<span>dots</span>
        </a>
        <div className="topbar-right">
          <span className="admin-label">Administrator</span>
          <div className="avatar dark">A</div>
        </div>
      </header>
      <div className="admin-content">
        <div className="admin-intro">
          <div>
            <span className="eyebrow">Operations center</span>
            <h1>Platform overview</h1>
            <p className="muted">Keep the Connecting Dots community trusted, safe, and moving forward.</p>
          </div>
          <button className="ghost-button" onClick={() => location.reload()}>
            Refresh data
          </button>
        </div>

        <div className="stats-grid">
          {[
            ["Total users", data.stats.totalUsers, "Registered users"],
            ["Verified NGOs", data.stats.totalNGOs, "Active organizations"],
            ["Problem statements", data.stats.totalProblems, "Submitted problems"],
            ["Applications", data.stats.totalApplications, "Contributor applications"],
          ].map(([label, value, note]) => (
            <div className="stat-card" key={String(label)}>
              <span className="muted">{label}</span>
              <strong>{Number(value).toLocaleString()}</strong>
              <small>{note}</small>
            </div>
          ))}
        </div>

        <section className="admin-section">
          <div className="table-heading">
            <div>
              <span className="eyebrow">Trust management</span>
              <h2>NGO verification</h2>
            </div>
            <span className="count-pill">{data.ngos.length} organizations</span>
          </div>
          <Table
            headers={["Organization", "Domain", "Status", "Action"]}
            rows={data.ngos.map((n) => (
              <>
                <td>
                  <strong>{n.organizationName}</strong>
                </td>
                <td className="muted">{n.domain}</td>
                <td>
                  <span className={`status ${n.isVerified ? "verified" : "unverified"}`}>
                    {n.isVerified ? "VERIFIED" : "UNVERIFIED"}
                  </span>
                </td>
                <td>
                  <button
                    className="table-button"
                    disabled={busy === n.id}
                    onClick={() =>
                      mutate(`/api/v1/core/admin/ngos/${n.id}/verify`, "PUT", n.id, () =>
                        setData((d) => ({
                          ...d,
                          ngos: d.ngos.map((x) => (x.id === n.id ? { ...x, isVerified: !x.isVerified } : x)),
                        }))
                      )
                    }
                  >
                    {n.isVerified ? "Revoke" : "Verify"}
                  </button>
                </td>
              </>
            ))}
          />
        </section>

        <div className="split-tables">
          <section className="admin-section">
            <div className="table-heading">
              <div>
                <span className="eyebrow">Access control</span>
                <h2>Platform users</h2>
              </div>
            </div>
            <Table
              headers={["Email", "Role", "Status", ""]}
              rows={data.users.map((u) => (
                <>
                  <td>
                    <strong>{u.email}</strong>
                  </td>
                  <td>
                    <span className="role">{u.role}</span>
                  </td>
                  <td>
                    <span className="status active">{u.status}</span>
                  </td>
                  <td>
                    <button
                      className="danger-button"
                      onClick={() =>
                        mutate(`/api/v1/core/admin/users/${u.id}`, "DELETE", u.id, () =>
                          setData((d) => ({ ...d, users: d.users.filter((x) => x.id !== u.id) }))
                        )
                      }
                    >
                      Remove
                    </button>
                  </td>
                </>
              ))}
            />
          </section>

          <section className="admin-section">
            <div className="table-heading">
              <div>
                <span className="eyebrow">Safety review</span>
                <h2>Problem audit</h2>
              </div>
            </div>
            <Table
              headers={["Problem", "Organization", ""]}
              rows={data.problems.map((p) => (
                <>
                  <td>
                    <strong>{p.title}</strong>
                    <small>{p.status}</small>
                  </td>
                  <td className="muted">{p.organizationName}</td>
                  <td>
                    <button
                      className="danger-button"
                      onClick={() =>
                        mutate(`/api/v1/core/admin/problems/${p.id}`, "DELETE", p.id, () =>
                          setData((d) => ({ ...d, problems: d.problems.filter((x) => x.id !== p.id) }))
                        )
                      }
                    >
                      Delete
                    </button>
                  </td>
                </>
              ))}
            />
          </section>
        </div>
      </div>
    </main>
  )
}

function Table({ headers, rows }: { headers: string[]; rows: React.ReactNode[] }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {headers.map((h) => (
              <th key={h}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>{row}</tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
