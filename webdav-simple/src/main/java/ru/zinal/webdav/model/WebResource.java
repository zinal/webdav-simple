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

/**
 *
 * @author zinal
 */
public abstract class WebResource implements GenericResource {

    public String getLastModifiedHttp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getWebappPath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream getInputStream() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getETag() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMimeType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean delete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    

}
