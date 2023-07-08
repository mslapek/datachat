package datachat.webserver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DataChatWebSocketHandler implements WebSocketHandler {
    final ObjectMapper objectMapper;
    final MessageHandler<InboundMessage.StartChat> startChatMessageHandler;
    final MessageHandler<InboundMessage.AddQuestion> addQuestionMessageHandler;


    public DataChatWebSocketHandler(ObjectMapper objectMapper, MessageHandler<InboundMessage.StartChat> startChatMessageHandler, MessageHandler<InboundMessage.AddQuestion> addQuestionMessageHandler) {
        this.objectMapper = objectMapper;
        this.startChatMessageHandler = startChatMessageHandler;
        this.addQuestionMessageHandler = addQuestionMessageHandler;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var allOutbound = session
                .receive()
                .map(this::parseInbound)
                .flatMap(this::handle)
                .map(this::formatOutbound)
                .map(session::textMessage);

        return session.send(allOutbound);
    }

    Flux<OutboundMessage> handle(InboundMessage inboundMessage) {
        if (inboundMessage instanceof InboundMessage.StartChat startChat) {
            return startChatMessageHandler.handle(startChat);
        } else if (inboundMessage instanceof InboundMessage.AddQuestion addQuestion) {
            return addQuestionMessageHandler.handle(addQuestion);
        } else {
            return Flux.empty();
        }
    }


    private String formatOutbound(OutboundMessage outboundMessage) {
        try {
            return objectMapper.writeValueAsString(outboundMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InboundMessage parseInbound(WebSocketMessage message) {
        try {
            return objectMapper.readValue(message.getPayloadAsText(), InboundMessage.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
