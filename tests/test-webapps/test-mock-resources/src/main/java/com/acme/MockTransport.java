//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package com.acme;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

/**
 * MockTransport
 */
public class MockTransport extends Transport
{
    /**
     *
     */
    public MockTransport(Session session, URLName urlname)
    {
        super(session, urlname);
    }

    /**
     * @see javax.mail.Transport#sendMessage(javax.mail.Message, javax.mail.Address[])
     */
    @Override
    public void sendMessage(Message arg0, Address[] arg1) throws MessagingException
    {
        System.err.println("Sending message");
    }
}
