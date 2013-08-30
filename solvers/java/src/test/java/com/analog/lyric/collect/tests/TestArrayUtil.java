package com.analog.lyric.collect.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.analog.lyric.collect.ArrayUtil;

public class TestArrayUtil
{
	@Test
	public void allocateArrayOfType()
	{
		String[] s3 = ArrayUtil.allocateArrayOfType(String.class, null, 3);
		assertSame(String[].class, s3.getClass());
		assertEquals(3, s3.length);
		
		Object[] o3 = ArrayUtil.allocateArrayOfType(Number.class, (Object[])s3, 3);
		assertSame(Number[].class, o3.getClass());
		assertEquals(3, o3.length);
		
		Object[] o2 = ArrayUtil.allocateArrayOfType(Double.class,  o3, 2);
		assertSame(o3, o2);
		
		Number[] n4 = ArrayUtil.allocateArrayOfType(Number.class, (Number[])o3, 4);
		assertEquals(4, n4.length);
	}
	
	@Test
	public void allFuzzyEqual()
	{
		assertTrue(ArrayUtil.allFuzzyEqual(new double[0], 0.0));
		assertTrue(ArrayUtil.allFuzzyEqual(new double[] { 1.234} , 0.0));
		assertTrue(ArrayUtil.allFuzzyEqual(new double[] { 1.04, 1.0, .95 }, .1));
		assertFalse(ArrayUtil.allFuzzyEqual(new double[] { 1.05, 1.0, .8999, 1.045 }, .1));
	}
	
	@Test
	public void subsetFuzzyEqual()
	{
		assertTrue(ArrayUtil.subsetFuzzyEqual(new double[] { 1.0, 2.0, 3.0 }, new int[] { 1 }, 0.0));
		assertTrue(ArrayUtil.subsetFuzzyEqual(new double[] { 1.0, 2.0, 3.0 }, new int[] { 1, 2 }, 1.1));
		assertFalse(ArrayUtil.subsetFuzzyEqual(new double[] { 1.0, 2.0, 3.0 }, new int[] { 0, 2 }, 1.1));
	}
	
	@Test
	public void cloneArray()
	{
		assertNull(ArrayUtil.cloneArray((double[])null));
		assertSame(ArrayUtil.EMPTY_DOUBLE_ARRAY, ArrayUtil.cloneArray(new double[0]));
		double[] d = new double[] { 1.2345, 2.234234, 4.5 };
		double[] d2 = ArrayUtil.cloneArray(d);
		assertNotSame(d, d2);
		assertArrayEquals(d, d2, 0.0);

		assertNull(ArrayUtil.cloneArray((int[])null));
		assertSame(ArrayUtil.EMPTY_INT_ARRAY, ArrayUtil.cloneArray(new int[0]));
		int[] i = new int[] { 1, 3, 45, 52 };
		int[] i2 = ArrayUtil.cloneArray(i);
		assertNotSame(i, i2);
		assertArrayEquals(i, i2);
		
		assertNull(ArrayUtil.cloneArray((int[][])null));
		assertSame(ArrayUtil.EMPTY_INT_ARRAY_ARRAY, ArrayUtil.cloneArray(new int[0][]));
		int ii[][] = new int[][] { new int[] { 1, 2}, new int[] { 3, 4} };
		int ii2[][] = ArrayUtil.cloneArray(ii);
		assertArrayEquals(ii, ii2);
		assertNotSame(ii, ii2);
		assertSame(ii[0], ii2[0]);
	}

	@Test
	public void copyArrayForInsert()
	{
		int i[] = new int[] { 1, 2, 3, 4 };
		assertArrayEquals(i, ArrayUtil.copyArrayForInsert(i, 2, 0));
		assertArrayEquals(new int[] { 1, 2, 0, 0, 0, 3, 4}, ArrayUtil.copyArrayForInsert(i, 2, 3));
		assertArrayEquals(new int[] { 0, 0 }, ArrayUtil.copyArrayForInsert((int[])null, 0, 2));
		try
		{
			ArrayUtil.copyArrayForInsert(i, 5, 1);
			fail("expected exception");
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
		}
		
		double d[] = new double[] { 1, 2, 3, 4 };
		assertArrayEquals(d, ArrayUtil.copyArrayForInsert(d, 2, 0), 0.0);
		assertArrayEquals(new double[] { 1, 2, 0, 0, 0, 3, 4}, ArrayUtil.copyArrayForInsert(d, 2, 3), 0.0);
		assertArrayEquals(new double[] { 0, 0 }, ArrayUtil.copyArrayForInsert((double[])null, 0, 2), 0.0);
		
		int ii[][] = new int[][] { new int[] {1}, new int[] {2,3}, new int[] {4, 5, 6} };
		assertArrayEquals(ii, ArrayUtil.copyArrayForInsert(ii, 2, 0));
		assertArrayEquals(new int[][] { ii[0], null, null, ii[1], ii[2] }, ArrayUtil.copyArrayForInsert(ii, 1, 2));
		assertArrayEquals(new int[][] { null, null }, ArrayUtil.copyArrayForInsert((int[][])null, 0, 2));
	}
	
	@Test
	public void toIntArray()
	{
		//
		// Test toIntArray
		//
		
		assertNull(ArrayUtil.toIntArray("hi"));
		assertNull(ArrayUtil.toIntArray(new Object[] { "barf" }));
		assertNull(ArrayUtil.toIntArray(new Object[] { 1, 2.5 }));
	
		assertArrayEquals(new int[] { 1, 2, 3 }, ArrayUtil.toIntArray(new Object[] { 1, 2.0, 3L }));
		
		int[] ints = new int[] { 23, 42, 123 };
		assertSame(ints, ArrayUtil.toIntArray(ints));
	}
	
	@Test
	public void toArray()
	{
		//
		// Test toArray
		//
		
		assertNull(ArrayUtil.toArray("hi"));
		assertArrayEquals(new Object[] { 42.0, 23.0, 2.3 }, ArrayUtil.toArray(new double[] { 42, 23, 2.3 }));
		
		Object[] objs = new Object[] { "foo", 42 };
		assertSame(objs, ArrayUtil.toArray(objs));
	}
}
