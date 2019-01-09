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
package ru.zinal.webdav.fs;

import java.io.File;
import java.io.InputStream;
import ru.zinal.webdav.WebdavContext;
import ru.zinal.webdav.model.*;
import ru.zinal.webdav.util.SmallT;

/**
 *
 * @author zinal
 */
public class FsRoot implements WebResourceRoot {

    private final FsDirectory root;

    public FsRoot(File dir) {
        this.root = new RootDir(dir);
    }

    public FsRoot(String dir) {
        this(new File(dir));
    }

    @Override
    public WebResource getResource(String path) {
        if (path==null)
            return root;
        return getResource( SmallT.splitPath(path) );
    }
    
    private WebResource getResource(String[] path) {
        if (path==null || path.length==0)
            return root;
        WebResource cur = root;
        for (String name : path) {
            cur = cur.lookup(name);
            if (cur==null)
                break;
        }
        return cur;
    }

    @Override
    public WebResource write(String path, InputStream data, boolean overwrite) {
        String[] pathNames = SmallT.splitPath(path);
        if (pathNames.length==0)
            throw new IllegalArgumentException("Cannot write to empty pathname");
        WebResource resource = getResource(pathNames);
        if (resource!=null) {
            if (overwrite) {
                resource.replaceData(data);
                return resource;
            }
        } else {
            String[] parentPath = new String[pathNames.length-1];
            System.arraycopy(pathNames, 0, parentPath, 0, parentPath.length);
            WebResource parent = getResource(parentPath);
            return parent.createFile(path, data);
        }
        return null;
    }

    @Override
    public WebdavContext getContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean mkdir(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private static final class RootDir extends FsDirectory {
        
        public RootDir(File file) {
            super(file);
        }

        @Override
        public boolean delete() {
            throw new UnsupportedOperationException();
        }
        
    }
    
}
