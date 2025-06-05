# Routines App Architecture

This document provides a visual overview of the Routines Android application architecture using Mermaid diagrams.

## Overall Architecture

```mermaid
graph
    subgraph "Presentation Layer"
        UI[MainActivity]
        Screen[AlarmSchedulerScreen]
        VM[AlarmViewModel]
    end
    
    subgraph "Domain Layer"
        Model[AlarmItem]
        Repo[AlarmRepository Interface]
    end
    
    subgraph "Data Layer"
        AndroidRepo[AndroidAlarmRepository]
        NotifMgr[AppNotificationManager]
        PermMgr[PermissionManager]
        SP[SharedPreferences]
    end
    
    subgraph "Services Layer"
        AlarmRec[AlarmReceiver]
        AlarmSvc[AlarmService]
    end
    
    subgraph "Android System"
        AM[AlarmManager]
        NS[NotificationService]
    end
    
    UI --> Screen
    Screen --> VM
    VM --> Repo
    Repo --> AndroidRepo
    AndroidRepo --> AM
    AndroidRepo --> SP
    AndroidRepo --> AlarmRec
    AlarmRec --> AlarmSvc
    AlarmSvc --> NotifMgr
    NotifMgr --> NS
    VM --> PermMgr
```

## Clean Architecture Layers

```mermaid
graph LR
    subgraph "🎨 Presentation"
        direction TB
        A1[MainActivity]
        A2[AlarmSchedulerScreen]
        A3[AlarmViewModel]
        A1 --> A2
        A2 --> A3
    end
    
    subgraph "🏛️ Domain"
        direction TB
        B1[AlarmItem]
        B2[AlarmRepository]
        B3[AlarmState]
    end
    
    subgraph "💾 Data"
        direction TB
        C1[AndroidAlarmRepository]
        C2[AppNotificationManager]
        C3[PermissionManager]
        C1 --> C2
    end
    
    subgraph "⚙️ Services"
        direction TB
        D1[AlarmReceiver]
        D2[AlarmService]
        D1 --> D2
    end
    
    A3 --> B2
    B2 --> C1
    C1 --> D1
```

## Data Flow - Adding an Alarm

```mermaid
sequenceDiagram
    participant User
    participant Screen as AlarmSchedulerScreen
    participant VM as AlarmViewModel
    participant Repo as AndroidAlarmRepository
    participant AM as AlarmManager
    participant SP as SharedPreferences
    
    User->>Screen: Click "Add Alarm Time"
    Screen->>Screen: Show TimePickerDialog
    User->>Screen: Select time and confirm
    Screen->>VM: onTimeSelectedAndAddAlarm(hour, minute)
    VM->>VM: addAlarmDirectly(hour, minute)
    VM->>Repo: scheduleAlarm(alarmItem)
    Repo->>AM: setExactAndAllowWhileIdle()
    Repo->>SP: saveAlarmToPreferences()
    Repo-->>VM: Result.success()
    VM->>VM: loadAlarms()
    VM-->>Screen: Update alarms StateFlow
    Screen-->>User: Display updated alarm list
```

## Data Flow - Alarm Execution

```mermaid
sequenceDiagram
    participant AM as AlarmManager
    participant AR as AlarmReceiver
    participant AS as AlarmService
    participant NM as AppNotificationManager
    participant NS as Android NotificationService
    
    AM->>AR: Broadcast Intent (alarm time reached)
    AR->>AR: Extract alarm data from Intent
    AR->>AS: startForegroundService()
    AS->>AS: onStartCommand()
    AS->>NM: createAlarmNotification()
    AS->>AS: startForegroundService()
    AS->>NS: Show notification
    AS->>AS: startAlarmSound()
    AS->>AS: startVibration()
    
    Note over AS: Service runs until stopped
    User->>AS: Stop alarm action
    AS->>AS: stopAlarm()
    AS->>AS: stopSelf()
```

## Component Dependencies

```mermaid
graph TD
    subgraph "UI Components"
        MainActivity --> AlarmSchedulerScreen
        AlarmSchedulerScreen --> TimePickerDialog
        AlarmSchedulerScreen --> AlarmItemCard
    end
    
    subgraph "ViewModels"
        AlarmViewModel --> AlarmRepository
        AlarmViewModel --> PermissionManager
    end
    
    subgraph "Data Management"
        AndroidAlarmRepository --> AlarmManager
        AndroidAlarmRepository --> SharedPreferences
        AndroidAlarmRepository --> AlarmReceiver
        AppNotificationManager --> NotificationManager
    end
    
    subgraph "Services"
        AlarmReceiver --> AlarmService
        AlarmService --> AppNotificationManager
        AlarmService --> MediaPlayer
        AlarmService --> Vibrator
    end
    
    AlarmSchedulerScreen --> AlarmViewModel
    AndroidAlarmRepository -.implements.-> AlarmRepository
```

## Permission Flow

```mermaid
graph TD
    App[App Launch] --> Check{Check Permissions}
    Check -->|Missing POST_NOTIFICATIONS| ReqNotif[Request Notification Permission]
    Check -->|Missing SCHEDULE_EXACT_ALARM| ReqAlarm[Open Alarm Settings]
    Check -->|All Granted| Ready[Ready to Schedule Alarms]
    
    ReqNotif --> NotifResult{Permission Result}
    NotifResult -->|Granted| CheckAlarm{Check Alarm Permission}
    NotifResult -->|Denied| Limited[Limited Functionality]
    
    CheckAlarm -->|Has Permission| Ready
    CheckAlarm -->|Missing| ReqAlarm
    
    ReqAlarm --> Ready
    Limited --> Ready
    
    Ready --> Schedule[Can Schedule Alarms]
```

## State Management

```mermaid
stateDiagram-v2
    [*] --> Loading
    Loading --> Ready: Permissions granted
    Loading --> PermissionError: Missing permissions
    
    Ready --> SchedulingAlarm: User adds alarm
    SchedulingAlarm --> Ready: Success
    SchedulingAlarm --> Error: Failed to schedule
    
    Ready --> TestingAlarm: User tests alarm
    TestingAlarm --> Ready: Test complete
    
    Ready --> TogglingAlarm: User toggles alarm
    TogglingAlarm --> Ready: Toggle complete
    
    Ready --> RemovingAlarm: User removes alarm
    RemovingAlarm --> Ready: Removal complete
    
    Error --> Ready: Error cleared
    PermissionError --> Ready: Permissions granted
```

## File Structure

```mermaid
graph TD
    Root[routines/] --> App[app/]
    App --> Src[src/main/]
    
    Src --> Java[java/com/excalibur/routines/]
    Src --> Manifest[AndroidManifest.xml]
    Src --> Res[res/]
    
    Java --> Data[data/]
    Java --> Domain[domain/]
    Java --> Presentation[presentation/]
    Java --> Services[services/]
    Java --> UI[ui/]
    Java --> MainActivity[MainActivity.kt]
    Java --> AlarmService[AlarmService.kt → services/]
    
    Data --> Managers[managers/]
    Data --> Repositories[repositories/]
    
    Managers --> NotificationManager[AppNotificationManager.kt]
    Managers --> PermissionManager[PermissionManager.kt]
    
    Repositories --> AndroidAlarmRepository[AndroidAlarmRepository.kt]
    
    Domain --> Models[models/]
    Domain --> RepoInterface[repositories/]
    
    Models --> AlarmItem[AlarmItem.kt]
    Models --> AlarmState[AlarmState.kt]
    RepoInterface --> AlarmRepository[AlarmRepository.kt]
    
    Presentation --> Screens[screens/]
    Presentation --> ViewModels[viewmodels/]
    
    Screens --> AlarmSchedulerScreen[AlarmSchedulerScreen.kt]
    ViewModels --> AlarmViewModel[AlarmViewModel.kt]
    
    Services --> AlarmReceiver[AlarmReceiver.kt]
    Services --> AlarmServiceFile[AlarmService.kt]
    
    UI --> Theme[theme/]
    Theme --> Color[Color.kt]
    Theme --> ThemeFile[Theme.kt]
    Theme --> Type[Type.kt]
```

## Key Design Patterns

### 1. **Clean Architecture**
- **Presentation Layer**: UI components and ViewModels
- **Domain Layer**: Business logic and entities
- **Data Layer**: Repository implementations and data sources

### 2. **MVVM Pattern**
- **Model**: AlarmItem, AlarmState
- **View**: Compose UI screens
- **ViewModel**: AlarmViewModel with StateFlow for reactive UI

### 3. **Repository Pattern**
- Abstract AlarmRepository interface in domain layer
- AndroidAlarmRepository implementation in data layer
- Enables easy testing and platform independence

### 4. **Observer Pattern**
- StateFlow for reactive state management
- UI automatically updates when data changes
- Unidirectional data flow

### 5. **Dependency Injection**
- Manual DI through constructor injection
- Clear separation of concerns
- Easy to test and maintain