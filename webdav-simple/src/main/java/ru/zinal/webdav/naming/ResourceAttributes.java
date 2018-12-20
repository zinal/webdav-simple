/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.zinal.webdav.naming;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import ru.zinal.webdav.util.ConcurrentDateFormat;

/**
 * Attributes implementation.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 *
 */
public class ResourceAttributes implements Attributes {
    
    
    // Default attribute names
    
    /**
     * Creation date.
     */
    public static final String CREATION_DATE = "creationdate";
    
    
    /**
     * Creation date.
     */
    public static final String ALTERNATE_CREATION_DATE = "creation-date";
    
    
    /**
     * Last modification date.
     */
    public static final String LAST_MODIFIED = "getlastmodified";
    
    
    /**
     * Last modification date.
     */
    public static final String ALTERNATE_LAST_MODIFIED = "last-modified";
    
    
    /**
     * Name.
     */
    public static final String NAME = "displayname";
    
    
    /**
     * Type.
     */
    public static final String TYPE = "resourcetype";
    
    
    /**
     * Type.
     */
    public static final String ALTERNATE_TYPE = "content-type";
    
    
    /**
     * Source.
     */
    public static final String SOURCE = "source";
    
    
    /**
     * MIME type of the content.
     */
    public static final String CONTENT_TYPE = "getcontenttype";
    
    
    /**
     * Content language.
     */
    public static final String CONTENT_LANGUAGE = "getcontentlanguage";
    
    
    /**
     * Content length.
     */
    public static final String CONTENT_LENGTH = "getcontentlength";
    
    
    /**
     * Content length.
     */
    public static final String ALTERNATE_CONTENT_LENGTH = "content-length";
    
    
    /**
     * ETag.
     */
    public static final String ETAG = "getetag";
    
    
    /**
     * ETag.
     */
    public static final String ALTERNATE_ETAG = "etag";
    
    
    /**
     * Collection type.
     */
    public static final String COLLECTION_TYPE = "<collection/>";
    
    /**
     * Date formats using for Date parsing.
     */
    protected static final ConcurrentDateFormat formats[] = {
        new ConcurrentDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", 
                Locale.US, TimeZone.getTimeZone("GMT")),
        new ConcurrentDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", 
                Locale.US, TimeZone.getTimeZone("GMT")),
        new ConcurrentDateFormat("EEE MMMM d HH:mm:ss yyyy", 
                Locale.US, TimeZone.getTimeZone("GMT"))
    };
    
    /**
     * Default constructor.
     */
    public ResourceAttributes() {
    }
    
    
    /**
     * Merges with another attribute set.
     */
    public ResourceAttributes(Attributes attributes) {
        this.attributes = attributes;
    }
    
    
    // ----------------------------------------------------- Instance Variables


    /**
     * Collection flag.
     */
    protected boolean collection = false;


    /**
     * Content length.
     */
    protected long contentLength = -1;


    /**
     * Creation date.
     */
    protected Date creationDate = null;


    /**
     * Last modified date.
     */
    protected Date lastModifiedDate = null;

    
    /**
     * Last modified date in HTTP format.
     */
    protected String lastModifiedHttp = null;
    

    /**
     * MIME type.
     */
    protected String mimeType = null;
    

    /**
     * Name.
     */
    protected String name = null;


    /**
     * Weak ETag.
     */
    protected String weakETag = null;


    /**
     * Strong ETag.
     */
    protected String strongETag = null;


    /**
     * External attributes.
     */
    protected Attributes attributes = null;

    /**
     * Is collection.
     * @return true, if collection, and false otherwise
     */
    public boolean isCollection() {
        if (attributes != null) {
            return (COLLECTION_TYPE.equals(getResourceType()));
        } else {
            return (collection);
        }
    }
    
    
    /**
     * Set collection flag.
     *
     * @param collection New flag value
     */
    public void setCollection(boolean collection) {
        this.collection = collection;
        if (attributes != null) {
            String value = "";
            if (collection)
                value = COLLECTION_TYPE;
            attributes.put(TYPE, value);
        }
    }
    
    
    /**
     * Get content length.
     * 
     * @return content length value
     */
    public long getContentLength() {
        if (contentLength != -1L)
            return contentLength;
        if (attributes != null) {
            Attribute attribute = attributes.get(CONTENT_LENGTH);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        contentLength = ((Long) value);
                    } else {
                        try {
                            contentLength = Long.parseLong(value.toString());
                        } catch (NumberFormatException e) {}
                    }
                } catch (NamingException e) {}
            }
        }
        return contentLength;
    }
    
    
    /**
     * Set content length.
     * 
     * @param contentLength New content length value
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
        if (attributes != null)
            attributes.put(CONTENT_LENGTH, contentLength);
    }

    /**
     * Parsing the HTTP Date
     * @param dv
     * @return 
     */
    private Date parseDate(String dv) {
        for (ConcurrentDateFormat df : formats) {
            try {
                final Date result = df.parse(dv);
                if (result!=null)
                    return result;
            } catch (ParseException e) {
                // noop
            }
        }
        return null;
    }
    
    /**
     * Convert the attribute value to date
     * @param attribute Source attribute
     * @return Converted date value, or null, if conversion not possible
     * @throws NamingException 
     */
    private Date convertAttribute(Attribute attribute) {
        if (attribute==null)
            return null;
        Object value;
        try {
            value = attribute.get();
        } catch(NamingException e) {
            return null;
        }
        if (value instanceof Long) {
            return new Date((Long) value);
        } else if (value instanceof Date) {
            return (Date) value;
        } else {
            String lastModifiedDateValue = value.toString();
            Date result = parseDate(lastModifiedDateValue);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    /**
     * Get creation time.
     * 
     * @return creation time value
     */
    public long getCreation() {
        Date date = getCreationDate();
        if (date==null)
            return -1L;
        return date.getTime();
    }
    
    /**
     * Set creation.
     * 
     * @param value New creation value
     */
    public void setCreation(long value) {
        if (value < 0L)
            setCreationDate(null);
        else
            setCreationDate(new Date(value));
    }
    
    
    /**
     * Get creation date.
     * 
     * @return Creation date value
     */
    public Date getCreationDate() {
        if (creationDate != null)
            return creationDate;
        if (attributes != null) {
            Attribute attribute = attributes.get(CREATION_DATE);
            creationDate = convertAttribute(attribute);
        }
        return creationDate;
    }
    
    
    /**
     * Creation date mutator.
     * 
     * @param creationDate New creation date
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
        if (attributes != null)
            attributes.put(CREATION_DATE, creationDate);
    }
    
    
    /**
     * Get last modified time.
     * 
     * @return lastModified time value
     */
    public long getLastModified() {
        Date date = getLastModifiedDate();
        if (date==null)
            return -1L;
        return date.getTime();
    }
    
    
    /**
     * Set last modified.
     * 
     * @param value New last modified value
     */
    public void setLastModified(long value) {
        if (value < 0L)
            setLastModifiedDate(null);
        else
            setLastModifiedDate(new Date(value));
    }

    /**
     * Set last modified date.
     * 
     * @param lastModified New last modified date value
     * @deprecated
     */
    public void setLastModified(Date lastModified) {
        setLastModifiedDate(lastModified);
    }


    /**
     * Get lastModified date.
     * 
     * @return LastModified date value
     */
    public Date getLastModifiedDate() {
        if (lastModifiedDate != null)
            return lastModifiedDate;
        if (attributes != null) {
            Attribute attribute = attributes.get(LAST_MODIFIED);
            lastModifiedDate = convertAttribute(attribute);
        }
        return lastModifiedDate;
    }
    
    
    /**
     * Last modified date mutator.
     * 
     * @param date New last modified date
     */
    public void setLastModifiedDate(Date date) {
        this.lastModifiedDate = date;
        if (attributes != null)
            attributes.put(LAST_MODIFIED, date);
    }
    
    
    /**
     * @return Returns the lastModifiedHttp.
     */
    public String getLastModifiedHttp() {
        if (lastModifiedHttp != null)
            return lastModifiedHttp;
        Date modifiedDate = getLastModifiedDate();
        if (modifiedDate == null) {
            modifiedDate = getCreationDate();
        }
        if (modifiedDate == null) {
            modifiedDate = new Date();
        }
        lastModifiedHttp = formats[0].format(modifiedDate);
        return lastModifiedHttp;
    }
    
    
    /**
     * @param lastModifiedHttp The lastModifiedHttp to set.
     */
    public void setLastModifiedHttp(String lastModifiedHttp) {
        this.lastModifiedHttp = lastModifiedHttp;
    }
    
    
    /**
     * @return Returns the mimeType.
     */
    public String getMimeType() {
        return mimeType;
    }
    
    
    /**
     * @param mimeType The mimeType to set.
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    
    /**
     * Get name.
     * 
     * @return Name value
     */
    public String getName() {
        if (name != null)
            return name;
        if (attributes != null) {
            Attribute attribute = attributes.get(NAME);
            if (attribute != null) {
                try {
                    name = attribute.get().toString();
                } catch (NamingException e) {
                    ; // No value for the attribute
                }
            }
        }
        return name;
    }


    /**
     * Set name.
     * 
     * @param name New name value
     */
    public void setName(String name) {
        this.name = name;
        if (attributes != null)
            attributes.put(NAME, name);
    }
    
    
    /**
     * Get resource type.
     * 
     * @return String resource type
     */
    public String getResourceType() {
        String result = null;
        if (attributes != null) {
            Attribute attribute = attributes.get(TYPE);
            if (attribute != null) {
                try {
                    result = attribute.get().toString();
                } catch (NamingException e) {
                    ; // No value for the attribute
                }
            }
        }
        if (result == null) {
            if (collection)
                result = COLLECTION_TYPE;
            else
                result = null;
        }
        return result;
    }
    
    
    /**
     * Type mutator.
     * 
     * @param resourceType New resource type
     */
    public void setResourceType(String resourceType) {
        collection = resourceType.equals(COLLECTION_TYPE);
        if (attributes != null)
            attributes.put(TYPE, resourceType);
    }


    /**
     * Get ETag.
     * 
     * @return strong ETag if available, else weak ETag. 
     */
    public String getETag() {
        String result = null;
        if (attributes != null) {
            Attribute attribute = attributes.get(ETAG);
            if (attribute != null) {
                try {
                    result = attribute.get().toString();
                } catch (NamingException e) {
                    ; // No value for the attribute
                }
            }
        }
        if (result == null) {
            if (strongETag != null) {
                // The strong ETag must always be calculated by the resources
                result = strongETag;
            } else {
                // The weakETag is contentLength + lastModified
                if (weakETag == null) {
                    long contentLength = getContentLength();
                    long lastModified = getLastModified();
                    if ((contentLength >= 0) || (lastModified >= 0)) {
                        weakETag = "W/\"" + contentLength + "-" +
                                   lastModified + "\"";
                    }
                }
                result = weakETag;
            }
        } 
        return result;
    }


    /**
     * Get ETag.
     * 
     * @param strong Ignored
     * @return strong ETag if available, else weak ETag.
     * @deprecated
     */
    public String getETag(boolean strong) {
        return getETag();
    }


    /**
     * Set strong ETag.
     */
    public void setETag(String eTag) {
        this.strongETag = eTag;
        if (attributes != null)
            attributes.put(ETAG, eTag);
    }

    
    /**
     * Return the canonical path of the resource, to possibly be used for 
     * direct file serving. Implementations which support this should override
     * it to return the file path.
     * 
     * @return The canonical path of the resource
     */
    public String getCanonicalPath() {
        return null;
    }
    
    
    // ----------------------------------------------------- Attributes Methods


    /**
     * Get attribute.
     */
    public Attribute get(String attrID) {
        if (attributes == null) {
            if (attrID.equals(CREATION_DATE)) {
                Date creationDate = getCreationDate();
                if (creationDate == null) return null;
                return new BasicAttribute(CREATION_DATE, creationDate);
            } else if (attrID.equals(ALTERNATE_CREATION_DATE)) {
                Date creationDate = getCreationDate();
                if (creationDate == null) return null;
                return new BasicAttribute(ALTERNATE_CREATION_DATE, creationDate);
            } else if (attrID.equals(LAST_MODIFIED)) {
                Date lastModifiedDate = getLastModifiedDate();
                if (lastModifiedDate == null) return null;
                return new BasicAttribute(LAST_MODIFIED, lastModifiedDate);
            } else if (attrID.equals(ALTERNATE_LAST_MODIFIED)) {
                Date lastModifiedDate = getLastModifiedDate();
                if (lastModifiedDate == null) return null;
                return new BasicAttribute(ALTERNATE_LAST_MODIFIED, lastModifiedDate);
            } else if (attrID.equals(NAME)) {
                String name = getName();
                if (name == null) return null;
                return new BasicAttribute(NAME, name);
            } else if (attrID.equals(TYPE)) {
                String resourceType = getResourceType();
                if (resourceType == null) return null;
                return new BasicAttribute(TYPE, resourceType);
            } else if (attrID.equals(ALTERNATE_TYPE)) {
                String resourceType = getResourceType();
                if (resourceType == null) return null;
                return new BasicAttribute(ALTERNATE_TYPE, resourceType);
            } else if (attrID.equals(CONTENT_LENGTH)) {
                long contentLength = getContentLength();
                if (contentLength < 0) return null;
                return new BasicAttribute(CONTENT_LENGTH, new Long(contentLength));
            } else if (attrID.equals(ALTERNATE_CONTENT_LENGTH)) {
                long contentLength = getContentLength();
                if (contentLength < 0) return null;
                return new BasicAttribute(ALTERNATE_CONTENT_LENGTH, new Long(contentLength));
            } else if (attrID.equals(ETAG)) {
                String etag = getETag();
                if (etag == null) return null;
                return new BasicAttribute(ETAG, etag);
            } else if (attrID.equals(ALTERNATE_ETAG)) {
                String etag = getETag();
                if (etag == null) return null;
                return new BasicAttribute(ALTERNATE_ETAG, etag);
            }
        } else {
            return attributes.get(attrID);
        }
        return null;
    }
    
    
    /**
     * Put attribute.
     */
    public Attribute put(Attribute attribute) {
        if (attributes == null) {
            try {
                return put(attribute.getID(), attribute.get());
            } catch (NamingException e) {
                return null;
            }
        } else {
            return attributes.put(attribute);
        }
    }
    
    
    /**
     * Put attribute.
     */
    public Attribute put(String attrID, Object val) {
        if (attributes == null) {
            return null; // No reason to implement this
        } else {
            return attributes.put(attrID, val);
        }
    }
    
    
    /**
     * Remove attribute.
     */
    public Attribute remove(String attrID) {
        if (attributes == null) {
            return null; // No reason to implement this
        } else {
            return attributes.remove(attrID);
        }
    }
    
    
    /**
     * Get all attributes.
     */
    public NamingEnumeration getAll() {
        if (attributes == null) {
            Vector attributes = new Vector();
            Date creationDate = getCreationDate();
            if (creationDate != null) {
                attributes.addElement(new BasicAttribute
                                      (CREATION_DATE, creationDate));
                attributes.addElement(new BasicAttribute
                                      (ALTERNATE_CREATION_DATE, creationDate));
            }
            Date lastModifiedDate = getLastModifiedDate();
            if (lastModifiedDate != null) {
                attributes.addElement(new BasicAttribute
                                      (LAST_MODIFIED, lastModifiedDate));
                attributes.addElement(new BasicAttribute
                                      (ALTERNATE_LAST_MODIFIED, lastModifiedDate));
            }
            String name = getName();
            if (name != null) {
                attributes.addElement(new BasicAttribute(NAME, name));
            }
            String resourceType = getResourceType();
            if (resourceType != null) {
                attributes.addElement(new BasicAttribute(TYPE, resourceType));
                attributes.addElement(new BasicAttribute(ALTERNATE_TYPE, resourceType));
            }
            long contentLength = getContentLength();
            if (contentLength >= 0) {
                Long contentLengthLong = new Long(contentLength);
                attributes.addElement(new BasicAttribute(CONTENT_LENGTH, contentLengthLong));
                attributes.addElement(new BasicAttribute(ALTERNATE_CONTENT_LENGTH, contentLengthLong));
            }
            String etag = getETag();
            if (etag != null) {
                attributes.addElement(new BasicAttribute(ETAG, etag));
                attributes.addElement(new BasicAttribute(ALTERNATE_ETAG, etag));
            }
            return new RecyclableNamingEnumeration(attributes);
        } else {
            return attributes.getAll();
        }
    }
    
    
    /**
     * Get all attribute IDs.
     */
    public NamingEnumeration getIDs() {
        if (attributes == null) {
            Vector attributeIDs = new Vector();
            Date creationDate = getCreationDate();
            if (creationDate != null) {
                attributeIDs.addElement(CREATION_DATE);
                attributeIDs.addElement(ALTERNATE_CREATION_DATE);
            }
            Date lastModifiedDate = getLastModifiedDate();
            if (lastModifiedDate != null) {
                attributeIDs.addElement(LAST_MODIFIED);
                attributeIDs.addElement(ALTERNATE_LAST_MODIFIED);
            }
            if (getName() != null) {
                attributeIDs.addElement(NAME);
            }
            String resourceType = getResourceType();
            if (resourceType != null) {
                attributeIDs.addElement(TYPE);
                attributeIDs.addElement(ALTERNATE_TYPE);
            }
            long contentLength = getContentLength();
            if (contentLength >= 0) {
                attributeIDs.addElement(CONTENT_LENGTH);
                attributeIDs.addElement(ALTERNATE_CONTENT_LENGTH);
            }
            String etag = getETag();
            if (etag != null) {
                attributeIDs.addElement(ETAG);
                attributeIDs.addElement(ALTERNATE_ETAG);
            }
            return new RecyclableNamingEnumeration(attributeIDs);
        } else {
            return attributes.getIDs();
        }
    }
    
    
    /**
     * Retrieves the number of attributes in the attribute set.
     */
    public int size() {
        if (attributes == null) {
            int size = 0;
            if (getCreationDate() != null) size += 2;
            if (getLastModifiedDate() != null) size += 2;
            if (getName() != null) size++;
            if (getResourceType() != null) size += 2;
            if (getContentLength() >= 0) size += 2;
            if (getETag() != null) size += 2;
            return size;
        } else {
            return attributes.size();
        }
    }
    
    
    /**
     * Clone the attributes object (WARNING: fake cloning).
     */
    public Object clone() {
        return this;
    }
    
    
    /**
     * Case sensitivity.
     */
    public boolean isCaseIgnored() {
        return false;
    }
    
    
}
