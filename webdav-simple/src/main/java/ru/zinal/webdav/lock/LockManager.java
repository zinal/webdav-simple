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

import java.util.List;
import java.util.Collection;

/**
 *
 * @author zinal
 */
public interface LockManager {
    
    /**
     * Create a lock with the specified attributes
     * @param lock 
     * @return Operation status
     */
    LockResult createLock(LockInfo lock);

    /**
     * Refresh a lock with the specified attributes
     * @param lock Lock information
     * @return actual lock information, or null if lock was not found
     */
    LockInfo refreshLock(LockInfo lock);
    
    /**
     * Check whether the resource is locked
     * @param path Path to the resource
     * @param tokens Tokens to be skipped
     * @return true, if the resource is locked, and false otherwise
     */
    boolean isLocked(String path, Collection<String> tokens);
    
    /**
     * Remove the specified lock
     * @param path Path to locked object
     * @param token Previously defined lock token
     * @return true, if the resource was unlocked,
     *   and false if the specified resource lock was not found
     */
    boolean removeLock(String path, String token);
    
    /**
     * Find locks for a specified object
     * @param path Path to object
     * @return Lock information
     */
    List<LockInfo> discoverLocks(String path);
    
    /**
     * Remove null-lock sign of the specified path
     * @param path Path of the object
     */
    void removeNullLock(String path);
    
    /**
     * Get the list of child null locks for a parent object.
     * @param parentPath Path of the parent object
     * @return List of null locks
     */
    List<LockInfo> listNullLocks(String parentPath);
    
}
