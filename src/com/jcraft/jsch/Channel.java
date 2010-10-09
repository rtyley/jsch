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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


public abstract class Channel implements Runnable{
  static int index=0; 
  private static java.util.Vector pool=new java.util.Vector();
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
    if(type.equals("subsystem")){
      return new ChannelSubsystem();
    }
    return null;
  }
  static Channel getChannel(int id, Session session){
    synchronized(pool){
      for(int i=0; i<pool.size(); i++){
	Channel c=(Channel)(pool.elementAt(i));
	if(c.id==id && c.session==session) return c;
      }
    }
    return null;
  }
  static void del(Channel c){
    synchronized(pool){
      pool.removeElement(c);
    }
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
  Thread thread=null;

  boolean eof_local=false;
  boolean eof_remote=false;

  boolean close=false;
  boolean connected=false;

  int exitstatus=-1;

  int reply=0; 

  Session session;

  Channel(){
    synchronized(pool){
      id=index++;
      pool.addElement(this);
    }
  }
  void setRecipient(int foo){
    this.recipient=foo;
  }
  int getRecipient(){
    return recipient;
  }

  void init(){
  }

  public void connect() throws JSchException{
    if(!session.isConnected()){
      throw new JSchException("session is down");
    }
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

      int retry=1000;
      while(this.getRecipient()==-1 &&
	    session.isConnected() &&
	    retry>0){
	try{Thread.sleep(50);}catch(Exception ee){}
	retry--;
      }
      if(!session.isConnected()){
	throw new JSchException("session is down");
      }
      if(retry==0){
        throw new JSchException("channel is not opened.");
      }
      start();
      connected=true;
    }
    catch(Exception e){
      if(e instanceof JSchException) throw (JSchException)e;
    }
  }

  public void setXForwarding(boolean foo){
  }

  public void start() throws JSchException{}

  public boolean isEOF() {return eof_remote;}

  void getData(Buffer buf){
    setRecipient(buf.getInt());
    setRemoteWindowSize(buf.getInt());
    setRemotePacketSize(buf.getInt());
  }

  public void setInputStream(InputStream in){
    io.setInputStream(in, false);
  }
  public void setInputStream(InputStream in, boolean dontclose){
    io.setInputStream(in, dontclose);
  }
  public void setOutputStream(OutputStream out){
    io.setOutputStream(out, false);
  }
  public void setOutputStream(OutputStream out, boolean dontclose){
    io.setOutputStream(out, dontclose);
  }
  public void setExtOutputStream(OutputStream out){
    io.setExtOutputStream(out, false);
  }
  public void setExtOutputStream(OutputStream out, boolean dontclose){
    io.setExtOutputStream(out, dontclose);
  }
  public InputStream getInputStream() throws IOException {
    PipedInputStream in=
      new MyPipedInputStream(
                             32*1024  // this value should be customizable.
                             );
    io.setOutputStream(new PassiveOutputStream(in), false);
    return in;
  }
  public InputStream getExtInputStream() throws IOException {
    PipedInputStream in=
      new MyPipedInputStream(
                             32*1024  // this value should be customizable.
                             );
    io.setExtOutputStream(new PassiveOutputStream(in), false);
    return in;
  }
  public OutputStream getOutputStream() throws IOException {
    PipedOutputStream out=new PipedOutputStream();
    io.setInputStream(new PassiveInputStream(out
                                             , 32*1024
                                             ), false);
//  io.setInputStream(new PassiveInputStream(out), false);
    return out;
  }
  class MyPipedInputStream extends PipedInputStream{
    MyPipedInputStream() throws IOException{ super(); }
    MyPipedInputStream(int size) throws IOException{
      super();
      buffer=new byte[size];
    }
    MyPipedInputStream(PipedOutputStream out) throws IOException{ super(out); }
    MyPipedInputStream(PipedOutputStream out, int size) throws IOException{
      super(out);
      buffer=new byte[size];
    }
  }
  void setLocalWindowSizeMax(int foo){ this.lwsize_max=foo; }
  void setLocalWindowSize(int foo){ this.lwsize=foo; }
  void setLocalPacketSize(int foo){ this.lmpsize=foo; }
  synchronized void setRemoteWindowSize(int foo){ this.rwsize=foo; }
  synchronized void addRemoteWindowSize(int foo){ this.rwsize+=foo; }
  void setRemotePacketSize(int foo){ this.rmpsize=foo; }

  public void run(){
  }

  void write(byte[] foo) throws IOException {
    write(foo, 0, foo.length);
  }
  void write(byte[] foo, int s, int l) throws IOException {
    if(io.out!=null)
      io.put(foo, s, l);
  }
  void write_ext(byte[] foo, int s, int l) throws IOException {
    if(io.out_ext!=null)
      io.put_ext(foo, s, l);
  }

  void eof_remote() throws IOException {
    eof_remote=true;
    if(io.out!=null){
      io.out.close();
      io.out=null;
    }
  }

  void eof(){
//System.out.println("EOF!!!! "+this);
//Thread.dumpStack();
    if(close)return;
    if(eof_local)return;
    eof_local=true;
    //close=eof;
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
    /*
    if(!isConnected()){ disconnect(); }
    */
  }

  /*
  http://www1.ietf.org/internet-drafts/draft-ietf-secsh-connect-24.txt

5.3  Closing a Channel
  When a party will no longer send more data to a channel, it SHOULD
   send SSH_MSG_CHANNEL_EOF.

            byte      SSH_MSG_CHANNEL_EOF
            uint32    recipient_channel

  No explicit response is sent to this message.  However, the
   application may send EOF to whatever is at the other end of the
  channel.  Note that the channel remains open after this message, and
   more data may still be sent in the other direction.  This message
   does not consume window space and can be sent even if no window space
   is available.

     When either party wishes to terminate the channel, it sends
     SSH_MSG_CHANNEL_CLOSE.  Upon receiving this message, a party MUST
   send back a SSH_MSG_CHANNEL_CLOSE unless it has already sent this
   message for the channel.  The channel is considered closed for a
     party when it has both sent and received SSH_MSG_CHANNEL_CLOSE, and
   the party may then reuse the channel number.  A party MAY send
   SSH_MSG_CHANNEL_CLOSE without having sent or received
   SSH_MSG_CHANNEL_EOF.

            byte      SSH_MSG_CHANNEL_CLOSE
            uint32    recipient_channel

   This message does not consume window space and can be sent even if no
   window space is available.

   It is recommended that any data sent before this message is delivered
     to the actual destination, if possible.
  */

  void close(){
    //System.out.println("close!!!!");
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
      //e.printStackTrace();
    }
  }
  public boolean isClosed(){
    return close;
  }
  static void disconnect(Session session){
    Channel[] channels=null;
    int count=0;
    synchronized(pool){
      channels=new Channel[pool.size()];
      for(int i=0; i<pool.size(); i++){
	try{
	  Channel c=((Channel)(pool.elementAt(i)));
	  if(c.session==session){
	    channels[count++]=c;
	  }
	}
	catch(Exception e){
	}
      } 
    }
    for(int i=0; i<count; i++){
      channels[i].disconnect();
    }
  }

  public void finalize() throws Throwable{
    disconnect();
    super.finalize();
    session=null;
  }

  public void disconnect(){
//System.out.println(this+":disconnect "+((ChannelExec)this).command+" "+io.in);
//System.out.println(this+":disconnect "+io+" "+io.in);
    if(!connected){
      return;
    }
    connected=false;

    //eof();
    close();
    thread=null;

    try{
      if(io!=null){
        io.close();
        /*
	try{
	  //System.out.println(" io.in="+io.in);
	  if(io.in!=null && 
	     (io.in instanceof PassiveInputStream)
	     )
	    io.in.close();
          io.in=null;
	}
	catch(Exception ee){}
	try{
	  //System.out.println(" io.out="+io.out);
	  if(io.out!=null && 
	     (io.out instanceof PassiveOutputStream)
	     )
	    io.out.close();
          io.out=null;
	}
	catch(Exception ee){}
	try{
	  //System.out.println(" io.out_ext="+out_ext);
	  if(io.out_ext!=null &&
	     (io.out_ext instanceof PassiveOutputStream)
	     )
	    io.out_ext.close();
          io.out_ext=null;
	}
	catch(Exception ee){}
        */
      }
    }
    catch(Exception e){
      //e.printStackTrace();
    }

    io=null;
    Channel.del(this);
  }

  public boolean isConnected(){
    if(this.session!=null){
      return session.isConnected() && connected;
    }
    return false;
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

  class PassiveInputStream extends MyPipedInputStream{
    PipedOutputStream out;
    PassiveInputStream(PipedOutputStream out, int size) throws IOException{
      super(out, size);
      this.out=out;
    }
    PassiveInputStream(PipedOutputStream out) throws IOException{
      super(out);
      this.out=out;
    }
    public void close() throws IOException{
      if(out!=null){
        this.out.close();
      }
      out=null;
    }
  }
  class PassiveOutputStream extends PipedOutputStream{
    PassiveOutputStream(PipedInputStream in) throws IOException{
      super(in);
    }
  }

  void setExitStatus(int foo){ exitstatus=foo; }
  public int getExitStatus(){ return exitstatus; }

  void setSession(Session session){
    this.session=session;
  }
  public Session getSession(){ return session; }
  public int getId(){ return id; }
  //public int getRecipientId(){ return getRecipient(); }
}
