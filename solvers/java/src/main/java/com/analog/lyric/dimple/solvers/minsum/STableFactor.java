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

package com.analog.lyric.dimple.solvers.minsum;

import java.util.Arrays;

import com.analog.lyric.cs.Sort;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.factorfunctions.core.FactorTableRepresentation;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.solvers.core.STableFactorDoubleArray;
import com.analog.lyric.dimple.solvers.core.kbest.IKBestFactor;
import com.analog.lyric.dimple.solvers.core.kbest.KBestFactorEngine;
import com.analog.lyric.dimple.solvers.core.kbest.KBestFactorTableEngine;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;

public class STableFactor extends STableFactorDoubleArray implements IKBestFactor
{
	/*
	 * We cache all of the double arrays we use during the update.  This saves
	 * time when performing the update.
	 */
    protected double [][] _savedOutMsgArray;
    protected double [] _dampingParams;
    protected int _k;
    protected TableFactorEngine _tableFactorEngine;
    protected KBestFactorEngine _kbestFactorEngine;
    protected boolean _kIsSmallerThanDomain;
    protected boolean _dampingInUse = false;

    /*--------------
     * Construction
     */
    
    public STableFactor(Factor factor)
	{
    	super(factor);
    	
		_dampingParams = new double[_factor.getSiblingCount()];
		_tableFactorEngine = new TableFactorEngine(this);
		
		if (factor.getFactorFunction().factorTableExists(getFactor()))
			_kbestFactorEngine = new KBestFactorTableEngine(this);
		else
			_kbestFactorEngine = new KBestFactorEngine(this);
		
		//setK(Integer.MAX_VALUE);
		
	}

    /*---------------------
     * ISolverNode methods
     */


	@Override
	public void moveMessages(ISolverNode other, int portNum, int otherPort)
	{
		super.moveMessages(other,portNum,otherPort);
	    if (_dampingInUse)
	    	_savedOutMsgArray[portNum] = ((STableFactor)other)._savedOutMsgArray[otherPort];
	    
	}
	
	@Override
	protected void doUpdate()
	{
		if (_kIsSmallerThanDomain)
			_kbestFactorEngine.update();
		else
			_tableFactorEngine.update();
	}
	
	@Override
	public void doUpdateEdge(int outPortNum)
	{
		if (_kIsSmallerThanDomain)
			_kbestFactorEngine.updateEdge(outPortNum);
		else
			_tableFactorEngine.updateEdge(outPortNum);

	}
	
	public void setK(int k)
	{
		
		_k = k;
		_kbestFactorEngine.setK(k);
		_kIsSmallerThanDomain = false;
		for (int i = 0; i < _inputMsgs.length; i++)
		{
			if (_k < _inputMsgs[i].length)
			{
				_kIsSmallerThanDomain = true;
				break;
			}
		}
	}
    
	/*-----------------------
	 * ISolverFactor methods
	 */

	@Override
	public void createMessages()
	{
		super.createMessages();
		
		int numPorts = _factor.getSiblingCount();
	    if (_dampingInUse)
	    	_savedOutMsgArray = new double[numPorts][];
	    
		for (int port = 0; port < numPorts; port++)
	    {
	    	if (_dampingInUse)
	    		_savedOutMsgArray[port] = new double[_inputMsgs[port].length];
	    }
	    
	    setK(Integer.MAX_VALUE);
	}

	/*--------------------------
	 * STableFactorBase methods
	 */
	
	@Override
	protected void setTableRepresentation(IFactorTable table)
	{
		table.setRepresentation(FactorTableRepresentation.SPARSE_ENERGY_WITH_INDICES);
	}
	
	/*----------------------
	 * IKBestFactor methods
	 */
    
	@Override
	public FactorFunction getFactorFunction()
	{
		return getFactor().getFactorFunction();
	}

	@Override
	public double initAccumulator()
	{
		return 0;
	}

	@Override
	public double accumulate(double oldVal, double newVal)
	{
		return oldVal + newVal;
	}

	@Override
	public double combine(double oldVal, double newVal)
	{
		if (oldVal < newVal)
			return oldVal;
		else
			return newVal;
	}

	@Override
	public void normalize(double[] outputMsg)
	{
		double minVal = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < outputMsg.length; i++)
			if (outputMsg[i] < minVal)
				minVal = outputMsg[i];
		
		for (int i = 0; i < outputMsg.length; i++)
			outputMsg[i] -= minVal;
	}

	@Override
	public double evalFactorFunction(Object[] inputs)
	{
		return getFactorFunction().evalEnergy(inputs);
	}

	@Override
	public void initMsg(double[] msg)
	{
		Arrays.fill(msg, Double.POSITIVE_INFINITY);
	}

	@Override
	public double getFactorTableValue(int index)
	{
		return getFactorTable().getEnergiesSparseUnsafe()[index];
	}
	
	@Override
	public int[] findKBestForMsg(double[] msg, int k)
	{
		return Sort.quickfindFirstKindices(msg, k);
	}


	@Override
	public double[][] getInPortMsgs()
	{
		return _inputMsgs;
	}


	@Override
	public double[][] getOutPortMsgs()
	{
		return _outputMsgs;
	}
	
   /*-------------
     * New methods
     */
	
	public void setDamping(int index, double val)
	{
		_dampingParams[index] = val;
		
		if (val != 0)
			_dampingInUse = true;
		
		_savedOutMsgArray = new double[_dampingParams.length][];
		for (int i = 0; i < _inputMsgs.length; i++)
			_savedOutMsgArray[i] = new double[_inputMsgs[i].length];

	}
	
	public double getDamping(int index)
	{
		return _dampingParams[index];
	}
	
}

