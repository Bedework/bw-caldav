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

package org.bedework.caldav.server;

import org.bedework.davdefs.CaldavDefs;

import edu.rpi.sss.util.xml.QName;

/** Define ICal tags for XMlEmit.
 *
 * @author Mike Douglass   douglm@rpi.edu
 */
public class ICalTags {
  /** */
  public static final String namespace = CaldavDefs.icalNamespace;

  /** */
  public static final QName completed = new QName(namespace,
                                                  "completed");

  /** */
  public static final QName dtend = new QName(namespace,
                                              "dtend");

  /** */
  public static final QName dtstart = new QName(namespace,
                                                "dtstart");

  /** */
  public static final QName due = new QName(namespace,
                                            "due");

  /** */
  public static final QName duration = new QName(namespace,
                                                 "duration");

  /** */
  public static final QName hasAlarm = new QName(namespace,
                                                 "has-alarm");

  /** */
  public static final QName hasAttachment = new QName(namespace,
                                                      "has-attachment");

  /** */
  public static final QName hasRecurrence = new QName(namespace,
                                                      "has-recurrence");

  /** */
  public static final QName percentComplete = new QName(namespace,
                                                        "percent-complete");

  /** */
  public static final QName priority = new QName(namespace,
                                                 "priority");

  /** */
  public static final QName recurrenceId = new QName(namespace,
                                                     "recurrence-id");

  /** */
  public static final QName sequence = new QName(namespace,
                                                 "sequence");

  /** */
  public static final QName status = new QName(namespace,
                                               "status");

  /** */
  public static final QName summary = new QName(namespace,
                                                      "summary");

  /** */
  public static final QName transp = new QName(namespace,
                                               "transp");

  /** */
  public static final QName trigger = new QName(namespace,
                                                "trigger");

  /** */
  public static final QName uid = new QName(namespace,
                                            "uid");
}

