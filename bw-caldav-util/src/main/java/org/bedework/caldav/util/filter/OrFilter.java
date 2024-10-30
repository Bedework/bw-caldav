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
package org.bedework.caldav.util.filter;

import org.bedework.util.misc.ToString;

/** A filter that is composed of a boolean OR of zero or more filters
 *
 * @author Mike Douglass
 * @version 2.0
 */
public class OrFilter extends FilterBase {
  /**
   */
  public OrFilter() {
    super("OR");
  }

  /* ==============================================================
   *                   Object methods
   * ============================================================== */

  public String toString() {
    final ToString ts = new ToString(this);

    super.toStringSegment(ts);

    ts.append("children", getChildren());

    return ts.toString();
  }
}
