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

import org.bedework.caldav.server.PropertyHandler;
import org.bedework.caldav.server.SysIntf;
import org.bedework.caldav.server.PropertyHandler.PropertyType;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventProxy;
import org.bedework.calfacade.BwPrincipal;
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.BwSystem;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.exc.CalFacadeAccessException;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.exc.CalFacadeStaleStateException;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.util.ChangeTable;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.calsvci.CalSvcIPars;
import org.bedework.calsvci.EventsI.CopyMoveStatus;
import org.bedework.icalendar.IcalMalformedException;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
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
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Calendar;

import org.apache.log4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
      currentPrincipal = getSvci(account).getUsersHandler().getUser(account);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
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
      String account = getSvci().getDirectories().caladdrToUser(caladdr);
      if (account == null) {
        return null;
      }

      return getSvci().getUsersHandler().getPrincipal(account);
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

      BwPrincipal p = getSvci().getUsersHandler().getPrincipal(principal.getPrincipalRef());
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

      BwCalendar cal = getCalendar(path);
      if (cal != null) {
        cui = getCalPrincipalInfo(getPrincipal(cal.getOwnerHref()));
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

  public ScheduleResult schedule(EventInfo ei) throws WebdavException {
    try {
      BwEvent ev = ei.getEvent();
      ev.setOwnerHref(currentPrincipal.getPrincipalRef());
      if (Icalendar.itipReplyMethodType(ev.getScheduleMethod())) {
        return getSvci().getScheduler().scheduleResponse(ei);
      }

      return getSvci().getScheduler().schedule(ei, null, false);
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
   * @see org.bedework.caldav.server.SysIntf#addEvent(org.bedework.calfacade.svc.EventInfo, boolean, boolean)
   */
  public Collection<BwEventProxy> addEvent(EventInfo ei,
                                           boolean noInvites,
                                           boolean rollbackOnError) throws WebdavException {
    try {
      /* Is the event a scheduling object? */

      return getSvci().getEventsHandler().add(ei, noInvites,
                                              false,  // scheduling - inbox
                                              rollbackOnError).failedOverrides;
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

  public void updateEvent(EventInfo event,
                          ChangeTable changes) throws WebdavException {
    try {
      getSvci().getEventsHandler().update(event, false,
                                          null,
                                          null, changes);
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
      return getSvci().getEventsHandler().getEvents(cal, filter, null, null,
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
      return getSvci().getEventsHandler().get(cal.getPath(), val, recurRetrieval);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void deleteEvent(BwEvent ev,
                          boolean scheduleReply) throws WebdavException {
    try {
      getSvci().getEventsHandler().delete(ev, scheduleReply);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void deleteCalendar(BwCalendar cal) throws WebdavException {
    try {
      getSvci().getCalendarsHandler().delete(cal, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public ScheduleResult requestFreeBusy(EventInfo val) throws WebdavException {
    try {
      BwEvent ev = val.getEvent();
      if (currentPrincipal != null) {
        ev.setOwnerHref(currentPrincipal.getPrincipalRef());
      }
      if (Icalendar.itipReplyMethodType(ev.getScheduleMethod())) {
        return getSvci().getScheduler().scheduleResponse(val);
      }

      return getSvci().getScheduler().schedule(val, null, false);
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

  public BwEvent getFreeBusy(final Collection<BwCalendar> cals,
                             final String account,
                             final BwDateTime start,
                             final BwDateTime end) throws WebdavException {
    try {
      BwUser user = getSvci().getUsersHandler().getUser(account);
      if (user == null) {
        throw new WebdavUnauthorized();
      }

      return getSvci().getScheduler().getFreeBusy(cals, user, start, end);
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
                           Acl acl) throws WebdavException {
    try {
      getSvci().changeAccess(cal, acl.getAces(), true);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void updateAccess(BwEvent ev,
                           Acl acl) throws WebdavException{
    try {
      getSvci().changeAccess(ev, acl.getAces(), true);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeCollection(org.bedework.calfacade.BwCalendar, boolean, java.lang.String)
   */
  public int makeCollection(BwCalendar cal,
                            boolean calendarCollection,
                            String parentPath) throws WebdavException {
    if (calendarCollection) {
      cal.setCalType(BwCalendar.calTypeCollection);
    } else {
      cal.setCalType(BwCalendar.calTypeFolder);
    }

    try {
      getSvci().getCalendarsHandler().add(cal, parentPath);
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
    try {
      if (!copy) {
        /* Move the from collection to the new location "to".
         * If the parent calendar is the same in both cases, this is just a rename.
         */
        if ((from.getColPath() == null) || (to.getColPath() == null)) {
          throw new WebdavForbidden("Cannot move root");
        }

        if (from.getColPath().equals(to.getColPath())) {
          // Rename
          getSvci().getCalendarsHandler().rename(from, to.getName());
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

  public boolean copyMove(EventInfo from,
                          BwCalendar to,
                          String name,
                          boolean copy,
                          boolean overwrite) throws WebdavException {
    CopyMoveStatus cms;
    try {
      cms = getSvci().getEventsHandler().copyMoveNamed(from, to, name,
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
        name = from.getEvent().getName();
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
   * @see org.bedework.caldav.server.SysIntf#getCalendar(java.lang.String)
   */
  public BwCalendar getCalendar(String path) throws WebdavException {
    try {
      return getSvci().getCalendarsHandler().get(path);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateCalendar(org.bedework.calfacade.BwCalendar)
   */
  public void updateCalendar(BwCalendar val) throws WebdavException {
    try {
      getSvci().getCalendarsHandler().update(val);
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
   * @see org.bedework.caldav.server.SysIntf#getCalendars(org.bedework.calfacade.BwCalendar)
   */
  public Collection<BwCalendar> getCalendars(BwCalendar cal) throws WebdavException {
    try {
      return getSvci().getCalendarsHandler().getChildren(cal);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#resolveAlias(org.bedework.calfacade.BwCalendar)
   */
  public void resolveAlias(BwCalendar cal) throws WebdavException {
    try {
      getSvci().getCalendarsHandler().resolveAlias(cal, true, false);
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
   * @see org.bedework.caldav.server.SysIntf#putFile(org.bedework.calfacade.BwCalendar, org.bedework.calfacade.BwResource)
   */
  public void putFile(BwCalendar coll,
                      BwResource val) throws WebdavException {
    try {
      getSvci().getResourcesHandler().save(coll.getPath(), val);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFile(org.bedework.calfacade.BwCalendar, java.lang.String)
   */
  public BwResource getFile(BwCalendar coll,
                            String name) throws WebdavException {
    try {
      return getSvci().getResourcesHandler().get(coll.getPath() + "/" + name);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getContent(org.bedework.calfacade.BwResource)
   */
  public void getFileContent(BwResource val) throws WebdavException {
    try {
      getSvci().getResourcesHandler().getContent(val);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFiles(org.bedework.calfacade.BwCalendar)
   */
  public Collection<BwResource> getFiles(BwCalendar coll) throws WebdavException {
    try {
      return getSvci().getResourcesHandler().getAll(coll.getPath());
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateFile(org.bedework.calfacade.BwResource, boolean)
   */
  public void updateFile(BwResource val,
                         boolean updateContent) throws WebdavException {
    try {
      getSvci().getResourcesHandler().update(val, updateContent);
    } catch (CalFacadeAccessException cfae) {
      throw new WebdavForbidden();
    } catch (CalFacadeException cfe) {
      throw new WebdavException(cfe);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#deleteFile(org.bedework.calfacade.BwResource)
   */
  public void deleteFile(BwResource val) throws WebdavException {
    try {
      getSvci().getResourcesHandler().delete(val.getColPath() + "/" +
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
   * @see org.bedework.caldav.server.SysIntf#copyMoveFile(org.bedework.calfacade.BwResource, java.lang.String, java.lang.String, boolean, boolean)
   */
  public boolean copyMoveFile(BwResource from,
                              String toPath,
                              String name,
                              boolean copy,
                              boolean overwrite) throws WebdavException {
    try {
      return getSvci().getResourcesHandler().copyMove(from, toPath, name,
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

  public Calendar toCalendar(EventInfo ev) throws WebdavException {
    try {
      return trans.toIcal(ev, ev.getEvent().getScheduleMethod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public Calendar toCalendar(Collection<EventInfo> evs,
                             int method) throws WebdavException {
    try {
      return trans.toIcal(evs, method);
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
                                         true,    // caldav
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
