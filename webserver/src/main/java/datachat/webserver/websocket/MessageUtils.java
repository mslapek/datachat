package datachat.webserver.websocket;

import reactor.core.publisher.Mono;

import java.util.function.Function;

public class MessageUtils {
    /**
     * Capture the result of a Mono and return a message with the result or an error.
     */
    public static <R, M extends WithErrorMessage<M>> Mono<M> captureResult(
            Mono<R> result,
            Function<R, M> messageWithResult
    ) {
        return result
                .map(messageWithResult)
                .onErrorResume(e -> Mono.just(
                        messageWithResult.apply(null)
                                .withError(e.getMessage())
                ));
    }
}
