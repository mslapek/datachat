package datachat.core.chat;

import datachat.core.QueryResult;
import reactor.util.annotation.Nullable;

public record Question(
        String sqlQuery,
        @Nullable QueryResult queryResult,
        @Nullable String error
) {
    public Question withError(String error) {
        return new Question(sqlQuery, queryResult, error);
    }

    public Question withQueryResult(QueryResult queryResult) {
        return new Question(sqlQuery, queryResult, error);
    }
}
