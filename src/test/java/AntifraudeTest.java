import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** R4: auditoría y antifraude por cada transferencia exitosa; ninguna si se rechaza. */
class AntifraudeTest {
    private final List<Transferencia> auditadas = new ArrayList<>();
    private final List<Transferencia> antifraude = new ArrayList<>();

    private TransaccionService servicio() {
        return new TransaccionService(
            List.of(new ReglaMontoPositivo()),
            new CalculadorComisionPorTipo(List.of(new ComisionMismoBanco())),
            t -> { }, t -> { }, t -> { },
            new AuditoriaCompuesta(List.of(auditadas::add, antifraude::add)));
    }

    @Test
    void cadaTransferenciaExitosaPasaPorAuditoriaYAntifraude() {
        servicio().transferir(new CuentaAhorros("1", "Ana", 100_000),
                              new CuentaAhorros("2", "Luis", 0), 10_000, "MISMO_BANCO");
        assertEquals(1, auditadas.size());
        assertEquals(1, antifraude.size());
    }

    @Test
    void unaTransferenciaRechazadaNoGeneraNiAuditoriaNiAntifraude() {
        assertThrows(IllegalStateException.class, () ->
            servicio().transferir(new CuentaAhorros("1", "Ana", 1_000),
                                  new CuentaAhorros("2", "Luis", 0), 10_000, "MISMO_BANCO"));
        assertThrows(IllegalArgumentException.class, () ->
            servicio().transferir(new CuentaAhorros("1", "Ana", 100_000),
                                  new CuentaAhorros("2", "Luis", 0), 0, "MISMO_BANCO"));
        assertTrue(auditadas.isEmpty());
        assertTrue(antifraude.isEmpty());
    }
}
