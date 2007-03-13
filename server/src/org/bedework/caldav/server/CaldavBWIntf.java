/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import org.bedework.caldav.server.SysIntf.CalUserInfo;
import org.bedework.caldav.server.calquery.CalendarData;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.filter.Filter;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.RecurringRetrievalMode.Rmode;
import org.bedework.calfacade.env.CalEnvFactory;
import org.bedework.calfacade.env.CalEnvI;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.Headers;
import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.common.PrincipalMatchReport;
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
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavServerError;
import edu.rpi.cct.webdav.servlet.shared.WebdavUnauthorized;
import edu.rpi.cct.webdav.servlet.shared.WebdavUnsupportedMediaType;
import edu.rpi.cmt.access.AccessException;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.AceWho;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.Privileges;
import edu.rpi.cmt.access.WhoDefs;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.QName;

import net.fortuna.ical4j.model.TimeZone;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLDecoder;
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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

  private EmitAccess emitAccess;

  /** Namespace based on the request url.
   */
  @SuppressWarnings("unused")
  private String namespace;

  /* Prefix for our properties */
  private String envPrefix;

  SysIntf sysi;

  /* These two set after a call to getSvci()
   */
  //private IcalTranslator trans;
  //private CalSvcI svci;

  //private CalEnv env;

  /** We store CaldavURI objects here
   */
  private HashMap<String, CaldavURI> uriMap = new HashMap<String, CaldavURI>();

  /** An object representing the current users access
   */
//  LuwakAccess access;

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

      envPrefix = "org.bedework.app." + appName + ".";

      namespacePrefix = WebdavUtils.getUrlPrefix(req);
      namespace = namespacePrefix + "/schema";

      CalEnvI env = CalEnvFactory.getEnv(envPrefix, debug);

      sysi = (SysIntf)env.getAppObject("sysintfimpl", SysIntf.class);

      sysi.init(req, envPrefix, account, debug);

      emitAccess = new EmitAccess(namespacePrefix, xml, sysi);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getDavHeader(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
   */
  public String getDavHeader(WebdavNsNode node) throws WebdavException {
    return "1, access-control, calendar-access, calendar-schedule";
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavException {
    return sysi.getDirectoryBrowsingDisallowed();
  }

  public void close() throws WebdavException {
    sysi.close();
  }

  /**
   * @return SysIntf
   */
  public SysIntf getSysi() {
    return sysi;
  }

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

  public void addNamespace() throws WebdavException {
    super.addNamespace();

    try {
      xml.addNs(CaldavDefs.caldavNamespace);
      xml.addNs(CaldavDefs.icalNamespace);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public String getLocation(WebdavNsNode node) throws WebdavException {
    return namespacePrefix + node.getUri();
  }

  public WebdavNsNode getNode(String uri,
                              int existance,
                              int nodeType) throws WebdavException {
    return getNodeInt(uri, existance, nodeType, true, null, null);
  }

  public WebdavNsNode getNodeEncoded(String uri,
                                     int existance,
                                     int nodeType) throws WebdavException {
    return getNodeInt(uri, existance, nodeType, false, null, null);
  }

  private WebdavNsNode getNodeInt(String uri,
                                  int existance,
                                  int nodeType,
                                  boolean decoded,
                                  BwCalendar cal,
                                  EventInfo ei) throws WebdavException {
    if (debug) {
      debugMsg("About to get node for " + uri);
    }

    if (uri == null)  {
      return null;
    }

    try {
      CaldavURI wi = findURI(uri, existance, nodeType, decoded, cal, ei);

      if (wi == null) {
        throw new WebdavNotFound(uri);
      }

      WebdavNsNode nd = null;

      if (wi.isUser()) {
        nd = new CaldavUserNode(wi, sysi, null, debug);
      } else if (wi.isGroup()) {
        nd = new CaldavGroupNode(wi, sysi, debug);
      } else if (wi.isCalendar()) {
        nd = new CaldavCalNode(wi, sysi, debug);
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
      CaldavBwNode uwnode = getBwnode(node);

      if (uwnode instanceof CaldavComponentNode) {
        CaldavComponentNode cnode = (CaldavComponentNode)uwnode;

        BwEvent ev = cnode.getEventInfo().getEvent();

        if (ev != null) {
          if (debug) {
            trace("About to delete event " + ev);
          }
          sysi.deleteEvent(ev);
        } else {
          if (debug) {
            trace("No event object available");
          }
        }
      } else {
        if (!(uwnode instanceof CaldavCalNode)) {
          throw new WebdavUnauthorized();
        }

        CaldavCalNode cnode = (CaldavCalNode)uwnode;

        BwCalendar cal = cnode.getCDURI().getCal();

        sysi.deleteCalendar(cal);
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Iterator<WebdavNsNode> getChildren(WebdavNsNode node) throws WebdavException {
    try {
      CaldavBwNode uwnode = getBwnode(node);

      ArrayList<WebdavNsNode> al = new ArrayList<WebdavNsNode>();

      if (!uwnode.getCollection()) {
        // Don't think we should have been called
        return al.iterator();
      }

      if (debug) {
        debugMsg("About to get children for " + uwnode.getUri());
      }

      Collection children = uwnode.getChildren();
      String uri = uwnode.getUri();
      BwCalendar parent = uwnode.getCDURI().getCal();

      Iterator it = children.iterator();

      while (it.hasNext()) {
        Object o = it.next();
        BwCalendar cal = null;
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
        } else if (o instanceof EventInfo) {
          cal = parent;
          ei = (EventInfo)o;
          name = ei.getEvent().getName();
          nodeType = WebdavNsIntf.nodeTypeEntity;
        } else {
          throw new WebdavException("Unexpected return type");
        }

        CaldavURI wi = findURI(uri + "/" + name,
                               WebdavNsIntf.existanceDoesExist,
                               nodeType, true, cal, ei);

        if (wi.isCalendar()) {
          if (debug) {
            debugMsg("Add child as calendar");
          }

          al.add(new CaldavCalNode(wi, sysi, debug));
        } else {
          if (debug) {
            debugMsg("Add child as component");
          }

          CaldavComponentNode cnode = new CaldavComponentNode(wi, sysi, debug);
          al.add(cnode);
        }
      }

      return al.iterator();
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

  /*
  private void addProp(Vector v, QName tag, Object val) {
    if (val != null) {
      v.addElement(new WebdavProperty(tag, String.valueOf(val)));
    }
  }*/

  public Reader getContent(WebdavNsNode node)
      throws WebdavException {
    try {
      if (!node.getAllowsGet()) {
        return null;
      }

      CaldavBwNode uwnode = getBwnode(node);

      return uwnode.getContent();
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
   * @param req
   * @return Icalendar
   * @throws WebdavException
   */
  public Icalendar getIcal(BwCalendar cal, HttpServletRequest req)
      throws WebdavException {
    try {
      return sysi.fromIcal(cal, new MyReader(req.getReader()));
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public PutContentResult putContent(WebdavNsNode node,
                                     Reader contentRdr,
                                     boolean create)
      throws WebdavException {
    try {
      PutContentResult pcr = new PutContentResult();
      pcr.node = node;
      pcr.created = create;

      CaldavComponentNode bwnode = (CaldavComponentNode)getBwnode(node);
      CaldavURI cdUri = bwnode.getCDURI();
      String entityName = cdUri.getEntityName();
      BwCalendar cal = cdUri.getCal();

      Icalendar ic = sysi.fromIcal(cal, new MyReader(contentRdr));

      /** We can only put a single resource - that resource will be an ics file
       * containing an event and possible overrides. The calendar may contain
       * timezones which we can ignore.
       */

      Iterator it = ic.iterator();
      String guid = null;
      boolean fail = false;

      while (it.hasNext()) {
        Object o = it.next();

        if (o instanceof EventInfo) {
          EventInfo evinfo = (EventInfo)o;
          BwEvent ev = evinfo.getEvent();

          if (guid == null) {
            guid = ev.getUid();
          } else if (!guid.equals(ev.getUid())) {
            fail = true;
            break;
          }

          if (debug) {
            debugMsg("putContent: intf has event with name " + entityName +
                     " and summary " + ev.getSummary());
          }

          if (evinfo.getNewEvent()) {
            pcr.created = true;
            ev.setName(entityName);

            /* Collection<BwEventProxy>failedOverrides = */
              sysi.addEvent(cal, ev, evinfo.getOverrideProxies(), true);

            StringBuffer sb = new StringBuffer(cdUri.getPath());
            sb.append("/");
            sb.append(entityName);
            if (!entityName.toLowerCase().endsWith(".ics")) {
              sb.append(".ics");
            }

            bwnode.setEventInfo(evinfo);
          } else {
            if (!entityName.equals(ev.getName())) {
              throw new WebdavBadRequest("Mismatched names");
            }

            /* XXX check calendar not changed */

            if (debug) {
              debugMsg("putContent: update event " + ev);
            }
            sysi.updateEvent(ev, evinfo.getOverrideProxies(),
                             evinfo.getChangeset());
          }
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
      CaldavBwNode bwnode = getBwnode(node);
      CaldavURI cdUri = bwnode.getCDURI();

      /* The uri should have an entity name representing the new collection
       * and a calendar object representing the parent.
       *
       * A namepart of null means that the path already exists
       */

      BwCalendar newCal = cdUri.getCal();
      BwCalendar parent = newCal.getCalendar();
      if (parent.getCalendarCollection()) {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN /*,
                       "Forbidden: Calendar collection as parent" */);
      }

      String name = newCal.getName();
      if (name == null) {
        throw new WebdavForbidden("Forbidden: Null name");
      }

      resp.setStatus(sysi.makeCollection(name,
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
      }

      BwCalendar fromCal = fromCalNode.cdURI.getCal();
      BwCalendar toCal = toCalNode.cdURI.getCal();

      getSysi().copyMove(fromCal, toCal, copy, overwrite);
      if (toCalNode.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        resp.setStatus(HttpServletResponse.SC_CREATED);
      }

      return;
    }

    if (!(from instanceof CaldavComponentNode)) {
      throw new WebdavBadRequest();
    }

    if (!(to instanceof CaldavComponentNode)) {
      throw new WebdavBadRequest();
    }

    // Copy entity
    if ((depth != Headers.depthNone) && (depth != 0)) {
      throw new WebdavBadRequest();
    }

    CaldavComponentNode fromNode = (CaldavComponentNode)from;
    CaldavComponentNode toNode = (CaldavComponentNode)to;

    if (toNode.getExists() && !overwrite) {
      resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
    }

    EventInfo fromEi = fromNode.getEventInfo();
    BwCalendar toCal = toNode.cdURI.getCal();

    if (!getSysi().copyMove(fromEi.getEvent(), fromEi.getOverrideProxies(),
                            toCal, toNode.getCDURI().getEntityName(), copy,
                            overwrite)) {
      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setStatus(HttpServletResponse.SC_CREATED);
    }
  }

  /* ====================================================================
   *                  Access methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipalPrefix()
   */
  public String getPrincipalPrefix() throws WebdavException {
    return getSysi().getPrincipalRoot();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#principalMatch(java.lang.String, edu.rpi.cct.webdav.servlet.common.PrincipalMatchReport)
   */
  public Collection<WebdavNsNode> principalMatch(String resourceUri,
                                                 PrincipalMatchReport pmatch)
          throws WebdavException {
    Collection<WebdavNsNode> res = new ArrayList<WebdavNsNode>();

    if (pmatch.self) {
      if (resourceUri.endsWith("/")) {
        resourceUri = resourceUri.substring(0, resourceUri.length() - 1);
      }

      /* ResourceUri should be the principals root or user principal root */
      if (!resourceUri.equals(getPrincipalPrefix()) &&
          !resourceUri.equals(getSysi().getUserPrincipalRoot())) {
        return res;
      }

      res.add(new CaldavUserNode(new CaldavURI(this.account,
                                               getSysi().getUserPrincipalRoot(),
                                               true),
                                 getSysi(), null, debug));
      return res;
    }

    return res;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipalCollectionSet(java.lang.String)
   */
  public Collection<String> getPrincipalCollectionSet(String resourceUri)
         throws WebdavException {
    return getSysi().getPrincipalCollectionSet(resourceUri);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipals(java.lang.String, edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch)
   */
  public Collection<CaldavBwNode> getPrincipals(String resourceUri,
                                                PrincipalPropertySearch pps)
          throws WebdavException {
    ArrayList<CaldavBwNode> pnodes = new ArrayList<CaldavBwNode>();

    for (CalUserInfo cui: sysi.getPrincipals(resourceUri, pps)) {
      pnodes.add(new CaldavUserNode(new CaldavURI(cui.account,
                                                  getSysi().getUserPrincipalRoot(),
                                                  true),
                                    getSysi(), cui, debug));
    }

    return pnodes;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#makeUserHref(java.lang.String)
   */
  public String makeUserHref(String id) throws WebdavException {
    return getSysi().makeUserHref(id);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#makeGroupHref(java.lang.String)
   */
  public String makeGroupHref(String id) throws WebdavException {
    return getSysi().makeGroupHref(id);
  }

  /** Object class passed around as we parse access.
   */
  public static class CdAclInfo extends AclInfo {
    String what;

    PrincipalInfo pi;

    boolean notWho;
    int whoType;
    String who;

    ArrayList<Ace> aces = new ArrayList<Ace>();

    Ace curAce;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#startAcl(java.lang.String)
   */
  public AclInfo startAcl(String uri) throws WebdavException {
    CdAclInfo ainfo = new CdAclInfo();

    ainfo.what = uri;

    return ainfo;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#parseAcePrincipal(edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf.AclInfo, org.w3c.dom.Node, boolean)
   */
  public boolean parseAcePrincipal(AclInfo ainfo, Node nd,
                                boolean inverted) throws WebdavException {
    CdAclInfo info = (CdAclInfo)ainfo;

    info.notWho = inverted;

    Element el = getOnlyChild(nd);

    info.whoType = -1;
    info.who = null;

    if (MethodBase.nodeMatches(el, WebdavTags.href)) {
      String href = getElementContent(el);

      if ((href == null) || (href.length() == 0)) {
        throw new WebdavBadRequest("Missing href");
      }
      info.pi = getPrincipalInfo(info.pi, href);
      if (info.pi == null) {
        info.errorTag = WebdavTags.recognizedPrincipal;
        return false;
      }
      info.whoType = info.pi.whoType;
      info.who = info.pi.who;
    } else if (MethodBase.nodeMatches(el, WebdavTags.all)) {
      info.whoType = Ace.whoTypeAll;
    } else if (MethodBase.nodeMatches(el, WebdavTags.authenticated)) {
      info.whoType = Ace.whoTypeAuthenticated;
    } else if (MethodBase.nodeMatches(el, WebdavTags.unauthenticated)) {
      info.whoType = Ace.whoTypeUnauthenticated;
    } else if (MethodBase.nodeMatches(el, WebdavTags.property)) {
      el = getOnlyChild(el);
      if (MethodBase.nodeMatches(el, WebdavTags.owner)) {
        info.whoType = Ace.whoTypeOwner;
      } else {
        throw new WebdavBadRequest("Bad WHO property");
      }
    } else if (MethodBase.nodeMatches(el, WebdavTags.self)) {
      info.whoType = Ace.whoTypeUser;
      info.who = account;
    } else {
      throw new WebdavBadRequest("Bad WHO");
    }

    info.curAce = null;

    if (debug) {
      debugMsg("Parsed ace/principal whoType=" + info.whoType +
               " who=\"" + info.who + "\"");
    }

    return true;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#parsePrivilege(edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf.AclInfo, org.w3c.dom.Node, boolean)
   */
  public void parsePrivilege(AclInfo ainfo, Node nd,
                             boolean denial) throws WebdavException {
    CdAclInfo info = (CdAclInfo)ainfo;

    Element el = getOnlyChild(nd);

    int priv;

    QName[] privTags = emitAccess.getPrivTags();

    if (info.curAce == null) {
      /* Look for this 'who' in the list */
      AceWho awho = new AceWho(info.who, info.whoType, info.notWho);
      for (Ace ace: info.aces) {
        if (ace.getWho().equals(awho)) {
          info.curAce = ace;
          break;
        }
      }

      if (info.curAce == null) {
        info.curAce = new Ace();
        info.curAce.setWho(awho);

        info.aces.add(info.curAce);
      }
    }

    findPriv: {
      // ENUM
      for (priv = 0; priv < privTags.length; priv++) {
        if (MethodBase.nodeMatches(el, privTags[priv])) {
          break findPriv;
        }
      }
      throw new WebdavBadRequest("Bad privilege");
    }

    info.curAce.addPriv(Privileges.makePriv(priv, denial));

  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#updateAccess(edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf.AclInfo)
   */
  public void updateAccess(AclInfo ainfo) throws WebdavException {
    CdAclInfo info = (CdAclInfo)ainfo;

    CaldavBwNode node = (CaldavBwNode)getNode(info.what,
                                              WebdavNsIntf.existanceMust,
                                              WebdavNsIntf.nodeTypeUnknown);

    try {
      if (node.isCollection()) {
        sysi.updateAccess(node.getCDURI().getCal(), info.aces);
      } else {
        sysi.updateAccess(((CaldavComponentNode)node).getEventInfo().getEvent(),
                          info.aces);
      }
    } catch (WebdavException wi) {
      throw wi;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void emitAcl(WebdavNsNode node) throws WebdavException {
    CaldavBwNode uwnode = getBwnode(node);
    CaldavURI cdUri = uwnode.getCDURI();
    Acl acl = null;

    try {
      if (cdUri.isCalendar()) {
        CurrentAccess ca = node.getCurrentAccess();
        if (ca != null) {
          acl = ca.acl;
        }
      } else if (node instanceof CaldavComponentNode) {
        acl = ((CaldavComponentNode)node).getEventInfo().getCurrentAccess().acl;
      }

      if (acl != null) {
        emitAccess.emitAcl(acl, true);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void emitSupportedPrivSet(WebdavNsNode node) throws WebdavException {
    try {
      emitAccess.emitSupportedPrivSet();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** This class is the result of interpreting a principal url
   */
  public static class PrincipalInfo {
    int whoType;   // from access.Ace user, group etc
    String who;    // id of user group etc.
    String prefix; // prefix of hierarchy e.g. /principals/users
  }

  private PrincipalInfo getPrincipalInfo(PrincipalInfo pi, String href)
          throws WebdavException {
    if (pi == null) {
      pi = new PrincipalInfo();
    }

    try {
      String uri = new URI(href).getPath();

      /*
      String[] segs = uri.split("/");

      // First element should be empty, second = "users" or "groups"
      // third is id.

      if ((segs.length != 3) || (segs[0].length() != 0)) {
        throw new WebdavBadRequest("0 or 3 elements expected");
      }

      if ("users".equals(segs[1])) {
        pi.whoType = Ace.whoTypeUser;
      } else if ("groups".equals(segs[1])) {
        pi.whoType = Ace.whoTypeGroup;
      } else {
        throw new WebdavBadRequest("Bad WHO type - expect 'users|groups'");
      }

      if (segs[2].length() == 0) {
        throw new WebdavBadRequest("Missing id");
      }

      pi.who = segs[2];
      */
      if (!uri.startsWith(getPrincipalPrefix())) {
        return null;
      }

      int start;

      int end = uri.length();
      if (uri.endsWith("/")) {
        end--;
      }

      String groupRoot = getSysi().getGroupPrincipalRoot();
      String userRoot = getSysi().getUserPrincipalRoot();

      if (uri.startsWith(userRoot)) {
        start = userRoot.length();
        pi.prefix = userRoot;
        pi.whoType = Ace.whoTypeUser;
      } else if (uri.startsWith(groupRoot)) {
        start = groupRoot.length();
        pi.prefix = groupRoot;
        pi.whoType = Ace.whoTypeGroup;
      } else {
        throw new WebdavNotFound(uri);
      }

      if (start == end) {
        // Trying to browse user principals?
        pi.who = null;
      } else if (uri.charAt(start) != '/') {
        throw new WebdavNotFound(uri);
      } else {
        pi.who = uri.substring(start + 1, end);
      }

      if (debug) {
        debugMsg("getPrincipalInfo \"" + pi.who +
                 "\" group=" + (pi.whoType == Ace.whoTypeGroup) +
                 " principalUri=\"" + uri + "\"");
      }

      return pi;
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavBadRequest();
    }
  }
  public Collection<String> getAclPrincipalInfo(WebdavNsNode node) throws WebdavException {
    try {
      TreeSet<String> hrefs = new TreeSet<String>();

      CurrentAccess ca = node.getCurrentAccess();
      if (ca != null) {
        for (Ace ace: ca.acl.getAces()) {
          AceWho who = ace.getWho();

          if (who.getWhoType() == WhoDefs.whoTypeUser) {
            hrefs.add(emitAccess.makeUserHref(who.getWho()));
          } else if (who.getWhoType() == WhoDefs.whoTypeGroup) {
            hrefs.add(emitAccess.makeGroupHref(who.getWho()));
          }
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

  /** Override to include free and busy access.
   *
   * @return QName[]
   */
  public QName[] getPrivTags() {
    return emitAccess.getPrivTags();
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
    if (!CaldavTags.calendarData.nodeMatches(propnode)) {
      return super.makeProp(propnode);
    }

    /* Handle the calendar-data element */

    CalendarData caldata = new CalendarData(new QName(propnode.getNamespaceURI(),
                                                      propnode.getLocalName()),
                                                      debug);
    caldata.parse(propnode);

    return caldata;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#generatePropValue(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, edu.rpi.cct.webdav.servlet.shared.WebdavProperty, boolean)
   */
  public int generatePropValue(WebdavNsNode node,
                               WebdavProperty pr,
                               boolean allProp) throws WebdavException {
    QName tag = pr.getTag();
    String ns = tag.getNamespaceURI();
    int status = HttpServletResponse.SC_OK;

    try {
      /* Deal with webdav properties */
      if ((!ns.equals(CaldavDefs.caldavNamespace) &&
          !ns.equals(CaldavDefs.icalNamespace))) {
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

        try {
          content = caldata.process(node);
        } catch (WebdavException wde) {
          status = wde.getStatusCode();
          if (debug && (status != HttpServletResponse.SC_NOT_FOUND)) {
            error(wde);
          }
        }

        if (status != HttpServletResponse.SC_OK) {
          xml.emptyTag(tag);
        } else {
          /* Output the (transformed) node.
           */

          xml.cdataProperty(CaldavTags.calendarData, content);
        }

        return status;
      }

      if (tag.equals(CaldavTags.calendarTimezone)) {
        TimeZone tz = sysi.getDefaultTimeZone();
        xml.property(tag, sysi.toStringTzCalendar(tz.getID()));
        return status;
      }

      if (tag.equals(CaldavTags.maxAttendeesPerInstance)) {
        return HttpServletResponse.SC_NOT_FOUND;
      }

      if (tag.equals(CaldavTags.maxDateTime)) {
        return HttpServletResponse.SC_NOT_FOUND;
      }

      if (tag.equals(CaldavTags.maxInstances)) {
        return HttpServletResponse.SC_NOT_FOUND;
      }

      if (tag.equals(CaldavTags.maxResourceSize)) {
        /* e.g.
         * <C:max-resource-size
         *    xmlns:C="urn:ietf:params:xml:ns:caldav">102400</C:max-resource-size>
         */
        xml.property(tag, String.valueOf(sysi.getMaxUserEntitySize()));
        return status;
      }

      if (tag.equals(CaldavTags.minDateTime)) {
        return HttpServletResponse.SC_NOT_FOUND;
      }

      if (node.generatePropertyValue(tag, this, allProp)) {
        // Generated by node
        return status;
      }

      // Not known
      xml.emptyTag(tag);
      return HttpServletResponse.SC_NOT_FOUND;
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
                                                   false,
                                                   cal,
                                                   ei);

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
   * @throws WebdavException
   */
  public void getFreeBusy(CaldavCalNode cnode,
                          FreeBusyQuery freeBusy) throws WebdavException {
    try {
      String user = cnode.getCDURI().getOwner();

      BwFreeBusy fb = freeBusy.getFreeBusy(sysi, cnode.getCDURI().getCal(),
                                           user);

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
   * @param decoded    true if the uri has been decoded
   * @param cal        Supplied BwCalendar object if we already have it.
   * @param ei
   * @return CaldavURI object representing the uri
   * @throws WebdavException
   */
  private CaldavURI findURI(String uri,
                            int existance,
                            int nodeType, boolean decoded,
                            BwCalendar cal,
                            EventInfo ei) throws WebdavException {
    try {
      if ((nodeType == WebdavNsIntf.nodeTypeUnknown) &&
          (existance != WebdavNsIntf.existanceMust)) {
        // We assume an unknown type must exist
        throw new WebdavServerError();
      }

      uri = normalizeUri(uri, decoded);

      if (!uri.startsWith("/")) {
        return null;
      }

      /* Look for it in the map */
      CaldavURI curi = getUriPath(uri);
      if (curi != null) {
        if (debug) {
          debugMsg("reuse uri - " + curi.getPath() +
                   "\" entityName=\"" + curi.getEntityName() + "\"");
        }
        return curi;
      }

      boolean isPrincipal = uri.startsWith(getPrincipalPrefix());

      if ((nodeType == WebdavNsIntf.nodeTypePrincipal) && !isPrincipal) {
        throw new WebdavNotFound(uri);
      }

      if (isPrincipal) {
        PrincipalInfo pi = getPrincipalInfo(null, uri);

        /*
        boolean group;
        int start;

        int end = uri.length();
        if (uri.endsWith("/")) {
          end--;
        }

        String groupRoot = getSysi().getGroupPrincipalRoot();
        String userRoot = getSysi().getUserPrincipalRoot();
        String prefix;

        if (uri.startsWith(userRoot)) {
          start = userRoot.length();
          prefix = userRoot;
          group = false;
        } else if (uri.startsWith(groupRoot)) {
          start = groupRoot.length();
          prefix = groupRoot;
          group = true;
        } else {
          throw new WebdavNotFound(uri);
        }

        if (start == end) {
          // Trying to browse user principals.
          throw new WebdavForbidden();
        }

        if (uri.charAt(start) != '/') {
          throw new WebdavNotFound(uri);
        }

        String account = uri.substring(start + 1, end);
        if (debug) {
          debugMsg("get uri for account \"" + account +
                   "\" group=" + group +
                   " principalUri=\"" + uri + "\"");
        }
        */

        if (pi.whoType == Ace.whoTypeGroup) {
          if (!sysi.validGroup(pi.who)) {
            throw new WebdavNotFound(uri);
          }
        } else if (!sysi.validUser(pi.who)) {
          throw new WebdavNotFound(uri);
        }

        return new CaldavURI(sysi.userToCaladdr(pi.who), pi.prefix,
                             pi.whoType == Ace.whoTypeUser);
      }

      if (existance == WebdavNsIntf.existanceDoesExist) {
        // Provided with calendar and entity if needed.
        String name = null;
        if (ei != null) {
          name = ei.getEvent().getName();
        }
        curi = new CaldavURI(cal, ei, name);
        putUriPath(curi);

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
              (existance != WebdavNsIntf.existanceNot)) {
            /* We asked for a collection and it doesn't exist */
            throw new WebdavNotFound(uri);
          }

          // We'll try as an entity for unknown
        } else {
          if (debug) {
            debugMsg("create collection uri - cal=\"" + cal.getPath() + "\"");
          }

          curi = new CaldavURI(cal, null, null);
          putUriPath(curi);

          return curi;
        }
      }

      // Entity or unknown
      String[] split = splitUri(uri);

      if (split[1] == null) {
        // No name part
        throw new WebdavNotFound(uri);
      }

      cal = sysi.getCalendar(split[0]);

      if (cal == null) {
        throw new WebdavNotFound(uri);
      }

      if (nodeType == WebdavNsIntf.nodeTypeCollection) {
        // Trying to create calendar/collection
        BwCalendar newCal = new BwCalendar();

        newCal.setCalendar(cal);
        newCal.setName(split[1]);
        newCal.setPath(cal.getPath() + "/" + newCal.getName());

        curi = new CaldavURI(newCal, null, null);
        putUriPath(curi);

        return curi;
      }

      if (debug) {
        debugMsg("find event(s) - cal=\"" + cal.getPath() + "\" name=\"" +
                 split[1] + "\"");
      }
      RecurringRetrievalMode rrm =
        new RecurringRetrievalMode(Rmode.overrides);
      ei = sysi.getEvent(cal, split[1], rrm);

      if ((existance == WebdavNsIntf.existanceMust) && (ei == null)) {
        throw new WebdavNotFound(uri);
      }

      curi = new CaldavURI(cal, ei, split[1]);
      putUriPath(curi);

      return curi;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* Split the uri so that result[0] is the path up to the name part result[1]
   */
  private String[] splitUri(String uri) throws WebdavException {
    int pos = uri.lastIndexOf("/");
    if (pos < 0) {
      // bad uri
      throw new WebdavBadRequest("Invalid uri: " + uri);
    }

    if (pos == 0) {
      return new String[]{
          uri,
          null
      };
    }

    return new String[]{
        uri.substring(0, pos),
        uri.substring(pos + 1)
    };
  }

  private String normalizeUri(String uri,
                              boolean decoded) throws WebdavException {
    /*Remove all "." and ".." components */
    try {
      if (decoded) {
        uri = new URI(null, null, uri, null).toString();
      }

      uri = new URI(uri).normalize().getPath();

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

  private CaldavURI getUriPath(String path) {
    return uriMap.get(path);
  }

  private void putUriPath(CaldavURI wi) {
    uriMap.put(wi.getPath(), wi);
  }
}
