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

class UserAuthPassword extends UserAuth{
  UserInfo userinfo;
  UserAuthPassword(UserInfo userinfo){
   this.userinfo=userinfo;
  }

  public boolean start(Session session) throws Exception{
    super.start(session);

    Packet packet=session.packet;
    Buffer buf=session.buf;

    do{
    // send
    // byte      SSH_MSG_USERAUTH_REQUEST(50)
    // string    user name
    // string    service name ("ssh-connection")
    // string    "password"
    // boolen    FALSE
    // string    plaintext password (ISO-10646 UTF-8)
    packet.reset();
    buf.putByte((byte)Const.SSH_MSG_USERAUTH_REQUEST);
    buf.putString(userinfo.getName().getBytes());
    buf.putString("ssh-connection".getBytes());
    buf.putString("password".getBytes());
    buf.putByte((byte)0);
    buf.putString(userinfo.getPassword().getBytes());
    session.write(packet);

    // receive
    // byte      SSH_MSG_USERAUTH_SUCCESS(52)
    // string    service name
    buf=session.read(buf);
    //System.out.println("read: 52 ? "+    buf.buffer[5]);
    if(buf.buffer[5]==Const.SSH_MSG_USERAUTH_SUCCESS){
      return true;
    }
    if(buf.buffer[5]==Const.SSH_MSG_USERAUTH_FAILURE){
      buf.getInt(); buf.getByte(); buf.getByte(); 
      byte[] foo=buf.getString();
      int partial_success=buf.getByte();
      System.out.println(new String(foo)+
			 " partial_success:"+(partial_success!=0));
    }
    else{
      System.out.println("USERAUTH fail ("+buf.buffer[5]+")");
      System.exit(-1);
    }
    }
    while(userinfo.retry());
    return false;

  }
}
