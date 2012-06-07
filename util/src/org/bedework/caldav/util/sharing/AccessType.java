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
package org.bedework.caldav.util.sharing;

import edu.rpi.sss.util.ToString;

/** Class to represent access rights in sharing.
 *
 * @author Mike Douglass douglm
 */
public class AccessType {
  private Boolean read;
  private Boolean readWrite;

  /**
   * @param val read
   */
  public void setRead(final Boolean val) {
    read = val;
  }

  /**
   * @return read
   */
  public Boolean getRead() {
    return read;
  }

  /**
   * @param val readWrite
   */
  public void setReadWrite(final Boolean val) {
    readWrite = val;
  }

  /**
   * @return readWrite
   */
  public Boolean getReadWrite() {
    return readWrite;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("read", getRead());
    ts.append("readWrite", getReadWrite());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
