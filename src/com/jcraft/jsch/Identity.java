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

import java.io.*;

class Identity{
  String identity;
  byte[] key;
  byte[] iv;
  private JSch jsch;
  private HASH hash;
  private byte[] encoded_data;

  private Cipher cipher;

  // DSA
  private byte[] P_array;    
  private byte[] Q_array;    
  private byte[] G_array;    
  private byte[] pub_array;    
  private byte[] prv_array;    
 
  // RSA
  private  byte[] n_array;   // modulus
  private  byte[] e_array;   // public exponent
  private  byte[] d_array;   // private exponent
 
//  private String algname="ssh-dss";
  private String algname="ssh-rsa";

  static final int RSA=0;
  static final int DSS=1;
  private int type=0;
  private byte[] publickeyblob=null;

  private boolean encrypted=true;

  Identity(String identity, JSch jsch){
    this.identity=identity;
    this.jsch=jsch;
    try{
      Class c;
      c=Class.forName(jsch.getConfig("3des-cbc"));
      cipher=(Cipher)(c.newInstance());
      key=new byte[cipher.getBlockSize()];   // 24
      iv=new byte[cipher.getIVSize()];       // 8
      c=Class.forName(jsch.getConfig("md5"));
      hash=(HASH)(c.newInstance());
      hash.init();
      File file=new File(identity);
      FileInputStream fis = new FileInputStream(identity);
      byte[] buf=new byte[(int)(file.length())];
      int len=fis.read(buf, 0, buf.length);
      fis.close();

      int i=0;
      while(i<len){
        if(buf[i]=='B'&& buf[i+1]=='E'&& buf[i+2]=='G'&& buf[i+3]=='I'){
          i+=6;	    
          if(buf[i]=='D'&& buf[i+1]=='S'&& buf[i+2]=='A'){ type=DSS; }
	  else if(buf[i]=='R'&& buf[i+1]=='S'&& buf[i+2]=='A'){ type=RSA; }
	  else{
            System.out.println("invalid format: "+identity);
	  }
          i+=2;
	  continue;
	}
        if(buf[i]=='C'&& buf[i+1]=='B'&& buf[i+2]=='C'&& buf[i+3]==','){
          i+=4;
	  for(int ii=0; ii<iv.length; ii++){
            iv[ii]=(byte)(((hexconv(buf[i++])<<4)&0xf0)+
			  (hexconv(buf[i++])&0xf));
  	  }
	  continue;
	}
	if(buf[i]==0x0a){
	  if(buf[i+1]==0x0a){ i+=2; break; }
	  boolean inheader=false;
	  for(int j=i+1; j<buf.length; j++){
	    if(buf[j]==0x0a) break;
	    if(buf[j]==':'){inheader=true; break;}
	  }
	  if(!inheader){
	    i++; 
	    encrypted=false;    // no passphrase
	    break;
	  }
	}
	i++;
      }
      int start=i;
      while(i<len){
        if(buf[i]==0x0a){
          System.arraycopy(buf, i+1, buf, i, len-i-1);
          len--;
          continue;
        }
        if(buf[i]=='-'){  break; }
        i++;
      }
      encoded_data=Util.fromBase64(buf, start, i-start);
//      if(encoded_data.length%8!=0){
//        byte[] foo=new byte[encoded_data.length/8*8];
//        System.arraycopy(encoded_data, 0, foo, 0, foo.length);
//        encoded_data=foo;
//      }

      file=new File(identity+".pub");
      fis=new FileInputStream(identity+".pub");
      buf=new byte[(int)(file.length())];
      len=fis.read(buf, 0, buf.length);
      fis.close();
      i=0;
      while(i<len){ if(buf[i]==' ')break; i++;} i++;
      if(i>=len) return;
      start=i;
      while(i<len){ if(buf[i]==' ')break; i++;}
      publickeyblob=Util.fromBase64(buf, start, i-start);
    }
    catch(Exception e){
      //System.out.println(e);
    }

  }

  String getAlgName(){
    if(type==RSA) return "ssh-rsa";
    return "ssh-dss"; 
  }

  boolean setPassphrase(String _passphrase) throws Exception{
    /*
      hash is MD5
      h(0) <- hash(passphrase, iv);
      h(n) <- hash(h(n-1), passphrase, iv);
      key <- (h(0),...,h(n))[0,..,key.length];
    */
    if(encrypted){
      if(_passphrase==null) return false;
      byte[] passphrase=_passphrase.getBytes();
      int hsize=hash.getBlockSize();
      byte[] hn=new byte[key.length/hsize*hsize+
			 (key.length%hsize==0?0:hsize)];
      byte[] tmp=null;
      for(int index=0; index+hsize<=hn.length;){
	if(tmp!=null){ hash.update(tmp, 0, tmp.length); }
	hash.update(passphrase, 0, passphrase.length);
	hash.update(iv, 0, iv.length);
	tmp=hash.digest();
	System.arraycopy(tmp, 0, hn, index, tmp.length);
	index+=tmp.length;
      }
      System.arraycopy(hn, 0, key, 0, key.length); 
    }
    if(decrypt()){
      encrypted=false;
      return true;
    }
    P_array=Q_array=G_array=pub_array=prv_array=null;
    return false;
  }

  byte[] getPublicKeyBlob(){
    if(publickeyblob!=null) return publickeyblob;
    if(type==RSA) return getPublicKeyBlob_rsa();
    return getPublicKeyBlob_dss();
  }

  byte[] getPublicKeyBlob_rsa(){
    if(e_array==null) return null;
    Buffer buf=new Buffer("ssh-rsa".length()+4+
			   e_array.length+4+ 
 			   n_array.length+4);
    buf.putString("ssh-rsa".getBytes());
    buf.putString(e_array);
    buf.putString(n_array);
    return buf.buffer;
  }

  byte[] getPublicKeyBlob_dss(){
    if(P_array==null) return null;
    Buffer buf=new Buffer("ssh-dss".length()+4+
			   P_array.length+4+ 
			   Q_array.length+4+ 
			   G_array.length+4+ 
			   pub_array.length+4);
    buf.putString("ssh-dss".getBytes());
    buf.putString(P_array);
    buf.putString(Q_array);
    buf.putString(G_array);
    buf.putString(pub_array);
    return buf.buffer;
  }

  byte[] getSignature(Session session, byte[] data){
    if(type==RSA) return getSignature_rsa(session, data);
    return getSignature_dss(session, data);
  }

  byte[] getSignature_rsa(Session session, byte[] data){
    try{      
      Class c=Class.forName(jsch.getConfig("signature.rsa"));
      SignatureRSA rsa=(SignatureRSA)(c.newInstance());

//      Class c=Class.forName(session.getConfig("signature.dsa"));
//      Class c=Class.forName(session.getConfig("signature.dsa"));
//      Signature dsa=(Signature)(c.newInstance());
//      SignatureRSA rsa=new com.jcraft.jsch.jce.SignatureRSA();

      rsa.init();
      rsa.setPrvKey(d_array, n_array);

      byte[] goo=new byte[4];
      goo[0]=(byte)(session.getSessionId().length>>>24);
      goo[1]=(byte)(session.getSessionId().length>>>16);
      goo[2]=(byte)(session.getSessionId().length>>>8);
      goo[3]=(byte)(session.getSessionId().length);
      rsa.update(goo);
      rsa.update(session.getSessionId());
      rsa.update(data);
      byte[] sig = rsa.sign();
      Buffer buf=new Buffer("ssh-rsa".length()+4+
			    sig.length+4);
      buf.putString("ssh-rsa".getBytes());
      buf.putString(sig);
      return buf.buffer;
    }
    catch(Exception e){
    }
    return null;
  }

  byte[] getSignature_dss(Session session, byte[] data){
    try{      
      Class c=Class.forName(jsch.getConfig("signature.dss"));
      SignatureDSA dsa=(SignatureDSA)(c.newInstance());
      dsa.init();
      dsa.setPrvKey(prv_array, P_array, Q_array, G_array);

      byte[] goo=new byte[4];
      goo[0]=(byte)(session.getSessionId().length>>>24);
      goo[1]=(byte)(session.getSessionId().length>>>16);
      goo[2]=(byte)(session.getSessionId().length>>>8);
      goo[3]=(byte)(session.getSessionId().length);
      dsa.update(goo);
      dsa.update(session.getSessionId());
      dsa.update(data);
      byte[] sig = dsa.sign();
      Buffer buf=new Buffer("ssh-dss".length()+4+
			    sig.length+4);
      buf.putString("ssh-dss".getBytes());
      buf.putString(sig);
      return buf.buffer;
    }
    catch(Exception e){
    }
    return null;
  }

  boolean decrypt(){
    if(type==RSA) return decrypt_rsa();
    return decrypt_dss();
  }

  boolean decrypt_rsa(){
    byte[] p_array;
    byte[] q_array;
    byte[] dmp1_array;
    byte[] dmq1_array;
    byte[] iqmp_array;

    try{
      byte[] plain;
      if(encrypted){
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        plain=new byte[encoded_data.length];
        cipher.update(encoded_data, 0, encoded_data.length, plain, 0);
      }
      else{
	if(n_array!=null) return true;
	plain=encoded_data;
      }

      int index=0;
      int length=0;

      if(plain[index]!=0x30)return false;
      index++; // SEQUENCE
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }

      if(plain[index]!=0x02)return false;
      index++; // INTEGER
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      n_array=new byte[length];
      System.arraycopy(plain, index, n_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      e_array=new byte[length];
      System.arraycopy(plain, index, e_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      d_array=new byte[length];
      System.arraycopy(plain, index, d_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      p_array=new byte[length];
      System.arraycopy(plain, index, p_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      q_array=new byte[length];
      System.arraycopy(plain, index, q_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      dmp1_array=new byte[length];
      System.arraycopy(plain, index, dmp1_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      dmq1_array=new byte[length];
      System.arraycopy(plain, index, dmq1_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      iqmp_array=new byte[length];
      System.arraycopy(plain, index, iqmp_array, 0, length);
      index+=length;
    }
    catch(Exception e){
      //System.out.println(e);
      return false;
    }
    return true;
  }

  boolean decrypt_dss(){
    try{
      byte[] plain;
      if(encrypted){
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        plain=new byte[encoded_data.length];
        cipher.update(encoded_data, 0, encoded_data.length, plain, 0);
      }
      else{
	if(P_array!=null) return true;
	plain=encoded_data;
      }

      int index=0;
      int length=0;

      if(plain[index]!=0x30)return false;
      index++; // SEQUENCE
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }

      if(plain[index]!=0x02)return false;
      index++; // INTEGER
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      P_array=new byte[length];
      System.arraycopy(plain, index, P_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      Q_array=new byte[length];
      System.arraycopy(plain, index, Q_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      G_array=new byte[length];
      System.arraycopy(plain, index, G_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      pub_array=new byte[length];
      System.arraycopy(plain, index, pub_array, 0, length);
      index+=length;

      index++;
      length=plain[index++]&0xff;
      if((length&0x80)!=0){
        int foo=length&0x7f; length=0;
        while(foo-->0){ length=(length<<8)+(plain[index++]&0xff); }
      }
      prv_array=new byte[length];
      System.arraycopy(plain, index, prv_array, 0, length);
      index+=length;
    }
    catch(Exception e){
      //System.out.println(e);
      e.printStackTrace();
      return false;
    }
    return true;
  }
  private byte hexconv(byte c){
    if('0'<=c&&c<='9') return (byte)(c-'0');
    return (byte)(c-'a'+10);
  }
  public boolean isEncrypted(){
    return encrypted;
  }
}
