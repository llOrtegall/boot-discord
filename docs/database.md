# Playlists y Base de Datos

## Room Database

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/local/MusicDatabase.kt`

Base de datos SQLite gestionada por Room con una sola tabla:

```
Database: play_music_free.db
Version: 1
Tables: playlists
```

## Modelo: Playlist

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/model/Playlist.kt`

```kotlin
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val songIds: String = ""  // IDs separados por comas
)
```

### Estrategia de almacenamiento de canciones

Las canciones NO se almacenan en la base de datos — se consultan desde MediaStore en cada carga. Las playlists solo almacenan los **IDs** de las canciones como un string CSV:

```
songIds = "142,87,203,15"
```

**Ventajas:**
- Schema simple, una sola tabla
- No hay duplicación de metadata
- Los datos de canciones siempre están actualizados (vienen de MediaStore)

**Métodos helper en Playlist:**
- `getSongIdList()` — Convierte el CSV a `List<Long>`
- `withSongId(id)` — Retorna copia con el ID agregado
- `withoutSongId(id)` — Retorna copia sin el ID

## PlaylistDao

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/local/PlaylistDao.kt`

| Operación | Método | Retorno |
|---|---|---|
| Listar todas | `getAll()` | `Flow<List<Playlist>>` |
| Obtener por ID | `getById(id)` | `Playlist?` |
| Crear | `insert(playlist)` | `Long` (ID generado) |
| Actualizar | `update(playlist)` | — |
| Eliminar | `delete(playlist)` | — |

`getAll()` retorna un `Flow` reactivo — la UI se actualiza automáticamente cuando se modifica una playlist.

## MusicRepository

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/repository/MusicRepository.kt`

Operaciones de alto nivel sobre playlists:

```kotlin
// Crear playlist
repository.createPlaylist("Mis Favoritas")

// Agregar canción a playlist
repository.addSongToPlaylist(playlistId = 1, songId = 142)

// Quitar canción de playlist
repository.removeSongFromPlaylist(playlistId = 1, songId = 142)

// Eliminar playlist
repository.deletePlaylist(playlist)
```

## Modelo: Song

**Archivo:** `app/src/main/java/com/playmusicfree/app/data/model/Song.kt`

```kotlin
data class Song(
    val id: Long,          // MediaStore ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,    // milisegundos
    val uri: Uri,          // content:// URI para reproducción
    val albumArtUri: Uri?  // URI de la carátula del álbum
)
```

`Song` es un data class puro (no es Entity de Room). Se construye desde las consultas a MediaStore y vive solo en memoria.
