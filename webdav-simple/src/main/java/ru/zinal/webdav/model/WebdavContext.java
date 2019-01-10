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

import java.io.File;

/**
 *
 * @author zinal
 */
public class WebdavContext {
    
    private String configPath;

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
    
    public void expand() {
        if (configPath==null)
            configPath = "./config/";
        configPath = expandDirectory(configPath);
    }
    
    protected static String expandDirectory(String src) {
        File f = new File(src);
        f = f.getAbsoluteFile();
        if (!f.exists()) {
            f.mkdirs();
        }
        if (!f.isDirectory())
            throw new IllegalArgumentException("Not a directory: " + src);
        return f.getPath();
    }
    
}
