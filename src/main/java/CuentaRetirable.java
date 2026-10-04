/** Cuenta de la que el cliente puede retirar libremente. Es el tipo que exigen transferir y cobrar cuota. */
public abstract class CuentaRetirable extends Cuenta {
    protected CuentaRetirable(String numero, String titular, double saldoInicial) {
        super(numero, titular, saldoInicial);
    }

    public void retirar(double monto) {
        if (monto > saldo) throw new IllegalStateException("Saldo insuficiente");
        saldo -= monto;
    }
}
