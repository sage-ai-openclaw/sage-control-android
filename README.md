# Sage Control Android

App nativa de Android para Sage Control - replicando la experiencia móvil del Web UI.

## Estructura del Proyecto

```
app/src/main/java/com/sage/control/
├── data/
│   ├── api/           # Ktor client + WebSocket
│   │   └── OpenClawApi.kt
│   ├── db/            # Room database
│   │   └── Database.kt
│   ├── model/         # Data classes
│   │   └── Models.kt
│   └── repository/    # Repositorios
│       ├── SessionRepository.kt
│       └── SettingsRepository.kt
├── di/
│   └── AppModule.kt   # Hilt DI
├── ui/
│   ├── screens/       # Pantallas principales
│   │   ├── LoginScreen.kt
│   │   ├── SessionListScreen.kt
│   │   └── ChatScreen.kt
│   ├── theme/         # Colores y temas
│   │   └── Theme.kt
│   └── viewmodel/     # ViewModels
│       ├── AuthViewModel.kt
│       ├── SessionViewModel.kt
│       └── ChatViewModel.kt
├── SageControlApp.kt
└── MainActivity.kt
```

## Features Implementadas

### MVP v1.0
- [x] Login con token OpenClaw
- [x] Lista de sesiones (Recent, Archived, Trash)
- [x] Chat en tiempo real (WebSocket)
- [x] Streaming de respuestas
- [x] Indicador "Thinking"
- [x] Operaciones inline (tool calls)
- [x] Persistencia local (Room)
- [x] Theme Material You (colores Sage green)

### Próximos Features
- [ ] Model selector en input
- [ ] Voice input (integración Whisper)
- [ ] Themes de chat personalizables
- [ ] Notificaciones push
- [ ] Swipe gestures para sesiones
- [ ] Exportar chats
- [ ] Settings completos

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34
- Kotlin 1.9.22

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requiere keystore)
./gradlew assembleRelease
```

El APK se genera en:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Configuración

1. Abrir proyecto en Android Studio
2. Sync project with Gradle files
3. Run en emulador o dispositivo físico

## Conexión

Para conectar con tu instancia OpenClaw:

1. Server URL: `http://192.168.1.100:3335` (o tu IP/URL)
2. Token: Obtener del dashboard OpenClaw o config

## Arquitectura

- **MVVM** con ViewModel + StateFlow
- **Hilt** para inyección de dependencias
- **Room** para persistencia local
- **Ktor** para HTTP y WebSocket
- **Material You** (Material3) para UI

## Screenshots

*Próximamente*

## Licencia

MIT
