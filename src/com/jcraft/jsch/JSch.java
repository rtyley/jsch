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

public class JSch{
  static java.util.Properties config=new java.util.Properties();
  static{
    config.put("random", "com.jcraft.jsch.jce.Random");
    config.put("kex", "com.jcraft.jsch.jce.DHGEX");
    config.put("dh", "com.jcraft.jsch.jce.DH");
    config.put("cipher.s2c", "com.jcraft.jsch.jce.BlowfishCBC");
    config.put("cipher.c2s", "com.jcraft.jsch.jce.BlowfishCBC");
    config.put("mac.s2c", "com.jcraft.jsch.jce.HMACMD5");
    config.put("mac.c2s", "com.jcraft.jsch.jce.HMACMD5");
    config.put("compress.s2c", "none");
    config.put("compress.c2s", "none");
  }
  private static java.util.Vector pool=new java.util.Vector();
  public JSch(){
  }
  public Session getSession(String host){ return getSession(host, 22); }
  public Session getSession(String host, int port){
    Session s=new Session(); 
    s.setHost(host);
    s.setPort(port);
    pool.addElement(s);
    return s;
  }

  static boolean isKnownHost(String host, byte[] key){
    return false;
  }
}
