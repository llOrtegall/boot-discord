# Interfaz de Usuario

## Stack UI

- **Jetpack Compose** — UI declarativa
- **Material 3** — Sistema de diseño
- **Navigation Compose** — Navegación entre pantallas
- **Coil** — Carga de imágenes (carátulas de álbum)

## Tema

**Archivos:** `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`

Tema oscuro fijo con los siguientes colores clave:

| Color | Hex | Uso |
|---|---|---|
| Surface | `#121212` | Fondo principal |
| SurfaceVariant | `#1E1E1E` | Mini player, navigation bar |
| OnSurface | `#E0E0E0` | Texto principal |
| Accent (Primary) | `#BB86FC` | Canción activa, botones activos, FAB |

## Navegación

### Estructura

```
PlayMusicFreeNavHost
├── songs (tab)         → HomeScreen
├── playlists (tab)     → PlaylistScreen
├── playlist/{id}       → PlaylistDetailScreen
├── player              → PlayerScreen
└── settings            → SettingsScreen
```

### Tabs principales

La app tiene 2 tabs en la barra de navegación inferior:

1. **Songs** — Lista de todas las canciones del dispositivo
2. **Playlists** — Lista de playlists del usuario

### TopAppBar

- Título: "Play Music Free"
- Acción: Icono de engranaje → navega a Settings

## Pantallas

### HomeScreen

Lista vertical de canciones escaneadas. Cada item muestra:
- Carátula del álbum (48x48dp, esquinas redondeadas)
- Título (resaltado en violeta si es la canción actual)
- Artista
- Duración (formato `m:ss`)

### PlayerScreen

Pantalla completa de reproducción:
- Carátula grande (80% del ancho, aspecto 1:1)
- Título y artista
- Barra de progreso (Slider) con tiempos
- Controles: Shuffle, Previous, Play/Pause, Next, Repeat
- Iconos de shuffle y repeat cambian color cuando están activos

### PlaylistScreen

Lista de playlists con:
- Nombre y cantidad de canciones
- Botón de eliminar por playlist
- FAB para crear nueva playlist (diálogo con campo de texto)

### PlaylistDetailScreen

Similar a HomeScreen pero con:
- TopAppBar con el nombre de la playlist y botón de retroceso
- Solo las canciones de la playlist seleccionada

### SettingsScreen

Ajustes de escaneo:
- **Duración mínima** — FilterChips: "All", "> 1 min", "> 2 min"
- **Carpetas** — Lista con checkboxes. Cada item muestra el nombre de la carpeta y la ruta completa

## Componentes reutilizables

### SongItem

Elemento de lista para canciones. Recibe:
- `song: Song`
- `isPlaying: Boolean` — resalta título en violeta
- `onClick: () -> Unit`

### MiniPlayer

Barra de reproducción fija en la parte inferior (sobre el NavigationBar):
- Barra de progreso lineal (2dp)
- Carátula (42dp), título, artista
- Botones Play/Pause y Skip Next
- Tap en el body → navega a PlayerScreen

Se muestra solo cuando hay una canción actual (`currentSong != null`) y solo en las pantallas principales (tabs).

## Helpers

### formatDuration(ms: Long): String

Ubicado en `SongItem.kt`. Convierte milisegundos a formato `m:ss`:

```
180000ms → "3:00"
65000ms  → "1:05"
```
