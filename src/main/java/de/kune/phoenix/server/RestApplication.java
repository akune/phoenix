package de.kune.phoenix.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jackson.JacksonFeature;

@ApplicationPath("api")
public class RestApplication extends Application {
	
	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(MessageResource.class);
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		final Set<Object> instances = new HashSet<Object>();
		instances.add(new JacksonFeature());
		instances.add(new LoggingFilter());
		return instances;
	}
}
