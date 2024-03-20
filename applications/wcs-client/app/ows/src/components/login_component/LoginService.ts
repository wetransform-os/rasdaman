/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2023 Peter Baumann /
 rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
///<reference path="../../../assets/typings/tsd.d.ts"/>
///<reference path="../../_all.ts"/>

module rasdaman {
    export class LoginService {

        public static $inject = [
            "$http",
            "$q",
            "$rootScope",            
            "$log",
            "rasdaman.WCSSettingsService",
            "rasdaman.WMSSettingsService",   
            "Notification",
            "rasdaman.ErrorHandlingService",
            "rasdaman.CredentialService",
            "$window",
            "Notification"
        ];

        private $http:angular.IHttpService;
        private $q:angular.IQService;
        private $rootScope:angular.IRootScopeService;
        private $log:angular.ILogService;              
        private wcsSettingsService:rasdaman.WCSSettingsService;
        private wmsSettingsService:rasdaman.WMSSettingsService;
        private alertService:any;
        private errorHandlingService:ErrorHandlingService;
        private credentialService:rasdaman.CredentialService;
        private $window:ng.IWindowService;
        private notificationService:any;


        public constructor(private $injectedHttp:angular.IHttpService,
            private $injectedQ:angular.IQService,
            private $injectedRootScope:angular.IRootScopeService,
            private $injectedLog:angular.ILogService,                          
            private injectedWcsSettingsService:rasdaman.WCSSettingsService,
            private injectedWmsSettingsService:rasdaman.WMSSettingsService,
            private injectedAlertService:any,
            private injectedErrorHandlingService:ErrorHandlingService,
            private injectedCredentialService:rasdaman.CredentialService,
            private injected$Window:ng.IWindowService,
            private injectedNotificationService:any) {

                this.$http = $injectedHttp;
                this.$q = $injectedQ;
                this.$rootScope = $injectedRootScope;
                this.$log = $injectedLog;
                this.wcsSettingsService = injectedWcsSettingsService;
                this.wmsSettingsService = injectedWmsSettingsService;
                this.alertService = injectedAlertService;
                this.errorHandlingService = injectedErrorHandlingService;
                this.credentialService = injectedCredentialService;
                this.$window = injected$Window;
                this.notificationService = injectedNotificationService;

        }

        // // --------- defined functions -------------

        /**
         * If petascope enable basic authentication header then it needs to show login form
         * if user has not authenticated
         */
        checkPetascopeEnableAuthentication = function(): angular.IPromise<any> {

            var result = this.$q.defer();
            var requestUrl = this.wcsSettingsService.contextPath + "/admin/authisactive";

            this.$http.get(requestUrl)
                .then(dataObj => {
                    let dataJSON = dataObj.data;
                    result.resolve(dataJSON);
                }, errorObj => {
                    alert("Fatal Error: Failed to connect to petascope at URL: \n" + requestUrl 
                            + "\nhence, WSClient cannot load.  \n\nHint: make sure petascope is running at the URL first, then, reload the web page.");
                });

            return result.promise;
        }

        /**
         * Authenticate to petascope endpoint with credentials         
         */
        authenticateToPetascope = function(username:string, password:string): boolean {
            var requestUrl = this.wcsSettingsService.contextPath + "/login"; 
            this.credentialService.get
            let headersObj = this.credentialService.createBasicAuthenticationHeader(username, password);

            this.$http.get(requestUrl, {
                headers: headersObj
            }).then(dataObj => {              
                // NOTE: here it is used array function for being possible to pass this from class to $http.get()                  
                // Valid stored credentials
                this.$rootScope.homeLoggedIn = true;
                this.$rootScope.usernameLoggedIn = username;  
                // get list of logged in user's roles                          
                this.$rootScope.userLoggedInRoles = dataObj.data.split(",");

                this.credentialService.persitCredential(this.wcsSettingsService.wcsEndpoint, new login.Credential(username, password));
                
                // If logged in successfully, then request capabilities
                this.$rootScope.wcsReloadServerCapabilities = true;
                this.$rootScope.wmsReloadServerCapabilities = true;
                return true;                                
            }, errorObj => {
                this.errorHandlingService.handleError(errorObj);
                return false;
            });

            return false;
        }

        /**
         * Check if stored username and password of rasdaman user are valid, if not and petascope is enabled with basic header
         * then it must show the login form.
         */
        checkStoredRadamanCredentials = function(): any {
                
            // check if stored credentials are usable                        
            var credentialsDict = this.credentialService.credentialsDict;
            if (credentialsDict != null) {
                var obj = credentialsDict[this.wcsSettingsService.wcsEndpoint];
                if (obj != null) { 

                    // Then, check if the stored credentials are still valid
                    let result = this.authenticateToPetascope(obj["username"], obj["password"]);
                    return result;
                } else {
                    // In this case there is no stored credentials
                    return null;
                }
            }

            return false;

        }        
    }
}
