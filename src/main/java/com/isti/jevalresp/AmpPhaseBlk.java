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
//AmpPhaseBlk.java:  Holds an amplitude and a phase value.
//
//  11/20/2001 -- [ET]  Initial version.
//   3/24/2005 -- [ET]  Made variables be declared "final"; added doc.
//

package com.isti.jevalresp;

/**
 * Class AmpPhaseBlk holds amplitude and a phase value.
 */
public class AmpPhaseBlk
{
    /** Amplitude value. */
  public final double amp;
    /** Phase value. */
  public final double phase;

  /**
   * Creates a block holding an amplitude and a phase value.
   * @param amp amplitude value.
   * @param phase phase value.
   */
  public AmpPhaseBlk(double amp,double phase)
  {
    this.amp = amp;
    this.phase = phase;
  }

  /**
   * Returns a string representation of the values held by this block.
   * @return A string representation of the values held by this block.
   */
  @Override
  public String toString()
  {
    return "amp=" + amp + ", phase=" + phase;
  }
}
