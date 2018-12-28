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

import java.io.Serializable;
import java.util.HashSet;
import ru.zinal.webdav.util.*;

/**
 *
 * @author zinal
 */
public class LockInfo implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    public final static int MAX_DEPTH = 3;

    private String path;
    private String type;
    private String scope;
    private int depth;
    private String owner;
    private long expiresAt;
    private long creationDate;
    private final HashSet<String> tokens;
    private boolean lockNull;

    public LockInfo() {
        this.path = "/";
        this.type = "write";
        this.scope = "exclusive";
        this.depth = 0;
        this.owner = "";
        this.tokens = new HashSet<>();
        this.expiresAt = 0;
        this.creationDate = System.currentTimeMillis();
        this.lockNull = false;
    }

    public LockInfo(LockEntry entry) {
        this.path = entry.getPath();
        this.type = entry.getType();
        this.scope = entry.getScope();
        this.depth = entry.getDepth();
        this.owner = entry.getOwner();
        this.tokens = new HashSet<>(entry.getTokenExp().keySet());
        this.expiresAt = entry.getExpiresAt();
        this.creationDate = entry.getCreationDate();
        this.lockNull = entry.isLockNull();
    }

    public LockInfo(LockInfo li) {
        this.path = li.path;
        this.type = li.type;
        this.scope = li.scope;
        this.depth = li.depth;
        this.owner = li.owner;
        this.tokens = new HashSet<>(li.tokens);
        this.expiresAt = li.expiresAt;
        this.creationDate = li.creationDate;
        this.lockNull = li.lockNull;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public int getMaxDepth() {
        return MAX_DEPTH;
    }

    public HashSet<String> getTokens() {
        return tokens;
    }

    public boolean isLockNull() {
        return lockNull;
    }

    public void setLockNull(boolean lockNull) {
        this.lockNull = lockNull;
    }

    /**
     * Get a String representation of this lock token.
     */
    @Override
    public String toString() {

        StringBuilder result = new StringBuilder("Type: ");
        result.append(type);
        result.append("\nScope: ");
        result.append(scope);
        result.append("\nDepth: ");
        result.append(depth);
        result.append("\nOwner: ");
        result.append(owner);
        result.append("\nExpiration: ");
        result.append(FastHttpDateFormat.formatDate(expiresAt));
        for (String token : tokens) {
            result.append("\nToken: ");
            result.append(token);
        }
        result.append("\nLockNull: ");
        result.append(lockNull);
        result.append("\n");
        return result.toString();
    }

    /**
     * @return true if the lock is exclusive.
     */
    public boolean isExclusive() {
        return scope.equals("exclusive");
    }

    /**
     * Get an XML representation of this lock token.
     *
     * @param generatedXML The XML write to which the fragment will be appended
     */
    public void toXML(XMLWriter generatedXML) {

        generatedXML.writeElement("D", "activelock", XMLWriter.OPENING);

        generatedXML.writeElement("D", "locktype", XMLWriter.OPENING);
        generatedXML.writeElement("D", type, XMLWriter.NO_CONTENT);
        generatedXML.writeElement("D", "locktype", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "lockscope", XMLWriter.OPENING);
        generatedXML.writeElement("D", scope, XMLWriter.NO_CONTENT);
        generatedXML.writeElement("D", "lockscope", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "depth", XMLWriter.OPENING);
        if (depth == MAX_DEPTH) {
            generatedXML.writeText("Infinity");
        } else {
            generatedXML.writeText("0");
        }
        generatedXML.writeElement("D", "depth", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "owner", XMLWriter.OPENING);
        generatedXML.writeText(owner);
        generatedXML.writeElement("D", "owner", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "timeout", XMLWriter.OPENING);
        long timeout = (expiresAt - System.currentTimeMillis()) / 1000;
        generatedXML.writeText("Second-" + timeout);
        generatedXML.writeElement("D", "timeout", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "locktoken", XMLWriter.OPENING);
        for (String token : tokens) {
            generatedXML.writeElement("D", "href", XMLWriter.OPENING);
            generatedXML.writeText("opaquelocktoken:" + token);
            generatedXML.writeElement("D", "href", XMLWriter.CLOSING);
        }
        generatedXML.writeElement("D", "locktoken", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "activelock", XMLWriter.CLOSING);
    }

    @Override
    public Object clone() {
        return new LockInfo(this);
    }
    
    public void update(LockInfo li) {
        if (this.expiresAt < li.expiresAt)
            this.expiresAt = li.expiresAt;
        this.tokens.addAll(li.tokens);
    }

}
