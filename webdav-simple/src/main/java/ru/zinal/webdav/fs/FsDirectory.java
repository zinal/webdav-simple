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
import java.util.List;
import ru.zinal.webdav.model.*;

/**
 *
 * @author zinal
 */
public class FsDirectory extends WebDirectory {
    
    private final File file;
    
    public FsDirectory(File file) {
        this.file = file;
    }
    
    @Override
    public long getCreation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public WebResource lookup(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public WebResource createDirectory(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public WebResource createFile(String name, InputStream data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean delete(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> list() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
