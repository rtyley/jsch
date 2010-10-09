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

class RequestX11 implements Request{
  public void setCookie(String cookie){
    ChannelX11.cookie=cookie.getBytes();
  }
  public void request(Session session, Channel channel) throws Exception{
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);

    // byte      SSH_MSG_CHANNEL_REQUEST(98)
    // uint32 recipient channel
    // string request type        // "x11-req"
    // boolean want reply         // 0
    // boolean   single connection
    // string    x11 authentication protocol // "MIT-MAGIC-COOKIE-1".
    // string    x11 authentication cookie
    // uint32    x11 screen number
    packet.reset();
    buf.putByte((byte) Session.SSH_MSG_CHANNEL_REQUEST);
    buf.putInt(channel.getRecipient());
    buf.putString("x11-req".getBytes());
    buf.putByte((byte)0);
    buf.putByte((byte)0);
    buf.putString("MIT-MAGIC-COOKIE-1".getBytes());
    buf.putString(ChannelX11.getFakedCookie(session));
    buf.putInt(0);
    session.write(packet);
  }
}
