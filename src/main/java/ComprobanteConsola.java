/** Si legal cambia el formato del comprobante, se toca SOLO este archivo. */
public class ComprobanteConsola implements ComprobanteTransferencia {
    @Override public void emitir(Transferencia t) {
        System.out.println("===== BANCO ANDINO - COMPROBANTE =====");
        System.out.println("Origen: " + t.origen());
        System.out.println("Destino: " + t.destino());
        System.out.println("Monto: $" + t.monto());
        System.out.println("Comisión: $" + t.comision());
        System.out.println("======================================");
    }
}
