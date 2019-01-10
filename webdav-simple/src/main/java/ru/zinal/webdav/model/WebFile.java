/*
 * Copyright 2018 zinal.
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

/**
 *
 * @author zinal
 */
public abstract class WebFile extends WebResource {

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public WebResource lookup(String name) {
        throw new UnsupportedOperationException("No lookup() for WebFile");
    }

    @Override
    public WebResource lookupDeep(String[] names) {
        throw new UnsupportedOperationException("No lookup() for WebFile");
    }

    @Override
    public WebResource createDirectory(String name) {
        throw new UnsupportedOperationException("No createDirectory() for WebFile");
    }

    @Override
    public WebResource createFile(String name, InputStream data) {
        throw new UnsupportedOperationException("No createFile() for WebFile");
    }

    @Override
    public boolean delete(String name) {
        throw new UnsupportedOperationException("No remove(x) for WebFile");
    }

    @Override
    public List<String> list() {
        throw new UnsupportedOperationException("No list() for WebFile");
    }
    
}
