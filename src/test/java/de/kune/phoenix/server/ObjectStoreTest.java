package de.kune.phoenix.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import de.kune.phoenix.shared.Identifiable;

public class ObjectStoreTest {

	private ObjectStore<TestElement> store;

	@Before
	public void setUp() {
		store = new TransientInMemoryObjectStore<>();
	}

//	@Test
//	public void listenMultipleThreadAccess() {
//		final AtomicLong added = new AtomicLong();
//		final AtomicLong removed = new AtomicLong();
//		final AtomicLong updated = new AtomicLong();
//		store.addListener(t -> true, countingListener(added, removed, updated));
//		final int initial = 5000;
//		final int add = 500;
//		final int update = 250;
//		final int remove = 250;
//		fill(store, initial);
//		assertThat(store.get().size()).isEqualTo(initial);
//		ExecutorService addExecutor = execute(store, add, store::add, TestElement::new);
//		ExecutorService updateExecutor = execute(store, update, store::update, store::any);
//		ExecutorService removeExecutor = execute(store, remove, store::remove, store::any);
//		try {
//			addExecutor.awaitTermination(30, TimeUnit.SECONDS);
//			updateExecutor.awaitTermination(30, TimeUnit.SECONDS);
//			removeExecutor.awaitTermination(30, TimeUnit.SECONDS);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		assertThat(added.get()).isEqualTo(initial + add + update - updated.get());
//		assertThat(removed.get()).isGreaterThan(0).isLessThan(remove + 1);
//		assertThat(updated.get()).isGreaterThan(0).isLessThan(update + 1);
//	}

	@Test(timeout = 1000)
	public void awaitSingleThreadAccess() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				store.add(new TestElement());
			}
		}.start();
		Set<TestElement> result = store.await(t -> true);
		assertThat(result).isNotEmpty();
	}

	@Test(timeout = 15000)
	public void awaitMultipleThreadAccess() {
		execute(store, 500, store::add, TestElement::new);
		while (store.get().size() < 500) {
			assertThat(store.await(t -> true)).isNotEmpty();
		}
		assertThat(store.get().size()).isEqualTo(500);
	}

//	private ObjectStoreListener<TestElement> countingListener(final AtomicLong added, final AtomicLong removed,
//			final AtomicLong updated) {
//		return new ObjectStoreListener<TestElement>() {
//			@Override
//			public void added(TestElement object) {
//				added.getAndIncrement();
//			}
//
//			@Override
//			public void removed(TestElement object) {
//				removed.getAndIncrement();
//			}
//
//			@Override
//			public void updated(TestElement object) {
//				updated.getAndIncrement();
//			}
//		};
//	}

//	private void fill(ObjectStore<TestElement> store, int count) {
//		for (long i = 0; i < count; i++) {
//			store.add(new TestElement());
//		}
//	}

	private ExecutorService execute(final ObjectStore<TestElement> store, int count, Consumer<TestElement> consumer,
			Supplier<TestElement> supplier) {
		ExecutorService executor = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			executor.submit(() -> {
				try {
					Thread.sleep(500 + (int) (Math.random() * 500));
				} catch (InterruptedException e) {
				}
				TestElement element = supplier.get();
				assertThat(element).isNotNull();
				consumer.accept(element);
			});
		}
		noise(store, executor, 100);
		executor.shutdown();
		return executor;
	}

	private void noise(final ObjectStore<TestElement> store, ExecutorService executor, int count) {
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

	private class TestElement implements Identifiable<String> {

		private final String id;

		public TestElement() {
			this.id = store.generateId();
		}

		@Override
		public String getId() {
			return id;
		}

	}

}
