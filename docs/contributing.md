# Guía de contribución

## Requisitos previos

- Android Studio Hedgehog (2023.1) o superior
- JDK 17
- Dispositivo Android con API 33+ o emulador

## Configuración del entorno

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/llOrtegall/Jarvis-Prieto.git
   ```

2. Abrir en Android Studio:
   - File > Open > seleccionar la carpeta del proyecto
   - Esperar a que Gradle sincronice

3. Conectar dispositivo:
   - Activar opciones de desarrollador en el dispositivo
   - Habilitar depuración USB
   - Conectar por USB y aceptar el diálogo de depuración

4. Ejecutar:
   - Click en Run (Shift+F10) o Debug (Shift+F9)

## Estructura de paquetes

```
com.playmusicfree.app/
├── data/
│   ├── local/       → Acceso a datos del sistema y persistencia
│   ├── model/       → Data classes
│   └── repository/  → Abstracción sobre las fuentes de datos
├── player/          → Servicio de audio y ViewModel
└── ui/
    ├── components/  → Composables reutilizables
    ├── screens/     → Pantallas completas
    └── theme/       → Colores, tipografía, tema
```

## Convenciones

### Código

- Kotlin con Jetpack Compose
- Sin framework de DI — inyección manual via Application class
- State management via `StateFlow` en ViewModels
- Coroutines para operaciones asíncronas

### Commits

Los mensajes de commit siguen la convención de [Conventional Commits](https://www.conventionalcommits.org/):

```
tipo: descripción breve

Descripción detallada si es necesario.
```

**Tipos:**
- `feat:` — Nueva funcionalidad
- `fix:` — Corrección de bugs
- `chore:` — Configuración, dependencias, tooling
- `refactor:` — Reestructuración sin cambio funcional
- `docs:` — Documentación

### Archivos nuevos

- Pantallas → `ui/screens/`
- Componentes reutilizables → `ui/components/`
- Data classes → `data/model/`
- Acceso a datos → `data/local/`

## Build variants

| Variant | Uso |
|---|---|
| `debug` | Desarrollo, incluye herramientas de depuración |
| `release` | Producción, con minificación (R8) y shrinking |

Para generar un APK release:

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/`.

## Posibles mejoras futuras

- Búsqueda de canciones
- Ecualizador
- Widget de reproducción
- Soporte para Android Auto
- Kotlin Multiplatform para iOS
- Themes adicionales (light, AMOLED)
- Importar/exportar playlists
