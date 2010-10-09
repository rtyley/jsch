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
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
VISIGOTH SOFTWARE SOCIETY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

import java.io.*;
import java.util.Vector;

class UserAuthPublicKey extends UserAuth{
  UserInfo userinfo;
  UserAuthPublicKey(UserInfo userinfo){
   this.userinfo=userinfo;
  }

  public boolean start(Session session) throws Exception{
    super.start(session);

    Vector identities=JSch.identities;

    Packet packet=session.packet;
    Buffer buf=session.buf;

    String passphrase=null;
    final String username=session.username;

    for(int i=0; i<identities.size(); i++){
    Identity identity=(Identity)(JSch.identities.elementAt(i));
    byte[] pubkeyblob=identity.getPublicKeyBlob();
    if(pubkeyblob!=null){
      // send
      // byte      SSH_MSG_USERAUTH_REQUEST(50)
      // string    user name
      // string    service name ("ssh-connection")
      // string    "publickey"
      // boolen    FALSE
      // string    plaintext password (ISO-10646 UTF-8)
      packet.reset();
      buf.putByte((byte)Session.SSH_MSG_USERAUTH_REQUEST);
      buf.putString(username.getBytes());
      buf.putString("ssh-connection".getBytes());
      buf.putString("publickey".getBytes());
      buf.putByte((byte)0);
      buf.putString(identity.getAlgName().getBytes());
      buf.putString(pubkeyblob);
      session.write(packet);

      // receive
      // byte      SSH_MSG_USERAUTH_PK_OK(52)
      // string    service name
      buf=session.read(buf);
      //System.out.println("read: 60 ? "+    buf.buffer[5]);
      if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_PK_OK){
      }
      else if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_FAILURE){
//	System.out.println("USERAUTH publickey "+session.getIdentity()+
//			   " is not acceptable.");
	continue;
      }
      else{
	System.out.println("USERAUTH fail ("+buf.buffer[5]+")");
	//throw new JSchException("USERAUTH fail ("+buf.buffer[5]+")");
	continue;
      }
    }

    int count=5;
    while(true){
      if((identity.isEncrypted() && passphrase==null)){
	if(userinfo==null) throw new JSchException("USERAUTH fail");
	if(identity.isEncrypted() &&
	   !userinfo.promptPassphrase("Passphrase for "+identity.identity)){
	  //throw new JSchException("USERAUTH cancel");
	  break;
	}
	passphrase=userinfo.getPassphrase();
      }

      if(!identity.isEncrypted() || passphrase!=null){
	if(identity.setPassphrase(passphrase))
          break;
      }
      passphrase=null;
      count--;
      if(count==0)break;
    }

    if(identity.isEncrypted()) continue;
    if(pubkeyblob==null)  pubkeyblob=identity.getPublicKeyBlob();
    if(pubkeyblob==null)  continue;

    // send
    // byte      SSH_MSG_USERAUTH_REQUEST(50)
    // string    user name
    // string    service name ("ssh-connection")
    // string    "publickey"
    // boolen    TRUE
    // string    plaintext password (ISO-10646 UTF-8)
    packet.reset();
    buf.putByte((byte)Session.SSH_MSG_USERAUTH_REQUEST);
    buf.putString(username.getBytes());
    buf.putString("ssh-connection".getBytes());
    buf.putString("publickey".getBytes());
    buf.putByte((byte)1);
    buf.putString(identity.getAlgName().getBytes());
    buf.putString(pubkeyblob);

    byte[] tmp=new byte[buf.index-5];
    System.arraycopy(buf.buffer, 5, tmp, 0, tmp.length);
    buf.putString(identity.getSignature(session, tmp));
    session.write(packet);

    // receive
    // byte      SSH_MSG_USERAUTH_SUCCESS(52)
    // string    service name
    buf=session.read(buf);
    //System.out.println("read: 52 ? "+    buf.buffer[5]);
    if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_SUCCESS){
      return true;
    }
    if(buf.buffer[5]==Session.SSH_MSG_USERAUTH_FAILURE){
      buf.getInt(); buf.getByte(); buf.getByte(); 
      byte[] foo=buf.getString();
      int partial_success=buf.getByte();
      System.out.println(new String(foo)+
			 " partial_success:"+(partial_success!=0));
    }
    else{
      System.out.println("USERAUTH fail ("+buf.buffer[5]+")");
      //throw new JSchException("USERAUTH fail ("+buf.buffer[5]+")");
    }
    }

    return false;
  }

}
