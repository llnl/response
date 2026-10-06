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
//RespUtils.java:  A group of static utility functions for JEvalResp.
//
//  12/11/2001 -- [ET]  Initial release version.
//   2/27/2002 -- [ET]  Added 'containsGlobChars()', 'isBeforeTime()',
//                      'isAfterTime()', 'inTimeRange()' and
//                      'enterDefaultPropValue()' methods.
//   4/29/2002 -- [ET]  Added 'checkFreqArrayParams()' and
//                      'generateFreqArray()' methods.
//    6/7/2002 -- [ET]  Added version of 'findRespfiles()' that accepts
//                      Vector of initial 'File' objects.
//   7/11/2002 -- [ET]  Improved 'parseRespDate()' method to be able to
//                      handle any number of fractional-second digits after
//                      the decimal point; added 'addDateFlag' parameter
//                      option to 'channelIdToFName()' method.
//   7/15/2002 -- [ET]  Added 'compareTimes()' method.
//    8/6/2002 -- [ET]  Added 'fissTimeToDate()', 'fissTimeToString()',
//                      'fissDateToString()' and 'channelIdToHdrString()'
//                      methods; added 'respEndDateObj' parameter to
//                      'channelIdToEvString()' method; changed so
//                      'result' is constructed via
//                      "NumberFormat.getInstance()".
//   2/28/2005 -- [ET]  Modified to use 'UtilFns.createDateFormatObj()';
//                      modified 'fmtNumber()' to convert "infinity" or
//                      "NAN" to "*".
//   3/10/2005 -- [ET]  Added 'isNegOrZero()' methods; modified min-freq
//                      checks to use 'isNegOrZero()' method; added
//                      'fileObjPathToUrlStr()' method.
//    4/5/2005 -- [ET]  Added 'getTextFormatRespStr()' method; modified
//                      'respStrToUnit()' method to setup "name" field
//                      of returned unit object and to allow given unit
//                      string to be surrounded by '>' and '<' or
//                      parenthesis or to be in "(METER(SECOND^-1))" style.
//   4/21/2005 -- [ET]  Changed "@returns" to "@return".
//   5/25/2005 -- [ET]  Added 'isNegativeOne()', 'isGainObjValid()' and
//                      'isSensObjValid()' methods.
//   11/3/2005 -- [ET]  Modified 'processFileNameList()' method to prevent
//                      it from confusing the colon in URLs with UNIX-style
//                      filename separator characters.
//   8/24/2006 -- [ET]  Modified to support "Tesla" units; added static
//                      'UnitImpl' variables for "Pascal" and "Tesla"
//                      units.
//   1/19/2012 -- [ET]  Fixed bug in 'findRespfiles()' where site/location
//                      parameters were not properly processed.
//  10/21/2013 -- [ET]  Modified to support "Centigrade" units.
//   8/26/2014 -- [ET]  Added 'globStringSiteArrMatch()' method; modified
//                      'findRespfiles()' method to make "--" match
//                      "no location".
//
package com.isti.jevalresp;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import edu.iris.Fissures.Quantity;
import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.Unit;
import edu.iris.Fissures.UnitBase;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Decimation;
import edu.iris.Fissures.IfNetwork.Gain;
import edu.iris.Fissures.IfNetwork.Response;
import edu.iris.Fissures.IfNetwork.Sensitivity;
import edu.iris.Fissures.model.ISOTime;
import edu.iris.Fissures.model.UnitImpl;

/**
 * Class RespUtils is a group of static utility functions for JEvalResp.
 */
public class RespUtils {

    /**
     * Date value of "2599,365,23:59:59" for "no end date".
     */
    public static final Date NO_ENDDATE_OBJ = new Date(19880899199000L);

    /**
     * UnitImpl object for Pascal pressure units.
     */
    public static final UnitImpl PASCAL_UNITIMPL_OBJ = UnitImpl.divide(UnitImpl.NEWTON, UnitImpl.SQUARE_METER, "PASCAL");

    /**
     * UnitImpl object for Tesla magnetic flux density units.
     */
    public static final UnitImpl TESLA_UNITIMPL_OBJ = UnitImpl.divide(UnitImpl.divide(UnitImpl.divide(UnitImpl.KILOGRAM, UnitImpl.SECOND), UnitImpl.SECOND), UnitImpl.AMPERE, "TESLA");

    /**
     * UnitImpl object for Centigrade temperature units.
     */
    public static final UnitImpl CENTIGRADE_UNITIMPL_OBJ = new UnitImpl(UnitBase.KELVIN, UnitImpl.NONE, "CENTIGRADE", 1, 1);

    //characters to be quoted in 'globToRegExString()':
    private static final String regExQuoteChars = ".^$[]{}()-|+";
    private static final double SMALL_FLOAT_VAL = 1e-40;

    //private constructor so that no object instances may be created
    // (static access only)
    private RespUtils() {
    }

    /**
     * @return an 'evalresp' output filename built from the given 'ChannelId'
     *         object, with periods separating the names ("net.sta.loc.cha").
     * @param chObj
     *            channel ID
     * @param addDateFlag
     *            if true then a date code in the format
     *            ".yyyy.DDD.HH.mm.ss.SSS" built from the channel-ID will be
     *            appended to the returned string.
     */
    public static String channelIdToFName(ChannelId chObj, boolean addDateFlag) {
        if (chObj == null) //if null handle then
        {
            return "(null)"; //return indicator string
        }
        String dateStr;
        if (addDateFlag) { //date/time code to be added to name
            Date dateObj;
            try { //convert FISSURES time string to Date:
                dateObj = (new ISOTime(chObj.begin_time.date_time)).getDate();
            } catch (Exception ex) { //error converting date string
                dateObj = null;
            }
            if (dateObj != null) { //begin-date converted OK; format into filename string
                FastDateFormat format = FastDateFormat.getInstance("yyyy.DDD.HH.mm.ss.SSS", TimeZone.getTimeZone("GMT"));
                dateStr = "." + format.format(dateObj);
                int p = dateStr.lastIndexOf('.'); //check if any trailing zeros can be trimmed:
                if (p > 0 && dateStr.substring(p).equals(".000")) { //string ends zero milliseconds
                    dateStr = dateStr.substring(0, p); //trim trailing zeros
                    while ((p = dateStr.lastIndexOf('.')) > 0 && dateStr.substring(p).equals(".00")) { //for each trailing ".00"; trim it
                        dateStr = dateStr.substring(0, p);
                    }
                }
            } else //error converting date string
            {
                dateStr = StringUtils.EMPTY;
            }
        } else //no date/time code to be added
        {
            dateStr = StringUtils.EMPTY;
        }
        return channelIdToFName(chObj.station_code, chObj.channel_code, ((chObj.network_id != null) ? chObj.network_id.network_code : StringUtils.EMPTY), chObj.site_code) + dateStr;
    }

    /**
     * @return an 'evalresp' output filename built from the given
     *         station/channel/network names, with periods separating the names
     *         ("net.sta.loc.cha").
     * @param staName
     *            station name
     * @param chaName
     *            channel name
     * @param netName
     *            net name
     * @param siteName
     *            site name
     */
    private static String channelIdToFName(String staName, String chaName, String netName, String siteName) {
        return fixIdStr(netName) + "." + fixIdStr(staName) + "." + fixIdStr(siteName) + "." + fixIdStr(chaName);
    }

    /**
     * Fix ID string to be valid in a filename
     *
     * @param str
     *            string
     * @return ID string
     */
    private static String fixIdStr(String str) {
        if (str == null || (str = str.trim()).length() <= 0 || str.startsWith("?")) {
            return StringUtils.EMPTY; //if no data then return empty string
        } //create buffer version of string:
        final StringBuffer buff = new StringBuffer(str);
        final int len = str.length();
        char ch;
        for (int i = 0; i < len; ++i) { //for each character in string; check if OK in filename
            if (!Character.isLetterOrDigit(ch = str.charAt(i)) && ch != '_') {
                buff.setCharAt(i, '_'); //if not OK then replace with '_'
            }
        }
        return buff.toString(); //return string version of buffer
    }

    /**
     * @return a string representation of the given 'ChannelId' object.
     * @param chObj
     *            channel object
     * @param shortFlag
     *            if true then a short version of the information is returned.
     */
    public static String channelIdToString(ChannelId chObj, boolean shortFlag) {
        if (chObj == null) //if null handle then
        {
            return "(null)"; //return indicator string
        }
        if (shortFlag) { //short version requested
            return chObj.station_code
                    + ","
                    + chObj.channel_code
                    + ","
                    + ((chObj.network_id != null && chObj.network_id.network_code != null && chObj.network_id.network_code.length() > 0) ? chObj.network_id.network_code : "??")
                    + ","
                    + ((chObj.site_code != null && chObj.site_code.length() > 0) ? chObj.site_code : "??");
        }
        ByteArrayOutputStream btOutStream = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(btOutStream);
        out.println("Station code:  " + chObj.station_code);
        out.println("Channel code:  " + chObj.channel_code);
        if (chObj.network_id != null) {
            out.println("Network code:  " + chObj.network_id.network_code);
            out.println("Network begin_time:  " + ((chObj.network_id.begin_time != null) ? chObj.network_id.begin_time.date_time : "(null)"));
        } else {
            out.println("'ChannelId.network_id' is null");
        }
        out.println("Site code:  " + chObj.site_code);
        out.println("Begin time:  " + ((chObj.begin_time != null) ? chObj.begin_time.date_time : "(null)"));

        out.flush(); //flush data out to byte array
        final String retStr = btOutStream.toString(); //save string data
        out.close(); //close stream
        return retStr; //return string data
    }

    /**
     * @return the sensitivity frequency for the given Response object, or 0.0
     *         if it cannot be returned.
     * @param respObj
     *            response object
     */
    private static double getRespSensFrequency(Response respObj) {
        return (respObj != null && respObj.the_sensitivity != null) ? respObj.the_sensitivity.frequency : 0.0;
    }

    /**
     * Return the length (in seconds) of the sampling interval of the given
     * 'Decimation' object.
     *
     * @param deciObj
     *            decimation object
     * @return A 'Double' object containing the length value; or null if the
     *         given 'Decimation' object contains null handles or if its units
     *         type is not based on 'seconds'.
     */
    public static Double deciToSampIntTime(Decimation deciObj) {
        final Sampling sampObj;
        final Unit unitObj;
        //extract sampling and units objects and check units:
        if (deciObj == null
                || (sampObj = deciObj.input_rate) == null
                || sampObj.numPoints <= 0
                || sampObj.interval == null
                || (unitObj = sampObj.interval.the_units) == null
                || unitObj.the_unit_base == null
                || !unitObj.the_unit_base.equals(UnitBase.SECOND)) {
            return null;
        }
        return sampObj.interval.value / sampObj.numPoints * pow10(unitObj.power);
    }

    /**
     * Return the length (in seconds) specified by the given time-interval
     * Quantity object.
     *
     * @param intervalObj
     *            interval object
     * @return A 'Double' object containing the length value; or null if the
     *         given 'Quantity' object contains null handles or if its units
     *         type is not based on 'seconds'.
     */
    public static Double quantityToIntTime(Quantity intervalObj) {
        final Unit unitObj;
        //extract and check units object:
        if (intervalObj == null || (unitObj = intervalObj.the_units) == null || unitObj.the_unit_base == null || !unitObj.the_unit_base.equals(UnitBase.SECOND)) {
            return null;
        }
        return intervalObj.value * pow10(unitObj.power);
    }

    /**
     * Parses the given 'evalresp' format date string into a 'Date' object. The
     * date string must be in "yyyy,D,HH:mm:ss.SSS" format, but may also be a
     * truncated subset of the format (for example, "yyyy,D,HH:mm:ss" or
     * "yyyy,D"). Any number of fractional-second digits are allowed after the
     * decimal point.
     *
     * @param dateStr
     *            date string
     * @return a Date object, or null if error.
     */
    public static Date parseRespDate(String dateStr) {
        if (dateStr != null && dateStr.length() > 0) { //date string contains data
            //string must be in "yyyy,D,HH:mm:ss.SSS" format before
            // it is parsed; add to end of string if necessary:
            //trim any trailing comma or colon:
            String[] tokens = dateStr.split(",|:");
            StringBuilder str = new StringBuilder();
            if (tokens.length > 0) {
                str.append(tokens[0]).append(",");
                if (tokens.length > 1) {
                    str.append(tokens[1]).append(",");
                    if (tokens.length > 2) {
                        str.append(tokens[2]).append(":");
                        if (tokens.length > 3) {
                            str.append(tokens[3]).append(":");
                            if (tokens.length > 4) {
                                double tmp = Double.parseDouble(tokens[4]);
                                str.append(String.format("%06.3f", tmp));
                            } else {
                                str.append("00.000");
                            }
                        } else {
                            str.append("00:00.000");
                        }
                    } else {
                        str.append("00:00:00.000");
                    }
                } else {
                    str.append("000,00:00:00.000");
                }
                try {
                    FastDateFormat format = FastDateFormat.getInstance("yyyy,D,HH:mm:ss.SSS", TimeZone.getTimeZone("GMT"));
                    return format.parse(str.toString());
                } catch (Exception ex) {
                    Logger.getLogger(RespUtils.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                return null; // No delimited content...
            }
        }
        return null;

    }

    /**
     * @return the value of the given power of 10 (10**exp).
     * @param exp
     *            exponent value to use.
     */
    public static double pow10(int exp) {
        long pVal = 1;
        final int absExp = Math.abs(exp);
        for (int i = 0; i < absExp; ++i) {
            pVal *= 10;
        }
        return (exp < 0) ? 1.0 / pVal : (double) pVal;
    }

    /**
     * @return true if given value is near zero.
     * @param val
     *            value to compare.
     */
    public static boolean isZero(float val) {
        return (Math.abs(val) < (float) SMALL_FLOAT_VAL);
    }

    /**
     * @return true if both parts of the given complex value are near zero.
     * @param val
     *            value to compare.
     */
    public static boolean isZero(ComplexBlk val) {
        return (Math.abs(val.real) < SMALL_FLOAT_VAL) && (Math.abs(val.imag) < SMALL_FLOAT_VAL);
    }

    /**
     * Returns true if the given value is "nearly" equal to '-1'.
     *
     * @param val
     *            value to compare.
     * @return true if the given value is "nearly" equal to '-1'.
     */
    private static boolean isNegativeOne(float val) {
        return (val > -1) ? (val + 1 < (float) SMALL_FLOAT_VAL) : (-1 - val < (float) SMALL_FLOAT_VAL);
    }

    /**
     * Determines if the given gain object is "valid". The gain object is
     * "valid" if it is not null, its gain value is not zero, and its frequency
     * and gain values are not equal to '-1'.
     *
     * @param gainObj
     *            gain object to test.
     * @return true if the given gain object is "valid", false if not.
     */
    public static boolean isGainObjValid(Gain gainObj) {
        return (gainObj != null && !isZero(gainObj.gain_factor) && (!isNegativeOne(gainObj.frequency) || !isNegativeOne(gainObj.gain_factor)));
    }

    /**
     * Determines if the given sensitivity object is "valid". The sensitivity
     * object is "valid" if it is not null, its sensitivity value is not zero,
     * and its frequency and sensitivity values are not equal to '-1'.
     *
     * @param sensObj
     *            sensitivity object to test.
     * @return true if the given sensitivity object is "valid", false if not.
     */
    public static boolean isSensObjValid(Sensitivity sensObj) {
        return (sensObj != null && !isZero(sensObj.sensitivity_factor) && (!isNegativeOne(sensObj.frequency) || !isNegativeOne(sensObj.sensitivity_factor)));
    }

    /**
     * Builds an array of base Unit objects built from the given Unit object.
     *
     * @param unitObj
     *            unit object
     * @return An array of Unit objects, each of which is a base Unit object
     *         (not composite).
     */
    public static Unit[] toUnitsArray(Unit unitObj) {
        if (unitObj != null && unitObj.the_unit_base != null) { //unit object is valid
            if (!unitObj.the_unit_base.equals(UnitBase.COMPOSITE)) {
                return new Unit[] { unitObj }; //if base unit, return array with Unit
            }
            final int len; //unit is composite type
            if (unitObj.elements != null && (len = unitObj.elements.length) > 0) { //elements array contains objects
                final Vector retVec = new Vector();
                Unit eUnit;
                for (int i = 0; i < len; ++i) { //for each Unit object in 'elements' array
                    eUnit = unitObj.elements[i];
                    if (eUnit != null && eUnit.the_unit_base != null) { //Unit object is valid
                        if (!eUnit.the_unit_base.equals(UnitBase.COMPOSITE)) {
                            retVec.add(eUnit); //if base unit then add to Vector
                        } else //if composite then make recursive call
                        {
                            retVec.addAll(Arrays.asList(toUnitsArray(eUnit)));
                        }
                    }
                }
                try { //convert Vector to array and return it:
                    return (Unit[]) retVec.toArray(new Unit[retVec.size()]);
                } catch (Exception ex) {
                }
            }
        } //if error then
        return new Unit[0]; //return empty array
    }

    /**
     * @return the 'power' value of the first base-unit of the given unit
     *         object, or 0 if no power value could be found.
     * @param unitObj
     *            unit object
     */
    public static int toFirstUnitPower(Unit unitObj) {
        while (unitObj != null && unitObj.the_unit_base != null) { //find first base unit in object
            if (!unitObj.the_unit_base.equals(UnitBase.COMPOSITE)) {
                return unitObj.power;
            }
            if (unitObj.elements == null || unitObj.elements.length < 1) {
                break;
            }
            unitObj = unitObj.elements[0]; //move to subunit
        }
        return 0;
    }

    /**
     * Converts a 'Unit' object to a 'UnitImpl' object.
     *
     * @param unitObj
     *            unit object
     * @return 'UnitImpl' object
     */
    public static UnitImpl unitToUnitImpl(Unit unitObj) {
        if (unitObj == null) //if null handle then
        {
            return null; //return null
        }
        if (unitObj instanceof UnitImpl) //if already a 'UnitImpl' then
        {
            return (UnitImpl) unitObj; //cast and return object
        } //build new 'UnitImpl' from fields of 'Unit' object:
        return (unitObj.the_unit_base != null && unitObj.the_unit_base.equals(UnitBase.COMPOSITE))
                ? new UnitImpl(unitObj.elements, unitObj.power, unitObj.name, unitObj.multi_factor, unitObj.exponent)
                : new UnitImpl(unitObj.the_unit_base, unitObj.power, unitObj.name, unitObj.multi_factor, unitObj.exponent);
    }

    /**
     * Appends all elements in 'dArr2' to end of 'dArr1'.
     *
     * @param fArr1
     *            first value array
     * @param fArr2
     *            second value array
     * @return A new array of float values.
     */
    public static float[] appendArrays(float[] fArr1, float[] fArr2) {
        //get length of arrays:
        final int fArr1Len = (fArr1 != null) ? fArr1.length : 0;
        final int fArr2Len = (fArr2 != null) ? fArr2.length : 0;
        final float[] retArr = new float[fArr1Len + fArr2Len];
        int i, rIdx = 0;
        for (i = 0; i < fArr1Len; ++i) //put in values from first array
        {
            retArr[rIdx++] = fArr1[i];
        }
        for (i = 0; i < fArr2Len; ++i) //put in values from second array
        {
            retArr[rIdx++] = fArr2[i];
        }
        return retArr; //return new array of values
    }

    /**
     * Converts an array of 'float' values to an array of 'double' values.
     *
     * @param fArr
     *            value array
     * @return A new array of 'double' values converted from the input array.
     */
    public static double[] floatToDoubleArray(float[] fArr) {
        final int len = (fArr != null) ? fArr.length : 0; //number of values
        final double[] retArr = new double[len]; //create double array
        for (int i = 0; i < len; ++i) //for each value
        {
            retArr[i] = (fArr[i]); //convert value
        }
        return retArr; //return new array
    }

    /**
     * Extracts and restores a URL string that has been saved into a 'File'
     * object.
     *
     * @param pathStr
     *            the path string from the 'File' object.
     * @return The URL string that was entered into the 'File' object.
     */
    public static String fileObjPathToUrlStr(String pathStr) {
        //convert any backslashes back to slashes:
        final String retStr = pathStr.replace('\\', '/');
        //fix "://" string that was converted to ":/":
        if (retStr.length() > 6) {
            if (retStr.startsWith("http:/") && retStr.charAt(6) != '/') {
                return retStr.substring(0, 6) + '/' + retStr.substring(6);
            }
            if (retStr.startsWith("ftp:/") && retStr.charAt(5) != '/') {
                return retStr.substring(0, 5) + '/' + retStr.substring(5);
            }
            if (retStr.startsWith("jar:/") && retStr.charAt(5) != '/') {
                return retStr.substring(0, 5) + '/' + retStr.substring(5);
            }
        }
        return retStr;
    }

}
