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
    export class WMSDeleteLayerController {

        public static $inject = [
            "$rootScope",
            "$scope",
            "$log",
            "Notification",
            "rasdaman.WMSService",
            "rasdaman.ErrorHandlingService"
        ];

        public constructor(
                           private $rootScope:angular.IRootScopeService,
                           private $scope:WMSDeleteLayerControllerScope,
                           private $log:angular.ILogService,
                           private alertService:any,
                           private wmsService:rasdaman.WMSService,
                           private errorHandlingService:ErrorHandlingService) {
                        

            function getLayerIndexToDelete(layerName:string):number {
                if ($scope.wmsStateInformation.serverCapabilities) {
                    let layers = $scope.wmsStateInformation.serverCapabilities.layers;
                    for (let i = 0; i < layers.length; i++) {
                        let layerNameTmp = layers[i].name;
                        if (layerNameTmp.trim() == layerName.trim()) {
                            return i;
                        }
                    }
                }

                return -1;
            }

            // NOTE: When DescribeCoverageController broadcasts message when a coverage id is renamed -> do some updatings
            $rootScope.$watch("renameCoverageId", (tupleObj:any) => {
                if (tupleObj != null) {
                    let oldCoverageId:string = tupleObj.oldCoverageId;
                    let newCoverageId:string = tupleObj.newCoverageId;

                    for (let i = 0; i < $scope.availableLayerNames.length; i++) {
                        if ($scope.availableLayerNames[i] == oldCoverageId) {
                            $scope.availableLayerNames[i] = newCoverageId;
                            break;
                        }
                    }
                }
                
            });             

            $scope.$watch("layerNameToDelete", (newValue:string, oldValue:string)=> {
                let foundIndex = getLayerIndexToDelete(newValue);
                $scope.isLayerNameValid = foundIndex == -1 ? false : true;  
            });

            $scope.$watch("wmsStateInformation.serverCapabilities", (capabilities:wms.Capabilities)=> {
                if (capabilities) {
                    $scope.availableLayerNames = [];
                    capabilities.layers.forEach((layer:wms.Layer)=> {
                        $scope.availableLayerNames.push(layer.name);
                    });
                }
            });

            $scope.deleteLayer = ()=> {
                if ($scope.requestInProgress) {
                    this.alertService.error("Cannot delete a layer while another delete request is in progress.");
                } else if (getLayerIndexToDelete($scope.layerNameToDelete) == -1) {
                    this.alertService.error("The layer <b>" + $scope.layerNameToDelete + "</b> does not exist to delete.");
                } else {
                    $scope.requestInProgress = true;

                    this.wmsService.deleteLayer($scope.layerNameToDelete).then(
                        (...args:any[])=> {
                            this.alertService.success("Deleted layer <b>" + $scope.layerNameToDelete + "</b>");

                            // // Reload GetCapabilities in children controllers
                            // // reload WMS (NOTE: this takes long time to send a new WMS GetCapabilities and rebuild everything) -> not used (!)
                            // $rootScope.$broadcast("reloadWMSServerCapabilities", true);      

                            // Ask WMS GetCapabilitiesController to remove the deleted layer's extent on the globe if it showed before
                            // and recalculate the sizes of available layers
                            $rootScope.removeDeletedWMSLayerName = $scope.layerNameToDelete;

                            $scope.availableLayerNames = $scope.availableLayerNames.filter(layerName => layerName != $scope.layerNameToDelete);

                        }, (...args:any[])=> {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {
                            $scope.requestInProgress = false;
                    });
                }
            };

            // string
            $scope.layerNameToDelete = null;

            $scope.requestInProgress = false;
            $scope.isLayerNameValid = false;            

        }
    }

    interface WMSDeleteLayerControllerScope extends WCSMainControllerScope {        
        layerNameToDelete:string;

        availableLayerNames:string[];
        requestInProgress:boolean;
        isLayerNameValid:boolean;        

        deleteLayer():void;
    }
}
