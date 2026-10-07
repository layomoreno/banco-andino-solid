import java.time.LocalDate;
import java.util.List;

/** Composition root: el ÚNICO lugar que conoce las clases concretas y arma el sistema. */
public class Main {
    public static void main(String[] args) {
        CuentaAhorros ana = new CuentaAhorros("001-1", "Ana", 2_000_000);
        CuentaAhorros luis = new CuentaAhorros("001-2", "Luis", 500_000);
        CDT cdtAna = new CDT("CDT-9", "Ana", 10_000_000, LocalDate.now().plusMonths(6));

        TransaccionService servicio = new TransaccionService(
            List.of(new ReglaMontoPositivo(), new ReglaTopeDiario(5_000_000)),
            new CalculadorComisionPorTipo(List.of(
                new ComisionMismoBanco(), new ComisionOtroBanco(), new ComisionInternacional(),
                new ComisionLlave())),
            new PostgresRepositorio(),   // para devolverse durante la migración: new OracleRepositorio(),
            new ComprobanteConsola(),
            new NotificadorCompuesto(List.of(
                new NotificadorSms(new SmsGateway()),
                new NotificadorPush(new PushGateway()))),
            new AuditoriaCompuesta(List.of(
                new AuditoriaConsola(),
                new AuditoriaAntifraude())));

        servicio.transferir(ana, luis, 150_000, "OTRO_BANCO");

        new CobroCuotaManejo().cobrarMensual(List.of(ana, luis));
        // new CobroCuotaManejo().cobrarMensual(List.of(ana, luis, cdtAna)); // <- NO COMPILA (control L)

        List<Extractable> productos =
            List.of(new TarjetaCredito(3_000_000), new CreditoVivienda(120_000_000));
        new GeneradorExtractos().imprimir(productos);
    }
}
