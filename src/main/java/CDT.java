import java.time.LocalDate;

/** Un CDT recibe dinero pero NO es CuentaRetirable: no existe retirar(), así que no puede usarse donde se retira. */
public class CDT extends Cuenta {
    private final LocalDate vencimiento;

    public CDT(String numero, String titular, double monto, LocalDate vencimiento) {
        super(numero, titular, monto);
        this.vencimiento = vencimiento;
    }

    public LocalDate getVencimiento() { return vencimiento; }

    public boolean estaVencido(LocalDate hoy) { return !hoy.isBefore(vencimiento); }

    @Override
    public String generarExtracto() {
        return "CDT " + numero + " - saldo: $" + saldo + " - vence: " + vencimiento;
    }
}
