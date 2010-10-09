/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002-2008 ymnk, JCraft,Inc. All rights reserved.

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

import java.util.*;

class ChannelSession extends Channel{
  private static byte[] _session="session".getBytes();

  protected boolean agent_forwarding=false;
  protected boolean xforwading=false;
  protected Hashtable env=null;

  protected boolean pty=false;

  protected String ttype="vt100";
  protected int tcol=80;
  protected int trow=24;
  protected int twp=640;
  protected int thp=480;
  protected byte[] terminal_mode=null;

  ChannelSession(){
    super();
    type=_session;
    io=new IO();
  }

  public void setAgentForwarding(boolean enable){ 
    agent_forwarding=enable;
  }
  public void setXForwarding(boolean enable){
    xforwading=enable; 
  }
  public void setEnv(Hashtable env){ 
    this.env=env; 
  }

  public void setPty(boolean enable){ 
    pty=enable; 
  }

  public void setTerminalMode(byte[] terminal_mode){
    this.terminal_mode=terminal_mode;
  }
  public void setPtySize(int col, int row, int wp, int hp){
    if(!pty){
      return;
    }
    try{
      RequestWindowChange request=new RequestWindowChange();
      request.setSize(col, row, wp, hp);
      request.request(session, this);
    }
    catch(Exception e){
      //System.err.println("ChannelSessio.setPtySize: "+e);
    }
  }

  public void setPtyType(String ttype){
    setPtyType(ttype, 80, 24, 640, 480);
  }

  public void setPtyType(String ttype, int col, int row, int wp, int hp){
    this.ttype=ttype;
    this.tcol=col;
    this.trow=row;
    this.twp=wp;
    this.thp=hp;
  }

  protected void sendRequests() throws Exception{
    Request request;
    if(agent_forwarding){
      request=new RequestAgentForwarding();
      request.request(session, this);
    }

    if(xforwading){
      request=new RequestX11();
      request.request(session, this);
    }

    if(pty){
      request=new RequestPtyReq();
      ((RequestPtyReq)request).setTType(ttype);
      ((RequestPtyReq)request).setTSize(tcol, trow, twp, thp);
      if(terminal_mode!=null){
        ((RequestPtyReq)request).setTerminalMode(terminal_mode);
      }
      request.request(session, this);
    }

    if(env!=null){
      for(Enumeration _env=env.keys() ; _env.hasMoreElements() ;) {
        String name=(String)(_env.nextElement());
        String value=(String)(env.get(name));
        request=new RequestEnv();
        ((RequestEnv)request).setEnv(name, value);
        request.request(session, this);
      }
    }
  }

  public void run(){
    //System.err.println(this+":run >");
/*
    if(thread!=null){ return; }
    thread=Thread.currentThread();
*/

    Buffer buf=new Buffer(rmpsize);
    Packet packet=new Packet(buf);
    int i=-1;
    try{
      while(isConnected() &&
	    thread!=null && 
            io!=null && 
            io.in!=null){
        i=io.in.read(buf.buffer, 
                     14,    
                     buf.buffer.length-14
                     -32 -20 // padding and mac
		     );
	if(i==0)continue;
	if(i==-1){
	  eof();
	  break;
	}
	if(close)break;
        //System.out.println("write: "+i);
        packet.reset();
        buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
        buf.putInt(recipient);
        buf.putInt(i);
        buf.skip(i);
	session.write(packet, this, i);
      }
    }
    catch(Exception e){
      //System.err.println("# ChannelExec.run");
      //e.printStackTrace();
    }
    if(thread!=null){
      synchronized(thread){ thread.notifyAll(); }
    }
    thread=null;
    //System.err.println(this+":run <");
  }
}
