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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;

import com.isti.jevalresp.ChannelMatchPolicy;
import com.isti.jevalresp.ResponseUnits;
import com.isti.jevalresp.UnitsStatus;

/**
 *
 * @author dodge1
 */
public class TransferFunctionProcessor {

    private final SACPZFTransfer sacTransfer;
    private final EvalrespTransfer evalrespTransfer;
    private final NDCTransfer ndcTransfer;

    public TransferFunctionProcessor() {
        this.sacTransfer = new SACPZFTransfer();
        this.evalrespTransfer = new EvalrespTransfer();
        this.ndcTransfer = new NDCTransfer();
    }

    public InverseTransferFunction getToTransferFunction(int nsamp, double samprate,
            double time, String network, String sta, String chan, String locid,
            ResponseMetaData rmd, FreqLimits limits, Unit<?> requestedUnits,
            Unit<?> forcedInputUnits) throws IOException {
        return javaGetToTransferFunction(nsamp, samprate, time, network, sta, chan, locid, rmd, limits, requestedUnits, forcedInputUnits);
    }

    public InverseTransferFunction getToTransferFunction(int nsamp, double samprate, double time,
            String network, String sta, String chan, String locid, ResponseMetaData rmd, FreqLimits limits) throws IOException {
        Unit<?> requestedUnits = ResponseUnits.DEFAULT;
        return javaGetToTransferFunction(nsamp, samprate, time, network, sta, chan, locid, rmd, limits, requestedUnits);
    }

    private InverseTransferFunction javaGetToTransferFunction(int nsamp,
            double samprate,
            double time,
            String network,
            String sta,
            String chan,
            String locid,
            ResponseMetaData rmd,
            FreqLimits limits,
            Unit<?> requestedUnits) throws IOException {
        return javaGetToTransferFunction(nsamp, samprate, time, network, sta, chan, locid, rmd, limits, requestedUnits, null);
    }

    private InverseTransferFunction javaGetToTransferFunction(int nsamp,
            double samprate,
            double time,
            String network,
            String sta,
            String chan,
            String locid,
            ResponseMetaData rmd,
            FreqLimits limits,
            Unit<?> requestedUnits,
            Unit<?> forcedInputUnits) throws IOException {
        ChannelMatchPolicy policy = ChannelMatchPolicyHolder.getInstance().getPolicy();
        ToResponseLookupKey key = new ToResponseLookupKey(TransferFunctionUtils.next2(nsamp), samprate, network, sta, chan, locid, rmd, policy, requestedUnits, forcedInputUnits);
        InverseTransferFunction cached = CachedResponseHolder.getInstance().retrieveInverseTransferFunction(key, time);
        if (cached != null) {
            return cached;
        } else {
            TransferData transferData = getFromTransferFunction(nsamp, samprate, time, network, sta, chan, locid, rmd, policy);
            if (forcedInputUnits != null && !forcedInputUnits.equals(transferData.getOriginalUnits().getInputUnits())) {
                ResponseUnits tmp = new ResponseUnits(forcedInputUnits, transferData.getOriginalUnits().getUnitObj(), UnitsStatus.FORCED_VALUE);
                transferData.setForcedUnits(tmp);
             }
             try {
                transferData.maybeTransformData( requestedUnits );
            } catch (ResponseUnitsException ex) {
                Logger.getLogger(TransferFunctionProcessor.class.getName()).log(Level.FINE, ex.toString());
            }
            ResponseType fromType = rmd.getRsptype();
            Unit<?> workingUnits = transferData.getWorkingUnits().getInputUnits();

            switch (fromType) {
                case SACPZF: {
                    InverseTransferFunction result = TransferFunctionUtils.buildInverseTransferFunction(nsamp, samprate, rmd, limits, transferData, workingUnits);

                    CachedResponseHolder.getInstance().cacheInverseTransferFunction(key, result);
                    return result;
                }
                case EVRESP: {
                    InverseTransferFunction result = TransferFunctionUtils.buildInverseTransferFunction(nsamp, samprate, rmd, limits, transferData, workingUnits);
                    CachedResponseHolder.getInstance().cacheInverseTransferFunction(key, result);
                    return result;
                }
                case PAZ:
                case FAP:
                case PAZFIR:
                case PAZFAP:
                case FIRFAP: {
                    InverseTransferFunction result = TransferFunctionUtils.buildInverseTransferFunction(nsamp, samprate, rmd, limits, transferData, workingUnits);
                    CachedResponseHolder.getInstance().cacheInverseTransferFunction(key, result);
                    return result;
                }
                default:
                    throw new IllegalArgumentException(String.format("Response type %s not supported!", fromType.toString()));
            }
        }
    }

    public TransferData getFromTransferFunction(int nsamp, double samprate, Double time, String network, String sta, String chan, String locid, ResponseMetaData rmd, ChannelMatchPolicy policy) throws IOException {

        double roundedRate = TransferFunctionUtils.standardizeSampleRate(samprate);
        FromResponseLookupKey key = new FromResponseLookupKey(TransferFunctionUtils.next2(nsamp), roundedRate, network, sta, chan, locid, rmd, policy);
        TransferData cached = CachedResponseHolder.getInstance().retrieveForwardTransferFunction(key, time);
        if (cached != null) {
            return TransferData.copyOf(cached); // defensive copy in case the data are mutated by client.
        } else {
            ResponseType fromType = rmd.getRsptype();

            switch (fromType) {
                case SACPZF: {
                    TransferData result = sacTransfer.getFromTransferFunction(nsamp, roundedRate, time, rmd);
                     CachedResponseHolder.getInstance().cacheForwardTransferFunction(key, result);
                    return result;
                }
                case EVRESP: {
                    TransferData result = evalrespTransfer.getFromTransferFunction(nsamp, roundedRate, time, network, sta, chan, locid, rmd, policy);
                    CachedResponseHolder.getInstance().cacheForwardTransferFunction(key, result);
                    return result;
                }
                case CSS:
                case PAZ:
                case FAP:
                case PAZFIR:
                case PAZFAP:
                case FIRFAP: {
                    TransferData result = ndcTransfer.getFromTransferFunction(nsamp, roundedRate, time, rmd);
                    CachedResponseHolder.getInstance().cacheForwardTransferFunction(key, result);
                    return result;
                }
                default:
                    throw new IllegalArgumentException(String.format("Response type %s not supported!", fromType.toString()));
            }
        }
    }
}
