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

import java.util.Objects;

public class Fap {

    protected double f;
    protected double a;
    protected double p;
    protected double ae;
    protected double pe;

    public Fap(double f, double a, double p, double ae, double pe) {
        this.f = f;
        this.a = a;
        this.p = p;
        this.ae = ae;
        this.pe = pe;
    }

    public double getF() {
        return f;
    }

    public void setF(double f) {
        this.f = f;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getAe() {
        return ae;
    }

    public void setAe(double ae) {
        this.ae = ae;
    }

    public double getPe() {
        return pe;
    }

    public void setPe(double pe) {
        this.pe = pe;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Fap [f=").append(f).append(", a=").append(a).append(", p=").append(p).append(", ae=").append(ae).append(", pe=").append(pe).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, ae, f, p, pe);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fap)) {
            return false;
        }
        Fap other = (Fap) obj;
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(other.a)
                && Double.doubleToLongBits(ae) == Double.doubleToLongBits(other.ae)
                && Double.doubleToLongBits(f) == Double.doubleToLongBits(other.f)
                && Double.doubleToLongBits(p) == Double.doubleToLongBits(other.p)
                && Double.doubleToLongBits(pe) == Double.doubleToLongBits(other.pe);
    }
}