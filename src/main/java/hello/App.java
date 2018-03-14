package hello;

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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.QueryableStoreRegistry;
import org.springframework.cloud.stream.binder.kafka.streams.annotations.KafkaStreamsProcessor;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class App {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @EnableBinding(KafkaStreamsProcessor.class)
  @EnableAutoConfiguration
  @EnableConfigurationProperties(ProductTrackerProperties.class)
  @EnableScheduling
  public static class InteractiveProductCountApplication {

    private static final String STORE_NAME = "prod-id-count-store";

    @Autowired
    private QueryableStoreRegistry queryableStoreRegistry;

    @Autowired
    ProductTrackerProperties productTrackerProperties;

    ReadOnlyKeyValueStore<Object, Object> keyValueStore;

    /**
     * Whew.
     */
    @StreamListener("input")
    @SendTo("output")
    public KStream<String, Long> process(KStream<Object, Product> input) {
      Serialized<String, Product> serialized = Serialized.with(
          Serdes.String(),
          new JsonSerde<>(Product.class));

      /*
      Materialized<Integer, Long, KeyValueStore<Bytes, byte[]>> materialized = Materialized
          .<Integer, Long, KeyValueStore<Bytes, byte[]>>as(STORE_NAME)
          .withKeySerde(Serdes.Integer())
          .withValueSerde(Serdes.Long());
      */
      Materialized<String, Long, KeyValueStore<Bytes, byte[]>> materialized = Materialized
          .<String, Long, KeyValueStore<Bytes, byte[]>>as(STORE_NAME)
          .withKeySerde(Serdes.String())
          .withValueSerde(Serdes.Long());
      return input
          .filter((key, product) -> productIds().contains(product.getId()))
          .map((key, value) -> new KeyValue<>(String.valueOf(value.id), value))
          .groupByKey(Serialized.with(Serdes.String(), new JsonSerde<>(Product.class)))
          .count(materialized)
          .toStream();
    }

    private Set<Integer> productIds() {
      return StringUtils.commaDelimitedListToSet(productTrackerProperties.getProductIds())
          .stream().map(Integer::parseInt).collect(Collectors.toSet());
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

      for (Integer id : productIds()) {
        System.out.printf("Product ID: %d Count: %d\n", id, keyValueStore.get(String.valueOf(id)));
      }
    }

  }

  @ConfigurationProperties(prefix = "app.product.tracker")
  static class  ProductTrackerProperties {

    private String productIds;

    public String getProductIds() {
      return productIds;
    }

    public void setProductIds(String productIds) {
      this.productIds = productIds;
    }

  }

  static class Product {

    Integer id;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }
  }
}
