import java.util.List;

/** Ejecuta varios registros posteriores a una transferencia exitosa (auditoría, antifraude, ...). */
public class AuditoriaCompuesta implements Auditoria {
    private final List<Auditoria> registros;
    public AuditoriaCompuesta(List<Auditoria> registros) { this.registros = registros; }

    @Override public void registrar(Transferencia t) {
        for (Auditoria registro : registros) registro.registrar(t);
    }
}
