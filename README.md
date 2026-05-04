# SheGuard

A personal safety app that provides emergency alerts, danger zone monitoring, and incident reporting.

## Project Structure

```
SheGuard/
├── app/                          # Android application
│   ├── src/main/java/com/sheguard/
│   │   ├── auth/                 # Authentication (Login, Register)
│   │   ├── sos/                  # SOS functionality (SOSManager, SOSMessenger, SirenManager)
│   │   ├── dashboard/            # Main dashboard and incident tracking
│   │   ├── zone/                 # Safety zone monitoring and alerts
│   │   ├── contacts/             # Emergency contact management
│   │   ├── history/              # Incident history
│   │   ├── notifications/        # Safety notifications
│   │   ├── services/             # Background services
│   │   ├── sensors/              # Shake detection for SOS
│   │   ├── network/              # Image/video upload utilities
│   │   ├── db/                   # Room database (Local contacts/incidents storage)
│   │   └── firebase/             # Firebase utilities
│   └── build.gradle
├── admin-dashboard/              # React admin dashboard
│   ├── src/
│   │   ├── App.jsx              # Main admin dashboard component
│   │   ├── firebase.js          # Firebase configuration
│   │   ├── main.jsx             # Entry point
│   │   └── styles.css           # Dashboard styles
│   ├── vite.config.js
│   └── package.json
├── firebase.json                 # Firebase configuration
└── database.rules.json           # Firebase database rules
```

## Android App

### Features
- **SOS Emergency**: Trigger emergency alerts via shake detection or button press
- **Live Location Tracking**: Track user location and zone transitions
- **Safety Zones**: Define circular safe/danger zones with geofencing
- **Evidence Capture**: Automatic photo/video capture during SOS events
- **Emergency Contacts**: Manage and notify contacts during emergencies
- **Incident History**: View past incidents and zone transition logs

### Tech Stack
- **Language**: Kotlin
- **Min SDK**: 24
- **Target SDK**: 34
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (local), Firebase Realtime Database (remote)
- **Location**: Google Play Services Location
- **Camera**: CameraX
- **Networking**: OkHttp

### Build & Run

```bash
# Build the project
./gradlew build

# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

### Requirements
- Android Studio Flamingo or newer
- Android SDK 24+
- Firebase project configuration

## Admin Dashboard

A web-based admin panel for monitoring safety zones, tracked users, and SOS events.

### Features
- **Zone Management**: Create/edit/delete circular safe/danger zones
- **Live Tracking**: View real-time user location and zone status
- **SOS Monitoring**: Review SOS alerts and hotspot analysis
- **Zone History**: Track user entry/exit from safety zones

### Tech Stack
- **Framework**: React 18
- **Build Tool**: Vite 5
- **Mapping**: Leaflet + React Leaflet
- **Backend**: Firebase Realtime Database

### Setup

```bash
cd admin-dashboard

# Install dependencies
npm install

# Copy environment template and configure
cp .env.example .env
# Edit .env with your Firebase credentials

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Environment Variables

Create `.env` file in `admin-dashboard/`:

```env
VITE_FIREBASE_API_KEY=your-api-key
VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
VITE_FIREBASE_DATABASE_URL=https://your-project.firebaseio.com
VITE_FIREBASE_PROJECT_ID=your-project
VITE_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=123456789
VITE_FIREBASE_APP_ID=1:123456789:web:abcdef
```

### Admin Credentials

The dashboard supports a single admin account:
- **Email**: `admin@shegurd.com`
- **Password**: `admin@123`

Run "Create admin account in Firebase Auth" on first setup.

## Firebase Configuration

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Register the Android app with package name `com.sheguard`
3. Register the web app for admin dashboard
4. Enable Authentication (Email/Password)
5. Enable Realtime Database

Import database rules from `database.rules.json`.

## License

[Add license information here]