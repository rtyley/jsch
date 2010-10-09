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

  static java.util.Vector pool=new java.util.Vector();

  static private final int LOCAL_WINDOW_SIZE_MAX=0x20000;
  static private final int LOCAL_MAXIMUM_PACKET_SIZE=0x4000;

  String host;
  int lport;
  int rport;

  ChannelForwardedTCPIP(){
    super();
    setLocalWindowSizeMax(LOCAL_WINDOW_SIZE_MAX);
    setLocalWindowSize(LOCAL_WINDOW_SIZE_MAX);
    setLocalPacketSize(LOCAL_MAXIMUM_PACKET_SIZE);
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
      while(thread!=null && io!=null && io.in!=null){
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
      //System.out.println(e);
    }
    //thread=null;
    //eof();
    disconnect();
  }
  public void disconnect(){
    close();
    thread=null;
    try{
      if(io!=null){
      if(io.in!=null)io.in.close();
      if(io.out!=null)io.out.close();
      }
    }
    catch(Exception e){
      //e.printStackTrace();
    }
    io=null;
    Channel.del(this);
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

    synchronized(pool){
      for(int i=0; i<pool.size(); i++){
        Object[] foo=(Object[])(pool.elementAt(i));
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

  static Object[] getPort(Session session, int rport){
    synchronized(pool){
      Object[] foo=null;
      for(int i=0; i<pool.size(); i++){
        Object[] bar=(Object[])(pool.elementAt(i));
        if(bar[0]!=session) continue;
        if(((Integer)bar[1]).intValue()!=rport) continue;
	return bar;
      }
      return null;
    }
  }

  static String[] getPortForwarding(Session session){
    java.util.Vector foo=new java.util.Vector();
    synchronized(pool){
      for(int i=0; i<pool.size(); i++){
        Object[] bar=(Object[])(pool.elementAt(i));
        if(bar[0]!=session) continue;
	foo.addElement(bar[1]+":"+bar[2]+":"+bar[3]);
      }
    }
    String[] bar=new String[foo.size()];
    for(int i=0; i<foo.size(); i++){
      bar[i]=(String)(foo.elementAt(i));
    }
    return bar;
  }

  static void addPort(Session session, int port, String host, int lport) throws JSchException{
    synchronized(pool){
      if(getPort(session, port)!=null){
        throw new JSchException("PortForwardingR: remote port "+port+" is already registered.");
      }
      Object[] foo=new Object[4];
      foo[0]=session; foo[1]=new Integer(port);
      foo[2]=host; foo[3]=new Integer(lport);
      pool.addElement(foo);
    }
  }
  static void delPort(ChannelForwardedTCPIP c){
    delPort(c.session, c.rport);
  }
  static void delPort(Session session, int rport){
    synchronized(pool){
      Object[] foo=null;
      for(int i=0; i<pool.size(); i++){
        Object[] bar=(Object[])(pool.elementAt(i));
        if(bar[0]!=session) continue;
        if(((Integer)bar[1]).intValue()!=rport) continue;
        foo=bar;
        break;
      }
      if(foo==null)return;
      pool.removeElement(foo);	
    }

    Buffer buf=new Buffer(100); // ??
    Packet packet=new Packet(buf);

    try{
      // byte SSH_MSG_GLOBAL_REQUEST 80
      // string "cancel-tcpip-forward"
      // boolean want_reply
      // string  address_to_bind (e.g. "127.0.0.1")
      // uint32  port number to bind
      packet.reset();
      buf.putByte((byte) 80/*SSH_MSG_GLOBAL_REQUEST*/);
      buf.putString("cancel-tcpip-forward".getBytes());
      buf.putByte((byte)0);
      buf.putString("0.0.0.0".getBytes());
      buf.putInt(rport);
      session.write(packet);
    }
    catch(Exception e){
//    throw new JSchException(e.toString());
    }
  }
  static void delPort(Session session){
    for(int i=0; i<pool.size(); i++){
      Object[] bar=(Object[])(pool.elementAt(i));
      if(bar[0]==session) {
        pool.removeElement(bar);
	i--;
      }
    }
  }

}
