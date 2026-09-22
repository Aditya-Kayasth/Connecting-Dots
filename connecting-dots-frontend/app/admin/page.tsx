"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { apiRequest } from "@/lib/api-client"
import { SkeletonTable } from "@/components/skeleton-card"

type NGO = {
  id: string;
  organizationName: string;
  domain: string;
  isVerified?: boolean;
  verified?: boolean;
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

const DEMO_EMAILS = new Set([
  "admin_demo@connectingdots.org",
  "demo_admin@connectingdots.org",
  "demo.admin@connectingdots.org",
  "ngo_demo@connectingdots.org",
  "ngo_test@connectingdots.org",
  "contributor_demo@connectingdots.org",
  "contributor_test@connectingdots.org",
  "demo.ngo@connectingdots.org",
  "demo.contributor@connectingdots.org",
]);

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
  const [isDemo, setIsDemo] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (typeof window !== "undefined") {
      const token = sessionStorage.getItem("auth_token");
      const role = sessionStorage.getItem("auth_role");
      const email = (sessionStorage.getItem("auth_email") || "").trim().toLowerCase();
      if (!token || role !== "ADMIN") {
        router.push("/");
      } else {
        setSessionEmail(email);
        setIsDemo(DEMO_EMAILS.has(email));
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
    if (isDemo) {
      alert("This shared demo admin account is read-only.\nLog in with real admin credentials (admin@connectingdots.org) to verify NGOs or manage users.");
      return;
    }
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

  const [adminPasswordOpen, setAdminPasswordOpen] = useState(false);
  const [adminOldPass, setAdminOldPass] = useState("");
  const [adminNewPass, setAdminNewPass] = useState("");
  const [adminConfirmPass, setAdminConfirmPass] = useState("");
  const [adminPassMsg, setAdminPassMsg] = useState("");
  const [adminPassErr, setAdminPassErr] = useState("");
  const [adminPassLoading, setAdminPassLoading] = useState(false);

  async function handleAdminPasswordChange(e: React.FormEvent) {
    e.preventDefault();
    setAdminPassMsg("");
    setAdminPassErr("");

    if (isDemo) {
      setAdminPassErr("This shared demo admin account is read-only.");
      return;
    }

    if (!adminOldPass) {
      setAdminPassErr("Current password is required.");
      return;
    }
    if (adminNewPass.length < 6) {
      setAdminPassErr("New password must be at least 6 characters long.");
      return;
    }
    if (adminNewPass !== adminConfirmPass) {
      setAdminPassErr("New password and confirmation do not match.");
      return;
    }

    setAdminPassLoading(true);
    try {
      await apiRequest("/api/v1/core/auth/change-password", {
        method: "POST",
        body: { oldPassword: adminOldPass, newPassword: adminNewPass }
      });
      setAdminPassMsg("✓ Admin password updated successfully!");
      setAdminOldPass("");
      setAdminNewPass("");
      setAdminConfirmPass("");
      setTimeout(() => {
        setAdminPassMsg("");
        setAdminPasswordOpen(false);
      }, 2500);
    } catch (err: any) {
      setAdminPassErr(err.message || "Failed to update admin password.");
    } finally {
      setAdminPassLoading(false);
    }
  }

  return (
    <main className="admin-shell">
      <div className="admin-content">
        <a className="back-link" href="/">← Back to explore</a>

        <div className="admin-intro">
          <div>
            <span className="eyebrow">Operations center</span>
            <h1>Platform overview</h1>
            <p className="muted">Keep the Connecting Dots community trusted, safe, and moving forward.</p>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button className="ghost-button" onClick={() => location.reload()}>Refresh data</button>
            <button className="outline-button" onClick={() => setAdminPasswordOpen(true)}>Change Password</button>
          </div>
        </div>

        {isDemo && (
          <div style={{
            border: '2px solid var(--accent)',
            background: '#fdf8ec',
            padding: '1.25rem 1.5rem',
            marginBottom: '1.75rem',
            borderRadius: '4px',
            display: 'grid',
            gap: '0.35rem'
          }}>
            <strong style={{ fontSize: '1rem', color: '#775b20' }}>👀 Demo Admin Account — Read-Only View</strong>
            <p style={{ margin: 0, color: '#775b20', fontSize: '0.88rem', lineHeight: 1.5 }}>
              You are signed in using the shared Demo Admin account (<code>{sessionEmail}</code>). You can inspect platform metrics and directory lists, but you cannot verify NGOs or remove users.
            </p>
          </div>
        )}

        {loading ? (
          <SkeletonTable rows={2} />
        ) : (
          <div className="stats-grid">
            {[
              ["Total users", data.stats.totalUsers, `${data.users.filter(u => u.role === 'CONTRIBUTOR').length} contributors · ${data.users.filter(u => u.role === 'NGO').length} NGOs`],
              ["Verified NGOs", data.stats.totalNgos, `${data.ngos.filter(n => Boolean(n.isVerified ?? n.verified)).length} verified · ${data.ngos.filter(n => !Boolean(n.isVerified ?? n.verified)).length} pending`],
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
            rows={data.ngos.map(n => {
              const isNgoVerified = Boolean(n.isVerified ?? n.verified);
              return (
                <>
                  <td>
                    <a href={`/profile/ngo/${n.id}`} className="text-link">
                      <strong>{n.organizationName}</strong>
                    </a>
                  </td>
                  <td className="muted">{n.domain}</td>
                  <td>
                    <span className={`status ${isNgoVerified ? 'verified' : 'unverified'}`}>
                      {isNgoVerified ? 'VERIFIED' : 'UNVERIFIED'}
                    </span>
                  </td>
                  <td>
                    <button
                      className="table-button"
                      disabled={busy === n.id}
                      onClick={() => mutate(`/api/v1/core/admin/ngos/${n.id}/verify`, "PUT", n.id, () => setData(d => ({
                        ...d,
                        ngos: d.ngos.map(x => x.id === n.id ? { ...x, isVerified: !isNgoVerified, verified: !isNgoVerified } : x)
                      })))}
                    >
                      {busy === n.id ? 'Processing...' : isNgoVerified ? 'Revoke' : 'Verify'}
                    </button>
                  </td>
                </>
              );
            })}
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

        {adminPasswordOpen && (
          <div className="modal-backdrop" role="dialog" aria-modal="true" onClick={() => setAdminPasswordOpen(false)}>
            <div className="edit-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
              <button className="close-button" onClick={() => setAdminPasswordOpen(false)}>×</button>
              <span className="eyebrow">Admin Security</span>
              <h2>Change Admin Password</h2>
              <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '1.5rem' }}>
                Account Email: <strong>{sessionEmail || "admin@connectingdots.org"}</strong>
              </p>

              <form onSubmit={handleAdminPasswordChange} style={{ display: 'grid', gap: '1rem' }}>
                <label style={{ display: 'grid', gap: '0.5rem', fontSize: '0.8rem', fontWeight: 'bold' }}>
                  Current Password *
                  <input
                    className="profile-input"
                    type="password"
                    value={adminOldPass}
                    onChange={(e) => setAdminOldPass(e.target.value)}
                    required
                  />
                </label>
                <label style={{ display: 'grid', gap: '0.5rem', fontSize: '0.8rem', fontWeight: 'bold' }}>
                  New Password *
                  <input
                    className="profile-input"
                    type="password"
                    value={adminNewPass}
                    onChange={(e) => setAdminNewPass(e.target.value)}
                    required
                  />
                </label>
                <label style={{ display: 'grid', gap: '0.5rem', fontSize: '0.8rem', fontWeight: 'bold' }}>
                  Confirm New Password *
                  <input
                    className="profile-input"
                    type="password"
                    value={adminConfirmPass}
                    onChange={(e) => setAdminConfirmPass(e.target.value)}
                    required
                  />
                </label>

                {adminPassErr && (
                  <p className="form-error" style={{ color: '#ef4444', margin: '0.5rem 0' }} role="alert">
                    {adminPassErr}
                  </p>
                )}

                {adminPassMsg && (
                  <div style={{ padding: '0.75rem 1rem', background: 'rgba(34, 197, 94, 0.15)', border: '1px solid #22c55e', borderRadius: '6px', color: '#22c55e', fontWeight: 500, fontSize: '0.9rem' }}>
                    {adminPassMsg}
                  </div>
                )}

                <button
                  type="submit"
                  className="primary-button"
                  disabled={adminPassLoading}
                  style={{ width: '100%', marginTop: '0.5rem' }}
                >
                  {adminPassLoading ? 'Updating...' : 'Save New Password'}
                </button>
              </form>
            </div>
          </div>
        )}
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
