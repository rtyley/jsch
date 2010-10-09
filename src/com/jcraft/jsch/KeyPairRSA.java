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

public class KeyPairRSA extends KeyPair{
  private byte[] prv_array;
  private byte[] pub_array;
  private byte[] n_array;

  private byte[] p_array;  // prime p
  private byte[] q_array;  // prime q
  private byte[] ep_array; // prime exponent p
  private byte[] eq_array; // prime exponent q
  private byte[] c_array;  // coefficient

  private int key_size=0;
  public KeyPairRSA(JSch jsch, int key_size) throws JSchException{
    super(jsch, key_size);
    this.key_size=key_size;
    try{
      Class c=Class.forName(jsch.getConfig("keypairgen.rsa"));
      KeyPairGenRSA keypairgen=(KeyPairGenRSA)(c.newInstance());
      keypairgen.init(key_size);
      pub_array=keypairgen.getE();
      prv_array=keypairgen.getD();
      n_array=keypairgen.getN();

      p_array=keypairgen.getP();
      q_array=keypairgen.getQ();
      ep_array=keypairgen.getEP();
      eq_array=keypairgen.getEQ();
      c_array=keypairgen.getC();

      keypairgen=null;
    }
    catch(Exception e){
      System.err.println("KeyPairRSA: "+e); 
      throw new JSchException(e.toString());
    }
  }

  private static final byte[] begin="-----BEGIN RSA PRIVATE KEY-----".getBytes();
  private static final byte[] end="-----END RSA PRIVATE KEY-----".getBytes();

  byte[] getBegin(){ return begin; }
  byte[] getEnd(){ return end; }

  byte[] getPrivateKey(){
    int content=
      1+countLength(1) + 1 +                           // INTEGER
      1+countLength(n_array.length) + n_array.length + // INTEGER  N
      1+countLength(pub_array.length) + pub_array.length + // INTEGER  pub
      1+countLength(prv_array.length) + prv_array.length+  // INTEGER  prv
      1+countLength(p_array.length) + p_array.length+      // INTEGER  p
      1+countLength(q_array.length) + q_array.length+      // INTEGER  q
      1+countLength(ep_array.length) + ep_array.length+    // INTEGER  ep
      1+countLength(eq_array.length) + eq_array.length+    // INTEGER  eq
      1+countLength(c_array.length) + c_array.length;      // INTEGER  c

    int total=
      1+countLength(content)+content;   // SEQUENCE

    byte[] plain=new byte[total];
    int index=0;
    index=writeSEQUENCE(plain, index, content);
    index=writeINTEGER(plain, index, new byte[1]);  // 0
    index=writeINTEGER(plain, index, n_array);
    index=writeINTEGER(plain, index, pub_array);
    index=writeINTEGER(plain, index, prv_array);
    index=writeINTEGER(plain, index, p_array);
    index=writeINTEGER(plain, index, q_array);
    index=writeINTEGER(plain, index, ep_array);
    index=writeINTEGER(plain, index, eq_array);
    index=writeINTEGER(plain, index, c_array);
    return plain;
  }

  byte[] getPublicKeyBlob(){
    Buffer buf=new Buffer(sshrsa.length+4+
			  pub_array.length+4+ 
			  n_array.length+4);
    buf.putString(sshrsa);
    buf.putString(pub_array);
    buf.putString(n_array);
    return buf.buffer;
  }

  private static final byte[] sshrsa="ssh-rsa".getBytes();
  byte[] getKeyTypeName(){return sshrsa;}
  public int getKeyType(){return RSA;}

  public int getKeySize(){return key_size; }
  public void dispose(){
    super.dispose();
    pub_array=null;
    prv_array=null;
    n_array=null;

    p_array=null;
    q_array=null;
    ep_array=null;
    eq_array=null;
    c_array=null;
  }
}
