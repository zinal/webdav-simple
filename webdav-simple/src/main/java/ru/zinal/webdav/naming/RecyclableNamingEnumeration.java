/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package ru.zinal.webdav.naming;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Naming enumeration implementation.
 *
 * @author Remy Maucherat
 *
 */
public class RecyclableNamingEnumeration implements NamingEnumeration {

    /**
     * Entries.
     */
    protected final List<?> entries;

    /**
     * Underlying enumeration.
     */
    protected Enumeration enumeration;

    public RecyclableNamingEnumeration(List<?> entries) {
        this.entries = Collections.unmodifiableList(entries);
        this.enumeration = Collections.enumeration(this.entries);
    }

    @Override
    public Object next() throws NamingException {
        return nextElement();
    }

    @Override
    public boolean hasMore() throws NamingException {
        return enumeration.hasMoreElements();
    }

    @Override
    public void close() throws NamingException {
    }

    @Override
    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

    @Override
    public Object nextElement() {
        return enumeration.nextElement();
    }

    /**
     * Recycle.
     */
    public void recycle() {
    	enumeration = Collections.enumeration(entries);
    }

}

