# Plan de trabajo colaborativo (Trello + GitHub)

La rúbrica IE 2.3.2 (20% de la nota) evalúa que **ambos integrantes** muestren actividad real y
distribuida. Este documento propone cómo repartirse el trabajo y qué dejar registrado.

## 1. Tablero de Trello

Listas: **Backlog** | **En desarrollo** | **En revisión** | **Hecho**

| Tarjeta | Integrante | Checklist |
|---|---|---|
| Configuración inicial del proyecto | A | Crear proyecto, Gradle, Room, estructura de carpetas, repo GitHub |
| Modelo y base de datos Room | A | Entidades, DAOs, VentasDatabase, repositorio |
| Seguridad y sesión | A | Hash de clave, SesionManager, datos demo |
| Login y creación de cuenta | A | Pantallas, SesionViewModel, RegistroUsuarioViewModel, validaciones |
| Tema visual y componentes | B | Colores, tipografía, campos reutilizables, tarjetas, barras |
| Formulario de venta | B | Campos, validaciones, borrador/envío, VentaFormViewModel |
| Recursos nativos | B | Cámara + permiso, galería, FileProvider, vibración, ImagenUtils |
| Flujo del vendedor | B | Inicio, Mis ventas, filtros, detalle, corrección |
| Flujo del encargado | A | Gestión, filtros, revisión, derivar, historial |
| Indicadores y alertas | A | Indicadores, comisiones simuladas, notificaciones internas |
| Navegación y control de acceso | A/B | Rutas, NavHost, guardia de sesión, transiciones |
| Animaciones | B | Transiciones, errores animados, números animados |
| Pruebas unitarias | A/B | ValidadoresTest, filtros, indicadores |
| Documentación y entrega | A/B | README, capturas, subir a AVA, activar enlace individual |

Cada tarjeta debe tener **responsable asignado**, **checklist con avance** y, al terminar, una
captura o enlace al commit como adjunto.

## 2. Flujo de GitHub

```
main                        <- siempre estable
 ├── feature/configuracion-room
 ├── feature/login-registro
 ├── feature/formulario-venta
 ├── feature/recursos-nativos
 ├── feature/flujo-encargado
 └── feature/indicadores-alertas
```

1. Cada integrante trabaja en su rama `feature/...`.
2. Al terminar, abre un **Pull Request** hacia `main` y el compañero lo revisa y aprueba.
3. Usar **Issues** para tareas y errores; cerrarlos desde el commit (`Cierra #3`).
4. Ambos deben tener commits **repartidos en varios días** (no todo el último día).

### Ejemplos de commits claros (usar este estilo)

```
feat(room): agrega entidades, DAOs y base de datos de ventas
feat(auth): implementa login con hash PBKDF2 y validación de campos
feat(venta): agrega formulario de venta con validación centralizada en ViewModel
feat(nativo): integra cámara con permiso en runtime y FileProvider
feat(nativo): agrega galería propia en español con permiso de lectura de imágenes
feat(gestion): agrega panel de revisión con validar, observar y rechazar
feat(historial): registra cambios de estado con usuario y fecha
feat(ui): agrega animaciones de error en campos y transiciones de navegación
fix(validacion): corrige cálculo del dígito verificador del RUT con K
test(validadores): agrega pruebas unitarias de RUT, monto, fechas y filtros
docs(readme): documenta arquitectura, funcionalidades y pasos de ejecución
```

Evitar mensajes como "cambios", "listo", "arreglos", "final2" (la rúbrica pide commits técnicos).

## 3. Reglas de entrega (del encargo)

- Repositorio **público** con acceso al docente y a los dos integrantes.
- Debe incluir carpeta del proyecto (`app`, Gradle), `README.md` con nombres, funcionalidades y pasos.
- **No hacer ningún commit después de la fecha de entrega y antes de la evaluación**: ni siquiera
  mínimo. Hacerlo implica nota 1.0 grupal e individual.
- Subir el código fuente al **AVA grupal** (un solo integrante) y activar el **AVA individual**
  con nombre, apellido, número de equipo y nombre de la aplicación.
