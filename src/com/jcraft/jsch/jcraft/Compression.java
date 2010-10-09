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

package com.jcraft.jsch.jcraft;
import com.jcraft.jzlib.*;
import com.jcraft.jsch.*;

public class Compression implements com.jcraft.jsch.Compression {
  static private final int BUF_SIZE=4096;

  private int type;
  private ZStream stream;
  private byte[] tmpbuf=new byte[BUF_SIZE];

  public Compression(){
    stream=new ZStream();
  }

  public void init(int type, int level){
    if(type==DEFLATER){
      stream.deflateInit(level);
      this.type=DEFLATER;
    }
    else if(type==INFLATER){
      stream.inflateInit();
      inflated_buf=new byte[BUF_SIZE];
      this.type=INFLATER;
    }
  }
  /*
  static Compression getDeflater(int level){
    Compression foo=new Compression();
    foo.stream.deflateInit(level);
    foo.type=DEFLATER;
    return foo;
  }
  */
  private byte[] inflated_buf;
  /*
  static Compression getInflater(){
    Compression foo=new Compression();
    foo.stream.inflateInit();
    foo.inflated_buf=new byte[BUF_SIZE];
    foo.type=INFLATER;
    return foo;
  }
  */

  public int compress(byte[] buf, int start, int len){
    stream.next_in=buf;
    stream.next_in_index=start;
    stream.avail_in=len-start;
    int status;
    int outputlen=start;

    do{
      stream.next_out=tmpbuf;
      stream.next_out_index=0;
      stream.avail_out=BUF_SIZE;
      status=stream.deflate(JZlib.Z_PARTIAL_FLUSH);
      switch(status){
        case JZlib.Z_OK:
	    System.arraycopy(tmpbuf, 0,
			     buf, outputlen,
			     BUF_SIZE-stream.avail_out);
	    outputlen+=(BUF_SIZE-stream.avail_out);
	    break;
        default:
	    System.err.println("compress: deflate returnd "+status);
      }
    }
    while(stream.avail_out==0);
    return outputlen;
  }

  public byte[] uncompress(byte[] buffer, int start, int[] length){
    int inflated_end=0;

    stream.next_in=buffer;
    stream.next_in_index=start;
    stream.avail_in=length[0];

    while(true){
      stream.next_out=tmpbuf;
      stream.next_out_index=0;
      stream.avail_out=BUF_SIZE;
      int status=stream.inflate(JZlib.Z_PARTIAL_FLUSH);
      switch(status){
        case JZlib.Z_OK:
	  if(inflated_buf.length<inflated_end+BUF_SIZE-stream.avail_out){
            byte[] foo=new byte[inflated_end+BUF_SIZE-stream.avail_out];
	    System.arraycopy(inflated_buf, 0, foo, 0, inflated_end);
	    inflated_buf=foo;
	  }
	  System.arraycopy(tmpbuf, 0,
			   inflated_buf, inflated_end,
			   BUF_SIZE-stream.avail_out);
	  inflated_end+=(BUF_SIZE-stream.avail_out);
          length[0]=inflated_end;
	  break;
        case JZlib.Z_BUF_ERROR:
          if(inflated_end>buffer.length-start){
            byte[] foo=new byte[inflated_end+5];
            System.arraycopy(buffer, 0, foo, 0, start);
            System.arraycopy(inflated_buf, 0, foo, 5, inflated_end);
	    buffer=foo;
	  }
	  else{
            System.arraycopy(inflated_buf, 0, buffer, 5, inflated_end);
	  }
          length[0]=inflated_end;
	  return buffer;
	default:
	  System.err.println("uncompress: inflate returnd "+status);
          return null;
      }
    }
  }
}
