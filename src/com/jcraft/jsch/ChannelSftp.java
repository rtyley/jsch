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

import java.net.*;
import java.io.*;

public class ChannelSftp extends ChannelSession{

  private static final byte SSH_FXP_INIT=               1;
  private static final byte SSH_FXP_VERSION=            2;
  private static final byte SSH_FXP_OPEN=               3;
  private static final byte SSH_FXP_CLOSE=              4;
  private static final byte SSH_FXP_READ=               5;
  private static final byte SSH_FXP_WRITE=              6;
  private static final byte SSH_FXP_LSTAT=              7;
  private static final byte SSH_FXP_FSTAT=              8;
  private static final byte SSH_FXP_SETSTAT=            9;
  private static final byte SSH_FXP_FSETSTAT=          10;
  private static final byte SSH_FXP_OPENDIR=           11;
  private static final byte SSH_FXP_READDIR=           12;
  private static final byte SSH_FXP_REMOVE=            13;
  private static final byte SSH_FXP_MKDIR=             14;
  private static final byte SSH_FXP_RMDIR=             15;
  private static final byte SSH_FXP_REALPATH=          16;
  private static final byte SSH_FXP_STAT=              17;
  private static final byte SSH_FXP_RENAME=            18;
  private static final byte SSH_FXP_READLINK=          19;
  private static final byte SSH_FXP_SYMLINK=           20;
  private static final byte SSH_FXP_STATUS=           101;
  private static final byte SSH_FXP_HANDLE=           102;
  private static final byte SSH_FXP_DATA=             103;
  private static final byte SSH_FXP_NAME=             104;
  private static final byte SSH_FXP_ATTRS=            105;
  private static final byte SSH_FXP_EXTENDED=         (byte)200;
  private static final byte SSH_FXP_EXTENDED_REPLY=   (byte)201;

  // pflags
  private static final int SSH_FXF_READ=           0x00000001;
  private static final int SSH_FXF_WRITE=          0x00000002;
  private static final int SSH_FXF_APPEND=         0x00000004;
  private static final int SSH_FXF_CREAT=          0x00000008;
  private static final int SSH_FXF_TRUNC=          0x00000010;
  private static final int SSH_FXF_EXCL=           0x00000020;

  private static final int SSH_FILEXFER_ATTR_SIZE=         0x00000001;
  private static final int SSH_FILEXFER_ATTR_UIDGID=       0x00000002;
  private static final int SSH_FILEXFER_ATTR_PERMISSIONS=  0x00000004;
  private static final int SSH_FILEXFER_ATTR_ACMODTIME=    0x00000008;
  private static final int SSH_FILEXFER_ATTR_EXTENDED=     0x80000000;

  private static final int SSH_FX_OK=                           0;
  private static final int SSH_FX_EOF=                          1;
  private static final int SSH_FX_NO_SUCH_FILE=                 2;
  private static final int SSH_FX_PERMISSION_DENIED=            3;
  private static final int SSH_FX_FAILURE=                      4;
  private static final int SSH_FX_BAD_MESSAGE=                  5;
  private static final int SSH_FX_NO_CONNECTION=                6;
  private static final int SSH_FX_CONNECTION_LOST=              7;
  private static final int SSH_FX_OP_UNSUPPORTED=               8;

//  private boolean interactive=true;
  private boolean interactive=false;
  private int count=1;
  private Buffer buf;
  private Packet packet=new Packet(buf);

  private String version="3";
  private String cwd;
  private String home;
  private String lcwd;

  ChannelSftp(){
    super();
    type="session".getBytes();
    io=new IO();
  }

  /*
  public void init(){
    io.setInputStream(session.in);
    io.setOutputStream(session.out);
  }
  */

  public void start(){
    try{

      PipedOutputStream pos=new PipedOutputStream();
      io.setOutputStream(pos);
      PipedInputStream pis=new PipedInputStream(pos);
      io.setInputStream(pis);

      Request request;
      request=new RequestSftp();
      request.request(session, this);

      thread=this;
      buf=new Buffer();
      packet=new Packet(buf);
      int i=0;
      int j=0;
      int length;
      int type;
      byte[] str;

      // send SSH_FXP_INIT
      sendINIT();

      // receive SSH_FXP_VERSION
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();           // 2 -> SSH_FXP_VERSION

      // send SSH_FXP_REALPATH
      sendREALPATH(".".getBytes());

      // receive SSH_FXP_NAME
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();          // 104 -> SSH_FXP_NAME
      buf.getInt();                //
      i=buf.getInt();              // count
      str=buf.getString();         // filename
      home=cwd=new String(str);
      str=buf.getString();         // logname
//    ATTR.getATTR(buf);           // attrs

      lcwd=new File(".").getAbsolutePath();

    }
    catch(Exception e){
      //System.out.println(e);
    }
    //thread=null;
  }

  public void quit(){ /*eof();*/ disconnect();}
  public void exit(){ /*eof();*/ disconnect();}
  public String lcd(String path){
    if(!path.startsWith("/")){ path=lcwd+"/"+path; }
    if((new File(path)).isDirectory()){
      lcwd=path;
      return lcwd;
    }
    return null;
  }

  /*
      cd /tmp
      c->s REALPATH
      s->c NAME
      c->s STAT
      s->c ATTR 
  */
  public String cd(String path){
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      sendREALPATH(path.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=101 && type!=104){ return null;} // ???
      if(type==101){ return null;}

      buf.getInt();
      i=buf.getInt();
      byte[] str=buf.getString();
      if(str!=null && str[0]!='/'){
        str=(cwd+"/"+new String(str)).getBytes();
      }
      cwd=new String(str);
      str=buf.getString();         // logname
      i=buf.getInt();              // attrs
      return cwd;
    }
    catch(Exception e){
    }
    return null;
  }

  /*
      put foo
      c->s OPEN
      s->c HANDLE
      c->s WRITE
      s->c STATUS
      c->s CLOSE 
      s->c STATUS 
  */
  public boolean put(String src, String dst){
    try{
      if(!dst.startsWith("/")){ dst=cwd+"/"+dst; } 
      if(!src.startsWith("/")){ src=lcwd+"/"+src; } 

      if(isRemoteDir(dst)){
        if(!dst.endsWith("/")){
          dst+="/";
	}
	int i=src.lastIndexOf('/');
	if(i==-1) dst+=src;
	else dst+=src.substring(i+1);
      }
      FileInputStream fis=new FileInputStream(src);

      sendOPENW(dst.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){ return false;} // ???
      if(type==SSH_FXP_STATUS){ return false;}
      buf.getInt();
      byte[] handle=buf.getString();         // filename

      byte[] data=new byte[1024];

      long offset=0;
      while(true){
        i=fis.read(data, 0, 1024);
        if(i<=0)break;
        sendWRITE(handle, offset, data, 0, i);
        offset+=i;

        buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
	if(type!=SSH_FXP_STATUS){ break;}
        buf.getInt();
        if(buf.getInt()!=SSH_FX_OK){
          break;
	}
      }
      fis.close();

      sendCLOSE(handle);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){ 
      System.out.println(e);
    }

    return false;
  }
  public boolean get(String src, String dst){
    try{
      if(!src.startsWith("/")){ src=cwd+"/"+src; } 
      if(!dst.startsWith("/")){ dst=lcwd+"/"+dst; } 

      if((new File(dst)).isDirectory()){
        if(!dst.endsWith("/")){
          dst+="/";
	}
	int i=src.lastIndexOf('/');
	if(i==-1) dst+=src;
	else dst+=src.substring(i+1);
      }

      sendOPENR(src.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();

      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){ return false;} // ???
      if(type==SSH_FXP_STATUS){ return false;}
      buf.getInt();
      byte[] handle=buf.getString();         // filename

      byte[] data=null;
      FileOutputStream fos=new FileOutputStream(dst);
      long offset=0;
      while(true){
        sendREAD(handle, offset, 1000);
        buf.rewind();
	i=io.in.read(buf.buffer, 0, buf.buffer.length);
	length=buf.getInt();
	type=buf.getByte();
        buf.getInt();
	if(type!=SSH_FXP_STATUS && type!=SSH_FXP_DATA){ break;}
	if(type==SSH_FXP_STATUS){
          if(buf.getInt()==SSH_FX_EOF){
            break;
   	  }
//	  break;
	}
	data=buf.getString();
        fos.write(data, 0, data.length);
        offset+=data.length;
      }
      fos.close();

      sendCLOSE(handle);
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){ 
      System.out.println(e);
    }

    return false;
  }

  public java.util.Vector ls(String path){ 
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendOPENDIR(path.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS && type!=SSH_FXP_HANDLE){ return null;} // ???
      if(type==SSH_FXP_STATUS){ return null;}
      buf.getInt();
      byte[] handle=buf.getString();         // filename

      java.util.Vector v=new java.util.Vector();
      while(true){
        sendREADDIR(handle);
        buf.rewind();
        i=io.in.read(buf.buffer, 0, buf.buffer.length);
        buf.index=i;
        length=buf.getInt();
        length=length-(i-4);
        type=buf.getByte();

        if(type!=SSH_FXP_STATUS && type!=SSH_FXP_NAME){ return null;}
        if(type==SSH_FXP_STATUS){ break; }

        buf.getInt();
        int count=buf.getInt();

        byte[] str;
        int flags;

        while(count>0){
          if(length>0){
            buf.shift();
            i=io.in.read(buf.buffer, buf.index, buf.buffer.length-buf.index);
  	    if(i<=0)break;
	    buf.index+=i;
            length-=i;
	  }
          str=buf.getString();
//	System.out.println("? "+new String(str));
          str=buf.getString();         // logname
//	System.out.println(new String(str));
	  v.addElement(new String(str));

	  ATTR.getATTR(buf);
	  /*
          flags=buf.getInt();              // attrs
          i=0;
          if((flags&SSH_FILEXFER_ATTR_SIZE)!=0) i+=8;
          if((flags&SSH_FILEXFER_ATTR_UIDGID)!=0) i+=8;
          if((flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0) i+=4;
          if((flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0) i+=8;
          if((flags&SSH_FILEXFER_ATTR_EXTENDED)!=0){
	      System.out.println("!!!!");
	  }
	  buf.getByte(i);
	  */
	  count--; 
        }
      }

      sendCLOSE(handle);
      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();

      if(type!=SSH_FXP_STATUS){ return null;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK)
        return v;
    }
    catch(Exception e){
    }
    return null;
  }
  public boolean symlink(String oldpath, String newpath){
    try{
      if(!oldpath.startsWith("/")){ oldpath=cwd+"/"+oldpath; } 
      if(!newpath.startsWith("/")){ newpath=cwd+"/"+newpath; } 

      sendSYMLINK(oldpath.getBytes(), newpath.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){
    }
    return false;
  }
  public boolean rename(String oldpath, String newpath){
    try{
      if(!oldpath.startsWith("/")){ oldpath=cwd+"/"+oldpath; } 
      if(!newpath.startsWith("/")){ newpath=cwd+"/"+newpath; } 

      sendRENAME(oldpath.getBytes(), newpath.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){
    }
    return false;
  }
  public boolean rm(String path){
    try{
     if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendREMOVE(path.getBytes());

      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){}
    return false;
  }
  boolean isRemoteDir(String path){
    try{
      sendSTAT(path.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){ return false; }
      buf.getInt();
      ATTR attr=ATTR.getATTR(buf);
      return attr.isDir();
    }
    catch(Exception e){}
    return false;
  }
  public boolean chgrp(int gid, String path){
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      sendSTAT(path.getBytes());
 
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){ return false; }
      buf.getInt();
      ATTR attr=ATTR.getATTR(buf);
      attr.setUIDGID(attr.uid, gid); 

      sendSETSTAT(path.getBytes(), attr);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){}
    return false;
  }
  public boolean chown(int uid, String path){
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      sendSTAT(path.getBytes());
 
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){ return false; }
      buf.getInt();
      ATTR attr=ATTR.getATTR(buf);
      attr.setUIDGID(uid, attr.gid); 

      sendSETSTAT(path.getBytes(), attr);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){}
    return false;
  }
  public boolean chmod(int permissions, String path){
    try{
      if(!path.startsWith("/")){ path=cwd+"/"+path; }

      sendSTAT(path.getBytes());
 
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_ATTRS){ return false; }

      buf.getInt();
      ATTR attr=ATTR.getATTR(buf);
      attr.setPERMISSIONS(permissions); 

      sendSETSTAT(path.getBytes(), attr);

      buf.rewind();
      i=io.in.read(buf.buffer, 0, buf.buffer.length);
      length=buf.getInt();
      type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false;}
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){}
    return false;
  }
  public boolean rmdir(String path){
    try{
     if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendRMDIR(path.getBytes());
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false; }
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){}
    return false;
  }

  public boolean mkdir(String path){
    try{
     if(!path.startsWith("/")){ path=cwd+"/"+path; }
      sendMKDIR(path.getBytes(), null);
      buf.rewind();
      int i=io.in.read(buf.buffer, 0, buf.buffer.length);
      int length=buf.getInt();
      int type=buf.getByte();
      if(type!=SSH_FXP_STATUS){ return false; }
      buf.getInt();
      if(buf.getInt()==SSH_FX_OK) return true;
    }
    catch(Exception e){}
    return false;
  }

  public String pwd(){ return cwd; }
  public String lpwd(){ return lcwd; }
  public String version(){ return version; }

  private void sendINIT() throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_INIT, 5);
    buf.putInt(3);                // version 3
   session.write(packet, this, 5+4);
  }

  private void sendREALPATH(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_REALPATH, path);
  }
  private void sendSTAT(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_STAT, path);
  }
  private void sendSETSTAT(byte[] path, ATTR attr) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_SETSTAT, 9+path.length+attr.length());
    buf.putInt(count++);
    buf.putString(path);             // path
    attr.dump(buf);
    session.write(packet, this, 9+path.length+attr.length()+4);
  }
  private void sendREMOVE(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_REMOVE, path);
  }
  private void sendMKDIR(byte[] path, ATTR attr) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_MKDIR, 9+path.length+(attr!=null?attr.length():4));
    buf.putInt(count++);
    buf.putString(path);             // path
    if(attr!=null) attr.dump(buf);
    else buf.putInt(0);
    session.write(packet, this, 9+path.length+(attr!=null?attr.length():4)+4);
  }
  private void sendRMDIR(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_RMDIR, path);
  }
  private void sendSYMLINK(byte[] p1, byte[] p2) throws Exception{
    sendPacketPath(SSH_FXP_SYMLINK, p1, p2);
  }
  private void sendREADLINK(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_READLINK, path);
  }
  private void sendLSTAT(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_LSTAT, path);
  }
  private void sendOPENDIR(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_OPENDIR, path);
  }
  private void sendREADDIR(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_READDIR, path);
  }
  private void sendRENAME(byte[] p1, byte[] p2) throws Exception{
    sendPacketPath(SSH_FXP_RENAME, p1, p2);
  }
  private void sendCLOSE(byte[] path) throws Exception{
    sendPacketPath(SSH_FXP_CLOSE, path);
  }
  private void sendOPENR(byte[] path) throws Exception{
    sendOPEN(path, SSH_FXF_READ);
  }
  private void sendOPENW(byte[] path) throws Exception{
    sendOPEN(path, SSH_FXF_WRITE|SSH_FXF_CREAT|SSH_FXF_TRUNC);
  }
  private void sendOPEN(byte[] path, int mode) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_OPEN, 17+path.length);
    buf.putInt(count++);
    buf.putString(path);
    buf.putInt(mode);
    buf.putInt(0);           // attrs
    session.write(packet, this, 17+path.length+4);
  }
  private void sendPacketPath(byte fxp, byte[] path) throws Exception{
    packet.reset();
    putHEAD(fxp, 9+path.length);
    buf.putInt(count++);
    buf.putString(path);             // path
    session.write(packet, this, 9+path.length+4);
  }
  private void sendPacketPath(byte fxp, byte[] p1, byte[] p2) throws Exception{
    packet.reset();
    putHEAD(fxp, 13+p1.length+p2.length);
    buf.putInt(count++);
    buf.putString(p1);
    buf.putString(p2);
    session.write(packet, this, 13+p1.length+p2.length+4);
  }

  private void sendWRITE(byte[] handle, long offset, 
			 byte[] data, int start, int length) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_WRITE, 21+handle.length+length);
    buf.putInt(count++);
    buf.putString(handle);
    buf.putLong(offset);
    buf.putString(data, start, length);
    session.write(packet, this, 21+handle.length+length+4);
  }

  private void sendREAD(byte[] handle, long offset, int length) throws Exception{
    packet.reset();
    putHEAD(SSH_FXP_READ, 21+handle.length);
    buf.putInt(count++);
    buf.putString(handle);
    buf.putLong(offset);
    buf.putInt(length);
    session.write(packet, this, 21+handle.length+4);
  }

  private void putHEAD(byte type, int length) throws Exception{
    buf.putByte((byte)Session.SSH_MSG_CHANNEL_DATA);
    buf.putInt(recipient);
    buf.putInt(length+4);
    buf.putInt(length);
    buf.putByte(type);
  }

  /*
  void write(byte[] foo){
    write(foo, 0, foo.length);
  }
  void write(byte[] foo, int s, int l){
    if(eof)return;
    io.put(foo, s, l);
  }
  */

/*
  uint32   flags
  uint64   size           present only if flag SSH_FILEXFER_ATTR_SIZE
  uint32   uid            present only if flag SSH_FILEXFER_ATTR_UIDGID
  uint32   gid            present only if flag SSH_FILEXFER_ATTR_UIDGID
  uint32   permissions    present only if flag SSH_FILEXFER_ATTR_PERMISSIONS
  uint32   atime          present only if flag SSH_FILEXFER_ACMODTIME
  uint32   mtime          present only if flag SSH_FILEXFER_ACMODTIME
  uint32   extended_count present only if flag SSH_FILEXFER_ATTR_EXTENDED
  string   extended_type
  string   extended_data
    ...      more extended data (extended_type - extended_data pairs),
             so that number of pairs equals extended_count
*/
  static class ATTR{
    static final int S_IFDIR=0x4000;

    int flags=0;
    long size;
    int uid;
    int gid;
    int permissions;
    int atime;
    int mtime;
    String[] extended=null;

    static ATTR getATTR(Buffer buf){
      ATTR attr=new ATTR();	
      attr.flags=buf.getInt();
      if((attr.flags&SSH_FILEXFER_ATTR_SIZE)!=0){ attr.size=buf.getLong(); }
      if((attr.flags&SSH_FILEXFER_ATTR_UIDGID)!=0){
        attr.uid=buf.getInt(); attr.gid=buf.getInt();
      }
      if((attr.flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0){ 
        attr.permissions=buf.getInt();
      }
      if((attr.flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0){ 
        attr.atime=buf.getInt();
      }
      if((attr.flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0){ 
        attr.mtime=buf.getInt(); 
      }
      if((attr.flags&SSH_FILEXFER_ATTR_EXTENDED)!=0){
        int count=buf.getInt();
        if(count>0){
          attr.extended=new String[count*2];
          for(int i=0; i<count; i++){
            attr.extended[i*2]=new String(buf.getString());
            attr.extended[i*2+1]=new String(buf.getString());
	  }
	}
      }
      return attr;
    } 

    int length(){
      int len=4;

      if((flags&SSH_FILEXFER_ATTR_SIZE)!=0){ len+=8; }
      if((flags&SSH_FILEXFER_ATTR_UIDGID)!=0){ len+=8; }
      if((flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0){ len+=4; }
      if((flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0){ len+=8; }
      if((flags&SSH_FILEXFER_ATTR_EXTENDED)!=0){
        len+=4;
        int count=extended.length/2;
        if(count>0){
          for(int i=0; i<count; i++){
            len+=4; len+=extended[i*2].length();
	    len+=4; len+=extended[i*2+1].length();
	  }
	}
      }
      return len;
    }
    void dump(Buffer buf){
      buf.putInt(flags);
      if((flags&SSH_FILEXFER_ATTR_SIZE)!=0){ buf.putLong(size); }
      if((flags&SSH_FILEXFER_ATTR_UIDGID)!=0){
        buf.putInt(uid); buf.putInt(gid);
      }
      if((flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0){ 
        buf.putInt(permissions);
      }
      if((flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0){ buf.putInt(atime); }
      if((flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0){ buf.putInt(mtime); }
      if((flags&SSH_FILEXFER_ATTR_EXTENDED)!=0){
        int count=extended.length/2;
        if(count>0){
          for(int i=0; i<count; i++){
            buf.putString(extended[i*2].getBytes());
            buf.putString(extended[i*2+1].getBytes());
	  }
	}
      }
    }
    void setSIZE(long size){
      flags|=SSH_FILEXFER_ATTR_SIZE;
      this.size=size;
    }
    void setUIDGID(int uid, int gid){
      flags|=SSH_FILEXFER_ATTR_UIDGID;
      this.uid=uid;
      this.gid=gid;
    }
    void setACMODTIME(int atime, int mtime){
      flags|=SSH_FILEXFER_ATTR_ACMODTIME;
      this.atime=atime;
      this.mtime=mtime;
    }
    void setPERMISSIONS(int permissions){
      flags|=SSH_FILEXFER_ATTR_PERMISSIONS;
      this.permissions=permissions;
    }
    boolean isDir(){
      return ((flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0 && 
	      ((permissions&S_IFDIR)!=0));
    }      
  }
}
