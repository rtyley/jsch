/* -*-mode:java; c-basic-offset:2; -*- */
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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


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
  boolean close=false;

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

  public void setInputStream(InputStream in){
    io.setInputStream(in);
  }
  public void setOutputStream(OutputStream out){
    io.setOutputStream(out);
  }

  public InputStream getInputStream() throws IOException {
    PipedInputStream in=new PipedInputStream();
    io.setOutputStream(new PassiveOutputStream(in));
    return in;
  }
  public OutputStream getOutputStream() throws IOException {
    PipedOutputStream out=new PipedOutputStream();
    io.setInputStream(new PassiveInputStream(out));
    return out;
  }

  void setLocalWindowSize(int foo){ this.lwsize=foo; }
  void setLocalPacketSize(int foo){ this.lmpsize=foo; }
  void setRemoteWindowSize(int foo){ this.rwsize=foo; }
  void addRemoteWindowSize(int foo){ this.rwsize+=foo; }
  void setRemotePacketSize(int foo){ this.rmpsize=foo; }

  public void run(){
  }

  void write(byte[] foo) throws IOException {
    write(foo, 0, foo.length);
  }
  void write(byte[] foo, int s, int l) throws IOException {
    if(eof)return;
    if(io.out!=null)
      io.put(foo, s, l);
  }

  void eof(){
    if(eof)return;
    close=eof;
    try{
      Buffer buf=new Buffer(100);
      Packet packet=new Packet(buf);
      packet.reset();
      buf.putByte((byte)Session.SSH_MSG_CHANNEL_EOF);
      buf.putInt(getRecipient());
      session.write(packet);
    }
    catch(Exception e){
      //System.out.println("Channel.eof");
      //e.printStackTrace();
    }
  }

  void close(){
    if(close)return;
    close=true;
    try{
      Buffer buf=new Buffer(100);
      Packet packet=new Packet(buf);
      packet.reset();
      buf.putByte((byte)Session.SSH_MSG_CHANNEL_CLOSE);
      buf.putInt(getRecipient());
      session.write(packet);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  public void disconnect(){
    close();
    thread=null;
    try{
      if(io!=null){
	try{
	  if(io.in!=null && (io.in instanceof PassiveInputStream))
	    io.in.close();
	}
	catch(Exception ee){}
	try{
	  if(io.out!=null && (io.out instanceof PassiveOutputStream))
	    io.out.close();
	}
	catch(Exception ee){}
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
    io=null;
    Channel.del(this);
  }

  public void sendSignal(String foo) throws Exception {
    RequestSignal request=new RequestSignal();
    request.setSignal(foo);
    request.request(session, this);
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

  class PassiveInputStream extends PipedInputStream{
    PassiveInputStream(PipedOutputStream out) throws IOException{
      super(out);
    }
  }
  class PassiveOutputStream extends PipedOutputStream{
    PassiveOutputStream(PipedInputStream in) throws IOException{
      super(in);
    }
  }

  void setSession(Session session){
    this.session=session;
  }
}
