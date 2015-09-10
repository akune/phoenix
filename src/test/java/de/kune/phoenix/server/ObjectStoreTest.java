package de.kune.phoenix.server;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import de.kune.phoenix.server.ObjectStore.ObjectStoreListener;
import de.kune.phoenix.shared.Identifiable;

public class ObjectStoreTest {

	public static class TestIdentifiable implements Identifiable<Long> {

		private final Long id;

		public TestIdentifiable(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return id;
		}

	}

	@Test
	public void listenMultipleThreadAccess() {
		final AtomicLong added = new AtomicLong();
		final AtomicLong removed = new AtomicLong();
		final AtomicLong updated = new AtomicLong();
		final ObjectStore<TestIdentifiable> store = new DefaultObjectStore<TestIdentifiable, Long>();
		store.addListener(t -> true, countingListener(added, removed, updated));
		fill(store, 500);
		ExecutorService addExecutor = addObjects(store, 500);
		ExecutorService removeExecutor = removeObjects(store, 250);
		ExecutorService updateExecutor = updateObjects(store, 250);
		try {
			addExecutor.awaitTermination(30, TimeUnit.SECONDS);
			removeExecutor.awaitTermination(30, TimeUnit.SECONDS);
			updateExecutor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(1000L, added.get());
		assertEquals(250L, removed.get());
		assertEquals(250L, updated.get());
	}

	private ObjectStoreListener<TestIdentifiable> countingListener(final AtomicLong added, final AtomicLong removed,
			final AtomicLong updated) {
		return new ObjectStoreListener<TestIdentifiable>() {
			@Override
			public void added(TestIdentifiable object) {
				added.getAndIncrement();
			}

			@Override
			public void removed(TestIdentifiable object) {
				removed.getAndIncrement();
			}

			@Override
			public void updated(TestIdentifiable object) {
				updated.getAndIncrement();
			}
		};
	}

	private void fill(ObjectStore<TestIdentifiable> store, int count) {
		for (long i = 0; i < count; i++) {
			store.add(new TestIdentifiable(i));
		}
	}

	@Test
	public void awaitSingleThreadAccess() {
		final Long reference = System.currentTimeMillis();
		final ObjectStore<TestIdentifiable> store = new DefaultObjectStore<TestIdentifiable, Long>();
		new Thread() {
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				store.add(new TestIdentifiable(System.currentTimeMillis()));
			}
		}.start();
		store.await(t -> (reference < t.getId()));
	}

	@Test
	public void awaitMultipleThreadAccess() {
		final Long reference = 0L;
		final ObjectStore<TestIdentifiable> store = new DefaultObjectStore<TestIdentifiable, Long>();
		addObjects(store, 500);
		while (store.get().size() < 500) {
			store.await(t -> (reference < t.getId()));
		}
	}

	private ExecutorService addObjects(final ObjectStore<TestIdentifiable> store, int count) {
		ExecutorService executor = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			final Long number = (long) i;
			executor.submit(() -> {
				try {
					Thread.sleep(500 + (int) (Math.random() * 500));
				} catch (InterruptedException e) {
				}
				store.add(new TestIdentifiable(number));
				return null;
			});
		}
		noise(store, executor, 100);
		executor.shutdown();
		return executor;
	}

	private ExecutorService removeObjects(final ObjectStore<TestIdentifiable> store, int count) {
		ExecutorService executor = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			executor.submit(() -> {
				try {
					Thread.sleep(500 + (int) (Math.random() * 500));
				} catch (InterruptedException e) {
				}
				store.remove(store.get().iterator().next());
				return null;
			});
		}
		noise(store, executor, 100);
		executor.shutdown();
		return executor;
	}

	private ExecutorService updateObjects(final ObjectStore<TestIdentifiable> store, int count) {
		ExecutorService executor = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			executor.submit(() -> {
				try {
					Thread.sleep(500 + (int) (Math.random() * 500));
				} catch (InterruptedException e) {
				}
				store.update(store.get().iterator().next());
			});
		}
		noise(store, executor, 100);
		executor.shutdown();
		return executor;
	}

	private void noise(final ObjectStore<TestIdentifiable> store, ExecutorService executor, int count) {
		for (int i = 0; i < count; i++) {
			executor.submit(new Callable<Void>() {
				public Void call() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					store.get();
					return null;
				}
			});
		}
	}

}
