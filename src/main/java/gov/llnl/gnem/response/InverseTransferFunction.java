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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.gnem.response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.apache.commons.math3.complex.Complex;

import com.isti.jevalresp.ResponseUnits;


/**
 *
 * @author dodge1
 */
public class InverseTransferFunction {

    private final Complex[] function;
    private final double[] frequencies;
    private final ResponseUnits responseUnits;
    private final Unit<? extends Quantity<?>> originalInputUnits;
    private final TransferData inputTransferData;

    public InverseTransferFunction(Complex[] function,
            double[] frequencies,
            ResponseUnits responseUnits,
            Unit<?> originalInputUnits,
            TransferData inputTransferData) {
        this.function = function.clone();
        this.frequencies = frequencies.clone();
        this.responseUnits = responseUnits;
        this.originalInputUnits = originalInputUnits;
        this.inputTransferData = inputTransferData;
    }

    public Complex[] getValues() {
        return function.clone();
    }

    /**
     * @return the xre
     */
    public double[] getXre() {
        double[] result = new double[function.length];
        for (int j = 0; j < result.length; ++j) {
            result[j] = function[j].getReal();
        }
        return result;
    }

    /**
     * @return the xim
     */
    public double[] getXim() {
        double[] result = new double[function.length];
        for (int j = 0; j < result.length; ++j) {
            result[j] = function[j].getImaginary();
        }
        return result;
    }

    public void writeToTextFile(String filename) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter(filename)) {
            Complex[] values = getValues();
            for (int j = 0; j < values.length; ++j) {
                pw.println(frequencies[j] + "  " + values[j].abs());
            }
        }
    }

    public double[] getFrequencies() {
        return frequencies.clone();
    }

    public ResponseUnits getResponseUnits() {
        return responseUnits;
    }

    public double[] getAmplitudes() {
        double[] result = new double[function.length];
        for (int j = 0; j < function.length; ++j) {
            result[j] = function[j].abs();
        }
        return result;
    }

    public Unit<? extends Quantity<?>> getOriginalInputUnits() {
        return originalInputUnits;
    }

    public TransferData getInputTransferData() {
        return inputTransferData;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.deepHashCode(this.function);
        hash = 59 * hash + Arrays.hashCode(this.frequencies);
        hash = 59 * hash + Objects.hashCode(this.responseUnits);
        hash = 59 * hash + Objects.hashCode(this.originalInputUnits);
        hash = 59 * hash + Objects.hashCode(this.inputTransferData);
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
        final InverseTransferFunction other = (InverseTransferFunction) obj;
        if (!Arrays.deepEquals(this.function, other.function)) {
            return false;
        }
        if (!Arrays.equals(this.frequencies, other.frequencies)) {
            return false;
        }
        if (!Objects.equals(this.responseUnits, other.responseUnits)) {
            return false;
        }
        if (!Objects.equals(this.originalInputUnits, other.originalInputUnits)) {
            return false;
        }
        if (!Objects.equals(this.inputTransferData, other.inputTransferData)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "InverseTransferFunction{" + "function=" + function + ", frequencies=" + frequencies + ", responseUnits=" + responseUnits + ", originalInputUnits=" + originalInputUnits + ", inputTransferData=" + inputTransferData + '}';
    }

}
