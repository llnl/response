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

public class Polar {

    protected double p;
    protected double a;

    public Polar(double p, double a) {
        this.p = p;
        this.a = a;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, p);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Polar)) {
            return false;
        }
        Polar other = (Polar) obj;
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(other.a) && Double.doubleToLongBits(p) == Double.doubleToLongBits(other.p);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Polar [p=").append(p).append(", a=").append(a).append("]");
        return builder.toString();
    }

}