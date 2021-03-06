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

package com.analog.lyric.dimple.model.variables;

import java.util.Arrays;

import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.domains.JointDiscreteDomain;
import com.analog.lyric.dimple.solvers.core.SDiscreteVariableBase;
import com.analog.lyric.dimple.solvers.interfaces.IDiscreteSolverVariable;
import com.analog.lyric.util.misc.Internal;

public class Discrete extends VariableBase
{
	/*--------------
	 * Construction
	 */
	public Discrete(DiscreteDomain domain)
	{
		this(domain, "Discrete");
	}

	public Discrete(Object... domain)
	{
		this(DiscreteDomain.create(domain),"Discrete");

		if (domain.length < 1)
			throw new DimpleException(String.format("ERROR Variable domain length %d must be at least 2", domain.length));
	}
	
	public Discrete(DiscreteDomain domain, String modelerClassName)
	{
		super(domain, modelerClassName);
	}
	


    public double [] getInput()
    {
    	return (double[])getInputObject();
    }
    
    /*---------------------
     * ISolverNode methods
     */
    
    @Override
    public IDiscreteSolverVariable getSolver()
    {
    	return (IDiscreteSolverVariable)super.getSolver();
    }
    
    /*----------------------
     * VariableBase methods
     */
    
    @Override
    public final Discrete asDiscreteVariable()
    {
    	return this;
    }
    
    @Override
	public DiscreteDomain getDomain()
    {
    	return (DiscreteDomain)super.getDomain();
    }
    
    public DiscreteDomain getDiscreteDomain()
    {
    	return getDomain();
    }
    
    @Override
    public Integer getFixedValueObject()
    {
    	return (Integer)super.getFixedValueObject();
    }
    
    @Override
	protected void setFixedValueObject(Object value, boolean leaveInput)
	{
    	super.setFixedValueObject(value, leaveInput);
	}

    @Override
    public Object getInputObject()
    {
    	Object tmp = super.getInputObject();
    	
    	if (tmp == null)
    		return getDefaultPriors(getDiscreteDomain());
    	else
    		return tmp;
    }
    
    /*------------------
     * Discrete methods
     */
    
    public double [] getBelief()
    {
    	return (double[])getBeliefObject();
    }
    
    @Override
	public Object getBeliefObject()
    {
    	if (_solverVariable != null)
    		return _solverVariable.getBelief();
    	else
    		return getInputObject();
    }
    
    public int getGuessIndex()
    {
    	return getSolver().getGuessIndex();
    }
    
    public void setGuessIndex(int guess)
    {
    	getSolver().setGuessIndex(guess);
    }
    
    public Object getValue()
    {
    	if (_solverVariable != null)
    		return _solverVariable.getValue();
    	else
    		return null;
    }
   
    public int getValueIndex()
    {
    	if (_solverVariable != null)
    		return ((SDiscreteVariableBase)_solverVariable).getValueIndex();
    	else
    		return 0;
    }

    
	private double [] getDefaultPriors(DiscreteDomain domain)
	{
		final int length = domain.size();
		double [] retval = new double[length];
		double val = 1.0/length;
		for (int i = 0; i < retval.length; i++)
			retval[i] = val;
		return retval;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * All {@code variables} must be of type {@link Discrete}. The domain of the returned
	 * variable will be a {@link JointDiscreteDomain} with the subdomains in the same order
	 * as {@code variables}.
	 */
	@Internal
    @Override
	public VariableBase createJointNoFactors(VariableBase ... variables)
    {
    	final boolean thisIsFirst = (variables[0] == this);
    	final int dimensions = thisIsFirst ? variables.length: variables.length + 1;
    	final DiscreteDomain[] domains = new DiscreteDomain[dimensions];
    	final double[][] subdomainWeights = new double[dimensions][];
    	domains[0] = getDomain();
    	subdomainWeights[0] = getInput();
    	
    	for (int i = thisIsFirst ? 1 : 0; i < dimensions; ++i)
    	{
    		final Discrete var = variables[i].asDiscreteVariable();
    		if (var == null)
    		{
    			throw new DimpleException("Discrete.createJointNoFactors does not support non-discrete variables");
    		}
    		domains[i] = var.getDomain();
    		subdomainWeights[i] = var.getInput();
    	}

    	final JointDiscreteDomain<?> jointDomain = DiscreteDomain.joint(domains);
    	final Discrete jointVar =  new Discrete(jointDomain);
    	jointVar.setInput(joinWeights(subdomainWeights));
    	return jointVar;
    }
	
	public void setInput(double ... value)
	{
		setInputObject(value);
	}
	
	
	// Fix the variable to a specific value
	public final int getFixedValueIndex()
	{
		Integer index = getFixedValueObject();
		if (index == null)
			throw new DimpleException("Fixed value not set");
		
		return index;
	}
	
	public final Object getFixedValue()
	{
		Integer index = getFixedValueObject();
		if (index == null)
			throw new DimpleException("Fixed value not set");
		
		return getDomain().getElement(index);
	}
	public void setFixedValueIndex(int fixedValueIndex)
	{
		// In case the solver doesn't directly support fixed-values, convert the fixed-value to an input
		double[] input = new double[getDiscreteDomain().size()];
		Arrays.fill(input, 0);
		input[fixedValueIndex] = 1;
		_input = input;
		setFixedValueObject(fixedValueIndex,true);
		_input = input;
		
	}
	public void setFixedValue(Object fixedValue)
	{
		int index = getDomain().getIndex(fixedValue);
		if (index < 0)
			throw new DimpleException("Attempt to set variable to a fixed value that is not an element of the variable's domain.");
		setFixedValueIndex(index);
	}
	
	/*-----------------
	 * Private methods
	 */

	private double[] joinWeights(double[] ... subdomainWeights)
	{
		// Validate dimensions
		//   This simply validates that the joint cardinality matches. It does not
		//   actually compare against the subdomains. Ideally we should do that but
		//   allowing for "drilling down" into subdomains that are joint domains.
		int cardinality = 1;
		for (double[] array : subdomainWeights)
		{
			cardinality *= array.length;
		}
		
		double[] weights = new double[cardinality];
		Arrays.fill(weights, 1.0);
		
		int inner = 1, outer = cardinality;
		for (double[] subweights : subdomainWeights)
		{
			final int size = subweights.length;
			int i = 0;
			
			outer /= size;
			for (int o = 0; o < outer; ++o)
			{
				for (double weight : subweights)
				{
					for (int r = 0; r < inner; ++r)
					{
						weights[i++] *= weight;
					}
				}
			}
			inner *= size;
		}
		
		return weights;
	}
	

}
