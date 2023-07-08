package datachat.webserver.mock;

import datachat.core.QueryResult;
import datachat.core.chat.ChatRepository;
import datachat.core.chat.InvalidQuestionIdException;
import datachat.core.chat.Question;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MockChatRepository implements ChatRepository {
    private final List<Question> data = new ArrayList<>(getSampleData());

    @Override
    public synchronized Mono<List<Question>> getAllQuestions(UUID chatId) {
        return Mono.just(List.copyOf(data));
    }

    @Override
    public synchronized Mono<Void> addQuestion(UUID chatId, int questionId, String sqlQuery) {
        if (questionId != data.size()) {
            return Mono.error(new InvalidQuestionIdException(chatId, questionId));
        }

        data.add(new Question(sqlQuery, null, null));
        return Mono.empty();
    }

    @Override
    public synchronized Mono<Void> setQuestionResult(UUID chatId, int questionId, QueryResult result, String error) {
        if (questionId >= data.size()) {
            return Mono.error(new InvalidQuestionIdException(chatId, questionId));
        }

        data.set(questionId, new Question(data.get(questionId).sqlQuery(), result, error));
        return Mono.empty();
    }

    public static List<Question> getSampleData() {
        return List.of(
                new Question(
                        "SELECT * FROM users",
                        new QueryResult(
                                List.of("id", "name"),
                                List.of(
                                        List.of("1", "Alice"),
                                        List.of("2", "Bob")
                                )
                        ),
                        null
                ),
                new Question(
                        "SELECT * FROM users WHERE id = 1",
                        new QueryResult(
                                List.of("id", "name"),
                                List.of(
                                        List.of("1", "Alice")
                                )
                        ),
                        null
                ),
                new Question(
                        "SELECT * FROM users WHERE id = 2",
                        new QueryResult(
                                List.of("id", "name"),
                                List.of(
                                        List.of("2", "Bob")
                                )
                        ),
                        null
                ),
                new Question(
                        "SELECT * FROM users WHERE id = 3",
                        null,
                        "Table 'users' does not contain row with id = 3"
                )
        );
    }
}
