# Play Music Free

Reproductor de música local minimalista para Android, construido con Kotlin y Jetpack Compose.

## Características

- **Reproducción local** — MP3, FLAC, WAV, OGG, AAC, M4A
- **Escaneo configurable** — Selección de carpetas y filtro de duración mínima
- **Playlists** — Crear, eliminar y gestionar listas de reproducción
- **Background playback** — Sigue reproduciendo con la pantalla apagada
- **Controles en notificación y lock screen** — Via MediaSession
- **Controles completos** — Play, pause, skip, previous, seek, shuffle, repeat
- **Carátulas de álbum** — Extrae y muestra artwork embebido
- **Tema oscuro** — Diseño minimalista con acento violeta

## Screenshots

_Pendiente_

## Requisitos

- Android 13 (API 33) o superior
- Android Studio Hedgehog o superior

## Setup

```bash
git clone https://github.com/llOrtegall/Jarvis-Prieto.git
```

1. Abrir el proyecto en Android Studio
2. Sync Gradle
3. Conectar dispositivo Android con depuración USB
4. Run

## Stack técnico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Reproducción | Media3 / ExoPlayer |
| Notificaciones | MediaSession |
| Base de datos | Room |
| Imágenes | Coil |
| Navegación | Navigation Compose |
| Min SDK | 33 (Android 13) |
| Target SDK | 35 |

## Estructura del proyecto

```
app/src/main/java/com/playmusicfree/app/
├── data/
│   ├── local/          # MediaScanner, Room DB, ScanPreferences
│   ├── model/          # Song, Playlist
│   └── repository/     # MusicRepository
├── player/             # MusicService (ExoPlayer), PlayerViewModel
├── ui/
│   ├── components/     # MiniPlayer, SongItem
│   ├── screens/        # Home, Player, Playlist, PlaylistDetail, Settings
│   └── theme/          # Color, Type, Theme (dark)
├── MainActivity.kt
├── PlayMusicFreeApp.kt
└── PlayMusicFreeNavHost.kt
```

## Documentación

La documentación detallada se encuentra en la carpeta [`docs/`](docs/):

- [Arquitectura](docs/architecture.md)
- [Reproducción y Media3](docs/playback.md)
- [Escaneo de música](docs/scanning.md)
- [Playlists y base de datos](docs/database.md)
- [Interfaz de usuario](docs/ui.md)
- [Guía de contribución](docs/contributing.md)

## Licencia

Proyecto de uso personal.
