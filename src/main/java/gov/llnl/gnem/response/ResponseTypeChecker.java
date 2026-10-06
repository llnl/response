/*-
 * #%L
 * Seismic Response Processing Module
 *  LLNL-CODE-856351
 *  This work was performed under the auspices of the U.S. Department of Energy
 *  by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
 * %%
 * Copyright (C) 2026Lawrence Livermore National Laboratory
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResponseTypeChecker {

    public static ResponseType determineResponseType(String fileName) {
        try {
            if (RespFormatChecker.isRespFormat(Paths.get(fileName))) {
                return ResponseType.EVRESP;
            } else if (SacPoleZeroValidator.isSacPoleZero(fileName)) {
                return ResponseType.SACPZF;
            } else {
                List<GroupStage> stages = NDCTransfer.computeGroupStagesFromFile(new File(fileName));
                if (!stages.isEmpty()) {
                    return ResponseType.CSS;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger("ResponseTypeChecker").log(Level.WARNING, ex.getMessage());
            return ResponseType.UNKNOWN;
        }
        return ResponseType.UNKNOWN;
    }
}
