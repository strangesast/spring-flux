package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;


@RestController
@RequestMapping(value = "/applications")
public class ApplicationRecordController {

  @Autowired
  private ApplicationRecordRepository repository;

  @GetMapping("")
  public Flux<ApplicationRecord> getAllApplications() {
    return repository.findAll();
  }

  @PostMapping("")
  public Mono<ApplicationRecord> createApplications(
      @Valid @RequestBody ApplicationRecord application) {
    return repository.save(application);
  }

  /**
   * Whew.
   */
  @GetMapping("/{id}")
  public Mono<ResponseEntity<ApplicationRecord>> getApplicationById(
      @PathVariable(value = "id") String applicationId) {
    return repository
      .findById(applicationId)
      .map(savedApplication -> ResponseEntity.ok(savedApplication))
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /**
   * Whew.
   */
  @PutMapping("/{id}")
  public Mono<ResponseEntity<ApplicationRecord>> updateApplication(
      @PathVariable(value = "id") String applicationId,
      @Valid @RequestBody ApplicationRecord application) {
    return repository.findById(applicationId)
      .flatMap(existingApplication -> {
        existingApplication.setName(application.getName());
        return repository.save(existingApplication);
      })
      .map(updatedApplication -> new ResponseEntity<>(updatedApplication, HttpStatus.OK))
      .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  /**
   * Whew.
   */
  @DeleteMapping("/application/{id}")
  public Mono<ResponseEntity<Void>> deleteApplication(
      @PathVariable(value = "id") String applicationId) {
    return repository.findById(applicationId)
      .flatMap(existingApplication -> repository.delete(existingApplication)
          .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK))))
      .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ApplicationRecord> streamAllApplications() {
    return repository.findAll();
  }
}
