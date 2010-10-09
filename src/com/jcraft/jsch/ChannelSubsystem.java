/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2002,2003 ymnk, JCraft,Inc. All rights reserved.
Copyright (c) 2003 Erwin Bolwidt, Amsterdam, The Netherlands.

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

public class ChannelSubsystem extends ChannelSession {
  private String subsystem;
  /*
  ChannelSubsystem(String subsystem){
    super();
    this.subsystem=subsystem;
  }
  */
  public void start(){
    try{
      RequestSubsystem request=new RequestSubsystem(subsystem);
      request.request(session, this);
    }
    catch (Exception e){
      e.printStackTrace();
    }
    (new Thread(this)).start();
  }
  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  public void run(){
    //System.err.println("# ChannelSubsystem.run starting");
    thread=this;
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);
    int i=0;
    try{
      while(thread!=null && io.in!=null){
	i=io.in.read(buf.buffer, 14, buf.buffer.length - 14);
	if(i==0)
	  continue;
	if(i==-1)
	  break;
	if(close)
	  break;
	packet.reset();
	buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
	buf.putInt(recipient);
	buf.putInt(i);
	buf.skip(i);
	session.write(packet, this, i);
      }
    }
    catch (Exception e){
      System.out.println("# ChannelSubsystem.run");
      e.printStackTrace();
    }
    //System.err.println("# ChannelSubsystem.run stopping");
    thread=null;
  }
  public void setSubsystem(String subsystem){
    this.subsystem=subsystem;
  }
  public String getSubsystem(){
    return subsystem;
  }
}
