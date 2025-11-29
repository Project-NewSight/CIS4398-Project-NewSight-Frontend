# NewSight - Android Assistive Technology Application

## Overview

NewSight is a comprehensive Android application designed to assist visually impaired users through multiple accessible features. The app leverages the device's camera, GPS, voice recognition, and real-time communication to provide environmental awareness, navigation assistance, emergency support, and social connectivity - all through hands-free, voice-activated interactions.

## Features

### 1. Voice-Activated Navigation
- **Hands-Free Operation**: Say "Hey Guide, nearest CVS" or "Give me directions to Starbucks" from any screen
- **AR-Style Visual Overlay**: Real-time camera feed with directional arrows, distance, and turn instructions overlaid
- **Turn-by-Turn Voice Guidance**: Announces upcoming turns with distance (e.g., "In 50 feet, turn right on Main Street")
- **Smart Place Recognition**: Understands generic places (CVS, Starbucks, bus stops) and finds the nearest location
- **Real-Time GPS Tracking**: Continuous location monitoring for accurate positioning and step advancement
- **Automatic Navigation Start**: Voice command from any activity automatically launches NavigateActivity with navigation ready
- **Google Maps Integration**: Walking directions powered by Google Maps Directions, Places, and Geocoding APIs
- **Proximity-Based Announcements**: Voice alerts at 100m, 50 feet, and 25 feet before turns

### 2. Voice Commands ("Hey Guide")
- **Wake Word Detection**: Always-listening for "Hey Guide" activation phrase
- **Multi-Feature Control**: Voice activation for all app features (navigation, detection, emergency, etc.)
- **Natural Language Processing**: Groq Whisper STT for accurate speech recognition
- **Intelligent Feature Routing**: AI-powered feature detection using Groq LLaMa LLM
- **Session-Based Tracking**: Persistent session IDs for location tracking across activities

### 3. Real-Time Object Detection
- **Live Camera Feed**: On-device TensorFlow Lite model for instant object recognition
- **Bounding Box Overlay**: High-contrast visual feedback with labeled objects
- **EfficientDet-Lite0 Model**: Optimized for mobile performance
- **Obstacle Detection**: Identifies obstacles in the user's path
- **CameraX Integration**: Modern camera implementation for reliability

### 4. Familiar Face Recognition
- **Real-Time Face Detection**: Identifies familiar contacts through camera feed
- **DeepFace Integration**: Advanced face recognition with configurable models (VGG-Face, Facenet, ArcFace)
- **WebSocket Communication**: Real-time face matching with backend
- **Gallery Management**: Cloud-stored face database via AWS S3
- **Confidence Scoring**: Distance thresholds for accurate matching

### 5. Emergency Contact System
- **Quick Access**: Emergency alert activation with location and photo capture
- **GPS Location Sharing**: Automatically sends current location to trusted contacts
- **Photo Documentation**: Captures and uploads emergency photo to AWS S3
- **SMS Alerts**: Sends emergency messages via Vonage API
- **Contact Management**: Add, view, and delete trusted emergency contacts

### 6. Additional Features
- **Home Dashboard**: Central navigation hub with large, accessible buttons
- **Text Detection**: OCR for reading text in the environment
- **Color Identification**: Identifies colors in the camera view
- **ASL Detection**: American Sign Language recognition
- **Modular Architecture**: Features separated into distinct activities for maintainability

## Core Components

### Main Activities

- **`HomeActivity.java`**: 
  - Central dashboard with navigation to all features
  - Voice command activation with "Hey Guide" wake word
  - Location tracking initialization for navigation
  - Session ID management for WebSocket connections

- **`NavigateActivity.java`**: 
  - AR-style navigation interface with CameraX integration
  - Real-time turn-by-turn navigation display
  - Voice command processing for destination input
  - GPS tracking and WebSocket communication for live updates
  - Visual overlay with distance, arrows, and instructions
  - Text-to-speech for voice announcements

- **`ObstacleActivity.java`**: 
  - Real-time object detection screen
  - Camera lifecycle management
  - TensorFlow Lite model integration
  - Bounding box overlay rendering

- **`CommunicateActivity.java`**: 
  - Communication features hub
  - Voice command integration
  - ASL detection entry point
  - Location tracking for navigation requests

- **`ObserveActivity.java`**: 
  - Observation features hub (text detection, color cue, facial recognition)
  - Voice command integration
  - Location tracking for navigation requests

- **`EmergencyActivity.java`**: 
  - Emergency contact management
  - Quick emergency alert activation
  - GPS and photo capture integration

### Helper Classes

- **`VoiceCommandHelper.java`**: 
  - Audio recording and playback
  - Wake word detection
  - Speech-to-text via backend API
  - Session ID management for navigation
  - OkHttp integration for audio file upload

- **`LocationHelper.java`**: 
  - FusedLocationProviderClient management
  - GPS updates every 2 seconds
  - Location permission handling
  - Callback interface for location updates

- **`LocationWebSocketHelper.java`**: 
  - WebSocket connection to `/location/ws` endpoint
  - Continuous GPS data streaming to backend
  - Session-based location tracking
  - Auto-reconnection on disconnection

- **`NavigationHelper.java`**: 
  - WebSocket connection to `/navigation/ws` endpoint
  - Real-time turn-by-turn update receiver
  - Navigation state management
  - Callback interface for UI updates

- **`TtsHelper.java`**: 
  - Text-to-speech for voice announcements
  - Android TextToSpeech engine wrapper
  - Navigation instruction announcements

### Data Models

- **`DirectionsResponse.java`**: 
  - Full directions response from Google Maps API
  - Origin, destination, total distance/duration
  - List of navigation steps

- **`NavigationStep.java`**: 
  - Single turn-by-turn instruction
  - Distance, duration, start/end locations
  - HTML instruction text

- **`NavigationUpdate.java`**: 
  - Real-time navigation state update
  - Current step, distance to next turn
  - Announcement flags and messages
  - Status: navigating, step_completed, arrived

- **`VoiceResponse.java`**: 
  - Voice command API response
  - Feature identification and confidence
  - Extracted parameters (destination, query)
  - TTS output message
  - Full directions object for navigation

- **`LocationCoordinates.java`**: 
  - GPS coordinate pair (lat, lng)
  - Used in navigation steps and directions

### UI Components

- **`DetectorProcessor.java`**: 
  - Custom `ImageAnalysis.Analyzer` for object detection
  - Frame rotation and preprocessing
  - TensorFlow Lite model inference
  - Results passed to OverlayView

- **`OverlayView.java`**: 
  - Custom View for detection overlay
  - Bounding box and label rendering
  - Scaling to match camera preview

## Technologies Used

### Core Android
- **Android SDK (Java)**: Primary development language and platform
- **AndroidX Libraries**: Modern Android Jetpack components
- **Material Design Components**: UI/UX components for accessibility

### Camera & Vision
- **CameraX**: Modern camera implementation for preview and image capture
- **TensorFlow Lite**: On-device ML inference for object detection
- **EfficientDet-Lite0**: Lightweight object detection model

### Location & Navigation
- **FusedLocationProviderClient**: GPS location tracking (Google Play Services Location)
- **Google Maps APIs**: Directions, Places, and Geocoding (via backend)
- **Haversine Distance Calculation**: GPS proximity detection (backend)

### Communication & Networking
- **OkHttp 5.2.1**: HTTP client for API calls and audio upload
- **WebSocket (OkHttp)**: Real-time bidirectional communication for:
  - Location tracking (`/location/ws`)
  - Navigation updates (`/navigation/ws`)
  - Face recognition (`/ws`)
- **Gson 2.10.1**: JSON parsing and serialization

### Audio & Speech
- **Android MediaRecorder**: Audio capture for voice commands
- **Android TextToSpeech**: Voice announcements for navigation
- **Android AudioManager**: Audio playback control
- **Backend Speech-to-Text**: Groq Whisper via backend API

### UI Components
- **PreviewView**: CameraX preview surface
- **RelativeLayout/FrameLayout**: AR overlay structure
- **Custom Views**: Bounding box overlay for object detection
- **ImageView**: Directional arrows (straight, left, right, slight turns)
- **TextView**: Distance, instructions, street names

### Dependencies (from build.gradle.kts)
```kotlin
// AndroidX Core
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.activity:activity:1.8.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// CameraX
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")

// Location Services
implementation("com.google.android.gms:play-services-location:21.0.1")

// Networking
implementation("com.squareup.okhttp3:okhttp:5.2.1")
implementation("com.google.code.gson:gson:2.10.1")

// TensorFlow Lite
implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
```

## Setup and Build

### Prerequisites

- **Android Studio** (latest version recommended)
- **Physical Android Device** (recommended for GPS, camera, and sensor testing)
- **Backend Server Running** (see Backend Setup below)
- **Google Maps API Key** (configured in backend)

### Backend Setup

The app requires the NewSight backend server to be running for full functionality:

1. Clone the backend repository:
   ```bash
   git clone <backend-repo-url>
   cd CIS4398-Project-NewSight-Backend
   ```

2. Set up Python virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # Mac/Linux
   venv\Scripts\activate     # Windows
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Configure `.env` file:
   ```env
   DATABASE_URL=postgresql+psycopg2://username:password@host:port/dbname
   AWS_ACCESS_KEY_ID=your-access-key
   AWS_SECRET_ACCESS_KEY=your-secret-key
   AWS_REGION=us-east-2
   AWS_S3_BUCKET_NAME=newsight-storage
   VONAGE_API_KEY=your-vonage-key
   VONAGE_API_SECRET=your-vonage-secret
   GROQ_API_KEY=your-groq-key
   GOOGLE_MAPS_API_KEY=your-google-maps-api-key
   ```

5. Start the backend server:
   ```bash
   python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

### Android App Setup

1. Clone the frontend repository:
   ```bash
   git clone <frontend-repo-url>
   cd CIS4398-Project-NewSight-Frontend
   ```

2. Open the project in Android Studio

3. **Configure Backend URLs**:
   - Update WebSocket and API URLs in the following files with your backend IP:
     - `VoiceCommandHelper.java` - Change `BACKEND_URL` to your server IP
     - `NavigateActivity.java` - Change `LOCATION_WS_URL` and `NAVIGATION_WS_URL`
     - `HomeActivity.java` - Change WebSocket URLs
     - `CommunicateActivity.java` - Change WebSocket URLs
     - `ObserveActivity.java` - Change WebSocket URLs
   
   Example:
   ```java
   private static final String BACKEND_URL = "http://192.168.1.254:8000/voice/transcribe";
   private static final String LOCATION_WS_URL = "ws://192.168.1.254:8000/location/ws";
   ```

4. **Add TensorFlow Lite Model**:
   - Place `efficientdet-lite0.tflite` in `app/src/main/assets/`

5. **Gradle Sync**:
   - Android Studio will automatically sync Gradle
   - Ensure all dependencies download successfully

6. **Connect Physical Device** (Recommended):
   - Enable Developer Options and USB Debugging on your Android device
   - Connect via USB
   - Android Studio should detect the device

7. **Build and Run**:
   - Click the Run button (green play icon)
   - Select your device
   - App will install and launch

### Network Configuration (Physical Device Testing)

If testing on a physical Android device connected to the same WiFi network:

1. Find your computer's local IP address:
   - **Windows**: `ipconfig` (look for IPv4 Address)
   - **Mac/Linux**: `ifconfig` or `ip addr` (look for inet address)

2. Ensure backend server is accessible on the network:
   ```bash
   python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

3. Test connectivity from phone:
   - Open browser on phone
   - Navigate to `http://<your-ip>:8000/docs`
   - Should see FastAPI documentation

### Network Configuration (USB ADB Port Forwarding)

Alternative to WiFi - use USB connection with ADB:

1. Connect device via USB

2. Set up port forwarding:
   ```bash
   adb devices  # Get device serial
   adb -s <device_serial> reverse tcp:8000 tcp:8000
   ```

3. Verify:
   ```bash
   adb -s <device_serial> reverse --list
   ```

4. In Android code, use `localhost`:
   ```java
   private static final String BACKEND_URL = "http://localhost:8000/voice/transcribe";
   ```

### Required Permissions

The app requests the following permissions at runtime:
- **CAMERA** - For AR navigation overlay and object detection
- **ACCESS_FINE_LOCATION** - For GPS navigation
- **ACCESS_COARSE_LOCATION** - For approximate location
- **RECORD_AUDIO** - For voice commands

Ensure these are granted when prompted for full functionality.

## Project Structure

```
CIS4398-Project-NewSight-Frontend/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/newsight/
│   │   │   │   ├── HomeActivity.java              # Main dashboard
│   │   │   │   ├── NavigateActivity.java          # AR navigation feature
│   │   │   │   ├── ObstacleActivity.java          # Object detection
│   │   │   │   ├── CommunicateActivity.java       # Communication hub
│   │   │   │   ├── ObserveActivity.java           # Observation features
│   │   │   │   ├── EmergencyActivity.java         # Emergency contacts
│   │   │   │   ├── MainActivity.java              # Login/entry
│   │   │   │   │
│   │   │   │   ├── helpers/
│   │   │   │   │   ├── VoiceCommandHelper.java    # Voice command processing
│   │   │   │   │   ├── LocationHelper.java        # GPS tracking
│   │   │   │   │   ├── LocationWebSocketHelper.java # Location streaming
│   │   │   │   │   ├── NavigationHelper.java      # Navigation updates
│   │   │   │   │   └── TtsHelper.java             # Text-to-speech
│   │   │   │   │
│   │   │   │   ├── models/
│   │   │   │   │   ├── DirectionsResponse.java    # Full directions
│   │   │   │   │   ├── NavigationStep.java        # Single step
│   │   │   │   │   ├── NavigationUpdate.java      # Real-time update
│   │   │   │   │   ├── VoiceResponse.java         # Voice API response
│   │   │   │   │   └── LocationCoordinates.java   # GPS coordinates
│   │   │   │   │
│   │   │   │   ├── DetectorProcessor.java         # Object detection processor
│   │   │   │   └── OverlayView.java              # Detection overlay
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_navigate.xml      # AR navigation UI
│   │   │   │   │   ├── activity_home.xml          # Dashboard UI
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── ic_arrow_straight.xml      # Navigation arrows
│   │   │   │   │   ├── ic_arrow_left.xml
│   │   │   │   │   ├── ic_arrow_right.xml
│   │   │   │   │   ├── ic_arrow_slight_left.xml
│   │   │   │   │   ├── ic_arrow_slight_right.xml
│   │   │   │   │   └── ar_overlay_background.xml  # Glass-morphism effect
│   │   │   │   │
│   │   │   │   └── values/
│   │   │   │       ├── strings.xml
│   │   │   │       ├── colors.xml
│   │   │   │       └── styles.xml
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   └── efficientdet-lite0.tflite     # Object detection model
│   │   │   │
│   │   │   └── AndroidManifest.xml               # Permissions & activities
│   │   │
│   │   └── build.gradle.kts                      # App dependencies
│   │
│   └── build.gradle.kts                          # Project config
│
└── README.md
```

## Using the Navigation Feature

### Quick Start

1. **Launch the app** → Opens HomeActivity

2. **Wait for initialization** (2-3 seconds):
   - Location WebSocket connects
   - GPS acquires position
   - Voice command system ready

3. **Say the wake word**: "Hey Guide"

4. **State your destination**:
   - Generic places: "nearest CVS", "Starbucks", "bus stop"
   - Specific places: "Temple University", "City Hall Philadelphia"
   - Addresses: "1801 N Broad St, Philadelphia"

5. **NavigateActivity launches automatically** with:
   - Camera feed for AR overlay
   - Turn-by-turn directions already loaded
   - Voice announcement: "Starting navigation to [destination]"

### During Navigation

**Visual Display:**
- **Top**: Current street name
- **Center**: Large distance number (in feet/miles)
- **Center**: Directional arrow (straight, left, right, slight turns)
- **Bottom**: Current instruction (e.g., "Turn right on Main Street")

**Voice Announcements:**
- Announces at 100 meters (if > 0.1 mi away)
- Announces at ~50 feet before turn
- Announces at ~25 feet before turn
- Example: "In 50 feet, turn right on Main Street"

**Progress:**
- Automatically advances to next step when you reach each turn
- Updates distance in real-time as you walk
- Announces "You have arrived at your destination" when complete

**Controls:**
- **Home Button**: Return to HomeActivity (stops navigation)
- **Mic Button**: Issue new voice command (change destination, activate other features)

### Navigation from Different Activities

The navigation feature works from ANY activity:

**From HomeActivity:**
```
"Hey Guide, nearest CVS" → NavigateActivity opens with directions
```

**From CommunicateActivity:**
```
"Hey Guide, Starbucks" → NavigateActivity opens with directions
```

**From ObserveActivity:**
```
"Hey Guide, bus stop" → NavigateActivity opens with directions
```

**Already in NavigateActivity:**
```
"Hey Guide, nearest ATM" → Gets new directions, restarts navigation
```

## Testing Checklist

### Navigation Feature
- [ ] Backend server is running on network
- [ ] Google Maps API key is configured with Directions, Places, and Geocoding APIs enabled
- [ ] App has location permissions granted
- [ ] App has camera permissions granted
- [ ] App has microphone permissions granted
- [ ] GPS has acquired location (check notification bar)
- [ ] Voice command "Hey Guide" works
- [ ] Generic place search works ("nearest CVS")
- [ ] Specific address works ("123 Main St")
- [ ] AR overlay displays correctly (distance, arrow, instruction)
- [ ] Voice announcements work at proper distances
- [ ] Navigation advances steps automatically
- [ ] "You have arrived" announcement at destination

### Object Detection
- [ ] Camera preview displays
- [ ] Bounding boxes appear around detected objects
- [ ] Labels are readable and correctly positioned

### Emergency Contact
- [ ] Can add emergency contacts
- [ ] Emergency alert sends SMS
- [ ] GPS location is captured
- [ ] Photo is uploaded to S3

### Voice Commands
- [ ] Wake word detection works
- [ ] Voice transcription is accurate
- [ ] Features activate correctly from voice commands

## Troubleshooting

### Navigation Issues

**"No directions found" or "Location not available"**
- **Cause**: GPS not ready or location WebSocket not connected
- **Fix**: Wait 3-5 seconds after opening app for GPS to acquire position
- Check location services are enabled on device
- Verify backend logs show "Location updated for [session-id]"

**Navigation doesn't start after voice command**
- **Cause**: Session ID not sent or backend not returning directions
- **Fix**: Check backend logs for "🔍 Checking navigation: feature=NAVIGATION, x_session_id="
- Verify backend server is accessible from phone
- Restart app and wait for location to initialize

**App crashes when navigation starts**
- **Cause**: Null values in navigation update (should be fixed in latest code)
- **Fix**: Ensure you have latest NavigateActivity.java with null checks
- Check Android logs for specific error

**Voice announcements not working**
- **Cause**: TextToSpeech not initialized or volume muted
- **Fix**: Check device volume is up
- Verify microphone permission is granted
- Restart app

**Generic places not found (e.g., "nearest CVS")**
- **Cause**: Too far from any matching location (10km search radius)
- **Fix**: Try being more specific: "CVS Pharmacy on Main Street"
- Check backend logs for "identified as generic place"
- Verify Google Places API is enabled

**AR overlay not showing**
- **Cause**: Camera permission denied or navigation not started
- **Fix**: Grant camera permission when prompted
- Check that directions were received from backend
- Verify AR overlay visibility in logs

### Voice Command Issues

**"Hey Guide" not detected**
- **Cause**: Microphone permission denied or noisy environment
- **Fix**: Grant microphone permission
- Speak clearly and closer to device
- Check device microphone is working in other apps

**Transcription is inaccurate**
- **Cause**: Background noise or unclear speech
- **Fix**: Speak slowly and clearly
- Try quieter environment
- Check backend Groq API key is valid

### Connection Issues

**"Cannot connect to server"**
- **Cause**: Backend server not running or wrong IP address
- **Fix**: Verify backend is running: `http://<ip>:8000/docs`
- Check IP address in VoiceCommandHelper.java matches your server
- If using ADB, verify port forwarding: `adb reverse --list`
- Ensure phone and computer are on same WiFi network

**WebSocket disconnects frequently**
- **Cause**: Network instability or backend timeout
- **Fix**: Use USB with ADB port forwarding for stable connection
- Check WiFi signal strength
- Backend has auto-reconnect - wait a few seconds

### Object Detection Issues

**No bounding boxes appear**
- **Cause**: TensorFlow Lite model missing
- **Fix**: Verify `efficientdet-lite0.tflite` is in `app/src/main/assets/`
- Rebuild app after adding model file

**Camera preview is rotated**
- **Cause**: Orientation handling issue
- **Fix**: This should be handled in DetectorProcessor.java
- Ensure device auto-rotate is enabled

## Development Notes

### Adding New Features

To add a new feature to the app:

1. **Create Activity**: Add new activity in `java/com/example/newsight/`
2. **Create Layout**: Add XML layout in `res/layout/`
3. **Update Manifest**: Declare activity in AndroidManifest.xml
4. **Add Navigation**: Add button in HomeActivity to launch new feature
5. **Voice Integration**: Update backend voice_agent.py to recognize feature
6. **Test**: Build and test on physical device

### Backend Communication

All backend communication uses:
- **OkHttp** for HTTP requests (voice transcription, API calls)
- **WebSocket** for real-time updates (location, navigation, face recognition)
- **Gson** for JSON parsing

URLs should be configured at the top of each helper/activity file for easy modification.

### Session Management

- **Session ID**: UUID generated on activity creation
- **Purpose**: Links location tracking with navigation requests
- **Lifetime**: Persists for activity lifecycle
- **Cleanup**: WebSockets disconnected in onDestroy()

## Future Enhancements

Potential improvements for the app:

### Navigation
- **Offline Mode**: Cache map tiles for areas without internet
- **Route Preferences**: Avoid stairs, prefer well-lit paths, accessible routes
- **Transit Integration**: Public transportation directions
- **Indoor Navigation**: Building-level navigation
- **Haptic Feedback**: Vibration patterns for turns

### Accessibility
- **Voice-Only Mode**: Complete navigation without looking at screen
- **Customizable TTS**: Different voices and speeds
- **Sound Cues**: Non-verbal audio feedback for turns
- **Gesture Control**: Swipe gestures for common actions

### Features
- **Obstacle Avoidance**: Integrate obstacle detection with navigation
- **Journey Sharing**: Share live location with emergency contacts during navigation
- **Historical Routes**: Save frequently visited destinations
- **Weather Integration**: Weather-aware route planning
- **Battery Optimization**: Reduce GPS frequency when stationary

## Contributors

This app is part of the NewSight capstone project for Temple University CIS 4398.

## License

[Specify license here]
