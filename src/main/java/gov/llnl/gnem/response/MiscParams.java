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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.gnem.response;

/**
 *
 * @author dodge1
 */
public class MiscParams {

    private final double referencePeriod = 20;
    private final double referenceDistance = 1.0;
    private final double taperPercent = 5.0;
    private final double tfactor = 2;
    private final double highpassFrac = 0.85;
    private final double highCutFrac = 0.9;
    private final double lowCutFrac = 0.8;

    /**
     * @return the referencePeriod
     */
    public double getReferencePeriod() {
        return referencePeriod;
    }

    /**
     * @return the taperPercent
     */
    public double getTaperPercent() {
        return taperPercent;
    }

    /**
     * @return the tfactor
     */
    public double getTfactor() {
        return tfactor;
    }

    /**
     * @return the highpassFrac
     */
    public double getHighpassFrac() {
        return highpassFrac;
    }

    /**
     * @return the highCutFrac
     */
    public double getHighCutFrac() {
        return highCutFrac;
    }

    /**
     * @return the lowCutFrac
     */
    public double getLowCutFrac() {
        return lowCutFrac;
    }

    public double getReferenceDistance() {
        return referenceDistance;
    }

    private static class MiscParamsHolder {

        private static final MiscParams instance = new MiscParams();
    }

    public static MiscParams getInstance() {
        return MiscParamsHolder.instance;
    }

    private MiscParams() {
    }
}
