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

class ChannelSftp extends ChannelSession{
  private boolean interactive=true;

  ChannelSftp(){
    super();
    type="session".getBytes();
    io=new IO();
  }

  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  public void start(){
    try{
      Request request;
      request=new RequestSftp();
      request.request(session, this);
    }
    catch(Exception e){
    }
    (new Thread(this)).start(); 
  }
  public void run(){
    thread=this;
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);
    int i=0;
    int j=0;
    int length;
    int type;
    byte[] str;

    try{
      PrintStream pr=((io.out instanceof PrintStream)?
                      (PrintStream)io.out:
		      new PrintStream(io.out));
      InputStream in=io.in;

      PipedOutputStream pos=new PipedOutputStream();
      io.setOutputStream(pos);
      PipedInputStream pis=new PipedInputStream(pos);
      io.setInputStream(pis);

      // send FXP_INIT
      packet.reset();
      buf.putByte((byte)94);
      buf.putInt(recipient);
      buf.putInt(9);
      buf.putInt(5);
      buf.putByte((byte)1);         // FXP_INIT
      buf.putInt(3);                // version 3
      packet.pack();
      session.write(packet);


      // receive FXP_VERSION
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();           // 2 -> FXP_VERSION


      // send FXP_REALPATH
      packet.reset();
      buf.putByte((byte)94);
      buf.putInt(recipient);
      buf.putInt(14);
      buf.putInt(10);
      buf.putByte((byte)16);         // FXP_REALPATH
      buf.putInt(1);                 // id(1)
      buf.putString(".".getBytes()); // path
      packet.pack();
      session.write(packet);

      // receive FXP_NAME
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();          // 104 -> FXP_NAME
System.out.println("type="+type+" FXP_NAME");
      buf.getInt();                //
      i=buf.getInt();              // count
      str=buf.getString();         // filename
System.out.println("filename: "+new String(str));
      str=buf.getString();         // logname
System.out.println("longname: "+new String(str));
      j=buf.getInt();              // attrs


      /*
      cd /tmp
      c->s REALPATH
      s->c NAME
      c->s STAT
      s->c ATTR 
      */
      // send FXP_REALPATH
      packet.reset();
      buf.putByte((byte)94);
      buf.putInt(recipient);
      buf.putInt(17);
      buf.putInt(13);
      buf.putByte((byte)16);         // FXP_REALPATH
      buf.putInt(2);                 // id(2)
      buf.putString("/tmp".getBytes()); // path
      packet.pack();
      session.write(packet);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();          // 104 -> FXP_NAME

      // send FXP_STAT
      packet.reset();
      buf.putByte((byte)94);
      buf.putInt(recipient);
      buf.putInt(17);
      buf.putInt(13);
      buf.putByte((byte)17);        // FXP_STAT
      buf.putInt(3);                 // id(3)
      buf.putString("/tmp".getBytes()); // path
      packet.pack();
      session.write(packet);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();          // 105 -> FXP_ATTR

      /*
      put foo
      c->s OPEN
      s->c HANDLE
      c->s WRITE
      s->c STATUS
      c->s CLOSE 
      s->c STATUS 
       */

      while(thread!=null && interactive){

        i=in.read(buf.buffer, 0, buf.buffer.length);

        pr.println(new String(buf.buffer, 0, i));

      }
    }
    catch(Exception e){
      System.out.println(e);
    }
    thread=null;
  }

  void write(byte[] foo){
    write(foo, 0, foo.length);
  }
  void write(byte[] foo, int s, int l){
    if(eof)return;
    io.put(foo, s, l);
  }
}
