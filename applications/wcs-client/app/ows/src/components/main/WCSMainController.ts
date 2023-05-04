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

///<reference path="../../../assets/typings/tsd.d.ts"/>
///<reference path="../../models/wcs_model/wcs/Capabilities.ts"/>
///<reference path="../../models/wcs_model/wcs/CoverageDescription.ts"/>

module rasdaman {
    /**
     * This class holds the state of the OWS client and orchestrates
     * interaction between the tabs and state transfer.
     * All the other WCS controllers will inherit 
     * this ****controller's scope****.
     */
    export class WCSMainController {
        public static $inject = ["$scope", "$rootScope", "$state", "rasdaman.AdminService"];

        public constructor(private $scope:WCSMainControllerScope, $rootScope:angular.IRootScopeService, $state:any, adminService:rasdaman.AdminService) {
            
            this.initializeTabs($scope);        
            
            $rootScope.wcsReloadServerCapabilities = null;

            // NOTE: When petascope admin user logged in, then show Insert and Delete Coverage tabs in WCS tab
            $rootScope.$watch("adminStateInformation.loggedIn", (obj:any) => {
                if ($rootScope.adminStateInformation.loggedIn == true) {

                    let roles = $rootScope.adminStateInformation.roles;
                    
                    // petascope admin logged in
                    if (AdminService.hasRole(roles, AdminService.PRIV_OWS_WCS_INSERT_COV)) {
                        $scope.wcsInsertCoverageTab.disabled = false;
                    }
                    if (AdminService.hasRole(roles, AdminService.PRIV_OWS_WCS_DELETE_COV)) {
                        $scope.wcsDeleteCoverageTab.disabled = false;
                    }
                } else {
                    // petascope admin logged out
                    $scope.wcsInsertCoverageTab.disabled = true;
                    $scope.wcsDeleteCoverageTab.disabled = true;
                }

                $rootScope.wcsReloadServerCapabilities = true;
            });

            $scope.tabs = [$scope.wcsGetCapabilitiesTab, $scope.wcsDescribeCoverageTab, $scope.wcsGetCoverageTab, $scope.wcsProcessCoverageTab, $scope.wcsDeleteCoverageTab, $scope.wcsInsertCoverageTab];

            // NOTE: must initialize wcsStateInformation first or watcher for ServerCapabilities in GetCapabilities
            // from DescribeCoverage, GetCoverage controllers will not work and return null.
            $scope.wcsStateInformation = {
                serverCapabilities: null,                
                selectedCoverageDescription: null,
                selectedGetCoverageId: null
            };

            // When click on the coverageId in the table of GetCapabilities tab,
            // it will change to DescribeCoverage tab and get metadata for this coverageId.
            $scope.describeCoverage = function(coverageId:string) {
                $scope.wcsDescribeCoverageTab.active = true;
                $rootScope.wcsSelectedGetCoverageId = coverageId;
            };

        }

        private initializeTabs($scope:WCSMainControllerScope) {
            $scope.wcsGetCapabilitiesTab = {
                heading: "GetCapabilities",
                view: "get_capabilities",
                active: true,
                disabled: false
            };

            $scope.wcsDescribeCoverageTab = {
                heading: "DescribeCoverage",
                view: "describe_coverage",
                active: false,
                disabled: false
            };

            $scope.wcsGetCoverageTab = {
                heading: "GetCoverage",
                view: "get_coverage",
                active: false,
                disabled: false
            };

            $scope.wcsProcessCoverageTab = {
                heading: "ProcessCoverages",
                view: "process_coverages",
                active: false,
                disabled: false
            };

            $scope.wcsDeleteCoverageTab = {
                heading: "DeleteCoverage",
                view: "delete_coverage",
                active: false,
                disabled: false
            };

            $scope.wcsInsertCoverageTab = {
                heading: "InsertCoverage",
                view: "insert_coverage",
                active: false,
                disabled: false
            };            
        }

        private resetState() {
            this.$scope.wcsDescribeCoverageTab.disabled = false;
            this.$scope.wcsGetCoverageTab.disabled = false;
            this.$scope.wcsProcessCoverageTab.disabled = false;
            this.$scope.wcsDeleteCoverageTab.disabled = false;
            this.$scope.wcsInsertCoverageTab.disabled = false;
        }

        private static isProcessCoverageEnabled(serverCapabilities:wcs.Capabilities) {
            var processExtensionUri = rasdaman.Constants.PROCESSING_EXT_URI;

            return serverCapabilities.serviceIdentification.profile.indexOf(processExtensionUri) != -1;
        }

        private static isCoverageTransactionEnabled(serverCapabilities:wcs.Capabilities) {
            var transactionExtensionUri = rasdaman.Constants.TRANSACTION_EXT_URI;

            return serverCapabilities.serviceIdentification.profile.indexOf(transactionExtensionUri) != -1;
        }
    }

    export interface WCSMainControllerScope extends angular.IScope {
        wcsStateInformation: {
            serverCapabilities:wcs.Capabilities,            
            selectedCoverageDescription:wcs.CoverageDescription,
            selectedGetCoverageId:string
        };

        tabs:TabState[];
        wcsGetCapabilitiesTab:TabState;
        wcsDescribeCoverageTab:TabState;
        wcsGetCoverageTab:TabState;
        wcsProcessCoverageTab:TabState;
        wcsInsertCoverageTab:TabState;
        wcsDeleteCoverageTab:TabState;

        //Implement a better way to navigate between tabs
        describeCoverage(coverageId:string);
    }

    interface TabState {
        heading:string;
        view:string;
        active:boolean;
        disabled:boolean;
    }    
}
