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
import java.io.*;

public class ChannelDirectTCPIP extends Channel{

  int lwsize_max=0x20000;
  int lwsize=lwsize_max;   // local initial window size
  int lmpsize=0x4000;      // local maximum packet size

  String host;
  int port;

  String originator_IP_address="127.0.0.1";
  int originator_port=0;

  ChannelDirectTCPIP(){
    super();
  }

  void init (){
    try{ 
      io=new IO();
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public void connect(){
    try{
      Buffer buf=new Buffer(100);
      Packet packet=new Packet(buf);
      // send
      // byte   SSH_MSG_CHANNEL_OPEN(90)
      // string channel type         //
     // uint32 sender channel       // 0
      // uint32 initial window size  // 0x100000(65536)
      // uint32 maxmum packet size   // 0x4000(16384)

      packet.reset();
      buf.putByte((byte)90);
      buf.putString("direct-tcpip".getBytes());
      buf.putInt(id);
      buf.putInt(lwsize);
      buf.putInt(lmpsize);
      buf.putString(host.getBytes());
      buf.putInt(port);
      buf.putString(originator_IP_address.getBytes());
      buf.putInt(originator_port);
      session.write(packet);
      try{
        while(this.getRecipient()==-1){
          Thread.sleep(500);
        }
      }
      catch(Exception ee){
      }
      (new Thread(this)).start();
    }
    catch(Exception e){
    }
  }

  public void run(){
    thread=this;
    Buffer buf=new Buffer(rmpsize);
    Packet packet=new Packet(buf);
    int i=0;
    try{
      while(thread!=null){
        i=io.in.read(buf.buffer, 
		     14, 
		     buf.buffer.length-14
		     -16 -20 // padding and mac
		     );
	if(i<=0){
          break;
	}
        packet.reset();
        buf.putByte((byte)Const.SSH_MSG_CHANNEL_DATA);
        buf.putInt(recipient);
        buf.putInt(i);
        buf.skip(i);
	session.write(packet);
      }
    }
    catch(Exception e){
    }
    thread=null;
/*
    try{
      packet.reset();
      buf.putByte((byte)Const.SSH_MSG_CHANNEL_EOF);
      buf.putInt(recipient);
      session.write(packet);
    }
    catch(Exception e){
    }
*/
//    close();
  }

  public void setInputStream(InputStream in){
    io.setInputStream(in);
  }
  public void setOutputStream(OutputStream out){
    io.setOutputStream(out);
  }

  public void setHost(String host){this.host=host;}
  public void setPort(int port){this.port=port;}
  public void setOrgIPAddress(String foo){this.originator_IP_address=foo;}
  public void setOrgPort(int foo){this.originator_port=foo;}
}
