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

import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavCalNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.CaldavPrincipalNode;
import org.bedework.caldav.server.RequestPars;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.server.soap.SoapHandler;
import org.bedework.caldav.server.sysinterface.CalDAVAuthProperties;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.caldav.server.sysinterface.SysIntf.SchedRecipientResult;
import org.bedework.caldav.server.sysinterface.SysIntf.UpdateResult;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;
import org.bedework.util.calendar.ScheduleMethods;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.XcalTags;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNotFound;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavUnauthorized;

import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.AttendeePropType;
import ietf.params.xml.ns.icalendar_2.DtendPropType;
import ietf.params.xml.ns.icalendar_2.DtstartPropType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.OrganizerPropType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VfreebusyType;
import org.oasis_open.docs.ns.xri.xrd_1.XRDType;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.AddItemType;
import org.oasis_open.docs.ws_calendar.ns.soap.ArrayOfHrefs;
import org.oasis_open.docs.ws_calendar.ns.soap.ArrayOfResponses;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseRequestType;
import org.oasis_open.docs.ws_calendar.ns.soap.BaseResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarDataResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarMultigetType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.DeleteItemType;
import org.oasis_open.docs.ws_calendar.ns.soap.ErrorResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FetchItemType;
import org.oasis_open.docs.ws_calendar.ns.soap.ForbiddenType;
import org.oasis_open.docs.ws_calendar.ns.soap.FreebusyReportResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.FreebusyReportType;
import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesType;
import org.oasis_open.docs.ws_calendar.ns.soap.InvalidCalendarCollectionLocationType;
import org.oasis_open.docs.ws_calendar.ns.soap.InvalidCalendarDataType;
import org.oasis_open.docs.ws_calendar.ns.soap.InvalidCalendarObjectResourceType;
import org.oasis_open.docs.ws_calendar.ns.soap.InvalidFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.MismatchedChangeTokenType;
import org.oasis_open.docs.ws_calendar.ns.soap.MissingChangeTokenType;
import org.oasis_open.docs.ws_calendar.ns.soap.MultiOpResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.MultiOpType;
import org.oasis_open.docs.ws_calendar.ns.soap.MultistatResponseElementType;
import org.oasis_open.docs.ws_calendar.ns.soap.MultistatusPropElementType;
import org.oasis_open.docs.ws_calendar.ns.soap.ObjectFactory;
import org.oasis_open.docs.ws_calendar.ns.soap.PropstatType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;
import org.oasis_open.docs.ws_calendar.ns.soap.TargetDoesNotExistType;
import org.oasis_open.docs.ws_calendar.ns.soap.TargetNotEntityType;
import org.oasis_open.docs.ws_calendar.ns.soap.UTCTimeRangeType;
import org.oasis_open.docs.ws_calendar.ns.soap.UidConflictType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemResponseType;
import org.oasis_open.docs.ws_calendar.ns.soap.UpdateItemType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public class CalwsHandler extends SoapHandler {
  static String calwsNs = "http://docs.oasis-open.org/ns/wscal/calws-soap";

  static ObjectFactory of = new ObjectFactory();

  /**
   * @param intf the interface
   * @throws WebdavException on soap error
   */
  public CalwsHandler(final CaldavBWIntf intf) {
    super(intf);
  }

  @Override
  protected String getJaxbContextPath() {
    return "org.oasis_open.docs.ws_calendar.ns.soap:" +
           XRDType.class.getPackage().getName();
  }

  /**
   * @param req the request
   * @param resp the response
   * @param pars request parameters
   * @throws WebdavException on soap error
   */
  public void processPost(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final RequestPars pars) {
    try {
      initResponse(resp);

      final UnmarshalResult ur = unmarshal(req);

      Object body = ur.body;
      if (body instanceof JAXBElement) {
        body = ((JAXBElement<?>)body).getValue();
      }

      processRequest(req, resp, (BaseRequestType)body, pars, false);
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  protected JAXBElement<? extends BaseResponseType> processRequest(
                                          final HttpServletRequest req,
                                          final HttpServletResponse resp,
                                          final BaseRequestType breq,
                                          final RequestPars pars,
                                          final boolean multi) {

    try {
      if (breq instanceof MultiOpType) {
        return doMultiOp((MultiOpType)breq, req, resp, pars);
      }

      if (breq instanceof GetPropertiesType) {
        return doGetProperties((GetPropertiesType)breq, resp, multi);
      }

      if (breq instanceof FreebusyReportType) {
        return doFreebusyReport((FreebusyReportType)breq, resp, multi);
      }

      if (breq instanceof CalendarMultigetType) {
        return doCalendarMultiget((CalendarMultigetType)breq, resp, multi);
      }

      if (breq instanceof CalendarQueryType) {
        return doCalendarQuery((CalendarQueryType)breq, resp, multi);
      }

      if (breq instanceof AddItemType) {
        return doAddItem((AddItemType)breq, req, resp, multi);
      }

      if (breq instanceof FetchItemType) {
        return doFetchItem((FetchItemType)breq, req, resp, multi);
      }

      if (breq instanceof DeleteItemType) {
        return doDeleteItem((DeleteItemType)breq, req, resp, multi);
      }

      if (breq instanceof UpdateItemType) {
        return doUpdateItem((UpdateItemType)breq, req, resp, multi);
      }

      throw new WebdavException("Unhandled request");
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private JAXBElement<MultiOpResponseType> doMultiOp(final MultiOpType mo,
                                                     final HttpServletRequest req,
                                                     final HttpServletResponse resp,
                                                     final RequestPars pars) {
    if (debug()) {
      debug("MultiOpType: ");
    }

    try {
      final MultiOpResponseType mor = new MultiOpResponseType();
      final JAXBElement<MultiOpResponseType> jax =
              of.createMultiOpResponse(mor);

      final ArrayOfResponses aor = new ArrayOfResponses();
      mor.setResponses(aor);

      for (final BaseRequestType breq:
           mo.getOperations().getGetPropertiesOrFreebusyReportOrCalendarQuery()) {
        aor.getBaseResponse().add(processRequest(req, resp, breq, pars, true));
      }

      marshal(jax, resp.getOutputStream());

      return jax;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private JAXBElement<GetPropertiesResponseType> doGetProperties(final GetPropertiesType gp,
                               final HttpServletResponse resp,
                               final boolean multi) {
    if (debug()) {
      debug("GetProperties: ");
    }

    try {
      final String url = gp.getHref();

      final GetPropertiesResponseType gpr = new GetPropertiesResponseType();
      final JAXBElement<GetPropertiesResponseType> jax =
              of.createGetPropertiesResponse(gpr);
      gpr.setId(gp.getId());
      gpr.setHref(url);

      if (url != null) {
        final WebdavNsNode calNode = getNsIntf().getNode(url,
                                                         WebdavNsIntf.existanceMust,
                                                         WebdavNsIntf.nodeTypeCollection,
                                                         false);

        if (calNode != null) {
          final CaldavBwNode nd = (CaldavBwNode)calNode;

          ((CaldavBWIntf)getNsIntf()).getCalWSProperties(nd,
                                                         gpr.getChildCollectionOrCreationDateTimeOrDisplayName());
        }

        if (!multi) {
          marshal(jax, resp.getOutputStream());
        }
      }

      return jax;
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private JAXBElement<FreebusyReportResponseType> doFreebusyReport(final FreebusyReportType fr,
                                final HttpServletResponse resp,
                                final boolean multi) {
    if (debug()) {
      debug("FreebusyReport: ");
    }

    final FreebusyReportResponseType frr = new FreebusyReportResponseType();
    frr.setId(fr.getId());
    final JAXBElement<FreebusyReportResponseType> jax =
            of.createFreebusyReportResponse(frr);

    try {
      final String url = fr.getHref();

      buildResponse: {
        if (url == null) {
          frr.setStatus(StatusType.ERROR);
          frr.setMessage("No href supplied");
          break buildResponse;
        }

        final WebdavNsNode elNode = getNsIntf().getNode(url,
                                                        WebdavNsIntf.existanceMust,
                                                        WebdavNsIntf.nodeTypeUnknown,
                                                        false);

        if (!(elNode instanceof CaldavPrincipalNode)) {
          frr.setStatus(StatusType.ERROR);
          frr.setMessage("Only principal href supported");
          break buildResponse;
        }

        final String cua =
                getSysi().principalToCaladdr(getSysi().getPrincipal(url));

        /* Build an icalendar freebusy object out of the parameters */

        final IcalendarType ical = new IcalendarType();
        final VcalendarType vcal = new VcalendarType();

        ical.getVcalendar().add(vcal);

        final VfreebusyType vfb = new VfreebusyType();

        final JAXBElement<VfreebusyType> compel =
                new JAXBElement<>(XcalTags.vfreebusy,
                                  VfreebusyType.class, vfb);
        final ArrayOfComponents aoc = new ArrayOfComponents();

        vcal.setComponents(aoc);
        aoc.getBaseComponent().add(compel);

        /* Use timerange to limit the requested time */

        final CalDAVAuthProperties authp = getSysi().getAuthProperties();

        final UTCTimeRangeType utr = fr.getTimeRange();

        final TimeRange tr = ParseUtil.getPeriod(
                XcalUtil.getIcalFormatDateTime(
                        utr.getStart().toString()),
                XcalUtil.getIcalFormatDateTime(utr.getEnd().toString()),
                java.util.Calendar.DATE,
                authp.getDefaultFBPeriod(),
                java.util.Calendar.DATE,
                authp.getMaxFBPeriod());

        final ArrayOfProperties aop = new ArrayOfProperties();
        vfb.setProperties(aop);

        final DtstartPropType dtstart = new DtstartPropType();
        XcalUtil.initDt(dtstart, tr.getStart().toString(), null);

        final JAXBElement<DtstartPropType> dtstartProp =
                new JAXBElement<>(XcalTags.dtstart,
                                  DtstartPropType.class, dtstart);

        aop.getBasePropertyOrTzid().add(dtstartProp);

        final DtendPropType dtend = new DtendPropType();
        XcalUtil.initDt(dtend, tr.getEnd().toString(), null);

        final JAXBElement<DtendPropType> dtendProp =
                new JAXBElement<>(XcalTags.dtend,
                                  DtendPropType.class, dtend);

        aop.getBasePropertyOrTzid().add(dtendProp);

        /* Add a uid */

        final UidPropType uid = new UidPropType();
        uid.setText(Util.makeRandomString(30, 35));

        final JAXBElement<UidPropType> uidProp =
                new JAXBElement<>(XcalTags.uid,
                                  UidPropType.class, uid);

        aop.getBasePropertyOrTzid().add(uidProp);

        /* Add the cua as the organizer */

        final OrganizerPropType org = new OrganizerPropType();
        org.setCalAddress(cua);

        final JAXBElement<OrganizerPropType> orgProp =
                new JAXBElement<>(XcalTags.organizer,
                                  OrganizerPropType.class, org);

        aop.getBasePropertyOrTzid().add(orgProp);

        /* We should be in as an attendee */

        final AttendeePropType att = new AttendeePropType();
        att.setCalAddress(getSysi().principalToCaladdr(getSysi().getPrincipal()));

        final JAXBElement<AttendeePropType> attProp =
                new JAXBElement<>(XcalTags.attendee,
                                  AttendeePropType.class, att);

        aop.getBasePropertyOrTzid().add(attProp);

        final SysiIcalendar sical = getSysi().fromIcal(null, ical,
                                                       IcalResultType.OneComponent);
        final CalDAVEvent<?> ev = sical.getEvent();

        ev.setScheduleMethod(ScheduleMethods.methodTypeRequest);
        final Set<String> recipients = new TreeSet<>();
        recipients.add(cua);
        ev.setRecipients(recipients);

        final Collection<SchedRecipientResult> srrs =
                getSysi().requestFreeBusy(ev, false);

        if (srrs.size() != 1) {
          frr.setStatus(StatusType.ERROR);
          frr.setMessage("No data returned");
          break buildResponse;
        }

        final SchedRecipientResult sr = srrs.iterator().next();

        frr.setIcalendar(getSysi().toIcalendar(sr.freeBusy, false, null));
        frr.setStatus(StatusType.OK);
      } // buildResponse

      if (!multi) {
        marshal(jax, resp.getOutputStream());
      }

      return jax;
    } catch (final WebdavException we) {
      frr.setStatus(StatusType.ERROR);
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private JAXBElement<CalendarQueryResponseType> doCalendarMultiget(final CalendarMultigetType cm,
                               final HttpServletResponse resp,
                               final boolean multi) {
    if (debug()) {
      debug("CalendarMultiget: ");
    }

    final CalendarQueryResponseType cqr = new CalendarQueryResponseType();
    final JAXBElement<CalendarQueryResponseType> jax =
            of.createCalendarQueryResponse(cqr);
    cqr.setId(cm.getId());

    try {
      final String url = cm.getHref();

      buildResponse: {
        if (url == null) {
          cqr.setStatus(StatusType.ERROR);
          cqr.setMessage("No href supplied");
          break buildResponse;
        }

        final ArrayOfHrefs hrefs = cm.getHrefs();
        if (hrefs == null) {
          break buildResponse;
        }

        final Report rpt = new Report(getNsIntf());

        final Collection<String> badHrefs = new ArrayList<>();

        buildQueryResponse(cqr,
                           rpt.getMgetNodes(hrefs.getHref(), badHrefs),
                           cm.getIcalendar());

        if (badHrefs.isEmpty()) {
          break buildResponse;
        }

        for (final String bh: badHrefs) {
          final MultistatResponseElementType mre =
                  new MultistatResponseElementType();

          mre.setHref(bh);

          cqr.getResponse().add(mre);

          final PropstatType ps = new PropstatType();

          mre.getPropstat().add(ps);

          ps.setStatus(StatusType.NOT_FOUND);
        }
      } // buildResponse
    } catch (final WebdavException we) {
      // Remove any partial results.
      cqr.getResponse().clear();
      errorResponse(cqr, we);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    if (!multi) {
      try {
        marshal(jax, resp.getOutputStream());
      } catch (final Throwable t) {
        if (debug()) {
          error(t);
        }
        throw new WebdavException(t);
      }
    }

    return jax;
  }

  private JAXBElement<CalendarQueryResponseType> doCalendarQuery(final CalendarQueryType cq,
                               final HttpServletResponse resp,
                               final boolean multi) {
    if (debug()) {
      debug("CalendarQuery: ");
    }

    //resp.setHeader("Content-Type", "application/soap+xml");

    final CalendarQueryResponseType cqr = new CalendarQueryResponseType();
    final JAXBElement<CalendarQueryResponseType> jax =
            of.createCalendarQueryResponse(cqr);
    cqr.setId(cq.getId());

    try {
      final String url = cq.getHref();

      buildResponse: {
        if (url == null) {
          cqr.setStatus(StatusType.ERROR);
          cqr.setMessage("No href supplied");
          break buildResponse;
        }

        final Report rpt = new Report(getNsIntf());

        buildQueryResponse(cqr, rpt.query(url, cq), cq.getIcalendar());

        cqr.setStatus(StatusType.OK);
      } // buildResponse
    } catch (final WebdavException we) {
      // Remove any partial results.
      cqr.getResponse().clear();
      errorResponse(cqr, we);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    if (!multi) {
      try {
        marshal(jax, resp.getOutputStream());
      } catch (final Throwable t) {
        if (debug()) {
          error(t);
        }
        throw new WebdavException(t);
      }
    }

    return jax;
  }

  private JAXBElement<AddItemResponseType> doAddItem(final AddItemType ai,
                         final HttpServletRequest req,
                         final HttpServletResponse resp,
                         final boolean multi) {
    if (debug()) {
      debug("AddItem: cal=" + ai.getHref());
    }

    final AddItemResponseType air = new AddItemResponseType();
    final JAXBElement<AddItemResponseType> jax = of.createAddItemResponse(air);
    air.setId(ai.getId());

    addEntity: {
      /* Manufacture a name */

      final UidPropType uidp = (UidPropType)XcalUtil.findProperty(
                 XcalUtil.findEntity(ai.getIcalendar()), XcalTags.uid);

      if ((uidp == null) || (uidp.getText() == null)) {
        air.setStatus(StatusType.ERROR);
        break addEntity;
      }

      final String entityPath = Util.buildPath(false, ai.getHref(), "/",
                                               getIntf().makeName(uidp.getText()) + ".ics");

      final WebdavNsNode elNode = getNsIntf().getNode(entityPath,
                                                      WebdavNsIntf.existanceNot,
                                                      WebdavNsIntf.nodeTypeEntity,
                                                      false);

      try {
        /*
         *     String ifStag = Headers.ifScheduleTagMatch(req);
               boolean noInvites = req.getHeader("Bw-NoInvites") != null; // based on header?
         */
        if ((elNode != null) &&
            getIntf().putEvent(resp,
                               (CaldavComponentNode)elNode,
                               ai.getIcalendar(),
                               true,
                               false,  // noinvites
                               null,   // ifStag
                               null)) {
          air.setStatus(StatusType.OK);
          air.setHref(elNode.getUri());
          air.setChangeToken(((CaldavBwNode)elNode).getEtokenValue());
        } else {
          air.setStatus(StatusType.ERROR);
        }
      } catch (final WebdavException we) {
        errorResponse(air, we);
      } catch (final Throwable t) {
        if (debug()) {
          error(t);
        }
        errorResponse(air, new WebdavException(t));
      }
    } // addEntity

    if (!multi) {
      try {
        marshal(jax, resp.getOutputStream());
      } catch (final WebdavException we) {
        throw we;
      } catch (final Throwable t) {
        throw new WebdavException(t);
      }
    }

    return jax;
  }

  private JAXBElement<FetchItemResponseType> doFetchItem(final FetchItemType fi,
                           final HttpServletRequest req,
                           final HttpServletResponse resp,
                           final boolean multi) {
    if (debug()) {
      debug("FetchItem:       cal=" + fi.getHref());
    }

    final FetchItemResponseType fir = new FetchItemResponseType();
    final JAXBElement<FetchItemResponseType> jax =
            of.createFetchItemResponse(fir);
    fir.setId(fi.getId());

    try {
      final WebdavNsNode elNode = getNsIntf().getNode(fi.getHref(),
                                                      WebdavNsIntf.existanceMust,
                                                      WebdavNsIntf.nodeTypeEntity,
                                                      false);

      if (elNode == null) {
        errorResponse(fir, new WebdavNotFound());
      } else {
        final CaldavComponentNode comp = (CaldavComponentNode)elNode;

        fir.setStatus(StatusType.OK);
        fir.setChangeToken(comp.getEtokenValue());

        final CalDAVEvent<?> ev = comp.getEvent();
        fir.setIcalendar(getIntf().getSysi().toIcalendar(ev, false, null));
      }
    } catch (final WebdavException we) {
      errorResponse(fir, we);
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      errorResponse(fir, new WebdavException(t));
    }

    if (!multi) {
      try {
        marshal(jax, resp.getOutputStream());
      } catch (final WebdavException we) {
        throw we;
      } catch (final Throwable t) {
        throw new WebdavException(t);
      }
    }

    return jax;
  }

  private JAXBElement<DeleteItemResponseType> doDeleteItem(final DeleteItemType di,
                           final HttpServletRequest req,
                           final HttpServletResponse resp,
                           final boolean multi) {
    if (debug()) {
      debug("DeleteItem:       cal=" + di.getHref());
    }

    final DeleteItemResponseType dir = new DeleteItemResponseType();
    final JAXBElement<DeleteItemResponseType> jax =
            of.createDeleteItemResponse(dir);
    dir.setId(di.getId());

    try {
      final WebdavNsNode node = getNsIntf().getNode(di.getHref(),
                                                    WebdavNsIntf.existanceMust,
                                                    WebdavNsIntf.nodeTypeUnknown,
                                                    false);

      if (node == null) {
        errorResponse(dir, new WebdavNotFound());
      } else if (node instanceof CaldavCalNode) {
        // Don't allow that here
        errorResponse(dir, new WebdavUnauthorized());
      } else {
        getNsIntf().delete(node);
        dir.setStatus(StatusType.OK);
      }
    } catch (final WebdavException we) {
      errorResponse(dir, we);
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      errorResponse(dir, new WebdavException(t));
    }

    if (!multi) {
      try {
        marshal(jax, resp.getOutputStream());
      } catch (final WebdavException we) {
        throw we;
      } catch (final Throwable t) {
        throw new WebdavException(t);
      }
    }

    return jax;
  }

  private JAXBElement<UpdateItemResponseType> doUpdateItem(final UpdateItemType ui,
                            final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final boolean multi) {
    if (debug()) {
      debug("UpdateItem:       cal=" + ui.getHref());
    }

    final UpdateItemResponseType uir = new UpdateItemResponseType();
    final JAXBElement<UpdateItemResponseType> jax =
            of.createUpdateItemResponse(uir);
    uir.setId(ui.getId());

    try {
      final WebdavNsNode elNode = getNsIntf().getNode(ui.getHref(),
                                                      WebdavNsIntf.existanceMust,
                                                      WebdavNsIntf.nodeTypeEntity,
                                                      false);

      updateItem: {
        if (elNode == null) {
          uir.setStatus(StatusType.ERROR);
          uir.setMessage("Href not found");
          break updateItem;
        }

        final CaldavComponentNode compNode = (CaldavComponentNode)elNode;
        final String changeToken = ui.getChangeToken();

        if (changeToken == null) {
          // Why can this happen? minOccurs = 1
          uir.setStatus(StatusType.ERROR);

          final ErrorResponseType er = new ErrorResponseType();

          final MissingChangeTokenType ec = new MissingChangeTokenType();
          er.setError(of.createMissingChangeToken(ec));
          uir.setErrorResponse(er);
          uir.setMessage("Missing token");
          break updateItem;
        }

        // XXX Just do a straight compare for the moment

        final String compEtoken = compNode.getEtokenValue();
        if (!changeToken.equals(compEtoken)) {
          uir.setStatus(StatusType.ERROR);

          final ErrorResponseType er = new ErrorResponseType();

          final MismatchedChangeTokenType ec = new MismatchedChangeTokenType();
          er.setError(of.createMismatchedChangeToken(ec));
          uir.setErrorResponse(er);
          uir.setMessage("Token mismatch");
          if (debug()) {
            debug("Try reindex for " + compNode.getEvent().getUid());
          }
          getSysi().reindexEvent(compNode.getEvent());

          break updateItem;
        }

        final CalDAVEvent<?> ev = compNode.getEvent();

        if (debug()) {
          debug("event: " + ev);
        }

        final UpdateResult ur = getSysi().updateEvent(ev, ui.getSelect());

        if (ur.getOk()) {
          uir.setStatus(StatusType.OK);
        } else {
          uir.setStatus(StatusType.ERROR);
          uir.setMessage(ur.getReason());
        }
      } //updateItem

    } catch (final WebdavException we) {
      errorResponse(uir, we);
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      errorResponse(uir, new WebdavException(t));
    }

    if (!multi) {
      try {
        marshal(jax, resp.getOutputStream());
      } catch (final WebdavException we) {
        throw we;
      } catch (final Throwable t) {
        throw new WebdavException(t);
      }
    }

    return jax;
  }

  private void buildQueryResponse(final CalendarQueryResponseType cqr,
                                  final Collection<WebdavNsNode> nodes,
                                  final IcalendarType pattern) {
    if (nodes == null) {
      return;
    }

    for (final WebdavNsNode curnode: nodes) {
      final MultistatResponseElementType mre =
              new MultistatResponseElementType();

      mre.setHref(curnode.getUri());
      mre.setChangeToken(((CaldavBwNode)curnode).getEtokenValue());

      cqr.getResponse().add(mre);

      final PropstatType ps = new PropstatType();

      mre.getPropstat().add(ps);

      ps.setStatus(StatusType.OK);
      ps.setMessage(getStatus(curnode.getStatus(), null));

      if (!curnode.getExists()) {
        continue;
      }

      if (!(curnode instanceof CaldavComponentNode)) {
        continue;
      }

      /* For the moment always return the full calendar data. Need to
       * implement the properties thing
       */

      final MultistatusPropElementType mpe =
              new MultistatusPropElementType();

      ps.getProp().add(mpe);

      final CalendarDataResponseType cdr = new CalendarDataResponseType();

      mpe.setCalendarData(cdr);

      final CalDAVEvent<?> ev = ((CaldavComponentNode)curnode).getEvent();

      cdr.setIcalendar(getIntf().getSysi().toIcalendar(ev, false, pattern));
      cdr.setContentType("application/calendar+xml");
      cdr.setVersion("2.0");
    }
  }

  private void errorResponse(final BaseResponseType br,
                             final WebdavException we) {
    br.setStatus(StatusType.ERROR);
    br.setMessage(we.getMessage());

    final ErrorResponseType er = new ErrorResponseType();

    setError: {
      if (we instanceof WebdavForbidden) {
        final ForbiddenType ec = new ForbiddenType();
        er.setError(of.createForbidden(ec));
        break setError;
      }

      if (we instanceof WebdavNotFound) {
        final TargetDoesNotExistType ec = new TargetDoesNotExistType();
        er.setError(of.createTargetDoesNotExist(ec));
        break setError;
      }

      if (we instanceof WebdavUnauthorized) {
        final TargetNotEntityType ec = new TargetNotEntityType();
        er.setError(of.createTargetNotEntity(ec));
        break setError;
      }

      final QName etag = we.getErrorTag();

      if (etag == null) {
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
        final InvalidCalendarCollectionLocationType ec =
                new InvalidCalendarCollectionLocationType();
        er.setError(of.createInvalidCalendarCollectionLocation(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.noUidConflict)) {
        final UidConflictType uc= new UidConflictType();
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
        final InvalidCalendarDataType ec = new InvalidCalendarDataType();
        er.setError(of.createInvalidCalendarData(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.validCalendarObjectResource)) {
        final InvalidCalendarObjectResourceType ec =
                new InvalidCalendarObjectResourceType();
        er.setError(of.createInvalidCalendarObjectResource(ec));
        break setError;
      }

      if (etag.equals(CaldavTags.validFilter)) {
        final InvalidFilterType iv = new InvalidFilterType();
        iv.setDetail(we.getMessage());
        er.setError(of.createInvalidFilter(iv));
        //break setError;
      }
    } // setError

    if (er.getError() != null) {
      br.setErrorResponse(er);
    }
  }
}
