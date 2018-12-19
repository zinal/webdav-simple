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

import javax.naming.directory.DirContext;

/**
 * Implements a cache entry.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 *
 */
public class CacheEntry {
    
    public long timestamp = -1;
    public String name = null;
    public ResourceAttributes attributes = null;
    public WebResource resource = null;
    public boolean exists = true;
    public long accessCount = 0;
    public int sizeEstimation = 1;

    public final boolean isCollection() {
        return (resource!=null)
                && resource.isCollection();
    }
    
    public final DirContext getContext() {
        if ( (resource!=null)
                && resource.isCollection() )
            return ((WebFolder)resource).getContext();
        return null;
    }

    public void recycle() {
        timestamp = -1;
        name = null;
        attributes = null;
        resource = null;
        exists = true;
        accessCount = 0;
        sizeEstimation = 1;
    }

    @Override
    public String toString() {
        return ("Cache entry: " + name + "\n"
                + "Exists: " + exists + "\n"
                + "Attributes: " + attributes + "\n"
                + "Resource: " + resource);
    }


}
