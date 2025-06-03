package com.sojka.pomeranian;

import com.datastax.oss.driver.api.core.CqlSession;
import com.sojka.pomeranian.chat.config.AstraConfig;
import com.sojka.pomeranian.chat.db.AstraTestcontainersConnector;
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
    AstraTestcontainersConnector connector;

    @Test
    void contextLoads() {
        System.out.println(config);

        connector.connect();
        CqlSession session = connector.getSession();
        session.execute("SELECT * FROM messages").all().forEach(r -> System.out.println(r.getFormattedContents()));
    }

}
