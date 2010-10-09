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

import java.io.FileOutputStream;

public abstract class KeyPair{
  public static final int DSA=0;
  public static final int RSA=1;

  private static final byte[] cr="\n".getBytes();

  public static KeyPair genKeyPair(JSch jsch, int type) throws JSchException{
    return genKeyPair(jsch, type, 1024);
  }
  public static KeyPair genKeyPair(JSch jsch, int type, int key_size) throws JSchException{
    if(type==DSA){
      return new KeyPairDSA(jsch, key_size);
    }
    else if(type==RSA){
      return new KeyPairRSA(jsch, key_size);
    }
    return null;
  }

  abstract byte[] getBegin();
  abstract byte[] getEnd();
  abstract int getKeySize();

  abstract byte[] getPrivateKey();

  private JSch jsch=null;
  private Cipher cipher;
  private HASH hash;
  private Random random;

  private byte[] passphrase;
  private byte[] iv;

  public KeyPair(JSch jsch, int key_size){
    this.jsch=jsch;
    this.cipher=genCipher();
    this.hash=genHash();
  }

  static byte[][] header={"Proc-Type: 4,ENCRYPTED".getBytes(),
			  "DEK-Info: DES-EDE3-CBC,".getBytes()};

  public void writePrivateKey(java.io.OutputStream out){
    byte[] plain=getPrivateKey();
    byte[] encoded=encrypt(plain);
    byte[] prv=Util.toBase64(encoded, 0, encoded.length);

    try{
      out.write(getBegin()); out.write(cr);
      if(passphrase!=null){
	out.write(header[0]); out.write(cr);
	out.write(header[1]); 
	for(int i=0; i<iv.length; i++){
	  out.write(b2a((byte)((iv[i]>>>4)&0x0f)));
	  out.write(b2a((byte)(iv[i]&0x0f)));
	}
        out.write(cr);
	out.write(cr);
      }
      int i=0;
      while(i<prv.length){
	if(i+64<prv.length){
	  out.write(prv, i, 64);
	  out.write(cr);
	  i+=64;
	  continue;
	}
	out.write(prv, i, prv.length-i);
	out.write(cr);
	break;
      }
      out.write(getEnd()); out.write(cr);
      //out.close();
    }
    catch(Exception e){
    }
  }

  private static byte[] space=" ".getBytes();

  abstract byte[] getPublicKeyBlob();
  abstract byte[] getKeyTypeName();
  public abstract int getKeyType();

  public void writePublicKey(java.io.OutputStream out, String comment){
    byte[] pubblob=getPublicKeyBlob();
    byte[] pub=Util.toBase64(pubblob, 0, pubblob.length);
    try{
      out.write(getKeyTypeName()); out.write(space);
      out.write(pub, 0, pub.length); out.write(space);
      out.write(comment.getBytes());
      out.write(cr);
      out.close();
    }
    catch(Exception e){
    }
  }

  public void writePublicKey(String name, String comment) throws java.io.FileNotFoundException, java.io.IOException{
    FileOutputStream fos=new FileOutputStream(name);
    writePublicKey(fos, comment);
    fos.close();
  }
  public void writePrivateKey(String name) throws java.io.FileNotFoundException, java.io.IOException{
    FileOutputStream fos=new FileOutputStream(name);
    writePrivateKey(fos);
    fos.close();
  }

  public String getFingerPrint(){
    HASH hash=null;
    try{
      Class c=Class.forName(jsch.getConfig("md5"));
      hash=(HASH)(c.newInstance());
    }
    catch(Exception e){ System.err.println("getFingerPrint: "+e); }
    return getKeySize()+" "+Util.getFingerPrint(hash, getPublicKeyBlob());
  }

  private byte[] encrypt(byte[] plain){
    if(passphrase==null) return plain;
    iv=new byte[cipher.getIVSize()];

    random=genRandom();
    random.fill(iv, 0, iv.length);

    byte[] key=genKey(passphrase, iv);
    byte[] encoded=plain;
    int bsize=cipher.getBlockSize();
    if(encoded.length%bsize!=0){
      byte[] foo=new byte[(encoded.length/bsize+1)*bsize];
      System.arraycopy(encoded, 0, foo, 0, encoded.length);
      encoded=foo;
    }

    try{
      cipher.init(Cipher.ENCRYPT_MODE, key, iv);
      cipher.update(encoded, 0, encoded.length, encoded, 0);
    }
    catch(Exception e){
      //System.out.println(e);
    }
    return encoded;
  }

  int writeSEQUENCE(byte[] buf, int index, int len){
    buf[index++]=0x30;
    index=writeLength(buf, index, len);
    return index;
  }
  int writeINTEGER(byte[] buf, int index, byte[] data){
    buf[index++]=0x02;
    index=writeLength(buf, index, data.length);
    System.arraycopy(data, 0, buf, index, data.length);
    index+=data.length;
    return index;
  }

  int countLength(int len){
    int i=1;
    if(len<=0x7f) return i;
    while(len>0){
      len>>>=8;
      i++;
    }
    return i;
  }

  int writeLength(byte[] data, int index, int len){
    int i=countLength(len)-1;
    if(i==0){
      data[index++]=(byte)len;
      return index;
    }
    data[index++]=(byte)(0x80|i);
    int j=index+i;
    while(i>0){
      data[index+i-1]=(byte)(len&0xff);
      len>>>=8;
      i--;
    }
    return j;
  }

  private Random genRandom(){
    if(random==null){
      try{
	Class c=Class.forName(jsch.getConfig("random"));
        random=(Random)(c.newInstance());
      }
      catch(Exception e){ System.err.println("connect: random "+e); }
    }
    return random;
  }

  private HASH genHash(){
    try{
      Class c=Class.forName(jsch.getConfig("md5"));
      hash=(HASH)(c.newInstance());
      hash.init();
    }
    catch(Exception e){
    }
    return hash;
  }
  private Cipher genCipher(){
    try{
      Class c;
      c=Class.forName(jsch.getConfig("3des-cbc"));
      cipher=(Cipher)(c.newInstance());
    }
    catch(Exception e){
    }
    return cipher;
  }

  /*
    hash is MD5
    h(0) <- hash(passphrase, iv);
    h(n) <- hash(h(n-1), passphrase, iv);
    key <- (h(0),...,h(n))[0,..,key.length];
  */
  synchronized byte[] genKey(byte[] passphrase, byte[] iv){
    byte[] key=new byte[cipher.getBlockSize()];
    int hsize=hash.getBlockSize();
    byte[] hn=new byte[key.length/hsize*hsize+
		       (key.length%hsize==0?0:hsize)];
    try{
      byte[] tmp=null;
      // OPENSSH
      for(int index=0; index+hsize<=hn.length;){
	if(tmp!=null){ hash.update(tmp, 0, tmp.length); }
	hash.update(passphrase, 0, passphrase.length);
	hash.update(iv, 0, iv.length);
	tmp=hash.digest();
	System.arraycopy(tmp, 0, hn, index, tmp.length);
	index+=tmp.length;
      }
      System.arraycopy(hn, 0, key, 0, key.length); 

    /*
    // FSECURE
    for(int index=0; index+hsize<=hn.length;){
      if(tmp!=null){ hash.update(tmp, 0, tmp.length); }
      hash.update(passphrase, 0, passphrase.length);
      tmp=hash.digest();
      System.arraycopy(tmp, 0, hn, index, tmp.length);
      index+=tmp.length;
    }
    System.arraycopy(hn, 0, key, 0, key.length); 
    */

    }
    catch(Exception e){
      System.out.println(e);
    }
    return key;
  } 

  public void setPassphrase(String passphrase){
    if(passphrase==null || passphrase.length()==0){
      setPassphrase((byte[])null);
    }
    else{
      setPassphrase(passphrase.getBytes());
    }
  }
  public void setPassphrase(byte[] passphrase){
    this.passphrase=passphrase;
    iv=null;
  }

  private byte a2b(byte c){
    if('0'<=c&&c<='9') return (byte)(c-'0');
    return (byte)(c-'a'+10);
  }
  private byte b2a(byte c){
    if(0<=c&&c<=9) return (byte)(c+'0');
    return (byte)(c-10+'A');
  }

  public void dispose(){
    passphrase=null;
    iv=null;
  }
}
