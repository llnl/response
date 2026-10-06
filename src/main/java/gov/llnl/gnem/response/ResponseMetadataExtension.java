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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;

import com.isti.jevalresp.ResponseUnits;

/**
 *
 * @author dodge1
 */
public class ResponseMetadataExtension implements Serializable {

    private static final long serialVersionUID = -5754118158828367991L;
    private final Long responseId;
    private final ResponseType responseType;
    private final Unit<?> likelyUnitsForType;
    private final Unit<?> analysisDerivedUnits;
    private final Double medianResidualThisResponse;
    private final Double residualStdThisResponse;
    private final Integer evaluationsThisResponse;
    private final String failureProblemDetail;

    public ResponseMetadataExtension() {
        responseId = null;
        responseType = null;
        likelyUnitsForType = null;
        analysisDerivedUnits = null;
        medianResidualThisResponse = null;
        residualStdThisResponse = null;
        evaluationsThisResponse = null;
        failureProblemDetail = null;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeLong(responseId != null ? responseId : Long.MAX_VALUE);
        s.writeUTF(responseType != null ? responseType.toString() : "-");
        s.writeUTF(likelyUnitsForType != null ? likelyUnitsForType.toString() : "-");
        s.writeUTF(analysisDerivedUnits != null ? analysisDerivedUnits.toString() : "-");
        s.writeDouble(medianResidualThisResponse != null ? medianResidualThisResponse : Double.MAX_VALUE);
        s.writeDouble(residualStdThisResponse != null ? residualStdThisResponse : Double.MAX_VALUE);
        s.writeInt(evaluationsThisResponse != null ? evaluationsThisResponse : Integer.MAX_VALUE);
        s.writeUTF(failureProblemDetail != null ? failureProblemDetail : "-");
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {

        try {
            Class type = ResponseMetadataExtension.class;
            Field field = type.getDeclaredField("responseId");
            // make the field non final
            field.setAccessible(true);
            Long tmpLong = s.readLong();
            field.set(this, tmpLong == Long.MAX_VALUE ? null : tmpLong);

            // make the field final again
            field.setAccessible(false);
            field = type.getDeclaredField("responseType");
            field.setAccessible(true);
            String tmp = (String) s.readUTF();
            field.set(this, !tmp.equals("-") ? ResponseType.getResponseType(tmp) : null);
            field.setAccessible(false);
            tmp = (String) s.readUTF();
            field = type.getDeclaredField("likelyUnitsForType");
            field.setAccessible(true);
            field.set(this, !tmp.equals("-") ? ResponseUnits.parse(tmp) : null);
            field.setAccessible(false);

            tmp = (String) s.readUTF();
            field = type.getDeclaredField("analysisDerivedUnits");
            field.setAccessible(true);
            field.set(this, !tmp.equals("-") ? ResponseUnits.parse(tmp) : null);
            field.setAccessible(false);

            field = type.getDeclaredField("medianResidualThisResponse");
            field.setAccessible(true);
            Double tmpDouble = (Double) s.readDouble();
            field.set(this, tmpDouble == Double.MAX_VALUE ? null : tmpDouble);
            field.setAccessible(false);

            field = type.getDeclaredField("residualStdThisResponse");
            field.setAccessible(true);
            tmpDouble = (Double) s.readDouble();
            field.set(this, tmpDouble == Double.MAX_VALUE ? null : tmpDouble);
            field.setAccessible(false);

            field = type.getDeclaredField("evaluationsThisResponse");
            field.setAccessible(true);
            Integer tmpInt = (Integer) s.readInt();
            field.set(this, tmpInt == Integer.MAX_VALUE ? null : tmpInt);
            field.setAccessible(false);

            field = type.getDeclaredField("failureProblemDetail");
            field.setAccessible(true);
            tmp = (String) s.readUTF();
            field.set(this, tmp.equals("-") ? null : tmp);
            field.setAccessible(false);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ToResponseLookupKey.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ResponseMetadataExtension(long responseId,
            ResponseType responseType,
            Unit<?> likelyUnitsForType,
            Unit<?> analysisDerivedUnits,
            Double medianResidualThisResponse,
            Double residualStdThisResponse,
            Integer evaluationsThisResponse,
            String failureProblemDetail) {
        this.responseId = responseId;
        this.responseType = responseType;
        this.likelyUnitsForType = likelyUnitsForType;
        this.analysisDerivedUnits = analysisDerivedUnits;
        this.medianResidualThisResponse = medianResidualThisResponse;
        this.residualStdThisResponse = residualStdThisResponse;
        this.evaluationsThisResponse = evaluationsThisResponse;
        this.failureProblemDetail = failureProblemDetail;
    }

    public boolean isForceInputUnitsRequired() {
        if (responseType == null) {
            return false;
        }

        if (responseType == ResponseType.EVRESP) {
            return analysisDerivedUnits != null;
        } else {
            return analysisDerivedUnits != null || likelyUnitsForType != null;
        }
    }

    public Unit<?> getForceInputUnits() {
        if (analysisDerivedUnits != null) {
            return analysisDerivedUnits;
        } else if (likelyUnitsForType != null) {
            return likelyUnitsForType;
        } else {
            throw new IllegalStateException("No units available!");
        }
    }

    public ResponseUnitsSource getResponseUnitsSource() {
        if (analysisDerivedUnits != null) {
            return ResponseUnitsSource.EVALUATED;
        } else if (responseType != null && responseType == ResponseType.EVRESP) {
            return ResponseUnitsSource.REPORTED;
        } else if (likelyUnitsForType != null) {
            return ResponseUnitsSource.INFERRED;
        } else {
            return ResponseUnitsSource.UNKNOWN;
        }
    }

    public long getResponseId() {
        return responseId;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public Unit<?> getLikelyUnitsForType() {
        return likelyUnitsForType;
    }

    public Unit<?> getAnalysisDerivedUnits() {
        return analysisDerivedUnits;
    }

    public Double getMedianResidualThisResponse() {
        return medianResidualThisResponse;
    }

    public Double getResidualStdThisResponse() {
        return residualStdThisResponse;
    }

    public Integer getEvaluationsThisResponse() {
        return evaluationsThisResponse;
    }

    public String getFailureProblemDetail() {
        return failureProblemDetail;
    }

    @Override
    public String toString() {
        return "ResponseMetadataExtension{" + "responseId=" + responseId + ", responseType=" + responseType + ", likelyUnitsForType=" + likelyUnitsForType + ", analysisDerivedUnits=" + analysisDerivedUnits + ", medianResidualThisResponse=" + medianResidualThisResponse + ", residualStdThisResponse=" + residualStdThisResponse + ", evaluationsThisResponse=" + evaluationsThisResponse + ", failureProblemDetail=" + failureProblemDetail + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.responseId);
        hash = 61 * hash + Objects.hashCode(this.responseType);
        hash = 61 * hash + Objects.hashCode(this.likelyUnitsForType);
        hash = 61 * hash + Objects.hashCode(this.analysisDerivedUnits);
        hash = 61 * hash + Objects.hashCode(this.medianResidualThisResponse);
        hash = 61 * hash + Objects.hashCode(this.residualStdThisResponse);
        hash = 61 * hash + Objects.hashCode(this.evaluationsThisResponse);
        hash = 61 * hash + Objects.hashCode(this.failureProblemDetail);
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
        final ResponseMetadataExtension other = (ResponseMetadataExtension) obj;
        if (!Objects.equals(this.failureProblemDetail, other.failureProblemDetail)) {
            return false;
        }
        if (!Objects.equals(this.responseId, other.responseId)) {
            return false;
        }
        if (this.responseType != other.responseType) {
            return false;
        }
        if (!ResponseUnits.areUnitsEquivalent(this.likelyUnitsForType, other.likelyUnitsForType)) {
            return false;
        }
        if (!ResponseUnits.areUnitsEquivalent(this.analysisDerivedUnits, other.analysisDerivedUnits)) {
            return false;
        }
        if (!Objects.equals(this.medianResidualThisResponse, other.medianResidualThisResponse)) {
            return false;
        }
        if (!Objects.equals(this.residualStdThisResponse, other.residualStdThisResponse)) {
            return false;
        }
        if (!Objects.equals(this.evaluationsThisResponse, other.evaluationsThisResponse)) {
            return false;
        }
        return true;
    }

}
