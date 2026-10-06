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
public enum Units {

    Default(0, "def"), Displacement(1, "dis"), Velocity(2, "vel"), Acceleration(3, "acc");
    private final int evrespCode;
    private final String shortString;

    Units(int code, String shortString) {
        evrespCode = code;
        this.shortString = shortString;
    }

    public int getEvrespCode() {
        return evrespCode;
    }

    public String getShortString() {
        return shortString;
    }
}
