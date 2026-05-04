# data.md

This document defines how AI agents must work in this repository.

Follow these rules strictly to avoid build errors.

---

# Project Overview

Application Name: SheGuard
Package Name: com.sheguard

Platform: Android
Language: Kotlin
UI Framework: XML Layouts + Material Components

Jetpack Compose MUST NOT be used.

---

# Build Commands

Build project:

```
./gradlew build
```

Run lint checks:

```
./gradlew lint
```

Run unit tests:

```
./gradlew test
```

Run instrumentation tests:

```
./gradlew connectedAndroidTest
```

---

# Critical Rules For Agents

1. DO NOT modify Gradle configuration unless necessary.

2. DO NOT introduce Jetpack Compose.

3. All UI must be built using XML layouts.

4. Use Material Components for UI widgets.

5. Place source code only inside:

```
app/src/main/java
```

6. Place layouts inside:

```
app/src/main/res/layout
```

7. All user visible text must be inside:

```
res/values/strings.xml
```

8. Never generate duplicate Gradle blocks.

9. Avoid editing generated files inside:

```
build/
app/build/
```

---

# Technology Stack

Language
Kotlin

UI
XML Layouts
Material Components

Architecture
MVVM

Database
Room

Backend
Firebase

Firebase Services:

* Firebase Authentication
* Firebase Realtime Database
* Firebase Cloud Messaging

Location

Google Play Services Location API

Background Services

Foreground Service
WorkManager

---

# Application Features

The SheGuard application provides emergency safety functionality.

Core modules:

Authentication
Dashboard
Emergency Contacts
SOS System
Location Sharing
Emergency Triggers
Helpline
Incident History
Settings

---

# Emergency Contacts

Users can:

Add contacts
Edit contacts
Delete contacts

Contact fields:

id
name
phone
relationship
priorityLevel

Store contacts in Room database.

---

# SOS Emergency System

Pressing the SOS button must:

1. Fetch GPS location
2. Generate Google Maps link
3. Send WhatsApp message to emergency contacts

Message format:

HELP! I am in danger and need assistance.

My location:
https://maps.google.com/?q=LAT,LONG

Replace LAT and LONG dynamically.

---

# Emergency Triggers

App must support:

Manual SOS button
Shake phone detection
Power button multiple press
Volume button combination

Triggers should work even if the screen is locked.

---

# Helpline Numbers

Quick call buttons:

Police → 100
Women Helpline → 1091
Ambulance → 108
Fire → 101

---

# Incident History

Each emergency event must be saved.

Stored data:

timestamp
latitude
longitude
contacts notified

Display history using RecyclerView.

---

# UI Components

Allowed UI components:

MaterialButton
TextInputLayout
TextInputEditText
RecyclerView
MaterialToolbar
CardView
FloatingActionButton

Compose components are NOT allowed.

---

# Project Structure

```
com.sheguard

auth
dashboard
contacts
database
repository
viewmodel
sos
location
history
helpline
settings
```

Each feature must follow MVVM pattern.

---

# Code Style

Follow official Kotlin style guide.

Indentation: 4 spaces
Max line length: 100 characters

Naming:

Classes → PascalCase
Functions → camelCase
Variables → camelCase
Constants → UPPER_SNAKE_CASE

---

# Error Handling

Use try-catch for external APIs.

Use sealed classes or Result for internal error states.

Avoid catching generic Exception unless necessary.

---

# Important

This project uses XML UI.

Agents must NOT generate Jetpack Compose code.
