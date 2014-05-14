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

package com.analog.lyric.dimple.solvers.core.proxy;

import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;
import com.analog.lyric.dimple.solvers.interfaces.ISolverVariable;

/**
 * @since 0.05
 */
public abstract class ProxySolverVariable extends ProxySolverNode implements ISolverVariable
{
	protected final VariableBase _modelVariable;
	
	/*--------------
	 * Construction
	 */
	
	/**
	 * @param modelVariable
	 */
	protected ProxySolverVariable(VariableBase modelVariable)
	{
		_modelVariable = modelVariable;
	}

	/*---------------------
	 * ISolverNode methods
	 */
	
	@Override
	public VariableBase getModelObject()
	{
		return _modelVariable;
	}
	
	@Override
	public ISolverFactor getSibling(int edge)
	{
		return getModelObject().getSibling(edge).getSolver();
	}

	/*-------------------------
	 * ISolverVariable methods
	 */
	
	@Override
	public Object[] createMessages(ISolverFactor factor)
	{
		throw unsupported("createMessages");
	}

	@Override
	public void createNonEdgeSpecificState()
	{
		ISolverVariable delegate = getDelegate();
		if (delegate != null)
		{
			delegate.createNonEdgeSpecificState();
		}
	}

	@Override
	public Object getBelief()
	{
		return getDelegate().getBelief();
	}

	@Override
	public Object getGuess()
	{
		return getDelegate().getGuess();
	}

	@Override
	public void setGuess(Object guess)
	{
		getDelegate().setGuess(guess);
	}

	@Override
	public Object getValue()
	{
		return getDelegate().getValue();
	}

	@Override
	public void moveNonEdgeSpecificState(ISolverNode other)
	{
		if (other instanceof ProxySolverNode)
		{
			other = ((ProxySolverNode)other).getDelegate();
		}
		getDelegate().moveNonEdgeSpecificState(other);
	}

	@Override
	public Object resetInputMessage(Object message)
	{
		throw unsupported("resetInputMessage");
	}

	@Override
	public Object resetOutputMessage(Object message)
	{
		throw unsupported("resetOutputMessage");
	}

	/*-------------------------
	 * ISolverVariable methods
	 */
	
	@Override
	public void setInputOrFixedValue(Object input, Object fixedValue, boolean hasFixedValue)
	{
		ISolverVariable delegate = getDelegate();
		if (delegate != null)
		{
			delegate.setInputOrFixedValue(input, fixedValue, hasFixedValue);
		}
	}
	
	/*-------------------------
	 * ProxySolverNode methods
	 */
	
	@Override
	public abstract ISolverVariable getDelegate();
}
