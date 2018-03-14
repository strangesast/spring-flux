package hello;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRecordRepository
    extends ReactiveMongoRepository<ApplicationRecord, String> {
}
