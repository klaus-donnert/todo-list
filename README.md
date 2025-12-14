# MyFirstApp

A simple task list Android application built with Kotlin and Jetpack Compose.

## Features

- Add new tasks
- Mark tasks as completed or incomplete
- Delete tasks
- Reorder incomplete tasks using drag-and-drop
- Persistent storage using SharedPreferences
- Dark theme UI

## Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- Android SDK with API level 21 or higher

### Installation

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Build and run the app on an emulator or physical device.

### Usage

- Enter a task in the text field and tap "Add Task" to add it to the list.
- Tap the checkbox to mark a task as completed (moves it below the divider).
- Long-press and drag the drag handle (six dots) to reorder incomplete tasks.
- Tap the delete icon to remove a task.

## Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Serialization**: Kotlinx.serialization for JSON storage
- **Persistence**: SharedPreferences

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
