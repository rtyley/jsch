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

public class KeyPairDSA extends KeyPair{
  private byte[] P_array;
  private byte[] Q_array;
  private byte[] G_array;
  private byte[] pub_array;
  private byte[] prv_array;
  public KeyPairDSA(JSch jsch, int key_size) throws JSchException{
    super(jsch, key_size);
    try{
      Class c=Class.forName(jsch.getConfig("keypairgen.dsa"));
      KeyPairGenDSA keypairgen=(KeyPairGenDSA)(c.newInstance());
      keypairgen.init(key_size);
      P_array=keypairgen.getP();
      Q_array=keypairgen.getQ();
      G_array=keypairgen.getG();
      pub_array=keypairgen.getY();
      prv_array=keypairgen.getX();
      keypairgen=null;
    }
    catch(Exception e){
      System.err.println("KeyPairDSA: "+e); 
      throw new JSchException(e.toString());
    }
  }

  private static final byte[] begin="-----BEGIN DSA PRIVATE KEY-----".getBytes();
  private static final byte[] end="-----END DSA PRIVATE KEY-----".getBytes();

  byte[] getBegin(){ return begin; }
  byte[] getEnd(){ return end; }

  byte[] getPrivateKey(){
    int content=
      1+countLength(1) + 1 +                           // INTEGER
      1+countLength(P_array.length) + P_array.length + // INTEGER  P
      1+countLength(Q_array.length) + Q_array.length + // INTEGER  Q
      1+countLength(G_array.length) + G_array.length + // INTEGER  G
      1+countLength(pub_array.length) + pub_array.length + // INTEGER  pub
      1+countLength(prv_array.length) + prv_array.length;  // INTEGER  prv

    int total=
      1+countLength(content)+content;   // SEQUENCE

    byte[] plain=new byte[total];
    int index=0;
    index=writeSEQUENCE(plain, index, content);
    index=writeINTEGER(plain, index, new byte[1]);  // 0
    index=writeINTEGER(plain, index, P_array);
    index=writeINTEGER(plain, index, Q_array);
    index=writeINTEGER(plain, index, G_array);
    index=writeINTEGER(plain, index, pub_array);
    index=writeINTEGER(plain, index, prv_array);
    return plain;
  }

  byte[] getPublicKeyBlob(){
    Buffer buf=new Buffer(sshdss.length+4+
			  P_array.length+4+ 
			  Q_array.length+4+ 
			  G_array.length+4+ 
			  pub_array.length+4);
    buf.putString(sshdss);
    buf.putString(P_array);
    buf.putString(Q_array);
    buf.putString(G_array);
    buf.putString(pub_array);
    return buf.buffer;
  }

  private static final byte[] sshdss="ssh-dss".getBytes();
  byte[] getType(){return sshdss;}

  public void dispose(){
    super.dispose();
    P_array=null;
    Q_array=null;
    G_array=null;
    pub_array=null;
    prv_array=null;
  }
}
