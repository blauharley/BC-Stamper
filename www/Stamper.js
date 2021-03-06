/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

var exec = require("cordova/exec");
module.exports = {
    /*
     @info
     request provider

     @params

     successCallback: Function
     @param coords:object
         @param accurancy:integer
         @param latitude:integer
         @param longitude:integer
         @param heading:float
         @param altitude:float
         @param speed:float
         @param gps_fix:boolean
         @param provider_enabled:boolean
         @param out_of_service:boolean
     @param timestamp:integer

     errorCallback: Function
     options:{
         interval: 60000 (in milliseconds)
     }
     */
    requestProvider : function (successCallback, errorCallback, options) {
        var interval = options.interval ? options.interval : new Error('no interval');
        exec(successCallback, errorCallback, "Stamper", "request", [interval]);
    },

    stopService: function(successCallback, errorCallback){
        exec(successCallback, errorCallback, "Stamper", "stop", []);
    }

};