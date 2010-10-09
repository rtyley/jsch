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

public class Packet{

  private static Random random;
  static void setRandom(Random foo){ random=foo;}

  Buffer buffer;
  byte[] tmp=new byte[4]; 
  public Packet(Buffer buffer){
    this.buffer=buffer;
  }
  public void reset(){
    buffer.index=5;
  }
  void padding(){
    int len=buffer.index;
    int pad=(-len)&7;
    if(pad<8){
      pad+=8;
    }
    len=len+pad-4;
    tmp[0]=(byte)(len>>>24);
    tmp[1]=(byte)(len>>>16);
    tmp[2]=(byte)(len>>>8);
    tmp[3]=(byte)(len);
    System.arraycopy(tmp, 0, buffer.buffer, 0, 4);
    buffer.buffer[4]=(byte)pad;
    random.fill(buffer.buffer, buffer.index, pad); buffer.skip(pad);
    //buffer.putPad(pad);
/*
for(int i=0; i<buffer.index; i++){
  System.out.print(Integer.toHexString(buffer.buffer[i]&0xff)+":");
}
System.out.println("");
*/
  }

}
