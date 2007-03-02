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

package org.bedework.caldav.server.filter;
import org.bedework.caldav.server.CalDavParseUtil;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.TimeRange;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.sss.util.xml.QName;
import edu.rpi.sss.util.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.model.Component;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Class to represent a calendar-query filter
 *  <pre>
 *   <!ELEMENT filter comp-filter>
 *
 *   10.3.1 CALDAV:comp-filter XML Element
 *
 *    Name:
 *        comp-filter
 *    Namespace:
 *        urn:ietf:params:xml:ns:caldav
 *    Purpose:
 *        Limits the search to only the chosen component types.
 *    Description:
 *        The "name" attribute is an iCalendar component type (e.g., "VEVENT").
 *        When this element is present, the server should only return a component
 *        if it matches the filter, which is to say:
 *
 *       A component of the type specified by the "name" attribute
 *       exists, and the CALDAV:comp-filter is empty, OR
 *
 *       it contains at least one recurrence instance scheduled to overlap a
 *       given time range if a CALDAV:time-range XML element is specified, and
 *       that any CALDAV:prop-filter and CALDAV:comp-filter child elements
 *       also match.
 *
 *
 *    <!ELEMENT comp-filter (is-not-defined | (time-range?,
 *                              prop-filter*, comp-filter*))>
 *
 *    <!ATTLIST comp-filter name CDATA #REQUIRED>
 *
 *    10.3.2 CALDAV:prop-filter XML Element
 *
 *    Name:
 *        prop-filter
 *    Namespace:
 *        urn:ietf:params:xml:ns:caldav
 *    Purpose:
 *        Limits the search to specific properties.
 *    Description:
 *        The "name" attribute MUST contain an iCalendar property name
 *        (e.g., "ATTENDEE"). When the 'prop-filter' executes, a property matches if:
 *
 *       A property of the type specified by the "name" attribute
 *       exists, and the CALDAV:prop-filter is empty, OR it matches the
 *       CALDAV:time-range XML element or CALDAV:text-match conditions
 *       if specified, and that any CALDAV:param-filter child elements
 *       also match.
 *
 *    or:
 *
 *       A property of the type specified by the "name" attribute does
 *       not exist, and the CALDAV:is-not-defined element is specified.
 *
 *    <!ELEMENT prop-filter ((is-not-defined |
 *                              ((time-range | text-match)?,
 *                               param-filter*))>
 *
 *    <!ATTLIST prop-filter name CDATA #REQUIRED>
 *
 *    10.3.3 CALDAV:param-filter XML Element
 *
 *    Name:
 *        param-filter
 *    Namespace:
 *        urn:ietf:params:xml:ns:caldav
 *    Purpose:
 *        Limits the search to specific parameters.
 *    Description:
 *        The "param-filter" element limits the search result to the set of resources containing properties with parameters that meet the parameter filter rules. When this filter executes, a parameter matches if:
 *
 *    ("is-defined matches" OR "text-match matches")
 *
 *    <!ELEMENT param-filter (is-defined | text-match) >
 *
 *    <!ATTLIST param-filter name CDATA #REQUIRED>
 *
 * </pre>
 *
 *   @author Mike Douglass   douglm @ rpi.edu
 */
public class Filter {
  private boolean debug;

  private CaldavBWIntf intf;

  protected transient Logger log;

  /** The root element.
   */
  private CompFilter filter;

  /* Set by internal methods to indicate failure reason.
   */
  private int status;

  static class EventQuery {
    /** */
    public BwFilter filter;

    /** */
    public TimeRange trange;

    //Collection propFilters;

    /* true if we have to postfilter the result obtained via a search
     */
    boolean postFilter;

    /** If non-null apply to retrieved event components
     */
    public Collection<PropFilter> eventFilters;

    /** If non-null apply to retrieved tod components
     */
    public Collection<PropFilter> todoFilters;

    /** If non-null apply to retrieved journal components
     */
    public Collection<PropFilter> journalFilters;

    /** If non-null apply to retrieved alarm components
     */
    public Collection<PropFilter> alarmFilters;
  }

  /* Query we executed */
  private EventQuery eventq;

  /** Constructor
   *
   * @param intf
   * @param debug
   */
  public Filter(WebdavNsIntf intf, boolean debug) {
    this.intf = (CaldavBWIntf)intf;
    this.debug = debug;
  }

  /** The given node must be the Filter element
   *
   * @param nd
   * @return int     htp status
   * @throws WebdavException
   */
  public int parse(Node nd) throws WebdavException {
    try {
      /* We expect exactly one comp-filter child. */

      Element[] children = getChildren(nd);

      if ((children.length != 1) ||
          (!MethodBase.nodeMatches(children[0], CaldavTags.compFilter))) {
        if (debug) {
          trace("<filter>: child count error: " + children.length);
        }
        throw new WebdavBadRequest();
      }

      filter = parseCompFilter((Node)children[0]);

      if (filter == null) {
        return status;
      }
    } catch (WebdavBadRequest wbr) {
      return wbr.getStatusCode();
    }

    return HttpServletResponse.SC_OK;
  }

  /** Use the given query to return a collection of nodes. An exception will
   * be raised if the entire query fails for some reason (access, etc). An
   * empty collection will be returned if no objects match.
   *
   * @param wdnode    WebdavNsNode defining root of search
   * @param retrieveRecur  How we retrieve recurring events
   * @return Collection of result nodes (empty for no result)
   * @throws WebdavException
   */
  public Collection<EventInfo> query(CaldavBwNode wdnode,
                                     RecurringRetrievalMode retrieveRecur) throws WebdavException {
    eventq = new EventQuery();

    eventq.filter = filter.getQuery(eventq, 0);

    if (debug) {
      if (eventq.trange == null) {
        trace("No time-range specified for uri " + wdnode.getUri());
      } else {
        trace("time-range specified for uri " + wdnode.getUri() +
              " with start=" + eventq.trange.getStart() +
              " end=" + eventq.trange.getEnd());
      }
    }

    Collection<EventInfo> events;

    try {
      BwDateTime start = null;
      BwDateTime end = null;
      if (eventq.trange != null) {
        start = eventq.trange.getStart();
        end = eventq.trange.getEnd();
      }

      events = wdnode.getSysi().getEvents(wdnode.getCDURI().getCal(),
                                          eventq.filter,
                                          start, end, retrieveRecur);
    } catch (Throwable t) {
      error(t);
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    if (debug) {
      trace("Query returned " + events.size());
    }

    return events;
  }

  /** Carry out any postfiltering on the collection of initialised nodes,
   *
   * @param nodes     Collection of initialised CaldavBwNode
   * @return Collection of filtered nodes (empty for no result)
   * @throws WebdavException
   */
  public Collection<WebdavNsNode> postFilter(
                   Collection<WebdavNsNode> nodes) throws WebdavException {
    if (!eventq.postFilter) {
      return nodes;
    }

    if (debug) {
      trace("post filtering needed");
    }

    CompFilter cfltr = filter;

    // Currently only handle VCALENDAR for top level.
    if (!"VCALENDAR".equals(cfltr.getName())) {
      return new ArrayList<WebdavNsNode>();
    }

    ArrayList<WebdavNsNode> filtered = new ArrayList<WebdavNsNode>();

    for (WebdavNsNode node: nodes) {
      CaldavComponentNode curnode = null;

      if (!(node instanceof CaldavComponentNode)) {
        // Cannot match to anything - don't pass it?
      } else {
        curnode = (CaldavComponentNode)node;

        int entityType = curnode.getEventInfo().getEvent().getEntityType();

        Collection<PropFilter> pfs = null;

        if (entityType == CalFacadeDefs.entityTypeEvent) {
          pfs = eventq.eventFilters;
        } else if (entityType == CalFacadeDefs.entityTypeTodo) {
          pfs = eventq.todoFilters;
        } else if (entityType == CalFacadeDefs.entityTypeJournal) {
          pfs = eventq.journalFilters;
        //} else if (entityType == CalFacadeDefs.entityTypeAlarm) {
        //  pfs = addPropFilter(eq.alarmFilters, pf);
        }

        if (!WebdavUtils.emptyCollection(pfs)) {
          Component comp = curnode.getVevent();

          for (PropFilter pf: pfs) {
            if (pf.filter(comp)) {
              filtered.add(curnode);
              break;
            }
          }
        }
      }
    }

    return filtered;
  }

  /** The given node must be a comp-filter element
   *    <!ELEMENT comp-filter (is-not-defined | (time-range?,
   *                            prop-filter*, comp-filter*))>
   *
   *    <!ATTLIST comp-filter name CDATA #REQUIRED>
   *
   * @param nd
   * @return CompFilter
   * @throws WebdavException
   */
  private CompFilter parseCompFilter(Node nd) throws WebdavException {
    String name = getOnlyAttrVal(nd, "name");
    if (name == null) {
      throw new WebdavBadRequest("Missing comp-filter name");
    }

    CompFilter cf = new CompFilter(name);

    Element[] children = getChildren(nd);

    if (children.length == 0) {
      // Empty
      return cf;
    }

    if ((children.length == 1) &&
        MethodBase.nodeMatches(children[0], CaldavTags.isNotDefined)) {
      cf.setIsNotDefined(true);
      return cf;
    }

    /* (time-range?, prop-filter*, comp-filter*) */

    try {
      for (int i = 0; i < children.length; i++) {
        Node curnode = children[i];

        if (debug) {
          trace("compFilter element: " +
              curnode.getNamespaceURI() + ":" +
              curnode.getLocalName());
        }

        QName isDefined = new QName(CaldavDefs.caldavNamespace,
                                    "is-defined");
        if (MethodBase.nodeMatches(curnode, isDefined)) {
          // Probably out of date evolution - ignore it
        } else if (MethodBase.nodeMatches(curnode, CaldavTags.timeRange)) {
          cf.setTimeRange(CalDavParseUtil.parseTimeRange(curnode,
                          intf.getSysi().getTimezones()));

          if (cf.getTimeRange() == null) {
            return null;
          }
        } else if (MethodBase.nodeMatches(curnode, CaldavTags.compFilter)) {
          CompFilter chcf = parseCompFilter(curnode);

          if (chcf == null) {
            return null;
          }

          cf.addCompFilter(chcf);
        } else if (MethodBase.nodeMatches(curnode, CaldavTags.propFilter)) {
          PropFilter chpf = parsePropFilter(curnode);

          if (chpf == null) {
            return null;
          }

          cf.addPropFilter(chpf);
        } else {
          throw new WebdavBadRequest();
        }
      }
    } catch (WebdavBadRequest wbr) {
      throw wbr;
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }

    return cf;
  }

  /* The given node must be a prop-filter element
   *    <!ELEMENT prop-filter (is-not-defined | time-range | text-match)?
   *                            param-filter*>
   *
   *    <!ATTLIST prop-filter name CDATA #REQUIRED>
   */
  private PropFilter parsePropFilter(Node nd) throws WebdavException {
    String name = getOnlyAttrVal(nd, "name");
    if (name == null) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }

    PropFilter pf = new PropFilter(name);

    Element[] children = getChildren(nd);
    boolean idTrTm = false; // flag is-defined | time-range | text-match

    try {
      for (int i = 0; i < children.length; i++) {
        Node curnode = children[i];

        if (debug) {
          trace("propFilter element: " +
              curnode.getNamespaceURI() + " " +
              curnode.getLocalName());
        }

        if (idTrTm) {
          // Only have param-filter*
          if (MethodBase.nodeMatches(curnode, CaldavTags.paramFilter)) {
            ParamFilter parf = parseParamFilter(curnode);

            if (parf == null) {
              return null;
            }

            pf.addParamFilter(parf);
          } else {
            status = HttpServletResponse.SC_BAD_REQUEST;
            return null;
          }
        } else {
          idTrTm = true;

          // one of is-defined | time-range | text-match
          if (MethodBase.nodeMatches(curnode, CaldavTags.isNotDefined)) {
            pf.setIsNotDefined(true);
          } else if (MethodBase.nodeMatches(curnode, CaldavTags.timeRange)) {
            pf.setTimeRange(CalDavParseUtil.parseTimeRange(curnode,
                                               intf.getSysi().getTimezones()));
          } else if (MethodBase.nodeMatches(curnode, CaldavTags.textMatch)) {
            pf.setMatch(parseTextMatch(curnode));

            if (pf.getMatch() == null) {
              return null;
            }
          } else {
          }
        }
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return pf;
  }

  /* The given node must be a param-filter element
   *    <!ELEMENT param-filter (is-not-defined | text-match) >
   *
   *    <!ATTLIST param-filter name CDATA #REQUIRED>
   */
  private ParamFilter parseParamFilter(Node nd) throws WebdavException {
    String name = getOnlyAttrVal(nd, "name");
    if (name == null) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }

    // Only one child - either is-defined | text-match
    Element child = getOnlyChild(nd);

    if (debug) {
      trace("paramFilter element: " +
            child.getNamespaceURI() + " " +
            child.getLocalName());
    }

    if (MethodBase.nodeMatches(child, CaldavTags.isNotDefined)) {
      return new ParamFilter(name, true);
    }

    if (MethodBase.nodeMatches(child, CaldavTags.textMatch)) {
      TextMatch match = parseTextMatch(child);

      if (match == null) {
        return null;
      }

      return new ParamFilter(name, match);
    }

    status = HttpServletResponse.SC_BAD_REQUEST;
    return null;
  }

  /* The given node must be a text-match element
   *  <!ELEMENT text-match #PCDATA>
   *
   *  <!ATTLIST text-match caseless (yes|no)>
   */
  private TextMatch parseTextMatch(Node nd) throws WebdavException {
    int numAttrs = XmlUtil.numAttrs(nd);

    Boolean caseless = null;
    if (numAttrs == 1) {
      caseless = yesNoAttr(nd, "caseless");
    }

    try {
      return new TextMatch(caseless, XmlUtil.getReqOneNodeVal(nd));
    } catch (Throwable t) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }
  }

  private Element[] getChildren(Node nd) {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }
  }

  private Element getOnlyChild(Node nd) {
    try {
      return XmlUtil.getOnlyElement(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }
  }

  /** Fetch required attribute. Return null for error
   *
   * @param nd
   * @param name
   * @return String
   */
  public String getOnlyAttrVal(Node nd, String name) {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() != 1)) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }

    String res = XmlUtil.getAttrVal(nnm, name);
    if (res == null) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }

    return res;
  }

  /** Fetch required yes/no Return null for error
   *
   * @param nd
   * @param name
   * @return Boolean
   */
  public Boolean yesNoAttr(Node nd, String name) {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() == 0)) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }

    Boolean res = null;

    try {
      res = XmlUtil.getYesNoAttrVal(nnm, name);
    } catch (Throwable t) {}

    if (res == null) {
      status = HttpServletResponse.SC_BAD_REQUEST;
      return null;
    }

    return res;
  }

  /** ===================================================================
   *                   Dump methods
   *  =================================================================== */

  public void dump() {
    trace("  <filter>");
    filter.dump(getLogger(), "    ");
    trace("  </filter>");
  }

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(String msg) {
    getLogger().debug(msg);
  }

  protected void error(Throwable t) {
    getLogger().error(this, t);
  }

  protected void logIt(String msg) {
    getLogger().info(msg);
  }

  protected void trace(String msg) {
    getLogger().debug(msg);
  }
}

