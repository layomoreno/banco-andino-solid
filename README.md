# Laboratorio L2 — SOLID: backend de Banco Andino

- **Lenguaje:** Java 17 (código base sin traducir). Pruebas con JUnit 5 (`mvn test`).
- **Integrantes:** Layo Andres Moreno Cortes, Gabriel Jeronimo Puentes Umbarila
- **Diagramas:** `docs/uml_antes.md` y `docs/uml_despues.md` (Mermaid; se ven en GitHub o en mermaid.live).
- **Código original (bloque 0):** carpeta `original_bloque0/` y salida en `salida_original.txt`.

---

## Bloque 1 — Diagnóstico

### 1.1 Tabla de hallazgos

| Clase / método | Letra | Evidencia en el código | Consecuencia para el banco o el cliente |
|---|---|---|---|
| `TransaccionService.transferir` | **S** | Un solo método valida, calcula comisión, mueve dinero, guarda en Oracle, imprime el comprobante, manda SMS y audita (7 pasos comentados 1–7). | Si legal cambia el comprobante o si cambia el texto del SMS hay que editar la misma clase que mueve el dinero, y un error de tipeo ahí puede cobrar mal una transferencia. Siete áreas distintas compiten por el mismo archivo y nadie se atreve a tocarlo. |
| `TransaccionService.transferir` (`switch (tipo)`) | **O** | Las comisiones están en un `switch` sobre un `String`. | Cada tipo nuevo de transferencia obliga a editar y volver a probar el método que ya funciona; se puede romper el cálculo de `MISMO_BANCO` al agregar otro tipo. Un error de tipeo en el `String` solo se descubre en ejecución. |
| `CDT.retirar` (hereda de `Cuenta`) | **L** | `CDT extends Cuenta` pero `retirar` lanza `UnsupportedOperationException`: endurece el contrato de la clase padre. | Cualquier código que reciba una `Cuenta` y la retire (como `CobroCuotaManejo`) puede explotar con un CDT. El error aparece en ejecución, de noche, en el cobro masivo. |
| `CobroCuotaManejo.cobrarMensual(List<Cuenta>)` | **L** (síntoma) | Acepta cualquier `Cuenta` y llama `retirar` sin saber si se puede. | Una sola cuenta CDT detiene el proceso por lotes: las cuentas siguientes no se cobran y las anteriores ya fueron cobradas. |
| `ProductoBancario` | **I** | Interfaz "gorda" de 5 métodos; `CreditoVivienda.depositar/retirar` están vacíos y `TarjetaCredito.depositar` está vacío. | Quien llame `depositar` sobre un crédito cree que abonó y no pasó nada: el dinero del cliente "desaparece" sin error. Cada producto nuevo arrastra métodos que no entiende. |
| `TarjetaCredito.retirar` | **I / L** | `retirar` significa "avance en efectivo" (suma deuda), pero el nombre promete otra cosa. | Tratar una tarjeta como una cuenta (`retirar`) aumenta la deuda en lugar de bajar el saldo; confusión conceptual entre productos. |
| `TransaccionService` (campos) | **D** | `new OracleRepositorio()` y `new SmsGateway()` dentro de la clase. | Es imposible probar sin conectarse a la base de producción y mandar SMS reales; migrar de Oracle o de proveedor de SMS obliga a modificar la lógica de negocio. |

**Observaciones adicionales (deuda técnica fuera del alcance de SOLID; no se corrigen porque el laboratorio exige no cambiar el comportamiento):**

| Dónde | Observación | Consecuencia |
|---|---|---|
| `Cuenta.retirar` | No valida `monto <= 0`. | Un retiro negativo aumentaría el saldo. |
| Todo el dominio | El dinero se maneja con `double`. | Errores de redondeo acumulados en saldos y comisiones. |
| Validación del "tope diario" | Se compara el monto de **una** transferencia contra $5.000.000; no se acumula por día. | Un cliente puede hacer varias transferencias de $5.000.000 el mismo día y el "tope diario" no lo detiene. El nombre `ReglaTopeDiario` promete más de lo que hace. |
| `Cuenta` (`protected`) | `numero`, `titular` y `saldo` son `protected`. | Las subclases pueden modificar el saldo sin pasar por `depositar` ni `retirar`, saltándose las validaciones. |
| `TarjetaCredito.pagarCuota` / `CreditoVivienda.pagarCuota` | No validan que el pago no supere la deuda. | La deuda puede quedar negativa. |

### 1.2 Experimentos

**Experimento 1 — El CDT.** Cambiamos `List.of(ana, luis)` por `List.of(ana, luis, cdtAna)`. Resultado real:

```
Cuota de manejo cobrada a 001-1
Cuota de manejo cobrada a 001-2
Exception in thread "main" java.lang.UnsupportedOperationException: Un CDT no permite retiros antes del vencimiento
    at CDT.retirar(CDT.java:14)
    at CobroCuotaManejo.cobrarMensual(CobroCuotaManejo.java:8)
```

En producción, con la cuenta 500 000 siendo un CDT: el proceso se cae a la mitad, las 499 999 primeras ya fueron cobradas, las restantes **no** se cobran, y al reintentar sin control se cobraría doble a las primeras. Además nadie se entera hasta la mañana siguiente.

**Experimento 2 — La prueba imposible.** No lo logramos. `TransaccionService` crea `new OracleRepositorio()` y `new SmsGateway()` en sus campos: no hay constructor ni *setter* por donde inyectar un reemplazo. Al ejecutar `transferir`, siempre "se conecta a Oracle" y siempre "manda SMS". Lo único posible sería capturar `System.out`, pero eso solo es mirar el daño después de hecho.

### 1.3 Medición "antes"

| Métrica | Antes |
|---|---|
| Líneas del método `transferir` | 36 (líneas 7–42 del archivo) |
| Razones distintas por las que `TransaccionService` podría cambiar | 7 (validación, comisión, movimiento, persistencia, comprobante, notificación, auditoría) |
| Clases concretas que `TransaccionService` crea con `new` | 2 (`OracleRepositorio`, `SmsGateway`) |
| Métodos vacíos o que lanzan excepción por "no aplica" | 4 (`TarjetaCredito.depositar`, `CreditoVivienda.depositar`, `CreditoVivienda.retirar`, `CDT.retirar`) |
| ¿Se puede probar `transferir` sin Oracle ni SMS? | **No** |

### 1.4 Diagrama de clases original

Ver `docs/uml_antes.md` (las dependencias y herencias problemáticas están en rojo).

---

## Bloque 2 — Refactorización

Prueba de caracterización: `diff salida_original.txt salida_nueva.txt` → solo difiere la hora de `[AUDITORIA]`.

**Control S.** `TransaccionService` ahora *coordina el flujo de una transferencia delegando cada paso en una abstracción*. En esa frase **no aparece la palabra "y"** como unión de responsabilidades: queda una sola. Si legal cambia el formato del comprobante se toca únicamente `ComprobanteConsola.java`; el texto del SMS vive en `NotificadorSms.java`; el SQL, en `OracleRepositorio.java`.

**Control O.** Cada comisión es una clase que implementa `PoliticaComision`. Un tipo nuevo (por ejemplo `ComisionPremium`) requiere 1 archivo nuevo y 1 línea en `Main` (donde se arma `List.of(...)`). Archivos existentes a modificar: **solo `Main.java`**. La prueba `tipoNuevoSeAgregaSinModificarCodigoExistente` lo demuestra.

**Control L.** Se separó la jerarquía: `Cuenta` (recibe dinero) → `CuentaRetirable` (además retira) → `CuentaAhorros`; `CDT` extiende `Cuenta` y **no tiene `retirar`**. `CobroCuotaManejo` y `transferir` exigen `CuentaRetirable`. Verificado: `List.of(ana, luis, cdtAna)` produce `error: incompatible types ... equality constraints: CuentaRetirable, lower bounds: CDT,CuentaAhorros`. El error se detecta **al compilar**, que es mejor porque aparece en el computador del desarrollador y no a las 2 a. m. con un millón de cuentas. El `try/catch` que ignora los CDT no arregla el diseño: el tipo `Cuenta` seguiría prometiendo algo que no cumple, cada nuevo llamador tendría que recordar el `catch`, se esconden errores reales (una excepción por otra causa se traga igual) y el compilador no ayuda.

**Control I.** `ProductoBancario` se eliminó y se partió en interfaces por capacidad: `Extractable`, `CalculaIntereses`, `PagaCuota`, `AvanceEfectivo`. `GeneradorExtractos` recibe `List<? extends Extractable>` y funciona para cuentas, CDT, tarjetas y créditos a la vez; solo necesita `generarExtracto()`, por eso no conoce los demás métodos. Ningún producto tiene métodos vacíos.

**Control D.** `TransaccionService` conoce **0 clases concretas**: solo interfaces (`ReglaTransferencia`, `CalculadorComision`, `RepositorioTransacciones`, `ComprobanteTransferencia`, `NotificadorTransferencias`, `Auditoria`) y los tipos de dominio `CuentaRetirable`, `Cuenta` y `Transferencia`. Quien decide si se usa Oracle o SMS es `Main` (composition root). El experimento 2 ya es posible: ver `TransaccionServiceTest`.

---

## Bloque 3 — Pruebas unitarias

`src/test/java/TransaccionServiceTest.java` (5 pruebas obligatorias + 3 extra) y `CobroCuotaManejoTest.java` (3 pruebas). Dobles: `guardadas::add` (repositorio en memoria), `notificadas::add` (notificador que anota en vez de enviar) y lambdas vacías para comprobante y auditoría.

Ejecución: `mvn test`.

- **¿Cuánto tardan?** *(completar con el tiempo que reporta `mvn test`)*. No hay red ni disco, así que deberían ser milisegundos.
- **¿Cuántas líneas de `TransaccionService` hubo que cambiar para probarla?** Ninguna adicional: el cambio del punto D (constructor que recibe las dependencias) ya la hacía probable.
- **¿Qué habría pasado en el bloque 1?** Imposible sin tocar la clase (experimento 2): habríamos tocado Oracle y enviado SMS reales.

---

## Bloque 4 — Negocio pidió cambios

Orden de trabajo: estimar sobre el código original (`original_bloque0/`), implementar sobre el refactorizado, registrar datos reales y correr `mvn test`. Un commit por requerimiento.

La columna "Archivos nuevos" cuenta archivos de `src/main`; entre paréntesis, archivos de prueba nuevos. Las pruebas existentes (`TransaccionServiceTest`, `CobroCuotaManejoTest`) no se editaron: cada requerimiento trae su propio archivo de pruebas. El `README.md` y los diagramas no se cuentan como código modificado.

| Req. | Estimado en el original (archivos a modificar) | Existentes modificados (real) | Archivos nuevos | ¿Se rompió alguna prueba? |
|---|---|---|---|---|
| R1 | 1 (`TransaccionService`: agregar un `case` al `switch`) | 1 (`Main.java`: una línea para registrar la política) | 1 (+1 de prueba) | No |
| R2 | 0 (`CuentaInfantil` sería una subclase nueva de `Cuenta` que sobrescribe `retirar`; ninguna clase existente cambia) | 0 (ni siquiera `Main`: el requerimiento no pide cambiar el programa principal) | 1 (+1 de prueba) | No |
| R3 | 1 (`TransaccionService`: agregar el envío del push junto al SMS) | 1 (`Main.java`: se arma la lista de canales) | 3 (+1 de prueba): `NotificadorPush`, `PushGateway`, `NotificadorCompuesto` | No |
| R4 | 1 (`TransaccionService`: agregar la llamada al antifraude después de la auditoría) | 1 (`Main.java`: se arma la lista de registros) | 2 (+1 de prueba): `AuditoriaAntifraude`, `AuditoriaCompuesta` | No |
| R5 | 1 (`TransaccionService`: reemplazar `new OracleRepositorio()` por el nuevo repositorio) | 1 (`Main.java`: una línea; `OracleRepositorio` no se toca ni se borra) | 1 (+1 de prueba): `PostgresRepositorio` | No (`TransaccionServiceTest` y `CobroCuotaManejoTest` sin cambios) |

---

## Bloque 5 — Revisión cruzada

- **Repositorio revisado:** *(completar: enlace del repo de la otra pareja)*. Rama `revision-cruzada`.
- **Requerimiento:** R6, pago de servicios públicos.
- **Lista de revisión que recibimos de la otra pareja:** *(pegar aquí la lista completa, con Sí/No y los dos comentarios finales)*.

---

## Bloque 6 — Cierre

### 6.1 Diagramas

Antes: `docs/uml_antes.md`. Después: `docs/uml_despues.md` (clases nuevas del bloque 4 en verde).

### 6.2 Tabla comparativa

| Métrica | Antes | Después |
|---|---|---|
| Líneas del método `transferir` | 36 | 14 |
| Razones distintas por las que `TransaccionService` podría cambiar | 7 | 1 (cambiar el flujo de la transferencia) |
| Clases concretas que `TransaccionService` crea con `new` | 2 | 0 (solo `new Transferencia(...)`, un registro de datos) |
| Métodos vacíos o que lanzan "no aplica" | 4 | 0 |
| ¿Se puede probar `transferir` sin Oracle ni SMS? | No | Sí |
| Número total de archivos | 11 | 41 en `src/main` (+7 de prueba) |
| Archivos existentes modificados en total en el bloque 4 | — | 1 archivo distinto (`Main.java`), modificado en 4 de los 5 requerimientos (R1, R3, R4, R5). `TransaccionService` no se tocó ni una vez. |

Estimación en el código original para el mismo bloque: 4 modificaciones de `TransaccionService` (R1, R3, R4 y R5) y ninguna para R2.

### 6.3 Reflexión

**(a) ¿Más archivos es un problema?** No en sí mismo: pasamos de 11 a 41 archivos, pero cada uno es corto, su nombre dice qué hace y tiene un solo motivo de cambio. Encontrar y cambiar algo cuesta menos que bucear en un método de 36 líneas. **Sí** sería un problema si las abstracciones no aportaran nada (interfaces con una sola implementación que nunca va a variar, capas que solo reenvían llamadas) o si el equipo no pudiera navegar el código. Una señal de alerta en nuestro propio diseño: `CalculadorComision` y `CalculadorComisionPorTipo` son defendibles solo porque `TransaccionService` debe depender de una abstracción y poder probarse; si el equipo las considerara excesivas se podrían colapsar.

**(b) ¿En qué requerimiento se notó más la diferencia?** En R5 (migración a PostgreSQL), y en general en R3, R4 y R5. En el código original los tres obligaban a editar `TransaccionService`, la clase que "nadie se atreve a tocar", y en R5 además a editar y volver a probar el método que mueve el dinero solo para cambiar de base de datos. En el refactorizado fueron una clase nueva más una línea en `Main`, y las pruebas existentes siguieron pasando sin cambios. En R1 la diferencia fue menor, porque el `switch` original solo habría necesitado un `case` más.

**(c) ¿Algún requerimiento que el diseño no aguantó bien?** Sí, tres puntos que preferimos documentar:

1. **R2 (cuenta infantil).** `CuentaInfantil extends CuentaRetirable` y su `retirar` agrega una condición (el tope diario) que la clase padre no tiene. Quien reciba una `CuentaRetirable` y retire un monto menor al saldo puede encontrarse con una excepción nueva, que es una forma más leve del mismo problema del CDT (precondición reforzada). Lo aceptamos porque el requerimiento exige que sea usable como origen de transferencias y en el cobro de la cuota, pero tiene una consecuencia: la cuota de manejo cuenta para el tope diario y un cobro masivo puede fallar en una cuenta infantil que ya retiró ese día. Cambiaríamos el contrato: que `CuentaRetirable` exponga `puedeRetirar(monto)` y que el cobro por lotes lo consulte, o separar el límite en una política inyectable.
2. **R3 y R4.** Para tener varios canales y varios registros creamos `NotificadorCompuesto` y `AuditoriaCompuesta`, que son el mismo patrón repetido. Además el antifraude quedó implementando la interfaz `Auditoria`, que no es lo que hace. Un nombre más honesto sería una interfaz genérica de "observadores de transacciones exitosas".
3. **`Main` crece.** Todos los requerimientos terminaron en `Main`, que es lo esperado en un *composition root*, pero con cada cambio se vuelve más largo. Si crece más habría que separar el armado en fábricas o módulos.

Anticipamos además, **sin haberlo implementado**, que el R6 de la revisión cruzada (pago de servicios) pondrá a prueba `transferir(CuentaRetirable origen, Cuenta destino, ...)`: un pago no tiene una cuenta destino sino una referencia de factura, y el texto del `NotificadorSms` dice "Transferiste ... a la cuenta ...".

**(d) ¿Qué nos dijo la otra pareja?** *(completar después de la revisión cruzada, y decir si estamos de acuerdo)*.

**(e) Argumento al jefe, con datos.** En la clase que mueve el dinero pasamos de 7 motivos de cambio a 1, de 2 dependencias concretas a 0 y de no poder probarla a tener pruebas que corren sin Oracle ni SMS. Un error que antes explotaba de noche con un CDT en el cobro de un millón de cuentas ahora no compila. Los cinco requerimientos del bloque 4 habrían tocado cuatro veces `TransaccionService` en el original; en el refactorizado no se tocó ninguna vez y las pruebas existentes siguieron pasando. Dos semanas de inversión se pagan con el primer incidente de producción evitado. (Con la salvedad honesta de (c): el diseño todavía tiene puntos por mejorar.)
