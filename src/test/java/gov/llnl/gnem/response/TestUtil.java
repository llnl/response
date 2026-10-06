/*-
 * #%L
 * Seismic Response Processing Module
 *  LLNL-CODE-856351
 *  This work was performed under the auspices of the U.S. Department of Energy
 *  by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
 * %%
 * Copyright (C) 2025 Lawrence Livermore National Laboratory
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class TestUtil {
    
    
    public static List<ImmutablePair<Double,Double>> getTruth(ClassLoader classLoader,String transferFunctionTruth, String testFileDirectory) throws FileNotFoundException {
        
        List<ImmutablePair<Double,Double>> result = new ArrayList<>();
        File file = new File(classLoader.getResource(testFileDirectory + transferFunctionTruth).getFile());
        try (Scanner scanner = new Scanner(file)) {
            scanner.useDelimiter("\n");
            while(scanner.hasNext()){
                String line = scanner.next();
                StringTokenizer st = new StringTokenizer(line);
                if(st.countTokens()==2){
                    Double v1 = Double.valueOf(st.nextToken());
                    Double v2 = Double.valueOf(st.nextToken());
                    result.add(new ImmutablePair<>( v1, v2));
                }
            }}
        return result;
    }

}
