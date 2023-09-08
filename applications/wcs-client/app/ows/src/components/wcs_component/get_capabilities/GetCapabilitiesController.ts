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
///<reference path="../../../models/wcs_model/wcs/GetCapabilities.ts"/>
///<reference path="../../wcs_component/WCSService.ts"/>
///<reference path="../../web_world_wind/WebWorldWindService.ts"/>
///<reference path="../../main/WCSMainController.ts"/>
///<reference path="../../../_all.ts"/>
///<reference path="../settings/SettingsService.ts"/>

module rasdaman {
    export class WCSGetCapabilitiesController {

        public static $inject = [            
            "$window",
            "$scope",
            "$rootScope",
            "$log",
            "rasdaman.WCSService",
            "rasdaman.WCSSettingsService",            
            "Notification",
            "rasdaman.ErrorHandlingService",
            "rasdaman.WebWorldWindService"           
        ];

        // Store the extents of all geo-referenced coverages in WGS84 BBOX
        public coveragesExtents:wms.CoverageExtent[] = [];

        public constructor(private $window,
                           private $scope:WCSCapabilitiesControllerScope,
                           private $rootScope:angular.IRootScopeService,
                           private $log:angular.ILogService,                           
                           private wcsService:rasdaman.WCSService,
                           private settings:rasdaman.WCSSettingsService,                           
                           private alertService:any,
                           private errorHandlingService:ErrorHandlingService,
                           private webWorldWindService:rasdaman.WebWorldWindService
                           ) {

            $scope.totalCoverages = 0;        
            // NOTE: human-readable numbers, e.g. 20 GB                    
            $scope.totalCoverageSizeInBytes = "";
            $scope.totalLocalCoverageSizeInBytes = "";
            $scope.totalRemoteCoverageSizeInBytes = "";
                                           
            $scope.isAvailableCoveragesOpen = false;
            $scope.isCoveragesExtentsOpen = false;
            $scope.isServiceIdentificationOpen = false;
            $scope.isServiceProviderOpen = false;
            $scope.isCapabilitiesDocumentOpen = false;

            $scope.recalculateCoverageSizesFromFilteredRows = 0;

            $scope.displayCoveragesDropdownItems = [{"name": "Display all coverages", "value": ""},
                                                    {"name": "Display local coverages", "value": "local"},
                                                    {"name": "Display remote coverages", "value": "remote"}
                                                ];
            $scope.selectedDisplayCoveragesByTypeDropdown = "all";                               
                            

            $scope.showAllFootprints = {isChecked: false};

            // Only display 10 rows in a smart table's page
            $scope.rowPerPageSmartTable = 10;

            $scope.wcsServerEndpoint = settings.wcsEndpoint;
            // To init the Globe on this canvas           
            let canvasId = "wcsCanvasGetCapabilities";

            $scope.hasBlackWhiteListeCoverageRole = AdminService.hasRole($rootScope.userLoggedInRoles, AdminService.PRIV_OWS_WCS_BLACKWHITELIST_COV);

            // NOTE: not all coverages could be loaded as geo-referenced, only possible coverages will have checkboxes nearby coveargeId
            $scope.initCheckboxesForCoverageIds = () => {
                // all coverages
                let coverageSummaryArray = $scope.capabilities.contents.coverageSummaries;
                for (let i = 0; i < coverageSummaryArray.length; i++) {
                    // only geo-referenced coverages
                    for (let j = 0; j < this.coveragesExtents.length; j++) {
                        if (this.coveragesExtents[j].coverageId === coverageSummaryArray[i].coverageId) {
                            coverageSummaryArray[i].displayFootprint = false;
                            break;
                        }
                    }
                }                     
            }

            // Return a coverage's summary by coverageId
            $scope.getCoverageSummaryByCoverageId = (coverageId:string):wcs.CoverageSummary => {
                // all coverages
                let coverageSummaryArray = $scope.capabilities.contents.coverageSummaries;
                for (let i = 0; i < coverageSummaryArray.length; i++) {
                    if (coverageSummaryArray[i].coverageId == coverageId) {
                        return coverageSummaryArray[i];
                    }
                }
            }

            // NOTE: When filtering rows on smart table (broadcasted by SmartTableGetFilteredRows.ts) -> recalculate the total coverages and their size from the filtered rows
            $scope.$on("filteredRowsEventWCS", (event, obj:any) => {

                if ($window.wcsGetCapabilitiesFilteredRows != null) {                            
                    let filteredRows = $window.wcsGetCapabilitiesFilteredRows;

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

            // Load all coverages' extents on globe
            $scope.displayAllFootprintsOnGlobe = (status:boolean) => {                              

                if (status == true) {
                    // Get filtered rows from smart table
                    let filteredRows = $window.wcsGetCapabilitiesFilteredRows;
                    $scope.hideAllFootprintsOnGlobe();

                    for (let i = 0; i < filteredRows.length; i++) {
                        let obj = filteredRows[i];
                        let covId = obj["coverageId"];
                        // load all unloaded footprints from all pages on globe                    
                        for (let j = 0; j < this.coveragesExtents.length; j++) {                        
                            let coverageId = this.coveragesExtents[j].coverageId;                        
                            if (covId === coverageId) {
                                // checkbox is checked
                                $scope.getCoverageSummaryByCoverageId(coverageId).displayFootprint = true;

                                let coverageExtent:any = this.webWorldWindService.getCoveragesExtentByCoverageId(this.webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents, coverageId)
                                webWorldWindService.showCoverageExtentOnGlobe(canvasId, coverageId, coverageExtent, false);                                
                                break;
                            }                     
                        }                    
                    }
                } else {
                    // unload all loaded footprints from all pages on globe
                    $scope.hideAllFootprintsOnGlobe();
                }                
            }

            // Unload all coverages' extents on globe
            $scope.hideAllFootprintsOnGlobe = () => {
                // unload all loaded footprints from all pages on globe
                for (let i = 0; i < this.coveragesExtents.length; i++) {
                    let coverageId = this.coveragesExtents[i].coverageId;     
                    let obj:any = $scope.getCoverageSummaryByCoverageId(coverageId);

                    if (obj != null && obj.displayFootprint == true) {
                        // checkbox is unchecked
                        $scope.getCoverageSummaryByCoverageId(coverageId).displayFootprint = false;
                        webWorldWindService.hideCoverageExtentOnGlobe(canvasId, coverageId);
                    }                        
                }
            }

            // Handle click on checkbox in smart table
            $scope.showHideFootprintOnGlobe = (coverageId) => {

                // all coverages
                let coverageSummaryArray = $scope.capabilities.contents.coverageSummaries;
                for (let i = 0; i < coverageSummaryArray.length; i++) {
                    let coverageSummary:wcs.CoverageSummary = coverageSummaryArray[i];

                    // NOTE: coverageExtents contain a list of geo-referenced coverages, while coverageSummaries contain non-georeferenced coverages as well
                    if (coverageSummary.coverageId == coverageId) {                        
                        let coverageExtent:wms.CoverageExtent = null;
                        for (let j = 0; j < this.coveragesExtents.length; j++) {
                            coverageExtent = this.coveragesExtents[j];
                            if (coverageExtent.coverageId == coverageId) {
                                break;
                            }
                        }

                        if (coverageSummary.displayFootprint == true) {
                            webWorldWindService.showCoverageExtentOnGlobe(canvasId, coverageId, coverageExtent, false);
                        } else {
                            webWorldWindService.hideCoverageExtentOnGlobe(canvasId, coverageId);
                        }

                        break;
                    }
                }
    
            }            

            // If a coverage is checked as blacklist, no one, except petascope admin user can see it from GetCapabilities
            $scope.handleBlackListOneCoverage = (coverageId:string) => {
                let status = $scope.getCoverageSummaryByCoverageId(coverageId).customizedMetadata.isBlackedList;
                if (status == true) {
                    // coverage is added to blacklist

                    this.wcsService.blackListOneCoverage(coverageId).then(
                        (...args:any[]) => {
                            this.alertService.success("Blacklisted coverage <b>" + coverageId + "</b>");
                        }, (...args:any[]) => {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {

                        });

                } else {
                    // coverage is removed from blacklist (whitelisted)
                    
                    this.wcsService.whiteListOneCoverage(coverageId).then(
                        (...args:any[]) => {
                            this.alertService.success("Whitelisted coverage <b>" + coverageId + "</b>");
                        }, (...args:any[]) => {
                            this.errorHandlingService.handleError(args);
                            this.$log.error(args);
                        }).finally(function () {

                        });
                }
            }

            // Handle black list all coverages button
            $scope.handleBlackListAllCoverages = () => {

                this.wcsService.blackListAllCoverages().then(
                    (...args:any[]) => {
                        this.alertService.success("Blacklisted <b>all coverages</b>");

                        // Check all checkboxes in blacklist column
                        let coverageSummaryArray = $scope.capabilities.contents.coverageSummaries;
                        for (let i = 0; i < coverageSummaryArray.length; i++) {
                            coverageSummaryArray[i].customizedMetadata.isBlackedList = true;
                        }
                    }, (...args:any[]) => {
                        this.errorHandlingService.handleError(args);
                        this.$log.error(args);
                    }).finally(function () {

                    });
                
            }

            // Handle white list all coverages button
            $scope.handleWhiteListAllCoverages = () => {

                this.wcsService.whiteListAllCoverages().then(
                    (...args:any[]) => {
                        this.alertService.success("Whitelisted <b>all coverages</b>");

                        // Uncheck all checkboxes in blacklist column
                        let coverageSummaryArray = $scope.capabilities.contents.coverageSummaries;
                        for (let i = 0; i < coverageSummaryArray.length; i++) {
                            coverageSummaryArray[i].customizedMetadata.isBlackedList = false;
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
                            $scope.capabilities.contents.coverageSummaries[i].coverageId = newCoverageId;
                            this.coveragesExtents[i].coverageId = newCoverageId;
                            break;
                        }
                    }

                    webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents = this.coveragesExtents;
                    webWorldWindService.updateSurfacePolygonCoverageId(canvasId, oldCoverageId, newCoverageId);
                }
            });

            // NOTE: When DeleteCoverageController broadcasts message -> do some cleanings
            $rootScope.$on("deletedCoverageId", (event, coverageIdToDelete:string) => {
                if (coverageIdToDelete != null) {
                    try {
                        let coverageIdToDeleteIndex = -1;
                        let coverageToDeleteObj:wcs.CoverageSummary = null;
                        let coverages:wcs.CoverageSummary[] = $scope.capabilities.contents.coverageSummaries;
                        for (let i = 0; i < coverages.length; i++) {
                            if (coverages[i].coverageId == coverageIdToDelete) {
                                coverageIdToDeleteIndex = i;
                                coverageToDeleteObj = coverages[i];
                                break;                            
                            }
                        }

                        let coverageExtentToDeleteIndex = -1;
                        for (let i = 0; i < this.coveragesExtents.length; i++) {
                            if (this.coveragesExtents[i].coverageId == coverageIdToDelete) {
                                coverageExtentToDeleteIndex = i;
                                break;
                            }
                        }

                        if (coverageIdToDeleteIndex != -1) {
                            // remove the deleted coverage from the cached layes
                            $scope.capabilities.contents.coverageSummaries.splice(coverageIdToDeleteIndex, 1);
                            // and delete it from cached coverageExtents array
                            this.coveragesExtents.splice(coverageExtentToDeleteIndex, 1);
                            webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents = this.coveragesExtents;

                            // Then recalculate the total coverages and their sizes after the deleted coverage is removed
                            $scope.capabilities.contents.recalculateTotalAndSizes(coverageToDeleteObj);
                            // And hide its extent on webworldwind if it is shown before
                            webWorldWindService.hideCoverageExtentOnGlobe(canvasId, coverageIdToDelete);
                        }
                    } catch (error) {
                        errorHandlingService.handleError(error);
                        console.log("Error in WCS GetCapabilitiesController");
                        console.log(error);
                    } finally {
                    }
                }
            });       
     
            // When insertCoverage is called sucessfully, it should reload the new capabilities
            // NOTE: using $broadcast in WCSMainController and $on here will not make this method invoked when loading web page
            $rootScope.$watch("wcsReloadServerCapabilities", (obj:any) => {                

                console.log(obj);

                if (obj == true) {                    
                    $scope.getServerCapabilities();

                    // NOTE: Mark as false to trigger the event when admin user logged out and log in again
                    // $rootScope.wcsReloadServerCapabilities = null;
                }
            });

            /**
             * From WGS84BoundingBox elements, parse them to get xmin, ymin, xmax, ymax.
             */
            $scope.parseCoveragesExtents = () => {
                this.coveragesExtents = [];

                let coverageSummaries = $scope.capabilities.contents.coverageSummaries;
                coverageSummaries.forEach((coverageSummary) => {
                    let coverageId = coverageSummary.coverageId;
                    
                    let wgs84BoundingBox = coverageSummary.wgs84BoundingBox;
                    // Only parse possible coverage extents to WGS84 CRS
                    if (wgs84BoundingBox != null) {
                        let lowerArrayTmp = wgs84BoundingBox.lowerCorner.split(" ");
                        let xmin = parseFloat(lowerArrayTmp[0]);
                        let ymin = parseFloat(lowerArrayTmp[1]);

                        let upperArrayTmp = wgs84BoundingBox.upperCorner.split(" ");
                        let xmax = parseFloat(upperArrayTmp[0]);
                        let ymax = parseFloat(upperArrayTmp[1]);

                        let sizeInBytes:number = coverageSummary.customizedMetadata.getSizeInBytes();

                        let coverageExtentObj:wms.CoverageExtent = new wms.CoverageExtent(coverageId, xmin, ymin, xmax, ymax, sizeInBytes);
                        this.coveragesExtents.push(coverageExtentObj);
                    }
                });

                // // Also, store the CoveragesExtents to Service class then can be used later
                // webWorldWindService.setCoveragesExtentsArray(this.coveragesExtents);
                $scope.isCoveragesExtentsOpen = true;                                    

                // Init all possible checkboxes for geo-reference coverages and set to false
                $scope.initCheckboxesForCoverageIds();
            }

            // Handle the click event on GetCapabilities button
            $scope.handleGetServerCapabilities = () => {              
                $scope.getServerCapabilities();
                
                $scope.showAllFootprints.isChecked = false;
            }

            // Handle server capabilities request
            $scope.getServerCapabilities = (...args: any[])=> {                            
                if (!$scope.wcsServerEndpoint) {
                    alertService.error("The entered WCS endpoint is invalid.");
                    return;
                }

                // Hide any coverages' footprints which are shown before
                $scope.hideAllFootprintsOnGlobe();                     

                // Load new coverage extents
                this.coveragesExtents = [];     

                // Update settings:
                settings.wcsEndpoint = $scope.wcsServerEndpoint;

                // Create capabilities request
                let capabilitiesRequest = new wcs.GetCapabilities();       
                
                $scope.generatedGETURL = settings.wcsEndpoint + "?" + capabilitiesRequest.toKVP();

                wcsService.getServerCapabilities(capabilitiesRequest)
                    .then((response:rasdaman.common.Response<wcs.Capabilities>) => {
                            //Success handler
                            $scope.capabilitiesDocument = response.document;
                            $scope.capabilities = response.value;

                            $scope.isAvailableCoveragesOpen = true;
                            $scope.isServiceIdentificationOpen = true;
                            $scope.isServiceProviderOpen = true;

                            // for displaying coverages' footprints on WebWorldWind
                            $scope.parseCoveragesExtents();

                            webWorldWindService.initWebWorldWind(canvasId);
                
                            // share data to other WCS controllers
                            webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents = this.coveragesExtents;      
                            
                            // NOTE: loaded and $broadcast to be used with $on in other controllers, such as: WMS CreateLayer controller
                            $rootScope.$broadcast("wcsReloadServerCapabilitiesDone", true);

                            $rootScope.wcsServerCapabilities = response;

                            $rootScope.wcsReloadServerCapabilities = null;
                        },
                        (...args:any[])=> {
                            //Error handler
                            $scope.capabilitiesDocument = null;
                            $scope.capabilities = null;

                            $scope.isAvailableCoveragesOpen = false;
                            $scope.isServiceIdentificationOpen = false;
                            $scope.isServiceProviderOpen = false;

                            errorHandlingService.handleError(args);
                            $log.error(args);
                        })
                    .finally(() => {
                        $scope.wcsStateInformation.serverCapabilities = $scope.capabilities;
                    });
            };            
        }
    }

    interface WCSCapabilitiesControllerScope extends rasdaman.WCSMainControllerScope {
        wcsServerEndpoint:string;
        isAvailableCoveragesOpen:boolean;
        isCoveragesExtentsOpen:boolean;
        isServiceIdentificationOpen:boolean;
        isServiceProviderOpen:boolean;
        isCapabilitiesDocumentOpen:boolean;
        capabilitiesDocument:rasdaman.common.ResponseDocument;
        capabilities:wcs.Capabilities;  
        rowPerPageSmartTable:number;

        showAllFootprints:any;
        adminUserLoggedIn:boolean;

        generatedGETURL:string;

        parseCoveragesExtents():void;

        // Show/Hide the checked coverage extent on globe of current page
        showHideFootprintOnGlobe(coverageId:string):void;
        // Load all the coverages's extents on globe from all pages
        displayAllFootprintsOnGlobe(status:boolean):void;

        // Blacklist / whitelist a specific coverage when admin changes on the checkbox for it
        handleBlackListOneCoverage(coverageId:string):void;

        // Handle blacklist / whiltelist all coverages buttons' events
        handleBlackListAllCoverages():void;
        handleWhiteListAllCoverages():void;

        getServerCapabilities():void;
	
	    initCheckboxesForCoverageIds():void;
        getCoverageSummaryByCoverageId(coverageId):wcs.CoverageSummary;


	
    }




}
