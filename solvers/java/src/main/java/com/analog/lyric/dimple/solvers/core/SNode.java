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

package com.analog.lyric.dimple.solvers.core;

import com.analog.lyric.dimple.events.IDimpleEventListener;
import com.analog.lyric.dimple.events.SolverEventSource;
import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.model.core.Node;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.IParameterizedMessage;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;
import com.analog.lyric.options.IOptionHolder;

public abstract class SNode extends SolverEventSource implements ISolverNode
{
	/*-----------
	 * Constants
	 */
	
	/**
	 * Bits in {@link #_flags} reserved by this class and its superclasses.
	 */
	protected static final int RESERVED_FLAGS = 0xFF000000;

	private static final int MESSAGE_EVENT_MASK = 0x03000000;

	private static final int MESSAGE_EVENT_UNKNOWN = 0x00000000;

	private static final int MESSAGE_EVENT_NONE = 0x01000000;

	private static final int MESSAGE_EVENT_ENABLED = 0x03000000;
	
	/*-------
	 * State
	 */

	private final Node _model;
	
	/*--------------
	 * Construction
	 */
	
	public SNode(Node n)
	{
		_model = n;
	}
	
	/*-----------------------
	 * IOptionHolder methods
	 */
	
	@Override
	public IOptionHolder getOptionParent()
	{
		return getParentGraph();
	}
	
	/*---------------------
	 * ISolverNode methods
	 */
	
	@Override
	public Node getModelObject()
    {
    	return _model;
    }
	
	@Override
	public ISolverNode getSibling(int edge)
	{
		return getModelObject().getSibling(edge).getSolver();
	}
	
	@Override
	public int getSiblingCount()
	{
		return getModelObject().getSiblingCount();
	}
	
	@Override
	public void initialize()
	{
		clearFlags();
		for (int i = 0, end = getModelObject().getSiblingCount(); i < end; i++)
			resetEdgeMessages(i);
	}
	
	@Override
	public void setInputMsg(int portIndex, Object obj) {
		throw new DimpleException("Not supported by " + this);
	}
	@Override
	public void setOutputMsg(int portIndex, Object obj) {
		throw new DimpleException("Not supported by " + this);
	}

	@Override
	public void setInputMsgValues(int portIndex, Object obj) {
		throw new DimpleException("Not supported by " + this);
	}
	@Override
	public void setOutputMsgValues(int portIndex, Object obj) {
		throw new DimpleException("Not supported by " + this);
	}

	@Override
	public void update()
	{
		if (raiseMessageEvents())
		{
			final IParameterizedMessage[] oldMessages = cloneMessages();
			
			doUpdate();
			
			if (oldMessages != null)
			{
				final IParameterizedMessage[] newMessages = cloneMessages();
				for (int edge = 0, nEdges = newMessages.length; edge < nEdges; ++edge)
				{
					final IParameterizedMessage oldMessage = oldMessages[edge];
					final IParameterizedMessage newMessage = newMessages[edge];
					
					if (newMessage != null)
					{
						raiseMessageEvent(edge, oldMessage, newMessage);
					}
				}
			}
		}
		else
		{
			doUpdate();
		}
	}

	@Override
	public void updateEdge(int edge)
	{
		if (raiseMessageEvents())
		{
			final IParameterizedMessage oldMessage = cloneMessage(edge);
			
			doUpdateEdge(edge);
			
			final IParameterizedMessage newMessage = cloneMessage(edge);
			if (newMessage != null)
			{
				raiseMessageEvent(edge, oldMessage, newMessage);
			}
		}
		else
		{
			doUpdateEdge(edge);
		}
	}

	/*-------------------------
	 * Protected SNode methods
	 */
	
	/**
	 * Returns a clone of outgoing message for given {@code edge}.
	 * <p>
	 * @param edge index in the range [0, {@link #getSiblingCount()}-1].
	 * @return clone of message if applicable. May return null if there is no message or if subclass
	 * does not support messages. In the latter case, {@link #supportsMessageEvents()} should return false.
	 * <p>
	 * The default implementation returns null.
	 * <p>
	 * Subclasses that override this method to return a non-null message should also override
	 * {@link #supportsMessageEvents()}.
	 * <p>
	 * @since 0.06
	 */
	protected IParameterizedMessage cloneMessage(int edge)
	{
		return null;
	}

	private final IParameterizedMessage[] cloneMessages()
	{
		IParameterizedMessage[] messages = null;
		
		final int size = getSiblingCount();
		if (size > 0)
		{
			final IParameterizedMessage firstMessage = cloneMessage(0);
			if (firstMessage != null)
			{
				messages = new IParameterizedMessage[size];
				messages[0] = firstMessage;
				for (int i = 1; i < size; ++i)
				{
					messages[i] = cloneMessage(i);
				}
			}
		}
		
		return messages;
	}

	protected void doUpdate()
	{
		for (int i = 0, end = getSiblingCount(); i < end; i++)
		{
			doUpdateEdge(i);
		}
	}

	protected abstract void doUpdateEdge(int edge);

	/**
	 * Raise a {@link IMessageUpdateEvent} event.
	 * <p>
	 * Default implementation does nothing. When this method does nothing, then {@link #supportsMessageEvents()}
	 * should also return false.
	 * <p>
	 * @param edge is the outgoing edge for the messages.
	 * @param oldMessage is the previous value of the message, which may be null.
	 * @param newMessage is the new message value, which must not be null.
	 * @since 0.06
	 */
	protected void raiseMessageEvent(
		int edge,
		IParameterizedMessage oldMessage,
		IParameterizedMessage newMessage)
	{
	}

	/**
	 * Indicate subclass has a concept of passing a {@link IParameterizedMessage} messages.
	 * <p>
	 * Should be false if {@link #cloneMessage(int)} can only return null or {@link #raiseMessageEvent}
	 * has no effect.
	 * <p>
	 * The default implementation returns false.
	 * <p>
	 * @since 0.06
	 */
	protected boolean supportsMessageEvents()
	{
		return false;
	}
	
	/*-----------------
	 * Private methods
	 */

	/**
	 * Indicates whether to generate {@link IMessageUpdateEvent}s.
	 * @return
	 * @since 0.06
	 */
	private boolean raiseMessageEvents()
	{
		final int flags = _flags & MESSAGE_EVENT_MASK;
		
		if (flags == MESSAGE_EVENT_NONE)
		{
			// Check this first to minimize overhead when known to be disabled
			return false;
		}
		else if (flags == MESSAGE_EVENT_UNKNOWN)
		{
			boolean enabled = false;
			if (supportsMessageEvents())
			{
				final IDimpleEventListener listener = getEventListener();
				if (listener != null)
				{
					enabled = listener.isListeningFor(VariableToFactorMessageEvent.class, this);
				}
			}
			setFlagValue(MESSAGE_EVENT_MASK, enabled ? MESSAGE_EVENT_ENABLED : MESSAGE_EVENT_NONE);
			return enabled;
		}
		else
		{
			return flags == MESSAGE_EVENT_ENABLED;
		}
	}
}
