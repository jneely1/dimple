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

classdef NBitXorCache < SymmetricRelationTreeGraphCache
    
    methods
        function [graph_out,external_vars] = createGraph(obj,n)
            [graph_out,external_vars] = ...
                createGraph@SymmetricRelationTreeGraphCache(obj,@xorDelta,@Variable,[0 1],n);
        end
    end
    
    methods (Access=protected)
        function key = createKey(obj,n)
            key = createKey@SymmetricRelationTreeGraphCache(obj,@xorDelta,@Variable,[0 1],n);
        end
        
    end
    
end

