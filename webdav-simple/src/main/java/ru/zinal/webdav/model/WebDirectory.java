/*
 * Copyright 2018 zinal.
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
package ru.zinal.webdav.model;

import java.io.InputStream;

/**
 *
 * @author zinal
 */
public abstract class WebDirectory extends WebResource {

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    /**
     * Default generic iterative implementation of name lookup
     * @param names Sub-names array
     * @return resource link, or null of the resource was not found
     */
    @Override
    public WebResource lookupDeep(String[] names) {
        WebResource root = lookup("");
        if (names==null || names.length==0)
            return root;
        WebResource cur = root;
        for (String name : names) {
            cur = cur.lookup(name);
            if (cur==null)
                break;
        }
        return cur;
    }

    @Override
    public InputStream getData() {
        throw new UnsupportedOperationException("No getData() for WebDirectory");
    }

    @Override
    public InputStream getData(long start, long finish) {
        throw new UnsupportedOperationException("No getData() for WebDirectory");
    }

    @Override
    public boolean replaceData(InputStream data) {
        throw new UnsupportedOperationException("No replaceData() for WebDirectory");
    }

    @Override
    public boolean replaceData(InputStream data, long start) {
        throw new UnsupportedOperationException("No replaceData() for WebDirectory");
    }
    
}
