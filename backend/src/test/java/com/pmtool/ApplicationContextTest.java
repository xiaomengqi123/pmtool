package com.pmtool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationContextTest {
    @Autowired UserRepository users;
    @Test void contextLoadsWithJpaSchemaAndSeedsAdmin() {
        assertThat(users.findByUsernameAndDeletedFalse("test-admin")).isPresent();
    }
}
