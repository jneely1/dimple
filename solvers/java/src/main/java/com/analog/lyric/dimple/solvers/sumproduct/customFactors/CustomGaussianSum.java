/*******************************************************************************
*   Copyright 2012-2014 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.sumproduct.customFactors;

import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunctionUtilities;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.RealJoint;
import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.NormalParameters;


public class CustomGaussianSum extends GaussianFactorBase
{
	protected int _sumIndex;
	private int _sumPort;
	private double _constantSum;
	
	public CustomGaussianSum(Factor factor)
	{
		super(factor);
		_sumIndex = 0;		// Index that is the sum of all the others
		
		for (int i = 0, endi = factor.getSiblingCount(); i < endi; i++)
		{
			VariableBase v = factor.getSibling(i);
			
			if (v.getDomain().isDiscrete())
				throw new DimpleException("Cannot connect discrete variable to this factor");
		}
	}

	@Override
	public void updateEdge(int outPortNum)
	{
		// uout = ua + ub + uc
		// ub = uout-ua-uc
		// sigma^2 = othersigma^2 + theothersigma^2 ...
		
		if (outPortNum == _sumPort)
			updateSumEdge();
		else
			updateSummandEdge(outPortNum);
	}
	
	private void updateSumEdge()
	{
		double mean = _constantSum;
		double variance = 0;
		
		for (int i = 0; i < _inputMsgs.length; i++)
		{
			if (i != _sumPort)
			{
				NormalParameters msg = _inputMsgs[i];
				mean += msg.getMean();
				variance += msg.getVariance();
			}
		}

		NormalParameters outMsg = _outputMsgs[_sumPort];
		outMsg.setMean(mean);
		outMsg.setVariance(variance);
	}
	
	
	private void updateSummandEdge(int outPortNum)
	{
		
		double mean = -_constantSum;		// For summands, use negative of constant sum
		double variance = 0;
		
		for (int i = 0; i < _inputMsgs.length; i++)
		{
			if (i != outPortNum)
			{
				NormalParameters msg = _inputMsgs[i];
				if (i == _sumPort)
					mean += msg.getMean();
				else
					mean -= msg.getMean();

				variance += msg.getVariance();
			}
		}

		NormalParameters outMsg = _outputMsgs[outPortNum];
		outMsg.setMean(mean);
		outMsg.setVariance(variance);
	}


	@Override
	public void initialize()
	{
		super.initialize();
		
		// Pre-compute sum associated with any constant edges
		FactorFunction factorFunction = _factor.getFactorFunction();
		_sumPort = factorFunction.isConstantIndex(_sumIndex) ? -1 : _sumIndex;	// If sum isn't a variable, then set port to invalid value
		_constantSum = 0;
		if (factorFunction.hasConstants())
		{
			Object[] constantValues = factorFunction.getConstants();
			int[] constantIndices = factorFunction.getConstantIndices();
			for (int i = 0; i < constantValues.length; i++)
			{
				if (constantIndices[i] == _sumIndex)
					_constantSum -= FactorFunctionUtilities.toDouble(constantValues[i]);	// Constant sum value counts as negative
				else
					_constantSum += FactorFunctionUtilities.toDouble(constantValues[i]);	// Constant summand value counts as positive
			}
		}

	}
	
	
	// Utility to indicate whether or not a factor is compatible with the requirements of this custom factor
	public static boolean isFactorCompatible(Factor factor)
	{
		for (int i = 0, end = factor.getSiblingCount(); i < end; i++)
		{
			VariableBase v = factor.getSibling(i);
			
			// Must be real
			if (v.getDomain().isDiscrete())
				return false;
			
			// Must be univariate
			if (v instanceof RealJoint)
				return false;
			
			// Must be unbounded
			if (v.getDomain().asReal().isBounded())
				return false;
		}
		return true;
	}


}
