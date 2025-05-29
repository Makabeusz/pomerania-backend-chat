package com.sojka.pomeranian;

import com.datastax.oss.driver.api.core.CqlSession;
import com.sojka.pomeranian.chat.config.AstraConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PomeranianChatApplicationTests {

	@Autowired
	AstraConfig config;

	@Autowired
	CqlSession cqlSession;

	@Test
	void contextLoads() {
		System.out.println(config);


		cqlSession.execute("SELECT * FROM messages").all().forEach(r -> System.out.println(r.getFormattedContents()));
	}

}
