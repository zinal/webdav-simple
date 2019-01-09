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
package ru.zinal.webdav.lock;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author zinal
 */
public class LockResult {
    
    private final LockInfo info;
    private final Set<String> lockedPaths = new HashSet<>();

    public LockResult() {
        this.info = null;
    }

    public LockResult(LockInfo info) {
        this.info = (info==null) ? null : new LockInfo(info);
    }

    public LockResult(LockEntry entry) {
        this.info = (entry==null) ? null : new LockInfo(entry);
    }

    public boolean isSuccess() {
        return info!=null;
    }

    public Set<String> getLockedPaths() {
        return lockedPaths;
    }
    
}
