package hello;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Component
public class MainWebSocketHandler implements WebSocketHandler {

  /*
  @Autowired
  private ApplicationRecordRepository applicationRepo;
  */

  private static final ObjectMapper json = new ObjectMapper();

  private Flux<String> eventFlux = Flux.generate(sink -> {
    Event event = new Event(randomUUID().toString(), now().toString());
    try {
      sink.next(json.writeValueAsString(event));
    } catch (JsonProcessingException ex) {
      sink.error(ex);
    }
  });

  private Flux<String> intervalFlux = Flux.interval(Duration.ofMillis(1000L))
      .zipWith(eventFlux, (time, event) -> event);

  @Override
  public Mono<Void> handle(WebSocketSession webSocketSession) {
    return webSocketSession.send(intervalFlux
      .map(webSocketSession::textMessage))
      .and(webSocketSession.receive()
        .map(WebSocketMessage::getPayloadAsText).log());
  }

  public class Event {
    private String eventId;
    private String eventDt;

    public Event(String eventId, String eventDt) {
      this.eventId = eventId;
      this.eventDt = eventDt;
    }
  }

}
