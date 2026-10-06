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

package gov.llnl.gnem.response;

import java.io.Serializable;
import java.util.Objects;

import com.isti.jevalresp.ChannelMatchPolicy;

/**
 *
 * @author dodge1
 */
public class FromResponseLookupKey implements Serializable{

    private static final long serialVersionUID = -2181120151631745321L;

    private final int nfft;
    private final double samprate;
    private final String network;
    private final String sta;
    private final String chan;
    private final String locid;
    private final ResponseMetaData rmd;
    private final ChannelMatchPolicy policy;

    public FromResponseLookupKey(int nfft,
            double samprate,
            String network,
            String sta,
            String chan,
            String locid,
            ResponseMetaData rmd,
            ChannelMatchPolicy policy) {
        this.nfft = nfft;
        this.samprate = samprate;
        this.network = network;
        this.sta = sta;
        this.chan = chan;
        this.locid = locid;
        this.rmd = rmd;
        this.policy = policy;
    }

    public int getNfft() {
        return nfft;
    }

    public double getSamprate() {
        return samprate;
    }

    public String getNetwork() {
        return network;
    }

    public String getSta() {
        return sta;
    }

    public String getChan() {
        return chan;
    }

    public String getLocid() {
        return locid;
    }

    public ResponseMetaData getRmd() {
        return rmd;
    }

    public ChannelMatchPolicy getPolicy() {
        return policy;
    }


    public boolean contains(double time) {
        return rmd.isContains(time);
    }

    @Override
    public String toString() {
        return "FromResponseLookupKey{" + "nfft=" + nfft + ", samprate=" + samprate + ", network=" + network + ", sta=" + sta + ", chan=" + chan + ", locid=" + locid + ", rmd=" + rmd + ", policy=" + policy + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.nfft;
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.samprate) ^ (Double.doubleToLongBits(this.samprate) >>> 32));
        hash = 47 * hash + Objects.hashCode(this.network);
        hash = 47 * hash + Objects.hashCode(this.sta);
        hash = 47 * hash + Objects.hashCode(this.chan);
        hash = 47 * hash + Objects.hashCode(this.locid);
        hash = 47 * hash + Objects.hashCode(this.rmd);
        hash = 47 * hash + Objects.hashCode(this.policy);
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
        final FromResponseLookupKey other = (FromResponseLookupKey) obj;
        if (this.nfft != other.nfft) {
            return false;
        }
        if (Double.doubleToLongBits(this.samprate) != Double.doubleToLongBits(other.samprate)) {
            return false;
        }
        if (!Objects.equals(this.network, other.network)) {
            return false;
        }
        if (!Objects.equals(this.sta, other.sta)) {
            return false;
        }
        if (!Objects.equals(this.chan, other.chan)) {
            return false;
        }
        if (!Objects.equals(this.locid, other.locid)) {
            return false;
        }
        if (!Objects.equals(this.rmd, other.rmd)) {
            return false;
        }
        if (!Objects.equals(this.policy, other.policy)) {
            return false;
        }
        return true;
    }

}
