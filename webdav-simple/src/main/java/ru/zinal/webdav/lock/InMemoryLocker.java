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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import ru.zinal.webdav.util.*;

/**
 *
 * @author zinal
 */
public class InMemoryLocker implements LockManager {
    
    private static final long CLEANUP_TIMEOUT = 5000L;
    
    /**
     * The latest expiration cleanup operation.
     */
    private long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * Structure of lock-null resources.
     */
    private final LockRecord locks = new LockRecord();

    @Override
    public LockResult createLock(LockInfo lock) {
        final String[] entryPath = SmallT.splitPath(lock.getPath());
        synchronized(this) {
            cleanupExpired();
            // 1. Checking the upper-level locks
            LockRecord dir = locks.traverse(entryPath);
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
    private LockResult checkUpperLocks(LockInfo lock, LockRecord dir) {
        LockResult result = null;
        while (dir!=null) {
            if (dir.getLock()!=null
                    && dir.getLock().getDepth() > 0) {
                // the above check for null+depth is NECESSARY
                result = checkConflict(result, lock, dir.getLock());
            }
            dir = dir.getParent();
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
    private LockResult checkDownLocks(LockInfo lock, LockRecord dir) {
        if (lock.getDepth()==0) {
            // Checking the current entry ONLY
            return checkConflict(null, lock, dir.getLock());
        }
        // Checking the current entry and all possible sub-entries
        LockResult result = null;
        final Stack<LockRecord> stack = new Stack<>();
        stack.push(dir);
        while (!stack.empty()) {
            dir = stack.pop();
            stack.addAll(dir.getChildren().values());
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
        synchronized(this) {
            cleanupExpired();
            LockRecord dir = locks.find(lock.getPath());
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
        synchronized(this) {
            cleanupExpired();
            LockRecord dir = locks.find(path);
            if (dir==null || dir.getLock()==null)
                return false;
            if (tokens==null || tokens.isEmpty())
                return true;
            for (String token : tokens) {
                if (dir.getLock().getTokenExp().containsKey(token))
                    return false; // found skipped token
            }
            // we have a locked resource without skipped tokens
            return true;
        }
    }

    @Override
    public LockInfo findLock(String path) {
        synchronized(this) {
            cleanupExpired();
            LockRecord dir = locks.find(path);
            if (dir==null || dir.getLock()==null)
                return null;
            return new LockInfo(dir.getLock());
        }
    }

    @Override
    public boolean removeLock(String path, String token) {
        boolean retval = false;
        synchronized(this) {
            LockRecord dir = locks.find(path);
            if (dir!=null && dir.getLock()!=null) {
                if ( dir.getLock().getTokenExp().remove(token) != null )
                    retval = true;
            }
            cleanupExpired();
        }
        return retval;
    }

    @Override
    public List<LockInfo> discoverLocks(String path) {
        final List<LockInfo> retval = new ArrayList<>();
        synchronized(this) {
            cleanupExpired();
            LockRecord dir = locks.traverse(path);
            while (dir!=null) {
                if (dir.getLock()!=null)
                    retval.add(new LockInfo(dir.getLock()));
                dir = dir.getParent();
            }
        }
        return retval;
    }

    @Override
    public void removeNullLock(String path) {
        synchronized(this) {
            LockRecord dir = locks.find(path);
            if (dir!=null && dir.getLock()!=null)
                dir.getLock().setLockNull(false);
            cleanupExpired();
        }
    }

    @Override
    public List<LockInfo> listNullLocks(String parentPath) {
        final List<LockInfo> retval = new ArrayList<>();
        synchronized(this) {
            cleanupExpired();
            LockRecord dir = locks.find(parentPath);
            if (dir!=null) {
                for (LockRecord cur : dir.getChildren().values()) {
                    if (cur.getLock()!=null && cur.getLock().isLockNull())
                        retval.add(new LockInfo(cur.getLock()));
                }
            }
        }
        return retval;
    }
    
    private void cleanupExpired() {
        final long tv = System.currentTimeMillis();
        if (tv - lastCleanupTime < CLEANUP_TIMEOUT)
            return;
        lastCleanupTime = tv;
        locks.cleanup(tv);
    }
    
    static class LockRecord {

        private final LockRecord parent;
        private final String name;
        private final HashMap<String, LockRecord> children = new HashMap<>();
        private LockEntry lock = null;

        public LockRecord() {
            this.parent = null;
            this.name = null;
        }

        public LockRecord(LockRecord owner, String name) {
            this.parent = owner;
            this.name = name;
        }

        public LockRecord getParent() {
            return parent;
        }

        public String getName() {
            return name;
        }

        public HashMap<String, LockRecord> getChildren() {
            return children;
        }

        public LockEntry getLock() {
            return lock;
        }

        public void setLock(LockEntry lock) {
            this.lock = lock;
        }

        public LockRecord find(String path) {
            return find(SmallT.splitPath(path));
        }

        public LockRecord find(String[] path) {
            LockRecord cur = this;
            for (String item : path) {
                cur = cur.children.get(item);
                if (cur==null)
                    return null;
            }
            return cur;
        }

        public LockRecord traverse(String path) {
            return traverse(SmallT.splitPath(path));
        }
        
        public LockRecord traverse(String[] path) {
            LockRecord cur = this;
            for (String item : path) {
                LockRecord next = cur.children.get(item);
                if (next==null)
                    break;
                cur = next;
            }
            return cur;
        }
        
        public LockRecord create(String path) {
            return create(SmallT.splitPath(path));
        }

        public LockRecord create(String[] path) {
            LockRecord cur = this;
            for (String item : path) {
                LockRecord next = cur.children.get(item);
                if (next==null) {
                    next = new LockRecord(cur, item);
                    cur.children.put(item, next);
                }
                cur = next;
            }
            return cur;
        }
        
        public void cleanup() {
            cleanup(0L);
        }
        
        public void cleanup(long tv) {
            final Stack<LockRecord> stack1 = new Stack<>();
            final Stack<LockRecord> stack2 = new Stack<>();
            // Round one - clean up the expired locks
            stack1.push(this);
            while (!stack1.empty()) {
                LockRecord cur = stack1.pop();
                // second stack collects the hierarchy of all sub-items
                stack2.push(cur);
                // handle the expiration of the current lock
                if (cur.lock!=null) {
                    if (tv<=0L)
                        tv = System.currentTimeMillis();
                    if (cur.lock.hasExpired(tv))
                        cur.lock = null;
                }
                // process all children buckets
                for (LockRecord next : children.values())
                    stack1.push(next);
            }
            // Round two - remove the empty lock buckets
            while (!stack2.empty()) {
                LockRecord cur = stack1.pop();
                if (cur.lock == null && children.isEmpty()) {
                    if (cur.parent!=null)
                        cur.parent.children.remove(cur.name);
                }
            }
        }

    } // class LockRecord

}
