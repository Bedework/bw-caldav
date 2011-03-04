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
package org.bedework.caldav.util.filter.parse;

import org.bedework.caldav.util.ParseUtil;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavDefs;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
public class QueryFilter {
  protected boolean debug;

  protected transient Logger log;

  /** The root element.
   */
  protected CompFilter filter;

  /** Constructor
   *
   * @param debug
   */
  public QueryFilter(final boolean debug) {
    this.debug = debug;
  }

  /** Given a caldav like xml filter parse it
   *
   * @param xmlStr
   * @param tzid timezone from request - may be null
   * @throws WebdavException
   */
  public void parse(final String xmlStr,
                    final String tzid) throws WebdavException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

      parse(doc.getDocumentElement(), tzid);
    } catch (WebdavException cfe) {
      throw cfe;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** The given node must be the Filter element
   *
   * @param nd
   * @param tzid timezone from request - may be null
   * @throws WebdavException
   */
  public void parse(final Node nd,
                    final String tzid) throws WebdavException {
    /* We expect exactly one comp-filter child. */

    ChildArray ca = getChildren(nd);

    if ((ca.length() != 1) ||
        (!XmlUtil.nodeMatches(ca.children[0], CaldavTags.compFilter))) {
      StringBuilder sb = new StringBuilder();

      if (ca.length() == 0) {
        sb.append("<filter>: no children");
      } else if (ca.length() == 1) {
        sb.append("<filter>: child error: " + ca.children[0]);
      } else {
        sb.append("<filter>: child error, count: " + ca.length() +
                  " node[0]: " + ca.children[0] +
                  " node[1]: " + ca.children[1]);
      }
      if (debug) {
        trace(sb.toString());
      }
      throw new WebdavBadRequest(sb.toString());
    }

    filter = parseCompFilter(ca.children[0], tzid);
  }

  /** Return an object encapsulating the filter query.
   *
   * @return EventQuery
   * @throws WebdavException
   */
  public EventQuery getQuery() throws WebdavException {
    EventQuery eventq = new EventQuery();

    eventq.filter = filter.getQueryFilter(eventq, 0);

    return eventq;
  }

  /** The given node must be a comp-filter element
   *    <!ELEMENT comp-filter (is-not-defined | (time-range?,
   *                            prop-filter*, comp-filter*))>
   *
   *    <!ATTLIST comp-filter name CDATA #REQUIRED>
   *
   * @param nd
   * @param tzid used to resolve floating times.
   * @return CompFilter
   * @throws WebdavException
   */
  private CompFilter parseCompFilter(final Node nd,
                                     final String tzid) throws WebdavException {
    CompFilter cf = new CompFilter(getOnlyAttrVal(nd, "name"));

    ChildArray ca = getChildren(nd);

    if (ca.length() == 0) {
      // Empty
      return cf;
    }

    if ((ca.length() == 1) &&
        XmlUtil.nodeMatches(ca.children[0], CaldavTags.isNotDefined)) {
      cf.setIsNotDefined(true);
      return cf;
    }

    QName isDefined = new QName(CaldavDefs.caldavNamespace,
                                "is-defined");

    Node curnode = ca.next();

    if (XmlUtil.nodeMatches(curnode, isDefined)) {
      // Probably out of date evolution - ignore it
      curnode = ca.next();
    }

    /* (time-range?, prop-filter*, comp-filter*) */

    try {
      if ((curnode != null) &&
          XmlUtil.nodeMatches(curnode, CaldavTags.timeRange)) {
        cf.setTimeRange(ParseUtil.parseTimeRange(curnode, false));

        if (cf.getTimeRange() == null) {
          return null;
        }

        cf.getTimeRange().setTzid(tzid);

        curnode = ca.next();
      }

      while ((curnode != null) &&
             XmlUtil.nodeMatches(curnode, CaldavTags.propFilter)) {
        PropFilter chpf = parsePropFilter(curnode);

        cf.addPropFilter(chpf);

        curnode = ca.next();
      }

      while ((curnode != null) &&
             XmlUtil.nodeMatches(curnode, CaldavTags.compFilter)) {
        CompFilter chcf = parseCompFilter(curnode, tzid);

        if (chcf == null) {
          return null;
        }

        cf.addCompFilter(chcf);

        curnode = ca.next();
      }

      if (curnode != null) {
        throw new WebdavBadRequest();
      }
    } catch (WebdavException cfe) {
      throw cfe;
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
  private PropFilter parsePropFilter(final Node nd) throws WebdavException {
    try {
      PropFilter pf = new PropFilter(getOnlyAttrVal(nd, "name"));

      ChildArray ca = getChildren(nd);

      if (ca.length() == 0) {
        // Presence filter
        return pf;
      }

      Node curnode = ca.next();

      if (XmlUtil.nodeMatches(curnode, CaldavTags.isNotDefined)) {
        pf.setIsNotDefined(true);
        curnode = ca.next();
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.timeRange)) {
        pf.setTimeRange(ParseUtil.parseTimeRange(curnode, false));
        curnode = ca.next();
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.textMatch)) {
        pf.setMatch(parseTextMatch(curnode));
        curnode = ca.next();
      }

      while (curnode != null) {
        // Can only have param-filter*
        if (!XmlUtil.nodeMatches(curnode, CaldavTags.paramFilter)) {
          throw new WebdavBadRequest();
        }

        ParamFilter parf = parseParamFilter(curnode);

        pf.addParamFilter(parf);

        curnode = ca.next();
      }

      return pf;
    } catch (WebdavException cfe) {
      throw cfe;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* The given node must be a param-filter element
   *    <!ELEMENT param-filter (is-not-defined | text-match) >
   *
   *    <!ATTLIST param-filter name CDATA #REQUIRED>
   */
  private ParamFilter parseParamFilter(final Node nd) throws WebdavException {
    String name = getOnlyAttrVal(nd, "name");

    // Only one child - either is-defined | text-match
    Element child = getOnlyChild(nd);

    if (debug) {
      trace("paramFilter element: " +
            child.getNamespaceURI() + " " +
            child.getLocalName());
    }

    if (XmlUtil.nodeMatches(child, CaldavTags.isNotDefined)) {
      return new ParamFilter(name, true);
    }

    if (XmlUtil.nodeMatches(child, CaldavTags.textMatch)) {
      TextMatch match = parseTextMatch(child);

      return new ParamFilter(name, match);
    }

    throw new WebdavBadRequest();
  }

  /* The given node must be a text-match element
   *  <!ELEMENT text-match #PCDATA>
   *
   *  <!ATTLIST text-match caseless (yes|no)>
   */
  private TextMatch parseTextMatch(final Node nd) throws WebdavException {
    //int numAttrs = XmlUtil.numAttrs(nd);
    int numValid = 0;

    Boolean caseless = null;
    caseless = yesNoAttr(nd, "caseless");
    if (caseless != null) {
      numValid++;
    }

    Boolean tempBool = null;
    boolean negated = false;
    tempBool = yesNoAttr(nd, "negate-condition");
    if (tempBool != null) {
      numValid++;
      negated = tempBool;
    }

    /*
    if (numAttrs != numValid) {
      throw new WebdavBadRequest();
    }
    */

    try {
      return new TextMatch(caseless, negated, XmlUtil.getReqOneNodeVal(nd));
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }
  }

  private class ChildArray {
    int cur;
    Element[] children;

    Element next() {
      if (cur >= children.length) {
        return null;
      }

      Element e = children[cur];
      cur++;

      if (debug) {
        trace("compFilter element: " +
            e.getNamespaceURI() + ":" +
            e.getLocalName());
      }

      return e;
    }

    int length() {
      return children.length;
    }
  }

  private ChildArray getChildren(final Node nd) throws WebdavException {
    try {
      ChildArray ca = new ChildArray();
      ca.children = XmlUtil.getElementsArray(nd);

      return ca;
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  private Element getOnlyChild(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getOnlyElement(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  private String getOnlyAttrVal(final Node nd, final String name) throws WebdavException {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() != 1)) {
      throw new WebdavBadRequest("Missing comp-filter name");
    }

    String res = XmlUtil.getAttrVal(nnm, name);
    if (res == null) {
      throw new WebdavBadRequest("Missing comp-filter name");
    }

    return res;
  }

  private Boolean yesNoAttr(final Node nd, final String name) throws WebdavException {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() == 0)) {
      return null;
    }

    try {
      return XmlUtil.getYesNoAttrVal(nnm, name);
    } catch (Throwable t) {
      throw new WebdavBadRequest(t.getMessage());
    }
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

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

