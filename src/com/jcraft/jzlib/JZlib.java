/* -*-mode:java; c-basic-offset:2; -*- */
/* JZlib -- zlib in pure Java
 *  
 * Copyright (C) 2000 ymnk, JCraft, Inc.
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

final public class JZlib{

  // compression levels
  static final public int Z_NO_COMPRESSION=0;
  static final public int Z_BEST_SPEED=1;
  static final public int Z_BEST_COMPRESSION=9;
  static final public int Z_DEFAULT_COMPRESSION=(-1);

  // compression strategy
  static final public int Z_FILTERED=1;
  static final public int Z_HUFFMAN_ONLY=2;
  static final public int Z_DEFAULT_STRATEGY=0;

  static final public int Z_NO_FLUSH=0;
  static final public int Z_PARTIAL_FLUSH=1;
  static final public int Z_SYNC_FLUSH=2;
  static final public int Z_FULL_FLUSH=3;
  static final public int Z_FINISH=4;

  static final public int Z_OK=0;
  static final public int Z_STREAM_END=1;
  static final public int Z_NEED_DICT=2;
  static final public int Z_ERRNO=-1;
  static final public int Z_STREAM_ERROR=-2;
  static final public int Z_DATA_ERROR=-3;
  static final public int Z_MEM_ERROR=-4;
  static final public int Z_BUF_ERROR=-5;
  static final public int Z_VERSION_ERROR=-6;
}
