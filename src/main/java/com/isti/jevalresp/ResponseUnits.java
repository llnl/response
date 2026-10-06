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

import static tec.units.ri.AbstractUnit.ONE;
import static tec.units.ri.unit.Units.AMPERE;
import static tec.units.ri.unit.Units.CELSIUS;
import static tec.units.ri.unit.Units.METRE;
import static tec.units.ri.unit.Units.METRE_PER_SECOND;
import static tec.units.ri.unit.Units.METRE_PER_SQUARE_SECOND;
import static tec.units.ri.unit.Units.PASCAL;
import static tec.units.ri.unit.Units.PERCENT;
import static tec.units.ri.unit.Units.RADIAN;
import static tec.units.ri.unit.Units.SECOND;
import static tec.units.ri.unit.Units.TESLA;
import static tec.units.ri.unit.Units.VOLT;
import static tec.units.ri.unit.Units.WATT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Acceleration;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Length;
import javax.measure.quantity.MagneticFluxDensity;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;

import edu.iris.Fissures.model.UnitImpl;
import systems.uom.quantity.Information;
import systems.uom.quantity.InformationRate;
import tec.units.ri.format.SimpleUnitFormat;
import tec.units.ri.unit.AlternateUnit;
import tec.units.ri.unit.MetricPrefix;
import tec.units.ri.unit.ProductUnit;

/**
 *
 * @author dodge1
 */
public class ResponseUnits implements Serializable {

    private static final long serialVersionUID = 6244556146016308579L;

    private final Unit<? extends Quantity<?>> inputUnits;
    private final UnitImpl unitObj;
    private final UnitsStatus unitsStatus;

    public final static Unit<Angle> DEGREE = RADIAN.divide(Math.PI).multiply(180.0).asType(Angle.class);

    public final static Unit<Information> BIT = new AlternateUnit(ONE, "bit");
    public final static ProductUnit<InformationRate> BITS_PER_SECOND = new ProductUnit<>(BIT.divide(SECOND));
    public final static Unit<Information> BYTE = BIT.multiply(8);

    public final static Unit<Speed> MILLIMETER_PER_HOUR = METRE_PER_SECOND.multiply(1000 * 3600);

    public final static Unit<Speed> INCH_PER_SECOND = METRE_PER_SECOND.divide(39.3701);
    public final static Unit<Speed> MILE_PER_HOUR = METRE_PER_SECOND.multiply(0.44704);

    public final static Unit<?> WATT_PER_SQUARE_METRE = WATT.divide(METRE.pow(2));

    public final static Unit<Speed> NANOMETER_PER_SECOND = MetricPrefix.NANO(METRE_PER_SECOND);
    public final static Unit<?> NANOMETER_PER_SQUARE_SECOND = MetricPrefix.NANO(METRE_PER_SQUARE_SECOND);

    public final static Unit<Length> NANOMETER = MetricPrefix.NANO(METRE);
    public final static Unit<Dimensionless> COUNT = new AlternateUnit(ONE, "count");
    public final static Unit<Dimensionless> CYCLE = new AlternateUnit(ONE, "cycle");
    public final static Unit<Dimensionless> NUMBER = new AlternateUnit(ONE, "number");
    public final static Unit<Dimensionless> REBOOT_COUNT = new AlternateUnit(ONE, "reboot");
    public final static Unit<Dimensionless> STRAIN = new AlternateUnit(ONE, "strain");
    public final static Unit<Dimensionless> VOLUMETRIC_STRAIN = new AlternateUnit(ONE, "vstrain");
    public final static Unit<Dimensionless> MICRO_STRAIN = MetricPrefix.MICRO(ResponseUnits.STRAIN);
    public final static Unit<Dimensionless> NANO_STRAIN = MetricPrefix.NANO(ResponseUnits.STRAIN);
    public final static Unit<Pressure> MICRO_PASCAL = MetricPrefix.MICRO(PASCAL);
    public final static Unit<Pressure> BAR = MetricPrefix.KILO(MetricPrefix.HECTO(PASCAL));
    public final static Unit<Pressure> MILLIBAR = MetricPrefix.HECTO(PASCAL);

    public final static Unit<Dimensionless> GAPS = new AlternateUnit(ONE, "gaps");
    public final static Unit<Dimensionless> HITS = new AlternateUnit(ONE, "hits");
    public final static Unit<?> HAIL_INTENSITY = HITS.divide(METRE.pow(2)).divide(SECOND);
    public final static Unit<Dimensionless> DEFAULT = new AlternateUnit(ONE, "default");

    public final static Unit<Length> MICRON = MetricPrefix.MICRO(METRE);
    public final static Unit<Length> PICO_METRE = MetricPrefix.PICO(METRE);

    private static final ArrayList<SimpleEntry<String, String>> UNITS_DESCRIPTION = new ArrayList<>();

    public static ArrayList<SimpleEntry<String, String>> getUnitsDescription() {
        return new ArrayList<>(UNITS_DESCRIPTION);
    }

    static {
        SimpleUnitFormat.getInstance().label(STRAIN, "strain");
        SimpleUnitFormat.getInstance().label(VOLUMETRIC_STRAIN, "vstrain");
        SimpleUnitFormat.getInstance().label(REBOOT_COUNT, "reboot");
        SimpleUnitFormat.getInstance().label(NUMBER, "number");
        SimpleUnitFormat.getInstance().label(CYCLE, "cycle");
        SimpleUnitFormat.getInstance().label(COUNT, "count");
        SimpleUnitFormat.getInstance().label(BIT, "bit");
        SimpleUnitFormat.getInstance().label(BYTE, "byte");
        SimpleUnitFormat.getInstance().label(GAPS, "gaps");
        SimpleUnitFormat.getInstance().label(HITS, "hits");
        SimpleUnitFormat.getInstance().label(DEFAULT, "default");
        SimpleUnitFormat.getInstance().label(DEGREE, "degree");
        SimpleUnitFormat.getInstance().label(MILE_PER_HOUR, "mph");
        SimpleUnitFormat.getInstance().label(INCH_PER_SECOND, "ips");
        SimpleUnitFormat.getInstance().label(MILLIMETER_PER_HOUR, "mm/h");
        SimpleUnitFormat.getInstance().label(NANOMETER_PER_SECOND, "nm/s");
        SimpleUnitFormat.getInstance().label(NANOMETER_PER_SQUARE_SECOND, "nm/s^2");
        SimpleUnitFormat.getInstance().label(NANOMETER, "nm");
        SimpleUnitFormat.getInstance().label(MICRO_STRAIN, "microstrain");
        SimpleUnitFormat.getInstance().label(NANO_STRAIN, "nanostrain");
        SimpleUnitFormat.getInstance().label(MICRON, "micron");
        SimpleUnitFormat.getInstance().label(PICO_METRE, "pm");
        SimpleUnitFormat.getInstance().label(MICRO_PASCAL, "uPa");
        SimpleUnitFormat.getInstance().label(CELSIUS, "degC");
        SimpleUnitFormat.getInstance().label(METRE_PER_SQUARE_SECOND, "m/s^2");
        SimpleUnitFormat.getInstance().label(WATT_PER_SQUARE_METRE, "W/m^2");
        SimpleUnitFormat.getInstance().label(MetricPrefix.CENTI(METRE).divide(SECOND.pow(2)), "cm/s^2");
        SimpleUnitFormat.getInstance().label(HAIL_INTENSITY.multiply(0.36), "H/cm^2/hr");
        SimpleUnitFormat.getInstance().label(HAIL_INTENSITY, "hail");

        UNITS_DESCRIPTION.add(new SimpleEntry<>("default", "No units requested"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("strain", "Strain e.g. meters per meter"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("vstrain", "Volumetric strain e.g. m^3 / m^3"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("bit/s", "Information rate in bits per second"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("byte", "Digital Size in Digital bytes"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("degC", "temperature in degrees Celsius"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("cm/s^2", "acceleration in centimeters per seconds squared"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("count", "Digital Counts"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("cPa", "Pressure in centiPascal"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("cycle", "Cycles"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("degree", "Direction in Degrees"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("hail", "hail intensity in hits per meters squared sec"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("kPa", "Pressure in kilo-Pascals"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("m", "Displacement in Meters"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("m/s^2", "ACCELERATION in Meters per square second"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("mrad", "Angle in Microradians"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("microstrain", "Microstrain"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("micron", "length in units of 10^-6 METRE"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("pm", "length in units of 10^-12 METRE"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("hPa", "Pressure in hectoPascals or millibars"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("mm/h", "Velocity in millimeters per hour"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("ips", "Velocity in inches per second"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("mPa", "Pressure in milliPascals"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("mph", "Velocity in miles per hour"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("nm/s", "Velocity in nanometers per second"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("number", "Dimensionless number"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("nT", "Magnetic field in nanoTeslas"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("Pa", "Pressure in Pascals"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("%", "Percentage 0-100"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("rad/s", "Angular velocity in radians per second"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("reboot", "Reboot count"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("s", "Time in Seconds"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("T", "Magnetic field in Teslas"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("rad", "Angular rotation in radians"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("ns", "Time in nanoseconds"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("V", "EMF in Volts"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("V/m", "Electric field in Volts per meter"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("A", "Electric Current in Amperes"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("gaps", "Gap count"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("m/s", "VELOCITY in Meters Per Second"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("mV", "EMF in millivolts"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("nm", "DISPLACEMENT IN NANOMETERS"));
        UNITS_DESCRIPTION.add(new SimpleEntry<>("W/m^2", "Watts Per Square Meter"));

    }

    private static Unit<?>[] allUnits = { ResponseUnits.STRAIN, //
            ResponseUnits.VOLUMETRIC_STRAIN, //
            ResponseUnits.BITS_PER_SECOND, //
            ResponseUnits.BYTE, //
            CELSIUS, //
            MetricPrefix.CENTI(METRE).divide(SECOND.pow(2)), //
            ResponseUnits.COUNT, //
            MetricPrefix.CENTI(PASCAL), //
            ResponseUnits.CYCLE, //
            ResponseUnits.DEGREE, //
            ResponseUnits.HAIL_INTENSITY.multiply(0.36), //
            ResponseUnits.HAIL_INTENSITY, //
            MetricPrefix.HECTO(PASCAL), //
            MetricPrefix.KILO(PASCAL), METRE, //
            METRE_PER_SQUARE_SECOND, //
            MetricPrefix.MILLI(RADIAN), //
            MICRO_STRAIN, MICRON, //
            NANO_STRAIN,
            PICO_METRE, //
            ResponseUnits.MILLIBAR, ResponseUnits.MILLIMETER_PER_HOUR, //
            MetricPrefix.MILLI(PASCAL), //
            MILE_PER_HOUR, //
            NANOMETER_PER_SECOND, //
            ResponseUnits.NUMBER, //
            MetricPrefix.NANO(TESLA), //
            PASCAL, //
            PERCENT, //
            RADIAN.divide(SECOND), //
            ResponseUnits.REBOOT_COUNT, //
            SECOND, //
            TESLA, //
            RADIAN, //
            MetricPrefix.NANO(SECOND), //
            VOLT, //
            VOLT.divide(METRE), //
            AMPERE, //
            ResponseUnits.GAPS, //
            METRE_PER_SECOND, //
            ResponseUnits.MILLIBAR, //
            MetricPrefix.MILLI(VOLT), //
            MetricPrefix.NANO(METRE), //
            WATT.divide(METRE.pow(2)) };//

    public ResponseUnits(Unit<?> inputUnits, UnitImpl unitObj, UnitsStatus unitsStatus) {
        this.inputUnits = inputUnits;
        this.unitObj = unitObj;
        this.unitsStatus = unitsStatus;
    }

    public ResponseUnits() {
        unitsStatus = UnitsStatus.UNDETERMINED;
        unitObj = UnitImpl.UNKNOWN;
        inputUnits = new AlternateUnit(ONE, "unknown");
    }

    public ResponseUnits(UnitsStatus unitsStatus) {
        this.unitsStatus = unitsStatus;
        unitObj = UnitImpl.UNKNOWN;
        inputUnits = new AlternateUnit(ONE, "unknown");
    }

    public ResponseUnits(ResponseUnits units) {
        unitsStatus = units.unitsStatus;
        unitObj = units.unitObj;
        inputUnits = units.inputUnits;
    }

    public Unit<? extends Quantity<?>> getInputUnits() {
        return inputUnits;
    }

    public UnitImpl getUnitObj() {
        return unitObj;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.inputUnits);
        hash = 59 * hash + Objects.hashCode(this.unitObj);
        hash = 59 * hash + Objects.hashCode(this.unitsStatus);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResponseUnits other = (ResponseUnits) obj;
        if (!Objects.equals(this.inputUnits, other.inputUnits)) {
            return false;
        }
        if (!Objects.equals(this.unitObj, other.unitObj)) {
            return false;
        }
        if (this.unitsStatus != other.unitsStatus) {
            return false;
        }
        return true;
    }

    public UnitsStatus getUnitsStatus() {
        return unitsStatus;
    }

    @Override
    public String toString() {
        return "ResponseUnits{" + "inputUnits=" + inputUnits + ", unitObj=" + unitObj + ", unitsStatus=" + unitsStatus + '}';
    }


    public static double getScaleFactor(Unit<?> inUnit, Unit<?> outUnit) {

        if (!inUnit.isCompatible(outUnit)) {
            throw new IllegalArgumentException(String.format("The input units (%s) are incompatible with the output units(%s)", inUnit.toString(), outUnit.toString()));
        }
        if (inUnit.isCompatible(METRE)) {
            UnitConverter converter = inUnit.asType(Length.class).getConverterTo(outUnit.asType(Length.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(METRE_PER_SECOND)) {
            UnitConverter converter = inUnit.asType(Speed.class).getConverterTo(outUnit.asType(Speed.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(METRE_PER_SQUARE_SECOND)) {
            UnitConverter converter = inUnit.asType(Acceleration.class).getConverterTo(outUnit.asType(Acceleration.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(PASCAL)) {
            UnitConverter converter = inUnit.asType(Pressure.class).getConverterTo(outUnit.asType(Pressure.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(RADIAN)) {
            UnitConverter converter = inUnit.asType(Angle.class).getConverterTo(outUnit.asType(Angle.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(SECOND)) {
            UnitConverter converter = inUnit.asType(Time.class).getConverterTo(outUnit.asType(Time.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(AMPERE)) {
            UnitConverter converter = inUnit.asType(ElectricCurrent.class).getConverterTo(outUnit.asType(ElectricCurrent.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(CELSIUS)) {
            UnitConverter converter = inUnit.asType(Temperature.class).getConverterTo(outUnit.asType(Temperature.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(PERCENT)) {
            UnitConverter converter = inUnit.asType(Dimensionless.class).getConverterTo(outUnit.asType(Dimensionless.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(TESLA)) {
            UnitConverter converter = inUnit.asType(MagneticFluxDensity.class).getConverterTo(outUnit.asType(MagneticFluxDensity.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(VOLT)) {
            UnitConverter converter = inUnit.asType(ElectricPotential.class).getConverterTo(outUnit.asType(ElectricPotential.class));
            return converter.convert(1.0);
        } else if (inUnit.isCompatible(WATT)) {
            UnitConverter converter = inUnit.asType(Power.class).getConverterTo(outUnit.asType(Power.class));
            return converter.convert(1.0);
        } else {
            throw new IllegalArgumentException(String.format("Unhandled units: %s", inUnit));
        }
    }

    public static Unit<?> parse(String unitString) {
        return SimpleUnitFormat.getInstance().parse(unitString);
    }

    public static void listUnits(PrintStream pw) {

        for (SimpleEntry<String, String> SimpleEntry : UNITS_DESCRIPTION) {
            String tmp = String.format("Use code \"%s\" for %s", SimpleEntry.getKey(), SimpleEntry.getValue());
            pw.println(tmp);
        }
    }

    public boolean isGroundMotionType() {
        return inputUnits.isCompatible(METRE) || inputUnits.isCompatible(METRE_PER_SECOND) || inputUnits.isCompatible(METRE_PER_SQUARE_SECOND);
    }

    public static boolean areUnitsEquivalent(Unit<?> unit1, Unit<?> unit2) {
        if (unit1 == null && unit2 == null) {
            return true;
        }
        if (unit1 == null && unit2 != null) {
            return false;
        }
        if (unit2 == null && unit1 != null) {
            return false;
        }

        if (unit1.isCompatible(unit2)) {
            return SimpleUnitFormat.getInstance().parse(unit1.toString()).equals(SimpleUnitFormat.getInstance().parse(unit2.toString()));
        }
        return false;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(inputUnits.toString());
        s.writeObject(unitObj.toString());
        s.writeObject(unitsStatus.toString());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        Class type = ResponseUnits.class;
        String tmp = (String) s.readObject();
        try {
            Field field = type.getDeclaredField("inputUnits");
            // make the field non final
            field.setAccessible(true);
            field.set(this, ResponseUnits.parse(tmp));

            // make the field final again
            field.setAccessible(false);

            tmp = (String) s.readObject();
            field = type.getDeclaredField("unitObj");
            // make the field non final
            field.setAccessible(true);
            field.set(this, UnitImpl.getUnitFromString(tmp));

            // make the field final again
            field.setAccessible(false);

            tmp = (String) s.readObject();
            field = type.getDeclaredField("unitsStatus");
            // make the field non final
            field.setAccessible(true);
            field.set(this, UnitsStatus.valueOf(tmp));

            // make the field final again
            field.setAccessible(false);

        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ResponseUnits.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ResponseUnits.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ResponseUnits.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ResponseUnits.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
