import java.util.List;

public class CobroCuotaManejo {
    private static final double CUOTA = 12_900;

    /** Recibe solo CuentaRetirable: pasar un CDT NO compila. */
    public void cobrarMensual(List<CuentaRetirable> cuentas) {
        for (CuentaRetirable cuenta : cuentas) {
            cuenta.retirar(CUOTA);
            System.out.println("Cuota de manejo cobrada a " + cuenta.getNumero());
        }
    }
}
