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

package com.analog.lyric.dimple.test.solvers.gibbs;

import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.domains.Domain;
import com.analog.lyric.dimple.model.values.Value;
import com.analog.lyric.dimple.solvers.core.SolverRandomGenerator;
import com.analog.lyric.dimple.solvers.core.proposalKernels.BlockProposal;
import com.analog.lyric.dimple.solvers.core.proposalKernels.IBlockProposalKernel;

/**
 * 
 * @since 0.06
 * @author jbernst2
 */
public class TrivialUniformBlockProposer implements IBlockProposalKernel
{
	@Override
	public BlockProposal next(Value[] currentValue, Domain[] variableDomain)
	{
		int numVariables = currentValue.length;
		Value[] newValue = new Value[numVariables];
		for (int i = 0; i < numVariables; i++)
		{
			Domain domain = variableDomain[i];
			if (domain.isDiscrete())
			{
				DiscreteDomain discreteDomain = domain.asDiscrete();
				int domainSize = discreteDomain.size();
				Value v = Value.create(discreteDomain);
				v.setIndex(SolverRandomGenerator.rand.nextInt(domainSize));
				newValue[i] = v;
			}
			else
			{
				throw new DimpleException("Not supported");
			}
		}
		
		double hastingsTerm = 0;
		return new BlockProposal(newValue, hastingsTerm);
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
