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
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class RespFormatChecker {

    // Common SEED blockette identifiers found in RESP files (e.g., B050F03, B052F04)
    private static final Pattern BLOCK_PATTERN = Pattern.compile("^B\\d{3}F\\d{2}\\s+.*", Pattern.CASE_INSENSITIVE);

    public static boolean isRespFormat(Path filePath) throws FileNotFoundException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            int validLineCount = 0;

            // Read the first few non-empty/non-comment lines to look for RESP signatures
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comment lines (starting with #)
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check if the line matches blockette patterns like B050F03 Station: ...
                if (BLOCK_PATTERN.matcher(line).matches()) {
                    validLineCount++;
                    // If we find at least 2 valid blockette signatures, consider it a valid RESP file
                    if (validLineCount > 2) {
                        return true;
                    }
                }
            }
        } 
        return false;
    }

}