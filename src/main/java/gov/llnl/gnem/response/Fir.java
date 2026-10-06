package gov.llnl.gnem.response;

import java.util.Arrays;
import java.util.Objects;

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
public class Fir {

    protected double isr;
    protected int nnc;
    protected int ndc;
    protected double[] nu;
    protected double[] nue;
    protected double[] de;
    protected double[] dee;

    public Fir(double isr) {
        this.isr = isr;
    }

    public void setNumerator(int nnc) {
        this.nnc = nnc;
        nu = new double[nnc];
        nue = new double[nnc];
    }

    public void setDenominator(int ndc) {
        this.ndc = ndc;
        de = new double[ndc];
        dee = new double[ndc];
    }

    public double getIsr() {
        return isr;
    }

    public void setIsr(double isr) {
        this.isr = isr;
    }

    public int getNnc() {
        return nnc;
    }

    public void setNnc(int nnc) {
        this.nnc = nnc;
    }

    public int getNdc() {
        return ndc;
    }

    public void setNdc(int ndc) {
        this.ndc = ndc;
    }

    public double[] getNu() {
        return nu;
    }

    public void setNu(double[] nu) {
        this.nu = nu;
    }

    public double[] getNue() {
        return nue;
    }

    public void setNue(double[] nue) {
        this.nue = nue;
    }

    public double[] getDe() {
        return de;
    }

    public void setDe(double[] de) {
        this.de = de;
    }

    public double[] getDee() {
        return dee;
    }

    public void setDee(double[] dee) {
        this.dee = dee;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(de);
        result = prime * result + Arrays.hashCode(dee);
        result = prime * result + Arrays.hashCode(nu);
        result = prime * result + Arrays.hashCode(nue);
        result = prime * result + Objects.hash(isr, ndc, nnc);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fir)) {
            return false;
        }
        Fir other = (Fir) obj;
        return Arrays.equals(de, other.de)
                && Arrays.equals(dee, other.dee)
                && Double.doubleToLongBits(isr) == Double.doubleToLongBits(other.isr)
                && ndc == other.ndc
                && nnc == other.nnc
                && Arrays.equals(nu, other.nu)
                && Arrays.equals(nue, other.nue);
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("Fir [isr=")
               .append(isr)
               .append(", nnc=")
               .append(nnc)
               .append(", ndc=")
               .append(ndc)
               .append(", nu=")
               .append(nu != null ? Arrays.toString(Arrays.copyOf(nu, Math.min(nu.length, maxLen))) : null)
               .append(", nue=")
               .append(nue != null ? Arrays.toString(Arrays.copyOf(nue, Math.min(nue.length, maxLen))) : null)
               .append(", de=")
               .append(de != null ? Arrays.toString(Arrays.copyOf(de, Math.min(de.length, maxLen))) : null)
               .append(", dee=")
               .append(dee != null ? Arrays.toString(Arrays.copyOf(dee, Math.min(dee.length, maxLen))) : null)
               .append("]");
        return builder.toString();
    }

}