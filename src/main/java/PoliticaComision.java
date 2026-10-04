/** Una política por tipo de transferencia. Un tipo nuevo = una clase nueva que implementa esto. */
public interface PoliticaComision {
    String tipo();
    double calcular(double monto);
}
