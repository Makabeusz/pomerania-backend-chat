package com.sojka.pomeranian;

import com.sojka.pomeranian.chat.PomeranianChatApplication;
import org.springframework.boot.SpringApplication;

public class TestPomeranianChatApplication {

	public static void main(String[] args) {
		SpringApplication.from(PomeranianChatApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
