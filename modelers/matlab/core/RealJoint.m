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

classdef RealJoint < VariableBase
    
    
    methods
        function obj = RealJoint(varargin)
            obj@VariableBase([],[]);
            %first arg can be a number
            %or it can be a domain
            %for now, only take one argument
            
            
            if length(varargin) == 4 && strcmp('existing',varargin{2})
                obj.Domain = varargin{1};
                obj.VectorObject = varargin{3};
                obj.VectorIndices = varargin{4};
            else
                
                % First argument can be either a RealJointDomain or
                % the number of dimensions in the joint varaible
                if (isa(varargin{1}, 'RealJointDomain'));
                    domain = varargin{1};
                    nextArg = 2;
                elseif (isnumeric(varargin{1}) && (nargin > 1) && isa(varargin{2}, 'RealJointDomain'))
                    % This case is for Complex, where size 2 is always
                    % passed as the first argument
                    domain = varargin{2};
                    if (varargin{1} ~= domain.NumElements);
                        error('Number of domain elements must match the number of joint variable elements');
                    end
                    nextArg = 3;
                else
                    domain = RealJointDomain(varargin{1});
                    nextArg = 2;
                end
                obj.Domain = domain;
               
                % Determine the size of the array of RealJoint variables
                if nargin < nextArg
                    dims = 1;
                else
                    dimArgs = varargin(nextArg:end);
                    dims = [dimArgs{:}];
                    if size(dims) == 1
                        dimArgs = {dimArgs{1}, dimArgs{1}};
                        dims = [dimArgs{:}];
                    end
                end
                
                numEls = prod(dims);
                modeler = getModeler();
                varMat = modeler.createRealJointVariableVector(class(obj),domain.IDomain,numEls);
                obj.VectorObject = varMat;
                obj.VectorIndices = 0:(numEls-1);
                if length(dims)==1
                    dims = [dims 1];
                end
                obj.VectorIndices = reshape(obj.VectorIndices,dims);
            end
            
        end
        
    end
    
    methods (Access=protected)
        
        function var = createObject(obj,varMat,VectorIndices)
            var = RealJoint(obj.Domain,'existing',varMat,VectorIndices);
        end
        
        
        function x = getInput(obj)
            error('not implemented');
        end
        
        function setFixedValue(obj,value)
            error('not implemented');
        end
        
        function x = getFixedValue(obj)
            error('not implemented');
        end
        
        function b = getBelief(obj)
            sz = size(obj);
            
            b = cell(sz);
            
            varids = reshape(obj.VectorIndices,numel(obj.VectorIndices),1);
            a = cell(obj.VectorObject.getBeliefs(varids));
            
            if prod(sz) == 1
                m = MultivariateMsg(0,0);
                m.IMsg = a{1};
                b = m;
            else
                for i = 1:numel(b)
                    m = MultivariateMsg(0,0);
                    m.IMsg = a{i};
                    b{i} = m;
                end
            end
        end
        
        
        function v = getValue(obj)
            varids = reshape(obj.VectorIndices,numel(obj.VectorIndices),1);
            values = obj.VectorObject.getValues(varids);
            v = MatrixObject.unpack(values,obj.VectorIndices);
        end
        

        
        function setInput(obj,input)
            v = obj.VectorIndices;
            
            if isa(input,'Msg')
                input = input.IMsg;
            elseif iscell(input)
                if (isa(input{1}, 'FactorFunction'))
                    for i=1:length(input)
                        input{i} = input{i}.get();
                    end
                elseif (iscell(input{1}))
                    for i=1:length(input)
                        input{i} = FactorFunction(input{i}{:}).get();
                    end
                end
            end
            
            varids = reshape(v,numel(v),1);
            obj.VectorObject.setInput(varids,input);
        end
        
    end
    
end
