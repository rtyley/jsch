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

class Util{

  private static final byte[] b64 ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".getBytes();
  private static byte val(byte foo){
    if(foo == '=') return 0;
    for(int j=0; j<b64.length; j++){
      if(foo==b64[j]) return (byte)j;
    }
    return 0;
  }
  static byte[] fromBase64(byte[] buf, int start, int length){
    byte[] foo=new byte[length];
    int j=0;
    int l=length;
    for (int i=start;i<start+length;i+=4){
      foo[j]=(byte)((val(buf[i])<<2)|((val(buf[i+1])&0x30)>>>4));
      if(buf[i+2]==(byte)'='){ j++; break;}
      foo[j+1]=(byte)(((val(buf[i+1])&0x0f)<<4)|((val(buf[i+2])&0x3c)>>>2));
      if(buf[i+3]==(byte)'='){ j+=2; break;}
      foo[j+2]=(byte)(((val(buf[i+2])&0x03)<<6)|(val(buf[i+3])&0x3f));
      j+=3;
    }
    byte[] bar=new byte[j];
    System.arraycopy(foo, 0, bar, 0, j);
    return bar;
  }
  static byte[] toBase64(byte[] buf, int start, int length){
    byte[] tmp=new byte[length*2];
    int i,j,k;
    
    int foo=(length/3)*3+start;
    i=0;
    for(j=start; j<foo; j+=3){
      k=(buf[j]>>>2)&0x3f;
      tmp[i++]=b64[k];
      k=(buf[j]&0x03)<<4|(buf[j+1]>>>4)&0x3f;
      tmp[i++]=b64[k];
      k=(buf[j+1]&0x0f)<<2|(buf[j+2]>>>6)&0x3f;
      tmp[i++]=b64[k];
      k=buf[j+2]&0x3f;
      tmp[i++]=b64[k];
    }

    foo=(start+length)-foo;
    if(foo==1){
      k=(buf[j]>>>2)&0x3f;
      tmp[i++]=b64[k];
      k=((buf[j]&0x03)<<4)&0x3f;
      tmp[i++]=b64[k];
      tmp[i++]=(byte)'=';
      tmp[i++]=(byte)'=';
    }
    else if(foo==2){
      k=(buf[j]>>>2)&0x3f;
      tmp[i++]=b64[k];
      k=(buf[j]&0x03)<<4|(buf[j+1]>>>4)&0x3f;
      tmp[i++]=b64[k];
      k=((buf[j+1]&0x0f)<<2)&0x3f;
      tmp[i++]=b64[k];
      tmp[i++]=(byte)'=';
    }
    byte[] bar=new byte[i];
    System.arraycopy(tmp, 0, bar, 0, i);
    return bar;
  }
}
