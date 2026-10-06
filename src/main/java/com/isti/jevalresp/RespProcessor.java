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
//RespProcessor.java:  High-level processing functions for 'JEvalResp'.
//
//  12/12/2001 -- [ET]  Initial release version.
//   2/28/2002 -- [ET]  Slight modifications for 'RespNetProc'
//                      compatibility.
//    5/2/2002 -- [ET]  Added version of 'outputFiles()' that accepts
//                      'PrintStream' parameter.
//   5/15/2002 -- [ET]  Added 'getOutputFileNamesStr()' method and support.
//   5/28/2002 -- [ET]  Added 'getNumberErrors()' method and support.
//    6/6/2002 -- [ET]  Added support for detecting and forwarding 'info'
//                      messages from the 'RespFileParser' object; added
//                      support for multiple file/pathnames and wildcards
//                      in the findResponses 'fileNameParam' parameter.
//   7/10/2002 -- [ET]  Changed 'apOutputFlag' to 'respTypeIndex' and
//                      implemented single amp/phase file option; renamed
//                      'outputFiles()' to 'outputData()'; added parameter
//                      'multiOutputFlag' and support to allow multiple
//                      outputs with same "net.sta.loc.cha" code.
//   7/11/2002 -- [ET]  Changed find-response methods to have 'beginDateObj'
//                      and 'endDateObj' parameters.
//    8/6/2002 -- [ET]  Changed so that the response's end-date is reported
//                      via the 'responseInfo()' call; added
//                      'logSpacingFlag' param to 'findAndOutputResponses()'
//                      method and implemented passing it on to the
//                      'OutputGenerator.calculateResponse()' method; added
//                      'headerFlag' parameter to constructor and implemented
//                      using it to enable header information in output
//                      files.
//   3/26/2003 -- [KF]  Added 'outputDirectory' parameter.
//    5/6/2003 -- [ET]  Modified javadoc (modified 'outputDirectory' param
//                      description).
//   2/25/2005 -- [ET]  Modified 'findResponses()' method to suppress
//                      "No matching response files found" error message
//                      when all matching response files contain errors.
//   3/10/2005 -- [ET]  Added optional 'useDelayFlag' parameter to methods
//                      'processResponse()' and 'findAndOutputResponses()';
//                      added support in 'findResponses()' method for URLs
//                      as input file names.
//    4/1/2005 -- [ET]  Added optional 'showInputFlag' parameter to methods
//                      'processResponse()' and 'findAndOutputResponses()'.
//  10/25/2005 -- [ET]  Added optional List-blockette interpolation
//                      parameters to methods 'processResponse()' and
//                      'findAndOutputResponses()'.
//   5/24/2010 -- [ET]  Added optional parameters 'unwrapPhaseFlag' and
//                      'totalSensitFlag' to 'processResponse()' and
//                      'findAndOutputResponses()' methods; modified to
//                      handle response-output type "fap".
//   1/27/2012 -- [ET]  Added 'doReadResponses()' method and modified
//                      'findResponses()' method to use it.
//   3/29/2012 -- [ET]  Added 'idNameAppendStr' parameter to method
//                      'doReadResponses()'.
//  10/22/2013 -- [ET]  Added optional 'b62XValue' parameter to methods
//                      'processResponse()' and 'findAndOutputResponses()'.
//
package com.isti.jevalresp;

import java.io.File;
import java.util.Date;

import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Response;

/**
 * Class RespProcessor contains high-level processing functions for 'JEvalResp'.
 */
public class RespProcessor {
    //prefix used when searching for RESP files:

    public static final String RESP_FILE_PREFIX = "RESP.";
    public static final String LINE_SEP_STR = //"line" separator string
            "--------------------------------------------------";
    //flag set true to allow multiple outputs with same net.sta.loc.cha:
    protected final boolean multiOutputFlag;
    protected final boolean headerFlag; //true for header info in output
    protected int numRespFound = 0; //# of responses found
    protected String errorMessage = null; //error message from parsing
    protected int numberErrors = 0; //# of errors that occurred
    //holds names of output files generated:
    protected String outputFileNamesStr = "";
    //number of names in 'outputFileNamesStr':
    protected int outputFileNamesCount = 0;
    protected final File outputDirectory;

    /**
     * Creates a response-processor object.
     *
     * @param multiOutputFlag
     *            true to allow multiple response outputs with the same
     *            "net.sta.loc.cha" code.
     * @param headerFlag
     *            true to enable header information in the output file; false
     *            for no header information.
     * @param outputDirectory
     *            output directory, or null for current directory.
     */
    public RespProcessor(boolean multiOutputFlag, boolean headerFlag, File outputDirectory) {
        this.multiOutputFlag = multiOutputFlag;
        this.headerFlag = headerFlag;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Processes the given response object, calculating the complex spectra
     * output values.
     *
     * @param inFName
     *            the file name associated with the response object.
     * @param respObj
     *            the response object to be processed.
     * @param freqArr
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConvIdx
     *            output units conversion index for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useDelayFlag
     *            true to use estimated delay in phase calculation.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     * @param listInterpOutFlag
     *            true to interpolate amp/phase output from responses containing
     *            List blockettes.
     * @param listInterpInFlag
     *            true to interpolate amp/phase input from List blockettes in
     *            responses (before output is calculated).
     * @param listInterpTension
     *            tension value for List-blockette interpolation algorithm.
     * @param unwrapPhaseFlag
     *            true to unwrap phase output values.
     * @param totalSensitFlag
     *            true to use stage 0 (total) sensitivity; false to use computed
     *            sensitivity.
     * @param b62XValue
     *            sample value for polynomial blockette (62).
     * @return An 'OutputGenerator' object loaded with complex spectra response
     *         output data; or null if error (in which case 'getErorMessage()'
     *         may be used to see information about the error).
     */
    public OutputGenerator processResponse(String inFName, Response respObj, double[] freqArr, boolean logSpacingFlag, int outUnitsConvIdx, int startStageNum, int stopStageNum, boolean useDelayFlag,
            boolean showInputFlag, boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag, double b62XValue) {
        //create output generator:
        final OutputGenerator outGenObj = new OutputGenerator(respObj);
        //check validity of response:
        if (!outGenObj.checkResponse( //if 'def', don't check units
                outUnitsConvIdx == OutputGenerator.DEFAULT_UNIT_CONV)) { //error in response; set error code & msg
            setErrorMessage("Error in response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
            return null;
        }
        //response checked OK; do normalization:
        if (!outGenObj.normalizeResponse(startStageNum, stopStageNum)) { //normalization error; set error message
            setErrorMessage("Error normalizing response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
            return null;
        }
        //response normalized OK; calculate output:
        if (!outGenObj.calculateResponse(
                freqArr,
                    logSpacingFlag,
                    outUnitsConvIdx,
                    startStageNum,
                    stopStageNum,
                    useDelayFlag,
                    showInputFlag,
                    listInterpOutFlag,
                    listInterpInFlag,
                    listInterpTension,
                    unwrapPhaseFlag,
                    totalSensitFlag,
                    b62XValue)) { //calculation error; set error message
            setErrorMessage("Error calculating response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
            return null;
        }
        return outGenObj;
    }

    /**
     * Processes the given response object, calculating the complex spectra
     * output values.
     *
     * @param inFName
     *            the file name associated with the response object.
     * @param respObj
     *            the response object to be processed.
     * @param freqArr
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConvIdx
     *            output units conversion index for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useDelayFlag
     *            true to use estimated delay in phase calculation.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     * @param listInterpOutFlag
     *            true to interpolate amp/phase output from responses containing
     *            List blockettes.
     * @param listInterpInFlag
     *            true to interpolate amp/phase input from List blockettes in
     *            responses (before output is calculated).
     * @param listInterpTension
     *            tension value for List-blockette interpolation algorithm.
     * @param unwrapPhaseFlag
     *            true to unwrap phase output values.
     * @param totalSensitFlag
     *            true to use stage 0 (total) sensitivity; false to use computed
     *            sensitivity.
     * @return An 'OutputGenerator' object loaded with complex spectra response
     *         output data; or null if error (in which case 'getErorMessage()'
     *         may be used to see information about the error).
     */
    public OutputGenerator processResponse(String inFName, Response respObj, double[] freqArr, boolean logSpacingFlag, int outUnitsConvIdx, int startStageNum, int stopStageNum, boolean useDelayFlag,
            boolean showInputFlag, boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag) {
        return processResponse(
                inFName,
                    respObj,
                    freqArr,
                    logSpacingFlag,
                    outUnitsConvIdx,
                    startStageNum,
                    stopStageNum,
                    useDelayFlag,
                    showInputFlag,
                    listInterpOutFlag,
                    listInterpInFlag,
                    listInterpTension,
                    unwrapPhaseFlag,
                    totalSensitFlag,
                    0.0);
    }

    /**
     * Processes the given response object, calculating the complex spectra
     * output values.
     *
     * @param inFName
     *            the file name associated with the response object.
     * @param respObj
     *            the response object to be processed.
     * @param freqArr
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConvIdx
     *            output units conversion index for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useDelayFlag
     *            true to use estimated delay in phase calculation.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     * @param listInterpOutFlag
     *            true to interpolate amp/phase output from responses containing
     *            List blockettes.
     * @param listInterpInFlag
     *            true to interpolate amp/phase input from List blockettes in
     *            responses (before output is calculated).
     * @param listInterpTension
     *            tension value for List-blockette interpolation algorithm.
     * @return An 'OutputGenerator' object loaded with complex spectra response
     *         output data; or null if error (in which case 'getErorMessage()'
     *         may be used to see information about the error).
     */
    public OutputGenerator processResponse(String inFName, Response respObj, double[] freqArr, boolean logSpacingFlag, int outUnitsConvIdx, int startStageNum, int stopStageNum, boolean useDelayFlag,
            boolean showInputFlag, boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension) {
        return processResponse(
                inFName,
                    respObj,
                    freqArr,
                    logSpacingFlag,
                    outUnitsConvIdx,
                    startStageNum,
                    stopStageNum,
                    useDelayFlag,
                    showInputFlag,
                    listInterpOutFlag,
                    listInterpInFlag,
                    listInterpTension,
                    false,
                    false,
                    0.0);
    }

    /**
     * Processes the given response object, calculating the complex spectra
     * output values.
     *
     * @param inFName
     *            the file name associated with the response object.
     * @param respObj
     *            the response object to be processed.
     * @param freqArr
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConvIdx
     *            output units conversion index for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @return An 'OutputGenerator' object loaded with complex spectra response
     *         output data; or null if error (in which case 'getErorMessage()'
     *         may be used to see information about the error).
     */
    public OutputGenerator processResponse(String inFName, Response respObj, double[] freqArr, boolean logSpacingFlag, int outUnitsConvIdx, int startStageNum, int stopStageNum) {
        return processResponse(inFName, respObj, freqArr, logSpacingFlag, outUnitsConvIdx, startStageNum, stopStageNum, false, false, false, false, 0.0, false, false, 0.0);
    }

    /**
     * @return number of responses found by last call to 'findResponses()'.
     */
    public int getNumRespFound() {
        return numRespFound;
    }

    /**
     * @return a String containing the names of the most recently generated
     *         output files (by the last call to 'outputData()'), or an empty
     *         string if none have been generated.
     */
    public String getOutputFileNamesStr() {
        return outputFileNamesStr;
    }

    /**
     * @return the number of names returned by 'getOutputFileNamesStr()' (will
     *         be 0, 1 or 2).
     */
    public int getOutputFileNamesCount() {
        return outputFileNamesCount;
    }

    /**
     * Enters error message (if none previously entered).
     *
     * @param str
     *            error message string
     */
    protected void setErrorMessage(String str) {
        if (errorMessage == null) //if no previous error then
        {
            errorMessage = str; //set error message
        }
        ++numberErrors; //increment error count
    }

    /**
     * @return true if an error was detected. The error message may be fetched
     *         via the 'getErrorMessage()' method.
     */
    public boolean getErrorFlag() {
        return (errorMessage != null);
    }

    /**
     * @return message string for last error (or 'No error' if none).
     */
    public String getErrorMessage() {
        return (errorMessage != null) ? errorMessage : "No error";
    }

    /**
     * Clears the error message string.
     */
    public void clearErrorMessage() {
        errorMessage = null;
    }

    /**
     * @return the number of errors that have occurred. This total is not
     *         cleared by 'clearErrorMessage()'.
     */
    public int getNumberErrors() {
        return numberErrors;
    }

    /**
     * Finds responses with matching channel IDs. Each found channel ID and
     * response is reported via the "RespCallback.responseInfo()' method.
     *
     * @param staArr
     *            an array of station name patterns to search for, or a null or
     *            empty array to accept all station names.
     * @param chaArr
     *            an array of channel name patterns to search for, or a null or
     *            empty array to accept all channel names.
     * @param netArr
     *            an array of network name patterns to search for, or a null or
     *            empty array to accept all network names.
     * @param siteArr
     *            an array of site name patterns to search for, or a null or
     *            empty array to accept all site names.
     * @param beginDateObj
     *            the beginning of a date range to search for, or null for no
     *            begin date. If no end-date is given then this becomes a single
     *            date that must be within the date-range of matched responses.
     * @param endDateObj
     *            the end of a date range to search for, or null for no end
     *            date.
     * @param fileName
     *            a specific filename (or directory) to use, or a null or empty
     *            string for all matching files.
     * @param respCallbackObj
     *            a 'RespCallback' object whose 'responseInfo()' method will be
     *            called to report on each response found.
     * @param matchPolicy
     * @return true if successful; false if error (in which case
     *         'getErorMessage()' may be used to see information about the
     *         error).
     */
    public boolean findResponses(String staArr, String chaArr, String netArr, String siteArr, Date beginDateObj, Date endDateObj, String fileName, RespCallback respCallbackObj,
            ChannelMatchPolicy matchPolicy) {
        numRespFound = 0; //clear responses found count
        //create parser object for file:
        final RespFileParser parserObj = new RespFileParser(fileName);
        ChannelId channelIdObj = null; //handle for channel ID
        Date respEndDateObj = null; //end-date for channel
        Response respObj = null; //handle for response object
        if (!parserObj.getErrorFlag()) { //no errors detected so far
            ChannelIdHldr chanIdHldrObj = parserObj.findChannelIdX(staArr, chaArr, netArr, siteArr, beginDateObj, endDateObj, matchPolicy);
            if (chanIdHldrObj != null) { //matching channel ID found
                //set handle to channel-ID object for found channel:
                channelIdObj = chanIdHldrObj.channelIdObj;
                //set handle to end-date for found channel:
                respEndDateObj = chanIdHldrObj.respEndDateObj;
                //read and parse response data from file:
                respObj = parserObj.readResponse();
                if (respObj == null) { //'readResponse()' returned error; set error msg
                    setErrorMessage(String.format("Error parsing response from %s: %s", parserObj.getInputFileName(), parserObj.getErrorMessage()));
                }
            } else if (parserObj.getErrorFlag()) { //parser detected error; set message
                setErrorMessage(String.format("Error parsing channel ID from %s: %s", parserObj.getInputFileName(), parserObj.getErrorMessage()));
            } else { //no parser error; set message
                setErrorMessage(String.format("Unable to find matching channel ID in %s.", parserObj.getInputFileName()));
            }
        } else { //error creating parser object for file; set error message
            setErrorMessage(String.format("Input file %s error: %s", parserObj.getInputFileName(), parserObj.getErrorMessage()));
        }
        parserObj.close(); //close input file

        ResponseUnits units = parserObj.getInputUnits();
        if (units == null) {
            units = new ResponseUnits();
        }
        //send response information to callback (even if error),
        //generate filename from channel ID info (if allowing
        // multiple outputs with same net.sta.loc.cha then
        // include date code):
        respCallbackObj.responseInfo(parserObj.getInputFileName(), channelIdObj, respEndDateObj, RespUtils.channelIdToFName(channelIdObj, multiOutputFlag), respObj, units, null);
        if (respObj == null) //if error then
        {
            return false; //return flag
        }
        ++numRespFound; //increment responses found count
        if (parserObj.getInfoFlag()) { //info message is available; forward it along
            respCallbackObj.showInfoMessage(parserObj.getInfoMessage());
            parserObj.clearInfoMessage(); //clear info message
        }
        return true;
    }
}
