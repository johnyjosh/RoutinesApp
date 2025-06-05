# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Clean and build
./gradlew clean build

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Install debug APK
./gradlew installDebug

# Lint checking
./gradlew lint

# Build release APK
./gradlew assembleRelease
```

## Project Architecture

This is a multi-module Android project using Clean Architecture with MVVM pattern:

- **App Module** (`:app`): Main application with alarm scheduling functionality
- **Feature Module** (`:feature_alarms`): Modularized alarm features (currently minimal)

### Package Structure
- `com.excalibur.routines` - Main application package
- `com.excalibur.feature_alarms` - Feature module package

### Architecture Layers
- **Domain**: Business logic (`models/`, `repositories/` interfaces)
- **Data**: Android implementations (`repositories/`, `managers/`)
- **Presentation**: Compose UI (`screens/`, `viewmodels/`)

## Technology Stack

- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture + MVVM
- **State Management**: StateFlow and ViewModels
- **Build System**: Gradle with Kotlin DSL
- **Target SDK**: 35 (Android 15), Min SDK: 24

## Key Permissions

The app requires specific Android permissions for alarm functionality:
- `SCHEDULE_EXACT_ALARM` - Precise alarm scheduling
- `POST_NOTIFICATIONS` - Notification display
- `VIBRATE` - Alarm vibration
- `FOREGROUND_SERVICE` - Background alarm service

## Development Notes

- Manual dependency injection (no DI framework)
- SharedPreferences for alarm persistence
- AlarmManager integration for system-level alarms
- Notification management through custom NotificationManager