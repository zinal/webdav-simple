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
import ru.zinal.webdav.WebdavContext;
import ru.zinal.webdav.model.*;

/**
 *
 * @author zinal
 */
public class FsRoot implements WebResourceRoot {

    @Override
    public WebResource getResource(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] list(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean write(String path, InputStream data, boolean overwrite) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WebdavContext getContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean mkdir(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
