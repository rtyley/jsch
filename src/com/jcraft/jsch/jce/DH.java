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

package com.jcraft.jsch.jce;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

class DH implements com.jcraft.jsch.DH{
  BigInteger p;
  BigInteger g;
  BigInteger e;  // my public key
  byte[] e_array;
  BigInteger f;  // your public key
  BigInteger K;  // shared secret key
  byte[] K_array;

  private KeyPairGenerator myKpairGen;
  private KeyAgreement myKeyAgree;
  public void init() throws Exception{
    myKpairGen=KeyPairGenerator.getInstance("DH");
    myKeyAgree=KeyAgreement.getInstance("DH");
  }
  public byte[] getE() throws Exception{
    if(e==null){
      DHParameterSpec dhSkipParamSpec=new DHParameterSpec(p, g);
      myKpairGen.initialize(dhSkipParamSpec);
      KeyPair myKpair=myKpairGen.generateKeyPair();
      myKeyAgree.init(myKpair.getPrivate());
//    BigInteger x=((javax.crypto.interfaces.DHPrivateKey)(myKpair.getPrivate())).getX();
      byte[] myPubKeyEnc=myKpair.getPublic().getEncoded();
      e=((javax.crypto.interfaces.DHPublicKey)(myKpair.getPublic())).getY();
      e_array=e.toByteArray();
    }
    return e_array;
  }
  public byte[] getK() throws Exception{
    if(K==null){
      KeyFactory myKeyFac=KeyFactory.getInstance("DH");
      DHPublicKeySpec keySpec=new DHPublicKeySpec(f, p, g);
      PublicKey yourPubKey=myKeyFac.generatePublic(keySpec);
      myKeyAgree.doPhase(yourPubKey, true);
      byte[] mySharedSecret=myKeyAgree.generateSecret();
      K=new BigInteger(mySharedSecret);
      K_array=K.toByteArray();
    }
    return K_array;
  }
  public void setP(byte[] p){ setP(new BigInteger(p)); }
  public void setG(byte[] g){ setG(new BigInteger(g)); }
  public void setF(byte[] f){ setF(new BigInteger(f)); }
  void setP(BigInteger p){this.p=p;}
  void setG(BigInteger g){this.g=g;}
  void setF(BigInteger f){this.f=f;}
}
