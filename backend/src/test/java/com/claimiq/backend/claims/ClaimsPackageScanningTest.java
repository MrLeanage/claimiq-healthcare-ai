package com.claimiq.backend.claims;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.claimiq.BackendApplication;
import com.claimiq.backend.claims.api.ApiExceptionHandler;
import com.claimiq.backend.claims.api.ClaimController;
import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.mapper.ClaimMapper;
import com.claimiq.backend.claims.repository.ClaimRepository;
import com.claimiq.backend.claims.service.ClaimService;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

class ClaimsPackageScanningTest {
    @Test
    void applicationDiscoversClaimComponentsAndWiresTheirDependencies() {
        new ApplicationContextRunner()
                .withUserConfiguration(BackendApplication.class)
                .withBean(ClaimRepository.class, () -> mock(ClaimRepository.class))
                .withPropertyValues(
                        "spring.autoconfigure.exclude="
                                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
                .run(
                        context -> {
                            assertNull(context.getStartupFailure());
                            assertNotNull(context.getBean(ClaimController.class));
                            assertNotNull(context.getBean(ApiExceptionHandler.class));
                            assertNotNull(context.getBean(ClaimService.class));
                            assertNotNull(context.getBean(ClaimMapper.class));
                        });
    }

    @Test
    void entityAndRepositoryRemainUnderTheApplicationScanRoot() {
        String scanRoot = BackendApplication.class.getPackageName();
        var entityScanner = new ClassPathScanningCandidateComponentProvider(false);
        entityScanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        assertTrue(
                entityScanner.findCandidateComponents(scanRoot).stream()
                        .anyMatch(bean -> Claim.class.getName().equals(bean.getBeanClassName())));

        var repositoryScanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                        return definition.getMetadata().isIndependent();
                    }
                };
        repositoryScanner.addIncludeFilter(new AssignableTypeFilter(ClaimRepository.class));
        assertTrue(
                repositoryScanner.findCandidateComponents(scanRoot).stream()
                        .anyMatch(
                                bean ->
                                        ClaimRepository.class
                                                .getName()
                                                .equals(bean.getBeanClassName())));
    }
}
