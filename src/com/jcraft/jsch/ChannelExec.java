/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002,2003,2004,2005 ymnk, JCraft,Inc. All rights reserved.

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

public class ChannelExec extends ChannelSession{
  boolean xforwading=false;
  boolean pty=false;
  String command="";
  /*
  ChannelExec(){
    super();
    type="session".getBytes();
    io=new IO();
  }
  */
  public void setXForwarding(boolean foo){ xforwading=foo; }
  public void setPty(boolean foo){ pty=foo; }
  public void start() throws JSchException{
    try{
      Request request;

      if(xforwading){
        request=new RequestX11();
        request.request(session, this);
      }

      if(pty){
	request=new RequestPtyReq();
	request.request(session, this);
      }

      request=new RequestExec(command);
      request.request(session, this);
    }
    catch(Exception e){
      throw new JSchException("ChannelExec");
    }
    thread=new Thread(this);
    thread.setName("Exec thread "+session.getHost());
    thread.start();
  }
  public void setCommand(String foo){ command=foo;}
  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  //public void finalize() throws java.lang.Throwable{ super.finalize(); }
  public void setErrStream(java.io.OutputStream out){
    setExtOutputStream(out);
  }
  public java.io.InputStream getErrStream() throws java.io.IOException {
    return getExtInputStream();
  }
}
