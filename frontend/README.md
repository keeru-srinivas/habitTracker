# habit_traacker

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

lib/
├── main.dart
├── app.dart

├── core/                         # Backend-agnostic, app-wide
│   ├── config/
│   │   ├── env.dart              # API base URL
│   │   └── app_constants.dart
│   │
│   ├── network/
│   │   ├── api_client.dart       # Talks to backend
│   │   ├── interceptors.dart     # JWT, auth headers
│   │
│   ├── storage/
│   │   ├── secure_storage.dart   # Token storage
│   │   └── local_storage.dart
│   │
│   ├── routing/
│   │   └── app_router.dart
│   │
│   ├── theme/
│   │   └── app_theme.dart
│   │
│   ├── utils/
│   │   ├── date_utils.dart       # Formatting only
│   │   └── validators.dart
│   │
│   └── widgets/
│       ├── app_button.dart
│       └── app_loader.dart

├── features/

│   ├── auth/                     # Backend: Users + Auth
│   │   ├── data/
│   │   │   ├── auth_api.dart     # POST /login, /register
│   │   │   ├── auth_repository.dart
│   │   │   └── auth_dto.dart
│   │   │
│   │   ├── domain/
│   │   │   └── user.dart         # Mirrors backend User entity
│   │   │
│   │   └── presentation/
│   │       ├── bloc/
│   │       │   ├── auth_bloc.dart
│   │       │   ├── auth_event.dart
│   │       │   └── auth_state.dart
│   │       └── pages/
│   │           ├── login_page.dart
│   │           └── register_page.dart

│   ├── habits/                   # Backend: Habits + Entries
│   │   ├── data/
│   │   │   ├── habit_api.dart        # /habits endpoints
│   │   │   ├── habit_repository.dart
│   │   │   ├── habit_dto.dart
│   │   │   └── habit_entry_dto.dart  # HabitEntry mirror
│   │   │
│   │   ├── domain/
│   │   │   ├── habit.dart            # Habit entity
│   │   │   └── habit_entry.dart      # HabitEntry entity
│   │   │
│   │   └── presentation/
│   │       ├── bloc/
│   │       │   ├── habit_bloc.dart
│   │       │   ├── habit_event.dart
│   │       │   └── habit_state.dart
│   │       │
│   │       ├── pages/
│   │       │   ├── habits_page.dart
│   │       │   └── habit_detail_page.dart
│   │       │
│   │       └── widgets/
│   │           ├── habit_tile.dart
│   │           └── streak_badge.dart

├── l10n/
│   └── app_en.arb

└── di/
└── injector.dart              # Wires everything together

