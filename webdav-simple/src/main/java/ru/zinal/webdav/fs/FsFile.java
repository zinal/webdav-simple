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

import java.io.InputStream;
import ru.zinal.webdav.model.*;

/**
 *
 * @author zinal
 */
public class FsFile extends WebFile {

    @Override
    public long getCreation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getContentLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream getData() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
