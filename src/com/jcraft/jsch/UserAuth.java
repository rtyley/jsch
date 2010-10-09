/* JSch
 * Copyright (C) 2002 ymnk, JCraft,Inc.
 *  
 * Written by: 2002 ymnk<ymnk@jcaft.com>
 *   
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jsch;

abstract class UserAuth{
  boolean start(Session session) throws Exception{
    Packet packet=session.packet;
    Buffer buf=session.buf;
    // send
    // byte      SSH_MSG_SERVICE_REQUEST(5)
    // string    service name "ssh-userauth"
    packet.reset();
    buf.putByte((byte)Const.SSH_MSG_SERVICE_REQUEST);
    buf.putString("ssh-userauth".getBytes());
    packet.pack();
    session.write(packet);

    // receive
    // byte      SSH_MSG_SERVICE_ACCEPT(6)
    // string    service name
    buf=session.read(buf);
    //System.out.println("read: 6 ? "+buf.buffer[5]);
    return buf.buffer[5]==6;
  }
}
