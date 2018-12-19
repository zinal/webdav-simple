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
public class WebResourceData implements WebResource {
    
    public WebResourceData() {
    }
    
    public WebResourceData(byte[] binaryContent) {
        setContent(binaryContent);
    }
    
    /**
     * Binary content.
     */
    protected byte[] binaryContent = null;
    
    
    @Override
    public InputStream streamContent() throws IOException {
        if (binaryContent != null) {
            return new ByteArrayInputStream(binaryContent);
        }
        return null;
    }

    @Override
    public boolean hasContent() {
        return (binaryContent != null);
    }
    
    /**
     * Content accessor.
     * 
     * @return binary content
     */
    public byte[] getContent() {
        return binaryContent;
    }
    
    
    /**
     * Content mutator.
     * 
     * @param binaryContent New bin content
     */
    public final void setContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }
    
}
