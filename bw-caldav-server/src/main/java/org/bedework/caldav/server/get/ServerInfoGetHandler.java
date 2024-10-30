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
package org.bedework.caldav.server.get;

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.RequestPars;
import org.bedework.util.xml.XmlEmit;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.serverInfo.ServerInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle freebusy GET requests.
 *
 * @author Mike Douglass
 */
public class ServerInfoGetHandler extends GetHandler {
  /**
   * @param intf the interface
   */
  public ServerInfoGetHandler(final CaldavBWIntf intf) {
    super(intf);
  }

  @Override
  public void process(final HttpServletRequest req,
                      final HttpServletResponse resp,
                      final RequestPars pars) {
    try {
      final ServerInfo serverInfo = intf.getServerInfo();

      if (serverInfo == null) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      final String name = pars.getNoPrefixResourceUri();

      if (!"/serverinfo.xml".equals(name)) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      final XmlEmit xml = intf.getXmlEmit();

      startEmit(resp);

      resp.setContentType("application/server-info+xml;charset=utf-8");

      serverInfo.toXml(xml);
      resp.setStatus(HttpServletResponse.SC_OK);
    } catch (final WebdavForbidden wdf) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
