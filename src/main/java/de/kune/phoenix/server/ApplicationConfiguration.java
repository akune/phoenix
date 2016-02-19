package de.kune.phoenix.server;

import javax.inject.Singleton;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.kune.phoenix.shared.Message;

@Configuration
public class ApplicationConfiguration {

	@Bean
	@Singleton
	public ObjectStore<Message> messageStore() {
		return new TransientInMemoryObjectStore<>();
	}

}
