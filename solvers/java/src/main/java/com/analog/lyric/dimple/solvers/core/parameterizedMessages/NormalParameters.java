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

package com.analog.lyric.dimple.solvers.core.parameterizedMessages;


public class NormalParameters implements IParameterizedMessage
{
	private double _mean = 0;
	private double _precision = 0;
	
	public NormalParameters() {}
	public NormalParameters(double mean, double precision)
	{
		_mean = mean;
		_precision = precision;
	}
	public NormalParameters(NormalParameters other)		// Copy constructor
	{
		this(other._mean, other._precision);
	}

	public NormalParameters clone()
	{
		return new NormalParameters(this);
	}

	public final double getMean() {return _mean;}
	public final double getPrecision() {return _precision;}
	public final double getVariance() {return 1/_precision;}
	public final double getStandardDeviation() {return 1/Math.sqrt(_precision);}
	
	public final void setMean(double mean) {_mean = mean;}
	public final void setPrecision(double precision) {_precision = precision;}
	public final void setVariance(double variance) {_precision = 1/variance;}
	public final void setStandardDeviation(double standardDeviation) {_precision = 1/(standardDeviation*standardDeviation);}

	public final void set(NormalParameters other)	// Set from copy
	{
		_mean = other._mean;
		_precision = other._precision;
	}
	
	@Override
	public final void setNull()
	{
		_mean = 0;
		_precision = 0;
	}
}
