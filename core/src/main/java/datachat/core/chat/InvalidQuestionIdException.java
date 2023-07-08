package datachat.core.chat;

import java.util.UUID;

public class InvalidQuestionIdException extends Exception {
    public InvalidQuestionIdException(UUID chatId, int questionId) {
        super("Question with id " + questionId + " in " + chatId + " already exists or is too large.");
    }
}
