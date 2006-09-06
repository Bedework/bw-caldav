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

import org.bedework.caldav.server.calquery.CalendarData;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.filter.Filter;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.env.CalEnvFactory;
import org.bedework.calfacade.env.CalEnvI;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.common.PrincipalMatchReport;
import edu.rpi.cct.webdav.servlet.common.WebdavServlet;
import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.Privileges;
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
  private HashMap uriMap = new HashMap();

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
   * @throws WebdavIntfException
   */
  public void init(WebdavServlet servlet,
                   HttpServletRequest req,
                   Properties props,
                   boolean debug) throws WebdavIntfException {
    super.init(servlet, req, props, debug);

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

      emitAccess = new EmitAccess(namespacePrefix, xml);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /** Return DAV header
   *
   * @retrun  String
   */
  public String getDavHeader() {
    return "1, access-control, calendar-access";
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavIntfException {
    return sysi.getDirectoryBrowsingDisallowed();
  }

  public void close() throws WebdavIntfException {
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

  public void addNamespace() throws WebdavIntfException {
    super.addNamespace();

    try {
      xml.addNs(CaldavDefs.caldavNamespace);
      xml.addNs(CaldavDefs.icalNamespace);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public String getLocation(WebdavNsNode node) throws WebdavIntfException {
    return namespacePrefix + node.getUri();
  }

  public WebdavNsNode getNode(String uri) throws WebdavIntfException {
    return getNodeInt(uri, true);
  }

  public WebdavNsNode getNodeEncoded(String uri) throws WebdavIntfException {
    return getNodeInt(uri, false);
  }

  private WebdavNsNode getNodeInt(String uri, boolean decoded) throws WebdavIntfException {
    if (debug) {
      debugMsg("About to get node for " + uri);
    }

    if (uri == null)  {
      return null;
    }

    try {
      CaldavURI wi = findURI(uri, decoded, null);

      if (wi == null) {
        throw WebdavIntfException.notFound();
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
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public WebdavNsNode getNode(String uri, boolean followAlias)
      throws WebdavIntfException {
    //XXX probably needs fixing
    return getNode(uri);
  }

  public void putNode(WebdavNsNode node)
      throws WebdavIntfException {
  }

  public void delete(WebdavNsNode node) throws WebdavIntfException {
    try {
      CaldavBwNode uwnode = getBwnode(node);

      if (!(uwnode instanceof CaldavComponentNode)) {
        throw WebdavIntfException.unauthorized();
      }

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
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Iterator getChildren(WebdavNsNode node)
      throws WebdavIntfException {
    try {
      CaldavBwNode uwnode = getBwnode(node);

      ArrayList al = new ArrayList();

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
        BwEvent ev = null;
        String name;

        if (o instanceof BwCalendar) {
          cal = (BwCalendar)o;
          name = cal.getName();
          if (debug) {
            debugMsg("Found child " + cal);
          }
        } else if (o instanceof EventInfo) {
          cal = parent;
          EventInfo ei = (EventInfo)o;
          ev = ei.getEvent();
          name = ev.getName();
        } else {
          throw new WebdavIntfException("Unexpected return type");
        }

        CaldavURI wi = findURI(uri + "/" + name, true, cal);

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
          cnode.addEvent(ev);
          al.add(cnode);
        }
      }

      return al.iterator();
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public WebdavNsNode getParent(WebdavNsNode node)
      throws WebdavIntfException {
    return null;
  }

  public String getEtagValue(WebdavNsNode node, boolean strong)
      throws WebdavIntfException {
    return namespace + String.valueOf(node.getLastmodDate());
  }

  public Iterator iterateProperties(WebdavNsNode node) throws WebdavIntfException {
    try {
      CaldavBwNode bwnode = getBwnode(node);

      return WebdavProperty.iterator(bwnode.getProperties(namespace));
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /*
  private void addProp(Vector v, QName tag, Object val) {
    if (val != null) {
      v.addElement(new WebdavProperty(tag, String.valueOf(val)));
    }
  }*/

  public Reader getContent(WebdavNsNode node)
      throws WebdavIntfException {
    try {
      if (!node.getAllowsGet()) {
        return null;
      }

      CaldavBwNode uwnode = getBwnode(node);

      return uwnode.getContent();
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
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
   * @throws WebdavIntfException
   */
  public Icalendar getIcal(BwCalendar cal, HttpServletRequest req)
      throws WebdavIntfException {
    try {
      return sysi.fromIcal(cal, new MyReader(req.getReader()));
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public PutContentResult putContent(WebdavNsNode node,
                                     Reader contentRdr,
                                     boolean create)
      throws WebdavIntfException {
    try {
      PutContentResult pcr = new PutContentResult();
      pcr.node = node;
      pcr.created = create;

      CaldavBwNode bwnode = getBwnode(node);
      CaldavURI cdUri = bwnode.getCDURI();
      String entityName = cdUri.getEntityName();
      BwCalendar cal = cdUri.getCal();

      Icalendar ic = sysi.fromIcal(cal, new MyReader(contentRdr));

      /** if more than one event these must all be instances of the same recurrence, i.e the
       * uid must be the same for each.
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
            guid = ev.getGuid();
          } else if (!guid.equals(ev.getGuid())) {
            fail = true;
            break;
          }

          if (debug) {
            debugMsg("putContent: intf has event with name " + entityName +
                     " and summary " + ev.getSummary());
          }

          /* For non recurring (or just the master) we have a single event which we
           * found by searching for it's guid.
           *
           * For an event with a recurrence id we might have multiple events at the
           * moment.
           *
           * This is incomplete - if we continue with sending multiple vevents
           * for recurring events we need to do some work here
           */
          if (evinfo.getNewEvent()) {
            pcr.created = true;
            ev.setName(entityName);
            sysi.addEvent(cal, ev, evinfo.getOverrides());

            StringBuffer sb = new StringBuffer(cdUri.getPath());
            sb.append("/");
            sb.append(entityName);
            if (!entityName.toLowerCase().endsWith(".ics")) {
              sb.append(".ics");
            }
            if (pcr.node == null) {
              pcr.node = getNode(sb.toString());
            }
          } else {
            if (!entityName.equals(ev.getName())) {
              throw WebdavIntfException.badRequest();
            }

            /* XXX check calendar not changed */

            if (debug) {
              debugMsg("putContent: update event " + ev);
            }
            sysi.updateEvent(ev);
          }
        } else {
          fail = true;
          break;
        }
      }

      if (fail) {
        warn("More than one calendar object for PUT or not event");
        throw WebdavIntfException.badRequest();
      }


      return pcr;
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void create(WebdavNsNode node)
      throws WebdavIntfException {
  }

  public void createAlias(WebdavNsNode alias)
      throws WebdavIntfException {
  }

  public void acceptMkcolContent(HttpServletRequest req)
          throws WebdavIntfException {
    throw WebdavIntfException.unsupportedMediaType();
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
   * @throws WebdavIntfException
   */
  public void makeCollection(HttpServletRequest req, WebdavNsNode node) throws WebdavIntfException {
    try {
      CaldavBwNode uwnode = getBwnode(node);
      CaldavURI cdUri = uwnode.getCDURI();

      /* The uri should have an entity name representing the new collection
       * and a calendar object representing the parent.
       *
       * A namepart of null means that the path already exists
       */

      BwCalendar parent = cdUri.getCal();
      if (parent.getCalendarCollection()) {
        throw WebdavIntfException.forbidden();
      }

      String name = cdUri.getEntityName();

      if (name == null) {
        throw WebdavIntfException.forbidden();
      }

      sysi.makeCollection(name,
                          "MKCALENDAR".equalsIgnoreCase(req.getMethod()),
                          parent.getPath());
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /* ====================================================================
   *                  Access methods
   * ==================================================================== */

  public String getPrincipalPrefix() throws WebdavIntfException {
    return SysIntf.principalPrefix;
  }

  public Collection principalMatch(String resourceUri,
                                   PrincipalMatchReport pmatch)
          throws WebdavIntfException {
    Collection res = new ArrayList();

    if (pmatch.self) {
      /* ResourceUri should be the principals root */
      if (!resourceUri.equals(SysIntf.principalPrefix)) {
        return res;
      }

      res.add(new CaldavUserNode(new CaldavURI(this.account, true),
                                 getSysi(), null, debug));
      return res;
    }

    return res;
  }

  public Collection getPrincipalCollectionSet(String resourceUri)
         throws WebdavIntfException {
    return getSysi().getPrincipalCollectionSet(resourceUri);
  }

  public Collection getPrincipals(String resourceUri,
                                  PrincipalPropertySearch pps)
          throws WebdavIntfException {
    return sysi.getPrincipals(resourceUri, pps);
  }

  public String makeUserHref(String id) throws WebdavIntfException {
    return namespacePrefix + SysIntf.userPrincipalPrefix + "/" + id + "/";
  }

  /**
   * @param id
   * @return String
   * @throws WebdavIntfException
   */
  public String makeGroupHref(String id) throws WebdavIntfException {
    return namespacePrefix + SysIntf.groupPrincipalPrefix + id + "/";
  }

  /** Object class passed around as we parse access.
   */
  public static class CdAclInfo extends AclInfo {
    String what;

    PrincipalInfo pi;

    boolean notWho;
    int whoType;
    String who;

    ArrayList aces = new ArrayList();
  }

  public AclInfo startAcl(String uri) throws WebdavIntfException {
    CdAclInfo ainfo = new CdAclInfo();

    ainfo.what = uri;

    return ainfo;
  }

  public void parseAcePrincipal(AclInfo ainfo, Node nd,
                                boolean inverted) throws WebdavIntfException {
    CdAclInfo info = (CdAclInfo)ainfo;

    info.notWho = inverted;

    Element el = getOnlyChild(nd);

    info.whoType = -1;
    info.who = null;

    if (MethodBase.nodeMatches(el, WebdavTags.href)) {
      String href = getElementContent(el);

      if ((href == null) || (href.length() == 0)) {
        throw WebdavIntfException.badRequest();
      }
      info.pi = getPrincipalInfo(info.pi, href);
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
        throw WebdavIntfException.badRequest();
      }
    } else if (MethodBase.nodeMatches(el, WebdavTags.self)) {
      info.whoType = Ace.whoTypeUser;
      info.who = account;
    } else {
      throw WebdavIntfException.badRequest();
    }

    if (debug) {
      debugMsg("Parsed ace/principal whoType=" + info.whoType +
               " who=\"" + info.who + "\"");
    }
  }

  public void parsePrivilege(AclInfo ainfo, Node nd,
                             boolean grant) throws WebdavIntfException {
    CdAclInfo info = (CdAclInfo)ainfo;

    if (!grant) {
      // There's probably a way to block this
      throw WebdavIntfException.badRequest();
    }

    Element el = getOnlyChild(nd);

    int priv;

    QName[] privTags = emitAccess.getPrivTags();

    findPriv: {
      // ENUM
      for (priv = 0; priv < privTags.length; priv++) {
        if (MethodBase.nodeMatches(el, privTags[priv])) {
          break findPriv;
        }
      }
      throw WebdavIntfException.badRequest();
    }

    info.aces.add(new Ace(info.who, info.notWho, info.whoType,
                          Privileges.makePriv(priv)));
  }

  public void updateAccess(AclInfo ainfo) throws WebdavIntfException {
    CdAclInfo info = (CdAclInfo)ainfo;

    CaldavBwNode node = (CaldavBwNode)getNode(info.what);

    try {
      if (node.isCollection()) {
        sysi.updateAccess(node.getCDURI().getCal(), info.aces);
      } else {
        sysi.updateAccess(((CaldavComponentNode)node).getEventInfo().getEvent(),
                          info.aces);
      }
    } catch (WebdavIntfException wi) {
      throw wi;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void emitAcl(WebdavNsNode node) throws WebdavIntfException {
    CaldavBwNode uwnode = getBwnode(node);
    CaldavURI cdUri = uwnode.getCDURI();
    Acl acl = null;

    try {
      if (cdUri.isCalendar()) {
        acl = cdUri.getCal().getCurrentAccess().acl;
      } else {
        acl = ((CaldavComponentNode)node).getEventInfo().getCurrentAccess().acl;
      }

      emitAccess.emitAcl(acl);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void emitSupportedPrivSet(WebdavNsNode node) throws WebdavIntfException {
    try {
      emitAccess.emitSupportedPrivSet();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /** This class is the result of interpreting a principal url
   */
  public static class PrincipalInfo {
    int whoType;  // from access.Ace user, group etc
    String who;   // id of user group etc.
  }

  private PrincipalInfo getPrincipalInfo(PrincipalInfo pi, String href)
          throws WebdavIntfException {
    if (pi == null) {
      pi = new PrincipalInfo();
    }

    try {
      URI uri = new URI(href);

      String[] segs = uri.getPath().split("/");

      // First element should be empty, second = "users" or "groups"
      // third is id.

      if ((segs.length != 3) || (segs[0].length() != 0)) {
        throw WebdavIntfException.badRequest();
      }

      if ("users".equals(segs[1])) {
        pi.whoType = Ace.whoTypeUser;
      } else if ("groups".equals(segs[1])) {
        pi.whoType = Ace.whoTypeGroup;
      } else {
        throw WebdavIntfException.badRequest();
      }

      if (segs[2].length() == 0) {
        throw WebdavIntfException.badRequest();
      }

      pi.who = segs[2];
      return pi;
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw WebdavIntfException.badRequest();
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

  /** Generate a response for a single webdav property. This should be overrriden
   * to handle other namespaces.
   *
   * @param node
   * @param pr
   * @return int status
   * @throws WebdavIntfException
   */
  public int generatePropValue(WebdavNsNode node,
                               WebdavProperty pr) throws WebdavIntfException {
    QName tag = pr.getTag();
    String ns = tag.getNamespaceURI();
    int status = HttpServletResponse.SC_OK;

    try {
      /* Deal with webdav properties */
      if ((!ns.equals(CaldavDefs.caldavNamespace) &&
          !ns.equals(CaldavDefs.icalNamespace))) {
        // Not ours
        return super.generatePropValue(node, pr);
      }

      if (tag.equals(CaldavTags.calendarData)) {
        // pr should be a CalendarData object
        if (!(pr instanceof CalendarData)) {
          // XXX software error
          if (debug) {
            warn("!(pr instanceof CalendarData)");
          }
        } else {
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

      if (node.generatePropertyValue(tag, this)) {
        // Generated by node
        return status;
      }

      // Not known
      xml.emptyTag(tag);
      return HttpServletResponse.SC_NOT_FOUND;
    } catch (WebdavIntfException wie) {
      throw wie;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
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
   * @throws WebdavIntfException
   */
  public Collection query(WebdavNsNode wdnode, int retrieveRecur,
                          Filter fltr) throws WebdavIntfException {
    CaldavBwNode node = getBwnode(wdnode);
    Collection events;

    try {
      events = fltr.query(node, retrieveRecur);
    } catch (WebdavException wde) {
      throw new WebdavIntfException(wde.getStatusCode());
    }

    /* We now need to build a node for each of the events in the collection.
       For each event we first determine what calendar it's in. We then take the
       incoming uri, strip any calendar names off it and append the calendar
       name and event name to create the new uri.

       If there is no calendar name for the event we just give it the default.
     */

    Collection evnodes = new ArrayList();
    HashMap evnodeMap = new HashMap();

    try {
      Iterator evit = events.iterator();

      while (evit.hasNext()) {
        EventInfo ei = (EventInfo)evit.next();
        BwEvent ev = ei.getEvent();

        String uri = ev.getCalendar().getPath();

        /* If no name was assigned use the guid */
        String evName = ev.getName();
        if (evName == null) {
          evName = ev.getGuid() + ".ics";
        }

        String evuri = uri + "/" + evName;

        /* See if we've seen this one already - possible for recurring */
        CaldavComponentNode evnode;

        evnode = (CaldavComponentNode)evnodeMap.get(evuri);

        if (evnode == null) {
          evnode = (CaldavComponentNode)getNode(evuri);
        }
        evnode.addEvent(ev);

        evnodes.add(evnode);
        evnodeMap.put(evuri, evnode);
      }

      evnodes = fltr.postFilter(evnodes);
    } catch (Throwable t) {
      error(t);
      throw WebdavIntfException.serverError();
    }

    return evnodes;
  }

  /** The node represents a calendar resource for which we must get free-busy
   * information.
   *
   * @param cnode  CaldavCalNode
   * @param freeBusy
   * @throws WebdavIntfException
   */
  public void getFreeBusy(CaldavCalNode cnode,
                          FreeBusyQuery freeBusy) throws WebdavIntfException {
    try {
      String user = cnode.getCDURI().getOwner();

      BwFreeBusy fb = freeBusy.getFreeBusy(sysi, cnode.getCDURI().getCal(),
                                           user);

      cnode.setFreeBusy(fb);
    } catch (WebdavIntfException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /**
   * @param node
   * @return CaldavBwNode
   * @throws WebdavIntfException
   */
  public CaldavBwNode getBwnode(WebdavNsNode node)
      throws WebdavIntfException {
    if (!(node instanceof CaldavBwNode)) {
      throw new WebdavIntfException("Not a valid node object " +
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

  /*
  private void setAccess(HttpServletRequest request,
                         LuwakState ks) {
    access = ks.getAccess();

    if (access.getAccessSet()) {
      return;
    }

    access.init(ks.getUser(),
            request.isUserInRole(
              props.getProperty("edu.rpi.cct.luwak.role.admin")),
            request.isUserInRole(
              props.getProperty("edu.rpi.cct.luwak.role.chiefeditor")),
            request.isUserInRole(
              props.getProperty("edu.rpi.cct.luwak.role.editor")),
            request.isUserInRole(
              props.getProperty("edu.rpi.cct.luwak.role.writer")),
            request.isUserInRole(
              props.getProperty("edu.rpi.cct.luwak.role.broadcaster")),
            request.isUserInRole(
              props.getProperty("edu.rpi.cct.luwak.role.eventpublisher")),
            debug);
    /** security-role-ref in web.xml doesn't seem to work (or I missed something
        somewhere). Until it does work use the above
    access.setAdminUser(request.isUserInRole("admin"));
    access.setWriterUser(request.isUserInRole("writer")); * /
  }*/

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
   * @param decoded    true if the uri has been decoded
   * @param cal        Supplied CalendarVO object if we already have it.
   * @return CaldavURI object representing the uri
   * @throws WebdavIntfException
   */
  private CaldavURI findURI(String uri, boolean decoded,
                            BwCalendar cal) throws WebdavIntfException {
    try {
      /*Remove all "." and ".." components */
      if (decoded) {
        try {
          uri = new URI(null, null, uri, null).toString();
        } catch (Throwable t) {
          if (debug) {
            error(t);
          }
          throw WebdavIntfException.badRequest();
        }
      }

      uri = new URI(uri).normalize().getPath();
      if (debug) {
        debugMsg("Normalized uri=" + uri);
      }

      try {
        uri = URLDecoder.decode(uri, "UTF-8");
      } catch (Throwable t) {
        if (debug) {
          error(t);
        }
        throw WebdavIntfException.badRequest();
      }

      if (!uri.startsWith("/")) {
        return null;
      }

      if (uri.endsWith("/")) {
        uri = uri.substring(0, uri.length() - 1);
      }

      String[] ss = uri.split("/");
      int pathLength = ss.length - 1;  // First element is empty string
      String namePart = null;

      if (pathLength == 0) {
        throw WebdavIntfException.badRequest();
      }

      int minLength = 1;

      /* If the uri ends with .ics or .ifb split it off
       */
      if (uri.endsWith(".ics") || uri.endsWith(".ifb")) {
        if (pathLength == minLength) {
          throw WebdavIntfException.notFound();
        }

        int pos = uri.lastIndexOf("/");
        if (pos < 0) {
          // bad uri
          throw WebdavIntfException.badRequest();
        }

        namePart = uri.substring(pos + 1);
        uri = uri.substring(0, pos);
      }

      if (cal == null) {
        // See if the full path is in the map
        CaldavURI curi = getUriPath(uri);
        if (curi != null) {
          /* We've avoided a search down the path - can we use this caldavuri object
           */

          if (curi.sameName(namePart)) {
            if (debug) {
              debugMsg("reuse uri - cal=\"" + curi.getPath() +
                       "\" entityName=\"" + namePart + "\"");
            }
            return curi;
          }

          if (debug) {
            debugMsg("create uri from mapped uri -" +
                     " cal=\"" + curi.getPath() +
                     "\" entityName=\"" + namePart + "\"");
          }

          if (curi.isUser() || curi.isGroup()) {
            curi = new CaldavURI(namePart, curi.isUser());
          } else {
            curi = new CaldavURI(curi.getCal(), namePart);
          }
          putUriPath(curi);

          return curi;
        }

        // Search to see if the uri exists
        if (debug) {
          trace("SEARCH: for " + uri);
        }

        if (uri.startsWith(SysIntf.userPrincipalPrefix)) {
          if (!sysi.validUser(namePart)) {
            throw WebdavIntfException.forbidden();
          }

          curi = new CaldavURI(namePart, true);
          putUriPath(curi);

          return curi;
        }

        if (uri.startsWith(SysIntf.groupPrincipalPrefix)) {
          if (!sysi.validGroup(namePart)) {
            throw WebdavIntfException.forbidden();
          }

          curi = new CaldavURI(namePart, false);
          putUriPath(curi);

          return curi;
        }

        /* uri for data objects */
        cal = sysi.getCalendar(uri);

        if (cal == null) {
          if (namePart != null) {
            throw WebdavIntfException.notFound();
          }
          /* Try removing the last element - this could be mkcalendar/mkcol
           */
          if (pathLength == minLength) {
            throw WebdavIntfException.notFound();
          }

          int pos = uri.lastIndexOf("/");
          if (pos < 0) {
            // bad uri
            throw WebdavIntfException.notFound();
          }

          namePart = uri.substring(pos + 1);
          uri = uri.substring(0, pos);

          // See if the uri exists
          if (debug) {
            trace("SEARCH: for calendar " + uri);
          }
          cal = sysi.getCalendar(uri);
        }

        if (cal == null) {
          throw WebdavIntfException.notFound();
        }
      }

      /* The situation now is that the uri is the path of a collection that
         exists and that we have access to.

         namePart, if not null, represents an entity we are going to access
         or create, or possibly a calendar we want to create.
       */

      if (debug) {
        debugMsg("create uri - cal=\"" + cal.getPath() +
                 "\" entityName=\"" + namePart + "\"");
      }
      CaldavURI curi = new CaldavURI(cal, namePart);
      putUriPath(curi);

      return curi;
    } catch (WebdavIntfException wi) {
      throw wi;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  private CaldavURI getUriPath(String path) {
    return (CaldavURI)uriMap.get(path);
  }

  private void putUriPath(CaldavURI wi) {
    uriMap.put(wi.getPath(), wi);
  }
}
