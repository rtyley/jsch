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
import java.net.*;

public class ProxyHTTP implements Proxy{
  private String proxy_host;
  private int proxy_port;
  private String host;
  private int port;
  private InputStream in;
  private OutputStream out;

  private String user;
  private String passwd;

  public ProxyHTTP(String proxy_host){
    this(proxy_host, 80);
  }
  public ProxyHTTP(String proxy_host, int proxy_port){
    this.proxy_host=proxy_host;
    this.proxy_port=proxy_port;
  }
  public void setUserPasswd(String user, String passwd){
    this.user=user;
    this.passwd=passwd;
  }
  public void connect(String host, int port) throws Exception{
    this.host=host;
    this.port=port;
    Socket socket=new Socket(proxy_host, proxy_port);    
    socket.setTcpNoDelay(true);
    in=socket.getInputStream();
    out=socket.getOutputStream();
    out.write(("CONNECT "+host+":"+port+" HTTP/1.0\n").getBytes());

    out.write("\n".getBytes());
    out.flush();

    int foo;
    while(true){
      foo=in.read(); if(foo!=13) continue;
      foo=in.read(); if(foo!=10) continue;
      foo=in.read(); if(foo!=13) continue;      
      foo=in.read(); if(foo!=10) continue;
      break;
    }

  }
  public InputStream getInputStream(){ return in; }
  public OutputStream getOutputStream(){ return out; }
}
