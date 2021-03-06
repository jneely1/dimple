/*******************************************************************************
*   Copyright 2013 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.gibbs.customFactors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.factorfunctions.CategoricalEnergyParameters;
import com.analog.lyric.dimple.factorfunctions.CategoricalUnnormalizedParameters;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunctionUtilities;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.GammaParameters;
import com.analog.lyric.dimple.solvers.gibbs.SDiscreteVariable;
import com.analog.lyric.dimple.solvers.gibbs.SRealFactor;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.GammaSampler;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.IRealConjugateSamplerFactory;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.NegativeExpGammaSampler;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;

public class CustomCategoricalUnnormalizedOrEnergyParameters extends SRealFactor implements IRealConjugateFactor
{
	private Object[] _outputMsgs;
	private SDiscreteVariable[] _outputVariables;
	private FactorFunction _factorFunction;
	private int _numParameters;
	private int _numParameterEdges;
	private int _numOutputEdges;
	private int[] _constantOutputCounts;
	private boolean _hasConstantOutputs;
	private boolean _hasFactorFunctionConstructorConstants;
	private boolean _useEnergyParameters;

	public CustomCategoricalUnnormalizedOrEnergyParameters(Factor factor)
	{
		super(factor);
	}

	@Override
	public void updateEdgeMessage(int portNum)
	{
		if (portNum < _numParameterEdges)
		{
			// Port is a parameter input
			// Determine sample alpha and beta parameters
			// NOTE: This case works for either CategoricalUnnormalizedParameters or CategoricalEnergyParameters factor functions
			// since the actual parameter value doesn't come into play in determining the message in this direction

			GammaParameters outputMsg = (GammaParameters)_outputMsgs[portNum];

			// The parameter being updated corresponds to this value
			int parameterIndex = _factorFunction.getIndexByEdge(portNum);

			// Start with the ports to variable outputs
			int count = 0;
			for (int i = 0; i < _numOutputEdges; i++)
			{
				int outputIndex = _outputVariables[i].getCurrentSampleIndex();
				if (outputIndex == parameterIndex)
					count++;
			}
			
			// Include any constant outputs also
			if (_hasConstantOutputs)
				count += _constantOutputCounts[parameterIndex];
			
			outputMsg.setAlphaMinusOne(count);		// Sample alpha
			outputMsg.setBeta(0);					// Sample beta
		}
		else
			super.updateEdgeMessage(portNum);
	}
	
	
	@Override
	public Set<IRealConjugateSamplerFactory> getAvailableRealConjugateSamplers(int portNumber)
	{
		Set<IRealConjugateSamplerFactory> availableSamplers = new HashSet<IRealConjugateSamplerFactory>();
		if (isPortParameter(portNumber))					// Conjugate sampler if edge is a parameter input
			if (_useEnergyParameters)
				availableSamplers.add(NegativeExpGammaSampler.factory);	// Parameter inputs have conjugate negative exp-Gamma distribution
			else
				availableSamplers.add(GammaSampler.factory);			// Parameter inputs have conjugate Gamma distribution
		return availableSamplers;
	}
	
	public boolean isPortParameter(int portNumber)
	{
		determineParameterConstantsAndEdges();	// Call this here since initialize may not have been called yet
		return (portNumber < _numParameterEdges);
	}

	
	
	@Override
	public void initialize()
	{
		super.initialize();
		
		
		// Determine what parameters are constants or edges, and save the state
		determineParameterConstantsAndEdges();
		
		
		// Pre-compute statistics associated with any constant output values
		_constantOutputCounts = null;
		if (_hasConstantOutputs)
		{
			FactorFunction factorFunction = _factor.getFactorFunction();
			Object[] constantValues = factorFunction.getConstants();
			int[] constantIndices = factorFunction.getConstantIndices();
			_constantOutputCounts = new int[_numParameters];
			for (int i = 0; i < constantIndices.length; i++)
			{
				if (_hasFactorFunctionConstructorConstants || constantIndices[i] >= _numParameters)
				{
					int outputValue = FactorFunctionUtilities.toInteger(constantValues[i]);
					_constantOutputCounts[outputValue]++;	// Histogram among constant outputs
				}
			}
		}
	}
	
	
	private void determineParameterConstantsAndEdges()
	{
		// Get the factor function and related state
		FactorFunction factorFunction = _factor.getFactorFunction();
		FactorFunction containedFactorFunction = factorFunction.getContainedFactorFunction();	// In case the factor function is wrapped
		_factorFunction = factorFunction;
		boolean hasFactorFunctionConstants = factorFunction.hasConstants();
		if (containedFactorFunction instanceof CategoricalUnnormalizedParameters)
		{
			CategoricalUnnormalizedParameters specificFactorFunction = (CategoricalUnnormalizedParameters)containedFactorFunction;
			_hasFactorFunctionConstructorConstants = specificFactorFunction.hasConstantParameters();
			_numParameters = specificFactorFunction.getDimension();
			_useEnergyParameters = false;
		}
		else if (containedFactorFunction instanceof CategoricalEnergyParameters)
		{
			CategoricalEnergyParameters specificFactorFunction = (CategoricalEnergyParameters)containedFactorFunction;
			_hasFactorFunctionConstructorConstants = specificFactorFunction.hasConstantParameters();
			_numParameters = specificFactorFunction.getDimension();
			_useEnergyParameters = true;
		}
		else
			throw new DimpleException("Invalid factor function");

		// Pre-determine whether or not the parameters are constant; if so save the value; if not save reference to the variable
		_numParameterEdges = _numParameters;
		_hasConstantOutputs = false;
		if (_hasFactorFunctionConstructorConstants)
		{
			// The factor function has fixed parameters provided in the factor-function constructor
			_numParameterEdges = 0;
			_hasConstantOutputs = hasFactorFunctionConstants;
		}
		else if (hasFactorFunctionConstants)
		{
			_hasConstantOutputs = factorFunction.hasConstantAtOrAboveIndex(_numParameters);
			int numConstantParameters = factorFunction.numConstantsInIndexRange(0, _numParameters - 1);
			_numParameterEdges = _numParameters - numConstantParameters;
		}
		_numOutputEdges = _numPorts - _numParameterEdges;
	
		// Save output variables
		List<? extends VariableBase> siblings = _factor.getSiblings();
		_outputVariables = new SDiscreteVariable[_numOutputEdges];
		for (int i = 0; i < _numOutputEdges; i++)
			_outputVariables[i] = (SDiscreteVariable)((siblings.get(i + _numParameterEdges)).getSolver());
	}
	
	
	@Override
	public void createMessages()
	{
		super.createMessages();
		determineParameterConstantsAndEdges();	// Call this here since initialize may not have been called yet
		_outputMsgs = new Object[_numPorts];
		for (int port = 0; port < _numParameterEdges; port++)	// Only parameter edges
			_outputMsgs[port] = new GammaParameters();
	}
	
	@Override
	public Object getOutputMsg(int portIndex)
	{
		return _outputMsgs[portIndex];
	}
	
	@Override
	public void moveMessages(ISolverNode other, int thisPortNum, int otherPortNum)
	{
		super.moveMessages(other, thisPortNum, otherPortNum);
		_outputMsgs[thisPortNum] = ((CustomCategoricalUnnormalizedOrEnergyParameters)other)._outputMsgs[otherPortNum];
	}
}
