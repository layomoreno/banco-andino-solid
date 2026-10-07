/** R5: persistencia en PostgreSQL. OracleRepositorio se conserva por si hay que devolverse durante la migración. */
public class PostgresRepositorio implements RepositorioTransacciones {
    @Override public void guardar(Transferencia t) {
        System.out.println("[POSTGRES] Conectando a jdbc:postgresql://prod-db:5432/banco ...");
        System.out.println("[POSTGRES] INSERT INTO transacciones VALUES ('"
            + t.origen() + "', '" + t.destino() + "', " + t.monto() + ", " + t.comision() + ")");
    }
}
