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
package org.bedework.caldav.server.soap.calws;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.CaldavPrincipalNode;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.soap.SoapHandler;
import org.bedework.caldav.server.sysinterface.SystemProperties;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.caldav.server.sysinterface.SysIntf.SchedRecipientResult;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.calendar.ScheduleMethods;
import edu.rpi.cmt.calendar.XcalUtil;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.NsContext;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import org.oasis_open.docs.ns.wscal.calws_soap.AddItem;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.AddType;
import org.oasis_open.docs.ns.wscal.calws_soap.ArrayOfHrefs;
import org.oasis_open.docs.ns.wscal.calws_soap.ArrayOfUpdates;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseUpdateType;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarDataResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarMultiget;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarQuery;
import org.oasis_open.docs.ns.wscal.calws_soap.CalendarQueryResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.ErrorCodeType;
import org.oasis_open.docs.ns.wscal.calws_soap.ErrorResponseType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItem;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.FreebusyReport;
import org.oasis_open.docs.ns.wscal.calws_soap.FreebusyReportResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.GetProperties;
import org.oasis_open.docs.ns.wscal.calws_soap.GetPropertiesResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.InvalidFilterType;
import org.oasis_open.docs.ns.wscal.calws_soap.MultistatResponseElementType;
import org.oasis_open.docs.ns.wscal.calws_soap.MultistatusPropElementType;
import org.oasis_open.docs.ns.wscal.calws_soap.NamespaceType;
import org.oasis_open.docs.ns.wscal.calws_soap.NewValueType;
import org.oasis_open.docs.ns.wscal.calws_soap.ObjectFactory;
import org.oasis_open.docs.ns.wscal.calws_soap.Propstat;
import org.oasis_open.docs.ns.wscal.calws_soap.RemoveType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;
import org.oasis_open.docs.ns.wscal.calws_soap.UTCTimeRangeType;
import org.oasis_open.docs.ns.wscal.calws_soap.UidConflictType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItem;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.AttendeePropType;
import ietf.params.xml.ns.icalendar_2.DtendPropType;
import ietf.params.xml.ns.icalendar_2.DtstartPropType;
import ietf.params.xml.ns.icalendar_2.Icalendar;
import ietf.params.xml.ns.icalendar_2.OrganizerPropType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.Vcalendar;
import ietf.params.xml.ns.icalendar_2.VfreebusyType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public class CalwsHandler extends SoapHandler {
  /** This represents an active connection to a synch engine. It's possible we
   * would have more than one of these running I guess. For the moment we'll
   * only have one but these probably need a table indexed by url.
   *
   */
  class ActiveConnectionInfo {
    String subscribeUrl;

    String synchToken;
  }

  static String calwsNs = "http://docs.oasis-open.org/ns/wscal/calws-soap";

  static ActiveConnectionInfo activeConnection;

  static ObjectFactory of = new ObjectFactory();

  /**
   * @param intf
   * @throws WebdavException
   */
  public CalwsHandler(final CaldavBWIntf intf) throws WebdavException {
    super(intf);
  }

  @Override
  protected String getJaxbContextPath() {
    return "org.oasis_open.docs.ns.wscal.calws_soap";
  }

  /**
   * @param req
   * @param resp
   * @param pars
   * @throws WebdavException
   */
  public void processPost(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final RequestPars pars) throws WebdavException {

    try {
      initResponse(resp);

      Object o = unmarshal(req);
      if (o instanceof JAXBElement) {
        o = ((JAXBElement)o).getValue();
      }

      if (o instanceof GetProperties) {
        doGetProperties((GetProperties)o, resp);
        return;
      }

      if (o instanceof FreebusyReport) {
        doFreebusyReport((FreebusyReport)o, resp);
        return;
      }

      if (o instanceof CalendarMultiget) {
        doCalendarMultiget((CalendarMultiget)o, resp);
        return;
      }

      if (o instanceof CalendarQuery) {
        doCalendarQuery((CalendarQuery)o, resp);
        return;
      }

      if (o instanceof AddItem) {
        doAddItem((AddItem)o, req, resp);
        return;
      }

      if (o instanceof FetchItem) {
        doFetchItem((FetchItem)o, req, resp);
        return;
      }

      if (o instanceof UpdateItem) {
        doUpdateItem((UpdateItem)o, req, resp);
        return;
      }

      throw new WebdavException("Unhandled request");
    } catch (WebdavException we) {
      throw we;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private void doGetProperties(final GetProperties gp,
                               final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetProperties: ");
    }

    try {
      String url = gp.getHref();

      GetPropertiesResponse gpr = new GetPropertiesResponse();

      if (url != null) {
        WebdavNsNode calNode = getNsIntf().getNode(url,
                                                   WebdavNsIntf.existanceMust,
                                                   WebdavNsIntf.nodeTypeCollection);

        if (calNode != null) {
          CaldavBwNode nd = (CaldavBwNode)calNode;

          gpr.setXRD(((CaldavBWIntf)getNsIntf()).getXRD(nd));
        }

        marshal(gpr, resp.getOutputStream());
      }
    } catch (WebdavException we) {
      throw we;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doFreebusyReport(final FreebusyReport fr,
                                final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("FreebusyReport: ");
    }

    FreebusyReportResponse frr = new FreebusyReportResponse();

    try {
      String url = fr.getHref();

      buildResponse: {
        if (url == null) {
          frr.setStatus(StatusType.ERROR);
          frr.setMessage("No href supplied");
          break buildResponse;
        }

        WebdavNsNode elNode = getNsIntf().getNode(url,
                                                  WebdavNsIntf.existanceMust,
                                                  WebdavNsIntf.nodeTypeUnknown);

        if (!(elNode instanceof CaldavPrincipalNode)) {
          frr.setStatus(StatusType.ERROR);
          frr.setMessage("Only principal href supported");
          break buildResponse;
        }

        String cua = getSysi().principalToCaladdr(getSysi().getPrincipal(url));

        /* Build an icalendar freebusy object out of the parameters */

        Icalendar ical = new Icalendar();
        Vcalendar vcal = new Vcalendar();

        ical.getVcalendars().add(vcal);

        VfreebusyType vfb = new VfreebusyType();

        JAXBElement<VfreebusyType> compel =
          new JAXBElement<VfreebusyType>(XcalTags.vfreebusy,
                                         VfreebusyType.class, vfb);
        ArrayOfComponents aoc = new ArrayOfComponents();

        vcal.setComponents(aoc);
        aoc.getBaseComponents().add(compel);

        /* Use timerange to limit the requested time */

        SystemProperties sysp = getSysi().getSystemProperties();

        UTCTimeRangeType utr = fr.getTimeRange();

        TimeRange tr = ParseUtil.getPeriod(utr.getStart(),
                                           utr.getEnd(),
                                           java.util.Calendar.DATE,
                                           sysp.getDefaultFBPeriod(),
                                           java.util.Calendar.DATE,
                                           sysp.getMaxFBPeriod());

        ArrayOfProperties aop = new ArrayOfProperties();
        vfb.setProperties(aop);

        DtstartPropType dtstart = new DtstartPropType();
        dtstart.setDateTime(tr.getStart().toString());

        JAXBElement<DtstartPropType> dtstartProp =
          new JAXBElement<DtstartPropType>(XcalTags.dtstart,
                                           DtstartPropType.class, dtstart);

        aop.getBaseProperties().add(dtstartProp);

        DtendPropType dtend = new DtendPropType();
        dtend.setDateTime(tr.getEnd().toString());

        JAXBElement<DtendPropType> dtendProp =
          new JAXBElement<DtendPropType>(XcalTags.dtend,
                                           DtendPropType.class, dtend);

        aop.getBaseProperties().add(dtendProp);

        /* Add a uid */

        UidPropType uid = new UidPropType();
        uid.setText(Util.makeRandomString(30, 35));

        JAXBElement<UidPropType> uidProp =
          new JAXBElement<UidPropType>(XcalTags.uid,
                                       UidPropType.class, uid);

        aop.getBaseProperties().add(uidProp);

        /* Add the cua as the organizer */

        OrganizerPropType org = new OrganizerPropType();
        org.setCalAddress(cua);

        JAXBElement<OrganizerPropType> orgProp =
          new JAXBElement<OrganizerPropType>(XcalTags.organizer,
                                             OrganizerPropType.class, org);

        aop.getBaseProperties().add(orgProp);

        /* We should be in as an attendee */

        AttendeePropType att = new AttendeePropType();
        att.setCalAddress(getSysi().principalToCaladdr(getSysi().getPrincipal()));

        JAXBElement<AttendeePropType> attProp =
          new JAXBElement<AttendeePropType>(XcalTags.attendee,
                                            AttendeePropType.class, att);

        aop.getBaseProperties().add(attProp);

        SysiIcalendar sical = getSysi().fromIcal(null, ical,
                                                 IcalResultType.OneComponent);
        CalDAVEvent ev = sical.getEvent();

        ev.setScheduleMethod(ScheduleMethods.methodTypeRequest);
        Set<String> recipients = new TreeSet<String>();
        recipients.add(cua);
        ev.setRecipients(recipients);

        Collection<SchedRecipientResult> srrs = getSysi().requestFreeBusy(ev);

        if (srrs.size() != 1) {
          frr.setStatus(StatusType.ERROR);
          frr.setMessage("No data returned");
          break buildResponse;
        }

        SchedRecipientResult sr = srrs.iterator().next();

        frr.setIcalendar(getSysi().toIcalendar(sr.freeBusy, false, null));
        frr.setStatus(StatusType.OK);
      } // buildResponse

      marshal(frr, resp.getOutputStream());
    } catch (WebdavException we) {
      frr.setStatus(StatusType.ERROR);
      throw we;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doCalendarMultiget(final CalendarMultiget cm,
                               final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("CalendarMultiget: ");
    }

    CalendarQueryResponse cqr = new CalendarQueryResponse();

    try {
      String url = cm.getHref();

      buildResponse: {
        if (url == null) {
          cqr.setStatus(StatusType.ERROR);
          cqr.setMessage("No href supplied");
          break buildResponse;
        }

        ArrayOfHrefs hrefs = cm.getHrefs();
        if (hrefs == null) {
          break buildResponse;
        }

        Report rpt = new Report(getNsIntf());

        Collection<String> badHrefs = new ArrayList<String>();

        buildQueryResponse(cqr,
                           rpt.getMgetNodes(hrefs.getHreves(), badHrefs),
                           cm.getIcalendar());

        if (badHrefs.isEmpty()) {
          break buildResponse;
        }

        for (String bh: badHrefs) {
          MultistatResponseElementType mre = new MultistatResponseElementType();

          mre.setHref(bh);

          cqr.getResponses().add(mre);

          Propstat ps = new Propstat();

          mre.getPropstats().add(ps);

          ps.setStatus(getStatus(HttpServletResponse.SC_NOT_FOUND, null));
        }
      } // buildResponse
    } catch (WebdavException we) {
      // Remove any partial results.
      cqr.getResponses().clear();
      errorResponse(cqr, we);
    } catch(Throwable t) {
      throw new WebdavException(t);
    }

    try {
      marshal(cqr, resp.getOutputStream());
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavException(t);
    }
  }

  private void doCalendarQuery(final CalendarQuery cq,
                               final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("CalendarQuery: ");
    }

    CalendarQueryResponse cqr = new CalendarQueryResponse();

    try {
      String url = cq.getHref();

      buildResponse: {
        if (url == null) {
          cqr.setStatus(StatusType.ERROR);
          cqr.setMessage("No href supplied");
          break buildResponse;
        }

        Report rpt = new Report(getNsIntf());

        buildQueryResponse(cqr, rpt.query(url, cq), cq.getIcalendar());

        cqr.setStatus(StatusType.OK);
      } // buildResponse
    } catch (WebdavException we) {
      // Remove any partial results.
      cqr.getResponses().clear();
      errorResponse(cqr, we);
    } catch(Throwable t) {
      throw new WebdavException(t);
    }

    try {
      marshal(cqr, resp.getOutputStream());
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavException(t);
    }
  }

  private void buildQueryResponse(final CalendarQueryResponse cqr,
                                  final Collection<WebdavNsNode> nodes,
                                  final Icalendar pattern) throws WebdavException {
    if (nodes == null) {
      return;
    }

    for (WebdavNsNode curnode: nodes) {
      MultistatResponseElementType mre = new MultistatResponseElementType();

      mre.setHref(curnode.getUri());
      mre.setEtag(curnode.getEtagValue(true));

      cqr.getResponses().add(mre);

      Propstat ps = new Propstat();

      mre.getPropstats().add(ps);

      ps.setStatus(getStatus(curnode.getStatus(), null));

      if (!curnode.getExists()) {
        continue;
      }

      if (!(curnode instanceof CaldavComponentNode)) {
        continue;
      }

      /* For the moment always return the full calendar data. Need to
       * implement the properties thing
       */

      MultistatusPropElementType mpe = new MultistatusPropElementType();

      ps.getProps().add(mpe);

      CalendarDataResponseType cdr = new CalendarDataResponseType();

      mpe.setCalendarData(cdr);

      CalDAVEvent ev = ((CaldavComponentNode)curnode).getEvent();

      cdr.setIcalendar(getIntf().getSysi().toIcalendar(ev, false, pattern));
      cdr.setContentType("application/xml+calendar");
      cdr.setVersion("2.0");
    }
  }

  private void errorResponse(final BaseResponseType br,
                             final WebdavException we) {
    br.setStatus(StatusType.ERROR);
    br.setMessage(we.getMessage());

    ErrorResponseType er = new ErrorResponseType();

    QName etag = we.getErrorTag();

    setError: {
      if (etag == null) {
        break setError;
      }

      if (etag.equals(CaldavTags.validFilter)) {
        InvalidFilterType invf = new InvalidFilterType();
        er.setError(of.createInvalidFilter(invf));
        break setError;
      }

      /*
      if (etag.equals(CaldavTags.attendeeAllowed)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of.(ec));
        break setError;
      }
      */

      if (etag.equals(CaldavTags.calendarCollectionLocationOk)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of.createInvalidCalendarCollectionLocation(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.noUidConflict)) {
        UidConflictType uc= new UidConflictType();
        uc.setHref(we.getMessage()); // WRONG
        er.setError(of.createUidConflict(uc));
        break setError;
      }

      /* sched
      if (etag.equals(CaldavTags.organizerAllowed)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.originatorAllowed)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.recipientPermissions)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of(ec));
        break setError;
      }
      */

      if (etag.equals(CaldavTags.validCalendarData)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of.createInvalidCalendarData(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.validCalendarObjectResource)) {
        ErrorCodeType ec = new ErrorCodeType();
        er.setError(of.createInvalidCalendarObjectResource(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.validFilter)) {
        InvalidFilterType iv = new InvalidFilterType();
        iv.setDetail(we.getMessage());
        er.setError(of.createInvalidFilter(iv));
        break setError;
      }
    } // setError

    if (er.getError() != null) {
      br.setErrorResponse(er);
    }
  }

  private void doAddItem(final AddItem ai,
                         final HttpServletRequest req,
                         final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("AddItem: cal=" + ai.getHref());
    }

    AddItemResponse air = new AddItemResponse();

    addEntity: {
      /* Manufacture a name */

      UidPropType uidp = (UidPropType)XcalUtil.findProperty(
                 XcalUtil.findEntity(ai.getIcalendar()), XcalTags.uid);

      if ((uidp == null) || (uidp.getText() == null)) {
        air.setStatus(StatusType.ERROR);
        break addEntity;
      }

      String entityPath = ai.getHref();

      if (!entityPath.endsWith("/")) {
        entityPath += "/";
      }

      entityPath += uidp.getText() + ".ics";

      WebdavNsNode elNode = getNsIntf().getNode(entityPath,
                                                WebdavNsIntf.existanceNot,
                                                WebdavNsIntf.nodeTypeEntity);

      try {
        /*
         *     String ifStag = Headers.ifScheduleTagMatch(req);
               boolean noInvites = req.getHeader("Bw-NoInvites") != null; // based on header?
         */
        if (getIntf().putEvent((CaldavComponentNode)elNode,
                               ai.getIcalendar(),
                               true,
                               false,  // noinvites
                               null,   // ifStag
                               null)) {
          air.setStatus(StatusType.OK);
          air.setHref(elNode.getUri());
        } else {
          air.setStatus(StatusType.ERROR);
        }
      } catch (WebdavException we) {
        errorResponse(air, we);
      } catch (Throwable t) {
        if (debug) {
          error(t);
        }
      }
    } // addEntity

    try {
      marshal(air, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doFetchItem(final FetchItem fi,
                           final HttpServletRequest req,
                           final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("FetchItem:       cal=" + fi.getHref());
    }

    WebdavNsNode elNode = getNsIntf().getNode(fi.getHref(),
                                              WebdavNsIntf.existanceMust,
                                              WebdavNsIntf.nodeTypeEntity);

    FetchItemResponse fir = new FetchItemResponse();

    if (elNode == null) {
      fir.setStatus(StatusType.ERROR);
    } else {
      fir.setStatus(StatusType.OK);
      CalDAVEvent ev = ((CaldavComponentNode)elNode).getEvent();
      fir.setIcalendar(getIntf().getSysi().toIcalendar(ev, false, null));
    }

    try {
      marshal(fir, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doUpdateItem(final UpdateItem ui,
                            final HttpServletRequest req,
                            final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("UpdateItem:       cal=" + ui.getHref());
    }

    WebdavNsNode elNode = getNsIntf().getNode(ui.getHref(),
                                              WebdavNsIntf.existanceMust,
                                              WebdavNsIntf.nodeTypeEntity);

    UpdateItemResponse uir = new UpdateItemResponse();

    if (elNode == null) {
      uir.setStatus(StatusType.ERROR);
      uir.setMessage("Href not found");
      return;
    }

    CaldavComponentNode compNode = (CaldavComponentNode)elNode;
    CalDAVEvent ev = compNode.getEvent();

    if (debug) {
      trace("event: " + ev);
    }

    Document doc = makeDoc(XcalTags.icalendar,
                           getIntf().getSysi().toIcalendar(ev, false, null));

    ArrayOfUpdates aupd = ui.getUpdates();

    NsContext ctx = new NsContext(null);
    ctx.clear();

    for (NamespaceType ns: ui.getNamespaces().getNamespaces()) {
      ctx.add(ns.getPrefix(), ns.getUri());
    }

    XPathFactory xpathFact = XPathFactory.newInstance();
    XPath xpath = xpathFact.newXPath();

    xpath.setNamespaceContext(ctx);

    uir.setStatus(StatusType.OK);

    try {
      for (JAXBElement<? extends BaseUpdateType> jel: aupd.getBaseUpdates()) {
        BaseUpdateType but = jel.getValue();

        XPathExpression expr = xpath.compile(but.getSel());

        NodeList nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);

        int nlen = nodes.getLength();
        if (debug) {
          trace("expr: " + but.getSel() + " found " + nlen);
        }

        if (nlen != 1) {
          // We only allow updates to a single node.
          uir.setStatus(StatusType.ERROR);
          getIntf().getSysi().rollback();
          break;
        }

        Node nd = nodes.item(0);

        if (but instanceof RemoveType) {
          removeNode(nd);
          continue;
        }

        NewValueType nv = (NewValueType)but;

        /* Replacement or new value must be of same type
         */
        if (!processNewValue(nv, nd, doc)) {
          uir.setStatus(StatusType.ERROR);
          getIntf().getSysi().rollback();
          break;
        }
      }

      if (uir.getStatus() == StatusType.OK) {
        Unmarshaller u = jc.createUnmarshaller();

        Icalendar ical = (Icalendar)u.unmarshal(doc);

  //      WebdavNsNode calNode = getNsIntf().getNode(ui.getCalendarHref(),
  //                                                 WebdavNsIntf.existanceMust,
  //                                                 WebdavNsIntf.nodeTypeCollection);
        CalDAVCollection col = (CalDAVCollection)compNode.getCollection(true); // deref

        SysiIcalendar cal = getIntf().getSysi().fromIcal(col,
                                                         ical,
                                                         IcalResultType.OneComponent);

        CalDAVEvent newEv = (CalDAVEvent)cal.iterator().next();

        ev.setParentPath(col.getPath());
        newEv.setName(ev.getName());

        getIntf().getSysi().updateEvent(newEv);
      }

      marshal(uir, resp.getOutputStream());
    } catch (XPathExpressionException xpe) {
      throw new WebdavException(xpe);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private boolean processNewValue(final NewValueType nv,
                                  final Node nd,
                                  final Document evDoc) throws WebdavException {
    Node parent = nd.getParentNode();
    Node matchNode;

    boolean add = nv instanceof AddType;
    QName valName;

    if (add) {
      matchNode = nd;
      valName = new QName("urn:ietf:params:xml:ns:pidf-diff", "add");
    } else {
      matchNode = parent;
      valName = new QName("urn:ietf:params:xml:ns:pidf-diff", "replace");
    }

    validate: {
      if (nv.getBaseComponent() != null) {
        // parent must be a components element
        if (!XmlUtil.nodeMatches(matchNode, XcalTags.components)) {
          return false;
        }

        break validate;
      }

      if (nv.getBaseProperty() != null) {
        // parent must be a properties element
        if (!XmlUtil.nodeMatches(matchNode, XcalTags.properties)) {
          return false;
        }
        break validate;
      }

      if (nv.getBaseParameter() != null) {
        // parent must be a parameters element
        if (!XmlUtil.nodeMatches(matchNode, XcalTags.parameters)) {
          return false;
        }
        break validate;
      }

      return false;
    } // validate

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.newDocument();

      Marshaller m = jc.createMarshaller();

      m.marshal(makeJAXBElement(valName, nv.getClass(), nv),
                doc);

      Node newNode = doc.getFirstChild().getFirstChild();

      if (add) {
        matchNode.appendChild(evDoc.importNode(newNode, true));

        return true;
      }

      parent.replaceChild(evDoc.importNode(newNode, true), nd);

      return true;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
