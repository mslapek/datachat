package datachat.webserver.websocket;

import datachat.core.chat.ChatRepository;
import datachat.core.db.TargetDatabase;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class StartChatHandler implements MessageHandler<InboundMessage.StartChat> {
    final TargetDatabase targetDatabase;
    final ChatRepository chatRepository;

    public StartChatHandler(TargetDatabase targetDatabase, ChatRepository chatRepository) {
        this.targetDatabase = targetDatabase;
        this.chatRepository = chatRepository;
    }

    @Override
    public Flux<OutboundMessage> handle(InboundMessage.StartChat startChat) {
        var chatId = startChat.chatId();
        var schemaMessage = new OutboundMessage.DatabaseSchema(chatId, null, null);
        var schemaMono = MessageUtils.captureResult(
                targetDatabase.getSchema(),
                schemaMessage::withSchema
        );

        var questionsMessage = new OutboundMessage.AllQuestions(chatId, null, null);
        var questionsMono = MessageUtils.captureResult(
                chatRepository.getAllQuestions(chatId),
                questionsMessage::withQuestions
        );

        return Flux.merge(schemaMono, questionsMono);
    }
}
