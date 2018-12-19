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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import ru.zinal.webdav.util.StringManager;

/**
 * Directory Context implementation helper class.
 *
 * @author Remy Maucherat
 *
 */
public abstract class BaseDirContext implements DirContext {

    /**
     * The document base path.
     */
    protected String docBase = null;

    /**
     * Environment.
     */
    protected final Hashtable env;

    /**
     * The string manager for this package.
     */
    protected final StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * Name parser for this context.
     */
    protected final NameParser nameParser = new NameParserImpl();

    /**
     * Cached.
     */
    protected boolean cached = true;

    /**
     * Cache TTL.
     */
    protected int cacheTTL = 5000; // 5s

    /**
     * Max size of cache for resources.
     */
    protected int cacheMaxSize = 10240; // 10 MB

    /**
     * Max size of resources that will be content cached.
     */
    protected int cacheObjectMaxSize = 512; // 512 K

    /**
     * Builds a base directory context.
     */
    public BaseDirContext() {
        this.env = new Hashtable();
    }

    /**
     * Builds a base directory context using the given environment.
     */
    public BaseDirContext(Hashtable env) {
        this.env = env;
    }

    /**
     * Return the document root for this component.
     */
    public String getDocBase() {
        return (this.docBase);
    }

    /**
     * Set the document root for this component.
     *
     * @param docBase The new document root
     *
     * @exception IllegalArgumentException if the specified value is not
     * supported by this implementation
     * @exception IllegalArgumentException if this would create a malformed URL
     */
    public void setDocBase(String docBase) {

        // Validate the format of the proposed document root
        if (docBase == null) {
            throw new IllegalArgumentException(sm.getString("resources.null"));
        }

        // Change the document root property
        this.docBase = docBase;

    }

    /**
     * Set cached.
     *
     * @param cached
     */
    public void setCached(boolean cached) {
        this.cached = cached;
    }

    /**
     * Is cached ?
     *
     * @return
     */
    public boolean isCached() {
        return cached;
    }

    /**
     * Set cache TTL.
     *
     * @param cacheTTL
     */
    public void setCacheTTL(int cacheTTL) {
        this.cacheTTL = cacheTTL;
    }

    /**
     * Get cache TTL.
     *
     * @return
     */
    public int getCacheTTL() {
        return cacheTTL;
    }

    /**
     * Return the maximum size of the cache in KB.
     *
     * @return
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    /**
     * Set the maximum size of the cache in KB.
     *
     * @param cacheMaxSize
     */
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    /**
     * Return the maximum size of objects to be cached in KB.
     *
     * @return
     */
    public int getCacheObjectMaxSize() {
        return cacheObjectMaxSize;
    }

    /**
     * Set the maximum size of objects to be placed the cache in KB.
     *
     * @param cacheObjectMaxSize
     */
    public void setCacheObjectMaxSize(int cacheObjectMaxSize) {
        this.cacheObjectMaxSize = cacheObjectMaxSize;
    }

    /**
     * Allocate resources for this directory context.
     */
    public void allocate() {
        // No action taken by the default implementation
    }

    /**
     * Release any resources allocated for this directory context.
     */
    public void release() {
        // No action taken by the default implementation
    }

    @Override
    public Object lookup(Name name)
            throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public abstract Object lookup(String name)
            throws NamingException;

    @Override
    public void bind(Name name, Object obj)
            throws NamingException {
        bind(name.toString(), obj);
    }

    @Override
    public void bind(String name, Object obj)
            throws NamingException {
        bind(name, obj, null);
    }

    @Override
    public void rebind(Name name, Object obj)
            throws NamingException {
        rebind(name.toString(), obj);
    }

    @Override
    public void rebind(String name, Object obj)
            throws NamingException {
        rebind(name, obj, null);
    }

    @Override
    public void unbind(Name name)
            throws NamingException {
        unbind(name.toString());
    }

    @Override
    public abstract void unbind(String name)
            throws NamingException;

    @Override
    public void rename(Name oldName, Name newName)
            throws NamingException {
        rename(oldName.toString(), newName.toString());
    }

    @Override
    public abstract void rename(String oldName, String newName)
            throws NamingException;

    @Override
    public NamingEnumeration list(Name name)
            throws NamingException {
        return list(name.toString());
    }

    @Override
    public abstract NamingEnumeration list(String name)
            throws NamingException;

    @Override
    public NamingEnumeration listBindings(Name name)
            throws NamingException {
        return listBindings(name.toString());
    }

    @Override
    public abstract NamingEnumeration listBindings(String name)
            throws NamingException;

    @Override
    public void destroySubcontext(Name name)
            throws NamingException {
        destroySubcontext(name.toString());
    }

    @Override
    public abstract void destroySubcontext(String name)
            throws NamingException;

    @Override
    public Context createSubcontext(Name name)
            throws NamingException {
        return createSubcontext(name.toString());
    }

    @Override
    public Context createSubcontext(String name)
            throws NamingException {
        return createSubcontext(name, null);
    }

    @Override
    public Object lookupLink(Name name)
            throws NamingException {
        return lookupLink(name.toString());
    }

    @Override
    public abstract Object lookupLink(String name)
            throws NamingException;

    @Override
    public NameParser getNameParser(Name name)
            throws NamingException {
        return new NameParserImpl();
    }

    @Override
    public NameParser getNameParser(String name)
            throws NamingException {
        return new NameParserImpl();
    }

    @Override
    public Name composeName(Name name, Name prefix)
            throws NamingException {
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
        return env.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName)
            throws NamingException {
        return env.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment()
            throws NamingException {
        return env;
    }

    @Override
    public void close()
            throws NamingException {
        env.clear();
    }

    @Override
    public abstract String getNameInNamespace()
            throws NamingException;

    @Override
    public Attributes getAttributes(Name name)
            throws NamingException {
        return getAttributes(name.toString());
    }

    @Override
    public Attributes getAttributes(String name)
            throws NamingException {
        return getAttributes(name, null);
    }

    @Override
    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        return getAttributes(name.toString(), attrIds);
    }

    @Override
    public abstract Attributes getAttributes(String name, String[] attrIds)
            throws NamingException;

    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        modifyAttributes(name.toString(), mod_op, attrs);
    }

    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        modifyAttributes(name.toString(), mods);
    }

    @Override
    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        bind(name.toString(), obj, attrs);
    }

    @Override
    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        rebind(name.toString(), obj, attrs);
    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException {
        return createSubcontext(name.toString(), attrs);
    }

    @Override
    public DirContext getSchema(Name name)
            throws NamingException {
        return getSchema(name.toString());
    }

    @Override
    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        return getSchemaClassDefinition(name.toString());
    }

    @Override
    public NamingEnumeration search(Name name, Attributes matchingAttributes,
            String[] attributesToReturn)
            throws NamingException {
        return search(name.toString(), matchingAttributes, attributesToReturn);
    }

    @Override
    public NamingEnumeration search(Name name, Attributes matchingAttributes)
            throws NamingException {
        return search(name.toString(), matchingAttributes);
    }

    @Override
    public NamingEnumeration search(Name name, String filter, SearchControls cons)
            throws NamingException {
        return search(name.toString(), filter, cons);
    }

    @Override
    public NamingEnumeration search(Name name, String filterExpr,
            Object[] filterArgs, SearchControls cons)
            throws NamingException {
        return search(name.toString(), filterExpr, filterArgs, cons);
    }

}
