import { useEffect, useMemo, useState } from 'react'
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth'
import { onValue, push, ref, remove, set } from 'firebase/database'
import { Circle, CircleMarker, MapContainer, Popup, TileLayer, useMapEvents } from 'react-leaflet'
import { auth, database } from './firebase'

const ADMIN_EMAIL = 'admin@shegurd.com'
const ADMIN_PASSWORD = 'admin@123'
const DEFAULT_CENTER = [20.5937, 78.9629]

const emptyZoneForm = {
  id: '',
  name: '',
  type: 'danger',
  latitude: '',
  longitude: '',
  radiusMeters: '500',
}

function App() {
  const [user, setUser] = useState(null)
  const [authReady, setAuthReady] = useState(false)
  const [loginError, setLoginError] = useState('')
  const [seedMessage, setSeedMessage] = useState('')
  const [zones, setZones] = useState([])
  const [trackingUsers, setTrackingUsers] = useState([])
  const [sosEvents, setSosEvents] = useState([])
  const [zoneHistory, setZoneHistory] = useState([])
  const [zoneForm, setZoneForm] = useState(emptyZoneForm)
  const [mapDraftPosition, setMapDraftPosition] = useState(null)

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async currentUser => {
      if (!currentUser) {
        setUser(null)
        setAuthReady(true)
        return
      }

      if (currentUser.email !== ADMIN_EMAIL) {
        await signOut(auth)
        setLoginError('Only the SheGuard admin account can access this dashboard.')
        setAuthReady(true)
        return
      }

      setUser(currentUser)
      setAuthReady(true)
    })

    return unsubscribe
  }, [])

  useEffect(() => {
    if (!user) {
      return undefined
    }

    const zonesRef = ref(database, 'zones')
    const trackingRef = ref(database, 'tracking')
    const sosEventsRef = ref(database, 'sosEvents')
    const zoneHistoryRef = ref(database, 'zoneHistory')

    const unsubscribeZones = onValue(zonesRef, snapshot => {
      const value = snapshot.val() || {}
      const items = Object.entries(value).map(([id, zone]) => ({ id, ...zone }))
      setZones(items.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0)))
    })

    const unsubscribeTracking = onValue(trackingRef, snapshot => {
      const value = snapshot.val() || {}
      const items = Object.entries(value).map(([id, trackedUser]) => ({ id, ...trackedUser }))
      setTrackingUsers(items.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0)))
    })

    const unsubscribeSosEvents = onValue(sosEventsRef, snapshot => {
      const value = snapshot.val() || {}
      const items = Object.entries(value).map(([id, event]) => ({ id, ...event }))
      setSosEvents(items.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)))
    })

    const unsubscribeZoneHistory = onValue(zoneHistoryRef, snapshot => {
      const value = snapshot.val() || {}
      const items = Object.entries(value).flatMap(([userId, historyEntries]) =>
        Object.entries(historyEntries || {}).map(([historyId, entry]) => ({
          id: historyId,
          userId,
          ...entry,
        })),
      )
      setZoneHistory(items.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)))
    })

    return () => {
      unsubscribeZones()
      unsubscribeTracking()
      unsubscribeSosEvents()
      unsubscribeZoneHistory()
    }
  }, [user])

  const stats = useMemo(
    () => [
      { label: 'Zones', value: zones.length.toString().padStart(2, '0') },
      {
        label: 'Danger zones',
        value: zones.filter(zone => zone.type === 'danger').length.toString().padStart(2, '0'),
      },
      { label: 'Tracked users', value: trackingUsers.length.toString().padStart(2, '0') },
      { label: 'SOS events', value: sosEvents.length.toString().padStart(2, '0') },
      { label: 'Zone transitions', value: zoneHistory.length.toString().padStart(2, '0') },
    ],
    [zones, trackingUsers, sosEvents, zoneHistory],
  )

  const hotspotData = useMemo(() => {
    const grouped = sosEvents.reduce((accumulator, event) => {
      const key = event.zoneName || event.zoneState || 'Unmapped area'
      accumulator[key] = (accumulator[key] || 0) + 1
      return accumulator
    }, {})

    return Object.entries(grouped)
      .map(([label, total]) => ({ label, total }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 6)
  }, [sosEvents])

  const mapCenter = useMemo(() => {
    const firstTracked = trackingUsers.find(item => item.latitude && item.longitude)
    if (firstTracked) {
      return [Number(firstTracked.latitude), Number(firstTracked.longitude)]
    }

    const firstZone = zones.find(zone => zone.latitude && zone.longitude)
    if (firstZone) {
      return [Number(firstZone.latitude), Number(firstZone.longitude)]
    }

    return DEFAULT_CENTER
  }, [trackingUsers, zones])

  const handleLogin = async event => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const email = String(form.get('email') || '').trim().toLowerCase()
    const password = String(form.get('password') || '')

    if (email !== ADMIN_EMAIL) {
      setLoginError('Use the dedicated admin account to continue.')
      return
    }

    try {
      setLoginError('')
      await signInWithEmailAndPassword(auth, email, password)
    } catch (error) {
      setLoginError(error.message || 'Unable to sign in.')
    }
  }

  const handleSeedAdmin = async () => {
    try {
      setSeedMessage('')
      await createUserWithEmailAndPassword(auth, ADMIN_EMAIL, ADMIN_PASSWORD)
      await signOut(auth)
      setSeedMessage('Admin account created in Firebase Authentication. You can now sign in.')
    } catch (error) {
      if (error.code === 'auth/email-already-in-use') {
        setSeedMessage('Admin account already exists. Use the credentials shown to sign in.')
        return
      }

      setSeedMessage(error.message || 'Unable to create the admin account.')
    }
  }

  const handleZoneSubmit = async event => {
    event.preventDefault()
    if (!user) {
      return
    }

    const latitude = Number(zoneForm.latitude)
    const longitude = Number(zoneForm.longitude)
    const radiusMeters = Number(zoneForm.radiusMeters)

    if (!zoneForm.name || Number.isNaN(latitude) || Number.isNaN(longitude) || Number.isNaN(radiusMeters)) {
      return
    }

    const zoneRef = zoneForm.id ? ref(database, `zones/${zoneForm.id}`) : push(ref(database, 'zones'))
    const existingZone = zones.find(zone => zone.id === zoneForm.id)
    const payload = {
      id: zoneRef.key,
      name: zoneForm.name,
      type: zoneForm.type,
      latitude,
      longitude,
      radiusMeters,
      createdAt: existingZone?.createdAt || Date.now(),
      updatedAt: Date.now(),
      createdBy: user.uid,
    }

    await set(zoneRef, payload)
    setZoneForm(emptyZoneForm)
    setMapDraftPosition(null)
  }

  const handleEditZone = zone => {
    setZoneForm({
      id: zone.id,
      name: zone.name,
      type: zone.type,
      latitude: String(zone.latitude),
      longitude: String(zone.longitude),
      radiusMeters: String(zone.radiusMeters),
    })
    setMapDraftPosition([Number(zone.latitude), Number(zone.longitude)])
  }

  const handleDeleteZone = async zoneId => {
    await remove(ref(database, `zones/${zoneId}`))
    if (zoneForm.id === zoneId) {
      setZoneForm(emptyZoneForm)
      setMapDraftPosition(null)
    }
  }

  const handleMapDraftChange = coordinates => {
    setMapDraftPosition(coordinates)
    setZoneForm(current => ({
      ...current,
      latitude: coordinates[0].toFixed(6),
      longitude: coordinates[1].toFixed(6),
    }))
  }

  const handleLogout = async () => {
    await signOut(auth)
  }

  if (!authReady) {
    return <div className="screen-center">Loading SheGuard admin dashboard...</div>
  }

  if (!user) {
    return (
      <div className="login-shell">
        <div className="login-card">
          <p className="eyebrow">SheGuard Command Center</p>
          <h1>Monitor danger zones, safe zones, and SOS hotspots in real time.</h1>
          <p className="lede">
            Sign in with the single admin account to add circular zones, monitor live user
            movement, and review the places where SOS alerts happen most often.
          </p>

          <div className="credential-card">
            <span>Admin email</span>
            <strong>{ADMIN_EMAIL}</strong>
            <span>Admin password</span>
            <strong>{ADMIN_PASSWORD}</strong>
          </div>

          <form className="login-form" onSubmit={handleLogin}>
            <label>
              <span>Email</span>
              <input name="email" type="email" defaultValue={ADMIN_EMAIL} required />
            </label>
            <label>
              <span>Password</span>
              <input name="password" type="password" defaultValue={ADMIN_PASSWORD} required />
            </label>
            <button type="submit">Sign in as admin</button>
          </form>

          <button className="secondary-button" type="button" onClick={handleSeedAdmin}>
            Create admin account in Firebase Auth
          </button>

          {loginError ? <p className="error-text">{loginError}</p> : null}
          {seedMessage ? <p className="info-text">{seedMessage}</p> : null}
          <p className="footer-note">
            For production, replace <code>VITE_FIREBASE_APP_ID</code> in
            <code> admin-dashboard/.env</code> with your web app ID from Firebase console.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="app-shell">
      <header className="hero-panel">
        <div>
          <p className="eyebrow">Live Safety Grid</p>
          <h1>Zone intelligence for SheGuard responders</h1>
          <p className="lede">
            Define circular safe and danger zones, watch live user movement, and find the
            locations that generate the highest SOS traffic.
          </p>
        </div>
        <div className="hero-actions">
          <div className="admin-pill">Signed in as {user.email}</div>
          <button className="secondary-button" type="button" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </header>

      <section className="stats-grid">
        {stats.map(item => (
          <article key={item.label} className="stat-card">
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </article>
        ))}
      </section>

      <section className="dashboard-grid">
        <article className="panel map-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Map Overview</p>
              <h2>Live tracked users and circular zones</h2>
            </div>
          </div>
          <MapContainer center={mapCenter} zoom={13} scrollWheelZoom className="map-view">
            <ZonePicker onPick={handleMapDraftChange} />
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {zones.map(zone => (
              <Circle
                key={zone.id}
                center={[Number(zone.latitude), Number(zone.longitude)]}
                radius={Number(zone.radiusMeters)}
                pathOptions={{
                  color: zone.type === 'danger' ? '#c44536' : '#1f8a70',
                  fillColor: zone.type === 'danger' ? '#f4a261' : '#6dd3b8',
                  fillOpacity: 0.24,
                }}
              >
                <Popup>
                  <strong>{zone.name}</strong>
                  <br />
                  {zone.type} zone
                  <br />
                  Radius: {zone.radiusMeters}m
                </Popup>
              </Circle>
            ))}

            {trackingUsers.map(trackedUser => (
              <CircleMarker
                key={trackedUser.id}
                center={[Number(trackedUser.latitude), Number(trackedUser.longitude)]}
                radius={10}
                pathOptions={{
                  color:
                    trackedUser.zoneState === 'danger'
                      ? '#c44536'
                      : trackedUser.zoneState === 'safe'
                        ? '#1f8a70'
                        : '#355070',
                  fillOpacity: 1,
                }}
              >
                <Popup>
                  <strong>{trackedUser.name || trackedUser.email || trackedUser.uid}</strong>
                  <br />
                  State: {trackedUser.zoneState || 'unmapped'}
                  <br />
                  Zone: {trackedUser.zoneName || 'Outside all zones'}
                </Popup>
              </CircleMarker>
            ))}

            {mapDraftPosition ? (
              <Circle
                center={mapDraftPosition}
                radius={Number(zoneForm.radiusMeters || 0)}
                pathOptions={{
                  color: zoneForm.type === 'danger' ? '#9f2f23' : '#156a55',
                  fillColor: zoneForm.type === 'danger' ? '#e76f51' : '#2a9d8f',
                  fillOpacity: 0.18,
                  dashArray: '8 8',
                }}
              >
                <Popup>
                  Draft zone<br />
                  Click anywhere on the map to move this circle.
                </Popup>
              </Circle>
            ) : null}
          </MapContainer>
        </article>

        <article className="panel form-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Zone Builder</p>
              <h2>{zoneForm.id ? 'Edit circular zone' : 'Add circular zone'}</h2>
            </div>
          </div>

          <form className="zone-form" onSubmit={handleZoneSubmit}>
            <label>
              <span>Zone name</span>
              <input
                value={zoneForm.name}
                onChange={event => setZoneForm(current => ({ ...current, name: event.target.value }))}
                placeholder="Campus Gate"
                required
              />
            </label>

            <label>
              <span>Zone type</span>
              <select
                value={zoneForm.type}
                onChange={event => setZoneForm(current => ({ ...current, type: event.target.value }))}
              >
                <option value="danger">Danger zone</option>
                <option value="safe">Safe zone</option>
              </select>
            </label>

            <div className="form-split">
              <label>
                <span>Latitude</span>
                <input
                  value={zoneForm.latitude}
                  onChange={event =>
                    setZoneForm(current => ({ ...current, latitude: event.target.value }))
                  }
                  placeholder="19.0760"
                  required
                />
              </label>

              <label>
                <span>Longitude</span>
                <input
                  value={zoneForm.longitude}
                  onChange={event =>
                    setZoneForm(current => ({ ...current, longitude: event.target.value }))
                  }
                  placeholder="72.8777"
                  required
                />
              </label>
            </div>

            <label>
              <span>Radius in meters</span>
              <input
                value={zoneForm.radiusMeters}
                onChange={event =>
                  setZoneForm(current => ({ ...current, radiusMeters: event.target.value }))
                }
                placeholder="500"
                required
              />
            </label>

            <p className="helper-text">
              Tip: click anywhere on the map to auto-fill latitude and longitude for the circle.
            </p>

            <div className="form-actions">
              <button type="submit">{zoneForm.id ? 'Update zone' : 'Create zone'}</button>
              <button
                className="secondary-button"
                type="button"
                onClick={() => setZoneForm(emptyZoneForm)}
              >
                Reset
              </button>
            </div>
          </form>

          <div className="zone-list">
            {zones.map(zone => (
              <article key={zone.id} className="zone-item">
                <div>
                  <strong>{zone.name}</strong>
                  <p>
                    {zone.type} zone • {zone.radiusMeters}m
                  </p>
                </div>
                <div className="zone-actions">
                  <button className="chip-button" type="button" onClick={() => handleEditZone(zone)}>
                    Edit
                  </button>
                  <button
                    className="chip-button danger-chip"
                    type="button"
                    onClick={() => handleDeleteZone(zone.id)}
                  >
                    Delete
                  </button>
                </div>
              </article>
            ))}
          </div>
        </article>

        <article className="panel list-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Tracked Users</p>
              <h2>Realtime zone status</h2>
            </div>
          </div>
          <div className="stack-list">
            {trackingUsers.length === 0 ? <p>No live user tracking yet.</p> : null}
            {trackingUsers.map(item => (
              <article key={item.id} className="list-item">
                <div>
                  <strong>{item.name || item.email || item.uid}</strong>
                  <p>
                    {item.zoneState || 'unmapped'} • {item.zoneName || 'Outside all zones'}
                  </p>
                </div>
                <span>{formatDate(item.updatedAt)}</span>
              </article>
            ))}
          </div>
        </article>

        <article className="panel list-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">SOS Hotspots</p>
              <h2>Where alerts happen most</h2>
            </div>
          </div>
          <div className="stack-list">
            {hotspotData.length === 0 ? <p>No SOS events captured yet.</p> : null}
            {hotspotData.map(item => (
              <article key={item.label} className="list-item hotspot-item">
                <div>
                  <strong>{item.label}</strong>
                  <p>SOS alerts from this area</p>
                </div>
                <span>{item.total}</span>
              </article>
            ))}
          </div>
        </article>

        <article className="panel list-panel full-width-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Zone History</p>
              <h2>Entry and exit timeline</h2>
            </div>
          </div>
          <div className="stack-list">
            {zoneHistory.length === 0 ? <p>No zone movement history yet.</p> : null}
            {zoneHistory.slice(0, 40).map(item => (
              <article key={`${item.userId}-${item.id}`} className="list-item history-item">
                <div>
                  <strong>{item.userName || item.userEmail || item.userId}</strong>
                  <p>{item.summary || `${item.fromState} -> ${item.toState}`}</p>
                </div>
                <span>{formatDate(item.timestamp)}</span>
              </article>
            ))}
          </div>
        </article>
      </section>
    </div>
  )
}

function ZonePicker({ onPick }) {
  useMapEvents({
    click(event) {
      onPick([event.latlng.lat, event.latlng.lng])
    },
  })

  return null
}

function formatDate(value) {
  if (!value) {
    return 'Waiting'
  }

  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(value)
}

export default App
