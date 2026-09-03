"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { apiRequest } from "@/lib/api-client"
import { SkeletonTable } from "@/components/skeleton-card"

type NGO = {
  id: string;
  organizationName: string;
  domain: string;
  isVerified: boolean;
  user?: {
    id: string;
  };
};

type User = {
  id: string;
  email: string;
  role: string;
  status: string;
};

type Contributor = {
  id: string;
  firstName: string;
  lastName: string;
  user?: {
    id: string;
  };
};

type Problem = {
  id: string;
  title: string;
  status: string;
  ngoProfile?: {
    id: string;
    organizationName: string;
  };
};

type Stats = {
  totalUsers: number;
  totalNgos: number;
  totalProblems: number;
  totalApplications: number;
};

const demo = {
  stats: {
    totalUsers: 0,
    totalNgos: 0,
    totalProblems: 0,
    totalApplications: 0
  } as Stats,
  ngos: [] as NGO[],
  users: [] as User[],
  problems: [] as Problem[],
  contributors: [] as Contributor[]
};

export default function AdminPage() {
  const [data, setData] = useState(demo);
  const [busy, setBusy] = useState("");
  const [loading, setLoading] = useState(true);
  const [sessionEmail, setSessionEmail] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    if (typeof window !== "undefined") {
      const token = sessionStorage.getItem("auth_token");
      const role = sessionStorage.getItem("auth_role");
      const email = sessionStorage.getItem("auth_email");
      if (!token || role !== "ADMIN") {
        router.push("/");
      } else {
        setSessionEmail(email);
      }
    }
  }, [router]);

  useEffect(() => {
    Promise.all([
      apiRequest<Stats>("/api/v1/core/admin/stats"),
      apiRequest<NGO[]>("/api/v1/core/admin/ngos"),
      apiRequest<User[]>("/api/v1/core/admin/users"),
      apiRequest<Problem[]>("/api/v1/core/admin/problems"),
      apiRequest<Contributor[]>("/api/v1/core/profiles/contributors")
    ]).then(([stats, ngos, users, problems, contributors]) => {
      setData({
        stats,
        ngos: Array.isArray(ngos) ? ngos : [],
        users: Array.isArray(users) ? users : [],
        problems: Array.isArray(problems) ? problems : [],
        contributors: Array.isArray(contributors) ? contributors : []
      });
    }).then(() => setLoading(false))
      .catch(() => setLoading(false));
  }, []);

  const mutate = async (path: string, method: "PUT" | "DELETE", id: string, action: () => void) => {
    setBusy(id);
    try {
      await apiRequest(path, { method });
      action();
    } catch (err: any) {
      alert(err.message || 'Operation failed. Please try again.');
    } finally {
      setBusy("");
    }
  };

  const getProfileLink = (u: User) => {
    if (u.role === 'NGO') {
      const ngo = data.ngos.find(n => n.user?.id === u.id);
      return ngo ? `/profile/ngo/${ngo.id}` : null;
    }
    if (u.role === 'CONTRIBUTOR') {
      const contr = data.contributors.find(c => c.user?.id === u.id);
      return contr ? `/profile/contributor/${contr.id}` : null;
    }
    return null;
  };

  return (
    <main className="admin-shell">
      <div className="admin-content">
        <div className="admin-intro">
          <div>
            <span className="eyebrow">Operations center</span>
            <h1>Platform overview</h1>
            <p className="muted">Keep the Connecting Dots community trusted, safe, and moving forward.</p>
          </div>
          <button className="ghost-button" onClick={() => location.reload()}>Refresh data</button>
        </div>

        {loading ? (
          <SkeletonTable rows={2} />
        ) : (
          <div className="stats-grid">
            {[
              ["Total users", data.stats.totalUsers, `${data.users.filter(u => u.role === 'CONTRIBUTOR').length} contributors · ${data.users.filter(u => u.role === 'NGO').length} NGOs`],
              ["Verified NGOs", data.stats.totalNgos, `${data.ngos.filter(n => n.isVerified).length} verified · ${data.ngos.filter(n => !n.isVerified).length} pending`],
              ["Problem statements", data.stats.totalProblems, `${data.problems.filter(p => p.status === 'OPEN').length} public · ${data.problems.filter(p => p.status !== 'OPEN').length} drafts`],
              ["Applications", data.stats.totalApplications, `${data.stats.totalApplications} volunteer matches`]
            ].map(([label, value, note]) => (
              <div className="stat-card" key={String(label)}>
                <span className="muted">{label}</span>
                <strong>{Number(value).toLocaleString()}</strong>
                <small>{note}</small>
              </div>
            ))}
          </div>
        )}

        <section className="admin-section">
          <div className="table-heading">
            <div>
              <span className="eyebrow">Trust management</span>
              <h2>NGO verification</h2>
            </div>
            <span className="count-pill">{data.ngos.length} organizations</span>
          </div>
          <Table
            headers={['Organization', 'Domain', 'Status', 'Action']}
            rows={data.ngos.map(n => (
              <>
                <td>
                  <a href={`/profile/ngo/${n.id}`} className="text-link">
                    <strong>{n.organizationName}</strong>
                  </a>
                </td>
                <td className="muted">{n.domain}</td>
                <td>
                  <span className={`status ${n.isVerified ? 'verified' : 'unverified'}`}>
                    {n.isVerified ? 'VERIFIED' : 'UNVERIFIED'}
                  </span>
                </td>
                <td>
                  <button
                    className="table-button"
                    disabled={busy === n.id}
                    onClick={() => mutate(`/api/v1/core/admin/ngos/${n.id}/verify`, "PUT", n.id, () => setData(d => ({
                      ...d,
                      ngos: d.ngos.map(x => x.id === n.id ? { ...x, isVerified: !x.isVerified } : x)
                    })))}
                  >
                    {busy === n.id ? 'Processing...' : n.isVerified ? 'Revoke' : 'Verify'}
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
              headers={['Email', 'Role', 'Status', '']}
              rows={data.users.map(u => (
                <>
                  <td>
                    {getProfileLink(u) ? (
                      <a href={getProfileLink(u)!} className="text-link">
                        <strong>{u.email}</strong>
                      </a>
                    ) : (
                      <strong>{u.email}</strong>
                    )}
                  </td>
                  <td><span className="role">{u.role}</span></td>
                  <td><span className="status active">{u.status}</span></td>
                  <td>
                    {u.email !== sessionEmail && (
                      <button
                        className="danger-button"
                        disabled={busy === u.id}
                        onClick={() => mutate(`/api/v1/core/admin/users/${u.id}`, "DELETE", u.id, () => setData(d => ({
                          ...d,
                          users: d.users.filter(x => x.id !== u.id)
                        })))}
                      >
                        {busy === u.id ? 'Removing...' : 'Remove'}
                      </button>
                    )}
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
              headers={['Problem', 'Organization', '']}
              rows={data.problems.map(p => (
                <>
                  <td>
                    <strong>{p.title}</strong>
                    <small>{p.status}</small>
                  </td>
                  <td className="muted">
                    {p.ngoProfile?.id ? (
                      <a href={`/profile/ngo/${p.ngoProfile.id}`} className="text-link">
                        {p.ngoProfile.organizationName}
                      </a>
                    ) : (
                      p.ngoProfile?.organizationName || "System"
                    )}
                  </td>
                  <td>
                    <button
                      className="danger-button"
                      disabled={busy === p.id}
                      onClick={() => mutate(`/api/v1/core/admin/problems/${p.id}`, "DELETE", p.id, () => setData(d => ({
                        ...d,
                        problems: d.problems.filter(x => x.id !== p.id)
                      })))}
                    >
                      {busy === p.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </td>
                </>
              ))}
            />
          </section>
        </div>
      </div>
    </main>
  );
}

function Table({ headers, rows }: { headers: string[]; rows: React.ReactNode[] }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {headers.map(h => <th key={h}>{h}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => <tr key={i}>{row}</tr>)}
        </tbody>
      </table>
    </div>
  );
}
