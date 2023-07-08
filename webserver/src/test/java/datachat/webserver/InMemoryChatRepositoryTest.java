package datachat.webserver;

import datachat.core.QueryResult;
import datachat.core.chat.InvalidQuestionIdException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryChatRepositoryTest {
    static final UUID FIRST_CHAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    static final UUID SECOND_CHAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void testEmpty() {
        var repo = new InMemoryChatRepository();

        var questions = repo.getAllQuestions(FIRST_CHAT_ID).block();

        assertNotNull(questions);
        assertEquals(0, questions.size());
    }

    @Test
    void testAddQuestion() {
        var repo = new InMemoryChatRepository();

        repo.addQuestion(FIRST_CHAT_ID, 0, "SELECT * FROM users").block();
        var questions = repo.getAllQuestions(FIRST_CHAT_ID).block();

        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("SELECT * FROM users", questions.get(0).sqlQuery());
    }

    @Test
    void testDuplicateAddQuestion() {
        var repo = new InMemoryChatRepository();

        repo.addQuestion(FIRST_CHAT_ID, 0, "SELECT * FROM users").block();

        StepVerifier.create(
                repo.addQuestion(FIRST_CHAT_ID, 0, "SELECT * FROM users")
        ).expectError(InvalidQuestionIdException.class).verify();
    }

    @Test
    void testAddTwoQuestions() {
        var repo = new InMemoryChatRepository();

        repo.addQuestion(FIRST_CHAT_ID, 0, "SELECT * FROM users").block();
        repo.addQuestion(FIRST_CHAT_ID, 1, "SELECT * FROM users WHERE id = 1").block();
        var questions = repo.getAllQuestions(FIRST_CHAT_ID).block();

        assertNotNull(questions);
        assertEquals(2, questions.size());
        assertEquals("SELECT * FROM users", questions.get(0).sqlQuery());
        assertEquals("SELECT * FROM users WHERE id = 1", questions.get(1).sqlQuery());
    }

    @Test
    void testTwoChats() {
        var repo = new InMemoryChatRepository();

        repo.addQuestion(FIRST_CHAT_ID, 0, "SELECT * FROM users").block();
        repo.addQuestion(SECOND_CHAT_ID, 0, "SELECT * FROM users WHERE id = 1").block();
        var firstQuestions = repo.getAllQuestions(FIRST_CHAT_ID).block();
        var secondQuestions = repo.getAllQuestions(SECOND_CHAT_ID).block();

        assertNotNull(firstQuestions);
        assertEquals(1, firstQuestions.size());
        assertEquals("SELECT * FROM users", firstQuestions.get(0).sqlQuery());

        assertNotNull(secondQuestions);
        assertEquals(1, secondQuestions.size());
        assertEquals("SELECT * FROM users WHERE id = 1", secondQuestions.get(0).sqlQuery());
    }

    @Test
    void testSetQuestionResult() {
        var repo = new InMemoryChatRepository();

        repo.addQuestion(FIRST_CHAT_ID, 0, "SELECT * FROM users").block();
        repo.setQuestionResult(FIRST_CHAT_ID, 0, new QueryResult(
                List.of("id", "name"),
                List.of(
                        List.of("1", "Alice"),
                        List.of("2", "Bob")
                )
        ), null).block();
        var questions = repo.getAllQuestions(FIRST_CHAT_ID).block();

        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals("SELECT * FROM users", questions.get(0).sqlQuery());
        assertEquals(new QueryResult(
                List.of("id", "name"),
                List.of(
                        List.of("1", "Alice"),
                        List.of("2", "Bob")
                )
        ), questions.get(0).queryResult());
    }
}