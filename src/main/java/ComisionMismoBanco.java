public class ComisionMismoBanco implements PoliticaComision {
    @Override public String tipo() { return "MISMO_BANCO"; }
    @Override public double calcular(double monto) { return 0; }
}
