/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
  public QueryFilter(boolean debug) {
    this.debug = debug;
  }

  /** Given a caldav like xml filter parse it
   *
   * @param xmlStr
   * @param tzid timezone from request - may be null
   * @throws WebdavException
   */
  public void parse(String xmlStr,
                    String tzid) throws WebdavException {
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
  public void parse(Node nd,
                    String tzid) throws WebdavException {
    /* We expect exactly one comp-filter child. */

    Element[] children = getChildren(nd);

    if ((children.length != 1) ||
        (!XmlUtil.nodeMatches(children[0], CaldavTags.compFilter))) {
      StringBuilder sb = new StringBuilder();

      if (children.length == 0) {
        sb.append("<filter>: no children");
      } else if (children.length == 1) {
        sb.append("<filter>: child error: " + children[0]);
      } else {
        sb.append("<filter>: child error, count: " + children.length +
                  " node[0]: " + children[0] +
                  " node[1]: " + children[1]);
      }
      if (debug) {
        trace(sb.toString());
      }
      throw new WebdavBadRequest(sb.toString());
    }

    filter = parseCompFilter((Node)children[0], tzid);
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
  private CompFilter parseCompFilter(Node nd,
                                     String tzid) throws WebdavException {
    CompFilter cf = new CompFilter(getOnlyAttrVal(nd, "name"));

    Element[] children = getChildren(nd);

    if (children.length == 0) {
      // Empty
      return cf;
    }

    if ((children.length == 1) &&
        XmlUtil.nodeMatches(children[0], CaldavTags.isNotDefined)) {
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
        if (XmlUtil.nodeMatches(curnode, isDefined)) {
          // Probably out of date evolution - ignore it
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.timeRange)) {
          cf.setTimeRange(ParseUtil.parseTimeRange(curnode, false));

          if (cf.getTimeRange() == null) {
            return null;
          }

          cf.getTimeRange().setTzid(tzid);
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.compFilter)) {
          CompFilter chcf = parseCompFilter(curnode, tzid);

          if (chcf == null) {
            return null;
          }

          cf.addCompFilter(chcf);
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.propFilter)) {
          PropFilter chpf = parsePropFilter(curnode);

          cf.addPropFilter(chpf);
        } else {
          throw new WebdavBadRequest();
        }
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
  private PropFilter parsePropFilter(Node nd) throws WebdavException {
    try {
      PropFilter pf = new PropFilter(getOnlyAttrVal(nd, "name"));

      Element[] children = getChildren(nd);

      if (children.length == 0) {
        // Presence filter
        return pf;
      }

      int i = 0;
      Node curnode = children[i];

      if (XmlUtil.nodeMatches(curnode, CaldavTags.isNotDefined)) {
        pf.setIsNotDefined(true);
        i++;
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.timeRange)) {
        pf.setTimeRange(ParseUtil.parseTimeRange(curnode, false));
        i++;
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.textMatch)) {
        pf.setMatch(parseTextMatch(curnode));
        i++;
      }

      if (debug) {
        trace("propFilter element: " +
              curnode.getNamespaceURI() + " " +
              curnode.getLocalName());
      }

      while (i < children.length) {
        curnode = children[i];
        if (debug) {
          trace("propFilter element: " +
                curnode.getNamespaceURI() + " " +
                curnode.getLocalName());
        }

        // Can only have param-filter*
        if (!XmlUtil.nodeMatches(curnode, CaldavTags.paramFilter)) {
          throw new WebdavBadRequest();
        }

        ParamFilter parf = parseParamFilter(curnode);

        pf.addParamFilter(parf);
        i++;
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
  private ParamFilter parseParamFilter(Node nd) throws WebdavException {
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
  private TextMatch parseTextMatch(Node nd) throws WebdavException {
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

  private Element[] getChildren(Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  private Element getOnlyChild(Node nd) throws WebdavException {
    try {
      return XmlUtil.getOnlyElement(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  private String getOnlyAttrVal(Node nd, String name) throws WebdavException {
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

  private Boolean yesNoAttr(Node nd, String name) throws WebdavException {
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

