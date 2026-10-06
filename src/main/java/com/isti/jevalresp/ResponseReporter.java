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
package com.isti.jevalresp;

import java.util.ArrayList;
import java.util.Date;

import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Response;

/**
 *
 * @author dodge1
 */
public class ResponseReporter implements RespCallback {

    private final RespProcessor respProcObj;
    private final double[] freqArr;
    private final int outUnitsConvIdx;
    private final int startStageNum;
    private final int stopStageNum;
    private final boolean useDelayFlag;
    private final boolean listInterpOutFlag;
    private final boolean listInterpInFlag;
    private final double listInterpTension;
    private final boolean unwrapPhaseFlag;
    private final boolean totalSensitFlag;
    private final double b62XValue;
    private final RunDirect owner;
    private final ArrayList<RespInfoBlk> respBlkVec;
    private Response respObj;
    private ResponseUnits inputUnits;

    public ResponseReporter(RunDirect owner, RespProcessor respProcObj, int outUnitsConvIdx, double[] freqArr, int startStageNum, int stopStageNum, boolean useDelayFlag, boolean listInterpOutFlag,
            boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag, double b62XValue, ArrayList<RespInfoBlk> respBlkVec) {
        this.owner = owner;
        this.respProcObj = respProcObj;
        this.freqArr = freqArr;
        this.outUnitsConvIdx = outUnitsConvIdx;
        this.startStageNum = startStageNum;
        this.stopStageNum = stopStageNum;
        this.useDelayFlag = useDelayFlag;
        this.listInterpOutFlag = listInterpOutFlag;
        this.listInterpInFlag = listInterpInFlag;
        this.listInterpTension = listInterpTension;
        this.unwrapPhaseFlag = unwrapPhaseFlag;
        this.totalSensitFlag = totalSensitFlag;
        this.b62XValue = b62XValue;
        this.respBlkVec = respBlkVec;
    }

    public Response getRespObj() {
        return respObj;
    }

    public ResponseUnits getInputUnits() {
        return inputUnits;
    }

    @Override
    public void setRespProcObj(RespProcessor respProcObj) {
    }
    //for each response found; process it:

    @Override
    public boolean responseInfo(String fileName, ChannelId channelIdObj, Date respEndDateObj, String channelIdFName, Response respObj, ResponseUnits units, String errMsgStr) {
        if (respObj != null) { //response object contains data
            this.respObj = respObj;
            //process response information:
            final OutputGenerator outGenObj = respProcObj.processResponse(
                    fileName,
                        respObj,
                        freqArr,
                        true,
                        outUnitsConvIdx,
                        startStageNum,
                        stopStageNum,
                        useDelayFlag,
                        false,
                        listInterpOutFlag,
                        listInterpInFlag,
                        listInterpTension,
                        unwrapPhaseFlag,
                        totalSensitFlag,
                        b62XValue);
            if ((outGenObj) == null) { //error processing response; enter error message
                owner.setErrorMessage(respProcObj.getErrorMessage());
                if (respProcObj.getNumRespFound() > 1) //if >1 response
                {
                    respProcObj.clearErrorMessage(); // then clear err
                }
                return false; //indicate not processed OK
            }
            outGenObj.setInputUnits(units);
            this.inputUnits = units;
            //reponse output calculated OK
            if (channelIdObj != null) { //channel ID OK; create new response blk & add to Vector:
                respBlkVec.add(
                        new RespInfoBlk(channelIdObj.station_code,
                                        channelIdObj.channel_code,
                                        ((channelIdObj.network_id != null) ? channelIdObj.network_id.network_code : ""),
                                        channelIdObj.site_code,
                                        outGenObj.getCSpectraArray(),
                                        outGenObj.getCalcFreqArray(),
                                        fileName));
            }
            return true; //indicate processed OK
        }
        //no data in response object
        if (errMsgStr != null) //if not null then
        {
            owner.setErrorMessage(errMsgStr); //enter error message
        }
        return false; //indicate not processed OK
    }

    @Override
    public void showInfoMessage(String msgStr) {
    }
}
