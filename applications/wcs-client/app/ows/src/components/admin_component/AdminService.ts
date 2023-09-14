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
 * Copyright 2003 - 2017 Peter Baumann /
 rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
/// <reference path="../../../assets/typings/tsd.d.ts"/>
/// <reference path="../../common/_common.ts"/>
/// <reference path="../../models/login/_login.ts"/>
/// <reference path="../wcs_component/settings/SettingsService.ts"/>

module rasdaman {
    export class AdminService {
        public static $inject = ["$rootScope", "$http", "$q", "rasdaman.WCSSettingsService",                                
                                "rasdaman.CredentialService"];

        public static RW_RIGHTS_COMMUNITY:string = "RW";

        public static PRIV_OWS_UPDATE_SRV:string = "PRIV_OWS_UPDATE_SRV";
        public static PRIV_OWS_WCS_INSERT_COV:string = "PRIV_OWS_WCS_INSERT_COV";
        public static PRIV_OWS_WCS_UPDATE_COV:string = "PRIV_OWS_WCS_UPDATE_COV";
        public static PRIV_OWS_WCS_DELETE_COV:string = "PRIV_OWS_WCS_DELETE_COV";

        public static PRIV_OWS_WMS_INSERT_LAYER:string = "PRIV_OWS_WMS_INSERT_LAYER";
        public static PRIV_OWS_WMS_UPDATE_LAYER:string = "PRIV_OWS_WMS_UPDATE_LAYER";
        public static PRIV_OWS_WMS_DELETE_LAYER:string = "PRIV_OWS_WMS_DELETE_LAYER";

        public static PRIV_OWS_WMS_INSERT_STYLE:string = "PRIV_OWS_WMS_INSERT_STYLE";
        public static PRIV_OWS_WMS_UPDATE_STYLE:string = "PRIV_OWS_WMS_UPDATE_STYLE";
        public static PRIV_OWS_WMS_DELETE_STYLE:string = "PRIV_OWS_WMS_DELETE_STYLE";

        public static PRIV_OWS_WCS_BLACKWHITELIST_COV = "PRIV_OWS_WCS_BLACKWHITELIST_COV";
        public static PRIV_OWS_WMS_BLACKWHITELIST_LAYER = "PRIV_OWS_WMS_BLACKWHITELIST_LAYER";


        public static adminRoles = [AdminService.PRIV_OWS_UPDATE_SRV, AdminService.PRIV_OWS_WCS_INSERT_COV, AdminService.PRIV_OWS_WCS_UPDATE_COV, AdminService.PRIV_OWS_WCS_DELETE_COV,
                        AdminService.PRIV_OWS_WMS_INSERT_LAYER, AdminService.PRIV_OWS_WMS_UPDATE_LAYER, AdminService.PRIV_OWS_WMS_DELETE_LAYER,
                        AdminService.PRIV_OWS_WMS_INSERT_STYLE, AdminService.PRIV_OWS_WMS_UPDATE_STYLE, AdminService.PRIV_OWS_WMS_DELETE_STYLE,
                        AdminService.PRIV_OWS_WCS_BLACKWHITELIST_COV, AdminService.PRIV_OWS_WMS_BLACKWHITELIST_LAYER,
                        AdminService.RW_RIGHTS_COMMUNITY // -- special rights for community
        ];                                

        public constructor(private $rootScope:angular.IRootScopeService,
                           private $http:angular.IHttpService,
                           private $q:angular.IQService,
                           private settings:rasdaman.WCSSettingsService,
                           private credentialService:rasdaman.CredentialService) {

        }

        /**
            Check from the list of granted roles of an admin user, if a given role exists
         */
        public static hasRole(roles:any[], role:string) {
            return roles.includes(AdminService.RW_RIGHTS_COMMUNITY)
               || roles.includes(role);
        }

        // OWS Metadata Management

        public updateServiceIdentification(serviceIdentification:admin.ServiceIdentification):angular.IPromise<any> {
            var result = this.$q.defer();                                               
            var requestUrl = this.settings.adminEndpoint + "/ows/serviceinfo";

            var requestHeaders = this.credentialService.getAuthorizationHeader(this.settings.wcsEndpoint);

            var request:angular.IRequestConfig = {
                method: 'POST',
                url: requestUrl,
                //Removed the transformResponse to prevent angular from parsing non-JSON objects.
                transformResponse: null,                
                headers: requestHeaders,
                data: serviceIdentification.toKVP()
            };

            // send request to Petascope and get response (headers and contents)
            this.$http(request).then(function (data:any) {
                result.resolve(data);
            }, function (error) {
                result.reject(error);
            });

            return result.promise;
        }      
        
        public updateServiceProvider(serviceProvider:admin.ServiceProvider):angular.IPromise<any> {
            var result = this.$q.defer();                                               
            var requestUrl = this.settings.adminEndpoint + "/ows/serviceinfo";

            var requestHeaders = this.credentialService.getAuthorizationHeader(this.settings.wcsEndpoint);

            var request:angular.IRequestConfig = {
                method: 'POST',
                url: requestUrl,
                //Removed the transformResponse to prevent angular from parsing non-JSON objects.
                transformResponse: null,        
                headers: requestHeaders,        
                data: serviceProvider.toKVP()
            };

            // send request to Petascope and get response (headers and contents)
            this.$http(request).then(function (data:any) {
                result.resolve(data);
            }, function (error) {
                result.reject(error);
            });

            return result.promise;
        }  
    }

}
