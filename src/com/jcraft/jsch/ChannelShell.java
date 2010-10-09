/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002,2003,2004,2005,2006 ymnk, JCraft,Inc. All rights reserved.

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

public class ChannelShell extends ChannelSession{

  private boolean pty=true;

  private String ttype="vt100";
  private int tcol=80;
  private int trow=24;
  private int twp=640;
  private int thp=480;
  private byte[] terminal_mode=null;

  public void setPty(boolean enable){ 
    pty=enable; 
  }
  public void setTerminalMode(byte[] terminal_mode){
    this.terminal_mode=terminal_mode;
  }
  public void start() throws JSchException{
    try{
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

      request=new RequestShell();
      request.request(session, this);
    }
    catch(Exception e){
      if(e instanceof JSchException) throw (JSchException)e;
      if(e instanceof Throwable)
        throw new JSchException("ChannelShell", (Throwable)e);
      throw new JSchException("ChannelShell");
    }

    if(io.in!=null){
      thread=new Thread(this);
      thread.setName("Shell for "+session.host);
      if(session.daemon_thread){
        thread.setDaemon(session.daemon_thread);
      }
      thread.start();
    }
  }
  //public void finalize() throws Throwable{ super.finalize(); }
  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  public void setPtySize(int col, int row, int wp, int hp){
    //if(thread==null) return;
    try{
      RequestWindowChange request=new RequestWindowChange();
      request.setSize(col, row, wp, hp);
      request.request(session, this);
    }
    catch(Exception e){
      //System.err.println("ChannelShell.setPtySize: "+e);
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
}
