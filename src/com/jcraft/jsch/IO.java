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

import java.io.*;

public class IO{
  InputStream in;
  OutputStream out;

  void setOutputStream(OutputStream out){
    this.out=out;
  }
  void setInputStream(InputStream in){
    this.in=in;
  }

  public void put(Packet p){
    try {
      out.write(p.buffer.buffer, 0, p.buffer.index);
      out.flush();
    }
    catch (IOException e) {
      System.out.println(e);
    }
  }
  void put(byte[] array, int begin, int length) {
    try{
      out.write(array, begin, length);
      out.flush();
    }
    catch(Exception e){
    }
  }

  int getByte() {
    int val = 0;
    try {
      val = in.read();
    }
    catch (IOException e) {
      System.err.println(e);
    }
    return val&0xff;
  }

  void getByte(byte[] array) {
    getByte(array, 0, array.length);
  }

  void getByte(byte[] array, int begin, int length) {
    try {
      do {
	int completed = in.read(array, begin, length);
	begin += completed;
	length -= completed;
      } while (length > 0);
    }
    catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
      System.out.println("array: "+array+", begin="+begin+", length="+length+",array.length="+array.length);
    }
  }

}
