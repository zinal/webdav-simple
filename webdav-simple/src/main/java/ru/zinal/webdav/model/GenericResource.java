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

import java.io.InputStream;
import java.util.List;

/**
 *
 * @author zinal
 */
public interface GenericResource {
    
    /**
     * Get the name of the resource
     * @return Name of the resource, relative to the containing collection
     */
    String getName();
    
    /**
     * Check if the resource is the file
     * @return true, if the resource is a file, and false for collections
     */
    boolean isFile();
    
    /**
     * Check of the resource is the collection
     * @return true, if the resource is a collection, and false for files
     */
    boolean isDirectory();
    
    /**
     * Get the creation time
     * @return Last modified timestamp
     */
    long getCreatedAt();

    /**
     * Get the last modified time
     * @return Last modified timestamp
     */
    long getLastModified();
    
    /**
     * Delete the resource.
     * For file, delete it, including its content.
       For collection, delete it and all the included files and collections.
     * @return true, if the resource was actually deleted
     */
    boolean delete();
    
    // ******** Collection-only methods
    
    /**
     * Find the resource with the specified name
     * @param name Name of the resource
     * @return Resource information, or null if the resource was not found
     */
    WebResource lookup(String name);
    
    /**
     * Create the collection.
     * In case the copllection already exists, do nothing.
     * @param name Name of the collection to be created
     * @return The resource referencing the collection
     */
    WebResource createDirectory(String name);
    
    /**
     * Create the file.
     * In case the file already exists, replace it.
     * @param name Name of the file to be created
     * @param data Data to be written as the file
     * @return The resource referencing the file
     */
    WebResource createFile(String name, InputStream data);
    
    /**
     * Remove the resource
     * @param name Name of the resource
     * @return true, of the resource was removed, and false, if the resource
     *    does not exist
     */
    boolean delete(String name);
    
    /**
     * List the resources in the collection specified by path
     * @return List of the resource names under the specified path.
     *     null, if path is not the name of the collection.
     */
    List<String> list();
    
    // ******** File-only methods
    
    /**
     * Get the file size
     * @return File size, in bytes
     */
    long getContentLength();
    
    /**
     * Open the input stream for the file.
     * The input stream returned must be closed by the caller.
     * @return Input stream
     */
    InputStream getData();
    
    /**
     * Open the range-limited input stream for the file.
     * The input stream returned must be closed by the caller.
     * @param start Range start position
     * @param finish Range finish position
     * @return Input stream
     */
    InputStream getData(long start, long finish);
    
    /**
     * Replace the file content with data from the input stream
     * @param data Input stream containing the replacement data
     */
    void replaceData(InputStream data);
    
    /**
     * Replace part of the file content with data from the input stream.
     * Also can be used to trim the file to the specified length.
     * @param data Input stream containing the replacement data
     * @param start Start position to write the data
     * @param totalSize Total size of the file (can be -1, e.g. unspecified)
     */
    void replaceData(InputStream data, long start, long totalSize);

}
