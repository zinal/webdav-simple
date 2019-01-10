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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import ru.zinal.webdav.model.*;

/**
 *
 * @author zinal
 */
public class FsDirectory extends WebDirectory {
    
    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(FsDirectory.class);
    
    private final File file;
    private long createdAt = 0L;
    
    public FsDirectory(File file) {
        this.file = file;
    }
    
    public FsDirectory(String pathname) {
        this.file = new File(pathname);
    }
    
    @Override
    public long getCreation() {
        if (createdAt==0L) {
            try {
                BasicFileAttributes bfa = Files.readAttributes(file.toPath(), 
                        BasicFileAttributes.class);
                createdAt = bfa.creationTime().toMillis();
            } catch(IOException ex) {
                LOG.warn("Cannot get creation time for directory {}", file, ex);
                createdAt = 1L;
            }
        }
        return createdAt;
    }
    
    @Override
    public long getLastModified() {
        return file.lastModified();
    }

    @Override
    public long getContentLength() {
        return file.length();
    }

    @Override
    public WebResource lookup(String name) {
        if (name==null || name.length()==0)
            return this;
        name = secureName(name);
        final File f = new File(file, name);
        if (!f.exists())
            return null;
        if (f.isDirectory())
            return new FsDirectory(f);
        return new FsFile(f);
    }

    @Override
    public WebResource lookupDeep(String[] names) {
        if (names==null || names.length==0)
            return this;
        File f = file;
        for (String name : names) {
            f = new File(f, secureName(name));
        }
        if (!f.exists())
            return null;
        if (f.isDirectory())
            return new FsDirectory(f);
        return new FsFile(f);
    }

    @Override
    public WebResource createDirectory(String name) {
        final File f = new File(file, secureName(name));
        if ( f.mkdir() )
            return new FsDirectory(f);
        return null;
    }
    
    @Override
    public WebResource createFile(String name, InputStream data) {
        final File f = new File(file, secureName(name));
        if (f.exists())
            return null;
        if (data!=null) {
            try {
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    final byte datum[] = new byte[32768];
                    while (true) {
                        int bytes = data.read(datum);
                        if (bytes<=0) break;
                        fos.write(datum, 0, bytes);
                    }
                }
            } catch(Exception ex) {
                LOG.warn("Error writing to file {}", f, ex);
                f.delete();
                return null;
            }
        }
        return new FsFile(f);
    }

    @Override
    public boolean delete(String name) {
        final File f = new File(file, secureName(name));
        return f.delete();
    }

    @Override
    public List<String> list() {
        return Arrays.asList(file.list());
    }

    @Override
    public boolean delete() {
        return file.delete();
    }
    
    public static String secureName(String name) {
        if (name==null || name.length()==0)
            return "-";
        if ("..".equals(name))
            return "__";
        if (name.contains("/") || name.contains("\\"))
            name = name.replace('/', '_').replace('\\', '_');
        return name;
    }
    
}
