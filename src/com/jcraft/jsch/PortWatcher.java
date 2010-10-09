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
