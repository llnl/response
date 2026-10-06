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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.apache.commons.math3.complex.Complex;

import com.isti.jevalresp.ResponseUnits;

/**
 *
 * @author dodge1
 */
public class SACPZFTransfer {

    public TransferData getFromTransferFunction(int nsamp, double samprate, double time, ResponseMetaData metadata) throws IOException {

        /*
         * - Set up scratch space for TRANSFER subroutine. It needs four DOUBLE PRECISION arrays,two for the ffts that
         * must be sized to the next power of two above the maximum number of dat(nfft) and two that are half that size
         * plus 1(nfreq.)
         */
        int nfft = TransferFunctionUtils.next2(nsamp);
        int nfreq = nfft / 2 + 1;

        double[] xre = new double[nfreq];
        double[] xim = new double[nfreq];

        double delta = 1.0 / samprate;
        double delfrq = 1.0 / (nfft * delta);
        ResponseType fromType = metadata.getRsptype();

        computeTransferFunctionFromFile(nfreq, delfrq, time, xre, xim, fromType, metadata.getFilename());

        return TransferFunctionUtils.packageTransferFunctionSamples(delfrq, xre, xim, new ResponseUnits(), metadata);
    }

    public static void computeTransferFunctionFromFile(int nfreq, double delfrq, double epoch, double[] xre, double[] xim, ResponseType type, String responseFileName)
            throws IOException {
        /*
         * Branching routine to apply the individual instrument responses.
         */
        for (int idx = 0; idx < nfreq; idx++) {
            xre[idx] = 1.0e0;
            xim[idx] = 0.0e0;
        }
        switch (type) {
        case SACPZF:
            polezero(delfrq, xre, xim, responseFileName);
            break;
        default:
            throw new IllegalStateException("Unhandled type: " + type);
        }

    }

    public static void polezero(double delfrq, double xre[], double xim[], String poleZeroFile) throws IOException {
        PoleZeroData pzd = getPoleZeroDataFromFile(poleZeroFile);
        TransferFunctionUtils.getran(delfrq, pzd, xre, xim);
    }

    public static PoleZeroData getPoleZeroDataFromFile(String poleZeroFile) throws NumberFormatException, FileNotFoundException {
        double constant;
        /*
        * - Set default values for constant, poles, and zeros.
         */
        constant = 1.0;
        String[] lines = readAllLines(poleZeroFile);
        constant = getConstant(lines, constant);
        // Get zeros
        ArrayList<Complex> zeros = getZeros(lines);
        // Get poles
        ArrayList<Complex> poles = getPoles(lines);
        PoleZeroData pzd = new PoleZeroData(constant, poles, zeros);
        return pzd;
    }

    private static ArrayList<Complex> getZeros(String[] lines) {
        ArrayList<Complex> zeros = new ArrayList<>();
        int nzeros = 0;
        boolean inZerosSection = false;
        for (String line : lines) {
            line = line.toUpperCase();
            StringTokenizer tokenizer = new StringTokenizer(line);
            if (tokenizer.countTokens() == 2) {
                String first = tokenizer.nextToken();
                if (first.equalsIgnoreCase("ZEROS")) {
                    inZerosSection = true;
                    nzeros = Integer.parseInt(tokenizer.nextToken());
                    continue;
                }
                if (!inZerosSection) {
                    continue;
                }
                if (inZerosSection && (first.contains("POLES") || first.contains("CONSTANT") || first.startsWith("*"))) {
                    break;
                }

                double real = Double.parseDouble(first);
                double imaginary = Double.parseDouble(tokenizer.nextToken());
                zeros.add(new Complex(real, imaginary));
            }
        }
        while (zeros.size() < nzeros) {
            zeros.add(new Complex(0.0, 0.0));
        }
        return zeros;
    }

    private static ArrayList<Complex> getPoles(String[] lines) {
        ArrayList<Complex> poles = new ArrayList<>();
        int npoles = 0;

        boolean inPolesSection = false;
        for (String line : lines) {
            line = line.toUpperCase();
            StringTokenizer tokenizer = new StringTokenizer(line);
            if (tokenizer.countTokens() == 2) {
                String first = tokenizer.nextToken();
                if (first.equalsIgnoreCase("POLES")) {
                    npoles = Integer.parseInt(tokenizer.nextToken());
                    inPolesSection = true;
                    continue;
                }
                if (!inPolesSection) {
                    continue;
                }
                if (inPolesSection && (first.contains("ZEROS") || first.contains("CONSTANT") || first.startsWith("*"))) {
                    break;
                }

                double real = Double.parseDouble(first);
                double imaginary = Double.parseDouble(tokenizer.nextToken());
                poles.add(new Complex(real, imaginary));

            }
        }
        while (poles.size() < npoles) {
            poles.add(new Complex(0.0, 0.0));
        }
        return poles;
    }

    private static double getConstant(String[] lines, double constant) throws NumberFormatException {
        // Get constant
        for (String line : lines) {
            StringTokenizer tokenizer = new StringTokenizer(line);
            if (tokenizer.hasMoreTokens() && tokenizer.nextToken().equalsIgnoreCase("CONSTANT")) {
                constant = Double.parseDouble(tokenizer.nextToken());
            }
        }
        return constant;
    }

    private static String[] readAllLines(String poleZeroFile) throws FileNotFoundException {
        ArrayList<String> tmp = new ArrayList<>();
        File file = new File(poleZeroFile);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                tmp.add(sc.nextLine());
            }
        }
        return tmp.toArray(new String[1]);
    }

}
