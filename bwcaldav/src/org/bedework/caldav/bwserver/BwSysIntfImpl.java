/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
import org.bedework.calfacade.BwEventProxy;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.BwSystem;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.BwUserInfo;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.env.CalEnvFactory;
import org.bedework.calfacade.env.CalEnvI;
import org.bedework.calfacade.exc.CalFacadeAccessException;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.exc.CalFacadeStaleStateException;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.svc.BwSubscription;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calfacade.util.ChangeTable;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.calsvci.CalSvcIPars;
import org.bedework.calsvci.CalSvcI.CopyMoveStatus;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.IcalMalformedException;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavUnauthorized;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.XmlUtil;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;

import org.apache.log4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Bedework implementation of SysIntf.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class BwSysIntfImpl implements SysIntf {
  private boolean debug;

  protected transient Logger log;

  /* Prefix for our properties */
  private String envPrefix;

  private CalEnvI env;

  private String account;

  //private String principalCollectionSetUri;
  private String userPrincipalCollectionSetUri;
  private String groupPrincipalCollectionSetUri;

  /* These two set after a call to getSvci()
   */
  private IcalTranslator trans;
  private CalSvcI svci;

  private String urlPrefix;

  public void init(HttpServletRequest req,
                   String envPrefix,
                   String account,
                   boolean debug) throws WebdavException {
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

      String hostPortContext;

      hostPortContext = sb.toString();

      //BwSystem sys = getSvci().getSyspars();
      //String userRootPath = sys.getUserCalendarRoot();

      //principalCollectionSetUri = "/" + userRootPath + "/";
      userPrincipalCollectionSetUri = hostPortContext + getUserPrincipalRoot() +
                                      "/";
      groupPrincipalCollectionSetUri = hostPortContext + getGroupPrincipalRoot() +
                                       "/";
      urlPrefix = WebdavUtils.getUrlPrefix(req);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPrincipalRoot()
   */
  public String getPrincipalRoot() throws WebdavException {
    try {
      return getSvci().getSyspars().getPrincipalRoot();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getUserPrincipalRoot()
   */
  public String getUserPrincipalRoot() throws WebdavException {
    try {
      return getSvci().getSyspars().getUserPrincipalRoot();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getGroupPrincipalRoot()
   */
  public String getGroupPrincipalRoot() throws WebdavException {
    try {
      return getSvci().getSyspars().getGroupPrincipalRoot();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeUserHref(java.lang.String)
   */
  public String makeUserHref(String id) throws WebdavException {
    StringBuffer sb = new StringBuffer(getUrlPrefix());

    String root = getUserPrincipalRoot();
    if (!root.startsWith("/")) {
      sb.append("/");
    }

    sb.append(root);
    sb.append("/");
    sb.append(id);

    return sb.toString();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeGroupHref(java.lang.String)
   */
  public String makeGroupHref(String id) throws WebdavException {
    StringBuffer sb = new StringBuffer(getUrlPrefix());

    String root = getGroupPrincipalRoot();
    if (!root.startsWith("/")) {
      sb.append("/");
    }

    sb.append(root);
    sb.append("/");
    sb.append(id);

    return sb.toString();
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavException {
    try {
      return getSvci().getSyspars().getDirectoryBrowsingDisallowed();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#caladdrToUser(java.lang.String)
   */
  public String caladdrToUser(String caladdr) throws WebdavException {
    try {
      return getSvci().caladdrToUser(caladdr);
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
      return getSvci().userToCaladdr(account);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCalUserInfo(java.lang.String, boolean)
   */
  public CalUserInfo getCalUserInfo(String account,
                                    boolean getDirInfo) throws WebdavException {
    try {
      if (account == null) {
        return null;
      }

      BwSystem sys = getSvci().getSyspars();
      String userHomePath = "/" + sys.getUserCalendarRoot() +
                            "/" + account + "/";
      String defaultCalendarPath = userHomePath + sys.getUserDefaultCalendar();
      String inboxPath = userHomePath + sys.getUserInbox() + "/";
      String outboxPath = userHomePath + sys.getUserOutbox() + "/";

      BwUserInfo dirInfo = null;

      if (getDirInfo) {
        dirInfo = getSvci().getDirInfo(account);
      }

      return new CalUserInfo(account,
                             userHomePath,
                             defaultCalendarPath,
                             inboxPath,
                             outboxPath,
                             dirInfo);
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

      al.add(userPrincipalCollectionSetUri);
      al.add(groupPrincipalCollectionSetUri);

      return al;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Collection<CalUserInfo> getPrincipals(String resourceUri,
                                               PrincipalPropertySearch pps)
          throws WebdavException {
    ArrayList<CalUserInfo> principals = new ArrayList<CalUserInfo>();

    if (pps.applyToPrincipalCollectionSet) {
      /* I believe it's valid (if unhelpful) to return nothing
       */
      return principals;
    }

    if (!resourceUri.endsWith("/")) {
      resourceUri += "/";
    }

    if (!resourceUri.equals(userPrincipalCollectionSetUri)) {
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
        throw new WebdavException("org.bedework.caldavintf.badvalue");
      }

      if (debug) {
        debugMsg("Try to match " + mval);
      }

      if ((caladdr != null) && (!caladdr.equals(mval))) {
        return principals;
      }

      caladdr = mval;
    }

    CalUserInfo cui = getCalUserInfo(caladdrToUser(caladdr), true);

    principals.add(cui);

    return principals;
  }

  public boolean validUser(String account) throws WebdavException {
    try {
      return getSvci().validUser(account);
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

  public ScheduleResult schedule(BwEvent event) throws WebdavException {
    try {
      event.setOwner(svci.findUser(account, false));
      if (Icalendar.itipReplyMethodType(event.getScheduleMethod())) {
        return getSvci().scheduleResponse(event);
      }

      return getSvci().schedule(event);
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

  public Collection<BwEventProxy> addEvent(BwCalendar cal,
                                           BwEvent event,
                                           Collection<BwEventProxy> overrides,
                                           boolean rollbackOnError) throws WebdavException {
    try {
      return getSvci().addEvent(cal, event, overrides, rollbackOnError).failedOverrides;
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      if (CalFacadeException.duplicateGuid.equals(cfe.getMessage())) {
        throw new WebdavBadRequest("Duplicate-guid");
      }
      if (CalFacadeException.duplicateName.equals(cfe.getMessage())) {
        throw new WebdavForbidden(CaldavTags.noUidConflict);
      }
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void updateEvent(BwEvent event,
                          Collection<BwEventProxy> overrides,
                          ChangeTable changes) throws WebdavException {
    try {
      getSvci().updateEvent(event, overrides, changes);
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

  public Collection<EventInfo> getEvents(BwCalendar cal,
                                         BwFilter filter,
                                         RecurringRetrievalMode recurRetrieval)
          throws WebdavException {
    try {
      BwSubscription sub = BwSubscription.makeSubscription(cal);

      return getSvci().getEvents(sub, filter, null, null,
                                 recurRetrieval);
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

  public EventInfo getEvent(BwCalendar cal, String val,
                            RecurringRetrievalMode recurRetrieval)
              throws WebdavException {
    try {
      return getSvci().getEvent(cal, val, recurRetrieval);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void deleteEvent(BwEvent ev) throws WebdavException {
    try {
      getSvci().deleteEvent(ev, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void deleteCalendar(BwCalendar cal) throws WebdavException {
    try {
      getSvci().deleteCalendar(cal, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public ScheduleResult requestFreeBusy(BwEvent val) throws WebdavException {
    try {
      val.setOwner(svci.findUser(account, false));
      if (Icalendar.itipReplyMethodType(val.getScheduleMethod())) {
        return getSvci().scheduleResponse(val);
      }

      return getSvci().schedule(val);
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

  public BwFreeBusy getFreeBusy(BwCalendar cal,
                                String account,
                                BwDateTime start,
                                BwDateTime end) throws WebdavException {
    try {
      BwUser user = getSvci().findUser(account, false);
      if (user == null) {
        throw new WebdavUnauthorized();
      }

      if (getSvci().isUserRoot(cal)) {
        cal = null;
      }

      return getSvci().getFreeBusy(null, cal, user, start, end,
                                   null, false);
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
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
                           Collection<Ace> aces) throws WebdavException {
    try {
      getSvci().changeAccess(cal, aces);
      getSvci().updateCalendar(cal);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void updateAccess(BwEvent ev,
                           Collection<Ace> aces) throws WebdavException{
    try {
      getSvci().changeAccess(ev, aces);
      getSvci().updateEvent(ev, null, null);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public int makeCollection(String name, boolean calendarCollection,
                             String parentPath) throws WebdavException {
    BwCalendar newcal = new BwCalendar();

    newcal.setName(name);
    newcal.setCalendarCollection(calendarCollection);

    try {
      getSvci().addCalendar(newcal, parentPath);
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
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.calfacade.BwCalendar, org.bedework.calfacade.BwCalendar, boolean)
   */
  public void copyMove(BwCalendar from,
                       BwCalendar to,
                       boolean copy,
                       boolean overwrite) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.calfacade.BwEvent, org.bedework.calfacade.BwCalendar, java.lang.String, boolean)
   */
  public boolean copyMove(BwEvent from, Collection<BwEventProxy>overrides,
                          BwCalendar to,
                          String name,
                          boolean copy,
                          boolean overwrite) throws WebdavException {
    CopyMoveStatus cms;
    try {
      cms = getSvci().copyMoveNamed(from, overrides, to, name,
                                    copy, overwrite);
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

  public BwCalendar getCalendar(String path) throws WebdavException {
    try {
      return getSvci().getCalendar(path);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Collection<BwCalendar> getCalendars(BwCalendar cal) throws WebdavException {
    try {
      return getSvci().getCalendars(cal);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Calendar toCalendar(EventInfo ev) throws WebdavException {
    try {
      return trans.toIcal(ev, Icalendar.methodTypeNone);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Icalendar fromIcal(BwCalendar cal, Reader rdr) throws WebdavException {
    getSvci(); // Ensure open
    try {
      return trans.fromIcal(cal, rdr);
    } catch (IcalMalformedException ime) {
      throw new WebdavBadRequest(ime.getMessage());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public CalTimezones getTimezones() throws WebdavException {
    try {
      return getSvci().getTimezones();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public TimeZone getDefaultTimeZone() throws WebdavException {
    try {
      return getSvci().getTimezones().getDefaultTimeZone();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
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

  public int getMaxUserEntitySize() throws WebdavException {
    try {
      return getSvci().getSyspars().getMaxUserEntitySize();
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
   * @return CalSvcI
   * @throws WebdavException
   */
  private CalSvcI getSvci() throws WebdavException {
    if (svci != null) {
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

    try {
      String runAsUser = account;
      boolean superUser = false;

      if (account == null) {
        runAsUser = getEnv().getAppProperty("run.as.user");
      } else if (account.equals("root")) {
        superUser = true;
      }

      /* account is what we authenticated with.
       * user, if non-null, is the user calendar we want to access.
       */
      CalSvcIPars pars = new CalSvcIPars(account,
                                         runAsUser,
                                         null,    // calsuite
                                         envPrefix,
                                         false,   // publicAdmin
                                         false,  // adminCanEditAllPublicCategories
                                         false,  // adminCanEditAllPublicLocations
                                         false,  // adminCanEditAllPublicSponsors
                                         true,    // caldav
                                         null, // synchId
                                         debug);
      svci = new CalSvcFactoryDefault().getSvc(pars);

      if (superUser) {
        svci.setSuperUser(true);
      }

      svci.open();
      svci.beginTransaction();

      trans = new IcalTranslator(svci.getIcalCallback(), debug);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return svci;
  }

  private CalEnvI getEnv() throws WebdavException {
    try {
      if (env != null) {
        return env;
      }

      env = CalEnvFactory.getEnv(envPrefix, debug);
      return env;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
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
