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

package com.jcraft.jsch.jce;

import com.jcraft.jsch.*;

public class DHG1 extends KeyExchange{

  static final byte[] g={ 2 };
  static final byte[] p={
(byte)0x00,
(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF, 
(byte)0xC9,(byte)0x0F,(byte)0xDA,(byte)0xA2,(byte)0x21,(byte)0x68,(byte)0xC2,(byte)0x34,
(byte)0xC4,(byte)0xC6,(byte)0x62,(byte)0x8B,(byte)0x80,(byte)0xDC,(byte)0x1C,(byte)0xD1,
(byte)0x29,(byte)0x02,(byte)0x4E,(byte)0x08,(byte)0x8A,(byte)0x67,(byte)0xCC,(byte)0x74,
(byte)0x02,(byte)0x0B,(byte)0xBE,(byte)0xA6,(byte)0x3B,(byte)0x13,(byte)0x9B,(byte)0x22,
(byte)0x51,(byte)0x4A,(byte)0x08,(byte)0x79,(byte)0x8E,(byte)0x34,(byte)0x04,(byte)0xDD,
(byte)0xEF,(byte)0x95,(byte)0x19,(byte)0xB3,(byte)0xCD,(byte)0x3A,(byte)0x43,(byte)0x1B,
(byte)0x30,(byte)0x2B,(byte)0x0A,(byte)0x6D,(byte)0xF2,(byte)0x5F,(byte)0x14,(byte)0x37,
(byte)0x4F,(byte)0xE1,(byte)0x35,(byte)0x6D,(byte)0x6D,(byte)0x51,(byte)0xC2,(byte)0x45,
(byte)0xE4,(byte)0x85,(byte)0xB5,(byte)0x76,(byte)0x62,(byte)0x5E,(byte)0x7E,(byte)0xC6,
(byte)0xF4,(byte)0x4C,(byte)0x42,(byte)0xE9,(byte)0xA6,(byte)0x37,(byte)0xED,(byte)0x6B,
(byte)0x0B,(byte)0xFF,(byte)0x5C,(byte)0xB6,(byte)0xF4,(byte)0x06,(byte)0xB7,(byte)0xED,
(byte)0xEE,(byte)0x38,(byte)0x6B,(byte)0xFB,(byte)0x5A,(byte)0x89,(byte)0x9F,(byte)0xA5,
(byte)0xAE,(byte)0x9F,(byte)0x24,(byte)0x11,(byte)0x7C,(byte)0x4B,(byte)0x1F,(byte)0xE6,
(byte)0x49,(byte)0x28,(byte)0x66,(byte)0x51,(byte)0xEC,(byte)0xE6,(byte)0x53,(byte)0x81,
(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF
};

  static final int RSA=0;
  static final int DSS=1;
  private int type=0;

  com.jcraft.jsch.DH dh;
  HASH sha;

  byte[] K;
  byte[] H;

  byte[] V_S;
  byte[] V_C;
  byte[] I_S;
  byte[] I_C;

  byte[] K_S;

  public void init(byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws Exception{
    this.V_S=V_S;      
    this.V_C=V_C;      
    this.I_S=I_S;      
    this.I_C=I_C;      

    sha=new SHA1();
    sha.init();
  }

  public boolean start(Session session) throws Exception{

    try{
      Class c=Class.forName(session.getConfig("dh"));
      dh=(com.jcraft.jsch.DH)(c.newInstance());
      dh.init();
    }
    catch(Exception e){
      System.err.println(e);
    }

    int i,j;
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);

    dh.setP(p);
    dh.setG(g);

    // The client responds with:
    // byte  SSH_MSG_KEXDH_INIT(30)
    // mpint e <- g^x mod p
    //         x is a random number (1 < x < (p-1)/2)

    byte[] e=dh.getE();

    packet.reset();
    buf.putByte((byte)30);
    buf.putMPInt(e);
    session.write(packet);

    // The server responds with:
    // byte      SSH_MSG_KEXDH_REPLY(31)
    // string    server public host key and certificates (K_S)
    // mpint     f
    // string    signature of H
    buf=session.read(buf);
    j=buf.getInt();
    j=buf.getByte();
    j=buf.getByte();
if(j!=31){
System.err.println("type: must be 31 "+j);
return false;
}

    K_S=buf.getString();
    // K_S is server_key_blob, which includes ....
    // string ssh-dss
    // impint p of dsa
    // impint q of dsa
    // impint g of dsa
    // impint pub_key of dsa
    //System.out.print("K_S: "); dump(K_S, 0, K_S.length);

    byte[] f=buf.getMPInt();
    byte[] sig_of_H=buf.getString();

    dh.setF(f);
    K=dh.getK();

    //The hash H is computed as the HASH hash of the concatenation of the
    //following:
    // string    V_C, the client's version string (CR and NL excluded)
    // string    V_S, the server's version string (CR and NL excluded)
    // string    I_C, the payload of the client's SSH_MSG_KEXINIT
    // string    I_S, the payload of the server's SSH_MSG_KEXINIT
    // string    K_S, the host key
    // mpint     e, exchange value sent by the client
    // mpint     f, exchange value sent by the server
    // mpint     K, the shared secret
    // This value is called the exchange hash, and it is used to authenti-
    // cate the key exchange.

    buf.reset();
    buf.putString(V_C); buf.putString(V_S);
    buf.putString(I_C); buf.putString(I_S);
    buf.putString(K_S);
    buf.putMPInt(e); buf.putMPInt(f);
    buf.putMPInt(K);

    byte[] foo=new byte[buf.getLength()];
    buf.getByte(foo);
    sha.update(foo, 0, foo.length);

    H=sha.digest();

    // System.out.print("H -> "); dump(H, 0, H.length);

    i=0;
    j=0;
    j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
      ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
    String alg=new String(K_S, i, j);
    i+=j;

    if(alg.equals("ssh-rsa")){
      byte[] tmp;
      byte[] ee;
      byte[] n;

      type=RSA;

      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      ee=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      n=tmp;

      SignatureRSA sig=new SignatureRSA();
      sig.init();
      sig.setPubKey(ee, n);   
      sig.update(H);
      return sig.verify(sig_of_H);
    }
    else if(alg.equals("ssh-dss")){
      byte[] q=null;
      byte[] tmp;
      byte[] p;
      byte[] g;

      type=DSS;

      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      p=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      q=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      g=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      f=tmp;

      SignatureDSA sig=new SignatureDSA();
      sig.init();
      sig.setPubKey(f, p, q, g);   
      sig.update(H);
      return sig.verify(sig_of_H);
    }
    else{
	System.out.println("unknow alg");
	return false;
    }	    

  }

  public byte[] getK(){ return K; }
  public byte[] getH(){ return H; }
  public HASH getHash(){ return sha; }
  public byte[] getHostKey(){ return K_S; }

  static String[] chars={
    "0","1","2","3","4","5","6","7","8","9", "a","b","c","d","e","f"
  };
  public String getFingerPrint(){
    try{
      java.security.MessageDigest md=java.security.MessageDigest.getInstance("MD5");
      md.update(K_S, 0, K_S.length);
      byte[] foo=md.digest();
      StringBuffer sb=new StringBuffer();
      int bar;
      for(int i=0; i<foo.length;i++){
        bar=foo[i]&0xff;
        sb.append(chars[(bar>>>4)&0xf]);
        sb.append(chars[(bar)&0xf]);
        if(i+1<foo.length)
          sb.append(":");
      }
      return sb.toString();
    }
    catch(Exception e){
      return "???";
    }
  }
  public String getKeyType(){
    if(type==DSS) return "DSA";
    return "RSA";
  }
}
