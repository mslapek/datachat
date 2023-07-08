package datachat.core.db;

import datachat.core.QueryResult;
import reactor.core.publisher.Mono;

/**
 * Represents a target database.
 */
public interface TargetDatabase {
    /**
     * Get the schema of the target database.
     */
    Mono<DbSchema> getSchema();

    /**
     * Execute a query on the target database.
     */
    Mono<QueryResult> executeQuery(String sqlQuery);
}
