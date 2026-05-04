# SheGuard Android Application

## Overview

SheGuard is a mobile safety application designed to help users send emergency alerts quickly during dangerous situations.

The application focuses on rapid SOS activation, automatic location sharing, and emergency contact notification.

---

# Application Information

App Name: SheGuard
Package Name: com.sheguard
Platform: Android

Minimum SDK: 24
Target SDK: 34

Language: Kotlin
Architecture: MVVM

---

# Technology Stack

Programming Language
Kotlin

UI Framework
XML Layouts

Design System
Material Components

Architecture
MVVM + Repository Pattern

Database
Room (SQLite)

Backend
Firebase

Firebase Services

Firebase Authentication
Firebase Realtime Database
Firebase Cloud Messaging

Location Services

Google Play Services Location API

Background Tasks

Foreground Service
WorkManager

Messaging

WhatsApp Intent only

External messaging services are not allowed.

---

# Firebase Configuration

The Firebase configuration file is already present in:

```
app/google-services.json
```

Firebase will be used for:

User authentication
Cloud messaging notifications
Optional contact backup

---

# Database Design

Room Database must include the following entities.

Contact Table

id
name
phone
relationship
priorityLevel

Incident Table

incidentId
timestamp
latitude
longitude
contactsNotified
status

---

# Core Features

## Authentication

Login screen
Register screen
Firebase authentication

---

## Dashboard

Main screen must contain:

SOS button
Emergency contacts
Share location
Helpline numbers
Siren alarm
Incident history
Settings

---

## Emergency Contacts

Users must be able to:

Add contacts
Edit contacts
Delete contacts

Contacts stored locally using Room database.

---

## SOS System

When SOS is activated the application must:

Fetch current GPS location
Generate Google Maps location link
Send WhatsApp message to all emergency contacts

Message format:

HELP! I am in danger and need assistance.

My location:
https://maps.google.com/?q=LAT,LONG

LAT and LONG must be replaced dynamically.

---

## Emergency Triggers

SOS must also activate when:

User shakes phone 3 times
User presses power button 3 times
User holds volume up + volume down for 3 seconds
Voice trigger phrase is detected

Triggers must work even if the phone screen is locked.

---

## Helpline Numbers

Quick dial buttons must exist for:

Police – 100
Women Helpline – 1091
Ambulance – 108
Fire – 101

---

## Incident History

All SOS activations must be stored locally.

Each record contains:

timestamp
latitude
longitude
contacts notified

Display using RecyclerView.

---

# Permissions

The application must request runtime permissions for:

Location
Microphone
Contacts
Phone
Internet
Foreground Service

---

# UI Components

Only XML UI must be used.

Use Material Components:

MaterialButton
TextInputLayout
TextInputEditText
RecyclerView
MaterialToolbar
CardView

Jetpack Compose must NOT be used.

---

# Project Structure

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

---

# Development Rules

Do not modify Gradle files unnecessarily.

Do not generate Jetpack Compose UI.

Do not edit generated build files.

All code must compile successfully.
