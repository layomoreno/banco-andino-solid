import java.time.Clock;
import java.time.LocalDate;

/**
 * R2: cuenta para menores de edad. Recibe depósitos sin límite, pero lo retirado en un mismo día
 * no puede superar TOPE_RETIRO_DIARIO. Es CuentaRetirable, así que sirve como origen de transferencias
 * y para el cobro de la cuota de manejo (esos retiros también cuentan para el tope).
 * El reloj se inyecta para poder probar el cambio de día sin esperar a mañana.
 */
public class CuentaInfantil extends CuentaRetirable {
    public static final double TOPE_RETIRO_DIARIO = 200_000;

    private final Clock reloj;
    private LocalDate diaDelAcumulado;
    private double retiradoHoy;

    public CuentaInfantil(String numero, String titular, double saldoInicial) {
        this(numero, titular, saldoInicial, Clock.systemDefaultZone());
    }

    public CuentaInfantil(String numero, String titular, double saldoInicial, Clock reloj) {
        super(numero, titular, saldoInicial);
        this.reloj = reloj;
    }

    @Override
    public void retirar(double monto) {
        LocalDate hoy = LocalDate.now(reloj);
        if (!hoy.equals(diaDelAcumulado)) {
            diaDelAcumulado = hoy;
            retiradoHoy = 0;
        }
        if (retiradoHoy + monto > TOPE_RETIRO_DIARIO) {
            throw new IllegalStateException("Supera el límite diario de retiros de la cuenta infantil");
        }
        super.retirar(monto);      // si el saldo no alcanza, lanza antes de acumular
        retiradoHoy += monto;
    }
}
