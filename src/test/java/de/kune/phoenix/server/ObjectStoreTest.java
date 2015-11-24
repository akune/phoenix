package de.kune.phoenix.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Test;

import de.kune.phoenix.server.ObjectStore.ObjectStoreListener;
import de.kune.phoenix.shared.Identifiable;

public class ObjectStoreTest {

	public static class RandomTestIdentifiable extends TestIdentifiable {
		private static final Random RANDOM = new Random();

		public RandomTestIdentifiable() {
			super(RANDOM.nextLong());
		}
	}

	public static class TestIdentifiable implements Identifiable<Long> {

		private static final AtomicLong GENERATOR = new AtomicLong();

		private final Long id;

		public TestIdentifiable(Long id) {
			// this.id = id;
			this.id = GENERATOR.getAndIncrement();
		}

		public TestIdentifiable() {
			// this.id = id;
			this.id = GENERATOR.getAndIncrement();
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
		final int initial = 5000;
		final int add = 500;
		final int update = 250;
		final int remove = 250;
		fill(store, initial);
		assertThat(store.get().size()).isEqualTo(initial);
		ExecutorService addExecutor = execute(store, add, store::add, TestIdentifiable::new);
		ExecutorService updateExecutor = execute(store, update, store::update, store::any);
		ExecutorService removeExecutor = execute(store, remove, store::remove, store::any);
		try {
			addExecutor.awaitTermination(30, TimeUnit.SECONDS);
			updateExecutor.awaitTermination(30, TimeUnit.SECONDS);
			removeExecutor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(added.get()).isEqualTo(initial + add + update - updated.get());
		assertThat(removed.get()).isGreaterThan(0).isLessThan(remove + 1);
		assertThat(updated.get()).isGreaterThan(0).isLessThan(update + 1);
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

	@Test(timeout = 1000)
	public void awaitSingleThreadAccess() {
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
		Set<TestIdentifiable> result = store.await(t -> true);
		assertThat(result).isNotEmpty();
	}

	@Test(timeout = 15000)
	public void awaitMultipleThreadAccess() {
		final ObjectStore<TestIdentifiable> store = new DefaultObjectStore<TestIdentifiable, Long>();
		final AtomicLong id = new AtomicLong();
		execute(store, 500, store::add, () -> new TestIdentifiable(id.getAndIncrement()));
		while (store.get().size() < 500) {
			assertThat(store.await(t -> true)).isNotEmpty();
		}
		assertThat(store.get().size()).isEqualTo(500);
	}

	private ExecutorService execute(final ObjectStore<TestIdentifiable> store, int count,
			Consumer<TestIdentifiable> consumer, Supplier<TestIdentifiable> supplier) {
		ExecutorService executor = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			executor.submit(() -> {
				try {
					Thread.sleep(500 + (int) (Math.random() * 500));
				} catch (InterruptedException e) {
				}
				TestIdentifiable element = supplier.get();
				assertThat(element).isNotNull();
				consumer.accept(element);
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
