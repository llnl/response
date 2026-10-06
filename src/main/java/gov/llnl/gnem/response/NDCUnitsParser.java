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

import edu.iris.Fissures.model.UnitImpl;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import com.isti.jevalresp.ResponseUnits;
import com.isti.jevalresp.UnitsStatus;

import tec.units.ri.unit.MetricPrefix;
import static tec.units.ri.unit.Units.METRE;
import static tec.units.ri.unit.Units.METRE_PER_SECOND;
import static tec.units.ri.unit.Units.METRE_PER_SQUARE_SECOND;
import static tec.units.ri.unit.Units.PASCAL;

/**
 *
 * @author dodge1
 */
public class NDCUnitsParser {

    public ResponseUnits getResponseUnits(File file) throws FileNotFoundException {
        ResponseUnits quantity = new ResponseUnits();

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.trim().startsWith("#")) {
                    String tmp = line.toLowerCase();
                    if (tmp.contains("nm/s/s")) {
                        return new ResponseUnits(MetricPrefix.NANO(METRE_PER_SQUARE_SECOND), UnitImpl.NANOMETER_PER_SECOND_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("nm/s")) {
                        return new ResponseUnits(MetricPrefix.NANO(METRE_PER_SECOND), UnitImpl.NANOMETER_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("nm/count")) {
                        return new ResponseUnits(MetricPrefix.NANO(METRE), UnitImpl.NANOMETER, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("nanometer")) {
                        return new ResponseUnits(MetricPrefix.NANO(METRE), UnitImpl.NANOMETER, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("m/s^2") || tmp.contains("mps^2") || tmp.contains("m/s/s")) {
                        return new ResponseUnits(METRE_PER_SQUARE_SECOND, UnitImpl.METER_PER_SECOND_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("m/s/s")) {
                        return new ResponseUnits(METRE_PER_SQUARE_SECOND, UnitImpl.METER_PER_SECOND_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("m/s")) {
                        return new ResponseUnits(METRE_PER_SECOND, UnitImpl.METER_PER_SECOND, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("pascal")) {
                        return new ResponseUnits(PASCAL, UnitImpl.PASCAL, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("upa")) {
                        return new ResponseUnits(MetricPrefix.MICRO(PASCAL), UnitImpl.PASCAL, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("response in units lookup:              	pa")) {
                        return new ResponseUnits(PASCAL, UnitImpl.PASCAL, UnitsStatus.QUANTITY_AND_UNITS);
                    }
                    if (tmp.contains("counts/pa") || tmp.contains("counts/(pa)") || tmp.contains("pa/count")) {
                        return new ResponseUnits(PASCAL, UnitImpl.PASCAL, UnitsStatus.QUANTITY_AND_UNITS);
                    }
//                    if (tmp.contains("inch/second")) {
//                        return new ResponseUnits(ResponseUnits.INCH_PER_SECOND, UnitImpl.VELOCITY, UnitsStatus.QUANTITY_AND_UNITS);
//                    }

                }

            }

        }

        return quantity;
    }
}
