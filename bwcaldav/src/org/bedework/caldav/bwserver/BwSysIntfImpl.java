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
package org.bedework.caldav.bwserver;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CalDAVResource;
import org.bedework.caldav.server.Organizer;
import org.bedework.caldav.server.PropertyHandler;
import org.bedework.caldav.server.SysIntfReader;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.PropertyHandler.PropertyType;
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.RetrievalMode;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SystemProperties;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventObj;
import org.bedework.calfacade.BwEventProxy;
import org.bedework.calfacade.BwOrganizer;
import org.bedework.calfacade.BwPrincipal;
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.BwSystem;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.RecurringRetrievalMode.Rmode;
import org.bedework.calfacade.ScheduleResult.ScheduleRecipientResult;
import org.bedework.calfacade.base.TimeRange;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.exc.CalFacadeAccessException;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.exc.CalFacadeStaleStateException;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.calsvci.CalSvcIPars;
import org.bedework.calsvci.EventsI.CopyMoveStatus;
import org.bedework.icalendar.IcalMalformedException;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavUnauthorized;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.UrlHandler;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.cmt.calendar.IcalDefs.IcalComponentType;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VFreeBusy;

import org.apache.log4j.Logger;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Bedework implementation of SysIntf.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class BwSysIntfImpl implements SysIntf {
  private boolean debug;

  protected transient Logger log;

  protected AccessPrincipal currentPrincipal;

  /* These two set after a call to getSvci()
   */
  private IcalTranslator trans;
  private CalSvcI svci;

  private UrlHandler urlHandler;

  private SystemProperties sysProperties = new SystemProperties();

  private CalDAVConfig conf;

  public void init(HttpServletRequest req,
                   String account,
                   CalDAVConfig conf,
                   boolean debug) throws WebdavException {
    try {
      this.conf = conf;
      this.debug = debug;

      urlHandler = new UrlHandler(req, true);

      // Call to set up ThreadLocal variables

      CalSvcI svc = getSvci(account);

      currentPrincipal = svc.getUsersHandler().getUser(account);

      BwSystem sys = svc.getSysparsHandler().get();

      if (sys != null) {
        sysProperties.setMaxUserEntitySize(sys.getMaxUserEntitySize());
        sysProperties.setMaxInstances(sys.getMaxInstances());
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.sysinterface.SysIntf#getSystemProperties()
   */
  public SystemProperties getSystemProperties() throws WebdavException {
    return sysProperties;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPrincipal()
   */
  public AccessPrincipal getPrincipal() throws WebdavException {
    return currentPrincipal;
  }

  private static class MyPropertyHandler extends PropertyHandler {
    private final static HashMap<QName, PropertyTagEntry> propertyNames =
      new HashMap<QName, PropertyTagEntry>();

    public Map<QName, PropertyTagEntry> getPropertyNames() {
      return propertyNames;
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPropertyHandler(org.bedework.caldav.server.PropertyHandler.PropertyType)
   */
  public PropertyHandler getPropertyHandler(PropertyType ptype) throws WebdavException {
    return new MyPropertyHandler();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getUrlHandler()
   */
  public UrlHandler getUrlHandler() {
    return urlHandler;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getUrlPrefix()
   * /
  public String getUrlPrefix() {
    return urlPrefix;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getRelativeUrls()
   * /
  public boolean getRelativeUrls() {
    return relativeUrls;
  }*/

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#isPrincipal(java.lang.String)
   */
  public boolean isPrincipal(String val) throws WebdavException {
    try {
      return getSvci().getDirectories().isPrincipal(val);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPrincipal(java.lang.String)
   */
  public AccessPrincipal getPrincipal(String href) throws WebdavException {
    try {
      return getSvci().getDirectories().getPrincipal(href);
    } catch (CalFacadeException cfe) {
      if (cfe.getMessage().equals(CalFacadeException.principalNotFound)) {
        throw new WebdavNotFound(href);
      }
      throw new WebdavException(cfe);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeHref(java.lang.String, boolean)
   */
  public String makeHref(String id, int whoType) throws WebdavException {
    try {
      return getUrlHandler().prefix(getSvci().getDirectories().makePrincipalUri(id, whoType));
//      return getUrlPrefix() + getSvci().getDirectories().makePrincipalUri(id, whoType);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getGroups(java.lang.String, java.lang.String)
   */
  public Collection<String>getGroups(String rootUrl,
                                     String principalUrl) throws WebdavException {
    try {
      return getSvci().getDirectories().getGroups(rootUrl, principalUrl);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavException {
    try {
      return getSvci().getSysparsHandler().get().getDirectoryBrowsingDisallowed();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#caladdrToUser(java.lang.String)
   */
  public AccessPrincipal caladdrToPrincipal(String caladdr) throws WebdavException {
    try {
      // XXX This needs to work for groups.
      return getSvci().getDirectories().caladdrToPrincipal(caladdr);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#userToCaladdr(java.lang.String)
   */
  public String userToCaladdr(String account) throws WebdavException {
    try {
      return getSvci().getDirectories().userToCaladdr(account);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCalPrincipalInfo(edu.rpi.cmt.access.AccessPrincipal)
   */
  public CalPrincipalInfo getCalPrincipalInfo(AccessPrincipal principal) throws WebdavException {
    try {
      if (principal == null) {
        return null;
      }

      BwPrincipal p = getSvci().getUsersHandler().getPrincipal(principal.getPrincipalRef(),
                                                               false);
      if (p == null) {
        return null;
      }

      if (!(p instanceof BwUser)) {
        // XXX Cannot handle this yet
        return null;
      }

      BwUser u = (BwUser)p;
      if (u == null) {
        return null;
      }

      // SCHEDULE - just get home path and get default cal from user prefs.
      BwSystem sys = getSvci().getSysparsHandler().get();
      BwCalendar cal = getSvci().getCalendarsHandler().getHome(u, false);
      if (cal == null) {
        return null;
      }

      String userHomePath = cal.getPath() + "/";

      //String userHomePath = "/" + sys.getUserCalendarRoot() +
      //                      "/" + account + "/";
      String defaultCalendarPath = userHomePath + sys.getUserDefaultCalendar();
      String inboxPath = userHomePath + sys.getUserInbox() + "/";
      String outboxPath = userHomePath + sys.getUserOutbox() + "/";

      return new CalPrincipalInfo(p,
                                  userHomePath,
                                  defaultCalendarPath,
                                  inboxPath,
                                  outboxPath);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Collection<String> getPrincipalCollectionSet(String resourceUri)
          throws WebdavException {
    try {
      ArrayList<String> al = new ArrayList<String>();

      al.add(getSvci().getDirectories().getPrincipalRoot());

      return al;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Collection<CalPrincipalInfo> getPrincipals(String resourceUri,
                                               PrincipalPropertySearch pps)
          throws WebdavException {
    ArrayList<CalPrincipalInfo> principals = new ArrayList<CalPrincipalInfo>();

    if (pps.applyToPrincipalCollectionSet) {
      /* I believe it's valid (if unhelpful) to return nothing
       */
      return principals;
    }

    if (!resourceUri.endsWith("/")) {
      resourceUri += "/";
    }

    try {
      String proot = getSvci().getDirectories().getPrincipalRoot();

      if (!proot.endsWith("/")) {
        proot += "/";
      }

      if (!resourceUri.equals(proot)) {
        return principals;
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    /* If we don't support any of the properties in the searches we don't match.
     *
     * Currently we only support calendarUserAddressSet or calendarHomeSet.
     *
     * For calendarUserAddressSet the value to match must be a valid CUA
     *
     * For calendarHomeSet it must be a valid home uri
     */
    String matchVal = null;
    boolean calendarUserAddressSet = false;
    boolean calendarHomeSet = false;

    for (PrincipalPropertySearch.PropertySearch ps: pps.propertySearches) {
      for (WebdavProperty prop: ps.props) {
        if (CaldavTags.calendarUserAddressSet.equals(prop.getTag())) {
          calendarUserAddressSet = true;
        } else if (CaldavTags.calendarHomeSet.equals(prop.getTag())) {
          calendarHomeSet = true;
        } else {
          return principals;
        }
      }

      if (calendarUserAddressSet && calendarHomeSet) {
        return principals;
      }

      String mval;
      try {
        mval = XmlUtil.getElementContent(ps.match);
      } catch (Throwable t) {
        throw new WebdavException("org.bedework.caldavintf.badvalue");
      }

      if (debug) {
        debugMsg("Try to match " + mval);
      }

      if ((matchVal != null) && (!matchVal.equals(mval))) {
        return principals;
      }

      matchVal = mval;
    }

    CalPrincipalInfo cui = null;

    if (calendarUserAddressSet) {
      cui = getCalPrincipalInfo(caladdrToPrincipal(matchVal));
    } else {
      String path = getUrlHandler().unprefix(matchVal);

      CalDAVCollection col = getCollection(path);
      if (col != null) {
        cui = getCalPrincipalInfo(col.getOwner());
      }
    }

    if (cui != null) {
      principals.add(cui);
    }

    return principals;
  }

  public boolean validUser(String account) throws WebdavException {
    try {
      return getSvci().getDirectories().validUser(account);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public boolean validGroup(String account) throws WebdavException {
    // XXX do this
    return true;
  }

  /* ====================================================================
   *                   Scheduling
   * ==================================================================== */

  public Collection<String> getFreebusySet() throws WebdavException {
    try {
      Collection<BwCalendar> cals = svci.getScheduler().getFreebusySet();
      Collection<String> hrefs = new ArrayList<String>();

      if (cals == null) {
        return hrefs;
      }

      for (BwCalendar cal: cals) {
        hrefs.add(getUrlHandler().prefix(cal.getPath()));
        //hrefs.add(getUrlPrefix() + cal.getPath());
      }

      return hrefs;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Collection<SchedRecipientResult> schedule(CalDAVEvent ev) throws WebdavException {
    try {
      ScheduleResult sr;

      BwEvent event = getEvent(ev);
      event.setOwnerHref(currentPrincipal.getPrincipalRef());
      if (Icalendar.itipReplyMethodType(event.getScheduleMethod())) {
        sr = getSvci().getScheduler().scheduleResponse(getEvinfo(ev));
      } else {
        sr = getSvci().getScheduler().schedule(getEvinfo(ev), null, false);
      }

      return checkStatus(sr);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw new WebdavBadRequest("Duplicate-guid");
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Events
   * ==================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#addEvent(org.bedework.caldav.server.CalDAVEvent, boolean, boolean)
   */
  public Collection<CalDAVEvent> addEvent(CalDAVEvent ev,
                                          boolean noInvites,
                                          boolean rollbackOnError) throws WebdavException {
    try {
      /* Is the event a scheduling object? */

      Collection<BwEventProxy> bwevs =
             getSvci().getEventsHandler().add(getEvinfo(ev), noInvites,
                                              false,  // scheduling - inbox
                                              rollbackOnError).failedOverrides;

      if (bwevs == null) {
        return null;
      }

      Collection<CalDAVEvent> evs = new ArrayList<CalDAVEvent>();

      for (BwEvent bwev: bwevs) {
        evs.add(new BwCalDAVEvent(this, new EventInfo(bwev)));
      }

      return evs;
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw new WebdavForbidden(CaldavTags.noUidConflict);
      }
      if (CalFacadeException.duplicateName.equals(cfe.getMessage())) {
        throw new WebdavBadRequest("Duplicate-name");
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void updateEvent(CalDAVEvent event) throws WebdavException {
    try {
      EventInfo ei = getEvinfo(event);

      getSvci().getEventsHandler().update(ei, false,
                                          null,
                                          null, ei.getChangeset());
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw new WebdavBadRequest("Duplicate-guid");
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getEvents(org.bedework.caldav.server.CalDAVCollection, org.bedework.calfacade.filter.BwFilter, org.bedework.caldav.server.SysIntf.RetrievalMode)
   */
  public Collection<CalDAVEvent> getEvents(CalDAVCollection col,
                                           BwFilter filter,
                                           RetrievalMode recurRetrieval)
          throws WebdavException {
    try {
      Collection<EventInfo> bwevs =
             getSvci().getEventsHandler().getEvents(unwrap(col), filter,
                                                    null, null,
                                                    getRrm(recurRetrieval));

      if (bwevs == null) {
        return null;
      }

      Collection<CalDAVEvent> evs = new ArrayList<CalDAVEvent>();

      for (EventInfo ei: bwevs) {
        evs.add(new BwCalDAVEvent(this, ei));
      }

      return evs;
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getEvent(org.bedework.caldav.server.CalDAVCollection, java.lang.String, org.bedework.caldav.server.SysIntf.RetrievalMode)
   */
  public CalDAVEvent getEvent(CalDAVCollection col, String val,
                              RetrievalMode recurRetrieval)
              throws WebdavException {
    try {
      EventInfo ei = getSvci().getEventsHandler().get(col.getPath(), val,
                                                      getRrm(recurRetrieval));

      if (ei == null) {
        return null;
      }

      return new BwCalDAVEvent(this, ei);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void deleteEvent(CalDAVEvent ev,
                          boolean scheduleReply) throws WebdavException {
    try {
      if (ev == null) {
        return;
      }

      getSvci().getEventsHandler().delete(getEvinfo(ev), scheduleReply);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#requestFreeBusy(org.bedework.caldav.server.CalDAVEvent)
   */
  public Collection<SchedRecipientResult> requestFreeBusy(CalDAVEvent val) throws WebdavException {
    try {
      ScheduleResult sr;

      BwEvent ev = getEvent(val);
      if (currentPrincipal != null) {
        ev.setOwnerHref(currentPrincipal.getPrincipalRef());
      }

      if (Icalendar.itipReplyMethodType(ev.getScheduleMethod())) {
        sr = getSvci().getScheduler().scheduleResponse(getEvinfo(val));
      } else {
        sr = getSvci().getScheduler().schedule(getEvinfo(val), null, false);
      }

      return checkStatus(sr);
    } catch (CalFacadeAccessException cfae) {
      if (debug) {
        error(cfae);
      }
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw new WebdavBadRequest("Duplicate-guid");
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void getSpecialFreeBusy(String cua, String user,
                                 RequestPars pars,
                                 TimeRange tr,
                                 Writer wtr) throws WebdavException {
    if (cua == null) {
      cua = userToCaladdr(user);
    }

    pars.recipients.add(cua);

    BwOrganizer org = new BwOrganizer();
    org.setOrganizerUri(cua);

    BwEvent ev = new BwEventObj();
    ev.setDtstart(tr.getStart());
    ev.setDtend(tr.getEnd());

    ev.setEntityType(CalFacadeDefs.entityTypeFreeAndBusy);

    ev.setScheduleMethod(Icalendar.methodTypeRequest);

    ev.setRecipients(pars.recipients);
    ev.setOriginator(pars.originator);
    ev.setOrganizer(org);

    Collection<SchedRecipientResult> srrs = requestFreeBusy(
                         new BwCalDAVEvent(this, new EventInfo(ev)));

    for (SchedRecipientResult srr: srrs) {
      // We expect one only
      BwCalDAVEvent rfb = (BwCalDAVEvent)srr.freeBusy;
      if (rfb != null) {
        rfb.getEv().setOrganizer(org);

        try {
          VFreeBusy vfreeBusy = VFreeUtil.toVFreeBusy(rfb.getEv());
          net.fortuna.ical4j.model.Calendar ical = IcalTranslator.newIcal(Icalendar.methodTypeReply);
          ical.getComponents().add(vfreeBusy);
          IcalTranslator.writeCalendar(ical, wtr);
        } catch (Throwable t) {
          if (debug) {
            error(t);
          }
          throw new WebdavException(t);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFreeBusy(org.bedework.caldav.server.CalDAVCollection, int, java.lang.String, org.bedework.calfacade.BwDateTime, org.bedework.calfacade.BwDateTime)
   */
  public Calendar getFreeBusy(final CalDAVCollection col,
                             final int depth,
                             final String account,
                             final BwDateTime start,
                             final BwDateTime end) throws WebdavException {
    try {
      BwUser user = getSvci().getUsersHandler().getUser(account);
      if (user == null) {
        throw new WebdavUnauthorized();
      }

      BwCalendar bwCol = unwrap(col);

      int calType = bwCol.getCalType();

      if (!bwCol.getCollectionInfo().allowFreeBusy) {
        throw new WebdavForbidden(WebdavTags.supportedReport);
      }

      Collection<BwCalendar> cals = new ArrayList<BwCalendar>();

      if (calType == BwCalendar.calTypeCalendarCollection) {
        cals.add(bwCol);
      } else if (depth == 0) {
        /* Cannot return anything */
      } else {
        /* Make new cal object with just calendar collections as children */

        for (BwCalendar ch: getSvci().getCalendarsHandler().getChildren(bwCol)) {
          // For depth 1 we only add calendar collections
          if ((depth > 1) ||
              (ch.getCalType() == BwCalendar.calTypeCalendarCollection)) {
            cals.add(ch);
          }
        }
      }

      BwEvent fb;
      if (cals.isEmpty()) {
        // Return an empty object
        fb = new BwEventObj();
        fb.setEntityType(CalFacadeDefs.entityTypeFreeAndBusy);
        fb.setDtstart(start);
        fb.setDtend(end);
      } else {
        fb = getSvci().getScheduler().getFreeBusy(cals, user, start, end);
      }

      VFreeBusy vfreeBusy = VFreeUtil.toVFreeBusy(fb);
      if (vfreeBusy != null) {
        Calendar ical = IcalTranslator.newIcal(Icalendar.methodTypeNone);
        ical.getComponents().add(vfreeBusy);

        return ical;
      }

      return null;
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public CurrentAccess checkAccess(WdEntity ent,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException {
    try {
      if (ent instanceof CalDAVCollection) {
        return getSvci().checkAccess(unwrap((CalDAVCollection)ent),
                                     desiredAccess, returnResult);
      }

      if (ent instanceof CalDAVEvent) {
        return getSvci().checkAccess(getEvent((CalDAVEvent)ent),
                                     desiredAccess, returnResult);
      }

      if (ent instanceof CalDAVResource) {
        return getSvci().checkAccess(getRsrc((CalDAVResource)ent),
                                     desiredAccess, returnResult);
      }

      throw new WebdavBadRequest();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    }
  }

  public void updateAccess(CalDAVEvent ev,
                           Acl acl) throws WebdavException{
    try {
      getSvci().changeAccess(getEvent(ev), acl.getAces(), true);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Collections
   * ==================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#newCollectionObject(boolean, java.lang.String)
   */
  public CalDAVCollection newCollectionObject(boolean isCalendarCollection,
                                              String parentPath) throws WebdavException {
    BwCalendar col = new BwCalendar();

    if (isCalendarCollection) {
      col.setCalType(BwCalendar.calTypeCalendarCollection);
    } else {
      col.setCalType(BwCalendar.calTypeFolder);
    }

    col.setColPath(parentPath);

    return new BwCalDAVCollection(this, col);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateAccess(org.bedework.caldav.server.CalDAVCollection, edu.rpi.cmt.access.Acl)
   */
  public void updateAccess(CalDAVCollection col,
                           Acl acl) throws WebdavException {
    try {
      getSvci().changeAccess(unwrap(col), acl.getAces(), true);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeCollection(org.bedework.caldav.server.CalDAVCollection)
   */
  public int makeCollection(CalDAVCollection col) throws WebdavException {
    BwCalendar bwCol = unwrap(col);

    try {
      getSvci().getCalendarsHandler().add(bwCol, bwCol.getColPath());
      return HttpServletResponse.SC_CREATED;
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      String msg = cfe.getMessage();
      if (CalFacadeException.duplicateCalendar.equals(msg)) {
        throw new WebdavForbidden(WebdavTags.resourceMustBeNull);
      }
      if (CalFacadeException.illegalCalendarCreation.equals(msg)) {
        throw new WebdavForbidden();
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.caldav.server.CalDAVCollection, org.bedework.caldav.server.CalDAVCollection, boolean, boolean)
   */
  public void copyMove(CalDAVCollection from,
                       CalDAVCollection to,
                       boolean copy,
                       boolean overwrite) throws WebdavException {
    try {
      BwCalendar bwFrom = unwrap(from);
      BwCalendar bwTo = unwrap(to);

      if (!copy) {
        /* Move the from collection to the new location "to".
         * If the parent calendar is the same in both cases, this is just a rename.
         */
        if ((bwFrom.getColPath() == null) || (bwTo.getColPath() == null)) {
          throw new WebdavForbidden("Cannot move root");
        }

        if (bwFrom.getColPath().equals(bwTo.getColPath())) {
          // Rename
          getSvci().getCalendarsHandler().rename(bwFrom, to.getName());
          return;
        }
      }
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.caldav.server.CalDAVEvent, org.bedework.caldav.server.CalDAVCollection, java.lang.String, boolean, boolean)
   */
  public boolean copyMove(CalDAVEvent from,
                          CalDAVCollection to,
                          String name,
                          boolean copy,
                          boolean overwrite) throws WebdavException {
    CopyMoveStatus cms;
    try {
      cms = getSvci().getEventsHandler().copyMoveNamed(getEvinfo(from),
                                                       unwrap(to), name,
                                                       copy, overwrite, false);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    if (cms == CopyMoveStatus.changedUid) {
      throw new WebdavForbidden("Cannot change uid");
    }

    if (cms == CopyMoveStatus.duplicateUid) {
      throw new WebdavForbidden("duplicate uid");
    }

    if (cms == CopyMoveStatus.destinationExists) {
      if (name == null) {
        name = from.getName();
      }
      throw new WebdavForbidden("Destination exists: " + name);
    }

    if (cms == CopyMoveStatus.ok) {
      return false;
    }

    if (cms == CopyMoveStatus.created) {
      return true;
    }

    throw new WebdavException("Unexpected response from copymove");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCollection(java.lang.String)
   */
  public CalDAVCollection getCollection(String path) throws WebdavException {
    try {
      BwCalendar col = getSvci().getCalendarsHandler().get(path);

      if (col == null) {
        return null;
      }

      return new BwCalDAVCollection(this, col);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void updateCollection(CalDAVCollection col) throws WebdavException {
    try {
      getSvci().getCalendarsHandler().update(unwrap(col));
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw new WebdavBadRequest("Duplicate-guid");
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#deleteCollection(org.bedework.caldav.server.CalDAVCollection)
   */
  public void deleteCollection(CalDAVCollection col) throws WebdavException {
    try {
      getSvci().getCalendarsHandler().delete(unwrap(col), true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCollections(org.bedework.caldav.server.CalDAVCollection)
   */
  public Collection<CalDAVCollection> getCollections(CalDAVCollection col) throws WebdavException {
    try {
      Collection<BwCalendar> bwch = getSvci().getCalendarsHandler().getChildren(unwrap(col));

      Collection<CalDAVCollection> ch = new ArrayList<CalDAVCollection>();

      if (bwch == null) {
        return ch;
      }

      for (BwCalendar c: bwch) {
        ch.add(new BwCalDAVCollection(this, c));
      }

      return ch;
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#resolveAlias(org.bedework.caldav.server.CalDAVCollection)
   */
  public void resolveAlias(CalDAVCollection col) throws WebdavException {
    try {
      getSvci().getCalendarsHandler().resolveAlias(unwrap(col), true, false);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Files
   * ==================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#newResourceObject(java.lang.String)
   */
  public CalDAVResource newResourceObject(String parentPath) throws WebdavException {
    CalDAVResource r = new BwCalDAVResource(this, null);

    r.setParentPath(parentPath);

    return r;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#putFile(org.bedework.caldav.server.CalDAVCollection, org.bedework.caldav.server.CalDAVResource)
   */
  public void putFile(CalDAVCollection coll,
                      CalDAVResource val) throws WebdavException {
    try {
      getSvci().getResourcesHandler().save(coll.getPath(), getRsrc(val));
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFile(org.bedework.caldav.server.CalDAVCollection, java.lang.String)
   */
  public CalDAVResource getFile(CalDAVCollection coll,
                                String name) throws WebdavException {
    try {
      BwResource rsrc = getSvci().getResourcesHandler().get(coll.getPath() +
                                                            "/" + name);

      if (rsrc == null) {
        return null;
      }

      return new BwCalDAVResource(this, rsrc);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFileContent(org.bedework.caldav.server.CalDAVResource)
   */
  public void getFileContent(CalDAVResource val) throws WebdavException {
    try {
      getSvci().getResourcesHandler().getContent(getRsrc(val));
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFiles(org.bedework.caldav.server.CalDAVCollection)
   */
  public Collection<CalDAVResource> getFiles(CalDAVCollection coll) throws WebdavException {
    try {
      Collection<BwResource> bwrs =
            getSvci().getResourcesHandler().getAll(coll.getPath());

      if (bwrs == null) {
        return  null;
      }

      Collection<CalDAVResource> rs = new ArrayList<CalDAVResource>();

      for (BwResource r: bwrs) {
        rs.add(new BwCalDAVResource(this, r));
      }

      return rs;
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateFile(org.bedework.caldav.server.CalDAVResource, boolean)
   */
  public void updateFile(CalDAVResource val,
                         boolean updateContent) throws WebdavException {
    try {
      getSvci().getResourcesHandler().update(getRsrc(val), updateContent);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#deleteFile(org.bedework.caldav.server.CalDAVResource)
   */
  public void deleteFile(CalDAVResource val) throws WebdavException {
    try {
      getSvci().getResourcesHandler().delete(val.getParentPath() + "/" +
                                             val.getName());
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMoveFile(org.bedework.caldav.server.CalDAVResource, java.lang.String, java.lang.String, boolean, boolean)
   */
  public boolean copyMoveFile(CalDAVResource from,
                              String toPath,
                              String name,
                              boolean copy,
                              boolean overwrite) throws WebdavException {
    try {
      return getSvci().getResourcesHandler().copyMove(getRsrc(from),
                                                      toPath, name,
                                                      copy,
                                                      overwrite);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Calendar toCalendar(CalDAVEvent ev) throws WebdavException {
    try {
      return trans.toIcal(getEvinfo(ev), getEvent(ev).getScheduleMethod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#writeCalendar(java.util.Collection, int, java.io.Writer)
   */
  public void writeCalendar(Collection<CalDAVEvent> evs,
                            int method,
                            Writer wtr) throws WebdavException {
    try {
      Collection<EventInfo> bwevs = new ArrayList<EventInfo>();

      for (CalDAVEvent cde: evs) {
        BwCalDAVEvent bcde = (BwCalDAVEvent)cde;

        bwevs.add(bcde.getEvinfo());
      }

      Calendar ical = trans.toIcal(bwevs, method);
      IcalTranslator.writeCalendar(ical, wtr);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#fromIcal(org.bedework.caldav.server.CalDAVCollection, java.io.Reader)
   */
  public SysiIcalendar fromIcal(CalDAVCollection col, Reader rdr) throws WebdavException {
    getSvci(); // Ensure open
    try {
      Icalendar ic = trans.fromIcal(unwrap(col), new SysIntfReader(rdr, debug));

      return new MySysiIcalendar(this, ic);
    } catch (IcalMalformedException ime) {
      throw new WebdavBadRequest(ime.getMessage());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public String toStringTzCalendar(String tzid) throws WebdavException {
    try {
      return trans.toStringTzCalendar(tzid);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public String tzidFromTzdef(String val) throws WebdavException {
    try {
      getSvci(); // Ensure open
      StringReader sr = new StringReader(val);

      // This call automatically saves the timezone in the db
      Icalendar ic = trans.fromIcal(null, sr);

      if ((ic == null) ||
          (ic.size() != 0) || // No components other than timezones
          (ic.getTimeZones().size() != 1)) {
        if (debug) {
          debugMsg("Not icalendar");
        }
        throw new WebdavForbidden(CaldavTags.validCalendarData, "Not icalendar");
      }

      TimeZone tz = ic.getTimeZones().iterator().next();

      return tz.getID();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    }
  }

  public int getMaxUserEntitySize() throws WebdavException {
    try {
      return getSvci().getSysparsHandler().get().getMaxUserEntitySize();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void close() throws WebdavException {
    close(svci);
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

  /**
   * @param sr
   * @return recipient results
   * @throws WebdavException
   */
  private Collection<SchedRecipientResult> checkStatus(ScheduleResult sr) throws WebdavException {
    if ((sr.errorCode == null) ||
        (sr.errorCode == CalFacadeException.schedulingNoRecipients)) {
      Collection<SchedRecipientResult> srrs = new ArrayList<SchedRecipientResult>();

      for (ScheduleRecipientResult bwsrr: sr.recipientResults) {
        SchedRecipientResult srr = new SchedRecipientResult();

        srr.recipient = bwsrr.recipient;
        srr.status = bwsrr.status;

        if (bwsrr.freeBusy != null) {
          srr.freeBusy = new BwCalDAVEvent(this, new EventInfo(bwsrr.freeBusy));
        }

        srrs.add(srr);
      }

      return srrs;
    }

    if (sr.errorCode == CalFacadeException.schedulingBadMethod) {
      throw new WebdavForbidden(CaldavTags.validCalendarData, "Bad METHOD");
    }

    if (sr.errorCode == CalFacadeException.schedulingBadAttendees) {
      throw new WebdavForbidden(CaldavTags.attendeeAllowed, "Bad attendees");
    }

    if (sr.errorCode == CalFacadeException.schedulingAttendeeAccessDisallowed) {
      throw new WebdavForbidden(CaldavTags.attendeeAllowed, "attendeeAccessDisallowed");
    }

    throw new WebdavForbidden(sr.errorCode);
  }

  private BwCalendar unwrap(CalDAVCollection col) throws WebdavException {
    if (!(col instanceof BwCalDAVCollection)) {
      throw new WebdavBadRequest("Unknown implemenation of BwCalDAVCollection" +
                                 col.getClass());
    }

    return ((BwCalDAVCollection)col).getCol();
  }

  private EventInfo getEvinfo(CalDAVEvent ev) throws WebdavException {
    if (ev == null) {
      return null;
    }

    return ((BwCalDAVEvent)ev).getEvinfo();
  }

  private BwEvent getEvent(CalDAVEvent ev) throws WebdavException {
    if (ev == null) {
      return null;
    }

    return ((BwCalDAVEvent)ev).getEv();
  }

  private BwResource getRsrc(CalDAVResource rsrc) throws WebdavException {
    if (rsrc == null) {
      return null;
    }

    return ((BwCalDAVResource)rsrc).getRsrc();
  }

  /**
   * @return CalSvcI
   * @throws WebdavException
   */
  private CalSvcI getSvci() throws WebdavException {
    if (!svci.isOpen()) {
      try {
        svci.open();
        svci.beginTransaction();
      } catch (Throwable t) {
        throw new WebdavException(t);
      }
    }

    return svci;
  }

  private CalSvcI getSvci(String account) throws WebdavException {
    try {
      /* account is what we authenticated with.
       * user, if non-null, is the user calendar we want to access.
       */
      CalSvcIPars pars = new CalSvcIPars(account,
                                         null,    // calsuite
                                         false,   // publicAdmin
                                         "root".equals(account),  // allow SuperUser
                                         false,  // adminCanEditAllPublicCategories
                                         false,  // adminCanEditAllPublicLocations
                                         false,  // adminCanEditAllPublicSponsors
                                         true,    // sessionless
                                         null, // synchId
                                         conf,
                                         debug);
      svci = new CalSvcFactoryDefault().getSvc(pars);

      svci.open();
      svci.beginTransaction();

      trans = new IcalTranslator(svci.getIcalCallback(), debug);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return svci;
  }

  private void close(CalSvcI svci) throws WebdavException {
    if ((svci == null) || !svci.isOpen()) {
      return;
    }

    try {
      svci.endTransaction();
    } catch (Throwable t) {
      try {
        svci.close();
      } catch (Throwable t1) {
      }
      svci = null;
      if (t instanceof CalFacadeStaleStateException) {
        throw new WebdavException(HttpServletResponse.SC_CONFLICT);
      }

      throw new WebdavException(t);
    }

    try {
      svci.close();
    } catch (Throwable t) {
      svci = null;
      throw new WebdavException(t);
    }
  }

  private RecurringRetrievalMode getRrm(RetrievalMode rm) {
    if (rm == null) {
      return new RecurringRetrievalMode(Rmode.overrides);
    }

    if (rm.expanded) {
      /* expand with time range */
      return new RecurringRetrievalMode(Rmode.expanded,
                                        rm.start, rm.end);
    }

    if (rm.limitRecurrenceSet) {
      /* Only return master event and overrides in range */

      return new RecurringRetrievalMode(Rmode.overrides,
                                        rm.start, rm.end);
    }

    /* Return master + overrides */
    return new RecurringRetrievalMode(Rmode.overrides);
  }

  /**
   * @author douglm
   *
   */
  private static class MySysiIcalendar extends SysiIcalendar {
    private Icalendar ic;
    private BwSysIntfImpl sysi;

    private Iterator icIterator;

    private MySysiIcalendar(BwSysIntfImpl sysi, Icalendar ic) {
      this.ic = ic;
    }

    public String getProdid() {
      return ic.getProdid();
    }

    public String getVersion() {
      return ic.getVersion();
    }

    public String getCalscale() {
      return ic.getCalscale();
    }

    public String getMethod() {
      return ic.getMethod();
    }

    public Collection<TimeZone> getTimeZones() {
      return ic.getTimeZones();
    }

    public Collection<Object> getComponents() {
      return ic.getComponents();
    }

    public IcalComponentType getComponentType() {
      return ic.getComponentType();
    }

    public int getMethodType() {
      return ic.getMethodType();
    }

    public int getMethodType(String val) {
      return Icalendar.getMethodType(val);
    }

    public String getMethodName(int mt) {
      return Icalendar.getMethodName(mt);
    }

    public Organizer getOrganizer() {
      BwOrganizer bworg = ic.getOrganizer();

      if (bworg == null) {
        return null;
      }

      return new Organizer(bworg.getCn(), bworg.getDir(),
                           bworg.getLanguage(),
                           bworg.getSentBy(),
                           bworg.getOrganizerUri());
    }

    public CalDAVEvent getEvent() throws WebdavException {
      //if ((size() != 1) || (getComponentType() != ComponentType.event)) {
      //  throw new RuntimeException("org.bedework.icalendar.component.not.event");
      //}

      EventInfo ei = (EventInfo)iterator().next();

      if (ei == null) {
        return null;
      }

      return new BwCalDAVEvent(sysi, ei);
    }

    public Iterator iterator() {
      return this;
    }

    public int size() {
      return ic.size();
    }

    public boolean validItipMethodType() {
      return validItipMethodType(getMethodType());
    }

    public boolean requestMethodType() {
      return itipRequestMethodType(getMethodType());
    }

    public boolean replyMethodType() {
      return itipReplyMethodType(getMethodType());
    }

    public boolean itipRequestMethodType(int mt) {
      return Icalendar.itipRequestMethodType(mt);
    }

    public boolean itipReplyMethodType(int mt) {
      return Icalendar.itipReplyMethodType(mt);
    }

    public boolean validItipMethodType(int val) {
      return Icalendar.validItipMethodType(val);
    }

    /* ====================================================================
     *                        Iterator methods
     * ==================================================================== */

    public boolean hasNext() {
      return getIcIterator().hasNext();
    }

    public WdEntity next() {
      Object o = getIcIterator().next();

      if (!(o instanceof EventInfo)) {
        return null;
      }

      EventInfo ei = (EventInfo)o;

      if (ei == null) {
        return null;
      }

      try {
        return new BwCalDAVEvent(sysi, ei);
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private Iterator getIcIterator() {
      if (icIterator == null) {
        icIterator = ic.iterator();
      }

      return icIterator;
    }
  }

  /* ====================================================================
   *                        Protected methods
   * ==================================================================== */

  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void trace(String msg) {
    getLogger().debug(msg);
  }

  protected void debugMsg(String msg) {
    getLogger().debug(msg);
  }

  protected void warn(String msg) {
    getLogger().warn(msg);
  }

  protected void error(Throwable t) {
    getLogger().error(this, t);
  }

  protected void logIt(String msg) {
    getLogger().info(msg);
  }
}
