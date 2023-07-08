package datachat.webserver.websocket;

import datachat.core.QueryResult;
import datachat.core.chat.InvalidQuestionIdException;
import datachat.core.db.DbSchema;
import datachat.core.db.TargetDatabase;
import datachat.webserver.mock.MockChatRepository;
import datachat.webserver.mock.MockTargetDatabase;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AddQuestionHandlerTest {
    static UUID firstChatId = UUID.fromString("12340000-0000-0000-0000-000000000000");

    @Test
    void testHandle() {
        var chatRepository = new MockChatRepository();
        var targetDatabase = new MockTargetDatabase();
        var handler = new AddQuestionHandler(chatRepository, targetDatabase);

        StepVerifier.create(handler.handle(new InboundMessage.AddQuestion(firstChatId, 4, "SELECT * FROM table1")))
                .expectNext(new OutboundMessage.QuestionResult(
                        firstChatId,
                        4,
                        new datachat.core.chat.Question(
                                "SELECT * FROM table1",
                                targetDatabase.executeQuery("SELECT * FROM table1").block(),
                                null
                        )
                ))
                .verifyComplete();

        var allQuestions = chatRepository.getAllQuestions(firstChatId).block();
        assertEquals(5, allQuestions.size());

        var lastQuestion = allQuestions.get(4);
        assertEquals(lastQuestion.sqlQuery(), "SELECT * FROM table1");
        assertNull(lastQuestion.error());
        assertEquals(lastQuestion.queryResult(), targetDatabase.executeQuery("SELECT * FROM table1").block());
    }

    @Test
    void testDuplicateQuestionIdIsIdempotent() {
        var chatRepository = new MockChatRepository();
        var targetDatabase = new MockTargetDatabase();
        var handler = new AddQuestionHandler(chatRepository, targetDatabase);

        StepVerifier.create(handler.handle(new InboundMessage.AddQuestion(firstChatId, 1, "SELECT * FROM table1")))
                .verifyError(InvalidQuestionIdException.class);

        var allQuestions = chatRepository.getAllQuestions(firstChatId).block();
        assertEquals(4, allQuestions.size());

        var questionOne = allQuestions.get(1);
        // The question should not have been changed
        assertEquals(questionOne.sqlQuery(), "SELECT * FROM users WHERE id = 1");
    }

    @Test
    void testDatabaseErrorIsStored() {
        var chatRepository = new MockChatRepository();
        var targetDatabase = new TargetDatabase() {
            @Override
            public Mono<DbSchema> getSchema() {
                return null;
            }

            @Override
            public Mono<QueryResult> executeQuery(String sqlQuery) {
                return Mono.error(new RuntimeException("Table 'table2' does not exist"));
            }
        };
        var handler = new AddQuestionHandler(chatRepository, targetDatabase);

        StepVerifier.create(handler.handle(new InboundMessage.AddQuestion(firstChatId, 4, "SELECT * FROM table2")))
                .expectNext(new OutboundMessage.QuestionResult(
                        firstChatId,
                        4,
                        new datachat.core.chat.Question(
                                "SELECT * FROM table2",
                                null,
                                "Table 'table2' does not exist"
                        )
                ))
                .verifyComplete();

        var allQuestions = chatRepository.getAllQuestions(firstChatId).block();
        assertEquals(5, allQuestions.size());

        var lastQuestion = allQuestions.get(4);
        assertEquals(lastQuestion.sqlQuery(), "SELECT * FROM table2");
        assertNull(lastQuestion.queryResult());
        assertEquals(lastQuestion.error(), "Table 'table2' does not exist");
    }
}