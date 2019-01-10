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
package ru.zinal.webdav.model;

import java.io.InputStream;
import java.util.List;
import ru.zinal.webdav.util.SmallT;

/**
 *
 * @author zinal
 */
public class WebRootImpl implements WebRoot {
    
    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(WebRootImpl.class);
    
    private final WebdavContext context;
    private final RootWrapper root;

    public WebRootImpl(WebdavContext context, WebDirectory root) {
        this.context = context;
        this.root = new RootWrapper(root);
    }

    @Override
    public WebResource getResource(String path) {
        if (path==null || path.length()==0 || "/".equals(path))
            return root;
        return root.lookupDeep(SmallT.splitPath(path));
    }

    @Override
    public WebResource write(String path, InputStream data, boolean overwrite) {
        String[] pathNames = SmallT.splitPath(path);
        if (pathNames.length==0) {
            LOG.warn("Attempt to write to empty pathname [{}]", path);
            return null;
        }
        WebResource resource = root.lookupDeep(pathNames);
        if (resource!=null) {
            if (overwrite) {
                resource.replaceData(data);
                return resource;
            }
        } else {
            String[] parentPath = new String[pathNames.length-1];
            System.arraycopy(pathNames, 0, parentPath, 0, parentPath.length);
            WebResource parent = root.lookupDeep(parentPath);
            return parent.createFile(path, data);
        }
        return null;
    }

    @Override
    public WebResource mkdir(String path) {
        String[] pathNames = SmallT.splitPath(path);
        if (pathNames.length<1) {
            LOG.warn("Attempt to mkdir empty pathname [{}]", path);
            return null;
        }
        String[] parentPath = new String[pathNames.length-1];
        System.arraycopy(pathNames, 0, parentPath, 0, parentPath.length);
        WebResource parent = root.lookupDeep(parentPath);
        if (parent==null)
            return null;
        return parent.createDirectory(pathNames[pathNames.length-1]);
    }

    @Override
    public WebdavContext getContext() {
        return context;
    }
    
    private static final class RootWrapper extends WebDirectory {
        
        private final WebDirectory dir;

        public RootWrapper(WebDirectory dir) {
            this.dir = dir;
        }
        
        @Override
        public long getCreation() {
            return dir.getCreation();
        }

        @Override
        public long getLastModified() {
            return dir.getLastModified();
        }

        @Override
        public boolean delete() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WebResource lookup(String name) {
            return dir.lookup(name);
        }

        @Override
        public WebResource lookupDeep(String[] names) {
            return dir.lookupDeep(names);
        }

        @Override
        public WebResource createDirectory(String name) {
            return dir.createDirectory(name);
        }

        @Override
        public WebResource createFile(String name, InputStream data) {
            return dir.createFile(name, data);
        }

        @Override
        public boolean delete(String name) {
            return dir.delete(name);
        }

        @Override
        public List<String> list() {
            return dir.list();
        }

        @Override
        public long getContentLength() {
            return dir.getContentLength();
        }
        
    }
    
}
