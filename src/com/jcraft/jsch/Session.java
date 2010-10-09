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
import java.net.*;
import java.lang.*;

public class Session implements Runnable{
  static private final String version="JSCH-0.0.13";

  // http://ietf.org/internet-drafts/draft-ietf-secsh-assignednumbers-01.txt
  static final int SSH_MSG_DISCONNECT=                      1;
  static final int SSH_MSG_IGNORE=                          2;
  static final int SSH_MSG_UNIMPLEMENTED=                   3;
  static final int SSH_MSG_DEBUG=                           4;
  static final int SSH_MSG_SERVICE_REQUEST=                 5;
  static final int SSH_MSG_SERVICE_ACCEPT=                  6;
  static final int SSH_MSG_KEXINIT=                        20;
  static final int SSH_MSG_NEWKEYS=                        21;
  static final int SSH_MSG_KEXDH_INIT=                     30;
  static final int SSH_MSG_KEXDH_REPLY=                    31;
  static final int SSH_MSG_USERAUTH_REQUEST=               50;
  static final int SSH_MSG_USERAUTH_FAILURE=               51;
  static final int SSH_MSG_USERAUTH_SUCCESS=               52;
  static final int SSH_MSG_USERAUTH_BANNER=                53;
  static final int SSH_MSG_USERAUTH_PK_OK=                 60;
  static final int SSH_MSG_GLOBAL_REQUEST=                 80;
  static final int SSH_MSG_REQUEST_SUCCESS=                81;
  static final int SSH_MSG_REQUEST_FAILURE=                82;
  static final int SSH_MSG_CHANNEL_OPEN=                   90;
  static final int SSH_MSG_CHANNEL_OPEN_CONFIRMATION=      91;
  static final int SSH_MSG_CHANNEL_OPEN_FAILURE=           92;
  static final int SSH_MSG_CHANNEL_WINDOW_ADJUST=          93;
  static final int SSH_MSG_CHANNEL_DATA=                   94;
  static final int SSH_MSG_CHANNEL_EXTENDED_DATA=          95;
  static final int SSH_MSG_CHANNEL_EOF=                    96;
  static final int SSH_MSG_CHANNEL_CLOSE=                  97;
  static final int SSH_MSG_CHANNEL_REQUEST=                98;
  static final int SSH_MSG_CHANNEL_SUCCESS=                99;
  static final int SSH_MSG_CHANNEL_FAILURE=               100;

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
  private byte[] mac_buf;

  private Compression deflater;
  private Compression inflater;

  private IO io;
  private Socket socket;

  private boolean isConnected=false;

  InputStream in=null;
  OutputStream out=null;

  static Random random;

  Buffer buf;
  Packet packet;

  SocketFactory socket_factory=null;

  private java.util.Properties config=null;

  private Proxy proxy=null;
  private UserInfo userinfo;
  private Channel channel;

  String host="127.0.0.1";
  int port=22;

  String username=null;
  String password=null;
  //String passphrase=null;

  JSch jsch;


  static{
    Class ccc=Channel.class;
  }

  Session(JSch jsch) throws JSchException{
    super();
    this.jsch=jsch;
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

  public void connect() throws JSchException{    
    try	{
      int i, j;
      int pad=0;
      if(proxy==null){
        InputStream in;
        OutputStream out;
	if(socket_factory==null){
          socket=new Socket(host, port);
	  in=socket.getInputStream();
	  out=socket.getOutputStream();
	}
	else{
          socket=socket_factory.createSocket(host, port);
	  in=socket_factory.getInputStream(socket);
	  out=socket_factory.getOutputStream(socket);
	}
        socket.setTcpNoDelay(true);
        io.setInputStream(in);
        io.setOutputStream(out);
      }
      else{
        proxy.connect(this, host, port);
        io.setInputStream(proxy.getInputStream());
        io.setOutputStream(proxy.getOutputStream());
      }

      i=0;
      while(true){
        j=io.getByte();
        buf.buffer[i]=(byte)j; i++; 
        if(j==10)break;
      }

      if(buf.buffer[i-1]==10){    // 0x0a
	i--;
	if(buf.buffer[i-1]==13){  // 0x0d
	  i--;
	}
      }
      else{
	throw new JSchException("invalid server's version string");
      }

      V_S=new byte[i]; System.arraycopy(buf.buffer, 0, V_S, 0, i);

      //System.out.println("V_S: "+new String(V_S));

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
      buf.putByte((byte) SSH_MSG_KEXINIT);
      random.fill(buf.buffer, buf.index, 16); buf.skip(16);
      buf.putString(getConfig("kex").getBytes());
      buf.putString(getConfig("server_host_key").getBytes());
      buf.putString(getConfig("cipher.c2s").getBytes());
      buf.putString(getConfig("cipher.s2c").getBytes());
      buf.putString(getConfig("mac.c2s").getBytes());
      buf.putString(getConfig("mac.s2c").getBytes());
      buf.putString(getConfig("compression.c2s").getBytes());
      buf.putString(getConfig("compression.s2c").getBytes());
      buf.putString(getConfig("lang.c2s").getBytes());
      buf.putString(getConfig("lang.s2c").getBytes());
      buf.putByte((byte)0);
      buf.putInt(0);
      I_C=new byte[buf.getLength()];
      buf.getByte(I_C);
      write(packet);

      KeyExchange kex=null;
      try{
	Class c=Class.forName(getConfig(getConfig("kex")));
        kex=(KeyExchange)(c.newInstance());
      }
      catch(Exception e){ System.err.println(e); }

      kex.init(V_S, V_C, I_S, I_C);
      boolean result=kex.start(this);
      if(!result){
        //System.out.println("verify: "+result);
        throw new JSchException("verify: "+result);
      }
      byte[] K_S=kex.getHostKey();
      i=jsch.getKnownHosts().check(host, K_S);
      if(i==KnownHosts.CHANGED){
        String message=
"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n"+
"@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @\n"+
"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n"+
"IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!      \n"+
"Someone could be eavesdropping on you right now (man-in-the-middle attack)!\n"+
"It is also possible that the "+kex.getKeyType()+" host key has just been changed.\n"+
"The fingerprint for the "+kex.getKeyType()+" key sent by the remote host is\n"+
	  kex.getFingerPrint()+".\n"+
"Please contact your system administrator.\n"+
"Add correct host key in "+jsch.getKnownHosts().getKnownHosts()+" to get rid of this message.";
      if(userinfo!=null){
	  userinfo.showMessage(message);
      }
      throw new JSchException("HostKey has been changed");
      }

      if(i!=KnownHosts.OK){
	  if(userinfo!=null){
	      boolean foo=userinfo.promptYesNo("The authenticity of host '"+host+"' can't be established.\n"+
					       kex.getKeyType()+" key fingerprint is "+kex.getFingerPrint()+".\n"+
                                         "Are you sure you want to continue connecting (yes/no)?"
					       );
	      if(!foo){
		  throw new JSchException("reject HostKey");
	      }
	      jsch.getKnownHosts().insert(host, K_S);
	  }
	  else{
	      if(i==KnownHosts.NOT_INCLUDED) throw new JSchException("UnknownHostKey");
	      else  throw new JSchException("HostKey has been changed.");
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
      write(packet);

      updateKeys(hash, K, H, session_id);

      UserAuth us;
      boolean auth=false;
      if(jsch.identities.size()>0){
        us=new UserAuthPublicKey(userinfo);
        auth=us.start(this);
      }
      if(auth){
        (new Thread(this)).start();
        return; 
      }
      us=new UserAuthPassword(userinfo);
      auth=us.start(this);
      if(auth){
        (new Thread(this)).start();
	return;
      }
      throw new JSchException("Auth fail");
    }
    catch(IOException e) {
      e.printStackTrace();
      throw new JSchException("Session.connect: "+e);
    }
    catch(Exception e) {
      e.printStackTrace();
      throw new JSchException("Session.connect: "+e);
    }
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
      e.printStackTrace();
    }
    return null;
  }


  // encode will bin invoked in write with synchronization.
  public void encode(Packet packet) throws Exception{
    if(deflater!=null){
      packet.buffer.index=deflater.compress(packet.buffer.buffer, 
					    5, packet.buffer.index);
    }
    packet.padding();
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

  int[] uncompress_len=new int[1];

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
	io.getByte(mac_buf, 0, mac_buf.length);
        if(!java.util.Arrays.equals(result, mac_buf)){
          System.out.println("mac error");
	  throw new IOException("MAC Error");
	}
      }
      seqi++;

      if(inflater!=null){
        //inflater.uncompress(buf);
	int pad=buf.buffer[4];
	uncompress_len[0]=buf.index-5-pad;
	byte[] foo=inflater.uncompress(buf.buffer, 5, uncompress_len);
	if(foo!=null){
	  buf.buffer=foo;
	  buf.index=5+uncompress_len[0];
	}
	else{
	  System.err.println("fail in inflater");
	  break;
	}
      }

      int type=buf.buffer[5]&0xff;
      //System.out.println("read: "+type);
      if(type==SSH_MSG_DISCONNECT){
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
      else if(type==SSH_MSG_IGNORE){
      }
      else if(type==SSH_MSG_DEBUG){
        buf.rewind();
        buf.getInt();buf.getShort();
	byte always_display=(byte)buf.getByte();
	byte[] message=buf.getString();
	byte[] language_tag=buf.getString();
	System.err.println("SSH_MSG_DEBUG:"+
			   " "+new String(message)+
			   " "+new String(language_tag));
      }
      else if(type==SSH_MSG_CHANNEL_WINDOW_ADJUST){
          buf.rewind();
          buf.getInt();buf.getShort();
	  Channel c=Channel.getChannel(buf.getInt());
          c.addRemoteWindowSize(buf.getInt()); 
      }
      else{
        break;
      }
    }
    buf.rewind();
    return buf;
  }

  byte[] getSessionId(){
    return session_id;
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
  
      c=Class.forName(getConfig(getConfig("cipher.s2c")));
      s2ccipher=(Cipher)(c.newInstance());
      while(s2ccipher.getBlockSize()>Es2c.length){
        buf.reset();
        buf.putMPInt(K);
        buf.putByte(H);
        buf.putByte(Es2c);
        hash.update(buf.buffer, 0, buf.index);
        byte[] foo=hash.digest();
        byte[] bar=new byte[Es2c.length+foo.length];
	System.arraycopy(Es2c, 0, bar, 0, Es2c.length);
	System.arraycopy(foo, 0, bar, Es2c.length, foo.length);
	Es2c=bar;
      }
      s2ccipher.init(Cipher.DECRYPT_MODE, Es2c, IVs2c);

      c=Class.forName(getConfig(getConfig("mac.s2c")));
      s2cmac=(MAC)(c.newInstance());
      s2cmac.init(MACs2c);
      mac_buf=new byte[s2cmac.getBlockSize()];

      c=Class.forName(getConfig(getConfig("cipher.c2s")));
      c2scipher=(Cipher)(c.newInstance());
      while(c2scipher.getBlockSize()>Ec2s.length){
        buf.reset();
        buf.putMPInt(K);
        buf.putByte(H);
        buf.putByte(Ec2s);
        hash.update(buf.buffer, 0, buf.index);
        byte[] foo=hash.digest();
        byte[] bar=new byte[Ec2s.length+foo.length];
	System.arraycopy(Ec2s, 0, bar, 0, Ec2s.length);
	System.arraycopy(foo, 0, bar, Ec2s.length, foo.length);
	Ec2s=bar;
      }
      c2scipher.init(Cipher.ENCRYPT_MODE, Ec2s, IVc2s);

      c=Class.forName(getConfig(getConfig("mac.c2s")));
      c2smac=(MAC)(c.newInstance());
      c2smac.init(MACc2s);

      if(!getConfig("compression.c2s").equals("none")){
	String foo=getConfig(getConfig("compression.c2s"));
	if(foo!=null){
	  try{
	    c=Class.forName(foo);
	    deflater=(Compression)(c.newInstance());
	    deflater.init(Compression.DEFLATER, 6);
	  }
	  catch(Exception ee){
	    System.err.println(foo+" isn't accessible.");
	  }
	}
      }
      if(!getConfig("compression.s2c").equals("none")){
	String foo=getConfig(getConfig("compression.s2c"));
	if(foo!=null){
	  try{
	    c=Class.forName(foo);
	    inflater=(Compression)(c.newInstance());
	    inflater.init(Compression.INFLATER, 0);
	  }
	  catch(Exception ee){
	    System.err.println(foo+" isn't accessible.");
	  }
	}
      }
    }
    catch(Exception e){ System.err.println(e); }
  }

  public /*synchronized*/ void write(Packet packet, Channel c, int length) throws Exception{
    while(true){
      if(c.rwsize>length){
        c.rwsize-=length;
        break;
      }
      try{Thread.sleep(10);}catch(Exception e){};
    }
    write(packet);
  }
  public synchronized void write(Packet packet) throws Exception{
     encode(packet);
     if(io!=null){
       io.put(packet);
       seqo++;
     }
  }

  Runnable thread;
  public void run(){
    thread=this;

    isConnected=true;

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
	int msgType=buf.buffer[5]&0xff;
//      if(msgType!=94)
//        System.out.println("read: 94 ? "+msgType);
        switch(msgType){
	case SSH_MSG_CHANNEL_DATA:
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
	    buf.putByte((byte)SSH_MSG_CHANNEL_WINDOW_ADJUST);
	    buf.putInt(channel.getRecipient());
	    buf.putInt(channel.lwsize_max-channel.lwsize);
	    write(packet);
	    channel.setLocalWindowSize(channel.lwsize_max);
	  }
	  break;
        case SSH_MSG_CHANNEL_EXTENDED_DATA:
          buf.getInt();
	  buf.getShort();
	  i=buf.getInt();
	  channel=Channel.getChannel(i);
	  buf.getInt();                   // data_type_code == 1
	  foo=buf.getString(start, length);
	  channel.write(foo, start[0], length[0]);
	  break;
	case SSH_MSG_CHANNEL_WINDOW_ADJUST:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  channel.addRemoteWindowSize(buf.getInt()); 
	  break;
	case SSH_MSG_CHANNEL_EOF:
          buf.getInt(); 
          buf.getShort(); 
          i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  if(channel!=null){
	      channel.eof();
	  }
	  /*
	  packet.reset();
	  buf.putByte((byte)SSH_MSG_CHANNEL_EOF);
	  buf.putInt(channel.getRecipient());
	  write(packet);
	  */
	  break;
	case SSH_MSG_CHANNEL_CLOSE:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  if(channel!=null)
//	      channel.close();
	    channel.disconnect();
	  /*
          if(Channel.pool.size()==0){
	    thread=null;
	  }
	  */
	  break;
	case SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  channel=Channel.getChannel(i);
	  channel.setRecipient(buf.getInt());
	  channel.setRemoteWindowSize(buf.getInt());
	  channel.setRemotePacketSize(buf.getInt());
	  break;
	case SSH_MSG_CHANNEL_REQUEST:
          buf.getInt(); 
	  buf.getShort(); 
	  i=buf.getInt(); 
	  foo=buf.getString(); 
          boolean reply=(buf.getByte()!=0);
          channel=Channel.getChannel(i);
          byte reply_type=(byte)SSH_MSG_CHANNEL_FAILURE;
          if((new String(foo)).equals("exit-status")){
   	    i=buf.getInt();             // exit-status
//	    System.out.println("exit-stauts: "+i);
//          channel.close();
            reply_type=(byte)SSH_MSG_CHANNEL_SUCCESS;
	  }
          if(reply){
	      packet.reset();
	      buf.putByte(reply_type);
	      buf.putInt(channel.getRecipient());
	      write(packet);
          }
	  break;
	case SSH_MSG_CHANNEL_OPEN:
          buf.getInt(); 
	  buf.getShort(); 
	  foo=buf.getString(); 
	  //System.out.println("type="+new String(foo));
 	  channel=Channel.getChannel(new String(foo));
	  addChannel(channel);
	  channel.getData(buf);
	  channel.init();

	  packet.reset();
	  buf.putByte((byte)SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
	  buf.putInt(channel.getRecipient());
	  buf.putInt(channel.id);
	  buf.putInt(channel.lwsize);
	  buf.putInt(channel.lmpsize);
	  write(packet);
	  (new Thread(channel)).start();
          break;
	default:
          System.out.println("Session.run: unsupported type "+msgType); 
	  throw new IOException("Unknown SSH message type "+msgType);
	}
      }
    }
    catch(Exception e){
      //System.out.println("# Session.run");
      //e.printStackTrace();
    }
    try{
      disconnect();
    }
    catch(NullPointerException e){
      System.out.println("@1");
      e.printStackTrace();
    }
    catch(Exception e){
      System.out.println("@2");
      e.printStackTrace();
    }
    isConnected=false;
  }

  public void disconnect(){
    for(int i=0; i<Channel.pool.size(); i++){
      try{
        ((Channel)(Channel.pool.elementAt(i))).eof();
      }
      catch(Exception e){
      }
    }      
    thread=null;
    try{
      if(io!=null){
	if(io.in!=null) io.in.close();
	if(io.out!=null) io.out.close();
      }
      if(proxy==null){
        if(socket!=null)
	  socket.close();	  
      }
      else{
        proxy.close();	  
	proxy=null;
      }
    }
    catch(Exception e){
//      e.printStackTrace();
    }
    io=null;
    socket=null;
//    jsch.pool.removeElement(this);
  }

  public void setPortForwardingL(int lport, String host, int rport){
    PortWatcher pw=new PortWatcher(this, lport, host, rport);
    (new Thread(pw)).start();
  }

  public void setPortForwardingR(int rport, String host, int lport) throws JSchException{
    Buffer buf=new Buffer(100); // ??
    Packet packet=new Packet(buf);

    ChannelForwardedTCPIP.addPort(this, rport, host, lport);

    try{
      // byte SSH_MSG_GLOBAL_REQUEST 80
      // string "tcpip-forward"
      // boolean want_reply
      // string  address_to_bind
      // uint32  port number to bind
      packet.reset();
      buf.putByte((byte) SSH_MSG_GLOBAL_REQUEST);
      buf.putString("tcpip-forward".getBytes());
      buf.putByte((byte)0);
      buf.putString("0.0.0.0".getBytes());
      buf.putInt(rport);
      write(packet);
    }
    catch(Exception e){
      throw new JSchException(e.toString());
    }
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
    foo=jsch.getConfig(name);
    if(foo instanceof String) return (String)foo;
    return null;
  }
  public Channel getChannel(){ return channel; }
  public void setProxy(Proxy proxy){ this.proxy=proxy; }
  public void setHost(String host){ this.host=host; }
  public void setPort(int port){ this.port=port; }
  void setUserName(String foo){ this.username=foo; }
  public void setPassword(String foo){ this.password=foo; }
  public void setUserInfo(UserInfo userinfo){ this.userinfo=userinfo; }
  public void setInputStream(InputStream in){ this.in=in; }
  public void setOutputStream(OutputStream out){ this.out=out; }
  public void setX11Host(String host){ ChannelX11.setHost(host); }
  public void setX11Port(int port){ ChannelX11.setPort(port); }
  public void setX11Cookie(String cookie){ ChannelX11.setCookie(cookie); }
  public void setConfig(java.util.Properties foo){
    if(config==null) config=new java.util.Properties();
    for(java.util.Enumeration e=foo.keys() ; e.hasMoreElements() ;) {
      String key=(String)(e.nextElement());
      config.put(key, (String)(foo.get(key)));
    }
  }
  public void setSocketFactory(SocketFactory foo){ socket_factory=foo;}
  public boolean isConnected(){ return isConnected; }
}
