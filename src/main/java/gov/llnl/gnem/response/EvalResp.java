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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleEntry;

import com.isti.jevalresp.ChannelMatchPolicy;
import com.isti.jevalresp.ComplexBlk;
import com.isti.jevalresp.OutputGenerator;
import com.isti.jevalresp.ResponseUnits;
import com.isti.jevalresp.RunDirect;
import java.util.Arrays;

import java.util.Date;
import java.util.Objects;

/**
 *
 * @author addair1
 */
public class EvalResp {

    public static TransferData getEvrespTransferFunction(int nfreq, double delfrq, double epoch, double[] xre, double[] xim, String netCode, String inSta, String inChan, String locid,
            ResponseMetaData metadata, ChannelMatchPolicy policy) throws IOException {
        int i;

        Units units = Units.Default;
        double[] freq_lims = new double[2];
        double[] freqs;

        double val;
        freq_lims[0] = 0.0;
        freq_lims[1] = (nfreq - 1) * delfrq;

        /*
         * allocate space for the frequencies and fill with appropriate values
         */
        freqs = new double[nfreq];
        for (i = 0, val = freq_lims[0]; i < nfreq; i++) {
            freqs[i] = val;
            val += delfrq;
        }

        SimpleEntry<ComplexBlk[], ResponseUnits> pair = computeResponseSpectrum(netCode, inSta, inChan, locid, metadata.getFilename(), policy, epoch, units, freqs);

        fillArrays(pair.getKey(), xre, xim);
        return TransferFunctionUtils.packageTransferFunctionSamples(delfrq, xre, xim, pair.getValue(), metadata);
    }

    private static SimpleEntry<ComplexBlk[], ResponseUnits> computeResponseSpectrum(String netCode, String inSta, String inChan, String locid, String inFile, ChannelMatchPolicy policy, double epoch,
            Units units, double[] freqs) throws IllegalArgumentException, IllegalStateException, IOException {
        String station = inSta;
        String component = inChan;
        Identifier idInFile = Identifier.getIdentifier(inFile);
        if (idInFile != null) {
            netCode = idInFile.getNet();
            locid = idInFile.getLocid();
            if (!station.equals(idInFile.getSta())) {
                throw new IllegalArgumentException(String.format("Supplied station " + "(%s) does not match station in RESP file (%s)", station, idInFile.getSta()));
            }
            String chan = idInFile.getChan();
            if (!component.equals(chan)) {
                if (component.contains(chan) || (chan != null && !chan.isEmpty() && chan.contains(component))) {
                    component = chan;
                } else {
                    throw new IllegalArgumentException(String.format("Supplied chan " + "(%s) does not match chan in RESP file (%s)", component, chan));
                }
            }
        }
        RunDirect runDirectObj = new RunDirect();

        Instant instant = Instant.ofEpochSecond((long) epoch);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        Date date = Date.from(zdt.toInstant());

        //(new TimeT(epoch)).getDate();
        int startStage = -1;
        int stopStage = 0;

        OutputGenerator respOutput = runDirectObj.processOneResponse(station, component, netCode, locid, date, units.getEvrespCode(), inFile, freqs, startStage, stopStage, policy);
        if (respOutput == null) {
            throw new IllegalStateException(runDirectObj.getErrorMessage());
        }
        respOutput.calculateResponse(freqs, false, units.getEvrespCode(), startStage, stopStage);
        ComplexBlk[] cspectra = respOutput.getCSpectraArray();
        cspectra[0] = new ComplexBlk(0, 0); // Nolonger computing transfer function for the zero-frequency
        ResponseUnits inputUnits = respOutput.getInputUnits();
        return new SimpleEntry<>(cspectra, inputUnits);

    }

    private static void fillArrays(ComplexBlk[] cspectra, double[] xre, double[] xim) {
        Arrays.fill(xre, 0.0);
        Arrays.fill(xim, 0.0);
        for (int i = 0; i < cspectra.length; i++) {
            ComplexBlk blk = cspectra[i];
            if (!Objects.isNull(blk)) {
                xre[i] = blk.real;
                xim[i] = blk.imag;
            } else {
                xre[i] = 0.0;
                xim[i] = 0.0;
            }
        }
    }

}
