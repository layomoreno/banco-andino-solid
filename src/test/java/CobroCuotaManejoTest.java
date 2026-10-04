import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CobroCuotaManejoTest {
    @Test
    void cobraLaCuotaATodasLasCuentasRetirables() {
        CuentaAhorros a = new CuentaAhorros("1", "Ana", 100_000);
        CuentaAhorros b = new CuentaAhorros("2", "Luis", 50_000);

        new CobroCuotaManejo().cobrarMensual(List.of(a, b));

        assertEquals(87_100, a.getSaldo(), 0.001);
        assertEquals(37_100, b.getSaldo(), 0.001);
    }

    @Test
    void elCDTSeRecibeDineroPeroNoTieneRetiro() {
        CDT cdt = new CDT("CDT-1", "Ana", 1_000_000, LocalDate.now().plusMonths(6));
        cdt.depositar(500);
        assertEquals(1_000_500, cdt.getSaldo(), 0.001);
        assertFalse(cdt.estaVencido(LocalDate.now()));
        assertTrue(cdt.estaVencido(LocalDate.now().plusMonths(7)));
        // new CobroCuotaManejo().cobrarMensual(List.of(cdt)); // no compila: eso ES la prueba del principio L
    }

    @Test
    void elMismoGeneradorDeExtractosSirveParaTodosLosProductos() {
        List<Extractable> todos = List.of(
            new CuentaAhorros("1", "Ana", 10),
            new CDT("CDT-1", "Ana", 10, LocalDate.now()),
            new TarjetaCredito(1_000),
            new CreditoVivienda(5_000));
        for (Extractable e : todos) assertFalse(e.generarExtracto().isBlank());
    }
}
