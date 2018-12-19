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
package ru.zinal.webdav.naming;

import java.io.IOException;
import java.io.InputStream;
import javax.naming.directory.DirContext;

/**
 * Encapsultes the collection.
 * 
 * @author <a href="mailto:maxzinal@yandex.ru">Maksim Zinal</a>
 *
 */
public class WebFolder implements WebResource {
    
    public WebFolder() {
    }
    
    public WebFolder(DirContext context) {
        setContext(context);
    }
    
    /**
     * Binary content.
     */
    protected DirContext context = null;
    
    @Override
    public InputStream streamContent() throws IOException {
        return null;
    }

    @Override
    public boolean hasContent() {
        return false;
    }
    
    @Override
    public boolean isCollection() {
        return true;
    }

    public final DirContext getContext() {
        return context;
    }

    public final void setContext(DirContext context) {
        this.context = context;
    }
    
}
