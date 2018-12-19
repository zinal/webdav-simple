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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsultes the contents of a resource.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author <a href="mailto:maxzinal@yandex.ru">Maksim Zinal</a>
 *
 */
public class WebResourceStream implements WebResource {
    
    public WebResourceStream() {
    }
    
    public WebResourceStream(InputStream inputStream) {
        setContent(inputStream);
    }
    
    public WebResourceStream(byte[] binaryContent) {
        setContent(binaryContent);
    }
    
    /**
     * Input stream.
     */
    private InputStream inputStream = null;
    
    
    @Override
    public InputStream streamContent() throws IOException {
        return inputStream;
    }

    @Override
    public boolean hasContent() {
        return inputStream!=null;
    }
    
    @Override
    public boolean isCollection() {
        return false;
    }

    /**
     * Content mutator.
     * 
     * @param inputStream New input stream
     */
    public final void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    
    /**
     * Content mutator.
     * 
     * @param binaryContent New bin content
     */
    public final void setContent(byte[] binaryContent) {
        this.inputStream = new ByteArrayInputStream(binaryContent);
    }

}
