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

import java.util.Arrays;
import java.util.Objects;

public class GroupStage {

    private Paz paz;
    private Fap[] fap;
    private Fir fir;
    private double sampleRate;
    private int decimation;
    private double gain;
    private double normalization;
    private double delay;

    public GroupStage(double sampleRate, int decimation, double gain, double normalization, double delay) {
        this.sampleRate = sampleRate;
        this.decimation = decimation;
        this.gain = gain;
        this.normalization = normalization;
        this.delay = delay;
    }

    public GroupStage setPaz(Paz paz) {
        this.paz = paz;
        return this;
    }

    public Paz getPaz() {
        return paz;
    }

    public GroupStage setFap(Fap[] fap) {
        this.fap = fap;
        return this;
    }

    public GroupStage setFir(Fir fir) {
        this.fir = fir;
        return this;
    }

    public Fap[] getFap() {
        return fap;
    }

    public Fir getFir() {
        return fir;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getDecimation() {
        return decimation;
    }

    public void setDecimation(int decimation) {
        this.decimation = decimation;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(fap);
        result = prime * result + Objects.hash(decimation, delay, fir, gain, normalization, paz, sampleRate);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GroupStage)) {
            return false;
        }
        GroupStage other = (GroupStage) obj;
        return decimation == other.decimation
                && Double.doubleToLongBits(delay) == Double.doubleToLongBits(other.delay)
                && Arrays.equals(fap, other.fap)
                && Objects.equals(fir, other.fir)
                && Double.doubleToLongBits(gain) == Double.doubleToLongBits(other.gain)
                && Double.doubleToLongBits(normalization) == Double.doubleToLongBits(other.normalization)
                && Objects.equals(paz, other.paz)
                && Double.doubleToLongBits(sampleRate) == Double.doubleToLongBits(other.sampleRate);
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("GroupStage [paz=")
               .append(paz)
               .append(", fap=")
               .append(fap != null ? Arrays.asList(fap).subList(0, Math.min(fap.length, maxLen)) : null)
               .append(", fir=")
               .append(fir)
               .append(", sampleRate=")
               .append(sampleRate)
               .append(", decimation=")
               .append(decimation)
               .append(", gain=")
               .append(gain)
               .append(", normalization=")
               .append(normalization)
               .append(", delay=")
               .append(delay)
               .append("]");
        return builder.toString();
    }

}
