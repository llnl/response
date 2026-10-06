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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Quantity;
import javax.measure.Unit;

import com.isti.jevalresp.ChannelMatchPolicy;
import com.isti.jevalresp.ResponseUnits;

/**
 *
 * @author dodge1
 */
public class ToResponseLookupKey implements Serializable{

    private static final long serialVersionUID = -2181120151631745321L;

    private final int nfft;
    private final double samprate;
    private final String network;
    private final String sta;
    private final String chan;
    private final String locid;
    private final ResponseMetaData rmd;
    private final ChannelMatchPolicy policy;
    private final Unit<?> requestedUnits;
    private final Unit<? extends Quantity<?>> forcedInputUnits;

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(nfft);
        s.writeDouble(samprate);
        s.writeObject(network);
        s.writeObject(sta);
        s.writeObject(chan);
        s.writeObject(locid);
        s.writeObject(rmd);
        s.writeObject(policy);
         s.writeObject(requestedUnits.toString());
         s.writeObject(forcedInputUnits != null ? forcedInputUnits.toString() : null);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        
        try {
            Class type = ToResponseLookupKey.class;
            Field field = type.getDeclaredField("nfft");
            // make the field non final
            field.setAccessible(true);
            field.set(this, s.readInt());

            // make the field final again
            field.setAccessible(false);
            
            field = type.getDeclaredField("samprate");
            field.setAccessible(true);
            field.set(this, s.readDouble());
            field.setAccessible(false);
            
            field = type.getDeclaredField("network");
            field.setAccessible(true);
            field.set(this, (String)s.readObject());
            field.setAccessible(false);
            
            field = type.getDeclaredField("sta");
            field.setAccessible(true);
            field.set(this, (String)s.readObject());
            field.setAccessible(false);
            
            field = type.getDeclaredField("chan");
            field.setAccessible(true);
            field.set(this, (String)s.readObject());
            field.setAccessible(false);
            
            field = type.getDeclaredField("locid");
            field.setAccessible(true);
            field.set(this, (String)s.readObject());
            field.setAccessible(false);
            
            field = type.getDeclaredField("rmd");
            field.setAccessible(true);
            field.set(this, (ResponseMetaData)s.readObject());
            field.setAccessible(false);
             
            field = type.getDeclaredField("policy");
            field.setAccessible(true);
            field.set(this, (ChannelMatchPolicy)s.readObject());
            field.setAccessible(false);
             
            String tmp = (String) s.readObject();
            field = type.getDeclaredField("requestedUnits");
            field.setAccessible(true);
            field.set(this, tmp != null ? ResponseUnits.parse(tmp) : null);
            field.setAccessible(false);
             
            tmp = (String) s.readObject();
            field = type.getDeclaredField("forcedInputUnits");
            field.setAccessible(true);
            field.set(this, tmp != null ? ResponseUnits.parse(tmp) : null);
            field.setAccessible(false);
           
         } catch (NoSuchFieldException ex) {
            Logger.getLogger(ToResponseLookupKey.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ToResponseLookupKey.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ToResponseLookupKey.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ToResponseLookupKey.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ToResponseLookupKey(int nfft,
            double samprate,
            String network,
            String sta,
            String chan,
            String locid,
            ResponseMetaData rmd,
            ChannelMatchPolicy policy,
            Unit<?> requestedUnits,
            Unit<?>  forcedInputUnits) {
        this.nfft = nfft;
        this.samprate = samprate;
        this.network = network;
        this.sta = sta;
        this.chan = chan;
        this.locid = locid;
        this.rmd = rmd;
        this.policy = policy;
        this.requestedUnits = requestedUnits;
        this.forcedInputUnits = forcedInputUnits;
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

    public Unit<?> getRequestedUnits() {
        return requestedUnits;
    }
  
    public boolean contains(double time) {
        return rmd.isContains(time);
    }

    public Unit<? extends Quantity<?>> getForcedInputUnits() {
        return forcedInputUnits;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.nfft;
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.samprate) ^ (Double.doubleToLongBits(this.samprate) >>> 32));
        hash = 89 * hash + Objects.hashCode(this.network);
        hash = 89 * hash + Objects.hashCode(this.sta);
        hash = 89 * hash + Objects.hashCode(this.chan);
        hash = 89 * hash + Objects.hashCode(this.locid);
        hash = 89 * hash + Objects.hashCode(this.rmd);
        hash = 89 * hash + Objects.hashCode(this.policy);
        hash = 89 * hash + Objects.hashCode(this.requestedUnits);
        hash = 89 * hash + Objects.hashCode(this.forcedInputUnits);
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
        final ToResponseLookupKey other = (ToResponseLookupKey) obj;
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
        if (!Objects.equals(this.requestedUnits, other.requestedUnits)) {
            return false;
        }
        if (!Objects.equals(this.forcedInputUnits, other.forcedInputUnits)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ToResponseLookupKey{" + "nfft=" + nfft + ", samprate=" + samprate + ", network=" + network + ", sta=" + sta + ", chan=" + chan + ", locid=" + locid + ", rmd=" + rmd + ", policy=" + policy + ", requestedUnits=" + requestedUnits + ", forcedInputUnits=" + forcedInputUnits + '}';
    }

}
