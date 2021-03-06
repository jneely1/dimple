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

package com.analog.lyric.dimple.solvers.gaussian;

import com.analog.lyric.dimple.solvers.core.SolverBase;

public class Solver extends SolverBase<com.analog.lyric.dimple.solvers.sumproduct.SFactorGraph>
{

	// The Gaussian solver is now identical to the SumProduct solver
	// This remains for backward compatibility
	@Override
	public com.analog.lyric.dimple.solvers.sumproduct.SFactorGraph createFactorGraph(com.analog.lyric.dimple.model.core.FactorGraph factorGraph)
	{
		return new com.analog.lyric.dimple.solvers.sumproduct.SFactorGraph(factorGraph);
	}
	
}
