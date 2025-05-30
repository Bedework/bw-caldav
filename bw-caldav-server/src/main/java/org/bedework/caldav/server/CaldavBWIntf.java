/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.caldav.server;

import org.bedework.access.AccessException;
import org.bedework.access.AccessPrincipal;
import org.bedework.access.AccessXmlUtil.AccessXmlCb;
import org.bedework.access.Ace;
import org.bedework.access.AceWho;
import org.bedework.access.Acl;
import org.bedework.access.PrivilegeDefs;
import org.bedework.access.WhoDefs;
import org.bedework.base.response.GetEntityResponse;
import org.bedework.base.response.Response;
import org.bedework.caldav.server.CaldavBwNode.PropertyTagXrdEntry;
import org.bedework.caldav.server.calquery.CalData;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.filter.FilterHandler;
import org.bedework.caldav.server.get.FreeBusyGetHandler;
import org.bedework.caldav.server.get.GetHandler;
import org.bedework.caldav.server.get.IscheduleGetHandler;
import org.bedework.caldav.server.get.ServerInfoGetHandler;
import org.bedework.caldav.server.get.WebcalGetHandler;
import org.bedework.caldav.server.soap.synch.SynchConnections;
import org.bedework.caldav.server.soap.synch.SynchConnectionsMBean;
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.RetrievalMode;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.caldav.server.sysinterface.SysIntf.SynchReportData;
import org.bedework.caldav.server.sysinterface.SysIntf.SynchReportData.SynchReportDataItem;
import org.bedework.util.jmx.AnnotatedMBean;
import org.bedework.util.jmx.ManagementContext;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.AppleIcalTags;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CalWSXrdDefs;
import org.bedework.util.xml.tagdefs.CaldavDefs;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.util.xml.tagdefs.XcalTags;
import org.bedework.util.xml.tagdefs.XrdTags;
import org.bedework.util.xml.tagdefs.XsiTags;
import org.bedework.webdav.servlet.common.AccessUtil;
import org.bedework.webdav.servlet.common.Headers;
import org.bedework.webdav.servlet.common.Headers.IfHeaders;
import org.bedework.webdav.servlet.common.MethodBase.MethodInfo;
import org.bedework.webdav.servlet.common.WebdavServlet;
import org.bedework.webdav.servlet.common.WebdavUtils;
import org.bedework.webdav.servlet.shared.PrincipalPropertySearch;
import org.bedework.webdav.servlet.shared.WdEntity;
import org.bedework.webdav.servlet.shared.WdSynchReport;
import org.bedework.webdav.servlet.shared.WdSynchReport.WdSynchReportItem;
import org.bedework.webdav.servlet.shared.WdSysIntf;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import org.bedework.webdav.servlet.shared.WebdavPrincipalNode;
import org.bedework.webdav.servlet.shared.WebdavProperty;
import org.bedework.webdav.servlet.shared.WebdavServerError;
import org.bedework.webdav.servlet.shared.WebdavUnauthorized;
import org.bedework.webdav.servlet.shared.WebdavUnsupportedMediaType;
import org.bedework.webdav.servlet.shared.serverInfo.Application;
import org.bedework.webdav.servlet.shared.serverInfo.Feature;
import org.bedework.webdav.servlet.shared.serverInfo.ServerInfo;

import ietf.params.xml.ns.caldav.FilterType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.oasis_open.docs.ns.xri.xrd_1.AnyURI;
import org.oasis_open.docs.ns.xri.xrd_1.LinkType;
import org.oasis_open.docs.ns.xri.xrd_1.XRDType;
import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesBasePropertyType;
import org.w3c.dom.Element;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;

import javax.management.ObjectName;
import javax.xml.namespace.QName;

import static org.bedework.base.response.Response.Status.forbidden;
import static org.bedework.base.response.Response.Status.notFound;

/** This class implements a namespace interface for the webdav abstract
 * servlet. One of these interfaces is associated with each current session.
 *
 * <p>As a first pass we'll define webdav urls as starting with <br/>
 * /user/user-name/calendar-name/<br/>
 *
 * <p>uri resolution should be made part of the core calendar allowing all
 * such distinctions to be removed from this code.
 *
 * <p>The part following the above prefix probably determines exactly what is
 * delivered. We may want the entire calendar (or what we show by default) or
 * a single event from the calendar
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public class CaldavBWIntf extends WebdavNsIntf {
  /** Namespace prefix based on the request url.
   */
  private String namespacePrefix;

  private AccessUtil accessUtil;

  /** Namespace based on the request url.
   */
  @SuppressWarnings("unused")
  private String namespace;

  SysIntf sysi;

  /* true if this is a CalWS server */
  private boolean calWs;

  private static ServerInfo serverInfo;

  /* ==============================================================
   *                     JMX configuration
   * ============================================================== */

  /* Marks the bedework end of the synch service. This is a web
    service called by the synch engine to get information out of
    bedework and to update events and status.

    This service should probably be restricted to a given host only.

    Coming up on a separate port might help to lock it down.
   */
  private boolean synchWs;

  /* Marks the bedework end of the notification service. This is a web
    service called by the notification engine to get information out of
    bedework and to update notifications.

    This service should probably be restricted to a given host only.

    Coming up on a separate port might help to lock it down.
   */
  private boolean notifyWs;

  /* Marks the bedework end of the websockets service. That service
    acts as a websockets proxy to CalDAV.
   */
  private boolean socketWs;

  private final static Set<ObjectName> registeredMBeans =
          new CopyOnWriteArraySet<>();
  private static ManagementContext managementContext;
  private static SynchConnections synchConn;

  /*
  static {
    try {
      synchConn = new SynchConnections();
      registerMbean(new ObjectName(synchConn.getServiceName()),
                    synchConn);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  */

  public static void registerMbean(final ObjectName key,
                                   final Object bean) {
    try {
      AnnotatedMBean.registerMBean(getManagementContext(), bean, key);
      registeredMBeans.add(key);
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * @param key bean objectname
   */
  public static void unregister(final ObjectName key) {
    if (registeredMBeans.remove(key)) {
      try {
        getManagementContext().unregisterMBean(key);
      } catch (final Throwable e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @return the management context.
   */
  public static ManagementContext getManagementContext() {
    if (managementContext == null) {
      /* Try to find the jboss mbean server * /

      MBeanServer mbsvr = null;

      for (MBeanServer svr: MBeanServerFactory.findMBeanServer(null)) {
        if (svr.getDefaultDomain().equals("jboss")) {
          mbsvr = svr;
          break;
        }
      }

      if (mbsvr == null) {
        Logger.getLogger(ConfBase.class).warn("Unable to locate jboss mbean server");
      }
      managementContext = new ManagementContext(mbsvr);
      */
      managementContext = new ManagementContext(ManagementContext.DEFAULT_DOMAIN);
    }
    return managementContext;
  }

  static void contextInitialized(final ServletContextEvent sce) {
    /* We may enter a number of times for each context implementing
       DAV or SOAP services.

       We cannot set flags such as synchws as static fields because
       they are context dependent and the context share a common
       set of loaded classes.

       We'll only do this once - based on the presence of a management
       context.
     */
    try {
      synchronized (registeredMBeans) {
        if (managementContext != null) {
          // Already done
          return;
        }

        final ServletContext sc = sce.getServletContext();

        synchConn = new SynchConnections();
        registerMbean(new ObjectName(synchConn.getServiceName()),
                      synchConn);
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  static void contextDestroyed(final ServletContextEvent sce) {
    synchronized (registeredMBeans) {
      if (managementContext == null) {
        // Already cleaned up.
        return;
      }

      try {
        for (final ObjectName on: registeredMBeans) {
          unregister(on);
        }
      } catch (final Throwable t) {
        t.printStackTrace();
      } finally {
        try {
          managementContext.stop();
        } catch (final Throwable ignored) {}

        managementContext = null;
      }
    }
  }

  /* ==============================================================
   *                     Interface methods
   * ============================================================== */

  @Override
  public void init(final WebdavServlet servlet,
                   final HttpServletRequest req,
                   final HashMap<String, MethodInfo> methods,
                   final boolean dumpContent) {
    try {
      // Needed before any other initialization
      calWs = Boolean.parseBoolean(servlet.getInitParameter("calws"));
      synchWs = Boolean.parseBoolean(servlet.getInitParameter("synchws"));
      notifyWs = Boolean.parseBoolean(servlet.getInitParameter("notifyws"));
      socketWs = Boolean.parseBoolean(servlet.getInitParameter("socketws"));
      sysi = getSysi(servlet.getInitParameter("sysintfImpl"));

      super.init(servlet, req, methods, dumpContent);

      namespacePrefix = WebdavUtils.getUrlPrefix(req);
      namespace = namespacePrefix + "/schema";

      account = sysi.init(req, account, false,
                          calWs, synchWs, notifyWs, socketWs, null);

      accessUtil = new AccessUtil(namespacePrefix, xml,
                                  new CalDavAccessXmlCb(sysi));
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  public SynchConnectionsMBean getActiveConnections() {
    /*
    if (conns == null) {
      conns = (SynchConnectionsMBean)MBeanUtil.getMBean(
              SynchConnectionsMBean.class,
              "org.bedework:service=CalDAVSynchConnections");
    }
*/
    return synchConn;
  }

  /** See if we can reauthenticate. Use for real-time service which needs to
   * authenticate as a particular principal.
   *
   * @param req http request
   * @param account to reinit as
   * @param service - true if this is a service call - e.g. iSchedule -
   *                rather than a real user.
   * @param opaqueData  - possibly from headers
   */
  public void reAuth(final HttpServletRequest req,
                     final String account,
                     final boolean service,
                     final String opaqueData) {
    try {
      if (sysi != null) {
        try {
          sysi.close();
        } catch (final Throwable t) {
          throw new WebdavException(t);
        }
      } else {
        sysi = getSysi(servlet.getInitParameter("sysintfImpl"));
      }

      this.account = account;

      sysi.init(req, account, service,
                calWs, synchWs, notifyWs, socketWs, opaqueData);

      accessUtil = new AccessUtil(namespacePrefix, xml,
                                  new CalDavAccessXmlCb(sysi));
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @return boolean
   */
  public boolean getCalWS() {
    return calWs;
  }

  /** Get the synch web service flag
   *
   * @return true if it's a synch service
   */
  public boolean getSynchWs() {
    return synchWs;
  }

  /** Get the notify web service flag
   *
   * @return true if it's a notify service
   */
  public boolean getNotifyWs() {
    return notifyWs;
  }

  @Override
  public String getDavHeader(final WebdavNsNode node) {
    if (account == null) {
      return super.getDavHeader(node) + ", calendar-access";
    }

    String hdr = super.getDavHeader(node) +
        ", calendar-access" +
        ", calendar-schedule" +
        ", calendar-auto-schedule" +
        ", calendar-default-alarms" +
        ", calendarserver-sharing";

    if (getSysi().getSystemProperties().getTimezonesByReference()) {
        hdr += ", calendar-no-timezone";
    }

    return hdr;
  }

  @Override
  public ServerInfo getServerInfo() {
    if (serverInfo !=  null) {
      return serverInfo;
    }

    serverInfo = super.getServerInfo();

    /* Augment with our services */

    //<calendarserver-principal-property-search xmlns='http://calendarserver.org/ns/'/>
    //<calendarserver-principal-search xmlns='http://calendarserver.org/ns/'/>

    final Application app = new Application("caldav");

    app.addFeature(new Feature(CaldavTags.calendarAccess));
    app.addFeature(new Feature(CaldavTags.calendarAutoschedule));
    app.addFeature(new Feature(CaldavTags.calendarDefaultAlarms));
    app.addFeature(new Feature(CaldavTags.calendarNoTimezone));
    app.addFeature(new Feature(AppleServerTags.calendarServerSharing));

    serverInfo.addApplication(app);

    return serverInfo;
  }

  @Override
  public void emitError(final QName errorTag, final String extra,
                        final XmlEmit xml) {
    if (errorTag.equals(CaldavTags.noUidConflict)) {
      xml.openTag(errorTag);
      if (extra != null) {
        xml.property(WebdavTags.href, sysi.getUrlHandler().prefix(extra));
      }
      xml.closeTag(errorTag);
    } else {
      super.emitError(errorTag, extra, xml);
    }
  }

  /**
   */
  private static class CalDavAccessXmlCb implements AccessXmlCb, Serializable {
    private final SysIntf sysi;

    private QName errorTag;
    private String errorMsg;

    CalDavAccessXmlCb(final SysIntf sysi) {
      this.sysi = sysi;
    }

    @Override
    public String makeHref(final String id, final int whoType) throws AccessException {
      try {
        return sysi.makeHref(id, whoType);
      } catch (final Throwable t) {
        throw new AccessException(t);
      }
    }

    @Override
    public AccessPrincipal getPrincipal() throws AccessException {
      try {
        return sysi.getPrincipal();
      } catch (final Throwable t) {
        throw new AccessException(t);
      }
    }

    @Override
    public AccessPrincipal getPrincipal(final String href) throws AccessException {
      try {
        return sysi.getPrincipal(sysi.getUrlHandler().unprefix(href));
      } catch (final Throwable t) {
        throw new AccessException(t);
      }
    }

    @Override
    public void setErrorTag(final QName tag) {
      errorTag = tag;
    }

    @Override
    public QName getErrorTag() {
      return errorTag;
    }

    @Override
    public void setErrorMsg(final String val) {
      errorMsg = val;
    }

    @Override
    public String getErrorMsg() {
      return errorMsg;
    }
  }

  @Override
  public AccessUtil getAccessUtil() {
    return accessUtil;
  }

  @Override
  public boolean canPut(final WebdavNsNode node) {
    CalDAVEvent<?> ev = null;

    if (node instanceof final CaldavComponentNode comp) {
      ev = comp.getEvent();
    } else if (!(node instanceof CaldavResourceNode)) {
      return false;
    }

    if (ev != null) {
      return sysi.checkAccess(ev,
                              PrivilegeDefs.privWriteContent,
                              true).getAccessAllowed();
    } else {
      return sysi.checkAccess(node.getCollection(true), // deref - we're trying to put into the target
                              PrivilegeDefs.privBind, true).getAccessAllowed();
    }
  }

  @Override
  public String getAddMemberSuffix() {
    return ";add-member";
  }

  @Override
  public boolean getDirectoryBrowsingDisallowed() {
    return sysi.getAuthProperties().getDirectoryBrowsingDisallowed();
  }

  @Override
  public void rollback() {
    sysi.rollback();
  }

  @Override
  public void close() {
    sysi.close();
  }

  @Override
  public WdSysIntf getSysIntf() {
    return sysi;
  }

  /**
   * @return SysIntf
   */
  public SysIntf getSysi() {
    return sysi;
  }

  @Override
  public String getSupportedLocks() {
    return null; // No locks
    /*
     return  "<DAV:lockentry>" +
             "  <DAV:lockscope>" +
             "    <DAV:exclusive/>" +
             "  </DAV:lockscope>" +
             "  <DAV:locktype><DAV:write/></DAV:locktype>" +
             "</DAV:lockentry>";
             */
  }

  @Override
  public boolean getAccessControl() {
    return true;
  }

  @Override
  public void addNamespace(final XmlEmit xml) {
    if (calWs) {
      xml.addNs(new NameSpace(CalWSXrdDefs.namespace, "CalWS"), true);
      xml.addNs(new NameSpace(XsiTags.namespace, "xsi"), false);
      xml.addNs(new NameSpace(XrdTags.namespace, "xrd"), false);

      // Need these for the time being
      xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
      xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);

      return;
    }

    super.addNamespace(xml);

    xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), true);
    xml.addNs(new NameSpace(AppleIcalTags.appleIcalNamespace, "AI"), false);
    xml.addNs(new NameSpace(CaldavDefs.icalNamespace, "ical"), false);
    xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CS"), false);
    xml.addNs(new NameSpace(BedeworkServerTags.bedeworkCaldavNamespace, "BSS"), false);
  }

  @Override
  public WebdavNsNode getNode(final String uri,
                              final int existence,
                              final int nodeType,
                              final boolean addMember) {
    return getNodeInt(uri, existence, nodeType, addMember,
                      null, null, null);
  }

  @Override
  public void putNode(final WebdavNsNode node) {
  }

  @Override
  public void delete(final WebdavNsNode node) {
    try {
      if (node instanceof final CaldavResourceNode rnode) {
        sysi.deleteFile(rnode.getResource());
      } else if (node instanceof final CaldavComponentNode cnode) {
        final CalDAVEvent<?> ev = cnode.getEvent();

        if (ev != null) {
          if (debug()) {
            debug("About to delete event " + ev);
          }

          boolean sendSchedulingMessage = true;

          if (sysi.testMode()) {
            final String userAgent = getRequest().getHeader("user-agent");

            if ((userAgent != null) &&
                    (userAgent.contains("| END_REQUESTS") ||
                             userAgent.contains("| START_REQUESTS")) &&
                    userAgent.contains("| DELETEALL")) {
              sendSchedulingMessage = false;
            }
          }

          if (!CalDavHeaders.scheduleReply(getRequest())) {
            sendSchedulingMessage = false;
          }

          sysi.deleteEvent(ev, sendSchedulingMessage);
        } else {
          if (debug()) {
            debug("No event object available");
          }
        }
      } else {
        if (!(node instanceof final CaldavCalNode cnode)) {
          throw new WebdavUnauthorized();
        }

        final CalDAVCollection<?> col =
                (CalDAVCollection<?>)cnode.getCollection(false); // Don't deref for delete

        boolean sendSchedulingMessage = true;

        if (sysi.testMode()) {
          final String userAgent = getRequest().getHeader("user-agent");

          if ((userAgent != null) &&
                  userAgent.contains("| END_REQUESTS") &&
                  userAgent.contains("| DELETEALL")) {
            sendSchedulingMessage = false;
          }
        }
        sysi.deleteCollection(col, sendSchedulingMessage);
      }
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<WebdavNsNode> getChildren(
          final WebdavNsNode node,
          final Supplier<Object> filterGetter) {
    try {
      final ArrayList<WebdavNsNode> al = new ArrayList<>();

      if (!node.isCollection()) {
        // Don't think we should have been called
        return al;
      }

      if (debug()) {
        debug("About to get children for " + node.getUri());
      }

      final Collection<? extends WdEntity<?>> children =
              node.getChildren(filterGetter);

      if (children == null) {
        // Perhaps no access
        return al;
      }

      final String uri = node.getUri();
      final CalDAVCollection<?> parent =
              (CalDAVCollection<?>)node.getCollection(false);  // don't deref

      for (final WdEntity<?> wde: children) {
        final CalDAVCollection<?> col;
        CalDAVResource<?> r = null;
        CalDAVEvent<?> ev = null;

        final String name = wde.getName();
        final int nodeType;

        switch (wde) {
          case final CalDAVCollection<?> calDAVCollection -> {
            col = calDAVCollection;

            nodeType = WebdavNsIntf.nodeTypeCollection;
            if (debug()) {
              debug("Found child " + col);
            }
          }
          case final CalDAVResource<?> calDAVResource -> {
            col = parent;
            r = calDAVResource;

            nodeType = WebdavNsIntf.nodeTypeEntity;
          }
          case final CalDAVEvent<?> calDAVEvent -> {
            col = parent;
            ev = calDAVEvent;

            nodeType = WebdavNsIntf.nodeTypeEntity;
          }
          default ->
                  throw new WebdavException("Unexpected return type");
        }

        al.add(getNodeInt(Util.buildPath(false, uri, "/", name),
                          WebdavNsIntf.existanceDoesExist,
                          nodeType,
                          false,
                          col, ev, r));
      }

      return al;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public WebdavNsNode getParent(final WebdavNsNode node) {
    return null;
  }

  @Override
  public boolean prefetch(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final WebdavNsNode node) {
    if (!super.prefetch(req, resp, node)) {
      return false;
    }

    if (calWs) {
      return true;
    }

    if (!(node instanceof final CaldavComponentNode cnode)) {
      return true;
    }

    if (!cnode.getEvent().getOrganizerSchedulingObject() &&
        !cnode.getEvent().getAttendeeSchedulingObject()) {
      return true;
    }

    // Add the schedule tag header

    resp.setHeader("Schedule-Tag", cnode.getStagValue());

    return true;
  }

  @Override
  public Content getContent(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final String contentType,
                            final WebdavNsNode node) {
    try {
      String ctype = contentType;

      if (ctype == null) {
        final String accept = req.getHeader("ACCEPT");

        if (accept != null) {
          ctype = accept.trim();
        }
      }

      if (node.isCollection()) {
        if ((ctype == null) || ctype.contains("text/html")) {
          if (getDirectoryBrowsingDisallowed()) {
            throw new WebdavException(HttpServletResponse.SC_FORBIDDEN);
          }

          final Content c = new Content();

          final String content = generateHtml(req, node);
          c.rdr = new CharArrayReader(content.toCharArray());
          c.contentType = "text/html";
          c.contentLength = content.getBytes().length;

          return c;
        }
      }

      /* ===================  Try for XRD fetch ======================= */

      if (calWs && "application/xrd+xml".equals(ctype)) {
        return doXrd(req, resp, (CaldavBwNode)node);
      }

      /* ===================  Try for calendar fetch ======================= */

      if (node.isCollection() && "text/calendar".equals(ctype)) {
        final GetHandler handler = new WebcalGetHandler(this);
        final RequestPars pars = new RequestPars(req, this, getResourceUri(req));

        pars.setWebcalGetAccept(true);

        handler.process(req, resp, pars);

        final Content c = new Content();

        c.written = true; // set content to say it's done

        return c;
      }

      if (node.isCollection()) {
        return null;
      }

      if (!node.getAllowsGet()) {
        return null;
      }

      if (!"text/calendar" .equals(ctype) &&
              !"application/calendar+json" .equals(ctype) &&
              !"application/jscalendar+json" .equals(ctype) &&
              !XcalTags.mimetype.equals(ctype)) {
        ctype = sysi.getDefaultContentType();
      }

      resp.setContentType(ctype + ";charset=utf-8");

      final Content c = new Content();
      c.written = true;

      c.contentType = node.writeContent(null, resp.getWriter(), ctype);

      if (c.contentType.indexOf(';') < 0) {
        // No charset
        c.contentType += ";charset=utf-8";
      }

      return c;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Content getBinaryContent(final WebdavNsNode node) {
    try {
      if (!node.getAllowsGet()) {
        return null;
      }

      if (!(node instanceof final CaldavResourceNode bwnode)) {
        throw new WebdavException("Unexpected node type");
      }

      final Content c = new Content();

      c.stream = bwnode.getContentStream();
      c.contentType = node.getContentType();
      c.contentLength = node.getContentLen();

      return c;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public String getAcceptContentType(final HttpServletRequest req) {
    final String accept = req.getHeader("Accept");

    if (accept != null) {
      return accept;
    }

    final String[] contentTypePars;
    final String contentType = req.getContentType();

    String ctype = null;

    if (contentType != null) {
      contentTypePars = contentType.split(";");
      ctype = contentTypePars[0];
    }

    if (ctype == null) {
      return ctype;
    }

    return sysi.getDefaultContentType();
  }

  @Override
  public PutContentResult putContent(final HttpServletRequest req,
                                     final HttpServletResponse resp,
                                     final WebdavNsNode node,
                                     final String[] contentTypePars,
                                     final Reader contentRdr,
                                     final IfHeaders ifHeaders) {
    try {
      final PutContentResult pcr = new PutContentResult();
      pcr.node = node;

      if (node instanceof CaldavResourceNode) {
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      final CaldavComponentNode bwnode = (CaldavComponentNode)node;
      final CalDAVCollection<?> col =
              (CalDAVCollection<?>)node.getCollection(true); // deref - put into target

      boolean calContent = false;
      if ((contentTypePars != null) && (contentTypePars.length > 0)) {
        calContent = contentTypePars[0].equals("text/calendar") ||
                     contentTypePars[0].equals("application/calendar+xml") ||
                     contentTypePars[0].equals("application/calendar+json") ||
                     contentTypePars[0].equals("application/jscalendar+json");
      }

      if ((col.getCalType() != CalDAVCollection.calTypeCalendarCollection) ||
          !calContent) {
        throw new WebdavForbidden(CaldavTags.supportedCalendarData);
      }

      /* We can only put a single resource - that resource will be an ics file
       * containing freebusy information or an event or vtodo and possible overrides.
       */

      pcr.created = putEvent(req, resp, bwnode,
                             contentRdr,
                             contentTypePars[0],
                             ifHeaders);

      return pcr;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public PutContentResult putBinaryContent(final HttpServletRequest req,
                                           final WebdavNsNode node,
                                           final String[] contentTypePars,
                                           final InputStream contentStream,
                                           final IfHeaders ifHeaders) {
    try {
      final PutContentResult pcr = new PutContentResult();
      pcr.node = node;

      if (!(node instanceof final CaldavResourceNode bwnode)) {
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      final CalDAVCollection<?> col =
              (CalDAVCollection<?>)node.getCollection(true);

      if ((col == null) ||
          (col.getCalType() == CalDAVCollection.calTypeCalendarCollection)) {
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      final CalDAVResource<?> r = bwnode.getResource();

      if (r.isNew()) {
        ifHeaders.create = true;
      }

      String contentType = null;

      if ((contentTypePars != null) && (contentTypePars.length > 0)) {
        for (final String c: contentTypePars) {
          if (c == null) {
            continue;
          }

          if (contentType != null) {
            contentType += ";" + c;
          } else {
            contentType = c;
          }
        }
      }

      r.setContentType(contentType);
      r.setBinaryContent(contentStream);

      if (ifHeaders.create) {
        sysi.putFile(col, r);
      } else {
        sysi.updateFile(r, true);
      }
      return pcr;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private boolean putEvent(final HttpServletRequest req,
                           final HttpServletResponse resp,
                           final CaldavComponentNode bwnode,
                           final Reader contentRdr,
                           final String contentType,
                           final IfHeaders ifHeaders) {
    final var ifStag = Headers.ifScheduleTagMatch(req);
    final var noInvites = req.getHeader("Bw-NoInvites") != null; // based on header?
    final var ifEtag = ifHeaders.ifEtag;
    final var create = ifHeaders.create;

    //BwEvent ev = evinfo.getEvent();
    String entityName = bwnode.getEntityName();

    final CalDAVCollection<?> col =
            (CalDAVCollection<?>)bwnode.getCollection(true);
    boolean created = false;

    final SysiIcalendar cal =
            sysi.fromIcal(col, contentRdr, contentType,
                          IcalResultType.OneComponent,
                          true); // mergeAttendees
    cal.assertNoMethod("PUT");

    final var ev = cal.getOnlyEvent();

    if (entityName == null) {
      entityName = ev.getUid() + ".ics";
      bwnode.setEntityName(entityName);
    }

    if (debug()) {
      debug("putContent: intf has event with name " + entityName);
    }

    if (ev.isNew()) {
      created = true;
      ev.setName(entityName);

      /* Collection<BwEventProxy>failedOverrides = */
      sysi.addEvent(ev, noInvites, true);
    } else if (create) {
      /* Resource already exists */

      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
    } else {
      if (!entityName.equals(ev.getName())) {
        /* Probably specifying a different uid */
        throw new WebdavForbidden(CaldavTags.noUidConflict);
      }

      if ((ifEtag != null) &&
          (!ifEtag.equals(bwnode.getPrevEtagValue(true)))) {
        if (debug()) {
          debug("putContent: etag mismatch if=" + ifHeaders.ifEtag +
                   "prev=" + bwnode.getPrevEtagValue(true));
        }
        rollback();
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      if ((ifStag != null) &&
          (!ifStag.equals(bwnode.getPrevStagValue()))) {
        if (debug()) {
          debug("putContent: stag mismatch if=" + ifStag +
                   "prev=" + bwnode.getPrevStagValue());
        }
        rollback();
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      if (debug()) {
        debug("putContent: update event " + ev);
      }
      sysi.updateEvent(ev);
    }

    bwnode.setEvent(ev);

    if (ev.getOrganizerSchedulingObject() ||
        ev.getAttendeeSchedulingObject()) {
      resp.setHeader("Schedule-Tag", ev.getScheduleTag());
    }

    return created;
  }

  /**
   * @param resp http response
   * @param bwnode component node
   * @param ical ical object
   * @param create true for create
   * @param noInvites true for send no invites
   * @param ifStag schedule tag
   * @param ifEtag etag
   * @return true for OK
   */
  public boolean putEvent(final HttpServletResponse resp,
                          final CaldavComponentNode bwnode,
                          final IcalendarType ical,
                          final boolean create,
                          final boolean noInvites,
                          final String ifStag,
                          final String ifEtag) {
    String entityName = bwnode.getEntityName();

    final CalDAVCollection<?> col =
            (CalDAVCollection<?>)bwnode.getCollection(true); // deref
    boolean created = false;

    final SysiIcalendar cal = sysi.fromIcal(col, ical,
                                            IcalResultType.OneComponent);
    cal.assertNoMethod("PUT");

    final var ev = cal.getOnlyEvent();

    if (entityName == null) {
      entityName = ev.getUid() + ".ics";
      bwnode.setEntityName(entityName);
    }

    if (debug()) {
      debug("putContent: intf has event with name " + entityName);
    }

    if (ev.isNew()) {
      created = true;
      ev.setName(entityName);

      /* Collection<BwEventProxy>failedOverrides = */
      sysi.addEvent(ev, noInvites, true);

      bwnode.setEvent(ev);
    } else if (create) {
      /* Resource already exists */

      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
    } else {
      if (!entityName.equals(ev.getName())) {
        /* Probably specifying a different uid */
        throw new WebdavForbidden(CaldavTags.noUidConflict);
      }

      if ((ifEtag != null) &&
          (!ifEtag.equals(bwnode.getPrevEtagValue(true)))) {
        if (debug()) {
          debug("putContent: etag mismatch if=" + ifEtag +
                   "prev=" + bwnode.getPrevEtagValue(true));
        }
        rollback();
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      if ((ifStag != null) &&
          (!ifStag.equals(bwnode.getPrevStagValue()))) {
        if (debug()) {
          debug("putContent: stag mismatch if=" + ifStag +
                   "prev=" + bwnode.getPrevStagValue());
        }
        rollback();
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      if (debug()) {
        debug("putContent: update event " + ev);
      }
      sysi.updateEvent(ev);
    }

    if (ev.getOrganizerSchedulingObject() ||
        ev.getAttendeeSchedulingObject()) {
      resp.setHeader("Schedule-Tag", ev.getScheduleTag());
    }

    return created;
  }

  @Override
  public void create(final WebdavNsNode node) {
  }

  @Override
  public void createAlias(final WebdavNsNode alias) {
  }

  @Override
  public void acceptMkcolContent(final HttpServletRequest req) {
    throw new WebdavUnsupportedMediaType();
  }

  /** Create an empty collection at the given location.
   *
   * <pre>
   *  201 (Created) - The calendar collection resource was created in its entirety.
   *  403 (Forbidden) - This indicates at least one of two conditions: 1) the
   *          server does not allow the creation of calendar collections at the
   *          given location in its namespace, or 2) the parent collection of the
   *          Request-URI exists but cannot accept members.
   *  405 (Method Not Allowed) - MKCALENDAR can only be executed on a null resource.
   *  409 (Conflict) - A collection cannot be made at the Request-URI until one
   *          or more intermediate collections have been created.
   *  415 (Unsupported Media Type)- The server does not support the request type
   *          of the body.
   *  507 (Insufficient Storage) - The resource does not have sufficient space
   *          to record the state of the resource after the execution of this method.
   *
   * @param req       HttpServletRequest
   * @param node             node to create
   */
  @Override
  public void makeCollection(final HttpServletRequest req,
                             final HttpServletResponse resp,
                             final WebdavNsNode node) {
    try {
      if (!(node instanceof final CaldavCalNode bwnode)) {
        throw new WebdavBadRequest("Not a valid node object " +
                                   node.getClass().getName());
      }

      /* The uri should have an entity name representing the new collection
       * and a collection object representing the parent.
       *
       * A namepart of null means that the path already exists
       */

      final CalDAVCollection<?> newCol =
              (CalDAVCollection<?>)bwnode.getCollection(false); // No deref?

      final CalDAVCollection<?> parent =
              getSysi().getCollection(newCol.getParentPath());
      if (parent == null) {
        throw new RuntimeException("No parent collection");
      }

      if (parent.getCalType() == CalDAVCollection.calTypeCalendarCollection) {
        throw new WebdavForbidden(CaldavTags.calendarCollectionLocationOk);
      }

      if (newCol.getName() == null) {
        throw new WebdavForbidden("Forbidden: Null name");
      }

      resp.setStatus(sysi.makeCollection(newCol));
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public void copyMove(final HttpServletRequest req,
                       final HttpServletResponse resp,
                       final WebdavNsNode from,
                       final WebdavNsNode to,
                       final boolean copy,
                       final boolean overwrite,
                       final int depth) {
    if (from instanceof CaldavCalNode) {
      copyMoveCollection(resp, (CaldavCalNode)from,
                         to, copy, overwrite, depth);

      return;
    }

    // Copy entity or resource
    if ((depth != Headers.depthNone) && (depth != 0)) {
      throw new WebdavBadRequest();
    }

    if (from instanceof CaldavComponentNode) {
      copyMoveComponent(resp, (CaldavComponentNode)from,
                        to, copy, overwrite);
      return;
    }

    if (from instanceof CaldavResourceNode) {
      copyMoveResource(resp, (CaldavResourceNode)from,
                       to, copy, overwrite);
      return;
    }

    throw new WebdavBadRequest();
  }

  private Content doXrd(final HttpServletRequest req,
                        final HttpServletResponse resp,
                        final CaldavBwNode node) {
    resp.setContentType("application/xrd+xml;charset=utf-8");

    try {
      final XRDType xrd = getXRD(node);

      final JAXBContext jc = JAXBContext.newInstance(xrd.getClass().getPackage().getName());

      final Marshaller m = jc.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(xrd, resp.getOutputStream());

      final Content c = new Content();

      c.written = true; // set content to say it's done

      return c;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param node
   * @param props
   */
  public void getCalWSProperties(final CaldavBwNode node,
                                 final List<GetPropertiesBasePropertyType> props) {
    for (final PropertyTagEntry pte: node.getCalWSSoapNames()) {
      if (pte.inPropAll) {
        node.generateCalWsProperty(props, pte.tag, this, true);
      }
    }
  }

  /**
   * @param node
   * @return the XRD object for the node
   */
  public XRDType getXRD(final CaldavBwNode node) {
    try {
      final XRDType xrd = new XRDType();

      final AnyURI uri = new AnyURI();

      uri.setValue(node.getUrlValue());
      xrd.setSubject(uri);

      for (final PropertyTagXrdEntry pxe: node.getXrdNames()) {
        if (pxe.inPropAll) {
          node.generateXrdProperties(xrd.getAliasOrPropertyOrLink(),
                                     pxe.xrdName, this, true);
        }
      }

      if (node.isCollection()) {
        // Provide link info for each child collection

        for (final WebdavNsNode child: getChildren(node, null)) {
          final CaldavBwNode cn = (CaldavBwNode)child;

          final LinkType l = new LinkType();
          l.setRel(CalWSXrdDefs.childCollection);
          l.setHref(cn.getUrlValue());

          for (final PropertyTagXrdEntry pxe: node.getXrdNames()) {
            if (pxe.inLink) {
              cn.generateXrdProperties(l.getTitleOrPropertyOrAny(),
                                       pxe.xrdName, this, true);
            }
          }
        }
      }

      return xrd;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void copyMoveCollection(final HttpServletResponse resp,
                                  final CaldavCalNode from,
                                  final WebdavNsNode to,
                                  final boolean copy,
                                  final boolean overwrite,
                                  final int depth) {
    if (!(to instanceof final CaldavCalNode toCalNode)) {
      throw new WebdavBadRequest();
    }

    // Copy folder
    if ((depth != Headers.depthNone) && (depth != Headers.depthInfinity)) {
      throw new WebdavBadRequest();
    }

    if (toCalNode.getExists() && !overwrite) {
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

      return;
    }

    /* XXX This is NOT rename - we really move or copy.
     * For the moment we'll deref both - but I think there are alias issues here
     */
    final CalDAVCollection<?> fromCol =
            (CalDAVCollection<?>)from.getCollection(true);
    final CalDAVCollection<?> toCol =
            (CalDAVCollection<?>)toCalNode.getCollection(true);

    if ((fromCol == null) || (toCol == null)) {
      // What do we do here?
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

      return;
    }

    getSysi().copyMove(fromCol, toCol, copy, overwrite);
    if (toCalNode.getExists()) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setStatus(HttpServletResponse.SC_CREATED);
      Headers.makeLocation(resp, getLocation(to));
    }
  }

  private void copyMoveComponent(final HttpServletResponse resp,
                                 final CaldavComponentNode from,
                                 final WebdavNsNode to,
                                 final boolean copy,
                                 final boolean overwrite) {
    if (!(to instanceof final CaldavComponentNode toNode)) {
      throw new WebdavBadRequest();
    }

    if (toNode.getExists() && !overwrite) {
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

      return;
    }

    /* deref - copy/move into targetted collection */
    final CalDAVCollection<?> toCol =
            (CalDAVCollection<?>)toNode.getCollection(true);

    if (!getSysi().copyMove(from.getEvent(),
                            toCol, toNode.getEntityName(), copy,
                            overwrite)) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setStatus(HttpServletResponse.SC_CREATED);
      Headers.makeLocation(resp, getLocation(to));
    }
  }

  private void copyMoveResource(final HttpServletResponse resp,
                               final CaldavResourceNode from,
                               final WebdavNsNode to,
                               final boolean copy,
                               final boolean overwrite) {
    if (!(to instanceof final CaldavResourceNode toNode)) {
      throw new WebdavForbidden(CaldavTags.supportedCalendarData);
    }

    if (toNode.getExists() && !overwrite) {
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

      return;
    }

    if (!getSysi().copyMoveFile(from.getResource(),
                                toNode.getPath(), toNode.getEntityName(), copy,
                            overwrite)) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setStatus(HttpServletResponse.SC_CREATED);
      Headers.makeLocation(resp, getLocation(to));
    }
  }

  @Override
  public boolean specialUri(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final String resourceUri) {
    final RequestPars pars = new RequestPars(req, this, resourceUri);
    GetHandler handler = null;

    if (pars.isiSchedule()) {
      handler = new IscheduleGetHandler(this);
    } else if (pars.isServerInfo()) {
      handler = new ServerInfoGetHandler(this);
    } else if (pars.isFreeBusy()) {
      handler = new FreeBusyGetHandler(this);
    } else if (pars.isWebcal()) {
      handler = new WebcalGetHandler(this);
    }

    if (handler == null) {
      return false;
    }

    handler.process(req, resp, pars);

    return true;
  }

  @Override
  public WdSynchReport getSynchReport(final String path,
                                      final String token,
                                      final int limit,
                                      final boolean recurse) {
    final SynchReportData srd = getSysi().getSyncReport(path, token, limit, recurse);

    if (srd == null) {
      return null;
    }

    final WdSynchReport wsr = new WdSynchReport();

    wsr.tokenValid = srd.tokenValid;

    if (!srd.tokenValid) {
      return wsr;
    }

    wsr.token = srd.token;
    wsr.truncated = srd.truncated;
    wsr.items = new ArrayList<>();

    final Map<String, WebdavNsNode> parents =
            new HashMap<>();

    for (final SynchReportDataItem srdi: srd.items) {
      final int nodeType;
      final CalDAVCollection<?> col;
      CalDAVResource<?> r = null;
      CalDAVEvent<?> ev = null;
      final WdSynchReportItem wri;
      WebdavNsNode parent; // Need for non-collection
      final String name;
      final boolean canSync;

      if (srdi.getCol() == null) {
        parent = parents.get(srdi.getVpath());
        if (parent == null) {
          parent = getNode(srdi.getVpath(),
                           WebdavNsIntf.existanceMust,
                           WebdavNsIntf.nodeTypeCollection,
                           false);

          parents.put(srdi.getVpath(), parent);
        }

        col = (CalDAVCollection<?>)parent.getCollection(false);
        canSync = true;

        if (srdi.getEntity() != null) {
          nodeType = WebdavNsIntf.nodeTypeEntity;
          ev = srdi.getEntity();
          name = ev.getName();
        } else if (srdi.getResource() != null) {
          nodeType = WebdavNsIntf.nodeTypeEntity;
          r = srdi.getResource();
          name = r.getName();
        } else {
          throw new WebdavException("Unexpected return type");
        }
      } else {
        nodeType = WebdavNsIntf.nodeTypeCollection;
        col = srdi.getCol();
        name = col.getName();
        canSync = srdi.getCanSync();
      }

      wri = new WdSynchReportItem(
              getNodeInt(Util.buildPath(false,
                                        srdi.getVpath(),
                                        "/", name),
                         WebdavNsIntf.existanceDoesExist,
                         nodeType,
                         false,
                         col, ev, r),
              srdi.getToken(),
              canSync);

      wsr.items.add(wri);
    }

    return wsr;
  }

  @Override
  public String getSyncToken(final String path) throws WebdavException{
    final String url = sysi.getUrlHandler().unprefix(fixPath(path));

    final CalDAVCollection<?> col = getSysi().getCollection(url);

    if (col == null) {
      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED,
                                "Bad If header - unknown resource");
    }

    return getSysi().getSyncToken(col);
  }

  /* ====================================================================
   *                  Access methods
   * ==================================================================== */

  @Override
  public Collection<WebdavNsNode> getGroups(final String resourceUri,
                                            final String principalUrl) {
    final Collection<WebdavNsNode> res = new ArrayList<>();

    final Collection<String> hrefs = getSysi().getGroups(resourceUri, principalUrl);
    for (String href: hrefs) {
      if (href.endsWith("/")) {
        href = href.substring(0, href.length());
      }

      res.add(new CaldavUserNode(new CaldavURI(getSysi().getPrincipal(href)),
                                 getSysi(), null));
    }

    return res;
  }

  @Override
  public Collection<String> getPrincipalCollectionSet(final String resourceUri) {
    final ArrayList<String> al = new ArrayList<>();

    for (final String s: getSysi().getPrincipalCollectionSet(resourceUri)) {
      al.add(sysi.getUrlHandler().prefix(s));
    }

    return al;
  }

  @Override
  public Collection<WebdavPrincipalNode> getPrincipals(final String resourceUri,
                                                final PrincipalPropertySearch pps) {
    final ArrayList<WebdavPrincipalNode> pnodes = new ArrayList<>();

    for (final CalPrincipalInfo cui: sysi.getPrincipals(resourceUri, pps)) {
      pnodes.add(new CaldavUserNode(new CaldavURI(cui.principal),
                                    getSysi(), cui));
    }

    return pnodes;
  }

  @Override
  public String makeUserHref(final String id) {
    return getSysi().makeHref(id, Ace.whoTypeUser);
  }

  @Override
  public void updateAccess(final AclInfo info) {
    final CaldavBwNode node =
            (CaldavBwNode)getNode(info.what,
                                  WebdavNsIntf.existanceMust,
                                  WebdavNsIntf.nodeTypeUnknown,
                                  false);
    updateAccess(info, node);
  }

  /**
   * @param info
   * @param node
   */
  public void updateAccess(final AclInfo info,
                           final CaldavBwNode node) {
    try {
      // May need a real principal hierarchy
      if (node instanceof CaldavCalNode) {
        // XXX to dref or not deref?
        sysi.updateAccess((CalDAVCollection<?>)node.getCollection(false), info.acl);
      } else if (node instanceof CaldavComponentNode) {
        sysi.updateAccess(((CaldavComponentNode)node).getEvent(),
                          info.acl);
      } else {
        throw new WebdavException(HttpServletResponse.SC_NOT_IMPLEMENTED);
      }
    } catch (final WebdavException wi) {
      throw wi;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public void emitAcl(final WebdavNsNode node) {
    Acl acl = null;

    try {
      if (node.isCollection()) {
        acl = node.getCurrentAccess().getAcl();
      } else if (node instanceof CaldavComponentNode) {
        acl = node.getCurrentAccess().getAcl();
      }

      if (acl != null) {
        accessUtil.emitAcl(acl, true);
      }
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<String> getAclPrincipalInfo(final WebdavNsNode node) {
    try {
      final TreeSet<String> hrefs = new TreeSet<>();

      for (final Ace ace: node.getCurrentAccess().getAcl().getAces()) {
        final AceWho who = ace.getWho();

        if (who.getWhoType() == WhoDefs.whoTypeUser) {
          hrefs.add(accessUtil.makeUserHref(who.getWho()));
        } else if (who.getWhoType() == WhoDefs.whoTypeGroup) {
          hrefs.add(accessUtil.makeGroupHref(who.getWho()));
        }
      }

      return hrefs;
    } catch (final AccessException ae) {
      if (debug()) {
        error(ae);
      }
      throw new WebdavServerError();
    }
  }

  /* ==============================================================
   *                Property value methods
   * ============================================================== */

  @Override
  public WebdavProperty makeProp(final Element propnode) {
    if (!XmlUtil.nodeMatches(propnode, CaldavTags.calendarData)) {
      return super.makeProp(propnode);
    }

    /* Handle the calendar-data element */

    final CalData caldata =
            new CalData(new QName(propnode.getNamespaceURI(),
                                  propnode.getLocalName()));
    caldata.parse(propnode);

    return caldata;
  }

  /** Properties we can process */
  private static final QName[] knownProperties = {
    CaldavTags.calendarData,
    CaldavTags.calendarTimezone,
    //  CaldavTags.maxAttendeesPerInstance,
    //  CaldavTags.maxDateTime,
    //  CaldavTags.maxInstances,
    CaldavTags.maxResourceSize,
    //  CaldavTags.minDateTime,
  };

  @Override
  public boolean knownProperty(final WebdavNsNode node,
                               final WebdavProperty pr) {
    final QName tag = pr.getTag();

    if (node.knownProperty(tag)) {
      return true;
    }

    for (final QName knownProperty : knownProperties) {
      if (tag.equals(knownProperty)) {
        return true;
      }
    }

    /* Try the node for a value */

    return super.knownProperty(node, pr);
  }

  @Override
  public boolean generatePropValue(final WebdavNsNode node,
                                   WebdavProperty pr,
                                   final boolean allProp) {
    final QName tag = pr.getTag();
    final String ns = tag.getNamespaceURI();

    try {
      /* Deal with anything but webdav properties */
      if (ns.equals(WebdavTags.namespace)) {
        // Not ours
        return super.generatePropValue(node, pr, allProp);
      }

      if (tag.equals(CaldavTags.calendarData)) {
        // pr may be a CalendarData object - if not it's probably allprops
        if (!(pr instanceof CalData)) {
          pr = new CalData(tag);
        }

        final CalData caldata = (CalData)pr;

        if (debug()) {
          debug("do CalendarData for " + node.getUri());
        }

        final String contentType = caldata.getCalendarData().getContentType();
        String[] contentTypePars = null;

        if (contentType != null) {
          contentTypePars = contentType.split(";");
        }

        String ctype = null;
        if (contentTypePars !=null) {
          ctype = contentTypePars[0];
        }

        final int status;
        try {
          /* Output the (transformed) node.
           */

          if (ctype != null) {
            xml.openTagNoNewline(CaldavTags.calendarData,
                                 "content-type", ctype);
          } else {
            xml.openTagNoNewline(CaldavTags.calendarData);
          }

          caldata.process(node, xml, ctype);

          return true;
        } catch (final WebdavException wde) {
          status = wde.getStatusCode();
          if (debug() && (status != HttpServletResponse.SC_NOT_FOUND)) {
            error(wde);
          }
          return false;
        } finally {
          xml.closeTagNoblanks(CaldavTags.calendarData);
        }
      }

      if (tag.equals(CaldavTags.maxAttendeesPerInstance)) {
        return false;
      }

      if (tag.equals(CaldavTags.maxDateTime)) {
        return false;
      }

      if (tag.equals(CaldavTags.maxInstances)) {
        return false;
      }

      if (tag.equals(CaldavTags.maxResourceSize)) {
        /* e.g.
         * <C:max-resource-size
         *    xmlns:C="urn:ietf:params:xml:ns:caldav">102400</C:max-resource-size>
         */
        xml.property(tag, String.valueOf(sysi.getAuthProperties().getMaxUserEntitySize()));
        return true;
      }

      if (tag.equals(CaldavTags.minDateTime)) {
        return false;
      }

      return node.generatePropertyValue(tag, this, allProp);
    } catch (final WebdavException wie) {
      throw wie;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                         Caldav methods
   * ==================================================================== */

  /** Use the given query to return a collection of nodes. An exception will
   * be raised if the entire query fails for some reason (access, etc). An
   * empty collection will be returned if no objects match.
   *
   * @param wdnode    WebdavNsNode defining root of search
   * @param retrieveList   If non-null limit required fields.
   * @param retrieveRecur  How we retrieve recurring events
   * @param fltr      Filter object defining search
   * @return Collection of result nodes (empty for no result)
   */
  public Collection<WebdavNsNode> query(final WebdavNsNode wdnode,
                                        final List<String> retrieveList,
                                        final RetrievalMode retrieveRecur,
                                        final FilterType fltr) {
    final CaldavBwNode node = (CaldavBwNode)wdnode;

    final FilterHandler fh = new FilterHandler(fltr);
    final Collection<CalDAVEvent<?>> events =
            fh.query(node,
                     retrieveList, retrieveRecur);

    /* We now need to build a node for each of the events in the collection.
       For each event we first determine what calendar it's in. We then take the
       incoming uri, strip any calendar names off it and append the calendar
       name and event name to create the new uri.

       If there is no calendar name for the event we just give it the default.
     */

    final Collection<WebdavNsNode> evnodes = new ArrayList<>();

    if (events == null) {
      return evnodes;
    }

    try {
      for (final var ev: events) {
        final var col =
                getSysi().getCollection(ev.getParentPath());
        if (col == null) {
          throw new RuntimeException("No parent collection");
        }
        final var uri = col.getPath();

        /* If no name was assigned use the guid */
        String evName = ev.getName();
        if (evName == null) {
          evName = ev.getUid() + ".ics";
        }

        final var evuri = Util.buildPath(false, uri, "/", evName);

        final var evnode = (CaldavComponentNode)getNodeInt(
                evuri,
                WebdavNsIntf.existanceDoesExist,
                WebdavNsIntf.nodeTypeEntity,
                false,
                col, ev, null);

        evnodes.add(evnode);
      }

      return fh.postFilter(evnodes);
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      error(t);
      throw new WebdavServerError();
    }
  }

  /** The node represents a calendar resource for which we must get free-busy
   * information.
   *
   * @param cnode  CaldavCalNode
   * @param freeBusy query
   * @param depth DAV depth
   */
  public void getFreeBusy(final CaldavCalNode cnode,
                          final FreeBusyQuery freeBusy,
                          final int depth) {
    try {
      /* We need to deref as the freebusy comes from the target */
      final CalDAVCollection<?> c =
              (CalDAVCollection<?>)cnode.getCollection(true);

      if (c == null) {
        // XXX - exception?
        return;
      }

      cnode.setFreeBusy(freeBusy.getFreeBusy(sysi, c,
                                             depth));
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

  private SysIntf getSysi(final String className) {
    try {
      final ClassLoader loader = Thread.currentThread().getContextClassLoader();
      final Class<?> cl = loader.loadClass(className);

      if (cl == null) {
        throw new RuntimeException("Class " + className + " not found");
      }

      final Object o = cl.getDeclaredConstructor().newInstance();

      if (!(o instanceof SysIntf)) {
        throw new RuntimeException("Class " + className +
                                  " is not a subclass of " +
                                  SysIntf.class.getName());
      }

      return (SysIntf)o;
    } catch (final RuntimeException re) {
      throw re;
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private WebdavNsNode getNodeInt(final String uri,
                                  final int existance,
                                  final int nodeType,
                                  final boolean addMember,
                                  final CalDAVCollection<?> col,
                                  final CalDAVEvent<?> ev,
                                  final CalDAVResource<?> r) {
    if (debug()) {
      debug("About to get node for " + uri);
    }

    if (uri == null)  {
      return null;
    }

    try {
      final GetEntityResponse<CaldavURI> ger =
              findURI(uri, existance, nodeType,
                      col, ev, r);

      /* For the moment just throw all the exceptions here
       */
      if (ger.getException() != null) {
        throw ger.getException();
      }

      if (ger.getStatus() == notFound) {
        return null;
      }

      if (!ger.isOk()) {
        final int scode;
        if (ger.getStatus() == forbidden) {
          scode = HttpServletResponse.SC_FORBIDDEN;
        } else {
          scode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        throw new WebdavException(scode, ger.getErrorTag(),
                                  ger.getMessage());
      }

      final var wi = ger.getEntity();
      if (wi == null) {
        return null;
      }

      WebdavNsNode nd = null;
      final AccessPrincipal ap = wi.getPrincipal();

      if (ap != null) {
        if (ap.getKind() == Ace.whoTypeUser) {
          nd = new CaldavUserNode(wi, sysi, sysi.getCalPrincipalInfo(ap));
        } else if (ap.getKind() == Ace.whoTypeGroup) {
          nd = new CaldavGroupNode(wi, sysi, sysi.getCalPrincipalInfo(ap));
        }
      } else if (wi.isCollection()) {
        nd = new CaldavCalNode(wi, sysi);
      } else if (wi.isResource()) {
        nd = new CaldavResourceNode(wi, sysi);
      } else {
        nd = new CaldavComponentNode(wi, sysi);
      }

      return nd;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Find the named item by following down the path from the root.
   * This requires the names at each level to be unique (and present)
   * <p>
   * I don't think the name.now has to have an ics suffix. Draft 7 goes as
   * far as saying it may have ".ics" or ".ifb"
   * <p>
   * For the moment enforce one or the other
   *
   * <p>Uri is at least /user/user-id or <br/>
   *    /public
   * <br/>followed by one or more calendar path elements possibly followed by an
   * entity name.
   *
   * @param uri        String uri - just the path part
   * @param existance        Say's something about the state of existance
   * @param nodeType         Say's something about the type of node
   * @param collection        Supplied CalDAVCollection object if we already have it.
   * @param ev               an entity
   * @param rsrc             a resource
   * @return Response with status or CaldavURI object representing the uri
   */
  private GetEntityResponse<CaldavURI> findURI(String uri,
                                               final int existance,
                                               final int nodeType,
                                               final CalDAVCollection<?> collection,
                                               CalDAVEvent<?> ev,
                                               CalDAVResource<?> rsrc) {
    final var resp = new GetEntityResponse<CaldavURI>();

    try {
      if ((nodeType == WebdavNsIntf.nodeTypeUnknown) &&
          (existance != WebdavNsIntf.existanceMust)) {
        // We assume an unknown type must exist
        throw new RuntimeException("Incompatible parameters");
      }

      uri = normalizeUri(uri);

      if (!uri.startsWith("/")) {
        return resp.invalid("Invalid URI - must start with \"/\"");
      }

      final boolean isPrincipal = sysi.isPrincipal(uri);

      if ((nodeType == WebdavNsIntf.nodeTypePrincipal) && !isPrincipal) {
        return resp.notFound(uri);
      }

      if (isPrincipal) {
        final AccessPrincipal p = getSysi().getPrincipal(uri);

        if (p == null) {
          return resp.notFound(uri);
        }

        resp.setEntity(new CaldavURI(p));
        return resp;
      }

      if (existance == WebdavNsIntf.existanceDoesExist) {
        // Provided with calendar and entity if needed.
        if (ev != null) {
          resp.setEntity(new CaldavURI(collection, ev,
                                       ev.getName(), true, false));
        } else if (rsrc != null) {
          resp.setEntity(new CaldavURI(collection, rsrc, true));
        } else {
          resp.setEntity(new CaldavURI(collection,
                                       null, null, true, false));
        }
        //putUriPath(curi);

        return resp;
      }

      if (debug()) {
        debug("search for collection uri \"" + uri + "\"");
      }
      CalDAVCollection<?> col = sysi.getCollection(uri);

      if ((nodeType == WebdavNsIntf.nodeTypeCollection) ||
          (nodeType == WebdavNsIntf.nodeTypeUnknown)) {
        // For unknown we try the full path first as a calendar.

        if (col == null) {
          if ((nodeType == WebdavNsIntf.nodeTypeCollection) &&
              (existance != WebdavNsIntf.existanceNot) &&
              (existance != WebdavNsIntf.existanceMay)) {
            /* We asked for a collection and it doesn't exist */
            return resp.notFound(uri);
          }

          // We'll try as an entity for unknown
        } else {
          if (existance == WebdavNsIntf.existanceNot) {
            if (debug()) {
              debug("collection already exists - col=\"" +
                            col.getPath() + "\"");
            }

            return resp.notOk(forbidden,
                              WebdavTags.resourceMustBeNull);
          }

          if (debug()) {
            debug("create collection uri - cal=\"" + col.getPath() + "\"");
          }

          resp.setEntity(new CaldavURI(col, true));

          return resp;
        }
      } else if (col != null) {
        return resp.notOk(forbidden,
                          WebdavTags.resourceMustBeNull);
      }

      // Entity or unknown

      /* Split name into parent path and entity name part */
      final SplitResult split = splitUri(uri);
      if (!split.isOk()) {
        return resp.fromResponse(split);
      }

      if (split.name == null) {
        // No name part
        return resp.notFound(uri);
      }

      final String parentPath = split.path;
      final String entityName = split.name;

      /* Look for the parent */
      col = sysi.getCollection(parentPath);

      if (col == null) {
        if (nodeType == WebdavNsIntf.nodeTypeCollection) {
          // Trying to create calendar/collection with no parent
          return resp.notOk(forbidden, "No parent");
        }

        return resp.notFound(uri);
      }

      if (nodeType == WebdavNsIntf.nodeTypeCollection) {
        // Trying to create calendar/collection
        final CalDAVCollection<?> newCol = getSysi().newCollectionObject(false,
                                                                col.getPath());
        newCol.setName(entityName);
        newCol.setPath(Util.buildPath(false, col.getPath(), "/", newCol.getName()));

        resp.setEntity(new CaldavURI(newCol, false));

        return resp;
      }

      final int ctype = col.getCalType();
      if ((ctype == CalDAVCollection.calTypeCalendarCollection) ||
          (ctype == CalDAVCollection.calTypeInbox) ||
          (ctype == CalDAVCollection.calTypeOutbox)) {
        if (debug()) {
          debug("find event(s) - cal=\"" + col.getPath() + "\" name=\"" +
                        entityName + "\"");
        }

        ev = sysi.getEvent(col, entityName);

        if ((existance == existanceMust) && (ev == null)) {
          return resp.notFound(uri);
        }

        resp.setEntity(new CaldavURI(col, ev, entityName, ev != null,
                                     false));
      } else {
        if (debug()) {
          debug("find resource - cal=\"" + col.getPath() + "\" name=\"" +
                        entityName + "\"");
        }

        /* Look for a resource */
        rsrc = sysi.getFile(col, entityName);

        if ((existance == existanceMust) && (rsrc == null)) {
          return resp.notFound(uri);
        }

        final boolean exists = rsrc != null;

        if (!exists) {
          rsrc = getSysi().newResourceObject(col.getPath());
          rsrc.setName(entityName);
        }

        resp.setEntity(new CaldavURI(col, rsrc, exists));
      }

      //putUriPath(curi);

      return resp;
    } catch (final Throwable t) {
      return resp.error(t);
    }
  }

  private static class SplitResult
          extends Response<SplitResult> {
    final String path;
    final String name;

    SplitResult(final String path, final String name) {
      this.path = path;
      this.name = name;
    }
  }

  /* Split the uri so that result.path is the path up to the name part result.name
   *
   * NormalizeUri was called previously so we have no trailing "/"
   */
  private SplitResult splitUri(final String uri) {
    final int pos = uri.lastIndexOf("/");
    if (pos < 0) {
      // bad uri
      return new SplitResult(null, null).invalid("Invalid uri: " + uri);
    }

    if (pos == 0) {
      return new SplitResult(uri, null);
    }

    return new SplitResult(uri.substring(0, pos), uri.substring(pos + 1));
  }

  /*
  private CaldavURI getUriPath(String path) {
    return uriMap.get(path);
  }

  private void putUriPath(CaldavURI wi) {
    uriMap.put(wi.getPath(), wi);
  }
  */
}
