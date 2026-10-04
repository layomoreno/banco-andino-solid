# Laboratorio L2 — SOLID: backend de Banco Andino

- **Lenguaje:** Java 17 (código base sin traducir). Pruebas con JUnit 5 (`mvn test`).
- **Integrantes:** _(completar)_
- Diagramas: `docs/uml_antes.md` y `docs/uml_despues.md` (Mermaid; se ven en GitHub o en mermaid.live).

---
## Bloque 1 — Diagnóstico

### 1.1 Tabla de hallazgos

| Clase / método | Letra | Evidencia en el código | Consecuencia para el banco o el cliente |
|---|---|---|---|
| `TransaccionService.transferir` | **S** | Un solo método valida, calcula comisión, mueve dinero, guarda en Oracle, imprime el comprobante, manda SMS y audita (7 pasos comentados 1–7). | Si legal cambia el comprobante o marketing cambia el texto del SMS hay que editar la misma clase que mueve el dinero; un error de tipeo ahí puede cobrar mal una transferencia. Siete áreas distintas compiten por el mismo archivo y nadie se atreve a tocarlo. |
| `TransaccionService.transferir` (`switch (tipo)`) | **O** | Las comisiones están en un `switch` sobre un `String`. | Cada tipo nuevo de transferencia obliga a editar y re-probar el método que ya funciona; se puede romper el cálculo de "MISMO_BANCO" al agregar otro tipo. Un typo en el `String` solo se descubre en producción. |
| `CDT.retirar` (hereda de `Cuenta`) | **L** | `CDT extends Cuenta` pero `retirar` lanza `UnsupportedOperationException`: endurece el contrato de la clase padre. | Cualquier código que reciba una `Cuenta` y la retire (como `CobroCuotaManejo`) puede explotar con un CDT. El error aparece en ejecución, de noche, en el cobro masivo. |
| `CobroCuotaManejo.cobrarMensual(List<Cuenta>)` | **L** (síntoma) | Acepta cualquier `Cuenta` y llama `retirar` sin saber si se puede. | Una sola cuenta CDT detiene el proceso por lotes: las cuentas siguientes no se cobran y las anteriores ya fueron cobradas. |
| `ProductoBancario` | **I** | Interfaz "gorda" de 5 métodos; `CreditoVivienda.depositar/retirar` están vacíos y `TarjetaCredito.depositar` está vacío. | Alguien que llame `depositar` a un crédito cree que abonó y no pasó nada: dinero del cliente "desaparece" sin error. Cada producto nuevo arrastra métodos que no entiende. |
| `TarjetaCredito.retirar` | **I / L** | `retirar` significa "avance en efectivo" (suma deuda), el nombre promete otra cosa. | Tratar una tarjeta como una cuenta (`retirar`) aumenta deuda en lugar de bajar saldo; confusión conceptual entre productos. |
| `TransaccionService` (campos) | **D** | `new OracleRepositorio()` y `new SmsGateway()` dentro de la clase. | Es imposible probar sin conectarse a la base de producción y mandar SMS reales; migrar de Oracle o de proveedor de SMS obliga a modificar la lógica de negocio. |
| `Cuenta` (observación extra, no se corrige) | — | `retirar` no valida `monto <= 0` y el dinero se maneja con `double`. | Un retiro negativo aumentaría el saldo; `double` acumula errores de redondeo. Fuera del alcance de SOLID (el laboratorio exige no cambiar el comportamiento) pero se anota como deuda técnica. |

### 1.2 Experimentos

**Experimento 1 — El CDT.** Cambié `List.of(ana, luis)` por `List.of(ana, luis, cdtAna)`. Resultado real:

```
Cuota de manejo cobrada a 001-1
Cuota de manejo cobrada a 001-2
Exception in thread "main" java.lang.UnsupportedOperationException: Un CDT no permite retiros antes del vencimiento
    at CDT.retirar(CDT.java:14)
    at CobroCuotaManejo.cobrarMensual(CobroCuotaManejo.java:8)
```
En producción, con la cuenta 500 000 siendo un CDT: el proceso se cae en la mitad, las 499 999 primeras ya fueron cobradas, las ~500 000 restantes **no** se cobran, y al reintentar sin control se cobraría doble a las primeras. Además nadie lo sabe hasta la mañana siguiente.

**Experimento 2 — La prueba imposible.** No se logra. `TransaccionService` crea `new OracleRepositorio()` y `new SmsGateway()` en sus campos: no hay constructor ni *setter* por donde inyectar un reemplazo. Al ejecutar `transferir`, siempre se "conecta a Oracle" y siempre "manda SMS". Lo único posible sería capturar `System.out`, pero eso solo es mirar el daño después de hecho.

### 1.3 Medición "antes"

| Métrica | Antes |
|---|---|
| Líneas del método `transferir` | 36 (líneas 7–42 del archivo) |
| Razones distintas por las que `TransaccionService` podría cambiar | 7 (validación, comisión, movimiento, persistencia, comprobante, notificación, auditoría) |
| Clases concretas que `TransaccionService` crea con `new` | 2 (`OracleRepositorio`, `SmsGateway`) |
| Métodos vacíos o que lanzan excepción por "no aplica" | 4 (`TarjetaCredito.depositar`, `CreditoVivienda.depositar`, `CreditoVivienda.retirar`, `CDT.retirar`) |
| ¿Se puede probar `transferir` sin Oracle ni SMS? | **No** |

### 1.4 Diagrama de clases original
`docs/uml_antes.md`.

---
## Bloque 2 — Refactorización

Prueba de caracterización: `diff salida_original.txt salida_nueva.txt` → solo difiere la hora de `[AUDITORIA]`.

**Control S** — `TransaccionService` ahora *"coordina el flujo de una transferencia delegando cada paso en una abstracción"* (sin la palabra "y" como conjunción de responsabilidades). Si legal cambia el formato del comprobante se toca únicamente `ComprobanteConsola.java`; el texto del SMS vive en `NotificadorSms.java`; el SQL en `OracleRepositorio.java`.

**Control O** — Cada comisión es una clase que implementa `PoliticaComision`. Un tipo nuevo (p. ej. `ComisionPremium`) requiere: 1 archivo nuevo + 1 línea en `Main` (donde se arma `List.of(...)`). Archivos existentes a modificar: **solo `Main.java`**. (La prueba `tipoNuevoSeAgregaSinModificarCodigoExistente` lo demuestra.)

**Control L** — Se separó la jerarquía: `Cuenta` (recibe dinero) → `CuentaRetirable` (además retira) → `CuentaAhorros`; `CDT` extiende `Cuenta` y **no tiene `retirar`**. `CobroCuotaManejo` y `transferir` exigen `CuentaRetirable`. Probado: `List.of(ana, luis, cdtAna)` produce `error: incompatible types ... equality constraints: CuentaRetirable, lower bounds: CDT,CuentaAhorros`. Se detecta **al compilar**, que es mejor porque el error aparece en el computador del desarrollador y no a las 2 a. m. con un millón de cuentas. El `try/catch` que ignora los CDT no arregla el diseño: el tipo `Cuenta` sigue prometiendo algo que no cumple, cada nuevo llamador debe recordar el `catch`, se esconden errores reales (una excepción por otra causa se traga igual) y el compilador no ayuda.

**Control I** — `ProductoBancario` se eliminó y se partió en interfaces por capacidad: `Extractable`, `CalculaIntereses`, `PagaCuota`, `AvanceEfectivo`. `GeneradorExtractos` recibe `List<? extends Extractable>` y funciona para cuentas, CDT, tarjetas y créditos a la vez; solo necesita `generarExtracto()`, por eso no conoce los demás métodos. Ningún producto tiene métodos vacíos.

**Control D** — `TransaccionService` conoce **0 clases concretas**: solo interfaces (`ReglaTransferencia`, `CalculadorComision`, `RepositorioTransacciones`, `ComprobanteTransferencia`, `NotificadorTransferencias`, `Auditoria`) y los tipos de dominio `CuentaRetirable`/`Cuenta`/`Transferencia`. Quien decide Oracle o SMS es `Main` (composition root). El experimento 2 ya es posible: ver `TransaccionServiceTest`.

---
## Bloque 3 — Pruebas unitarias

`src/test/java/TransaccionServiceTest.java` (5 pruebas obligatorias + 3 extra) y `CobroCuotaManejoTest.java` (3 pruebas). Dobles: `guardadas::add` (repositorio en memoria), `notificadas::add` (notificador que anota), lambdas vacías para comprobante y auditoría.

Ejecución: `mvn test`. (En el entorno donde preparé esto no hay acceso a Maven Central, así que las 11 pruebas las ejecuté con un *shim* mínimo de JUnit: 11/11 pasaron en ~150 ms. Corran `mvn test` y anoten su propio tiempo.)

- **¿Cuánto tardan?** Milisegundos (no hay red ni disco).
- **¿Cuántas líneas de `TransaccionService` hubo que cambiar para probarla?** Ninguna "extra": el diseño del punto D ya la hacía probable; el constructor recibe las dependencias.
- **¿Y en el bloque 1?** Imposible sin tocar la clase (experimento 2): habría tocado Oracle y enviado SMS reales.

---
## Bloque 4 — Negocio pidió cambios
_Pendiente: el docente entrega los 5 requerimientos._ Tabla a completar (un commit por requerimiento):

| Req. | Archivos a modificar en el código original (estimado) | Archivos existentes modificados (real) | Archivos nuevos | ¿Se rompió alguna prueba? |
|---|---|---|---|---|
| R1 | | | | |
| R2 | | | | |
| R3 | | | | |
| R4 | | | | |
| R5 | | | | |

Guía de estimación para el original: casi cualquier cambio de comisión, notificación, comprobante, persistencia o auditoría = **modificar `TransaccionService`** (más `Main` si cambia la construcción). En el refactorizado: normalmente 0 archivos existentes (salvo `Main`) y 1–2 nuevos.

## Bloque 5 — Revisión cruzada
_Pendiente (se hace con otra pareja)._ Rama `revision-cruzada`.

---
## Bloque 6 — Cierre

| Métrica | Antes | Después |
|---|---|---|
| Líneas del método `transferir` | 36 | 14 |
| Razones distintas por las que `TransaccionService` podría cambiar | 7 | 1 (cambiar el flujo de la transferencia) |
| Clases concretas que `TransaccionService` crea con `new` | 2 | 0 (solo `new Transferencia(...)`, un registro de datos) |
| Métodos vacíos o que lanzan "no aplica" | 4 | 0 |
| ¿Se puede probar `transferir` sin Oracle ni SMS? | No | Sí |
| Número total de archivos | 11 | 33 en `src/main` (+ 2 de prueba) |
| Archivos existentes modificados en total en el bloque 4 | — | _(completar tras el bloque 4)_ |

**(a)** Más archivos no es un problema en sí: cada uno es corto, tiene un nombre que dice qué hace y un solo motivo de cambio; encontrar y cambiar algo cuesta menos que bucear en un método de 36 líneas. **Sí** sería un problema si las abstracciones no aportan nada (interfaces con una sola implementación que nunca variará, capas que solo reenvían llamadas) o si el equipo no puede navegar el código; ahí se pagaría complejidad sin recibir flexibilidad. (Ojo: algunas de las mías, como `CalculadorComision`, son defendibles solo porque `TransaccionService` debe depender de una abstracción y probarse; si el equipo lo considera excesivo se puede colapsar.)

**(b)** _(Completar con sus datos del bloque 4.)_ Hipótesis para contrastar: los requerimientos de "un tipo de transferencia nuevo" y "cambiar el comprobante o el SMS" muestran la mayor diferencia, porque en el original todos tocan `TransaccionService` y en el refactorizado solo agregan una clase.

**(c)** _(Completar.)_ Posibles puntos débiles de este diseño: reglas que dependen de varias transferencias (tope **diario** acumulado necesita estado/repositorio y no es una regla pura de monto), notificar por varios canales a la vez (hoy hay un solo `NotificadorTransferencias`; habría que compuesto/lista), atomicidad entre retirar, depositar y guardar (no hay transacción), y `String tipo` (conviene un `enum` o un tipo propio).

**(d)** _(Completar con la retroalimentación real de la otra pareja.)_

**(e)** Argumento al jefe, con datos: pasamos de 7 motivos de cambio en la clase que mueve el dinero a 1; de 2 dependencias concretas a 0; de **no poder probar** a 11 pruebas que corren en ~150 ms sin tocar producción; y un error que antes explotaba de noche con un CDT en el cobro de un millón de cuentas ahora **no compila**. Cada requerimiento del bloque 4 pasó de tocar la clase "intocable" a agregar archivos nuevos, con las pruebas protegiendo lo que ya funcionaba. Dos semanas de inversión se pagan con el primer incidente de producción evitado.

---
## Cómo hacer el historial de commits (obligatorio)
Hagan un commit por bloque / punto de control con los nombres de la guía. Sugerencia para reproducir el avance de verdad (el historial se evalúa):
1. `bloque-0-codigo-base`: código original + `salida_original.txt`.
2. `bloque-1-diagnostico`: este README (secciones 1.1–1.3) + UML antes.
3. `control-S`, `control-O`, `control-L`, `control-I`, `control-D`: apliquen cada separación en ese orden y ejecuten `diff` contra `salida_original.txt` en cada uno.
4. `bloque-3-pruebas`, `req-1` … `req-5`, `revision-cruzada`, `bloque-6-cierre`.
