package datachat.webserver.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InboundMessage.StartChat.class, name = "startChat"),
        @JsonSubTypes.Type(value = InboundMessage.AddQuestion.class, name = "addQuestion"),
})
public sealed interface InboundMessage {
    record StartChat(UUID chatId) implements InboundMessage {
    }

    record AddQuestion(UUID chatId, int questionId, String sqlQuery) implements InboundMessage {
    }
}
