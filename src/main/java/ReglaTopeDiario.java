public class ReglaTopeDiario implements ReglaTransferencia {
    private final double tope;
    public ReglaTopeDiario(double tope) { this.tope = tope; }
    @Override public void validar(double monto) {
        if (monto > tope) throw new IllegalArgumentException("Supera el tope diario");
    }
}
