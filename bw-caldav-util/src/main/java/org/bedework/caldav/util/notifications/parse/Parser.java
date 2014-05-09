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
package org.bedework.caldav.util.notifications.parse;

import org.bedework.caldav.util.notifications.BaseNotificationType;
import org.bedework.caldav.util.notifications.CalendarChangesType;
import org.bedework.caldav.util.notifications.ChangedByType;
import org.bedework.caldav.util.notifications.ChangedParameterType;
import org.bedework.caldav.util.notifications.ChangedPropertyType;
import org.bedework.caldav.util.notifications.ChangesType;
import org.bedework.caldav.util.notifications.ChildCreatedType;
import org.bedework.caldav.util.notifications.ChildDeletedType;
import org.bedework.caldav.util.notifications.ChildUpdatedType;
import org.bedework.caldav.util.notifications.CollectionChangesType;
import org.bedework.caldav.util.notifications.CreatedType;
import org.bedework.caldav.util.notifications.DeletedDetailsType;
import org.bedework.caldav.util.notifications.DeletedType;
import org.bedework.caldav.util.notifications.NotificationType;
import org.bedework.caldav.util.notifications.ProcessorType;
import org.bedework.caldav.util.notifications.ProcessorsType;
import org.bedework.caldav.util.notifications.PropType;
import org.bedework.caldav.util.notifications.RecurrenceType;
import org.bedework.caldav.util.notifications.ResourceChangeType;
import org.bedework.caldav.util.notifications.UpdatedType;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/** Class to parse properties and requests related to CalDAV sharing
 * (as defined by Apple).
 *
 * @author Mike Douglass douglm
 */
public class Parser {
  /* Notifications we know about */
  private final static Map<QName, BaseNotificationParser> parsers =
      new HashMap<>();

  static {
    for (final BaseNotificationParser bnp:
         org.bedework.caldav.util.sharing.parse.Parser.getParsers()) {
      parsers.put(bnp.getElement(), bnp);
    }

    final BaseNotificationParser bnp = new ResourceChangeParser();
    parsers.put(bnp.getElement(), bnp);
  }

  /* General notifications elements */

  private static final QName dtstampTag = AppleServerTags.dtstamp;

  private static final QName notificationTag = AppleServerTags.notification;

  private static abstract class AbstractNotificationParser implements BaseNotificationParser {
    private static final int maxPoolSize = 10;
    private final List<Parser> parsers = new ArrayList<>();

    protected Parser parser;

    protected QName element;

    protected AbstractNotificationParser(final QName element) {
      this.element = element;
    }

    protected Parser getParser() {
      if (parser != null) {
        return parser;
      }

      synchronized (parsers) {
        if (parsers.size() > 0) {
          parser = parsers.remove(0);
          return parser;
        }

        parser = new Parser();
        parsers.add(parser);

        return parser;
      }
    }

    protected void putParser() {
      synchronized (parsers) {
        if (parsers.size() >= maxPoolSize) {
          return;
        }

        parsers.add(parser);
      }
    }

    @Override
    public QName getElement() {
      return element;
    }
  }

  static class ResourceChangeParser extends AbstractNotificationParser {
    ResourceChangeParser() {
      super(AppleServerTags.resourceChange);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseResourceChangeNotification(nd);
      } finally {
        putParser();
      }
    }
  }

  /**
   * @param val
   * @return parsed notification or null
   * @throws WebdavException
   */
  public static NotificationType fromXml(final String val) throws WebdavException {
    ByteArrayInputStream bais = new ByteArrayInputStream(val.getBytes());

    return fromXml(bais);
  }

  /**
   * @param is stream
   * @return parsed notification or null
   * @throws WebdavException
   */
  public static NotificationType fromXml(final InputStream is) throws WebdavException{
    final Document doc = parseXmlString(is);

    if (doc == null) {
      return null;
    }

    return new Parser().parseNotification(doc.getDocumentElement());
  }

  /**
   * @param is
   * @return parsed Document
   * @throws WebdavException
   */
  public static Document parseXmlString(final InputStream is) throws WebdavException{
    if (is == null) {
      return null;
    }

    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(is));
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the notification xml element
   * @return populated ShareType object
   * @throws WebdavException
   */
  public NotificationType parseNotification(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, notificationTag)) {
        throw new WebdavBadRequest("Expected " + notificationTag);
      }

      NotificationType n = new NotificationType();
      final Element[] els = XmlUtil.getElementsArray(nd);

      int pos = parseCommonElements(n, nd);

      while (pos < els.length) {
        final Element curnode = els[pos];

        if (XmlUtil.nodeMatches(curnode, dtstampTag)) {
          n.setDtstamp(XmlUtil.getElementContent(curnode));
        } else {
          final BaseNotificationParser bnp = parsers.get(XmlUtil.fromNode(curnode));
          if ((bnp == null) ||
              (n.getNotification() != null)) {
            throw badNotification(curnode);
          }

          n.setNotification(bnp.parse(curnode));
        }
        pos++;
      }

      return n;
    } catch (final SAXException e) {
      dumpXml(nd);
      throw new WebdavBadRequest();
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Parse elements common to all
   *
   * @param note the notification
   * @param nd   the node
   * @throws WebdavException
   * @return number of elements parsed
   */
  public int parseCommonElements(final NotificationType note,
                                 final Node nd) throws Throwable {
    final Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length == 0) {
      return 0;
    }

    if (!XmlUtil.nodeMatches(els[0], BedeworkServerTags.processors)) {
      return 0;
    }

    note.setProcessors(parseProcessors(els[0]));

    return 1;
  }

  ProcessorsType parseProcessors(final Element nd) throws Throwable {
    final ProcessorsType pt = new ProcessorsType();

    final Element[] els = XmlUtil.getElementsArray(nd);

    for (final Element curnode: els) {
      if (!XmlUtil.nodeMatches(curnode, BedeworkServerTags.processor)) {
        throw new WebdavBadRequest("Expected " + BedeworkServerTags.processor);
      }

      pt.getProcessor().add(parseProcessor(curnode));
    }

    return pt;
  }

  ProcessorType parseProcessor(final Element nd) throws Throwable {
    final ProcessorType pt = new ProcessorType();

    final Element[] els = XmlUtil.getElementsArray(nd);
    final int len = els.length;

    int pos = 0;

    if ((len > pos) &&
            XmlUtil.nodeMatches(els[pos], BedeworkServerTags.type)) {
      pt.setType(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
            XmlUtil.nodeMatches(els[pos], dtstampTag)) {
      pt.setDtstamp(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if ((len > pos) &&
            XmlUtil.nodeMatches(els[pos], WebdavTags.status)) {
      pt.setStatus(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    return pt;
  }

  /**
   * @param nd MUST be the resource-change xml element
   * @return populated ResourceChangeType object
   * @throws WebdavException
   */
  public ResourceChangeType parseResourceChangeNotification(final Element nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, AppleServerTags.resourceChange)) {
        throw new WebdavBadRequest("Expected " + AppleServerTags.resourceChange);
      }

      ResourceChangeType rc = new ResourceChangeType();

      Element[] els = XmlUtil.getElementsArray(nd);

      Object parsed = null;

      for (final Element curnode: els) {
        if (XmlUtil.nodeMatches(curnode, AppleServerTags.created)) {
          if (parsed != null) {
            throw badNotification(curnode);
          }

          CreatedType c = parseCreated(curnode);
          rc.setCreated(c);
          parsed = c;
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, AppleServerTags.updated)) {
          if ((parsed != null) &&
              (!(parsed instanceof UpdatedType))) {
            throw badNotification(curnode);
          }

          UpdatedType u = parseUpdated(curnode);
          rc.addUpdate(u);
          parsed = u;
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, AppleServerTags.deleted)) {
          if (parsed != null) {
            throw badNotification(curnode);
          }

          DeletedType d = parseDeleted(curnode);
          rc.setDeleted(d);
          parsed = d;
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, AppleServerTags.collectionChanges)) {
          if (parsed != null) {
            throw badNotification(curnode);
          }

          CollectionChangesType cc = parseCollectionChanges(curnode);
          rc.setCollectionChanges(cc);
          parsed = cc;
          continue;
        }

        throw badNotification(curnode);
      }

      return rc;
    } catch (SAXException e) {
      throw parseException(e);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private CreatedType parseCreated(final Element nd) throws Throwable {
    CreatedType c = new CreatedType();

    Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length < 1) {
      throw badNotification("No elements for create");
    }

    c.setHref(parseHref(els[0]));

    if (els.length > 1) {
      if (XmlUtil.nodeMatches(els[1], AppleServerTags.changedBy)) {
        c.setChangedBy(parseChangedBy(els[1]));
      }
    }

    return c;
  }

  private UpdatedType parseUpdated(final Element nd) throws Throwable {
    UpdatedType u = new UpdatedType();

    Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length < 1) {
      throw badNotification("No elements for update");
    }

    int pos = 0;

    u.setHref(parseHref(els[pos]));

    pos++;

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.changedBy)) {
      u.setChangedBy(parseChangedBy(els[1]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.content)) {
      u.setContent(true);
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], WebdavTags.prop)) {
      u.setProp(parseProps(els[pos]));
      pos++;
    }

    while (els.length > pos) {
      expect(els[pos], AppleServerTags.calendarChanges);
      u.getCalendarChanges().add(parseCalendarChange(els[pos]));
      pos++;
    }

    return u;
  }

  private DeletedType parseDeleted(final Element nd) throws Throwable {
    DeletedType d = new DeletedType();

    Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length < 1) {
      throw badNotification("No elements for delete");
    }

    int pos = 0;

    d.setHref(parseHref(els[pos]));

    pos++;

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.changedBy)) {
      d.setChangedBy(parseChangedBy(els[pos]));
      pos++;
    }

    expect(els[pos], AppleServerTags.deletedDetails);
    d.setDeletedDetails(parseDeletedDetails(els[pos]));

    return d;
  }

  private CollectionChangesType parseCollectionChanges(final Element nd) throws Throwable {
    CollectionChangesType cc = new CollectionChangesType();

    Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length < 1) {
      throw badNotification("No elements for collection-changes");
    }

    int pos = 0;

    cc.setHref(parseHref(els[pos]));

    pos++;

    while ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.changedBy)) {
      cc.getChangedBy().add(parseChangedBy(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], WebdavTags.prop)) {
      cc.setProp(parseProps(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.childCreated)) {
      ChildCreatedType chc = new ChildCreatedType();
      chc.setCount(getIntContent(els[pos]));

      cc.setChildCreated(chc);
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.childUpdated)) {
      ChildUpdatedType chu = new ChildUpdatedType();
      chu.setCount(getIntContent(els[pos]));

      cc.setChildUpdated(chu);
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.childDeleted)) {
      ChildDeletedType chd = new ChildDeletedType();
      chd.setCount(getIntContent(els[pos]));

      cc.setChildDeleted(chd);
      pos++;
    }

    if (els.length > pos) {
      throw badNotification(els[pos]);
    }

    return cc;
  }

  private int getIntContent(final Element nd) throws Throwable {
    String val = XmlUtil.getElementContent(nd);

    return Integer.valueOf(val);
  }

  private DeletedDetailsType parseDeletedDetails(final Element nd) throws Throwable {
    DeletedDetailsType dd = new DeletedDetailsType();

    Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length < 1) {
      throw badNotification("No elements for deleted-details");
    }

    int pos = 0;

    if (XmlUtil.nodeMatches(els[pos], AppleServerTags.deletedDisplayname)) {
      dd.setDeletedDisplayname(XmlUtil.getElementContent(els[pos]));
      pos++;

      if (els.length > pos) {
        throw badNotification(els[pos]);
      }

      return dd;
    }

    expect(els[pos], AppleServerTags.deletedComponent);
    dd.setDeletedComponent(XmlUtil.getElementContent(els[pos]));

    pos++;

    expect(els[pos], AppleServerTags.deletedSummary);
    dd.setDeletedSummary(XmlUtil.getElementContent(els[pos]));

    pos++;

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.deletedNextInstance)) {
      dd.setDeletedNextInstance(XmlUtil.getElementContent(els[pos]));
      dd.setDeletedNextInstanceTzid(XmlUtil.getAttrVal(els[pos], "tzid"));
      pos++;
    }

    if (els.length > pos) {
      expect(els[pos], AppleServerTags.deletedHadMoreInstances);
      dd.setDeletedHadMoreInstances(true);
      pos++;
    }

    if (els.length > pos) {
      throw badNotification(els[pos]);
    }

    return dd;
  }

  private PropType parseProps(final Element nd) throws Throwable {
    PropType p = new PropType();

    for (Element curnode: XmlUtil.getElementsArray(nd)) {
      p.getQnames().add(XmlUtil.fromNode(curnode));
    }

    return p;
  }

  private CalendarChangesType parseCalendarChange(final Element nd) throws Throwable {
    CalendarChangesType cc = new CalendarChangesType();

    for (Element curnode: XmlUtil.getElementsArray(nd)) {
      expect(curnode, AppleServerTags.recurrence);

      cc.getRecurrence().add(parseRecurrence(curnode));
    }

    return cc;
  }

  private RecurrenceType parseRecurrence(final Element nd) throws Throwable {
    RecurrenceType r = new RecurrenceType();
    Element[] els = XmlUtil.getElementsArray(nd);

    if (els.length < 1) {
      throw badNotification("No elements for recurrence");
    }

    int pos = 0;

    if (XmlUtil.nodeMatches(els[pos], AppleServerTags.master)) {
      // Nothing to set
      pos++;
    } else {
      expect(els[pos], AppleServerTags.recurrenceid);
      r.setRecurrenceid(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.added)) {
      r.setAdded(true);
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.removed)) {
      r.setRemoved(true);
      pos++;
    }

    while (els.length > pos) {
      expect(els[pos], AppleServerTags.changes);
      r.getChanges().add(parseChanges(els[pos]));
      pos++;
    }

    return r;
  }

  private ChangesType parseChanges(final Element nd) throws Throwable {
    ChangesType c = new ChangesType();

    for (Element curnode: XmlUtil.getElementsArray(nd)) {
      expect(curnode, AppleServerTags.changedProperty);

      c.getChangedProperty().add(parseChangedProperty(curnode));
    }

    return c;
  }

  private ChangedPropertyType parseChangedProperty(final Element nd) throws Throwable {
    ChangedPropertyType cp = new ChangedPropertyType();

    cp.setName(XmlUtil.getAttrVal(nd, "name"));

    Element[] els = XmlUtil.getElementsArray(nd);

    int pos = 0;

    while ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], AppleServerTags.changedParameter)) {
      cp.getChangedParameter().add(parseChangedParameter(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], BedeworkServerTags.dataFrom)) {
      cp.setDataFrom(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], BedeworkServerTags.dataTo)) {
      cp.setDataTo(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if (els.length > pos) {
      throw badNotification(els[pos]);
    }

    return cp;
  }

  private ChangedParameterType parseChangedParameter(final Element nd) throws Throwable {
    ChangedParameterType cp = new ChangedParameterType();

    cp.setName(XmlUtil.getAttrVal(nd, "name"));

    Element[] els = XmlUtil.getElementsArray(nd);

    int pos = 0;

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], BedeworkServerTags.dataFrom)) {
      cp.setDataFrom(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if ((els.length > pos) &&
      XmlUtil.nodeMatches(els[pos], BedeworkServerTags.dataTo)) {
      cp.setDataTo(XmlUtil.getElementContent(els[pos]));
      pos++;
    }

    if (els.length > pos) {
      throw badNotification(els[pos]);
    }

    return cp;
  }

  private ChangedByType parseChangedBy(final Element nd) throws Throwable {
    ChangedByType cb = new ChangedByType();

    Element[] els = XmlUtil.getElementsArray(nd);

    int pos;

    if (XmlUtil.nodeMatches(els[0], AppleServerTags.commonName)) {
      cb.setCommonName(XmlUtil.getElementContent(els[0]));
      pos = 1;
    } else {
      expect(els[0], AppleServerTags.firstName);
      cb.setFirstName(XmlUtil.getElementContent(els[0]));

      expect(els[1], AppleServerTags.lastName);
      cb.setLastName(XmlUtil.getElementContent(els[1]));

      pos = 2;
    }

    if (XmlUtil.nodeMatches(els[pos], AppleServerTags.dtstamp)) {
      cb.setDtstamp(XmlUtil.getElementContent(els[pos]));

      pos++;
    }

    cb.setHref(parseHref(els[pos]));

    return cb;
  }

  private String parseHref(final Element nd) throws Throwable {
    expect(nd, WebdavTags.href);
    return XmlUtil.getElementContent(nd);
  }

  private static void dumpXml(final Node nd) {
    Logger log = getLog();

    if (!log.isDebugEnabled()) {
      return;
    }

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
//      XMLWriter writer = new XMLWriter(out, format);

  //    writer.write(nd);
      TransformerFactory tfactory = TransformerFactory.newInstance();
      Transformer serializer;

      serializer = tfactory.newTransformer();
      //Setup indenting to "pretty print"
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      serializer.transform(new DOMSource(nd), new StreamResult(out));

      log.debug(out.toString());
    } catch (Throwable t) {
      log.error("Unable to dump XML");
    }
  }

  private void expect(final Element nd,
                      final QName expected) throws Throwable {
    if (!XmlUtil.nodeMatches(nd, expected)) {
      throw badNotification(nd, expected);
    }
  }

  private WebdavBadRequest badNotification(final String msg) {
    return new WebdavBadRequest(msg);
  }

  private WebdavBadRequest badNotification(final Element curnode,
                                           final QName expected) {
    return new WebdavBadRequest("Unexpected element " + curnode +
                                " expected " + expected);
  }

  private WebdavBadRequest badNotification(final Element curnode) {
    return new WebdavBadRequest("Unexpected element " + curnode);
  }

  private static WebdavException parseException(final SAXException e) throws WebdavException {
    Logger log = getLog();

    if (log.isDebugEnabled()) {
      log.error("Parse error:", e);
    }

    return new WebdavBadRequest();
  }

  private static Logger getLog() {
    return Logger.getLogger(Parser.class);
  }
}
