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
//RespFileParser.java:  Manages the parsing of an 'rdseed' ASCII response
//                      ("RESP") file.
//
//  12/11/2001 -- [ET]  Initial release version.
//    6/5/2002 -- [ET]  Added support for Generic Response Blockette (56);
//                      added 'infoMessage' methods and implementation.
//   7/11/2002 -- [ET]  Changed 'findChannelId()' to have 'beginDateObj'
//                      and 'endDateObj' parameters.
//   1/28/2004 -- [ET]  Modified 'readFIR()' to allow FIR blockettes (61)
//                      to have a "Response Name" (F04) field.
//   3/15/2004 -- [SH]  Added 'readNextBlockFieldNums(int,int)' method
//                      that supports the requesting of a specific field
//                      number (skipping over unneeded fields); modified
//                      to use the 'readNextBlockFieldNums(int,int)' method
//                      in a number of places (these changes allow SHAPE-
//                      compatible RESP files to be parsed).
//    3/2/2005 -- [ET]  Improved 'readNextBlockFieldNums(int,int)' method;
//                      miscellaneous general cleanup.
//   3/10/2005 -- [ET]  Added support for URLs as input file names.
//   3/31/2005 -- [ET]  Modified to allow a site/location field consisting
//                      of blank characters; modified to parse "units"
//                      entries to end of line.
//   4/21/2005 -- [ET]  Changed "@returns" to "@return".
//   7/11/2005 -- [ET]  Modified to handle List Blockettes (55) that do not
//                      contain an "index" ("i") column in their data.
//   11/1/2005 -- [ET]  Modified to be able to successfully parse response
//                      files that contain no "B052F03 Location:" entry.
//   5/21/2010 -- [ET]  Made storage of symmetrical FIR filters more
//                      efficient in 'readFIR()' method.
//  10/21/2013 -- [ET]  Added support for Polynomial Blockette (62).
//   8/26/2014 -- [ET]  Modified 'findChannelId()' method to properly
//                      handle location/site value of "--" (meaning
//                      location value empty).
//
package com.isti.jevalresp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import edu.iris.Fissures.Time;
import edu.iris.Fissures.Unit;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.CoefficientErrored;
import edu.iris.Fissures.IfNetwork.CoefficientFilter;
import edu.iris.Fissures.IfNetwork.ComplexNumberErrored;
import edu.iris.Fissures.IfNetwork.Decimation;
import edu.iris.Fissures.IfNetwork.Filter;
import edu.iris.Fissures.IfNetwork.Gain;
import edu.iris.Fissures.IfNetwork.ListFilter;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Normalization;
import edu.iris.Fissures.IfNetwork.PoleZeroFilter;
import edu.iris.Fissures.IfNetwork.PolynomialFilter;
import edu.iris.Fissures.IfNetwork.Response;
import edu.iris.Fissures.IfNetwork.Sensitivity;
import edu.iris.Fissures.IfNetwork.Stage;
import edu.iris.Fissures.IfNetwork.TransferType;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;

/**
 * Class RespFileParser manages the parsing of an 'rdseed' ASCII response
 * ("RESP") file.
 */
public class RespFileParser {

    private final String inputFileName; //name of input file
    private final Reader inReaderObj; //input stream object
    private final RespTokenizer inTokens; //tokenizer object
    private BlockFieldSpec firstBFSpec = null; //pre-read spec object
    protected String errorMessage = null; //error message from parsing
    protected String infoMessage = null; //info message from parsing
    private ArrayList<Stage> currentStagesList = null; //Vector of 'Stage' objects
    private int curStageSeqNum = 0; //stage seq # tracker
    private Sensitivity curSensitivityObj = null; //sensitivity for response
    private final ArrayList<ResponseUnits> inputUnits;

    public ResponseUnits getInputUnits() {
        if (!inputUnits.isEmpty()) {
            return inputUnits.get(0);
        } else {
            return null;
        }
    }

    /**
     * Creates an 'rdseed' ASCII file parsing object.
     *
     * @param fNameStr
     *            the name of an input file from which to read data.
     */
    public RespFileParser(String fNameStr) {
        inputUnits = new ArrayList<>();
        Reader rdr;
        Pattern p1 = Pattern.compile("\\s*#.*");// comment line
        Pattern p2 = Pattern.compile("#.*");// comment line
        Pattern p3 = Pattern.compile("B050F03     Station:\\s*\\w*#.*");// only allowing station names to have a #
        Pattern p4 = Pattern.compile(".*#.*");// line with data and comments
        Pattern p5 = Pattern.compile(".*B\\d{3,4}F\\d{2,3}.*");// line with data and comments
        try { //open file for input:
              //rdr = new BufferedReader(toReader(new FileInputStream(fNameStr)));
            BufferedReader cleanerReader = new BufferedReader(new FileReader(fNameStr));
            String line = cleanerReader.readLine();
            StringBuilder noComments = new StringBuilder();
            while (line != null) {
                Matcher m1 = p1.matcher(line);
                Matcher m2 = p2.matcher(line);
                Matcher m3 = p3.matcher(line);
                Matcher m4 = p4.matcher(line);
                Matcher m5 = p5.matcher(line);

                if (m1.matches() || m2.matches()) {
                    // toss comment lines
                } else if (m3.matches()) {// station lines with #,Only allowing one #
                    while (line.lastIndexOf("#") != line.indexOf("#")) {
                        line = line.substring(0, line.lastIndexOf("#"));
                    }
                    noComments.append(line);
                    noComments.append(System.lineSeparator());
                } else if (m4.matches()) {// line with data and comment
                    int ix = line.indexOf("#");
                    line = line.substring(0, ix);
                    if (m5.matches()) {
                        noComments.append(line);
                        noComments.append(System.lineSeparator());
                    }
                } else {
                    noComments.append(line);
                    noComments.append(System.lineSeparator());
                }
                line = cleanerReader.readLine();
            }
            cleanerReader.close();
            String noCommentsStr = noComments.toString();
            InputStream is = new ByteArrayInputStream(noCommentsStr.getBytes());
            rdr = new BufferedReader(new InputStreamReader(is));

        } catch (Exception ex) {
            rdr = null;
            setErrorMessage("Unable to open input file:  " + ex);
        }
        if (rdr == null) { //unable to open input file (as a local file)
            try { //attempt to open as a URL path:
                final String urlStr = RespUtils.fileObjPathToUrlStr(fNameStr);
                rdr = getBufferedReader(urlStr);
                if (rdr != null) { //opened successfully as a URL path
                    fNameStr = urlStr; //enter "restored" URL path
                    clearErrorMessage(); //clear previous error message
                } else if (isURLAddress(urlStr)) { //name is a URL
                    fNameStr = urlStr; //enter "restored" URL path
                    clearErrorMessage(); //enter new error message
                    setErrorMessage("Unable to open URL for input");
                }
            } catch (Exception ex) { //some kind of exception error; enter new error message
                clearErrorMessage();
                setErrorMessage("Unable to open input file:  " + ex);
            } finally {
                if (rdr != null) {
                    try {
                        rdr.close();
                    } catch (IOException e) {
                    }
                }
                rdr = null;
            }
        }
        inputFileName = fNameStr; //save file name
        inReaderObj = rdr; //save input reader object
        if (rdr != null) { //input file opened OK
            inTokens = new RespTokenizer(rdr); //create tokenizer
            checkInput(); //check input data
        } else //if file not opened OK then
        {
            inTokens = null; //set tokenizer to null
        }
    }

    private BufferedReader getBufferedReader(String filename) throws IOException {
        BufferedReader reader = null;
        InputStream inStream = null;
        if (isURLAddress(filename)) {
            URL httpResource = new URL(filename);
            reader = new BufferedReader(new InputStreamReader(httpResource.openStream()));
        } else {
            inStream = new FileInputStream(filename);
            reader = new BufferedReader(new InputStreamReader(inStream));
        }

        return reader;
    }

    private boolean isURLAddress(String urlStr) {
        boolean isUrl = false;
        if (urlStr != null && urlStr.length() > 3) {
            int index = urlStr.indexOf("://");
            if (index > 0 && index <= 10) {
                isUrl = true;
            }
        }
        return isUrl;
    }

    //Checks if input contains valid response data.
    private void checkInput() {
        try {
            int tType;
            while (true) { //for each token parsed
                tType = inTokens.nextToken();
                if (tType == RespTokenizer.TT_EOF) { //end-of-file reached; set error message
                    setErrorMessage("No data found");
                    break;
                }
                if (tType == RespTokenizer.TT_WORD) { //word token found; check if station ID item
                    firstBFSpec = parseBlockFieldNums(inTokens.getTokenString());
                    if (firstBFSpec == null || firstBFSpec.blockNum != 50) { //station ID line not found; set error message
                        setErrorMessage("No valid response data found at line " + inTokens.lineno());
                    }
                    break;
                }
            }
        } catch (IOException ex) { //error reading token; set error message
            setErrorMessage("Error reading from input file: " + ex);
        }
    }

    /**
     * Closes the input file or stream used by this parser.
     */
    public void close() {
        try {
            if (inReaderObj != null) //if handle not null then
            {
                inReaderObj.close(); //close input stream
            }
        } catch (IOException ex) {
        }
        ;
    }

    /**
     * Finds a station/channel ID entry. The name pattern parameters are
     * expected to be glob-style expressions (can have '*' or '?' characters).
     * After a station/channel ID entry is found the 'readResponse()' method may
     * be used to parse and return the response data.
     *
     * @param station
     *            an array of station name patterns to search for, or a null or
     *            empty array to accept all station names.
     * @param channel
     *            an array of channel name patterns to search for, or a null or
     *            empty array to accept all channel names.
     * @param network
     *            an array of network name patterns to search for, or a null or
     *            empty array to accept all network names.
     * @param locid
     *            an array of site name patterns to search for, or a null or
     *            empty array to accept all site names.
     * @param requestedBeginDate
     *            the beginning of a date range to search for, or null for no
     *            begin date. If no end-date is given then this becomes a single
     *            date that must be within the date-range of matched responses.
     * @param requestedEndDate
     *            the end of a date range to search for, or null for no end
     *            date.
     * @param matchPolicy
     * @return A new 'ChannelIdHldr' object filled with data from the
     *         station/channel ID found, or null if no matching ID was found (in
     *         which case 'getErorFlag()' may be used to see if any errors were
     *         detected and 'getErorMessage()' may be used to see information
     *         about the first error detected).
     */
    public ChannelIdHldr findChannelIdX(String station, String channel, String network, String locid, Date requestedBeginDate, Date requestedEndDate, ChannelMatchPolicy matchPolicy) {

        String chaNameStr, netNameStr, siteNameStr;
        if (inTokens == null) { //tokenizer not set up; set error message
            setErrorMessage("Unable to read from input file");
            return null;
        }
        if (firstBFSpec == null || firstBFSpec.blockNum != 50 || firstBFSpec.fieldNum != 3) { //station ID line was not found; set error message
            setErrorMessage("Valid response data not found at line " + inTokens.lineno());
            return null;
        }
        firstBFSpec = null; //clear "first" block/field specifier
        clearErrorMessage(); //clear any previous error message
        BlockFieldSpec bfSpecObj;
        while (true) { //for each station/channel ID searched in file
            String staNameStr = readBasicDataValue();
            if (staNameStr == null) //fetch station name
            { //error parsing station name; set error message
                setErrorMessage("Unable to parse station name at line " + inTokens.lineno());
                return null;
            }
            netNameStr = readBlockFieldValue(50, 16);
            //fetch network name:
            if (netNameStr == null) { //error parsing network name; set error message
                setErrorMessage("Unable to parse network name at line " + inTokens.lineno());
                return null;
            }
            bfSpecObj = readNextBlockFieldNums(52, 3, 52, 4);
            //fetch location/site and channel names:
            // (locate/site name is optional)
            if (bfSpecObj != null) { //next block/field numbers fetched OK
                if (bfSpecObj.blockNum == 52 && bfSpecObj.fieldNum == 3) { //block and field numbers for location/site name found
                    siteNameStr = readBasicDataValue(); //fetch site/location value
                    //if blank or "??" site name then change to empty string:
                    if (siteNameStr == null || siteNameStr.trim().length() <= 0 || siteNameStr.equals("??")) {
                        siteNameStr = "??";
                    }
                    bfSpecObj = readNextBlockFieldNums(52, 4); //get next block/field #s
                } else //block and field numbers for location/site name not found
                {
                    siteNameStr = ""; //set empty string
                }
            } else //next block/field numbers not fetched OK
            {
                siteNameStr = ""; //set empty string
            }
            if (bfSpecObj == null || bfSpecObj.blockNum != 52 || bfSpecObj.fieldNum != 4 || (chaNameStr = readBasicDataValue()) == null) { //error fetching matching item or value; set error message
                setErrorMessage("Unable to parse channel name (sta=\"" + staNameStr + "\") at line " + inTokens.lineno());
                return null;
            }
            Date parsedStartDateObj = getParsedStartDate(staNameStr, chaNameStr);
            if (parsedStartDateObj == null) {
                return null;
            }
            Date parsedEndDateObj = getParsedEndDate(staNameStr, chaNameStr);
            if (parsedEndDateObj == null) {
                return null;
            }
            //test if found channel ID is a match:
            if (matchPolicy.isParsedFieldsMatchRequest(
                    station,
                        staNameStr,
                        channel,
                        chaNameStr,
                        network,
                        netNameStr,
                        locid,
                        siteNameStr,
                        requestedEndDate,
                        requestedBeginDate,
                        parsedStartDateObj,
                        parsedEndDateObj)) { //date matched OK; accept response
                //create Fissures Time object:
                FastDateFormat format = FastDateFormat.getInstance("yyyyDDD'T'HH:mm:ss.SSS'z'", TimeZone.getTimeZone("GMT"));
                final Time fissTimeObj = new Time(format.format(parsedStartDateObj), -1);
                //create and return 'ChannelIdHldr' object:
                return new ChannelIdHldr(new ChannelId(new NetworkId(netNameStr, fissTimeObj), staNameStr, siteNameStr, chaNameStr, fissTimeObj), parsedEndDateObj);

            }
            //found channel ID is not a match; find next one:
            try {
                int tType = inTokens.nextToken(); //pre-read first token
                while (true) //read in and discard rest of response data:
                { //parse through any blank or comment lines:
                    while (tType == RespTokenizer.TT_EOL) {
                        tType = inTokens.nextToken();
                    }
                    if (tType == RespTokenizer.TT_EOF) //if end-of-file then
                    {
                        return null; //exit method
                    }
                    if (tType != RespTokenizer.TT_WORD || (bfSpecObj = parseBlockFieldNums(inTokens.getTokenString())) == null) { //not word token or not "B###F##" format string
                        setErrorMessage("Invalid format in file at line " + inTokens.lineno());
                        return null;
                    }
                    if (bfSpecObj.blockNum == 50) { //station ID data found
                        if (bfSpecObj.fieldNum != 3) { //not field number for station name; set error message
                            setErrorMessage("Invalid format in file at line " + inTokens.lineno());
                            return null; //return error
                        }
                        break; //exit inner loop; process station/channel ID data
                    }
                    //read in and discard rest of tokens on line:
                    while ((tType = inTokens.nextToken()) == RespTokenizer.TT_WORD) {
                        ;
                    }
                }
            } catch (IOException ex) { //error reading token; set error message
                setErrorMessage("Error reading from input file: " + ex);
                return null; //return error
            }
        }
    }

    private Date getParsedEndDate(String staNameStr, String chaNameStr) {
        Date parsedEndDateObj;
        //fetch end date (all tokens to end of line):
        String parsedEndDateStr = readBlockFieldValue(52, 23, true);
        if (parsedEndDateStr == null) { //field not found; set error message
            setErrorMessage(String.format("Unable to find end date field (sta=%s, chan = %s) at line %d.", staNameStr, chaNameStr, inTokens.lineno()));
            return null;
        }
        if (!parsedEndDateStr.equalsIgnoreCase("No Ending Time") && !parsedEndDateStr.equalsIgnoreCase("null")) { //not special "No Ending Time" string; parse into Date object
            parsedEndDateObj = RespUtils.parseRespDate(parsedEndDateStr);

            if (parsedEndDateObj == null) { //error parsing end date; set error message
                setErrorMessage(String.format("Unable to parse end date (sta=%s, chan = %s) at line %d.", staNameStr, chaNameStr, inTokens.lineno()));
                return null;
            }
        } else //special "No Ending Time" string
        {
            parsedEndDateObj = RespUtils.NO_ENDDATE_OBJ; //indicate no ending date
        }
        return parsedEndDateObj;
    }

    private Date getParsedStartDate(String staNameStr, String chaNameStr) {
        Date parsedStartDateObj;
        //fetch begin date (all tokens to end of line):
        parsedStartDateObj = null;
        String parsedStartDateStr = readBlockFieldValue(52, 22, true);
        if (parsedStartDateStr != null) {
            parsedStartDateObj = RespUtils.parseRespDate(parsedStartDateStr);
        }
        if (parsedStartDateStr == null || parsedStartDateObj == null) { //error parsing begin date; set error message
            String msg = String.format("Unable to parse begin date (sta=%s, chan = %s) at line %d.", staNameStr, chaNameStr, inTokens.lineno());
            setErrorMessage(msg);
            return null;
        }
        return parsedStartDateObj;
    }

    /**
     * Finds a station/channel ID entry. The name pattern parameters are
     * expected to be glob-style expressions (can have '*' or '?' characters).
     * After a station/channel ID entry is found the 'readResponse()' method may
     * be used to parse and return the response data.
     *
     * @param stationPatArr
     *            an array of station name patterns to search for, or a null or
     *            empty array to accept all station names.
     * @param channelPatArr
     *            an array of channel name patterns to search for, or a null or
     *            empty array to accept all channel names.
     * @param networkPatArr
     *            an array of network name patterns to search for, or a null or
     *            empty array to accept all network names.
     * @param sitePat
     *            a site name pattern to search for, or a null or empty string
     *            to accept all site names.
     * @param beginDateObj
     *            the beginning of a date range to search for, or null for no
     *            begin date. If no end-date is given then this becomes a single
     *            date that must be within the date-range of matched responses.
     * @param endDateObj
     *            the end of a date range to search for, or null for no end
     *            date.
     * @param matchPolicy
     * @return A new 'ChannelIdHldr' object filled with data from the
     *         station/channel ID found, or null if no matching ID was found (in
     *         which case 'getErorFlag()' may be used to see if any errors were
     *         detected and 'getErorMessage()' may be used to see information
     *         about the first error detected).
     */
    public ChannelIdHldr findChannelId(String stationPatArr, String channelPatArr, String networkPatArr, String sitePat, Date beginDateObj, Date endDateObj, ChannelMatchPolicy matchPolicy) {
        return findChannelIdX(stationPatArr, channelPatArr, networkPatArr, sitePat, beginDateObj, endDateObj, matchPolicy);
    }

    /**
     * Parses and returns a set of response data. The 'findChannelId()' method
     * should be used to find the desired station/channel ID before this method
     * is used.
     *
     * @return A new 'Response' object filled with parsed data, or null if an
     *         error occurred (in which case 'getErorMessage()' may be used to
     *         see information about the error).
     */
    public Response readResponse() {
        int tType;
        BlockFieldSpec bfSpecObj = readNextBlockFieldNums();

        if (bfSpecObj == null) { //error fetching first "B###F##" item
            setErrorMessage("Unable to read any valid response data at line " + inTokens.lineno());
            return null;
        }
        currentStagesList = new ArrayList<>(); //create Vector for 'Stage' objects
        curStageSeqNum = 0; //initialize stage seq # tracker
        curSensitivityObj = null; //init sensitivity for response
        mainLoop: while (true) {
            switch (bfSpecObj.blockNum) {
            case 50: //Station Identifier Blockette
                firstBFSpec = bfSpecObj; //in case of next 'findChannelId()'
                break mainLoop; //exit the 'while' loop

            case 53: //Response (Poles & Zeros) Blockette
                if (!readPolesZeros(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 54: //Response (Coefficients) Blockette
                if (!readCoefficients(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 55: //Response (Coefficients) Blockette
                if (!readList(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 56: //Generic Response Blockette
                if (!readGeneric(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 57: //Decimation Blockette
                if (!readDecimation(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 58: //Channel Sensitivity/Gain Blockette
                if (!readSensGain(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 60: //Response Reference Blockette
                if (!readReference(bfSpecObj)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 61: //FIR Response Blockette
                if (!readFIR(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            case 62: //Polynomial Response Blockette
                if (!readPolynomial(bfSpecObj, false)) //read blockette data
                {
                    return null; //if error then exit method
                }
                break;

            default: //unexpected blockette; set error message
                setErrorMessage("Unexpected Blockette type (" + bfSpecObj.blockNum + ")");
                return null;
            }
            try //fetch next line of response data:
            { //skip any blank (or comment) lines:
                while ((tType = inTokens.nextToken()) == RespTokenizer.TT_EOL) {
                    ;
                }
            } catch (IOException ex) { //error reading token; set error message
                setErrorMessage("Error reading from input file: " + ex);
                return null;
            }
            if (tType == RespTokenizer.TT_EOF) {
                break; //if end-of-file reached then exit loop
            } //if word token then fetch and parse "B###F##..." string:
            if (tType != RespTokenizer.TT_WORD || (bfSpecObj = parseBlockFieldNums(inTokens.getTokenString())) == null) { //valid "B###F##..." string not found; set error message
                setErrorMessage("Invalid data in input file at line " + inTokens.lineno());
                return null;
            }
        }
        //convert Vector of stage objects to array:
        Stage[] stageArr;
        try {
            stageArr = new Stage[currentStagesList.size()];
            stageArr = (currentStagesList.toArray(stageArr));
        } catch (Exception ex) { //error detected (shouldn't happen); set error message:
            setErrorMessage("Internal error:  Unable to create stages array " + "from vector in 'readResponse()':  " + ex);
            return null;
        }
        //check for null handles in response elements:
        //if no channel sensitivity was found then set default
        if (curSensitivityObj == null) {
            curSensitivityObj = new Sensitivity((float) 0.0, (float) 0.0);
        }
        Stage stageObj;
        Stage nextStageObj;
        int j;
        TransferType prevTypeObj = TransferType.DIGITAL; //previous stage type
        Unit prevOutUnitsObj = null; //output units for previous stage
        for (int i = 0; i < stageArr.length; ++i) { //for each stage in response
            stageObj = stageArr[i];
            if (stageObj == null) { //array element is null; set error message
                setErrorMessage("No data entered for stage #" + (i + 1) + " (out of " + stageArr.length + " stages)");
                return null;
            }
            //if no Normalization object then setup empty array
            if (stageObj.the_normalization == null) {
                stageObj.the_normalization = new Normalization[0];
            }
            //if no Decimation object then setup empty array
            if (stageObj.the_decimation == null) {
                stageObj.the_decimation = new Decimation[0];
            }
            //if filters array handle is null then setup empty array:
            if (stageObj.filters == null) {
                stageObj.filters = new Filter[0];
            }
            if (stageObj.type == null) { //no type object; check if stage is a gain-only stage
                if (stageObj.the_gain == null || stageObj.filters.length > 0) { //no gain entered or more than 0 filters; set error message
                    setErrorMessage("No transfer type for stage #" + (i + 1));
                    return null;
                }
                //stage is a gain-only stage; set type to same as previous
                stageObj.type = prevTypeObj; // (or default type)
                if (prevOutUnitsObj != null) //if OK then set output units to prev
                {
                    stageObj.output_units = prevOutUnitsObj;
                } else //"previous" output units not set
                { //find input units type of next non-gain-only stage:
                    j = i; //initialize next-stage index value
                    do { //check each proceeding stage until valid input units found
                        if (++j >= stageArr.length) { //no more stages left; set error message
                            setErrorMessage("Unable to find valid non-gain-only " + "stage after #" + (i + 1));
                            return null;
                        }
                    } //loop if next stage or its input units are invalid:
                    while ((nextStageObj = stageArr[j]) == null || nextStageObj.input_units == null);
                    //set output units to input units of proceeding stage:
                    stageObj.output_units = nextStageObj.input_units;
                }
                //set input units same as output units:
                stageObj.input_units = stageObj.output_units;
            } else { //stage type OK (not gain-only stage or invalid type)
                if (stageObj.input_units == null) { //no input units object; set error message
                    setErrorMessage("No input units for stage #" + (i + 1));
                    return null;
                }
                if (stageObj.output_units == null) { //no output units object; set error message
                    setErrorMessage("No output units for stage #" + (i + 1));
                    return null;
                }
                //if no Gain object then setup default values
                if (stageObj.the_gain == null) {
                    stageObj.the_gain = new Gain((float) 0.0, (float) 0.0);
                }
            }
            prevTypeObj = stageObj.type; //save stage type
            prevOutUnitsObj = stageObj.output_units; //save output units type
        }
        //create and return response object:
        return new Response(curSensitivityObj, stageArr);
    }

    //Create a new Stage object and adds it to the current list (if
    // the given sequence number is after the current number).
    // @param seqNum sequence number for stage.
    // @param forceFlag true to force creation of new stage object.
    private Stage createAddNewStageObj(int seqNum, boolean forceFlag) {
        try {
            if (forceFlag || seqNum > curStageSeqNum) { //stage sequence number is new
                Stage stageObj;
                currentStagesList.add( //create new Stage object and add to Vector
                        stageObj = new Stage(null, null, null, null, null, null, null));
                curStageSeqNum = seqNum; //save new stage sequence number
                return stageObj;
            }
        } catch (Exception ex) { //some kind of exception; just return null
        }
        return null;
    }

    //Create a new Stage object and adds it to the current list (if
    // the given sequence number is after the current number).
    // @param seqNum sequence number for stage.
    private Stage createAddNewStageObj(int seqNum) {
        return createAddNewStageObj(seqNum, false);
    }

    //Reads Response (Poles & Zeros) Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readPolesZeros(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (43) or normal (53) block number:
        final int pzBlkNum = dictFlag ? 43 : 53;
        //dictionary entry type (43) starts with field 5;
        // normal type (53) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        final String typeStr;
        //check block/field #s and get "Transfer function type" value:
        if (initBfSpecObj.blockNum != pzBlkNum || initBfSpecObj.fieldNum != fieldNum || (typeStr = readBasicDataValue()) == null) { //type field not found; set error message
            setErrorMessage("Unable to find response type field in Poles/Zeros" + " Blockette at line " + inTokens.lineno());
            return false;
        }
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            str = readBlockFieldValue(pzBlkNum, ++fieldNum);
            if (str == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in Poles/Zeros " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in Poles/Zeros Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readPolesZeros()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        final TransferType typeObj;
        //process filter/stage type:
        if (typeStr.equals("A")) {
            typeObj = TransferType.LAPLACE;
        } else if (typeStr.equals("B")) {
            typeObj = TransferType.ANALOG;
        } else if (typeStr.equals("C")) {
            typeObj = TransferType.COMPOSITE;
        } else if (typeStr.equals("D")) {
            typeObj = TransferType.DIGITAL;
        } else { //unrecognized value; set error message
            setErrorMessage("Invalid transfer type (\"" + typeStr + "\") in Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.type != null && !typeObj.equals(stageObj.type)) { //different stage type set previously
            setErrorMessage("Different transfer type (\"" + typeStr + "\") in same stage in Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        stageObj.type = typeObj; //save new transfer/response type
        Unit unitObj;
        str = readBlockFieldValue(pzBlkNum, ++fieldNum, true);
        //process in-units type value:
        if (str == null) { //in-units field not found; set error message
            setErrorMessage("Unable to find in-units field in Poles/Zeros " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.input_units == null) { //no previous in-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized in-units field value; set error message
                setErrorMessage(String.format("Invalid in-units type %s  in Poles/Zeros Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.input_units = units.getUnitObj(); //enter new units value
            inputUnits.add(units);
        }
        str = readBlockFieldValue(pzBlkNum, ++fieldNum, true);
        //process out-units type value:
        if (str == null) { //out-units field not found; set error message
            setErrorMessage("Unable to find out-units field in Poles/Zeros " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.output_units == null) { //no previous out-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized out-units field value; set error message
                setErrorMessage(String.format("Invalid out-units type %s  in Poles/Zeros Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.output_units = units.getUnitObj();
            ; //enter new units value
        }
        str = readBlockFieldValue(pzBlkNum, ++fieldNum);
        //process normalization factor:
        if (str == null) { //normalization factor field not found; set error message
            setErrorMessage("Unable to find normalization factor field in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        final float nFactVal;
        try { //convert string to float value:
            nFactVal = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse normalization factor (" + str + ") in Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(pzBlkNum, ++fieldNum);
        //process normalization frequency:
        if (str == null) { //normalization frequency field not found; set error message
            setErrorMessage("Unable to find normalization frequency field in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        final float nFreqVal;
        try { //convert string to float value:
            nFreqVal = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse normalization frequency (" + str + ") in Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.the_normalization == null) { //normalization object not previously entered
            //create normalization object and enter into stage:
            stageObj.the_normalization = new Normalization[] { new Normalization(nFactVal, nFreqVal) };
        } else if (stageObj.the_normalization.length <= 0 || nFactVal != stageObj.the_normalization[0].ao_normalization_factor || nFreqVal != stageObj.the_normalization[0].normalization_freq) { //previous normalization is different; set error message
            setErrorMessage("Different normalization values in same stage in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(pzBlkNum, ++fieldNum);
        //process number of zeros count:
        if (str == null) { //number of zeros field not found; set error message
            setErrorMessage("Unable to find number of zeros field in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numZeros;
        try { //convert string to integer:
            numZeros = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of zeros field (" + str + ") in Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(pzBlkNum, fieldNum + 5);
        //process number of poles count (field number is +5 after zeros):
        if (str == null) { //number of poles field not found; set error message
            setErrorMessage("Unable to find number of poles field in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numPoles;
        try { //convert string to integer:
            numPoles = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of poles field (" + str + ") in Poles/Zeros Blockette at line " + inTokens.lineno());
            return false;
        }
        BlockFieldSpec bfSpecObj;
        int i;
        ComplexNumberErrored[] zerosArr = new ComplexNumberErrored[numZeros];
        ++fieldNum; //move to next field number
        for (i = 0; i < numZeros; ++i) { //for each line of zeros items
            bfSpecObj = readNextBlockFieldNums();
            //read and check "B###F##" string; read and discard integer
            // index value; then read 4 floating-point values:
            if (bfSpecObj == null
                    || bfSpecObj.blockNum != pzBlkNum
                    || bfSpecObj.fieldNum != fieldNum
                    || bfSpecObj.endFieldNum != fieldNum + 3
                    || readNextTokenString() == null
                    || (zerosArr[i] = readComplexNumberErrored()) == null) {
                setErrorMessage("Error parsing zeros data items in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        ComplexNumberErrored[] polesArr = new ComplexNumberErrored[numPoles];
        fieldNum += 5; //move to poles data items field
        for (i = 0; i < numPoles; ++i) { //for each line of poles items
            bfSpecObj = readNextBlockFieldNums(pzBlkNum, fieldNum);
            //read and check "B###F##" string; read and discard integer
            // index value; then read 4 floating-point values:
            if (bfSpecObj == null
                    || bfSpecObj.blockNum != pzBlkNum
                    || bfSpecObj.fieldNum != fieldNum
                    || bfSpecObj.endFieldNum != fieldNum + 3
                    || readNextTokenString() == null
                    || (polesArr[i] = readComplexNumberErrored()) == null) {
                setErrorMessage("Error parsing poles data items in " + "Poles/Zeros Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        //enter new filter into stage:
        if (stageObj.filters == null || stageObj.filters.length <= 0) { //no previous filter entered
            //create and configure Coefficients filter:
            final Filter filterObj = new Filter();
            filterObj.pole_zero_filter(new PoleZeroFilter(polesArr, zerosArr));
            //create and enter array containing filter item:
            stageObj.filters = new Filter[] { filterObj };
        } else { //previous filter entered; append new values
            try {
                final PoleZeroFilter pzFilter = //get last filter object
                        stageObj.filters[stageObj.filters.length - 1].pole_zero_filter();
                //build Vector containing old and new pole values,
                // then convert it back to an array and enter into filter:
                final ArrayList<ComplexNumberErrored> vec = new ArrayList<>(Arrays.asList(pzFilter.poles));
                vec.addAll(Arrays.asList(polesArr));
                pzFilter.poles = (vec.toArray(new ComplexNumberErrored[vec.size()]));
                //clear Vector and do same thing for zeros:
                vec.clear();
                vec.addAll(Arrays.asList(pzFilter.zeros));
                vec.addAll(Arrays.asList(zerosArr));
                pzFilter.zeros = (vec.toArray(new ComplexNumberErrored[vec.size()]));
            } catch (Exception ex) {
                setErrorMessage("Internal error:  Exception extending arrays " + "in 'readPolesZeros()':  " + ex);
                return false;
            }
        }
        return true;
    }

    //Reads Response (Coefficients) Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readCoefficients(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (44) or normal (54) block number:
        final int cfBlkNum = dictFlag ? 44 : 54;
        //dictionary entry type (44) starts with field 5;
        // normal type (54) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        final String typeStr;
        //check block/field #s and get "Response function type" value:
        if (initBfSpecObj.blockNum != cfBlkNum || initBfSpecObj.fieldNum != fieldNum || (typeStr = readBasicDataValue()) == null) { //type field not found; set error message
            setErrorMessage("Unable to find response type field in Coefficients" + " Blockette at line " + inTokens.lineno());
            return false;
        }
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            str = readBlockFieldValue(cfBlkNum, ++fieldNum);
            if (str == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in Coefficients " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in Coefficients Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "Coefficients Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readCoefficients()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        final TransferType typeObj;
        //process filter/stage type:
        if (typeStr.equals("A")) {
            typeObj = TransferType.ANALOG;
        } else if (typeStr.equals("B")) {
            typeObj = TransferType.ANALOG;
        } else if (typeStr.equals("C")) {
            typeObj = TransferType.COMPOSITE;
        } else if (typeStr.equals("D")) {
            typeObj = TransferType.DIGITAL;
        } else { //unrecognized value; set error message
            setErrorMessage("Invalid transfer type (\"" + typeStr + "\") in Coefficients Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.type != null && !typeObj.equals(stageObj.type)) { //different stage type set previously
            setErrorMessage("Different transfer type (\"" + typeStr + "\") in same stage in Coefficients Blockette at line " + inTokens.lineno());
            return false;
        }
        stageObj.type = typeObj; //save new transfer/response type
        str = readBlockFieldValue(cfBlkNum, ++fieldNum, true);
        //process in-units type value:
        if (str == null) { //in-units field not found; set error message
            setErrorMessage("Unable to find in-units field in Coefficients " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.input_units == null) { //no previous in-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized in-units field value; set error message
                setErrorMessage(String.format("Invalid in-units type %s  in Coefficients Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.input_units = units.getUnitObj(); //enter new units value
            inputUnits.add(units);

        }
        str = readBlockFieldValue(cfBlkNum, ++fieldNum, true);
        //process out-units type value:
        if (str == null) { //out-units field not found; set error message
            setErrorMessage("Unable to find out-units field in Coefficients " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.output_units == null) { //no previous out-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized out-units field value; set error message
                setErrorMessage(String.format("Invalid out-units type %s  in Coefficients Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.output_units = units.getUnitObj(); //enter new units value
        }

        str = readBlockFieldValue(cfBlkNum, ++fieldNum);
        //process number of numerators count:
        if (str == null) { //number of numerators field not found; set error message
            setErrorMessage("Unable to find number of numerators field in " + "Coefficients Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numNumerators;
        try { //convert string to integer:
            numNumerators = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of numerators field (" + str + ") in Coefficients Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(cfBlkNum, fieldNum + 3);
        //process # of denominators count (field # is +3 after numerators):
        if (str == null) { //number of denominators field not found; set error message
            setErrorMessage("Unable to find number of denominators field in " + "Coefficients Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numDenominators;
        try { //convert string to integer:
            numDenominators = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of denominators field (" + str + ") in Coefficients Blockette at line " + inTokens.lineno());
            return false;
        }
        BlockFieldSpec bfSpecObj;
        int i;
        CoefficientErrored[] numeratorsArr = new CoefficientErrored[numNumerators];
        ++fieldNum; //move to next field number
        for (i = 0; i < numNumerators; ++i) { //for each line of numerators items
            bfSpecObj = readNextBlockFieldNums(cfBlkNum, fieldNum);
            //read and check "B###F##" string; read and discard integer
            // index value; then read 2 floating-point values:
            if (bfSpecObj == null
                    || bfSpecObj.blockNum != cfBlkNum
                    || bfSpecObj.fieldNum != fieldNum
                    || bfSpecObj.endFieldNum != fieldNum + 1
                    || readNextTokenString() == null
                    || (numeratorsArr[i] = readCoefficientErrored()) == null) {
                setErrorMessage("Error parsing numerators data items in " + "Coefficients Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        CoefficientErrored[] denominatorsArr = new CoefficientErrored[numDenominators];
        fieldNum += 3; //move to denominators data items field
        for (i = 0; i < numDenominators; ++i) { //for each line of denominators items
            bfSpecObj = readNextBlockFieldNums(cfBlkNum, fieldNum);
            //read and check "B###F##" string; read and discard integer
            // index value; then read 2 floating-point values:
            if (bfSpecObj == null
                    || bfSpecObj.blockNum != cfBlkNum
                    || bfSpecObj.fieldNum != fieldNum
                    || bfSpecObj.endFieldNum != fieldNum + 1
                    || readNextTokenString() == null
                    || (denominatorsArr[i] = readCoefficientErrored()) == null) {
                setErrorMessage("Error parsing denominators data items in " + "Coefficients Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        //enter new filter into stage:
        if (stageObj.filters == null || stageObj.filters.length <= 0) { //no previous filter entered
            //create and configure Coefficients filter:
            final Filter filterObj = new Filter();
            filterObj.coeff_filter(new CoefficientFilter(numeratorsArr, denominatorsArr));
            //create and enter array containing filter item:
            stageObj.filters = new Filter[] { filterObj };
        } else { //previous filter entered; append new values
            try {
                final CoefficientFilter coeffFilter = //get last filter object
                        stageObj.filters[stageObj.filters.length - 1].coeff_filter();
                //build Vector containing old and new numerator values,
                // then convert it back to an array and enter into filter:
                final ArrayList<CoefficientErrored> vec = new ArrayList<>(Arrays.asList(coeffFilter.numerator));
                vec.addAll(Arrays.asList(numeratorsArr));
                coeffFilter.numerator = (vec.toArray(new CoefficientErrored[vec.size()]));
                //clear Vector and do same thing for denominator:
                vec.clear();
                vec.addAll(Arrays.asList(coeffFilter.denominator));
                vec.addAll(Arrays.asList(denominatorsArr));
                coeffFilter.denominator = (vec.toArray(new CoefficientErrored[vec.size()]));
            } catch (Exception ex) {
                setErrorMessage("Internal error:  Exception extending arrays " + "in 'readCoefficients()':  " + ex);
                return false;
            }
        }
        return true;
    }

    //Reads Response List Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readList(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (45) or normal (55) block number:
        final int rlBlkNum = dictFlag ? 45 : 55;
        //dictionary entry type (45) starts with field 5;
        // normal type (54) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            if (initBfSpecObj.blockNum != rlBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in List " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in List Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "List Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
            ++fieldNum; //increment number for next field
            initBfSpecObj = readNextBlockFieldNums(rlBlkNum, fieldNum);
            //read in next field:
            if (initBfSpecObj == null) { //error reading next field; set error message
                setErrorMessage("Unable to find in-units field in " + "List Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readList()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        //process in-units type value:
        if (initBfSpecObj.blockNum != rlBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue(true)) == null) { //in-units field not found; set error message
            setErrorMessage("Unable to find in-units field in List " + "Blockette at line " + inTokens.lineno());
            return false;
        }

        if (stageObj.input_units == null) { //no previous in-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized in-units field value; set error message
                setErrorMessage(String.format("Invalid in-units type %s  in List Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.input_units = units.getUnitObj(); //enter new units value
            inputUnits.add(units);
        }
        str = readBlockFieldValue(rlBlkNum, ++fieldNum, true);
        //process out-units type value:
        if (str == null) { //out-units field not found; set error message
            setErrorMessage("Unable to find out-units field in List " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.output_units == null) { //no previous out-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized out-units field value; set error message
                setErrorMessage(String.format("Invalid out-units type %s  in List Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.output_units = units.getUnitObj(); //enter new units value
        }
        str = readBlockFieldValue(rlBlkNum, ++fieldNum);
        //process number of responses count:
        if (str == null) { //number of responses field not found; set error message
            setErrorMessage("Unable to find number of responses field in " + "List Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numResponses;
        try { //convert string to integer:
            numResponses = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of responses field (" + str + ") in List Blockette at line " + inTokens.lineno());
            return false;
        }
        BlockFieldSpec bfSpecObj;
        final float[] freqArr = new float[numResponses]; //array of freqs
        final float[] ampArr = new float[numResponses]; //array of amp values
        final float[] ampErrArr = new float[numResponses]; //amp error vals
        final float[] phaseArr = new float[numResponses]; //phase values
        final float[] phaseErrArr = new float[numResponses]; //phase err vals
        RespListItem rListObj;
        ++fieldNum; //move to next field number
        for (int i = 0; i < numResponses; ++i) { //for each line of response items
            bfSpecObj = readNextBlockFieldNums(rlBlkNum, fieldNum);
            //read and check "B###F##" string; read and discard integer
            // index value; then read 5 floating-point values:
            if (bfSpecObj == null || bfSpecObj.blockNum != rlBlkNum || bfSpecObj.fieldNum != fieldNum || bfSpecObj.endFieldNum != fieldNum + 4 || //                                            readNextTokenString() == null ||
                    (rListObj = readRespListItem()) == null) {
                setErrorMessage("Error parsing response data items in " + "List Blockette at line " + inTokens.lineno());
                return false;
            }
            freqArr[i] = rListObj.freq; //save frequency value
            ampArr[i] = rListObj.amp; //save amplitude value
            ampErrArr[i] = rListObj.ampError; //save amplitude error value
            phaseArr[i] = rListObj.phase; //save phase value
            phaseErrArr[i] = rListObj.phaseError; //save phase error value
        }
        //enter new filter into stage:
        if (stageObj.filters == null || stageObj.filters.length <= 0) { //no previous filter entered; create and configure List filter
            // (order of parameters in 'ListFilter' constructer is a
            //  bit strange, so each member is assigned by name):
            final ListFilter listFilterObj = new ListFilter();
            listFilterObj.frequency = freqArr;
            listFilterObj.amplitude = ampArr;
            listFilterObj.amplitude_error = ampErrArr;
            listFilterObj.phase = phaseArr;
            listFilterObj.phase_error = phaseErrArr;
            listFilterObj.frequency_unit = UnitImpl.HERTZ;
            listFilterObj.phase_unit = UnitImpl.DEGREE;
            final Filter filterObj = new Filter();
            filterObj.list_filter(listFilterObj);
            //create and enter array containing filter item:
            stageObj.filters = new Filter[] { filterObj };
            //transfer/response type needs to be set to something:
            stageObj.type = TransferType.DIGITAL;
        } else { //previous filter entered; append new values
            try {
                final ListFilter listFilterObj = //get last filter object
                        stageObj.filters[stageObj.filters.length - 1].list_filter();
                //append new values to the end of each array:
                listFilterObj.frequency = RespUtils.appendArrays(listFilterObj.frequency, freqArr);
                listFilterObj.amplitude = RespUtils.appendArrays(listFilterObj.amplitude, ampArr);
                listFilterObj.amplitude_error = RespUtils.appendArrays(listFilterObj.amplitude_error, ampErrArr);
                listFilterObj.phase = RespUtils.appendArrays(listFilterObj.phase, phaseArr);
                listFilterObj.phase_error = RespUtils.appendArrays(listFilterObj.phase_error, phaseErrArr);
            } catch (Exception ex) {
                setErrorMessage("Internal error:  Exception extending arrays " + "in 'readList()':  " + ex);
                return false;
            }
        }
        return true;
    }

    //Reads Generic Response Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readGeneric(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (46) or normal (56) block number:
        final int grBlkNum = dictFlag ? 46 : 56;
        //dictionary entry type (46) starts with field 5;
        // normal type (56) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            if (initBfSpecObj.blockNum != grBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in Generic Response " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in Generic Response Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "Generic Response Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
            ++fieldNum; //increment number for next field
            initBfSpecObj = readNextBlockFieldNums(grBlkNum, fieldNum);
            //read in next field:
            if (initBfSpecObj == null) { //error reading next field; set error message
                setErrorMessage("Unable to find in-units field in " + "Generic Response Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readGeneric()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }

        //process in-units type value:
        if (initBfSpecObj.blockNum != grBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue(true)) == null) { //in-units field not found; set error message
            setErrorMessage("Unable to find in-units field in Generic Response " + "Blockette at line " + inTokens.lineno());
            return false;
        }

        if (stageObj.input_units == null) { //no previous in-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized in-units field value; set error message
                setErrorMessage(String.format("Invalid in-units type %s  in Generic Response Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.input_units = units.getUnitObj(); //enter new units value
            inputUnits.add(units);
        }
        str = readBlockFieldValue(grBlkNum, ++fieldNum, true);
        //process out-units type value:
        if (str == null) { //out-units field not found; set error message
            setErrorMessage("Unable to find out-units field in Generic " + "Response Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.output_units == null) { //no previous out-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized out-units field value; set error message
                setErrorMessage(String.format("Invalid out-units type %s  in Generic Response Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.output_units = units.getUnitObj(); //enter new units value
        }
        str = readBlockFieldValue(grBlkNum, ++fieldNum);
        //process number of corners count:
        if (str == null) { //number of corners field not found; set error message
            setErrorMessage("Unable to find number of corners field in " + "Generic Response Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numCorners;
        try { //convert string to integer:
            numCorners = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of corners field (" + str + ") in Generic Response Blockette at line " + inTokens.lineno());
            return false;
        }
        for (int i = 0; i < numCorners; ++i) { //for each line of corner data; skip line
            if (readNextBlockFieldNums() == null) { //error reading line; set error message
                setErrorMessage("Error reading corner data in " + "Generic Response Blockette at line " + inTokens.lineno());
                return false;
            }
            if (!clearToEndOfLine()) //read & discard remaining items on line
            {
                return false; //if error then return flag
            }
        }
        if (numCorners > 0) { //corner data was specified
            setInfoMessage( //set warning message for blockette
                    "WARNING:  Corner data in Generic Response Blockette ignored");
        }
        return true;
    }

    //Reads Decimation Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readDecimation(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (47) or normal (57) block number:
        final int dcBlkNum = dictFlag ? 47 : 57;
        //dictionary entry type (47) starts with field 5;
        // normal type (57) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            if (initBfSpecObj.blockNum != dcBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in Decimation " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in Decimation Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "Decimation Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
            ++fieldNum; //increment number for next field
            initBfSpecObj = readNextBlockFieldNums(dcBlkNum, fieldNum);
            //read in next field:
            if (initBfSpecObj == null) { //error reading next field; set error message
                setErrorMessage("Unable to find input sample rate field in " + "Decimation Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readDecimation()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        //process input sample rate field:
        if (initBfSpecObj.blockNum != dcBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //unable to find input sample rate field; set error message
            setErrorMessage("Unable to find input sample rate field in " + "Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        float sampRate;
        try { //convert string to integer:
            sampRate = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse input sample rate field (" + str + ") in Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        //convert floating-point sample rate to integer sample rate
        // and interval value:
        int sampRateIntVal = (int) (sampRate * 10000.0 + 0.5); //int for testing
        final int sampInterval;
        //figure out number of significant digits to deal with:
        if (sampRateIntVal % 10000 == 0) {
            sampInterval = 1;
        } else if (sampRateIntVal % 1000 == 0) {
            sampInterval = 10;
        } else if (sampRateIntVal % 100 == 0) {
            sampInterval = 100;
        } else if (sampRateIntVal % 10 == 0) {
            sampInterval = 1000;
        } else {
            sampInterval = 10000;
        }
        //create sampling-interval object for FISSURES Decimation object:
        final SamplingImpl samplingImplObj = // (use scaled-up integer value)
                new SamplingImpl((int) (sampRate * sampInterval + 0.5), new TimeInterval(sampInterval, UnitImpl.SECOND));
        str = readBlockFieldValue(dcBlkNum, ++fieldNum);
        //process decimation factor field:
        if (str == null) { //unable to find decimation factor field; set error message
            setErrorMessage("Unable to find decimation factor field in " + "Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        final int deciFactor;
        try { //convert string to integer:
            deciFactor = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse decimation factor field (" + str + ") in Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(dcBlkNum, ++fieldNum);
        //process decimation offset field:
        if (str == null) { //unable to find decimation offset field; set error message
            setErrorMessage("Unable to find decimation offset field in " + "Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        final int deciOffset;
        try { //convert string to integer:
            deciOffset = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse decimation offset field (" + str + ") in Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(dcBlkNum, ++fieldNum);
        //process estimated delay field:
        if (str == null) { //unable to find estimated delay field; set error message
            setErrorMessage("Unable to find estimated delay field in " + "Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        final float estDelay;
        try { //convert string to integer:
            estDelay = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse estimated delay field (" + str + ") in Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        //create delay-interval object for FISSURES Decimation object:
        final TimeInterval delayIntervalObj = new TimeInterval(estDelay, UnitImpl.SECOND);
        str = readBlockFieldValue(dcBlkNum, ++fieldNum);
        //process correction applied field:
        if (str == null) { //unable to find correction applied field; set error message
            setErrorMessage("Unable to find correction applied field in " + "Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        final float corrApplied;
        try { //convert string to integer:
            corrApplied = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse correction applied field (" + str + ") in Decimation Blockette at line " + inTokens.lineno());
            return false;
        }
        //create correction-interval object for FISSURES Decimation object:
        final TimeInterval corrIntervalObj = new TimeInterval(corrApplied, UnitImpl.SECOND);

        if (stageObj.the_decimation != null) { //decimation data already entered; set error message
            setErrorMessage("Extra Decimation Blockette for stage at line " + inTokens.lineno());
            return false;
        }
        //create and add array containing FISSURES decimation object:
        stageObj.the_decimation = new Decimation[] { new Decimation(samplingImplObj, deciFactor, deciOffset, delayIntervalObj, corrIntervalObj) };
        return true;
    }

    //Reads Sensitivity/Gain Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    // @param seqNum sequence number for "dictionary" type entry.
    private boolean readSensGain(BlockFieldSpec initBfSpecObj, boolean dictFlag, int seqNum) {
        //setup dictionary (48) or normal (58) block number:
        final int sgBlkNum = dictFlag ? 48 : 58;
        //dictionary entry type (48) starts with field 5;
        // normal type (58) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            if (initBfSpecObj.blockNum != sgBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in Sensitivity/Gain " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in Sensitivity/Gain Blockette at line " + inTokens.lineno());
                return false;
            }
            //(seqNum==0 is OK; means sensitivity for entire channel)
            if (seqNum != 0 && seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "Sensitivity/Gain Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
            ++fieldNum; //increment number for next field
            initBfSpecObj = readNextBlockFieldNums(sgBlkNum, fieldNum);
            //read in next field:
            if (initBfSpecObj == null) { //unable to find sensitivity field; set error message
                setErrorMessage("Unable to find sensitivity field in " + "Sensitivity/Gain Blockette at line " + inTokens.lineno());
                return false;
            }
        } else if (seqNum != 0 && seqNum != curStageSeqNum) { //bad stage sequence # (shouldn't happen); set error message
            setErrorMessage("Internal error:  Stage seq# (" + seqNum + ") out of order in 'readSensGain()'");
            return false;
        }
        if (seqNum > 0 && stageObj == null) { //stage sequence number is not 0 and not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readSensitivity/Gain()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        //process sensitivity field:
        if (initBfSpecObj.blockNum != sgBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //unable to find sensitivity field; set error message
            setErrorMessage("Unable to find sensitivity field in " + "Sensitivity/Gain Blockette at line " + inTokens.lineno());
            return false;
        }
        final float sensVal;
        try { //convert string to integer:
            sensVal = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse sensitivity field (" + str + ") in Sensitivity/Gain Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(sgBlkNum, ++fieldNum);
        //process frequency of sensitivity field:
        if (str == null) { //unable to find frequency of sensitivity field; set error message
            setErrorMessage("Unable to find frequency of sensitivity field in " + "Sensitivity/Gain Blockette at line " + inTokens.lineno());
            return false;
        }
        final float sensFreq;
        try { //convert string to integer:
            sensFreq = Float.parseFloat(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse frequency of sensitivity field (" + str + ") in Sensitivity/Gain Blockette at line " + inTokens.lineno());
            return false;
        }
        str = readBlockFieldValue(sgBlkNum, ++fieldNum);
        //process number of calibrations count:
        if (str == null) { //number of calibrations field not found; set error message
            setErrorMessage("Unable to find number of calibrations field in " + "Sensitivity/Gain Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numCalibrations;
        try { //convert string to integer:
            numCalibrations = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of calibrations field (" + str + ") in Sensitivity/Gain Blockette at line " + inTokens.lineno());
            return false;
        }
        for (int i = 0; i < numCalibrations; ++i) { //for each line of calibration history data; skip line
            if (readNextBlockFieldNums() == null) { //error reading line; set error message
                setErrorMessage("Error reading calibration history data in " + "Sensitivity/Gain Blockette at line " + inTokens.lineno());
                return false;
            }
            if (!clearToEndOfLine()) //read & discard remaining items on line
            {
                return false; //if error then return flag
            }
        }
        if (seqNum == 0) { //this is a stage 0 channel sensitivity setting
            if (curSensitivityObj != null) { //response sensitivity already set; set error message
                setErrorMessage("Extra Sensitivity/Gain Blockette in response " + "at line " + inTokens.lineno());
                return false;
            }
            //set sensitivity for response:
            curSensitivityObj = new Sensitivity(sensVal, sensFreq);
        } else { //not a stage 0 channel sensitivity setting
            if (stageObj.the_gain != null) { //decimation data already entered; set error message
                setErrorMessage("Extra Decimation Blockette for stage at line " + inTokens.lineno());
                return false;
            }
            //create and add Gain object to stage:
            stageObj.the_gain = new Gain(sensVal, sensFreq);
        }
        return true;
    }

    //Reads Sensitivity/Gain Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readSensGain(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        return readSensGain(initBfSpecObj, dictFlag, -1);
    }

    //Reads FIR Response Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readFIR(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (41) or normal (61) block number:
        final int frBlkNum = dictFlag ? 41 : 61;
        //dictionary entry type (41) starts with field 5;
        // normal type (61) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            if (initBfSpecObj.blockNum != frBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue()) == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in FIR " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in FIR Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "FIR Blockette at line " + inTokens.lineno());
                return false;
            }
            //if new seqNum then create new Stage object and add to list:
            stageObj = createAddNewStageObj(seqNum);
            //if "Response Name" field before symmetry code field then skip it:
            ++fieldNum; //increment to next field number; read in next field
            initBfSpecObj = readNextBlockFieldNums();
            if (initBfSpecObj != null && initBfSpecObj.fieldNum == fieldNum) { //field numbers fetched OK and next sequential field was found
                clearToEndOfLine(); //skip rest of data on line
                initBfSpecObj = readNextBlockFieldNums(); //read in next field
            }
            ++fieldNum; //increment to symmetry code field number
            if (initBfSpecObj == null) { //unable to find symmetry code field; set error message
                setErrorMessage("Unable to find symmetry code field in " + "FIR Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readFIR()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        if (stageObj.type == null) //if no previous then
        {
            stageObj.type = TransferType.DIGITAL; //set stage type
        } else if (!stageObj.type.equals(TransferType.DIGITAL)) { //stage type different from previous type
            setErrorMessage("Non-digital filter type already in stage in FIR " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        final String symStr;
        //process symmetry code field:
        if (initBfSpecObj.blockNum != frBlkNum || initBfSpecObj.fieldNum != fieldNum || (symStr = readBasicDataValue()) == null) { //unable to find symmetry code field; set error message
            setErrorMessage("Unable to find symmetry code field in " + "FIR Blockette at line " + inTokens.lineno());
            return false;
        }
        //save current line number (just in case of error):
        final int symLineNum = inTokens.lineno();
        Unit unitObj;
        str = readBlockFieldValue(frBlkNum, ++fieldNum, true);
        //process in-units type value:
        if (str == null) { //in-units field not found; set error message
            setErrorMessage("Unable to find in-units field in FIR " + "Blockette at line " + inTokens.lineno());
            return false;
        }

        if (stageObj.input_units == null) { //no previous in-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized in-units field value; set error message
                setErrorMessage(String.format("Invalid in-units type %s  in FIR Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.input_units = units.getUnitObj(); //enter new units value
            inputUnits.add(units);
        }
        str = readBlockFieldValue(frBlkNum, ++fieldNum, true);
        //process out-units type value:
        if (str == null) { //out-units field not found; set error message
            setErrorMessage("Unable to find out-units field in FIR " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.output_units == null) { //no previous out-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized out-units field value; set error message
                setErrorMessage(String.format("Invalid out-units type %s  in FIR Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.output_units = units.getUnitObj(); //enter new units value
        }
        str = readBlockFieldValue(frBlkNum, ++fieldNum);
        //process number of coefficients count:
        if (str == null) { //number of coefficients field not found; set error message
            setErrorMessage("Unable to find number of coefficients field in " + "FIR Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numCoefficients;
        try { //convert string to integer:
            numCoefficients = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of coefficients field (" + str + ") in FIR Blockette at line " + inTokens.lineno());
            return false;
        }
        //process coefficients:
        final boolean symFlag;
        final int numNumerators;
        if (symStr.equals("A")) { //no symmetry
            symFlag = false;
            numNumerators = numCoefficients;
        } else if (symStr.equals("B")) { //odd number of coefficients, with symmetry
            symFlag = true;
            numNumerators = (numCoefficients * 2) - 1;
        } else if (symStr.equals("C")) { //even number of coefficients, with symmetry
            symFlag = true;
            numNumerators = numCoefficients * 2;
        } else { //unexpected symmetry code; set error message
            setErrorMessage("Invalid symmetry code field (" + symStr + ") in FIR Blockette at line " + symLineNum);
            return false;
        }
        //create array of numerators:
        final CoefficientErrored[] numeratorsArr = new CoefficientErrored[numNumerators];
        ++fieldNum; //increment field number
        int i, j;
        BlockFieldSpec bfSpecObj;
        float fVal;
        for (i = 0; i < numCoefficients; ++i) { //for each coefficient expected
            bfSpecObj = readNextBlockFieldNums(frBlkNum, fieldNum);
            //read and check "B###F##" string; read and discard integer
            // index value; then read floating-point value string:
            if (bfSpecObj == null || bfSpecObj.blockNum != frBlkNum || bfSpecObj.fieldNum != fieldNum || readNextTokenString() == null || (str = readNextTokenString()) == null) { //error reading field; set error message
                setErrorMessage("Unable to find coefficient field in FIR " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            try { //convert string to floating-point value:
                fVal = Float.parseFloat(str);
            } catch (NumberFormatException ex) { //error converting string to floating-point value; set message
                setErrorMessage("Error parsing coefficient value (" + str + ") in FIR Blockette at line " + inTokens.lineno());
                return false;
            }
            //enter coefficient value:
            numeratorsArr[i] = new CoefficientErrored(fVal, (float) 0.0);
            if (symFlag && (j = numNumerators - i - 1) != i) { //symmetry is active and mirror index is not same value
                numeratorsArr[j] = numeratorsArr[i]; //enter duplicate value
            }
        }
        //enter new filter into stage:
        if (stageObj.filters == null || stageObj.filters.length <= 0) { //no previous filter entered
            //create and configure Coefficients filter:
            final Filter filterObj = new Filter();
            filterObj.coeff_filter(new CoefficientFilter(numeratorsArr, new CoefficientErrored[0]));
            //create and enter array containing filter item:
            stageObj.filters = new Filter[] { filterObj };
        } else { //previous filter entered; append new values
            try {
                final CoefficientFilter coeffFilterObj = //get last filter object
                        stageObj.filters[stageObj.filters.length - 1].coeff_filter();
                //build Vector containing old and new numerator values,
                // then convert it back to an array and enter into filter:
                final ArrayList<CoefficientErrored> vec = new ArrayList<>(Arrays.asList(coeffFilterObj.numerator));
                vec.addAll(Arrays.asList(numeratorsArr));
                coeffFilterObj.numerator = (vec.toArray(new CoefficientErrored[vec.size()]));
            } catch (Exception ex) {
                setErrorMessage("Internal error:  Exception extending arrays " + "in 'readFIR()':  " + ex);
                return false;
            }
        }
        return true;
    }

    //Reads Response Polynomial Blockette.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    // @param dictFlag true for dictionary blockette, false for not.
    private boolean readPolynomial(BlockFieldSpec initBfSpecObj, boolean dictFlag) {
        //setup dictionary (42) or normal (62) block number:
        final int rpBlkNum = dictFlag ? 42 : 62;
        //dictionary entry type (42) starts with field 5;
        // normal type (62) starts with field 3:
        int fieldNum = dictFlag ? 5 : 3;

        final String typeStr;
        //check block/field #s and get "Transfer function type" value:
        if (initBfSpecObj.blockNum != rpBlkNum || initBfSpecObj.fieldNum != fieldNum || (typeStr = readBasicDataValue()) == null) { //type field not found; set error message
            setErrorMessage("Unable to find transfer-function type field in " + "Polynomial Blockette at line " + inTokens.lineno());
            return false;
        }
        Stage stageObj = null;
        String str;
        if (!dictFlag) { //not a "dictionary" type entry; get "Stage sequence number"
            str = readBlockFieldValue(rpBlkNum, ++fieldNum);
            if (str == null) { //seq # field not found; set error message
                setErrorMessage("Unable to find stage seq# in Polynomial " + "Blockette at line " + inTokens.lineno());
                return false;
            }
            final int seqNum;
            try { //convert stage seq# string to integer:
                seqNum = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse stage seq# (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum && seqNum != curStageSeqNum + 1 && seqNum != 0) { //bad stage sequence number; set error message
                setErrorMessage("Stage seq# (" + seqNum + ") out of order in " + "Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
            if (seqNum != curStageSeqNum || (seqNum == 0 && currentStagesList.size() <= 0)) { //stage sequence number is new or is very first stage
                //create new Stage object and add to list:
                stageObj = createAddNewStageObj(seqNum, true);
            }
            ++fieldNum; //increment number for next field
            initBfSpecObj = readNextBlockFieldNums(rpBlkNum, fieldNum);
            //read in next field:
            if (initBfSpecObj == null) { //error reading next field; set error message
                setErrorMessage("Unable to find in-units field in " + "Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        if (stageObj == null) { //stage sequence number is not new; use latest stage
            final int len = currentStagesList.size();
            if (len <= 0) //get current # of stages
            { //no current stages (shouldn't happen); set error message
                setErrorMessage("Internal error:  Vector of stages empty " + "in 'readPolynomial()'");
                return false;
            }
            //setup handle to last Stage in Vector:
            stageObj = currentStagesList.get(len - 1);
        }
        final TransferType typeObj;
        //process filter/stage type:
        if (typeStr.equals("P")) {
            typeObj = TransferType.TTPOLYNOMIAL;
        } else { //unrecognized value; set error message
            setErrorMessage("Invalid transfer-function type (\"" + typeStr + "\") in Polynomial Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.type != null && !typeObj.equals(stageObj.type)) { //different stage type set previously
            setErrorMessage("Different transfer-function type (\"" + typeStr + "\") in same stage in Polynomial Blockette at line " + inTokens.lineno());
            return false;
        }
        stageObj.type = typeObj; //save new transfer/response type
        Unit unitObj;
        //process in-units type value:
        if (initBfSpecObj.blockNum != rpBlkNum || initBfSpecObj.fieldNum != fieldNum || (str = readBasicDataValue(true)) == null) { //in-units field not found; set error message
            setErrorMessage("Unable to find in-units field in Polynomial " + "Blockette at line " + inTokens.lineno());
            return false;
        }

        if (stageObj.input_units == null) { //no previous in-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized in-units field value; set error message
                setErrorMessage(String.format("Invalid in-units type %s  in Polynomial Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.input_units = units.getUnitObj(); //enter new units value
            inputUnits.add(units);
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum, true);
        //process out-units type value:
        if (str == null) { //out-units field not found; set error message
            setErrorMessage("Unable to find out-units field in Polynomial " + "Blockette at line " + inTokens.lineno());
            return false;
        }
        if (stageObj.output_units == null) { //no previous out-units
            ResponseUnits units = UnitsParser.parseResponseString(str);
            if (units == null) { //unrecognized out-units field value; set error message
                setErrorMessage(String.format("Invalid out-units type %s  in Polynomial Blockette at line %d", str, inTokens.lineno()));
                return false;
            }
            stageObj.output_units = units.getUnitObj(); //enter new units value
        }
        //create filter object for polynomial blockette:
        final PolynomialFilter polyFilterObj = new PolynomialFilter();
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Polynomial Approximation Type field:
        if (str == null) { //Polynomial Approximation Type field not found; set error message
            setErrorMessage("Unable to find Polynomial Approximation Type field" + " in Polynomial Blockette at line " + inTokens.lineno());
            return false;
        }
        polyFilterObj.approximation_type = str; //set value
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Valid Frequency Units field (if available):
        if (str != null) {
            polyFilterObj.frequency_units = str; //set value
        } else //if field not found then put in empty string
        {
            polyFilterObj.frequency_units = StringUtils.EMPTY;
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Lower Valid Frequency Bound field (if available):
        if (str != null) { //field found
            try { //convert string to float:
                polyFilterObj.lower_freq_bound = Float.parseFloat(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse Lower Valid Frequency Bound " + "field (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        } else //field not found; put in zero
        {
            polyFilterObj.lower_freq_bound = 0.0f;
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Upper Valid Frequency Bound field (if available):
        if (str != null) { //field found
            try { //convert string to float:
                polyFilterObj.upper_freq_bound = Float.parseFloat(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse Upper Valid Frequency Bound " + "field (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        } else //field not found; put in zero
        {
            polyFilterObj.upper_freq_bound = 0.0f;
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Lower Bound of Approximation field (if available):
        if (str != null) { //field found
            try { //convert string to float:
                polyFilterObj.lower_approx_bound = Float.parseFloat(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse Lower Bound of Approximation " + "field (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        } else //field not found; put in zero
        {
            polyFilterObj.lower_approx_bound = 0.0f;
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Upper Bound of Approximation field (if available):
        if (str != null) { //field found
            try { //convert string to float:
                polyFilterObj.upper_approx_bound = Float.parseFloat(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse Upper Bound of Approximation " + "field (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        } else //field not found; put in zero
        {
            polyFilterObj.upper_approx_bound = 0.0f;
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process Maximum Absolute Error field (if available):
        if (str != null) { //field found
            try { //convert string to float:
                polyFilterObj.max_abs_error = Float.parseFloat(str);
            } catch (NumberFormatException ex) {
                setErrorMessage("Unable to parse Maximum Absolute Error " + "field (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        } else //field not found; put in zero
        {
            polyFilterObj.max_abs_error = 0.0f;
        }
        str = readBlockFieldValue(rpBlkNum, ++fieldNum);
        //process number of coefficients count:
        if (str == null) { //number of coefficients field not found; set error message
            setErrorMessage("Unable to find number of coefficients field in " + "Polynomial Blockette at line " + inTokens.lineno());
            return false;
        }
        final int numCoefficients;
        try { //convert string to integer:
            numCoefficients = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse number of coefficients field (" + str + ") in Polynomial Blockette at line " + inTokens.lineno());
            return false;
        }
        BlockFieldSpec bfSpecObj;
        int i;
        final CoefficientErrored[] coeffErrValsArr = new CoefficientErrored[numCoefficients];
        ++fieldNum; //move to next field number
        for (i = 0; i < numCoefficients; ++i) { //for each line of coefficients items
            bfSpecObj = readNextBlockFieldNums(rpBlkNum, fieldNum);
            //read and check "B###F##" string; read and discard integer
            // index value; then read 2 floating-point values:
            if (bfSpecObj == null
                    || bfSpecObj.blockNum != rpBlkNum
                    || bfSpecObj.fieldNum != fieldNum
                    || bfSpecObj.endFieldNum != fieldNum + 1
                    || readNextTokenString() == null
                    || (coeffErrValsArr[i] = readCoefficientErrored()) == null) {
                setErrorMessage("Error parsing coefficients data items in " + "Polynomial Blockette at line " + inTokens.lineno());
                return false;
            }
        }
        //enter array of parsed coefficient/error values:
        polyFilterObj.coeff_err_values = coeffErrValsArr;
        //enter new filter into stage:
        if (stageObj.filters == null || stageObj.filters.length <= 0) { //no previous filter entered; enter polynomial filter
            final Filter filterObj = new Filter();
            filterObj.polynomial_filter(polyFilterObj);
            //create and enter array containing filter item:
            stageObj.filters = new Filter[] { filterObj };
        } else { //previous filter entered
            setErrorMessage("More than one instance of Polynomial Blockette " + "found at line " + inTokens.lineno());
            return false;
        }
        return true;
    }

    //Reads Response Reference Blockette and associated dictionary
    // blockettes.
    // @param initBfSpecObj the initial block/field number read in for
    // the response.
    private boolean readReference(BlockFieldSpec initBfSpecObj) {
        RefInfoSpec refInfoObj = readReferenceInfo(initBfSpecObj);
        //read first reference information fields:
        if (refInfoObj == null) {
            return false; //if error then return false (err msg already set)
        }
        final int totalStageCount = refInfoObj.numStages; //save # of stages
        int stageNum = refInfoObj.stageNum; //set initial stage #
        int sCount = 0; //initialize stage counter
        int numResponses, rCount;
        BlockFieldSpec bfSpecObj;
        while (true) { //for each stage in reference blockette
            if (stageNum != 0) //allow stage#==0 (for now)
            { //stage number not zero
                if (stageNum != curStageSeqNum && stageNum != curStageSeqNum + 1) { //bad stage sequence number; set error message
                    setErrorMessage("Stage seq# (" + stageNum + ") out of order in " + "Reference Blockette at line " + inTokens.lineno());
                    return false;
                }
                if (stageNum > curStageSeqNum) { //stage seq# is new; create new Stage obj and add to Vector
                    currentStagesList.add(new Stage(null, null, null, null, null, null, null));
                    curStageSeqNum = stageNum; //save new stage sequence number
                }
            }
            numResponses = refInfoObj.numResps; //set # of responses
            for (rCount = 0; rCount < numResponses; ++rCount) { //for each response in stage
                bfSpecObj = readNextBlockFieldNums();
                if (bfSpecObj == null) { //error fetching first "B###F##" item
                    setErrorMessage("Unable to read any valid response data in " + "Reference Blockette at line " + inTokens.lineno());
                    return false;
                }
                if (stageNum == 0 && bfSpecObj.blockNum != 48) { //stage seq# is 0 but response type is not Sensitivity/Gain
                    setErrorMessage("Stage seq# 0 but not Sensitivity/Gain " + "blockette (" + bfSpecObj.blockNum + ") in Reference Blockette at line " + inTokens.lineno());
                    return false;
                }
                switch (bfSpecObj.blockNum) {
                case 43: //Response (Poles & Zeros) Blockette
                    if (!readPolesZeros(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 44: //Response (Coefficients) Blockette
                    if (!readCoefficients(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 45: //Response List Blockette
                    if (!readList(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 46: //Generic Response Blockette
                    if (!readGeneric(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 47: //Decimation Blockette
                    if (!readDecimation(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 48: //Channel Sensitivity/Gain Blockette
                    if (!readSensGain(bfSpecObj, true, stageNum)) //read blkt data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 41: //FIR Response Blockette
                    if (!readFIR(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                case 42: //Polynomial Response Blockette
                    if (!readPolynomial(bfSpecObj, true)) //read blockette data
                    {
                        return false; //if error then exit method
                    }
                    break;

                default: //unexpected blockette; set error message
                    setErrorMessage("Unexpected Blockette type (" + bfSpecObj.blockNum + ")");
                    return false;
                }
            }
            if (++sCount >= totalStageCount) {
                break; //if through all stages then exit loop (and method)
            } //read next reference information fields:
            refInfoObj = readReferenceInfo(readNextBlockFieldNums());
            if (refInfoObj == null) {
                return false; //if error then return false (err msg already set)
            }
            if (refInfoObj.numStages != totalStageCount) { //#-of-stages value changed; set error message
                setErrorMessage("Non-matching #-of-stages value (" + refInfoObj.numStages + ") in Reference Blockette at line " + inTokens.lineno());
                return false;
            }
            stageNum = refInfoObj.stageNum; //set new stage #
        }
        return true;
    }

    //@return reference block values; or null if error.
    // @param bfSpecObj the block and field number from the pre-read
    // "B###F##" string for the reference information.
    private RefInfoSpec readReferenceInfo(BlockFieldSpec bfSpecObj) {
        String str;
        if (bfSpecObj == null || bfSpecObj.blockNum != 60 || bfSpecObj.fieldNum != 3 || (str = readBasicDataValue()) == null) { //# of stages value not found; set error message
            setErrorMessage("Unable to find # of stages field in Reference " + "Blockette at line " + inTokens.lineno());
            return null;
        }
        final int numStages;
        try { //convert # of stages string to integer:
            numStages = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse # of stages (" + str + ") in Reference Blockette at line " + inTokens.lineno());
            return null;
        }
        if (numStages <= 0) { //# of stages value is zero or less; set error message
            setErrorMessage("Bad # of stages value (" + numStages + ") in Reference Blockette at line " + inTokens.lineno());
            return null;
        }
        str = readBlockFieldValue(60, 4);
        if (str == null) { //stage number not found; set error message
            setErrorMessage("Unable to find stage number in Reference " + "Blockette at line " + inTokens.lineno());
            return null;
        }
        final int stageNum;
        try { //convert stage number string to integer:
            stageNum = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse stage number (" + str + ") in Reference Blockette at line " + inTokens.lineno());
            return null;
        }
        str = readBlockFieldValue(60, 5);
        if (str == null) { //# of responses field not found; set error message
            setErrorMessage("Unable to find # of responses field in Reference " + "Blockette at line " + inTokens.lineno());
            return null;
        }
        final int numResps;
        try { //convert # of responses value string to integer:
            numResps = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            setErrorMessage("Unable to parse # of responses value (" + str + ") in Reference Blockette at line " + inTokens.lineno());
            return null;
        }
        if (numResps <= 0) { //# of responses value is zero or less; set error message
            setErrorMessage("Bad # of stages value (" + numResps + ") in Reference Blockette at line " + inTokens.lineno());
            return null;
        }
        //create and return block of reference information:
        return new RefInfoSpec(numStages, stageNum, numResps);
    }

    /**
     * @return the given block/field value or null if the next item cannot be be
     *         found, its numbers do not match, or if an error occurs.
     * @param blockNum
     *            block number
     * @param fieldNum
     *            field number
     * @param lineFlag
     *            if true then all the rest of the tokens on the line are also
     *            returned; if false then only the next token is returned and
     *            the rest on the line are read in and discarded.
     */
    public String readBlockFieldValue(int blockNum, int fieldNum, boolean lineFlag) {
        BlockFieldSpec bfSpecObj = readNextBlockFieldNums();
        if (bfSpecObj == null) //fetch "B###F##"
        {
            return null; //if not found then return error
        }
        while (bfSpecObj.blockNum <= blockNum && bfSpecObj.fieldNum != fieldNum) {
            //field numbers fetched OK and next sequential field was found:
            clearToEndOfLine(); //skip rest of data on line
            bfSpecObj = readNextBlockFieldNums();
            //read in next field:
            if (bfSpecObj == null) //fetch "B###F##"
            {
                return null; //if not found then return error
            }
        }

        if (bfSpecObj.blockNum != blockNum || bfSpecObj.fieldNum != fieldNum) {
            return null; //if numbers don't match then return error
        }
        return readBasicDataValue(lineFlag); //fetch and return value
    }

    /**
     * @return the given block/field number item and returns its associated
     *         value. Returns null if the next item cannot be be found, its
     *         numbers do not match, or if an error occurs.
     * @param blockNum
     *            block number
     * @param fieldNum
     *            field number
     */
    public String readBlockFieldValue(int blockNum, int fieldNum) {
        return readBlockFieldValue(blockNum, fieldNum, false);
    }

    /**
     * @return the block and field numbers of the next "B###F##..." string or
     *         null if valid string not found or error. Any leading blank for
     *         comment lines are skipped, but the next word token must be a
     *         valid "B###F##..." string or null is returned.
     */
    public BlockFieldSpec readNextBlockFieldNums() {
        int tType;
        try { //skip any blank (or comment) lines:
            while ((tType = inTokens.nextToken()) == RespTokenizer.TT_EOL) {
                ;
            }
            //if word token then fetch, parse and return "B###F##..." string:
            if (tType == RespTokenizer.TT_WORD) {
                return parseBlockFieldNums(inTokens.getTokenString());
            }
        } catch (IOException ex) { //error reading token; set error message
            setErrorMessage("Error reading from input file: " + ex);
        }
        return null;
    }

    /**
     * Scans until a block with the specified values is found.
     *
     * @param blockNum
     *            block number to find.
     * @param fieldNum
     *            field number to find.
     * @return the block and field numbers of the for requested block, or null
     *         if valid string not found or error.
     */
    public BlockFieldSpec readNextBlockFieldNums(int blockNum, int fieldNum) {
        BlockFieldSpec bfSpecObj;
        while (true) { //for each block to be scanned; fetch next block
            bfSpecObj = readNextBlockFieldNums();
            if (bfSpecObj == null) {
                return null; //if not found then return error
            }
            if (bfSpecObj.blockNum == blockNum) { //block number matches; check field number
                if (bfSpecObj.fieldNum == fieldNum) {
                    return bfSpecObj; //if field # matches then return object
                }
            } else if (bfSpecObj.blockNum > blockNum) {
                return null; //if beyond block # then return null
            }
            clearToEndOfLine(); //skip rest of data on line
        }
    }

    /**
     * Scans until a block matching one of the specified set of values is found.
     *
     * @param blockNum1
     *            first block number to find.
     * @param fieldNum1
     *            first field number to find.
     * @param blockNum2
     *            second block number to find.
     * @param fieldNum2
     *            second field number to find.
     * @return The block and field numbers for the requested block, or null if
     *         valid string not found or error.
     */
    public BlockFieldSpec readNextBlockFieldNums(int blockNum1, int fieldNum1, int blockNum2, int fieldNum2) {
        BlockFieldSpec bfSpecObj;
        while (true) { //for each block to be scanned; fetch next block
            bfSpecObj = readNextBlockFieldNums();
            if (bfSpecObj == null) {
                return null; //if not found then return error
            }
            if ((bfSpecObj.blockNum == blockNum1) && (bfSpecObj.fieldNum == fieldNum1)) {
                return bfSpecObj; //if field # matches then return object
            }
            if (bfSpecObj.blockNum == blockNum2) { //block number matches; check field number
                if (bfSpecObj.fieldNum == fieldNum2) {
                    return bfSpecObj; //if field # matches then return object
                }
            } else if (bfSpecObj.blockNum > blockNum1 && bfSpecObj.blockNum > blockNum2) { //beyond both block numbers
                return null; //no match
            }
            clearToEndOfLine(); //skip rest of data on line
        }
    }

    /**
     * @return the next token or null if token not found or error.
     */
    public String readNextTokenString() {
        try {
            if (inTokens.nextToken() != RespTokenizer.TT_WORD) {
                return null;
            }
        } catch (IOException ex) { //error reading token; set error message
            setErrorMessage("Error reading from input file: " + ex);
        }
        return inTokens.getTokenString();
    }

    /**
     * @return the token after the next "xxx:" token or null if token not found
     *         or error.
     * @param lineFlag
     *            if true then all the rest of the tokens on the line are also
     *            returned; if false then only the next token is returned and
     *            the rest on the line are read in and discarded.
     */
    public String readBasicDataValue(boolean lineFlag) {
        String str;
        try {
            while (inTokens.nextToken() == RespTokenizer.TT_WORD) { //for each word token found; check for "xxx:"
                if (inTokens.getNonNullTokenString().endsWith(":")) { //leading "xxx:" token found
                    if (inTokens.nextToken() == RespTokenizer.TT_WORD) { //next word token found
                        str = inTokens.getTokenString(); //get token string
                        //read in any remaining word tokens on line:
                        while (inTokens.nextToken() == RespTokenizer.TT_WORD) { //for each token remaining on line
                            if (lineFlag) //if flag then append token
                            {
                                str += " " + inTokens.getNonNullTokenString();
                            }
                        }
                        return str; //return token string
                    } //if word token after "xxx:" not found then
                    return null; //return error
                }
            }
        } catch (IOException ex) { //error reading token; set error message
            setErrorMessage("Error reading from input file: " + ex);
        }
        return null;
    }

    /**
     * @return the token after the next "xxx:" token or null if token not found
     *         or error. The single token is returned and the rest on the line
     *         are read in and discarded.
     */
    public String readBasicDataValue() {
        return readBasicDataValue(false);
    }

    /**
     * @return four floating-point values; returns null if unexpected input or
     *         error.
     */
    public ComplexNumberErrored readComplexNumberErrored() {
        final float[] floatArr = new float[4];
        int tType;
        for (int p = 0; p < 4; ++p) { //for each floating-point value converted
            try { //skip any blank (or comment) lines:
                while ((tType = inTokens.nextToken()) == RespTokenizer.TT_EOL) {
                    ;
                }
            } catch (IOException ex) { //error reading token; set error message
                setErrorMessage("Error reading from input file: " + ex);
                return null;
            }
            if (tType != RespTokenizer.TT_WORD) {
                return null; //if not word token then return error
            }
            try { //convert string to floating-point number:
                floatArr[p] = Float.parseFloat(inTokens.getTokenString());
            } catch (NumberFormatException ex) { //error converting string to number
                return null;
            }
        }
        return new ComplexNumberErrored( //create and return object
                                        floatArr[0],
                                        floatArr[2],
                                        floatArr[1],
                                        floatArr[3]);
    }

    /**
     * @return two floating-point values; returns null if unexpected input or
     *         error.
     */
    public CoefficientErrored readCoefficientErrored() {
        final float[] floatArr = new float[2];
        int tType;
        for (int p = 0; p < 2; ++p) { //for each floating-point value converted
            try { //skip any blank (or comment) lines:
                while ((tType = inTokens.nextToken()) == RespTokenizer.TT_EOL) {
                    ;
                }
            } catch (IOException ex) { //error reading token; set error message
                setErrorMessage("Error reading from input file: " + ex);
                return null;
            }
            if (tType != RespTokenizer.TT_WORD) {
                return null; //if not word token then return error
            }
            try { //convert string to floating-point number:
                floatArr[p] = Float.parseFloat(inTokens.getTokenString());
            } catch (NumberFormatException ex) { //error converting string to number
                return null;
            }
        }
        //create and return object:
        return new CoefficientErrored(floatArr[0], floatArr[1]);
    }

    /**
     * Reads a response list item (freq, amp & phase).
     *
     * @return A new 'RespListItem' object, or null if unexpected input or
     *         error.
     */
    public RespListItem readRespListItem() {
        try {
            int tType; //skip any blank (or comment) lines:
            while ((tType = inTokens.nextToken()) == RespTokenizer.TT_EOL) {
                ;
            }
            final float[] floatArr = new float[6];
            int p; //read in 5 or 6 values:
            for (p = 0; p < 6; ++p) { //for each floating-point value converted
                if (tType != RespTokenizer.TT_WORD) {
                    return null; //if not word token then return error
                }
                try { //convert string to floating-point number:
                    floatArr[p] = Float.parseFloat(inTokens.getTokenString());
                } catch (NumberFormatException ex) { //error converting string to number
                    return null;
                }
                tType = inTokens.nextToken();
                if (tType == RespTokenizer.TT_EOL) {
                    break; //if end-of-line then exit loop
                }
            }
            if (p < 4) //if not enough values then
            {
                return null; //return error
            } //use last 5 items read in to create list-item object
              // (skip value from "index" column if present):
            return new RespListItem(floatArr[p - 4], floatArr[p - 3], floatArr[p - 2], floatArr[p - 1], floatArr[p]);
        } catch (IOException ex) { //error reading token; set error message
            setErrorMessage("Error reading from input file: " + ex);
            return null;
        }
    }

    /**
     * Reads in and discards all tokens remaining on the current line.
     *
     * @return true if successful, false if an I/O error is detected (in which
     *         case an error message will be set).
     */
    public boolean clearToEndOfLine() {
        try { //read until end of line or file:
            while (inTokens.nextToken() == RespTokenizer.TT_WORD) {
                ;
            }
        } catch (IOException ex) { //error reading token; set error message
            setErrorMessage("Error reading from input file: " + ex);
            return false;
        }
        return true;
    }

    /**
     * Parses "B###F##..." string into block and field numbers.
     *
     * @param str
     *            string
     * @return block field spec
     */
    public BlockFieldSpec parseBlockFieldNums(String str) {
        final int len;
        if (str == null || (len = str.length()) < 7 || len > 10 || str.charAt(0) != 'B') { //string data not valid or does not start with 'B'
            return null;
        }
        int blockNum, fieldNum, endFieldNum;
        try { //parse block number:
            blockNum = Integer.parseInt(str.substring(1, 4));
        } catch (NumberFormatException ex) { //error parsing number
            return null;
        }
        if (str.charAt(4) != 'F') //if 'F' not found then
        {
            return null; //return error
        }
        try { //parse (first) field number:
            fieldNum = Integer.parseInt(str.substring(5, 7));
        } catch (NumberFormatException ex) { //error parsing number
            return null;
        }
        if (len > 7) { //more data available; must have end field number
            if (str.charAt(7) != '-' || len < 10) //if no dash or too short then
            {
                return null; //return error
            }
            try { //parse end field number:
                endFieldNum = Integer.parseInt(str.substring(8, 10));
            } catch (NumberFormatException ex) { //error parsing number
                return null;
            }
        } else {
            endFieldNum = -1;
        }
        //create and return 'BlockFieldSpec' object:
        return new BlockFieldSpec(blockNum, fieldNum, endFieldNum);
    }

    //Enters error message (if none previously entered).
    protected void setErrorMessage(String str) {
        if (errorMessage == null) //if no previous error then
        {
            errorMessage = str; //set error message
        }
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

    //Enters info message.
    protected void setInfoMessage(String str) {
        infoMessage = str; //set info message
    }

    /**
     * @return true if an info message has been entered. The info message may be
     *         fetched via the 'getInfoMessage()' method.
     */
    public boolean getInfoFlag() {
        return (infoMessage != null);
    }

    /**
     * @return the info message string (or 'No message' if none).
     */
    public String getInfoMessage() {
        return (infoMessage != null) ? infoMessage : "No message";
    }

    /**
     * Clears the info message string.
     */
    public void clearInfoMessage() {
        infoMessage = null;
    }

    /**
     * @return the input file name for this parser.
     */
    public String getInputFileName() {
        return inputFileName;
    }

    /**
     * @return the current-sensitivity object.
     */
    public Sensitivity getCurSensitivityObj() {
        return curSensitivityObj;
    }

    /**
     * Class BlockFieldSpec holds block and field numbers.
     */
    class BlockFieldSpec {

        public final int blockNum; //block number
        public final int fieldNum; //(first) field number
        public final int endFieldNum; //end field number (-1 if none)

        public BlockFieldSpec(int blockNum, int fieldNum, int endFieldNum) {
            this.blockNum = blockNum;
            this.fieldNum = fieldNum;
            this.endFieldNum = endFieldNum;
        }
    }

    /**
     * Class RefInfoSpec holds Reference Response block information.
     */
    class RefInfoSpec {

        public final int numStages; //number of stages
        public final int stageNum; //stage number
        public final int numResps; //number of responses

        public RefInfoSpec(int numStages, int stageNum, int numResps) {
            this.numStages = numStages;
            this.stageNum = stageNum;
            this.numResps = numResps;
        }
    }

    /**
     * Class RespListItem holds a set of frequency, amplitude and phase values,
     * as found in the Response List Blockette.
     */
    class RespListItem {

        public final float freq; //frequency
        public final float amp, ampError; //amplitude value and error
        public final float phase, phaseError; //phae value and error

        public RespListItem(float freq, float amp, float ampError, float phase, float phaseError) {
            this.freq = freq;
            this.amp = amp;
            this.ampError = ampError;
            this.phase = phase;
            this.phaseError = phaseError;
        }
    }
}
