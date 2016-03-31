package com.teslamotors.artifacts.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.Lists;

public class FileNavigableObject extends NavigableObject {

	private final File file;
	public FileNavigableObject(File file) {
		this.file = file;
	}

	@Override
	protected List<NavigableObject> getIntermediateChildList() throws IOException {

		List<NavigableObject> navigable_objects = Lists.newArrayList();

		String[] file_list = this.file.list();
		if (file_list == null)
			throw new IOException( String.format("You probably don't have permission to access \"%s\"",
					this.file.getPath()) );

		for (String f : file_list)
			navigable_objects.add(new FileNavigableObject(new File(this.file, f)));

		return navigable_objects;
	}

	@Override
	public NavigableObject getParent() {
		return new FileNavigableObject(this.file.getParentFile());	
	}

	@Override
	public
	long lastModified() {
		return this.file.lastModified();
	}

	@Override
	public
	File asFile() {
		return this.file;
	}

	@Override
	public boolean isDirectory() {
		return this.file.isDirectory();
	}

	@Override
	public String getName() {
		return this.file.getName();
	}

	
	@Override
	public long getSize() {
		return this.file.length();
	}

	@Override
	public boolean exists() {
		return this.file.exists();
	}

	@Override
	public InputStream getInputStream() throws FileNotFoundException {
		return new FileInputStream(this.file);
	}
}