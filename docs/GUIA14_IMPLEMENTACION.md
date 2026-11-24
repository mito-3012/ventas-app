# Guía 14 — Consumiendo una API REST en VentasApp

## Adaptación realizada

La integración se incorporó al proyecto VentasApp existente, manteniendo Room, autenticación, perfiles, ventas y navegación. La funcionalidad de publicaciones se encuentra en el menú del perfil Administrador, en la opción **API**.

## Arquitectura

- `model/Post.kt`: modelos de respuesta, creación y actualización parcial.
- `model/data/remote/ApiService.kt`: endpoints GET, POST, PUT, PATCH y DELETE.
- `model/data/remote/RetrofitInstance.kt`: configuración de Retrofit, Gson y OkHttp.
- `model/data/repository/PostRepository.kt`: capa repository para aislar Retrofit de la interfaz.
- `viewmodel/PostViewModel.kt`: estado de pantalla y operaciones en corrutinas con MVVM.
- `model/PostUiState.kt`, `model/PostFormUiState.kt` y `model/PostFormErrores.kt`: estados de carga, formulario y errores.
- `ui/screen/PostScreen.kt`: interfaz Jetpack Compose.
- `util/ErroresRed.kt`: mensajes de error de red y HTTP.

La URL base utilizada es `https://jsonplaceholder.typicode.com/` y los recursos son `/posts`.

## Operaciones incluidas

| Operación | Método | Acción en la pantalla |
|---|---|---|
| Consultar publicaciones | GET | Carga la lista desde la API |
| Crear publicación | POST | Botón flotante y formulario |
| Reemplazar publicación | PUT | Acción de reemplazo en cada tarjeta |
| Actualizar título | PATCH | Acción de edición parcial |
| Eliminar publicación | DELETE | Acción de eliminación con confirmación |

JSONPlaceholder es una API de demostración: las operaciones de escritura responden como si se hubieran realizado, pero los cambios no quedan guardados de forma permanente en el servidor. La aplicación refleja los cambios localmente durante la sesión.

## Integración con el flujo de ventas

Cuando el Encargado deriva una venta validada, se envía a la misma API un resumen de la operación. No se incluyen nombre, RUT, correo ni teléfono del cliente. Solo después de recibir una respuesta HTTP satisfactoria se marca la venta como gestionada en Room y se registra el identificador de la publicación en la observación del historial.

## Requisitos técnicos agregados

- Permiso `android.permission.INTERNET` en `AndroidManifest.xml`.
- Retrofit y Gson Converter.
- OkHttp para configurar tiempos de espera.
- Corrutinas de Kotlin para llamadas suspendidas.
- KSP alineado con Kotlin 2.2.10 para mantener la generación de código de Room.

## Comprobación manual en Android Studio

1. Abrir el proyecto y ejecutar **Sync Project with Gradle Files**.
2. Ejecutar la app en un emulador o dispositivo con conexión a Internet.
3. Iniciar sesión con un usuario Administrador.
4. Abrir la pestaña **API** y comprobar que se cargan publicaciones.
5. Probar crear una publicación (POST), reemplazarla (PUT), cambiar solo el título (PATCH) y eliminarla (DELETE).
6. Iniciar sesión como Encargado, abrir una venta validada y probar **Derivar al prestador (API de pruebas)**.
7. Revisar el historial de la venta y verificar que aparece la referencia de confirmación de JSONPlaceholder.
8. Probar sin conexión para comprobar los mensajes de error.

## Trabajo colaborativo solicitado por la guía

La guía también solicita crear un repositorio privado de GitHub, agregar a la pareja y al docente como colaboradores, y gestionar tareas en Trello. Esas acciones dependen de las cuentas del equipo y no se ejecutan desde el código del proyecto.

Mensaje sugerido para el primer commit de la actividad:

`Inicio de proyecto + estructura base para consumo de API REST`

Tablero Trello sugerido: **Por hacer**, **En curso**, **Finalizado**. Tarjetas recomendadas: configuración Retrofit/dependencias; implementación GET; implementación POST/PUT/PATCH/DELETE; integración de derivación de ventas; pruebas en emulador; documentación y capturas.
