package de.kune.phoenix.server;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.kune.phoenix.shared.Identifiable;

@RunWith(Parameterized.class)
public class ObjectStoreTest {

	@Parameters
	public static Collection<Object[]> data() {
		return asList(new Object[][] { { new TransientInMemoryObjectStore<>() }
				// , { new FileSystemBackedObjectStore<>() }
		});
	}

	private ObjectStore<TestElement, String> store;

	public ObjectStoreTest(ObjectStore<TestElement, String> store) {
		this.store = store;
	}

	@Before
	public void setUp() {
		store.clear();
	}

	@Test
	public void should_be_empty_initially() {
		assertThat(store.get()).isEmpty();
	}

	@Test
	public void should_be_empty_after_clear() {
		store.add(new TestElement());
		assertThat(store.get()).isNotEmpty();
		store.clear();
		assertThat(store.get()).isEmpty();
	}

	@Test
	public void should_contain_any_element_after_add() {
		store.add(new TestElement());
		assertThat(store.any()).isNotNull();
	}

	@Test
	public void shold_not_contain_element_after_remove() {
		TestElement testElement = new TestElement();
		store.add(testElement);
		assertThat(store.contains(testElement.getId())).isTrue();
		store.remove(testElement);
		assertThat(store.contains(testElement.getId())).isFalse();
	}

	@Test
	public void shold_not_contain_element_after_remove_by_predicate() {
		TestElement testElement = new TestElement();
		store.add(testElement);
		assertThat(store.contains(testElement.getId())).isTrue();
		store.remove(e -> e.getId().equals(testElement.getId()));
		assertThat(store.contains(testElement.getId())).isFalse();
	}

	@Test
	public void should_get_element_by_predicate_after_add() {
		TestElement testElement = new TestElement();
		assertThat(store.get()).isEmpty();
		store.add(testElement);
		assertThat(store.get(e -> e.getId().equals(testElement.getId()))).isNotEmpty();
	}

	@Test(expected = IllegalStateException.class)
	public void should_fail_to_add_one_element_twice() {
		TestElement testElement = new TestElement();
		assertThat(store.get()).isEmpty();
		store.add(testElement);
		store.add(testElement);
	}

	@Test
	public void should_get_all_elements_after_add() {
		List<TestElement> testElements = asList(new TestElement(), new TestElement());
		assertThat(store.get()).isEmpty();
		store.add(testElements.get(0));
		store.add(testElements.get(1));
		assertThat(store.get().size()).isSameAs(testElements.size());
		for (TestElement t : testElements) {
			assertThat(store.get()).contains(t);
		}
	}

	@Test(timeout = 1000)
	public void should_wait_for_object_being_added_single_threaded() {
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
	public void should_wait_for_objects_being_added_multi_threaded() {
		execute(store, 500, store::add, TestElement::new);
		while (store.get().size() < 500) {
			assertThat(store.await(t -> true)).isNotEmpty();
		}
		assertThat(store.get().size()).isEqualTo(500);
	}

	private ExecutorService execute(final ObjectStore<TestElement, String> store, int count,
			Consumer<TestElement> consumer, Supplier<TestElement> supplier) {
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

	private void noise(final ObjectStore<TestElement, String> store, ExecutorService executor, int count) {
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
			this.id = store.generateSequenceKey();
		}

		@Override
		public String getId() {
			return id;
		}

	}

}
