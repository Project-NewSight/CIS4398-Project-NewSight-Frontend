# NewSight - Android Object Detection Application

## Overview

NewSight is an Android application designed to assist visually impaired users by leveraging the device's camera for real-time environmental understanding. The core feature is live object detection, which identifies objects in the camera's view and displays labeled bounding boxes, providing crucial information about the user's surroundings.

## Features

- **Real-Time Object Detection**: Utilizes an on-device TensorFlow Lite model to detect objects in a live camera stream.
- **Customizable Overlay**: Draws clean, high-contrast bounding boxes and labels over the camera feed for clear visual feedback.
- **CameraX Integration**: Built with modern Android CameraX libraries for a robust and efficient camera implementation.
- **Landscape-Locked View**: The detection screen is locked to landscape mode to ensure a consistent and stable user experience.
- **Modular Architecture**: Features are separated into distinct activities (e.g., `MainActivity` for login, `NavigateActivity` for navigation, `ObstacleActivity` for detection) for better code management.

## Core Components

- **`ObstacleActivity.java`**: The main screen for the object detection feature. It handles the camera lifecycle and displays the live feed.
- **`DetectorProcessor.java`**: A custom `ImageAnalysis.Analyzer` that processes frames from the camera. It performs the following key tasks:
  - Rotates the camera image to the correct orientation.
  - Runs the TensorFlow Lite object detection model (`efficientdet-lite0.tflite`) on each frame.
  - Passes the detection results to the `OverlayView` for rendering.
- **`OverlayView.java`**: A custom `View` that sits on top of the camera preview. It is responsible for drawing the bounding boxes and labels for each detected object, scaling them correctly to match the preview image.

## Technologies Used

- **Android SDK (Java)**: The core platform for the application.
- **CameraX**: The modern Android Jetpack library for camera development, simplifying camera access and management.
- **TensorFlow Lite**: An on-device machine learning framework for running inference with low latency.
- **EfficientDet-Lite0**: A lightweight, efficient object detection model optimized for mobile devices.

## Setup and Build

To build and run this project:

1.  Clone the repository to your local machine.
2.  Open the project in the latest version of Android Studio.
3.  **Crucial Step**: Ensure you have the TensorFlow Lite model file (`efficientdet-lite0.tflite`) placed in the correct directory: `app/src/main/assets/`.
4.  Perform a Gradle Sync to ensure all dependencies are downloaded correctly.
5.  Build and run the application on a physical Android device or an emulator.
