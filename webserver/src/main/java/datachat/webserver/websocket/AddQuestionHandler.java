package datachat.webserver.websocket;

import datachat.core.chat.ChatRepository;
import datachat.core.db.TargetDatabase;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AddQuestionHandler implements MessageHandler<InboundMessage.AddQuestion> {
    private final ChatRepository chatRepository;
    private final TargetDatabase targetDatabase;

    public AddQuestionHandler(ChatRepository chatRepository, TargetDatabase targetDatabase) {
        this.chatRepository = chatRepository;
        this.targetDatabase = targetDatabase;
    }

    @Override
    public Flux<OutboundMessage> handle(InboundMessage.AddQuestion inboundMessage) {
        return addToChatRepository(inboundMessage)
                .then(queryDatabase(inboundMessage))
                .flatMap(this::updateInChatRepository)
                .cast(OutboundMessage.class).flux();
    }


    private Mono<OutboundMessage.QuestionResult> updateInChatRepository(OutboundMessage.QuestionResult questionResult) {
        return chatRepository.setQuestionResult(
                        questionResult.chatId(),
                        questionResult.questionId(),
                        questionResult.question().queryResult(),
                        questionResult.question().error()
                )
                .thenReturn(questionResult);
    }

    private Mono<Void> addToChatRepository(InboundMessage.AddQuestion inboundMessage) {
        return chatRepository.addQuestion(inboundMessage.chatId(), inboundMessage.questionId(), inboundMessage.sqlQuery());
    }

    private Mono<OutboundMessage.QuestionResult> queryDatabase(InboundMessage.AddQuestion inboundMessage) {
        var message = new OutboundMessage.QuestionResult(
                inboundMessage.chatId(),
                inboundMessage.questionId(),
                new datachat.core.chat.Question(
                        inboundMessage.sqlQuery(),
                        null,
                        null
                )
        );
        return MessageUtils.captureResult(
                targetDatabase.executeQuery(inboundMessage.sqlQuery()),
                message::withQueryResult
        );
    }
}
