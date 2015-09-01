package de.kune.phoenix.server;

import java.util.function.Predicate;

import org.junit.Test;

import de.kune.phoenix.server.MessageResource.ObjectStore;

public class ObjectStoreTest {

	@Test
	public void singleThreadAccess() {
		final Long reference = System.currentTimeMillis();
		final ObjectStore<Long> store = new ObjectStore<Long>();
		new Thread() {
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				store.add(System.currentTimeMillis());
			}
		}.start();
		store.await(new Predicate<Long>() {
			@Override
			public boolean test(Long t) {
				return reference < t;
			}
		});
	}

	@Test
	public void multipleThreadAccess() {
		final Long reference = 0L;
		final ObjectStore<Long> store = new ObjectStore<Long>();
		for (int i = 0; i < 500; i++) {
			final Long number = (long) i;
			new Thread() {
				public void run() {
					try {
						Thread.sleep(500 + (int) (Math.random() * 500));
					} catch (InterruptedException e) {
					}
					store.add(number);
				}
			}.start();
		}
		for (int i = 0; i < 1500; i++) {
			new Thread() {
				public void run() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					store.get();
				}
			}.start();
		}
		while (store.get().size() < 500) {
			store.await(new Predicate<Long>() {
				@Override
				public boolean test(Long t) {
					return reference < t;
				}
			});
		}
	}

}
