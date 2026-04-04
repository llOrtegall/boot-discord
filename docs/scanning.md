# Escaneo de música

## Cómo funciona

El escaneo de música se basa en `MediaStore`, la base de datos de medios del sistema Android. No se escanean archivos manualmente — se consulta lo que Android ya tiene indexado.

### MediaScanner

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/local/MediaScanner.kt`

#### scanSongs()

Consulta `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` con los siguientes campos:

| Campo | Uso |
|---|---|
| `_ID` | Identificador único, se usa para construir la URI de contenido |
| `TITLE` | Título de la canción |
| `ARTIST` | Artista |
| `ALBUM` | Álbum |
| `ALBUM_ID` | Se usa para construir la URI de la carátula |
| `DURATION` | Duración en milisegundos |
| `DATA` | Ruta absoluta del archivo (para determinar la carpeta) |

**Filtro base:** `IS_MUSIC != 0` — solo archivos marcados como música por Android.

**Filtros configurables:**

- **Duración mínima:** se descartan archivos con duración menor a `minDurationSeconds * 1000` ms. Útil para filtrar tonos de llamada, notificaciones y audios de WhatsApp.
- **Carpetas excluidas:** se descartan archivos cuya carpeta padre coincida con alguna de las carpetas excluidas por el usuario.

#### getAvailableFolders()

Consulta todas las rutas de archivos de audio y extrae las carpetas únicas:

```kotlin
// Ejemplo de carpetas típicas:
/storage/emulated/0/Music
/storage/emulated/0/Download
/storage/emulated/0/WhatsApp/Media/WhatsApp Audio
/storage/emulated/0/Telegram/Telegram Audio
```

El usuario puede excluir carpetas no deseadas (WhatsApp, Telegram, etc.) desde la pantalla de ajustes.

### ScanPreferences

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/local/ScanPreferences.kt`

Wrapper de `SharedPreferences` que persiste:

| Clave | Tipo | Default | Descripción |
|---|---|---|---|
| `min_duration_seconds` | `Int` | `0` | Duración mínima en segundos (0 = sin filtro) |
| `excluded_folders` | `Set<String>` | `emptySet()` | Rutas de carpetas excluidas |

### Flujo de configuración

1. Usuario abre Settings (icono de engranaje)
2. Selecciona duración mínima: "All", "> 1 min", "> 2 min"
3. Desmarca carpetas que no quiere escanear
4. Los cambios se guardan inmediatamente en `SharedPreferences`
5. Se ejecuta `loadSongs()` automáticamente al cambiar cualquier preferencia
6. La lista de canciones se actualiza en la UI

### Permisos

En Android 13+ se requiere:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

El permiso se solicita al iniciar la app en `MainActivity`. Si el usuario lo deniega, el escaneo retornará una lista vacía y la pantalla mostrará "No songs found".
