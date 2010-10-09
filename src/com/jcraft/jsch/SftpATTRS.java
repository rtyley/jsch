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
public class SftpATTRS {
  public static final int SSH_FILEXFER_ATTR_SIZE=         0x00000001;
  public static final int SSH_FILEXFER_ATTR_UIDGID=       0x00000002;
  public static final int SSH_FILEXFER_ATTR_PERMISSIONS=  0x00000004;
  public static final int SSH_FILEXFER_ATTR_ACMODTIME=    0x00000008;
  public static final int SSH_FILEXFER_ATTR_EXTENDED=     0x80000000;

  static final int S_IFDIR=0x4000;

  int flags=0;
  long size;
  int uid;
  int gid;
  int permissions;
  int atime;
  int mtime;
  String[] extended=null;

  static SftpATTRS getATTR(Buffer buf){
    SftpATTRS attr=new SftpATTRS();	
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

  public int getFlags() { return flags; }
  public long getSize() { return size; }
  public int getUId() { return uid; }
  public int getGId() { return gid; }
  public int getPermissions() { return permissions; }
  public int getATime() { return atime; }
  public int getMTime() { return mtime; }
  public String[] getExtended() { return extended; }

  public String toString(){
    return (((flags&SSH_FILEXFER_ATTR_SIZE)!=0) ? ("size:"+size+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_UIDGID)!=0) ? ("uid:"+uid+",gid:"+gid+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0) ? ("permissions:0x"+Integer.toHexString(permissions)+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0) ? ("atime:"+atime+",mtime:"+mtime+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_EXTENDED)!=0) ? ("extended:?"+" ") : "");
  }

}
