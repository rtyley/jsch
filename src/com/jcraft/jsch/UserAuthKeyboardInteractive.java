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

class UserAuthKeyboardInteractive extends UserAuth{
  UserInfo userinfo;
  UserAuthKeyboardInteractive(UserInfo userinfo){
   this.userinfo=userinfo;
  }

  public boolean start(Session session) throws Exception{
//System.out.println("UserAuthKeyboardInteractive: start");
    Packet packet=session.packet;
    Buffer buf=session.buf;
    final String username=session.username;
    String dest=username+"@"+session.host+((session.port==22) ? ""  : new Integer(session.port).toString());

    boolean cancel=false;

    byte[] _username=null;
    try{ _username=username.getBytes("UTF-8"); }
    catch(java.io.UnsupportedEncodingException e){
      _username=username.getBytes();
    }

    while(true){
      // send
      // byte      SSH_MSG_USERAUTH_REQUEST(50)
      // string    user name (ISO-10646 UTF-8, as defined in [RFC-2279])
      // string    service name (US-ASCII) "ssh-userauth" ? "ssh-connection"
      // string    "keyboard-interactive" (US-ASCII)
      // string    language tag (as defined in [RFC-3066])
      // string    submethods (ISO-10646 UTF-8)
      packet.reset();
      buf.putByte((byte)Session.SSH_MSG_USERAUTH_REQUEST);
      buf.putString(_username);
      buf.putString("ssh-connection".getBytes());
      //buf.putString("ssh-userauth".getBytes());
      buf.putString("keyboard-interactive".getBytes());
      buf.putString("".getBytes());
      buf.putString("".getBytes());
      session.write(packet);

      boolean firsttime=true;
      loop:
      while(true){
	// receive
	// byte      SSH_MSG_USERAUTH_SUCCESS(52)
	// string    service name
	try{  buf=session.read(buf); }
	catch(java.io.IOException e){
	  return false;
	}
//System.out.println("read: 52 ? "+    buf.buffer[5]);
	if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_SUCCESS){
	  return true;
	}
	if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_BANNER){
	  buf.getInt(); buf.getByte(); buf.getByte();
	  byte[] _message=buf.getString();
	  byte[] lang=buf.getString();
	  String message=null;
	  try{ message=new String(_message, "UTF-8"); }
	  catch(java.io.UnsupportedEncodingException e){
	    message=new String(_message);
	  }
	  userinfo.showMessage(message);
	  continue loop;
	}
	if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_FAILURE){
	  buf.getInt(); buf.getByte(); buf.getByte(); 
	  byte[] foo=buf.getString();
	  int partial_success=buf.getByte();
//	  System.out.println(new String(foo)+
//			     " partial_success:"+(partial_success!=0));
	  if(firsttime)
	    cancel=true;  // ??
	  break;
	}
	if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_INFO_REQUEST){
	  firsttime=false;
	  buf.getInt(); buf.getByte(); buf.getByte();
	  String name=new String(buf.getString());
	  String instruction=new String(buf.getString());
	  String languate_tag=new String(buf.getString());
	  int num=buf.getInt();
//System.out.println("name: "+name);
//System.out.println("instruction: "+instruction);
//System.out.println("lang: "+languate_tag);
//System.out.println("num: "+num);
	  String[] prompt=new String[num];
	  boolean[] echo=new boolean[num];
	  for(int i=0; i<num; i++){
	    prompt[i]=new String(buf.getString());
	    echo[i]=(buf.getByte()!=0);
//System.out.println("  "+prompt[i]+","+echo[i]);
	  }

	  UIKeyboardInteractive kbi=(UIKeyboardInteractive)userinfo;
	  String[] response=
	    kbi.promptKeyboardInteractive(dest,
					  name,
					  instruction,
					  languate_tag,
					  prompt,
					  echo);
	  // byte      SSH_MSG_USERAUTH_INFO_RESPONSE(61)
	  // int       num-responses
	  // string    response[1] (ISO-10646 UTF-8)
	  // ...
	  // string    response[num-responses] (ISO-10646 UTF-8)
//if(response!=null)
//System.out.println("response.length="+response.length);
//else
//System.out.println("response is null");
	  packet.reset();
	  buf.putByte((byte)Session.SSH_MSG_USERAUTH_INFO_RESPONSE);
	  if(response==null ||  // cancel
	     num!=response.length){
	    buf.putInt(0);
	    if(response==null)
	      cancel=true;
	  }
	  else{
	    buf.putInt(num);
	    for(int i=0; i<num; i++){
//System.out.println("response: |"+response[i]+"| <- replace here with **** if you need");
	      buf.putString(response[i].getBytes());
	    }
	  }
	  session.write(packet);
//System.out.println("continue loop");
	  continue loop;
	}
	throw new JSchException("USERAUTH fail ("+buf.buffer[5]+")");
      }
      if(cancel) break;
    }
    return false;
  }
}
