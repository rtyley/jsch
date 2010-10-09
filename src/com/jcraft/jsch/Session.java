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

import java.io.*;
import java.net.*;
import java.lang.*;

public class Session implements Runnable{
  private String version="JSCH-0.1";
  private byte[] V_S;                                 // server version
  private byte[] V_C=("SSH-2.0-"+version).getBytes(); // client version

  private byte[] I_C; // the payload of the client's SSH_MSG_KEXINIT
  private byte[] I_S; // the payload of the server's SSH_MSG_KEXINIT
  private byte[] K_S; // the host key

  private byte[] session_id;

  private byte[] IVc2s;
  private byte[] IVs2c;
  private byte[] Ec2s;
  private byte[] Es2c;
  private byte[] MACc2s;
  private byte[] MACs2c;

  private int seqi=0;
  private int seqo=0;

  private Cipher s2ccipher;
  private Cipher c2scipher;
  private MAC s2cmac;
  private MAC c2smac;

  private IO io;

  InputStream in=System.in;
  OutputStream out=System.out;

  static Random random;

  Buffer buf;
  Packet packet;

  private java.util.Properties config=null;

  private Proxy proxy=null;
  private UserInfo userinfo;
  private Channel channel;
  private String host="127.0.0.1";
  private int port=22;

  Session(){    
    if(random==null){
      try{
	Class c=Class.forName(getConfig("random"));
        random=(Random)(c.newInstance());
      }
      catch(Exception e){ System.err.println(e); }
    }
    buf=new Buffer();
    packet=new Packet(buf);
    io=new IO();
    Packet.setRandom(random);
  }

  public void connect(){    
    try	{
      int i, j;
      int pad=0;

      if(proxy==null){
        Socket socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        io.setInputStream(socket.getInputStream());
        io.setOutputStream(socket.getOutputStream());
      }
      else{
        proxy.connect(host, port);
        io.setInputStream(proxy.getInputStream());
        io.setOutputStream(proxy.getOutputStream());
      }

      i=0;
      while(true){
        j=io.getByte();
        buf.buffer[i]=(byte)j; i++; 
        if(j==10)break;
      }

      V_S=new byte[i-1]; System.arraycopy(buf.buffer, 0, V_S, 0, i-1);

      io.put(V_C, 0, V_C.length); io.put("\n".getBytes(), 0, 1);

      buf=read(buf);
      //System.out.println("read: 20 ? "+buf.buffer[5]);
      j=buf.getInt();
      I_S=new byte[j-1-buf.getByte()];
      System.arraycopy(buf.buffer, buf.s, I_S, 0, I_S.length);

      // byte      SSH_MSG_KEXINIT(20)
      // byte[16]  cookie (random bytes)
      // string    kex_algorithms
      // string    server_host_key_algorithms
      // string    encryption_algorithms_client_to_server
      // string    encryption_algorithms_server_to_client
      // string    mac_algorithms_client_to_server
      // string    mac_algorithms_server_to_client
      // string    compression_algorithms_client_to_server
      // string    compression_algorithms_server_to_client
      // string    languages_client_to_server
      // string    languages_server_to_client
      packet.reset();
      buf.putByte((byte) Const.SSH_MSG_KEXINIT);
      random.fill(buf.buffer, buf.index, 16); buf.skip(16);
      buf.putString(KeyExchange.kex.getBytes());
      buf.putString(KeyExchange.server_host_key.getBytes());
      buf.putString(KeyExchange.enc_c2s.getBytes());
      buf.putString(KeyExchange.enc_s2c.getBytes());
      buf.putString(KeyExchange.mac_c2s.getBytes());
      buf.putString(KeyExchange.mac_s2c.getBytes());
      buf.putString(KeyExchange.comp_c2s.getBytes());
      buf.putString(KeyExchange.comp_s2c.getBytes());
      buf.putString(KeyExchange.lang_c2s.getBytes());
      buf.putString(KeyExchange.lang_s2c.getBytes());
      buf.putByte((byte)0);
      buf.putInt(0);
      I_C=new byte[buf.getLength()];
      buf.getByte(I_C);
      packet.pack();
      write(packet);

      KeyExchange kex=null;
      try{
	Class c=Class.forName(getConfig("kex"));
        kex=(KeyExchange)(c.newInstance());
      }
      catch(Exception e){ System.err.println(e); }

      kex.init(V_S, V_C, I_S, I_C);
      boolean result=kex.start(this);

      if(!result){
        System.out.println("verify: "+result);
        System.exit(-1);
      }

      byte[] K_S=kex.getHostKey();
      if(!JSch.isKnownHost(host, K_S)){
        result=userinfo.prompt(
          "The authenticity of host '"+host+"' can't be established.\n"+
          "DSA key fingerprint is "+kex.getFingerPrint()+".\n"+
          "Are you sure you want to continue connecting (yes/no)?"
        );
        if(!result){
          System.exit(-1);
        }
      }

      byte[] K=kex.getK();
      byte[] H=kex.getH();
      HASH hash=kex.getHash();

      if(session_id==null){
        session_id=new byte[H.length];
	System.arraycopy(H, 0, session_id, 0, H.length);
      }

      //  receive SSH_MSG_NEWKEYS(21)
      buf=read(buf);
      //System.out.println("read: 21 ? "+buf.buffer[5]);

      //  send SSH_MSG_NEWKEYS(21)
      packet.reset();
      buf.putByte((byte)0x15);
      packet.pack();
      write(packet);

      updateKeys(hash, K, H, session_id);

      UserAuth us=new UserAuthPassword(userinfo);
      us.start(this);

      (new Thread(this)).start();
    }
    catch(IOException e) {
      System.out.println("Session.connect: "+e);
      System.exit(-1);
    }
    catch(Exception e) {
      System.out.println("Session.connect: "+e);
      System.exit(-1);
    }
    return;
  }

//public void start(){ (new Thread(this)).start();  }

  public Channel openChannel(String type){
    try{
      Channel channel=Channel.getChannel(type);
      addChannel(channel);
      channel.init();
      return channel;
    }
    catch(Exception e){
      System.out.println("??"+e);
    }
    return null;
  }



  // encode will bin invoked in write with synchronization.
  public void encode(Packet packet) throws Exception{
     byte[] mac=null;
     if(c2scipher!=null){
       int pad=packet.buffer.buffer[4];
       random.fill(packet.buffer.buffer, packet.buffer.index-pad, pad);
     }
     if(c2smac!=null){
       c2smac.update(seqo);
       c2smac.update(packet.buffer.buffer, 0, packet.buffer.index);
       mac=c2smac.doFinal();
     }
     if(c2scipher!=null){
       byte[] buf=packet.buffer.buffer;
       c2scipher.update(buf, 0, packet.buffer.index, buf, 0);
     }
     if(mac!=null){
       packet.buffer.putByte(mac);
     }
  }

  byte[] mac=new byte[16];
  public Buffer read(Buffer buf) throws Exception{
    int j=0;
    while(true){
      buf.reset();
      io.getByte(buf.buffer, buf.index, 8); buf.index+=8;
      if(s2ccipher!=null){
        s2ccipher.update(buf.buffer, 0, 8, buf.buffer, 0);
      }
      j=((buf.buffer[0]<<24)&0xff000000)|
        ((buf.buffer[1]<<16)&0x00ff0000)|
        ((buf.buffer[2]<< 8)&0x0000ff00)|
        ((buf.buffer[3]    )&0x000000ff);
      io.getByte(buf.buffer, buf.index, j-4); buf.index+=(j-4);
      if(s2ccipher!=null){
        s2ccipher.update(buf.buffer, 8, j-4, buf.buffer, 8);
      }
      if(s2cmac!=null){
	s2cmac.update(seqi);
	s2cmac.update(buf.buffer, 0, buf.index);
	byte[] result=s2cmac.doFinal();
	io.getByte(mac, 0, 16);
        if(java.util.Arrays.equals(result, mac)){
	}
      }
      seqi++;

      if(buf.buffer[5]==1){
        buf.rewind();
        buf.getInt();buf.getShort();
	int reason_code=buf.getInt();
	byte[] description=buf.getString();
	byte[] language_tag=buf.getString();
	System.err.println("SSH_MSG_DISCONNECT:"+
                           " "+reason_code+
			   " "+new String(description)+
			   " "+new String(language_tag));
	break;
      }
      else if(buf.buffer[5]==93){ // ADJUST
          buf.rewind();
          buf.getInt();buf.getShort();
	  Channel c=Channel.getChannel(buf.getInt());
	  c.setRemoteWindowSize(c.rwsize+buf.getInt()); 
      }
      else if(buf.buffer[5]==2){ // IGNORE
      }
      else if(buf.buffer[5]==4){ // DEBUG
        buf.rewind();
        buf.getInt();buf.getShort();
	byte always_display=(byte)buf.getByte();
	byte[] message=buf.getString();
	byte[] language_tag=buf.getString();
	System.err.println("SSH_MSG_DEBUG:"+
			   " "+new String(message)+
			   " "+new String(language_tag));
      }
      else{
        break;
      }
    }
    buf.rewind();
    return buf;
  }

  private void updateKeys(HASH hash,
			  byte[] K, byte[] H, byte[] session_id) throws Exception{
    /*
      Initial IV client to server:     HASH (K || H || "A" || session_id)
      Initial IV server to client:     HASH (K || H || "B" || session_id)
      Encryption key client to server: HASH (K || H || "C" || session_id)
      Encryption key server to client: HASH (K || H || "D" || session_id)
      Integrity key client to server:  HASH (K || H || "E" || session_id)
      Integrity key server to client:  HASH (K || H || "F" || session_id)
    */

    buf.reset();
    buf.putMPInt(K);
    buf.putByte(H);
    buf.putByte((byte)0x41);
    buf.putByte(session_id);
    hash.update(buf.buffer, 0, buf.index);
    IVc2s=hash.digest();

    int j=buf.index-session_id.length-1;

    buf.buffer[j]++;
    hash.update(buf.buffer, 0, buf.index);
    IVs2c=hash.digest();

    buf.buffer[j]++;
    hash.update(buf.buffer, 0, buf.index);
    Ec2s=hash.digest();

    buf.buffer[j]++;
    hash.update(buf.buffer, 0, buf.index);
    Es2c=hash.digest();

    buf.buffer[j]++;
    hash.update(buf.buffer, 0, buf.index);
    MACc2s=hash.digest();

    buf.buffer[j]++;
    hash.update(buf.buffer, 0, buf.index);
    MACs2c=hash.digest();

    try{
      Class c;
  
      c=Class.forName(getConfig("cipher.s2c"));
      s2ccipher=(Cipher)(c.newInstance());
      s2ccipher.init(Cipher.DECRYPT_MODE, Es2c, IVs2c);

      c=Class.forName(getConfig("mac.s2c"));
      s2cmac=(MAC)(c.newInstance());
      s2cmac.init(MACs2c);

      c=Class.forName(getConfig("cipher.c2s"));
      c2scipher=(Cipher)(c.newInstance());
      c2scipher.init(Cipher.ENCRYPT_MODE, Ec2s, IVc2s);

      c=Class.forName(getConfig("mac.c2s"));
      c2smac=(MAC)(c.newInstance());
      c2smac.init(MACc2s);
    }
    catch(Exception e){ System.err.println(e); }
  }

  public synchronized void write(Packet packet) throws Exception{
     encode(packet);
     io.put(packet);
     seqo++;
  }

  Runnable thread;
  public void run(){
    thread=this;
    byte[] foo;
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);
    int i=0;
    Channel channel;
    int[] start=new int[1];
    int[] length=new int[1];

    try{
      while(thread!=null){
        buf=read(buf);
//      if(buf.buffer[5]!=94)
//System.out.println("read: 94 ? "+buf.buffer[5]);
        switch(buf.buffer[5]){
	case Const.SSH_MSG_CHANNEL_DATA:
          buf.getInt(); 
          buf.getByte(); 
          buf.getByte(); 
          i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  foo=buf.getString(start, length);
	  channel.write(foo, start[0], length[0]);

	  int len=length[0];
	  channel.setLocalWindowSize(channel.lwsize-len);
 	  if(channel.lwsize<channel.lwsize_max/2){
            packet.reset();
	    buf.putByte((byte)93);
	    buf.putInt(channel.getRecipient());
	    buf.putInt(channel.lwsize_max-channel.lwsize);
	    packet.pack();
	    write(packet);
	    channel.setLocalWindowSize(channel.lwsize_max);
	  }
	  break;
        case Const.SSH_MSG_CHANNEL_EXTENDED_DATA:
          buf.getInt();
	  buf.getShort();
	  i=buf.getInt();
	  channel=Channel.getChannel(i);
	  buf.getInt();                   // data_type_code == 1
	  foo=buf.getString(start, length);
	  channel.write(foo, start[0], length[0]);
	  break;
	case Const.SSH_MSG_CHANNEL_WINDOW_ADJUST:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  channel.setRemoteWindowSize(channel.rwsize+buf.getInt()); 
	  break;
	case Const.SSH_MSG_CHANNEL_EOF:
          buf.getInt(); 
          buf.getShort(); 
          i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  channel.setEOF();

	  packet.reset();
	  buf.putByte((byte)Const.SSH_MSG_CHANNEL_EOF);
	  buf.putInt(channel.getRecipient());
	  packet.pack();
	  write(packet);
	  break;
	case Const.SSH_MSG_CHANNEL_CLOSE:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  channel.close();

	  packet.reset();
	  buf.putByte((byte) Const.SSH_MSG_CHANNEL_CLOSE);
	  buf.putInt(channel.getRecipient());
	  packet.pack();
	  write(packet);
	  Channel.del(channel);
	  break;
	case Const.SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  channel.setRecipient(buf.getInt());
	  channel.setRemoteWindowSize(buf.getInt());
	  channel.setRemotePacketSize(buf.getInt());
	  break;
	case Const.SSH_MSG_CHANNEL_REQUEST:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  foo=buf.getString(); 
          if((new String(foo)).equals("exit-status")){
            buf.getByte();
            channel=Channel.getChannel(i);
   	    i=buf.getInt();             // exit-status
	    System.out.println("exit-stauts: "+i);
	  }
	  break;
	case Const.SSH_MSG_CHANNEL_OPEN:
          buf.getInt(); 
	  buf.getShort(); 
	  foo=buf.getString(); 
	  //System.out.println("type="+new String(foo));
 	  channel=Channel.getChannel(new String(foo));
	  addChannel(channel);
	  channel.getData(buf);
	  channel.init();

	  packet.reset();
	  buf.putByte((byte)Const.SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
	  buf.putInt(channel.getRecipient());
	  buf.putInt(channel.id);
	  buf.putInt(channel.lwsize);
	  buf.putInt(channel.lmpsize);
	  packet.pack();
	  write(packet);
	  (new Thread(channel)).start();
          break;
	default:
          System.out.println("Session.run: unsupported type "+buf.buffer[5]); 
          System.exit(-1);
	}
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public void setPortForwardingL(int lport, String host, int rport){
    PortWatcher pw=new PortWatcher(this, lport, host, rport);
    (new Thread(pw)).start();
  }

  public void setPortForwardingR(int rport, String host, int lport) throws Exception{
    Buffer buf=new Buffer(100); // ??
    Packet packet=new Packet(buf);

    ChannelForwardedTCPIP.addPort(this, rport, host, lport);

    // byte SSH_MSG_GLOBAL_REQUEST 80
    // string "tcpip-forward"
    // boolean want_reply
    // string  address_to_bind
    // uint32  port number to bind
    packet.reset();
    buf.putByte((byte) Const.SSH_MSG_GLOBAL_REQUEST);
    buf.putString("tcpip-forward".getBytes());
    buf.putByte((byte)0);
    buf.putString("0.0.0.0".getBytes());
    buf.putInt(rport);
    packet.pack();
    write(packet);
  }

  void addChannel(Channel channel){
    channel.session=this;
  }

  public String getConfig(String name){
    Object foo=null;
    if(config!=null){
      foo=config.get(name);
      if(foo instanceof String) return (String)foo;
    }
    foo=JSch.config.get(name);
    if(foo instanceof String) return (String)foo;
    return null;
  }
  public Channel getChannel(){ return channel; }
  public void setProxy(Proxy proxy){ this.proxy=proxy; }
  public void setHost(String host){ this.host=host; }
  public void setPort(int port){ this.port=port; }
  public void setUserInfo(UserInfo userinfo){ this.userinfo=userinfo; }
  public void setInputStream(InputStream in){ this.in=in; }
  public void setOutputStream(OutputStream out){ this.out=out; }
  public void setX11Host(String host){ ChannelX11.setHost(host); }
  public void setX11Port(int port){ ChannelX11.setPort(port); }
  public void setX11Cookie(String cookie){ ChannelX11.setCookie(cookie); }
}
