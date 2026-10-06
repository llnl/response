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

/*

 *  COPYRIGHT NOTICE

 *  RBAP Version 1.0

 *  Copyright (C) 2002 Lawrence Livermore National Laboratory.

 */
/**
 * A type-safe enum class for response types.
 *
 * @author Doug Dodge
 */
public enum ResponseType {

    EVRESP("evresp"), SACPZF("sacpzf"), PAZ("paz"), FAP("fap"), PAZFIR(
            "pazfir"), PAZFAP("pazfap"), FIRFAP("firfap"), CSS("css"), UNKNOWN("unknown");

    private final String dbValue;

    ResponseType(String dbvalue) {
        this.dbValue = dbvalue;
    }

    public boolean isNDCType() {
        return this == PAZ || this == FAP || this == PAZFIR || this == PAZFAP || this == FIRFAP || this == CSS;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static boolean isCssType(ResponseType type) {
        switch (type) {
            case PAZ:
            case FAP:
            case PAZFIR:
            case FIRFAP:
            case PAZFAP:
            case CSS:
                return true;
            default:
                return false;
        }

    }

    /**
     * Utility method to convert a String representation of the response type.
     * This methods supports aliases in addition to the 'name' for some of the
     * ResponseTypes.
     *
     * @param type String representing the response type
     * @return ResponseType enum
     */
    public static ResponseType getResponseType(String type) {
        type = type.toLowerCase();

        switch (type) {
            case "evresp":
            case "resp":
                return ResponseType.EVRESP;
            case "sacpzf":
            case "sacpz":
            case "polezero":
            case "pz":
                return ResponseType.SACPZF;
            case "paz":
                return ResponseType.PAZ;
            case "fap":
                return ResponseType.FAP;
            case "pazfir":
                return ResponseType.PAZFIR;
            case "firfap":
                return ResponseType.FIRFAP;
            case "pazfap":
                return ResponseType.PAZFAP;
            case "css":
                return ResponseType.CSS;
            case "unknown":
                return ResponseType.UNKNOWN;
            default:
                throw new IllegalStateException("Unrecognized response type " + type);
        }
    }
}
