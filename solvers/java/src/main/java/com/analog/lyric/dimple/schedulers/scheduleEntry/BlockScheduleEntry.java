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

package com.analog.lyric.dimple.schedulers.scheduleEntry;

import java.util.ArrayList;
import java.util.Map;

import com.analog.lyric.dimple.model.core.INode;
import com.analog.lyric.dimple.model.core.Node;
import com.analog.lyric.dimple.model.core.Port;



/**
 * @author jeffb
 * 
 *         A schedule entry that contains a collection of nodes to be updated together.
 *         This class is primarily targeted at the Gibbs solver for block Gibbs updates.
 *         For that case, all of the nodes in the collection would be variables.
 */
public class BlockScheduleEntry implements IScheduleEntry
{
	IBlockUpdater _blockUpdater;
	
	public BlockScheduleEntry(INode[] nodeList, IBlockUpdater blockUpdater)
	{
		_blockUpdater = blockUpdater;
		_blockUpdater.attachNodes(nodeList);
	}
	public BlockScheduleEntry(INode[] nodeList, Class<IBlockUpdater> blockUpdaterClass) throws Exception
	{
		this(nodeList, blockUpdaterClass.newInstance());
	}

	
	@Override
	public void update()
	{
		_blockUpdater.update();
	}

	public INode[] getNodeList()
	{
		return _blockUpdater.getNodeList();
	}
	
	public IBlockUpdater getBlockUpdater()
	{
		return _blockUpdater;
	}
	
	@Override
	public IScheduleEntry copy(Map<Node,Node> old2newObjs)
	{
		return copy(old2newObjs, false);
	}
	@Override
	public IScheduleEntry copyToRoot(Map<Node,Node> old2newObjs)
	{
		return copy(old2newObjs, true);
	}
	
	public IScheduleEntry copy(Map<Node,Node> old2newObjs, boolean copyToRoot)
	{
		INode[] oldNodeList = _blockUpdater.getNodeList();
		INode[] newNodeList = new INode[oldNodeList.length];
		
		for (int i = 0; i < oldNodeList.length; i++)
			newNodeList[i] = old2newObjs.get(oldNodeList[i]);
		
		return new BlockScheduleEntry(newNodeList, _blockUpdater.create());
	}

	@Override
	public Iterable<Port> getPorts()
	{
		ArrayList<Port> ports = new ArrayList<Port>();
		
		// For all nodes in the block
		for (INode node : _blockUpdater.getNodeList())
		{
			// Add each port of this node to the list.
			for (int index = 0, end = node.getSiblingCount(); index < end; index++)
			{
				ports.add(new Port(node,index));
			}
		}
		return ports;
	}

}
