package datachat.webserver.mock;

import datachat.core.db.DbSchema;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//@Component
public class MockTargetDatabase implements datachat.core.db.TargetDatabase {
    @Override
    public Mono<DbSchema> getSchema() {
        return Mono.just(new DbSchema(
            java.util.List.of(
                new DbSchema.Table(
                    "users",
                    java.util.List.of(
                        new DbSchema.Column("id", "int"),
                        new DbSchema.Column("name", "varchar"),
                        new DbSchema.Column("email", "varchar")
                    )
                ),
                new DbSchema.Table(
                    "messages",
                    java.util.List.of(
                        new DbSchema.Column("id", "int"),
                        new DbSchema.Column("sender_id", "int"),
                        new DbSchema.Column("receiver_id", "int"),
                        new DbSchema.Column("content", "varchar")
                    )
                )
            )
        ));
    }

    @Override
    public Mono<datachat.core.QueryResult> executeQuery(String sqlQuery) {
        if (sqlQuery.startsWith("error")) {
            return Mono.error(new RuntimeException("Error " + sqlQuery));
        }

        return Mono.just(new datachat.core.QueryResult(
                java.util.List.of(
                        "id",
                        "name",
                        "email"
                ),
                java.util.List.of(
                        java.util.List.of(
                                "1",
                                "Alice",
                                "example@example.com"
                        ),
                        java.util.List.of(
                                "2",
                                "Bob",
                                sqlQuery
                        )
                )
        ));
    }
}
