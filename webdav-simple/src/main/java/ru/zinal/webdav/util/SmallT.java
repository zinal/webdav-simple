/*
 * Copyright 2019 zinal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.zinal.webdav.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zinal
 */
public class SmallT {
    
    private static final Pattern RX_TOKEN 
            = Pattern.compile(".*:([a-zA-Z0-9\\-]+)>.*");
    
    public static String extractToken(String value) {
        if (value==null || value.length()==0)
            return "";
        Matcher m = RX_TOKEN.matcher(value);
        if (! m.matches())
            throw new IllegalArgumentException("Illegal token data: " + value);
        return m.group(1);
    }

    public static String normalizePath(String path) {
        if (path==null)
            return "/";
        path = path.trim().replace('\\', '/');
        int oldlen;
        int newlen = path.length();
        do {
            oldlen = newlen;
            path = path.replace("//", "/");
            newlen = path.length();
        } while (oldlen != newlen);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        newlen = path.length();
        do {
            oldlen = newlen;
            path = path.replaceAll("/[.]+/", "/");
            newlen = path.length();
        } while (oldlen != newlen);
        return path;
    }

    public static String[] splitPath(String path) {
        path = normalizePath(path);
        if ("/".equals(path)) {
            return new String[0];
        }
        if (path.endsWith("/")) {
            return path.substring(1, path.length() - 1).split("[/]");
        } else {
            return path.substring(1, path.length()).split("[/]");
        }
    }
    
}
