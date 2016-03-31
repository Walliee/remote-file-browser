package com.teslamotors.artifacts.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.collect.Lists;

/**
 * This class has commonalities with both FileNavigableObject
 * and ZipMemberNavigableObject. Since we can only inherit from one,
 * we choose FileNavigableObject, and just re-use static methods
 * from ZipMemberNavigableObject.
 */
public class ZipParentNavigableObject extends FileNavigableObject {

	public static final String ZIP_EXTENSION = ".zip";

	private final List<ZipEntry> zip_entries;

	public List<ZipEntry> getEntries() {
		return this.zip_entries;
	}
	
	private ZipParentNavigableObject(File zip_file, List<ZipEntry> zip_entries) {
		super(zip_file);
		this.zip_entries = zip_entries;
	}

	public static ZipParentNavigableObject createNew(File zip_file) throws IOException {

		List<ZipEntry> zip_entries = Lists.newArrayList();
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip_file));

		ZipEntry zip_entry;
		while ( (zip_entry = zipIn.getNextEntry()) != null)
			zip_entries.add(zip_entry);

		zipIn.close();

		return new ZipParentNavigableObject(zip_file, zip_entries);
	}

	protected List<NavigableObject> getIntermediateChildList() throws IOException {
		return ZipMemberNavigableObject.getDirectChildMembers(this, this);
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public String getIconFilename() {
		return "zip_icon.png";
	}

	@Override
	public boolean isDownloadable() {
		return true;
	}
}
