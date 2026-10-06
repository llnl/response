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
//RespArraysInfo.java:  Contains arrays and other information for a
//                      response or one stage of a response.
//
//  3/25/2005 -- [ET]  Initial version.
// 10/27/2005 -- [ET]  Added methods 'setFrequencyArray()' and
//                     'setAmpPhaseArray()'.
//

package com.isti.jevalresp;

/**
 * Class RespArraysInfo contains arrays and other information for a
 * response or one stage of a response.
 */
public class RespArraysInfo
{
    /** Array of complex-spectra values for response. */
  public final ComplexBlk [] cSpectraArray;
    /** Array of corresponding frequency values. */
  public double [] frequencyArr;
    /** Identification string for response information. */
  public final String identifyStr;
    /** Handle to array of amplitude/phase values for response. */
  public AmpPhaseBlk [] ampPhaseArray = null;

  /**
   * Creates a response arrays/information object.
   * @param cSpectraArrayLen length of array of complex-spectra
   * values for response.
   * @param frequencyArr array of corresponding frequency values.
   * @param identifyStr identification string for response.
   * @param ampPhaseArray array of amplitude/phase values for response.
   */
  public RespArraysInfo(int cSpectraArrayLen, double [] frequencyArr,
                           String identifyStr, AmpPhaseBlk [] ampPhaseArray)
  {
    cSpectraArray = new ComplexBlk[cSpectraArrayLen];
    for(int i=0; i<cSpectraArrayLen; ++i)
      cSpectraArray[i] = null;
    this.frequencyArr = frequencyArr;
    this.identifyStr = identifyStr;
    this.ampPhaseArray = ampPhaseArray;
  }

  /**
   * Creates a response arrays/information object.
   * @param cSpectraArrayLen length of array of complex-spectra
   * values for response.
   * @param frequencyArr array of corresponding frequency values.
   * @param identifyStr identification string for response.
   */
  public RespArraysInfo(int cSpectraArrayLen, double [] frequencyArr,
                                                         String identifyStr)
  {
    this(cSpectraArrayLen,frequencyArr,identifyStr,null);
  }

  /**
   * Creates a response arrays/information object.
   * @param cSpectraArray array of complex-spectra values for response.
   * @param frequencyArr array of corresponding frequency values.
   * @param identifyStr identification string for response.
   * @param ampPhaseArray array of amplitude/phase values for response.
   */
  public RespArraysInfo(ComplexBlk [] cSpectraArray, double [] frequencyArr,
                           String identifyStr, AmpPhaseBlk [] ampPhaseArray)
  {
    this.cSpectraArray = cSpectraArray;
    this.frequencyArr = frequencyArr;
    this.identifyStr = identifyStr;
    this.ampPhaseArray = ampPhaseArray;
  }

  /**
   * Creates a response arrays/information object.
   * @param cSpectraArray array of complex-spectra values for response.
   * @param frequencyArr array of corresponding frequency values.
   * @param identifyStr identification string for response.
   */
  public RespArraysInfo(ComplexBlk [] cSpectraArray, double [] frequencyArr,
                                                         String identifyStr)
  {
    this(cSpectraArray,frequencyArr,identifyStr,null);
  }

  /**
   * Enters the given value into the array of complex-spectra values for
   * the response.  The values from the given 'ComplexBlk' object are
   * copied into a new 'ComplexBlk' object which is then entered into
   * the array.
   * @param idx the array index value to use.
   * @param cBlkObj the 'ComplexBlk' object from which to take the values.
   */
  public void enterCSpectraVal(int idx, ComplexBlk cBlkObj)
  {
    cSpectraArray[idx] = new ComplexBlk(cBlkObj.real,cBlkObj.imag);
  }

  /**
   * Enters the given array as the frequency array for this object.
   * @param freqArr frequency array to be entered.
   */
  public void setFrequencyArray(double [] freqArr)
  {
    frequencyArr = freqArr;
  }

  /**
   * Enters the given array as the amp/phase array for this object.
   * @param ampPhaArr amp/phase array to be entered.
   */
  public void setAmpPhaseArray(AmpPhaseBlk [] ampPhaArr)
  {
    ampPhaseArray = ampPhaArr;
  }

  /**
   * Enters the given arrays to be the amp/phase array for this object.
   * @param ampArr amplitude array to be entered.
   * @param phaArr phase array to be entered.
   */
  public void setAmpPhaseArray(double [] ampArr, double [] phaArr)
  {
    final AmpPhaseBlk [] newAmpPhaseArr;
    final int arrLen;
    if((arrLen=ampArr.length) == phaArr.length)
    {    //array lengths are the same
      newAmpPhaseArr = new AmpPhaseBlk[arrLen];
      for(int i=0; i<arrLen; ++i)      //copy in values
        newAmpPhaseArr[i] = new AmpPhaseBlk(ampArr[i],phaArr[i]);
    }
    else
    {    //array lengths are different
      final int ampArrLen = arrLen;
      final int phaArrLen = phaArr.length;
      final int maxArrLen = (ampArrLen >= phaArrLen) ? ampArrLen : phaArrLen;
      newAmpPhaseArr = new AmpPhaseBlk[maxArrLen];
      for(int i=0; i<maxArrLen; ++i)
      {                                          //copy in values
        newAmpPhaseArr[i] = new AmpPhaseBlk(
                                        ((i < ampArrLen) ? ampArr[i] : 0.0),
                                       ((i < phaArrLen) ? phaArr[i] : 0.0));
      }
    }
    ampPhaseArray = newAmpPhaseArr;       //set new array
  }

//  /**
//   * Returns an array containing the amplitude values held by this object.
//   * @return A new 'double' array containing the amplitude values held by
//   * this object, or an empty array if none are available.
//   */
//  public double [] getAmpArray()
//  {
//         //set local handle to amp/phase array (in case it gets changed):
//    final AmpPhaseBlk [] localAmpPhaseArr = ampPhaseArray;
//    final int arrLen = (localAmpPhaseArr != null) ?
//                                                localAmpPhaseArr.length : 0;
//    final double [] retArr = new double[arrLen];      //amp values array
//    for(int i=0; i<arrLen; ++i)             //copy over values
//      retArr[i] = localAmpPhaseArr[i].amp;
//    return retArr;
//  }
//
//  /**
//   * Returns an array containing the phase values held by this object.
//   * @return A new 'double' array containing the phase values held by
//   * this object, or an empty array if none are available.
//   */
//  public double [] getPhaseArray()
//  {
//         //set local handle to amp/phase array (in case it gets changed):
//    final AmpPhaseBlk [] localAmpPhaseArr = ampPhaseArray;
//    final int arrLen = (localAmpPhaseArr != null) ?
//                                                localAmpPhaseArr.length : 0;
//    final double [] retArr = new double[arrLen];      //phase values array
//    for(int i=0; i<arrLen; ++i)             //copy over values
//      retArr[i] = localAmpPhaseArr[i].phase;
//    return retArr;
//  }
}
