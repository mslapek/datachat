package datachat.core.chat;

import datachat.core.QueryResult;
import datachat.core.db.DbSchema;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Represents a chat repository.
 */
public interface ChatRepository {
    /**
     * Get all questions for the chat.
     * Returns empty list if the chat does not exist.
     */
    Mono<List<Question>> getAllQuestions(UUID chatId);

    /**
     * Add a new question to the chat.
     * Returns {@link InvalidQuestionIdException} if the question ID is invalid.
     */
    Mono<Void> addQuestion(UUID chatId, int questionId, String sqlQuery);

    /**
     * Set the result of a question.
     * Returns {@link InvalidQuestionIdException} if the question wasn't created with {@link #addQuestion}.
     */
    Mono<Void> setQuestionResult(UUID chatId, int questionId, @Nullable QueryResult result, @Nullable String error);
}
