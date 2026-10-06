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

import java.util.prefs.Preferences;

/**
 * Created by dodge1 Date: Mar 12, 2010 COPYRIGHT NOTICE Copyright (C) 2007
 * Lawrence Livermore National Laboratory.
 */
public class TransferParams {

    private final Preferences prefs;

    private static class TransferParamsHolder {

        private static final TransferParams instance = new TransferParams();
    }

    public static TransferParams getInstance() {
        return TransferParamsHolder.instance;
    }

    private TransferParams() {
        prefs = Preferences.userNodeForPackage(this.getClass());
    }

    /**
     * Gets a FreqLimits object to be used when removing the instrument response
     * from a segment of waveform data in the current project.
     *
     * @param nyquist The Nyquist frequency of the data for which the result
     * FreqLimits object will be used.
     * @param windowLength The length of the data window to be transferred.
     * @return A FreqLimits object containing the appropriate frequency limits
     * for the deconvolution.
     */
    public FreqLimits getFreqLimits(double nyquist, double windowLength) {
        double highpass = MiscParams.getInstance().getHighpassFrac() * nyquist;
        double highcut = MiscParams.getInstance().getHighCutFrac() * nyquist;

        double minfreq = prefs.getDouble("MIN_LOWPASS_FREQ", 0.001);
        double tfactor = MiscParams.getInstance().getTfactor();
        double lowpass = Math.max(tfactor / windowLength, minfreq);
        double lowcut = MiscParams.getInstance().getLowCutFrac() * lowpass;
        return new FreqLimits(lowcut, lowpass, highpass, highcut);
    }

    public FreqLimits produceFromTransferFunction(double[] freqs, double[] amplitude) {
        double nyquist = freqs[freqs.length - 1];
        Double max = amplitude[0];
        int index = 0;
        for (int j = 0; j < freqs.length; ++j) {
            double v = amplitude[j];
            if (v > max) {
                max = v;
                index = j;
            }
        }

        double stopValue = max / 1000;
        double passValue = stopValue * 10;

        int lowStopIndex = -1;
        int lowPassIndex = -1;
        for (int j = index; j > 0; --j) {
            double value = amplitude[j];
            if (value <= passValue && lowPassIndex < 0) {
                lowPassIndex = j;
            }
             if (value <= stopValue && lowStopIndex < 0) {
                lowStopIndex = j;
            }
        }
        int highStopIndex = -1;
        int highPassIndex = -1;
        for (int j = index; j < amplitude.length; ++j) {
            double value = amplitude[j];
            if (value <= passValue && highPassIndex < 0) {
                highPassIndex = j;
            }
            if (value <= stopValue && highStopIndex < 0) {
                highStopIndex = j;
            }
        }
        
        
        
        double lowpass = lowPassIndex >= 0 ? freqs[lowPassIndex] : 0.0001;
        double lowcut =  lowStopIndex >= 0 ? freqs[lowStopIndex] :  MiscParams.getInstance().getLowCutFrac() * lowpass;
        double highPass = highPassIndex > 0 ? freqs[highPassIndex] : nyquist * .95;
        double highStop = highStopIndex > 0 ? freqs[highStopIndex] : nyquist * .99;

        highPass = highPass > nyquist * .95 ? nyquist * .95 : highPass;
        highStop = highStop > nyquist * .99 ? nyquist * .99 : highStop;
        return new FreqLimits(lowcut, lowpass, highPass, highStop);
    }
}
