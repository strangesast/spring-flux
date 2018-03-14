package hello;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public class MainWebSocketHandler implements WebSocketHandler {
  @Override
  public Mono<Void> handle(WebSocketSession session) {
    System.out.println("got session");

    // switchMap table
    //

    return session.send(session.receive().doOnNext(WebSocketMessage::getPayload));
  }
}
