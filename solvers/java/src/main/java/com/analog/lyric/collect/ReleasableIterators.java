/*******************************************************************************
*   Copyright 2012 Analog Devices, Inc.
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

import java.util.Collection;
import java.util.Iterator;

public class ReleasableIterators
{

	private static final class Wrapper<T> implements ReleasableIterator<T>
	{
		private Iterator<T> _iter;

		private Wrapper(Iterator<T> iter)
		{
			this.reset(iter);
		}

		private static final ThreadLocal<Wrapper<?>> instance = new ThreadLocal<Wrapper<?>>();

		static <T> Wrapper<T> make(Iterator<T> iter)
		{
			Wrapper<T> wrapper = (Wrapper<T>)Wrapper.instance.get();
			if (wrapper != null)
			{
				Wrapper.instance.set(null);
				wrapper.reset(iter);
			}
			else
			{
				wrapper = new Wrapper<T>(iter);
			}

			return wrapper;
		}

		public void reset(Iterator<T> iter)
		{
			this._iter = iter;
		}

		@Override
		public boolean hasNext()
		{
			return this._iter.hasNext();
		}

		@Override
		public T next()
		{
			return this._iter.next();
		}

		@Override
		public void remove()
		{
			this._iter.remove();
		}

		@Override
		public void release()
		{
			if (instance.get() == null)
			{
				this.reset(null);
				instance.set(this);
			}
		}
	}

	/**
	 * Returns a {@link ReleasableIterator} for given collection.
	 * <p>
	 * This should only be used for situations in which the caller may encounter
	 * some collections that return {@link ReleasableIterator}s and some that do
	 * not and you want to avoid an explicit {@code instanceof} check.
	 */
	public static <T> ReleasableIterator<T> iteratorFor(Collection<T> c)
	{
		Iterator<T> iter = c.iterator();

		if (iter instanceof ReleasableIterator)
		{
			return (ReleasableIterator<T>)iter;
		}

		return Wrapper.make(iter);
	}
}
