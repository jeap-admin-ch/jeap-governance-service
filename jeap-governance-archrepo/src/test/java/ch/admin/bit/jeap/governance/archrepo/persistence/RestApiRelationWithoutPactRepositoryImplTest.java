package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.persistence.PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RestApiRelationWithoutPactRepositoryImpl.class)
class RestApiRelationWithoutPactRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaRestApiRelationWithoutPactRepository jpaRepository;

    @Autowired
    private RestApiRelationWithoutPactRepositoryImpl repository;

    @Test
    void add() {
        PersistenceTestUtility.TwoSystemComponents systemComponents = PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents(entityManager);
        SystemComponent systemComponent1 = systemComponents.first();
        SystemComponent systemComponent2 = systemComponents.second();


        RestApiRelationWithoutPact restApiRelationWithoutPact = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent1)
                .consumerSystemComponent(systemComponent2)
                .method("GET")
                .path("/api/test")
                .build();

        repository.add(restApiRelationWithoutPact);
        flushAndClear();

        RestApiRelationWithoutPact found = entityManager.find(RestApiRelationWithoutPact.class, restApiRelationWithoutPact.getId());
        assertNotNull(found);
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedAt());
        assertEquals("GET", found.getMethod());
        assertEquals("/api/test", found.getPath());
    }

    @Test
    void findByProviderSystemComponentId() {
        PersistenceTestUtility.TwoSystemComponents systemComponents = PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents(entityManager);
        SystemComponent systemComponent1 = systemComponents.first();
        SystemComponent systemComponent2 = systemComponents.second();
        RestApiRelationWithoutPact restApiRelationWithoutPact1 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent1)
                .consumerSystemComponent(systemComponent2)
                .method("GET")
                .path("/api/test")
                .build();
        RestApiRelationWithoutPact restApiRelationWithoutPact2 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent2)
                .consumerSystemComponent(systemComponent1)
                .method("GET")
                .path("/api/test")
                .build();
        repository.add(restApiRelationWithoutPact1);
        repository.add(restApiRelationWithoutPact2);
        flushAndClear();

        List<RestApiRelationWithoutPact> found = repository.findAllByProviderSystemComponentId(systemComponent1.getId());
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(restApiRelationWithoutPact1.getId(), found.getFirst().getId());
    }

    @Test
    void findByConsumerSystemComponentId() {
        PersistenceTestUtility.TwoSystemComponents systemComponents = PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents(entityManager);
        SystemComponent systemComponent1 = systemComponents.first();
        SystemComponent systemComponent2 = systemComponents.second();
        RestApiRelationWithoutPact restApiRelationWithoutPact1 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent1)
                .consumerSystemComponent(systemComponent2)
                .method("GET")
                .path("/api/test")
                .build();
        RestApiRelationWithoutPact restApiRelationWithoutPact2 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent2)
                .consumerSystemComponent(systemComponent1)
                .method("GET")
                .path("/api/test")
                .build();
        repository.add(restApiRelationWithoutPact1);
        repository.add(restApiRelationWithoutPact2);
        flushAndClear();

        List<RestApiRelationWithoutPact> found = repository.findAllByConsumerSystemComponentId(systemComponent1.getId());
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(restApiRelationWithoutPact2.getId(), found.getFirst().getId());
    }

    @Test
    void findAllByProviderSystemComponentId_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        List<RestApiRelationWithoutPact> found = repository.findAllByProviderSystemComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByConsumerSystemComponentId_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        List<RestApiRelationWithoutPact> found = repository.findAllByConsumerSystemComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void delete() {
        PersistenceTestUtility.TwoSystemComponents systemComponents = PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents(entityManager);
        SystemComponent systemComponent1 = systemComponents.first();
        SystemComponent systemComponent2 = systemComponents.second();


        RestApiRelationWithoutPact restApiRelationWithoutPact = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent1)
                .consumerSystemComponent(systemComponent2)
                .method("GET")
                .path("/api/test")
                .build();
        repository.add(restApiRelationWithoutPact);
        flushAndClear();

        RestApiRelationWithoutPact found = entityManager.find(RestApiRelationWithoutPact.class, restApiRelationWithoutPact.getId());
        assertNotNull(found);

        repository.delete(found);
        flushAndClear();

        RestApiRelationWithoutPact deleted = entityManager.find(RestApiRelationWithoutPact.class, restApiRelationWithoutPact.getId());
        assertNull(deleted);
    }

    @Test
    void deleteAllByProviderSystemComponentId() {
        PersistenceTestUtility.TwoSystemComponents systemComponentsSystem1 = createAndPersistSystemWithTwoSystemComponents("TestSystem1", entityManager);
        PersistenceTestUtility.TwoSystemComponents systemComponentsSystem2 = createAndPersistSystemWithTwoSystemComponents("TestSystem2", entityManager);
        SystemComponent systemComponent11 = systemComponentsSystem1.first();
        SystemComponent systemComponent12 = systemComponentsSystem1.second();
        SystemComponent systemComponent21 = systemComponentsSystem2.first();

        RestApiRelationWithoutPact restApiRelationWithoutPact1112 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent11)
                .consumerSystemComponent(systemComponent12)
                .method("GET")
                .path("/api/test")
                .build();
        RestApiRelationWithoutPact restApiRelationWithoutPact1121 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent11)
                .consumerSystemComponent(systemComponent21)
                .method("GET")
                .path("/api/test")
                .build();
        RestApiRelationWithoutPact restApiRelationWithoutPact1211 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent12)
                .consumerSystemComponent(systemComponent11)
                .method("GET")
                .path("/api/test")
                .build();
        RestApiRelationWithoutPact restApiRelationWithoutPact1221 = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(systemComponent12)
                .consumerSystemComponent(systemComponent21)
                .method("GET")
                .path("/api/test")
                .build();

        repository.add(restApiRelationWithoutPact1112);
        repository.add(restApiRelationWithoutPact1121);
        repository.add(restApiRelationWithoutPact1211);
        repository.add(restApiRelationWithoutPact1221);
        flushAndClear();


        Iterable<RestApiRelationWithoutPact> all = jpaRepository.findAll();
        assertThat(all).hasSize(4);

        repository.deleteAllByProviderSystemComponentId(systemComponent11.getId());
        flushAndClear();

        Iterable<RestApiRelationWithoutPact> allAfterDeletion = jpaRepository.findAll();
        assertThat(allAfterDeletion).hasSize(2);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
