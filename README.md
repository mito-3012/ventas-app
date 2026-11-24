# Gestión de ventas - Registro y validación de ventas

Aplicación móvil Android (Kotlin + Jetpack Compose + Material 3) desarrollada para la asignatura
**DSY1105 - Desarrollo de Aplicaciones Móviles** (Evaluación Parcial 2).

Resuelve el caso **"Gestión móvil del registro y validación de ventas"**: una agencia de convenios
(dental, automotriz, veterinario, otros) hoy registra sus ventas con fotos enviadas por mensajería y
planillas manuales. La app permite que el vendedor registre la venta desde su teléfono y que el
encargado la revise, valide, observe o rechace, dejando todo trazado.

> **Importante:** es un producto mínimo viable (PMV) académico. Usa **solo datos ficticios** (clientes, RUT, correos, teléfonos,
> montos, prestadores y comprobantes). No se conecta a ningún sistema real ni procesa pagos.

## Integrantes

| Nombre y apellido | Rol en el proyecto |
|-------------------|--------------------|
| Daniel Pinto      | Developer          |
| Jaime erez        | Developer          |

Equipo N°: (completar) - Nombre de la aplicación: **Gestión de ventas**

## Funcionalidades implementadas

**Acceso y cuentas**
- Inicio de sesión con validación de campos y mensajes de error visuales.
- Creación de cuenta ficticia de vendedor (formulario con confirmación de clave y términos).
- Control de acceso por perfil: **Vendedor**, **Encargado de gestión** y **Administrador**.
- Cierre de sesión. La contraseña se guarda como hash PBKDF2 con sal (nunca como texto visible).

**Perfil vendedor**
- Pantalla de inicio con resumen ficticio de operaciones y comisión estimada.
- Registrar venta: tipo de convenio, prestador ficticio, fecha de la operación **o cita a futuro**
  (calendario, hasta 1 año hacia adelante), monto, código de autorización, datos ficticios del cliente (RUT con dígito verificador) y respaldo fotográfico.
- Guardar como borrador o enviar a revisión.
- Corregir ventas observadas y reenviarlas.
- Historial propio con búsqueda por código y filtro por estado.
- Alertas internas (venta observada, validada, rechazada, gestionada).
- **Eliminar ventas** propias en borrador, observadas o rechazadas (con confirmación). Se borran
  también su historial y la foto de respaldo guardada en el teléfono.

**Perfil encargado**
- Bandeja con todas las ventas recibidas (nunca ve borradores).
- Búsqueda por código y filtros por estado, vendedor y fecha.
- Revisión: ver datos y respaldo, **validar**, **solicitar corrección** o **rechazar** (con observación obligatoria).
- Derivar ventas validadas al prestador (envío simulado).
- Indicadores generales y comisiones simuladas por vendedor.
- Alertas de nuevas ventas y correcciones recibidas.
- Eliminación de las fotos de prueba al terminar el proyecto.
- Puede eliminar ventas **rechazadas** (con confirmación y aviso al vendedor).

**Perfil administrador**
- Crea cuentas ficticias asignando el perfil, las activa o desactiva y cambia el perfil de un usuario.
  Su propia cuenta queda protegida (no puede desactivarse ni cambiarse el perfil).
- Configura el **porcentaje de comisión (ficticio)** de cada tipo de convenio (0 a 30%). Los
  indicadores y las comisiones estimadas usan estos valores.
- Elimina las fotos de prueba del teléfono.
- No ve ventas.

**Reglas de negocio del caso**
- No se puede enviar una venta sin los datos obligatorios ni sin respaldo.
- Monto inválido y códigos de autorización duplicados son rechazados.
- Un vendedor no puede validar sus propias ventas ni ver/editar las de otros.
- Una venta validada, en revisión o gestionada no se puede modificar ni eliminar.
- Las fotos descartadas o sin venta se borran del teléfono (al reemplazarlas, al eliminar la venta
  y al abrir la aplicación).
- Cada cambio de estado queda en el **historial** con usuario, fecha y observación.

## Arquitectura (MVVM)

```
View (Compose)  ->  ViewModel (StateFlow)  ->  Repository  ->  Room (DAO / Database)
   pinta estado      valida y decide            reglas de       tablas SQLite
   envía eventos     expone UiState             negocio         persistentes
```

- **View:** solo muestra el estado (`collectAsState`) y envía eventos. No contiene reglas.
- **ViewModel:** guarda el `UiState` inmutable (`data class` + `copy`), valida con `Validadores`
  y llama al repositorio. Mismo patrón de la guía 11 (`UsuarioUiState`, `UsuarioErrores`).
- **Repository:** aplica las reglas críticas (permisos, estados, duplicados) dentro de transacciones.
- **Room (model/data/local):** persistencia local. Tablas: `usuarios`, `ventas`, `historial`, `notificaciones`.

### Estructura de carpetas

Organización idéntica a la de la guía 11 (`model`, `model/data/local`, `navigation`, `ui/screen`,
`ui/theme`, `viewmodel`), ampliada con las capas que necesita esta app.

```
com.example.ventasapp
├── model                       Estados del formulario (UiState + Errores) y modelos de dominio
│   ├── LoginUiState / LoginErrores
│   ├── RegistroUsuarioUiState / RegistroUsuarioErrores
│   ├── NuevoUsuarioUiState / NuevoUsuarioErrores
│   ├── VentaFormUiState / VentaFormErrores, RevisionUiState / RevisionErrores, DetalleUi
│   ├── ConfiguracionUiState
│   ├── PostUiState, PostFormUiState, PostFormErrores, ModoFormularioPost
│   ├── Perfil, EstadoVenta, TipoConvenio, FiltrosVentas, Indicadores, Post
│   └── data
│       ├── local               Room: una Entity y un Dao por tabla + la base
│       │                       UsuarioEntity / UsuarioDao, VentaEntity / VentaDao,
│       │                       HistorialEntity / HistorialDao, NotificacionEntity / NotificacionDao,
│       │                       ComisionEntity / ComisionDao, VentasDatabase
│       ├── remote              ApiService, RetrofitInstance (API REST)
│       └── repository          VentasRepository, SesionManager, PostRepository
├── navigation
│   ├── Rutas.kt                Rutas centralizadas
│   └── AppNavigation.kt        NavHost, transiciones y control de acceso
├── ui
│   ├── screen                  Todas las pantallas: Login, RegistroUsuario, InicioVendedor, MisVentas,
│   │                           VentaForm, DetalleVenta, Gestion, Indicadores, Alertas, Usuarios,
│   │                           Configuracion y Post
│   ├── components              Campos, tarjetas, barras, filtros y SelectorGaleria
│   └── theme                   Color, Type, Theme
├── viewmodel                   Un ViewModel por pantalla o flujo (Login, RegistroUsuario, MisVentas,
│                               VentaForm, DetalleVenta, Gestion, Alertas, Usuarios, Configuracion, Post)
├── util                        Validadores, Seguridad, Formato, ImagenUtils, EventoUi, Excepciones
├── MainActivity.kt
└── VentasApplication.kt
```

Pruebas: `src/test` (ValidadoresTest) y `src/androidTest` (ExampleInstrumentedTest).

## Recursos nativos (IE 2.4.1)

1. **Cámara:** permiso `CAMERA` solicitado en tiempo de ejecución; la foto se entrega a la app de
   cámara mediante `FileProvider` (URI segura). Si se rechaza el permiso, la app lo informa y sigue
   funcionando con la galería.
2. **Galería:** galería **propia de la app** (`SelectorGaleria`), con todos sus textos en español.
   Pide el permiso de lectura de imágenes (`READ_MEDIA_IMAGES` en Android 13+) después de un
   diálogo explicativo en español. Se hizo propia porque el selector que trae Android es una pantalla
   del sistema y muestra sus textos en el idioma del teléfono.
3. **Vibración (háptica):** respuesta táctil cuando una validación falla.

**Idioma:** la app fuerza el español (castellano) en sus propios textos y en los de las librerías
(por ejemplo, el calendario). Las pantallas del **sistema** (la app de cámara y la ventana que pide
el permiso) no las controla la aplicación: siguen el idioma del teléfono. Para verlas en español,
cambiar el idioma en *Ajustes > Sistema > Idiomas*.

Las imágenes se validan (JPG/PNG/WEBP, máximo 2 MB finales), se comprimen y se copian al
almacenamiento **privado** de la app.

## Animaciones (IE 2.2.2)

- Transiciones entre pantallas (deslizamiento + desvanecido; solo desvanecido entre pestañas).
- Mensajes de error de cada campo con `AnimatedContent`; banners con `AnimatedVisibility`.
- Color de las etiquetas de estado con `animateColorAsState`.
- Números y barras de los indicadores con `animateIntAsState` / `animateFloatAsState`.
- Listas con `animateItem()` y tarjetas con `animateContentSize()`.
- Botones con estado de carga animado.

## Cuentas de demostración (ficticias)

Se crean automáticamente la primera vez que se abre la app.

| Perfil | Correo | Contraseña |
|---|---|---|
| Vendedor | vendedor1@demo.cl | Ventas123 |
| Vendedor | vendedor2@demo.cl | Ventas123 |
| Encargado | encargado@demo.cl | Gestion123 |
| Administrador | admin@demo.cl | Admin12345 |

En la pantalla de acceso hay botones que rellenan estos datos. Para ver el flujo completo:
1. Ingresar como **vendedor1**, registrar una venta con foto y enviarla.
2. Cerrar sesión e ingresar como **encargado**: la venta aparece en la bandeja y como alerta.
3. Validarla, observarla o rechazarla. Volver como vendedor1 para ver la alerta y el historial.
4. Ingresar como **administrador** para
   crear usuarios y cambiar los porcentajes de comisión.

## Cómo ejecutar

1. Abrir la carpeta del proyecto (la que contiene `settings.gradle.kts`) con Android Studio.
   Usar la misma versión de Android Studio con la que se trabaja en clase.
2. Esperar el **Gradle Sync** (requiere internet la primera vez).
3. Crear o elegir un emulador (API 24 o superior) o conectar un teléfono.
4. Presionar **Run**.

Si falla el sync, revisar en *Settings > Build > Gradle* que el **Gradle JDK** sea el mismo que
usan en el curso.

## Pruebas

Pruebas unitarias en `app/src/test/.../ValidadoresTest.kt` (validaciones, RUT, fechas, hash de
clave, citas a futuro, eliminación por perfil, comisiones configurables, filtros, indicadores y reglas
de estado). Ejecutar con clic derecho sobre el archivo >
*Run 'ValidadoresTest'*. Corren sin emulador porque la lógica es independiente de la interfaz.

## Flujo de trabajo del equipo

- Ramas por funcionalidad: `feature/...`. Integración a `main` mediante Pull Request.
- Commits técnicos y descriptivos (ver `docs/PLAN_TRELLO_GITHUB.md`).
- Planificación de tareas en Trello (enlace: completar).

## Limitaciones y trabajo futuro (hacia la EFT)

- La sesión no se conserva al cerrar la app (decisión de seguridad para el PMV).
- Catálogo de prestadores editable por el administrador (hoy son ficticios y fijos).
- Cámara dentro de la app (CameraX): hoy se usa la app de cámara del sistema.
- Integración con una API REST de prueba (Retrofit + Spring Boot) y APK firmado en modo release.
- Room está en la versión 2 (se agregó la tabla de comisiones, con migración que conserva los datos).
  Si cambia otra vez la estructura de las tablas, hay que subir la versión y escribir la migración.

## Guía 14 — Consumo de API REST

La aplicación integra Retrofit y JSONPlaceholder (`/posts`) mediante MVVM. Desde el perfil Administrador se accede a la pestaña **API** para probar GET, POST, PUT, PATCH y DELETE. La derivación de ventas validadas también envía un resumen no identificable del cliente a la API de pruebas. Consulta [`docs/GUIA14_IMPLEMENTACION.md`](docs/GUIA14_IMPLEMENTACION.md) para la arquitectura, las pruebas manuales y los pasos pendientes de GitHub/Trello.
#   V e n t a s a p p 1 
 
 #   v e n t a s - a p p  
 