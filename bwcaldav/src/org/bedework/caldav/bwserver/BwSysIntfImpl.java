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

import org.bedework.caldav.server.SysIntf;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.BwSystem;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.CalFacadeAccessException;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.CalFacadeException;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.svc.BwSubscription;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.calsvci.CalSvcIPars;
import org.bedework.icalendar.IcalMalformedException;
import org.bedework.icalendar.IcalTranslator;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cmt.access.Acl.CurrentAccess;

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

  /* Prefix for our properties */
  private String envPrefix;

  private String account;

  private String hostPortContext;

  /* These two set after a call to getSvci()
   */
  private IcalTranslator trans;
  private CalSvcI svci;

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
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
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
      String sysid = getSvci().getSysid();

      String addrPart = sysid.substring(sysid.indexOf("@"));

      if (!caladdr.endsWith(addrPart)) {
        return caladdr;
      }

      String acc = caladdr.substring(0, caladdr.length() - addrPart.length());

      // XXX We need a way to validate this against a user directory
      // Yet another pluggable class

      return acc;
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
      String userHomePath = sys.getUserCalendarRoot() + "/" + account;
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

  public Collection getPrincipalCollectionSet(String resourceUri)
          throws WebdavIntfException {
    try {
      StringBuffer sb = new StringBuffer();

      BwSystem sys = getSvci().getSyspars();
      String userRootPath = sys.getUserCalendarRoot();

      sb.append(hostPortContext);
      sb.append("/");
      sb.append(userRootPath);
      sb.append("/");

      ArrayList al = new ArrayList();

      al.add(sb.toString());

      return al;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection getPrincipals(String resourceUri,
                           PrincipalPropertySearch pps)
          throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public boolean validUser(String account) throws WebdavIntfException {
    // XXX do this
    return true;
  }

  public boolean validGroup(String account) throws WebdavIntfException {
    // XXX do this
    return true;
  }

  public void addEvent(BwCalendar cal,
                       BwEvent event,
                       Collection overrides) throws WebdavIntfException {
    try {
      getSvci().addEvent(cal, event, overrides);
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

  public void updateEvent(BwEvent event) throws WebdavIntfException {
    try {
      getSvci().updateEvent(event);
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

  /** Get events in the given calendar with recurrences expanded
   *
   * @param cal
   * @return Collection of BwEvent
   * @throws WebdavIntfException
   */
  public Collection getEventsExpanded(BwCalendar cal) throws WebdavIntfException {
    try {
      BwSubscription sub = BwSubscription.makeSubscription(cal);

      Collection events = getSvci().getEvents(sub, CalFacadeDefs.retrieveRecurExpanded);

      if (events == null) {
        return new ArrayList();
      }

      return events;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection getEvents(BwCalendar cal,
                              int recurRetrieval) throws WebdavIntfException {
    try {
      BwSubscription sub = BwSubscription.makeSubscription(cal);

      return getSvci().getEvents(sub, recurRetrieval);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection getEvents(BwCalendar cal,
                              BwDateTime startDate, BwDateTime endDate,
                              int recurRetrieval) throws WebdavIntfException {
    try {
      BwSubscription sub = BwSubscription.makeSubscription(cal);

      return getSvci().getEvents(sub, null, startDate, endDate, recurRetrieval);
    } catch (CalFacadeAccessException cfae) {
      throw WebdavIntfException.forbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavIntfException(cfe);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection findEventsByName(BwCalendar cal, String val)
              throws WebdavIntfException {
    try {
      return getSvci().findEventsByName(cal, val);
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

  public BwFreeBusy getFreeBusy(BwCalendar cal,
                                String account,
                                BwDateTime start,
                                BwDateTime end) throws WebdavException {
    try {
      BwUser user = getSvci().findUser(account);
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
                           Collection aces) throws WebdavIntfException {
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
                           Collection aces) throws WebdavIntfException{
    try {
      getSvci().changeAccess(ev, aces);
      getSvci().updateEvent(ev);
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

  public Calendar toCalendar(BwEvent ev) throws WebdavIntfException {
    getSvci(); // Ensure open
    try {
      return trans.toIcal(ev);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Calendar toCalendar(Collection evs) throws WebdavIntfException {
    getSvci(); // Ensure open
    try {
      return trans.toIcal(evs);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection fromIcal(BwCalendar cal, Reader rdr) throws WebdavIntfException {
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
}
