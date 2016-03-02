package de.kune.phoenix.server;

import javax.inject.Singleton;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import de.kune.phoenix.shared.Message;

@Configuration
public class ApplicationConfiguration {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	@Singleton
	public ObjectStore<Message, String> messageStore() {
		return new FileSystemBackedObjectStore<>();
	}

}
