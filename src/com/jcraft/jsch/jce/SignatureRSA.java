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

public class SignatureRSA implements com.jcraft.jsch.SignatureRSA{

  java.security.Signature signature;
  KeyFactory keyFactory;

  public void init() throws Exception{
    signature=java.security.Signature.getInstance("SHA1withRSA");
    keyFactory=KeyFactory.getInstance("RSA");
  }     
  public void setPubKey(byte[] e, byte[] n) throws Exception{
    RSAPublicKeySpec rsaPubKeySpec = 
	new RSAPublicKeySpec(new BigInteger(n),
			     new BigInteger(e));
    PublicKey pubKey=keyFactory.generatePublic(rsaPubKeySpec);
    signature.initVerify(pubKey);
  }
  public void setPrvKey(byte[] d, byte[] n) throws Exception{
    RSAPrivateKeySpec rsaPrivKeySpec = 
	new RSAPrivateKeySpec(new BigInteger(n),
			      new BigInteger(d));
    PrivateKey prvKey = keyFactory.generatePrivate(rsaPrivKeySpec);
    signature.initSign(prvKey);
  }
  public byte[] sign() throws Exception{
    byte[] sig=signature.sign();      
    return sig;
  }
  public void update(byte[] foo) throws Exception{
   signature.update(foo);
  }
  public boolean verify(byte[] sig) throws Exception{
    int i=0;
    int j=0;
    j=((sig[i++]<<24)&0xff000000)|((sig[i++]<<16)&0x00ff0000)|
	((sig[i++]<<8)&0x0000ff00)|((sig[i++])&0x000000ff);
    i+=j;
    j=((sig[i++]<<24)&0xff000000)|((sig[i++]<<16)&0x00ff0000)|
	((sig[i++]<<8)&0x0000ff00)|((sig[i++])&0x000000ff);
    byte[] tmp=new byte[j]; 
    System.arraycopy(sig, i, tmp, 0, j); sig=tmp;
    return signature.verify(sig);
  }
}
