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

package com.analog.lyric.dimple.factorfunctions;

import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunctionUtilities;
import com.analog.lyric.dimple.model.domains.FiniteFieldNumber;
import com.analog.lyric.dimple.model.values.Value;


/**
 * Deterministic finite field (GF(2^n)) addition. This is a deterministic directed factor.
 * 
 * The variables are ordered as follows in the argument list:
 * 
 * 1) Output (FiniteFieldVariable; Output = input1 + input2)
 * 2) Input1 (FiniteFieldVariable)
 * 3) Input2 (FiniteFieldVariable)
 * 
 * @since 0.05
 */
public class FiniteFieldAdd extends FactorFunction
{
    @Override
    public double evalEnergy(Object ... arguments)
    {
    	// Allow one constant input
    	int result = ((FiniteFieldNumber)arguments[0]).intValue();
    	Object arg1 = arguments[1];
    	int input1 = (arg1 instanceof FiniteFieldNumber) ? ((FiniteFieldNumber)arg1).intValue() : FactorFunctionUtilities.toInteger(arg1);
    	Object arg2 = arguments[2];
    	int input2 = (arg2 instanceof FiniteFieldNumber) ? ((FiniteFieldNumber)arg2).intValue() : FactorFunctionUtilities.toInteger(arg2);

    	double computedResult = input1 ^ input2;
    	
    	return (computedResult == result) ? 0 : Double.POSITIVE_INFINITY;
    }
    
    @Override
    public double evalEnergy(Value[] values)
    {
    	int result = values[0].getInt();
    	int input1 = values[1].getInt();
    	int input2 = values[2].getInt();
    	
    	int computedResult = input1 ^ input2;
    	
    	return (computedResult == result) ? 0 : Double.POSITIVE_INFINITY;
    }
    
    
    @Override
    public final boolean isDirected()	{return true;}
    @Override
	public final int[] getDirectedToIndices() {return new int[]{0};}
    @Override
	public final boolean isDeterministicDirected() {return true;}
    @Override
	public final void evalDeterministic(Object[] arguments)
    {
    	// Allow one constant input
    	Object arg1 = arguments[1];
    	int input1 = (arg1 instanceof FiniteFieldNumber) ? ((FiniteFieldNumber)arg1).intValue() : FactorFunctionUtilities.toInteger(arg1);
    	Object arg2 = arguments[2];
    	int input2 = (arg2 instanceof FiniteFieldNumber) ? ((FiniteFieldNumber)arg2).intValue() : FactorFunctionUtilities.toInteger(arg2);
    	arguments[0] = input1 ^ input2;		// Replace the output value
    }
}
