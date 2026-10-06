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

import javax.measure.Unit;

import org.apache.commons.math3.complex.Complex;

import com.isti.jevalresp.ResponseUnits;

public class TransferFunctionUtils {

    private final static double[] DISCRETE_SAMPLE_RATE = { 0.01, 0.1, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 20.0, 25.0, 40.0, 50.0, 80.0, 100.0, 120.0, 125.0, 200.0, 250.0, 500.0, 1000.0 };
    private final static double SAMPLE_RATE_FRACTIONAL_TOLERANCE = 0.001;

    /*
     * given a int, return the next power of 2 (the smalles power of 2 greater than num).
     */
    public static int next2(int num) {
        int result = 2;
        while (true) {
            if (result > num) {
                return result;
            } else {
                result *= 2;
            }
        }
    }

    public static String removeRegexEscape(String tmpSta) {
        char oldChar = '\\';
        StringBuilder sb = new StringBuilder();
        for (char c : tmpSta.toCharArray()) {
            if (c != oldChar) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static TransferData packageTransferFunctionSamples(double delfreq, double[] xre, double[] xim, ResponseUnits units, ResponseMetaData metadata) {
        Complex[] data = new Complex[xre.length];
        for (int i = 0; i < xre.length; i++) {
            data[i] = new Complex(xre[i], xim[i]);
        }
        return new TransferData(data, delfreq, units, metadata);
    }

    public static InverseTransferFunction buildInverseTransferFunction(int nsamp, double samprate, ResponseMetaData metadata, FreqLimits limits, TransferData transferData, Unit<?> originalInputUnits)
            throws IOException {
        return buildInverseTransferFunction(nsamp, samprate, metadata, limits, transferData, null, originalInputUnits);
    }

    public static InverseTransferFunction buildInverseTransferFunction(int nsamp, double samprate, ResponseMetaData metadata, FreqLimits limits, TransferData fromTransferData,
            TransferData toTransferData, Unit<?> originalInputUnits) throws IOException {
        int nfft = TransferFunctionUtils.next2(nsamp);
        int nfreq = nfft / 2 + 1;

        double delta = 1.0 / samprate;
        double delfrq = 1.0 / (nfft * delta);
        double[] frequencies = new double[nfreq];
        for (int i = 0; i < nfreq; i++) {
            double freq = i * delfrq;
            frequencies[i] = freq;
        }

        Complex[] fromTransferFunctionSamples = fromTransferData != null ? fromTransferData.getWorkingData() : null;
        Complex[] toTransferFunctionSamples = toTransferData != null ? toTransferData.getWorkingData() : null;
        int numFreqencies = getNumFrequencies(fromTransferFunctionSamples, toTransferFunctionSamples);
        if (numFreqencies != nfreq) {
            throw new IllegalStateException("Transfer function lengths do not match length of zero-padded seismogram!");
        }
        Complex[] inverse = new Complex[numFreqencies];
        Complex one = new Complex(1.0, 0.0);
        Complex zero = new Complex(0.0, 0.0);
        for (int i = 0; i < nfreq; i++) {
            Complex fromSamp = fromTransferFunctionSamples != null ? fromTransferFunctionSamples[i] : one;
            double denr = fromSamp.multiply(fromSamp.conjugate()).abs(); // Compute water level
            Complex tmp = denr <= Float.MIN_NORMAL ? zero : one.divide(fromSamp); // Divide by transfer function value or set to zero
            double freq = i * delfrq;
            // original code multiplies by delfrq here.
            double fac = limits != null ? taper(freq, limits.getLowpass(), limits.getLowcut()) * taper(freq, limits.getHighpass(), limits.getHighcut()) : 1.0;
            Complex toMultiplier = toTransferFunctionSamples != null ? toTransferFunctionSamples[i] : one;
            inverse[i] = tmp.multiply(fac).multiply(toMultiplier); // Apply taper and maybe sample from toTransfer function.

        }
        ResponseUnits responseUnits = new ResponseUnits();
        if (fromTransferData != null && toTransferData == null) {
            responseUnits = fromTransferData.getWorkingUnits();
        }
        return new InverseTransferFunction(inverse, frequencies, responseUnits, originalInputUnits, fromTransferData);
    }

    private static double taper(double freq, double fqh, double fql) {
        final double twopi = Math.PI * 2;

        /*
         * SUBROUTINE TO TAPER SPECTRA BY A COSINE
         *
         * CALLING ARGUMENTS:
         *
         * FREQ - FREQUENCY IN QUESTION FQH - FREQUENCY AT WHICH THERE IS A TRANSITION BETWEEN UNITY AND THE TAPER FQL -
         * FREQUENCY AT WHICH THERE IS A TRANSITION BETWEEN ZERO AND THE TAPER NOTE: IF FQL>FQH LO-PASS IF FQH>FQL
         * HI-PASS
         *
         */
        double dblepi = 0.5e0 * twopi;

        double taper_v = 0.0;
        if (fql > fqh) {
            /*
             * LO-PASS CASE
             *
             */
            if (freq < fqh) {
                taper_v = 1.0e0;
            }
            if (freq >= fqh && freq <= fql) {
                taper_v = 0.5e0 * (1.0e0 + Math.cos(dblepi * (freq - fqh) / (fql - fqh)));
            }
            if (freq > fql) {
                taper_v = 0.0e0;
            }
            return taper_v;
        }
        if (fqh > fql) {
            /*
             * HI-PASS CASE
             *
             */
            if (freq < fql) {
                taper_v = 0.0e0;
            }
            if (freq >= fql && freq <= fqh) {
                taper_v = 0.5e0 * (1.0e0 - Math.cos(dblepi * (freq - fql) / (fqh - fql)));
            }
            if (freq > fqh) {
                taper_v = 1.0e0;
            }
            return taper_v;
        }

        return taper_v;
    }

    public static void getran(double delfrq, PoleZeroData pzd, double[] xre, double[] xim) {
        int idx, jdx;
        double delomg, fac, omega, ti, ti0, tid, tin, tr, tr0, trd, trn;
        final double twopi = Math.PI * 2;
        /*
         * .....Subroutine to compute the transfer function.....
         *
         */

        int nzero = pzd.getNzeros();
        int npole = pzd.getNpoles();
        Complex[] zero = pzd.getZeros();
        Complex[] pole = pzd.getPoles();
        double constant = pzd.getConstant();

        int nfreq = xre.length;
        delomg = twopi * delfrq;
        for (jdx = 0; jdx < nfreq; jdx++) {
            omega = delomg * (jdx);
            trn = 1.0e0;
            tin = 0.0e0;
            if (nzero != 0) {
                for (idx = 0; idx < nzero; idx++) {
                    tr = -zero[idx].getReal();
                    ti = omega - zero[idx].getImaginary();
                    tr0 = trn * tr - tin * ti;
                    ti0 = trn * ti + tin * tr;
                    trn = tr0;
                    tin = ti0;
                }
            }
            trd = 1.0e0;
            tid = 0.0e0;
            if (npole != 0) {
                for (idx = 0; idx < npole; idx++) {
                    tr = -pole[idx].getReal();
                    ti = omega - pole[idx].getImaginary();
                    tr0 = trd * tr - tid * ti;
                    ti0 = trd * ti + tid * tr;
                    trd = tr0;
                    tid = ti0;
                }
            }

            fac = (constant) / (Math.pow(trd, 2) + Math.pow(tid, 2));
            xre[jdx] = fac * (trn * trd + tin * tid);
            xim[jdx] = fac * (trd * tin - trn * tid);
        }
    }

    public static double standardizeSampleRate(double value) {

        for (double element : DISCRETE_SAMPLE_RATE) {
            double error = Math.abs((value - element) / element);
            if (error <= SAMPLE_RATE_FRACTIONAL_TOLERANCE) {
                return element;
            }
        }
        return value;
    }

    private static int getNumFrequencies(Complex[] fromTransferFunctionSamples, Complex[] toTransferFunctionSamples) {
        int result = 0;
        if (fromTransferFunctionSamples != null) {
            result = fromTransferFunctionSamples.length;
        }
        if (toTransferFunctionSamples != null) {
            int tmp = toTransferFunctionSamples.length;
            if (result > 0 && tmp != result) {
                throw new IllegalStateException("From Transfer function and To Transfer function are not the same length!");
            } else {
                result = tmp;
            }

        }
        return result;
    }

    private void convolveWithTransferFunction(InverseTransferFunction ctf, int nfreq, double[] sre, double[] sim, int nfft) {
        /*
        * - Multiply transformed data by composite transfer operator.
         */
        double[] xxre = ctf.getXre();
        double[] xxim = ctf.getXim();
        for (int i = 0; i < nfreq; ++i) {
            double tempR = xxre[i] * sre[i] - xxim[i] * sim[i];
            double tempI = xxre[i] * sim[i] + xxim[i] * sre[i];
            sre[i] = tempR;
            sim[i] = tempI;

            /*
            * Input data are real so F(N-j) = F(j)
             */
            if (i > 0 && i < nfreq - 1) {
                int j = nfft - i;
                sre[j] = tempR;
                sim[j] = -tempI;
            }
        }
    }

    public static Complex[] convolveWithTransferFunction(InverseTransferFunction ctf, int nfreq, Complex[] transformedSeis, int nfft) {
        Complex[] result = new Complex[transformedSeis.length];
        Complex[] transfer = ctf.getValues();
        for (int i = 0; i < nfreq; ++i) {
            result[i] = transformedSeis[i].multiply(transfer[i]);

            /*
            * Input data are real so F(N-j) = F(j)
             */
            if (i > 0 && i < nfreq - 1) {
                int j = nfft - i;
                result[j] = result[i].conjugate();
            }
        }
        return result;
    }

}
