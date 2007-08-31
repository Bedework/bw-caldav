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
package org.bedework.caldav.client;

import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.svc.EventInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author douglm
 *
 */
public class FbResponse extends Response {
  private BwDateTime start;
  private BwDateTime end;

  /**
   */
  public static class FbResponseElement {
    private String recipient;
    private String reqStatus;
    private EventInfo freeBusy;

    private String davError;

    /**
     * @param val
     */
    public void setRecipient(String val) {
      recipient = val;
    }

    /**
     * @return recipient
     */
    public String getRecipient() {
      return recipient;
    }

    /**
     * @param val
     */
    public void setReqStatus(String val) {
      reqStatus = val;
    }

    /**
     * @return reqStatus
     */
    public String getReqStatus() {
      return reqStatus;
    }

    /**
     * @param val
     */
    public void setFreeBusy(EventInfo val) {
      freeBusy = val;
    }

    /**
     * @return reqStatus
     */
    public EventInfo getFreeBusy() {
      return freeBusy;
    }
    /**
     * @param val
     */
    public void setDavError(String val) {
      davError = val;
    }

    /**
     * @return String
     */
    public String getDavError() {
      return davError;
    }

  }

  private Collection<FbResponseElement> responses = new ArrayList<FbResponseElement>();

  /**
   * @param val
   */
  public void setStart(BwDateTime val) {
    start = val;
  }

  /**
   * @return BwDateTime start
   */
  public BwDateTime getStart() {
    return start;
  }

  /**
   * @param val
   */
  public void setEnd(BwDateTime val) {
    end = val;
  }

  /**
   * @return BwDateTime end
   */
  public BwDateTime getEnd() {
    return end;
  }

  /**
   * @return Collection<FbResponseElement>
   */
  public Collection<FbResponseElement> getResponses() {
    return responses;
  }

  /**
   * @param val
   */
  public void addResponse(FbResponseElement val) {
    responses.add(val);
  }
}
