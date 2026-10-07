import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/** R5: PostgreSQL es solo otra implementación de RepositorioTransacciones. */
class PostgresRepositorioTest {

    private String capturar(RepositorioTransacciones repo, Transferencia t) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try { repo.guardar(t); } finally { System.setOut(original); }
        return buffer.toString();
    }

    @Test
    void guardaEnPostgresYOracleSigueDisponibleComoAlternativa() {
        Transferencia t = new Transferencia("OTRO_BANCO", "001-1", "Ana", "001-2", 150_000, 7_500);

        String salidaPostgres = capturar(new PostgresRepositorio(), t);
        String salidaOracle = capturar(new OracleRepositorio(), t);

        assertTrue(salidaPostgres.contains("[POSTGRES]"));
        assertFalse(salidaPostgres.contains("[ORACLE]"));
        assertTrue(salidaOracle.contains("[ORACLE]"));
    }
}
