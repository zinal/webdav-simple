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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import ru.zinal.webdav.model.*;

/**
 *
 * @author zinal
 */
public class FsFile extends WebFile {
    
    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(FsFile.class);
    
    private final File file;
    private long createdAt = 0L;

    public FsFile(File file) {
        this.file = file;
    }
    
    @Override
    public long getCreation() {
        if (createdAt==0L) {
            try {
                BasicFileAttributes bfa = Files.readAttributes(file.toPath(), 
                        BasicFileAttributes.class);
                createdAt = bfa.creationTime().toMillis();
            } catch(IOException ex) {
                LOG.warn("Cannot get creation time for file {}", file, ex);
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
    public InputStream getData() {
        try {
            return new FileInputStream(file);
        } catch(IOException ex) {
            LOG.warn("Cannot open file {}", file, ex);
            return null;
        }
    }

    @Override
    public InputStream getData(long start, long finish) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void replaceData(InputStream data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void replaceData(InputStream data, long start, long totalSize) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean delete() {
        return file.delete();
    }
    
}
