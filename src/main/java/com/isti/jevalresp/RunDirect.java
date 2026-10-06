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
//RunDirect.java:  Extension of 'Run.java' that supports input and output
//                 via method calls for the processing of a single response.
//
// 11/10/2009 -- [ET]  Initial version.
//  5/19/2010 -- [ET]  Added optional parameters 'unwrapPhaseFlag' and
//                     'totalSensitFlag' to 'processOneResponse()' method.
// 10/23/2013 -- [ET]  Added optional parameter 'b62XValue' to method
//                     'processOneResponse()'.
//
package com.isti.jevalresp;

import java.util.ArrayList;
import java.util.Date;

/**
 * Class RunDirect is an extension of 'Run' that supports input and output via
 * method calls for the processing of a single response.
 */
public class RunDirect extends Run {

    /**
     * Unit conversion type index value for "default" (0).
     */
    public static final int DEFAULT_UNIT_CONV
            = OutputGenerator.DEFAULT_UNIT_CONV;
    /**
     * Unit conversion type index value for "displacement" (1).
     */
    public static final int DISPLACE_UNIT_CONV
            = OutputGenerator.DISPLACE_UNIT_CONV;
    /**
     * Unit conversion type index value for "velocity" (2).
     */
    public static final int VELOCITY_UNIT_CONV
            = OutputGenerator.VELOCITY_UNIT_CONV;
    /**
     * Unit conversion type index value for "acceleration" (3).
     */
    public static final int ACCEL_UNIT_CONV = OutputGenerator.ACCEL_UNIT_CONV;

    /**
     * Finds and processes one response, returning the output in an
     * 'OutputGenerator' object.
     *
     * @param stationStr station name to search for, or a null or empty string
     * to accept all station names.
     * @param channelStr channel name to search for, or a null or empty string
     * to accept all channel names.
     * @param networkStr network name to search for, or a null or empty string
     * to accept all network names.
     * @param siteStr site name to search for, or a null or empty string to
     * accept all site names.
     * @param dateObj date to search for, or null to accept all dates.
     * @param outUnitsConvIdx output units conversion index for the requested
     * output units type; one of the '..._UNIT_CONV' values.
     * @param fileNameParam a specific filename (or directory) to use, or a null
     * or empty string for all matching files.
     * @param freqArr an array of frequency values to use.
     * @param startStageNum if greater than zero then the start of the range of
     * stage sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum if greater than zero then the end of the range of
     * stage sequence numbers to use, otherwise only the single stage specified
     * by 'startStageNum' is used.
     * @param useDelayFlag true to use estimated delay in phase calculation.
     * @param listInterpOutFlag true to interpolate amp/phase output from
     * responses containing List blockettes.
     * @param listInterpInFlag true to interpolate amp/phase input from List
     * blockettes in responses (before output is calculated).
     * @param listInterpTension tension value for List-blockette interpolation
     * algorithm.
     * @param unwrapPhaseFlag true to unwrap phase output values.
     * @param totalSensitFlag true to use stage 0 (total) sensitivity; false to
     * use computed sensitivity.
     * @param b62XValue sample value for polynomial blockette (62).
      */
    public OutputGenerator processOneResponse(String stationStr, 
            String channelStr, 
            String networkStr, 
            String siteStr, 
            Date dateObj, 
            final int outUnitsConvIdx, 
            String fileNameParam, 
            final double[] freqArr, 
            final int startStageNum, 
            final int stopStageNum, 
            final boolean useDelayFlag, 
            final boolean listInterpOutFlag, 
            final boolean listInterpInFlag, 
            final double listInterpTension, 
            final boolean unwrapPhaseFlag, 
            final boolean totalSensitFlag, 
            final double b62XValue, 
            ChannelMatchPolicy matchPolicy) {
            final ArrayList<RespInfoBlk> respBlkVec = new ArrayList<>();      //RespInfoBlk objs
        //create response processor object:
        final RespProcessor respProcObj = new RespProcessor(false, false,
                outputDirectory);

        ResponseReporter reporter = new ResponseReporter(this,
                respProcObj,
                outUnitsConvIdx,
                freqArr,
                startStageNum,
                stopStageNum,
                useDelayFlag,
                listInterpOutFlag,
                listInterpInFlag,
                listInterpTension,
                unwrapPhaseFlag,
                totalSensitFlag,
                b62XValue,
                respBlkVec);
        //find responses (each one is reported via 'RespCallback'):
        boolean foundResponse = respProcObj.findResponses(stationStr, channelStr, networkStr,
                siteStr, dateObj, null, fileNameParam, reporter,matchPolicy);
        if (!foundResponse) {    //error finding responses; set error code and message
            setErrorMessage(respProcObj.getErrorMessage());
            return null;
        }
        if (!respProcObj.getErrorFlag()) {    //no errors flagged
            if (respBlkVec.isEmpty()) {  //no 'RespInfoBlk' objects found in vector
                setErrorMessage("No matching responses found");
                return null;
            }
            RespInfoBlk info = respBlkVec.get(0);
            //return output generator via first 'RespInfoBlk' in vector:
            return new OutputGenerator( reporter.getRespObj(),  info.cSpectraArray, info.freqArr, true,reporter.getInputUnits());
        }
        //errors were flagged
        exitStatusValue = 16;         //set non-zero exit status code
        return null;
    }



    /**
     * Finds and processes one response, returning the output in an
     * 'OutputGenerator' object.
     *
     * @param stationStr station name to search for, or a null or empty string
     * to accept all station names.
     * @param channelStr channel name to search for, or a null or empty string
     * to accept all channel names.
     * @param networkStr network name to search for, or a null or empty string
     * to accept all network names.
     * @param siteStr site name to search for, or a null or empty string to
     * accept all site names.
     * @param dateObj date to search for, or null to accept all dates.
     * @param outUnitsConvIdx output units conversion index for the requested
     * output units type; one of the '..._UNIT_CONV' values.
     * @param fileNameParam a specific filename (or directory) to use, or a null
     * or empty string for all matching files.
     * @param freqArr an array of frequency values to use.
     * @param startStageNum if greater than zero then the start of the range of
     * stage sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum if greater than zero then the end of the range of
     * stage sequence numbers to use, otherwise only the single stage specified
     * by 'startStageNum' is used.
     * @param useDelayFlag true to use estimated delay in phase calculation.
     * @param listInterpOutFlag true to interpolate amp/phase output from
     * responses containing List blockettes.
     * @param listInterpInFlag true to interpolate amp/phase input from List
     * blockettes in responses (before output is calculated).
     * @param listInterpTension tension value for List-blockette interpolation
     * algorithm.
     * @param policy
     * @param stdioFlag true for input from 'stdin', false for input from file.
     * @return An 'OutputGenerator' object, or null if an error occurred (in
     * which case 'getErrorMessage()' may be used to fetch information about the
     * error).
     */
    public OutputGenerator processOneResponse(String stationStr,
            String channelStr, String networkStr, String siteStr,
            Date dateObj, final int outUnitsConvIdx,
            String fileNameParam, final double[] freqArr,
            final int startStageNum, final int stopStageNum,
            final boolean useDelayFlag,
            final boolean listInterpOutFlag, final boolean listInterpInFlag,
            final double listInterpTension, final ChannelMatchPolicy policy ) {
        return processOneResponse(stationStr, channelStr, networkStr, siteStr,
                dateObj, outUnitsConvIdx, fileNameParam, freqArr, startStageNum,
                stopStageNum, useDelayFlag, listInterpOutFlag, listInterpInFlag,
                listInterpTension, false, false, 1.0, policy);
    }

    /**
     * Finds and processes one response, returning the output in an
     * 'OutputGenerator' object.
     *
     * @param stationStr station name to search for, or a null or empty string
     * to accept all station names.
     * @param channelStr channel name to search for, or a null or empty string
     * to accept all channel names.
     * @param networkStr network name to search for, or a null or empty string
     * to accept all network names.
     * @param siteStr site name to search for, or a null or empty string to
     * accept all site names.
     * @param dateObj date to search for, or null to accept all dates.
     * @param outUnitsConvIdx output units conversion index for the requested
     * output units type; one of the '..._UNIT_CONV' values.
     * @param fileNameParam a specific filename (or directory) to use, or a null
     * or empty string for all matching files.
     * @param freqArr an array of frequency values to use.
     * @param startStageNum if greater than zero then the start of the range of
     * stage sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum if greater than zero then the end of the range of
     * stage sequence numbers to use, otherwise only the single stage specified
     * by 'startStageNum' is used.
     * @param policy
     * @param stdioFlag true for input from 'stdin', false for input from file.
     * @return An 'OutputGenerator' object, or null if an error occurred (in
     * which case 'getErrorMessage()' may be used to fetch information about the
     * error).
     */
    public OutputGenerator processOneResponse(String stationStr, 
            String channelStr, 
            String networkStr, 
            String siteStr, 
            Date dateObj, 
            int outUnitsConvIdx, 
            String fileNameParam, 
            double[] freqArr, 
            int startStageNum, 
            int stopStageNum,
            ChannelMatchPolicy policy) {
        return processOneResponse(stationStr, channelStr, networkStr, siteStr,
                dateObj, outUnitsConvIdx, fileNameParam, freqArr, startStageNum,
                stopStageNum, false, true, true, 0.0, false, false, 1.0,policy);
    }




}
