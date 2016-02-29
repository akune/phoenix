package de.kune.phoenix.server;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.kune.phoenix.shared.Identifiable;

public class FileSystemBackedObjectStore<T extends Identifiable<I>, I> extends LockingObjectStore<T, I> {

	public static <T extends Identifiable<I>, I> FileSystemBackedObjectStore<T, I> getInstance(String directory) {
		FileSystemBackedObjectStore<T, I> store = new FileSystemBackedObjectStore<>();
		store.fileSystemLocation = directory;
		store.init();
		return store;
	}

	private static final Pattern filenamePattern = Pattern.compile("(?<id>.*?)\\[(?<type>.*?)\\]\\.json");

	private ObjectMapper mapper = new ObjectMapper();

	@Value("${filesystemlocation:store}")
	private String fileSystemLocation;

	private Path path;

	private File file;

	@PostConstruct
	protected void init() {
		file = new File(fileSystemLocation);
		if (!file.exists()) {
			file.mkdirs();
		}
		if (!file.isDirectory()) {
			throw new IllegalStateException(format("file [%s] exists and is not a directory"));
		}
		path = file.toPath();
	}

	@Override
	protected void doClear() {
		for (File f : file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return filenamePattern.matcher(name).matches();
			}
		})) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	protected boolean doesContain(I id) {
		return file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				Matcher m = filenamePattern.matcher(name);
				return m.matches() && m.group("id").equals(id.toString());
			}
		}).length > 0;
	}

	@Override
	protected void doPut(T object) {
		requireNonNull(object);
		requireNonNull(object.getId());
		try {
			OutputStream os = new FileOutputStream(
					path.resolve(object.getId().toString() + "[" + object.getClass().getName() + "].json").toFile());
			mapper.writeValue(os, object);
			os.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected Set<T> doGetAll() {
		Set<T> result = new LinkedHashSet<>();
		for (File f : file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return filenamePattern.matcher(name).matches();
			}
		})) {
			result.add(read(f));
		}
		return result;
	}

	private T read(File file) {
		Matcher m = filenamePattern.matcher(file.getName());
		m.find();
		String valueTypeName = m.group("type");
		try {
			@SuppressWarnings("unchecked")
			Class<T> valueType = (Class<T>) Class.forName(valueTypeName);
			FileInputStream inputStream = new FileInputStream(file);
			return mapper.readValue(inputStream, valueType);
		} catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}

	}

	@Override
	protected Iterator<T> doIterate() {
		return new Iterator<T>() {
			private File[] files;
			int index = -1;

			{
				files = file.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return filenamePattern.matcher(name).matches();
					}
				});
			}

			@Override
			public boolean hasNext() {
				return index < files.length - 1;
			}

			@Override
			public T next() {
				return read(files[++index]);
			}

			@Override
			public void remove() {
				try {
					Files.delete(files[index].toPath());
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		};
	}

	@Override
	protected T doGetAny() {
		Iterator<T> iterator = doIterate();
		if (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

	@Override
	protected void doRemove(I id) {
		for (File f : file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				Matcher m = filenamePattern.matcher(name);
				return m.matches() && m.group("id").equals(id.toString());
			}
		})) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

}
