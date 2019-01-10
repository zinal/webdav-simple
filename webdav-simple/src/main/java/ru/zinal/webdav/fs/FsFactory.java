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

import ru.zinal.webdav.model.WebdavContext;
import ru.zinal.webdav.model.*;

/**
 *
 * @author zinal
 */
public class FsFactory implements WebRootFactory {
    
    @Override
    public WebRoot create(WebdavContext context) {
        if (!(context instanceof FsContext))
            throw new IllegalArgumentException("Context should be instance of FsContext");
        FsContext fc = (FsContext) context;
        return new WebRootImpl(context, new FsDirectory(fc.getDataPath()));
    }
    
}
