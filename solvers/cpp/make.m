%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%   Copyright 2012 Analog Devices, Inc.
%
%   Licensed under the Apache License, Version 2.0 (the "License");
%   you may not use this file except in compliance with the License.
%   You may obtain a copy of the License at
%
%       http://www.apache.org/licenses/LICENSE-2.0
%
%   Unless required by applicable law or agreed to in writing, software
%   distributed under the License is distributed on an "AS IS" BASIS,
%   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
%   See the License for the specific language governing permissions and
%   limitations under the License.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function make(debug)

    %clear all;
    %clear classes;

    if nargin < 1
        debug = 0;
    end

    files = {'DimpleEntry.cpp',...
        'CombinationTableFactory.cpp',...
        'Domain.cpp',...
        'DoubleDomainItem.cpp',...
        'Function.cpp',...
        'IDomainItem.cpp',...
        'IFunctionPointer.cpp',...
        'INode.cpp',...
        'Variable.cpp',...
        'FactorGraph.cpp',...
        'DimpleException.cpp',...
        'Port.cpp',...
        'DimpleManager.cpp',...
        'MatlabFunctionPointer.cpp',...
        'MatrixDomainItem.cpp',...
        'MexHelpers.cpp',...
        };

    %TODO: have common file list
    if ~debug
        mex(files{:});
    else
        mex('-g',files{:});
    end
end
