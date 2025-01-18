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

import org.bedework.base.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;

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

  /**
   * @return true if read set and true, false otherwise
   */
  public boolean testRead() {
    final Boolean f = getRead();
    if (f == null) {
      return false;
    }

    return f;
  }

  /**
   * @return true if read-write set and true, false otherwise
   */
  public boolean testReadWrite() {
    final Boolean f = getReadWrite();
    if (f == null) {
      return false;
    }

    return f;
  }

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.access);
    if (testRead()) {
      xml.emptyTag(AppleServerTags.read);
    } else if (testReadWrite()) {
      xml.emptyTag(AppleServerTags.readWrite);
    }
    xml.closeTag(AppleServerTags.access);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("read", getRead());
    ts.append("readWrite", getReadWrite());
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  @Override
  public boolean equals(final Object o) {
    final AccessType that  = (AccessType)o;

    return (testRead() == that.testRead()) ||
         (testReadWrite() == that.testReadWrite());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
