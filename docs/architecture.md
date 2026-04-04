# Arquitectura

## Visión general

Play Music Free sigue una arquitectura MVVM (Model-View-ViewModel) con una capa de repositorio que abstrae el acceso a datos.

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  Compose Screens ← StateFlows ← ViewModel       │
├─────────────────────────────────────────────────┤
│                 Domain Layer                     │
│  PlayerViewModel                                 │
│  - Estado de reproducción                        │
│  - Operaciones de playlist                       │
│  - Configuración de escaneo                      │
├─────────────────────────────────────────────────┤
│                  Data Layer                      │
│  MusicRepository                                 │
│  ├── MediaScanner (MediaStore queries)           │
│  ├── Room DB (playlists)                         │
│  └── ScanPreferences (SharedPreferences)         │
├─────────────────────────────────────────────────┤
│               Service Layer                      │
│  MusicService (MediaSessionService)              │
│  └── ExoPlayer + MediaSession                    │
└─────────────────────────────────────────────────┘
```

## Flujo de datos

### Canciones

1. `MediaScanner` consulta `MediaStore.Audio` del sistema
2. Aplica filtros de duración mínima y carpetas excluidas
3. `MusicRepository.loadSongs()` devuelve la lista filtrada
4. `PlayerViewModel` expone las canciones via `StateFlow<List<Song>>`
5. `HomeScreen` observa el flow y renderiza la lista

### Reproducción

1. Usuario toca una canción en la UI
2. `PlayerViewModel.playSong()` construye `MediaItem`s y los envía al `MediaController`
3. `MediaController` se comunica con `MusicService` via IPC
4. `MusicService` contiene el `ExoPlayer` que reproduce el audio
5. `MediaSession` publica los controles en notificación y lock screen
6. El `Player.Listener` en el ViewModel actualiza los `StateFlow`s de estado

### Playlists

1. Las playlists se almacenan en Room (`playlists` table)
2. `PlaylistDao` expone `Flow<List<Playlist>>` para reactividad
3. Las canciones se referencian por ID (campo `songIds` como CSV)
4. Al mostrar una playlist, se filtran las canciones cargadas por sus IDs

## Dependencias entre módulos

```
MainActivity
└── PlayMusicFreeNavHost (Compose Navigation)
    └── PlayerViewModel
        ├── MusicRepository
        │   ├── MediaScanner
        │   ├── PlaylistDao (Room)
        │   └── ScanPreferences
        └── MediaController → MusicService
                              └── ExoPlayer + MediaSession
```

## Inyección de dependencias

El proyecto usa inyección manual via la clase `PlayMusicFreeApp` (Application):

- `PlayMusicFreeApp` crea e inicializa `MusicDatabase` y `ScanPreferences`
- `PlayerViewModel` accede a estos via casting de `Application` a `PlayMusicFreeApp`
- `MusicRepository` recibe sus dependencias por constructor

Esta estrategia es simple y suficiente para el tamaño actual del proyecto. Si el proyecto crece, se puede migrar a Hilt.

## Gestión de estado

Todo el estado de la app fluye a través de `PlayerViewModel`:

| Estado | Tipo | Fuente |
|---|---|---|
| `songs` | `StateFlow<List<Song>>` | MediaScanner |
| `playlists` | `StateFlow<List<Playlist>>` | Room (Flow) |
| `currentSong` | `StateFlow<Song?>` | Player.Listener |
| `isPlaying` | `StateFlow<Boolean>` | Player.Listener |
| `currentPosition` | `StateFlow<Long>` | Polling cada 500ms |
| `shuffleEnabled` | `StateFlow<Boolean>` | Player.Listener |
| `repeatMode` | `StateFlow<Int>` | Player.Listener |
| `minDurationSeconds` | `StateFlow<Int>` | ScanPreferences |
| `availableFolders` | `StateFlow<List<String>>` | MediaScanner |
| `excludedFolders` | `StateFlow<Set<String>>` | ScanPreferences |
