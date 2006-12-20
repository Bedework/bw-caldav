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
package org.bedework.caldav.bwserver;

import org.apache.log4j.Logger;
import org.bedework.caldav.server.SysIntf;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventProxy;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.BwSystem;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.exc.CalFacadeAccessException;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.filter.BwEntityTypeFilter;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.filter.BwOrFilter;
import org.bedework.calfacade.svc.BwSubscription;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calfacade.util.ChangeTable;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.calsvci.CalSvcIPars;
import org.bedework.davdefs.CaldavTags;
import org.bedework.icalendar.IcalMalformedException;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.XmlUtil;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

/** Bedework implementation of SysIntf.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class BwSysIntfImpl implements SysIntf {
  private boolean debug;

  protected transient Logger log;

  /* Prefix for our properties */
  private String envPrefix;

  private String account;

  private String hostPortContext;

  private String principalCollectionSetUri;

  /* These two set after a call to getSvci()
   */
  private IcalTranslator trans;
  private CalSvcI svci;

  private String urlPrefix;

  public void init(HttpServletRequest req,
                   String envPrefix,
                   String account,
                   boolean debug) throws WebdavIntfException {
    try {
      this.envPrefix = envPrefix;
      this.account = account;
      this.debug = debug;

      StringBuffer sb = new StringBuffer();

      sb.append("http://");
      sb.append(req.getLocalName());

      int port = req.getLocalPort();
      if (port != 80) {
        sb.append(":");
        sb.append(port);
      }

      sb.append(req.getContextPath());
      sb.append("/");

      hostPortContext = sb.toString();

      BwSystem sys = getSvci().getSyspars();
      String userRootPath = sys.getUserCalendarRoot();

      principalCollectionSetUri = "/" + userRootPath + "/";
      urlPrefix = WebdavUtils.getUrlPrefix(req);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPrincipalRoot()
   */
  public String getPrincipalRoot() throws WebdavIntfException {
    try {
      return getSvci().getSyspars().getPrincipalRoot();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getUserPrincipalRoot()
   */
  public String getUserPrincipalRoot() throws WebdavIntfException {
    try {
      return getSvci().getSyspars().getUserPrincipalRoot();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getGroupPrincipalRoot()
   */
  public String getGroupPrincipalRoot() throws WebdavIntfException {
    try {
      return getSvci().getSyspars().getGroupPrincipalRoot();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeUserHref(java.lang.String)
   */
  public String makeUserHref(String id) throws WebdavIntfException {
    return getUrlPrefix() + "/" + getUserPrincipalRoot() + "/" + id;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeGroupHref(java.lang.String)
   */
  public String makeGroupHref(String id) throws WebdavIntfException {
    return getUrlPrefix() + "/" + getGroupPrincipalRoot() + "/" + id;
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavIntfException {
    try {
      return getSvci().getSyspars().getDirectoryBrowsingDisallowed();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public String caladdrToUser(String caladdr) throws WebdavIntfException {
    try {
      return getSvci().caladdrToUser(caladdr);
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public CalUserInfo getCalUserInfo(String caladdr) throws WebdavIntfException {
    try {
      String account = caladdrToUser(caladdr);
      BwSystem sys = getSvci().getSyspars();
      String userHomePath = "/" + sys.getUserCalendarRoot() +
                            "/" + account + "/";
      String defaultCalendarPath = userHomePath + sys.getUserDefaultCalendar();
      String inboxPath = userHomePath + sys.getUserInbox();
      String outboxPath = userHomePath + sys.getUserOutbox();

      return new CalUserInfo(account,
                             userHomePath,
                             defaultCalendarPath,
                             inboxPath,
                             outboxPath);
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection<String> getPrincipalCollectionSet(String resourceUri)
          throws WebdavIntfException {
    try {
      StringBuffer sb = new StringBuffer();

      sb.append(hostPortContext);
      sb.append(principalCollectionSetUri);

      ArrayList<String> al = new ArrayList<String>();

      al.add(sb.toString());

      return al;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection<CalUserInfo> getPrincipals(String resourceUri,
                                               PrincipalPropertySearch pps)
          throws WebdavIntfException {
    if (pps.applyToPrincipalCollectionSet) {
      throw new WebdavIntfException("unimplemented");
    }

    if (!resourceUri.endsWith("/")) {
      resourceUri += "/";
    }

    ArrayList<CalUserInfo> principals = new ArrayList<CalUserInfo>();

    if (!resourceUri.equals(principalCollectionSetUri)) {
      return principals;
    }

    /* If we don't support any of the properties in the searches we don't match
     */
    String caladdr = null;

    for (PrincipalPropertySearch.PropertySearch ps: pps.propertySearches) {
      for (WebdavProperty prop: ps.props) {
        if (!CaldavTags.calendarUserAddressSet.equals(prop.getTag())) {
          return principals;
        }
      }

      String mval;
      try {
        mval = XmlUtil.getElementContent(ps.match);
      } catch (Throwable t) {
        throw new WebdavIntfException("org.bedework.caldavintf.badvalue");
      }

      if (debug) {
        debugMsg("Try to match " + mval);
      }

      if ((caladdr != null) && (!caladdr.equals(mval))) {
        return principals;
      }

      caladdr = mval;
    }

    /* For the moment only support the above
     */
    CalUserInfo cui = getCalUserInfo(caladdr);

    // XXX This needs to do a search of a system user directory - probably ldap

    principals.add(cui);

    return principals;
  }

  public boolean validUser(String account) throws WebdavIntfException {
    // XXX do this
    return true;
  }

  public boolean validGroup(String account) throws WebdavIntfException {
    // XXX do this
    return true;
  }

  /* ====================================================================
   *                   Scheduling
   * ==================================================================== */

  public ScheduleResult schedule(BwEvent event) throws WebdavIntfException {
    try {
      event.setOwner(svci.findUser(account, false));
      return getSvci().schedule(event);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw WebdavIntfException.badRequest("Duplicate-guid");
      }
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection<BwEventProxy> addEvent(BwCalendar cal,
                                           BwEvent event,
                                           Collection<BwEventProxy> overrides,
                                           boolean rollbackOnError) throws WebdavIntfException {
    try {
      return getSvci().addEvent(cal, event, overrides, rollbackOnError).failedOverrides;
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw WebdavIntfException.badRequest("Duplicate-guid");
      }
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void updateEvent(BwEvent event,
                          Collection<BwEventProxy> overrides,
                          ChangeTable changes) throws WebdavIntfException {
    try {
      getSvci().updateEvent(event, overrides, changes);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw WebdavIntfException.badRequest("Duplicate-guid");
      }
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection<EventInfo> getEvents(BwCalendar cal,
                                         boolean getEvents,
                                         boolean getTodos,
                                         boolean getJournals,
                                         BwDateTime startDate, BwDateTime endDate,
                                         RecurringRetrievalMode recurRetrieval)
          throws WebdavIntfException {
    try {
      BwSubscription sub = BwSubscription.makeSubscription(cal);

      if ((startDate == null) && (endDate == null)) {
        return getSvci().getEvents(sub, recurRetrieval);
      }

      BwFilter filter = makeFilter(getEvents, getTodos, getJournals);
      return getSvci().getEvents(sub, filter, startDate, endDate,
                                 recurRetrieval);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (WebdavIntfException wdie) {
      throw wdie;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public EventInfo getEvent(BwCalendar cal, String val,
                            RecurringRetrievalMode recurRetrieval)
              throws WebdavIntfException {
    try {
      return getSvci().getEvent(cal, val, recurRetrieval);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void deleteEvent(BwEvent ev) throws WebdavIntfException {
    try {
      getSvci().deleteEvent(ev, true);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void deleteCalendar(BwCalendar cal) throws WebdavIntfException {
    try {
      getSvci().deleteCalendar(cal);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public BwFreeBusy getFreeBusy(BwCalendar cal,
                                String account,
                                BwDateTime start,
                                BwDateTime end) throws WebdavException {
    try {
      BwUser user = getSvci().findUser(account, false);
      if (user == null) {
        throw WebdavIntfException.unauthorized();
      }

      if (getSvci().isUserRoot(cal)) {
        cal = null;
      }

      return getSvci().getFreeBusy(null, cal, user, start, end,
                                   null, false);
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (WebdavIntfException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public CurrentAccess checkAccess(BwShareableDbentity ent,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException {
    try {
      return getSvci().checkAccess(ent, desiredAccess, returnResult);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    }
  }

  public void updateAccess(BwCalendar cal,
                           Collection<Ace> aces) throws WebdavIntfException {
    try {
      getSvci().changeAccess(cal, aces);
      getSvci().updateCalendar(cal);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void updateAccess(BwEvent ev,
                           Collection<Ace> aces) throws WebdavIntfException{
    try {
      getSvci().changeAccess(ev, aces);
      getSvci().updateEvent(ev, null, null);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void makeCollection(String name, boolean calendarCollection,
                             String parentPath) throws WebdavIntfException {
    BwCalendar newcal = new BwCalendar();

    newcal.setName(name);
    newcal.setCalendarCollection(calendarCollection);

    try {
      getSvci().addCalendar(newcal, parentPath);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public BwCalendar getCalendar(String path) throws WebdavIntfException {
    try {
      return getSvci().getCalendar(path);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection<BwCalendar> getCalendars(BwCalendar cal) throws WebdavIntfException {
    try {
      return getSvci().getCalendars(cal);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Calendar toCalendar(EventInfo ev) throws WebdavIntfException {
    getSvci(); // Ensure open
    try {
      return trans.toIcal(ev, Icalendar.methodTypeNone);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Icalendar fromIcal(BwCalendar cal, Reader rdr) throws WebdavIntfException {
    getSvci(); // Ensure open
    try {
      return trans.fromIcal(cal, rdr);
    } catch (IcalMalformedException ime) {
      throw WebdavIntfException.badRequest(ime.getMessage());
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public CalTimezones getTimezones() throws WebdavIntfException {
    try {
      return getSvci().getTimezones();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public TimeZone getDefaultTimeZone() throws WebdavIntfException {
    try {
      return getSvci().getTimezones().getDefaultTimeZone();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public String toStringTzCalendar(String tzid) throws WebdavIntfException {
    try {
      return trans.toStringTzCalendar(tzid);
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public int getMaxUserEntitySize() throws WebdavIntfException {
    try {
      return getSvci().getSyspars().getMaxUserEntitySize();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void close() throws WebdavIntfException {
    close(svci);
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

  private BwFilter makeFilter(boolean getEvents,
                              boolean getTodos,
                              boolean getJournals) throws WebdavIntfException {
    int numTypes = 0;

    if (getEvents) {
      numTypes++;
    }

    if (getTodos) {
      numTypes++;
    }

    if (getJournals) {
      numTypes++;
    }

    if (numTypes == 0) {
      throw new WebdavIntfException("org.bedework.caldav.badquery");
    }

    if (numTypes == 3) {
      // No filter
      return null;
    }

    BwFilter filter = null;
    BwOrFilter orFilter = null;

    if (numTypes > 1) {
      orFilter = new BwOrFilter();
    }

    if (getEvents) {
      filter = BwEntityTypeFilter.eventFilter(null);
      if (numTypes > 1) {
        orFilter.addChild(filter);
      }
    }

    if (getTodos) {
      filter = BwEntityTypeFilter.todoFilter(null);
      if (numTypes > 1) {
        orFilter.addChild(filter);
      }
    }

    if (getJournals) {
      filter = BwEntityTypeFilter.journalFilter(null);
      if (numTypes > 1) {
        orFilter.addChild(filter);
      }
    }

    if (numTypes > 1) {
      return orFilter;
    }

    return filter;
  }

  /**
   * @return CalSvcI
   * @throws WebdavIntfException
   */
  private CalSvcI getSvci() throws WebdavIntfException {
    if (svci != null) {
      if (!svci.isOpen()) {
        try {
          svci.open();
          svci.beginTransaction();
        } catch (Throwable t) {
          throw new WebdavIntfException(t);
        }
      }

      return svci;
    }

    try {
      /* account is what we authenticated with.
       * user, if non-null, is the user calendar we want to access.
       */
      CalSvcIPars pars = new CalSvcIPars(account,
                                         account,
                                         null,
                                         envPrefix,
                                         false,   // publicAdmin
                                         false,  // adminCanEditAllPublicCategories
                                         false,  // adminCanEditAllPublicLocations
                                         false,  // adminCanEditAllPublicSponsors
                                         true,    // caldav
                                         null, // synchId
                                         debug);
      svci = new CalSvcFactoryDefault().getSvc(pars);

      svci.open();
      svci.beginTransaction();

      trans = new IcalTranslator(svci.getIcalCallback(), debug);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }

    return svci;
  }

  private void close(CalSvcI svci) throws WebdavIntfException {
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
      throw new WebdavIntfException(t);
    }

    try {
      svci.close();
    } catch (Throwable t) {
      svci = null;
      throw new WebdavIntfException(t);
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
