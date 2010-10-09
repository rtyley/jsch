/* JSch
 * Copyright (C) 2002 Lucas Bruand
 *  
 * Written by: 2002 Lucas Bruand<lbruand@users.sourforge.net>
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
 /**
  * This class contains all the constants provided by the following document:
  * http://ietf.org/internet-drafts/draft-ietf-secsh-assignednumbers-01.txt
  *
  */
public class Const {
    /*  The Message Number is an 8-bit value, which describes the payload of
   a packet.

   Protocol packets have message numbers in the range 1 to 255.  These
   numbers have been allocated as follows in [SSH-ARCH]:

     Transport layer protocol:

       1 to 19    Transport layer generic (e.g. disconnect, ignore, debug, etc.)
       20 to 29   Algorithm negotiation
       30 to 49   Key exchange method specific (numbers can be reused for
                  different authentication methods)

     User authentication protocol:

       50 to 59   User authentication generic
       60 to 79   User authentication method specific (numbers can be
                  reused for different authentication methods)

     Connection protocol:

       80 to 89   Connection protocol generic
       90 to 127  Channel related messages

     Reserved for client protocols:

       128 to 191 Reserved

     Local extensions:

       192 to 255 Local extensions
 */
   /* [SSH-TRANS] */
   final public static byte SSH_MSG_DISCONNECT =                     1;
   final public static byte SSH_MSG_IGNORE =                         2;
   final public static byte SSH_MSG_UNIMPLEMENTED =                  3;
   final public static byte SSH_MSG_DEBUG =                          4;
   final public static byte SSH_MSG_SERVICE_REQUEST =                5;
   final public static byte SSH_MSG_SERVICE_ACCEPT =                 6;
   final public static byte SSH_MSG_KEXINIT =                       20;
   final public static byte SSH_MSG_NEWKEYS =                       21;
   final public static byte SSH_MSG_KEXDH_INIT =                    30;
   final public static byte SSH_MSG_KEXDH_REPLY =                   31;
   /* [SSH-USERAUTH] */
   final public static byte SSH_MSG_USERAUTH_REQUEST =              50;
   final public static byte SSH_MSG_USERAUTH_FAILURE =              51;
   final public static byte SSH_MSG_USERAUTH_SUCCESS =              52;
   final public static byte SSH_MSG_USERAUTH_BANNER =               53;
   final public static byte SSH_MSG_USERAUTH_PK_OK =                60;
   /* [SSH-CONNECT] */
   final public static byte SSH_MSG_GLOBAL_REQUEST =                80;
   final public static byte SSH_MSG_REQUEST_SUCCESS =               81;
   final public static byte SSH_MSG_REQUEST_FAILURE =               82;
   final public static byte SSH_MSG_CHANNEL_OPEN =                  90;
   final public static byte SSH_MSG_CHANNEL_OPEN_CONFIRMATION =     91;
   final public static byte SSH_MSG_CHANNEL_OPEN_FAILURE =          92;
   final public static byte SSH_MSG_CHANNEL_WINDOW_ADJUST =         93;
   final public static byte SSH_MSG_CHANNEL_DATA =                  94;
   final public static byte SSH_MSG_CHANNEL_EXTENDED_DATA =         95;
   final public static byte SSH_MSG_CHANNEL_EOF =                   96;
   final public static byte SSH_MSG_CHANNEL_CLOSE =                 97;
   final public static byte SSH_MSG_CHANNEL_REQUEST =               98;
   final public static byte SSH_MSG_CHANNEL_SUCCESS =               99;
   final public static byte SSH_MSG_CHANNEL_FAILURE =              100;

   /* Disconnect codes */
   public static byte SSH_DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT =      1;
   public static byte SSH_DISCONNECT_PROTOCOL_ERROR =                   2;
   public static byte SSH_DISCONNECT_KEY_EXCHANGE_FAILED =              3;
   public static byte SSH_DISCONNECT_RESERVED =                         4;
   public static byte SSH_DISCONNECT_MAC_ERROR =                        5;
   public static byte SSH_DISCONNECT_COMPRESSION_ERROR =                6;
   public static byte SSH_DISCONNECT_SERVICE_NOT_AVAILABLE =            7;
   public static byte SSH_DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED =   8;
   public static byte SSH_DISCONNECT_HOST_KEY_NOT_VERIFIABLE =          9;
   public static byte SSH_DISCONNECT_CONNECTION_LOST =                 10;
   public static byte SSH_DISCONNECT_BY_APPLICATION =                  11;
   public static byte SSH_DISCONNECT_TOO_MANY_CONNECTIONS =            12;
   public static byte SSH_DISCONNECT_AUTH_CANCELLED_BY_USER =          13;
   public static byte SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE =  14;
   public static byte SSH_DISCONNECT_ILLEGAL_USER_NAME =               15;
}
