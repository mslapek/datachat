package datachat.webserver.websocket;

import datachat.core.QueryResult;
import datachat.core.chat.Question;
import datachat.core.db.DbSchema;
import datachat.core.db.TargetDatabase;
import datachat.webserver.mock.MockChatRepository;
import datachat.webserver.mock.MockTargetDatabase;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

class StartChatHandlerTest {
    static final UUID firstChatId = UUID.fromString("12340000-0000-0000-0000-000000000000");
    @Test
    void handleStartChat() {
        var targetDatabase = new MockTargetDatabase();
        var chatRepository = new MockChatRepository();
        var handler = new StartChatHandler(targetDatabase, chatRepository);

        DbSchema expectedSchema = targetDatabase.getSchema().block();
        OutboundMessage expectedDatabaseMessage = new OutboundMessage.DatabaseSchema(
                firstChatId,
                expectedSchema,
                null
        );
        OutboundMessage expectedAllQuestionsMessage = new OutboundMessage.AllQuestions(
                firstChatId,
                chatRepository.getAllQuestions(firstChatId).block(),
                null
        );

        StepVerifier.create(handler.handle(new InboundMessage.StartChat(firstChatId)))
                .expectNext(expectedDatabaseMessage)
                .expectNext(expectedAllQuestionsMessage)
                .verifyComplete();
    }

    @Test
    void handleStartChatWithGetSchemaError() {
        var targetDatabase = new TargetDatabase() {
            @Override
            public Mono<DbSchema> getSchema() {
                return Mono.error(new RuntimeException("Error getting schema XYZ"));
            }

            @Override
            public Mono<QueryResult> executeQuery(String sqlQuery) {
                return null;
            }
        };
        var chatRepository = new MockChatRepository();
        var handler = new StartChatHandler(targetDatabase, chatRepository);

        OutboundMessage expectedDatabaseMessage = new OutboundMessage.DatabaseSchema(
                firstChatId,
                null,
                "Error getting schema XYZ"
        );

        StepVerifier.create(handler.handle(new InboundMessage.StartChat(firstChatId)))
                .expectNext(expectedDatabaseMessage)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void handleStartChatWithGetAllQuestionsError() {
        var targetDatabase = new MockTargetDatabase();
        var chatRepository = new MockChatRepository() {
            @Override
            public Mono<List<Question>> getAllQuestions(UUID chatId) {
                return Mono.error(new RuntimeException("Error getting questions"));
            }
        };
        var handler = new StartChatHandler(targetDatabase, chatRepository);

        OutboundMessage expectedAllQuestionsMessage = new OutboundMessage.AllQuestions(
                firstChatId,
                null,
                "Error getting questions"
        );

        StepVerifier.create(handler.handle(new InboundMessage.StartChat(firstChatId)))
                .expectNextCount(1)
                .expectNext(expectedAllQuestionsMessage)
                .verifyComplete();
    }
}