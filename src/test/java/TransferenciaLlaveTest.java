import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** R1: transferencias por llave. */
class TransferenciaLlaveTest {
    private final List<Transferencia> guardadas = new ArrayList<>();

    private TransaccionService servicio() {
        return new TransaccionService(
            List.of(new ReglaMontoPositivo(), new ReglaTopeDiario(5_000_000)),
            new CalculadorComisionPorTipo(List.of(new ComisionMismoBanco(), new ComisionLlave())),
            guardadas::add, t -> { }, t -> { }, t -> { });
    }

    @Test
    void transferenciaPorLlaveDescuentaExactamenteElMonto() {
        CuentaAhorros origen = new CuentaAhorros("1", "Ana", 1_000_000);
        CuentaAhorros destino = new CuentaAhorros("2", "Luis", 0);

        servicio().transferir(origen, destino, 50_000, "LLAVE");

        assertEquals(950_000, origen.getSaldo(), 0.001);
        assertEquals(50_000, destino.getSaldo(), 0.001);
        assertEquals(0, guardadas.get(0).comision(), 0.001);
    }
}
