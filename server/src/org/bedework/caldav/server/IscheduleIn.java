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

import edu.rpi.cct.webdav.servlet.shared.UrlHandler;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import org.apache.james.jdkim.tagvalue.SignatureRecordImpl;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/** An incoming iSchedule message
 *
 * @author douglm
 *
 */
public class IscheduleIn extends IscheduleMessage {
  private HttpServletRequest req;

  private String httpUri;

  /** Constructor
   *
   * @param req
   * @param urlHandler
   * @throws WebdavException
   */
  @SuppressWarnings("unchecked")
  public IscheduleIn(final HttpServletRequest req,
                          final UrlHandler urlHandler) throws WebdavException {
    this.req = req;

    /* Expect originator and recipient headers */
    for (Enumeration<String> e = req.getHeaderNames();
         e.hasMoreElements();) {
      String name = e.nextElement();
      String nameLc = name.toLowerCase();

      addField(nameLc);

      for (Enumeration<String> hvals = req.getHeaders(name);
           hvals.hasMoreElements();) {
        String hval = hvals.nextElement();
        addHeader(nameLc, hval);

        if ("originator".equals(nameLc)) {
          if (originator != null) {
            throw new WebdavBadRequest("Multiple originator headers");
          }

          originator = adjustPrincipal(hval, urlHandler);
          continue;
        }

        if ("recipient".equals(nameLc)) {
          String[] rlist = hval.split(",");

          if (rlist != null) {
            for (String r: rlist) {
              recipients.add(adjustPrincipal(r.trim(), urlHandler));
            }
          }

          continue;
        }

        if ("ischedule-version".equals(nameLc)) {
          if (iScheduleVersion != null) {
            throw new WebdavBadRequest("Multiple iSchedule-Version headers");
          }

          iScheduleVersion = hval;

          continue;
        }

        if ("ischedule-message-id".equals(nameLc)) {
          if (iScheduleMessageId != null) {
            throw new WebdavBadRequest("Multiple iSchedule-Message-Id headers");
          }

          iScheduleMessageId = hval;

          continue;
        }

        if ("dkim-signature".equals(nameLc)) {
          if (dkimSignature != null) {
            throw new WebdavBadRequest("Multiple dkim-signature headers");
          }

          dkimSignature = SignatureRecordImpl.forIschedule(hval);
          //dkimSignature.validate();

          String[] httpVals = dkimSignature.getHttpVals();

          if (!"post".equals(httpVals[0].toLowerCase())) {
            throw new WebdavBadRequest("DKIM: Only allow for POST in http tag value");
          }

          httpUri = httpVals[1];

          continue;
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

  /** Get the http Uri from the request line
   *
   *  @return String     httpUri
   */
  public String getHttpUri() {
    return httpUri;
  }

  /* We seem to be getting both absolute and relative principals as well as mailto
   * forms of calendar user.
   *
   * If we get an absolute principal - turn it into a relative
   */
  private String adjustPrincipal(final String val,
                                 final UrlHandler urlHandler) throws WebdavException {
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
