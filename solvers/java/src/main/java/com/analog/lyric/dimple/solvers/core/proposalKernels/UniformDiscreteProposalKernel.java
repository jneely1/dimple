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

package com.analog.lyric.dimple.solvers.core.proposalKernels;

import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.domains.Domain;
import com.analog.lyric.dimple.model.values.DiscreteValue;
import com.analog.lyric.dimple.model.values.Value;
import com.analog.lyric.dimple.solvers.core.SolverRandomGenerator;

/**
 * @since 0.05
 */
public class UniformDiscreteProposalKernel implements IProposalKernel
{

	@Override
	public Proposal next(Value currentValue, Domain variableDomain)
	{
		// Choose uniformly at random from among all values except the current value
		DiscreteDomain domain = (DiscreteDomain)variableDomain;
		int currentIndex = ((DiscreteValue)currentValue).getIndex();
		int nextIndex = SolverRandomGenerator.rand.nextInt(domain.size() - 1);
		if (nextIndex >= currentIndex) nextIndex++;
		Value value = Value.create(domain);
		value.setIndex(nextIndex);
		return new Proposal(value);
	}

	@Override
	public void setParameters(Object... parameters)
	{
	}

	@Override
	public Object[] getParameters()
	{
		return null;
	}

}
