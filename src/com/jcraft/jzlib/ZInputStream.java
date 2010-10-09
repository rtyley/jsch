/* -*-mode:java; c-basic-offset:2; -*- */
/* JZlib -- zlib in pure Java
 *
 * Copyright (C) 2001 Lapo Luchini.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;
import java.io.*;

public class ZInputStream extends InputStream {

  protected ZStream z=new ZStream();
  protected int bufsize=512;
  protected int flush=JZlib.Z_NO_FLUSH;
  protected byte[] buf=new byte[bufsize],
                   buf1=new byte[1];
  protected boolean compress;

  private InputStream in=null;

  public ZInputStream(InputStream in) {
    super();
    this.in=in;
    z.inflateInit();
    compress=false;
    z.next_in=buf;
    z.next_in_index=0;
    z.avail_in=0;
  }

  public ZInputStream(InputStream in, int level) {
    super();
    this.in=in;
    z.deflateInit(level);
    compress=true;
    z.next_in=buf;
    z.next_in_index=0;
    z.avail_in=0;
  }

  /*public int available() throws IOException {
    return inf.finished() ? 0 : 1;
  }*/

  public int read() throws IOException {
    if(read(buf1, 0, 1)==-1)
      return(-1);
    return(buf1[0]&0xFF);
  }

  private boolean nomoreinput=false;

  public int read(byte[] b, int off, int len) throws IOException {
    if(len==0)
      return(0);
    int err;
    z.next_out=b;
    z.next_out_index=off;
    z.avail_out=len;
    do {
      if((z.avail_in==0)&&(!nomoreinput)) { // if buffer is empty and more input is avaiable, refill it
	z.next_in_index=0;
	z.avail_in=in.read(buf, 0, bufsize);//(bufsize<z.avail_out ? bufsize : z.avail_out));
	if(z.avail_in==-1) {
	  z.avail_in=0;
	  nomoreinput=true;
	}
      }
      if(compress)
	err=z.deflate(flush);
      else
	err=z.inflate(flush);
      if(nomoreinput&&(err==JZlib.Z_BUF_ERROR))
        return(-1);
      if(err!=JZlib.Z_OK && err!=JZlib.Z_STREAM_END)
	throw new ZStreamException((compress ? "de" : "in")+"flating: "+z.msg);
      if(nomoreinput&&(z.avail_out==len))
	return(-1);
    } while(z.avail_out==len);
    //System.err.print("("+(len-z.avail_out)+")");
    return(len-z.avail_out);
  }

  public long skip(long n) throws IOException {
    int len=512;
    if(n<len)
      len=(int)n;
    byte[] tmp=new byte[len];
    return((long)read(tmp));
  }

  public int getFlushMode() {
    return(flush);
  }

  public void setFlushMode(int flush) {
    this.flush=flush;
  }

  /**
   * Returns the total number of bytes input so far.
   */
  public long getTotalIn() {
    return z.total_in;
  }

  /**
   * Returns the total number of bytes output so far.
   */
  public long getTotalOut() {
    return z.total_out;
  }

  public void close() throws IOException{
    in.close();
  }
}
