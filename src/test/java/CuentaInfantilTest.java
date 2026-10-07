import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** R2: cuenta infantil. */
class CuentaInfantilTest {

    /** Reloj que podemos adelantar para simular el paso de los días. */
    static class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-10-04T15:00:00Z");
        void avanzarUnDia() { ahora = ahora.plusSeconds(86_400); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojManual reloj = new RelojManual();

    @Test
    void siYaRetiro150milUnRetiroDe60milSeRechazaYElSaldoNoCambia() {
        CuentaInfantil cuenta = new CuentaInfantil("I-1", "Sofía", 1_000_000, reloj);
        cuenta.retirar(150_000);

        assertThrows(IllegalStateException.class, () -> cuenta.retirar(60_000));

        assertEquals(850_000, cuenta.getSaldo(), 0.001);
    }

    @Test
    void permiteRetirarExactamenteElTope() {
        CuentaInfantil cuenta = new CuentaInfantil("I-1", "Sofía", 1_000_000, reloj);
        cuenta.retirar(200_000);
        assertEquals(800_000, cuenta.getSaldo(), 0.001);
    }

    @Test
    void alDiaSiguienteElTopeSeReinicia() {
        CuentaInfantil cuenta = new CuentaInfantil("I-1", "Sofía", 1_000_000, reloj);
        cuenta.retirar(200_000);
        reloj.avanzarUnDia();
        cuenta.retirar(200_000);
        assertEquals(600_000, cuenta.getSaldo(), 0.001);
    }

    @Test
    void recibeDepositosSinLimite() {
        CuentaInfantil cuenta = new CuentaInfantil("I-1", "Sofía", 0, reloj);
        cuenta.depositar(50_000_000);
        assertEquals(50_000_000, cuenta.getSaldo(), 0.001);
    }

    @Test
    void unRetiroRechazadoPorSaldoNoConsumeElTope() {
        CuentaInfantil cuenta = new CuentaInfantil("I-1", "Sofía", 100_000, reloj);
        assertThrows(IllegalStateException.class, () -> cuenta.retirar(150_000));  // saldo insuficiente
        cuenta.retirar(100_000);                                                   // sigue habiendo cupo
        assertEquals(0, cuenta.getSaldo(), 0.001);
    }

    @Test
    void sirveComoOrigenDeTransferenciasYLaComisionCuentaParaElTope() {
        List<Transferencia> guardadas = new ArrayList<>();
        TransaccionService servicio = new TransaccionService(
            List.of(new ReglaMontoPositivo()),
            new CalculadorComisionPorTipo(List.of(new ComisionOtroBanco())),
            guardadas::add, t -> { }, t -> { }, t -> { });
        CuentaInfantil origen = new CuentaInfantil("I-1", "Sofía", 1_000_000, reloj);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        servicio.transferir(origen, destino, 100_000, "OTRO_BANCO");           // retira 107.500
        assertEquals(892_500, origen.getSaldo(), 0.001);

        assertThrows(IllegalStateException.class,                               // 107.500 + 100.000 > 200.000
            () -> servicio.transferir(origen, destino, 92_500, "OTRO_BANCO"));
        assertEquals(892_500, origen.getSaldo(), 0.001);
        assertEquals(1, guardadas.size());
    }

    @Test
    void seLeCobraLaCuotaDeManejoComoACualquierCuenta() {
        CuentaInfantil cuenta = new CuentaInfantil("I-1", "Sofía", 100_000, reloj);
        new CobroCuotaManejo().cobrarMensual(List.of(cuenta));
        assertEquals(87_100, cuenta.getSaldo(), 0.001);
    }
}
