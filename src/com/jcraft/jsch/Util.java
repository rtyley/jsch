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
      k=(buf[j]&0x03)<<4|(buf[j+1]>>>4)&0x0f;
      tmp[i++]=b64[k];
      k=(buf[j+1]&0x0f)<<2|(buf[j+2]>>>6)&0x03;
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
      k=(buf[j]&0x03)<<4|(buf[j+1]>>>4)&0x0f;
      tmp[i++]=b64[k];
      k=((buf[j+1]&0x0f)<<2)&0x3f;
      tmp[i++]=b64[k];
      tmp[i++]=(byte)'=';
    }
    byte[] bar=new byte[i];
    System.arraycopy(tmp, 0, bar, 0, i);
    return bar;

//    return sun.misc.BASE64Encoder().encode(buf);
  }
}
