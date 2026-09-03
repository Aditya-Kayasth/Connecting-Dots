'use client'

import { useMemo, useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { apiRequest, getAuthRole } from '@/lib/api-client'
import ReviewsList from '@/components/reviews-list'

function getInitials(name: string) {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (!words.length) return 'CD';
  if (words.length === 1) return `${words[0][0]}${words[0][0]}`.toUpperCase();
  return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase();
}

export default function ProfilePage() {
  const [tab, setTab] = useState('Profile Details');
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [role, setRole] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);
  const router = useRouter();

  // Form states
  const [name, setName] = useState('');
  const [domain, setDomain] = useState(''); // Used as Focus Domain for NGO
  const [title, setTitle] = useState(''); // Used as Job Title for Contributor
  const [location, setLocation] = useState(''); // Used as Location
  const [contact, setContact] = useState('');
  const [skills, setSkills] = useState('');
  const [portfolio, setPortfolio] = useState('');
  const [lang, setLang] = useState('en');
  const [saved, setSaved] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Reputation & review states
  const [reviews, setReviews] = useState<any[]>([]);
  const [avgRating, setAvgRating] = useState<number>(0);

  const initials = useMemo(() => getInitials(name || 'User'), [name]);

  // Tab list without redundant "Account Settings" tab
  const tabs = role === 'NGO'
    ? ['Profile Details']
    : ['Profile Details', 'Reviews & Activity'];

  useEffect(() => {
    setMounted(true);
    setRole(getAuthRole());
  }, []);

  useEffect(() => {
    if (!mounted || !role) return;

    const token = sessionStorage.getItem('auth_token');
    const authRole = sessionStorage.getItem('auth_role');
    if (!token || authRole === 'ADMIN') {
      router.push('/');
      return;
    }

    const fetchProfile = async () => {
      try {
        const endpoint = role === 'NGO'
          ? '/api/v1/core/profiles/ngo/me'
          : '/api/v1/core/profiles/contributor/me';
        const data = await apiRequest<any>(endpoint);
        if (data) {
          setProfile(data);
          if (role === 'NGO') {
            setName(data.organizationName || '');
            setDomain(data.domain || '');
            setContact(data.contactNumber || '');
            setLang(data.preferredLanguage || 'en');
            setLocation(data.location || 'Global Community');
          } else {
            setName((data.firstName || '') + ' ' + (data.lastName || ''));
            setTitle(data.title || 'Technical Contributor');
            setLocation(data.location || 'Community Member');
            setContact(data.contactNumber || '');
            setSkills(data.skillsSummary || '');
            setPortfolio(data.portfolioUrl || '');
            setLang(data.preferredLanguage || 'en');

            // Fetch reviews to calculate reputation average
            try {
              if (data.user?.id) {
                const reviewData = await apiRequest<any[]>(`/api/v1/core/users/${data.user.id}/reviews`);
                if (Array.isArray(reviewData)) {
                  setReviews(reviewData);
                  if (reviewData.length > 0) {
                    const sum = reviewData.reduce((acc, r) => acc + (r.rating || 0), 0);
                    setAvgRating(parseFloat((sum / reviewData.length).toFixed(1)));
                  }
                }
              }
            } catch (err) {
              console.error('Failed to load reviews for rating calculation:', err);
            }
          }
        }
      } catch (err) {
        console.error('Failed to load profile.', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [mounted, role, router]);

  async function saveProfile() {
    setSaved(false);
    setErrorMsg('');

    if (contact && !/^\+?[0-9]{7,15}$/.test(contact.trim())) {
      setErrorMsg('Please enter a valid phone number (7 to 15 digits, optional + prefix).');
      return;
    }

    if (!profile) return;
    try {
      const endpoint = role === 'NGO'
        ? `/api/v1/core/profiles/ngo/${profile.id}`
        : `/api/v1/core/profiles/contributor/${profile.id}`;

      const body = role === 'NGO' ? {
        organizationName: name,
        domain: domain,
        contactNumber: contact,
        preferredLanguage: lang,
        location: location
      } : {
        firstName: name.split(' ')[0] || '',
        lastName: name.split(' ').slice(1).join(' ') || '',
        skillsSummary: skills,
        portfolioUrl: portfolio,
        preferredLanguage: lang,
        title: title || 'Technical Contributor',
        location: location || 'Community Member',
        contactNumber: contact
      };

      await apiRequest(endpoint, {
        method: 'PUT',
        body
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 4000);
    } catch (err) {
      console.error('Failed to save profile:', err);
      setErrorMsg(err instanceof Error ? err.message : 'Failed to save profile. Please try again.');
    }
  }

  async function handleDeleteAccount() {
    if (!confirm('Are you absolutely sure you want to delete your account? This action is permanent and cannot be undone.')) {
      return;
    }
    try {
      await apiRequest('/api/v1/core/profiles/me', {
        method: 'DELETE'
      });
      // Clear authentication storage
      sessionStorage.removeItem('auth_token');
      sessionStorage.removeItem('auth_role');
      sessionStorage.removeItem('auth_email');
      sessionStorage.removeItem('auth_user_id');
      window.dispatchEvent(new StorageEvent('storage'));
      router.push('/');
    } catch (err) {
      console.error('Failed to delete account:', err);
      alert('Failed to delete account. Please try again.');
    }
  }

  if (!mounted) {
    return (
      <main className="page-shell">
        <div className="profile-content" style={{ padding: '4rem', textAlign: 'center' }}>
          <div className="empty-state">Initializing profile settings...</div>
        </div>
      </main>
    );
  }

  return (
    <main className="page-shell">
      <div className="profile-content">
        <a className="back-link" href="/">← Back to explore</a>
        
        <section className="profile-hero settings-hero">
          <div className="profile-avatar dynamic-avatar">{initials}</div>
          <div>
            <span className="eyebrow">{role === 'NGO' ? 'NGO Organization' : 'Technical contributor'}</span>
            <h1>Profile settings</h1>
            <p className="muted">Keep your profile details current and manage your platform access.</p>
          </div>
        </section>

        <div className="settings-tabs" role="tablist">
          {tabs.map((item) => (
            <button
              key={item}
              role="tab"
              aria-selected={tab === item}
              className={tab === item ? 'settings-tab active' : 'settings-tab'}
              onClick={() => setTab(item)}
            >
              {item}
            </button>
          ))}
        </div>

        {tab === 'Profile Details' && (
          <section className="settings-panel">
            <div className="settings-heading">
              <div>
                <span className="eyebrow">Public profile</span>
                <h2>Profile details</h2>
              </div>
              <span className="status verified">
                {role === 'NGO' ? 'VERIFIED NGO' : 'TRUSTED CONTRIBUTOR'}
              </span>
            </div>

            {loading ? (
              <div className="empty-state">Loading profile...</div>
            ) : (
              <div className="settings-grid">
                <label>
                  Contact Email (Account)
                  <input
                    className="profile-input"
                    value={profile?.user?.email || ''}
                    disabled
                    style={{ opacity: 0.6, cursor: 'not-allowed' }}
                  />
                </label>

                <label>
                  {role === 'NGO' ? 'Organization name' : 'Full name'}
                  <input
                    className="profile-input"
                    value={name}
                    onChange={(e) => { setName(e.target.value); setSaved(false) }}
                  />
                </label>

                {role === 'NGO' ? (
                  <>
                    <label>
                      Focus Domain
                      <input
                        className="profile-input"
                        value={domain}
                        onChange={(e) => { setDomain(e.target.value); setSaved(false) }}
                      />
                    </label>
                    <label>
                      Contact number
                      <input
                        className="profile-input"
                        type="tel"
                        value={contact}
                        placeholder="+15550192831"
                        onChange={(e) => { setContact(e.target.value); setSaved(false) }}
                      />
                    </label>
                    <label>
                      Location
                      <input
                        className="profile-input"
                        value={location}
                        onChange={(e) => { setLocation(e.target.value); setSaved(false) }}
                      />
                    </label>
                  </>
                ) : (
                  <>
                    <label>
                      Role / Professional Title
                      <input
                        className="profile-input"
                        value={title}
                        onChange={(e) => { setTitle(e.target.value); setSaved(false) }}
                      />
                    </label>
                    <label>
                      Contact number
                      <input
                        className="profile-input"
                        type="tel"
                        value={contact}
                        placeholder="+15550192831"
                        onChange={(e) => { setContact(e.target.value); setSaved(false) }}
                      />
                    </label>
                    <label>
                      Location
                      <input
                        className="profile-input"
                        value={location}
                        onChange={(e) => { setLocation(e.target.value); setSaved(false) }}
                      />
                    </label>
                    <label>
                      Portfolio or GitHub URL
                      <input
                        className="profile-input"
                        type="url"
                        value={portfolio}
                        onChange={(e) => { setPortfolio(e.target.value); setSaved(false) }}
                      />
                    </label>
                    <label className="wide-field">
                      Skills summary
                      <textarea
                        className="profile-input"
                        value={skills}
                        onChange={(e) => { setSkills(e.target.value); setSaved(false) }}
                        rows={4}
                      />
                    </label>
                  </>
                )}

                <label>
                  Preferred language
                  <select
                    className="profile-input"
                    value={lang}
                    onChange={(e) => { setLang(e.target.value); setSaved(false) }}
                  >
                    <option value="en">English</option>
                    <option value="hi">Hindi (हिन्दी)</option>
                    <option value="mr">Marathi (मराठी)</option>
                    <option value="sw">Swahili (Kiswahili)</option>
                  </select>
                </label>
              </div>
            )}

            {errorMsg && (
              <p className="form-error" style={{ color: '#ef4444', marginTop: '1rem' }} role="alert">
                {errorMsg}
              </p>
            )}

            {saved && (
              <div style={{ marginTop: '1rem', padding: '0.75rem 1rem', background: 'rgba(34, 197, 94, 0.15)', border: '1px solid #22c55e', borderRadius: '6px', color: '#22c55e', fontWeight: 500, fontSize: '0.9rem' }}>
                ✓ Profile details saved successfully!
              </div>
            )}

            <button className="primary-button" onClick={saveProfile} style={{ marginTop: '1.5rem' }}>
              Save profile
            </button>

            <div className="danger-zone" style={{ marginTop: '3.5rem', paddingTop: '2rem', borderTop: '1px solid rgba(239, 68, 68, 0.2)' }}>
              <h3 style={{ color: '#ef4444', marginBottom: '0.5rem' }}>Danger Zone</h3>
              <p className="muted" style={{ fontSize: '0.9rem', marginBottom: '1.5rem' }}>
                Permanently delete your account and all associated profiles, statements, and applications from the platform. This action is irreversible.
              </p>
              <button
                className="outline-button"
                onClick={handleDeleteAccount}
                style={{ color: '#ef4444', borderColor: '#ef4444' }}
              >
                Delete Account
              </button>
            </div>
          </section>
        )}

        {tab === 'Reviews & Activity' && role === 'CONTRIBUTOR' && (
          <section className="settings-panel">
            <div className="settings-heading">
              <div>
                <span className="eyebrow">Reputation</span>
                <h2>Reviews & activity</h2>
              </div>
              {reviews.length > 0 && (
                <div className="activity-score">{avgRating} <span>/ 5</span></div>
              )}
            </div>
            
            <div className="completed-list" style={{ marginBottom: '2rem' }}>
              <span className="eyebrow" style={{ display: 'block', marginBottom: '0.5rem' }}>Completed Projects</span>
              {profile?.completedProjects > 0 ? (
                <article>
                  <strong>Active community contributions</strong>
                  <span>{profile.completedProjects} project{profile.completedProjects > 1 ? 's' : ''} completed</span>
                </article>
              ) : (
                <div className="empty-state" style={{ padding: '1rem', border: '1px dashed rgba(255, 255, 255, 0.1)', borderRadius: '6px' }}>
                  No completed projects registered yet.
                </div>
              )}
            </div>
            
            <ReviewsList userId={profile?.user?.id} />
          </section>
        )}
      </div>
    </main>
  );
}
