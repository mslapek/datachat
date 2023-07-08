package datachat.webserver.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import datachat.core.QueryResult;
import datachat.core.chat.Question;
import datachat.core.db.DbSchema;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OutboundMessage.QuestionResult.class, name = "questionResult"),
        @JsonSubTypes.Type(value = OutboundMessage.AllQuestions.class, name = "allQuestions"),
        @JsonSubTypes.Type(value = OutboundMessage.DatabaseSchema.class, name = "databaseSchema"),
})
public sealed interface OutboundMessage {
    record QuestionResult(UUID chatId, int questionId, Question question) implements OutboundMessage, WithErrorMessage<QuestionResult> {
        public QuestionResult withError(String error) {
            return new QuestionResult(chatId, questionId, question.withError(error));
        }

        public QuestionResult withQueryResult(QueryResult queryResult) {
            return new QuestionResult(chatId, questionId, question.withQueryResult(queryResult));
        }
    }

    record AllQuestions(UUID chatId, @Nullable List<Question> questions, @Nullable String error) implements OutboundMessage, WithErrorMessage<AllQuestions> {
        public AllQuestions withError(String error) {
            return new AllQuestions(chatId, questions, error);
        }

        public AllQuestions withQuestions(List<Question> questions) {
            return new AllQuestions(chatId, questions, error);
        }
    }

    record DatabaseSchema(UUID chatId, @Nullable DbSchema schema, @Nullable String error) implements OutboundMessage, WithErrorMessage<DatabaseSchema> {
        public DatabaseSchema withError(String error) {
            return new DatabaseSchema(chatId, schema, error);
        }

        public DatabaseSchema withSchema(DbSchema dbSchema) {
            return new DatabaseSchema(chatId, dbSchema, error);
        }
    }
}
