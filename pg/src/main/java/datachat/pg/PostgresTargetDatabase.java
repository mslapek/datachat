package datachat.pg;

import datachat.core.QueryResult;
import datachat.core.db.DbSchema;
import datachat.core.db.TargetDatabase;
import io.r2dbc.spi.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostgresTargetDatabase implements TargetDatabase {
    private final ConnectionFactory connectionFactory;

    public PostgresTargetDatabase(PgConnectionConfig config) {
        this.connectionFactory = ConnectionFactories.get(config.getConnectionFactoryOptions());
    }

    @Override
    public Mono<DbSchema> getSchema() {
        return createConnection()
                .flatMapMany(connection -> connection.createStatement("SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public'").execute())
                .flatMap(result -> result
                        .map((row, rowMetadata) -> new Tuple(rowMetadata, parseRow(row)))
                ).collectList()
                .map(this::getSchema);
    }

    private record Tuple(RowMetadata rowMetadata, List<String> row) {}
    @Override
    public Mono<QueryResult> executeQuery(String sqlQuery) {
        return createConnection()
                .flatMapMany(connection -> connection.createStatement(sqlQuery).execute())
                .flatMap(result -> result
                        .map((row, rowMetadata) -> new Tuple(rowMetadata, parseRow(row)))
                ).collectList()
                .map(this::getQueryResult);
    }

    private DbSchema getSchema(List<Tuple> tuples) {
        // group by table name
        var tables = tuples.stream()
                .collect(
                        Collectors.groupingBy(
                                tuple -> tuple.row.get(0),
                                Collectors.mapping(
                                        tuple -> new DbSchema.Column(tuple.row.get(1), tuple.row.get(2)),
                                        Collectors.toList()
                                )
                        )
                )
                .entrySet().stream()
                .map(entry -> new DbSchema.Table(entry.getKey(), entry.getValue()))
                .toList();
        return new DbSchema(tables);
    }

    private Mono<Connection> createConnection() {
        return Mono.from(connectionFactory.create());
    }

    private QueryResult getQueryResult(List<Tuple> tuples) {
        var columnNames = getColumnNames(tuples.get(0).rowMetadata);
        var rows = tuples.stream().map(tuple -> tuple.row).toList();
        return new QueryResult(columnNames, rows);
    }

    private List<String> parseRow(Row row) {
        int n = row.getMetadata().getColumnMetadatas().size();
        var result = new String[n];
        for (int i = 0; i < n; i++) {
            var obj = row.get(i, Object.class);
            result[i] = obj == null ? null : obj.toString();
        }
        return List.of(result);
    }

    private List<String> getColumnNames(RowMetadata rowMetadata) {
        return rowMetadata.getColumnMetadatas().stream().map(ReadableMetadata::getName).toList();
    }
}
