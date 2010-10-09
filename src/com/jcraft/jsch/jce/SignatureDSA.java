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

public class SignatureDSA implements com.jcraft.jsch.SignatureDSA{

  java.security.Signature signature;
  KeyFactory keyFactory;

  public void init() throws Exception{
    signature=java.security.Signature.getInstance("SHA1withDSA");
    keyFactory=KeyFactory.getInstance("DSA");
  }     
  public void setPubKey(byte[] y, byte[] p, byte[] q, byte[] g) throws Exception{
    DSAPublicKeySpec dsaPubKeySpec = 
	new DSAPublicKeySpec(new BigInteger(y),
			     new BigInteger(p),
			     new BigInteger(q),
			     new BigInteger(g));
    PublicKey pubKey=keyFactory.generatePublic(dsaPubKeySpec);
    signature.initVerify(pubKey);
  }
  public void setPrvKey(byte[] x, byte[] p, byte[] q, byte[] g) throws Exception{
    DSAPrivateKeySpec dsaPrivKeySpec = 
	new DSAPrivateKeySpec(new BigInteger(x),
			      new BigInteger(p),
			      new BigInteger(q),
			      new BigInteger(g));
    PrivateKey prvKey = keyFactory.generatePrivate(dsaPrivKeySpec);
    signature.initSign(prvKey);
  }
  public byte[] sign() throws Exception{
    byte[] sig=signature.sign();      
/*
System.out.print("sign["+sig.length+"] ");
for(int i=0; i<sig.length;i++){
System.out.print(Integer.toHexString(sig[i]&0xff)+":");
}
System.out.println("");
*/
    // sig is in ASN.1
    // SEQUENCE::={ r INTEGER, s INTEGER }
    int len=0;	
    int index=3;
    len=sig[index++]&0xff;
//System.out.println("! len="+len);
    byte[] r=new byte[len];
    System.arraycopy(sig, index, r, 0, r.length);
    index=index+len+1;
    len=sig[index++]&0xff;
//System.out.println("!! len="+len);
    byte[] s=new byte[len];
    System.arraycopy(sig, index, s, 0, s.length);

    byte[] result=new byte[40];

    // result must be 40 bytes, but length of r and s may not be 20 bytes  

    System.arraycopy(r, (r.length>20)?1:0,
		     result, (r.length>20)?0:20-r.length,
		     (r.length>20)?20:r.length);
    System.arraycopy(s, (s.length>20)?1:0,
		     result, (s.length>20)?20:40-s.length,
		     (s.length>20)?20:s.length);
 
//  System.arraycopy(sig, (sig[3]==20?4:5), result, 0, 20);
//  System.arraycopy(sig, sig.length-20, result, 20, 20);

    return result;
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

    // ASN.1
    tmp=new byte[sig.length+6];
    tmp[0]=(byte)0x30; tmp[1]=(byte)0x2c; 
    tmp[2]=(byte)0x02; tmp[3]=(byte)0x14;
    System.arraycopy(sig, 0, tmp, 4, 20);
    tmp[24]=(byte)0x02; tmp[25]=(byte)0x14;
    System.arraycopy(sig, 20, tmp, 26, 20); sig=tmp;
    
    return signature.verify(sig);
  }
}
