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
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class SacPoleZeroValidator {

    // Regex to match scientific or standard floating-point numbers (e.g., 0.0, -1.23e+02)
    private static final String NUMBER_REGEX = "^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?";
    private static final Pattern ONE_NUMBER_PATTERN = Pattern.compile("^\\s*" + NUMBER_REGEX + "\\s*$");

    /**
     * Validates whether a file conforms to the SAC polezero format.
     *
     * @param filePath Path to the instrument file
     * @return true if it matches the format, false otherwise
     */
    public static boolean isSacPoleZero(String filePath) {
        boolean hasZerosKeyword = false;
        boolean hasPolesKeyword = false;

        int expectedZeros = -1;
        int expectedPoles = -1;

        int zerosCount = 0;
        int polesCount = 0;

        String currentSection = ""; // "ZEROS", "POLES", or "CONSTANT"

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comment lines (* or #)
                if (line.isEmpty() || line.startsWith("*") || line.startsWith("#")) {
                    continue;
                }

                String upperLine = line.toUpperCase();

                // 1. Parse Key Headers
                if (upperLine.startsWith("ZEROS")) {
                    hasZerosKeyword = true;
                    currentSection = "ZEROS";
                    expectedZeros = parseCountAfterKeyword(line);
                    if (expectedZeros == -1) {
                        return false; // Invalid count format
                    }
                    continue;
                }

                if (upperLine.startsWith("POLES")) {
                    hasPolesKeyword = true;
                    currentSection = "POLES";
                    expectedPoles = parseCountAfterKeyword(line);
                    if (expectedPoles == -1) {
                        return false; // Invalid count format
                    }
                    continue;
                }

                if (upperLine.startsWith("CONSTANT")) {
                    currentSection = "CONSTANT";
                    // Constant should have a number right after it or on the next token
                    String remainder = line.substring("CONSTANT".length()).trim();
                    if (!remainder.isEmpty() && !ONE_NUMBER_PATTERN.matcher(remainder).matches()) {
                        return false;
                    }
                    continue;
                }

                // 2. Validate Data Lines based on active section
                switch (currentSection) {
                    case "ZEROS":
                        if(hasTwoNumbers( line)) {
                            zerosCount++;
                        } else {
                            return false; // Found non-complex number line where zero was expected
                        }
                        break;

                    case "POLES":
                        if(hasTwoNumbers( line)) {
                            polesCount++;
                        } else {
                            return false; // Found non-complex number line where pole was expected
                        }
                        break;

                    case "CONSTANT":
                        // If constant value wasn't inline, it might be on its own line
                        if (!ONE_NUMBER_PATTERN.matcher(line).matches()) {
                            return false;
                        }
                        // Reset section after parsing the constant value
                        currentSection = "";
                        break;

                    default:
                        // Found data lines before any valid keyword section
                        return false;
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return false;
        }

        // 3. Final structural validation checks
        boolean basicKeywordsPresent = hasZerosKeyword && hasPolesKeyword;
        boolean zeroCountMatches = (expectedZeros >= 0 && (zerosCount == expectedZeros || zerosCount == 0));// Zeros can be generated from the zeros count.|| zerosCount == expectedZeros);
        boolean poleCountMatches = (expectedPoles == -1 || polesCount == expectedPoles);

        return basicKeywordsPresent && zeroCountMatches && poleCountMatches;
    }

    private static boolean hasTwoNumbers(String line) {
        StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() < 2) {
            return false;
        } else {
            String token1 = st.nextToken();
            String token2 = st.nextToken();
            if (!ONE_NUMBER_PATTERN.matcher(token1).matches()) {
                return false;
            }
            if (!ONE_NUMBER_PATTERN.matcher(token2).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts the block integer count following ZEROS or POLES keywords (e.g.,
     * "ZEROS 3")
     */
    private static int parseCountAfterKeyword(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return 0; // Standard default fallback if just "ZEROS" is typed without a trailing integer
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
