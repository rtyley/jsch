/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2002,2003,2004 ymnk, JCraft,Inc. All rights reserved.

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

import java.io.*;

public
class KnownHosts{
  private static final String _known_hosts="known_hosts";

  static final int OK=0;
  static final int NOT_INCLUDED=1;
  static final int CHANGED=2;

  static final int SSHDSS=0;
  static final int SSHRSA=1;

  private JSch jsch=null;
  private String known_hosts=null;
  private java.util.Vector pool=null;

  KnownHosts(JSch jsch){
    super();
    this.jsch=jsch;
    pool=new java.util.Vector();
  }

  void setKnownHosts(String foo) throws JSchException{
    try{
      known_hosts=foo;
      FileInputStream fis=new FileInputStream(foo);
      setKnownHosts(fis);
    }
    catch(FileNotFoundException e){
    } 
  }
  void setKnownHosts(InputStream foo) throws JSchException{
    pool.removeAllElements();
    StringBuffer sb=new StringBuffer();
    int i;
    boolean error=false;
    try{
      InputStream fis=foo;
      String host;
      String key;
      int type;
loop:
      while(true){
        sb.setLength(0);
        while(true){
          i=fis.read();
	  if(i==-1){
	    if(sb.length()>0)
	      error=true;
	    break loop;
	  }
          if(i==0x20){
            host=sb.toString();
            break;
	  }
          sb.append((char)i);
	}

        sb.setLength(0);
        while(true){
          i=fis.read();
	  if(i==-1){error=true; break loop; }
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
	  if(i==-1){error=true; break loop; }
          if(i==0x0d){ continue; }
          if(i==0x0a){
            key=sb.toString();
            break;
	  }
          sb.append((char)i);
	}

	//System.out.println(host);
	//System.out.println("|"+key+"|");

	HostKey hk = new HostKey(host, type, 
				 Util.fromBase64(key.getBytes(), 0, 
						 key.length()));
        pool.addElement(hk);
      }
      fis.close();
      if(error){
	throw new JSchException("KnownHosts: invalid format");
      }
    }
    catch(Exception e){
      if(e instanceof JSchException){
	throw (JSchException)e;         
      }
      throw new JSchException(e.toString());
    }
  }

  String getKnownHostsFile(){ return known_hosts; }

  int check(String host, byte[] key){
    String foo; 
    byte[] bar;
    HostKey hk;
    int type=getType(key);
    for(int i=0; i<pool.size(); i++){
      hk=(HostKey)(pool.elementAt(i));
      if(isIncluded(hk.host, host) && hk.type==type){
        if(equals(hk.key, key)){
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
        if(equals(hk.key, key)){
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
  HostKey[] getHostKeys(){
    HostKey[] foo=new HostKey[pool.size()];
    for(int i=0; i<foo.length; i++){
      foo[i]=(HostKey)pool.elementAt(i);
    }
    return foo;
  }
  void removeHostKey(String host, String type){
    for(int i=0; i<pool.size(); i++){
      HostKey hk=(HostKey)(pool.elementAt(i));
      if(hk.getHost().equals(host) && 
	 hk.getType().equals(type)){
	pool.removeElement(hk);
	try{sync();}catch(Exception e){};
	return;
      }
    }
  }

  void sync() throws IOException { 
    if(known_hosts!=null)
      sync(known_hosts); 
  }
  void sync(String foo) throws IOException {
    if(foo==null) return;
    FileOutputStream fos=new FileOutputStream(foo);
    dump(fos);
    fos.close();
  }
  void dump(OutputStream out) throws IOException {
    try{
      HostKey hk;
      for(int i=0; i<pool.size(); i++){
        hk=(HostKey)(pool.elementAt(i));
        hk.dump(out);
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
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
       //return hosts.substring(i).equals(host);
      }
      if(hostlen==(j-i)){
	if(hosts.regionMatches(true, i, host, 0, hostlen)) return true;
        //if(hosts.substring(i, i+hostlen).equals(host)) return true;
      }
      i=j+1;
    }
    return false;
  }

  private static boolean equals(byte[] foo, byte[] bar){
    if(foo.length!=bar.length)return false;
    for(int i=0; i<foo.length; i++){
      if(foo[i]!=bar[i])return false;
    }
    return true;
  }

  private static final byte[] space={(byte)0x20};
  private static final byte[] sshdss="ssh-dss".getBytes();
  private static final byte[] sshrsa="ssh-rsa".getBytes();
  private static final byte[] cr="\n".getBytes();

  public class HostKey{
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
      else out.write(sshrsa);
      out.write(space);
      out.write(Util.toBase64(key, 0, key.length));
      out.write(cr);
    }

    public String getHost(){ return host; }
    public String getType(){
      if(type==SSHDSS) return new String(sshdss);
      else  return new String(sshrsa);
    }
    public String getKey(){
      return new String(Util.toBase64(key, 0, key.length));
    }
    public String getFingerPrint(){
      HASH hash=null;
      try{
	Class c=Class.forName(jsch.getConfig("md5"));
	hash=(HASH)(c.newInstance());
      }
      catch(Exception e){ System.err.println("getFingerPrint: "+e); }
      return Util.getFingerPrint(hash, key);
    }
  }
}
