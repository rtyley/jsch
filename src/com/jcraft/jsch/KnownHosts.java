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

import java.io.*;

class KnownHosts{
  private static final String _known_hosts="known_hosts";

  static final int OK=0;
  static final int NOT_INCLUDED=1;
  static final int CHANGED=2;

  static final int SSHDSS=0;
  static final int SSHRSA=1;
  private String known_hosts=null;

  private java.util.Vector pool=new java.util.Vector();

  void setKnownHosts(String foo){
    known_hosts=foo;
    StringBuffer sb=new StringBuffer();
    int i;
    try{
      FileInputStream fis=new FileInputStream(known_hosts);
      String host;
      String key;
      int type;
loop:
      while(true){
        sb.setLength(0);
        while(true){
          i=fis.read();
	  if(i==-1) break loop;
          if(i==0x20){
            host=sb.toString();
            break;
	  }
          sb.append((char)i);
	}

        sb.setLength(0);
        while(true){
          i=fis.read();
	  if(i==-1) break loop;
          if(i==0x20){
            if(sb.toString().equals("ssh-dss")) type=SSHDSS;
	    else type=SSHRSA;
            break;
	  }
          sb.append((char)i);
	}

        sb.setLength(0);
        while(true){
          i=fis.read();
	  if(i==-1) break loop;
          if(i==0x0a){
            key=sb.toString();
            break;
	  }
          sb.append((char)i);
	}
	/*
System.out.println(host);
System.out.println("|"+key+"|");
	*/
	HostKey hk = new HostKey(host, type, 
				 Util.fromBase64(key.getBytes(), 0, 
						 key.length()));
        pool.addElement(hk);
      }
    }
    catch(Exception e){
    }
  }

  String getKnownHosts(){ return known_hosts; }

  int check(String host, byte[] key){
    String foo; 
    byte[] bar;
    HostKey hk;
    int type=getType(key);
    for(int i=0; i<pool.size(); i++){
      hk=(HostKey)(pool.elementAt(i));
      if(isIncluded(hk.host, host) && hk.type==type){
        if(java.util.Arrays.equals(hk.key, key)){
	  //System.out.println("find!!");
          return OK;
	}
	else{
          return CHANGED;
	}
      }
    }
    //System.out.println("fail!!");
    return NOT_INCLUDED;
  }

  void insert(String host, byte[] key){
    HostKey hk;
    int type=getType(key);
    for(int i=0; i<pool.size(); i++){
      hk=(HostKey)(pool.elementAt(i));
      if(isIncluded(hk.host, host) && hk.type==type){
/*
        if(java.util.Arrays.equals(hk.key, key)){
          return;
	}
        if(hk.host.equals(host)){
          hk.key=key;
          return;
	}
	else{
          hk.host=deleteSubString(hk.host, host);
	  break;
	}
*/
      }
    }
    hk=new HostKey(host, type, key);
    pool.addElement(hk);
  }
  private int getType(byte[] key){
    if(key[8]=='d') return SSHDSS;
    return SSHRSA;
  }
  private String deleteSubString(String hosts, String host){
    int i=0;
    int hostlen=host.length();
    int hostslen=hosts.length();
    int j;
    while(i<hostslen){
      j=hosts.indexOf(',', i);
      if(j==-1) break;
      if(!host.equals(hosts.substring(i, j))){
        i=j+1;	  
        continue;
      }
      return hosts.substring(0, i)+hosts.substring(j+1);
    }
    if(hosts.endsWith(host) && hostslen-i==hostlen){
      return hosts.substring(0, (hostlen==hostslen) ? 0 :hostslen-hostlen-1);
    }
    return hosts;
  }
  private boolean isIncluded(String hosts, String host){
    int i=0;
    int hostlen=host.length();
    int hostslen=hosts.length();
    int j;
    while(i<hostslen){
      j=hosts.indexOf(',', i);
      if(j==-1){
       if(hostlen!=hostslen-i) return false;
        return hosts.regionMatches(true, i, host, 0, hostlen);
      }
      if(hostlen==(j-i)){
        if(hosts.regionMatches(true, i, host, 0, hostlen)) return true;
      }
      i=j+1;
    }
    return false;
  }

    static final byte[] space={(byte)0x20};
    static final byte[] sshdss="ssh-dss".getBytes();
    static final byte[] sshrsa="ssh-rsa".getBytes();
    static final byte[] cr="\n".getBytes();

  class HostKey{
    String host;
    int type;
    byte[] key;
    HostKey(String host, int type, byte[] key){
      this.host=host; this.type=type; this.key=key;
    }
    void dump(OutputStream out) throws IOException{
      out.write(host.getBytes());
      out.write(space);
      if(type==SSHDSS) out.write(sshdss);
      else  out.write(sshrsa);
      out.write(space);
      out.write(Util.toBase64(key, 0, key.length));
      out.write(cr);
    }
  }
}
