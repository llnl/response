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
//RespCallback.java:  Specifies a callback method.
//
//  12/10/2001 -- [ET]  Initial release version.
//    5/2/2002 -- [ET]  Added 'setRespProcObj()' method.
//    6/5/2002 -- [ET]  Added 'showInfoMessage()' method.
//   7/29/2002 -- [ET]  Added 'respEndDateObj' parameter to 'responseInfo()'.
//

package com.isti.jevalresp;

import java.util.Date;

import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Response;

/**
 * Interface RespCallback specifies a callback method that provides
 * information about a response.
 */
public interface RespCallback
{
    /**
     * Sets the 'RespProcessor' object to be used.
     * @param respProcObj response processing object
     */
  public void setRespProcObj(RespProcessor respProcObj);

    /**
     * Provides information about a response that has been located and
     * fetched.
     * @param fileName the name of source file for the response.
     * @param channelIdObj the channel ID associated with the response, or
     * null if a channel ID was not found.
     * @param respEndDateObj end date for channel ID, or null if a channel ID
     * was not found.
     * @param channelIdFName a string version of the channel ID associated
     * with the response, or null if a channel ID was not found.
     * @param respObj the response information, or null if a channel ID
     * was not found (error message in 'errMsgStr').
     * @param units
     * @param errMsgStr if 'channelIdObj' or 'respObj' is null then
     * 'errMsgStr' contains an error message string; otherwise null.
     * @return true if the response was processed successfully, false if
     * an error occurred.
     */
  public boolean responseInfo(String fileName,ChannelId channelIdObj,
                                  Date respEndDateObj,String channelIdFName,
                                         Response respObj,ResponseUnits units, String errMsgStr);

    /**
     * Shows the given informational message.
     * @param msgStr message string
     */
  public void showInfoMessage(String msgStr);
}
