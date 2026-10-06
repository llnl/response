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
//ComplexBlk.java:  Holds a complex number.
//
//   11/7/2001 -- [ET]
//

package com.isti.jevalresp;

/**
 * Class ComplexBlk holds a complex number.
 */
public class ComplexBlk
{
  public double real,imag;

  public ComplexBlk(double real,double imag)
  {
    this.real = real;
    this.imag = imag;
  }

    /**
     * Performs complex multiplication; complex version of this *= val.
     * @param val value
     */
  public void zMultiply(ComplexBlk val)
  {
    final double r = real*val.real - imag*val.imag;
    final double i = imag*val.real + real*val.imag;
    real = r;
    imag = i;
  }

    /**
     * Performs complex multiplication; complex version of
     * this *= (realVal,imagVal).
     * @param realVal real value
     * @param imagVal imaginary value
     */
  public void zMultiply(double realVal,double imagVal)
  {
    final double r = real*realVal - imag*imagVal;
    final double i = imag*realVal + real*imagVal;
    real = r;
    imag = i;
  }

  @Override
  public String toString()
  {
    return "real=" + real + ", imag=" + imag;
  }
}
