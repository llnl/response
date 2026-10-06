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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Objects;
import javax.measure.Unit;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;

import com.isti.jevalresp.ResponseUnits;
import com.isti.jevalresp.UnitsStatus;

import tec.units.ri.AbstractUnit;
import static tec.units.ri.unit.Units.SECOND;

/**
 * Contains the "From" Transfer function
 *
 * @author dodge1
 */
public class TransferData {

    private final Complex[] originalData;
    private Complex[] workingData;
    private Complex[] convertedData;
    private final double delfreq;
    private final ResponseUnits originalUnits;
    private ResponseUnits forcedUnits;
    private ResponseUnits convertedUnits;
    private ResponseUnits workingUnits;
    private final ResponseMetaData metadata;
    private Double amplitudeAtWfdiscCalper;
    private NormalizationStatus normalizationStatus;
    private double appliedScaleFactor;
    private Double amplitudeAtNominalCalper;
    private AppliedScaling appliedScaling;
    private double unitConversionScaleFactor;
    private ConversionType conversionType;
    private Double appliedCalib;

    public static enum NormalizationStatus {

        Normalized, UnNormalized, Unknown
    }

    public static enum AppliedScaling {
        None, ScaledByWfdiscCalib, ScaledByNominalCalib
    }

    public static enum SpectrumDataType {
        Original, Converted, Working
    }

    public static enum ConversionType {
        None, IntegrateOnce, IntegrateTwice, DifferentiateOnce, differentiateTwice, ScaleOnly
    }

    public static TransferData copyOf(TransferData other) {
        return new TransferData(other.originalData,
                other.workingData,
                other.convertedData,
                other.delfreq,
                other.originalUnits,
                other.forcedUnits,
                other.convertedUnits,
                other.workingUnits,
                other.metadata,
                other.amplitudeAtWfdiscCalper,
                other.normalizationStatus,
                other.appliedScaleFactor,
                other.amplitudeAtNominalCalper,
                other.appliedScaling,
                other.unitConversionScaleFactor,
                other.conversionType,
                other.appliedCalib);
    }

    private TransferData(Complex[] originalData,
            Complex[] workingData,
            Complex[] convertedData,
            double delfreq,
            ResponseUnits originalUnits,
            ResponseUnits forcedUnits,
            ResponseUnits convertedUnits,
            ResponseUnits workingUnits,
            ResponseMetaData metadata,
            Double amplitudeAtWfdiscCalper,
            NormalizationStatus normalizationStatus,
            double appliedScaleFactor,
            Double amplitudeAtNominalCalper,
            AppliedScaling appliedScaling,
            double unitConversionScaleFactor,
            ConversionType conversionType,
            Double appliedCalib) {
        this.originalData = originalData.clone();
        this.workingData = workingData.clone();
        this.convertedData = convertedData != null ? convertedData.clone() : null;
        this.delfreq = delfreq;
        this.originalUnits = originalUnits;
        this.forcedUnits = forcedUnits;
        this.convertedUnits = convertedUnits;
        this.workingUnits = workingUnits;
        this.metadata = metadata;
        this.amplitudeAtWfdiscCalper = amplitudeAtWfdiscCalper;
        this.normalizationStatus = normalizationStatus;
        this.appliedScaleFactor = appliedScaleFactor;
        this.amplitudeAtNominalCalper = amplitudeAtNominalCalper;
        this.appliedScaling = appliedScaling;
        this.unitConversionScaleFactor = unitConversionScaleFactor;
        this.conversionType = conversionType;
        this.appliedCalib = appliedCalib;
    }

    public TransferData(Complex[] workingData, double delfreq, ResponseUnits units, ResponseMetaData metadata) {
        this.originalData = workingData;
        this.workingData = originalData.clone();
        this.convertedData = null;
        this.delfreq = delfreq;
        this.originalUnits = units;
        workingUnits = originalUnits;
        forcedUnits = null;
        convertedUnits = null;
        this.metadata = metadata;

        amplitudeAtWfdiscCalper = null;
        normalizationStatus = NormalizationStatus.Unknown;
        appliedScaleFactor = 1.0;
        amplitudeAtNominalCalper = null;
        appliedScaling = AppliedScaling.None;
        unitConversionScaleFactor = 1.0;
        conversionType = ConversionType.None;
        appliedCalib = null;
        maybeScaleResponse();  //possibly scale by wfdisc CALIB or by nominal CALIB
    }

    /**
     * This does not change the transfer function data values. Only the
     * interpretation.
     *
     * @param newUnits
     */
    public void setForcedUnits(ResponseUnits newUnits) {
        forcedUnits = newUnits;
        workingUnits = forcedUnits;
    }

    public ResponseUnits getWorkingUnits() {
        return workingUnits;
    }

    public ResponseUnits getOriginalUnits() {
        return originalUnits;
    }

    public ResponseUnits getConvertedUnits() {
        return convertedUnits;
    }

    public ResponseUnits getForcedUnits() {
        return forcedUnits;
    }

    public ResponseUnits getUnits(SpectrumDataType type) {
        switch (type) {
            case Original:
                return getOriginalUnits();
            case Converted:
                return getConvertedUnits();
            case Working:
                return getForcedUnits();
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public Complex[] getData() {
        return getData(SpectrumDataType.Working);
    }

    public Complex[] getData(SpectrumDataType type) {
        switch (type) {
            case Original:
                return getOriginalData();
            case Converted:
                return getConvertedData();
            case Working:
                return getWorkingData();
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public Complex[] getWorkingData() {
        return workingData != null ? workingData.clone() : null;
    }

    public Complex[] getConvertedData() {
        return convertedData != null ? convertedData.clone() : null;
    }

    public Complex[] getOriginalData() {
        return originalData.clone();
    }

    public double[] getAmplitudes() {
        return getAmplitudes(SpectrumDataType.Working);
    }

    public double[] getAmplitudes(SpectrumDataType type) {
        switch (type) {
            case Original:
                return getAmplitudes(originalData);
            case Converted:
                return getAmplitudes(convertedData);
            case Working:
                return getAmplitudes(workingData);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public Double getAppliedCalib() {
        return appliedCalib;
    }

    public double[] getAmplitudes(Complex[] data) {
        if (data == null) {
            throw new IllegalStateException("Attempted to get amplitude array for uncomputed data type!");
        }
        double[] result = new double[data.length];
        for (int j = 0; j < data.length; ++j) {
            result[j] = data[j].abs();
        }
        return result;
    }

    public double[] getAmplitudes(int maxIndex) {
        return getAmplitudes(maxIndex, SpectrumDataType.Working);
    }

    public double[] getAmplitudes(int maxIndex, SpectrumDataType type) {
        switch (type) {
            case Original:
                return getAmplitudes(maxIndex, originalData);
            case Converted:
                return getAmplitudes(maxIndex, convertedData);
            case Working:
                return getAmplitudes(maxIndex, workingData);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public double[] getAmplitudes(int maxIndex, Complex[] data) {
        if (data == null) {
            throw new IllegalStateException("Attempted to get amplitude array for uncomputed data type!");
        }
        int N = Math.min(maxIndex, data.length);
        double[] result = new double[N];
        for (int j = 0; j < N; ++j) {
            result[j] = data[j].abs();
        }
        return result;
    }

    public double[] getFrequencies() {
        double[] result = new double[workingData.length];
        for (int j = 0; j < workingData.length; ++j) {
            result[j] = j * delfreq;
        }
        return result;
    }

    public double[] getFrequencies(int maxIndex) {
        int N = Math.min(maxIndex, workingData.length);
        double[] result = new double[N];
        for (int j = 0; j < N; ++j) {
            result[j] = j * delfreq;
        }
        return result;
    }

    public AbstractMap.SimpleEntry<double[], Complex[]> getSpectrum() {
        return getSpectrum(SpectrumDataType.Working);
    }

    public AbstractMap.SimpleEntry<double[], Complex[]> getSpectrum(SpectrumDataType type) {
        switch (type) {
            case Original:
                return getSpectrum(originalData);
            case Converted:
                return getSpectrum(convertedData);
            case Working:
                return getSpectrum(workingData);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);

        }
    }

    private AbstractMap.SimpleEntry<double[], Complex[]> getSpectrum(Complex[] data) {
        return new AbstractMap.SimpleEntry<>(getFrequencies(), data.clone());
    }

    public void writeToTextFile(String filename) throws FileNotFoundException {
        writeToTextFile(filename, SpectrumDataType.Working);
    }

    public double getUnitConversionScaleFactor() {
        return unitConversionScaleFactor;
    }

    public ConversionType getConversionType() {
        return conversionType;
    }

    public void writeToTextFile(String filename, SpectrumDataType type) throws FileNotFoundException {
        switch (type) {
            case Original:
                writeToTextFile(filename, originalData);
                break;
            case Converted:
                writeToTextFile(filename, convertedData);
                break;
            case Working:
                writeToTextFile(filename, workingData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private void writeToTextFile(String filename, Complex[] data) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter(filename)) {
            for (int j = 0; j < data.length; ++j) {
                pw.println(j * delfreq + "  " + data[j].abs());
            }
            pw.close();
        }
    }

    public void writeFAPFile(String filename, String unit) throws FileNotFoundException {
        double[] freq = new double[workingData.length];
        double[] amp = new double[workingData.length];
        double[] phase = new double[workingData.length];
        for (int j = 0; j < workingData.length; ++j) {
            freq[j] = j * delfreq;
            amp[j] = workingData[j].abs();
            phase[j] = workingData[j].getArgument();

        }
        phase = unwrap(phase);
        for (int j = 0; j < phase.length; ++j) {
            phase[j] = Math.toDegrees(phase[j]);
        }
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println("# Freq(Hz)     Amp(counts/"+unit+")   Phase(deg)   Error(amp)  Error(phase)");
            pw.println("#");
            pw.println(" theoretical   0   instrument   fap");
            pw.println(phase.length - 1);
            for (int j = 1; j < phase.length; ++j) {
                String line = String.format("%10.6f  %16.9f  %12.6f      0.0     0.0", freq[j], amp[j], phase[j]);
                pw.println(line);
            }
        }
    }


    /**
     * Unwraps a 1D array of phase angles. The algorithm assumes that the
     * absolute difference between consecutive angles is less than a certain
     * tolerance (default: Math.PI).
     *
     * @param wrappedPhase The input array of wrapped phase angles in radians.
     * @return The unwrapped phase array.
     */
    private static double[] unwrap(double[] wrappedPhase) {
        if (wrappedPhase == null || wrappedPhase.length < 2) {
            return wrappedPhase;
        }

        double[] unwrappedPhase = new double[wrappedPhase.length];
        unwrappedPhase[0] = wrappedPhase[0];

        for (int i = 1; i < wrappedPhase.length; i++) {
            double phaseDiff = wrappedPhase[i] - wrappedPhase[i - 1];

            // Correct phase jumps greater than Math.PI
            if (phaseDiff > Math.PI) {
                unwrappedPhase[i] = wrappedPhase[i] - 2 * Math.PI;
            } else if (phaseDiff < -Math.PI) {
                unwrappedPhase[i] = wrappedPhase[i] + 2 * Math.PI;
            } else {
                unwrappedPhase[i] = wrappedPhase[i];
            }

            // Keep adding previous unwrapped value to maintain continuity
            unwrappedPhase[i] += unwrappedPhase[i - 1] - wrappedPhase[i - 1];
        }

        return unwrappedPhase;
    }

    public Double getAmplitudeAtWfdiscCalper() {
        return amplitudeAtWfdiscCalper;
    }

    public NormalizationStatus getNormalizationStatus() {
        return normalizationStatus;
    }

    public double getAppliedScaleFactor() {
        return appliedScaleFactor;
    }

    public Double getAmplitudeAtNominalCalper() {
        return amplitudeAtNominalCalper;
    }

    public AppliedScaling getAppliedScaling() {
        return appliedScaling;
    }

    private void maybeScaleResponse() {
        double maxFreq = (originalData.length - 1) * delfreq;
        if (metadata.hasFullWfdiscCalibration()) {
            double calFreq = 1.0 / metadata.getWfdiscCalper();
            if (calFreq < maxFreq) {
                appliedCalib = metadata.getWfdiscCalib(); // Units of CALIB are nm/COUNT.
                // This means that data in counts are to be multiplied by CALIB to get to nm.
                // CALIB applies only at CALPER. So the associated transfer function must have a value of
                // 1.0 at CALPER. Therefore the transfer function should be divided by its value at CALPER
                // to normalize and also divided by CALIB (because the data will be divided in the frequency domain
                // by the transfer function.
                amplitudeAtWfdiscCalper = getAmplitudeAt(calFreq);
                normalizationStatus = computeNormalizationStatus(amplitudeAtWfdiscCalper);

                // If response is normalized, amplitudeAtWfdiscCalper should be 1.0
                // but it will be divided into the spectrum just in case. at the same time
                // the spectrum is divided by calib.
                if (amplitudeAtWfdiscCalper > 0) {
                    appliedScaleFactor = metadata.getWfdiscCalib() * amplitudeAtWfdiscCalper;
                    for (int j = 0; j < originalData.length; ++j) {
                        workingData[j] = originalData[j].divide(appliedScaleFactor);
                    }
                    appliedScaling = AppliedScaling.ScaledByWfdiscCalib;
                }
            }
        } else if (metadata.hasWfdiscCalibration()) {
            appliedCalib = metadata.getWfdiscCalib();
            for (int j = 0; j < originalData.length; ++j) {
                workingData[j] = originalData[j].divide(appliedCalib);
            }
            appliedScaling = AppliedScaling.ScaledByWfdiscCalib;

        } else if (metadata.hasFullNominalCalibration()) {
            double calFreq = 1.0 / metadata.getNominalCalper();
            if (calFreq < maxFreq) {
                appliedCalib = metadata.getNominalCalib(); // Units of CALIB are nm/COUNT.
                // This means that data in counts are to be multiplied by CALIB to get to nm.
                // CALIB applies only at CALPER. So the associated transfer function must have a value of
                // 1.0 at CALPER. Therefore the transfer function should be divided by its value at CALPER
                // to normalize and also divided by CALIB (because the data will be divided in the frequency domain
                // by the transfer function.
                amplitudeAtNominalCalper = getAmplitudeAt(calFreq);
                if (amplitudeAtNominalCalper > 0) {
                    appliedScaleFactor = metadata.getNominalCalib() * amplitudeAtNominalCalper;
                    for (int j = 0; j < originalData.length; ++j) {
                        workingData[j] = originalData[j].divide(appliedScaleFactor);
                    }
                    appliedScaling = AppliedScaling.ScaledByNominalCalib;
                }
            }
        } else if (metadata.hasNominalCalib()) {
            appliedCalib = metadata.getNominalCalib();
            for (int j = 0; j < originalData.length; ++j) {
                workingData[j] = originalData[j].divide(appliedCalib);
            }
            appliedScaling = AppliedScaling.ScaledByNominalCalib;
        }
    }

    private NormalizationStatus computeNormalizationStatus(Double amplitudeAtCalper) {
        return amplitudeAtCalper >= 0.99 && amplitudeAtCalper <= 1.01 ? NormalizationStatus.Normalized : NormalizationStatus.UnNormalized;
    }

    public double getAmplitudeAt(double value) {
        double[] amplitude = getAmplitudes();
        double[] frequencies = getFrequencies();
        LinearInterpolator li = new LinearInterpolator(); // or other interpolator
        PolynomialSplineFunction psf = li.interpolate(frequencies, amplitude);
        return psf.value(value);
    }

    public double getDelfreq() {
        return delfreq;
    }

    public ResponseMetaData getMetadata() {
        return new ResponseMetaData(metadata);
    }

    public FreqLimits computeFreqLimits(double dataSamprate) {
        double dataNyquist = dataSamprate / 2;
        double instrumentNyquist = dataNyquist;
        Double instrumentSampleRate = this.getMetadata().getInstrumentSampleRate();
        if (instrumentSampleRate != null && instrumentSampleRate > 0 && instrumentSampleRate < dataSamprate) {
            instrumentNyquist = instrumentSampleRate / 2;
        }
        int nyquistIndex = Math.min((int) Math.round(instrumentNyquist / delfreq), originalData.length - 1);

        double[] amplitude = getAmplitudes(nyquistIndex);
        double[] frequencies = getFrequencies(nyquistIndex);

        if (nyquistIndex >= amplitude.length) {
            nyquistIndex = amplitude.length - 1;
        }

        int idx1 = getStartIndex(frequencies);
        double logFullRange = getLogFullRange(amplitude, idx1);
        int testIndex = getMidLogFreqIndex(frequencies, idx1);

        double currentMax = amplitude[testIndex];
        double currentMin = currentMax;

        int jdx = testIndex;
        boolean onDownSlope = false;

        while (jdx < frequencies.length) {
            double v = amplitude[jdx];
            if (onDownSlope && v > currentMin) {
                nyquistIndex = jdx - 1;
                break;
            }
            if (v < currentMin) {
                currentMin = v;
                double range = currentMax - currentMin;
                double logRange = Math.log10(range);
                if (logRange > logFullRange / 2) {
                    onDownSlope = true;
                }
            }
            if (v > currentMax) {
                currentMax = v;
            }
            ++jdx;
        }

        double nyquist = frequencies[nyquistIndex];
        double minFreq = frequencies[idx1];
        double logFreqRange = Math.log10(nyquist) - Math.log10(minFreq);

        double highCut = Math.pow(10, Math.log10(nyquist) - logFreqRange * .01);
        double highPass = Math.pow(10, Math.log10(nyquist) - logFreqRange * .05);
        double lowPass = Math.pow(10, Math.log10(minFreq) + logFreqRange * .05);
        double lowCut = Math.pow(10, Math.log10(minFreq) + logFreqRange * .01);

        return new FreqLimits(lowCut, lowPass, highPass, highCut);
    }

    private static int getMidLogFreqIndex(double[] freq, int idx1) {
        double minFreq = freq[idx1];
        double maxFreq = freq[freq.length - 1];
        double testLFreq = (Math.log10(maxFreq) - Math.log10(minFreq)) / 2 + Math.log10(minFreq);
        double testFreq = Math.pow(10, testLFreq);
        int testIndex = 0;
        for (int j = 0; j < freq.length - 1; ++j) {
            if (freq[j] <= testFreq && freq[j + 1] >= testFreq) {
                testIndex = j;
                break;
            }
        }
        return testIndex;
    }

    private static int getStartIndex(double[] freq) throws IllegalStateException {
        int jdxx = 0;
        double minFreq = freq[jdxx];
        while (minFreq == 0 && jdxx < freq.length - 1) {
            minFreq = freq[++jdxx];
        }
        if (minFreq == 0) {
            throw new IllegalStateException("No non-zero frequencies in input data!");
        }
        return jdxx;
    }

    private static double getLogFullRange(double[] amp, int idx) {
        double minVal = amp[idx];
        double maxVal = minVal;
        for (int j = idx; j < amp.length; ++j) {
            double v = amp[j];
            if (minVal > v) {
                minVal = v;
            }
            if (maxVal < v) {
                maxVal = v;
            }
        }
        double range = maxVal - minVal;
        if (range <= 0) {
            throw new IllegalStateException("No range in amplitude distribution!");
        }
        return Math.log10(range);
    }

    public void maybeTransformData(Unit<?> requestedUnits) {
        double delfreqTmp = getDelfreq();
        Complex[] data = workingData.clone();
        Unit<?> inputUnits = (AbstractUnit) workingUnits.getInputUnits();
        Unit<?> requested = (AbstractUnit) requestedUnits;

        if (requested.equals(workingUnits.getInputUnits())) {
            return;
        } else if (requested.isCompatible(inputUnits)) { // Just need to scale the data, no conversion to different quantity required.
            unitConversionScaleFactor = ResponseUnits.getScaleFactor(inputUnits, requested);
            for (int j = 0; j < data.length; ++j) {
                data[j] = data[j].multiply(1.0 / unitConversionScaleFactor); //The from transfer function is counts/g.m.u. so use 1/scale appliedScaleFactor
            }
            convertedUnits = new ResponseUnits(requested, workingUnits.getUnitObj(), UnitsStatus.QUANTITY_AND_UNITS);
            workingUnits = convertedUnits;
            conversionType = ConversionType.ScaleOnly;
            workingData = data;
            convertedData = workingData.clone();
        } else if (requested.isCompatible(inputUnits.divide(SECOND))) { //integrate w.r.t. time once
            Unit<?> transformed = inputUnits.divide(SECOND);
            unitConversionScaleFactor = ResponseUnits.getScaleFactor(transformed, requested);
            double twoPI = 2.0 * Math.PI;
            for (int i = 1; i < data.length; ++i) {
                double omega = twoPI * i * delfreqTmp;
                Complex jOmega = new Complex(0.0, omega);
                data[i] = data[i].divide(jOmega).divide(unitConversionScaleFactor);
            }
            conversionType = ConversionType.IntegrateOnce;
            convertedUnits = new ResponseUnits(requested, workingUnits.getUnitObj(), UnitsStatus.QUANTITY_AND_UNITS);
            workingUnits = convertedUnits;
            workingData = data;
            convertedData = workingData.clone();

        } else if (requested.isCompatible(inputUnits.divide(SECOND.pow(2)))) { //integrate w.r.t. time twice
            Unit<?> transformed = inputUnits.divide(SECOND.pow(2));
            unitConversionScaleFactor = ResponseUnits.getScaleFactor(transformed, requested);
            double twoPI = 2.0 * Math.PI;
            for (int i = 1; i < data.length; ++i) {
                double omega = twoPI * i * delfreqTmp;
                Complex jOmega = new Complex(0.0, omega);
                data[i] = data[i].divide(jOmega.multiply(jOmega)).divide(unitConversionScaleFactor);
            }
            conversionType = ConversionType.IntegrateTwice;
            convertedUnits = new ResponseUnits(requested, workingUnits.getUnitObj(), UnitsStatus.QUANTITY_AND_UNITS);
            workingUnits = convertedUnits;
            workingData = data;
            convertedData = workingData.clone();
        } else if (requested.isCompatible(inputUnits.multiply(SECOND))) { //differentiate w.r.t. time once
            Unit<?> transformed = inputUnits.multiply(SECOND);
            unitConversionScaleFactor = ResponseUnits.getScaleFactor(transformed, requested);
            double twoPI = 2.0 * Math.PI;
            for (int i = 1; i < data.length; ++i) {
                double omega = twoPI * i * delfreqTmp;
                Complex jOmega = new Complex(0.0, omega);
                data[i] = data[i].multiply(jOmega).divide(unitConversionScaleFactor);
            }
            conversionType = ConversionType.DifferentiateOnce;
            convertedUnits = new ResponseUnits(requested, workingUnits.getUnitObj(), UnitsStatus.QUANTITY_AND_UNITS);
            workingUnits = convertedUnits;
            workingData = data;
            convertedData = workingData.clone();

        } else if (requested.isCompatible(inputUnits.multiply(SECOND.pow(2)))) { //differentiate w.r.t. time twice
            Unit<?> transformed = inputUnits.multiply(SECOND.pow(2));
            unitConversionScaleFactor = ResponseUnits.getScaleFactor(transformed, requested);
            double twoPI = 2.0 * Math.PI;
            for (int i = 1; i < data.length; ++i) {
                double omega = twoPI * i * delfreqTmp;
                Complex jOmega = new Complex(0.0, omega);
                data[i] = data[i].multiply(jOmega.multiply(jOmega)).divide(unitConversionScaleFactor);
            }
            conversionType = ConversionType.differentiateTwice;
            //TODO: Ask Doug if workingUnits.getUnitObj() is appropriate - these disagree with requested 
            convertedUnits = new ResponseUnits(requested, workingUnits.getUnitObj(), UnitsStatus.QUANTITY_AND_UNITS);
            workingUnits = convertedUnits;
            workingData = data;
            convertedData = workingData.clone();

        } else {
            String msg = String.format("Requested units (%s) are not compatible with response units (%s)!", requested.toString(), workingUnits.getInputUnits().toString());
            throw new ResponseUnitsException(msg);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Arrays.deepHashCode(this.originalData);
        hash = 89 * hash + Arrays.deepHashCode(this.workingData);
        hash = 89 * hash + Arrays.deepHashCode(this.convertedData);
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.delfreq) ^ (Double.doubleToLongBits(this.delfreq) >>> 32));
        hash = 89 * hash + Objects.hashCode(this.originalUnits);
        hash = 89 * hash + Objects.hashCode(this.forcedUnits);
        hash = 89 * hash + Objects.hashCode(this.convertedUnits);
        hash = 89 * hash + Objects.hashCode(this.workingUnits);
        hash = 89 * hash + Objects.hashCode(this.metadata);
        hash = 89 * hash + Objects.hashCode(this.amplitudeAtWfdiscCalper);
        hash = 89 * hash + Objects.hashCode(this.normalizationStatus);
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.appliedScaleFactor) ^ (Double.doubleToLongBits(this.appliedScaleFactor) >>> 32));
        hash = 89 * hash + Objects.hashCode(this.amplitudeAtNominalCalper);
        hash = 89 * hash + Objects.hashCode(this.appliedScaling);
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.unitConversionScaleFactor) ^ (Double.doubleToLongBits(this.unitConversionScaleFactor) >>> 32));
        hash = 89 * hash + Objects.hashCode(this.conversionType);
        hash = 89 * hash + Objects.hashCode(this.appliedCalib);
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
        final TransferData other = (TransferData) obj;
        if (Double.doubleToLongBits(this.delfreq) != Double.doubleToLongBits(other.delfreq)) {
            return false;
        }
        if (Double.doubleToLongBits(this.appliedScaleFactor) != Double.doubleToLongBits(other.appliedScaleFactor)) {
            return false;
        }
        if (Double.doubleToLongBits(this.unitConversionScaleFactor) != Double.doubleToLongBits(other.unitConversionScaleFactor)) {
            return false;
        }
        if (!Arrays.deepEquals(this.originalData, other.originalData)) {
            return false;
        }
        if (!Arrays.deepEquals(this.workingData, other.workingData)) {
            return false;
        }
        if (!Arrays.deepEquals(this.convertedData, other.convertedData)) {
            return false;
        }
        if (!Objects.equals(this.originalUnits, other.originalUnits)) {
            return false;
        }
        if (!Objects.equals(this.forcedUnits, other.forcedUnits)) {
            return false;
        }
        if (!Objects.equals(this.convertedUnits, other.convertedUnits)) {
            return false;
        }
        if (!Objects.equals(this.workingUnits, other.workingUnits)) {
            return false;
        }
        if (!Objects.equals(this.metadata, other.metadata)) {
            return false;
        }
        if (!Objects.equals(this.amplitudeAtWfdiscCalper, other.amplitudeAtWfdiscCalper)) {
            return false;
        }
        if (this.normalizationStatus != other.normalizationStatus) {
            return false;
        }
        if (!Objects.equals(this.amplitudeAtNominalCalper, other.amplitudeAtNominalCalper)) {
            return false;
        }
        if (this.appliedScaling != other.appliedScaling) {
            return false;
        }
        if (this.conversionType != other.conversionType) {
            return false;
        }
        if (!Objects.equals(this.appliedCalib, other.appliedCalib)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TransferData{" + "originalData=" + originalData + ", workingData=" + workingData + ", convertedData=" + convertedData + ", delfreq=" + delfreq + ", originalUnits=" + originalUnits + ", forcedUnits=" + forcedUnits + ", convertedUnits=" + convertedUnits + ", workingUnits=" + workingUnits + ", metadata=" + metadata + ", amplitudeAtWfdiscCalper=" + amplitudeAtWfdiscCalper + ", normalizationStatus=" + normalizationStatus + ", appliedScaleFactor=" + appliedScaleFactor + ", amplitudeAtNominalCalper=" + amplitudeAtNominalCalper + ", appliedScaling=" + appliedScaling + ", unitConversionScaleFactor=" + unitConversionScaleFactor + ", conversionType=" + conversionType + ", appliedCalib=" + appliedCalib + '}';
    }

}
