# Project NewSight - Android Frontend Application

Project NewSight is an Android assistive technology application designed to help visually impaired users with AI-powered features including face recognition, voice navigation, object detection, text reading, emergency alerts, ASL detection, and smart clothing recognition.

---

## Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Features](#features)
4. [UI/UX Design Overview](#uiux-design-overview)
5. [Tech Stack](#tech-stack)
6. [Requirements](#requirements)
7. [Build, Install & Configuration](#build-install--configuration)
8. [Running the Applications](#running-the-applications)
9. [Testing](#testing)
10. [Configuration](#configuration)
11. [Known Issues](#known-issues)
12. [Future Work](#future-work)

---

## Overview

This frontend consists of four separate Android projects:

**Main app** - The primary unified Android application with 12 integrated features (in `app/` folder)

**asl-frontend** - ASL detection feature in a separate folder

**color-cue** - Clothing recognition feature in a separate folder

**main-branch** - Legacy UI implementation preserved for reference (old UI before rebranding)

**Why are asl-frontend and color-cue separate?**
These two features were last-minute integrations before the final merge to main. To avoid breaking the currently working features in the main app, we kept them in their own self-contained folders with separate Gradle configurations. They can be built and run independently.

**Why is main-branch separate?**
The `main-branch` folder contains our old UI implementation before we rebranded to the new UI design. We preserved it to avoid losing the previous work, but it is not actively maintained.

---

## Project Structure

```
CIS4398-Project-NewSight-Frontend/
│
├── app/                          # Main Android app (Current UI)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/newsight/
│   │   │   │   ├── MainActivity.java              # Login & face recognition
│   │   │   │   ├── HomeActivity.java               # Main dashboard with rewards
│   │   │   │   ├── NavigateActivity.java           # AR navigation
│   │   │   │   ├── ObstacleActivity.java           # Object detection
│   │   │   │   ├── ReadTextActivity.java           # OCR text detection
│   │   │   │   ├── EmergencyActivity.java          # Emergency contacts
│   │   │   │   ├── CommunicateActivity.java        # Communication hub
│   │   │   │   ├── ObserveActivity.java            # Observation features
│   │   │   │   ├── ColorCueActivity.java           # Color cue integration
│   │   │   │   ├── SettingsActivity.java           # App settings
│   │   │   │   ├── UserProfileActivity.java        # User profile
│   │   │   │   ├── TrustedContactsActivity.java    # Contact management
│   │   │   │   ├── RewardsActivity.java            # Rewards display
│   │   │   │   ├── RedeemActivity.java            # Points redemption
│   │   │   │   ├── HelpAndSupportActivity.java    # Help & support
│   │   │   │   ├── PrivacyAndDataActivity.java     # Privacy settings
│   │   │   │   ├── LogoutActivity.java            # Logout handling
│   │   │   │   │
│   │   │   │   ├── helpers/
│   │   │   │   │   ├── VoiceCommandHelper.java    # Voice command processing
│   │   │   │   │   ├── LocationHelper.java        # GPS tracking
│   │   │   │   │   ├── LocationWebSocketHelper.java # Location streaming
│   │   │   │   │   ├── NavigationHelper.java      # Navigation updates
│   │   │   │   │   ├── TtsHelper.java             # Text-to-speech
│   │   │   │   │   └── ReadTextTTSHelper.java     # OCR TTS
│   │   │   │   │
│   │   │   │   ├── models/
│   │   │   │   │   ├── DirectionsResponse.java    # Navigation directions
│   │   │   │   │   ├── NavigationStep.java        # Navigation step
│   │   │   │   │   ├── NavigationUpdate.java      # Real-time update
│   │   │   │   │   ├── VoiceResponse.java         # Voice API response
│   │   │   │   │   ├── LocationCoordinates.java   # GPS coordinates
│   │   │   │   │   └── TransitInfo.java           # Transit information
│   │   │   │   │
│   │   │   │   ├── DetectorProcessor.java         # Object detection processor
│   │   │   │   ├── OverlayView.java              # Detection overlay
│   │   │   │   ├── WebSocketManager.java          # WebSocket management
│   │   │   │   ├── VibrationMotor.java            # Haptic feedback motor
│   │   │   │   ├── PatternGenerator.java          # Vibration patterns
│   │   │   │   ├── HapticPermissionHelper.java   # Vibration permissions
│   │   │   │   └── VibrationPattern.java          # Pattern data models
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/                         # XML layouts for activities
│   │   │   │   ├── drawable/                       # Icons, backgrounds, shapes
│   │   │   │   ├── values/                         # Colors, strings, styles, themes
│   │   │   │   └── mipmap/                        # App icons
│   │   │   ├── assets/
│   │   │   │   └── efficientdet-lite0.tflite     # Object detection model
│   │   │   └── AndroidManifest.xml                 # Permissions & activities
│   │   │
│   │   ├── test/                                  # Unit tests
│   │   │   └── java/com/example/newsight/         # Test classes
│   │   │
│   │   └── androidTest/                           # Instrumented tests
│   │       └── java/com/example/newsight/         # Android test classes
│   │   │
│   │   └── build.gradle.kts                       # App dependencies
│   │
│   └── build.gradle.kts                            # Project config
│
├── asl-frontend/                  # ASL Detection App (Separate)
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/example/newsight/      # ASL detection activities
│   │   │   │   ├── res/                            # ASL UI resources
│   │   │   │   └── AndroidManifest.xml
│   │   │   └── build.gradle.kts
│   │   └── build.gradle.kts
│   ├── gradle/
│   ├── settings.gradle.kts
│   └── README.md
│
├── color-cue/                     # Color-Cue App (Separate)
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/example/newsight/      # Color-cue activities
│   │   │   │   ├── res/                            # Color-cue UI resources
│   │   │   │   └── AndroidManifest.xml
│   │   │   └── build.gradle.kts
│   │   └── build.gradle.kts
│   ├── gradle/
│   ├── settings.gradle.kts
│   └── README.md
│
├── main-branch/                    # Legacy UI (Old Implementation)
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/example/newsight/      # Old UI activities
│   │   │   │   ├── res/                            # Old UI resources
│   │   │   │   └── AndroidManifest.xml
│   │   │   └── build.gradle.kts
│   │   └── build.gradle.kts
│   ├── gradle/
│   ├── settings.gradle.kts
│   └── README.md
│
├── gradle/                         # Gradle wrapper
├── settings.gradle.kts            # Project settings
├── build.gradle.kts                # Root build config
└── README.md
```

---

## Features

### Main App Features (app/ folder)

#### 1. Home Dashboard
**Status: 100% Complete**

Central hub with feature cards, rewards display, and voice command access.

**How it works:**
- `HomeActivity.java` manages dashboard state and navigation
- Rewards card shows points, level, and streak (frontend-only, stored locally)
- Voice search bar activates "Hey Guide" command system
- 2-column grid layout with feature cards (Emergency, Navigation, Read Text, Identify, Faces, ASL, Colors)
- Floating bottom navigation bar (Home, Voice, Settings)
- Staggered animations on screen load

**UI/UX:**
- Large touch targets (48dp minimum) for accessibility
- High contrast colors and bold text
- Rewards card clickable to full Rewards screen
- Voice-first navigation available from any screen

---

#### 2. Voice-Activated Navigation
**Status: 100% Complete**

AR-style navigation with camera overlay showing distance, direction arrows, and turn instructions.

**How it works:**
- `NavigateActivity.java` manages navigation state and UI
- CameraX provides full-screen camera preview
- `NavigationHelper.java` receives real-time updates via WebSocket
- `LocationWebSocketHelper.java` streams GPS coordinates to backend
- `TtsHelper.java` provides voice announcements at 100m, 50ft, and 25ft before turns
- AR overlay shows distance (48sp white text), direction arrow (180dp), street name (22sp cyan), and instruction (24sp white)
- Transit banner displays bus/train info when in transit mode
- Automatic step advancement based on GPS proximity

**UI/UX:**
- Semi-transparent dark overlays for text readability
- Large, high-contrast text for visibility
- Voice announcements complement visual information

---

#### 3. Real-Time Object Detection
**Status: 100% Complete**

On-device object detection with bounding box overlay on camera feed.

**How it works:**
- `ObstacleActivity.java` manages camera lifecycle
- `DetectorProcessor.java` processes frames with EfficientDet-Lite0 TensorFlow Lite model
- `OverlayView.java` custom view draws bounding boxes and labels
- Real-time processing at camera frame rate
- Works offline (no backend dependency)

**UI/UX:**
- High-contrast bounding boxes with color-coded object types
- Labels show object name and confidence above boxes
- White text on dark background for readability

---

#### 4. Text Detection (OCR)
**Status: 100% Complete**

Real-time text detection from camera feed with text-to-speech output.

**How it works:**
- `ReadTextActivity.java` manages camera and WebSocket connection
- Camera frames sent to backend via WebSocket
- Backend EasyOCR processes frames and returns detected text
- Stability filtering prevents text flickering across frames
- `ReadTextTTSHelper.java` handles text-to-speech output
- Text display updates in real-time

**UI/UX:**
- Large text box (24sp, bold) at bottom-center showing detected text
- Start/Stop and Read Aloud buttons for user control
- Connection status indicator in top-left corner

---

#### 5. Familiar Face Recognition
**Status: 100% Complete**

Real-time face recognition to identify familiar contacts using camera feed.

**How it works:**
- `MainActivity.java` handles face recognition mode
- Camera frames sent to backend via WebSocket
- Backend DeepFace matches faces against S3 gallery
- Recognition results displayed as overlay with person's name
- WebSocketManager handles real-time communication

**User Experience:**
- Real-time face matching as camera moves
- Clear name display for recognized faces
- Seamless integration with voice commands

---

#### 6. Emergency Contact System
**Status: 100% Complete**

Quick emergency alert with location sharing and photo capture.

**How it works:**
- `EmergencyActivity.java` manages contact list
- Emergency button captures GPS location and photo
- Photo uploaded to AWS S3 via backend
- SMS alerts sent to all contacts via backend Vonage API
- Contact management UI for adding/removing trusted contacts

---

#### 7. Voice Commands ("Hey Guide")
**Status: 100% Complete**

Intelligent voice command system that routes user requests to appropriate features.

**How it works:**
- `VoiceCommandHelper.java` handles voice processing:
  - Wake word detection for "Hey Guide"
  - Audio recording via MediaRecorder
  - Audio upload to backend for transcription via Groq Whisper
  - Backend LLM routes to features (NAVIGATION, OBJECT_DETECTION, TEXT_DETECTION, etc.)
- `TtsHelper.java` provides voice feedback
- Integrated in all activities for hands-free operation
- Session ID management for navigation tracking

---

#### 8. Text-to-Speech (TTS)
**Status: 100% Complete**

Audio feedback system providing spoken information throughout the app.

**How it works:**
- `TtsHelper.java` wraps Android TextToSpeech engine
- `ReadTextTTSHelper.java` specialized helper for OCR reading
- Automatic speech for navigation instructions, voice command responses, OCR text, and system notifications
- Configurable speech rate and language

---

#### 9. Haptic Feedback
**Status: 100% Complete**

Vibration patterns providing tactile feedback for navigation and alerts.

**How it works:**
- `VibrationMotor.java` manages device vibration
- `PatternGenerator.java` creates vibration patterns (directional, obstacle warnings, proximity alerts, arrival celebrations)
- `HapticPermissionHelper.java` checks vibration permissions
- Integrated with navigation for turn alerts and obstacle detection warnings

---

#### 10. Rewards System
**Status: 100% Complete (Frontend-Only Feature)**

Gamification system to encourage app usage. Points stored locally (no backend integration).

**How it works:**
- `RewardsActivity.java` displays rewards information
- `RedeemActivity.java` handles point redemption
- Points calculation: 1000 points = $1.00
- Reward tiers: $10 gift cards at 10,000 points
- Level progression based on total points

---

#### 11. Settings Menu
**Status: 100% Complete**

Settings interface for app configuration and user preferences.

**How it works:**
- `SettingsActivity.java` manages settings navigation
- Categories: User Profile, Trusted Contacts, Privacy & Data, Help & Support, Logout
- Voice commands integrated for hands-free navigation
- Settings persist locally using SharedPreferences

#### 12. User Profile
**Status: 100% Complete**

User account management and profile information interface.

**How it works:**
- `UserProfileActivity.java` displays profile information
- Profile data stored locally (can be extended with backend)
- Edit functionality for updating information
- Logout integration with authentication system

---

### asl-frontend Features

#### 13. ASL (American Sign Language) Detection
**Status: 90% Complete (Separate App)**

Real-time ASL hand sign recognition to convert sign language into text/letters.

**How it works:**
- Separate Android app with dedicated activities
- MediaPipe hand tracking for landmark detection
- TensorFlow Lite model for sign classification
- Real-time letter prediction display

**Why separate:** Last-minute integration kept isolated to preserve main app stability. Can be built and run independently.

---

### color-cue Features

#### 14. Color Cue (Clothing Recognition & Virtual Closet)
**Status: 100% Complete (Separate App)**

Smart clothing detection and virtual closet management.

**How it works:**
- Separate Android app with dedicated activities
- Camera integration for photo capture
- Backend API calls for color/pattern detection
- Database storage for clothing items
- AI suggests outfit combinations based on occasion, weather, and style

**Why separate:** Last-minute integration kept isolated to avoid breaking working features in main app. Can be built and run independently.

---

### main-branch (Legacy UI)

**Status: Archived (Not Maintained)**

The `main-branch` folder contains the old UI implementation before rebranding. This folder is preserved for reference but is not actively maintained or integrated with the current backend.

**Note:** This folder is kept for historical reference only. Use the main `app/` folder for active development.

---

## UI/UX Design Overview

The app follows accessibility-first design principles for visually impaired users:

**Design Principles:**
- High contrast colors (primary blue #00D4FF, white text on dark backgrounds)
- Large text sizes (16sp minimum, up to 48sp for critical info)
- Large touch targets (48dp minimum)
- Voice-first navigation ("Hey Guide" available from any screen)
- Consistent floating bottom navigation (Home, Voice, Settings)
- Text-to-speech and haptic feedback for multi-modal accessibility

**Color Scheme:**
- Primary: Cyan (#00D4FF) for actions and active states
- Background: Dark theme
- Text: White primary, muted gray secondary
- Emergency: Red for urgent actions

**Typography:**
- Headings: 28-32sp, bold
- Body: 16-18sp
- Large display: 24-48sp for critical info
- Small: 10-14sp for labels

**Accessibility:**
- TalkBack support with content descriptions
- Large text scaling support
- High contrast mode compatible
- Screen reader friendly markup

---

## Tech Stack

### Main App
- **Android SDK (Java)** - Primary development language
- **AndroidX Libraries** - Modern Android Jetpack components
- **Material Design Components** - UI/UX components for accessibility
- **CameraX** - Modern camera implementation
- **TensorFlow Lite** - On-device ML inference for object detection
- **EfficientDet-Lite0** - Lightweight object detection model
- **FusedLocationProviderClient** - GPS location tracking
- **OkHttp 5.2.1** - HTTP client and WebSocket support
- **Gson 2.10.1** - JSON parsing and serialization
- **Android TextToSpeech** - Voice announcements
- **Android MediaRecorder** - Audio capture for voice commands

### asl-frontend
- **Android SDK (Java)**
- **TensorFlow Lite** - ASL sign classification
- **MediaPipe** - Hand tracking and landmark detection
- **CameraX** - Camera preview

### color-cue
- **Android SDK (Java)**
- **CameraX** - Camera preview and image capture
- **OkHttp** - HTTP client for backend API calls
- **Gson** - JSON parsing

---

## Requirements

### Hardware Requirements

#### Android Device (Physical Device Recommended)
- **Minimum Android Version:** Android 7.0 (API level 24)
- **Target Android Version:** Android 15 (API level 36)
- **RAM:** 2GB minimum (4GB+ recommended for smooth ML model inference)
- **Storage:** 500MB free space for app installation and TensorFlow Lite models
- **Camera:** Rear-facing camera required for all camera-based features
- **GPS:** Built-in GPS chip required for navigation features
- **Microphone:** Required for voice commands
- **Vibrator:** Required for haptic feedback features
- **Network:** WiFi or mobile data for backend communication

#### Development Machine
- **Operating System:** Windows 10+, macOS 10.14+, or Linux (Ubuntu 18.04+)
- **RAM:** 8GB minimum (16GB recommended)
- **Storage:** 10GB free space for Android Studio, SDK, and emulators
- **CPU:** Multi-core processor recommended
- **USB Port:** For connecting physical Android devices

### Software Requirements

#### Development Tools
- **Android Studio:** Latest stable version (Hedgehog/Iguana or newer)
  - Includes Android SDK, Android Emulator, and build tools
  - Download from: https://developer.android.com/studio
- **Java Development Kit (JDK):** JDK 11 or higher
  - Included with Android Studio or install separately
- **Android SDK:** API level 24+ (Android 7.0)
  - Install via Android Studio SDK Manager
- **Gradle:** Included with Android Studio (Gradle 8.0+)
- **Git:** For cloning the repository

#### Android SDK Components
- **Android SDK Platform:** API 24 (minimum), API 36 (target)
- **Android SDK Build-Tools:** Latest version
- **Android Emulator:** Optional (limited functionality for camera/GPS features)
- **Android SDK Platform-Tools:** For ADB (Android Debug Bridge)

#### Backend Services
The app requires the NewSight backend servers to be running for full functionality:
- **Main Backend:** Port 8000 (required for most features)
- **AslBackend:** Port 8001 (required only for ASL detection app)
- **color-cue Backend:** Port 8002 (required only for color-cue app)

See the [Backend README](../CIS4398-Project-NewSight-Backend/README.md) for detailed backend setup instructions.

#### Network Requirements
- **Internet Connection:** Required for:
  - Downloading dependencies during build
  - Backend API communication
  - Google Maps API calls (via backend)
  - AWS S3 uploads
  - Groq API calls (via backend)
- **Local Network:** WiFi network for device-to-backend communication (or USB ADB port forwarding)

### Android Permissions

The app requires the following runtime permissions (requested at first use):

- **CAMERA** - Required for:
  - AR navigation overlay
  - Object detection
  - Face recognition
  - Text detection (OCR)
  - Emergency photo capture
- **ACCESS_FINE_LOCATION** - Required for:
  - GPS navigation
  - Turn-by-turn directions
  - Emergency location sharing
- **ACCESS_COARSE_LOCATION** - Required for:
  - Approximate location services
- **RECORD_AUDIO** - Required for:
  - Voice commands ("Hey Guide")
  - Speech-to-text transcription
- **VIBRATE** - Required for:
  - Haptic feedback patterns
  - Navigation alerts
  - Obstacle warnings

Permissions are requested at runtime following Android best practices. Users must grant permissions for features to function.

### Dependencies & Libraries

#### Core Android
- **AndroidX Libraries:** Modern Android Jetpack components
- **Material Design Components:** UI/UX components for accessibility
- **ConstraintLayout:** Flexible layout system

#### Camera & Vision
- **CameraX 1.3.4:** Modern camera API for preview and capture
- **TensorFlow Lite 0.4.4:** On-device ML inference
- **EfficientDet-Lite0 Model:** Pre-trained object detection model (~5MB)

#### Location & Navigation
- **Google Play Services Location 21.0.1:** GPS location tracking
- **FusedLocationProviderClient:** High-accuracy location updates

#### Networking
- **OkHttp 5.2.1:** HTTP client and WebSocket support
- **Gson 2.10.1:** JSON serialization/deserialization

#### Audio & Speech
- **Android TextToSpeech:** Built-in TTS engine
- **Android MediaRecorder:** Audio capture for voice commands

All dependencies are managed via Gradle and automatically downloaded during build.

---

## Build, Install & Configuration

This section provides detailed instructions to build, install, and configure the entire Project NewSight frontend on target devices.

**Note:** This project uses Gradle for build automation (Android standard). There are no Makefiles - all builds are handled through Gradle via Android Studio or command line.

**Note:** Ensure you have met all [Requirements](#requirements) before proceeding with installation.

### Target Devices

This frontend can be deployed on:
- **Physical Android devices** (recommended for GPS, camera, and sensor testing)
- **Android emulators** (limited functionality - GPS and camera may not work properly)

---

### Main App - Build & Install

**1. Clone Repository**

```bash
git clone <repository-url>
cd CIS4398-Project-NewSight-Frontend
```

**2. Open in Android Studio**

- Open Android Studio
- Select "Open an Existing Project"
- Navigate to `CIS4398-Project-NewSight-Frontend`
- Select the root folder

**3. Configure Backend URLs**

Update WebSocket and API URLs in the following files with your backend server IP:

- `app/src/main/java/com/example/newsight/helpers/VoiceCommandHelper.java` - Change `BACKEND_URL`
- `app/src/main/java/com/example/newsight/NavigateActivity.java` - Change `LOCATION_WS_URL` and `NAVIGATION_WS_URL`
- `app/src/main/java/com/example/newsight/HomeActivity.java` - Change WebSocket URLs
- `app/src/main/java/com/example/newsight/MainActivity.java` - Change WebSocket URLs
- `app/src/main/java/com/example/newsight/ReadTextActivity.java` - Change WebSocket URLs
- `app/src/main/java/com/example/newsight/CommunicateActivity.java` - Change WebSocket URLs
- `app/src/main/java/com/example/newsight/ObserveActivity.java` - Change WebSocket URLs

Example:
```java
private static final String BACKEND_URL = "http://192.168.1.254:8000/voice/transcribe";
private static final String LOCATION_WS_URL = "ws://192.168.1.254:8000/location/ws";
private static final String NAVIGATION_WS_URL = "ws://192.168.1.254:8000/navigation/ws";
```

**4. Add TensorFlow Lite Model**

- Place `efficientdet-lite0.tflite` in `app/src/main/assets/`
- The model should already be present, but verify it exists

**5. Gradle Sync**

- Android Studio will automatically sync Gradle
- If not, click "Sync Project with Gradle Files" (elephant icon)
- Ensure all dependencies download successfully

**6. Connect Physical Device** (Recommended)

- Enable Developer Options on your Android device:
  - Go to Settings > About Phone
  - Tap "Build Number" 7 times
- Enable USB Debugging:
  - Go to Settings > Developer Options
  - Enable "USB Debugging"
- Connect device via USB
- Android Studio should detect the device

**7. Build and Run**

**Using Android Studio (Recommended):**
- Click the Run button (green play icon) or press Shift+F10
- Select your device
- App will build, install, and launch

**Using Gradle Command Line:**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build and install in one command
./gradlew installDebug
```

**Note:** This project uses Gradle for build automation (Android standard). There are no Makefiles - all builds are handled through Gradle build scripts (`build.gradle.kts`).

---

### asl-frontend - Build & Install

**1. Navigate to asl-frontend Directory**

```bash
# From repository root
cd CIS4398-Project-NewSight-Frontend
cd asl-frontend
```

**2. Open in Android Studio**

- In Android Studio, select "File > Open"
- Navigate to `asl-frontend` folder
- Select the folder

**3. Configure Backend URLs**

Update backend URLs in ASL-related activities to point to AslBackend (Port 8001):

- Update WebSocket/HTTP URLs to `http://<your-ip>:8001` or `ws://<your-ip>:8001`

**4. Verify TensorFlow Lite Model**

- Ensure ASL TensorFlow Lite model is in `app/src/main/assets/`
- Model should be present for ASL detection

**5. Gradle Sync**

- Sync project with Gradle files
- Ensure all dependencies download

**6. Build and Run**

- Connect physical device (recommended for camera testing)
- Click Run button
- App will build and install

---

### color-cue - Build & Install

**1. Navigate to color-cue Directory**

```bash
# From repository root
cd CIS4398-Project-NewSight-Frontend
cd color-cue
```

**2. Open in Android Studio**

- In Android Studio, select "File > Open"
- Navigate to `color-cue` folder
- Select the folder

**3. Configure Backend URLs**

Update backend URLs in color-cue activities to point to color-cue backend (Port 8002):

- Update HTTP/WebSocket URLs to `http://<your-ip>:8002` or `ws://<your-ip>:8002`

**4. Gradle Sync**

- Sync project with Gradle files
- Ensure all dependencies download

**5. Build and Run**

- Connect physical device (recommended for camera testing)
- Click Run button
- App will build and install

---

### Network Configuration

#### Physical Device Testing (WiFi)

If testing on a physical Android device connected to the same WiFi network:

**1. Find your computer's local IP address:**
- **Windows**: `ipconfig` (look for IPv4 Address)
- **Mac/Linux**: `ifconfig` or `ip addr` (look for inet address)

**2. Ensure backend servers are accessible on the network:**
```bash
# Main backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# AslBackend (if using ASL app)
cd AslBackend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001

# color-cue backend (if using color-cue app)
cd color-cue
uvicorn app.main:app --reload --host 0.0.0.0 --port 8002
```

**3. Test connectivity from phone:**
- Open browser on phone
- Navigate to `http://<your-ip>:8000/docs`
- Should see FastAPI documentation

**4. Update URLs in Android code:**
- Use your computer's IP address in all backend URL configurations

#### USB ADB Port Forwarding

Alternative to WiFi - use USB connection with ADB:

**1. Connect device via USB**

**2. Set up port forwarding:**
```bash
adb devices  # Get device serial
adb -s <device_serial> reverse tcp:8000 tcp:8000  # Main backend
adb -s <device_serial> reverse tcp:8001 tcp:8001  # AslBackend (if needed)
adb -s <device_serial> reverse tcp:8002 tcp:8002  # color-cue backend (if needed)
```

**3. Verify:**
```bash
adb -s <device_serial> reverse --list
```

**4. In Android code, use `localhost`:**
```java
private static final String BACKEND_URL = "http://localhost:8000/voice/transcribe";
```

**5. Remove port forwarding (when done):**
```bash
adb -s <device_serial> reverse --remove-all
```

---

## Running the Applications

### Main App

**1. Ensure backend is running:**
```bash
cd CIS4398-Project-NewSight-Backend
source venv/bin/activate  # or venv\Scripts\activate on Windows
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**2. Open Android Studio:**
- Open the main project (root folder)
- Connect physical device
- Click Run button (green play icon)
- App will install and launch

**Access:** App launches to MainActivity (login) or HomeActivity (if already logged in)

---

### asl-frontend

**1. Ensure AslBackend is running:**
```bash
cd CIS4398-Project-NewSight-Backend
cd AslBackend
source venv_asl/bin/activate  # or venv_asl\Scripts\activate on Windows
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

**2. Open Android Studio:**
- Open `asl-frontend` folder as project
- Connect physical device
- Click Run button
- ASL detection app will install and launch

---

### color-cue

**1. Ensure color-cue backend is running:**
```bash
cd CIS4398-Project-NewSight-Backend
cd color-cue
source venv_colorcue/bin/activate  # or venv_colorcue\Scripts\activate on Windows
uvicorn app.main:app --reload --host 0.0.0.0 --port 8002
```

**2. Open Android Studio:**
- Open `color-cue` folder as project
- Connect physical device
- Click Run button
- Color-cue app will install and launch

---

### Running All Three Together

You can run all three Android apps simultaneously on the same device. They are separate applications with different package names, so they won't conflict. Each app connects to its corresponding backend:

- Main app → Backend Port 8000
- asl-frontend → AslBackend Port 8001
- color-cue → color-cue backend Port 8002

---

## Testing

### Manual Testing

#### Navigation Feature
- [ ] Backend server is running on network
- [ ] Google Maps API key is configured in backend
- [ ] App has location permissions granted
- [ ] App has camera permissions granted
- [ ] App has microphone permissions granted
- [ ] GPS has acquired location
- [ ] Voice command "Hey Guide" works
- [ ] Generic place search works ("nearest CVS")
- [ ] Specific address works ("123 Main St")
- [ ] AR overlay displays correctly (distance, arrow, instruction)
- [ ] Voice announcements work at proper distances
- [ ] Navigation advances steps automatically
- [ ] "You have arrived" announcement at destination

#### Object Detection
- [ ] Camera preview displays
- [ ] Bounding boxes appear around detected objects
- [ ] Labels are readable and correctly positioned
- [ ] Model file is present in assets

#### Face Recognition
- [ ] Camera preview displays
- [ ] WebSocket connects to backend
- [ ] Familiar faces are recognized
- [ ] Names are displayed correctly

#### Text Detection (OCR)
- [ ] Camera preview displays
- [ ] Text is detected in camera feed
- [ ] Detected text is read aloud
- [ ] Stability filtering works (no flickering)

#### Emergency Contact
- [ ] Can add emergency contacts
- [ ] Emergency alert sends SMS
- [ ] GPS location is captured
- [ ] Photo is uploaded to S3

#### Voice Commands
- [ ] Wake word detection works
- [ ] Voice transcription is accurate
- [ ] Features activate correctly from voice commands
- [ ] Session ID is maintained across activities

### Unit Testing

The project includes unit tests in `app/src/test/java/`:

- Test helper classes (LocationHelper, VoiceCommandHelper, etc.)
- Test model classes
- Test utility functions

**Run tests:**
```bash
# In Android Studio
Right-click on test folder > Run 'Tests in 'test''
```

Or via Gradle:
```bash
./gradlew test
```

---

## Configuration

### Backend URL Configuration

Backend URLs must be configured in Java source files before building. See [Build, Install & Configuration](#build-install--configuration) section for detailed setup instructions.

**Key Files to Update:**
- `VoiceCommandHelper.java` - Voice command API endpoint
- `NavigateActivity.java` - Location and navigation WebSocket URLs
- `MainActivity.java`, `ReadTextActivity.java`, etc. - Feature-specific WebSocket URLs

**URL Format:**
- HTTP endpoints: `http://<your-ip>:8000/...`
- WebSocket endpoints: `ws://<your-ip>:8000/...`
- For USB ADB port forwarding: Use `localhost` instead of IP address

### Build Configuration

**SDK Versions:**
- Minimum SDK: API level 24 (Android 7.0)
- Target SDK: API level 36 (Android 15)
- Compile SDK: API level 36

**Package Name:** `com.example.newsight`

**Gradle Configuration:**
- Build tool: Gradle 8.0+
- Java version: 11
- Kotlin DSL for build scripts

### Runtime Permissions

All required permissions are declared in `AndroidManifest.xml` and requested at runtime:
- `CAMERA` - Camera-based features
- `ACCESS_FINE_LOCATION` - GPS navigation
- `ACCESS_COARSE_LOCATION` - Location services
- `RECORD_AUDIO` - Voice commands
- `INTERNET` - Network communication
- `VIBRATE` - Haptic feedback

See [Requirements - Android Permissions](#android-permissions) for detailed permission usage.

---

## Known Issues

### Main App Issues

**1. WebSocket Connection Timeout**
- **Issue:** WebSocket connections may timeout after 5 minutes of inactivity
- **Workaround:** App sends periodic ping messages to keep connection alive
- **Severity:** Low - handled by reconnection logic

**2. GPS Accuracy in Emulators**
- **Issue:** Android emulators don't provide accurate GPS data
- **Workaround:** Use physical device for GPS testing
- **Severity:** Medium - affects development workflow

**3. Camera Permission on First Launch**
- **Issue:** Camera permission must be granted before camera features work
- **Workaround:** Grant permission when prompted on first launch
- **Severity:** Low - expected behavior

**4. Voice Command Background Processing**
- **Issue:** Voice commands may not work when app is in background
- **Workaround:** Keep app in foreground for voice commands
- **Severity:** Medium - affects hands-free operation

**5. TensorFlow Lite Model Loading**
- **Issue:** First launch may take longer to load object detection model
- **Workaround:** Model loads once and is cached
- **Severity:** Low - only affects first launch

### asl-frontend Issues

**1. Hand Tracking Lighting Requirements**
- **Issue:** MediaPipe hand tracking requires good lighting conditions
- **Workaround:** Users should ensure adequate lighting for best results
- **Severity:** Medium - affects detection accuracy

**2. Separate App Installation**
- **Issue:** ASL detection requires separate app installation
- **Workaround:** Install asl-frontend app separately
- **Severity:** Low - by design (separate deployment)

### color-cue Issues

**1. Separate App Installation**
- **Issue:** Color-cue requires separate app installation
- **Workaround:** Install color-cue app separately
- **Severity:** Low - by design (separate deployment)

**2. Backend Dependency**
- **Issue:** Color-cue app requires color-cue backend running on Port 8002
- **Workaround:** Ensure color-cue backend is running before using app
- **Severity:** Medium - app won't work without backend

### General Issues

**1. Network Configuration**
- **Issue:** Backend URLs must be manually configured for each device/network
- **Workaround:** Use ADB port forwarding for development, or configure IP addresses
- **Severity:** Low - development workflow consideration

**2. Multiple Backend Connections**
- **Issue:** Main app connects to multiple backend endpoints
- **Workaround:** Ensure all required backend services are running
- **Severity:** Low - expected behavior

**3. Battery Usage**
- **Issue:** Continuous GPS and camera usage drains battery quickly
- **Workaround:** Optimize usage patterns, reduce GPS update frequency when not navigating
- **Severity:** Medium - affects user experience

---

## Future Work

The following improvements are planned for future versions:

**Integration**
- Merge asl-frontend into main unified app
- Merge color-cue into main unified app
- Single app deployment with all features
- Unified backend connection management

**Performance**
- Optimize TensorFlow Lite model loading
- Reduce battery consumption for GPS and camera
- Implement background processing for voice commands
- Cache frequently used data

**Features**
- Offline mode with cached models
- Multi-language support for OCR and voice
- Indoor navigation support
- Word and phrase detection for ASL
- Wearable device integration
- Haptic feedback patterns for navigation

**UI/UX**
- Improved accessibility features
- Customizable voice command wake words
- Theme customization (light/dark mode)
- Gesture controls for common actions
- Improved AR overlay design

**Infrastructure**
- Automated backend URL configuration
- Environment-based configuration (dev/staging/prod)
- App signing and release builds
- Google Play Store deployment
- Analytics and crash reporting

---

## Architecture

```
┌─────────────────────────────────────────────┐
│         Android Frontend Apps               │
│    (Camera, Mic, GPS, UI)                   │
│                                             │
│  ┌──────────┐  ┌─────────┐  ┌──────────┐  │
│  │   Main   │  │   ASL   │  │  color-  │  │
│  │   App    │  │  App    │  │   cue    │  │
│  └────┬─────┘  └────┬────┘  └────┬─────┘  │
└───────┼─────────────┼────────────┼────────┘
        │             │            │
        v             v            v
    ┌──────────┐  ┌─────────┐  ┌──────────┐
    │  Main    │  │   ASL   │  │  color-  │
    │ Backend  │  │ Backend │  │   cue    │
    │ :8000    │  │ :8001   │  │ :8002    │
    └────┬─────┘  └────┬────┘  └────┬─────┘
         │            │            │
         └────────────┴────────────┘
                       │
           ┌───────────┴───────────┐
           │                       │
           v                       v
    ┌─────────────┐         ┌─────────┐
    │ PostgreSQL  │         │  AWS S3 │
    └─────────────┘         └─────────┘
```

**Communication Flow:**
- **HTTP/HTTPS:** Voice commands, API calls, file uploads
- **WebSocket:** Real-time location tracking, navigation updates, face recognition, text detection
- **OkHttp:** HTTP client library for all network requests
- **Gson:** JSON serialization/deserialization

---

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [TensorFlow Lite Documentation](https://www.tensorflow.org/lite)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Material Design Components](https://material.io/components)
- [Backend README](../CIS4398-Project-NewSight-Backend/README.md) - For backend setup and configuration

---

**Project NewSight** - See Beyond Limits With The Help Of AI

Developed by NewSight Team
