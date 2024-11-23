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
package org.bedework.caldav.server.sysinterface;

import org.bedework.access.AccessPrincipal;
import org.bedework.access.Acl;
import org.bedework.access.CurrentAccess;
import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CalDAVResource;
import org.bedework.caldav.server.PropertyHandler;
import org.bedework.caldav.server.PropertyHandler.PropertyType;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.util.TimeRange;
import org.bedework.caldav.util.filter.FilterBase;
import org.bedework.caldav.util.notifications.NotificationType;
import org.bedework.caldav.util.sharing.InviteReplyType;
import org.bedework.caldav.util.sharing.InviteType;
import org.bedework.caldav.util.sharing.ShareResultType;
import org.bedework.caldav.util.sharing.ShareType;
import org.bedework.util.calendar.ScheduleStates;
import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.webdav.servlet.shared.PrincipalPropertySearch;
import org.bedework.webdav.servlet.shared.UrlHandler;
import org.bedework.webdav.servlet.shared.WdEntity;
import org.bedework.webdav.servlet.shared.WdSysIntf;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import net.fortuna.ical4j.model.Calendar;
import org.apache.james.jdkim.api.JDKIM;
import org.oasis_open.docs.ws_calendar.ns.soap.ComponentSelectionType;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

/** All interactions with the underlying calendar system are made via this
 * interface.
 *
 * <p>We're using the bedework object classes here. To simplify matters (a little)
 * we don't have distinct event, task and journal classes. They are all currently
 * the BwEvent class with an entityType defining what the object represents.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public interface SysIntf extends WdSysIntf {
  /** Called before any other method is called to allow initialization to
   * take place at the first or subsequent requests
   *
   * @param req the http servlet request
   * @param account - possible account
   * @param service - true if this is a service call - e.g. iSchedule -
   *                rather than a real user.
   * @param calWs  true if this is a CalWs-SOAP service
   * @param synchWs  true if this is a SynchWs-SOAP service
   * @param notifyWs  true if this is a notification service
   * @param socketWs  true if this is a service for the websockets proxy
   * @param opaqueData  possibly from headers etc.
   * @return the account which may have changed
   */
  String init(HttpServletRequest req,
              String account,
              boolean service,
              boolean calWs,
              boolean synchWs,
              boolean notifyWs,
              boolean socketWs,
              String opaqueData);

  /** Allows some special handling of some requests - mostly to do with
   * cleanup of accounts when testing.
   *
   * @return true for test mode.
   */
  boolean testMode();

  /** true if bedework extensions are enabled for the request. Client
   * has sent a header "X-BEDEWORK-EXTENSIONS: TRUE".
   *
   * <p>Current extensions<ul>
   * <li>Extra elements in notifications</li>
   * </ul>
   * </p>
   *
   * @return true for extensions enabled.
   */
  boolean bedeworkExtensionsEnabled();

  /** Return CalDAV properties relevant to authentication state.
   *
   * @return CalDAVAuthProperties object - never null.
   */
  CalDAVAuthProperties getAuthProperties();

  /** Return CalDAV relevant properties about the system.
   *
   * @return CalDAVSystemProperties object - never null.
   */
  CalDAVSystemProperties getSystemProperties();

  /**
   *
   * @return a JDKIM implementation.
   */
  JDKIM getJDKIM();

  /** Return the current principal
   *
   * @return String
   */
  AccessPrincipal getPrincipal();

  /** Get a property handler
   *
   * @param ptype type of property
   * @return PropertyHandler
   */
  PropertyHandler getPropertyHandler(PropertyType ptype);

  /**
   * @return UrlHandler object to manipulate urls.
   */
  UrlHandler getUrlHandler();

  /* *
   * @return String url prefix derived from request.
   * /
  String getUrlPrefix();

  /* *
   * @return boolean - true if using relative urls for broken clients
   * /
  boolean getRelativeUrls();*/

  /** Does the value appear to represent a valid principal?
   *
   * @param val possible principal
   * @return true if it's a (possible) principal
   */
  boolean isPrincipal(String val);

  /** Return principal information for the given account.
   *
   *
   * @param account id
   * @return PrincipalInfo
   */
  AccessPrincipal getPrincipalForUser(String account);

  /** Return principal information for the given href. Also tests for a valid
   * principal.
   *
   *
   * @param href principal href
   * @return PrincipalInfo
   */
  AccessPrincipal getPrincipal(String href);

  /** Returns a public key for the given domain and service - either or both of
   * which may be null.
   *
   * <p>This allows us to have different keys for communication with different
   * domains and for different services. At its simplest, both are ignored and a
   * single key (pair) is used to secure all communications.
   *
   * <p>This is used, for example, by iSchedule for DKIM verification.
   *
   * <p>In keeping with the DKIM approach, <ul>
   * <li>if there are no keys an empty object is returned.</li>
   * <li>To refuse keys for a domain/service return null.</li>
   *
   *
   * <p>
   * @param domain
   * @param service
   * @return key, empty key object or null.
   */
  byte[] getPublicKey(String domain,
                      String service);

  /**
   * @param id
   * @param whoType - from WhoDefs
   * @return String href
   */
  String makeHref(String id, int whoType);

  /** The urls should be principal urls. principalUrl can null for the current user.
   * The result is a collection of principal urls of which the given url is a
   * member, based upon rootUrl. For example, if rootUrl points to the base of
   * the user principal hierarchy, then the rsult should be at least the current
   * user's principal url, remembering that user principals are themselves groups
   * and the user is considered a member of their own group.
   *
   * @param rootUrl - url to base search on.
   * @param principalUrl - url of principal or null for current user
   * @return Collection of urls - always non-null
   */
  Collection<String>getGroups(String rootUrl,
                              String principalUrl);

  /** Given a calendar address return the associated calendar account.
   * For example, we might have a calendar address<br/>
   *   auser@ahost.org
   * <br/>with the associated account of <br/>
   * auser.<br/>
   *
   * <p>Whereever we need a user account use the converted value. Call
   * userToCaladdr for the inverse.
   *
   * @param caladdr      calendar address
   * @return AccessPrincipal or null if not caladdr for this system
   */
  AccessPrincipal caladdrToPrincipal(String caladdr);

  /** The inverse of caladdrToPrincipal
   *
   * @param principal object
   * @return String calendar user address
   */
  String principalToCaladdr(AccessPrincipal principal);

  /** Given a valid AccessPrincipal return the associated calendar user information
   * needed for caldav interactions.
   *
   * @param principal     valid AccessPrincipal
   * @return CalUserInfo or null if not caladdr for this system
   * @throws RuntimeException  for errors
   */
  CalPrincipalInfo getCalPrincipalInfo(AccessPrincipal principal);

  /** Given a uri returns a Collection of uris that allow search operations on
   * principals for that resource.
   *
   * @param resourceUri reference to resource
   * @return Collection of String
   */
  Collection<String> getPrincipalCollectionSet(String resourceUri);

  /** Given a PrincipalPropertySearch returns a Collection of matching principals.
   *
   * @param resourceUri reference to resource
   * @param pps Collection of PrincipalPropertySearch
   * @return Collection of CalUserInfo
   */
  Collection<CalPrincipalInfo> getPrincipals(String resourceUri,
                                             PrincipalPropertySearch pps);

  /** Is href a valid principal?
   *
   * @param href of possible principal
   * @return boolean true for a valid user
   */
  boolean validPrincipal(String href);

  /* ==============================================================
   *                   Notifications
   * ============================================================== */

  /** Subscribe for email notifications to the notification engine for the
   * indicated calendar user.
   *
   * @param principalHref the subscriber
   * @param action "add"/"remove"
   * @param emails addresses to add or remove
   * @return false for not done
   */
  boolean subscribeNotification(String principalHref,
                                String action,
                                List<String> emails);

  /** Add the given notification to the notification collection for the
   * indicated calendar user.
   *
   * @param href principal
   * @param val notification
   * @return false for unknown CU
   */
  boolean sendNotification(String href,
                           NotificationType val);

  /** Remove the given notification from the notification collection for the
   * indicated calendar user.
   *
   * @param href principal
   * @param val notification
   */
  void removeNotification(String href,
                          NotificationType val);

  /**
   * @return notifications for this user
   */
  List<NotificationType> getNotifications();

  /**
   * @param href of principal
   * @param type of notification (null for all)
   * @return notifications for the given principal of the given type
   */
  List<NotificationType> getNotifications(String href,
                                          QName type);

  /**
   * @param col MUST be a sharable collection
   * @param share is the request
   * @return list of ok and !ok sharees
   */
  ShareResultType share(CalDAVCollection<?> col,
                        ShareType share);

  /** Handle a reply to a sharing notification.
   *
   * @param col - unchecked sharees calendar home
   * @param reply - the reply to the invitation with the path reset to be the
   *                relative path.
   * @return null for unknown sharer or no invitation otherwise the path to the
   *                   new alias in the sharees calendar home.
   */
  String sharingReply(CalDAVCollection<?> col,
                      InviteReplyType reply);

  /**
   * @param col collection
   * @return current invitations
   */
  InviteType getInviteStatus(CalDAVCollection<?> col);

  /* ==============================================================
   *                   Scheduling
   * ============================================================== */

  /** Return a set of hrefs for each resource affecting this users freebusy
   *
   * @return Collection of hrefs
   */
  Collection<String> getFreebusySet();

  /** Result for a single recipient.
   */
  class SchedRecipientResult implements ScheduleStates {
    /** */
    public String recipient;

    /** One of the above */
    public int status = scheduleUnprocessed;

    /** Set if this is the result of a freebusy request. */
    public CalDAVEvent<?> freeBusy;

    @Override
    public String toString() {
      return new ToString(this).append("recipient", recipient)
                               .append("status", String.valueOf(status))
                               .toString();
    }
  }

  /** Request to schedule a meeting. The event object must have the organizer
   * and attendees and possibly recipients set. If no recipients are set, they
   * will be set from the attendees.
   *
   * <p>The functioning of this method must conform to the requirements of iTip.
   * The event object must have the required method (publish, request etc) set.
   *
   * <p>The event will be added to the users outbox which will trigger the send
   * of requests to other users inboxes. For users within this system the
   * request will be immediately addded to the recipients inbox. For external
   * users they are sent via mail.
   *
   * @param ev         Event object
   * @return ScheduleResult
   */
  Collection<SchedRecipientResult> schedule(CalDAVEvent<?> ev);

  /* ==============================================================
   *                   Events
   * ============================================================== */

  /** Add an event/task/journal. If this is a scheduling event we are adding,
   * determined by examining the organizer and attendee properties, we will send
   * out invitations to the attendees, unless the noInvites flag is set.
   *
   * @param ev           CalDAVEvent object
   * @param noInvites    Set from request - if true don't send invites
   * @param rollbackOnError true if we rollback and throw an exception on error
   * @return Collection of overrides which did not match or null if all matched
   */
  Collection<CalDAVEvent<?>> addEvent(CalDAVEvent<?> ev,
                                      boolean noInvites,
                                      boolean rollbackOnError);

  /** Reindex an event after an error that may be the result of an out
   * of date index.
   *
   * @param event  a CalDAVEvent object
   */
  void reindexEvent(CalDAVEvent<?> event);

  /** Update an event/todo/journal.
   *
   * @param event         updated CalDAVEvent object
   */
  void updateEvent(CalDAVEvent<?> event);

  /** Show the outcome of an update
   * @author douglm
   */
  class UpdateResult {
    private boolean ok;

    private String reason;

    private static final UpdateResult okResult = new UpdateResult();

    /**
     * @return result indicating OK.
     */
    public static UpdateResult getOkResult() {
      return okResult;
    }

    /**
     */
    private UpdateResult() {
      ok = true;
    }

    /**
     * @param reason for failure
     */
    public UpdateResult(final String reason) {
      this.reason = reason;
    }

    /**
     * @return True for an OK update
     */
    public boolean getOk() {
      return ok;
    }

    /**
     * @return Non-null if !ok
     */
    public String getReason() {
      return reason;
    }
  }

  /** Update the supplied event using the web services update message.
   *
   * @param event         updated CalDAVEvent object
   * @param updates       set of updates to be applied
   * @return UpdateResult
   */
  UpdateResult updateEvent(CalDAVEvent<?> event,
                           List<ComponentSelectionType> updates);

  /** Return the events for the current user in the given collection using the
   * supplied filter. Stored freebusy objects are returned as BwEvent
   * objects with the appropriate entity type. If retrieveList is supplied only
   * those fields (and a few required fields) will be returned.
   *
   * <p>We flag the desired entity types.
   *
   * @param col collection
   * @param filter - if non-null defines a search filter
   * @param retrieveList List of properties to retrieve or null for a full event.
   * @param recurRetrieval How recurring event is returned.
   * @return Collection  populated event value objects
   */
  Collection<CalDAVEvent<?>> getEvents(CalDAVCollection<?> col,
                                       FilterBase filter,
                                       List<String> retrieveList,
                                       RetrievalMode recurRetrieval);

  /** Get events given the collection and String name. Return null for not
   * found. There should be only one event or none. For recurring, the
   * overrides and possibly the instances will be attached.
   *
   * @param col        CalDAVCollection object
   * @param val        String possible name
   * @return CalDAVEvent or null
   */
  CalDAVEvent<?> getEvent(CalDAVCollection<?> col,
                          String val);

  /**
   * @param ev event
   * @param scheduleReply - true if we want a scheduling reply posted
   */
  void deleteEvent(CalDAVEvent<?> ev,
                   boolean scheduleReply);

  /** Get the free busy for one or more principals based on the given VFREEBUSY
   * request.
   *
   * @param val    A representation of a scheduling freebusy request to be
   *               acted upon.
   * @param iSchedule true if this is from an ischedule request
   * @return ScheduleResult
   */
  Collection<SchedRecipientResult> requestFreeBusy(CalDAVEvent<?> val,
                                                   boolean iSchedule);

  /** Handle the special freebusy requests, i.e. non-CalDAV
   *
   * @param cua calendar address
   * @param originator value of the Originator header
   * @param recipients values of Recipient headers
   * @param tr time range
   * @param wtr writer for output
   */
  void getSpecialFreeBusy(String cua,
                          Set<String> recipients,
                          String originator,
                          TimeRange tr,
                          Writer wtr);

  /** Generate a free busy object for the given time period which reflects
   * the state of the given collection.
   *
   * @param col collection
   * @param depth to traverse
   * @param timeRange period
   * @return CalDAVEvent - as a freebusy entity
   */
  CalDAVEvent<?> getFreeBusy(CalDAVCollection<?> col,
                             int depth,
                             TimeRange timeRange);

  /** Check the access for the given entity. Returns the current access
   * or null or optionally throws a no access exception.
   *
   * @param ent entity we want access to
   * @param desiredAccess what access is needed
   * @param returnResult throw exception if false and no access
   * @return CurrentAccess
   */
  CurrentAccess checkAccess(WdEntity<?> ent,
                            int desiredAccess,
                            boolean returnResult);

  /**
   * @param ev event
   * @param acl new access
   */
  void updateAccess(CalDAVEvent<?> ev,
                    Acl acl);

  /** Copy or move the given entity to the destination collection with the given name.
   * Status is set on return
   *
   * @param from      Source entity
   * @param to        Destination collection
   * @param name      String name of new entity
   * @param copy      true for copying
   * @param overwrite destination exists
   * @return true if destination created (i.e. not updated)
   */
  boolean copyMove(CalDAVEvent<?> from,
                   CalDAVCollection<?> to,
                   String name,
                   boolean copy,
                   boolean overwrite);

  /* ====================================================================
   *                   Collections
   * ==================================================================== */

  /** Return a new object representing the parameters. No collection is
   * created. makeCollection must be called subsequently with the object.
   *
   * @param isCalendarCollection true for a calendar
   * @param parentPath parent collection
   * @return CalDAVCollection
   */
  CalDAVCollection<?> newCollectionObject(boolean isCalendarCollection,
                                          String parentPath);

  /**
   * @param col collection
   * @param acl access
   */
  void updateAccess(CalDAVCollection<?> col,
                    Acl acl);

  /**
   * @param col   Initialised collection object
   * @return int status
   */
  int makeCollection(CalDAVCollection<?> col);

  /** Copy or move the collection to another location.
   * Status is set on return
   *
   * @param from      Source collection
   * @param to        Destination collection
   * @param copy      true for copying
   * @param overwrite destination exists
   */
  void copyMove(CalDAVCollection<?> from,
                CalDAVCollection<?> to,
                boolean copy,
                boolean overwrite);

  /** Get a collection given the path
   *
   * @param  path     String path of collection
   * @return CalDAVCollection null for unknown collection
   */
  CalDAVCollection<?> getCollection(String path);

  /** Update a collection.
   *
   * @param val           updated CalDAVCollection object
   */
  void updateCollection(CalDAVCollection<?> val);

  /**
   * @param col to delete
   * @param sendSchedulingMessage  true if we should send cancels
   */
  void deleteCollection(CalDAVCollection<?> col,
                        boolean sendSchedulingMessage);

  /** Returns children of the given collection to which the current user has
   * some access.
   *
   * @param  col          parent collection
   * @return Collection   of CalDAVCollection
   */
  Collection<CalDAVCollection<?>> getCollections(CalDAVCollection<?> col);

  /* ==============================================================
   *                   Files
   * ============================================================== */

  /** Return a new object representing the parameters. No resource is
   * created. putFile must be called subsequently with the object.
   *
   * @param parentPath parent collection
   * @return CalDAVResource
   */
  CalDAVResource<?> newResourceObject(String parentPath);

  /** PUT a file.
   *
   * @param coll         CalDAVCollection defining recipient collection
   * @param val          CalDAVResource
   */
  void putFile(CalDAVCollection<?> coll,
               CalDAVResource<?> val);

  /** GET a file.
   *
   * @param coll         CalDAVCollection containing file
   * @param name of file
   * @return CalDAVResource
   */
  CalDAVResource<?> getFile(CalDAVCollection<?> coll,
                            String name);

  /** Get resource content given the resource. It will be set in the resource
   * object
   *
   * @param  val CalDAVResource
   * @throws RuntimeException on fatal error
   */
  void getFileContent(CalDAVResource<?> val);

  /** Get the files in a collection.
   *
   * @param coll         CalDAVCollection containing file
   * @return Collection of CalDAVResource
   */
  Collection<CalDAVResource<?>> getFiles(CalDAVCollection<?> coll);

  /** Update a file.
   *
   * @param val          CalDAVResource
   * @param updateContent if true we also update the content
   */
  void updateFile(CalDAVResource<?> val,
                  boolean updateContent);

  /** Delete a file.
   *
   * @param val          CalDAVResource
   */
  void deleteFile(CalDAVResource<?> val);

  /** Copy or move the given file to the destination collection with the given name.
   * Status is set on return
   *
   * @param from      Source resource
   * @param toPath    Destination collection path
   * @param name      String name of new entity
   * @param copy      true for copying
   * @param overwrite destination exists
   * @return true if destination created (i.e. not updated)
   */
  boolean copyMoveFile(CalDAVResource<?> from,
                       String toPath,
                       String name,
                       boolean copy,
                       boolean overwrite);

  /* ====================================================================
   *                   Synch Reports
   * ==================================================================== */

  /** Data for Synch Report
   *
   *   @author Mike Douglass   douglm   rpi.edu
   */
  class SynchReportData {
    /** The changed entity may be an event, a resource or a collection. If it is
     * deleted then it will be marked as tombstoned.
     *
     * <p>The parent indicates what collection is visible in the hierarchy. It may
     * be an alias to the actual parent.
     *
     * @author douglm
     */
    public static class SynchReportDataItem implements Comparable<SynchReportDataItem> {
      /**
       */
      private final String token;

      /** Non-null if this is for a calendar entity */
      private CalDAVEvent<?> entity;

      /** Non-null if this is for a resource - will not have its content */
      private CalDAVResource<?> resource;

      private CalDAVCollection<?> col;

      private final String vpath;

      /** true if we can provide sync info for this - usually false for aliases */
      private boolean canSync;

      /**
       * @param vpath virtual path
       * @param entity event
       * @param token synch token
       */
      public SynchReportDataItem(final String vpath,
                                 final CalDAVEvent<?> entity,
                                 final String token) {
        this.vpath = vpath;
        this.entity = entity;
        this.token = token;
      }

      /**
       * @param vpath virtual path
       * @param resource entity
       * @param token synch token
       */
      public SynchReportDataItem(final String vpath,
                                 final CalDAVResource<?> resource,
                                 final String token) {
        this.vpath = vpath;
        this.resource = resource;
        this.token = token;
      }

      /**
       * @param vpath virtual path
       * @param col collection
       * @param token synch token
       * @param canSync
       */
      public SynchReportDataItem(final String vpath,
                                 final CalDAVCollection<?> col,
                                 final String token,
                                 final boolean canSync) {
        this.vpath = vpath;
        this.col = col;
        this.canSync = canSync;
        this.token = token;
      }

      /**
       *
       * @return The token
       */
      public String getToken() {
        return token;
      }

      /** Non-null if this is for calendar entity
       *
       * @return event or null
       */
      public CalDAVEvent<?> getEntity() {
        return entity;
      }

      /** Non-null if this is for a resource
       *
       * @return resource or null
       */
      public CalDAVResource<?> getResource() {
        return resource;
      }

      /** Non-null if this is for a collection
       *
       * @return collection or null
       */
      public CalDAVCollection<?> getCol() {
        return col;
      }

      /** Always non-null - virtual path to the element this object represents (not
       * including this elements name).
       *
       * <p>For example, if (x) represents a collection x and [x] represents an alias
       * x then for element c we have:<pre>
       * (a)->(b)->(c) has the vpath and path a/b/c
       * while
       * (a)->[b]
       *       |
       *       v
       * (x)->(y)->(c) has the vpath a/b/c and path) x/y/c
       * </pre>
       *
       * @return parent collection
       */
      public String getVpath() {
        return vpath;
      }

      /** False if we can't do a direct sync report.
       *
       * @return boolean
       */
      public boolean getCanSync() {
        return canSync;
      }

      @Override
      public int compareTo(final SynchReportDataItem that) {
        return token.compareTo(that.token);
      }

      @Override
      public int hashCode() {
        return token.hashCode();
      }

      @Override
      public boolean equals(final Object o) {
        if (!(o instanceof final SynchReportDataItem srdi)) {
          return false;
        }
        return compareTo(srdi) == 0;
      }
    }

    /**
     */
    public List<SynchReportDataItem> items;

    /** True if the report was truncated
     */
    public boolean truncated;

    /** True if the token is valid.
     */
    public boolean tokenValid;

    /** Token for next time.
     */
    public String token;
  }

  /**
   * @param col collection
   * @return A sync-token which must be a URI.
   */
  String getSyncToken(CalDAVCollection<?> col);

  /**
   * @param path of resource(s)
   * @param token synch token
   * @param limit - negative for no limit on result set size
   * @param recurse
   * @return report
   */
  SynchReportData getSyncReport(String path,
                                String token,
                                int limit,
                                boolean recurse);

  /* ==============================================================
   *                   Misc
   * ============================================================== */

  /** Make an ical Calendar from an event.
   *
   * @param ev event
   * @param incSchedMethod - true if we should emit the scheduling method
   * @return Calendar
   */
  Calendar toCalendar(CalDAVEvent<?> ev,
                      boolean incSchedMethod);

  /** Make an XML IcalendarType from an event.
   *
   * @param ev event
   * @param incSchedMethod - true if we should emit the scheduling method
   * @param pattern - non-null to restrict returned properties
   * @return IcalendarType
   */
  IcalendarType toIcalendar(CalDAVEvent<?> ev,
                            boolean incSchedMethod,
                            IcalendarType pattern);

  /** Make a JSON jcal object from an event.
   *
   * @param ev event to convert
   * @param incSchedMethod - true if we should emit the scheduling method
   * @return String jcal representation
   */
  String toJcal(CalDAVEvent<?> ev,
                boolean incSchedMethod);

  /** Convert a Calendar to its string form
   *
   * @param cal Calendar to convert
   * @param contentType
   * @return String representation
   */
  String toIcalString(Calendar cal,
                      String contentType);

  /** What method do we want emitted */
  enum MethodEmitted {
    /** No method for calendar */
    noMethod,

    /** Method from event */
    eventMethod,

    /** It's a publish */
    publish
  }

  /** Write a collection of events as an ical calendar.
   *
   * @param evs collection of events
   * @param method - what scheduling method?
   * @param xml - if this is embedded in an xml stream
   * @param wtr - if standalone output or no xml stream initialized.
   * @param contentType - requested type. null for default
   * @return actual contentType written
   */
  String writeCalendar(Collection<CalDAVEvent<?>> evs,
                       MethodEmitted method,
                       XmlEmit xml,
                       Writer wtr,
                       String contentType);

  /** Expected result type */
  enum IcalResultType {
    /** Expect one (non-timezone) component only */
    OneComponent,

    /** Expect one timezone only */
    TimeZone
  }

  /** Convert the Icalendar reader to a Collection of Calendar objects
   *
   * @param col       collection in which to place entities
   * @param rdr for content
   * @param contentType  null for ICalendar or valid calendar mime type
   * @param rtype expected component type
   * @param mergeAttendees True if we should only update our own attendee.
   * @return SysiIcalendar
   */
  SysiIcalendar fromIcal(CalDAVCollection<?> col,
                         Reader rdr,
                         String contentType,
                         IcalResultType rtype,
                         boolean mergeAttendees);

  /** Convert the Icalendar object to a Collection of Calendar objects
   *
   * @param col       collection in which to place entities
   * @param ical
   * @param rtype expected component type
   * @return SysiIcalendar
   */
  SysiIcalendar fromIcal(CalDAVCollection<?> col,
                         IcalendarType ical,
                         IcalResultType rtype);

  /** Create a Calendar object from the named timezone and convert to
   * a String representation
   *
   * @param tzid       String timezone id
   * @return String
   */
  String toStringTzCalendar(String tzid);

  /** Given a timezone spec return the tzid
   *
   * @param val timezone spec
   * @return String tzid or null for failure
   */
  String tzidFromTzdef(String val);

  /** Validate an alarm component
   *
   * @param val an alarm component
   * @return boolean false for failure
   */
  boolean validateAlarm(String val);

  /** Called on the way out before close if there was an error.
   *
   */
  void rollback();

  /** End any transactions.
   *
   */
  void close();
}
