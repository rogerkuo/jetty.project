//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.core.internal;

import java.nio.ByteBuffer;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.FrameHandler.Configuration;
import org.eclipse.jetty.websocket.core.OpCode;

/**
 * Used to split large data frames into multiple frames below the maxFrameSize.
 * Control frames and dataFrames smaller than the maxFrameSize will be forwarded
 * directly to {@link #forwardFrame(Frame, Callback, boolean)}.
 */
public abstract class FragmentingFlusher extends TransformingFlusher
{
    private static final Logger LOG = Log.getLogger(FragmentingFlusher.class);
    private final Configuration configuration;
    private FrameEntry current;
    private ByteBuffer payload;

    public FragmentingFlusher(Configuration configuration)
    {
        this.configuration = configuration;
    }

    abstract void forwardFrame(Frame frame, Callback callback, boolean batch);

    @Override
    protected boolean onFrame(Frame frame, Callback callback, boolean batch)
    {
        long maxFrameSize = configuration.getMaxFrameSize();
        if (frame.isControlFrame() || maxFrameSize <= 0 || frame.getPayloadLength() <= maxFrameSize)
        {
            forwardFrame(frame, callback, batch);
            return true;
        }

        current = new FrameEntry(frame, callback, batch);
        payload = frame.getPayload().slice();

        boolean finished = fragment(callback, true);
        if (finished)
        {
            current = null;
            payload = null;
        }
        return finished;
    }

    @Override
    protected boolean transform(Callback callback)
    {
        boolean finished = fragment(callback, false);
        if (finished)
        {
            current = null;
            payload = null;
        }
        return finished;
    }

    private boolean fragment(Callback callback, boolean first)
    {
        Frame frame = current.frame;
        int remaining = payload.remaining();
        long maxFrameSize = configuration.getMaxFrameSize();
        int fragmentSize = (int)Math.min(remaining, maxFrameSize);

        boolean continuation = (frame.getOpCode() == OpCode.CONTINUATION) || !first;
        Frame fragment = new Frame(continuation ? OpCode.CONTINUATION : frame.getOpCode());
        boolean finished = (maxFrameSize <= 0 || remaining <= maxFrameSize);
        fragment.setFin(frame.isFin() && finished);

        // If we don't need to fragment just forward with original payload.
        if (finished)
        {
            fragment.setPayload(payload);
            forwardFrame(fragment, callback, current.batch);
            return true;
        }

        // Slice the fragmented payload from the buffer.
        int limit = payload.limit();
        int newLimit = payload.position() + fragmentSize;
        payload.limit(newLimit);
        ByteBuffer payloadFragment = payload.slice();
        payload.limit(limit);
        fragment.setPayload(payloadFragment);
        payload.position(newLimit);
        if (LOG.isDebugEnabled())
            LOG.debug("Fragmented {}->{}", frame, fragment);

        forwardFrame(fragment, callback, current.batch);
        return false;
    }
}
