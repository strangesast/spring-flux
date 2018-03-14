package hello;

import hello.ProductTrackerProperties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.QueryableStoreRegistry;
import org.springframework.cloud.stream.binder.kafka.streams.annotations.KafkaStreamsProcessor;
import org.springframework.cloud.stream.schema.avro.AvroSchemaMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@EnableBinding(KafkaStreamsProcessor.class)
@EnableAutoConfiguration
@EnableConfigurationProperties(ProductTrackerProperties.class)
@EnableScheduling
public class StreamsApplication {

  private static final String STORE_NAME = "prod-id-count-store";

  @Autowired
  private QueryableStoreRegistry queryableStoreRegistry;

  @Autowired
  ProductTrackerProperties productTrackerProperties;

  ReadOnlyKeyValueStore<Object, Object> keyValueStore;

  @Bean
  public MessageConverter avroMessageConverter() {
    return new AvroSchemaMessageConverter(MimeType.valueOf("avro/bytes"));
  }

  /**
   * Whew.
   */
  @StreamListener("input")
  @SendTo("output")
  public KStream<String, Long> process(KStream<Object, Product> input) {
    return input
        .filter((key, product) -> productIds().contains(product.getId()))
        .map((key, value) -> new KeyValue<>(String.valueOf(value.id), value))
        .groupByKey(Serialized.with(Serdes.String(), new JsonSerde<>(Product.class)))
        .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(STORE_NAME)
          .withKeySerde(Serdes.String())
          .withValueSerde(Serdes.Long()))
        .toStream();
  }

  private Set<String> productIds() {
    return StringUtils.commaDelimitedListToSet(productTrackerProperties.getProductIds())
        .stream()
        .collect(Collectors.toSet());
  }

  /**
   * Whew.
   */
  @Scheduled(fixedRate = 30000, initialDelay = 5000)
  public void printProductCounts() {
    if (keyValueStore == null) {
      keyValueStore = queryableStoreRegistry.getQueryableStoreType(
          STORE_NAME,
          QueryableStoreTypes.keyValueStore()
      );
    }

    for (String id : productIds()) {
      System.out.printf("Product ID: %s Count: %d\n", id, keyValueStore.get(id));
    }
  }
}
