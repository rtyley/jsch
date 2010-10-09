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

public class JSch{
  static java.util.Hashtable config=new java.util.Hashtable();
  static{
//  config.put("kex", "diffie-hellman-group-exchange-sha1");
    config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group-exchange-sha1");
    config.put("server_host_key", "ssh-rsa,ssh-dss");
    //config.put("server_host_key", "ssh-dss,ssh-rsa");
    config.put("cipher.s2c", "3des-cbc,blowfish-cbc");
    config.put("cipher.c2s", "3des-cbc,blowfish-cbc");
    config.put("mac.s2c", "hmac-md5,hmac-sha1,hmac-sha1-96,hmac-md5-96");
    config.put("mac.c2s", "hmac-md5,hmac-sha1,hmac-sha1-96,hmac-md5-96");
    config.put("compression.s2c", "none");
    config.put("compression.c2s", "none");
    config.put("lang.s2c", "");
    config.put("lang.c2s", "");

    config.put("diffie-hellman-group-exchange-sha1", 
                                "com.jcraft.jsch.jce.DHGEX");
    config.put("diffie-hellman-group1-sha1", 
	                        "com.jcraft.jsch.jce.DHG1");
    config.put("dh",            "com.jcraft.jsch.jce.DH");
    config.put("3des-cbc",      "com.jcraft.jsch.jce.TripleDESCBC");
    config.put("blowfish-cbc",  "com.jcraft.jsch.jce.BlowfishCBC");
    config.put("hmac-sha1",     "com.jcraft.jsch.jce.HMACSHA1");
    config.put("hmac-sha1-96",  "com.jcraft.jsch.jce.HMACSHA196");
    config.put("hmac-md5",      "com.jcraft.jsch.jce.HMACMD5");
    config.put("hmac-md5-96",   "com.jcraft.jsch.jce.HMACMD596");
    config.put("sha-1",         "com.jcraft.jsch.jce.SHA1");
    config.put("md5",           "com.jcraft.jsch.jce.MD5");
    config.put("signature.dss", "com.jcraft.jsch.jce.SignatureDSA");
    config.put("signature.rsa", "com.jcraft.jsch.jce.SignatureRSA");
    config.put("random",        "com.jcraft.jsch.jce.Random");

    config.put("zlib",          "com.jcraft.jsch.jcraft.Compression");

    config.put("StrictHostKeyChecking",  "ask");
  }
  /*static*/ java.util.Vector pool=new java.util.Vector();
  /*static*/ java.util.Vector identities=new java.util.Vector();
  private KnownHosts known_hosts=null;

  public JSch(){
    known_hosts=new KnownHosts();
  }

  public Session getSession(String username, String host) throws JSchException { return getSession(username, host, 22); }
  public Session getSession(String username, String host, int port) throws JSchException {
    Session s=new Session(this); 
    s.setUserName(username);
    s.setHost(host);
    s.setPort(port);
    pool.addElement(s);
    return s;
  }
  public void setKnownHosts(String foo){ known_hosts.setKnownHosts(foo); }
  public KnownHosts getKnownHosts(){ return known_hosts; }
  public void addIdentity(String foo) throws JSchException{
    addIdentity(foo, null);
  }
  public void addIdentity(String foo, String bar) throws JSchException{
    Identity identity=new Identity(foo, this);
    if(bar!=null) identity.setPassphrase(bar);
    identities.addElement(identity);
  }
  String getConfig(String foo){ return (String)(config.get(foo)); }

  private java.util.Vector proxies;
  void setProxy(String hosts, Proxy proxy){
    java.lang.String[] patterns=Util.split(hosts, ",");
    if(proxies==null){proxies=new java.util.Vector();}
    for(int i=0; i<patterns.length; i++){
      if(proxy==null){
	proxies.insertElementAt(null, 0);
	proxies.insertElementAt(patterns[i].getBytes(), 0);
      }
      else{
	proxies.addElement(patterns[i].getBytes());
	proxies.addElement(proxy);
      }
    }
  }
  Proxy getProxy(String host){
    if(proxies==null)return null;
    byte[] _host=host.getBytes();
    for(int i=0; i<proxies.size(); i+=2){
      if(Util.glob(((byte[])proxies.elementAt(i)), _host)){
	return (Proxy)(proxies.elementAt(i+1));
      }
    }
    return null;
  }
  void removeProxy(){
    proxies=null;
  }

  public static void setConfig(java.util.Hashtable foo){
    for(java.util.Enumeration e=foo.keys() ; e.hasMoreElements() ;) {
      String key=(String)(e.nextElement());
      config.put(key, (String)(foo.get(key)));
    }
  }
}
