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
