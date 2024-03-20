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


///<reference path="../../../../assets/typings/tsd.d.ts"/>
///<reference path="../../main/WCSMainController.ts"/>
///<reference path="../WCSService.ts"/>

module rasdaman {
    export class WCSDeleteCoverageController {

        public static $inject = [
            "$rootScope",
            "$scope",
            "$log",
            "Notification",
            "rasdaman.WCSService",
            "rasdaman.WCSSettingsService",
            "rasdaman.ErrorHandlingService"
        ];

        public constructor(
                           private $rootScope:angular.IRootScopeService,
                           private $scope:WCSDeleteCoverageControllerScope,
                           private $log:angular.ILogService,
                           private alertService:any,
                           private wcsService:rasdaman.WCSService,
                           private settings:rasdaman.WCSSettingsService,
                           private errorHandlingService:ErrorHandlingService) {

            function getCoverageIndexToDelete(coverageId:string):number {
                if ($scope.wcsStateInformation.serverCapabilities) {
                    var coverageSummaries = $scope.wcsStateInformation.serverCapabilities.contents.coverageSummaries;
                    for (var i = 0; i < coverageSummaries.length; ++i) {
                        if (coverageSummaries[i].coverageId == coverageId) {
                            return i;
                        }
                    }
                }

                return -1;
            }

            // NOTE: When DescribeCoverageController broadcasts message when a coverage id is renamed -> do some updatings
            $rootScope.$on("renamedCoverageId", (event, tupleObj:any) => {
                if (tupleObj != null) {
                    let oldCoverageId:string = tupleObj.oldCoverageId;
                    let newCoverageId:string = tupleObj.newCoverageId;

                    for (let i = 0; i < $scope.availableCoverageIds.length; i++) {
                        if ($scope.availableCoverageIds[i] == oldCoverageId) {
                            $scope.availableCoverageIds[i] = newCoverageId;
                            break;
                        }
                    }
                }
            });            

            $scope.$watch("coverageIdToDelete", (coverageIdToDelete:string) => {
                let foundIndex = getCoverageIndexToDelete(coverageIdToDelete);
                $scope.isCoverageIdValid = foundIndex == -1 ? false : true;      
                
                if (foundIndex != -1) {
                    $scope.generatedGETURL = settings.wcsFullEndpoint + "&REQUEST=DeleteCoverage&COVERAGEID=" + coverageIdToDelete;
                }
            });

            $scope.$watch("wcsStateInformation.serverCapabilities", (capabilities:wcs.Capabilities) => {
                if (capabilities) {
                    $scope.availableCoverageIds = [];
                    capabilities.contents.coverageSummaries.forEach((coverageSummary:wcs.CoverageSummary) => {
                        $scope.availableCoverageIds.push(coverageSummary.coverageId);
                    });
                }
            });

            $scope.deleteCoverage = () => {
                if ($scope.requestInProgress) {
                    this.alertService.error("Cannot delete a coverage while another delete request is in progress.");
                } else if (getCoverageIndexToDelete($scope.coverageIdToDelete) == -1) {
                    this.alertService.error("The coverage <b>" + $scope.coverageIdToDelete + "</b> does not exist to delete.");
                } else {
                    $scope.requestInProgress = true;

                    this.wcsService.deleteCoverage($scope.coverageIdToDelete).then(
                        (...args:any[])=> {
                            this.alertService.success("Deleted coverage <b>" + $scope.coverageIdToDelete + "</b>");
                            // NOTE: In WCS and WMS GetCapabilitiesController will act accordingly after this coverage is deleted
                            // - $broadcast is used when this event is listen in MULTIPLE children controllers
                            // - $watch is used in a child controller, when $rootScope.value changed, and it does something, before setting the $rootScope.value = null (!Important)
                            //   so next time when $rootScope.value changed again to e.g. true then the $watch will be invoked.
                            $rootScope.$broadcast("deletedCoverageId", $scope.coverageIdToDelete);
                            $scope.availableCoverageIds = $scope.availableCoverageIds.filter(coverageId => coverageId !== $scope.coverageIdToDelete);

                        }, (...args:any[])=> {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {
                
                            $scope.requestInProgress = false;
                    });
                }
            };

            // string
            $scope.coverageIdToDelete = null;

            $scope.requestInProgress = false;
            $scope.isCoverageIdValid = false;
        }
    }

    interface WCSDeleteCoverageControllerScope extends WCSMainControllerScope {               
        coverageIdToDelete:string;

        availableCoverageIds:string[];
        requestInProgress:boolean;
        isCoverageIdValid:boolean;

        generatedGETURL:string;

        deleteCoverage():void;
    }
}
