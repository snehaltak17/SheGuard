import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'
import { getDatabase } from 'firebase/database'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || 'AIzaSyAoxjBcwTVLRlog_VM5ohGpb8B7wjiMCSg',
  authDomain:
    import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || 'she-guard-96245.firebaseapp.com',
  databaseURL:
    import.meta.env.VITE_FIREBASE_DATABASE_URL ||
    'https://she-guard-96245-default-rtdb.firebaseio.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'she-guard-96245',
  storageBucket:
    import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || 'she-guard-96245.firebasestorage.app',
  messagingSenderId:
    import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '790582591206',
  appId:
    import.meta.env.VITE_FIREBASE_APP_ID || '1:790582591206:web:sheguard-admin-dashboard',
}

const app = initializeApp(firebaseConfig)

export const auth = getAuth(app)
export const database = getDatabase(app)
