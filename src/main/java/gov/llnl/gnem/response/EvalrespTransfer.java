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

import com.isti.jevalresp.ChannelMatchPolicy;

/**
 *
 * @author dodge1
 */
public class EvalrespTransfer {
    
    public TransferData getFromTransferFunction(int nsamp, double samprate, double time, String network, String sta, String chan, String locid, ResponseMetaData metadata, ChannelMatchPolicy policy)
            throws IOException {

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
    
        return computeTransferFunctionFromFile(nfreq, delfrq, time, xre, xim, fromType, metadata, network, sta,  chan, locid,policy);
    }
    
    
    
    private TransferData computeTransferFunctionFromFile(int nfreq, double delfrq, double epoch, double[] xre, double[] xim, ResponseType type, ResponseMetaData metadata,
            String network, String inSta, String inChan, String locid, ChannelMatchPolicy policy) throws IOException {
        /*
         * Branching routine to apply the individual instrument responses.
         */
        for (int idx = 0; idx < nfreq; idx++) {
            xre[idx] = 1.0e0;
            xim[idx] = 0.0e0;
        }
        String sta = TransferFunctionUtils.removeRegexEscape(inSta);
        switch (type) {

            case EVRESP:{
                return EvalResp.getEvrespTransferFunction(nfreq, delfrq, epoch, xre, xim, network, sta, inChan, locid, metadata, policy);
            }

            default:
                throw new IllegalStateException("Unhandled type: " + type);
        }
    }
}
