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
///<reference path="../../main/WMSMainController.ts"/>
///<reference path="../../wms_component/WMSService.ts"/>

module rasdaman {
    export class WMSCreateLayerController {

        public static $inject = [
            "$rootScope",
            "$scope",
            "$log",
            "Notification",
            "rasdaman.WMSService",
            "rasdaman.WMSSettingsService",            
            "rasdaman.ErrorHandlingService",
            "rasdaman.WebWorldWindService"
        ];

        public constructor(
                           private $rootScope:angular.IRootScopeService,
                           private $scope:WMSCreateLayerControllerScope,
                           private $log:angular.ILogService,
                           private alertService:any,
                           private wmsService:rasdaman.WMSService,
                           private settings:rasdaman.WMSSettingsService,
                           private errorHandlingService:ErrorHandlingService,
                           private webWorldWindService:rasdaman.WebWorldWindService) {
                        

            function getLayerIndexToCreate(layerName:string):number {
                if ($scope.inactivatedGeoReferencedCoverageIds != null) {
                    let coverageIds:string[] = $scope.inactivatedGeoReferencedCoverageIds;
                    for (let i = 0; i < coverageIds.length; i++) {
                        let layerNameTmp = coverageIds[i];
                        if (layerNameTmp.trim() == layerName.trim()) {
                            return i;
                        }
                    }
                }

                return -1;
            }

            // Only display geo-referenced coverage ids which are not activated as WMS layers yet
            // NOTE: $on is used with $broadcast from WCS GetCapabilities Controller, because $rootScope.variable = true doesn't work with $watch
            // when clicking on WCS GetCapabilities button multiple times
            $rootScope.$on("wcsReloadServerCapabilitiesDone", (event, args) => {                  
                $scope.initInactivatedGeoreferencedCoverageIds();
            });

            // NOTE: $on is used with $broadcast from WMS GetCapabilities Controller, because $rootScope.variable = true doesn't work with $watch
            // when clicking on WCS GetCapabilities button multiple times
            $rootScope.$on("wmsReloadedServerCapabilities", (event, args) => {                       
                $scope.initInactivatedGeoreferencedCoverageIds();                
            });    

            $scope.initInactivatedGeoreferencedCoverageIds = () => {
                $scope.inactivatedGeoReferencedCoverageIds = [];

                for (let i = 0; i < webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents.length; i++) {
                    let layerExists = false;
                    let geoReferencedCoverageId = webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents[i].coverageId;
                    for (let j = 0; j < webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents.length; j++) {
                        let layerName = webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents[j].coverageId;

                        if (geoReferencedCoverageId == layerName) {
                            layerExists = true;
                            break;
                        }
                    }

                    if (layerExists == false) {
                        if (!$scope.inactivatedGeoReferencedCoverageIds.includes(geoReferencedCoverageId)) {
                            $scope.inactivatedGeoReferencedCoverageIds.push(geoReferencedCoverageId);
                        }
                    }
                }
            }            

            // NOTE: When DeleteCoverageController broadcasts message -> do some cleanings
            $rootScope.$on("deletedCoverageId", (event, coverageIdToDelete:string) => {
                if (coverageIdToDelete != null) {
                    for (let i = 0; i < $scope.inactivatedGeoReferencedCoverageIds.length; i++) {
                        if ($scope.inactivatedGeoReferencedCoverageIds[i] == coverageIdToDelete) {
                            $scope.inactivatedGeoReferencedCoverageIds.splice(i, 1);
                            break;
                        }
                    }
                }
            });
            
            // NOTE: When DeleteLayerController broadcasts message -> add this deleted WMS layer name to the available coverage Ids list
            $rootScope.$on("deletedWMSLayerName", (event, layerNameToDelete:string) => {
                if (layerNameToDelete != null) {
                    $scope.inactivatedGeoReferencedCoverageIds.push(layerNameToDelete);
                }
            });

            // NOTE: When DescribeCoverageController broadcasts message when a coverage id is renamed -> do some updatings
            $rootScope.$on("renamedCoverageId", (event, tupleObj:any) => {
                if (tupleObj != null) {
                    let oldCoverageId:string = tupleObj.oldCoverageId;
                    let newCoverageId:string = tupleObj.newCoverageId;

                    for (let i = 0; i < $scope.inactivatedGeoReferencedCoverageIds.length; i++) {
                        if ($scope.inactivatedGeoReferencedCoverageIds[i] == oldCoverageId) {
                            $scope.inactivatedGeoReferencedCoverageIds[i] = newCoverageId;
                            break;
                        }
                    }
                }
                
            });             

            $scope.$watch("layerNameToCreate", (newValue:string) => {
                let foundIndex = getLayerIndexToCreate(newValue);
                $scope.isLayerNameValid = foundIndex == -1 ? false : true;  
                if (foundIndex != -1) {
                    $scope.generatedGETURL = settings.adminEndpoint + "/layer/activate?COVERAGEID=" + newValue;
                }
            });            


            $scope.createLayer = () => {
                if ($scope.requestInProgress) {
                    this.alertService.error("Cannot Create a layer while another Create request is in progress.");
                } else if (getLayerIndexToCreate($scope.layerNameToCreate) == -1) {
                    for (let i = 0; i < webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents.length; i++) {
                        if (webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents[i].coverageId == $scope.layerNameToCreate) {
                            this.alertService.error("The layer <b>" + $scope.layerNameToCreate + "</b> already exists.");
                            return;
                        }
                    }

                    this.alertService.error("The coverage <b>" + $scope.layerNameToCreate + "</b> does not exist to create an associated WMS layer.");                                        
                } else {
                    $scope.requestInProgress = true;

                    this.wmsService.createLayer($scope.layerNameToCreate).then(
                        (...args:any[])=> {
                            this.alertService.success("Created layer <b>" + $scope.layerNameToCreate + "</b>");

                            // NOTE: This is required, because WMS GetCapabilties Controller needs to get the WGS84 BBox and the coverageSizeInBytes of the newly created layer
                            // but it doesn't have these information here here, so it needs to do full request WMS GetCapabilities (!)
                            $rootScope.wmsReloadServerCapabilities = true;

                            // The layer is created -> remove it from the list of avaliable geo-referenced coverage ids
                            $scope.inactivatedGeoReferencedCoverageIds = $scope.inactivatedGeoReferencedCoverageIds.filter(layerName => layerName != $scope.layerNameToCreate);

                        }, (...args:any[])=> {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {
                            $scope.requestInProgress = false;
                    });
                }
            };

            // string
            $scope.layerNameToCreate = null;

            $scope.requestInProgress = false;
            $scope.isLayerNameValid = false;            

        }
    }

    interface WMSCreateLayerControllerScope extends WCSMainControllerScope {        
        layerNameToCreate:string;
        generatedGETURL:string;

        // the list of geo-referenced coverage Ids but not activated as WMS layers yet
        inactivatedGeoReferencedCoverageIds:string[];
        requestInProgress:boolean;
        isLayerNameValid:boolean;        

        createLayer():void;
        initInactivatedGeoreferencedCoverageIds():void;
    }
}
