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

import java.util.HashMap;
import ru.zinal.webdav.util.SmallT;

/**
 *
 * @author zinal
 */
public class LockEntry {
    
    private final String path;
    private final String type;
    private final String scope;
    private final int depth;
    private final String owner;
    private final long creationDate;
    private final HashMap<String, Long> tokenExp;
    private boolean lockNull;

    public LockEntry(LockInfo li) {
        this.path = SmallT.normalizePath(li.getPath());
        this.type = li.getType();
        this.scope = li.getScope();
        this.depth = li.getDepth();
        this.owner = li.getOwner();
        this.creationDate = li.getCreationDate();
        this.tokenExp = new HashMap<>();
        for (String token : li.getTokens()) {
            this.tokenExp.put(token, li.getExpiresAt());
        }
        this.lockNull = li.isLockNull();
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    public int getDepth() {
        return depth;
    }

    public String getOwner() {
        return owner;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public HashMap<String, Long> getTokenExp() {
        return tokenExp;
    }

    public boolean isLockNull() {
        return lockNull;
    }

    public void setLockNull(boolean lockNull) {
        this.lockNull = lockNull;
    }

    /**
     * @return true if the lock has expired.
     */
    public boolean hasExpired() {
        return hasExpired(System.currentTimeMillis());
    }
    
    /**
     * @param tv base timestamp to compare with locks
     * @return true if the lock has expired.
     */
    public boolean hasExpired(long tv) {
        if (tokenExp.isEmpty())
            return true;
        for (Long expTime : tokenExp.values()) {
            if (expTime > tv)
                return false;
        }
        return true;
    }
    
    public long getExpiresAt() {
        long tv = 0L;
        for (Long expTime : tokenExp.values()) {
            if (expTime > tv)
                tv = expTime;
        }
        return tv;
    }
    
    /**
     * @return true if the lock is exclusive.
     */
    public boolean isExclusive() {
        return scope.equals("exclusive");
    }

}
