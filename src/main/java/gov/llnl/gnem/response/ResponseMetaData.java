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

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import javax.measure.Unit;

public class ResponseMetaData implements Serializable {

    private static final long serialVersionUID = 2470284912740366355L;

    private String filename;
    private ResponseType rsptype;
    private Double nominalCalib;
    private Double nominalCalper;
    private Double sensorCalper;
    private Double sensorCalratio;
    private Double wfdiscCalib;
    private Double wfdiscCalper;
    private double time;
    private double endtime;
    private ResponseMetadataExtension extendedMetadata;
    private Double instrumentSampleRate;

    public ResponseMetaData(String filename, ResponseType rsptype, Double nominalCalib, Double nominalCalper, Double sensorCalper, Double sensorCalratio, Double wfdiscCalib, Double wfdiscCalper,
            double time, double endtime) {
        this.filename = filename;
        this.rsptype = rsptype;
        this.nominalCalib = nominalCalib;
        this.nominalCalper = nominalCalper;
        this.sensorCalper = sensorCalper;
        this.sensorCalratio = sensorCalratio;
        this.wfdiscCalib = wfdiscCalib;
        this.wfdiscCalper = wfdiscCalper;
        this.time = time;
        this.endtime = endtime;
        extendedMetadata = new ResponseMetadataExtension();
        instrumentSampleRate = null;
    }

    public ResponseMetaData(File file, ResponseType rsptype, Double nominalCalib, Double nominalCalper, Double sensorCalper, Double sensorCalratio, Double wfdiscCalib, Double wfdiscCalper,
            double time, double endtime) {
        this.filename = file.getAbsolutePath();
        this.rsptype = rsptype;
        this.nominalCalib = nominalCalib;
        this.nominalCalper = nominalCalper;
        this.sensorCalper = sensorCalper;
        this.sensorCalratio = sensorCalratio;
        this.wfdiscCalib = wfdiscCalib;
        this.wfdiscCalper = wfdiscCalper;
        this.time = time;
        this.endtime = endtime;
        extendedMetadata = new ResponseMetadataExtension();
        instrumentSampleRate = null;
    }

    ResponseMetaData(ResponseMetaData rmd) {
        this.filename = rmd.filename;
        this.rsptype = rmd.rsptype;
        this.nominalCalib = rmd.nominalCalib;
        this.nominalCalper = rmd.nominalCalper;
        this.sensorCalper = rmd.sensorCalper;
        this.sensorCalratio = rmd.sensorCalratio;
        this.wfdiscCalib = rmd.wfdiscCalib;
        this.wfdiscCalper = rmd.wfdiscCalper;
        this.time = rmd.time;
        this.endtime = rmd.endtime;
        this.extendedMetadata = rmd.extendedMetadata;
        this.instrumentSampleRate = rmd.instrumentSampleRate;

    }

    public ResponseMetaData(String filename, ResponseType rsptype, Double nominalCalib, Double nominalCalper, Double sensorCalper, Double sensorCalratio, Double wfdiscCalib, Double wfdiscCalper,
            double time, double endtime, ResponseMetadataExtension rme, Double instrumentSampleRate) {
        this.filename = filename;
        this.rsptype = rsptype;
        this.nominalCalib = nominalCalib;
        this.nominalCalper = nominalCalper;
        this.sensorCalper = sensorCalper;
        this.sensorCalratio = sensorCalratio;
        this.wfdiscCalib = wfdiscCalib;
        this.wfdiscCalper = wfdiscCalper;
        this.time = time;
        this.endtime = endtime;
        this.extendedMetadata = rme;
        this.instrumentSampleRate = instrumentSampleRate;
    }

    public boolean isContains(double aTime) {
        return aTime >= time && aTime <= endtime;
    }

    public String getFilename() {
        return filename;
    }

    public ResponseType getRsptype() {
        return rsptype;
    }

    public Double getNominalCalib() {
        return nominalCalib;
    }

    public Double getNominalCalper() {
        return nominalCalper;
    }

    public Double getSensorCalper() {
        return sensorCalper;
    }

    public Double getSensorCalratio() {
        return sensorCalratio;
    }

    public Double getWfdiscCalib() {
        return wfdiscCalib;
    }

    public Double getWfdiscCalper() {
        return wfdiscCalper;
    }

    public double getTime() {
        return time;
    }

    public double getEndtime() {
        return endtime;
    }

    public ResponseMetadataExtension getExtendedMetadata() {
        return extendedMetadata;
    }

    public Double getInstrumentSampleRate() {
        return instrumentSampleRate;
    }

    public boolean isForceInputUnitsRequired() {
        if (extendedMetadata != null) {
            return extendedMetadata.isForceInputUnitsRequired();
        } else {
            return false;
        }
    }

    public Unit<?> getForceInputUnits() {
        if (extendedMetadata != null) {
            return extendedMetadata.getForceInputUnits();
        } else {
            return null;
        }
    }

    public ResponseUnitsSource getResponseUnitsSource() {
        if (extendedMetadata != null) {
            return extendedMetadata.getResponseUnitsSource();
        } else {
            return ResponseUnitsSource.UNKNOWN;
        }
    }

    boolean hasFullWfdiscCalibration() {
        return wfdiscCalib != null && wfdiscCalper != null && wfdiscCalper > 0 && rsptype != ResponseType.EVRESP && rsptype != ResponseType.PAZ;
    }

    boolean hasWfdiscCalibration() {
        return wfdiscCalib != null && wfdiscCalib != 0 && rsptype != ResponseType.EVRESP && rsptype != ResponseType.PAZ;
    }

    boolean hasFullNominalCalibration() {
        return nominalCalib != null && nominalCalper != null && nominalCalper > 0;
    }
    
    boolean hasNominalCalib()
    {
        return nominalCalib != null && nominalCalib != 0;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setRsptype(ResponseType rsptype) {
        this.rsptype = rsptype;
    }

    public void setNominalCalib(Double nominalCalib) {
        this.nominalCalib = nominalCalib;
    }

    public void setNominalCalper(Double nominalCalper) {
        this.nominalCalper = nominalCalper;
    }

    public void setSensorCalper(Double sensorCalper) {
        this.sensorCalper = sensorCalper;
    }

    public void setSensorCalratio(Double sensorCalratio) {
        this.sensorCalratio = sensorCalratio;
    }

    public void setWfdiscCalib(Double wfdiscCalib) {
        this.wfdiscCalib = wfdiscCalib;
    }

    public void setWfdiscCalper(Double wfdiscCalper) {
        this.wfdiscCalper = wfdiscCalper;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public void setEndtime(double endtime) {
        this.endtime = endtime;
    }

    public void setExtendedMetadata(ResponseMetadataExtension extendedMetadata) {
        this.extendedMetadata = extendedMetadata;
    }

    public void setInstrumentSampleRate(Double instrumentSampleRate) {
        this.instrumentSampleRate = instrumentSampleRate;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.filename);
        hash = 59 * hash + Objects.hashCode(this.rsptype);
        hash = 59 * hash + Objects.hashCode(this.nominalCalib);
        hash = 59 * hash + Objects.hashCode(this.nominalCalper);
        hash = 59 * hash + Objects.hashCode(this.sensorCalper);
        hash = 59 * hash + Objects.hashCode(this.sensorCalratio);
        hash = 59 * hash + Objects.hashCode(this.wfdiscCalib);
        hash = 59 * hash + Objects.hashCode(this.wfdiscCalper);
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.time) ^ (Double.doubleToLongBits(this.time) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.endtime) ^ (Double.doubleToLongBits(this.endtime) >>> 32));
        hash = 59 * hash + Objects.hashCode(this.extendedMetadata);
        hash = 59 * hash + Objects.hashCode(this.instrumentSampleRate);
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
        final ResponseMetaData other = (ResponseMetaData) obj;
        if (Double.doubleToLongBits(this.time) != Double.doubleToLongBits(other.time)) {
            return false;
        }
        if (Double.doubleToLongBits(this.endtime) != Double.doubleToLongBits(other.endtime)) {
            return false;
        }
        if (!Objects.equals(this.filename, other.filename)) {
            return false;
        }
        if (this.rsptype != other.rsptype) {
            return false;
        }
        if (!Objects.equals(this.nominalCalib, other.nominalCalib)) {
            return false;
        }
        if (!Objects.equals(this.nominalCalper, other.nominalCalper)) {
            return false;
        }
        if (!Objects.equals(this.sensorCalper, other.sensorCalper)) {
            return false;
        }
        if (!Objects.equals(this.sensorCalratio, other.sensorCalratio)) {
            return false;
        }
        if (!Objects.equals(this.wfdiscCalib, other.wfdiscCalib)) {
            return false;
        }
        if (!Objects.equals(this.wfdiscCalper, other.wfdiscCalper)) {
            return false;
        }
        if (!Objects.equals(this.extendedMetadata, other.extendedMetadata)) {
            return false;
        }
        if (!Objects.equals(this.instrumentSampleRate, other.instrumentSampleRate)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ResponseMetaData{"
                + "filename="
                + filename
                + ", rsptype="
                + rsptype
                + ", nominalCalib="
                + nominalCalib
                + ", nominalCalper="
                + nominalCalper
                + ", sensorCalper="
                + sensorCalper
                + ", sensorCalratio="
                + sensorCalratio
                + ", wfdiscCalib="
                + wfdiscCalib
                + ", wfdiscCalper="
                + wfdiscCalper
                + ", time="
                + time
                + ", endtime="
                + endtime
                + ", extendedMetadata="
                + extendedMetadata
                + ", instrumentSampleRate="
                + instrumentSampleRate
                + '}';
    }
}
