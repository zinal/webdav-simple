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

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import ru.zinal.webdav.util.StringManager;

/**
 * Proxy Directory Context implementation. Implements caching of entries.
 *
 * @author Remy Maucherat
 * @author Maksim Zinal
 */
public class ProxyDirContext implements DirContext {

    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(ProxyDirContext.class);

    /**
     * The string manager for this package.
     */
    protected final StringManager sm
            = StringManager.getManager(Constants.Package);

    /**
     * Associated DirContext.
     */
    protected BaseDirContext dirContext;

    /**
     * Virtual path.
     */
    protected String vPath = null;

    /**
     * Cache.
     */
    protected ResourceCache cache = null;

    /**
     * Cache TTL.
     */
    protected int cacheTTL = 5000; // 5s

    /**
     * Max sizeEstimation of resources which will have their content cached.
     */
    protected int cacheObjectMaxSize = 512; // 512 KB

    /**
     * Immutable name not found exception.
     */
    protected final NameNotFoundException notFoundException
            = new ImmutableNameNotFoundException();

    /**
     * Non cacheable resources.
     */
    protected String[] nonCacheable = {"/WEB-INF/lib/", "/WEB-INF/classes/"};

    /**
     * Builds a proxy directory context using the given environment.
     *
     * @param dirContext
     */
    public ProxyDirContext(BaseDirContext dirContext) {
        this.dirContext = dirContext;
        if (dirContext.isCached()) {
            cacheTTL = dirContext.getCacheTTL();
            cacheObjectMaxSize = dirContext.getCacheObjectMaxSize();
            // cacheObjectMaxSize must be less than cacheMaxSize
            // Set a sensible limit
            if (cacheObjectMaxSize > dirContext.getCacheMaxSize() / 20) {
                cacheObjectMaxSize = dirContext.getCacheMaxSize() / 20;
            }
            try {
                cache = new ResourceCache();
                cache.setCacheMaxSize(cacheObjectMaxSize);
            } catch (Exception e) {
                LOG.warn("Cannot create cache", e);
                cache = null;
            }
        }
    }

    /**
     * Get the cache used for this context.
     *
     * @return
     */
    public ResourceCache getCache() {
        return cache;
    }

    /**
     * Return the actual directory context we are wrapping.
     *
     * @return
     */
    public DirContext getDirContext() {
        return dirContext;
    }

    /**
     * Return the document root for this component.
     *
     * @return
     */
    public String getDocBase() {
        if (dirContext instanceof BaseDirContext) {
            return ((BaseDirContext) dirContext).getDocBase();
        } else {
            return "";
        }
    }
    
    @Override
    public Object lookup(Name name) throws NamingException {
        CacheEntry entry = cacheLookup(name.toString());
        if (entry != null) {
            if (!entry.exists) {
                throw notFoundException;
            }
            return entry.resource;
        }
        Object object = dirContext.lookup(parseName(name));
        return wrapResource(object);
    }

    @Override
    public Object lookup(String name) throws NamingException {
        CacheEntry entry = cacheLookup(name);
        if (entry != null) {
            if (!entry.exists) {
                throw notFoundException;
            }
            return entry.resource;
        }
        Object object = dirContext.lookup(parseName(name));
        return wrapResource(object);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        dirContext.bind(parseName(name), obj);
        cacheUnload(name.toString());
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        dirContext.bind(parseName(name), obj);
        cacheUnload(name);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        dirContext.rebind(parseName(name), obj);
        cacheUnload(name.toString());
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        dirContext.rebind(parseName(name), obj);
        cacheUnload(name);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        dirContext.unbind(parseName(name));
        cacheUnload(name.toString());
    }

    @Override
    public void unbind(String name) throws NamingException {
        dirContext.unbind(parseName(name));
        cacheUnload(name);
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        dirContext.rename(parseName(oldName), parseName(newName));
        cacheUnload(oldName.toString());
    }

    @Override
    public void rename(String oldName, String newName)
            throws NamingException {
        dirContext.rename(parseName(oldName), parseName(newName));
        cacheUnload(oldName);
    }

    @Override
    public NamingEnumeration list(Name name) throws NamingException {
        return dirContext.list(parseName(name));
    }

    @Override
    public NamingEnumeration list(String name) throws NamingException {
        return dirContext.list(parseName(name));
    }

    @Override
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return dirContext.listBindings(parseName(name));
    }

    @Override
    public NamingEnumeration listBindings(String name)
            throws NamingException {
        return dirContext.listBindings(parseName(name));
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        dirContext.destroySubcontext(parseName(name));
        cacheUnload(name.toString());
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        dirContext.destroySubcontext(parseName(name));
        cacheUnload(name);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        Context context = dirContext.createSubcontext(parseName(name));
        cacheUnload(name.toString());
        return context;
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        Context context = dirContext.createSubcontext(parseName(name));
        cacheUnload(name);
        return context;
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return dirContext.lookupLink(parseName(name));
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return dirContext.lookupLink(parseName(name));
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return dirContext.getNameParser(parseName(name));
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return dirContext.getNameParser(parseName(name));
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        prefix = (Name) prefix.clone();
        return prefix.addAll(name);
    }

    @Override
    public String composeName(String name, String prefix)
            throws NamingException {
        return prefix + "/" + name;
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        return dirContext.addToEnvironment(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName)
            throws NamingException {
        return dirContext.removeFromEnvironment(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return dirContext.getEnvironment();
    }

    @Override
    public void close() throws NamingException {
        dirContext.close();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return dirContext.getNameInNamespace();
    }

    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        CacheEntry entry = cacheLookup(name.toString());
        if (entry != null) {
            if (!entry.exists) {
                throw notFoundException;
            }
            return entry.attributes;
        }
        Attributes attributes = dirContext.getAttributes(parseName(name));
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    @Override
    public Attributes getAttributes(String name) throws NamingException {
        CacheEntry entry = cacheLookup(name);
        if (entry != null) {
            if (!entry.exists) {
                throw notFoundException;
            }
            return entry.attributes;
        }
        Attributes attributes = dirContext.getAttributes(parseName(name));
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    @Override
    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        Attributes attributes
                = dirContext.getAttributes(parseName(name), attrIds);
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    @Override
    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException {
        Attributes attributes
                = dirContext.getAttributes(parseName(name), attrIds);
        if (!(attributes instanceof ResourceAttributes)) {
            attributes = new ResourceAttributes(attributes);
        }
        return attributes;
    }

    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        dirContext.modifyAttributes(parseName(name), mod_op, attrs);
        cacheUnload(name.toString());
    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        dirContext.modifyAttributes(parseName(name), mod_op, attrs);
        cacheUnload(name);
    }

    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        dirContext.modifyAttributes(parseName(name), mods);
        cacheUnload(name.toString());
    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException {
        dirContext.modifyAttributes(parseName(name), mods);
        cacheUnload(name);
    }

    @Override
    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        dirContext.bind(parseName(name), obj, attrs);
        cacheUnload(name.toString());
    }

    @Override
    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException {
        dirContext.bind(parseName(name), obj, attrs);
        cacheUnload(name);
    }

    @Override
    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        dirContext.rebind(parseName(name), obj, attrs);
        cacheUnload(name.toString());
    }

    @Override
    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException {
        dirContext.rebind(parseName(name), obj, attrs);
        cacheUnload(name);
    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException {
        DirContext context
                = dirContext.createSubcontext(parseName(name), attrs);
        cacheUnload(name.toString());
        return context;
    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException {
        DirContext context
                = dirContext.createSubcontext(parseName(name), attrs);
        cacheUnload(name);
        return context;
    }

    @Override
    public DirContext getSchema(Name name)
            throws NamingException {
        return dirContext.getSchema(parseName(name));
    }

    @Override
    public DirContext getSchema(String name)
            throws NamingException {
        return dirContext.getSchema(parseName(name));
    }

    @Override
    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        return dirContext.getSchemaClassDefinition(parseName(name));
    }

    @Override
    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        return dirContext.getSchemaClassDefinition(parseName(name));
    }

    @Override
    public NamingEnumeration search(Name name, Attributes matchingAttributes,
            String[] attributesToReturn)
            throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes,
                attributesToReturn);
    }

    @Override
    public NamingEnumeration search(String name, Attributes matchingAttributes,
            String[] attributesToReturn)
            throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes,
                attributesToReturn);
    }

    @Override
    public NamingEnumeration search(Name name, Attributes matchingAttributes)
            throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes);
    }

    @Override
    public NamingEnumeration search(String name, Attributes matchingAttributes)
            throws NamingException {
        return dirContext.search(parseName(name), matchingAttributes);
    }

    @Override
    public NamingEnumeration search(Name name, String filter,
            SearchControls cons)
            throws NamingException {
        return dirContext.search(parseName(name), filter, cons);
    }

    @Override
    public NamingEnumeration search(String name, String filter,
            SearchControls cons)
            throws NamingException {
        return dirContext.search(parseName(name), filter, cons);
    }

    @Override
    public NamingEnumeration search(Name name, String filterExpr,
            Object[] filterArgs, SearchControls cons)
            throws NamingException {
        return dirContext.search(parseName(name), filterExpr, filterArgs,
                cons);
    }

    @Override
    public NamingEnumeration search(String name, String filterExpr,
            Object[] filterArgs, SearchControls cons)
            throws NamingException {
        return dirContext.search(parseName(name), filterExpr, filterArgs,
                cons);
    }

    /**
     * Wraps the resource object to appropriate resource handler
     *
     * @param object
     * @return Wrapped object
     */
    public static WebResource wrapResource(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof InputStream) {
            return new WebResourceStream((InputStream) object);
        } else if (object instanceof DirContext) {
            return new WebFolder((DirContext)object);
        } else if (object instanceof WebResource) {
            return (WebResource)object;
        } else if (object instanceof byte[]) {
            return new WebResourceData((byte[]) object);
        } else {
            // Strange input resource
            return new WebResourceData(object.toString().getBytes());
        }
    }

    /**
     * Find the named entry in a cache
     * @param name
     * @return 
     */
    public CacheEntry lookupCache(String name) {
        CacheEntry entry = cacheLookup(name);
        if (entry == null) {
            entry = new CacheEntry();
            entry.name = name;
            try {
                Object object = dirContext.lookup(parseName(name));
                entry.resource = wrapResource(object);
                Attributes attributes = dirContext.getAttributes(parseName(name));
                if (!(attributes instanceof ResourceAttributes)) {
                    attributes = new ResourceAttributes(attributes);
                }
                entry.attributes = (ResourceAttributes) attributes;
            } catch (NamingException e) {
                entry.exists = false;
            }
        }
        return entry;
    }

    /**
     * Parses a name.
     *
     * @param name
     * @return the parsed name
     * @throws javax.naming.NamingException
     */
    protected String parseName(String name) throws NamingException {
        return name;
    }

    /**
     * Parses a name.
     *
     * @param name
     * @return the parsed name
     * @throws javax.naming.NamingException
     */
    protected Name parseName(Name name) throws NamingException {
        return name;
    }

    /**
     * Lookup the named resource in the cache.
     * @param name Resource name
     * @return Cache entry, if found, or null otherwise
     */
    protected CacheEntry cacheLookup(String name) {
        if (cache == null) {
            return (null);
        }
        if (name == null) {
            name = "";
        }
        for (String entry : nonCacheable) {
            if (name.startsWith(entry)) {
                return (null);
            }
        }
        CacheEntry cacheEntry = cache.lookup(name);
        if (cacheEntry == null) {
            cacheEntry = new CacheEntry();
            cacheEntry.name = name;
            // Load entry
            cacheLoad(cacheEntry);
        } else {
            if (!validate(cacheEntry)) {
                if (!revalidate(cacheEntry)) {
                    cacheUnload(cacheEntry.name);
                    return (null);
                } else {
                    cacheEntry.timestamp
                            = System.currentTimeMillis() + cacheTTL;
                }
            }
            cacheEntry.accessCount++;
        }
        return (cacheEntry);
    }

    /**
     * Validate entry.
     *
     * @param entry
     * @return
     */
    protected boolean validate(CacheEntry entry) {
        if (System.currentTimeMillis() >= entry.timestamp)
            return false;
        if (entry.exists) {
            return (entry.resource!=null)
                    && ( (entry.resource.hasContent()
                        || entry.resource.isCollection()) );
        }
        return false;
    }

    /**
     * Revalidate entry.
     *
     * @param entry
     * @return
     */
    protected boolean revalidate(CacheEntry entry) {
        // Get the attributes at the given path, and check the last 
        // modification date
        if (!entry.exists) {
            return false;
        }
        if (entry.attributes == null) {
            return false;
        }
        long lastModified = entry.attributes.getLastModified();
        long contentLength = entry.attributes.getContentLength();
        if (lastModified <= 0) {
            return false;
        }
        try {
            Attributes tempAttributes = dirContext.getAttributes(entry.name);
            final ResourceAttributes attributes;
            if (!(tempAttributes instanceof ResourceAttributes)) {
                attributes = new ResourceAttributes(tempAttributes);
            } else {
                attributes = (ResourceAttributes) tempAttributes;
            }
            long lastModified2 = attributes.getLastModified();
            long contentLength2 = attributes.getContentLength();
            return (lastModified == lastModified2)
                    && (contentLength == contentLength2);
        } catch (NamingException e) {
            return false;
        }
    }

    /**
     * Load entry into cache.
     *
     * @param entry
     */
    protected void cacheLoad(CacheEntry entry) {

        String name = entry.name;

        // Retrieve missing info
        boolean exists = true;

        // Retrieving attributes
        if (entry.attributes == null) {
            try {
                Attributes attributes = dirContext.getAttributes(entry.name);
                if (!(attributes instanceof ResourceAttributes)) {
                    entry.attributes
                            = new ResourceAttributes(attributes);
                } else {
                    entry.attributes = (ResourceAttributes) attributes;
                }
            } catch (NamingException e) {
                exists = false;
            }
        }

        // Retriving object
        if (exists && (entry.resource == null)) {
            try {
                Object object = dirContext.lookup(name);
                entry.resource = wrapResource(object);
            } catch (NamingException e) {
                exists = false;
            }
        }

        // Load object content
        if ((exists) && (entry.resource != null)
                && entry.resource.hasContent()
                && (entry.attributes.getContentLength() >= 0)
                && (entry.attributes.getContentLength()
                < (cacheObjectMaxSize * 1024))) {
            int length = (int) entry.attributes.getContentLength();
            // The entry sizeEstimation is 1 + the resource sizeEstimation in KB, if it will be 
            // cached
            entry.sizeEstimation += (entry.attributes.getContentLength() / 1024);
            InputStream is = null;
            try {
                is = entry.resource.streamContent();
                int pos = 0;
                byte[] b = new byte[length];
                while (pos < length) {
                    int n = is.read(b, pos, length - pos);
                    if (n < 0) {
                        break;
                    }
                    pos = pos + n;
                }
                entry.resource = new WebResourceData(b);
            } catch (IOException e) {
                ; // Ignore
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    ; // Ignore
                }
            }
        }

        // Set existence flag
        entry.exists = exists;

        // Set timestamp
        entry.timestamp = System.currentTimeMillis() + cacheTTL;

        // Add new entry to cache
        synchronized (cache) {
            // Check cache sizeEstimation, and remove elements if too big
            if ((cache.lookup(name) == null) 
                    && cache.allocate(entry.sizeEstimation)) {
                cache.load(entry);
            }
        }

    }

    /**
     * Remove entry from cache.
     * @param name
     * @return 
     */
    protected boolean cacheUnload(String name) {
        if (cache == null) {
            return false;
        }
        // To ensure correct operation, particularly of WebDAV, unload
        // the resource with and without a trailing /
        String name2;
        if (name.endsWith("/")) {
            name2 = name.substring(0, name.length() - 1);
        } else {
            name2 = name + "/";
        }
        synchronized (cache) {
            boolean result = cache.unload(name);
            cache.unload(name2);
            return result;
        }
    }
}
