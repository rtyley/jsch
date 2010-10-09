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

import java.net.*;
class ChannelX11 extends Channel{

  int lwsize_max=0x20000;
  int lwsize=lwsize_max;   // local initial window size
  int lmpsize=0x4000;      // local maximum packet size

  static String host="127.0.0.1";
  static int port=6000;

  boolean init=true;

  static byte[] cookie=null;
//static byte[] cookie_hex="0c281f065158632a427d3e074d79265d".getBytes();
  static byte[] cookie_hex=null;

  private static java.util.Hashtable faked_cookie_pool=new java.util.Hashtable();
  private static java.util.Hashtable faked_cookie_hex_pool=new java.util.Hashtable();

  static byte[] table={0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,
                        0x61,0x62,0x63,0x64,0x65,0x66};
  static int revtable(byte foo){
    for(int i=0; i<table.length; i++){
      if(table[i]==foo)return i;
    }
    return 0;
  }
  static void setCookie(String foo){
    cookie_hex=foo.getBytes(); 
    cookie=new byte[16];
    for(int i=0; i<16; i++){
	cookie[i]=(byte)(((revtable(cookie_hex[i*2])<<4)&0xf0) |
			 ((revtable(cookie_hex[i*2+1]))&0xf));
    }
  }
  static void setHost(String foo){ host=foo; }
  static void setPort(int foo){ port=foo; }
  static byte[] getFakedCookie(Session session){
    byte[] foo=(byte[])faked_cookie_hex_pool.get(session);
    if(foo==null){
      Random random=session.random;
      foo=new byte[16];
      random.fill(foo, 0, 16);
/*
System.out.print("faked_cookie: ");
for(int i=0; i<foo.length; i++){
    System.out.print(Integer.toHexString(foo[i]&0xff)+":");
}
System.out.println("");
*/
      faked_cookie_pool.put(session, foo);
      byte[] bar=new byte[32];
      for(int i=0; i<16; i++){
         bar[2*i]=table[(foo[i]>>>4)&0xf];
         bar[2*i+1]=table[(foo[i])&0xf];
      }
      faked_cookie_hex_pool.put(session, bar);
      foo=bar;
    }
    return foo;
  }

  Socket socket = null;
  ChannelX11(){
    super();
    type="x11".getBytes();
    try{ 
      socket=new Socket(host, port);
      socket.setTcpNoDelay(true);
      io=new IO();
      io.setInputStream(socket.getInputStream());
      io.setOutputStream(socket.getOutputStream());
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public void run(){
    thread=this;
    Buffer buf=new Buffer(rmpsize);
    Packet packet=new Packet(buf);
    int i=0;
    try{
      while(thread!=null){
        i=io.in.read(buf.buffer, 
		     14, 
		     buf.buffer.length-14
		     -16 -20 // padding and mac
		     );
	if(i<=0){
          break;
	}
	if(close)break;
        packet.reset();
        buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
        buf.putInt(recipient);
        buf.putInt(i);
        buf.skip(i);
	session.write(packet, this, i);
      }
    }
    catch(Exception e){
      //System.out.println(e);
    }
    thread=null;
  }

  void write(byte[] foo, int s, int l) throws java.io.IOException {
    if(eof)return;

    if(init){
      int plen=(foo[s+6]&0xff)*256+(foo[s+7]&0xff);
      int dlen=(foo[s+8]&0xff)*256+(foo[s+9]&0xff);
      if((foo[s]&0xff)==0x42){
      }
      else if((foo[s]&0xff)==0x6c){
         plen=((plen>>>8)&0xff)|((plen<<8)&0xff00);
         dlen=((dlen>>>8)&0xff)|((dlen<<8)&0xff00);
      }
      else{
	  // ??
      }
      byte[]bar=new byte[dlen];
      System.arraycopy(foo, s+12+plen+((-plen)&3), bar, 0, dlen);
      byte[] faked_cookie=(byte[])faked_cookie_pool.get(session);

      if(java.util.Arrays.equals(bar, faked_cookie)){
        if(cookie!=null)
          System.arraycopy(cookie, 0, foo, s+12+plen+((-plen)&3), dlen);
      }
      else{
	  System.out.println("wrong cookie");
      }
      init=false;
    }
    io.put(foo, s, l);
  }

  public void disconnect(){
    close();
    thread=null;
    try{
      if(io!=null){
	try{
	  if(io.in!=null)
	    io.in.close();
	}
	catch(Exception ee){}
	try{
	  if(io.out!=null)
	    io.out.close();
	}
	catch(Exception ee){}
      }
      try{
	if(socket!=null)
	  socket.close();
      }
      catch(Exception ee){}
    }
    catch(Exception e){
      e.printStackTrace();
    }
    io=null;
    Channel.del(this);
  }
}
