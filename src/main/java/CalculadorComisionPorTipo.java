import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculadorComisionPorTipo implements CalculadorComision {
    private final Map<String, PoliticaComision> politicas = new HashMap<>();

    public CalculadorComisionPorTipo(List<PoliticaComision> politicas) {
        for (PoliticaComision p : politicas) this.politicas.put(p.tipo(), p);
    }

    @Override public double calcular(String tipo, double monto) {
        PoliticaComision politica = politicas.get(tipo);
        if (politica == null) throw new IllegalArgumentException("Tipo de transferencia desconocido");
        return politica.calcular(monto);
    }
}
