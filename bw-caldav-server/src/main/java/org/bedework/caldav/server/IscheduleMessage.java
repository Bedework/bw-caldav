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

import org.bedework.util.misc.ToString;

import org.apache.james.jdkim.api.Headers;
import org.apache.james.jdkim.api.SignatureRecord;
import org.apache.james.jdkim.tagvalue.SignatureRecordImpl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author douglm
 *
 */
public class IscheduleMessage implements Headers, Serializable {
  private final Map<String, List<String>> headers = new HashMap<>();
  private final List<String> fields = new ArrayList<>();

  /** value of the Originator header */
  protected String originator;

  /** values of Recipient headers */
  protected Set<String> recipients = new TreeSet<>();

  protected String iScheduleVersion;
  protected String iScheduleMessageId;

  protected SignatureRecordImpl dkimSignature;

  /** Constructor
   */
  public IscheduleMessage() {
  }

  /** Add a field
   *
   * @param nameLc lower cased name
   */
  public void addField(final String nameLc) {
    fields.add(nameLc);
  }

  /** Update the headers
   *
   * @param name of header field
   * @param val its value
   */
  public void addHeader(final String name, final String val) {
    final String nameLc = name.toLowerCase();
    final List<String> vals = headers.computeIfAbsent(nameLc,
                                                k -> new ArrayList<>());

    if (!fields.contains(nameLc)) {
      addField(nameLc);
    }

    vals.add(val);
  }

  /** Get the originator
   *
   *  @return String     originator
   */
  public String getOriginator() {
    return originator;
  }

  /** Get the recipients
   *
   *  @return Set of String     recipients
   */
  public Set<String> getRecipients() {
    return recipients;
  }

  /** Get the iScheduleVersion
   *
   *  @return String     iScheduleVersion
   */
  public String getIScheduleVersion() {
    return iScheduleVersion;
  }

  /** Get the iScheduleMessageId
   *
   *  @return String     iScheduleMessageId
   */
  public String getIScheduleMessageId() {
    return iScheduleMessageId;
  }

  /** Get the dkim signature
   *
   *  @return SignatureRecord
   */
  public SignatureRecord getDkimSignature() {
    return dkimSignature;
  }

  @Override
  public List<String> getFields() {
    return fields;
  }

  @Override
  public List<String> getFields(final String val) {
    /* The rest of the package assumes each 'field' is a header record with name
     * and CR LF terminator.
     *
     * For Ischedule - we concatenate all the headers to produce a single value.
     */
    final List<String> l = headers.get(val.toLowerCase());
    if ((l == null) || (l.isEmpty())) {
      return l;
    }

    final StringBuilder sb = new StringBuilder();
    String delim = "";

    for (final String s: l) {
      sb.append(delim);
      delim = ",";
      sb.append(s);
    }

    final List<String> namedL = new ArrayList<>();

    namedL.add(val + ":" + sb);

    return namedL;
  }

  /**
   * @param val header name
   * @return header values without the name: part
   */
  public List<String> getFieldVals(final String val) {
    return headers.get(val.toLowerCase());
  }

  @Override
  public String toString() {
    return new ToString(this)
            .append("originator", getOriginator())
            .toString();
  }
}
