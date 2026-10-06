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

import com.isti.jevalresp.ResponseUnits;
import edu.iris.Fissures.IfNetwork.Stage;
import java.io.File;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;

public class NDCTransferTest {

    private static final String TEST_FILE_DIRECTORY = "gov/llnl/gnem/response/";

    private final Pattern pattern = Pattern.compile(NDCTransfer.NUMBER);

    @Test
    public void testValidNumbers() {
        assertTrue(pattern.matcher("123").matches());
        assertTrue(pattern.matcher("-456").matches());
        assertTrue(pattern.matcher("7.89").matches());
        assertTrue(pattern.matcher("1.2e3").matches());
        assertTrue(pattern.matcher("-4.5E-6").matches());
        assertTrue(pattern.matcher("+4.5e+6").matches());
    }

    @Test
    public void testInvalidNumbers() {
        assertFalse(pattern.matcher("abc").matches());
        assertFalse(pattern.matcher("123a").matches());
        assertFalse(pattern.matcher("123 ").matches());
        assertFalse(pattern.matcher(" 123").matches());
        assertFalse(pattern.matcher("12.3.").matches());
        assertFalse(pattern.matcher("e123").matches());
    }

    @Test
    public void testComputeTransferFunctionFromFilePazfir() throws Exception {
        System.out.println("ComputeTransferFunctionFromFilePazfir");
        ClassLoader classLoader = getClass().getClassLoader();
        List<ImmutablePair<Double, Double>> truth = TestUtil.getTruth(classLoader, "pazfir_expected_values.txt", TEST_FILE_DIRECTORY);

        File file = new File(classLoader.getResource(TEST_FILE_DIRECTORY + "pazfir_example.resp").getFile());
        int nsamp = 500;
        double samprate = 100;
        int nfft = TransferFunctionUtils.next2(nsamp);
        int nfreq = nfft / 2 + 1;

        double[] xre = new double[nfreq];
        double[] xim = new double[nfreq];

        double delta = 1.0 / samprate;
        double delfrq = 1.0 / (nfft * delta);
        double epoch = 0.0;
        ResponseType type = ResponseType.PAZFIR;
        String filename = file.toString();
        NDCTransfer instance = new NDCTransfer();
        instance.computeTransferFunctionFromFile(nfreq, delfrq, epoch, xre, xim, type, filename);

        testResults(nfreq, truth, xre, xim);
    }

    @Test
    public void testComputeTransferFunctionFromFileFap() throws Exception {
        System.out.println("ComputeTransferFunctionFromFileFap");
        ClassLoader classLoader = getClass().getClassLoader();
        List<ImmutablePair<Double, Double>> truth = TestUtil.getTruth(classLoader, "fap_expected_values.txt", TEST_FILE_DIRECTORY);

        File file = new File(classLoader.getResource(TEST_FILE_DIRECTORY + "fap_example.resp").getFile());
        int nsamp = 500;
        double samprate = 100;
        int nfft = TransferFunctionUtils.next2(nsamp);
        int nfreq = nfft / 2 + 1;

        double[] xre = new double[nfreq];
        double[] xim = new double[nfreq];

        double delta = 1.0 / samprate;
        double delfrq = 1.0 / (nfft * delta);
        double epoch = 0.0;
        ResponseType type = ResponseType.FAP;
        String filename = file.toString();
        NDCTransfer instance = new NDCTransfer();
        instance.computeTransferFunctionFromFile(nfreq, delfrq, epoch, xre, xim, type, filename);

        testResults(nfreq, truth, xre, xim);
    }

    @Test
    public void testComputeTransferFunctionFromFilePaz() throws Exception {
        System.out.println("ComputeTransferFunctionFromFilePaz");
        ClassLoader classLoader = getClass().getClassLoader();
        List<ImmutablePair<Double, Double>> truth = TestUtil.getTruth(classLoader, "paz_expected_values.txt", TEST_FILE_DIRECTORY);

        File file = new File(classLoader.getResource(TEST_FILE_DIRECTORY + "paz_example.resp").getFile());
        int nsamp = 500;
        double samprate = 100;
        int nfft = TransferFunctionUtils.next2(nsamp);
        int nfreq = nfft / 2 + 1;

        double[] xre = new double[nfreq];
        double[] xim = new double[nfreq];

        double delta = 1.0 / samprate;
        double delfrq = 1.0 / (nfft * delta);
        double epoch = 0.0;
        ResponseType type = ResponseType.PAZ;
        String filename = file.toString();
        NDCTransfer instance = new NDCTransfer();
        instance.computeTransferFunctionFromFile(nfreq, delfrq, epoch, xre, xim, type, filename);

        testResults(nfreq, truth, xre, xim);
    }

    @Test
    public void testComputeTransferFunctionFromFilePazFap() throws Exception {
        System.out.println("ComputeTransferFunctionFromFilePazFap");
        ClassLoader classLoader = getClass().getClassLoader();
        List<ImmutablePair<Double, Double>> truth = TestUtil.getTruth(classLoader, "pazfap_expected_values.txt", TEST_FILE_DIRECTORY);

        File file = new File(classLoader.getResource(TEST_FILE_DIRECTORY + "pazfap_example.resp").getFile());
        int nsamp = 500;
        double samprate = 100;
        int nfft = TransferFunctionUtils.next2(nsamp);
        int nfreq = nfft / 2 + 1;

        double[] xre = new double[nfreq];
        double[] xim = new double[nfreq];

        double delta = 1.0 / samprate;
        double delfrq = 1.0 / (nfft * delta);
        double epoch = 0.0;
        ResponseType type = ResponseType.PAZFAP;
        String filename = file.toString();
        NDCTransfer instance = new NDCTransfer();
        instance.computeTransferFunctionFromFile(nfreq, delfrq, epoch, xre, xim, type, filename);

        testResults(nfreq, truth, xre, xim);
    }

    private void testResults(int nfreq, List<ImmutablePair<Double, Double>> truth, double[] xre, double[] xim) {
        double norm = 0;
        for (int j = 0; j < nfreq; ++j) {
            ImmutablePair<Double, Double> v = truth.get(j);

            double dx = v.getLeft() - xre[j];
            double dy = v.getRight() - xim[j];
            double err = Math.sqrt(dx * dx + dy * dy);
            norm += err;
        }
        norm /= nfreq;
        Assertions.assertEquals(0, norm, 0.001, "PAZFIR-Transfer");
    }

    /**
     * Test of getFromTransferFunction method, of class NDCTransfer.
     */
    @Test
    public void testGetFromTransferFunction() throws Exception {
        System.out.println("getFromTransferFunction (not tested)");
    }

    /**
     * Test of computeTransferFunctionFromFile method, of class NDCTransfer.
     */
    @Test
    public void testComputeTransferFunctionFromFile() throws Exception {
        System.out.println("computeTransferFunctionFromFile (replaced with tests for each type)");
    }

    /**
     * Test of transfer method, of class NDCTransfer.
     */
    @Test
    public void testTransfer() throws Exception {
        System.out.println("transfer not tested");
    }

}
