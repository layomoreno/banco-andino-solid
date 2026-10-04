/** Datos de una transferencia ya realizada. Es un valor inmutable, no una dependencia. */
public record Transferencia(String tipo, String origen, String titularOrigen,
                            String destino, double monto, double comision) { }
