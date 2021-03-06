/*******************************************************************************
*   Copyright 2014 Analog Devices, Inc.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************/

package com.analog.lyric.collect;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Primitives;


/**
 * Static utility methods related to super classes.
 */
public abstract class Supers
{
	// NOTE: Originally I tried to use the CacheBuilder class from Google's guava library, but that created
	// problems when running in MATLAB because MATLAB uses an old and incompatible google-collections library
	// and puts it in the static java class path, with the result that the guava code ended up loading an
	// older version of the Objects utility class. The only way I could figure out to work around this was
	// to actually edit MATLAB's internal classpath.txt file to put the desired guava jar before google-collections.
	// In this case, it is easy enough to implement our own cache, but this may come up again - cbarber
	
	private static class SuperClassCache // JAVA7: extends ClassValue<ImmutableList<Class?>>
	{
		// JAVA7: In Java 7, we can simply subclass ClassValue and just implement computeValue.
		
		private final ConcurrentHashMap<Class<?>,ImmutableList<Class<?>>> _cache =
			new ConcurrentHashMap<Class<?>,ImmutableList<Class<?>>>();
		
		public ImmutableList<Class<?>> get(Class<?> c)
		{
			ImmutableList<Class<?>> supers = _cache.get(c);
			if (supers == null)
			{
				synchronized(this)
				{
					supers = _cache.get(c);
					if (supers == null)
					{
						_cache.put(c, supers = computeValue(c));
					}
				}
			}
			return supers;
		}
		
		private ImmutableList<Class<?>> computeValue(Class<?> c)// throws Exception
		{
			ArrayList<Class<?>> supers = new ArrayList<Class<?>>();

			while (true)
			{
				Class<?> s = c.getSuperclass();
				if (s == null)
				{
					break;
				}
				supers.add(s);
				c = s;
			}
			
			Collections.reverse(supers);
			
			// HACK: the google-collections version of guava contained in MATLAB only has
			// the Iterable version of copyOf, so we need to make sure that is what we are using
			// here. See comment above.
			return ImmutableList.copyOf((Iterable<Class<?>>)supers);
		}
	}
	
	private static final SuperClassCache _superClassCache = new SuperClassCache();
	
	/**
	 * Returns an array of super classes above {@code c}. The first class will always be {@link Object}
	 * or the array will be empty. Returns a cached value.
	 */
	public static ImmutableList<Class<?>> superClasses(Class<?> c)
	{
		return _superClassCache.get(c);
	}
	
	/**
	 * Looks up and invokes method with given name and applicable to given object and arguments
	 * using reflection.
	 * 
	 * WARNING: this is primarily intended for use in writing unit tests. Probably should
	 * avoid using in production code.
	 * 
	 * @param object either the {@link Class} of static method to be called, or object on which
	 * method should be invoked.
	 * @param methodName is the name of the method to invoke.
	 * @param args are the arguments to pass to the method.
	 * @return return value from method.

	 * @throws NoSuchMethodException if no method can be found with given name and matching the types
	 * of the arguments.
	 * @throws InvocationTargetException wraps exception thrown by reflectively invoked method.
	 * @throws IllegalAccessException
	 * @see #lookupMethod(Object, String, Object...)
	 * 
	 * @since 0.05
	 */
	public static Object invokeMethod(Object object, String methodName, Object ... args)
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		Method method = lookupMethod(object, methodName, args);
		
		if (method.isVarArgs())
		{
			Class<?>[] declaredTypes = method.getParameterTypes();
			int declaredSize = declaredTypes.length;
			
			int nArgs = args == null ? 0 : args.length;
			if (nArgs != declaredSize || !declaredTypes[declaredSize - 1].isInstance(args[declaredSize - 1]))
			{
				Class<?> varArgType = declaredTypes[declaredSize - 1].getComponentType();

				Object varargs = args == null ? null : Array.newInstance(varArgType, nArgs + 1 - declaredSize);
				for (int i = 0, j = declaredSize - 1; j < nArgs; ++i, ++j)
				{
					Array.set(varargs, i, args[j]);
				}

				if (nArgs > 0)
				{
					args = Arrays.copyOf(args, declaredSize);
				}
				else
				{
					args = new Object[declaredSize];
				}
				args[declaredSize - 1] = varargs;
			}
		}
		
		return method.invoke(object,  args);
	}
	
	/**
	 * Looks up method with given name and that can be called with the given arguments.
	 * 
	 * WARNING: this is primarily intended for use in writing unit tests. Probably should
	 * avoid using in production code.
	 * 
	 * @param object if this is a {@link Class}, this will look for match in that class, otherwise
	 * it will look in its runtime class ({@link Object#getClass()}. Must not be null.
	 * @param methodName is the name of the method to find.
	 * @param args are the runtime arguments to the method.
	 * @return {@link Method} which can be called with given arguments.
	 * @throws NoSuchMethodException if no matching method is found.
	 * 
	 * @see #invokeMethod(Object, String, Object...)
	 * 
	 * @since 0.05
	 */
	public static Method lookupMethod(Object object, String methodName, Object ... args) throws NoSuchMethodException
	{
		Class<?> objClass = object instanceof Class ? (Class<?>)object : object.getClass();
		
		int nArgs = args == null ? 0 : args.length;
		Class<?>[] argTypes = new Class<?>[nArgs];
		for (int i = 0; i < nArgs; ++i)
		{
			argTypes[i] = args[i] == null ? null : Primitives.unwrap(args[i].getClass());
		}
		
		// First try direct match:
		try
		{
			return objClass.getMethod(methodName, argTypes);
		}
		catch (NoSuchMethodException ex)
		{
		}
		
		// Finally, walk through all the methods and return the first one that matches.
		outer:
		for (Method method : objClass.getMethods())
		{
			if (!methodName.equals(method.getName()))
			{
				continue outer;
			}
			
			Class<?>[] declaredTypes = method.getParameterTypes();

			int end = declaredTypes.length;
			Class<?> varArgType = null;

			if (method.isVarArgs())
			{
				--end;
				varArgType = Primitives.unwrap(declaredTypes[end].getComponentType());
				if (end > argTypes.length)
				{
					continue outer;
				}
			}
			else if (end != argTypes.length)
			{
				continue outer;
			}

			for (int i = 0; i < end; ++i)
			{
				Class<?> declaredType = Primitives.unwrap(declaredTypes[i]);
				if (argTypes[i] == null)
				{
					// null matches all non-primitive declared types (at least until
					// java comes up with an actual non-null type declaration)
					if (declaredType.isPrimitive())
					{
						continue outer;
					}
				}
				else if (!declaredType.isAssignableFrom(argTypes[i]))
				{
					continue outer;
				}
			}
			
			for (int i = end; i < argTypes.length; ++i)
			{
				if (!varArgType.isAssignableFrom(argTypes[i]))
				{
					continue outer;
				}
			}
			
			return method;
		}
		
		throw new NoSuchMethodException(); // FIXME message
	}
	
	/**
	 * Returns a new array containing {@code elements} with component type set to the nearest common superclass
	 * {@link #nearestCommonSuperClass(Object...)} of the objects it contains.
	 */
	public static <T> T[] narrowArrayOf(T[] elements)
	{
		return narrowArrayOf(Object.class, Integer.MAX_VALUE, elements);
	}
	
	/**
	 * Returns a new array containing {@code elements}, which must be a subclass of {@code rootClass},
	 * with component type set to nearest common superclass ({@link #nearestCommonSuperClass(Object...)})
	 * of the objects it contains that is no deeper than {@code maxClassDepthBelowRoot} below {@code rootClass}.
	 * 
	 * @see #narrowArrayOf(Object...)
	 * @see #copyOf(Class, Object...)
	 */
	
	public static <T> T[] narrowArrayOf(
		Class<? extends Object> rootClass,
		int maxRelativeClassDepth,
		T[] elements)
	{
		if (elements.length == 0)
		{
			if (rootClass.equals(elements.getClass().getComponentType()))
			{
				return elements;
			}
			else
			{
				return (T[])Array.newInstance(rootClass, 0);
			}
		}
		Class<?> c = nearestCommonSuperClass(elements);
		if (maxRelativeClassDepth < 500)
		{
			ImmutableList<Class<?>> supers = superClasses(c);
			final int maxClassDepth = numberOfSuperClasses(rootClass) + maxRelativeClassDepth;
			if (maxClassDepth < supers.size())
			{
				c = supers.get(maxClassDepth);
			}
		}
		return copyOf(c, elements);
	}
	
	/**
	 * Returns a new array containing {@code elements} with specified {@code componentType}, which must
	 * be a super class or interface of all the elements.
	 * 
	 * @see #narrowArrayOf(T...)
	 * @see #copyOf(Class, T...)
	 */
	public static <T> T[] copyOf(Class<?> componentType, T[] elements)
	{
		final int n = elements.length;
		T[] array = (T[])Array.newInstance(componentType, n);
		for (int i = 0; i <n; ++i)
		{
			array[i] = elements[i];
		}
		return array;
	}
	
	/**
	 * Returns nearest common super class between {@code c1} and {@code c2}.
	 * <ul>
	 * <li>If {@code c1} and {@code c2} are equal, then {@code c1} will be returned,
	 * even if it is an interface or primitive.
	 * <li>Likewise if {@code c1} is assignable from {@code c2} ({@link Class#isAssignableFrom})
	 * then {@code c1} will be returned even if it is an interface, and vice versa if the
	 * {@code c2} is assignable from {@code c1}.
	 * <li>If the above is not true, and either argument is a primitive, then null will be returned.
	 * <li>If the above is not true, and either argument is an interface, then {@link Object} will
	 * be returned.
	 * <li>Otherwise, the nearest superclass of both {@code c1} and {@code c2} will be returned.
	 * </ul>
	 */
	public static Class<?> nearestCommonSuperClass(Class<?> c1, Class<?> c2)
	{
		if (c1.isAssignableFrom(c2))
		{
			return c1;
		}
		else if (c2.isAssignableFrom(c1))
		{
			return c2;
		}
		
		// At this point we know that the common superclass cannot be c1 or c2.
		
		if (c1.isPrimitive() || c2.isPrimitive())
		{
			return null;
		}

		if (c1.isInterface() || c2.isInterface())
		{
			return Object.class;
		}
		
		ImmutableList<Class<?>> supers1 = superClasses(c1);
		ImmutableList<Class<?>> supers2 = superClasses(c2);
		
		Class<?> common = Object.class;
		
		for (int i = 0, end = Math.min(supers1.size(),  supers2.size()); i < end; ++i)
		{
			Class<?> super1 = supers1.get(i);
			if (super1.equals(supers2.get(i)))
			{
				common = super1;
			}
			else
			{
				break;
			}
		}
		
		return common;
	}

	/**
	 * Returns the nearest common superclass (not interface) for all of the objects.
	 */
	public static <T> Class<?> nearestCommonSuperClass(T obj, Object ... moreObjects)
	{
		Class<?> superclass = nearestCommonSuperClass(moreObjects);
		if (obj != null)
		{
			superclass = nearestCommonSuperClass(superclass, obj.getClass());
		}
		return superclass;
	}
	
	/**
	 * Returns the nearest common superclass (not interface) for all of the objects.
	 * Returns {@link Object} if array contains no objects.
	 */
	public static <T> Class<?> nearestCommonSuperClass(T[] objects)
	{
		int n = objects.length;
		if (n == 0)
		{
			return Object.class;
		}
		
		// Skip over empty slots
		int i = 0;
		while (objects[i] == null)
		{
			if (++i == n)
			{
				return Object.class;
			}
		}
		
		Class<?> declaredType = objects.getClass().getComponentType();
		if (declaredType.isInterface())
		{
			declaredType = Object.class;
		}
		else if (declaredType.isEnum() || Modifier.isFinal(declaredType.getModifiers()))
		{
			// There cannot be any subclasses
			return declaredType;
		}
		
		Class<?> c = objects[i].getClass();
		
		// Compute common superclass for all of the classes of elements in the array.
		while (++i < n)
		{
			if (objects[i] == null)
			{
				continue;
			}
			if (c.isAssignableFrom(declaredType))
			{
				break;
			}
			c = nearestCommonSuperClass(c, objects[i].getClass());
		}
		
		return c;
	}
	
	/**
	 * Computes the number of super classes above (and not including) this one. Returns 0 if
	 * {@code c} is {@link Object}, a primitive type or is an interface.
	 */
	public static int numberOfSuperClasses(Class<?> c)
	{
		return superClasses(c).size();
	}
	
	// TODO: methods for computing nearest common super interface (or class). Much harder because it
	// requires searching two DAGs instead of just a list. Because some interfaces such as Clonable,
	// Comparable, and Serializable are very common, methods dealing with interface probably should
	// specify root interfaces of interest.
}
