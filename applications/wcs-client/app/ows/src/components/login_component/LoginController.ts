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
 * Copyright 2003 - 2019 Peter Baumann /
 rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

///<reference path="../../../assets/typings/tsd.d.ts"/>
///<reference path="../../_all.ts"/>
///<reference path="../wcs_component/settings/SettingsService.ts"/>
///<reference path="../wms_component/settings/SettingsService.ts"/>

module rasdaman {
    export class LoginController {

        public static $inject = [
            "$http",
            "$q",
            "$scope",
            "$rootScope",            
            "$log",
            "rasdaman.WCSSettingsService",
            "rasdaman.WMSSettingsService",   
            "Notification",
            "rasdaman.ErrorHandlingService",
            "rasdaman.CredentialService",
            "rasdaman.LoginService"
        ];

        public constructor(private $http:angular.IHttpService,
                           private $q:angular.IQService,
                           private $scope:LoginControllerScope,
                           private $rootScope:angular.IRootScopeService,
                           private $log:angular.ILogService,                          
                           private wcsSettingsService:rasdaman.WCSSettingsService,
                           private wmsSettingsService:rasdaman.WMSSettingsService,
                           private alertService:any,
                           private errorHandlingService:ErrorHandlingService,
                           private credentialService:rasdaman.CredentialService,
                           private loginService:rasdaman.LoginService) {

            $scope.petascopeEndPoint = wcsSettingsService.wcsEndpoint;            
            $scope.credential = new login.Credential("", "");
                
            // Login with rasdaman user credentials
            $scope.login = (...args: any[]) => {            
                loginService.authenticateToPetascope($scope.credential.username, $scope.credential.password);                
            }            
            
        }      
    }

    interface LoginControllerScope {
        displayError:boolean;

        petascopeEndPoint:string;
        credential:login.Credential;        
        login(...args: any[]):void;
        checkPetascopeEnableAuthentication(...args: any[]):angular.IPromise<any>;
    }
}
