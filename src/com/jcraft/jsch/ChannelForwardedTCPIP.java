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

class ChannelForwardedTCPIP extends Channel{

  static java.util.Vector port_pool=new java.util.Vector();

  int lwsize_max=0x20000;
  int lwsize=lwsize_max;   // local initial window size
  int lmpsize=0x4000;      // local maximum packet size

  String host;
  int lport;
  int rport;

  ChannelForwardedTCPIP(){
    super();
  }

  void init (){
    try{ 
      Socket socket = new Socket(host, lport);
      socket.setTcpNoDelay(true);
      io=new IO();
      io.setInputStream(socket.getInputStream());
      io.setOutputStream(socket.getOutputStream());
    }
    catch(Exception e){
      System.out.println(e);
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
        buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
        buf.putInt(recipient);
        buf.putInt(i);
        buf.skip(i);
	session.write(packet);
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
    thread=null;

    try{
      packet.reset();
      buf.putByte((byte) Session.SSH_MSG_CHANNEL_EOF);
      buf.putInt(recipient);
      session.write(packet);
    }
    catch(Exception e){
//      System.out.println(e);
    }

//    close();
  }
  void getData(Buffer buf){
    setRecipient(buf.getInt());
    setRemoteWindowSize(buf.getInt());
    setRemotePacketSize(buf.getInt());
    byte[] addr=buf.getString();
    int port=buf.getInt();
    byte[] orgaddr=buf.getString();
    int orgport=buf.getInt();

    /*
    System.out.println("addr: "+new String(addr));
    System.out.println("port: "+port);
    System.out.println("orgaddr: "+new String(orgaddr));
    System.out.println("orgport: "+orgport);
    */

    synchronized(port_pool){
      for(int i=0; i<port_pool.size(); i++){
        Object[] foo=(Object[])(port_pool.elementAt(i));
        if(foo[0]!=session) continue;
        if(((Integer)foo[1]).intValue()!=port) continue;
        this.rport=port;
        this.host=(String)foo[2];
        this.lport=((Integer)foo[3]).intValue();
        break;
      }
      if(host==null){
	  System.out.println("??");
      }
    }
  }
  static void addPort(Session session, int port, String host, int lport){
    synchronized(port_pool){
      Object[] foo=new Object[4];
      foo[0]=session; foo[1]=new Integer(port);
      foo[2]=host; foo[3]=new Integer(lport);
      port_pool.addElement(foo);
    }
  }
  static void delPort(ChannelForwardedTCPIP c){
    synchronized(port_pool){
      Object[] foo=null;
      for(int i=0; i<port_pool.size(); i++){
        Object[] bar=(Object[])(port_pool.elementAt(i));
        if(bar[0]!=c.session) continue;
        if(((Integer)bar[1]).intValue()!=c.rport) continue;
        foo=bar;
        break;
      }
      if(foo!=null){
        port_pool.removeElement(foo);	
      }
    }
  }
}
