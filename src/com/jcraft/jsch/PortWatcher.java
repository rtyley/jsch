/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2002,2003,2004 ymnk, JCraft,Inc. All rights reserved.

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

class PortWatcher implements Runnable{
  private static java.util.Vector pool=new java.util.Vector();

  Session session;
  int lport;
  int rport;
  String host;
  String boundaddress;
  ServerSocket ss;
  Runnable thread;

  static String[] getPortForwarding(Session session){
    java.util.Vector foo=new java.util.Vector();
    for(int i=0; i<pool.size(); i++){
      PortWatcher p=(PortWatcher)(pool.elementAt(i));
      if(p.session==session){
	foo.addElement(p.lport+":"+p.host+":"+p.rport);
      }
    }
    String[] bar=new String[foo.size()];
    for(int i=0; i<foo.size(); i++){
      bar[i]=(String)(foo.elementAt(i));
    }
    return bar;
  }
  static PortWatcher getPort(Session session, int lport){
    for(int i=0; i<pool.size(); i++){
      PortWatcher p=(PortWatcher)(pool.elementAt(i));
      if(p.session==session && p.lport==lport) return p;
    }
    return null;
  }
  static PortWatcher addPort(Session session, String address, int lport, String host, int rport) throws JSchException{
    if(getPort(session, lport)!=null){
      throw new JSchException("PortForwardingL: local port "+lport+" is already registered.");
    }
    PortWatcher pw=new PortWatcher(session, address, lport, host, rport);
    pool.addElement(pw);
    return pw;
  }
  static void delPort(Session session, int lport) throws JSchException{
    PortWatcher pw=getPort(session, lport);
    if(pw==null){
      throw new JSchException("PortForwardingL: local port "+lport+" is not registered.");
    }
    pw.delete();
    pool.removeElement(pw);
  }
  static void delPort(Session session){
    for(int i=0; i<pool.size(); i++){
      PortWatcher p=(PortWatcher)(pool.elementAt(i));
      if(p.session==session) {
	p.delete();
	pool.removeElement(p);
	i--;
      }
    }
  }
  PortWatcher(Session session, 
	      String boundaddress, int lport, 
	      String host, int rport) throws JSchException{
    this.session=session;
    this.boundaddress=boundaddress;
    this.lport=lport;
    this.host=host;
    this.rport=rport;
    try{
//    ss=new ServerSocket(port);
      ss=new ServerSocket(lport, 0, 
			  InetAddress.getByName(this.boundaddress));
    }
    catch(Exception e){ 
      System.out.println(e);
      throw new JSchException("PortForwardingL: local port "+lport+" cannot be bound.");
    }
  }

  public void run(){
    Buffer buf=new Buffer(300); // ??
    Packet packet=new Packet(buf);
    thread=this;
    try{
      while(thread!=null){
        Socket socket=ss.accept();
	socket.setTcpNoDelay(true);
        InputStream in=socket.getInputStream();
        OutputStream out=socket.getOutputStream();
        ChannelDirectTCPIP channel=new ChannelDirectTCPIP();
        channel.init();
        channel.setInputStream(in);
        channel.setOutputStream(out);
	session.addChannel(channel);
	((ChannelDirectTCPIP)channel).setHost(host);
	((ChannelDirectTCPIP)channel).setPort(rport);
	((ChannelDirectTCPIP)channel).setOrgIPAddress(socket.getInetAddress().getHostAddress());
	((ChannelDirectTCPIP)channel).setOrgPort(socket.getPort());
        channel.connect();
	if(channel.exitstatus!=-1){
	}
      }
    }
    catch(Exception e){
      //System.out.println("! "+e);
    }
  }

  void delete(){
    thread=null;
    try{ ss.close(); }
    catch(Exception e){
    }
  }
}
