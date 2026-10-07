/** R1: transferencia por llave (celular o cédula). Inmediata y sin comisión. */
public class ComisionLlave implements PoliticaComision {
    @Override public String tipo() { return "LLAVE"; }
    @Override public double calcular(double monto) { return 0; }
}
