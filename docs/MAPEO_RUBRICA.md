# Dónde se cumple cada indicador de la rúbrica

| Indicador (peso) | Qué se evalúa | Dónde está en el proyecto |
|---|---|---|
| IE 2.1.1 Interfaz coherente (15%) | Jerarquía, distribución, navegación entre vistas | `ui/components/Estructura.kt` (PantallaBase, BarraInferior), `ui/theme/*`, `navigation/AppNavigation.kt` |
| IE 2.1.2 Formularios con validación visual e íconos (15%) | Todos los formularios, error por campo, íconos | `ui/components/Campos.kt` (CampoFormulario: ícono inicial, ícono de error, mensaje animado). Formularios: login, crear cuenta, venta, revisión |
| IE 2.2.1 Validación desacoplada (10%) | Lógica fuera de la interfaz | `util/Validadores.kt` + los ViewModels de `viewmodel/` (UiState y Errores en `model/`). Las pantallas solo pintan el estado |
| IE 2.2.2 Animaciones (10%) | Animaciones que aporten fluidez o retroalimentación | Transiciones en `AppNavigation.kt`; `AnimatedContent` en `Campos.kt`; `animateColorAsState` y `animateIntAsState` en `Visuales.kt`; `animateItem()` en listas |
| IE 2.3.1 Modularidad y persistencia (15%) | MVVM, carpetas, Room integrado | Carpetas como la guía 11 (`model`, `model/data/local`, `navigation`, `ui/screen`, `viewmodel`); `model/data/local/*` (Room: una Entity y un Dao por tabla, más la base); `model/data/repository/VentasRepository.kt` |
| IE 2.3.2 GitHub y Trello (20%) | Commits distribuidos, tareas compartidas | `docs/PLAN_TRELLO_GITHUB.md` (guía de cómo hacerlo) |
| IE 2.4.1 Recursos nativos (15%) | Mínimo dos, con permisos y coherencia | Cámara con permiso en runtime + FileProvider; galería propia con permiso (`ui/components/SelectorGaleria.kt`); vibración. Ver `ui/screen/VentaFormScreen.kt` y `util/ImagenUtils.kt` |

## Guion sugerido para la defensa (5 a 7 minutos)

1. **Arquitectura:** mostrar las carpetas y explicar View -> ViewModel -> Repository -> Room.
2. **Login:** intentar ingresar vacío (errores en rojo con ícono) y con clave mala (banner + vibración).
3. **Vendedor:** crear una venta; mostrar errores de RUT/monto/código; adjuntar foto con cámara y
   galería; guardar borrador; enviar.
4. **Persistencia:** cerrar la app por completo y reabrir: la venta sigue ahí (Room).
5. **Encargado:** ver la bandeja, filtrar, abrir la venta, observarla con comentario.
6. **Trazabilidad:** volver como vendedor, ver la alerta y el historial; corregir y reenviar.
7. **Reglas:** mostrar que el vendedor no ve ventas ajenas y que una venta validada no se edita.
8. **Código:** abrir `Validadores.kt` y `ValidadoresTest.kt` para mostrar que la lógica está desacoplada.
9. **Perfil administrador:** entrar como administrador (crear usuario, desactivarlo,
   cambiar un porcentaje de comisión y ver cómo cambian los indicadores).
10. **Eliminar:** borrar una venta rechazada o en borrador y mostrar que la foto también se elimina.
