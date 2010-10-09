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

import java.security.SecureRandom;

public class Random implements com.jcraft.jsch.Random{
  private byte[] tmp=new byte[16];
  private SecureRandom random;
  public Random(){
    try{ random=SecureRandom.getInstance("SHA1PRNG"); }
    catch(Exception e){ System.out.println(e); }
  }
  public void fill(byte[] foo, int start, int len){
    if(len>tmp.length){ tmp=new byte[len]; }
    random.nextBytes(tmp);
    System.arraycopy(tmp, 0, foo, start, len);
  }
}
