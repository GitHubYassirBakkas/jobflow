package com.yassir.jobflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JobflowApplicationTests extends PostgresContainerTest {

	@Test
	void contextLoads() {
	}

}
