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
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
VISIGOTH SOFTWARE SOCIETY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

public class JSch{
  static java.util.Properties config=new java.util.Properties();
  static{
    config.put("random", "com.jcraft.jsch.jce.Random");
//  config.put("kex", "diffie-hellman-group-exchange-sha1");
    config.put("kex", "diffie-hellman-group1-sha1");
    config.put("dh", "com.jcraft.jsch.jce.DH");
    config.put("server_host_key", "ssh-rsa,ssh-dss");
//  config.put("server_host_key", "ssh-dss,ssh-rsa");
    config.put("cipher.s2c", "blowfish-cbc");
    config.put("cipher.c2s", "blowfish-cbc");
    config.put("mac.s2c", "hmac-md5");
    config.put("mac.c2s", "hmac-md5");
    config.put("compression.s2c", "none");
    config.put("compression.c2s", "none");
    config.put("lang.s2c", "");
    config.put("lang.c2s", "");

    config.put("diffie-hellman-group-exchange-sha1", "com.jcraft.jsch.jce.DHGEX");
    config.put("diffie-hellman-group1-sha1", "com.jcraft.jsch.jce.DHG1");

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

    config.put("zlib", "com.jcraft.jsch.jcraft.Compression");
  }
  private static java.util.Vector pool=new java.util.Vector();
  static java.util.Vector identities=new java.util.Vector();
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
  public void addIdentity(String foo) throws Exception{
    addIdentity(foo, null);
  }
  public void addIdentity(String foo, String bar) throws Exception{
    Identity identity=new Identity(foo, this);
    if(bar!=null) identity.setPassphrase(bar);
    identities.addElement(identity);
  }
  String getConfig(String foo){ return (String)(config.get(foo)); }
}
