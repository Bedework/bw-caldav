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
package org.bedework.caldav.server;

import org.bedework.webdav.servlet.shared.UrlHandler;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;

import org.apache.james.jdkim.tagvalue.SignatureRecordImpl;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/** An incoming iSchedule message
 *
 * @author douglm
 *
 */
public class IscheduleIn extends IscheduleMessage {
  private final HttpServletRequest req;

  /** Constructor
   *
   * @param req http request
   * @param urlHandler to manipulate urls
   */
  public IscheduleIn(final HttpServletRequest req,
                     final UrlHandler urlHandler) {
    this.req = req;

    /* Expect originator and recipient headers */
    for (final Enumeration<String> e = req.getHeaderNames();
         e.hasMoreElements();) {
      final String name = e.nextElement();
      final String nameLc = name.toLowerCase();

      addField(nameLc);

      for (final Enumeration<String> hvals = req.getHeaders(name);
           hvals.hasMoreElements();) {
        final String hval = hvals.nextElement();
        addHeader(nameLc, hval);

        switch (nameLc) {
          case "originator" -> {
            if (originator != null) {
              throw new WebdavBadRequest(
                      "Multiple originator headers");
            }

            originator = adjustPrincipal(hval, urlHandler);
          }
          case "recipient" -> {
            final String[] rlist = hval.split(",");

            for (final String r: rlist) {
              recipients.add(adjustPrincipal(r.trim(), urlHandler));
            }
          }
          case "ischedule-version" -> {
            if (iScheduleVersion != null) {
              throw new WebdavBadRequest(
                      "Multiple iSchedule-Version headers");
            }

            iScheduleVersion = hval;
          }
          case "ischedule-message-id" -> {
            if (iScheduleMessageId != null) {
              throw new WebdavBadRequest(
                      "Multiple iSchedule-Message-Id headers");
            }

            iScheduleMessageId = hval;
          }
          case "dkim-signature" -> {
            if (dkimSignature != null) {
              throw new WebdavBadRequest(
                      "Multiple dkim-signature headers");
            }

            dkimSignature = SignatureRecordImpl.forIschedule(hval);
            //dkimSignature.validate();
          }
        }

      }
    }
  }

  /** Get the request
   *
   *  @return the request
   */
  public HttpServletRequest getReq() {
    return req;
  }

  /* We seem to be getting both absolute and relative principals as well as mailto
   * forms of calendar user.
   *
   * If we get an absolute principal - turn it into a relative
   */
  private String adjustPrincipal(final String val,
                                 final UrlHandler urlHandler) {
    if (val == null) {
      return null;
    }

    return urlHandler.unprefix(val);
    /*
    if (val.startsWith(sysi.getUrlPrefix())) {
      return val.substring(sysi.getUrlPrefix().length());
    }

    return val;
    */
  }
}
