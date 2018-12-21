/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ru.zinal.webdav.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *  All URL decoding happens here. This way we can reuse, review, optimize
 *  without adding complexity to the buffers.
 *
 *  The conversion will modify the original buffer.
 * 
 *  This is a stripped-down version of the original Tomcat 9 code.
 *
 *  @author Costin Manolache
 */
public final class UDecoder {

    private static final StringManager sm 
            = StringManager.getManager(UDecoder.class);

    public static final boolean ALLOW_ENCODED_SLASH =
        Boolean.parseBoolean(System.getProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "false"));

    public UDecoder()
    {
    }

    /**
     * %xx decoding of a string. FIXME: this is inefficient.
     * @param str The URL encoded string
     * @param query <code>true</code> if this is a query string
     * @return the decoded string
     */
    public final String convert(String str, boolean query)
    {
        if (str == null) {
            return  null;
        }

        if( (!query || str.indexOf( '+' ) < 0) && str.indexOf( '%' ) < 0 ) {
            return str;
        }

        final boolean noSlash = !(ALLOW_ENCODED_SLASH || query);

        StringBuilder dec = new StringBuilder();    // decoded string output
        int strPos = 0;
        int strLen = str.length();

        dec.ensureCapacity(str.length());
        while (strPos < strLen) {
            int laPos;        // lookahead position

            // look ahead to next URLencoded metacharacter, if any
            for (laPos = strPos; laPos < strLen; laPos++) {
                char laChar = str.charAt(laPos);
                if ((laChar == '+' && query) || (laChar == '%')) {
                    break;
                }
            }

            // if there were non-metacharacters, copy them all as a block
            if (laPos > strPos) {
                dec.append(str.substring(strPos,laPos));
                strPos = laPos;
            }

            // shortcut out of here if we're at the end of the string
            if (strPos >= strLen) {
                break;
            }

            // process next metacharacter
            char metaChar = str.charAt(strPos);
            if (metaChar == '+') {
                dec.append(' ');
                strPos++;
                continue;
            } else if (metaChar == '%') {
                // We throw the original exception - the super will deal with
                // it
                //                try {
                char res = (char) Integer.parseInt(
                        str.substring(strPos + 1, strPos + 3), 16);
                if (noSlash && (res == '/')) {
                    throw new IllegalArgumentException(sm.getString("uDecoder.noSlash"));
                }
                dec.append(res);
                strPos += 3;
            }
        }

        return dec.toString();
    }


    /**
     * Decode and return the specified URL-encoded String.
     * When the byte array is converted to a string, UTF-8 is used. This may
     * be different than some other servers. It is assumed the string is not a
     * query string.
     *
     * @param str The url-encoded string
     * @return the decoded string
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(String str) {
        return URLDecode(str, StandardCharsets.UTF_8);
    }


    /**
     * Decode and return the specified URL-encoded String. It is assumed the
     * string is not a query string.
     *
     * @param str The url-encoded string
     * @param charset The character encoding to use; if null, UTF-8 is used.
     * @return the decoded string
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(String str, Charset charset) {
        if (str == null) {
            return null;
        }

        if (str.indexOf('%') == -1) {
            // No %nn sequences, so return string unchanged
            return str;
        }

        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        /*
         * Decoding is required.
         *
         * Potential complications:
         * - The source String may be partially decoded so it is not valid to
         *   assume that the source String is ASCII.
         * - Have to process as characters since there is no guarantee that the
         *   byte sequence for '%' is going to be the same in all character
         *   sets.
         * - We don't know how many '%nn' sequences are required for a single
         *   character. It varies between character sets and some use a variable
         *   length.
         */

        // This isn't perfect but it is a reasonable guess for the size of the
        // array required
        ByteArrayOutputStream baos = new ByteArrayOutputStream(str.length() * 2);

        OutputStreamWriter osw = new OutputStreamWriter(baos, charset);

        char[] sourceChars = str.toCharArray();
        int len = sourceChars.length;
        int ix = 0;

        try {
            while (ix < len) {
                char c = sourceChars[ix++];
                if (c == '%') {
                    osw.flush();
                    if (ix + 2 > len) {
                        throw new IllegalArgumentException(
                                sm.getString("uDecoder.urlDecode.missingDigit", str));
                    }
                    char c1 = sourceChars[ix++];
                    char c2 = sourceChars[ix++];
                    if (isHexDigit(c1) && isHexDigit(c2)) {
                        baos.write(x2c(c1, c2));
                    } else {
                        throw new IllegalArgumentException(
                                sm.getString("uDecoder.urlDecode.missingDigit", str));
                    }
                } else {
                    osw.append(c);
                }
            }
            osw.flush();

            return baos.toString(charset.name());
        } catch (IOException ioe) {
            throw new IllegalArgumentException(
                    sm.getString("uDecoder.urlDecode.conversionError", str, charset.name()), ioe);
        }
    }


    private static boolean isHexDigit( int c ) {
        return ( ( c>='0' && c<='9' ) ||
                 ( c>='a' && c<='f' ) ||
                 ( c>='A' && c<='F' ));
    }


    private static int x2c( byte b1, byte b2 ) {
        int digit= (b1>='A') ? ( (b1 & 0xDF)-'A') + 10 :
            (b1 -'0');
        digit*=16;
        digit +=(b2>='A') ? ( (b2 & 0xDF)-'A') + 10 :
            (b2 -'0');
        return digit;
    }


    private static int x2c( char b1, char b2 ) {
        int digit= (b1>='A') ? ( (b1 & 0xDF)-'A') + 10 :
            (b1 -'0');
        digit*=16;
        digit +=(b2>='A') ? ( (b2 & 0xDF)-'A') + 10 :
            (b2 -'0');
        return digit;
    }
}
