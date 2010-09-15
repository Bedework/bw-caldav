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
package org.bedework.caldav.server.calquery;

import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.util.ParseUtil;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class to represent a calendar-query calendar-data element
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CalendarData extends WebdavProperty {
  /*
      <!ELEMENT calendar-data ((comp?, (expand |
                                           limit-recurrence-set)?,
                                           limit-freebusy-set?) |
                          #PCDATA)?>

         pcdata is for response

      <!ATTLIST calendar-data return-content-type CDATA
                      "text/calendar">

      <!ELEMENT comp ((allcomp, (allprop | prop*)) |
                       (comp*, (allprop | prop*)))>

      <!ATTLIST comp name CDATA #REQUIRED>

      <!ELEMENT allcomp EMPTY>

      <!ELEMENT allprop EMPTY>

      <!ELEMENT prop EMPTY>

      <!ATTLIST prop name CDATA #REQUIRED
                     novalue (yes|no) "no">

      <!ELEMENT expand-recurrence-set EMPTY>

      <!ATTLIST expand-recurrence-set start CDATA #REQUIRED
                                      end CDATA #REQUIRED>
----------------------------------------------------------------------
         <!ELEMENT calendar-data ((comp?, (expand |
                                           limit-recurrence-set)?,
                                           limit-freebusy-set?) |
                                  #PCDATA)?>
         PCDATA value: iCalendar object

         <!ATTLIST calendar-data content-type CDATA "text/calendar">
                                 version CDATA "2.0">
         content-type value: a MIME media type
         version value: a version string

         <!ELEMENT comp ((allprop | prop*), (allcomp | comp*))>

         <!ATTLIST comp name CDATA #REQUIRED>
         name value: a calendar component name

         <!ELEMENT allcomp EMPTY>

         <!ELEMENT allprop EMPTY>

         <!ELEMENT prop EMPTY>

         <!ATTLIST prop name CDATA #REQUIRED
                     novalue (yes | no) "no">
         name value: a calendar property name
         novalue value: "yes" or "no"

         <!ELEMENT expand EMPTY>

         <!ATTLIST expand start CDATA #REQUIRED
                         end   CDATA #REQUIRED>
         start value: an iCalendar "date with UTC time"
         end value: an iCalendar "date with UTC time"

         <!ELEMENT limit-recurrence-set EMPTY>

         <!ATTLIST limit-recurrence-set start CDATA #REQUIRED
                                       end   CDATA #REQUIRED>
         start value: an iCalendar "date with UTC time"
         end value: an iCalendar "date with UTC time"

         <!ELEMENT limit-freebusy-set EMPTY>

         <!ATTLIST limit-freebusy-set start CDATA #REQUIRED
                                     end   CDATA #REQUIRED>
         start value: an iCalendar "date with UTC time"
         end value: an iCalendar "date with UTC time"

   */
  private boolean debug;

  protected transient Logger log;

  private String returnContentType; // null for defaulted
  private Comp comp;
  private Expand ers;
  private LimitRecurrenceSet lrs;
  private LimitFreebusySet lfs;

  /** Constructor
   *
   * @param tag  QName name
   * @param debug
   */
  public CalendarData(final QName tag,
                      final boolean debug) {
    super(tag, null);
    this.debug = debug;
  }

  /**
   * @return String returnContentType
   */
  public String getReturnContentType() {
    return returnContentType;
  }

  /**
   * @return Comp
   */
  public Comp getComp() {
    return comp;
  }

  /**
   * @return ExpandRecurrenceSet
   */
  public Expand getErs() {
    return ers;
  }

  /**
   * @return LimitRecurrenceSet
   */
  public LimitRecurrenceSet getLrs() {
    return lrs;
  }

  /**
   * @return LimitFreebusySet
   */
  public LimitFreebusySet getLfs() {
    return lfs;
  }

  /** The given node must be the Filter element
   *
   * @param nd
   * @throws WebdavException
   */
  public void parse(final Node nd) throws WebdavException {
    /* Either empty - show everything or
              comp + optional (expand-recurrence-set or
                               limit-recurrence-set)
     */
    NamedNodeMap nnm = nd.getAttributes();

    if (nnm != null) {
      for (int nnmi = 0; nnmi < nnm.getLength(); nnmi++) {
        Node attr = nnm.item(nnmi);

        if (attr.getNodeName().equals("return-content-type")) {
          returnContentType = attr.getNodeValue();
          if (returnContentType == null) {
            throw new WebdavBadRequest();
          }
        } else if (attr.getNodeName().equals("xmlns")) {
        } else {
          // Bad attribute(s)
          throw new WebdavBadRequest();
        }
      }
    }

    Element[] children = getChildren(nd);

    try {
      for (int i = 0; i < children.length; i++) {
        Node curnode = children[i];

        if (debug) {
          trace("calendar-data node type: " +
              curnode.getNodeType() + " name:" +
              curnode.getNodeName());
        }

        if (XmlUtil.nodeMatches(curnode, CaldavTags.comp)) {
          if (comp != null) {
            throw new WebdavBadRequest();
          }

          comp = parseComp(curnode);
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.expand)) {
          if (ers != null) {
            throw new WebdavBadRequest();
          }

          ers = new Expand(ParseUtil.parseTimeRange(curnode, true));
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.limitRecurrenceSet)) {
          if (lrs != null) {
            throw new WebdavBadRequest();
          }

          lrs = new LimitRecurrenceSet(ParseUtil.parseTimeRange(curnode, true));
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.limitFreebusySet)) {
          if (lfs != null) {
            throw new WebdavBadRequest();
          }

          lfs = new LimitFreebusySet(ParseUtil.parseTimeRange(curnode, true));
        } else {
          throw new WebdavBadRequest();
        }
      }
    } catch (WebdavBadRequest wbr) {
      throw wbr;
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }
  }

  /** Given the CaldavBwNode, returns the transformed content.
   *
   * @param wdnode
   * @param xml
   * @throws WebdavException
   */
  public void process(final WebdavNsNode wdnode,
                      final XmlEmit xml) throws WebdavException {
    if (!(wdnode instanceof CaldavComponentNode)) {
      return;
    }

    CaldavComponentNode node = (CaldavComponentNode)wdnode;

    if (comp == null) {
      node.writeContent(xml, null, returnContentType);
      return;
    }

    /** Ensure node exists */
    node.init(true);
    if (!node.getExists()) {
      throw new WebdavException(HttpServletResponse.SC_NOT_FOUND);
    }

    // Top level must be VCALENDAR at this point?
    if (!"VCALENDAR".equals(comp.getName())) {
      throw new WebdavBadRequest();
    }

    if (comp.getAllcomp()) {
      node.writeContent(xml, null, returnContentType);
      return;
    }

    // Assume all properties for that level.

    // Currently we only handle VEVENT -
    // If there's no VEVENT element what does that imply?

    Iterator it = comp.getComps().iterator();

    while (it.hasNext()) {
      Comp subcomp = (Comp)it.next();

      if ("VEVENT".equals(subcomp.getName())) {
        if (subcomp.getAllprop()) {
          node.writeContent(xml, null, returnContentType);
          return;
        }

        try {
          xml.cdataValue(transformVevent(node.getIcal(), subcomp.getProps()));
        } catch (IOException ioe) {
          throw new WebdavException(ioe);
        }
        return;
      }
    }

    // No special instructions.

    node.writeContent(xml, null, returnContentType);
  }

  /* Transform one or more VEVENT objects based on a list of required
   * properties.
   */
  private String transformVevent(final Calendar ical,
                                 final Collection<Prop> props)  throws WebdavException {
    try {
      Calendar nical = new Calendar();
      PropertyList pl = ical.getProperties();
      PropertyList npl = nical.getProperties();

      // Add all vcalendar properties to new cal
      Iterator it = pl.iterator();

      while (it.hasNext()) {
        npl.add((Property)it.next());
      }

      ComponentList cl = ical.getComponents();
      ComponentList ncl = nical.getComponents();

      it = cl.iterator();

      while (it.hasNext()) {
        Component c = (Component)it.next();

        if (!(c instanceof VEvent)) {
          ncl.add(c);
        } else {
          VEvent v = new VEvent();

          PropertyList vpl = c.getProperties();
          PropertyList nvpl = v.getProperties();

          for (Prop pr: props) {
            Property p = vpl.getProperty(pr.getName());

            if (p != null) {
              nvpl.add(p);
            }
          }

          ncl.add(v);
        }
      }

      return nical.toString();
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("transformVevent exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  /* ====================================================================
   *                   Private parsing methods
   * ==================================================================== */

  private Comp parseComp(final Node nd) throws WebdavException {
    /* Either allcomp + (either allprop or 0 or more prop) or
              0 or more comp + (either allprop or 0 or more prop)
     */
    String name = getOnlyAttrVal(nd, "name");
    if (name == null) {
      throw new WebdavBadRequest();
    }

    Comp c = new Comp(name);

    Element[] children = getChildren(nd);

    boolean hadComps = false;
    boolean hadProps = false;

    for (int i = 0; i < children.length; i++) {
      Node curnode = children[i];

      if (debug) {
        trace("comp node type: " +
              curnode.getNodeType() + " name:" +
              curnode.getNodeName());
      }

      if (XmlUtil.nodeMatches(curnode, CaldavTags.allcomp)) {
        if (hadComps) {
          throw new WebdavBadRequest();
        }

        c.setAllcomp(true);
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.comp)) {
        if (c.getAllcomp()) {
          throw new WebdavBadRequest();
        }

        c.addComp(parseComp(curnode));
        hadComps = true;
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.allprop)) {
        if (hadProps) {
          throw new WebdavBadRequest();
        }

        c.setAllprop(true);
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.prop)) {
        if (c.getAllprop()) {
          throw new WebdavBadRequest();
        }

        c.addProp(parseProp(curnode));
        hadProps = true;
      } else {
        throw new WebdavBadRequest();
      }
    }

    return c;
  }

  private Prop parseProp(final Node nd) throws WebdavException {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() == 0)) {
      throw new WebdavBadRequest();
    }

    int attrCt = nnm.getLength();

    String name = XmlUtil.getAttrVal(nnm, "name");
    if (name == null) {
      throw new WebdavBadRequest();
    }

    attrCt--;

    Boolean val = null;

    try {
      val = XmlUtil.getYesNoAttrVal(nnm, "novalue");
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }

    Prop pr = new Prop(name);

    if (val != null) {
      pr.setNovalue(val.booleanValue());
    }

    return pr;
  }

  private Element[] getChildren(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  /*
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

  private String getOnlyAttribute(Node nd, String name) throws WebdavException {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() != 1)) {
      throw new WebdavBadRequest();
    }

    String val = XmlUtil.getAttrVal(nnm, "name");
    if (val == null) {
      throw new WebdavBadRequest();
    }

    return val;
  }*/

  /** Fetch required attribute. Return null for error
   *
   * @param nd
   * @param name
   * @return String
   * @throws WebdavException
   */
  public String getOnlyAttrVal(final Node nd, final String name) throws WebdavException {
    NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() != 1)) {
      throw new WebdavBadRequest();
    }

    String res = XmlUtil.getAttrVal(nnm, name);
    if (res == null) {
      throw new WebdavBadRequest();
    }

    return res;
  }

  /* ====================================================================
   *                   Dump methods
   * ==================================================================== */

  /**
   *
   */
  public void dump() {
    StringBuffer sb = new StringBuffer("  <calendar-data");

    if (returnContentType != null) {
      sb.append("  return-content-type=\"");
      sb.append(returnContentType);
      sb.append("\"");
    }
    sb.append(">");
    trace(sb.toString());

    if (comp != null) {
      comp.dump(getLogger(), "    ");
    }

    if (ers != null) {
      ers.dump(getLogger(), "    ");
    }

    trace("  </calendar-data>");
  }

  /* ====================================================================
   *                   Logging methods
   * ==================================================================== */

  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

