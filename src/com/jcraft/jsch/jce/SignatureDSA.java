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

import com.jcraft.jsch.Signature;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

public class SignatureDSA implements Signature{
  BigInteger dsa_p;
  BigInteger dsa_q;
  BigInteger dsa_g;
  BigInteger dsa_f;

  java.security.Signature signature;

  public void init() throws Exception{
    signature=java.security.Signature.getInstance("SHA1withDSA");
  }     

  public void setPubKey(byte[] pubkey) throws Exception{
    byte[] tmp;
    int i=0;
    int j=0;
    j=((pubkey[i++]<<24)&0xff000000)|((pubkey[i++]<<16)&0x00ff0000)|
	((pubkey[i++]<<8)&0x0000ff00)|((pubkey[i++])&0x000000ff);
    i+=j;
    j=((pubkey[i++]<<24)&0xff000000)|((pubkey[i++]<<16)&0x00ff0000)|
	((pubkey[i++]<<8)&0x0000ff00)|((pubkey[i++])&0x000000ff);
    tmp=new byte[j]; System.arraycopy(pubkey, i, tmp, 0, j); i+=j;
    dsa_p=new BigInteger(tmp);
    j=((pubkey[i++]<<24)&0xff000000)|((pubkey[i++]<<16)&0x00ff0000)|
	((pubkey[i++]<<8)&0x0000ff00)|((pubkey[i++])&0x000000ff);
    tmp=new byte[j]; System.arraycopy(pubkey, i, tmp, 0, j); i+=j;
    dsa_q=new BigInteger(tmp);
    j=((pubkey[i++]<<24)&0xff000000)|((pubkey[i++]<<16)&0x00ff0000)|
	((pubkey[i++]<<8)&0x0000ff00)|((pubkey[i++])&0x000000ff);
    tmp=new byte[j]; System.arraycopy(pubkey, i, tmp, 0, j); i+=j;
    dsa_g=new BigInteger(tmp);
    j=((pubkey[i++]<<24)&0xff000000)|((pubkey[i++]<<16)&0x00ff0000)|
	((pubkey[i++]<<8)&0x0000ff00)|((pubkey[i++])&0x000000ff);
    tmp=new byte[j]; System.arraycopy(pubkey, i, tmp, 0, j); i+=j;
    dsa_f=new BigInteger(tmp);
    DSAPublicKeySpec dsaPubKeySpec = 
	new DSAPublicKeySpec(dsa_f, dsa_p, dsa_q, dsa_g);
    KeyFactory keyFactory=KeyFactory.getInstance("DSA");
    PublicKey pubKey=keyFactory.generatePublic(dsaPubKeySpec);

    signature.initVerify(pubKey);
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
