/*-
 * #%L
 * Seismic Response Processing Module
 *  LLNL-CODE-856351
 *  This work was performed under the auspices of the U.S. Department of Energy
 *  by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
 * %%
 * Copyright (C) 2023 Lawrence Livermore National Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.isti.jevalresp;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author dodge1
 */
public class ChannelMatchPolicy implements Serializable {

    private static final long serialVersionUID = 6639216586376908870L;

    public static enum Policy {
        NO_MATCH_REQUIRED, FULL_MATCH, STA_CHAN_EPOCH_MATCH, NET_STA_CHAN_EPOCH_MATCH, AGENCY_NET_STA_CHAN_LOCID_EPOCH_MATCH, NET_STA_CHAN_LOCID_EPOCH_MATCH, NET_STA_CHAN_MATCH, STA_CHAN_MATCH
    }

    private final boolean matchAgency;
    private final boolean matchNet;
    private final boolean matchNetJdate;
    private final boolean matchSta;
    private final boolean matchChannel;
    private final boolean matchLocationCode;
    private final boolean matchEpoch;
    private final Policy policy;

    public static String getPolicyListString() {
        Policy[] all = Policy.values();
        StringBuilder sb = new StringBuilder("Available policies are: (");
        for (int j = 0; j < all.length - 1; ++j) {
            sb.append(all[j].toString()).append(", ");
        }
        sb.append(all[all.length - 1].toString()).append(").");
        return sb.toString();
    }

    public boolean isParsedFieldsMatchRequest(String station, String staNameStr, String channel, String chaNameStr, String network, String netNameStr, String locid, String siteNameStr,
            Date requestedEndDate, Date requestedBeginDate, Date parsedStartDateObj, Date parsedEndDateObj) {
        if (matchNet && !network.equals(netNameStr)) {
            return false;
        }
        if (matchSta && !station.equals(staNameStr)) {
            return false;
        }
        if (matchChannel && !channel.equals(chaNameStr)) {
            return false;
        }

        if (matchEpoch && !requestedEpochIsValid(requestedEndDate, requestedBeginDate, parsedStartDateObj, parsedEndDateObj)) {
            return false;
        }
        if (matchLocationCode && !locationCodesMatch(locid, siteNameStr)) {
            return false;
        }
        return true;
    }

    private boolean locationCodesMatch(String locid, String siteNameStr) {
        if (locid == null || locid.equals("--") | locid.equals("") | locid.equals("??") | locid.equals("*") | locid.equals("**")) {
            locid = "--";
        }
        if (siteNameStr == null || siteNameStr.equals("--") | siteNameStr.equals("") | siteNameStr.equals("??") | siteNameStr.equals("*") | siteNameStr.equals("**")) {
            siteNameStr = "--";
        }

        return locid.equals(siteNameStr);
    }

    public ChannelMatchPolicy(Policy policy) {
        this.policy = policy;
        switch (policy) {
        case NO_MATCH_REQUIRED: {
            matchAgency = false;
            matchNet = false;
            matchNetJdate = false;
            matchSta = false;
            matchChannel = false;
            matchLocationCode = false;
            matchEpoch = false;
            break;
        }
        case AGENCY_NET_STA_CHAN_LOCID_EPOCH_MATCH: {
            matchAgency = true;
            matchNet = true;
            matchNetJdate = false;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = true;
            matchEpoch = true;
            break;
        }
        case FULL_MATCH: {
            matchAgency = true;
            matchNet = true;
            matchNetJdate = true;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = true;
            matchEpoch = true;
            break;
        }
        case STA_CHAN_EPOCH_MATCH: {
            matchAgency = false;
            matchNet = false;
            matchNetJdate = false;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = false;
            matchEpoch = true;
            break;
        }
        case NET_STA_CHAN_EPOCH_MATCH: {
            matchAgency = false;
            matchNet = true;
            matchNetJdate = false;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = false;
            matchEpoch = true;
            break;
        }
        case NET_STA_CHAN_LOCID_EPOCH_MATCH:
            matchAgency = false;
            matchNet = true;
            matchNetJdate = false;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = true;
            matchEpoch = true;
            break;
        case NET_STA_CHAN_MATCH:
            matchAgency = false;
            matchNet = true;
            matchNetJdate = false;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = false;
            matchEpoch = false;
            break;
        case STA_CHAN_MATCH:
            matchAgency = false;
            matchNet = false;
            matchNetJdate = false;
            matchSta = true;
            matchChannel = true;
            matchLocationCode = false;
            matchEpoch = false;
            break;

        default:
            throw new IllegalArgumentException("Unrecognized policy: " + policy);

        }

    }

    private static boolean requestedEpochIsValid(Date requestedEndDate, Date requestedBeginDate, Date parsedStartDateObj, Date parsedEndDateObj) {
        if (requestedBeginDate != null) {
            return (requestedBeginDate.compareTo(parsedStartDateObj) >= 0 && requestedBeginDate.compareTo(parsedEndDateObj) <= 0);
        } else if (requestedEndDate != null) {
            return (requestedEndDate.compareTo(parsedStartDateObj) >= 0 && requestedEndDate.compareTo(parsedEndDateObj) <= 0);
        } else {
            return false;
        }
    }

    public boolean isMatchNet() {
        return matchNet;
    }

    public boolean isMatchSta() {
        return matchSta;
    }

    public boolean isMatchChannel() {
        return matchChannel;
    }

    public boolean isMatchLocationCode() {
        return matchLocationCode;
    }

    public boolean isMatchEpoch() {
        return matchEpoch;
    }

    public Policy getPolicy() {
        return policy;
    }

    public boolean isMatchAgency() {
        return matchAgency;
    }

    public boolean isMatchNetJdate() {
        return matchNetJdate;
    }

    public static String getHelpMessageString() {
        return "The ChannelMatchPolicy controls how much of the channel metadata is used in "
                + "finding and parsing responses. Up to 7 fields can be required to match for "
                + "selection of response metadata and up to 5 may be used for matching blocks "
                + "in RESP-type responses. (Note: In the old LLNL schema only sta,chan, and "
                + "time can be used and more restrictive key combinations are not supported. \n"
                + getPolicyListString();
    }

    @Override
    public String toString() {
        return "ChannelMatchPolicy{"
                + "matchAgency="
                + matchAgency
                + ", matchNet="
                + matchNet
                + ", matchNetJdate="
                + matchNetJdate
                + ", matchSta="
                + matchSta
                + ", matchChannel="
                + matchChannel
                + ", matchLocationCode="
                + matchLocationCode
                + ", matchEpoch="
                + matchEpoch
                + ", policy="
                + policy
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.matchAgency ? 1 : 0);
        hash = 89 * hash + (this.matchNet ? 1 : 0);
        hash = 89 * hash + (this.matchNetJdate ? 1 : 0);
        hash = 89 * hash + (this.matchSta ? 1 : 0);
        hash = 89 * hash + (this.matchChannel ? 1 : 0);
        hash = 89 * hash + (this.matchLocationCode ? 1 : 0);
        hash = 89 * hash + (this.matchEpoch ? 1 : 0);
        hash = 89 * hash + Objects.hashCode(this.policy);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChannelMatchPolicy other = (ChannelMatchPolicy) obj;
        if (this.matchAgency != other.matchAgency) {
            return false;
        }
        if (this.matchNet != other.matchNet) {
            return false;
        }
        if (this.matchNetJdate != other.matchNetJdate) {
            return false;
        }
        if (this.matchSta != other.matchSta) {
            return false;
        }
        if (this.matchChannel != other.matchChannel) {
            return false;
        }
        if (this.matchLocationCode != other.matchLocationCode) {
            return false;
        }
        if (this.matchEpoch != other.matchEpoch) {
            return false;
        }
        if (this.policy != other.policy) {
            return false;
        }
        return true;
    }

}
