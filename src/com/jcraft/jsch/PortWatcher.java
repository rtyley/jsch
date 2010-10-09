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

class PortWatcher implements Runnable{
  Session session;
  int lport;
  int rport;
  String host;
  ServerSocket ss;
  Runnable thread;
  PortWatcher(Session session, int lport, String host, int rport){
    this.session=session;
    this.lport=lport;
    this.host=host;
    this.rport=rport;
    try{
//    ss=new ServerSocket(port);
      ss=new ServerSocket(lport, 0, InetAddress.getByName("127.0.0.1"));
    }
    catch(Exception e){ 
	System.out.println(e);
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
      }
    }
    catch(Exception e){
      System.out.println("! "+e);
    }
  }
}
