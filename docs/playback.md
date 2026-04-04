# Reproducción y Media3

## Componentes clave

### MusicService

**Archivo:** `app/src/main/java/com/playmusicfree/app/player/MusicService.kt`

Es un `MediaSessionService` que se ejecuta como foreground service. Contiene:

- **ExoPlayer** — motor de reproducción que soporta MP3, FLAC, WAV, OGG, AAC, M4A de forma nativa
- **MediaSession** — publica los controles de reproducción al sistema (notificación, lock screen, Bluetooth)

#### Configuración del ExoPlayer

```kotlin
ExoPlayer.Builder(this)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build(),
        true  // maneja audio focus automáticamente
    )
    .setHandleAudioBecomingNoisy(true)  // pausa al desconectar auriculares
    .build()
```

- **Audio Focus:** el parámetro `true` en `setAudioAttributes` indica que ExoPlayer gestiona el audio focus automáticamente (pausa cuando otra app reproduce, resume después)
- **Audio Becoming Noisy:** pausa automáticamente cuando se desconectan los auriculares

#### Ciclo de vida

- `onCreate()` — Crea ExoPlayer y MediaSession
- `onGetSession()` — Retorna la sesión al sistema y a los controllers
- `onTaskRemoved()` — Si no hay reproducción activa, detiene el servicio
- `onDestroy()` — Libera player y sesión

### MediaController (en MainActivity)

La actividad se conecta al servicio via `MediaController`:

```kotlin
val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
```

El controller se crea en `onStart()` y se libera en `onStop()`. Se usa `mutableStateOf` para que Compose detecte cuando el controller está disponible.

### PlayerViewModel

El ViewModel actúa como puente entre la UI y el MediaController:

1. Recibe el `MediaController` via `setMediaController()`
2. Registra un `Player.Listener` para observar cambios de estado
3. Expone el estado via `StateFlow`s que Compose observa
4. Traduce acciones de la UI a comandos del player

## Formatos soportados

ExoPlayer soporta de forma nativa sin decodificadores adicionales:

| Formato | Extensiones |
|---|---|
| MPEG Audio | .mp3 |
| FLAC | .flac |
| WAV | .wav |
| Vorbis | .ogg |
| AAC | .aac, .m4a |
| Opus | .opus |

## Reproducción en background

El servicio se declara en el `AndroidManifest.xml` como:

```xml
<service
    android:name=".player.MusicService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

- `foregroundServiceType="mediaPlayback"` — requerido en Android 14+ para servicios que reproducen audio
- El intent-filter permite que Media3 descubra el servicio automáticamente

## Permisos requeridos

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

## Cola de reproducción

Cuando el usuario toca una canción, el ViewModel:

1. Construye `MediaItem`s para toda la cola (lista actual de canciones o playlist)
2. Establece el índice de inicio en la canción seleccionada
3. Llama a `controller.setMediaItems(items, startIndex, 0L)`
4. Llama a `controller.prepare()` y `controller.play()`

Esto permite que skip next/previous naveguen por toda la cola.
