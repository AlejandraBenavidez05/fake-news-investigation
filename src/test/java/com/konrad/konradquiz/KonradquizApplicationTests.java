package com.konrad.konradquiz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"app.encryption.secret-key=test-secret-32chars-change!!!!!",
		"app.admin.password=test-admin-password",
		"app.cors.allowed-origins=http://localhost:3000"
})
class KonradquizApplicationTests {

	@Test
	void contextLoads() {
		// Verifies the entire Spring context starts without errors
		// If any bean is misconfigured this test fails immediately
	}
}