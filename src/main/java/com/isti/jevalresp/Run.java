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
//Run.java:  Entry point and managing module for 'JEvalResp'.
//
//  12/18/2001 -- [ET]  Version 0.92Beta:  Initial release version.
//    3/1/2002 -- [ET]  Version 0.93Beta:  Added "net" support (including
//                      'propsFile' parameter) for fetching responses from
//                      FISSURES servers.
//   4/30/2002 -- [ET]  Version 0.94Beta:  Changed source code directories
//                      from using "netsrc" to "filesrc" ("net" support
//                      becomes default; file-only version uses "filesrc"
//                      classes); merged "RunBase.java" into this file and
//                      improved modularization; added 'RunExt' class.
//    5/2/2002 -- [ET]  Version 0.95Beta:  Changed 'generateResponses()'
//                      methods to have their 'RespCallback' object be
//                      passed in as a parameter; other internal changes.
//   5/14/2002 -- [ET]  Version 0.96Beta:  Changes to the 'RunExt' class.
//   5/15/2002 -- [ET]  Version 0.961Beta:  Added verbose-mode message to
//                      show what output files were generated.
//   5/22/2002 -- [ET]  Version 0.962Beta:  Added new verbose messages to
//                      indicate number of items retrieved from FISSURES
//                      server and to indicate when instrumentation for
//                      channel object not found on server.
//   5/28/2002 -- [ET]  Version 0.963Beta:  Fixed bug where errors were
//                      sometimes reported twice.
//   6/10/2002 -- [ET]  Version 0.97Beta:  Added support for gain-only
//                      stages and Generic Response Blockettes (#56);
//                      modified how response delay, correction and sample-
//                      interval values are calculated; filename ('-f')
//                      parameter now accepts multiple file and directory
//                      names and filenames with wildcards.
//   6/11/2002 -- [ET]  Version 0.971Beta:  Modified help screen item for
//                      filename ('-f') parameter.
//   7/10/2002 -- [ET]  Version 0.972Beta:  Added "ap2" option for response-
//                      type that outputs a single amplitude/phase ("AP.")
//                      file; improved ability to handle date codes with
//                      with more than 3 fractional-second digits (such as
//                      "2001,360,14:50:15.2426"); added "multiOutputFlag"
//                      ('-m') parameter and support to allow multiple
//                      outputs with same "net.sta.loc.cha" code.
//   7/11/2002 -- [ET]  Version 0.973Beta:  Added optional "endYear" ('-ey'),
//                      "endDay" ('-ed') and "endTime" ('-et') parameters
//                      and time-range implementation.
//   7/15/2002 -- [ET]  Version 0.974Beta:  Implemented time-range for
//                      network server access; changes to 'RunExt' module.
//   7/16/2002 -- [ET]  Version 0.975Beta:  Minor change to network-access
//                      debug output (available via '-debug' parameter).
//   7/17/2002 -- [ET]  Version 0.976Beta:  Changed occurrences of
//                      "net.sta.cha.loc" to "net.sta.loc.cha".
//    8/6/2002 -- [ET]  Version 0.977Beta:  Added support for fetching and
//                      displaying end-times for responses; added support
//                      for optional header information in generated output
//                      files ('-h' parameter).
//    8/7/2002 -- [ET]  Version 1.0:  Release version.
//   3/26/2003 -- [KF]  Version 1.01:
//                      Display version on header,
//                      Added 'outputDirectory' parameter.
//    5/6/2003 -- [ET]  Version 1.1:  Added support for using a
//                      Network DataCenter object via a path (like
//                      "edu/iris/dmc/IRIS_NetworkDC"); implemented
//                      using an iterator when fetching all channel-IDs
//                      for a network.
//  10/22/2003 -- [ET]  Version 1.2:  Implemented fix to IIR PZ
//                      transformation (to synchronize with 'evalresp'
//                      version 3.2.22).
//   1/28/2004 -- [ET]  Version 1.21:  Modified to allow FIR blockettes (61)
//                      to have a "Response Name" (F04) field.
//   3/15/2004 -- [SH]  Version 1.22:  Modified RESP file parsing to allow
//                      unneeded fields to be skipped over (allowing SHAPE-
//                      compatible RESP files to be parsed).
//   4/22/2005 -- [ET]  Version 1.27:  Modified to suppress "No matching
//                      response files found" error message when all
//                      matching response files contain errors; modified
//                      to display infinite-number value as "*" in verbose
//                      output; changed how 'calculatedDelay' is computed
//                      (by allowing FIR stages with no coefficients to
//                      affect the 'calculatedDelay' sum) to synchronize
//                      with 'evalresp'; modified to interpret gain object
//                      with '-1' values as equivalent to "no gain object"
//                      (sometimes comes from network server); added support
//                      for "use-delay" and "showInputFlag" parameters;
//                      added support for URLs as input file names;
//                      improved parsing of units in RESP file.
//   5/23/2005 -- [ET]  Version 1.5:  Public release version.
//    6/1/2005 -- [ET]  Version 1.51:  Modified to allow negative values
//                      in gain stages.
//   6/29/2005 -- [ET]  Version 1.52:  Fixed bug where response sensitivity
//                      frequency was not properly selected when no "stage 0
//                      gain" blockette was provided.
//   7/11/2005 -- [ET]  Version 1.53:  Modified to handle List Blockettes
//                      (55) that do not contain an "index" ("i") column
//                      in their data.
//  10/13/2005 -- [ET]  Version 1.54:  Modified to allow decimation and gain
//                      blockettes in stages containing list blockettes;
//                      modified to support JPlotResp ability to use
//                      AMP/PHASE files as input.
//   11/3/2005 -- [ET]  Version 1.55:  Implemented interpolation of
//                      amplitude/phase values from responses containing
//                      List blockettes (55); modified to be able to
//                      successfully parse response files that contain
//                      no "B052F03 Location:" entry; fixed support for
//                      URLs as input file names under UNIX.
//   8/22/2006 -- [ET]  Version 1.56:  Modified to support 'Tesla' units;
//                      modified to make sure that if input units are
//                      'Pascal' or 'Tesla' then output units conversion
//                      must be 'Velocity' or 'Default'.
//   5/25/2007 -- [ET]  Version 1.57:  Modified to check if any FIR
//                      coefficients filter should be normalized to 1.0
//                      at frequency zero and adjust the filter values
//                      if so.
//   7/15/2009 -- [KF]  Version 1.58:  Added 'getConsoleOut()' method.
//  11/10/2009 -- [ET]  Version 1.59:  Added 'RunDirect' class for support
//                      of input and output via method calls for processing
//                      a single response.
//   5/27/2010 -- [ET]  Version 1.594:  Added "use-estimated-delay" alias
//                      for "use-delay" parameter; modified to apply delay
//                      correction to asymmetrical FIR filters (using
//                      estimated delay if 'use-estimated-delay' is given,
//                      otherwise using correction-applied value);
//                      modified text-listing output to show "FIR_ASYM",
//                      "FIR_SYM1" and "FIR_SYM2" (instead of just "FIR");
//                      modified computation of calculated delay so it
//                      ignores stages with no coefficients; added
//                      parameters for unwrapping phase values ('-unwrap')
//                      and for using stage 0 (total) sensitivity in
//                      response calculations ('-ts'); added response-output
//                      ('-r') type "fap"; added verbose-output note for
//                      '-ts' parameter in use and warnings for stage
//                      correction-applied and estimated-delay values
//                      being negative.
//    6/1/2010 -- [ET]  Version 1.595:  Fixed processing of '-stage'
//                      parameter when single stage value is given;
//                      modified verbose output to display only start/stop
//                      stages selected via '-stage' parameter; modified
//                      output-file headers to show response-sensitivity
//                      frequency and A0 normalization factor from first
//                      stage.
//    6/3/2010 -- [ET]  Version 1.596:  Changed "A0" to "NormA0" in
//                      output-file headers.
//    8/6/2010 -- [ET]  Version 1.6:  Updated version number for release.
//   1/27/2012 -- [ET]  Version 1.65:  Added support for network access to
//                      web-services servers; fixed bug where site/location
//                      parameters were not properly processed when finding
//                      local RESP input files.
//   1/30/2012 -- [ET]  Version 1.7:  Updated version number for release.
//    4/3/2012 -- [ET]  Version 1.75:  Added support for specification of
//                      multiple web-services-server URLs; added support
//                      for '--multiSvrFlag' ('-ms') parameter.
//  10/29/2013 -- [ET]  Version 1.76:  Added command-line parameter 'b62_x'
//                      and support for Polynomial Blockette (62); added
//                      support for 'Centigrade' temperature units; modified
//                      processing of phase values with FIR_ASYM filters
//                      (per Gabi Laske); redefined processing of decimation
//                      as correction applied minus calculated delay;
//                      modified to use calculated delay with FIR_ASYM
//                      filters when 'use-estimated-delay' is false;
//                      modified 'calculateResponse()' method to use
//                      calculated delay with FIR_ASYM filters when
//                      'use-estimated-delay' is false.
//   4/11/2014 -- [ET]  Version 1.77:  Updated web-service URLs in '.prop'
//                      files.
//   8/26/2014 -- [ET]  Version 1.78:  Modified to properly handle
//                      location/site value of "--" (meaning location
//                      code empty).
//

package com.isti.jevalresp;

import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

/**
 * Class Run is the entry point and managing module for 'JEvalResp'.
 */
public class Run {
    /** Displayed name of program. */
    public static final String PROGRAM_NAME = "JEvalResp";
    /** Displayed program version # (+"(file)" if files-only version). */
    public static final String VERSION_NUM_STR = "1.80";
    /** Displayed program revision string (name + version #). */
    public static final String REVISION_STR = PROGRAM_NAME + ", Version " + VERSION_NUM_STR;
    /** Strings for the 'outUnitsConv' (-u) parameter. */
    public static final String[] UNIT_CONV_STRS = //def,dis,vel,acc
            OutputGenerator.UNIT_CONV_STRS;
    /** Default index value for 'outUnitsConv' (-u) parameter. */
    public static final int UNIT_CONV_DEFIDX = OutputGenerator.UNIT_CONV_DEFIDX;
    /** Longer versions of strings for the 'outUnitsConv' (-u) parameter. */
    public static final String[] UNIT_CONV_LONGSTRS = OutputGenerator.UNIT_CONV_LONGSTRS;
    /** Strings for 'typeOfSpacing' (-s) parameter. */
    public static final String TYPE_SPACE_STRS[] = { "log", "lin" };
    /** Longer versions of strings for 'typeOfSpacing' (-s) parameter. */
    public static final String TYPE_SPACE_LONGSTRS[] = { "Logarithmic", "Linear" };
    /** Strings for 'responseType' (-r) parameter. */
    public static final String RESP_TYPE_STRS[] = { "ap", "cs", "ap2", "fap" };
    /** Longer versions of strings for 'responseType' (-r) parameter. */
    public static final String RESP_TYPE_LONGSTRS[] = { "Amplitude/Phase", "Complex-Spectra", "Amplitude/Phase2", "fAmplitude/Phase" };
    /** Index value (0) for "ap" response type (separate amp/phase files). */
    public static final int RESP_AP_TYPEIDX = 0;
    /** Index value (1) for "cs" response type (complex-spectra file). */
    public static final int RESP_CS_TYPEIDX = 1;
    /** Index value (2) for "ap2" response type (single amp/phase file). */
    public static final int RESP_AP2_TYPEIDX = 2;
    /** Index value (3) for "fap" response type (single, unwrapped file). */
    public static final int RESP_FAP_TYPEIDX = 3;
    /** Default value for List-blockette interpolation. */
    public static final double INTERP_TENSION_DEFVAL = 1000.0;
    /** String containing leading comment chars for output file headers. */
    public static final String HDR_CMT_STR = "# ";

    protected static final String VER1_STR = "version"; //parameter names for
    protected static final String VER2_STR = "ver"; // version info disp
    //format object for parsing time strings:
    protected static final DateFormat timeFmtObj = new SimpleDateFormat("HH:mm:ss.SSS");
    static {
        timeFmtObj.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    protected String[] staNamesArray = null; //array of station names
    protected String[] chaNamesArray = null; //array of channel names
    protected String[] netNamesArray = null; //array of network names
    protected String[] siteNamesArray = null; //array of location names
    protected Calendar beginCalObj = null; //begin date to match
    protected Calendar endCalObj = null; //end date to match
    protected boolean minimumFrequencyFlag = false; //true if minimum frequency entered

    protected double minFreqValue = 1.0; //minimum frequency value
    protected double maxFreqValue = 1.0; //maximum frequency value
    protected int numberFreqs = 1; //number of frequencies
    protected boolean stdioFlag = false; //true for stdin/stdout I/O
    protected int outUnitsConvIdx = UNIT_CONV_DEFIDX; //units index value
    protected double[] frequenciesArray = null; //array of frequencies
    protected boolean multiOutFlag = false; //true for multi-output
    protected boolean multiSvrFlag = false; //true for multiple web servers
    protected boolean headerFlag = false; //true for header in output file
    protected boolean verboseFlag = false; //true for verbose output
    protected boolean debugFlag = false; //true for debug messages
    protected int startStageNum = 0; //first stage to process
    protected int stopStageNum = 0; //last stage to process
    protected boolean logSpacingFlag = true; //true for log frequency spacing
    protected boolean useDelayFlag = false; //use est delay for phase calc
    protected boolean showInputFlag = false; //show RESP input
    protected boolean listInterpOutFlag = false; //interpolate List output
    protected boolean listInterpInFlag = false; //interpolate List input
    protected double listInterpTension = 0.0; //tension for List interp
    protected boolean unwrapPhaseFlag = false; //unwrap phase values
    protected boolean totalSensitFlag = false; //use stage 0 sensitivity
    protected double b62XValue = 0.0; //sample value for poly blockette
    protected int respTypeIndex = RESP_AP_TYPEIDX; //idx for amp/phase output
    protected String fileNameString = StringUtils.EMPTY; //fname entered
    protected String propsFileString = StringUtils.EMPTY; //svr-props file
    protected File outputDirectory = null; //output directory

    protected int exitStatusValue = 0; //exit status # returned by prog
    protected String errorMessage = null; //generated error message

    /**
     * Get the console output.
     *
     * @return the console output printstream.
     */
    public PrintStream getConsoleOut() {
        return System.err;
    }

    /**
     * Enters error message (if none previously entered).
     *
     * @param str
     *            error message string
     */
    protected void setErrorMessage(String str) {
        if (errorMessage == null) { //if no previous error then
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

    /** @return message string for last error (or 'No error' if none). */
    public String getErrorMessage() {
        return (errorMessage != null) ? errorMessage : "No error";
    }

    /** Clears the error message string. */
    public void clearErrorMessage() {
        errorMessage = null;
    }

    /**
     * Sets the exit status code and message for an error exit from the program.
     *
     * @param statusVal
     *            exit status value to be returned by program.
     * @param errMsgStr
     *            if specified then the error message to be sent to 'stderr'.
     */
    protected void setErr(int statusVal, String errMsgStr) {
        if (exitStatusValue == 0) { //if no previous value then
            exitStatusValue = statusVal; //enter status value
        }
        setErrorMessage(errMsgStr);
    }

    /**
     * @return the exit status value for the program.
     */
    public int getExitStatusValue() {
        return exitStatusValue;
    }

    /**
     * Converts the given comma-separated String of items to an array of Strings
     * with each element containing one item.
     *
     * @param str
     *            string
     * @return An array of strings, or null if an error occurred.
     */
    public static String[] listToStringArray(String str) {
        try {
            return str.split(",");
        } catch (Exception ex) {
        }
        return null; //if any error then return null
    }

    /**
     * Converts the given array of options strings to a displayable string.
     *
     * @param strArr
     *            array of strings
     * @return display string
     */
    public static String optionsArrToString(String[] strArr) {
        final StringBuffer buff = new StringBuffer();
        if (strArr != null) {
            int i = 0;
            while (true) { //add each option string
                buff.append(strArr[i]);
                if (++i >= strArr.length) {
                    break;
                }
                buff.append("|"); //if not last then add separator
            }
        }
        return buff.toString();
    }

    /**
     * Enters the interpreted value of the given 'time' string into the given
     * Calendar object. Any current hours/minutes/seconds values in the Calendar
     * object are overwritten.
     *
     * @param timeStr
     *            a time string value in "HH:MM:SS", "HH:MM" or "HH" format.
     * @param calObj
     *            a Calendar object to be modified.
     * @return true if successful, false if error in 'timeStr' format or any
     *         null parameters.
     */
    public static boolean addTimeToCalendar(Calendar calObj, String timeStr) {
        if (calObj == null || timeStr == null) {
            return false; //if null parameter then return error
        }
        int p = timeStr.indexOf(':');
        if (p < 0) { //if only hour value then
            timeStr += ":00:00.000"; //add ':MM:SS.SSS' before parsing
        } else if (timeStr.indexOf(':', p + 1) < 0) { //if only 'HH:MM' value then
            timeStr += ":00.000"; //add ':SS.SSS' before parsing
        } else if (timeStr.indexOf('.', p + 1) < 0) { //if only 'HH:MM:SS' value then
            timeStr += ".000"; //add '.SSS' before parsing
        }
        Date dateObj;
        try { //parse time string into a Date object:
            dateObj = timeFmtObj.parse(timeStr);
        } catch (ParseException ex) {
            dateObj = null;
        }
        if (dateObj == null) {
            return false; //if error parsing time string then return error
        }
        //create local Calendar object with time value in it:
        final Calendar dateCalObj = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        dateCalObj.setTime(dateObj); //enter parsed time value
        //put new time value into given Calendar object:
        calObj.set(Calendar.HOUR_OF_DAY, dateCalObj.get(Calendar.HOUR_OF_DAY));
        calObj.set(Calendar.MINUTE, dateCalObj.get(Calendar.MINUTE));
        calObj.set(Calendar.SECOND, dateCalObj.get(Calendar.SECOND));
        return true;
    }

}
