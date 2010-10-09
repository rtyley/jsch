/* -*-mode:java; c-basic-offset:2; -*- */
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

public class RequestSftp implements Request{
  public void request(Session session, Channel channel) throws Exception{
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);

    packet.reset();
    buf.putByte((byte)98);
    buf.putInt(channel.getRecipient());
    buf.putString("subsystem".getBytes());
//    buf.putByte((byte)1);
    buf.putByte((byte)0);
    buf.putString("sftp".getBytes());
    session.write(packet);

    /*
    buf=session.read(buf);
    buf.getInt();
    buf.getByte();
    buf.getByte();
    int foo=buf.getInt();  // recipient_channel
    */

    /*
    Channel channel1=Channel.getChannel("sftp");
    session.addChannel(channel1);
    channel1.init();
    channel1.setRecipient(foo);
    channel1.lwsize=channel.lwsize;
    channel1.lmpsize=channel.lmpsize;
    channel1.rwsize=channel.rwsize;
    channel1.rmpsize=channel.rmpsize;
    Channel.del(channel);
    channel1.id=channel.id;
    session.channel=channel1; 
    */

  }
}
