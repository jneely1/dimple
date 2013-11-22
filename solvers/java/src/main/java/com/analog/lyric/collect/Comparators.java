package com.analog.lyric.collect;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

import net.jcip.annotations.Immutable;

/**
 * 
 * @author CBarber
 * @since 0.05
 */
public final class Comparators
{
	private Comparators() {} // Prevent instantiation
	
	@Immutable
	private static enum LexicalIntArrayComparator implements Comparator<int[]>, Serializable
	{
		INSTANCE;
		
		@Override
		public int compare(int[] array1, int[] array2)
		{
			int diff = array1.length - array2.length;
			if (diff == 0)
			{
				for (int i = 0, end = array1.length; i < end; ++i)
				{
					int val1 = array1[i], val2 = array2[i];
					if (val1 != val2)
					{
						diff = val1 < val2 ? -1 : 1;
						break;
					}
				}
			}
			return diff;
		}
	}
	
	@Immutable
	private static enum ReverseLexicalIntArrayComparator implements Comparator<int[]>, Serializable
	{
		INSTANCE;
		
		@Override
		public int compare(int[] array1, int[] array2)
		{
			int diff = array1.length - array2.length;
			if (diff == 0)
			{
				for (int i = array1.length; --i >= 0;)
				{
					int val1 = array1[i], val2 = array2[i];
					if (val1 != val2)
					{
						diff = val1 < val2 ? -1 : 1;
						break;
					}
				}
			}
			return diff;
		}
	}

	/**
	 * Returns a comparator for int arrays that compares arrays in reverse lexicographical order, which
	 * compares elements from the last index backward (e.g. {@code [1,2,3]} is after {@code [1,3,2]}).
	 * If arrays are of different length, the shorter array comes first.
	 * 
	 * @see #lexicalIntArray()
	 */
	public static Comparator<int[]> reverseLexicalIntArray()
	{
		return ReverseLexicalIntArrayComparator.INSTANCE;
	}
	
	/**
	 * Returns a comparator for int arrays that compares arrays in lexicographical (aka "dictionary") order
	 * (e.g. {@code [1,2,3]} is before {@code [1,3,2]}). If arrays are of different length, the shorter array comes
	 * first.
	 * 
	 * @see #reverseLexicalIntArray()
	 */
	public static Comparator<int[]> lexicalIntArray()
	{
		return LexicalIntArrayComparator.INSTANCE;
	}

	/**
	 * Returns comparator used by collection.
	 * 
	 * <If collection is a {@link SortedSet} returns {@link SortedSet#comparator()}, else returns null.
	 */
	public static <T> Comparator<? super T> fromCollection(Collection<? extends T> collection)
	{
		if (collection instanceof SortedSet)
		{
			return (Comparator<? super T>) ((SortedSet<? extends T>)collection).comparator();
		}
		return null;
	}
}