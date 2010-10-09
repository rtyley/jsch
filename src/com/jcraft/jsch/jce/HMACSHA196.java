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

package com.jcraft.jsch.jce;

import com.jcraft.jsch.MAC;
import javax.crypto.*;
import javax.crypto.spec.*;

public class HMACSHA196 implements MAC{
  private String name="hmac-sha1-96";
  private int bsize=12;
  private Mac mac;
  private byte[] tmp=new byte[4];
  private byte[] buf=new byte[12];
  public int getBlockSize(){return bsize;};
  public void init(byte[] key) throws Exception{
    if(key.length>20){
      byte[] tmp=new byte[20];
      System.arraycopy(key, 0, tmp, 0, 20);	  
      key=tmp;
    }
    SecretKeySpec skey=new SecretKeySpec(key, "HmacSHA1");
    mac=Mac.getInstance("HmacSHA1");
    mac.init(skey);
  } 
  public void update(int i){
    tmp[0]=(byte)(i>>>24);
    tmp[1]=(byte)(i>>>16);
    tmp[2]=(byte)(i>>>8);
    tmp[3]=(byte)i;
    update(tmp, 0, 4);
  }
  public void update(byte foo[], int s, int l){
    mac.update(foo, s, l);      
  }
  public byte[] doFinal(){
    System.arraycopy(mac.doFinal(), 0, buf, 0, 12);
    return buf;
  }
  public String getName(){
    return name;
  }
}
