# MyedenFocus Android Application

MyedenFocus is a productivity app designed to enhance academic performance through focused study sessions, meditation, and task management. Built with modern Android development practices using Jetpack Compose.

<a href="https://play.google.com/store/apps/details?id=com.gorai.myedenfocus">
  <img src="https://github.com/user-attachments/assets/bdc18fc5-5a99-410c-b383-eaf9c737176e" alt="play_download" width="200"/>
</a>


![Screenshot 2025-01-17 222010 - Copy](https://github.com/user-attachments/assets/8c6b43bf-119d-4300-be75-af1f9140555c)

## Features

- **Study Session Timer**: Track study sessions with a customizable timer
- **Meditation Timer**: Built-in meditation timer with background music options
- **Task Management**: Schedule and track academic tasks with notifications
- **Subject Management**: Organize study sessions by subjects
- **Progress Tracking**: Monitor study hours and meditation sessions

## Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Navigation**: Compose Destinations
- **Background Processing**: Kotlin Coroutines
- **Local Storage**: Room Database
- **Services**: Foreground Services for timers
- **Notifications**: NotificationCompat for Android 8.0+

## Prerequisites

- Android Studio Arctic Fox or newer
- JDK 11 or higher
- Android SDK 33 (minimum SDK 24)
- Gradle 7.0 or higher

## Installation (Only for Contributions)

1. Clone the repository
   ```sh
   git clone https://github.com/sbhjt-gr/myedenfocus-kotlin/
   ```
2. Open in Android Studio
3. Sync Gradle files
4. Run on an emulator or physical device

## Project Structure

- **app/src/main/**
  - **java/com/gorai/myedenfocus/**
    - **data/**: Data layer with repositories and local storage
    - **domain/**: Business logic and models
    - **presentation/**: UI components and ViewModels
    - **service/**: Background services for timers
    - **util/**: Helper classes and extensions
  - **res/**: Resources (layouts, drawables, values)
  - **AndroidManifest.xml**: App configuration

## Contributing

Contributions are not needed for now but will be accepted in the future as the application is still under development by me.

## License

Distributed under a custom license. See `LICENSE` for more information.

## Contact

For inquiries and issues, please contact:
Subhajit Gorai - [sg_outlp@outlook.com](mailto:sg_outlp@outlook.com)
