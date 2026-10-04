import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Todas las pruebas usan dobles en memoria: sin Oracle, sin SMS, en milisegundos. */
class TransaccionServiceTest {

    // ---- dobles de prueba ----
    private final List<Transferencia> guardadas = new ArrayList<>();
    private final List<Transferencia> notificadas = new ArrayList<>();

    private TransaccionService servicio(List<PoliticaComision> politicas) {
        return new TransaccionService(
            List.of(new ReglaMontoPositivo(), new ReglaTopeDiario(5_000_000)),
            new CalculadorComisionPorTipo(politicas),
            guardadas::add,          // RepositorioTransacciones en memoria
            t -> { },                // comprobante que no imprime
            notificadas::add,        // notificador que anota en vez de enviar
            t -> { });               // auditoría silenciosa
    }

    private TransaccionService servicio() {
        return servicio(List.of(new ComisionMismoBanco(), new ComisionOtroBanco(), new ComisionInternacional()));
    }

    // ---- las 5 pruebas del laboratorio ----

    @Test
    void mismoBancoNoCobraComisionYMueveExactamenteElMonto() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 1_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 100_000);

        servicio().transferir(origen, destino, 200_000, "MISMO_BANCO");

        assertEquals(800_000, origen.getSaldo(), 0.001);
        assertEquals(300_000, destino.getSaldo(), 0.001);
        assertEquals(0, guardadas.get(0).comision(), 0.001);
    }

    @Test
    void otroBancoCobra7500YDescuentaMontoMasComision() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 1_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0.01);

        servicio().transferir(origen, destino, 150_000, "OTRO_BANCO");

        assertEquals(7_500, guardadas.get(0).comision(), 0.001);
        assertEquals(842_500, origen.getSaldo(), 0.001);
        assertEquals(150_000.01, destino.getSaldo(), 0.001);
    }

    @Test
    void saldoInsuficienteRechazaYNoGuardaNiNotifica() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 100_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        assertThrows(IllegalStateException.class,
            () -> servicio().transferir(origen, destino, 150_000, "OTRO_BANCO"));

        assertEquals(100_000, origen.getSaldo(), 0.001);
        assertEquals(0, destino.getSaldo(), 0.001);
        assertTrue(guardadas.isEmpty());
        assertTrue(notificadas.isEmpty());
    }

    @Test
    void transferenciaExitosaSeGuardaUnaVezYNotificaUnaVez() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 1_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        servicio().transferir(origen, destino, 50_000, "MISMO_BANCO");

        assertEquals(1, guardadas.size());
        assertEquals(1, notificadas.size());
    }

    @Test
    void tipoDesconocidoSeRechazaYElSaldoNoCambia() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 1_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        assertThrows(IllegalArgumentException.class,
            () -> servicio().transferir(origen, destino, 50_000, "MARCIANA"));

        assertEquals(1_000_000, origen.getSaldo(), 0.001);
        assertTrue(guardadas.isEmpty());
        assertTrue(notificadas.isEmpty());
    }

    // ---- pruebas extra: validaciones, internacional y principio abierto/cerrado ----

    @Test
    void rechazaMontoNoPositivoYMontoSobreElTope() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 10_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        assertThrows(IllegalArgumentException.class, () -> servicio().transferir(origen, destino, 0, "MISMO_BANCO"));
        assertThrows(IllegalArgumentException.class, () -> servicio().transferir(origen, destino, 5_000_001, "MISMO_BANCO"));
        assertEquals(10_000_000, origen.getSaldo(), 0.001);
    }

    @Test
    void internacionalCobraTresPorCientoMasVeinticincoMil() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 2_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        servicio().transferir(origen, destino, 1_000_000, "INTERNACIONAL");

        assertEquals(55_000, guardadas.get(0).comision(), 0.001);
    }

    @Test
    void tipoNuevoSeAgregaSinModificarCodigoExistente() {
        PoliticaComision premium = new PoliticaComision() {
            public String tipo() { return "PREMIUM"; }
            public double calcular(double monto) { return 1_000; }
        };
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 100_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        servicio(List.of(new ComisionMismoBanco(), premium)).transferir(origen, destino, 10_000, "PREMIUM");

        assertEquals(89_000, origen.getSaldo(), 0.001);
    }
}
