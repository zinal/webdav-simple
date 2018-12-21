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

import java.io.Serializable;
import java.util.HashSet;
import ru.zinal.webdav.util.FastHttpDateFormat;
import ru.zinal.webdav.util.XMLWriter;

/**
 *
 * @author zinal
 */
public class LockInfo implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private final int maxDepth;
    private String path;
    private String type;
    private String scope;
    private int depth;
    private String owner;
    private final HashSet<String> tokens;
    private long expiresAt;
    private long creationDate;

    public LockInfo(int maxDepth) {
        this.maxDepth = maxDepth;
        this.path = "/";
        this.type = "write";
        this.scope = "exclusive";
        this.depth = 0;
        this.owner = "";
        this.tokens = new HashSet<>();
        this.expiresAt = 0;
        this.creationDate = System.currentTimeMillis();
    }

    public LockInfo(LockInfo li) {
        this.maxDepth = li.maxDepth;
        this.path = li.path;
        this.type = li.type;
        this.scope = li.scope;
        this.depth = li.depth;
        this.owner = li.owner;
        this.tokens = new HashSet<>(li.tokens);
        this.expiresAt = li.expiresAt;
        this.creationDate = li.creationDate;
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
        return maxDepth;
    }

    public HashSet<String> getTokens() {
        return tokens;
    }

    /**
     * Get a String representation of this lock token.
     */
    @Override
    public String toString() {

        StringBuilder result = new StringBuilder("Type:");
        result.append(type);
        result.append("\nScope:");
        result.append(scope);
        result.append("\nDepth:");
        result.append(depth);
        result.append("\nOwner:");
        result.append(owner);
        result.append("\nExpiration:");
        result.append(FastHttpDateFormat.formatDate(expiresAt));
        for (String token : tokens) {
            result.append("\nToken:");
            result.append(token);
        }
        result.append("\n");
        return result.toString();
    }

    /**
     * @return true if the lock has expired.
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() > expiresAt;
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
        if (depth == maxDepth) {
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

}
