public class ReglaMontoPositivo implements ReglaTransferencia {
    @Override public void validar(double monto) {
        if (monto <= 0) throw new IllegalArgumentException("Monto inválido");
    }
}
