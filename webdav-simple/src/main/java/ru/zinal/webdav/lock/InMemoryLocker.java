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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author zinal
 */
public class InMemoryLocker implements LockManager {
    
    private static final long CLEANUP_TIMEOUT = 5000L;
    
    /**
     * The latest expiration cleanup operation
     */
    private long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * Structure of lock-null resources.
     */
    private final LockDirectory locks = new LockDirectory();

    @Override
    public LockResult createLock(LockInfo lock) {
        cleanupExpired();
        final String[] entryPath = LockEntry.splitPath(lock.getPath());
        synchronized(this) {
            // 1. Checking the upper-level locks
            LockDirectory dir = locks.traverse(entryPath);
            LockResult result = checkUpperLocks(lock, dir);
            if (result!=null)
                return result;
            // 2. Checking the current and the following locks
            dir = locks.create(entryPath);
            result = checkDownLocks(lock, dir);
            if (result!=null)
                return result;
            // 3. Put the lock on the resource
            if (dir.getLock() != null) {
                if (dir.getLock().getDepth() < lock.getDepth()) {
                    // Replace the old lock, merging the tokens
                    final LockEntry entry = new LockEntry(lock);
                    entry.getTokenExp().putAll(dir.getLock().getTokenExp());
                    dir.setLock(entry);
                } else {
                    // Add new lock tokens to the existing lock
                    Map<String,Long> m = dir.getLock().getTokenExp();
                    for (String token : lock.getTokens()) {
                        m.put(token, lock.getExpiresAt());
                    }
                }
            } else {
                // put the new lock on the resource
                dir.setLock(new LockEntry(lock));
            }
            return new LockResult(dir.getLock());
        } // synchronized(this)
    }

    /**
     * Check for conflicting locks above the specified path
     * @param lock The lock to be created
     * @param dir Traversed directory
     * @return null, if no conflicting locks found,
     *    or list of conflicting paths otherwise
     */
    private LockResult checkUpperLocks(LockInfo lock, LockDirectory dir) {
        LockResult result = null;
        while (dir!=null) {
            if (dir.getLock()!=null
                    && dir.getLock().getDepth() > 0) {
                // the above check for null+depth is NECESSARY
                result = checkConflict(result, lock, dir.getLock());
            }
            dir = dir.getOwner();
        }
        return result;
    }

    /**
     * Check for conflicting locks at or below the specified path 
     * @param lock The lock to be created
     * @param dir Current lock directory entry
     * @return null, if no conflicting locks found,
     *    or list of conflicting paths otherwise
     */
    private LockResult checkDownLocks(LockInfo lock, LockDirectory dir) {
        if (lock.getDepth()==0) {
            // Checking the current entry ONLY
            return checkConflict(null, lock, dir.getLock());
        }
        // Checking the current entry and all possible sub-entries
        LockResult result = null;
        final Stack<LockDirectory> stack = new Stack<>();
        stack.push(dir);
        while (!stack.empty()) {
            dir = stack.pop();
            stack.addAll(dir.getItems().values());
            result = checkConflict(result, lock, dir.getLock());
        }
        return result;
    }
    
    /**
     * Check for conflicts between the lock entry and the new lock
     * @param result Lock result with the conflict status
     * @param lock New lock data
     * @param entry Existing lock entry
     * @return Updated lock conflict status
     */
    private LockResult checkConflict(LockResult result, 
            LockInfo lock, LockEntry entry) {
        if (entry==null)
            return result;
        if (lock.isExclusive() || entry.isExclusive()) {
            if (result==null)
                result = new LockResult();
            result.getLockedPaths().add(entry.getPath());
        }
        return result;
    }

    @Override
    public LockInfo refreshLock(LockInfo lock) {
        cleanupExpired();
        synchronized(this) {
            LockDirectory dir = locks.find(lock.getPath());
            if (dir==null || dir.getLock()==null)
                return null;
            boolean retval = false;
            Map<String, Long> m = dir.getLock().getTokenExp();
            for (String token : lock.getTokens()) {
                if (m.containsKey(token)) {
                    m.put(token, lock.getExpiresAt());
                    retval = true;
                }
            }
            return retval ? new LockInfo(dir.getLock()) : null;
        }
    }

    @Override
    public boolean isLocked(String path, Collection<String> tokens) {
        cleanupExpired();
        synchronized(this) {
            LockDirectory dir = locks.find(path);
            if (dir==null || dir.getLock()==null)
                return false;
            if (tokens==null || tokens.isEmpty())
                return true;
            for (String token : tokens) {
                if (dir.getLock().getTokenExp().containsKey(token))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLock(String path, String token) {
        boolean retval = false;
        synchronized(this) {
            LockDirectory dir = locks.find(path);
            if (dir!=null && dir.getLock()!=null) {
                if ( dir.getLock().getTokenExp().remove(token) != null )
                    retval = true;
            }
        }
        cleanupExpired();
        return retval;
    }

    @Override
    public List<LockInfo> discoverLocks(String path) {
        cleanupExpired();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeNullLock(String path) {
        cleanupExpired();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LockInfo> listNullLocks(String parentPath) {
        cleanupExpired();
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void cleanupExpired() {
        synchronized(this) {
            final long tv = System.currentTimeMillis();
            if (tv - lastCleanupTime < CLEANUP_TIMEOUT)
                return;
            lastCleanupTime = tv;
            
        }
    }
    
    static class LockDirectory {

        private final LockDirectory owner;
        private final HashMap<String, LockDirectory> items = new HashMap<>();
        private LockEntry lock = null;

        public LockDirectory() {
            this.owner = null;
        }

        public LockDirectory(LockDirectory owner) {
            this.owner = owner;
        }

        public LockDirectory getOwner() {
            return owner;
        }

        public HashMap<String, LockDirectory> getItems() {
            return items;
        }

        public LockEntry getLock() {
            return lock;
        }

        public void setLock(LockEntry lock) {
            this.lock = lock;
        }

        public LockDirectory find(String path) {
            return find(LockEntry.splitPath(path));
        }

        public LockDirectory find(String[] path) {
            LockDirectory cur = this;
            for (String item : path) {
                cur = cur.items.get(item);
                if (cur==null)
                    return null;
            }
            return cur;
        }

        public LockDirectory traverse(String path) {
            return traverse(LockEntry.splitPath(path));
        }
        
        public LockDirectory traverse(String[] path) {
            LockDirectory cur = this;
            for (String item : path) {
                LockDirectory next = cur.items.get(item);
                if (next==null)
                    break;
                cur = next;
            }
            return cur;
        }
        
        public LockDirectory create(String path) {
            return create(LockEntry.splitPath(path));
        }

        public LockDirectory create(String[] path) {
            LockDirectory cur = this;
            for (String item : path) {
                LockDirectory next = cur.items.get(item);
                if (next==null) {
                    next = new LockDirectory(cur);
                    cur.items.put(item, next);
                }
                cur = next;
            }
            return cur;
        }

    } // class LockDirectory

}
