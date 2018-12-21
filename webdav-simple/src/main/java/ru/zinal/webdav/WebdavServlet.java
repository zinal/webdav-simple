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
package ru.zinal.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.zinal.webdav.util.*;
import ru.zinal.webdav.model.*;

/**
 * An adjusted version of WevdavServlet from Tomcat 9.
 *
 * @author Remy Maucherat
 * @author Maksim Zinal
 */
public class WebdavServlet extends DefaultServlet {

    private static final long serialVersionUID = 1L;

    private static final URLEncoder URL_ENCODER_XML;
    static {
        URL_ENCODER_XML = (URLEncoder) URLEncoder.DEFAULT.clone();
        // Remove '&' from the safe character set since while it it permitted
        // in a URI path, it is not permitted in XML and encoding it is a simple
        // way to address this.
        URL_ENCODER_XML.removeSafeCharacter('&');
    }

    private static final String METHOD_PROPFIND = "PROPFIND";
    private static final String METHOD_PROPPATCH = "PROPPATCH";
    private static final String METHOD_MKCOL = "MKCOL";
    private static final String METHOD_COPY = "COPY";
    private static final String METHOD_MOVE = "MOVE";
    private static final String METHOD_LOCK = "LOCK";
    private static final String METHOD_UNLOCK = "UNLOCK";


    /**
     * PROPFIND - Specify a property mask.
     */
    private static final int FIND_BY_PROPERTY = 0;


    /**
     * PROPFIND - Display all properties.
     */
    private static final int FIND_ALL_PROP = 1;


    /**
     * PROPFIND - Return property names.
     */
    private static final int FIND_PROPERTY_NAMES = 2;


    /**
     * Create a new lock.
     */
    private static final int LOCK_CREATION = 0;


    /**
     * Refresh lock.
     */
    private static final int LOCK_REFRESH = 1;


    /**
     * Default lock timeout value.
     */
    private static final int DEFAULT_TIMEOUT = 3600;


    /**
     * Maximum lock timeout.
     */
    private static final int MAX_TIMEOUT = 604800;


    /**
     * Default namespace.
     */
    protected static final String DEFAULT_NAMESPACE = "DAV:";


    /**
     * Repository of the locks put on single resources.
     * <p>
     * Key : path <br>
     * Value : LockInfo
     */
    private final Hashtable<String,LockInfo> resourceLocks = new Hashtable<>();


    /**
     * Repository of the lock-null resources.
     * <p>
     * Key : path of the collection containing the lock-null resource<br>
     * Value : Vector of lock-null resource which are members of the
     * collection. Each element of the Vector is the path associated with
     * the lock-null resource.
     */
    private final Hashtable<String,Vector<String>> lockNullResources =
        new Hashtable<>();


    /**
     * Vector of the heritable locks.
     * <p>
     * Key : path <br>
     * Value : LockInfo
     */
    private final Vector<LockInfo> collectionLocks = new Vector<>();


    /**
     * Secret information used to generate reasonably secure lock ids.
     */
    private String secret = "superpanda97";


    /**
     * Default depth in spec is infinite. Limit depth to 3 by default as
     * infinite depth makes operations very expensive.
     */
    private int maxDepth = 3;


    /**
     * Is access allowed via WebDAV to the special paths (/WEB-INF and
     * /META-INF)?
     */
    private boolean allowSpecialPaths = false;


    // --------------------------------------------------------- Public Methods


    /**
     * Initialize this servlet.
     */
    @Override
    public void init()
        throws ServletException {

        super.init();

        if (getServletConfig().getInitParameter("secret") != null)
            secret = getServletConfig().getInitParameter("secret");

        if (getServletConfig().getInitParameter("maxDepth") != null)
            maxDepth = Integer.parseInt(
                    getServletConfig().getInitParameter("maxDepth"));

        if (getServletConfig().getInitParameter("allowSpecialPaths") != null)
            allowSpecialPaths = Boolean.parseBoolean(
                    getServletConfig().getInitParameter("allowSpecialPaths"));
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return JAXP document builder instance.
     * @return the document builder
     * @throws ServletException document builder creation failed
     *  (wrapped <code>ParserConfigurationException</code> exception)
     */
    protected DocumentBuilder getDocumentBuilder()
        throws ServletException {
        DocumentBuilder documentBuilder = null;
        DocumentBuilderFactory documentBuilderFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.setEntityResolver(
                    new WebdavResolver(this.getServletContext()));
        } catch(ParserConfigurationException e) {
            throw new ServletException
                (sm.getString("webdavservlet.jaxpfailed"));
        }
        return documentBuilder;
    }


    /**
     * Handles the special WebDAV methods.
     * @param req
     * @param resp
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        final String path = getRelativePath(req);

        // Error page check needs to come before special path check since
        // custom error pages are often located below WEB-INF so they are
        // not directly accessible.
        if (req.getDispatcherType() == DispatcherType.ERROR) {
            doGet(req, resp);
            return;
        }

        // Block access to special subdirectories.
        // DefaultServlet assumes it services resources from the root of the web app
        // and doesn't add any special path protection
        // WebdavServlet remounts the webapp under a new path, so this check is
        // necessary on all methods (including GET).
        if (isSpecialPath(path)) {
            resp.sendError(WebdavStatus.SC_NOT_FOUND);
            return;
        }

        final String method = req.getMethod();

        if (debug > 0) {
            log("[" + method + "] " + path);
        }

        switch (method) {
            case METHOD_PROPFIND:
                doPropfind(req, resp);
                break;
            case METHOD_PROPPATCH:
                doProppatch(req, resp);
                break;
            case METHOD_MKCOL:
                doMkcol(req, resp);
                break;
            case METHOD_COPY:
                doCopy(req, resp);
                break;
            case METHOD_MOVE:
                doMove(req, resp);
                break;
            case METHOD_LOCK:
                doLock(req, resp);
                break;
            case METHOD_UNLOCK:
                doUnlock(req, resp);
                break;
            default:
                // DefaultServlet processing
                super.service(req, resp);
                break;
        }
    }


    /**
     * Checks whether a given path refers to a resource under
     * <code>WEB-INF</code> or <code>META-INF</code>.
     * @param path the full path of the resource being accessed
     * @return <code>true</code> if the resource specified is under a special path
     */
    private boolean isSpecialPath(final String path) {
        return !allowSpecialPaths && (
                path.toUpperCase(Locale.ENGLISH).startsWith("/WEB-INF") ||
                path.toUpperCase(Locale.ENGLISH).startsWith("/META-INF"));
    }


    @Override
    protected boolean checkIfHeaders(HttpServletRequest request,
                                     HttpServletResponse response,
                                     WebResource resource)
        throws IOException {

        if (!super.checkIfHeaders(request, response, resource))
            return false;

        // TODO : Checking the WebDAV If header
        return true;
    }


    /**
     * URL rewriter.
     *
     * @param path Path which has to be rewritten
     * @return the rewritten path
     */
    @Override
    protected String rewriteUrl(String path) {
        return URL_ENCODER_XML.encode(path, StandardCharsets.UTF_8);
    }


    /**
     * Override the DefaultServlet implementation and only use the PathInfo. If
     * the ServletPath is non-null, it will be because the WebDAV servlet has
     * been mapped to a url other than /* to configure editing at different url
     * than normal viewing.
     *
     * @param request The servlet request we are processing
     * @return 
     */
    @Override
    protected String getRelativePath(HttpServletRequest request) {
        return getRelativePath(request, false);
    }

    @Override
    protected String getRelativePath(HttpServletRequest request, 
            boolean allowEmptyPath) {
        String pathInfo;

        if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
            // For includes, get the info from the attributes
            pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
        } else {
            pathInfo = request.getPathInfo();
        }

        StringBuilder result = new StringBuilder();
        if (pathInfo != null) {
            result.append(pathInfo);
        }
        if (result.length() == 0) {
            result.append('/');
        }

        return result.toString();
    }


    /**
     * Determines the prefix for standard directory GET listings.
     * @return 
     */
    @Override
    protected String getPathPrefix(HttpServletRequest request) {
        // Repeat the servlet path (e.g. /webdav/) in the listing path
        String contextPath = request.getContextPath();
        if (request.getServletPath() !=  null) {
            contextPath = contextPath + request.getServletPath();
        }
        return contextPath;
    }


    /**
     * OPTIONS Method.
     *
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws ServletException If an error occurs
     * @throws IOException If an IO error occurs
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        resp.addHeader("DAV", "1,2");
        resp.addHeader("Allow", determineMethodsAllowed(req));
        resp.addHeader("MS-Author-Via", "DAV");
    }


    /**
     * PROPFIND Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws ServletException If an error occurs
     * @throws IOException If an IO error occurs
     */
    protected void doPropfind(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        if (!listings) {
            sendNotAllowed(req, resp);
            return;
        }

        String path = getRelativePath(req);
        if (path.length() > 1 && path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        // Properties which are to be displayed.
        List<String> properties = null;
        // Propfind depth
        int depth = maxDepth;
        // Propfind type
        int type = FIND_ALL_PROP;

        String depthStr = req.getHeader("Depth");

        if (depthStr == null) {
            depth = maxDepth;
        } else {
            if (depthStr.equals("0")) {
                depth = 0;
            } else if (depthStr.equals("1")) {
                depth = 1;
            } else if (depthStr.equals("infinity")) {
                depth = maxDepth;
            }
        }

        Node propNode = null;

        if (req.getContentLengthLong() > 0) {
            DocumentBuilder documentBuilder = getDocumentBuilder();

            try {
                Document document = documentBuilder.parse
                    (new InputSource(req.getInputStream()));

                // Get the root element of the document
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();

                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (currentNode.getNodeName().endsWith("prop")) {
                            type = FIND_BY_PROPERTY;
                            propNode = currentNode;
                        }
                        if (currentNode.getNodeName().endsWith("propname")) {
                            type = FIND_PROPERTY_NAMES;
                        }
                        if (currentNode.getNodeName().endsWith("allprop")) {
                            type = FIND_ALL_PROP;
                        }
                        break;
                    }
                }
            } catch (SAXException | IOException e) {
                // Something went wrong - bad request
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }
            
        }

        if (type == FIND_BY_PROPERTY) {
            properties = new ArrayList<>();
            // propNode must be non-null if type == FIND_BY_PROPERTY
            @SuppressWarnings("null")
            NodeList childList = propNode.getChildNodes();

            for (int i=0; i < childList.getLength(); i++) {
                Node currentNode = childList.item(i);
                switch (currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    String nodeName = currentNode.getNodeName();
                    String propertyName;
                    int semiIndex = nodeName.indexOf(':');
                    if (semiIndex != -1) {
                        propertyName 
                                = nodeName.substring(nodeName.indexOf(':') + 1);
                    } else {
                        propertyName = nodeName;
                    }
                    // href is a live property which is handled differently
                    properties.add(propertyName);
                    break;
                }
            }
        }

        WebResource resource = resources.getResource(path);

        if (resource==null) {
            int slash = path.lastIndexOf('/');
            if (slash != -1) {
                String parentPath = path.substring(0, slash);
                Vector<String> currentLockNullResources =
                    lockNullResources.get(parentPath);
                if (currentLockNullResources != null) {
                    Enumeration<String> lockNullResourcesList =
                        currentLockNullResources.elements();
                    while (lockNullResourcesList.hasMoreElements()) {
                        String lockNullPath =
                            lockNullResourcesList.nextElement();
                        if (lockNullPath.equals(path)) {
                            resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
                            resp.setContentType("text/xml; charset=UTF-8");
                            // Create multistatus object
                            XMLWriter generatedXML =
                                new XMLWriter(resp.getWriter());
                            generatedXML.writeXMLHeader();
                            generatedXML.writeElement("D", DEFAULT_NAMESPACE,
                                    "multistatus", XMLWriter.OPENING);
                            parseLockNullProperties
                                (req, generatedXML, lockNullPath, type,
                                 properties);
                            generatedXML.writeElement("D", "multistatus",
                                    XMLWriter.CLOSING);
                            generatedXML.sendData();
                            return;
                        }
                    }
                }
            }
        }

        if (resource==null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            return;
        }

        resp.setStatus(WebdavStatus.SC_MULTI_STATUS);

        resp.setContentType("text/xml; charset=UTF-8");

        // Create multistatus object
        XMLWriter generatedXML = new XMLWriter(resp.getWriter());
        generatedXML.writeXMLHeader();

        generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus",
                XMLWriter.OPENING);

        if (depth == 0) {
            parseProperties(req, generatedXML, path, type,
                            properties);
        } else {
            // The stack always contains the object of the current level
            Stack<String> stack = new Stack<>();
            stack.push(path);

            // Stack of the objects one level below
            Stack<String> stackBelow = new Stack<>();

            while ((!stack.isEmpty()) && (depth >= 0)) {

                String currentPath = stack.pop();
                parseProperties(req, generatedXML, currentPath,
                                type, properties);

                resource = resources.getResource(currentPath);

                if (resource!=null && resource.isDirectory() && (depth > 0)) {

                    String[] entries = resources.list(currentPath);
                    for (String entry : entries) {
                        String newPath = currentPath;
                        if (!(newPath.endsWith("/")))
                                newPath += "/";
                        newPath += entry;
                        stackBelow.push(newPath);
                    }

                    // Displaying the lock-null resources present in that
                    // collection
                    String lockPath = currentPath;
                    if (lockPath.endsWith("/"))
                        lockPath =
                            lockPath.substring(0, lockPath.length() - 1);
                    Vector<String> currentLockNullResources =
                        lockNullResources.get(lockPath);
                    if (currentLockNullResources != null) {
                        Enumeration<String> lockNullResourcesList =
                            currentLockNullResources.elements();
                        while (lockNullResourcesList.hasMoreElements()) {
                            String lockNullPath =
                                lockNullResourcesList.nextElement();
                            parseLockNullProperties
                                (req, generatedXML, lockNullPath, type,
                                 properties);
                        }
                    }

                }

                if (stack.isEmpty()) {
                    depth--;
                    stack = stackBelow;
                    stackBelow = new Stack<>();
                }

                generatedXML.sendData();

            }
        }

        generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);

        generatedXML.sendData();

    }


    /**
     * PROPPATCH Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws IOException If an IO error occurs
     */
    protected void doProppatch(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);

    }


    /**
     * MKCOL Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws ServletException If an error occurs
     * @throws IOException If an IO error occurs
     */
    protected void doMkcol(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        String path = getRelativePath(req);

        WebResource resource = resources.getResource(path);

        // Can't create a collection if a resource already exists at the given
        // path
        if (resource!=null) {
            sendNotAllowed(req, resp);
            return;
        }

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        if (req.getContentLengthLong() > 0) {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            try {
                // Document document =
                documentBuilder.parse(new InputSource(req.getInputStream()));
                // TODO : Process this request body
                resp.sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
                return;

            } catch(SAXException saxe) {
                // Parse error - assume invalid content
                resp.sendError(WebdavStatus.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
        }

        if (resources.mkdir(path)) {
            resp.setStatus(WebdavStatus.SC_CREATED);
            // Removing any lock-null resource which would be present
            lockNullResources.remove(path);
        } else {
            resp.sendError(WebdavStatus.SC_CONFLICT,
                           WebdavStatus.getStatusText
                           (WebdavStatus.SC_CONFLICT));
        }
    }


    /**
     * DELETE Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws ServletException If an error occurs
     * @throws IOException If an IO error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        if (readOnly) {
            sendNotAllowed(req, resp);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        deleteResource(req, resp);

    }


    /**
     * Process a PUT request for the specified resource.
     *
     * @param req The servlet request we are processing
     * @param resp The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        String path = getRelativePath(req);
        WebResource resource = resources.getResource(path);
        if (resource.isDirectory()) {
            sendNotAllowed(req, resp);
            return;
        }

        super.doPut(req, resp);

        // Removing any lock-null resource which would be present
        lockNullResources.remove(path);

    }

    /**
     * COPY Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws IOException If an IO error occurs
     */
    protected void doCopy(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        copyResource(req, resp);

    }


    /**
     * MOVE Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws IOException If an IO error occurs
     */
    protected void doMove(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        String path = getRelativePath(req);

        if (copyResource(req, resp)) {
            deleteResource(path, req, resp, false);
        }

    }


    /**
     * LOCK Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws ServletException If an error occurs
     * @throws IOException If an IO error occurs
     */
    protected void doLock(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        LockInfo lock = new LockInfo(maxDepth);

        // Parsing lock request

        // Parsing depth header

        String depthStr = req.getHeader("Depth");

        if (depthStr == null) {
            lock.setDepth(maxDepth);
        } else {
            if (depthStr.equals("0")) {
                lock.setDepth(0);
            } else {
                lock.setDepth(maxDepth);
            }
        }

        // Parsing timeout header

        int lockDuration = DEFAULT_TIMEOUT;
        String lockDurationStr = req.getHeader("Timeout");
        if (lockDurationStr != null) {
            int commaPos = lockDurationStr.indexOf(',');
            // If multiple timeouts, just use the first
            if (commaPos != -1) {
                lockDurationStr = lockDurationStr.substring(0,commaPos);
            }
            if (lockDurationStr.startsWith("Second-")) {
                lockDuration = Integer.parseInt(lockDurationStr.substring(7));
            } else {
                if (lockDurationStr.equalsIgnoreCase("infinity")) {
                    lockDuration = MAX_TIMEOUT;
                } else {
                    try {
                        lockDuration = Integer.parseInt(lockDurationStr);
                    } catch (NumberFormatException e) {
                        lockDuration = MAX_TIMEOUT;
                    }
                }
            }
            if (lockDuration == 0) {
                lockDuration = DEFAULT_TIMEOUT;
            }
            if (lockDuration > MAX_TIMEOUT) {
                lockDuration = MAX_TIMEOUT;
            }
        }
        lock.setExpiresAt(lock.getCreationDate() + (lockDuration * 1000));

        int lockRequestType = LOCK_CREATION;

        Node lockInfoNode = null;

        DocumentBuilder documentBuilder = getDocumentBuilder();

        try {
            Document document = documentBuilder.parse(new InputSource
                (req.getInputStream()));

            // Get the root element of the document
            Element rootElement = document.getDocumentElement();
            lockInfoNode = rootElement;
        } catch (IOException | SAXException e) {
            lockRequestType = LOCK_REFRESH;
        }

        if (lockInfoNode != null) {

            // Reading lock information

            NodeList childList = lockInfoNode.getChildNodes();
            StringWriter strWriter = null;
            DOMWriter domWriter = null;

            Node lockScopeNode = null;
            Node lockTypeNode = null;
            Node lockOwnerNode = null;

            for (int i=0; i < childList.getLength(); i++) {
                Node currentNode = childList.item(i);
                switch (currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    String nodeName = currentNode.getNodeName();
                    if (nodeName.endsWith("lockscope")) {
                        lockScopeNode = currentNode;
                    }
                    if (nodeName.endsWith("locktype")) {
                        lockTypeNode = currentNode;
                    }
                    if (nodeName.endsWith("owner")) {
                        lockOwnerNode = currentNode;
                    }
                    break;
                }
            }

            if (lockScopeNode != null) {

                childList = lockScopeNode.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        String tempScope = currentNode.getNodeName();
                        int semiIndex = tempScope.indexOf(':');
                        if (semiIndex != -1) {
                            lock.setScope(tempScope.substring(semiIndex + 1));
                        } else {
                            lock.setScope(tempScope);
                        }
                        break;
                    }
                }

                if (lock.getScope() == null) {
                    // Bad request
                    resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
                }

            } else {
                // Bad request
                resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
            }

            if (lockTypeNode != null) {

                childList = lockTypeNode.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        String tempType = currentNode.getNodeName();
                        int semiIndex = tempType.indexOf(':');
                        if (semiIndex != -1) {
                            lock.setType(tempType.substring(semiIndex + 1));
                        } else {
                            lock.setType(tempType);
                        }
                        break;
                    }
                }

                if (lock.getType() == null) {
                    // Bad request
                    resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
                }

            } else {
                // Bad request
                resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
            }

            if (lockOwnerNode != null) {
                
                final StringBuilder lockOwner = new StringBuilder();

                childList = lockOwnerNode.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        lockOwner.append(currentNode.getNodeValue());
                        break;
                    case Node.ELEMENT_NODE:
                        strWriter = new StringWriter();
                        domWriter = new DOMWriter(strWriter);
                        domWriter.print(currentNode);
                        lockOwner.append(strWriter.toString());
                        break;
                    }
                }
                
                if (lockOwner.length()==0) {
                    // Bad request
                    resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
                } else {
                    lock.setOwner(lockOwner.toString());
                }

            } else {
                lock.setOwner("");
            }

        }

        String path = getRelativePath(req);

        lock.setPath(path);

        WebResource resource = resources.getResource(path);
        
        final boolean isDirectory = (resource!=null) ?
                resource.isDirectory() :
                path.endsWith("/");

        Enumeration<LockInfo> locksList = null;

        if (lockRequestType == LOCK_CREATION) {

            // Generating lock id
            final byte[] lockTokenData = (req.getServletPath() 
                    + "-" + lock.getType()
                    + "-" + lock.getScope()
                    + "-" + req.getUserPrincipal()
                    + "-" + lock.getDepth()
                    + "-" + lock.getOwner()
                    + "-" + lock.getExpiresAt()
                    + "-" + System.currentTimeMillis()
                    + "-" + secret) 
                    .getBytes(StandardCharsets.ISO_8859_1);
            String lockToken = MD5Encoder.encode(
                    ConcurrentMessageDigest.digestMD5(lockTokenData));

            if (isDirectory && lock.getDepth() == maxDepth) {

                // Locking a collection (and all its member resources)

                // Checking if a child resource of this collection is
                // already locked
                List<String> lockedPaths = new ArrayList<>();
                locksList = collectionLocks.elements();
                while (locksList.hasMoreElements()) {
                    LockInfo currentLock = locksList.nextElement();
                    if (currentLock.hasExpired()) {
                        //resourceLocks.remove(currentLock.getPath());
                        collectionLocks.remove(currentLock);
                        continue;
                    }
                    if ( (currentLock.getPath().startsWith(lock.getPath())) &&
                         ((currentLock.isExclusive()) ||
                          (lock.isExclusive())) ) {
                        // A child collection of this collection is locked
                        lockedPaths.add(currentLock.getPath());
                    }
                }
                locksList = resourceLocks.elements();
                while (locksList.hasMoreElements()) {
                    LockInfo currentLock = locksList.nextElement();
                    if (currentLock.hasExpired()) {
                        resourceLocks.remove(currentLock.getPath());
                        continue;
                    }
                    if ( (currentLock.getPath().startsWith(lock.getPath())) &&
                         ((currentLock.isExclusive()) ||
                          (lock.isExclusive())) ) {
                        // A child resource of this collection is locked
                        lockedPaths.add(currentLock.getPath());
                    }
                }

                if (!lockedPaths.isEmpty()) {

                    // One of the child paths was locked
                    // We generate a multistatus error report

                    resp.setStatus(WebdavStatus.SC_CONFLICT);

                    XMLWriter generatedXML = new XMLWriter();
                    generatedXML.writeXMLHeader();

                    generatedXML.writeElement("D", DEFAULT_NAMESPACE,
                            "multistatus", XMLWriter.OPENING);

                    for (String lockedPath : lockedPaths) {
                        generatedXML.writeElement("D", "response",
                                XMLWriter.OPENING);
                        generatedXML.writeElement("D", "href",
                                XMLWriter.OPENING);
                        generatedXML.writeText(lockedPath);
                        generatedXML.writeElement("D", "href",
                                XMLWriter.CLOSING);
                        generatedXML.writeElement("D", "status",
                                XMLWriter.OPENING);
                        generatedXML
                            .writeText("HTTP/1.1 " + WebdavStatus.SC_LOCKED
                                       + " " + WebdavStatus
                                       .getStatusText(WebdavStatus.SC_LOCKED));
                        generatedXML.writeElement("D", "status",
                                XMLWriter.CLOSING);

                        generatedXML.writeElement("D", "response",
                                XMLWriter.CLOSING);
                    }

                    generatedXML.writeElement("D", "multistatus",
                            XMLWriter.CLOSING);

                    try (Writer writer = resp.getWriter()) {
                        writer.write(generatedXML.toString());
                    }

                    return;

                }

                boolean addLock = true;

                // Checking if there is already a shared lock on this path
                locksList = collectionLocks.elements();
                while (locksList.hasMoreElements()) {

                    LockInfo currentLock = locksList.nextElement();
                    if (currentLock.getPath().equals(lock.getPath())) {

                        if (currentLock.isExclusive()) {
                            resp.sendError(WebdavStatus.SC_LOCKED);
                            return;
                        } else {
                            if (lock.isExclusive()) {
                                resp.sendError(WebdavStatus.SC_LOCKED);
                                return;
                            }
                        }

                        currentLock.getTokens().add(lockToken);
                        lock = currentLock;
                        addLock = false;

                    }

                }

                if (addLock) {
                    lock.getTokens().add(lockToken);
                    collectionLocks.addElement(lock);
                }

            } else {

                // Locking a single resource

                // Retrieving an already existing lock on that resource
                LockInfo presentLock = resourceLocks.get(lock.getPath());
                if (presentLock != null) {

                    if ((presentLock.isExclusive()) || (lock.isExclusive())) {
                        // If either lock is exclusive, the lock can't be
                        // granted
                        resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                        return;
                    } else {
                        presentLock.getTokens().add(lockToken);
                        lock = presentLock;
                    }

                } else {

                    lock.getTokens().add(lockToken);
                    resourceLocks.put(lock.getPath(), lock);

                    // Checking if a resource exists at this path
                    if (resource==null) {

                        // "Creating" a lock-null resource
                        int slash = lock.getPath().lastIndexOf('/');
                        String parentPath = lock.getPath().substring(0, slash);

                        Vector<String> lockNulls =
                            lockNullResources.get(parentPath);
                        if (lockNulls == null) {
                            lockNulls = new Vector<>();
                            lockNullResources.put(parentPath, lockNulls);
                        }

                        lockNulls.addElement(lock.getPath());

                    }
                    // Add the Lock-Token header as by RFC 2518 8.10.1
                    // - only do this for newly created locks
                    resp.addHeader("Lock-Token", "<opaquelocktoken:"
                                   + lockToken + ">");
                }

            }

        }

        if (lockRequestType == LOCK_REFRESH) {

            String ifHeader = req.getHeader("If");
            if (ifHeader == null)
                ifHeader = "";

            // Checking resource locks

            LockInfo toRenew = resourceLocks.get(path);

            if (toRenew != null) {
                // At least one of the tokens of the locks must have been given
                for (String token : toRenew.getTokens()) {
                    if (ifHeader.contains(token)) {
                        toRenew.setExpiresAt(lock.getExpiresAt());
                        lock = toRenew;
                    }
                }
            }

            // Checking inheritable collection locks

            Enumeration<LockInfo> collectionLocksList =
                collectionLocks.elements();
            while (collectionLocksList.hasMoreElements()) {
                toRenew = collectionLocksList.nextElement();
                if (path.equals(toRenew.getPath())) {

                    for (String token : toRenew.getTokens()) {
                        if (ifHeader.contains(token)) {
                            toRenew.setExpiresAt(lock.getExpiresAt());
                            lock = toRenew;
                        }
                    }

                }
            }

        }

        // Set the status, then generate the XML response containing
        // the lock information
        XMLWriter generatedXML = new XMLWriter();
        generatedXML.writeXMLHeader();
        generatedXML.writeElement("D", DEFAULT_NAMESPACE, "prop",
                XMLWriter.OPENING);

        generatedXML.writeElement("D", "lockdiscovery", XMLWriter.OPENING);

        lock.toXML(generatedXML);

        generatedXML.writeElement("D", "lockdiscovery", XMLWriter.CLOSING);

        generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);

        resp.setStatus(WebdavStatus.SC_OK);
        resp.setContentType("text/xml; charset=UTF-8");
        try (Writer writer = resp.getWriter()) {
            writer.write(generatedXML.toString());
        }

    }


    /**
     * UNLOCK Method.
     * @param req The Servlet request
     * @param resp The Servlet response
     * @throws IOException If an IO error occurs
     */
    protected void doUnlock(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        if (isLocked(req)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return;
        }

        String path = getRelativePath(req);

        String lockTokenHeader = req.getHeader("Lock-Token");
        if (lockTokenHeader == null)
            lockTokenHeader = "";

        // Checking resource locks

        LockInfo lock = resourceLocks.get(path);
        if (lock != null) {

            // At least one of the tokens of the locks must have been given

            for (String token : lock.getTokens()) {
                if (lockTokenHeader.contains(token)) {
                    lock.getTokens().remove(token);
                }
            }

            if (lock.getTokens().isEmpty()) {
                resourceLocks.remove(path);
                // Removing any lock-null resource which would be present
                lockNullResources.remove(path);
            }

        }

        // Checking inheritable collection locks

        Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();
        while (collectionLocksList.hasMoreElements()) {
            lock = collectionLocksList.nextElement();
            if (path.equals(lock.getPath())) {

                for (String token : lock.getTokens()) {
                    if (lockTokenHeader.contains(token)) {
                        lock.getTokens().remove(token);
                        break;
                    }
                }

                if (lock.getTokens().isEmpty()) {
                    collectionLocks.removeElement(lock);
                    // Removing any lock-null resource which would be present
                    lockNullResources.remove(path);
                }

            }
        }

        resp.setStatus(WebdavStatus.SC_NO_CONTENT);

    }

    // -------------------------------------------------------- Private Methods

    /**
     * Check to see if a resource is currently write locked. The method
     * will look at the "If" header to make sure the client
     * has give the appropriate lock tokens.
     *
     * @param req Servlet request
     * @return <code>true</code> if the resource is locked (and no appropriate
     *  lock token has been found for at least one of
     *  the non-shared locks which are present on the resource).
     */
    private boolean isLocked(HttpServletRequest req) {

        String path = getRelativePath(req);

        String ifHeader = req.getHeader("If");
        if (ifHeader == null)
            ifHeader = "";

        String lockTokenHeader = req.getHeader("Lock-Token");
        if (lockTokenHeader == null)
            lockTokenHeader = "";

        return isLocked(path, ifHeader + lockTokenHeader);

    }


    /**
     * Check to see if a resource is currently write locked.
     *
     * @param path Path of the resource
     * @param ifHeader "If" HTTP header which was included in the request
     * @return <code>true</code> if the resource is locked (and no appropriate
     *  lock token has been found for at least one of
     *  the non-shared locks which are present on the resource).
     */
    private boolean isLocked(String path, String ifHeader) {

        // Checking resource locks

        LockInfo lock = resourceLocks.get(path);
        Enumeration<String> tokenList = null;
        if ((lock != null) && (lock.hasExpired())) {
            resourceLocks.remove(path);
        } else if (lock != null) {

            // At least one of the tokens of the locks must have been given

            boolean tokenMatch = false;
            for (String token : lock.getTokens()) {
                if (ifHeader.contains(token)) {
                    tokenMatch = true;
                    break;
                }
            }
            if (!tokenMatch)
                return true;

        }

        // Checking inheritable collection locks

        Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();
        while (collectionLocksList.hasMoreElements()) {
            lock = collectionLocksList.nextElement();
            if (lock.hasExpired()) {
                collectionLocks.removeElement(lock);
            } else if (path.startsWith(lock.getPath())) {

                boolean tokenMatch = false;
                for (String token : lock.getTokens()) {
                    if (ifHeader.contains(token)) {
                        tokenMatch = true;
                        break;
                    }
                }
                if (!tokenMatch)
                    return true;

            }
        }

        return false;

    }


    /**
     * Copy a resource.
     *
     * @param req Servlet request
     * @param resp Servlet response
     * @return boolean true if the copy is successful
     * @throws IOException If an IO error occurs
     */
    private boolean copyResource(HttpServletRequest req,
                                 HttpServletResponse resp)
            throws IOException {

        // Parsing destination header

        String destinationPath = req.getHeader("Destination");

        if (destinationPath == null) {
            resp.sendError(WebdavStatus.SC_BAD_REQUEST);
            return false;
        }

        // Remove url encoding from destination
        destinationPath = UDecoder.URLDecode(destinationPath, CS_UTF8);

        int protocolIndex = destinationPath.indexOf("://");
        if (protocolIndex >= 0) {
            // if the Destination URL contains the protocol, we can safely
            // trim everything upto the first "/" character after "://"
            int firstSeparator =
                destinationPath.indexOf('/', protocolIndex + 4);
            if (firstSeparator < 0) {
                destinationPath = "/";
            } else {
                destinationPath = destinationPath.substring(firstSeparator);
            }
        } else {
            String hostName = req.getServerName();
            if ((hostName != null) && (destinationPath.startsWith(hostName))) {
                destinationPath = destinationPath.substring(hostName.length());
            }

            int portIndex = destinationPath.indexOf(':');
            if (portIndex >= 0) {
                destinationPath = destinationPath.substring(portIndex);
            }

            if (destinationPath.startsWith(":")) {
                int firstSeparator = destinationPath.indexOf('/');
                if (firstSeparator < 0) {
                    destinationPath = "/";
                } else {
                    destinationPath =
                        destinationPath.substring(firstSeparator);
                }
            }
        }

        // Normalise destination path (remove '.' and '..')
        destinationPath = RequestUtil.normalize(destinationPath);

        String contextPath = req.getContextPath();
        if ((contextPath != null) &&
            (destinationPath.startsWith(contextPath))) {
            destinationPath = destinationPath.substring(contextPath.length());
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String servletPath = req.getServletPath();
            if ((servletPath != null) &&
                (destinationPath.startsWith(servletPath))) {
                destinationPath = destinationPath
                    .substring(servletPath.length());
            }
        }

        if (debug > 0)
            log("Dest path :" + destinationPath);

        // Check destination path to protect special subdirectories
        if (isSpecialPath(destinationPath)) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return false;
        }

        String path = getRelativePath(req);

        if (destinationPath.equals(path)) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return false;
        }

        // Parsing overwrite header

        boolean overwrite = true;
        String overwriteHeader = req.getHeader("Overwrite");

        if (overwriteHeader != null) {
            if (overwriteHeader.equalsIgnoreCase("T")) {
                overwrite = true;
            } else {
                overwrite = false;
            }
        }

        // Overwriting the destination

        WebResource destination = resources.getResource(destinationPath);

        if (overwrite) {
            // Delete destination resource, if it exists
            if (destination!=null) {
                if (!deleteResource(destinationPath, req, resp, true)) {
                    return false;
                }
            } else {
                resp.setStatus(WebdavStatus.SC_CREATED);
            }
        } else {
            // If the destination exists, then it's a conflict
            if (destination!=null) {
                resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                return false;
            }
        }

        // Copying source to destination

        Hashtable<String,Integer> errorList = new Hashtable<>();

        boolean result = copyResource(errorList, path, destinationPath);

        if ((!result) || (!errorList.isEmpty())) {
            if (errorList.size() == 1) {
                resp.sendError(errorList.elements().nextElement().intValue());
            } else {
                sendReport(req, resp, errorList);
            }
            return false;
        }

        // Copy was successful
        if (destination!=null) {
            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
        } else {
            resp.setStatus(WebdavStatus.SC_CREATED);
        }

        // Removing any lock-null resource which would be present at
        // the destination path
        lockNullResources.remove(destinationPath);

        return true;
    }


    /**
     * Copy a collection.
     *
     * @param errorList Hashtable containing the list of errors which occurred
     * during the copy operation
     * @param source Path of the resource to be copied
     * @param dest Destination path
     * @return <code>true</code> if the copy was successful
     */
    private boolean copyResource(Hashtable<String,Integer> errorList,
            String source, String dest) {

        if (debug > 1)
            log("Copy: " + source + " To: " + dest);

        WebResource sourceResource = resources.getResource(source);

        if (sourceResource.isDirectory()) {
            if (!resources.mkdir(dest)) {
                WebResource destResource = resources.getResource(dest);
                if (!destResource.isDirectory()) {
                    errorList.put(dest, Integer.valueOf(WebdavStatus.SC_CONFLICT));
                    return false;
                }
            }

            String[] entries = resources.list(source);
            for (String entry : entries) {
                String childDest = dest;
                if (!childDest.equals("/")) {
                    childDest += "/";
                }
                childDest += entry;
                String childSrc = source;
                if (!childSrc.equals("/")) {
                    childSrc += "/";
                }
                childSrc += entry;
                copyResource(errorList, childSrc, childDest);
            }
        } else if (sourceResource.isFile()) {
            WebResource destResource = resources.getResource(dest);
            if (destResource==null && !dest.endsWith("/")) {
                int lastSlash = dest.lastIndexOf('/');
                if (lastSlash > 0) {
                    String parent = dest.substring(0, lastSlash);
                    WebResource parentResource = resources.getResource(parent);
                    if (parentResource==null
                            || !parentResource.isDirectory()) {
                        errorList.put(source, WebdavStatus.SC_CONFLICT);
                        return false;
                    }
                }
            }
            // WebDAV Litmus test attempts to copy/move a file over a collection
            // Need to remove trailing / from destination to enable test to pass
            if (destResource==null && dest.endsWith("/") && dest.length() > 1) {
                // Convert destination name from collection (with trailing '/')
                // to file (without trailing '/')
                dest = dest.substring(0, dest.length() - 1);
            }
            try (InputStream is = sourceResource.getInputStream()) {
                if (!resources.write(dest, is, false)) {
                    errorList.put(source, WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                    return false;
                }
            } catch (IOException e) {
                log(sm.getString("webdavservlet.inputstreamclosefail", source), e);
            }
        } else {
            errorList.put(source, WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
        return true;
    }


    /**
     * Delete a resource.
     *
     * @param req Servlet request
     * @param resp Servlet response
     * @return <code>true</code> if the delete is successful
     * @throws IOException If an IO error occurs
     */
    private boolean deleteResource(HttpServletRequest req,
                                   HttpServletResponse resp)
            throws IOException {

        String path = getRelativePath(req);

        return deleteResource(path, req, resp, true);

    }


    /**
     * Delete a resource.
     *
     * @param path Path of the resource which is to be deleted
     * @param req Servlet request
     * @param resp Servlet response
     * @param setStatus Should the response status be set on successful
     *                  completion
     * @return <code>true</code> if the delete is successful
     * @throws IOException If an IO error occurs
     */
    private boolean deleteResource(String path, HttpServletRequest req,
                                   HttpServletResponse resp, boolean setStatus)
            throws IOException {

        String ifHeader = req.getHeader("If");
        if (ifHeader == null)
            ifHeader = "";

        String lockTokenHeader = req.getHeader("Lock-Token");
        if (lockTokenHeader == null)
            lockTokenHeader = "";

        if (isLocked(path, ifHeader + lockTokenHeader)) {
            resp.sendError(WebdavStatus.SC_LOCKED);
            return false;
        }

        WebResource resource = resources.getResource(path);

        if (resource==null) {
            resp.sendError(WebdavStatus.SC_NOT_FOUND);
            return false;
        }

        if (!resource.isDirectory()) {
            if (!resource.delete()) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                return false;
            }
        } else {

            Hashtable<String,Integer> errorList = new Hashtable<>();

            deleteCollection(req, path, errorList);
            if (!resource.delete()) {
                errorList.put(path, Integer.valueOf
                    (WebdavStatus.SC_INTERNAL_SERVER_ERROR));
            }

            if (!errorList.isEmpty()) {
                sendReport(req, resp, errorList);
                return false;
            }
        }
        if (setStatus) {
            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
        }
        return true;
    }


    /**
     * Deletes a collection.
     * @param req The Servlet request
     * @param path Path to the collection to be deleted
     * @param errorList Contains the list of the errors which occurred
     */
    private void deleteCollection(HttpServletRequest req,
                                  String path,
                                  Hashtable<String,Integer> errorList) {

        if (debug > 1)
            log("Delete:" + path);

        // Prevent deletion of special subdirectories
        if (isSpecialPath(path)) {
            errorList.put(path, Integer.valueOf(WebdavStatus.SC_FORBIDDEN));
            return;
        }

        String ifHeader = req.getHeader("If");
        if (ifHeader == null)
            ifHeader = "";

        String lockTokenHeader = req.getHeader("Lock-Token");
        if (lockTokenHeader == null)
            lockTokenHeader = "";

        String[] entries = resources.list(path);

        for (String entry : entries) {
            String childName = path;
            if (!childName.equals("/"))
                childName += "/";
            childName += entry;

            if (isLocked(childName, ifHeader + lockTokenHeader)) {

                errorList.put(childName, Integer.valueOf(WebdavStatus.SC_LOCKED));

            } else {
                WebResource childResource = resources.getResource(childName);
                if (childResource.isDirectory()) {
                    deleteCollection(req, childName, errorList);
                }

                if (!childResource.delete()) {
                    if (!childResource.isDirectory()) {
                        // If it's not a collection, then it's an unknown
                        // error
                        errorList.put(childName, Integer.valueOf(
                                WebdavStatus.SC_INTERNAL_SERVER_ERROR));
                    }
                }
            }
        }
    }


    /**
     * Send a multistatus element containing a complete error report to the
     * client.
     *
     * @param req Servlet request
     * @param resp Servlet response
     * @param errorList List of error to be displayed
     * @throws IOException If an IO error occurs
     */
    private void sendReport(HttpServletRequest req, HttpServletResponse resp,
                            Hashtable<String,Integer> errorList)
            throws IOException {

        resp.setStatus(WebdavStatus.SC_MULTI_STATUS);

        String absoluteUri = req.getRequestURI();
        String relativePath = getRelativePath(req);

        XMLWriter generatedXML = new XMLWriter();
        generatedXML.writeXMLHeader();

        generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus",
                XMLWriter.OPENING);

        Enumeration<String> pathList = errorList.keys();
        while (pathList.hasMoreElements()) {

            String errorPath = pathList.nextElement();
            int errorCode = errorList.get(errorPath);

            generatedXML.writeElement("D", "response", XMLWriter.OPENING);

            generatedXML.writeElement("D", "href", XMLWriter.OPENING);
            String toAppend = errorPath.substring(relativePath.length());
            if (!toAppend.startsWith("/"))
                toAppend = "/" + toAppend;
            generatedXML.writeText(absoluteUri + toAppend);
            generatedXML.writeElement("D", "href", XMLWriter.CLOSING);
            generatedXML.writeElement("D", "status", XMLWriter.OPENING);
            generatedXML.writeText("HTTP/1.1 " + errorCode + " "
                    + WebdavStatus.getStatusText(errorCode));
            generatedXML.writeElement("D", "status", XMLWriter.CLOSING);

            generatedXML.writeElement("D", "response", XMLWriter.CLOSING);

        }

        generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);

        try (Writer writer = resp.getWriter()) {
            writer.write(generatedXML.toString());
        }
    }


    /**
     * Propfind helper method.
     *
     * @param req The servlet request
     * @param generatedXML XML response to the Propfind request
     * @param path Path of the current resource
     * @param type Propfind type
     * @param propNames If the propfind type is find properties by
     * name, then this List contains those properties
     */
    private void parseProperties(HttpServletRequest req,
            XMLWriter generatedXML, String path, int type,
            List<String> propNames) {

        // Exclude any resource in the /WEB-INF and /META-INF subdirectories
        if (isSpecialPath(path))
            return;

        WebResource resource = resources.getResource(path);
        if (resource==null) {
            // File is in directory listing but doesn't appear to exist
            // Broken symlink or odd permission settings?
            return;
        }

        String href = req.getContextPath() + req.getServletPath();
        if ((href.endsWith("/")) && (path.startsWith("/")))
            href += path.substring(1);
        else
            href += path;
        if (resource.isDirectory() && (!href.endsWith("/")))
            href += "/";

        final PropFindResponseGen gen = new PropFindResponseGen(generatedXML);
        gen.rewrittenUrl = rewriteUrl(href);
        gen.path = path;
        gen.propFindType = type;
        gen.propNames = propNames;
        gen.isFile = resource.isFile();
        gen.isLockNull = false;
        gen.created = resource.getCreation();
        gen.lastModified = resource.getLastModified();
        gen.contentLength = resource.getContentLength();
        gen.contentType = getServletContext().getMimeType(resource.getName());
        gen.eTag = resource.getETag();
        gen.run();
    }


    /**
     * Propfind helper method. Displays the properties of a lock-null resource.
     *
     * @param req The servlet request
     * @param generatedXML XML response to the Propfind request
     * @param path Path of the current resource
     * @param type Propfind type
     * @param propNames If the propfind type is find properties by
     * name, then this List contains those properties
     */
    private void parseLockNullProperties(HttpServletRequest req,
            XMLWriter generatedXML, String path, int type,
            List<String> propNames) {

        // Exclude any resource in the /WEB-INF and /META-INF subdirectories
        if (isSpecialPath(path))
            return;

        // Retrieving the lock associated with the lock-null resource
        LockInfo lock = resourceLocks.get(path);

        if (lock == null)
            return;

        String absoluteUri = req.getRequestURI();
        String relativePath = getRelativePath(req);
        String toAppend = path.substring(relativePath.length());
        if (!toAppend.startsWith("/"))
            toAppend = "/" + toAppend;

        final PropFindResponseGen gen = new PropFindResponseGen(generatedXML);
        gen.rewrittenUrl 
                = rewriteUrl(RequestUtil.normalize(absoluteUri + toAppend));
        gen.path = path;
        gen.propFindType = type;
        gen.propNames = propNames;
        gen.created = lock.getCreationDate();
        gen.lastModified = lock.getCreationDate();
        gen.run();
    }

    /**
     * Print the lock discovery information associated with a path.
     *
     * @param path Path
     * @param generatedXML XML data to which the locks info will be appended
     * @return <code>true</code> if at least one lock was displayed
     */
    private boolean generateLockDiscovery
        (String path, XMLWriter generatedXML) {

        LockInfo resourceLock = resourceLocks.get(path);
        Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();

        boolean wroteStart = false;

        if (resourceLock != null) {
            wroteStart = true;
            generatedXML.writeElement("D", "lockdiscovery", XMLWriter.OPENING);
            resourceLock.toXML(generatedXML);
        }

        while (collectionLocksList.hasMoreElements()) {
            LockInfo currentLock = collectionLocksList.nextElement();
            if (path.startsWith(currentLock.getPath())) {
                if (!wroteStart) {
                    wroteStart = true;
                    generatedXML.writeElement("D", "lockdiscovery",
                            XMLWriter.OPENING);
                }
                currentLock.toXML(generatedXML);
            }
        }

        if (wroteStart) {
            generatedXML.writeElement("D", "lockdiscovery", XMLWriter.CLOSING);
        } else {
            return false;
        }

        return true;

    }


    /**
     * Determines the methods normally allowed for the resource.
     *
     * @param req The Servlet request
     *
     * @return The allowed HTTP methods
     */
    @Override
    protected String determineMethodsAllowed(HttpServletRequest req) {

        String path = getRelativePath(req);
        WebResource resource = resources.getResource(path);

        // These methods are always allowed. They may return a 404 (not a 405)
        // if the resource does not exist.
        StringBuilder methodsAllowed = new StringBuilder(
                "OPTIONS, GET, POST, HEAD");

        if (!readOnly) {
            methodsAllowed.append(", DELETE");
            if (!path.endsWith("/")) {
                methodsAllowed.append(", PUT");
            }
        }

        methodsAllowed.append(", LOCK, UNLOCK, PROPPATCH, COPY, MOVE");

        if (listings) {
            methodsAllowed.append(", PROPFIND");
        }

        if (resource==null) {
            methodsAllowed.append(", MKCOL");
        }

        return methodsAllowed.toString();
    }

    /**
     * Simple date format for the creation date ISO representation (partial).
     */
    private static final ConcurrentDateFormat creationDateFormat =
        new ConcurrentDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US,
                TimeZone.getTimeZone("GMT"));

    private class PropFindResponseGen {
        
        final XMLWriter generatedXML;
        String rewrittenUrl;
        String path;
        int propFindType;
        List<String> propNames;
        boolean isFile = true;
        boolean isLockNull = true;
        long created;
        long lastModified;
        long contentLength = 0;
        String contentType = "";
        String eTag = "";

        PropFindResponseGen(XMLWriter generatedXML) {
            this.generatedXML = generatedXML;
        }
        
        final void run() {
            generatedXML.writeElement("D", "response", XMLWriter.OPENING);
            String status = "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                    WebdavStatus.getStatusText(WebdavStatus.SC_OK);

            // Generating href element
            generatedXML.writeElement("D", "href", XMLWriter.OPENING);
            generatedXML.writeText(rewrittenUrl);
            generatedXML.writeElement("D", "href", XMLWriter.CLOSING);

            String resourceName = path;
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1)
                resourceName = resourceName.substring(lastSlash + 1);

            switch (propFindType) {

            case FIND_ALL_PROP :

                generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
                generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

                generatedXML.writeProperty("D", "creationdate", getISOCreationDate(created));
                generatedXML.writeElement("D", "displayname", XMLWriter.OPENING);
                generatedXML.writeData(resourceName);
                generatedXML.writeElement("D", "displayname", XMLWriter.CLOSING);
                if (isFile) {
                    generatedXML.writeProperty("D", "getlastmodified",
                            FastHttpDateFormat.formatDate(lastModified));
                    generatedXML.writeProperty("D", "getcontentlength", Long.toString(contentLength));
                    if (contentType != null) {
                        generatedXML.writeProperty("D", "getcontenttype", contentType);
                    }
                    generatedXML.writeProperty("D", "getetag", eTag);
                    if (isLockNull) {
                        generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
                        generatedXML.writeElement("D", "lock-null", XMLWriter.NO_CONTENT);
                        generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
                    } else {
                        generatedXML.writeElement("D", "resourcetype", XMLWriter.NO_CONTENT);
                    }
                } else {
                    generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
                    generatedXML.writeElement("D", "collection", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
                }

                generatedXML.writeProperty("D", "source", "");

                String supportedLocks = "<D:lockentry>"
                    + "<D:lockscope><D:exclusive/></D:lockscope>"
                    + "<D:locktype><D:write/></D:locktype>"
                    + "</D:lockentry>" + "<D:lockentry>"
                    + "<D:lockscope><D:shared/></D:lockscope>"
                    + "<D:locktype><D:write/></D:locktype>"
                    + "</D:lockentry>";
                generatedXML.writeElement("D", "supportedlock", XMLWriter.OPENING);
                generatedXML.writeText(supportedLocks);
                generatedXML.writeElement("D", "supportedlock", XMLWriter.CLOSING);

                generateLockDiscovery(path, generatedXML);

                generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
                generatedXML.writeElement("D", "status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
                generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

                break;

            case FIND_PROPERTY_NAMES :

                generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
                generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

                generatedXML.writeElement("D", "creationdate", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("D", "displayname", XMLWriter.NO_CONTENT);
                if (isFile) {
                    generatedXML.writeElement("D", "getcontentlanguage", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("D", "getcontentlength", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("D", "getcontenttype", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("D", "getetag", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("D", "getlastmodified", XMLWriter.NO_CONTENT);
                }
                generatedXML.writeElement("D", "resourcetype", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("D", "source", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("D", "lockdiscovery", XMLWriter.NO_CONTENT);

                generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
                generatedXML.writeElement("D", "status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
                generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

                break;

            case FIND_BY_PROPERTY :

                List<String> propertiesNotFound = new ArrayList<>();

                // Parse the list of properties

                generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
                generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

                for (String property : propNames) {

                switch (property) {
                    case "creationdate":
                        generatedXML.writeProperty ("D", "creationdate", 
                                getISOCreationDate(created));
                        break;
                    case "displayname":
                        generatedXML.writeElement("D", "displayname", XMLWriter.OPENING);
                        generatedXML.writeData(resourceName);
                        generatedXML.writeElement("D", "displayname", XMLWriter.CLOSING);
                        break;
                    case "getcontentlanguage":
                        if (isFile) {
                            generatedXML.writeElement("D", "getcontentlanguage",
                                    XMLWriter.NO_CONTENT);
                        } else {
                            propertiesNotFound.add(property);
                        }
                        break;
                    case "getcontentlength":
                        if (isFile) {
                            generatedXML.writeProperty("D", "getcontentlength",
                                    Long.toString(contentLength));
                        } else {
                            propertiesNotFound.add(property);
                        }
                        break;
                    case "getcontenttype":
                        if (isFile) {
                            generatedXML.writeProperty("D", "getcontenttype", contentType);
                        } else {
                            propertiesNotFound.add(property);
                        }
                        break;
                    case "getetag":
                        if (isFile) {
                            generatedXML.writeProperty("D", "getetag", eTag);
                        } else {
                            propertiesNotFound.add(property);
                        }
                        break;
                    case "getlastmodified":
                        if (isFile) {
                            generatedXML.writeProperty("D", "getlastmodified",
                                    FastHttpDateFormat.formatDate(lastModified));
                        } else {
                            propertiesNotFound.add(property);
                        }
                        break;
                    case "resourcetype":
                        if (isFile) {
                            if(isLockNull) {
                                generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
                                generatedXML.writeElement("D", "lock-null", XMLWriter.NO_CONTENT);
                                generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
                            } else {
                                generatedXML.writeElement("D", "resourcetype", XMLWriter.NO_CONTENT);
                            }
                        } else {
                            generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
                            generatedXML.writeElement("D", "collection", XMLWriter.NO_CONTENT);
                            generatedXML.writeElement("D", "resourcetype",XMLWriter.CLOSING);
                        }
                        break;
                    case "source":
                        generatedXML.writeProperty("D", "source", "");
                        break;
                    case "supportedlock":
                        supportedLocks = "<D:lockentry>"
                                + "<D:lockscope><D:exclusive/></D:lockscope>"
                                + "<D:locktype><D:write/></D:locktype>"
                                + "</D:lockentry>" + "<D:lockentry>"
                                + "<D:lockscope><D:shared/></D:lockscope>"
                                + "<D:locktype><D:write/></D:locktype>"
                                + "</D:lockentry>";
                        generatedXML.writeElement("D", "supportedlock", XMLWriter.OPENING);
                        generatedXML.writeText(supportedLocks);
                        generatedXML.writeElement("D", "supportedlock", XMLWriter.CLOSING);
                        break;
                    case "lockdiscovery":
                        if (!generateLockDiscovery(path, generatedXML))
                            propertiesNotFound.add(property);
                        break;
                    default:
                        propertiesNotFound.add(property);
                        break;
                }

                }

                generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
                generatedXML.writeElement("D", "status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
                generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

                if (!propertiesNotFound.isEmpty()) {

                    status = "HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND + " " +
                            WebdavStatus.getStatusText(WebdavStatus.SC_NOT_FOUND);

                    generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
                    generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

                    for (String propName : propertiesNotFound) {
                        generatedXML.writeElement("D", propName,
                                XMLWriter.NO_CONTENT);
                    }

                    generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
                    generatedXML.writeElement("D", "status", XMLWriter.OPENING);
                    generatedXML.writeText(status);
                    generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
                    generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

                }

                break;

            }

            generatedXML.writeElement("D", "response", XMLWriter.CLOSING);
        }
        
        /**
         * Get creation date in ISO format.
         * @return the formatted creation date
         */
        private String getISOCreationDate(long creationDate) {
            return creationDateFormat.format(new Date(creationDate));
        }

    }
    
    /**
     * Work around for XML parsers that don't fully respect
     * {@link DocumentBuilderFactory#setExpandEntityReferences(boolean)} when
     * called with <code>false</code>. External references are filtered out for
     * security reasons. See CVE-2007-5461.
     */
    private static class WebdavResolver implements EntityResolver {
        private ServletContext context;

        public WebdavResolver(ServletContext theContext) {
            context = theContext;
        }

        @Override
        public InputSource resolveEntity (String publicId, String systemId) {
            context.log(sm.getString("webdavservlet.enternalEntityIgnored",
                    publicId, systemId));
            return new InputSource(
                    new StringReader("Ignored external entity"));
        }
    }
}
