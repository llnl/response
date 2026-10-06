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

import com.oregondsp.signalProcessing.fft.CDFTdp;

/**
 *
 * @author addair1
 */
public class DCPFT {
    
    private int mpow2;
    private final CDFTdp fft;

    public DCPFT(int nfreq) {
        mpow2 = 0;

        while (Math.pow(2, mpow2) < nfreq) {
            mpow2 += 1;
        }

        
        fft = new CDFTdp(mpow2);
    }

    public void dcpft(double[] re, double[] im, int nfreq, int sgn) {
        double[] tre = new double[nfreq];
        double[] tim = new double[nfreq];
        if( sgn < 0 ){
            fft.evaluate(re, im, tre, tim);
            System.arraycopy(tre, 0, re, 0, nfreq);
            System.arraycopy(tim, 0, im, 0, nfreq);
        }
        else{
            fft.evaluateInverse(re, im, tre, tim);
            System.arraycopy(tre, 0, re, 0, nfreq);
            System.arraycopy(tim, 0, im, 0, nfreq);
            for( int j = 0; j < nfreq; ++j){
                re[j] *= nfreq;
                im[j] *= nfreq;
            }
        }

    }
}
