package datachat.webserver.websocket;

import reactor.util.annotation.Nullable;

public interface WithErrorMessage<T> {
    T withError(@Nullable String error);
}
