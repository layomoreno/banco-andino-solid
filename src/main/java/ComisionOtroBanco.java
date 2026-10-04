public class ComisionOtroBanco implements PoliticaComision {
    @Override public String tipo() { return "OTRO_BANCO"; }
    @Override public double calcular(double monto) { return 7_500; }
}
