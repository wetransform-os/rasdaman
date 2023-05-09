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
///<reference path="../../../models/wms_model/wms/GetCapabilities.ts"/>
///<reference path="../../wms_component/WMSService.ts"/>
///<reference path="../../web_world_wind/WebWorldWindService.ts"/>
///<reference path="../../main/WMSMainController.ts"/>
///<reference path="../../../_all.ts"/>
///<reference path="../settings/SettingsService.ts"/>

module rasdaman {
    export class WMSGetCapabilitiesController {

        public static $inject = [
            "$window",
            "$rootScope",
            "$scope",
            "$log",
            "rasdaman.WMSSettingsService",       
            "rasdaman.WMSService",                 
            "Notification",
            "rasdaman.ErrorHandlingService",
            "rasdaman.WebWorldWindService"
        ];

        // Store the extents of all WMS layers in WGS84 BBOX
        public coveragesExtents:wms.CoverageExtent[] = [];        

        public constructor(private $window,
                           private $rootScope:angular.IRootScopeService,
                           private $scope:WMSCapabilitiesControllerScope,
                           private $log:angular.ILogService,
                           private settings:rasdaman.WMSSettingsService,                           
                           private wmsService:rasdaman.WMSService,                           
                           private alertService:any,
                           private errorHandlingService:ErrorHandlingService,
                           private webWorldWindService:rasdaman.WebWorldWindService,
                           private adminService:rasdaman.AdminService) {

            $scope.totalCoverages = 0;        
            // NOTE: human-readable numbers, e.g. 20 GB                    
            $scope.totalCoverageSizeInBytes = "";
            $scope.totalLocalCoverageSizeInBytes = "";
            $scope.totalRemoteCoverageSizeInBytes = "";

            $scope.isAvailableLayersOpen = false;            
            $scope.isServiceIdentificationOpen = false;
            $scope.isServiceProviderOpen = false;
            $scope.isCapabilitiesDocumentOpen = false;                               
            // Only display 10 rows in a smart table's page
            $scope.rowPerPageSmartTable = 10;

            $scope.wmsServerEndpoint = settings.wmsEndpoint;
            // To init the Globe on this canvas           
            let canvasId = "wmsCanvasGetCapabilities";          
            // to know which page are selected
            let currentPageNumber = 1;

            $scope.displayLayersDropdownItems = [{"name": "Display all layers", "value": ""},
                                                 {"name": "Display local layers", "value": "local"},
                                                 {"name": "Display remote layers", "value": "remote"}
                                                ];
            
            $scope.display = true;
            $scope.showAllFootprints = {isChecked: false};

            // When petascope admin user logged in, show the blacklist / whitelist buttons
            $rootScope.$on("adminStateInformation.loggedIn", (event, newValue:boolean) => {
                if (newValue) {
                    // Admin logged in
                    $scope.adminUserLoggedIn = true;
                } else {
                    // Admin logged out
                    $scope.adminUserLoggedIn = false;
                }

            });

            // From the WMS's EX_GeographicBoundingBox
            // NOTE: not like WCS, all layers can be display on the globe as they are geo-referenced.            
            $scope.initCheckboxesForLayerNames = () => {
                // all layers
                let layerArray = $scope.capabilities.layers;
                for (let i = 0; i < layerArray.length; i++) {                        
                    layerArray[i].displayFootprint = false;
                }                     
            }

            // Return a layer object by a name
            $scope.getLayerByName = (layerName:string):wms.Layer => {
                let layerArray = $scope.capabilities.layers;
                for (let i = 0; i < layerArray.length; i++) {                        
                    if (layerArray[i].name == layerName) {
                        return layerArray[i];
                    }
                }

                return null;
            }


            // NOTE: When filtering rows on smart table (broadcasted by SmartTableGetFilteredRows.ts) -> recalculate the total layers and their size from the filtered rows
            $scope.$on("filteredRowsEventWMS", (event, obj:any) => {

                if ($window.wmsGetCapabilitiesFilteredRows != null) {                            
                    let filteredRows = $window.wmsGetCapabilitiesFilteredRows;

                    $scope.totalCoverages = filteredRows.length;
                    let totalCoverageSizeInBytesTmp:number = 0;
                    let totalLocalCoverageSizeInBytesTmp:number = 0;
                    let totalRemoteCoverageSizeInBytesTmp:number = 0;

                    for (let i = 0; i < filteredRows.length; i++) {
                        let obj = filteredRows[i];

                        let metadata:ows.CustomizedMetadata = obj["customizedMetadata"];
                        if (metadata != null) {
                            totalLocalCoverageSizeInBytesTmp += metadata.localCoverageSizeInBytes;
                            totalRemoteCoverageSizeInBytesTmp += metadata.remoteCoverageSizeInBytes;

                            let sizeInBytesTmp:number = metadata.localCoverageSizeInBytes > 0 
                                                                ? metadata.localCoverageSizeInBytes 
                                                                : metadata.remoteCoverageSizeInBytes;
                            totalCoverageSizeInBytesTmp += sizeInBytesTmp;
                        }
                    }

                    // Finally, make these numbers human-readable
                    $scope.totalCoverageSizeInBytes = ows.CustomizedMetadata.convertNumberOfBytesToHumanReadable(totalCoverageSizeInBytesTmp);
                    $scope.totalLocalCoverageSizeInBytes = ows.CustomizedMetadata.convertNumberOfBytesToHumanReadable(totalLocalCoverageSizeInBytesTmp);
                    $scope.totalRemoteCoverageSizeInBytes = ows.CustomizedMetadata.convertNumberOfBytesToHumanReadable(totalRemoteCoverageSizeInBytesTmp);
                }
            });            

            // Load all layers's extents on globe
            $scope.displayAllFootprintsOnGlobe = (status:boolean) => {
                // Array of coverageExtents belong to WMS layers                
                if (status == true) {

                    // Get filtered rows from smart table
                    let filteredRows = $window.wmsGetCapabilitiesFilteredRows;

                    $scope.hideAllFootprintsOnGlobe();

                    for (let i = 0; i < filteredRows.length; i++) {
                        let obj = filteredRows[i];
                        let layerName = obj["name"];
                        // load all unloaded footprints from all pages on globe                    
                        for (let j = 0; j < $scope.capabilities.layers.length; j++) {
                            let coverageExtent = $scope.capabilities.layers[j].coverageExtent;
                            let coverageId = coverageExtent.coverageId;
                            if (layerName == coverageId) {
                                // checkbox is checked
                                $scope.capabilities.layers[j].displayFootprint = true;
                                webWorldWindService.showCoverageExtentOnGlobe(canvasId, coverageId, coverageExtent, false);
                            }
                        }
                    }
                } else {
                    // unload all loaded footprints from all pages on globe
                    $scope.hideAllFootprintsOnGlobe();
                }
            }

            // Unload all layer's extents on globe
            $scope.hideAllFootprintsOnGlobe = () => {
                if ($scope.capabilities != null) {
                    for (let i = 0; i < $scope.capabilities.layers.length; i++) {
                        let layer:any = $scope.capabilities.layers[i];
                        // checkbox is unchecked
                        layer.displayFootprint = false;
                        webWorldWindService.hideCoverageExtentOnGlobe(canvasId, layer.name);
                        
                    }
                }
            }

            // Handle click on checkbox in smart table
            $scope.showHideFootprintOnGlobe = (layerName) => {
                let layerArray = $scope.capabilities.layers;
                for (let i = 0; i < layerArray.length; i++) {      
                    let layer:wms.Layer = layerArray[i];
                    if (layer.name == layerName) {
                        if (layer.displayFootprint == true) {
                            webWorldWindService.showCoverageExtentOnGlobe(canvasId, layerName, layer.coverageExtent, false);
                        } else {
                            webWorldWindService.hideCoverageExtentOnGlobe(canvasId, layerName);
                        }

                        break;
                    }
                }       
            }

            // If a layer is checked as blacklist, no one, except petascope admin user can see it from GetCapabilities
            $scope.handleBlackListOneLayer = (layerName:string) => {
                                
                let status = $scope.getLayerByName(layerName).customizedMetadata.isBlackedList;
                if (status == true) {
                    // layer is added to blacklist

                    this.wmsService.blackListOneLayer(layerName).then(
                        (...args:any[]) => {
                            this.alertService.success("Blacklisted layer <b>" + layerName + "</b>");
                        }, (...args:any[]) => {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {

                        });

                } else {
                    // layer is removed from blacklist (whitelisted)
                    
                    this.wmsService.whiteListOneLayer(layerName).then(
                        (...args:any[]) => {
                            this.alertService.success("Whitelisted layer <b>" + layerName + "</b>");
                        }, (...args:any[]) => {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {

                        });
                }
            }

            // Handle black list all layers button
            $scope.handleBlackListAllLayers = () => {

                this.wmsService.blackListAllLayers().then(
                    (...args:any[]) => {
                        this.alertService.success("Blacklisted <b>all layers</b>");

                        // Check all checkboxes in blacklist column
                        for (let i = 0; i < $scope.capabilities.layers.length; i++) {
                            $scope.capabilities.layers[i].customizedMetadata.isBlackedList = true;
                        }
                    }, (...args:any[]) => {
                        this.errorHandlingService.handleError(args);
                        this.$log.error(args);
                    }).finally(function () {

                    });
            }

            // Handle white list all layers button
            $scope.handleWhiteListAllLayers = () => {

                this.wmsService.whiteListAllLayers().then(
                    (...args:any[]) => {
                        this.alertService.success("Whitelisted <b>all layers</b>");

                        // Uncheck all checkboxes in blacklist column
                        for (let i = 0; i < $scope.capabilities.layers.length; i++) {
                            $scope.capabilities.layers[i].customizedMetadata.isBlackedList = false;
                        }
                    }, (...args:any[]) => {
                        this.errorHandlingService.handleError(args);
                        this.$log.error(args);
                    }).finally(function () {

                    });                    
            }

            // NOTE: When DescribeCoverageController broadcasts message when a coverage id is renamed -> do some updatings
            $rootScope.$on("renamedCoverageId", (event, tupleObj:any) => {
                if (tupleObj != null) {
                    let oldCoverageId:string = tupleObj.oldCoverageId;
                    let newCoverageId:string = tupleObj.newCoverageId;

                    for (let i = 0; i < this.coveragesExtents.length; i++) {
                        if (this.coveragesExtents[i].coverageId == oldCoverageId) {
                            this.coveragesExtents[i].coverageId = newCoverageId;
                            $scope.capabilities.layers[i].name = newCoverageId;
                            break;
                        }
                    }

                    // NOTE: also update any pyramid member coverage ids
                    for (let i = 0; i < $scope.capabilities.layers.length; i++) {
                        let layer:wms.Layer = $scope.capabilities.layers[i];
                        if (layer.pyramidCoverageMembers != null) {
                            for (let j = 0; j < layer.pyramidCoverageMembers.length; j++) {
                                if (layer.pyramidCoverageMembers[j].coverageId == oldCoverageId) {
                                    layer.pyramidCoverageMembers[j].coverageId = newCoverageId;
                                }
                            }
                        }
                    }

                    webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents = this.coveragesExtents;
                    webWorldWindService.updateSurfacePolygonCoverageId(canvasId, oldCoverageId, newCoverageId);
                }
            });
            
            // NOTE: When DeleteCoverageController broadcasts message -> do some cleanings
            $rootScope.$on("deletedCoverageId", (event, coverageIdToDelete:string) => {
                if (coverageIdToDelete != null) {
                    $scope.cleanAfterDeletingLayer(coverageIdToDelete);
                }
            });            

            // NOTE: When DeleteLayerController broadcasts message -> do some cleanings
            $rootScope.$on("deletedWMSLayerName", (event, layerNameToDelete:string) => {
                if (layerNameToDelete != null) {
                    $scope.cleanAfterDeletingLayer(layerNameToDelete);                   
                }
            });

            $scope.cleanAfterDeletingLayer = (layerNameToDelete:string) => {
                try {
                    let layerToDelete:wms.Layer = null;
                    let layerNameToDeleteIndex = -1;
                    let layers:wms.Layer[] = $scope.capabilities.layers;
                    for (let i = 0; i < layers.length; i++) {
                        if (layers[i].name == layerNameToDelete) {
                            layerNameToDeleteIndex = i;
                            layerToDelete = layers[i];
                            break;                            
                        }
                    }

                    if (layerNameToDeleteIndex != -1) {
                        // remove the deleted layer from the cached layes
                        $scope.wmsStateInformation.serverCapabilities.layers.splice(layerNameToDeleteIndex, 1);
                        let coverageExtent = this.coveragesExtents[layerNameToDelete];
                        // and delete it from cached coverageExtents array
                        this.coveragesExtents.splice(layerNameToDeleteIndex, 1);                            
                        webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents = this.coveragesExtents;

                        // Then recalculate the total layers and their sizes after the deleted layer is removed
                        $scope.wmsStateInformation.serverCapabilities.recalculateTotalAndSizes(layerToDelete);
                        // And hide its extent on webworldwind if it is shown before
                        webWorldWindService.hideCoverageExtentOnGlobe(canvasId, layerNameToDelete);
                    }

                    // Then, check if this deleted coverage exists in pyramid members of any layer and remove it from the pyramids
                    for (let i = 0; i < layers.length; i++) {
                        let layer:wms.Layer = layers[i];
                        let pyramidCoverageMembers:wms.PyramidCoverageMember[] = layer.pyramidCoverageMembers;

                        if (pyramidCoverageMembers != null) {
                            for (let j = 0; j < pyramidCoverageMembers.length; j++) {
                                if (pyramidCoverageMembers[j].coverageId == layerNameToDelete) {
                                    pyramidCoverageMembers.splice(j, 1);
                                    break;
                                }
                            }
                        }
                    }

                } catch(error) {
                    errorHandlingService.handleError(error);
                    console.log("Error in WMS GetCapabilitiesController");
                    console.log(error);
                } finally {

                }
            }

            // When WMS CreateLayer is called sucessfully, it should reload the new capabilities      
            // NOTE: using $broadcast in WCSMainController and $on here will not make this method invoked when loading web page      
            $rootScope.$watch("wmsReloadServerCapabilities", (obj:any) => {
                if (obj == true) {                    
                    $scope.getServerCapabilities();

                    // NOTE: Mark as null, so when it is set to true in some places, this watch will be invoked
                    $rootScope.wmsReloadServerCapabilities = null;
                }
            });  
            
            // Handle the click event on GetCapabilities button
            $scope.handleGetServerCapabilities = () => {              
                $scope.getServerCapabilities();
                
                $scope.showAllFootprints.isChecked = false;
            }
          
            // Handle server capabilities request
            $scope.getServerCapabilities = (...args: any[]) => {                
                if (!$scope.wmsServerEndpoint) {
                    alertService.error("The entered WMS endpoint is invalid.");
                    return;
                }

                // Hide any layers' footprints which are shown before
                $scope.hideAllFootprintsOnGlobe();      

                // Update settings:
                settings.wmsEndpoint = $scope.wmsServerEndpoint;
                // Reload the full WMS URL
                settings.setWMSFullEndPoint();   

                // Load new coverage extents
                this.coveragesExtents = [];     

                //Create capabilities request
                let capabilitiesRequest = new wms.GetCapabilities();

                $scope.generatedGETURL = settings.wmsFullEndpoint + "&" + capabilitiesRequest.toKVP();
    
                wmsService.getServerCapabilities(capabilitiesRequest)
                    .then((response:rasdaman.common.Response<wms.Capabilities>) => {
                            // Success handler
                            // This is output from GetCapabilities request in XML
                            $scope.capabilitiesDocument = response.document;                            
                            // This is the parsed object from XML output by wmsService
                            $scope.capabilities = response.value;

                            // If a GetCapabilities succeeds, all dropdown boxes should open
                            $scope.isAvailableLayersOpen = true;
                            $scope.isServiceIdentificationOpen = true;
                            $scope.isServiceProviderOpen = true;                            
                                                        
                            // NOTE: WMS already has the EX_GeographicBoundingBox element of each layer from GetCapabilities request.
                            // But, WMS still needs to convert the EX_GeographicBoundingBox the same outcome (CoverageExtent) to be displayable on globe.                            
                            $scope.initCheckboxesForLayerNames();
                            
                            for (let i = 0; i < $scope.capabilities.layers.length; i++) {
                                this.coveragesExtents.push($scope.capabilities.layers[i].coverageExtent);
                            }
                            webWorldWindService.wmsGetCapabilitiesWGS84CoverageExtents = this.coveragesExtents;

                            webWorldWindService.initWebWorldWind(canvasId);   
                            
                            // NOTE: loaded and $broadcast to be used with $on in other controllers, such as: WMS CreateLayer controller
                            $rootScope.$broadcast("wmsReloadServerCapabilitiesDone", true);
                        },
                        (...args:any[]) => {
                            //Error handler
                            $scope.capabilitiesDocument = null;
                            $scope.capabilities = null;

                            // If a GetCapabilities failed, all dropdown boxes should close
                            $scope.isAvailableLayersOpen = false;
                            $scope.isServiceIdentificationOpen = false;
                            $scope.isServiceProviderOpen = false;

                            errorHandlingService.handleError(args);
                            $log.error(args);
                        })
                    .finally(() => {
                        $scope.wmsStateInformation.serverCapabilities = $scope.capabilities;
                    });
            };            
        }
    }

    interface WMSCapabilitiesControllerScope extends rasdaman.WMSMainControllerScope {
        wmsServerEndpoint:string;
        capabilitiesDocument:rasdaman.common.ResponseDocument;
        capabilities:wms.Capabilities;                            
        rowPerPageSmartTable:number;
        showAllFootprints:any;
        
        isAvailableLayersOpen:boolean;        
        isServiceIdentificationOpen:boolean;
        isServiceProviderOpen:boolean;
        isCapabilitiesDocumentOpen:boolean;

        adminUserLoggedIn:boolean;

        generatedGETURL:string;

        // return a correspondent layer by a name
        getLayerByName(layerName:string):wms.Layer;
        
        // Show/Hide the checked layer's extent on globe of current page
        showHideFootprintOnGlobe(layerName:string):void;

        // Load all the layers's extents on globe from all pages
        displayAllFootprints(status:boolean):void;

        // Blacklist / whitelist a specific layer when admin changes on the checkbox for it
        handleBlackListOneLayer(layerName:string):void;

        // Handle blacklist / whiltelist all layers buttons' events
        handleBlackListAllLayers():void;
        handleWhiteListAllLayers():void;

        getServerCapabilities():void;
	
        display:boolean; 
        
        initCheckboxesForLayerNames():void;
        displayAllFootprintsOnGlobe(status:boolean):void;
        pageChanged(newPage: any):void;

        cleanAfterDeletingLayer(layerNameToDelete: string):void;
    }
}
