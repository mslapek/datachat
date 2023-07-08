package datachat.webserver;

import datachat.core.QueryResult;
import datachat.core.chat.ChatRepository;
import datachat.core.chat.InvalidQuestionIdException;
import datachat.core.chat.Question;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Primary
@Component
public class InMemoryChatRepository implements ChatRepository {
    private final HashMap<UUID, List<Question>> data = new HashMap<>();

    @Override
    public synchronized Mono<List<Question>> getAllQuestions(UUID chatId) {
        return Mono.just(List.copyOf(getOrCreateChat(chatId)));
    }

    @Override
    public synchronized Mono<Void> addQuestion(UUID chatId, int questionId, String sqlQuery) {
        var questions = getOrCreateChat(chatId);
        if (questionId != questions.size()) {
            return Mono.error(new InvalidQuestionIdException(chatId, questionId));
        }
        questions.add(new Question(sqlQuery, null, null));
        return Mono.empty();
    }

    private List<Question> getOrCreateChat(UUID chatId) {
        return data.computeIfAbsent(chatId, k -> new ArrayList<>());
    }

    @Override
    public synchronized Mono<Void> setQuestionResult(UUID chatId, int questionId, QueryResult result, String error) {
        var questions = getOrCreateChat(chatId);
        if (questionId >= questions.size()) {
            return Mono.error(new InvalidQuestionIdException(chatId, questionId));
        }
        questions.set(questionId, new Question(questions.get(questionId).sqlQuery(), result, error));
        return Mono.empty();
    }
}
