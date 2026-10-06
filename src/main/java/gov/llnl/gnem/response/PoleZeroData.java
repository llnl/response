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
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gov.llnl.gnem.response;

import java.util.ArrayList;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author dodge1
 */
public class PoleZeroData {

    private final double constant;
    private final Complex[] zeros;
    private final int nzeros;
    private final Complex[] poles;
    private final int npoles;

    public PoleZeroData(double constant, ArrayList<Complex> polesIn, ArrayList<Complex> zerosIn) {
        this.constant = constant;
        this.zeros = zerosIn.toArray(new Complex[0]);
        this.nzeros = zerosIn.size();
        this.poles = polesIn.toArray(new Complex[0]);
        this.npoles = polesIn.size();
    }

    public double getConstant() {
        return constant;
    }

    public Complex[] getZeros() {
        return zeros.clone();
    }

    public int getNzeros() {
        return nzeros;
    }

    public Complex[] getPoles() {
        return poles.clone();
    }

    public int getNpoles() {
        return npoles;
    }

    @Override
    public String toString() {
        return "PoleZeroData{" + "constant=" + constant + ", zeros=" + zeros + ", nzeros=" + nzeros + ", poles=" + poles + ", npoles=" + npoles + '}';
    }
    
}
