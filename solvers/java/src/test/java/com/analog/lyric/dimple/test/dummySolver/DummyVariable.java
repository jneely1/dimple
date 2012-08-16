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

package com.analog.lyric.dimple.test.dummySolver;

import com.analog.lyric.dimple.model.Discrete;
import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.Port;
import com.analog.lyric.dimple.model.VariableBase;
import com.analog.lyric.dimple.solvers.core.SVariableBase;


public class DummyVariable extends SVariableBase
{
	private double [] _input = new double[1];
	protected Discrete _varDiscrete;

	public DummyVariable(VariableBase var) 
	{
		super(var);
		_varDiscrete = (Discrete)_var;
		_input = (double[])getDefaultMessage(null);
	}

	public VariableBase getVariable()
	{
		return _var;
	}

	public Object getDefaultMessage(Port port) 
	{
		double[] msg = new double[_input.length];
		java.util.Arrays.fill(msg, java.lang.Math.PI);

		return msg;
	}


	public void setInput(Object input) 
	{
		double [] vals = (double[])input;

		int len = _varDiscrete.getDiscreteDomain().getElements().length;
		
		if (vals.length != len)
			throw new DimpleException("length of priors does not match domain");

		_input = vals;

	}	


	public void updateEdge(int outPortNum)
	{
	}


	public void update() 
	{
	}

	public Object getBelief()
	{
		return _input;
	}

	@Override
	public double getEnergy() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getGuess() 
	{
		throw new DimpleException("get and set guess not supported for this solver");
	}

	public void setGuess(Object guess) 
	{
		throw new DimpleException("get and set guess not supported for this solver");
	}

}
