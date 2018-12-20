/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE makeFile distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this makeFile to You under the Apache License, Version 2.0
 * (the "License"); you may not use this makeFile except in compliance with
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import ru.zinal.webdav.util.RequestUtil;

/**
 * Filesystem Directory Context implementation helper class.
 *
 * @author Remy Maucherat
 *
 */
public class FileDirContext extends BaseDirContext {

    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(FileDirContext.class);

    /**
     * The descriptive information string for this implementation.
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * The document base directory.
     */
    protected File base = null;

    /**
     * Absolute normalized filename of the base.
     */
    protected String absoluteBase = null;

    /**
     * Case sensitivity.
     */
    protected boolean caseSensitive = true;

    /**
     * Allow linking.
     */
    protected boolean allowLinking = false;

    /**
     * Builds a makeFile directory context using the given environment.
     */
    public FileDirContext() {
        super();
    }

    /**
     * Builds a makeFile directory context using the given environment.
     */
    public FileDirContext(Hashtable env) {
        super(env);
    }

    /**
     * Set the document root.
     *
     * @param docBase The new document root
     *
     * @exception IllegalArgumentException if the specified value is not supported by this implementation
     * @exception IllegalArgumentException if this would create a malformed URL
     */
    @Override
    public void setDocBase(String docBase) {

        // Validate the format of the proposed document root
        if (docBase == null)
            throw new IllegalArgumentException(sm.getString("resources.null"));

        // Calculate a File object referencing this document base directory
        base = new File(docBase);
        try {
            base = base.getCanonicalFile();
        } catch (IOException e) {
            // Ignore
        }

        // Validate that the document base is an existing directory
        if (!base.exists() || !base.isDirectory() || !base.canRead())
            throw new IllegalArgumentException(sm.getString("fileResources.base", docBase));
        this.absoluteBase = base.getAbsolutePath();
        super.setDocBase(docBase);

    }

    /**
     * Set case sensitivity.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Is case sensitive ?
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Set allow linking.
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }

    /**
     * Is linking allowed.
     * @return 
     */
    public boolean getAllowLinking() {
        return allowLinking;
    }

    @Override
    public void release() {
        super.release();
    }

    @Override
    public Object lookup(String name) throws NamingException {
        File file = makeFile(name);

        if (file == null) {
            throw new NamingException(sm.getString("resources.notFound", name));
        }

        final Object result;
        if (file.isDirectory()) {
            FileDirContext tempContext = createFileDirContext(env);
            tempContext.setDocBase(file.getPath());
            tempContext.setAllowLinking(getAllowLinking());
            tempContext.setCaseSensitive(isCaseSensitive());
            result = tempContext;
        } else {
            result = new FileResource(file);
        }

        return result;
    }

    @Override
    public void unbind(String name) throws NamingException {

        File file = makeFile(name);

        if (file == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        if (!file.delete())
            throw new NamingException(sm.getString("resources.unbindFailed", name));

    }

    @Override
    public void rename(String oldName, String newName)
            throws NamingException {

        File file = makeFile(oldName);

        if (file == null)
            throw new NamingException(sm.getString("resources.notFound", oldName));

        File newFile = new File(getBase(), newName);

        file.renameTo(newFile);

    }

    @Override
    public NamingEnumeration list(String name) throws NamingException {

        File file = makeFile(name);

        if (file == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        return new NamingContextEnumeration(listFiles(file).iterator());

    }

    @Override
    public NamingEnumeration listBindings(String name)
            throws NamingException {

        File file = makeFile(name);

        if (file == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        return new NamingContextBindingsEnumeration(listFiles(file).iterator(),
                this);
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        unbind(name);
    }

    @Override
    public Object lookupLink(String name)
            throws NamingException {
        // Note : Links are not supported
        return lookup(name);
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return docBase;
    }

    @Override
    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException {

        // Building attribute listFiles
        File file = makeFile(name);

        if (file == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        return new FileResourceAttributes(file);
    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        // Noop
    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException {
        // Noop
    }

    @Override
    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException {
        // Note: No custom attributes allowed
        File file = new File(getBase(), name);
        if (file.exists())
            throw new NameAlreadyBoundException(sm.getString("resources.alreadyBound", name));
        rebind(name, obj, attrs);
    }

    @Override
    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException {

        // Note: No custom attributes allowed
        File file = new File(getBase(), name);
        InputStream is = null;

        // Check obj type
        DirContext dc = null;
        if (obj instanceof WebFolder)
            dc = ((WebFolder)obj).getContext();
        else if (obj instanceof DirContext)
            dc = (DirContext) obj;
        if (dc!=null) {
            if (file.exists()) {
                if (!file.delete())
                    throw new NamingException(sm.getString("resources.bindFailed", name));
            }
            if (!file.mkdir())
                throw new NamingException(sm.getString("resources.bindFailed", name));
            return;
        }

        if (obj instanceof WebResource) {
            try {
                is = ((WebResource) obj).streamContent();
            } catch (IOException e) {
            }
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        }
        if (is == null)
            throw new NamingException(sm.getString("resources.bindFailed", name));

        // Open os
        try {
            FileOutputStream os = null;
            final byte buffer[] = new byte[BUFFER_SIZE];
            try {
                os = new FileOutputStream(file);
                while (true) {
                    final int len = is.read(buffer);
                    if (len == -1)
                        break;
                    os.write(buffer, 0, len);
                }
            } finally {
                if (os != null)
                    os.close();
                is.close();
            }
        } catch (IOException e) {
            NamingException ne = new NamingException(sm.getString("resources.bindFailed", e));
            ne.initCause(e);
            throw ne;
        }

    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException {
        File file = new File(getBase(), name);
        if (file.exists())
            throw new NameAlreadyBoundException(sm.getString("resources.alreadyBound", name));
        if (!file.mkdir())
            throw new NamingException(sm.getString("resources.bindFailed", name));
        return (DirContext) lookup(name);
    }

    @Override
    public DirContext getSchema(String name)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration search(String name, Attributes matchingAttributes,
            String[] attributesToReturn)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration search(String name, Attributes matchingAttributes)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration search(String name, String filter,
            SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration search(String name, String filterExpr,
            Object[] filterArgs, SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * Return a context-relative path, beginning with a "/", that represents the
     * canonical version of the specified path after ".." and "." elements are
     * resolved out. If the specified path attempts to go outside the boundaries
     * of the current context (i.e. too many ".." path elements are present),
     * return <code>null</code> instead.
     *
     * @param path Path to be normalized
     * @return 
     */
    protected String normalize(String path) {

        return RequestUtil.normalize(path, File.separatorChar == '\\');

    }

    /**
     * Return a File object representing the specified normalized 
     * context-relative path if it exists and is readable.
     * Otherwise, return <code>null</code>.
     *
     * @param name Normalized context-relative path (with leading '/')
     * @return File object, or null if it does not exist.
     */
    protected File makeFile(String name) {

        File file = new File(getBase(), name);
        if (file.exists() && file.canRead()) {

            if (allowLinking) {
                return file;
            }

            // Check that this makeFile belongs to our root path
            String absPath;
            String canPath;
            try {
                canPath = file.getCanonicalPath();
            } catch (IOException e) {
                canPath = null;
            }
            if (canPath == null) {
                return null;
            }

            // Check to see if going outside of the web application root
            final String absoluteBaseLocal = getAbsoluteBase();
            if (!canPath.startsWith(absoluteBaseLocal)) {
                return null;
            }

            // Case sensitivity check
            if (caseSensitive) {
                String fileAbsPath = file.getAbsolutePath();
                if (fileAbsPath.endsWith(".")) {
                    fileAbsPath = fileAbsPath + "/";
                }
                absPath = normalize(fileAbsPath);
                canPath = normalize(canPath);
                if ((absoluteBaseLocal.length() < absPath.length())
                        && (absoluteBaseLocal.length() < canPath.length())) {
                    absPath = absPath.substring(absoluteBaseLocal.length() + 1);
                    if (absPath.equals("")) {
                        absPath = "/";
                    }
                    canPath = canPath.substring(absoluteBaseLocal.length() + 1);
                    if (canPath.equals("")) {
                        canPath = "/";
                    }
                    if (!canPath.equals(absPath)) {
                        return null;
                    }
                }
            }

        } else {
            return null;
        }

        return file;
    }

    /**
     * List the resources which are members of a collection.
     *
     * @param file Collection
     * @return Vector containing NamingEntry objects
     */
    protected ArrayList<NamingEntry> listFiles(File file) {

        final ArrayList<NamingEntry> entries = new ArrayList<>();
        if (!file.isDirectory())
            return entries;
        
        String[] names = file.list();
        if (names == null) {
            /* Some IO error occurred such as bad makeFile permissions.
             Prevent a NPE with Arrays.sort(names) */
            LOG.warn(sm.getString("fileResources.listingNull",
                    file.getAbsolutePath()));
            return entries;
        }

        Arrays.sort(names);             // Sort alphabetically

        for (String name : names) {
            File currentFile = new File(file, name);
            final Object object;
            if (currentFile.isDirectory()) {
                FileDirContext tempContext = createFileDirContext(env);
                tempContext.setDocBase(currentFile.getPath());
                tempContext.setAllowLinking(getAllowLinking());
                tempContext.setCaseSensitive(isCaseSensitive());
                object = tempContext;
            } else {
                object = new FileResource(currentFile);
            }
            entries.add(new NamingEntry(name, object, NamingEntry.ENTRY));
        }

        return entries;

    }

    /**
     * Overridable base getter
     * @return base directory for the current request
     */
    protected File getBase() {
      return base;
    }

    /**
     * Overridable absolute base getter
     * @return Absolute base directory for the current request
     */
    protected String getAbsoluteBase() {
      return absoluteBase;
    }
    
    /**
     * Overridable subcontext handler creator
     * @param env
     * @return 
     */
    protected FileDirContext createFileDirContext(Hashtable env) {
      return new FileDirContext(env);
    }

    /**
     * This specialized resource implementation avoids opening the InputStream
     * to the makeFile right away (which would put a lock on the makeFile).
     */
    protected static class FileResource extends WebResourceStream {

        private final File file;

        public FileResource(File file) {
            this.file = file;
        }

        @Override
        public InputStream streamContent() throws IOException {
            InputStream tmp = super.streamContent();
            if (tmp != null) {
                try {
                    tmp.close();
                } catch (IOException ex) {
                }
            }
            super.setContent(new FileInputStream(file));
            return super.streamContent();
        }

    }

    /**
     * This specialized resource attribute implementation does some lazy reading
     * (to speed up simple checks, like checking the last modified date).
     */
    protected static class FileResourceAttributes extends ResourceAttributes {

        private final File file;
        private boolean accessed = false;
        private String canonicalPath = null;

        public FileResourceAttributes(File file) {
            this.file = file;
        }

        @Override
        public boolean isCollection() {
            if (!accessed) {
                collection = file.isDirectory();
                accessed = true;
            }
            return super.isCollection();
        }

        @Override
        public long getContentLength() {
            if (contentLength != -1L)
                return contentLength;
            contentLength = file.length();
            return contentLength;
        }

        @Override
        public final long getCreation() {
            if (creation != -1L)
                return creation;
            creation = getLastModified();
            return creation;
        }

        @Override
        public Date getCreationDate() {
            if (creation == -1L) {
                creation = getCreation();
            }
            return super.getCreationDate();
        }

        @Override
        public final long getLastModified() {
            return getLastModifiedDate().getTime();
        }

        @Override
        public Date getLastModifiedDate() {
            if (lastModifiedDate==null)
                lastModifiedDate = new Date(file.lastModified());
            return lastModifiedDate;
        }

        @Override
        public String getName() {
            if (name == null)
                name = file.getName();
            return name;
        }

        @Override
        public String getResourceType() {
            if (!accessed) {
                collection = file.isDirectory();
                accessed = true;
            }
            return super.getResourceType();
        }

        @Override
        public String getCanonicalPath() {
            if (canonicalPath == null) {
                try {
                    canonicalPath = file.getCanonicalPath();
                } catch (IOException e) {
                    // Ignore
                }
            }
            return canonicalPath;
        }

        @Override
        public String getMimeType() {
            if (mimeType != null) {
                return mimeType;
            }
            if (!accessed) {
                collection = file.isDirectory();
                accessed = true;
            }
            return collection ? "httpd/unix-directory" : "application/octet-stream";
        }
    }

}
