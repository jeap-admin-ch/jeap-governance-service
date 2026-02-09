package ch.admin.bit.jeap.governance.archrepo.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "RestApiRelationWithoutPact")
@Table(name = "ar_rest_api_relation_without_pact")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class RestApiRelationWithoutPact {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ar_rest_api_relation_without_pact_seq")
    @SequenceGenerator(
            name = "ar_rest_api_relation_without_pact_seq",
            sequenceName = "ar_rest_api_relation_without_pact_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @NonNull
    @Getter
    String method;

    @NonNull
    @Getter
    String path;

    @ManyToOne
    @Getter
    private SystemComponent providerSystemComponent;

    @ManyToOne
    @Getter
    private SystemComponent consumerSystemComponent;

    @Getter
    private ZonedDateTime createdAt;

    private RestApiRelationWithoutPact(String method, String path, SystemComponent providerSystemComponent, SystemComponent consumerSystemComponent, ZonedDateTime zonedDateTime) {
        this.method = method;
        this.path = path;
        this.providerSystemComponent = providerSystemComponent;
        this.consumerSystemComponent = consumerSystemComponent;
        this.createdAt = zonedDateTime;
    }

    @Builder
    private static RestApiRelationWithoutPact build(String method, String path, SystemComponent providerSystemComponent, SystemComponent consumerSystemComponent, ZonedDateTime createdAt) {
        return new RestApiRelationWithoutPact(method, path, providerSystemComponent, consumerSystemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
