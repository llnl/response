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

import static tec.units.ri.unit.Units.AMPERE;
import static tec.units.ri.unit.Units.CELSIUS;
import static tec.units.ri.unit.Units.METRE;
import static tec.units.ri.unit.Units.METRE_PER_SECOND;
import static tec.units.ri.unit.Units.METRE_PER_SQUARE_SECOND;
import static tec.units.ri.unit.Units.MINUTE;
import static tec.units.ri.unit.Units.PASCAL;
import static tec.units.ri.unit.Units.PERCENT;
import static tec.units.ri.unit.Units.RADIAN;
import static tec.units.ri.unit.Units.SECOND;
import static tec.units.ri.unit.Units.TESLA;
import static tec.units.ri.unit.Units.VOLT;
import static tec.units.ri.unit.Units.WATT;

import edu.iris.Fissures.model.UnitImpl;
import tec.units.ri.unit.MetricPrefix;

/**
 *
 * @author dodge1
 */
public class UnitsParser {

    public static ResponseUnits parseResponseString(String value) {
        switch (value) {
        case "1 - Strain":
        case "M/M - STRAIN":
        case "M/M - strain": {
            return new ResponseUnits(ResponseUnits.STRAIN, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "M**3/M**3 - Volumetric Strain": {
            return new ResponseUnits(ResponseUnits.VOLUMETRIC_STRAIN, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "A - 1 coulomb/second":
        case "A - AMPERES":
        case "A - Amperes":
        case "A - ELECTRIC CURRENT in Amperes":
        case "A - Electric Current in Amperes":
        case "AMPERES - null":
        case "C/S - Coulomb/Second":
        case "C/S - Coulombs/Second": {
            return new ResponseUnits(AMPERE, UnitImpl.AMPERE, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "BITS/SEC - (null)":
        case "BITS/SEC - null": {
            return new ResponseUnits(ResponseUnits.BITS_PER_SECOND, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "BYTE - Digital Size in Digital bytes":
        case "BYTES - (null)":
        case "BYTES - null": {
            return new ResponseUnits(ResponseUnits.BYTE, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "C - Degrees Celsius":
        case "C - Degrees Centigrade":
        case "C - temperature in degrees Celsius":
        case "CELSIUS - TEMPERATURE in Degrees Celsius":
        case "CELSIUS - temperature in degrees Celsius":
        case "CELSIUS -":
        case "DEGC - DEGREES CELSIUS":
        case "DEGC - Temperature in degree Celsius":
        case "DEGC - degree Celsius":
        case "DEGREES - C":
        case "celsius - temperature in degrees Celsius":
        case "degC - Temperature in degree Celsius":
        case "degC - temperature in degrees Celsius": {
            return new ResponseUnits(CELSIUS, UnitImpl.CELSIUS, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "CM/SEC**2 - acceleration in centimeters per seconds squared": {
            return new ResponseUnits(MetricPrefix.CENTI(METRE).divide(SECOND.pow(2)), UnitImpl.CENTIMETER_PER_SECOND_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "	COUNTS - Digital Counts":
        case "COUNT - Digital Counts":
        case "COUNT - Digital counts":
        case "COUNTS - COUNTS":
        case "COUNTS - DIGITAL COUNTS":
        case "COUNTS - DIGITAL UNIT in Counts":
        case "COUNTS - DIGITAL":
        case "COUNTS - Digital Count in Digital counts":
        case "COUNTS - Digital Counts":
        case "COUNTS - Digital counts":
        case "COUNTS - No Abbreviation Referenced":
        case "COUNTS - digital counts":
        case "COUNTS - null":
        case "COUNTS":
        case "count - DIGITAL UNIT in Counts":
        case "count - Digital Counts":
        case "count - digital counts":
        case "counts - DIGITAL UNIT in Counts":
        case "counts - Digital Count in Digital counts":
        case "counts - Digital Counts":
        case "counts - digital counts":
        case "COUNT_UNIT - COUNT_NAME": {
            return new ResponseUnits(ResponseUnits.COUNT, UnitImpl.COUNT, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "CPA - Pressure in centiPascal": {
            return new ResponseUnits(MetricPrefix.CENTI(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "CYCLES - (null)":
        case "CYCLES - null": {
            return new ResponseUnits(ResponseUnits.CYCLE, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "D - Degrees 0":
        case "D - Degrees 0 to 360 (directio":
        case "D - Degrees 0-360 (directio":
        case "DEGREE - direction 0-360":
        case "DEGREES - Direction in Degrees":
        case "DEGREES - LAT/LON DEGREES":
        case "DEGREES - direction in degrees":
        case "degree - direction in degrees": {
            return new ResponseUnits(ResponseUnits.DEGREE, UnitImpl.DEGREE, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "GAPS - (null)":
        case "GAPS - null": {
            return new ResponseUnits(ResponseUnits.GAPS, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "H/CM**2*HR - hail intensity in hits per cm squared hour":
        case "H/CM2/HR - Hits per square cm per hour": {
            return new ResponseUnits(ResponseUnits.HAIL_INTENSITY.multiply(0.36), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "H/M**2*S - hail intensity in hits per meters squared sec": {
            return new ResponseUnits(ResponseUnits.HAIL_INTENSITY, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "HPA - Pressure in Hectopascals":
        case "HPA - hPa Atmospheric pressure in hectopascals": {
            return new ResponseUnits(MetricPrefix.HECTO(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "KPA - Pressure in kilo-Pascals": {
            return new ResponseUnits(MetricPrefix.KILO(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "M - Displacement in Meters":
        case "M - METRES":
        case "M - Meters Displacement":
        case "M - displacement in meters":
        case "m - null": {
            return new ResponseUnits(METRE, UnitImpl.METER, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "M - Minute": {
            return new ResponseUnits(MINUTE, UnitImpl.MINUTE, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "M/S - VELOCITY in Meters Per Second":
        case "M/S - Velocity in Meters Per Second":
        case "M/S - Velocity in Meters/Second":
        case "M/S - velocity in meters per second":
        case "m/s - velocity in meters per second": {
            return new ResponseUnits(METRE_PER_SECOND, UnitImpl.METER_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "M/S**2 - ACCELERATION in Meters per square second":
        case "M/S**2 - Acceleration in Meters Per Second Per Second":
        case "M/S**2 - Acceleration in Meters Per Second**2":
        case "M/S**2 - Acceleration in Meters Per Second^2":
        case "M/S**2 - Acceleration in Metres Per Second Squared":
        case "M/S**2 - Acceleration in meters per second squared":
        case "M/S**2 - acceleration in meters per seconds squared":
        case "M/S**2 - null": {
            return new ResponseUnits(METRE_PER_SQUARE_SECOND, UnitImpl.METER_PER_SECOND_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
        }
        case "MICRORADIANS - Angle in Microradians":
            return new ResponseUnits(MetricPrefix.MICRO(RADIAN), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);

        case "MICROSTRAIN - Microstrain":
            return new ResponseUnits(MetricPrefix.MICRO(ResponseUnits.STRAIN), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);

        case "MB - Pressure in millibars":
        case "MILLIBAR - null":
        case "MBAR - millibars": {
            return new ResponseUnits(ResponseUnits.MILLIBAR, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "MM/HR - Millimeters per hour":
        case "MM/HOUR - null":
        case "MM/HR - rainfall intensity in millimeters per hour": {
            return new ResponseUnits(ResponseUnits.MILLIMETER_PER_HOUR, UnitImpl.VELOCITY, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "MILLIVOLTS - null":
            return new ResponseUnits(MetricPrefix.MILLI(VOLT), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);

        case "MPA - milliPascals":
            return new ResponseUnits(MetricPrefix.MILLI(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);

        case "MPH - Miles Per Hour":
            return new ResponseUnits(METRE_PER_SECOND.multiply(0.44704), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);

        case "NM - EARTH DISPLACEMENT IN NANOMETERS": {
            return new ResponseUnits(MetricPrefix.NANO(METRE), UnitImpl.NANOMETER, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "NM/S - velocity in nanometers per second":
        case "NM/SEC - velocity in nanometers per second": {
            return new ResponseUnits(MetricPrefix.NANO(METRE_PER_SECOND), UnitImpl.NANOMETER_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "NUMBER - Number Count in Number of occurrences": {
            return new ResponseUnits(ResponseUnits.NUMBER, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "NT - Magnetic Flux Density in nanoTeslas": {
            return new ResponseUnits(MetricPrefix.NANO(TESLA), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "PA - PRESSURE in Pascals":
        case "PA - Pascals":
        case "PA - Pressure in Pascals":
        case "PASCALS - null":
        case "Pa - Pascals": {
            return new ResponseUnits(PASCAL, UnitImpl.PASCAL, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "PERCENT - PERCENTAGE 0-100":
        case "PERCENT - Percent":
        case "PERCENT - Percentage":
        case "PERCENT - Percentage 0-100":
        case "PERCENT - percentage 0-100":
        case "PERCENT -":
        case "percent - percentage 0-100":
        case "% - Percent in Percentage":
        case "% - Percentage": {
            return new ResponseUnits(PERCENT, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "RAD/S - Angular Velocity":
        case "RAD/S - velocity in radians per second":
        case "RAD/SEC - Angular velocity in radians per second":
            return new ResponseUnits(RADIAN.divide(SECOND), UnitImpl.RADIAN_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);

        case "REBOOTS - (null)":
        case "REBOOTS - null": {
            return new ResponseUnits(ResponseUnits.REBOOT_COUNT, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "S - SECONDS":
        case "S - TIME in Seconds":
        case "S - Time in Seconds":
        case "S - null":
        case "S - second":
        case "SEC - second":
        case "s - Time in Seconds":
        case "s - second": {
            return new ResponseUnits(SECOND, UnitImpl.SECOND, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "T - Magnetic field in Teslas": {
            return new ResponseUnits(TESLA, UnitImpl.TESLA, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "TILT - Radian":
            return new ResponseUnits(RADIAN, UnitImpl.RADIAN, UnitsStatus.QUANTITY_AND_UNITS);

        case "USEC - Microseconds": {
            return new ResponseUnits(MetricPrefix.NANO(SECOND), UnitImpl.MICROSECOND, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "	V - Volts":
        case "V - EMF IN VOLTS":
        case "V - EMF in Volts":
        case "V - VOLTAGE":
        case "V - VOLTS":
        case "V - Voltage in Volts":
        case "V - Volts":
        case "V - emf in volts":
        case "V - null":
        case "V - volt":
        case "V -":
        case "VOLT - EMF in Volts":
        case "VOLTS - (null)":
        case "VOLTS - Volts":
        case "VOLTS - null":
        case "VOLTS -":
        case "VOLT_UNIT - VOLT_NAME": {
            return new ResponseUnits(VOLT, UnitImpl.VOLT, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "V/M - Electric field in Volts per meter": {
            return new ResponseUnits(VOLT.divide(METRE), UnitImpl.VOLT_PER_METER, UnitsStatus.QUANTITY_AND_UNITS);
        }

        case "W/M**2 - Solar Radiation in Watts Per Square Meter":
        case "W/M2 - Watts Per Square Meters": {
            return new ResponseUnits(WATT.divide(METRE.pow(2)), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
        }

        default:
            return UnitsParser.parseReducedString(value);
        }
    }

    private static ResponseUnits parseReducedString(String string) {
        String tmp = string.replaceAll("\\s+", "");
        int jdx = tmp.indexOf("-");
        if (jdx > 0) {
            tmp = tmp.substring(0, jdx).toUpperCase();
            switch (tmp) {
            case "1":
            case "M/M": {
                return new ResponseUnits(ResponseUnits.STRAIN, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "M**3/M**3": {
                return new ResponseUnits(ResponseUnits.VOLUMETRIC_STRAIN, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "A":
            case "AMPERES":
            case "C/S": {
                return new ResponseUnits(AMPERE, UnitImpl.AMPERE, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "BITS/SEC": {
                return new ResponseUnits(ResponseUnits.BITS_PER_SECOND, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "BYTE":
            case "BYTES": {
                return new ResponseUnits(ResponseUnits.BYTE, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "C":
            case "DEGC":
            case "CELSIUS": {
                return new ResponseUnits(CELSIUS, UnitImpl.CELSIUS, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "CM/SEC**2": {
                return new ResponseUnits(MetricPrefix.CENTI(METRE).divide(SECOND.pow(2)), UnitImpl.CENTIMETER_PER_SECOND_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "COUNT":
            case "COUNTS":
            case "COUNT_UNIT": {
                return new ResponseUnits(ResponseUnits.COUNT, UnitImpl.COUNT, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "CPA": {
                return new ResponseUnits(MetricPrefix.CENTI(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }
            case "CYCLES": {
                return new ResponseUnits(ResponseUnits.CYCLE, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "D":
            case "DEGREE":
            case "DEGREES": {
                return new ResponseUnits(ResponseUnits.DEGREE, UnitImpl.DEGREE, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "GAPS": {
                return new ResponseUnits(ResponseUnits.GAPS, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "H/CM**2*HR":
            case "H/CM2/HR": {
                return new ResponseUnits(ResponseUnits.HAIL_INTENSITY.multiply(0.36), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "H/M**2*S": {
                return new ResponseUnits(ResponseUnits.HAIL_INTENSITY, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "HPA": {
                return new ResponseUnits(MetricPrefix.HECTO(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "KPA": {
                return new ResponseUnits(MetricPrefix.KILO(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "M": {
                return new ResponseUnits(METRE, UnitImpl.METER, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "M/S": {
                return new ResponseUnits(METRE_PER_SECOND, UnitImpl.VELOCITY, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "M/S**2": {
                return new ResponseUnits(METRE_PER_SQUARE_SECOND, UnitImpl.ACCELERATION, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MB":
            case "MILLIBAR":
            case "MBAR": {
                return new ResponseUnits(ResponseUnits.MILLIBAR, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MICRORADIANS": {
                return new ResponseUnits(MetricPrefix.MILLI(RADIAN), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MICROSTRAIN": {
                return new ResponseUnits(MetricPrefix.MICRO(ResponseUnits.STRAIN), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MILLIVOLTS": {
                return new ResponseUnits(MetricPrefix.MILLI(VOLT), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MM/HOUR":
            case "MM/HR": {
                return new ResponseUnits(ResponseUnits.MILLIMETER_PER_HOUR, UnitImpl.VELOCITY, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MPA": {
                return new ResponseUnits(MetricPrefix.MILLI(PASCAL), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "MPH": {
                return new ResponseUnits(METRE_PER_SECOND.multiply(0.44704), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "NM": {
                return new ResponseUnits(MetricPrefix.NANO(METRE), UnitImpl.NANOMETER, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "NM/S":
            case "NM/SEC": {
                return new ResponseUnits(MetricPrefix.NANO(METRE_PER_SECOND), UnitImpl.VELOCITY, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "NT": {
                return new ResponseUnits(MetricPrefix.NANO(TESLA), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "NUMBER": {
                return new ResponseUnits(ResponseUnits.NUMBER, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "PA": {
            }
            case "PASCALS": {
                return new ResponseUnits(PASCAL, UnitImpl.PASCAL, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "%":
            case "PERCENT": {
                return new ResponseUnits(PERCENT, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "RAD/S":
            case "RAD/SEC": {
                return new ResponseUnits(RADIAN.divide(SECOND), UnitImpl.RADIAN_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "REBOOTS": {
                return new ResponseUnits(ResponseUnits.REBOOT_COUNT, UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "S":
            case "SEC": {
                return new ResponseUnits(SECOND, UnitImpl.SECOND, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "T": {
                return new ResponseUnits(TESLA, UnitImpl.TESLA, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "TILT": {
                return new ResponseUnits(RADIAN, UnitImpl.RADIAN, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "USEC": {
                return new ResponseUnits(MetricPrefix.NANO(SECOND), UnitImpl.MICROSECOND, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "V":
            case "VOLT":
            case "VOLTS":
            case "VOLT_UNIT": {
                return new ResponseUnits(VOLT, UnitImpl.VOLT, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "V/M": {
                return new ResponseUnits(VOLT.divide(METRE), UnitImpl.VOLT_PER_METER, UnitsStatus.QUANTITY_AND_UNITS);
            }

            case "W/M**2":
            case "W/M2": {
                return new ResponseUnits(WATT.divide(METRE.pow(2)), UnitImpl.UNKNOWN, UnitsStatus.QUANTITY_AND_UNITS);
            }
            default:
                return null;

            }
        }
        return null;
    }
}
