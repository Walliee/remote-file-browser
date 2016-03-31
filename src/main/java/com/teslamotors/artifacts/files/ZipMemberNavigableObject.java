package com.teslamotors.artifacts.files;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.collect.Lists;

public class ZipMemberNavigableObject extends NavigableObject {

	private final ZipParentNavigableObject zip_parent;
	
	/**
	 * This is the path within the Zip file.
	 */
	private final String relative_file_reference;
	
	public ZipMemberNavigableObject(ZipParentNavigableObject zip_parent, String relative_file_reference) {
		this.zip_parent = zip_parent;
		this.relative_file_reference = relative_file_reference;
	}
	
	private String normalizeDirName(String non_normalized) {

		if (non_normalized.endsWith("/"))
			return non_normalized.substring(0, non_normalized.length() - 1);
		return non_normalized;
	}
	
	private ZipEntry getZipEntry() {

		for (ZipEntry entry : this.zip_parent.getEntries()) {
			String stripped_ze_name = normalizeDirName(entry.getName());
			String stripped_relpath_name = normalizeDirName(this.relative_file_reference);
			if (stripped_ze_name.equals(stripped_relpath_name))
				return entry;
		}
		
		return null;
	}
	
	/**
	 * Find all zip children that exist below the current path.
	 */
	@Override
	protected List<NavigableObject> getIntermediateChildList() throws IOException {
		
		return getDirectChildMembers(this, this.zip_parent);
	}

	@Override
	public
	long lastModified() {
		return getZipEntry().getTime();
	}
	
	public boolean isDirectory() {

		ZipEntry ze = getZipEntry();
		
		if (ze != null) {
			boolean is_dir = ze.isDirectory();
			return is_dir;
		}
		
		return false;
	}
	
	@Override
	public File asFile() {
		return new File(this.zip_parent.asFile(), this.relative_file_reference);
	}

	@Override
	public String getName() {
		return getZipEntry().getName();
	}

	public String getDisplayName() {
		return getIsParentLink() ? PARENT_LINK_TEXT : asFile().getName();
	}

	@Override
	public long getSize() {

		ZipEntry ze = getZipEntry();
		if (ze != null)
			return getZipEntry().getSize();
		
		return 0;
	}

	@Override
	public boolean exists() {
		// TODO FIXME
		return true;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(this.zip_parent.asFile()));

		InputStream bais = null;
		ZipEntry zip_entry;
		while ( (zip_entry = zipIn.getNextEntry()) != null) {
			if (zip_entry.getName().equals(this.relative_file_reference)) {
				byte[] bytes = new byte[(int) zip_entry.getSize()];

				int totalBytesRead = 0;
				int bytes_left;
				while ( (bytes_left = bytes.length - totalBytesRead) > 0)
					totalBytesRead += zipIn.read(bytes, totalBytesRead, bytes_left);		

				bais = new ByteArrayInputStream(bytes);
				break;
			}
		}
		
		zipIn.close();
		return bais;
	}

	@Override
	public NavigableObject getParent() {
		
		// TODO If we're already at the "root" level within the Zip file, then the parent is the
		// containing filesystem directory.
		
		File relative_path_as_file = new File(this.relative_file_reference);
		String parent_path = relative_path_as_file.getParent();
		
		if (parent_path == null) {
			return this.zip_parent;
		} else {
			return new ZipMemberNavigableObject(this.zip_parent, parent_path);
		}
	}

	public static List<NavigableObject> getDirectChildMembers(NavigableObject self, ZipParentNavigableObject zip_parent_container) {
		
		List<NavigableObject> navigable_objects = Lists.newArrayList();
	
		for (ZipEntry ze : zip_parent_container.getEntries()) {
			NavigableObject potential_child = new ZipMemberNavigableObject(zip_parent_container, ze.getName());
			if (potential_child.getParent().getName().equals(self.getName()))
				navigable_objects.add( potential_child );
		}
		
		return navigable_objects;
	}
}