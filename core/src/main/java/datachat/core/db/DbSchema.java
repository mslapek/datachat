package datachat.core.db;

import java.util.List;

public record DbSchema(List<Table> tables) {
    public record Table(String name, List<Column> columns) {}
    public record Column(String name, String type) {}
}
