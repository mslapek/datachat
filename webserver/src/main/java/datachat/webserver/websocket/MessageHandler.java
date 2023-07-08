package datachat.webserver.websocket;

import reactor.core.publisher.Flux;

public interface MessageHandler<T extends InboundMessage> {
    Flux<OutboundMessage> handle(T inboundMessage);
}
