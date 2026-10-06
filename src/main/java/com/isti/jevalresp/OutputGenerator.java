/*-
 * #%L
 * Seismic Response Processing Module
 *  LLNL-CODE-856351
 *  This work was performed under the auspices of the U.S. Department of Energy
 *  by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
 * %%
 * Copyright (C) 2025 Lawrence Livermore National Laboratory
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
//OutputGenerator.java:  Generates 'evalresp'-style output from a FISSURES
//                       'Response' object.
//
//  12/19/2001 -- [ET]  Initial release version.
//   4/30/2002 -- [ET]  Added 'UNIT_CONV_LONGSTRS[]' array.
//    6/6/2002 -- [ET]  Modified how response delay, correction and
//                      sample-interval values are calculated.
//    8/6/2002 -- [ET]  Implemented support for optional header information
//                      in generated output files; removed unnecessary
//                      import; added 'getOutputHeaderString()' method.
//   3/26/2003 -- [KF]  Display version on header,
//                      added 'outputDirectory' parameter.
//  10/22/2003 -- [ET]  Implemented fix to 'iirPzTrans()' method of
//                      changing '+' operations to '-' in 4 places
//                      to synchronize with 'evalresp' version 3.2.22.
//    3/1/2005 -- [ET]  Changed how 'calculatedDelay' is computed in
//                      'calcIntvlDelayCorrValues()' method (by allowing
//                      FIR stages with no coefficients to affect the
//                      'calculatedDelay' sum).
//    3/7/2005 -- [ET]  Modified to interpret gain object with '-1' values
//                      as equivalent to "no gain object" (sometimes comes
//                      from network server); added 'anyAmpsNotPositive()'
//                      method; added optional 'useDelayFlag' parameter
//                      to constructor and 'calculateResponse()' method
//                      to support using estimated delay in phase
//                      calculations.
//   3/29/2005 -- [ET]  Modified to calculate and save separate response
//                      results for each stage in the response to support
//                      the fetching of separate amplitude/phase values
//                      for each stage; added 'getAllStagesAmpPhaseArrays()'
//                      and 'allStagesAnyAmpsNotPositive()' methods.
//    4/1/2005 -- [ET]  Added optional 'showInputFlag' parameter to
//                      constructor and 'calculateResponse()' method.
//   4/20/2005 -- [ET]  Modified 'getOutputHeaderString()' method to use
//                      'sepStr' parameter for stages and use-delay items.
//   5/25/2005 -- [ET]  Modified to use RespUtils 'isGainObjValid()' and
//                      'isSensObjValid()' methods to allow for negative
//                      gain values while still interpreting a gain object
//                      with '-1' values as equivalent to "no gain object".
//   6/29/2005 -- [ET]  Fixed bug in 'normalizeResponse()' where response
//                      sensitivity frequency was not properly selected
//                      when no "stage 0 gain" blockette was provided
//                      (fix was to check "gainObj.frequency!=0" in
//                      "find last gain with non-zero freq" loop).
//   9/23/2005 -- [ET]  Added constructors with "AmpPhaseBlk []" parameter;
//                      modified 'getAmpPhaseArray()' and other methods to
//                      return amp/phase data if entered regardless of
//                      whether or not 'cSpectraArray' exists.
//  10/13/2005 -- [ET]  Modified 'checkResponse()' method to allow
//                      decimation and gain blockettes in stages
//                      containing list blockettes.
//   11/1/2005 -- [ET]  Added support for List-blockette interpolation.
//   8/22/2006 -- [ET]  Modified 'calculateResponse()' to make sure that
//                      if input units are 'Pascal' or 'Tesla' then output
//                      units conversion must be 'Velocity' or 'Default'.
//   5/25/2007 -- [ET]  Added 'checkFixFirFreq0Norm()' method and usage;
//                      added "InfoMessage" methods.
//   5/24/2010 -- [ET]  Renamed 'useDelayFlag' to 'useEstDelayFlag';
//                      modified 'calculateResponse()' to make it always
//                      apply delay correction to asymmetrical FIR filters
//                      (using estimated delay if 'useEstDelayFlag'==true,
//                      otherwise using correction-applied value);
//                      modified text-listing output to show "FIR_ASYM",
//                      "FIR_SYM1" and "FIR_SYM2" (instead of just "FIR");
//                      modified 'firTrans()' to make it use the exact same
//                      algorithms as 'evalresp'; modified computation of
//                      calculated delay so it ignores stages with no
//                      coefficients; added optional parameters
//                      'unwrapPhaseFlag' and 'totalSensitFlag' to
//                      'calculateResponse()' method; added verbose-output
//                      note for 'totalSensitFlag'==true and warnings for
//                      stage correction-applied and estimated-delay values
//                      being negative.
//    6/1/2010 -- [ET]  Made 'getStagesListStr()' only display selected
//                      start/stop stages; modified 'getOutputHeaderString()'
//                      method to show response-sensitivity frequency and
//                      A0 normalization factor from first stage; took out
//                      call to 'isSensObjValid()' in 'calculateResponse()';
//                      added methods 'getRespSensitFactor()',
//                      'getRespSensitFreq()', 'getRespS1NormFactor()'
//                      and 'getRespS1NormFrequency()'
//    6/3/2010 -- [ET]  Changed "A0" to "NormA0" in output from method
//                      'getOutputHeaderString()'; renamed method
//                      'getRespS1NormFrequency()' to 'getRespS1NormFreq()'.
//  10/29/2013 -- [ET]  Added support for Polynomial Blockette (62);
//                      added optional 'b62XValue' parameter to method
//                      'calculateResponse()'; modified 'firTrans()' method
//                      processing of phase values with FIR_ASYM filters
//                      (per Gabi Laske); modified 'calculateResponse()'
//                      method to use calculated delay with FIR_ASYM filters
//                      when 'useEstDelayFlag' is false; modified method
//                      'calculateResponse()' to check Centigrade units
//                      similar to how Pascal and Tesla units are checked;
//                      modified 'normalizeResponse()' method to make it
//                      only call 'checkFixFirFreq0Norm()' if filter type
//                      is 'FIR_ASYM'; added 'B62_x' value to method
//                      'getOutputHeaderString()'.
//
package com.isti.jevalresp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;

import org.apache.commons.lang3.StringUtils;

import edu.iris.Fissures.Unit;
import edu.iris.Fissures.UnitBase;
import edu.iris.Fissures.IfNetwork.CoefficientErrored;
import edu.iris.Fissures.IfNetwork.CoefficientFilter;
import edu.iris.Fissures.IfNetwork.Decimation;
import edu.iris.Fissures.IfNetwork.Filter;
import edu.iris.Fissures.IfNetwork.FilterType;
import edu.iris.Fissures.IfNetwork.Gain;
import edu.iris.Fissures.IfNetwork.ListFilter;
import edu.iris.Fissures.IfNetwork.Normalization;
import edu.iris.Fissures.IfNetwork.PoleZeroFilter;
import edu.iris.Fissures.IfNetwork.Response;
import edu.iris.Fissures.IfNetwork.Sensitivity;
import edu.iris.Fissures.IfNetwork.Stage;
import edu.iris.Fissures.IfNetwork.TransferType;
import edu.iris.Fissures.model.UnitImpl;

/**
 * Class OutputGenerator generates 'evalresp'-style output from a FISSURES
 * 'Response' object.
 */
public class OutputGenerator {

    /**
     * Unit conversion type index value for "default".
     */
    public static final int DEFAULT_UNIT_CONV = 0;
    /**
     * Unit conversion type index value for "displacement".
     */
    public static final int DISPLACE_UNIT_CONV = 1;
    /**
     * Unit conversion type index value for "velocity".
     */
    public static final int VELOCITY_UNIT_CONV = 2;
    /**
     * Unit conversion type index value for "acceleration".
     */
    public static final int ACCEL_UNIT_CONV = 3;
    /**
     * Strings associated with the '..._UNIT_CONV' values.
     */
    public static final String[] UNIT_CONV_STRS = { "def", "dis", "vel", "acc" };
    /**
     * Default index value for 'outUnitsConv' (-u) parameter ("vel").
     */
    public static final int UNIT_CONV_DEFIDX = 2;
    /**
     * Longer versions of strings associated with the '..._UNIT_CONV' values.
     */
    public static final String[] UNIT_CONV_LONGSTRS = { "Default", "Displacement", "Velocity", "Acceleration" };
    /**
     * Value of 'pi' times 2.
     */
    public static final double TWO_PI = Math.PI * 2;
    /**
     * Tolerance value used by 'checkFixFirFreq0Norm()' method.
     */
    public static final double FIR_NORM_TOL = 0.02;

    /**
     * Formatter object for displaying floating-point decimal values.
     */
    public static final NumberFormat decValFormatObj = NumberFormat.getInstance();

    protected static final String ANALOG_STR = "ANALOG"; //stage ID string
    protected static final String COMPOSITE_STR = "COMPOSITE"; //stage ID
    protected static final String LIST_STR = "LIST"; //stage ID string
    protected static final String POLYNOMIAL_STR = "POLYNOMIAL"; //stage ID
    protected static final String NO_TYPE_STR = "(????)"; //stage ID string
    protected static final String A0_STR = "A0"; //normalization ID string
    protected static final String H0_STR = "H0"; //normalization ID string
    protected static final int FIR_ASYM = 0; //FIR symmetry index values
    protected static final int FIR_SYM1 = 1;
    protected static final int FIR_SYM2 = 2;
    protected static final int FIR_UNKNOWN = 3;

    //  protected static final boolean XDEBUG_FLAG = true;   //true for messages
    protected final Response respObj; //response object
    //calculated sensitivity values for stages (idx 0 == all stages):
    protected double[] calcSensitivityArray = null;
    protected double calcSenseFrequency = 0.0; //calculated sens. freq
    protected boolean normalizedFlag = false; //set true after normalized
    protected double[] requestedFreqArray = null; //requested frequency vals
    protected boolean logSpacingFlag = true; //true for log frequency spacing
    protected int outUnitsConv = UNIT_CONV_DEFIDX; //output units conv value
    protected int startStageNum = 0; //first stage to be processed
    protected int stopStageNum = 0; //last stage to be processed
    protected boolean useEstDelayFlag = false; //use est delay for phase calc
    protected boolean showInputFlag = false; //show RESP input
    protected boolean listInterpOutFlag = false; //interpolate List output
    protected boolean listInterpInFlag = false; //interpolate List input
    protected double listInterpTension = 0.0; //tension for List interp
    protected boolean unwrapPhaseFlag = false; //unwrap phase values
    protected boolean totalSensitFlag = false; //use stage 0 sensitivity
    protected double b62XValue = 0.0; //sample value for poly blockette
    protected Unit firstUnitProc = null; //first unit in stages processed
    protected Unit lastUnitProc = null; //last unit in stages processed
    protected int numCalcStages = 0; //number of stages calculated
    protected boolean listStageFlag = false; //true if list stage in response
    //array of response arrays/info objects:
    protected RespArraysInfo[] respArraysInfoArray = null;
    //flag set true after 'getAllStagesAmpPhaseArrays()' called:
    protected boolean allStagesAmpPhaseCalcFlag = false;
    //flag for 'anyAmpsNotPositive()' method:
    protected boolean anyAmpsNotPositiveFlag = false;
    //flag set true after 'anyAmpsNotPositiveFlag' is setup:
    protected boolean anyAmpsNotPosSetupFlag = false;
    //flag for 'allStagesAnyAmpsNotPositive()' method:
    protected boolean allStagesAnyAmpsNotPositiveFlag = false;
    //flag set true after 'allStagesAnyAmpsNotPositiveFlag' is setup:
    protected boolean allStagesAnyAmpsNotPosSetupFlag = false;
    //flag set true after interval/delay/corr values calculated:
    protected boolean calcIntDelayCorrFlag = false;
    protected double sampleInterval = 0.0; //sample interval for response
    protected double estimatedDelay = 0.0; //estimated delay for response
    protected double correctionApplied = 0.0; //correction applied for resp
    protected double calculatedDelay = 0.0; //calculated delay for response
    protected String errorMessage = null; //error message string
    protected String infoMessage = null; //info message string

    static //static initialization block; executes only once
    { //setup number formatter:
        decValFormatObj.setMinimumFractionDigits(0);
        decValFormatObj.setMaximumFractionDigits(8);
    }
    private ResponseUnits inputUnits;

    /**
     * Creates an output generating object.
     *
     * @param respObj
     *            an 'edu.iris.Fissures.IfNetwork.Response' object to generate
     *            output from.
     */
    public OutputGenerator(Response respObj) {
        this.respObj = respObj;
        inputUnits = null;
    }

    /**
     * Creates an output generating object.
     *
     * @param cSpectraArray
     *            an array of complex spectra output values from the calculated
     *            response.
     * @param calcFreqArray
     *            an array of frequency values for the calculated response.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     */
    public OutputGenerator(ComplexBlk[] cSpectraArray, double[] calcFreqArray, boolean logSpacingFlag, boolean useEstDelayFlag, boolean showInputFlag) {
        //create array with single entry for all-stages response:
        respArraysInfoArray = new RespArraysInfo[] { new RespArraysInfo(cSpectraArray, calcFreqArray, "Response") };
        requestedFreqArray = calcFreqArray;
        this.logSpacingFlag = logSpacingFlag;
        this.useEstDelayFlag = useEstDelayFlag;
        this.showInputFlag = showInputFlag;
        respObj = null;
        inputUnits = null;
    }

    public OutputGenerator(Response respObj, ComplexBlk[] cSpectraArray, double[] calcFreqArray, boolean logSpacingFlag, boolean useEstDelayFlag, boolean showInputFlag, ResponseUnits inputUnits) {
        //create array with single entry for all-stages response:
        respArraysInfoArray = new RespArraysInfo[] { new RespArraysInfo(cSpectraArray, calcFreqArray, "Response") };
        requestedFreqArray = calcFreqArray;
        this.logSpacingFlag = logSpacingFlag;
        this.useEstDelayFlag = useEstDelayFlag;
        this.showInputFlag = showInputFlag;
        this.respObj = respObj;
        this.inputUnits = inputUnits;
    }

    /**
     * Creates an output generating object.
     *
     * @param cSpectraArray
     *            an array of complex spectra output values from the calculated
     *            response.
     * @param calcFreqArray
     *            an array of frequency values for the calculated response.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     */
    public OutputGenerator(ComplexBlk[] cSpectraArray, double[] calcFreqArray, boolean logSpacingFlag) {
        this(cSpectraArray, calcFreqArray, logSpacingFlag, false, false);
    }

    public OutputGenerator(Response respObj, ComplexBlk[] cSpectraArray, double[] calcFreqArray, boolean logSpacingFlag, ResponseUnits inputUnits) {
        this(respObj, cSpectraArray, calcFreqArray, logSpacingFlag, false, false, inputUnits);
    }

    /**
     * Creates an output generating object.
     *
     * @param ampPhaseArray
     *            an array of amplitude/phase response values.
     * @param calcFreqArray
     *            an array of frequency values for the calculated response.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     */
    public OutputGenerator(AmpPhaseBlk[] ampPhaseArray, double[] calcFreqArray, boolean logSpacingFlag, boolean useEstDelayFlag, boolean showInputFlag) {
        //create array with single entry for all-stages response:
        respArraysInfoArray = new RespArraysInfo[] { new RespArraysInfo(null, calcFreqArray, "Response", ampPhaseArray) };
        requestedFreqArray = calcFreqArray;
        this.logSpacingFlag = logSpacingFlag;
        this.useEstDelayFlag = useEstDelayFlag;
        this.showInputFlag = showInputFlag;
        respObj = null; //no response object
        outUnitsConv = DEFAULT_UNIT_CONV; //"Default" units conversion
        inputUnits = null;
    }

    /**
     * Creates an output generating object.
     *
     * @param ampPhaseArray
     *            an array of amplitude/phase response values.
     * @param calcFreqArray
     *            an array of frequency values for the calculated response.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     */
    public OutputGenerator(AmpPhaseBlk[] ampPhaseArray, double[] calcFreqArray, boolean logSpacingFlag) {
        this(ampPhaseArray, calcFreqArray, logSpacingFlag, false, false);
    }

    public ResponseUnits getInputUnits() {
        return inputUnits;
    }

    /**
     * Constructs a FileWriter object given a file name.
     *
     * @param outputDirectory
     *            output directory or null for current directory.
     * @param fileName
     *            String The system-dependent filename.
     * @throws IOException
     *             if the specified file is not found or if some other I/O error
     *             occurs.
     * @return FileWriter object
     */
    public static FileWriter openFileWriter(File outputDirectory, String fileName) throws IOException {
        final File outputFile;
        if (outputDirectory != null) {
            outputFile = new File(outputDirectory, fileName);
        } else {
            outputFile = new File(fileName);
        }
        return new FileWriter(outputFile);
    }

    /**
     * Checks the validity of various elements of the response; if OK then
     * returns true, if error then sets an error message (retrievable via
     * 'getErrorMessage()') and returns false. For the response to be seen as
     * valid, it must meet the following conditions:
     *
     * <pre>
     * 1. The response must contain no null handles.
     * 2. If 'dontCheckUnitsFlag'==false and the response contains multiple
     *    stages, input units must match output units of previous stages.
     * 3. All stages must contain one filter (except for gain-only stages).
     * 4. Each filter must specify a supported transfer/response type.
     * 5. If a stage contains a Poles & Zeros or Coefficients filter with
     *    a transfer type of Digital then a decimation must be specified.
     * 6. If a stage contains a Poles & Zeros filter then it must also
     *    contain a normalization.
     * 7. If a stage contains a Response List filter then it must be the
     *    the only stage in the response, and there must be no normalization
     *    for the stage.  In the list filter object, the frequency units
     *    must be specified as 'Hertz' ('Seconds^-1') and the phase units
     *    must be specified as 'Degrees' or 'Radians'.
     * 8. If the response contains multiple stages, each stage must contain
     *    a gain value.  If the response contains only one stage then
     *    either an overall (stage 0) sensitivity or a gain value for
     *    the stage (or both) must be specified.
     * 9. If input units are 'Pascal' or 'Tesla' then output units
     *    conversion must be 'Velocity' or 'Default'.
     * </pre>
     *
     * @param dontCheckUnitsFlag
     *            if false and the response contains multiple stages then the
     *            input units must match the output units of previous stages; if
     *            true then the units are not checked.
     * @return true if response elements are valid.
     */
    public boolean checkResponse(boolean dontCheckUnitsFlag) {
        if (respObj == null) {
            setErrorMessage("Response object is null");
            return false;
        }
        if (respObj.stages == null) {
            setErrorMessage("'Response.stages' is null");
            return false;
        }
        final int numStages = respObj.stages.length;
        if (numStages <= 0) {
            setErrorMessage("No stages in response.");
            return false;
        }
        //check stages in response:
        boolean polynomialFlag = false; //set true if polynomial filter found
        Unit prevUnitObj = null; //stage to stage units tracker
        Stage stageObj;
        Filter filterObj;
        ListFilter listFilterObj;
        Unit unitObj;
        UnitImpl unitImplObj;
        boolean needDeciFlag;
        for (int stageNum = 0; stageNum < numStages; ++stageNum) { //for each stage in response
            stageObj = respObj.stages[stageNum];
            if (stageObj == null) { //array element is null; set error message
                setErrorMessage("No data entered for stage #" + (stageNum + 1) + " (out of " + numStages + " stages)");
                return false;
            }
            if (stageObj.type == null) { //no type object; set error message
                setErrorMessage("No transfer type for stage #" + (stageNum + 1));
                return false;
            }
            if (!dontCheckUnitsFlag && !polynomialFlag) { //units checking enabled and no polynomial filters were found
                if (stageObj.input_units == null) { //no input units object; set error message
                    setErrorMessage("No input units for stage #" + (stageNum + 1));
                    return false;
                }
                if (stageObj.output_units == null) { //no output units object; set error message
                    setErrorMessage("No output units for stage #" + (stageNum + 1));
                    return false;
                }
                if (prevUnitObj != null && !stageObj.input_units.equals(prevUnitObj)) { //not the first stage and units do not match
                    setErrorMessage(
                            "Input units ("
                                    + RespUtils.unitToUnitImpl(stageObj.input_units)
                                    + ") for stage #"
                                    + (stageNum + 1)
                                    + " do not match output units ("
                                    + RespUtils.unitToUnitImpl(prevUnitObj)
                                    + ") of previous stage");
                    return false;
                }
                prevUnitObj = stageObj.output_units; //set units for next stage
            }
            //check filters in stage:
            if ((stageObj.filters == null || stageObj.filters.length <= 0) && !RespUtils.isGainObjValid(stageObj.the_gain)) { //no filters and no gain value for stage; set error message
                setErrorMessage("No filters in stage #" + (stageNum + 1));
                return false;
            }
            if (stageObj.filters.length > 1) { //too many filters; set error message
                setErrorMessage("More than one filter in stage #" + (stageNum + 1));
                return false;
            }
            if (stageObj.filters != null && stageObj.filters.length > 0) { //stage contains a filter object (not a gain-only stage)
                try //check filter for stage:
                {
                    filterObj = stageObj.filters[0];
                    if (filterObj == null) { //no filter object; set error message
                        setErrorMessage("Filter object in stage #" + (stageNum + 1) + " is null");
                        return false;
                    }
                    if (filterObj.discriminator().equals(FilterType.POLYNOMIAL)) {
                        polynomialFlag = true; //indicate polynomial filter found
                    } else if (numStages > 1 && !RespUtils.isGainObjValid(stageObj.the_gain)) { //more than one stage and no gain value for stage; set message
                        setErrorMessage("No gain value for stage #" + (stageNum + 1) + " of multi-stage response");
                        return false;
                    }
                    if (filterObj.discriminator().equals(FilterType.POLEZERO)) { //poles/zeros type filter
                        if (stageObj.the_normalization == null || stageObj.the_normalization.length <= 0 || stageObj.the_normalization[0] == null) { //no normalization; set error message
                            setErrorMessage("No normalization for poles/zeros " + "filter in stage #" + (stageNum + 1));
                            return false;
                        }
                        if (stageObj.type == TransferType.DIGITAL) {
                            needDeciFlag = true; //decimation stage required
                        } else if (stageObj.type == TransferType.LAPLACE || stageObj.type == TransferType.ANALOG) {
                            needDeciFlag = false; //decimation stage not required
                        } else { //invalid transfer type; set error message
                            setErrorMessage("Invalid transfer type for poles/zeros " + "filter in stage #" + (stageNum + 1));
                            return false;
                        }
                    } else if (filterObj.discriminator().equals(FilterType.COEFFICIENT)) { //coefficients type filter
                        if (stageObj.type == TransferType.DIGITAL) {
                            needDeciFlag = true; //decimation stage required
                        } else { //invalid transfer type; set error message
                            setErrorMessage("Invalid transfer type for coefficients " + "filter in stage #" + (stageNum + 1));
                            return false;
                        }
                    } else if (filterObj.discriminator().equals(FilterType.LIST)) { //response list filter
                        if (stageNum > 0 || numStages > 1) { //response list not only stage; set error message
                            setErrorMessage("Other stages not allowed with Response " + "List in stage #" + (stageNum + 1));
                            return false;
                        }
                        if (stageObj.the_normalization != null && stageObj.the_normalization.length > 0 && stageObj.the_normalization[0] != null) { //normalization not allowed; set error message
                            setErrorMessage("Normalization not allowed with Response " + "List in stage #" + (stageNum + 1));
                            return false;
                        }
                        // Desired behavior is to allow decimation and gain blockettes
                        //  in stages containing list blockettes -- 10/13/2005 -- [ET]
                        //            if(stageObj.the_decimation != null &&
                        //                                       stageObj.the_decimation.length > 0 &&
                        //                                         stageObj.the_decimation[0] != null)
                        //            {   //decimation not allowed; set error message
                        //              setErrorMessage("Decimation not allowed with Response " +
                        //                                          "List in stage #" + (stageNum+1));
                        //              return false;
                        //            }
                        //            if(RespUtils.isGainObjValid(stageObj.the_gain))
                        //            {   //non-zero gain not allowed; set error message
                        //              setErrorMessage("Non-zero gain not allowed with Response " +
                        //                                          "List in stage #" + (stageNum+1));
                        //              return false;
                        //            }
                        listFilterObj = filterObj.list_filter();
                        unitObj = listFilterObj.frequency_unit;
                        if (unitObj != null && unitObj.the_unit_base != null && (!unitObj.the_unit_base.equals(UnitBase.SECOND) || unitObj.exponent != -1)) { //frequency unit not Hertz (sec^-1); set error message
                            setErrorMessage("Freq units (\"" + RespUtils.unitToUnitImpl(unitObj) + "\") not 'Hertz' " + "('Sec^-1') in Response List in stage #" + (stageNum + 1));
                            return false;
                        }
                        unitImplObj = RespUtils.unitToUnitImpl(listFilterObj.phase_unit);
                        if (unitImplObj != null && !unitImplObj.equals(UnitImpl.DEGREE) && !unitImplObj.equals(UnitImpl.RADIAN)) { //phase unit not degree or radian; set error message
                            setErrorMessage("Phase units (\"" + RespUtils.unitToUnitImpl(unitObj) + "\") not 'Degrees' " + "or 'Radians' in Response List in stage #" + (stageNum + 1));
                            return false;
                        }
                        needDeciFlag = false; //decimation stage not required
                    } else //unknown filter type
                    {
                        needDeciFlag = false; //decimation stage not required
                    }
                    if (needDeciFlag && (stageObj.the_decimation == null || stageObj.the_decimation.length <= 0 || stageObj.the_decimation[0] == null)) { //decimation required but not given; set error message
                        setErrorMessage("Required decimation not found in stage #" + (stageNum + 1));
                        return false;
                    }
                } catch (Exception ex) { //exception while processing (shouldn't happen); set message
                    setErrorMessage("Error checking filter in stage #" + (stageNum + 1) + ":  " + ex);
                    return false;
                }
            }
        }
        // stage must have sensitivity or gain
        if ((numStages == 1 && !polynomialFlag) && !RespUtils.isSensObjValid(respObj.the_sensitivity)) { //no stage 0 sensitivity for response
            stageObj = respObj.stages[0];
            if (stageObj != null && !RespUtils.isGainObjValid(stageObj.the_gain)) { //stage object OK and no gain value for stage; set message
                setErrorMessage("No 'stage 0' response sensitivity or gain " + "value for single stage");
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the validity of various elements of the response; if OK then
     * returns true, if error then sets an error message (retrievable via
     * 'getErrorMessage()') and returns false. For the response to be seen as
     * valid, it must meet the following conditions:
     *
     * <pre>
     * 1. The response must contain no null handles.
     * 2. If the response contains multiple stages, input units must
     *    match the output units of previous stages.
     * 3. All stages must contain one filter (except for gain-only stages).
     * 4. Each filter must specify a supported transfer/response type.
     * 5. If a stage contains a Poles & Zeros or Coefficients filter with
     *    a transfer type of Digital then a decimation must be specified.
     * 6. If a stage contains a Poles & Zeros filter then it must also
     *    contain a normalization.
     * 7. If the response contains multiple stages, each stage must contain
     *    a gain value.  If the response contains only one stage then
     *    either an overall (stage 0) sensitivity or a gain value for
     *    the stage (or both) must be specified.
     * </pre>
     *
     * @return true if response elements are valid
     */
    public boolean checkResponse() {
        return checkResponse(false);
    }

    /**
     * Normalizes the response and calculates the sensitivity for all the stages
     * in the response. Normalization objects may be added to the stages to hold
     * the normalization values, and the gain factor and frequency for stages
     * may be modified. The calculated sensitivity value is retrievable via the
     * 'getCalcSensitivity()' method. Note that this method assumes that each
     * stage contains only one filter object in its 'filters[]' array.
     *
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean normalizeResponse(int startStageNum, int stopStageNum) {
        try {
            double cSenseFreq; //local copy of sensitivity frequency
            final int numStages = respObj.stages.length; //get # of stages
            int stageNum;
            //allocate array of calculated sensitivity values,
            // 1 per stage, idx 0 == all-stages value:
            final double[] calcSensArr = new double[numStages + 1];
            for (stageNum = 0; stageNum <= numStages; ++stageNum) {
                calcSensArr[stageNum] = 1.0; //initialize values
            }
            Gain gainObj;
            //if no sensitivity for response then look for the last non-zero
            // frequency value among the stage gains (since filters are
            // typically for low pass purposes, the last non-zero frequency
            // is likely the best choice, as its pass band is the narrowest):
            if (!RespUtils.isSensObjValid(respObj.the_sensitivity)) { //no sensitivity for response
                cSenseFreq = 0.0; //put in default frequency value
                for (stageNum = 0; stageNum < numStages; ++stageNum) { //for each stage in response; find last gain with non-zero freq
                    if (RespUtils.isGainObjValid(gainObj = respObj.stages[stageNum].the_gain) && !RespUtils.isZero(gainObj.frequency)) { //stage contains valid gain entry with non-zero frequency
                        cSenseFreq = gainObj.frequency; //use freq from gain entry
                    }
                }
            } else //sensitivity exists for response; use its frequency
            {
                cSenseFreq = respObj.the_sensitivity.frequency;
            }
            //      if(XDEBUG_FLAG)
            //      {  //send debug message to default log file
            //        LogFile.getGlobalLogObj().debug(
            //           "Normalizing using channel frequency of " + cSenseFreq);
            //      }
            //interpret start/stop stage numbers:
            if (startStageNum <= 0) //if start value too small then
            {
                startStageNum = 1; //setup to use first stage
            }
            --startStageNum; //decrement for 0-based array access
            if (stopStageNum > startStageNum) { //stop value OK; decrement for 0-based array access
                if (--stopStageNum >= numStages) //if stop value too large then
                {
                    stopStageNum = numStages - 1; //setup to use last stage
                }
            } else //stop value too small; if 0 then last stage, else single stage
            {
                stopStageNum = (stopStageNum <= 0) ? numStages - 1 : startStageNum;
            }
            if (startStageNum >= numStages) { //no stages match; set error message
                setErrorMessage("No match for requested range of stage numbers");
                return false;
            }
            //save units for first and last stage processed:
            firstUnitProc = respObj.stages[startStageNum].input_units;
            lastUnitProc = respObj.stages[stopStageNum].output_units;
            //process stages:
            final double wVal = TWO_PI * cSenseFreq;
            Stage stageObj;
            Filter filterObj;
            Normalization normObj;
            Double sIntTimeObj;
            CoefficientFilter coeffFilterObj;
            ComplexBlk dfNum, ofNum;
            double cSenseVal, ofSqVal, newGainVal;
            for (stageNum = startStageNum; stageNum <= stopStageNum; ++stageNum) { //for each stage in desired range
                //        if(XDEBUG_FLAG)
                //        {     //send debug message to default log file
                //          LogFile.getGlobalLogObj().debug("Normalizing stage #" +
                //                                                              (stageNum+1));
                //        }
                stageObj = respObj.stages[stageNum];
                if (stageObj != null && stageObj.filters != null && stageObj.filters.length > 0) { //stage contains filters (not gain-only stage)
                    filterObj = stageObj.filters[0];
                    //get first filter object (we're assuming only one):
                    if (filterObj == null) { //no filter object; set error message
                        setErrorMessage("Filter[0] of stage #" + (stageNum + 1) + " is null");
                        return false;
                    }
                } else {
                    filterObj = null; //indicate no filters
                }
                if (!RespUtils.isGainObjValid(gainObj = stageObj.the_gain)) { //stage does not contain a gain value
                    if (filterObj == null || !filterObj.discriminator().equals(FilterType.POLYNOMIAL)) { //not a polynomial filter; check sensitivity/gain
                        if (numStages > 1) { //more than one stage in response; set error message
                            setErrorMessage("No gain value for stage #" + (stageNum + 1) + " of multi-stage response");
                            return false;
                        }
                        if (!RespUtils.isSensObjValid(respObj.the_sensitivity)) { //no stage 0 sensitivity for response; set error message
                            setErrorMessage("No 'stage 0' response sensitivity or gain " + "value for single stage");
                            return false;
                        }
                    }
                    //enter overall sensitivity as gain for single stage:
                    gainObj = stageObj.the_gain = new Gain(respObj.the_sensitivity.sensitivity_factor, respObj.the_sensitivity.frequency);
                }
                if (filterObj != null) { //stage contains filters (not gain-only stage)
                    //setup normalization object:
                    if (filterObj.discriminator().equals(FilterType.POLEZERO)) { //filter is poles/zeros type; get normalization
                        if (stageObj.the_normalization == null || stageObj.the_normalization.length <= 0 || (normObj = stageObj.the_normalization[0]) == null) { //no normalization; set error message
                            setErrorMessage("No normalization for poles/zeros " + "filter in stage #" + (stageNum + 1));
                            return false;
                        }
                    } else //filter is not poles/zeros type
                    {
                        normObj = null; //no preset normalization
                    }
                    if (!filterObj.discriminator().equals(FilterType.POLYNOMIAL)) { //not a polynomial filter
                        if ((gainObj.frequency != cSenseFreq || (normObj != null && normObj.normalization_freq != cSenseFreq))) { //gain or normalization frequency needs to change
                            if (filterObj.discriminator().equals(FilterType.POLEZERO)) { //poles/zeros type filter
                                if (stageObj.type == TransferType.LAPLACE || stageObj.type == TransferType.ANALOG) { //analog poles/zeros filter
                                    dfNum = analogTrans(filterObj.pole_zero_filter(), 1.0, ((stageObj.type == TransferType.LAPLACE) ? TWO_PI * gainObj.frequency : gainObj.frequency));
                                    ofNum = analogTrans(filterObj.pole_zero_filter(), 1.0, ((stageObj.type == TransferType.LAPLACE) ? TWO_PI * cSenseFreq : cSenseFreq));
                                    if (RespUtils.isZero(dfNum) || RespUtils.isZero(ofNum)) { //zero values returned from transformation; set message
                                        setErrorMessage("Zero frequency in bandpass analog " + "filter in stage #" + (stageNum + 1));
                                        return false;
                                    }
                                } else if (stageObj.type == TransferType.DIGITAL) { //digital poles/zeros filter; get decimation object
                                    if (stageObj.the_decimation == null || stageObj.the_decimation.length <= 0) { //decimation required but not given; set error message
                                        setErrorMessage("Required decimation not found in " + "stage #" + (stageNum + 1));
                                        return false;
                                    }
                                    sIntTimeObj = RespUtils.deciToSampIntTime(stageObj.the_decimation[0]);
                                    if (sIntTimeObj == null) { //unable to process decimation; set error message
                                        setErrorMessage("Invalid decimation object in stage #" + (stageNum + 1));
                                        return false;
                                    }
                                    dfNum = iirPzTrans(filterObj.pole_zero_filter(), 1.0, sIntTimeObj.doubleValue(), TWO_PI * gainObj.frequency);
                                    ofNum = iirPzTrans(filterObj.pole_zero_filter(), 1.0, sIntTimeObj.doubleValue(), wVal);
                                } else { //invalid transfer type; set error message
                                    setErrorMessage("Invalid transfer type for poles/zeros " + "filter in stage #" + (stageNum + 1));
                                    return false;
                                }
                            } else if (filterObj.discriminator().equals(FilterType.COEFFICIENT)) { //coefficients type filter
                                if (stageObj.type != TransferType.DIGITAL) { //invalid transfer type; set error message
                                    setErrorMessage("Invalid transfer type for coefficients " + "filter in stage #" + (stageNum + 1));
                                    return false;
                                }
                                if (stageObj.the_decimation == null || stageObj.the_decimation.length <= 0) { //decimation required but not given; set error message
                                    setErrorMessage("Required decimation not found in stage #" + (stageNum + 1));
                                    return false;
                                }
                                sIntTimeObj = RespUtils.deciToSampIntTime(stageObj.the_decimation[0]);
                                if (sIntTimeObj == null) { //unable to process decimation; set error message
                                    setErrorMessage("Invalid decimation object in stage #" + (stageNum + 1));
                                    return false;
                                }
                                coeffFilterObj = filterObj.coeff_filter();
                                if (coeffFilterObj.denominator.length <= 0) { //no denominators, process as FIR filter
                                    if (coeffFilterObj.numerator.length > 0) { //more than zero numerators
                                        //determine if asymmetrical or symmetrical FIR:
                                        final int firTypeVal = determineFirTypeVal(filterObj);
                                        //check/fix FIR coefficients normalization:
                                        if (firTypeVal == FIR_ASYM) // (only if ASYM type)
                                        {
                                            checkFixFirFreq0Norm(coeffFilterObj, stageNum);
                                        }
                                        dfNum = firTrans(coeffFilterObj, 1.0, sIntTimeObj.doubleValue(), TWO_PI * gainObj.frequency, firTypeVal);
                                        ofNum = firTrans(coeffFilterObj, 1.0, sIntTimeObj.doubleValue(), wVal, firTypeVal);
                                    } else //empty FIR filter; ignore it
                                    {
                                        dfNum = ofNum = null; //setup to not change gain, etc
                                    }
                                } else { //contains denominators, process as coefficients filter
                                    dfNum = iirTrans(coeffFilterObj, 1.0, sIntTimeObj.doubleValue(), TWO_PI * gainObj.frequency);
                                    ofNum = iirTrans(coeffFilterObj, 1.0, sIntTimeObj.doubleValue(), wVal);
                                }
                            } else //unknown filter type; ignore it
                            {
                                dfNum = ofNum = null; //setup to not change gain, etc
                            }
                            if (dfNum != null && ofNum != null) { //values were entered; process them
                                ofSqVal = Math.sqrt(ofNum.real * ofNum.real + ofNum.imag * ofNum.imag);
                                //put in new gain factor and frequency:
                                newGainVal = gainObj.gain_factor / Math.sqrt(dfNum.real * dfNum.real + dfNum.imag * dfNum.imag) * ofSqVal;
                                gainObj.gain_factor = (float) newGainVal;
                                gainObj.frequency = (float) cSenseFreq;
                                if (normObj != null) { //normalization object already exists; modify it
                                    normObj.ao_normalization_factor = (float) (1.0 / ofSqVal);
                                    normObj.normalization_freq = (float) cSenseFreq;
                                } else { //normalization object does not yet exist; create one
                                    normObj = new Normalization((float) (1.0 / ofSqVal), (float) cSenseFreq);
                                    //enter normalization object into stage:
                                    stageObj.the_normalization = new Normalization[] { normObj };
                                }
                                //enter new gain value into sensitivity calculation:
                                cSenseVal = newGainVal;
                            } else //enter gain value into sensitivity calculation:
                            {
                                cSenseVal = gainObj.gain_factor;
                            }
                        } else //enter gain value into sensitivity calculation:
                        {
                            cSenseVal = gainObj.gain_factor;
                        }
                    } else //is a polynomial filter
                    {
                        cSenseVal = 1.0; //use 1.0 for no change to gain factor
                    }
                } else //gain-only stage; enter gain value into sensitivity calc
                {
                    cSenseVal = gainObj.gain_factor;
                }
                //enter calculated sensitivity value for stage:
                calcSensArr[stageNum + 1] = cSenseVal;
                //apply value toward all-stages calc sensitivity value:
                calcSensArr[0] *= cSenseVal;
            }
            if (numStages == 1 && startStageNum < 1) { //single stage response and stage wasn't skipped
                //enter calc sensitivity as overall response sensitivity:
                if (respObj.the_sensitivity != null) { //already contains sensitivity object; modify it
                    respObj.the_sensitivity.sensitivity_factor = (float) calcSensArr[0];
                    respObj.the_sensitivity.frequency = (float) cSenseFreq;
                } else { //no previous sensitivity object; create and enter new one
                    respObj.the_sensitivity = new Sensitivity((float) calcSensArr[0], (float) cSenseFreq);
                }
            }
            calcSensitivityArray = calcSensArr; //save calc sensitivity values
            calcSenseFrequency = cSenseFreq; //save frequency used
            normalizedFlag = true; //indicate normalization performed
            return true; //return OK flag
        } catch (Exception ex) { //some kind of error occurred; set error message
            setErrorMessage("Error normalizing response:  " + ex);
            return false;
        }
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqArray
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
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
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double[] freqArray, boolean logSpacingFlag, int outUnitsConv, int startStageNum, int stopStageNum, boolean useEstDelayFlag, boolean showInputFlag,
            boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag, double b62XValue) {
        requestedFreqArray = freqArray; //save frequency array
        this.logSpacingFlag = logSpacingFlag; //save frequency spacing flag
        this.outUnitsConv = outUnitsConv; //save output units conv value
        this.startStageNum = startStageNum; //save start stage number
        this.stopStageNum = stopStageNum; //save stop stage number
        this.useEstDelayFlag = useEstDelayFlag; //save est-delay-in-phase flag
        this.showInputFlag = showInputFlag; //save show-RESP-input flag
        this.listInterpOutFlag = listInterpOutFlag; //save interp out flag
        this.listInterpInFlag = listInterpInFlag; //save interp in flag
        this.listInterpTension = listInterpTension; //save interp tension
        this.unwrapPhaseFlag = unwrapPhaseFlag; //save unwrap flag
        this.totalSensitFlag = totalSensitFlag; //save sensitivity flag
        this.b62XValue = b62XValue; //save sample value for B62
        numCalcStages = 0; //initialize count of stages calculated
        listStageFlag = false; //initialize list stage in response flag
        if (!normalizedFlag && !normalizeResponse(startStageNum, stopStageNum)) {
            return false; //if error then return
        }
        try {
            final int numStages = respObj.stages.length; //get # of stages
            Filter filterObj;
            ListFilter lsFilterObj;
            double[] listInterpAmpArr = null, listInterpPhaseArr = null;
            final double phaseConvVal;
            //check if stage has response list filter:
            if (numStages > 0
                    && respObj.stages[0] != null
                    && respObj.stages[0].filters != null
                    && respObj.stages[0].filters.length > 0
                    && (filterObj = respObj.stages[0].filters[0]) != null
                    && filterObj.discriminator().equals(FilterType.LIST)) { //list filter; use its frequency array instead of parameter
                lsFilterObj = filterObj.list_filter(); //set handle to filter
                freqArray = RespUtils.floatToDoubleArray(lsFilterObj.frequency);
                if (lsFilterObj.amplitude == null || lsFilterObj.amplitude.length < freqArray.length || lsFilterObj.phase == null || lsFilterObj.phase.length < freqArray.length) { //amp or phase array not large enough; set error message
                    setErrorMessage("Amp or phase array too small in Response " + " List stage");
                    return false;
                }
                if (listInterpInFlag) { //interpolate amp/phase values input from List blockette
                    //use req freqs, with any out-of-range freqs clipped:
                    final double[] newFreqArray = clipFreqArray(requestedFreqArray, freqArray, true);
                    if (newFreqArray.length <= 0) { //all requested frequencies were clipped
                        setErrorMessage("Error interpolating values in Response " + "List stage:  All requested freqencies out of range");
                        return false;
                    }
                    final CubicSpline splineObj = new CubicSpline();
                    try {
                        listInterpAmpArr = splineObj.calcSpline(freqArray, RespUtils.floatToDoubleArray(lsFilterObj.amplitude), listInterpTension, 1.0, newFreqArray);
                    } catch (IllegalArgumentException e) {
                        //interpolate List blockette amplitude values:
                        setErrorMessage("Error interpolating amplitude values in " + "Response List stage:  " + e.getLocalizedMessage());
                        return false;
                    }

                    final double[] srcPhaseArr = //get List phase data
                            RespUtils.floatToDoubleArray(lsFilterObj.phase);
                    //unwrap phase data:
                    final double[] unwrappedPhaseArr = unwrapPhaseArray(srcPhaseArr);
                    try {
                        listInterpPhaseArr = splineObj.calcSpline(freqArray, unwrappedPhaseArr, listInterpTension, 1.0, newFreqArray);
                        //interpolate List blockette phase values:
                    } catch (IllegalArgumentException e) {
                        setErrorMessage("Error interpolating phase values in " + "Response List stage:  " + e.getLocalizedMessage());
                        return false;
                    }
                    //if phase data was unwrapped then wrap interpolated data:
                    if (unwrappedPhaseArr != srcPhaseArr) {
                        listInterpPhaseArr = wrapPhaseArray(listInterpPhaseArr);
                    }
                    freqArray = newFreqArray; //use new frequency array
                }
                UnitImpl unitImplObj = RespUtils.unitToUnitImpl(lsFilterObj.phase_unit);
                if (unitImplObj != null) { //phase units object exists
                    if (unitImplObj.equals(UnitImpl.DEGREE)) //if degrees then
                    {
                        phaseConvVal = Math.PI / 180; //set conv to rad
                    } else if (unitImplObj.equals(UnitImpl.RADIAN)) //if radians then
                    {
                        phaseConvVal = 1.0; //no conversion
                    } else { //unknown phase units type; set error message
                        setErrorMessage("Invalid phase units type (\"" + unitImplObj + "\") in Response List stage");
                        return false;
                    }
                } else //no phase units object; assume that phase values in degrees
                {
                    phaseConvVal = Math.PI / 180; //set conversion to radians
                }
            } else //not response list filter
            {
                phaseConvVal = 1.0; //enter dummy value
            }
            final double unitScaleFact;
            int inpUnitsConv;
            if (outUnitsConv >= 0 && outUnitsConv != DEFAULT_UNIT_CONV && numStages > 0 && (inpUnitsConv = toUnitConvIndex(respObj.stages[0].input_units)) >= 0) { //not default units, >0 stages and input units conversion factor OK
                //use negated power of first input unit for unit scale factor:
                unitScaleFact = RespUtils.pow10(-RespUtils.toFirstUnitPower(respObj.stages[0].input_units));
            } else { //default units or zero stages in response
                unitScaleFact = 1.0; //set default unit scale factor
                //set default conversions (none performed):
                outUnitsConv = inpUnitsConv = DEFAULT_UNIT_CONV;
            }
            //interpret start/stop stage numbers:
            if (startStageNum <= 0) //if start value too small then
            {
                startStageNum = 1; //setup to use first stage
            }
            --startStageNum; //decrement for 0-based array access
            if (stopStageNum > startStageNum) { //stop value OK; decrement for 0-based array access
                if (--stopStageNum >= numStages) //if stop value too large then
                {
                    stopStageNum = numStages - 1; //setup to use last stage
                }
            } else //stop value too small; if 0 then last stage, else single stage
            {
                stopStageNum = (stopStageNum <= 0) ? numStages - 1 : startStageNum;
            }
            if (startStageNum >= numStages) { //no stages match; set error message
                setErrorMessage("No match for requested range of stage numbers");
                return false;
            }
            final int numFreq = freqArray.length;
            int stageNum;
            //create array of 'RespArraysInfo' objects, one for each stage;
            // stage-index-zero entry holds the response value for all stages;
            // then the proceeding entries are for the separate stages:
            final RespArraysInfo[] rArrsInfoArr = new RespArraysInfo[numStages + 1];
            //enter 'RespArraysInfo' object for all-stages response:
            rArrsInfoArr[0] = new RespArraysInfo(numFreq, freqArray, "Response");
            String typeStr;
            for (stageNum = startStageNum; stageNum <= stopStageNum; ++stageNum) { //for each stage to be processed; get type ID for stage
                typeStr = getTypeStrForStage(respObj.stages[stageNum]);
                if (typeStr != null && typeStr.length() > 0) { //type ID string contains data
                    typeStr = " " + typeStr; //prepend space to ID string
                }
                //enter 'RespArraysInfo' object into array:
                rArrsInfoArr[stageNum + 1] = new RespArraysInfo(numFreq, freqArray, ("Stg " + (stageNum + 1) + typeStr));
            }
            RespArraysInfo rArrInfoObj;
            //save units for first and last stage processed:
            firstUnitProc = respObj.stages[startStageNum].input_units;
            lastUnitProc = respObj.stages[stopStageNum].output_units;
            //if input units Pascal, Tesla or Centigrade then output
            // units conversion must be Velocity or Default:
            if ((RespUtils.PASCAL_UNITIMPL_OBJ.equals(firstUnitProc) || RespUtils.TESLA_UNITIMPL_OBJ.equals(firstUnitProc) || RespUtils.CENTIGRADE_UNITIMPL_OBJ.equals(firstUnitProc))
                    && this.outUnitsConv != VELOCITY_UNIT_CONV
                    && this.outUnitsConv != DEFAULT_UNIT_CONV) {
                setErrorMessage("Input units \"" + RespUtils.unitToUnitImpl(firstUnitProc) + "\" not allowed with \"" + getLongUnitConvString(this.outUnitsConv) + "\" output units conversion");
                return false;
            }
            //process set of stages for each frequency:
            Stage stageObj;
            double freqVal, wVal, normFact, ampVal, phaseVal;
            int numCoeffs, j;
            PoleZeroFilter pzFilterObj;
            CoefficientFilter coeffFilterObj;
            Double sIntTimeObj, tDoubleObj;
            double deltaVal;
            ComplexBlk ofNum = null;
            final ComplexBlk cNum = new ComplexBlk(1.0, 0.0);
            boolean evalFlag;
            for (int fIdx = 0; fIdx < numFreq; ++fIdx) { //for each frequency value
                freqVal = freqArray[fIdx];
                if (freqVal <= 0) {
                    continue;
                }
                wVal = TWO_PI * freqVal;
                for (stageNum = startStageNum; stageNum <= stopStageNum; ++stageNum) { //for each stage in desired range
                    cNum.real = 1.0;
                    cNum.imag = 0.0;
                    stageObj = respObj.stages[stageNum];
                    if (stageObj.filters != null && stageObj.filters.length > 0) { //stage contains filters (not a gain-only stage)
                        if (stageObj.the_normalization != null && stageObj.the_normalization.length > 0 && stageObj.the_normalization[0] != null) { //stage contains normalization; save value
                            normFact = stageObj.the_normalization[0].ao_normalization_factor;
                        } else //stage does not contain normalization
                        {
                            normFact = 1.0; //use default value
                        } //get first filter object (we're assuming only one):
                        filterObj = stageObj.filters[0];
                        if (filterObj == null) { //no filter object; set error message
                            setErrorMessage("Filter object for stage #" + (stageNum + 1) + " is null");
                            return false;
                        }
                        evalFlag = false; //set true if evaluation performed
                        if (filterObj.discriminator().equals(FilterType.POLEZERO)) { //poles/zeros type filter
                            pzFilterObj = filterObj.pole_zero_filter();
                            if (stageObj.type == TransferType.LAPLACE || stageObj.type == TransferType.ANALOG) { //analog poles/zeros filter
                                ofNum = analogTrans(pzFilterObj, normFact, ((stageObj.type == TransferType.LAPLACE) ? TWO_PI * freqVal : freqVal));
                                evalFlag = true; //indicate evaluation performed
                            } else if (stageObj.type == TransferType.DIGITAL) { //digital poles/zeros filter
                                if (pzFilterObj.poles.length > 0 || pzFilterObj.zeros.length > 0) { //filter is not empty
                                    if (stageObj.the_decimation == null || stageObj.the_decimation.length <= 0) { //decimation required but not given; set error message
                                        setErrorMessage("Required decimation not found in " + "stage #" + (stageNum + 1));
                                        return false;
                                    }
                                    sIntTimeObj = RespUtils.deciToSampIntTime(stageObj.the_decimation[0]);
                                    if (sIntTimeObj == null) { //unable to process decimation; set error message
                                        setErrorMessage("Invalid decimation object in stage #" + (stageNum + 1));
                                        return false;
                                    }
                                    ofNum = iirPzTrans(pzFilterObj, normFact, sIntTimeObj.doubleValue(), wVal);
                                    evalFlag = true; //indicate evaluation performed
                                }
                            } else { //invalid transfer type; set error message
                                setErrorMessage("Invalid transfer type for poles/zeros " + "filter in stage #" + (stageNum + 1));
                                return false;
                            }
                        } else if (filterObj.discriminator().equals(FilterType.COEFFICIENT)) { //coefficients type filter
                            if (stageObj.type != TransferType.DIGITAL) { //invalid transfer type; set error message
                                setErrorMessage("Invalid transfer type for coefficients " + "filter in stage #" + (stageNum + 1));
                                return false;
                            }
                            if (stageObj.the_decimation == null || stageObj.the_decimation.length <= 0) { //decimation required but not given; set error message
                                setErrorMessage("Required decimation not found in stage #" + (stageNum + 1));
                                return false;
                            }
                            sIntTimeObj = RespUtils.deciToSampIntTime(stageObj.the_decimation[0]);
                            if (sIntTimeObj == null) { //unable to process decimation; set error message
                                setErrorMessage("Invalid decimation object in stage #" + (stageNum + 1));
                                return false;
                            }
                            coeffFilterObj = filterObj.coeff_filter();
                            if (coeffFilterObj.denominator.length <= 0) { //no denominators, process as FIR filter
                                if (coeffFilterObj.numerator.length > 0) { //more than zero numerators
                                    //determine if asymmetrical or symmetrical FIR:
                                    final int firTypeVal = determineFirTypeVal(filterObj);
                                    ofNum = firTrans(coeffFilterObj, normFact, sIntTimeObj.doubleValue(), wVal, firTypeVal);
                                    if (firTypeVal == FIR_ASYM) { //asymmetric FIR; requires delay correction
                                        if (useEstDelayFlag) { //using estimated delay in phase calc correction
                                            tDoubleObj = RespUtils.quantityToIntTime(stageObj.the_decimation[0].estimated_delay);
                                            if (tDoubleObj != null) { //estimated-delay time value fetched OK
                                                deltaVal = tDoubleObj.doubleValue();
                                                //factor in estimated-delay value:
                                                ofNum.zMultiply(Math.cos(wVal * deltaVal), Math.sin(wVal * deltaVal));
                                            }
                                        } else if ((tDoubleObj = RespUtils.quantityToIntTime(stageObj.the_decimation[0].correction_applied)) != null) { //correction-applied time value fetched OK
                                            //calculate "delta = corrApplied - calcDelay":
                                            deltaVal = tDoubleObj.doubleValue() - ((((double) (coeffFilterObj.numerator.length - 1)) / 2) * sIntTimeObj.doubleValue());
                                            //factor in calculated-delay value:
                                            ofNum.zMultiply(Math.cos(wVal * deltaVal), Math.sin(wVal * deltaVal));
                                        }
                                    }
                                    evalFlag = true; //indicate evaluation performed
                                }
                            } else if (coeffFilterObj.numerator.length > 0) { //process as coefficients filter
                                ofNum = iirTrans(coeffFilterObj, normFact, sIntTimeObj.doubleValue(), wVal);
                                evalFlag = true; //indicate evaluation performed
                            }
                        } else if (filterObj.discriminator().equals(FilterType.LIST)) { //response list filter
                            listStageFlag = true; //indicate list stage in response
                            if (listInterpInFlag) { //using interpolated amp/phase values
                                ampVal = listInterpAmpArr[fIdx]; //get amplitude value
                                //get phase value, convert degrees to radians (if nec):
                                phaseVal = listInterpPhaseArr[fIdx] * phaseConvVal;
                            } else { //using amp/phase values from List blockette
                                lsFilterObj = filterObj.list_filter();
                                ampVal = lsFilterObj.amplitude[fIdx]; //get amplitude value
                                //get phase value, convert degrees to radians (if nec):
                                phaseVal = lsFilterObj.phase[fIdx] * phaseConvVal;
                            }
                            ofNum = new ComplexBlk(ampVal * Math.cos(phaseVal), ampVal * Math.sin(phaseVal));
                            evalFlag = true; //indicate evaluation performed
                        } else if (filterObj.discriminator().equals(FilterType.POLYNOMIAL)) { //polynomial type filter
                            if (b62XValue <= 0.0) { //b62_x value not positive
                                setErrorMessage("Valid 'b62_x' value must be specified " + "for polynomial response");
                                return false;
                            }
                            ampVal = 0.0; //initialize amplitude value
                            //get array of coefficient and error values:
                            final CoefficientErrored[] coeffsArr = filterObj.polynomial_filter().coeff_err_values;
                            numCoeffs = (coeffsArr != null) ? coeffsArr.length : 0;
                            if (numCoeffs > 0) { //array not empty
                                //compute first derivate of MacLaurin polynomial:
                                for (j = 1; j < numCoeffs; ++j) {
                                    ampVal += coeffsArr[j].value * j * Math.pow(b62XValue, j - 1);
                                }
                                //set phase value based on amplitude
                                phaseVal = (ampVal >= 0.0) ? 0.0 : Math.PI;
                                //apply values:
                                ofNum = new ComplexBlk(ampVal * Math.cos(phaseVal), ampVal * Math.sin(phaseVal));
                                evalFlag = true; //indicate evaluation performed
                            }
                        }
                        if (evalFlag) //if filter was evaluated then
                        {
                            cNum.zMultiply(ofNum); //multply in new value
                        }
                    }
                    //enter response value (at frequency) for given stage
                    // (response value for all stages calculated below):
                    rArrsInfoArr[stageNum + 1].enterCSpectraVal(fIdx, cNum);
                }
                //calculate response value for all stages put together:
                cNum.real = 1.0;
                cNum.imag = 0.0;
                for (stageNum = startStageNum; stageNum <= stopStageNum; ++stageNum) {
                    cNum.zMultiply(rArrsInfoArr[stageNum + 1].cSpectraArray[fIdx]);
                }
                //enter value into zero index in array:
                rArrsInfoArr[0].enterCSpectraVal(fIdx, cNum);
                //if using stage 0 (total) sensitivity then save value:
                final float totalSensitVal = (totalSensitFlag && respObj.the_sensitivity != null) ? respObj.the_sensitivity.sensitivity_factor : (float) 1.0;
                double calcSensVal;
                //process conversions for each stage entry:
                for (stageNum = 0; stageNum <= numStages; ++stageNum) { //for each possible stage entry
                    rArrInfoObj = rArrsInfoArr[stageNum];
                    if (rArrInfoObj != null && (ofNum = rArrInfoObj.cSpectraArray[fIdx]) != null) { //stage entry contains data
                        if (totalSensitFlag) { //using stage 0 (total) sensitivity
                            //multiply in sensitivity and unit conv scale factor:
                            ofNum.real *= totalSensitVal * unitScaleFact;
                            ofNum.imag *= totalSensitVal * unitScaleFact;
                        } else { //using computed sensitivity from each stage
                            //get calculated sensitivity value for stage index:
                            calcSensVal = (calcSensitivityArray != null && stageNum < calcSensitivityArray.length) ? calcSensitivityArray[stageNum] : 1.0;
                            //multiply in sensitivity and unit conv scale factor:
                            ofNum.real *= calcSensVal * unitScaleFact;
                            ofNum.imag *= calcSensVal * unitScaleFact;
                        }
                        //handle any units conversions:
                        if (outUnitsConv != inpUnitsConv) { //requested output units different from input units
                            if (inpUnitsConv == DISPLACE_UNIT_CONV) { //input unit is 'displacement'; convert to 'velocity'
                                if (wVal != 0.0) {
                                    ofNum.zMultiply(0.0, -1.0 / wVal);
                                } else {
                                    ofNum.real = ofNum.imag = 0.0;
                                }
                            } //if input unit is 'accel' then convert to 'velocity':
                            else if (inpUnitsConv == ACCEL_UNIT_CONV) {
                                ofNum.zMultiply(0.0, wVal);
                            }
                            //if requested output is 'displacement' then convert:
                            if (outUnitsConv == DISPLACE_UNIT_CONV) {
                                ofNum.zMultiply(0.0, wVal);
                            } else if (outUnitsConv == ACCEL_UNIT_CONV) { //requested output is 'acceleration'; convert it
                                if (wVal != 0.0) {
                                    ofNum.zMultiply(0.0, -1.0 / wVal);
                                } else {
                                    ofNum.real = ofNum.imag = 0.0;
                                }
                            }
                        }
                    }
                }
            }
            //save handle to array of response arrays/info objects:
            respArraysInfoArray = rArrsInfoArr;
            //enter # of stages calculated:
            numCalcStages = stopStageNum - startStageNum + 1;
            return true;
        } catch (

        Exception ex) { //some kind of error occurred; set error message
            setErrorMessage("Error calculating response:  " + ex);
            return false;
        }
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqArray
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
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
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double[] freqArray, boolean logSpacingFlag, int outUnitsConv, int startStageNum, int stopStageNum, boolean useEstDelayFlag, boolean showInputFlag,
            boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag) {
        return calculateResponse(
                freqArray,
                    logSpacingFlag,
                    outUnitsConv,
                    startStageNum,
                    stopStageNum,
                    useEstDelayFlag,
                    showInputFlag,
                    listInterpOutFlag,
                    listInterpInFlag,
                    listInterpTension,
                    unwrapPhaseFlag,
                    totalSensitFlag,
                    0.0);
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqArray
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
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
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double[] freqArray, boolean logSpacingFlag, int outUnitsConv, int startStageNum, int stopStageNum, boolean useEstDelayFlag, boolean showInputFlag,
            boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension) {
        return calculateResponse(
                freqArray,
                    logSpacingFlag,
                    outUnitsConv,
                    startStageNum,
                    stopStageNum,
                    useEstDelayFlag,
                    showInputFlag,
                    listInterpOutFlag,
                    listInterpInFlag,
                    listInterpTension,
                    false,
                    false);
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqArray
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double[] freqArray, boolean logSpacingFlag, int outUnitsConv, int startStageNum, int stopStageNum, boolean useEstDelayFlag, boolean showInputFlag) {
        return calculateResponse(freqArray, logSpacingFlag, outUnitsConv, startStageNum, stopStageNum, useEstDelayFlag, showInputFlag, false, false, 0.0, false, false);
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqArray
     *            an array of frequency values to use.
     * @param logSpacingFlag
     *            true to indicate that the frequency spacing is logarithmic;
     *            false to indicate linear spacing.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double[] freqArray, boolean logSpacingFlag, int outUnitsConv, int startStageNum, int stopStageNum) {
        return calculateResponse(freqArray, logSpacingFlag, outUnitsConv, startStageNum, stopStageNum, false, false);
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqVal
     *            a single frequency value to use.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @param useEstDelayFlag
     *            true to use estimated delay in phase calculations on
     *            asymmetrical FIR filters.
     * @param showInputFlag
     *            true to show RESP input text (sent to stdout).
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double freqVal, int outUnitsConv, int startStageNum, int stopStageNum, boolean useEstDelayFlag, boolean showInputFlag) {
        //create array with single frequency value:
        return calculateResponse((new double[] { freqVal }), true, outUnitsConv, startStageNum, stopStageNum, useEstDelayFlag, showInputFlag);
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqVal
     *            a single frequency value to use.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @param startStageNum
     *            if greater than zero then the start of the range of stage
     *            sequence numbers to use, otherwise all stages are used.
     * @param stopStageNum
     *            if greater than zero then the end of the range of stage
     *            sequence numbers to use, otherwise only the single stage
     *            specified by 'startStageNum' is used.
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double freqVal, int outUnitsConv, int startStageNum, int stopStageNum) {
        return calculateResponse(freqVal, outUnitsConv, startStageNum, stopStageNum, false, false);
    }

    /**
     * Calculates a response for each of the given frequencies. The array
     * containing the generated complex spectra response values may be fetched
     * via the 'getCSpectraArray()' method, and the amplitude/phase response
     * values may be calculated and returned via the 'getAmpPhaseArray()'
     * method. If 'normalizeResponse()' has not yet been called then it is
     * called first. Note that this method assumes that each stage contains only
     * one filter object in its 'filters[]' array.
     *
     * @param freqVal
     *            a single frequency value to use.
     * @param outUnitsConv
     *            output units conversion value for the requested output units
     *            type; one of the '..._UNIT_CONV' values.
     * @return true if successful, false if an error occurred (in which case
     *         'getErorMessage()' may be used to fetch information about the
     *         error).
     */
    public boolean calculateResponse(double freqVal, int outUnitsConv) {
        return calculateResponse(freqVal, outUnitsConv, 0, 0, false, false);
    }

    /**
     * Returns the array of complex spectra response values generated by
     * 'calculateResponse()'.
     *
     * @return The array of complex spectra response values generated by
     *         'calculateResponse()', or null if 'calculateResponse()' has not
     *         yet been performed.
     */
    public ComplexBlk[] getCSpectraArray() {
        return (respArraysInfoArray != null && respArraysInfoArray.length > 0 && respArraysInfoArray[0] != null) ? respArraysInfoArray[0].cSpectraArray : null;
    }

    /**
     * Calculates amplitude/phase values via the complex-spectra values from the
     * given reponse arrays/information object.
     *
     * @param rArrsInfoObj
     *            reponse arrays/information object to use.
     * @param respArrIdx
     *            associated index into 'respArraysInfoArray[]'.
     * @return true if successful; false if an error occurred.
     */
    protected boolean calcAmpPhaseArray(RespArraysInfo rArrsInfoObj, int respArrIdx) {
        int i = 0;
        final AmpPhaseBlk[] ampPhaseArr;
        try {
            final int len = rArrsInfoObj.cSpectraArray.length;
            ampPhaseArr = new AmpPhaseBlk[len];
            double real, imag;
            while (i < len) { //for each element in complex spectra array, convert to amp/phase
                real = rArrsInfoObj.cSpectraArray[i].real;
                imag = rArrsInfoObj.cSpectraArray[i].imag;
                ampPhaseArr[i] = new AmpPhaseBlk(Math.sqrt(real * real + imag * imag), Math.atan2(imag, real + 1.0e-200) * 180.0 / Math.PI);
                ++i;
            }
        } catch (Exception ex) { //some kind of error; set error message
            setErrorMessage("Exception error calculating amp/phase value" + ((rArrsInfoObj != null) ? (" for " + rArrsInfoObj.identifyStr) : StringUtils.EMPTY) + " (index=" + i + "):  " + ex);
            return false;
        }
        if (listInterpOutFlag) { //interpolate amp/phase values generated via List blockette
            try { //use req freqs, with any out-of-range freqs clipped
                  // (if all-stages entry then show "Note:" messages):
                final double[] newFreqArray = clipFreqArray(requestedFreqArray, rArrsInfoObj.frequencyArr, (respArrIdx == 0));
                if (newFreqArray.length <= 0) { //all requested frequencies were clipped
                    setErrorMessage("Error interpolating amp/phase output values:  " + "All requested freqencies out of range");
                    return false;
                }
                final CubicSpline splineObj = new CubicSpline();
                double[] listInterpAmpArr;
                double[] listInterpPhaseArr;

                try {
                    //interpolate List blockette amplitude values:
                    listInterpAmpArr = splineObj.calcSpline(rArrsInfoObj.frequencyArr, fetchAmpPhaAmpArray(ampPhaseArr), listInterpTension, 1.0, newFreqArray);
                } catch (IllegalArgumentException e) {
                    setErrorMessage("Error interpolating amplitude output values:  " + e.getLocalizedMessage());
                    return false;
                }
                //get generated phase data:
                final double[] srcPhaseArr = fetchAmpPhaPhaseArray(ampPhaseArr);
                //unwrap phase data:
                final double[] unwrappedPhaseArr = unwrapPhaseArray(srcPhaseArr);
                try {
                    //interpolate List blockette phase values:
                    listInterpPhaseArr = splineObj.calcSpline(rArrsInfoObj.frequencyArr, unwrappedPhaseArr, listInterpTension, 1.0, newFreqArray);
                } catch (IllegalArgumentException e) {
                    setErrorMessage("Error interpolating phase output values:  " + e.getLocalizedMessage());
                    return false;
                }
                //if unwrap flag not set and phase data was unwrapped
                // then wrap interpolated data:
                if (!unwrapPhaseFlag && unwrappedPhaseArr != srcPhaseArr) {
                    listInterpPhaseArr = wrapPhaseArray(listInterpPhaseArr);
                }
                //enter new frequency and amp/phase arrays:
                rArrsInfoObj.setFrequencyArray(newFreqArray);
                rArrsInfoObj.setAmpPhaseArray(listInterpAmpArr, listInterpPhaseArr);
                return true;
            } catch (Exception ex) { //some kind of error; set error message
                setErrorMessage("Exception error interpolating amp/phase output values" + ((rArrsInfoObj != null) ? (" for " + rArrsInfoObj.identifyStr) : StringUtils.EMPTY) + ":  " + ex);
                return false;
            }
        }
        if (unwrapPhaseFlag) { //flag set for unwrapping phase values (via "-unwrap" parameter)
            try { //get generated phase data:
                final double[] srcPhaseArr = fetchAmpPhaPhaseArray(ampPhaseArr);
                final double[] unwrappedPhaseArr = //unwrap phase data
                        unwrapPhaseArray(srcPhaseArr, true);
                if (unwrappedPhaseArr != srcPhaseArr) { //phase data changed; enter with new phase data
                    rArrsInfoObj.setAmpPhaseArray(fetchAmpPhaAmpArray(ampPhaseArr), unwrappedPhaseArr);
                    return true;
                }
            } catch (Exception ex) { //some kind of error; set error message
                setErrorMessage("Exception error unwrapping phase output values" + ((rArrsInfoObj != null) ? (" for " + rArrsInfoObj.identifyStr) : StringUtils.EMPTY) + ":  " + ex);
                return false;
            }
        }
        rArrsInfoObj.setAmpPhaseArray(ampPhaseArr); //enter generated array
        return true;
    }

    /**
     * Returns the array of amplitude/phase response values generated by
     * 'calculateResponse()'.
     *
     * @return The array of amplitude/phase response values generated by
     *         'calculateResponse()', or null if 'calculateResponse()' has not
     *         yet been performed or if an error occurs (in which case an error
     *         message may be fetched via the 'getErrorMessage()' method).
     */
    public AmpPhaseBlk[] getAmpPhaseArray() {
        //if not yet calculated then calc amp/phase values for all stages:
        if (respArraysInfoArray != null && respArraysInfoArray.length > 0 && respArraysInfoArray[0] != null) { //array of response arrays/info objs contains at least one entry
            //if amp/phase array already created then return it:
            if (respArraysInfoArray[0].ampPhaseArray != null) {
                return respArraysInfoArray[0].ampPhaseArray;
            }
            if (respArraysInfoArray[0].cSpectraArray != null) { //complex spectra array exists; calc amp/phase vals for all stages
                calcAmpPhaseArray(respArraysInfoArray[0], 0);
                return respArraysInfoArray[0].ampPhaseArray;
            }
        }
        //array of response arrays/info objs not setup; set error message:
        setErrorMessage("Method 'calculateResponse()' not yet performed");
        return null;
    }

    /**
     * Determines if the amp/phase array has been calculated (via the
     * 'getAmpPhaseArray()' method being called).
     *
     * @return true if the amp/phase array has been calculated; false if not.
     */
    public boolean isAmpPhaseArrayCalculated() {
        return (respArraysInfoArray != null && respArraysInfoArray.length > 0 && respArraysInfoArray[0] != null && respArraysInfoArray[0].ampPhaseArray != null);
    }

    /**
     * Returns the arrays of amplitude/phase response values generated by
     * 'calculateResponse()' for all stages in the response.
     *
     * @return An array of 'RespArraysInfo' objects whose amplitude/phase values
     *         have been calculated and entered, or null if
     *         'calculateResponse()' has not yet been performed or if an error
     *         occurs (in which case an error message may be fetched via the
     *         'getErrorMessage()' method).
     */
    public RespArraysInfo[] getAllStagesAmpPhaseArrays() {
        //if not yet calculated then calc amp/phase values for all stages:
        if (respArraysInfoArray != null && respArraysInfoArray.length > 0 && respArraysInfoArray[0] != null) { //array of response arrays/info objs contains at least one entry
            final boolean ampPhaseArrCreatedFlag;
            if (respArraysInfoArray[0].ampPhaseArray != null) { //amp/phase array has been created
                //if single entry or rest of stages already calculated
                // then return array of response arrays/info objects:
                if (respArraysInfoArray.length <= 1 || allStagesAmpPhaseCalcFlag) {
                    return respArraysInfoArray;
                }
                ampPhaseArrCreatedFlag = true; //indicate array created
            } else //amp/phase array has not been created
            {
                ampPhaseArrCreatedFlag = false; //ind array not created
            }
            if (respArraysInfoArray[0].cSpectraArray != null) { //complex spectra array exists
                //if amp/phase array not yet created then do it now:
                if (!ampPhaseArrCreatedFlag) {
                    calcAmpPhaseArray(respArraysInfoArray[0], 0);
                }
                if (!allStagesAmpPhaseCalcFlag) { //amp/phase values for rest of stages not yet calculated
                    allStagesAmpPhaseCalcFlag = true; //indicate calculated
                    for (int i = 1; i < respArraysInfoArray.length; ++i) { //for each remaining stage entry in array
                        //if entry exists then calculate amp/phase values for stage:
                        if (respArraysInfoArray[i] != null) {
                            calcAmpPhaseArray(respArraysInfoArray[i], i);
                        }
                    }
                }
                return respArraysInfoArray;
            }
        }
        //array of response arrays/info objs not setup; set error message:
        setErrorMessage("Method 'calculateResponse()' not yet performed");
        return null;
    }

    /**
     * Determines if any amplitude values are less than or equal to zero.
     *
     * @return true if any amplitude values are less than or equal to zero.
     */
    public boolean anyAmpsNotPositive() {
        if (!anyAmpsNotPosSetupFlag) { //'anyAmpsNotPositiveFlag' flag not yet setup
            try {
                final AmpPhaseBlk[] ampPhaseArr = getAmpPhaseArray();
                if (ampPhaseArr != null) //calc & fetch A/P data
                { //amplitude-phase array fetched OK
                    for (AmpPhaseBlk element : ampPhaseArr) { //for each amplitude value
                        if (element.amp <= 0.0) { //value is not positive
                            anyAmpsNotPositiveFlag = true; //setup to return true
                            break; //exit loop
                        }
                    }
                }
            } catch (Exception ex) { //some kind of exception error; setup to return false
                anyAmpsNotPositiveFlag = false;
            }
            anyAmpsNotPosSetupFlag = true; //ind 'anyAmpsNotPosFlag' is setup
        }
        return anyAmpsNotPositiveFlag;
    }

    /**
     * Determines, for all stages in the response, if any amplitude values are
     * less than or equal to zero.
     *
     * @return true if, for all stages in the response, any amplitude values are
     *         less than or equal to zero.
     */
    public boolean allStagesAnyAmpsNotPositive() {
        if (!allStagesAnyAmpsNotPosSetupFlag) { //'allStagesAnyAmpsNotPositiveFlag' flag not yet setup
            try {
                if (respArraysInfoArray != null
                        && respArraysInfoArray.length > 0
                        && respArraysInfoArray[0] != null
                        && (respArraysInfoArray[0].cSpectraArray != null || respArraysInfoArray[0].ampPhaseArray != null)) {
                    getAllStagesAmpPhaseArrays(); //calc amp/phase for all stages
                    AmpPhaseBlk[] ampPhaseArr;
                    outerLoop: for (int stgIdx = 0; stgIdx < respArraysInfoArray.length; ++stgIdx) { //for each response/stage entry in array
                        if (respArraysInfoArray[stgIdx] != null && (ampPhaseArr = respArraysInfoArray[stgIdx].ampPhaseArray) != null) { //amplitude-phase array fetched OK
                            for (int i = 0; i < ampPhaseArr.length; ++i) { //for each amplitude value
                                if (ampPhaseArr[i].amp <= 0.0) { //value is not positive
                                    allStagesAnyAmpsNotPositiveFlag = true; //setup ret value
                                    break outerLoop; //exit loop
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) { //some kind of exception error; setup to return false
                allStagesAnyAmpsNotPositiveFlag = false;
            }
            allStagesAnyAmpsNotPosSetupFlag = true; //indicate flag is setup
        }
        return allStagesAnyAmpsNotPositiveFlag;
    }

    /**
     * Calculates the response's sample interval, delay and correction applied
     * values. This is method is called automatically the first time any of the
     * values are requested.
     */
    protected void calcIntvlDelayCorrValues() {
        if (respObj == null || respObj.stages == null) {
            return;
        }
        //clear interval/delay/correction values for response:
        sampleInterval = estimatedDelay = correctionApplied = calculatedDelay = 0.0;
        final int numStages = respObj.stages.length;
        Stage stageObj;
        Filter filterObj;
        CoefficientFilter coeffFilterObj;
        Decimation deciObj;
        Double doubleObj;
        double stageSampInt;
        for (int stageNum = 0; stageNum < numStages; ++stageNum) { //for each stage in response
            stageObj = respObj.stages[stageNum];
            //        if(XDEBUG_FLAG)
            //        {     //send debug message to default log file
            //          LogFile.getGlobalLogObj().debug("Calculating sampInt/delay/" +
            //                                   "corr values for stage " + (stageNum+1));
            //        }
            if ((stageObj != null) && (stageObj.the_decimation != null && stageObj.the_decimation.length > 0 && (deciObj = stageObj.the_decimation[0]) != null)) { //stage has a valid Decimation object
                doubleObj = RespUtils.deciToSampIntTime(deciObj);
                //          if(XDEBUG_FLAG)
                //          {        //send debug message to default log file
                //            LogFile.getGlobalLogObj().debug("  Stage contains decimation");
                //          }
                if (doubleObj != null) { //sample interval calculated OK
                    //calculate using final stages's sample interval:
                    sampleInterval = // (save sample interval value for stage)
                            (stageSampInt = doubleObj.doubleValue()) * deciObj.factor;
                    //            if(XDEBUG_FLAG)
                    //            {      //send debug message to default log file
                    //              LogFile.getGlobalLogObj().debug("  dec_samp_int=" +
                    //                          doubleObj + ", sampleInterval=" + sampleInterval);
                    //            }
                } else //error calculating sample interval value
                {
                    stageSampInt = 0.0; //set value to zero
                } //get estimated delay value:
                doubleObj = RespUtils.quantityToIntTime(deciObj.estimated_delay);
                if (doubleObj != null) { //value fetched OK
                    estimatedDelay += doubleObj.doubleValue(); //add to total
                    //            if(XDEBUG_FLAG)
                    //            {      //send debug message to default log file
                    //              LogFile.getGlobalLogObj().debug("  dec_est_delay=" +
                    //                          doubleObj + ", estimatedDelay=" + estimatedDelay);
                    //            }
                }
                doubleObj = RespUtils.quantityToIntTime(deciObj.correction_applied);
                //get correction applied value:
                if (doubleObj != null) { //value fetched OK
                    correctionApplied += doubleObj.doubleValue(); //add to total
                    //            if(XDEBUG_FLAG)
                    //            {      //send debug message to default log file
                    //              LogFile.getGlobalLogObj().debug("  dec_corr_app=" +
                    //                  doubleObj + ", correctionApplied=" + correctionApplied);
                    //            }
                }
                //check if stage has coefficients filter:
                if (stageObj.filters != null && stageObj.filters.length > 0 && (filterObj = stageObj.filters[0]) != null && filterObj.discriminator().equals(FilterType.COEFFICIENT)) { //stage has coefficients filter
                    //            if(XDEBUG_FLAG)
                    //            {      //send debug message to default log file
                    //              LogFile.getGlobalLogObj().debug(
                    //                                    "  Stage contains coefficients filter");
                    //            }
                    //get handle to coefficient filter object:
                    coeffFilterObj = filterObj.coeff_filter();
                    //added "&& coeffFilterObj.numerator.length > 0"
                    // to the 'if' statement below - 5/18/2010 [ET]:
                    if (coeffFilterObj.denominator.length <= 0 && coeffFilterObj.numerator.length > 0) { //no denominators (FIR filter) and at least one numerator
                        calculatedDelay += //calc and add to calculated delay
                                (((double) (coeffFilterObj.numerator.length - 1)) / 2) * stageSampInt;
                        //              if(XDEBUG_FLAG)
                        //              {      //send debug message to default log file
                        //                LogFile.getGlobalLogObj().debug(
                        //                                "  Stage is FIR filter with >0 numerators");
                        //                LogFile.getGlobalLogObj().debug("  #numers=" +
                        //                       coeffFilterObj.numerator.length + ", stageSampInt=" +
                        //                     stageSampInt + ", calculatedDelay=" + calculatedDelay);
                        //              }
                    }
                }
            }
        }
        calcIntDelayCorrFlag = true; //indicate that this method has run
    }

    /**
     * @return the (final) sample interval value for the response.
     */
    public double getSampleInterval() {
        if (!calcIntDelayCorrFlag) //if not yet run then
        {
            calcIntvlDelayCorrValues(); //call calculating method
        }
        return sampleInterval;
    }

    /**
     * @return the estimated delay value for the response.
     */
    public double getEstimatedDelay() {
        if (!calcIntDelayCorrFlag) //if not yet run then
        {
            calcIntvlDelayCorrValues(); //call calculating method
        }
        return estimatedDelay;
    }

    /**
     * @return the correction applied value for the response.
     */
    public double getCorrectionApplied() {
        if (!calcIntDelayCorrFlag) //if not yet run then
        {
            calcIntvlDelayCorrValues(); //call calculating method
        }
        return correctionApplied;
    }

    /**
     * @return the calculated delay value for the response.
     */
    public double getCalculatedDelay() {
        if (!calcIntDelayCorrFlag) //if not yet run then
        {
            calcIntvlDelayCorrValues(); //call calculating method
        }
        return calculatedDelay;
    }

    /**
     * Returns the response-sensitivity factor.
     *
     * @return A new Float object containing the response-sensitivity factor, or
     *         null if none available.
     */
    public Float getRespSensitFactor() {
        return (respObj != null && respObj.the_sensitivity != null) ? Float.valueOf(respObj.the_sensitivity.sensitivity_factor) : null;
    }

    /**
     * Returns the response-sensitivity frequency.
     *
     * @return A new Float object containing the response-sensitivity frequency,
     *         or null if none available.
     */
    public Float getRespSensitFreq() {
        return (respObj != null && respObj.the_sensitivity != null) ? Float.valueOf(respObj.the_sensitivity.frequency) : null;
    }

    /**
     * Returns the normalization factor for the first stage of the response.
     *
     * @return A new Float object containing the normalization factor for the
     *         first stage of the response, or null if none available.
     */
    public Float getRespS1NormFactor() {
        final Stage stageObj;
        final Normalization normObj;
        return (respObj != null
                && respObj.stages != null
                && respObj.stages.length > 0
                && (stageObj = respObj.stages[0]) != null
                && stageObj.the_normalization != null
                && stageObj.the_normalization.length > 0
                && (normObj = stageObj.the_normalization[0]) != null) ? Float.valueOf(normObj.ao_normalization_factor) : null;
    }

    /**
     * Returns the normalization frequency for the first stage of the response.
     *
     * @return A new Float object containing the normalization frequency for the
     *         first stage of the response, or null if none available.
     */
    public Float getRespS1NormFreq() {
        final Stage stageObj;
        final Normalization normObj;
        return (respObj != null
                && respObj.stages != null
                && respObj.stages.length > 0
                && (stageObj = respObj.stages[0]) != null
                && stageObj.the_normalization != null
                && stageObj.the_normalization.length > 0
                && (normObj = stageObj.the_normalization[0]) != null) ? Float.valueOf(normObj.normalization_freq) : null;
    }

    /**
     * @return the sensitivity value calculated by 'normalizeResponse()', or 0.0
     *         if 'normalizeResponse()' has not been performed.
     */
    public double getCalcSensitivity() {
        return (calcSensitivityArray != null) ? calcSensitivityArray[0] : 0.0;
    }

    /**
     * @return the frequency used by 'normalizeResponse()', or 0.0 if
     *         'normalizeResponse()' has not been performed.
     */
    public double getCalcSenseFrequency() {
        return calcSenseFrequency;
    }

    /**
     * @return the first input unit processed by 'normalizeResponse()' or
     *         'calculateResponse()', or null if neither function has been
     *         called.
     */
    public Unit getFirstUnitProc() {
        return firstUnitProc;
    }

    /**
     * @return the last output unit processed by 'normalizeResponse()' or
     *         'calculateResponse()', or null if neither function has been
     *         called.
     */
    public Unit getLastUnitProc() {
        return lastUnitProc;
    }

    /**
     * @return the number of stages used in the last call to
     *         'calculateResponse()', or 0 if 'calculateResponse()' has not been
     *         called.
     */
    public int getNumCalcStages() {
        return numCalcStages;
    }

    /**
     * Determines if phase output values are to be unwrapped.
     *
     * @return true if phase output values are to be unwrapped; false if not.
     */
    public boolean getUnwrapPhaseFlag() {
        return unwrapPhaseFlag;
    }

    /**
     * Determines if stage 0 (total) sensitivity was used to calculate response.
     *
     * @return true if stage 0 (total) sensitivity was used to calculate
     *         response; false if computed sensitivity was used.
     */
    public boolean getTotalSensitFlag() {
        return totalSensitFlag;
    }

    /**
     * Returns true if the response contains a List (blockette 55) stage and
     * 'calculateResponse()' has been called.
     *
     * @return true if the response contains a List (blockette 55) stage and
     *         'calculateResponse()' has been called.
     */
    public boolean getListStageFlag() {
        return listStageFlag;
    }

    /**
     * Returns true if List-blockette "output" interpolation is enabled.
     *
     * @return true if List-blockette "output" interpolation is enabled.
     */
    public boolean getListInterpOutFlag() {
        return listInterpOutFlag;
    }

    /**
     * Returns true if List-blockette "input" interpolation is enabled.
     *
     * @return true if List-blockette "input" interpolation is enabled.
     */
    public boolean getListInterpInFlag() {
        return listInterpInFlag;
    }

    /**
     * Returns the array of frequencies used to calculate response output.
     *
     * @return the array of frequencies used to calculate response output.
     */
    public double[] getCalcFreqArray() {
        final double[] dArr;
        return (respArraysInfoArray != null && respArraysInfoArray[0] != null && (dArr = respArraysInfoArray[0].frequencyArr) != null) ? dArr : requestedFreqArray;
    }

    /**
     * Enters error message (if none previously entered).
     *
     * @param str
     *            message string.
     */
    private void setErrorMessage(String str) {
        if (errorMessage == null) //if no previous error then
        {
            errorMessage = str; //set error message
        }
    }

    /**
     * Returns true if an error was detected.
     *
     * @return true if an error was detected. The error message may be fetched
     *         via the 'getErrorMessage()' method.
     */
    public boolean getErrorFlag() {
        return (errorMessage != null);
    }

    /**
     * Returns message string for last error (or 'No error' if none).
     *
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
     * Enters info message.
     *
     * @param str
     *            message string.
     */
    protected void setInfoMessage(String str) {
        infoMessage = str; //set info message
    }

    /**
     * Determines an info message has been entered. The info message may be
     * fetched via the 'getInfoMessage()' method.
     *
     * @return true if an info message has been entered; false if not.
     */
    public boolean getInfoFlag() {
        return (infoMessage != null);
    }

    /**
     * Returns the info message string (or 'No message' if none).
     *
     * @return The info message string (or 'No message' if none).
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
     * Returns a type identification string for the given stage.
     *
     * @param stageObj
     *            stage object to use.
     * @return A type identification string for the given stage.
     */
    protected static String getTypeStrForStage(Stage stageObj) {
        Filter filterObj;
        if ((stageObj != null) && (stageObj.filters != null && stageObj.filters.length > 0)) { //stage contains filters (not gain-only stage)
            filterObj = stageObj.filters[0];
            if (filterObj != null) { //filter object not null
                //if poles/zeros type filter then return type ID string:
                if (filterObj.discriminator().equals(FilterType.POLEZERO)) {
                    return getPolesZerosStageTypeStr(stageObj).typeStr;
                }
                if (filterObj.discriminator().equals(FilterType.COEFFICIENT)) { //coefficients type filter
                    CoefficientFilter coeffFilterObj;
                    int numDenoms;
                    coeffFilterObj = filterObj.coeff_filter();
                    if (coeffFilterObj != null) { //coefficient filter object fetched OK
                        numDenoms = (coeffFilterObj.denominator != null) ? coeffFilterObj.denominator.length : 0;
                    } else //coefficient filter object not OK
                    {
                        numDenoms = 0;
                    }
                    //return type ID string:
                    return getCoefficientsStageTypeStr(stageObj, numDenoms).typeStr;
                }
                if (filterObj.discriminator().equals(FilterType.LIST)) {
                    return LIST_STR; //response list filter
                }
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Returns a type identification string for the given "poles and zeros"
     * stage object. A normalization identification string is also returned.
     *
     * @param stageObj
     *            stage object to use.
     * @return A new 'TypeNormStringBlock' object containing the type and
     *         normalization identification strings.
     */
    protected static TypeNormStringBlock getPolesZerosStageTypeStr(Stage stageObj) {
        String typeStr;
        String normStr = H0_STR; //setup initial normalization value
        if (stageObj.type != null) { //stage type object OK; set string for type
            if (stageObj.type.equals(TransferType.LAPLACE)) {
                typeStr = "LAPLACE";
                normStr = A0_STR; //analog stage; change norm field name
            } else if (stageObj.type.equals(TransferType.ANALOG)) {
                typeStr = ANALOG_STR;
                normStr = A0_STR; //analog stage; change norm field name
            } else if (stageObj.type.equals(TransferType.COMPOSITE)) {
                typeStr = COMPOSITE_STR;
            } else if (stageObj.type.equals(TransferType.DIGITAL)) {
                typeStr = "IIR_PZ";
            } else {
                typeStr = StringUtils.EMPTY;
            }
        } else //stage type object null
        {
            typeStr = StringUtils.EMPTY;
        }
        //create and return block containing strings:
        return new TypeNormStringBlock(typeStr, normStr);
    }

    /**
     * Returns a type identification string for the given "coefficients" stage
     * object. A normalization identification string is also returned.
     *
     * @param stageObj
     *            stage object to use.
     * @param numDenoms
     *            number of denominators in filter for stage.
     * @return A new 'TypeNormStringBlock' object containing the type and
     *         normalization identification strings.
     */
    protected static TypeNormStringBlock getCoefficientsStageTypeStr(Stage stageObj, int numDenoms) {
        String typeStr;
        String normStr = H0_STR; //setup initial normalization value
        if (stageObj.type != null) { //stage type object OK; set string for type
            if (stageObj.type.equals(TransferType.DIGITAL)) {
                typeStr = (numDenoms > 0) ? "IIR_COEFFS" : determineFirTypeStr(stageObj);
            } else if (stageObj.type.equals(TransferType.LAPLACE) || stageObj.type.equals(TransferType.ANALOG)) {
                typeStr = ANALOG_STR;
                normStr = A0_STR; //analog stage; change norm field name
            } else if (stageObj.type.equals(TransferType.COMPOSITE)) {
                typeStr = COMPOSITE_STR;
            } else {
                typeStr = StringUtils.EMPTY;
            }
        } else //stage type object null
        {
            typeStr = StringUtils.EMPTY;
        }
        //create and return block containing strings:
        return new TypeNormStringBlock(typeStr, normStr);
    }

    /**
     * Determines the symmetry type for the given FIR filter.
     *
     * @param filterObj
     *            FIR-filter object.
     * @return One of the 'FIR_...' values.
     */
    protected static int determineFirTypeVal(Filter filterObj) {
        //"B" FIR_SYM_1 odd number of values
        //0 1 2 3 4     numNumerators = 5
        //a b c b a
        //"C" FIR_SYM_2 even number of values
        //0 1 2 3 4 5   numNumerators = 6
        //a b c c b a
        try {
            final CoefficientFilter coeffFilterObj;
            final int numNumerators;
            final CoefficientErrored[] numeratorsArr;
            if (filterObj != null && (coeffFilterObj = filterObj.coeff_filter()) != null && (numeratorsArr = coeffFilterObj.numerator) != null && (numNumerators = numeratorsArr.length) > 1) { //numerators array OK and at least 2 entries
                int i = 0, j = numNumerators - 1;
                CoefficientErrored coeff1Obj, coeff2Obj;
                do { //for each potential pair of symmetrical entries; check values
                    coeff1Obj = numeratorsArr[i];
                    coeff2Obj = numeratorsArr[j];
                    if (coeff1Obj != coeff2Obj && (coeff1Obj.value != coeff2Obj.value || coeff1Obj.error != coeff2Obj.error)) { //symmetry pairs do not match
                        return FIR_ASYM; //indicate asymmetrical
                    }
                    ++i; //move to next pair
                    --j;
                } while (i < j); //loop if more pairs left to check
                //indicate symmetrical, type based on odd vs. even
                return (i == j) ? FIR_SYM1 : FIR_SYM2;
            }
        } catch (Exception ex) { //some kind of exception error; just return 'FIR_UNKNOWN'
            ex.printStackTrace();
        }
        return FIR_UNKNOWN;
    }

    /**
     * Determines the symmetry type for the given stage object containing an
     * FIR-filter object.
     *
     * @param stageObj
     *            stage object containing filter object.
     * @return A string indicating the FIR-filter type.
     */
    protected static String determineFirTypeStr(Stage stageObj) {
        //get filter object from stage object:
        final Filter filterObj = (stageObj.filters != null && stageObj.filters.length > 0) ? stageObj.filters[0] : null;
        switch (determineFirTypeVal(filterObj)) {
        case FIR_ASYM:
            return "FIR_ASYM";
        case FIR_SYM1:
            return "FIR_SYM1";
        case FIR_SYM2:
            return "FIR_SYM2";
        default:
            return "FIR";
        }
    }

    /**
     * Returns a modified version of the given 'srcArr' array with any of its
     * entries outside of the given 'chkArr' array clipped. If any entries are
     * clipped and 'showNotesFlag'==true then a message is sent to 'stderr'.
     *
     * @param srcArr
     *            source array.
     * @param chkArr
     *            check array.
     * @param showNotesFlag
     *            true to send "Note:" messages to 'stderr'.
     * @return A modified version of the given 'srcArr' array, or the original
     *         array if no entries were clipped.
     */
    protected static double[] clipFreqArray(double[] srcArr, double[] chkArr, boolean showNotesFlag) {
        final int srcArrLen;
        if (chkArr.length > 0 && (srcArrLen = srcArr.length) > 0) { //arrays contain data
            double firstVal = chkArr[0]; //get first and last "check" values
            double lastVal = chkArr[chkArr.length - 1];
            if (firstVal > lastVal) { //first "check" value larger than last; swap them
                final double tmpVal = firstVal;
                firstVal = lastVal;
                lastVal = tmpVal;
            }
            int sPos = 0;
            while (sPos < srcArrLen && (srcArr[sPos] < firstVal || srcArr[sPos] > lastVal)) { //for each out-of-range entry at beginning of "source" array
                ++sPos;
            }
            //if out-of-range entries found at beginning of "source" array
            // and last clipped value is within 0.0001% of first "check"
            // value then setup to replace it with first "check" value:
            final boolean fixFirstFlag;
            if (sPos > 0 && Math.abs(firstVal - srcArr[sPos - 1]) < firstVal * 1e-6) {
                --sPos; //restore clipped value
                fixFirstFlag = true; //indicate value should be "fixed"
            } else {
                fixFirstFlag = false;
            }
            int ePos = srcArrLen - 1;
            while (ePos > 0 && (srcArr[ePos] > lastVal || srcArr[ePos] < firstVal)) { //for each out-of-range entry at end of "source" array
                --ePos;
            }
            //if out-of-range entries found at end of "source" array
            // and last clipped value is within 0.0001% of last "check"
            // value then setup to replace it with last "check" value:
            final boolean fixLastFlag;
            if (ePos < srcArrLen - 1 && Math.abs(srcArr[ePos + 1] - lastVal) < lastVal * 1e-6) {
                ++ePos; //restore clipped value
                fixLastFlag = true; //indicate value should be "fixed"
            } else {
                fixLastFlag = false;
            }
            if (sPos > ePos) //if all values clipped then
            {
                return new double[0]; //return empty array
            }
            final int retArrLen = ePos - sPos + 1;
            if (retArrLen < srcArrLen || fixFirstFlag || fixLastFlag) { //at least one entry was clipped or first or last value "fixed"
                //create new, clipped array
                final double[] retArr = new double[retArrLen];
                for (int i = 0; i < retArrLen; ++i) //copy over entries
                {
                    retArr[i] = srcArr[i + sPos];
                }
                if (fixFirstFlag) //if indicator flag then
                {
                    retArr[0] = firstVal; //"fix" first entry
                }
                if (fixLastFlag) //if indicator flag then
                {
                    retArr[retArrLen - 1] = lastVal; //"fix" last entry
                }
                if (showNotesFlag) { //"Note:" messages to 'stderr' enabled
                    if (sPos > 0) { //at least one entry clipped from beginning; show note
                        System.err.println("Note:  " + sPos + " frequenc" + ((sPos != 1) ? "ies" : "y") + " clipped from beginning of requested range");
                    }
                    ePos = srcArrLen - ePos - 1;
                    if (ePos > 0) { //at least one entry clipped from beginning; show note
                        System.err.println("Note:  " + ePos + " frequenc" + ((ePos != 1) ? "ies" : "y") + " clipped from end of requested range");
                    }
                }
                return retArr;
            }
        }
        return srcArr;
    }

    /**
     * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
     * adding +/-360 to portions of the dataset to make all the array values be
     * between -180 and +180 (inclusive). This method "unwraps" the given array
     * by detecting transitions where the dataset has been "wrapped" and adding
     * +/-360 to restore the "original" values.
     *
     * @param srcPhaseArr
     *            phase array to unwrap.
     * @param firstNonNegFlag
     *            true if first phase value should always be made non-negative;
     *            false if not.
     * @return A new 'double' array containing the unwrapped values, or the
     *         given array if it was not wrapped.
     */
    protected static double[] unwrapPhaseArray(double[] srcPhaseArr, boolean firstNonNegFlag) {
        final int srcPhaseArrLen = srcPhaseArr.length;
        if (srcPhaseArrLen <= 0) {
            return srcPhaseArr; //if source array empty then just return
        }
        final double[] retArr = new double[srcPhaseArrLen];
        double offsetVal = 0.0; //offset value for unwrapping
        boolean wrapFlag = false; //flag set true if any unwrapping
        double prevPhaseVal = srcPhaseArr[0]; //initialize "previous" value
        if (firstNonNegFlag && prevPhaseVal < 0.0) { //flag set and first value is negative
            prevPhaseVal += 360.0; //add offset to value
            offsetVal = 360.0; //set new offset
            wrapFlag = true; //indicate unwrapping
        }
        retArr[0] = prevPhaseVal; //set first value in return array
        double newPhaseVal, diff;
        for (int i = 1; i < srcPhaseArrLen; ++i) { //for each remaining value in source array
            newPhaseVal = srcPhaseArr[i] + offsetVal;
            diff = newPhaseVal - prevPhaseVal;
            if (diff > 180.0) { //phase "wrap" transition detected
                offsetVal -= 360.0; //adjust offset
                newPhaseVal -= 360.0; //adjust phase value
                wrapFlag = true; //indicate unwrapping
            } else if (diff < -180.0) { //phase "wrap" transition detected
                offsetVal += 360.0; //adjust offset
                newPhaseVal += 360.0; //adjust phase value
                wrapFlag = true; //indicate unwrapping
            }
            //enter value into return array and set "previous" value:
            retArr[i] = prevPhaseVal = newPhaseVal;
        }
        //return generated array if unwrapped; source array if not:
        return wrapFlag ? retArr : srcPhaseArr;
    }

    /**
     * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
     * adding +/-360 to portions of the dataset to make all the array values be
     * between -180 and +180 (inclusive). This method "unwraps" the given array
     * by detecting transitions where the dataset has been "wrapped" and adding
     * +/-360 to restore the "original" values.
     *
     * @param srcPhaseArr
     *            phase array to unwrap.
     * @return A new 'double' array containing the unwrapped values, or the
     *         given array if it was not wrapped.
     */
    protected static double[] unwrapPhaseArray(double[] srcPhaseArr) {
        return unwrapPhaseArray(srcPhaseArr, false);
    }

    /**
     * Wraps the given array of 'phase' values. A phase array is "wrapped" by
     * adding +/-360 to portions of the dataset to make all the array values be
     * between -180 and +180 (inclusive).
     *
     * @param srcPhaseArr
     *            phase array to wrap.
     * @return A new 'double' array containing the wrapped values, or the given
     *         array if it did not need to be wrapped.
     */
    protected static double[] wrapPhaseArray(double[] srcPhaseArr) {
        final int srcPhaseArrLen = srcPhaseArr.length;
        if (srcPhaseArrLen <= 0) {
            return srcPhaseArr; //if source array empty then just return
        }
        double offsetVal = 0.0; //offset value for wrapping
        boolean wrapFlag = false; //flag set true if any wrapping
        double newPhaseVal = srcPhaseArr[0];
        //pre-check first phase value to make sure that if it's >360
        // or <360 then the initial offset is setup accordingly:
        if (newPhaseVal > 180.0) { //first phase value is too high
            do //set offset to put values in range
            {
                offsetVal -= 360.0;
            } while (newPhaseVal + offsetVal > 180.0);
        } else if (newPhaseVal < -180.0) { //first phase value is too low
            do //set offset to put values in range
            {
                offsetVal += 360.0;
            } while (newPhaseVal + offsetVal < -180.0);
        }
        final double[] retArr = new double[srcPhaseArrLen];
        for (int i = 0; i < srcPhaseArrLen; ++i) { //for each value in source array
            newPhaseVal = srcPhaseArr[i] + offsetVal;
            if (newPhaseVal > 180.0) { //phase value too high
                offsetVal -= 360.0; //adjust offset
                newPhaseVal -= 360.0; //adjust phase value
                wrapFlag = true; //indicate wrapping
            } else if (newPhaseVal < -180.0) { //phase value too low
                offsetVal += 360.0; //adjust offset
                newPhaseVal += 360.0; //adjust phase value
                wrapFlag = true; //indicate wrapping
            }
            retArr[i] = newPhaseVal;
        }
        //return generated array if wrapped; source array if not:
        return wrapFlag ? retArr : srcPhaseArr;
    }

    /**
     * Returns an array containing the amplitude values held by the given array
     * of 'AmpPhaseBlk' objects.
     *
     * @return A new 'double' array containing the amplitude values held by the
     *         given array of 'AmpPhaseBlk' objects, or an empty array if none
     *         are available.
     * @param ampPhaseArr
     *            array of 'AmpPhaseBlk' objects.
     */
    public static double[] fetchAmpPhaAmpArray(AmpPhaseBlk[] ampPhaseArr) {
        final int arrLen = (ampPhaseArr != null) ? ampPhaseArr.length : 0;
        final double[] retArr = new double[arrLen]; //amp values array
        for (int i = 0; i < arrLen; ++i) //copy over values
        {
            retArr[i] = ampPhaseArr[i].amp;
        }
        return retArr;
    }

    /**
     * Returns an array containing the phase values held by the given array of
     * 'AmpPhaseBlk' objects.
     *
     * @return A new 'double' array containing the phase values held by the
     *         given array of 'AmpPhaseBlk' objects, or an empty array if none
     *         are available.
     * @param ampPhaseArr
     *            array of 'AmpPhaseBlk' objects.
     */
    public static double[] fetchAmpPhaPhaseArray(AmpPhaseBlk[] ampPhaseArr) {
        final int arrLen = (ampPhaseArr != null) ? ampPhaseArr.length : 0;
        final double[] retArr = new double[arrLen]; //amp values array
        for (int i = 0; i < arrLen; ++i) //copy over values
        {
            retArr[i] = ampPhaseArr[i].phase;
        }
        return retArr;
    }

    /**
     * Calculates the response of an analog poles/zeros filter.
     *
     * @param filterObj
     *            a poles/zeros filter object.
     * @param normFact
     *            the normalization factor to use.
     * @param freq
     *            the frequency value to use. If a Laplace filter then the
     *            frequency should be multplied by 2*pi.
     * @return A 'ComplexBlk' object containing the response.
     */
    public static ComplexBlk analogTrans(PoleZeroFilter filterObj, double normFact, double freq) {
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("analogTrans() input:  norm=" +
        //                                               normFact + ", freq=" + freq);
        //    }
        final ComplexBlk omega = new ComplexBlk(0.0, freq);
        final ComplexBlk num = new ComplexBlk(1.0, 1.0);
        final ComplexBlk denom = new ComplexBlk(1.0, 1.0);
        //get number of zeros:
        final int numZeros = (filterObj != null && filterObj.zeros != null) ? filterObj.zeros.length : 0;
        //get number of poles:
        final int numPoles = (filterObj != null && filterObj.poles != null) ? filterObj.poles.length : 0;
        int i;
        for (i = 0; i < numZeros; i++) { //for each zero, numerator=numerator*(omega-zero[i])
            //      temp.real = omega.real - filterObj.zeros[i].real;
            //      temp.imag = omega.imag - filterObj.zeros[i].imaginary;
            //      zMultiply(num,temp);
            num.zMultiply(omega.real - filterObj.zeros[i].real, omega.imag - filterObj.zeros[i].imaginary);
        }
        for (i = 0; i < numPoles; i++) { //for each pole, denominator=denominator*(omega-pole[i])
            //      temp.real = omega.real - filterObj.poles[i].real;
            //      temp.imag = omega.imag - filterObj.poles[i].imaginary;
            //      zMultiply(denom,temp);
            denom.zMultiply(omega.real - filterObj.poles[i].real, omega.imag - filterObj.poles[i].imaginary);
        }
        //gain*num/denum
        final ComplexBlk temp = new ComplexBlk(denom.real, -denom.imag);
        temp.zMultiply(num);
        final double modSquared = denom.real * denom.real + denom.imag * denom.imag;
        temp.real /= modSquared;
        temp.imag /= modSquared;
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("analogTrans() output:  out.real=" +
        //                   normFact*temp.real + ", out.imag=" + normFact*temp.imag);
        //    }
        return new ComplexBlk(normFact * temp.real, normFact * temp.imag);
    }

    /**
     * Calculates the response of a "Digital (Z - transform)" IIR poles/zeros
     * filter.
     *
     * @param filterObj
     *            a poles/zeros filter object.
     * @param normFact
     *            the normalization factor to use.
     * @param sIntervalTime
     *            the sample interval time to use.
     * @param wVal
     *            the frequency value to use.
     * @return A 'ComplexBlk' object containing the response.
     */
    public static ComplexBlk iirPzTrans(PoleZeroFilter filterObj, double normFact, double sIntervalTime, double wVal) {
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("iirPzTrans() input:  norm=" +
        //               normFact + ", sIntTime=" + sIntervalTime + ", wVal=" + wVal);
        //    }
        //get number of zeros:
        final int numZeros = (filterObj != null && filterObj.zeros != null) ? filterObj.zeros.length : 0;
        //get number of poles:
        final int numPoles = (filterObj != null && filterObj.poles != null) ? filterObj.poles.length : 0;
        //calculate radial freq. time sample interval:
        final double wsint = wVal * sIntervalTime;
        final double cosWsint = Math.cos(wsint);
        final double sinWsint = Math.sin(wsint);
        int i;
        double rVal, iVal, mod = 1.0, pha = 0.0;
        for (i = 0; i < numZeros; i++) { //for each zero
            rVal = cosWsint - filterObj.zeros[i].real; //10/22/2003:  + to -
            iVal = sinWsint - filterObj.zeros[i].imaginary; //10/22/2003:  + to -
            mod *= Math.sqrt(rVal * rVal + iVal * iVal);
            if (rVal != 0.0 || iVal != 0.0) {
                pha += Math.atan2(iVal, rVal);
            }
        }
        for (i = 0; i < numPoles; i++) { //for each pole
            rVal = cosWsint - filterObj.poles[i].real; //10/22/2003:  + to -
            iVal = sinWsint - filterObj.poles[i].imaginary; //10/22/2003:  + to -
            mod /= Math.sqrt(rVal * rVal + iVal * iVal);
            if (rVal != 0.0 || iVal != 0.0) {
                pha -= Math.atan2(iVal, rVal);
            }
        }
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("iizPzTrans() output:  out.real=" +
        //                               mod*Math.cos(pha)*normFact + ", out.imag=" +
        //                                               mod*Math.sin(pha)*normFact);
        //    }
        return new ComplexBlk(mod * Math.cos(pha) * normFact, mod * Math.sin(pha) * normFact);
    }

    /**
     * Calculates the response of a digital IIR filter. It evaluates phase
     * directly from imaginary and real parts of IIR filter coefficients.
     *
     * @param filterObj
     *            a coefficients filter object.
     * @param normFact
     *            the normalization factor to use.
     * @param sIntervalTime
     *            the sample interval time to use.
     * @param wVal
     *            the frequency value to use.
     * @return A 'ComplexBlk' object containing the response.
     */
    public static ComplexBlk iirTrans(CoefficientFilter filterObj, double normFact, double sIntervalTime, double wVal) {
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("iirTrans() input:  norm=" +
        //               normFact + ", sIntTime=" + sIntervalTime + ", wVal=" + wVal);
        //    }
        //get number of numerators:
        final int numNumers = (filterObj != null && filterObj.numerator != null) ? filterObj.numerator.length : 0;
        //get number of denominators:
        final int numDenoms = (filterObj != null && filterObj.denominator != null) ? filterObj.denominator.length : 0;
        //calculate radial freq. time sample interval:
        final double wsint = wVal * sIntervalTime;

        double xre, xim, phase, amp;
        int i;
        //process numerator:
        if (numNumers > 0) {
            xre = filterObj.numerator[0].value;
            xim = 0.0;
            for (i = 1; i < numNumers; ++i) {
                xre += filterObj.numerator[i].value * Math.cos(-(i * wsint));
                xim += filterObj.numerator[i].value * Math.sin(-(i * wsint));
            }
            amp = Math.sqrt(xre * xre + xim * xim);
            phase = Math.atan2(xim, xre);
        } else {
            amp = phase = 0.0;
        }
        //process denominator:
        if (numDenoms > 0) {
            xre = filterObj.denominator[0].value;
            xim = 0.0;
            for (i = 1; i < numDenoms; ++i) {
                xre += filterObj.denominator[i].value * Math.cos(-(i * wsint));
                xim += filterObj.denominator[i].value * Math.sin(-(i * wsint));
            }
            amp /= Math.sqrt(xre * xre + xim * xim);
            phase -= Math.atan2(xim, xre);
        }
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("iirTrans() output:  out.real=" +
        //                              amp*Math.cos(phase)*normFact + ", out.imag=" +
        //                                              amp*Math.sin(phase)*normFact);
        //    }
        return new ComplexBlk(amp * Math.cos(phase) * normFact, amp * Math.sin(phase) * normFact);
    }

    /**
     * Calculates the response of a digital FIR filter. Only the numerators of
     * the given filter object are used.
     *
     * @param filterObj
     *            a coefficients filter object.
     * @param normFact
     *            the normalization factor to use.
     * @param sIntervalTime
     *            the sample interval time to use.
     * @param wVal
     *            the frequency value to use.
     * @param firTypeVal
     *            one of the 'FIR_...' values.
     * @return A 'ComplexBlk' object containing the response.
     */
    public static ComplexBlk firTrans(CoefficientFilter filterObj, double normFact, double sIntervalTime, double wVal, int firTypeVal) {
        //    if(XDEBUG_FLAG)
        //    {    //send debug message to default log file
        //      LogFile.getGlobalLogObj().debug("firTrans() input:  norm=" +
        //               normFact + ", sIntTime=" + sIntervalTime + ", wVal=" + wVal);
        //    }
        //"B" FIR_SYM_1 odd number of values
        //0 1 2 3 4     numNumerators = 5  numCoeffs = 3
        //a b c b a
        //"C" FIR_SYM_2 even number of values
        //0 1 2 3 4 5   numNumerators = 6  numCoeffs = 3
        //a b c c b a
        //get number of coefficients (numerators):
        final int numCoeffs = (filterObj != null && filterObj.numerator != null) ? filterObj.numerator.length : 0;
        //calculate radial freq. time sample interval:
        final double wsint = wVal * sIntervalTime;
        if (numCoeffs <= 0) //if no coefficients then return dummy value
        {
            return new ComplexBlk(0.0, 0.0);
        }
        if (firTypeVal == FIR_SYM1) { //FIR type is symmetrical 1
            final int numNumerators = (numCoeffs + 1) / 2;
            int i, factVal;
            double rVal = 0.0;
            for (i = 0; i < (numNumerators - 1); ++i) {
                factVal = numNumerators - (i + 1);
                rVal += filterObj.numerator[i].value * Math.cos(wsint * factVal);
            }
            return new ComplexBlk((filterObj.numerator[i].value + (2.0 * rVal)) * normFact, 0.0);
        } else if (firTypeVal == FIR_SYM2) { //FIR type is symmetrical 2
            final int numNumerators = numCoeffs / 2;
            int i, factVal;
            double rVal = 0.0;
            for (i = 0; i < numNumerators; ++i) {
                factVal = numNumerators - (i + 1);
                rVal += filterObj.numerator[i].value * Math.cos(wsint * (factVal + 0.5));
            }
            return new ComplexBlk(2.0 * rVal * normFact, 0.0);
        } else { //FIR type is asymmetrical
            //check if all coefficients have the same value:
            double val = filterObj.numerator[0].value;
            int i = 0;
            do {
                if (++i >= numCoeffs) { //all coefficients checked
                    return new ComplexBlk(((wsint == 0.0) ? 1.0 : ((Math.sin(wsint / 2.0 * numCoeffs) / Math.sin(wsint / 2.0)) * val)), 0.0);
                }
            } while (filterObj.numerator[i].value == val);
            //process coefficients (not all same value):
            double rVal = 0.0, iVal = 0.0;
            for (i = 0; i < numCoeffs; ++i) {
                val = wsint * i;
                rVal += filterObj.numerator[i].value * Math.cos(val);
                iVal += filterObj.numerator[i].value * -Math.sin(val);
            }
            final double mod = Math.sqrt(rVal * rVal + iVal * iVal);
            //      double pha = Math.atan2(iVal,rVal);
            //revert to previous version after Gabi Laske report:
            double pha = Math.atan2(iVal, rVal) + (wVal * ((numCoeffs - 1) / 2.0) * sIntervalTime);
            rVal = mod * Math.cos(pha);
            iVal = mod * Math.sin(pha);
            //      if(XDEBUG_FLAG)
            //      {    //send debug message to default log file
            //        LogFile.getGlobalLogObj().debug("firTrans() output:  out.real=" +
            //                             rVal*normFact + ", out.imag=" + iVal*normFact);
            //      }
            return new ComplexBlk(rVal * normFact, iVal * normFact);
        }
    }

    /**
     * Checks if the FIR coefficients filter should be normalized to 1.0 at
     * frequency zero and adjusts the filter values if so.
     *
     * @param filterObj
     *            a coefficients filter object.
     * @param stageNum
     *            current stage number.
     */
    public void checkFixFirFreq0Norm(CoefficientFilter filterObj, int stageNum) {
        //get number of coefficients (numerators):
        final int numCoeffs = (filterObj != null && filterObj.numerator != null) ? filterObj.numerator.length : 0;
        if (numCoeffs > 0) { //more than zero coefficients (numerators)
            double sumVal = 0.0; //calculate sum of coefficients:
            for (int i = 0; i < numCoeffs; ++i) {
                sumVal += filterObj.numerator[i].value;
            }
            if (sumVal < (1.0 - FIR_NORM_TOL) || sumVal > (1.0 + FIR_NORM_TOL)) { //sum of coefficients is not 1.0
                //divide by sum to make sum of coefficients be 1.0:
                for (int i = 0; i < numCoeffs; ++i) {
                    filterObj.numerator[i].value /= sumVal;
                }
                //set info message:
                setInfoMessage("WARNING:  FIR blockette normalized, sum[coef]=" + sumVal + " (stage #" + (stageNum + 1) + ")");
            }
        }
    }

    /**
     * Converts the given unit object to a UNIT_CONV index values or -1 if a
     * conversion could not be made.
     *
     * @param unitObj
     *            unit object.
     * @return One of the '..._UNIT_CONV' index values or -1 if a conversion
     *         could not be made.
     */
    public static int toUnitConvIndex(Unit unitObj) {
        //convert to array of base Unit objects:
        final Unit[] unitsArr = RespUtils.toUnitsArray(unitObj);
        final int len = unitsArr.length;
        //first unit must be 'meters':
        if (len > 0 && (unitObj = unitsArr[0]) != null && unitObj.the_unit_base != null && unitObj.the_unit_base.equals(UnitBase.METER) && unitObj.exponent == 1) {
            if (len <= 1) //if only 'meters' then
            {
                return DISPLACE_UNIT_CONV; //return 'displacement'
            } //second unit must be seconds:
            unitObj = unitsArr[1];
            if (unitObj != null && unitObj.the_unit_base != null && unitObj.the_unit_base.equals(UnitBase.SECOND)) {
                if (unitObj.exponent == -1) {
                    if (len <= 2) //if no more units then
                    {
                        return VELOCITY_UNIT_CONV; //return 'velocity'
                    } //third unit must also be seconds:
                    if (len == 3 && (unitObj = unitsArr[2]) != null && unitObj.the_unit_base != null && unitObj.the_unit_base.equals(UnitBase.SECOND) && unitObj.exponent == -1) {
                        return ACCEL_UNIT_CONV;
                    }
                } else if (len == 2 && unitObj.exponent == -2) {
                    return ACCEL_UNIT_CONV; //if (seconds**2) then return 'accel'
                }
            }
        }
        return -1; //return no-conversion value
    }

    /**
     * Converts the given unit object to a string.
     *
     * @param unitObj
     *            unit object.
     * @return One of the unit conversion strings. ("def", "dis", "vel", "acc"),
     *         or an emptry string if a conversion could not be made.
     */
    public static String toUnitConvString(Unit unitObj) {
        final int val;
        return ((val = toUnitConvIndex(unitObj)) >= 0 && val < UNIT_CONV_STRS.length) ? UNIT_CONV_STRS[val] : "";
    }

    /**
     * Converts the given output-units-conversion index to a string.
     *
     * @param indexVal
     *            one of the ".._UNIT_CONV" index values.
     * @return One of the unit conversion strings ("Default", "Displacement",
     *         "Velocity", "Acceleration"), or "???" if a conversion could not
     *         be made.
     */
    public static String getLongUnitConvString(int indexVal) {
        return (indexVal >= 0 && indexVal < UNIT_CONV_LONGSTRS.length) ? UNIT_CONV_LONGSTRS[indexVal] : "???";
    }

    void setInputUnits(ResponseUnits units) {
        inputUnits = units;
    }

    /**
     * Class TypeNormStringBlock defines a data block containing two strings,
     * one for "type" and one for "normalization".
     */
    public static class TypeNormStringBlock {

        /**
         * Type string.
         */
        public final String typeStr;
        /**
         * Normalization string.
         */
        public final String normStr;

        /**
         * Creates a data block containing two strings, one for "type" and one
         * for "normalization".
         *
         * @param typeStr
         *            type string.
         * @param normStr
         *            normalization string.
         */
        public TypeNormStringBlock(String typeStr, String normStr) {
            this.typeStr = typeStr;
            this.normStr = normStr;
        }
    }
}
