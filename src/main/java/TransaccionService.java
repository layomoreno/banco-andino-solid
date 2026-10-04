import java.util.List;

/** Coordina el flujo de una transferencia delegando cada paso en una abstracción. */
public class TransaccionService {
    private final List<ReglaTransferencia> reglas;
    private final CalculadorComision calculadorComision;
    private final RepositorioTransacciones repositorio;
    private final ComprobanteTransferencia comprobante;
    private final NotificadorTransferencias notificador;
    private final Auditoria auditoria;

    public TransaccionService(List<ReglaTransferencia> reglas,
                              CalculadorComision calculadorComision,
                              RepositorioTransacciones repositorio,
                              ComprobanteTransferencia comprobante,
                              NotificadorTransferencias notificador,
                              Auditoria auditoria) {
        this.reglas = reglas;
        this.calculadorComision = calculadorComision;
        this.repositorio = repositorio;
        this.comprobante = comprobante;
        this.notificador = notificador;
        this.auditoria = auditoria;
    }

    /** El origen debe ser CuentaRetirable: transferir desde un CDT no compila. */
    public void transferir(CuentaRetirable origen, Cuenta destino, double monto, String tipo) {
        for (ReglaTransferencia regla : reglas) regla.validar(monto);
        double comision = calculadorComision.calcular(tipo, monto);

        origen.retirar(monto + comision);
        destino.depositar(monto);

        Transferencia t = new Transferencia(tipo, origen.getNumero(), origen.getTitular(),
                                            destino.getNumero(), monto, comision);
        repositorio.guardar(t);
        comprobante.emitir(t);
        notificador.notificar(t);
        auditoria.registrar(t);
    }
}
