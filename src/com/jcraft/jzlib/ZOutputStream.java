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

public class ZOutputStream extends OutputStream {

  protected ZStream z=new ZStream();
  protected int bufsize=512;
  protected int flush=JZlib.Z_NO_FLUSH;
  protected byte[] buf=new byte[bufsize],
                   buf1=new byte[1];
  protected boolean compress;

  private OutputStream out;

  public ZOutputStream(OutputStream out) {
    super();
    this.out=out;
    z.inflateInit();
    compress=false;
  }

  public ZOutputStream(OutputStream out, int level) {
    super();
    this.out=out;
    z.deflateInit(level);
    compress=true;
  }

  public void write(int b) throws IOException {
    buf1[0]=(byte)b;
    write(buf1, 0, 1);
  }

  public void write(byte b[], int off, int len) throws IOException {
    if(len==0)
      return;
    int err;
    z.next_in=b;
    z.next_in_index=off;
    z.avail_in=len;
    do{
      z.next_out=buf;
      z.next_out_index=0;
      z.avail_out=bufsize;
      if(compress)
        err=z.deflate(flush);
      else
        err=z.inflate(flush);
      if(err!=JZlib.Z_OK)
        throw new ZStreamException((compress?"de":"in")+"flating: "+z.msg);
      out.write(buf, 0, bufsize-z.avail_out);
    } 
    while(z.avail_in>0 || z.avail_out==0);
  }

  public int getFlushMode() {
    return(flush);
  }

  public void setFlushMode(int flush) {
    this.flush=flush;
  }

  public void close() throws IOException {
    try {
      out.flush();
    } catch (IOException ignored) {
    }
    z.deflateEnd();
    z.free();
    z=null;
    out.close();
    out=null;
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

  public void flush() throws IOException {
    out.flush();
  }

}
