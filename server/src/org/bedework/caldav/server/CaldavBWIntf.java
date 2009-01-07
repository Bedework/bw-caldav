/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.caldav.server;

import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.SysIntf.CalUserInfo;
import org.bedework.caldav.server.calquery.CalendarData;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.filter.Filter;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventObj;
import org.bedework.calfacade.BwOrganizer;
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.BwResourceContent;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.RecurringRetrievalMode.Rmode;
import org.bedework.calfacade.ScheduleResult.ScheduleRecipientResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.base.TimeRange;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.env.CalOptionsFactory;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.util.CalFacadeUtil;
import org.bedework.calfacade.util.BwDateTimeUtil;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.webdav.servlet.common.AccessUtil;
import edu.rpi.cct.webdav.servlet.common.Headers;
import edu.rpi.cct.webdav.servlet.common.WebdavServlet;
import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.common.MethodBase.MethodInfo;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavPrincipalNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavServerError;
import edu.rpi.cct.webdav.servlet.shared.WebdavUnauthorized;
import edu.rpi.cct.webdav.servlet.shared.WebdavUnsupportedMediaType;
import edu.rpi.cmt.access.AccessException;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.AceWho;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.PrincipalInfo;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.WhoDefs;
import edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb;
import edu.rpi.sss.util.OptionsI;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavDefs;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VFreeBusy;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

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
 *   @author Mike Douglass   douglm @ rpi.edu
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

  private CalDAVConfig config;

  /** We store CaldavURI objects here
   * /
  private HashMap<String, CaldavURI> uriMap = new HashMap<String, CaldavURI>();
  */

  /* ====================================================================
   *                     Interface methods
   * ==================================================================== */

  /** Called before any other method is called to allow initialisation to
   * take place at the first or subsequent requests
   *
   * @param servlet
   * @param req
   * @param props
   * @param debug
   * @param methods    HashMap   table of method info
   * @param dumpContent
   * @throws WebdavException
   */
  public void init(WebdavServlet servlet,
                   HttpServletRequest req,
                   Properties props,
                   boolean debug,
                   HashMap<String, MethodInfo> methods,
                   boolean dumpContent) throws WebdavException {
    super.init(servlet, req, props, debug, methods, dumpContent);

    try {
      HttpSession session = req.getSession();
      ServletContext sc = session.getServletContext();

      String appName = sc.getInitParameter("bwappname");

      if ((appName == null) || (appName.length() == 0)) {
        appName = "unknown-app-name";
      }

      namespacePrefix = WebdavUtils.getUrlPrefix(req);
      namespace = namespacePrefix + "/schema";

      OptionsI opts = CalOptionsFactory.getOptions(debug);
      config = (CalDAVConfig)opts.getAppProperty(appName);
      if (config == null) {
        config = new CalDAVConfig();
      }

      sysi = (SysIntf)CalFacadeUtil.getObject(config.getSysintfImpl(),
                                              SysIntf.class);

      sysi.init(req, account, config, debug);

      accessUtil = new AccessUtil(namespacePrefix, xml,
                                  new CalDavAccessXmlCb(sysi), debug);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** See if we can reauthenticate. Use for real-time service which needs to
   * authenticate as a particular principal.
   *
   * @param req
   * @param account
   * @throws WebdavException
   */
  public void reAuth(HttpServletRequest req,
                     String account) throws WebdavException {
    try {
      if (sysi != null) {
        try {
          sysi.close();
        } catch (Throwable t) {
          throw new WebdavException(t);
        }
      }

      this.account = account;

      sysi = (SysIntf)CalFacadeUtil.getObject(config.getSysintfImpl(),
                                              SysIntf.class);

      sysi.init(req, account, config, debug);

      accessUtil = new AccessUtil(namespacePrefix, xml,
                                  new CalDavAccessXmlCb(sysi), debug);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getDavHeader(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public String getDavHeader(WebdavNsNode node) throws WebdavException {
    return super.getDavHeader(node) + ", calendar-access, calendar-schedule, calendar-auto-schedule";
  }

  protected CalDAVConfig getConfig() {
    return config;
  }

  /**
   */
  private static class CalDavAccessXmlCb implements AccessXmlCb, Serializable {
    private SysIntf sysi;

    private QName errorTag;

    CalDavAccessXmlCb(SysIntf sysi) {
      this.sysi = sysi;
    }

    /* (non-Javadoc)
     * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#makeHref(java.lang.String, int)
     */
    public String makeHref(String id, int whoType) throws AccessException {
      try {
        return sysi.makeHref(id, whoType);
      } catch (Throwable t) {
        throw new AccessException(t);
      }
    }

    /* (non-Javadoc)
     * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#getAccount()
     */
    public String getAccount() throws AccessException {
      try {
        return sysi.getAccount();
      } catch (Throwable t) {
        throw new AccessException(t);
      }
    }

    /* (non-Javadoc)
     * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#getPrincipalInfo(java.lang.String)
     */
    public PrincipalInfo getPrincipalInfo(String href) throws AccessException {
      try {
        return sysi.getPrincipalInfo(href);
      } catch (Throwable t) {
        throw new AccessException(t);
      }
    }

    /* (non-Javadoc)
     * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#setErrorTag(edu.rpi.sss.util.xml.QName)
     */
    public void setErrorTag(QName tag) throws AccessException {
      errorTag = tag;
    }

    /* (non-Javadoc)
     * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#getErrorTag()
     */
    public QName getErrorTag() throws AccessException {
      return errorTag;
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getAccessUtil()
   */
  public AccessUtil getAccessUtil() throws WebdavException {
    return accessUtil;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#canPut(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public boolean canPut(WebdavNsNode node) throws WebdavException {
    BwShareableDbentity ent;
    int access = PrivilegeDefs.privWriteContent;

    if (node instanceof CaldavComponentNode) {
      CaldavComponentNode comp = (CaldavComponentNode)node;

      if (comp.getEventInfo() != null) {
        ent = comp.getEventInfo().getEvent();
      } else {
        ent = comp.getCalendar();
        access = PrivilegeDefs.privBind;
      }
    } else {
      return false;
    }

    return sysi.checkAccess(ent, access, true).accessAllowed;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getDirectoryBrowsingDisallowed()
   */
  public boolean getDirectoryBrowsingDisallowed() throws WebdavException {
    return sysi.getDirectoryBrowsingDisallowed();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#close()
   */
  public void close() throws WebdavException {
    sysi.close();
  }

  /**
   * @return SysIntf
   */
  public SysIntf getSysi() {
    return sysi;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getSupportedLocks()
   */
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

  public boolean getAccessControl() {
    return true;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#addNamespace(edu.rpi.sss.util.xml.XmlEmit)
   */
  public void addNamespace(XmlEmit xml) throws WebdavException {
    super.addNamespace(xml);

    try {
      xml.addNs(CaldavDefs.caldavNamespace);
      xml.addNs(CaldavDefs.icalNamespace);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getNode(java.lang.String, int, int)
   */
  public WebdavNsNode getNode(String uri,
                              int existance,
                              int nodeType) throws WebdavException {
    return getNodeInt(uri, existance, nodeType, null, null, null);
  }

  private WebdavNsNode getNodeInt(String uri,
                                  int existance,
                                  int nodeType,
                                  BwCalendar cal,
                                  EventInfo ei,
                                  BwResource r) throws WebdavException {
    if (debug) {
      debugMsg("About to get node for " + uri);
    }

    if (uri == null)  {
      return null;
    }

    try {
      CaldavURI wi = findURI(uri, existance, nodeType, cal, ei, r);

      if (wi == null) {
        throw new WebdavNotFound(uri);
      }

      WebdavNsNode nd = null;

      if (wi.isUser()) {
        nd = new CaldavUserNode(wi, sysi, null, debug);
      } else if (wi.isGroup()) {
        nd = new CaldavGroupNode(wi, sysi, null, debug);
      } else if (wi.isCollection()) {
        nd = new CaldavCalNode(wi, sysi, debug);
      } else if (wi.isResource()) {
        nd = new CaldavResourceNode(wi, sysi, debug);
      } else {
        nd = new CaldavComponentNode(wi, sysi, debug);
      }

      return nd;
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void putNode(WebdavNsNode node)
      throws WebdavException {
  }

  public void delete(WebdavNsNode node) throws WebdavException {
    try {
      CaldavBwNode bwnode = getBwnode(node);

      if (bwnode instanceof CaldavResourceNode) {
        CaldavResourceNode rnode = (CaldavResourceNode)bwnode;

        BwResource r = rnode.getResource();

        sysi.deleteFile(r);
      } else if (bwnode instanceof CaldavComponentNode) {
        CaldavComponentNode cnode = (CaldavComponentNode)bwnode;

        BwEvent ev = cnode.getEventInfo().getEvent();

        if (ev != null) {
          if (debug) {
            trace("About to delete event " + ev);
          }
          sysi.deleteEvent(ev, CalDavHeaders.scheduleReply(getRequest()));
        } else {
          if (debug) {
            trace("No event object available");
          }
        }
      } else {
        if (!(bwnode instanceof CaldavCalNode)) {
          throw new WebdavUnauthorized();
        }

        CaldavCalNode cnode = (CaldavCalNode)bwnode;

        BwCalendar cal = cnode.getCalendar();

        sysi.deleteCalendar(cal);
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getChildren(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public Collection<WebdavNsNode> getChildren(WebdavNsNode node) throws WebdavException {
    try {
      CaldavBwNode wdnode = getBwnode(node);

      ArrayList<WebdavNsNode> al = new ArrayList<WebdavNsNode>();

      if (!wdnode.isCollection()) {
        // Don't think we should have been called
        return al;
      }

      if (debug) {
        debugMsg("About to get children for " + wdnode.getUri());
      }

      Collection children = wdnode.getChildren();
      String uri = wdnode.getUri();
      BwCalendar parent = wdnode.getCalendar();

      Iterator it = children.iterator();

      while (it.hasNext()) {
        Object o = it.next();
        BwCalendar cal = null;
        BwResource r = null;
        EventInfo ei = null;
        String name;
        int nodeType;

        if (o instanceof BwCalendar) {
          cal = (BwCalendar)o;
          name = cal.getName();
          nodeType = WebdavNsIntf.nodeTypeCollection;
          if (debug) {
            debugMsg("Found child " + cal);
          }
        } else if (o instanceof BwResource) {
          r = (BwResource)o;
          name = r.getName();
          nodeType = WebdavNsIntf.nodeTypeEntity;
        } else if (o instanceof EventInfo) {
          cal = parent;
          ei = (EventInfo)o;
          name = ei.getEvent().getName();
          nodeType = WebdavNsIntf.nodeTypeEntity;
        } else {
          throw new WebdavException("Unexpected return type");
        }

        al.add(getNodeInt(uri + "/" + name,
                          WebdavNsIntf.existanceDoesExist,
                          nodeType, cal, ei, r));
      }

      return al;
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public WebdavNsNode getParent(WebdavNsNode node)
      throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getContent(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public Reader getContent(WebdavNsNode node) throws WebdavException {
    try {
      if (!node.getAllowsGet()) {
        return null;
      }

      CaldavBwNode bwnode = getBwnode(node);

      return bwnode.getContent();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getBinaryContent(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public InputStream getBinaryContent(WebdavNsNode node) throws WebdavException {
    try {
      if (!node.getAllowsGet()) {
        return null;
      }

      if (!(node instanceof CaldavResourceNode)) {
        throw new WebdavException("Unexpected node type");
      }

      CaldavResourceNode bwnode = (CaldavResourceNode)getBwnode(node);
      BwResource r = bwnode.getResource();

      if (r.getContent() == null) {
        sysi.getFileContent(r);
      }

      return bwnode.getContentStream();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private class MyReader extends Reader {
    private LineNumberReader lnr;

    private char[] curChars;
    private int len;
    private int pos;
    private boolean doneCr;
    private boolean doneLf = true;

    private char nextChar;

    MyReader(Reader rdr) {
      super();
      lnr = new LineNumberReader(rdr);
    }

    public int read() throws IOException {
      if (!getNextChar()) {
        return -1;
      }

      return nextChar;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
      int ct = 0;
      while (ct < len) {
        if (!getNextChar()) {
          return ct;
        }

        cbuf[off + ct] = nextChar;
        ct++;
      }

      return ct;
    }

    private boolean getNextChar() throws IOException {
      if (doneLf) {
        // Get new line
        String ln = lnr.readLine();

        if (ln == null) {
          return false;
        }

        if (debug) {
          trace(ln);
        }

        pos = 0;
        len = ln.length();
        curChars = ln.toCharArray();
        doneLf = false;
        doneCr = false;
      }

      if (pos == len) {
        if (!doneCr) {
          doneCr = true;
          nextChar = '\r';
          return true;
        }

        doneLf = true;
        nextChar = '\n';
        return true;
      }

      nextChar = curChars[pos];
      pos ++;
      return true;
    }

    public void close() {
    }
  }

  /**
   * @param cal
   * @param rdr
   * @return Icalendar
   * @throws WebdavException
   */
  public Icalendar getIcal(BwCalendar cal, Reader rdr)
      throws WebdavException {
    return sysi.fromIcal(cal, new MyReader(rdr));
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#putContent(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, java.lang.String, java.io.Reader, boolean, java.lang.String)
   */
  public PutContentResult putContent(WebdavNsNode node,
                                     String[] contentTypePars,
                                     Reader contentRdr,
                                     boolean create,
                                     String ifEtag) throws WebdavException {
    try {
      PutContentResult pcr = new PutContentResult();
      pcr.node = node;

      if (node instanceof CaldavResourceNode) {
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      CaldavComponentNode bwnode = (CaldavComponentNode)getBwnode(node);
      BwCalendar cal = bwnode.getCalendar();

      boolean calContent = false;
      if ((contentTypePars != null) && contentTypePars.length > 0) {
        calContent = contentTypePars[0].equals("text/calendar");
      }

      if (!cal.getCalendarCollection() || !calContent) {
        throw new WebdavForbidden(CaldavTags.supportedCalendarData);
      }

      Icalendar ic = sysi.fromIcal(cal, new MyReader(contentRdr));

      /** We can only put a single resource - that resource will be an ics file
       * containing freebusy information or an event or todo and possible overrides.
       */

      Iterator it = ic.iterator();
      boolean fail = false;

      while (it.hasNext()) {
        Object o = it.next();

        if (o instanceof EventInfo) {
          pcr.created = putEvent(bwnode, (EventInfo)o, create, ifEtag);
        } else {
          fail = true;
          break;
        }
      }

      if (fail) {
        warn("More than one calendar object for PUT or not event");
        throw new WebdavBadRequest("More than one calendar object for PUT or not event");
      }


      return pcr;
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#putBinaryContent(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, java.lang.String, java.io.InputStream, boolean, java.lang.String)
   */
  public PutContentResult putBinaryContent(WebdavNsNode node,
                                           String[] contentTypePars,
                                           InputStream contentStream,
                                           boolean create,
                                           String ifEtag) throws WebdavException {
    try {
      PutContentResult pcr = new PutContentResult();
      pcr.node = node;

      if (!(node instanceof CaldavResourceNode)) {
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      CaldavResourceNode bwnode = (CaldavResourceNode)getBwnode(node);
      BwCalendar cal = bwnode.getCalendar();

      if (cal.getCalendarCollection()) {
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      BwResource r = bwnode.getResource();

      if (r.unsaved()) {
        create = true;
      }

      String contentType = null;

      if ((contentTypePars != null) && contentTypePars.length > 0) {
        for (String c: contentTypePars) {
          if (contentType != null) {
            contentType += ";";
          }
          contentType += c;
        }
      }

      r.setContentType(contentType);

      // XXX Fix this
      int bufflen = 5000;
      byte[] buff = new byte[bufflen];
      byte[] res = null;

      for (;;) {
        int len = contentStream.read(buff, 0, bufflen);
        if (len < 0) {
          break;
        }

        if (res == null) {
          res = buff;
          buff = new byte[bufflen];
        } else {
          byte[] newres = new byte[res.length + len];
          for (int i = 0; i < res.length; i++) {
            newres[i] = res[i];
          }

          for (int i = 0; i < len; i++) {
            newres[res.length + i] = buff[i];
          }

          res = newres;
        }
      }

      BwResourceContent rc = r.getContent();

      if (rc == null) {
        if (!r.unsaved()) {
          sysi.getFileContent(r);
          rc = r.getContent();
        }

        if (rc == null) {
          rc = new BwResourceContent();
          r.setContent(rc);
        }
      }

      rc.setValue(res);
      r.setContentLength(res.length);

      if (create) {
        sysi.putFile(cal, r);
      } else {
        sysi.updateFile(r, true);
      }
      return pcr;
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private boolean putEvent(CaldavComponentNode bwnode,
                           EventInfo evinfo,
                           boolean create,
                           String ifEtag) throws WebdavException {
    BwEvent ev = evinfo.getEvent();
    String entityName = bwnode.getEntityName();
    BwCalendar cal = bwnode.getCalendar();
    boolean created = false;

    if (debug) {
      debugMsg("putContent: intf has event with name " + entityName +
               " and summary " + ev.getSummary() +
               " new event = " + evinfo.getNewEvent());
    }

    if (evinfo.getNewEvent()) {
      created = true;
      ev.setName(entityName);

      boolean noInvites = false; // based on header?

      /* Collection<BwEventProxy>failedOverrides = */
        sysi.addEvent(cal, evinfo, noInvites, true);

      /*StringBuffer sb = new StringBuffer(cdUri.getPath());
      sb.append("/");
      sb.append(entityName);
      if (!entityName.toLowerCase().endsWith(".ics")) {
        sb.append(".ics");
      }*/

      bwnode.setEventInfo(evinfo);
    } else if (create) {
      /* Resource already exists */

      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
    } else {
      if (!entityName.equals(ev.getName())) {
        throw new WebdavBadRequest("Mismatched names");
      }

      if ((ifEtag != null) && (!ifEtag.equals(bwnode.getPrevEtagValue(true)))) {
        if (debug) {
          debugMsg("putContent: etag mismatch if=" + ifEtag +
                   "prev=" + bwnode.getPrevEtagValue(true));
        }
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      if (debug) {
        debugMsg("putContent: update event " + ev);
      }
      sysi.updateEvent(ev, evinfo.getOverrideProxies(), evinfo.getChangeset());
    }

    return created;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#create(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public void create(WebdavNsNode node) throws WebdavException {
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#createAlias(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public void createAlias(WebdavNsNode alias) throws WebdavException {
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#acceptMkcolContent(javax.servlet.http.HttpServletRequest)
   */
  public void acceptMkcolContent(HttpServletRequest req) throws WebdavException {
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
   * @throws WebdavException
   */
  public void makeCollection(HttpServletRequest req,
                             HttpServletResponse resp,
                             WebdavNsNode node) throws WebdavException {
    try {
      CaldavCalNode bwnode = (CaldavCalNode)getBwnode(node);

      /* The uri should have an entity name representing the new collection
       * and a calendar object representing the parent.
       *
       * A namepart of null means that the path already exists
       */

      BwCalendar newCal = bwnode.getCalendar();
      BwCalendar parent = newCal.getCalendar();
      if (parent.getCalendarCollection()) {
        throw new WebdavForbidden(CaldavTags.calendarCollectionLocationOk);
      }

      if (newCal.getName() == null) {
        throw new WebdavForbidden("Forbidden: Null name");
      }

      resp.setStatus(sysi.makeCollection(newCal,
                                         "MKCALENDAR".equalsIgnoreCase(req.getMethod()),
                                         parent.getPath()));
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void copyMove(HttpServletRequest req,
                       HttpServletResponse resp,
                       WebdavNsNode from,
                       WebdavNsNode to,
                       boolean copy,
                       boolean overwrite,
                       int depth) throws WebdavException {
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

  private void copyMoveCollection(HttpServletResponse resp,
                                  CaldavCalNode from,
                                  WebdavNsNode to,
                                  boolean copy,
                                  boolean overwrite,
                                  int depth) throws WebdavException {
    if (!(to instanceof CaldavCalNode)) {
      throw new WebdavBadRequest();
    }

    // Copy folder
    if ((depth != Headers.depthNone) && (depth != Headers.depthInfinity)) {
      throw new WebdavBadRequest();
    }

    CaldavCalNode fromCalNode = (CaldavCalNode)from;
    CaldavCalNode toCalNode = (CaldavCalNode)to;

    if (toCalNode.getExists() && !overwrite) {
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

      return;
    }

    BwCalendar fromCal = fromCalNode.getCalendar();
    BwCalendar toCal = toCalNode.getCalendar();

    getSysi().copyMove(fromCal, toCal, copy, overwrite);
    if (toCalNode.getExists()) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setStatus(HttpServletResponse.SC_CREATED);
      Headers.makeLocation(resp, getLocation(to), debug);
    }
  }

  private void copyMoveComponent(HttpServletResponse resp,
                                 CaldavComponentNode from,
                                 WebdavNsNode to,
                                 boolean copy,
                                 boolean overwrite) throws WebdavException {
    if (!(to instanceof CaldavComponentNode)) {
      throw new WebdavBadRequest();
    }

    CaldavComponentNode toNode = (CaldavComponentNode)to;

    if (toNode.getExists() && !overwrite) {
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

      return;
    }

    EventInfo fromEi = from.getEventInfo();
    BwCalendar toCal = toNode.getCalendar();

    if (!getSysi().copyMove(fromEi.getEvent(), fromEi.getOverrideProxies(),
                            toCal, toNode.getEntityName(), copy,
                            overwrite)) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setStatus(HttpServletResponse.SC_CREATED);
      Headers.makeLocation(resp, getLocation(to), debug);
    }
  }

  private void copyMoveResource(HttpServletResponse resp,
                               CaldavResourceNode from,
                               WebdavNsNode to,
                               boolean copy,
                               boolean overwrite) throws WebdavException {
    if (!(to instanceof CaldavResourceNode)) {
      throw new WebdavBadRequest();
    }

    CaldavResourceNode toNode = (CaldavResourceNode)to;

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
      Headers.makeLocation(resp, getLocation(to), debug);
    }
  }
  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#specialUri(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String)
   */
  public boolean specialUri(HttpServletRequest req,
                            HttpServletResponse resp,
                            String resourceUri) throws WebdavException {
    RequestPars pars = new RequestPars(req, this, resourceUri);

    if (pars.freeBusy) {
      doSpecialFreeBusy(req, resp, pars);
      return true;
    }

    if (pars.webcal) {
      doSpecialWebcal(req, resp, pars);
      return true;
    }

    return false;
  }

  private void doSpecialFreeBusy(HttpServletRequest req,
                                 HttpServletResponse resp,
                                 RequestPars pars) throws WebdavException {
    try {
      if (account != null) {
        pars.originator = getSysi().userToCaladdr(account);
      }

      String cua = req.getParameter("cua");

      if (cua == null) {
        String user = req.getParameter("user");
        if (user == null) {
          if (account == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing user/cua");
            return;
          }

          user = account;
        }

        cua = getSysi().userToCaladdr(user);
      }

      pars.recipients.add(cua);
      BwOrganizer org = new BwOrganizer();
      org.setOrganizerUri(cua);

      pars.contentType = "text/calendar; charset=UTF-8";

      TimeRange tr = BwDateTimeUtil.getPeriod(req.getParameter("start"),
                                              req.getParameter("end"),
                                              java.util.Calendar.DATE, 31,
                                              java.util.Calendar.DATE, 32);

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      BwEvent ev = new BwEventObj();
      ev.setDtstart(tr.getStart());
      ev.setDtend(tr.getEnd());

      ev.setEntityType(CalFacadeDefs.entityTypeFreeAndBusy);

      ev.setScheduleMethod(Icalendar.methodTypeRequest);

      ev.setRecipients(pars.recipients);
      ev.setOriginator(pars.originator);
      ev.setOrganizer(org);

      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"freebusy.ics\"");
      resp.setContentType("text/calendar; charset=UTF-8");

      ScheduleResult sr = getSysi().requestFreeBusy(new EventInfo(ev));
      PostMethod.checkStatus(sr);

      for (ScheduleRecipientResult srr: sr.recipientResults) {
        // We expect one only
        BwEvent rfb = srr.freeBusy;
        if (rfb != null) {
          rfb.setOrganizer(org);

          try {
            VFreeBusy vfreeBusy = VFreeUtil.toVFreeBusy(rfb);
            net.fortuna.ical4j.model.Calendar ical = IcalTranslator.newIcal(Icalendar.methodTypeReply);
            ical.getComponents().add(vfreeBusy);
            IcalTranslator.writeCalendar(ical, resp.getWriter());
          } catch (Throwable t) {
            if (debug) {
              error(t);
            }
            throw new WebdavException(t);
          }
        }
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doSpecialWebcal(HttpServletRequest req,
                               HttpServletResponse resp,
                               RequestPars pars) throws WebdavException {
    try {
      TimeRange tr = BwDateTimeUtil.getPeriod(req.getParameter("start"),
                                              req.getParameter("end"),
                                              java.util.Calendar.DATE, 31,
                                              java.util.Calendar.DATE, 32 * 3);

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      String calPath = req.getParameter("calPath");
      if (calPath == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No calPath");
        return;
      }

      calPath = WebdavNsIntf.fixPath(calPath);

      WebdavNsNode node = getNode(calPath,
                                  WebdavNsIntf.existanceMust,
                                  WebdavNsIntf.nodeTypeUnknown);

      if ((node == null) || !node.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (!node.isCollection()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not collection");
        return;
      }

      /*
      CaldavCalNode calNode = (CaldavCalNode)node;

      BwCalendar cal = calNode.getCalendar();
      if (cal == null) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, calPath);
        return;
      }

      BwFilter filter = null;

      String cat = req.getParameter("cat");
      if (cat != null) {
        PropertyInfo pi = PropertyIndex.propertyInfoByPname.get("CATEGORIES");
        BwObjectFilter<String> f = new BwObjectFilter<String>(null, pi.getPindex());
        f.setEntity(cat);
        f.setExact(false);

        filter = f;
      }

      try {
        BwDateTime sdt = dp.start;
        BwDateTime edt = dp.end;

        if (debug) {
          debugMsg("getEvents for start = " + sdt +
                   " end = " + edt);
        }

        BwSubscription sub = BwSubscription.makeSubscription(cal);

        RecurringRetrievalMode rrm = new RecurringRetrievalMode(Rmode.expanded,
                                                                sdt, edt);
                                                                */
      Collection<EventInfo> evs = new ArrayList<EventInfo>();

      for (WebdavNsNode child: getChildren(node)) {
        if (child instanceof CaldavComponentNode) {
          evs.add(((CaldavComponentNode)child).getEventInfo());
        }
      }

      Calendar ical = getSysi().toCalendar(evs, Icalendar.methodTypePublish);

      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"" +
                     node.getDisplayname() + ".ics\"");
      resp.setContentType("text/calendar; charset=UTF-8");

      IcalTranslator.writeCalendar(ical, resp.getWriter());
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                  Access methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getGroups(java.lang.String, java.lang.String)
   */
  public Collection<WebdavNsNode> getGroups(String resourceUri,
                                            String principalUrl)
          throws WebdavException {
    Collection<WebdavNsNode> res = new ArrayList<WebdavNsNode>();

    Collection<String> hrefs = getSysi().getGroups(resourceUri, principalUrl);
    for (String href: hrefs) {
      if (href.endsWith("/")) {
        href = href.substring(0, href.length());
      }
      int pos = href.lastIndexOf("/");
      String account = href.substring(pos + 1);
      String basePath = href.substring(0, pos);

      PrincipalInfo pi = new PrincipalInfo(Ace.whoTypeUser, account, basePath);
      res.add(new CaldavUserNode(new CaldavURI(pi),
                                 getSysi(), null, debug));
    }

    return res;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipalCollectionSet(java.lang.String)
   */
  public Collection<String> getPrincipalCollectionSet(String resourceUri)
         throws WebdavException {
    ArrayList<String> al = new ArrayList<String>();

    for (String s: getSysi().getPrincipalCollectionSet(resourceUri)) {
      al.add(sysi.getUrlHandler().prefix(s));
    }

    return al;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipals(java.lang.String, edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch)
   */
  public Collection<WebdavPrincipalNode> getPrincipals(String resourceUri,
                                                PrincipalPropertySearch pps)
          throws WebdavException {
    ArrayList<WebdavPrincipalNode> pnodes = new ArrayList<WebdavPrincipalNode>();

    for (CalUserInfo cui: sysi.getPrincipals(resourceUri, pps)) {
      PrincipalInfo pi = new PrincipalInfo(Ace.whoTypeUser,
                                           cui.account,
                                           cui.principalPathPrefix);
      pnodes.add(new CaldavUserNode(new CaldavURI(pi),
                                    getSysi(), cui, debug));
    }

    return pnodes;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#makeUserHref(java.lang.String)
   */
  public String makeUserHref(String id) throws WebdavException {
    return getSysi().makeHref(id, Ace.whoTypeUser);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#updateAccess(edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf.AclInfo)
   */
  public void updateAccess(AclInfo info) throws WebdavException {
    CaldavBwNode node = (CaldavBwNode)getNode(info.what,
                                              WebdavNsIntf.existanceMust,
                                              WebdavNsIntf.nodeTypeUnknown);

    try {
      // May need a real principal hierarchy
      if (node instanceof CaldavCalNode) {
        sysi.updateAccess(((CaldavCalNode)node).getCalendar(), info.acl);
      } else if (node instanceof CaldavComponentNode) {
        sysi.updateAccess(((CaldavComponentNode)node).getEventInfo().getEvent(),
                          info.acl);
      } else {
        throw new WebdavException(HttpServletResponse.SC_NOT_IMPLEMENTED);
      }
    } catch (WebdavException wi) {
      throw wi;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void emitAcl(WebdavNsNode node) throws WebdavException {
    CaldavBwNode cdnode = getBwnode(node);
    Acl acl = null;

    try {
      if (cdnode.isCollection()) {
        acl = node.getCurrentAccess().acl;
      } else if (node instanceof CaldavComponentNode) {
        acl = ((CaldavComponentNode)node).getEventInfo().getCurrentAccess().acl;
      }

      if (acl != null) {
        accessUtil.emitAcl(acl, true);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getAclPrincipalInfo(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public Collection<String> getAclPrincipalInfo(WebdavNsNode node) throws WebdavException {
    try {
      TreeSet<String> hrefs = new TreeSet<String>();

      for (Ace ace: node.getCurrentAccess().acl.getAces()) {
        AceWho who = ace.getWho();

        if (who.getWhoType() == WhoDefs.whoTypeUser) {
          hrefs.add(accessUtil.makeUserHref(who.getWho()));
        } else if (who.getWhoType() == WhoDefs.whoTypeGroup) {
          hrefs.add(accessUtil.makeGroupHref(who.getWho()));
        }
      }

      return hrefs;
    } catch (AccessException ae) {
      if (debug) {
        error(ae);
      }
      throw new WebdavServerError();
    }
  }

  /* ====================================================================
   *                Property value methods
   * ==================================================================== */

  /** Override this to create namespace specific property objects.
   *
   * @param propnode
   * @return WebdavProperty
   * @throws WebdavException
   */
  public WebdavProperty makeProp(Element propnode) throws WebdavException {
    if (!XmlUtil.nodeMatches(propnode, CaldavTags.calendarData)) {
      return super.makeProp(propnode);
    }

    /* Handle the calendar-data element */

    CalendarData caldata = new CalendarData(new QName(propnode.getNamespaceURI(),
                                                      propnode.getLocalName()),
                                                      debug);
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

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#knownProperty(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, edu.rpi.cct.webdav.servlet.shared.WebdavProperty)
   */
  public boolean knownProperty(WebdavNsNode node,
                               WebdavProperty pr) {
    QName tag = pr.getTag();

    if (node.knownProperty(tag)) {
      return true;
    }

    for (int i = 0; i < knownProperties.length; i++) {
      if (tag.equals(knownProperties[i])) {
        return true;
      }
    }

    /* Try the node for a value */

    return super.knownProperty(node, pr);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#generatePropValue(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, edu.rpi.cct.webdav.servlet.shared.WebdavProperty, boolean)
   */
  public boolean generatePropValue(WebdavNsNode node,
                                   WebdavProperty pr,
                                   boolean allProp) throws WebdavException {
    QName tag = pr.getTag();
    String ns = tag.getNamespaceURI();

    try {
      /* Deal with anything but webdav properties */
      if (ns.equals(WebdavTags.namespace)) {
        // Not ours
        return super.generatePropValue(node, pr, allProp);
      }

      if (tag.equals(CaldavTags.calendarData)) {
        // pr may be a CalendarData object - if not it's probably allprops
        if (!(pr instanceof CalendarData)) {
          pr = new CalendarData(tag, debug);
        }

        CalendarData caldata = (CalendarData)pr;
        String content = null;

        if (debug) {
          trace("do CalendarData for " + node.getUri());
        }

        int status = HttpServletResponse.SC_OK;
        try {
          content = caldata.process(node);
        } catch (WebdavException wde) {
          status = wde.getStatusCode();
          if (debug && (status != HttpServletResponse.SC_NOT_FOUND)) {
            error(wde);
          }
        }

        if (status != HttpServletResponse.SC_OK) {
          // XXX should be passing status back
          return false;
        }

        /* Output the (transformed) node.
         */

        xml.cdataProperty(CaldavTags.calendarData, content);
        return true;
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
        xml.property(tag, String.valueOf(sysi.getMaxUserEntitySize()));
        return true;
      }

      if (tag.equals(CaldavTags.minDateTime)) {
        return false;
      }

      return node.generatePropertyValue(tag, this, allProp);
    } catch (WebdavException wie) {
      throw wie;
    } catch (Throwable t) {
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
   * @param retrieveRecur  How we retrieve recurring events
   * @param fltr      Filter object defining search
   * @return Collection of result nodes (empty for no result)
   * @throws WebdavException
   */
  public Collection<WebdavNsNode> query(WebdavNsNode wdnode,
                                        RecurringRetrievalMode retrieveRecur,
                                        Filter fltr) throws WebdavException {
    CaldavBwNode node = getBwnode(wdnode);
    Collection<EventInfo> events;

    events = fltr.query(node, retrieveRecur);

    /* We now need to build a node for each of the events in the collection.
       For each event we first determine what calendar it's in. We then take the
       incoming uri, strip any calendar names off it and append the calendar
       name and event name to create the new uri.

       If there is no calendar name for the event we just give it the default.
     */

    Collection<WebdavNsNode> evnodes = new ArrayList<WebdavNsNode>();
    //HashMap evnodeMap = new HashMap();

    try {
      for (EventInfo ei: events) {
        BwEvent ev = ei.getEvent();

        BwCalendar cal = ev.getCalendar();
        String uri = cal.getPath();

        /* If no name was assigned use the guid */
        String evName = ev.getName();
        if (evName == null) {
          evName = ev.getUid() + ".ics";
        }

        String evuri = uri + "/" + evName;

        /* See if we've seen this one already - possible for recurring * /
        CaldavComponentNode evnode;

        evnode = (CaldavComponentNode)evnodeMap.get(evuri);

        if (evnode == null) {
          EventInfo rei = null;

          if (ev.getRecurrenceId() != null) {
            /* First add the master event * /
            rei = ei;

            BwEventProxy proxy = (BwEventProxy)ev;
            ei = new EventInfo(proxy.getTarget());
          } */

        CaldavComponentNode evnode = (CaldavComponentNode)getNodeInt(evuri,
                                                   WebdavNsIntf.existanceDoesExist,
                                                   WebdavNsIntf.nodeTypeEntity,
                                                   cal, ei, null);

          evnodes.add(evnode);
          /*evnodeMap.put(evuri, evnode);

          if (rei != null) {
            // Recurring - add first instance.
            evnode.addEvent(rei);
          }
        } else {
          evnode.addEvent(ei);
        }*/
      }

      return fltr.postFilter(evnodes);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      error(t);
      throw new WebdavServerError();
    }
  }

  /** The node represents a calendar resource for which we must get free-busy
   * information.
   *
   * @param cnode  CaldavCalNode
   * @param freeBusy
   * @param depth
   * @throws WebdavException
   */
  public void getFreeBusy(CaldavCalNode cnode,
                          FreeBusyQuery freeBusy,
                          int depth) throws WebdavException {
    try {
      BwEvent fb = freeBusy.getFreeBusy(sysi, cnode.getCalendar(),
                                        cnode.getOwner().getAccount(),
                                        depth);

      cnode.setFreeBusy(fb);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param node
   * @return CaldavBwNode
   * @throws WebdavException
   */
  public CaldavBwNode getBwnode(WebdavNsNode node)
      throws WebdavException {
    if (!(node instanceof CaldavBwNode)) {
      throw new WebdavException("Not a valid node object " +
                                    node.getClass().getName());
    }

    return (CaldavBwNode)node;
  }

  /**
   * @param node
   * @param errstatus
   * @return CaldavCalNode
   * @throws WebdavException
   */
  public CaldavCalNode getCalnode(WebdavNsNode node, int errstatus)
      throws WebdavException {
    if (!(node instanceof CaldavCalNode)) {
      throw new WebdavException(errstatus);
    }

    return (CaldavCalNode)node;
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

  /** Find the named item by following down the path from the root.
   * This requires the names at each level to be unique (and present)
   *
   * I don't think the name.now has to have an ics suffix. Draft 7 goes as
   * far as saying it may have ".ics" or ".ifb"
   *
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
   * @param cal        Supplied BwCalendar object if we already have it.
   * @param ei
   * @param rsrc
   * @return CaldavURI object representing the uri
   * @throws WebdavException
   */
  private CaldavURI findURI(String uri,
                            int existance,
                            int nodeType,
                            BwCalendar cal,
                            EventInfo ei,
                            BwResource rsrc) throws WebdavException {
    try {
      if ((nodeType == WebdavNsIntf.nodeTypeUnknown) &&
          (existance != WebdavNsIntf.existanceMust)) {
        // We assume an unknown type must exist
        throw new WebdavServerError();
      }

      uri = normalizeUri(uri);

      if (!uri.startsWith("/")) {
        return null;
      }

      CaldavURI curi = null;
      /* Look for it in the map * /
       * This is stateless so we probably never find it.
      CaldavURI curi = getUriPath(uri);
      if (curi != null) {
        if (debug) {
          debugMsg("reuse uri - " + curi.getPath() +
                   "\" entityName=\"" + curi.getEntityName() + "\"");
        }
        return curi;
      }*/

      boolean isPrincipal = sysi.isPrincipal(uri);

      if ((nodeType == WebdavNsIntf.nodeTypePrincipal) && !isPrincipal) {
        throw new WebdavNotFound(uri);
      }

      if (isPrincipal) {
        PrincipalInfo pi = getSysi().getPrincipalInfo(uri);

        return new CaldavURI(pi);
      }

      if (existance == WebdavNsIntf.existanceDoesExist) {
        // Provided with calendar and entity if needed.
        String name = null;
        if (ei != null) {
          name = ei.getEvent().getName();
          curi = new CaldavURI(cal, ei, name, true);
        } else if (rsrc != null) {
          curi = new CaldavURI(rsrc, true);
        } else {
          curi = new CaldavURI(cal, ei, name, true);
        }
        //putUriPath(curi);

        return curi;
      }

      if ((nodeType == WebdavNsIntf.nodeTypeCollection) ||
          (nodeType == WebdavNsIntf.nodeTypeUnknown)) {
        // For unknown we try the full path first as a calendar.
        if (debug) {
          debugMsg("search for collection uri \"" + uri + "\"");
        }
        cal = sysi.getCalendar(uri);

        if (cal == null) {
          if ((nodeType == WebdavNsIntf.nodeTypeCollection) &&
              (existance != WebdavNsIntf.existanceNot) &&
              (existance != WebdavNsIntf.existanceMay)) {
            /* We asked for a collection and it doesn't exist */
            throw new WebdavNotFound(uri);
          }

          // We'll try as an entity for unknown
        } else {
          if (existance == WebdavNsIntf.existanceNot) {
            throw new WebdavForbidden(WebdavTags.resourceMustBeNull);
          }

          if (debug) {
            debugMsg("create collection uri - cal=\"" + cal.getPath() + "\"");
          }

          curi = new CaldavURI(cal, true);
          //putUriPath(curi);

          return curi;
        }
      }

      // Entity or unknown

      /* Split name into parent path and entity name part */
      SplitResult split = splitUri(uri);

      if (split.name == null) {
        // No name part
        throw new WebdavNotFound(uri);
      }

      /* Look for the parent */
      cal = sysi.getCalendar(split.path);

      if (cal == null) {
        if (nodeType == WebdavNsIntf.nodeTypeCollection) {
          // Trying to create calendar/collection with no parent
          throw new WebdavException(HttpServletResponse.SC_CONFLICT);
        }

        throw new WebdavNotFound(uri);
      }

      if (nodeType == WebdavNsIntf.nodeTypeCollection) {
        // Trying to create calendar/collection
        BwCalendar newCal = new BwCalendar();

        newCal.setCalendar(cal);
        newCal.setName(split.name);
        newCal.setPath(cal.getPath() + "/" + newCal.getName());

        curi = new CaldavURI(newCal, false);
        //putUriPath(curi);

        return curi;
      }

      if (cal.getCalendarCollection()) {
        if (debug) {
          debugMsg("find event(s) - cal=\"" + cal.getPath() + "\" name=\"" +
                   split.name + "\"");
        }

        RecurringRetrievalMode rrm =
          new RecurringRetrievalMode(Rmode.overrides);
        ei = sysi.getEvent(cal, split.name, rrm);

        if ((existance == WebdavNsIntf.existanceMust) && (ei == null)) {
          throw new WebdavNotFound(uri);
        }

        curi = new CaldavURI(cal, ei, split.name, ei != null);
      } else {
        if (debug) {
          debugMsg("find resource - cal=\"" + cal.getPath() + "\" name=\"" +
                   split.name + "\"");
        }

        /* Look for a resource */
        rsrc = sysi.getFile(cal, split.name);

        if ((existance == WebdavNsIntf.existanceMust) && (rsrc == null)) {
          throw new WebdavNotFound(uri);
        }

        boolean exists = rsrc != null;

        if (!exists) {
          rsrc = new BwResource();
          rsrc.setName(split.name);
          rsrc.setCalendar(cal);
        }

        curi = new CaldavURI(rsrc, exists);
      }

      //putUriPath(curi);

      return curi;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private static class SplitResult {
    String path;
    String name;

    SplitResult(String path, String name) {
      this.path = path;
      this.name = name;
    }
  }

  /* Split the uri so that result.path is the path up to the name part result.name
   *
   * NormalizeUri was called previously so we have no trailing "/"
   */
  private SplitResult splitUri(String uri) throws WebdavException {
    int pos = uri.lastIndexOf("/");
    if (pos < 0) {
      // bad uri
      throw new WebdavBadRequest("Invalid uri: " + uri);
    }

    if (pos == 0) {
      return new SplitResult(uri, null);
    }

    return new SplitResult(uri.substring(0, pos), uri.substring(pos + 1));
  }

  private String normalizeUri(String uri) throws WebdavException {
    /*Remove all "." and ".." components */
    try {
      uri = new URI(null, null, uri, null).toString();

      uri = new URI(URLEncoder.encode(uri, "UTF-8")).normalize().getPath();

      uri = URLDecoder.decode(uri, "UTF-8");

      if (uri.endsWith("/")) {
        uri = uri.substring(0, uri.length() - 1);
      }

      if (debug) {
        debugMsg("Normalized uri=" + uri);
      }

      return uri;
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavBadRequest("Bad uri: " + uri);
    }
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
