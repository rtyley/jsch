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

public class Channel implements Runnable{
  static int index=0; 
  static java.util.Vector pool=new java.util.Vector();
  static Channel getChannel(String type){
    if(type.equals("session")){
      return new ChannelSession();
    }
    if(type.equals("shell")){
      return new ChannelShell();
    }
    if(type.equals("exec")){
      return new ChannelExec();
    }
    if(type.equals("x11")){
      return new ChannelX11();
    }
    if(type.equals("direct-tcpip")){
      return new ChannelDirectTCPIP();
    }
    if(type.equals("forwarded-tcpip")){
      return new ChannelForwardedTCPIP();
    }
    if(type.equals("sftp")){
      return new ChannelSftp();
    }
    return null;
  }
  static Channel getChannel(int id){
    for(int i=0; i<pool.size(); i++){
      Channel c=(Channel)(pool.elementAt(i));
      if(c.id==id) return c;
    }
    return null;
  }

  static void del(Channel c){
    pool.removeElement(c);
  }

  int id;
  int recipient=-1;
  byte[] type="foo".getBytes();
  int lwsize_max=0x100000;
  int lwsize=lwsize_max;  // local initial window size
  int lmpsize=0x4000;     // local maximum packet size

  int rwsize=0;         // remote initial window size
  int rmpsize=0;        // remote maximum packet size


  IO io=null;    
  Runnable thread=null;

  boolean eof=false;

  Session session;

  Channel(){
    id=index++;
    pool.addElement(this);
  }
  void setRecipient(int foo){
    this.recipient=foo;
  }
  int getRecipient(){
    return recipient;
  }
  void setEOF(){eof=true;}

  void init(){
  }

  public void connect(){
    try{
      Buffer buf=new Buffer(100);
      Packet packet=new Packet(buf);
      // send
      // byte   SSH_MSG_CHANNEL_OPEN(90)
      // string channel type         //
     // uint32 sender channel       // 0
      // uint32 initial window size  // 0x100000(65536)
      // uint32 maxmum packet size   // 0x4000(16384)
      packet.reset();
      buf.putByte((byte)90);
      buf.putString(this.type);
      buf.putInt(this.id);
      buf.putInt(this.lwsize);
      buf.putInt(this.lmpsize);
      packet.pack();
      session.write(packet);

      try{
        while(this.getRecipient()==-1){
          Thread.sleep(500);
        }
      }
      catch(Exception ee){
      }
      start();
    }
    catch(Exception e){
    }
  }

  public void setXForwarding(boolean foo){
  }

  void start(){ }

  void getData(Buffer buf){
    setRecipient(buf.getInt());
    setRemoteWindowSize(buf.getInt());
    setRemotePacketSize(buf.getInt());
  }

  public void setInputStream(java.io.InputStream in){
    io.setInputStream(in);
  }
  public void setOutputStream(java.io.OutputStream out){
    io.setOutputStream(out);
  }

  void setLocalWindowSize(int foo){ this.lwsize=foo; }
  void setLocalPacketSize(int foo){ this.lmpsize=foo; }
  void setRemoteWindowSize(int foo){ this.rwsize=foo; }
  void setRemotePacketSize(int foo){ this.rmpsize=foo; }

  public void run(){
  }

  void write(byte[] foo){
    write(foo, 0, foo.length);
  }
  void write(byte[] foo, int s, int l){
    if(eof)return;
    io.put(foo, s, l);
  }

  void close(){
    thread=null;
    try{
      if(io!=null){
        io.in.close();
        io.out.close();
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
    io=null;
  }

//  public String toString(){
//      return "Channel: type="+new String(type)+",id="+id+",recipient="+recipient+",window_size="+window_size+",packet_size="+packet_size;
//  }

/*
  class OutputThread extends Thread{
    Channel c;
    OutputThread(Channel c){ this.c=c;}
    public void run(){c.output_thread();}
  }
*/

  void setSession(Session session){
    this.session=session;
  }
}
