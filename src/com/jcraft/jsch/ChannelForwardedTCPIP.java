/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2002,2003 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
      while(thread!=null && io.in!=null){
        i=io.in.read(buf.buffer, 
		     14, 
		     buf.buffer.length-14
		     -16 -20 // padding and mac
		     );
	if(i<=0){
          break;
	}
        packet.reset();
	if(close)break;
        buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
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
