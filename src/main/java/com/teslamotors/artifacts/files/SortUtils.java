package com.teslamotors.artifacts.files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class SortUtils {

	public static Collection<SortCriteria> getSortersFromRequest(HttpServletRequest request) {
		String[] values = request.getParameterValues("sort");
		if (values == null)
			return Lists.newArrayList();

		return Collections2.transform(Arrays.asList(values), new Function<String, SortCriteria>() {
			@Override
			public SortCriteria apply(String input) {
				return SortCriteria.valueOf(input);
			}
		});
	}

	protected static final Comparator<NavigableObject> PARENT_LINK_FIRST_COMPARATOR = new Comparator<NavigableObject>() {
		@Override
		public int compare(NavigableObject d0, NavigableObject d1) {
			return Boolean.valueOf(d1.getIsParentLink()).compareTo(d0.getIsParentLink());
		}
	};
	
	protected static final Comparator<NavigableObject> DIRECTORIES_FIRST_COMPARATOR = new Comparator<NavigableObject>() {
		@Override
		public int compare(NavigableObject d0, NavigableObject d1) {
			return Boolean.valueOf(d1.isDirectory()).compareTo(d0.isDirectory());
		}
	};
	
	@SuppressWarnings("serial")
	private static final Collection<Comparator<NavigableObject>> STATIC_COMPARATORS = new ArrayList<Comparator<NavigableObject>>() {{
			add(PARENT_LINK_FIRST_COMPARATOR);
			add(DIRECTORIES_FIRST_COMPARATOR);
	}};
	
	protected static final Comparator<NavigableObject> DATE_MODIFIED_COMPARATOR = new Comparator<NavigableObject>() {
		@Override
		public int compare(NavigableObject d0, NavigableObject d1) {
			return Long.valueOf(d1.lastModified()).compareTo(d0.lastModified());
		}
	};
	
	protected static final Comparator<NavigableObject> NAME_COMPARATOR = new Comparator<NavigableObject>() {
		@Override
		public int compare(NavigableObject d0, NavigableObject d1) {
			return d0.getName().compareTo(d1.getName());
		}
	};
	
	protected static final Comparator<NavigableObject> SIZE_COMPARATOR = new Comparator<NavigableObject>() {
		@Override
		public int compare(NavigableObject d0, NavigableObject d1) {
			return Long.valueOf(d1.getSize()).compareTo(d0.getSize());
		}
	};
	
	public static List<NavigableObject> sortByCriteria(Collection<NavigableObject> files, Collection<SortCriteria> sorters) {

		Collection<Comparator<NavigableObject>> comparators = Lists.newArrayList(STATIC_COMPARATORS);
		Collection<Comparator<NavigableObject>> dynamic_comparators = Collections2.transform(
						sorters,
						new Function<SortCriteria, Comparator<NavigableObject>>() {
			@Override
			public Comparator<NavigableObject> apply(SortCriteria input) {
				return input.comparator;
			}
		});
		comparators.addAll(dynamic_comparators);
		return Ordering.compound(comparators).immutableSortedCopy(files);
	}
	
	public enum SortCriteria {
		
		DATE(DATE_MODIFIED_COMPARATOR), NAME(NAME_COMPARATOR), SIZE(SIZE_COMPARATOR);
		
		final public Comparator<NavigableObject> comparator;
		
		SortCriteria(Comparator<NavigableObject> c) {
			this.comparator = c;
		}
	}
}
