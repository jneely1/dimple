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

package com.analog.lyric.dimple.solvers.sumproduct;

import static com.analog.lyric.math.Utilities.*;

import java.util.Arrays;

import com.analog.lyric.cs.Sort;
import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.factorfunctions.core.FactorTableRepresentation;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.VariableBase;
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
	protected double [][][] _outPortDerivativeMsgs;
	protected double [] _dampingParams;
	protected TableFactorEngine _tableFactorEngine;
	protected KBestFactorEngine _kbestFactorEngine;
	protected int _k;
	protected boolean _kIsSmallerThanDomain = false;
	protected boolean _updateDerivative = false;
	protected boolean _dampingInUse = false;
	
	/*--------------
	 * Construction
	 */
	
	public STableFactor(Factor factor)
	{
		super(factor);
		
		_dampingParams = new double[_factor.getSiblingCount()];
		_tableFactorEngine = new TableFactorEngine(this);
		
		
		//TODO: should I recheck for factor table every once in a while?
		if (factor.getFactorFunction().factorTableExists(getFactor()))
		{
			_kbestFactorEngine = new KBestFactorTableEngine(this);
		}
		else
		{
			_kbestFactorEngine = new KBestFactorEngine(this);
		}
	}
	
	/*---------------------
	 * ISolverNode methods
	 */
	
	@Override
	public void moveMessages(ISolverNode other, int portNum, int otherPort)
	{
		super.moveMessages(other,portNum,otherPort);
		STableFactor sother = (STableFactor)other;
	    if (_dampingInUse)
	    	_savedOutMsgArray[portNum] = sother._savedOutMsgArray[otherPort];
	    
	}

	@Override
	protected void doUpdate()
	{
		
		if (_kIsSmallerThanDomain)
			//TODO: damping
			_kbestFactorEngine.update();
		else
			_tableFactorEngine.update();
		
		if (_updateDerivative)
			for (int i = 0; i < _inputMsgs.length ;i++)
				updateDerivative(i);
		
	}
	
	@Override
	public void doUpdateEdge(int outPortNum)
	{
		
		if (_kIsSmallerThanDomain)
			_kbestFactorEngine.updateEdge(outPortNum);
		else
			_tableFactorEngine.updateEdge(outPortNum);

		if (_updateDerivative)
			updateDerivative(outPortNum);
		
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
	    {
	    	_savedOutMsgArray = new double[numPorts][];
	    
    		for (int port = 0; port < numPorts; port++)
    				_savedOutMsgArray[port] = new double[_inputMsgs[port].length];
	    }
	    
		setK(Integer.MAX_VALUE);

	}

	/*
	 * (non-Javadoc)
	 * @see com.analog.lyric.dimple.solvers.core.SFactorBase#getBelief()
	 * 
	 * Calculates a piece of the beta free energy
	 */
	@Override
	public double [] getBelief()
	{
		double [] retval = getUnormalizedBelief();
		double sum = 0;
		for (int i = 0; i < retval.length; i++)
			sum += retval[i];
		for (int i = 0; i < retval.length; i++)
			retval[i] /= sum;
		return retval;
	}
	
	/*--------------------------
	 * STableFactorBase methods
	 */
	
	@Override
	protected void setTableRepresentation(IFactorTable table)
	{
		table.setRepresentation(FactorTableRepresentation.SPARSE_WEIGHT_WITH_INDICES);
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
	    
		for (int port = 0; port < _inputMsgs.length; port++)
				_savedOutMsgArray[port] = new double[_inputMsgs[port].length];

	}
	
	public double getDamping(int index)
	{
		return _dampingParams[index];
	}
	
	public int getK()
	{
		return _k;
	}
	
	public void setK(int k)
	{
		
		_k = k;
		_kbestFactorEngine.setK(k);
		_kIsSmallerThanDomain = false;
		for (int i = 0; i < _inputMsgs.length; i++)
		{
			if (_inputMsgs[i] != null && _k < _inputMsgs[i].length)
			{
				_kIsSmallerThanDomain = true;
				break;
			}
		}
	}


	public void setUpdateDerivative(boolean updateDer)
	{
		_updateDerivative = updateDer;
	}
	
	
	
	public double [] getUnormalizedBelief()
	{
		final int [][] table = getFactorTable().getIndicesSparseUnsafe();
		final double [] values = getFactorTable().getWeightsSparseUnsafe();
		final int nEntries = values.length;
		final double [] retval = new double[nEntries];
		
		
		for (int i = 0; i < nEntries; i++)
		{
			retval[i] = values[i];
			
			final int[] indices = table[i];
			for (int j = 0; j < indices.length; j++)
			{
				retval[i] *= _inputMsgs[j][indices[j]];
			}
		}
		
		return retval;
	}
	

	@Override
	public FactorFunction getFactorFunction()
	{
		return getFactor().getFactorFunction();
	}

	@Override
	public double initAccumulator()
	{
		return 1;
	}

	@Override
	public double accumulate(double oldVal, double newVal)
	{
		return oldVal*newVal;
	}

	@Override
	public double combine(double oldVal, double newVal)
	{
		return oldVal+newVal;
	}

	@Override
	public void normalize(double[] outputMsg)
	{
		double sum = 0;
		for (int i = 0; i < outputMsg.length; i++)
			sum += outputMsg[i];
		
		if (sum == 0)
			throw new DimpleException("Update failed in SumProduct Solver.  All probabilities were zero when calculating message for port "
					+ " on factor " +_factor.getLabel());

    	for (int i = 0; i < outputMsg.length; i++)
    		
    		outputMsg[i] /= sum;
	}

	@Override
	public double evalFactorFunction(Object[] inputs)
	{
		return getFactor().getFactorFunction().eval(inputs);
	}

	@Override
	public void initMsg(double[] msg)
	{
		Arrays.fill(msg, 0);
	}

	@Override
	public double getFactorTableValue(int index)
	{
		return getFactorTable().getWeightsSparseUnsafe()[index];
	}

	@Override
	public int[] findKBestForMsg(double[] msg, int k)
	{
		return Sort.quickfindLastKindices(msg, k);
	}

	/******************************************************
	 * Energy, Entropy, and derivatives of all that.
	 ******************************************************/


	@Override
	public double getInternalEnergy()
	{
		final double [] beliefs = getBelief();
		final double [] weights = getFactorTable().getWeightsSparseUnsafe();
		
		double sum = 0;
		for (int i = beliefs.length; --i>=0;)
		{
			sum += beliefs[i] * weightToEnergy(weights[i]);
		}
		
		return sum;
	}
	
	@Override
	public double getBetheEntropy()
	{
		double sum = 0;
		
		final double [] beliefs = getBelief();
		for (double belief : beliefs)
		{
			sum -= belief * Math.log(belief);
		}
		
		return sum;
	}
	
	public double calculateDerivativeOfInternalEnergyWithRespectToWeight(int weightIndex)
	{
		SFactorGraph sfg = (SFactorGraph)getRootGraph();
		
		boolean isFactorOfInterest = sfg.getCurrentFactorTable() == getFactor().getFactorTable();
		
		double [] weights = _factor.getFactorTable().getWeightsSparseUnsafe();
		//TODO: avoid recompute
		double [] beliefs = getBelief();
		
		double sum = 0;
		
		for (int i = 0; i < weights.length; i++)
		{
			//beliefs = getUnormalizedBelief();
	
			//Belief'(weightIndex)*(-log(weight(weightIndex))) + Belief(weightIndex)*(-log(weight(weightIndex)))'
			//(-log(weight(weightIndex)))' = - 1 / weight(weightIndex)
			double mlogweight = -Math.log(weights[i]);
			double belief = beliefs[i];
			double mlogweightderivative = 0;
			if (i == weightIndex && isFactorOfInterest)
				mlogweightderivative = -1.0 / weights[weightIndex];
			double beliefderivative = calculateDerivativeOfBeliefWithRespectToWeight(weightIndex,i,isFactorOfInterest);
			sum += beliefderivative*mlogweight + belief*mlogweightderivative;
			//sum += beliefderivative;
		}
		//return beliefderivative*mlogweight + belief*mlogweightderivative;
	
		return sum;
	}
	
	public double calculateDerivativeOfBeliefNumeratorWithRespectToWeight(int weightIndex, int index, boolean isFactorOfInterest)
	{
		double [] weights = _factor.getFactorTable().getWeightsSparseUnsafe();
		int [][] indices = _factor.getFactorTable().getIndicesSparseUnsafe();
		
		//calculate product of messages and phi
		double prod = weights[index];
		for (int i = 0; i < _inputMsgs.length; i++)
			prod *= _inputMsgs[i][indices[index][i]];

		double sum = 0;
		
		//if index == weightIndex, add in this term
		if (index == weightIndex && isFactorOfInterest)
		{
			sum = prod / weights[index];
		}
		
		//for each variable
		for (int i = 0; i < _inputMsgs.length; i++)
		{
			SDiscreteVariable var = (SDiscreteVariable)getFactor().getConnectedNodesFlat().getByIndex(i).getSolver();
			
			//divide out contribution
			sum += prod / _inputMsgs[i][indices[index][i]] * var.getMessageDerivative(weightIndex,getFactor())[indices[index][i]];
		}
		return sum;
	}
	
	public double calculateDerivativeOfBeliefDenomenatorWithRespectToWeight(int weightIndex, int index, boolean isFactorOfInterest)
	{
		double sum = 0;
		for (int i = 0, end = getFactor().getFactorTable().sparseSize(); i < end; i++)
			sum += calculateDerivativeOfBeliefNumeratorWithRespectToWeight(weightIndex,i,isFactorOfInterest);
		return sum;
	}
	
	public double calculateDerivativeOfBeliefWithRespectToWeight(int weightIndex,int index, boolean isFactorOfInterest)
	{
		double [] un = getUnormalizedBelief();
		double f = un[index];
		double fderivative = calculateDerivativeOfBeliefNumeratorWithRespectToWeight(weightIndex, index,isFactorOfInterest);
		double gderivative = calculateDerivativeOfBeliefDenomenatorWithRespectToWeight(weightIndex, index,isFactorOfInterest);
		double g = 0;
		for (int i = 0; i < un.length; i++)
			g += un[i];
		
		double tmp = (fderivative*g-f*gderivative)/(g*g);
		return tmp;
	}
	
	public void initializeDerivativeMessages(int weights)
	{
		_outPortDerivativeMsgs = new double[weights][_inputMsgs.length][];
		for (int i = 0; i < weights; i++)
			for (int j = 0; j < _inputMsgs.length; j++)
				_outPortDerivativeMsgs[i][j] = new double[_inputMsgs[j].length];
	}
	
	public double [] getMessageDerivative(int wn, VariableBase var)
	{
		int index = getFactor().getPortNum(var);
		return _outPortDerivativeMsgs[wn][index];
	}
	
	public double calculateMessageForDomainValueAndTableIndex(int domainValue, int outPortNum, int tableIndex)
	{
		IFactorTable ft = getFactor().getFactorTable();
		int [][] indices = ft.getIndicesSparseUnsafe();
		double [] weights = ft.getWeightsSparseUnsafe();

		if (indices[tableIndex][outPortNum] == domainValue)
		{
			double prod = weights[tableIndex];
			for (int j = 0; j < _inputMsgs.length; j++)
			{
				if (outPortNum != j)
				{
					prod *= _inputMsgs[j][indices[tableIndex][j]];
				}
			}
			
			return prod;
		}
		else
			return 0;
	}
	
	public double calculateMessageForDomainValue(int domainValue, int outPortNum)
	{
		IFactorTable ft = getFactor().getFactorTable();
		double sum = 0;
		int [][] indices = ft.getIndicesSparseUnsafe();
		
		for (int i = 0, end = ft.sparseSize(); i < end; i++)
			if (indices[i][outPortNum] == domainValue)
				sum += calculateMessageForDomainValueAndTableIndex(domainValue,outPortNum,i);
		
		return sum;
	}
	
	
	public double calculatedf(int outPortNum, int domainValue, int wn, boolean factorUsesTable)
	{
		IFactorTable ft = getFactor().getFactorTable();
		double sum = 0;
		int [][] indices = ft.getIndicesSparseUnsafe();
		double [] weights = ft.getWeightsSparseUnsafe();
		
		for (int i = 0; i < indices.length; i++)
		{
			if (indices[i][outPortNum] == domainValue)
			{
				double prod = calculateMessageForDomainValueAndTableIndex(domainValue,outPortNum,i);
				
				if (factorUsesTable && (wn == i))
				{
					sum += prod/weights[i];
				}
				
				for (int j = 0; j < _inputMsgs.length; j++)
				{
					
					if (j != outPortNum)
					{
						SDiscreteVariable sv = (SDiscreteVariable)getFactor().getConnectedNodesFlat().getByIndex(j).getSolver();
						double [] dvar = sv.getMessageDerivative(wn,getFactor());
								
						sum += (prod / _inputMsgs[j][indices[i][j]]) * dvar[indices[i][j]];
					}
				}
								
			}
		}
		
		return sum;
	}
	
	public double calculatedg(int outPortNum, int wn, boolean factorUsesTable)
	{
		double sum = 0;
		for (int i = 0; i < _inputMsgs[outPortNum].length; i++)
			sum += calculatedf(outPortNum,i,wn,factorUsesTable);
		
		return sum;
				
	}
	
	public void updateDerivativeForWeightAndDomain(int outPortNum, int wn, int d,boolean factorUsesTable)
	{
		//TODO: not re-using computation efficiently.
		
		//calculate f
		double f = calculateMessageForDomainValue(d,outPortNum);
		
		//calculate g
		double g = 0;
		for (int i = 0; i < _inputMsgs[outPortNum].length; i++)
			g += calculateMessageForDomainValue(i,outPortNum);
		
		double derivative = 0;
		if (g != 0)
		{
			double df = calculatedf(outPortNum,d,wn,factorUsesTable);
			double dg = calculatedg(outPortNum,wn,factorUsesTable);
		
			
			//derivative = df;
			derivative = (df*g - f*dg) / (g*g);
			
		}
		
		_outPortDerivativeMsgs[wn][outPortNum][d] = derivative;
		
		
	}
	
	public void updateDerivativeForWeight(int outPortNum, int wn,boolean factorUsesTable)
	{
		int D = _inputMsgs[outPortNum].length;
		
		for (int d = 0; d < D; d++)
		{
			updateDerivativeForWeightAndDomain(outPortNum,wn,d,factorUsesTable);
		}
	}
	
	public void updateDerivative(int outPortNum)
	{
		SFactorGraph sfg = (SFactorGraph)getRootGraph();
		IFactorTable ft = sfg.getCurrentFactorTable();
		int numWeights = ft.sparseSize();
		
		for (int wn = 0; wn < numWeights; wn++)
		{
			updateDerivativeForWeight(outPortNum,wn,ft == getFactor().getFactorTable());
		}
	}
	
	public double calculateDerivativeOfBetheEntropyWithRespectToWeight(int weightIndex)
	{
		
		boolean isFactorOfInterest = ((SFactorGraph)getRootGraph()).getCurrentFactorTable() == getFactor().getFactorTable();
				
		//Belief'(weightIndex)*(-log(Belief(weightIndex))) + Belief(weightIndex)*(-log(Belief(weightIndex)))'
		double [] beliefs = getBelief();
		double sum = 0;
		for (int i = 0; i < beliefs.length; i++)
		{
			double beliefderivative = calculateDerivativeOfBeliefWithRespectToWeight(weightIndex,i,isFactorOfInterest);
			double belief = beliefs[i];
			sum += beliefderivative*(-Math.log(belief)) - beliefderivative;
		}
		return sum;
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
	
}
