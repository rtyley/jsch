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

import java.net.*;
class ChannelSession extends Channel{
  ChannelSession(){
    super();
    type="session".getBytes();
    io=new IO();
  }

  /*
  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  */

  public void run(){
    thread=this;
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);
    int i=0;
    try{
      while(thread!=null && io.in!=null){
        i=io.in.read(buf.buffer, 14, buf.buffer.length-14);
	if(i==0)continue;
	if(i==-1)break;
        packet.reset();
        buf.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
        buf.putInt(recipient);
        buf.putInt(i);
        buf.skip(i);
	session.write(packet, this, i);
      }
    }
    catch(Exception e){
	System.out.println(e);
    }
    thread=null;
  }

//  public String toString(){
//      return "Channel: type="+new String(type)+",id="+id+",recipient="+recipient+",window_size="+window_size+",packet_size="+packet_size;
//  }
}
