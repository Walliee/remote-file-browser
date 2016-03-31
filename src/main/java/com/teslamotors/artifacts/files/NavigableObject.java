package com.teslamotors.artifacts.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.teslamotors.artifacts.files.SortUtils.SortCriteria;

public abstract class NavigableObject {

	public static final String INDEX_FILENAME = "index.html";
	public static final String PARENT_LINK_TEXT = "..";
	
	
	public boolean hasIndex() throws IOException {
		return getIndex() != null;
	}

	public NavigableObject getIndex() throws IOException{
		for (NavigableObject f : listChildren())
			if (f.getName().equals(INDEX_FILENAME))
				return f;

		return null;
	}
	
	public abstract String getName();

	final public Collection<NavigableObject> listChildren() throws IOException {
		return this.listChildren(new ArrayList<SortCriteria>());
	}
	
	public String getDisplayName() {
		return this.is_parent_link ? PARENT_LINK_TEXT : this.getName();
	}

	public abstract long lastModified();

	/**
	 * Even though the underlying implementation might not be a File,
	 * in some cases it is convenient to pretend that it is.
	 */
	public abstract File asFile();
	
	public abstract boolean isDirectory();

	public abstract long getSize();

	static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
		.appendHourOfHalfday(1)
		.appendLiteral(':')
		.appendMinuteOfHour(2)
		.appendHalfdayOfDayText()
		.appendLiteral(" on ")
		.appendMonthOfYearText().appendLiteral(' ')
		.appendDayOfMonth(0).appendLiteral(", ")
		.appendYear(0, 4)
		.toFormatter();

	static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
		.appendHours().appendSuffix(" hr ")
		.appendMinutes().appendSuffix(" min ")
		.appendSeconds().appendSuffix(" sec")
		.printZeroNever()
		.toFormatter();

	public String renderAge() {

		DateTime file_modification_date = new DateTime(lastModified());
		
//		System.out.println("Modification datetime: " + file_modification_date);
		DateTime now = new DateTime();
//		System.out.println("now datetime: " + now);
		
		Period period = new Period(file_modification_date, now);

		Duration time_ago = period.toDurationTo(now);
		// FIXME
		if (false && time_ago.isShorterThan(Duration.standardDays(1))) {
			
//			System.out.println("Hours: " + period.getHours());
			return PERIOD_FORMATTER.print(period) + " ago";
			
		} else
			return DATE_FORMATTER.print(file_modification_date);
	}

	public abstract NavigableObject getParent();
	
	
	public abstract boolean exists();

	public abstract InputStream getInputStream() throws IOException;

	private boolean is_parent_link;
	public void setIsParentLink(boolean b) {
		this.is_parent_link = b;
	}
	
	public boolean getIsParentLink() {
		return this.is_parent_link;
	}

	static private final Predicate<NavigableObject> IS_NOT_HIDDEN = new Predicate<NavigableObject>() {
		@Override
		public boolean apply(NavigableObject nav) {
			return !nav.getDisplayName().startsWith(".");
		}
	};
	
	final public Collection<NavigableObject> listChildren(Collection<SortCriteria> sorters) throws IOException {
		
		Collection<NavigableObject> navigable_objects = Collections2.filter(
				getIntermediateChildList(), IS_NOT_HIDDEN);

		// Add the parent link if we are not at the top
		// This conditional is a weird way of checking if we are at the top.
		// It seems to work for now...
		if ( !this.getName().isEmpty() ) {	
			NavigableObject parent = this.getParent();
			parent.setIsParentLink(true);
			navigable_objects.add(parent);
		}

		return SortUtils.sortByCriteria(navigable_objects, sorters);
	}
	

	abstract List<NavigableObject> getIntermediateChildList() throws IOException;

	public String getIconFilename() {
		if (this.isDirectory()) {
			return "folder_icon.png";
		} else {
			if (this.getName().endsWith(ZipParentNavigableObject.ZIP_EXTENSION))
				return "zip_icon.png";
			else
				return "file_icon.png";
		}
	}

	public boolean isDownloadable() {
		return !this.isDirectory() && !getIsParentLink();
	}
}