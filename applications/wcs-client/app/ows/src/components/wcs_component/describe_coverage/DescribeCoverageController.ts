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
///<reference path="../../wcs_component/WCSService.ts"/>
///<reference path="../../../models/wcs_model/wcs/Capabilities.ts"/>
///<reference path="../../main/WCSMainController.ts"/>
///<reference path="../../web_world_wind/WebWorldWindService.ts"/>

module rasdaman {
    export class WCSDescribeCoverageController {
        //Makes the controller work as a tab.
        private static selectedCoverageId:string;

        public static $inject = [
            "$scope",
            "$rootScope",
            "$log",
            "rasdaman.WCSService",
            "rasdaman.WCSSettingsService",
            "Notification",
            "rasdaman.ErrorHandlingService",
            "rasdaman.WebWorldWindService"
        ];

        public constructor($scope:WCSDescribeCoverageControllerScope,
                           $rootScope:angular.IRootScopeService,
                           $log:angular.ILogService,
                           wcsService:rasdaman.WCSService,
                           settings:rasdaman.WCSSettingsService,
                           alertService:any,
                           errorHandlingService:rasdaman.ErrorHandlingService,
                           webWorldWindService:rasdaman.WebWorldWindService) {            

            $scope.selectedCoverageId = null;
            $scope.newCoverageId = null;
            $scope.REGULAR_AXIS = "regular";
            $scope.IRREGULAR_AXIS = "irregular";
            $scope.NOT_AVALIABLE = "N/A";

            $scope.hasRoleUpdateCoverage = AdminService.hasRole($rootScope.userLoggedInRoles, AdminService.PRIV_OWS_WCS_UPDATE_COV);

            // default hide the div containing the Globe
            $scope.hideWebWorldWindGlobe = true;

            $scope.avaiableCisTypes = [
                    { "value": "CIS1.1", "text": "CIS 1.1 GeneralGridCoverage" },
                    { "value": "CIS1.0", "text": "CIS 1.0 GridCoverage / RectifiedGridCoverage / RectifiedGridCoverage (legacy)" }
                ];
            $scope.selectedCisType = $scope.avaiableCisTypes[0].value;

            // Show coverage's extent on the globe
            let canvasId = "wcsCanvasDescribeCoverage";

            $scope.isCoverageIdValid = function():boolean {
                if ($scope.wcsStateInformation.serverCapabilities) {
                    var coverageSummaries = $scope.wcsStateInformation.serverCapabilities.contents.coverageSummaries;
                    for (var i = 0; i < coverageSummaries.length; i++) {
                        if (coverageSummaries[i].coverageId == $scope.selectedCoverageId) {                            
                            return true;
                        }
                    }
                }

                return false;
            };

            // NOTE: it listens data changed from WCSMainController, when selecting a coverageId from smart table in WCS GetCapabilities tab
            $rootScope.$watch("wcsSelectedGetCoverageId", (newValue:string, oldValue:string) => {                
                if (newValue != null) {                    
                    $scope.selectedCoverageId = newValue;                    
                    $scope.describeCoverage();
                }
            });

            $scope.$watch("wcsStateInformation.serverCapabilities", (capabilities:wcs.Capabilities)=> {
                if (capabilities) {
                    $scope.availableCoverageIds = [];
                    $scope.coverageCustomizedMetadatasDict = {};
                    
                    capabilities.contents.coverageSummaries.forEach((coverageSummary:wcs.CoverageSummary)=> {
                        let coverageId = coverageSummary.coverageId;
                        $scope.availableCoverageIds.push(coverageId);

                        // coverage location, size,...
                        if (coverageSummary.customizedMetadata != null) {
                            $scope.coverageCustomizedMetadatasDict[coverageId] = coverageSummary.customizedMetadata;
                        }
                    });
                }                
            });

            // NOTE: When DeleteCoverageController broadcasts message -> do some cleanings
            $rootScope.$on("deletedCoverageId", (event, coverageIdToDelete:string) => {
                if (coverageIdToDelete != null) {
                    for (let i = 0; i < $scope.availableCoverageIds.length; i++) {
                        if ($scope.availableCoverageIds[i] == coverageIdToDelete) {
                            $scope.availableCoverageIds.splice(i, 1);
                            break;
                        }
                    }
                }
            });            


            // when GetCoverage triggers get coverage id, this function will be called to fill data for both DescribeCoverage and GetCoverage tabs
            $scope.$watch("wcsStateInformation.selectedGetCoverageId", (getCoverageId:string) => {
                if (getCoverageId) {
                    $scope.selectedCoverageId = getCoverageId;
                    $scope.describeCoverage();
                }
            });


            /**
             * Default CIS 1.1. is selected
             */
            $scope.updateGeneratedUrlForSelectedCisType = () => {
                let coverageIds:string[] = [];
                coverageIds.push($scope.selectedCoverageId);
                let describeCoverageRequest = new wcs.DescribeCoverage(coverageIds);
                let requestUrl:string = settings.wcsEndpoint + "?" + describeCoverageRequest.toKVP();

                if ($scope.selectedCisType == "CIS1.1") {
                    requestUrl += "&outputType=GeneralGridCoverage";

                    // NOTE: only WCS 2.1.0 supports GeneralGridCoverage
                    requestUrl = requestUrl.replace("2.0.1", "2.1.0");
                }

                $scope.generatedGETURL = requestUrl;
            };

            /**
             * Update coverage's metadata from a text file
             */
            $scope.updateCoverageMetadata = () => {
                // Get browsed file to upload
                var fileInput:any = document.getElementById("coverageMetadataUploadFile");
                var mimeType = fileInput.files[0].type;
                var requiredMimeTypes:any = ["", "text/xml", "", "application/json", "text/plain"];
                if (!requiredMimeTypes.includes(mimeType)) {
                    alertService.error("Coverage's metadata file to update must be <b>xml/json/text</b> format. Given: <b>'" + mimeType + "'</b>.");
                    return;
                }

                var formData = new FormData();
                formData.append("coverageId", $scope.selectedCoverageId);
                formData.append("fileName", fileInput.files[0]);          

                wcsService.updateCoverageMetadata(formData).then(
                    response => {
                        alertService.success("Successfully updated coverage's metadata from file.");
                        // Reload DescribeCoverage to see new changes
                        $scope.describeCoverage();
                    }, (...args:any[])=> {                            
                        errorHandlingService.handleError(args);
                        $log.error(args);
                    }
                );
            }

            /**
             * Rename coverage's id
             * NOTE: update coverage's id -> update several places in both WCS and WMS controllers
             */
            $scope.renameCoverageId = () => {    
                if ($scope.newCoverageId == null || $scope.newCoverageId.trim() == "") {
                    alertService.error("New coverage id cannot be empty.");
                    return;
                }

                var formData = new FormData();
                formData.append("coverageId", $scope.selectedCoverageId);
                formData.append("newCoverageId", $scope.newCoverageId);
                let tupleObj:any = {
                    "oldCoverageId": $scope.selectedCoverageId,
                    "newCoverageId": $scope.newCoverageId
                }

                wcsService.updateCoverageMetadata(formData).then(
                    response => {
                        alertService.success("Successfully rename coverage's id.");

                        // NOTE: Update name in WCS / WMS GetCapabilies and coverage extents on it and WebWorldWind coverage extents caches
                        $rootScope.$broadcast("renamedCoverageId", tupleObj);

                        // Reload DescribeCoverage to see new changes
                        $scope.selectedCoverageId = $scope.newCoverageId;
                        $scope.newCoverageId = null;

                        $scope.describeCoverage();

                        for (let i = 0; i < $scope.availableCoverageIds.length; i++) {
                            if ($scope.availableCoverageIds[i] == tupleObj.oldCoverageId) {
                                $scope.availableCoverageIds[i] = tupleObj.newCoverageId;
                                break;
                            }
                        }
                    }, (...args:any[])=> {                            
                        errorHandlingService.handleError(args);
                        $log.error(args);
                    }
                );
            }            

            /**
             * Parse coverage metadata as string and show it to a dropdown
             */
            $scope.parseCoverageMetadata = () => {
                $scope.metadata = null;
                
                // Extract the metadata from the coverage document (inside <rasdaman:covMetadata></rasdaman:covMetadata>)
                var parser = new DOMParser();
                var xmlDoc = parser.parseFromString($scope.rawCoverageDescription, "text/xml");

                var elements = xmlDoc.getElementsByTagName("rasdaman:covMetadata");
                if (elements.length == 0) {
                    // In case coverage's metadata is not created by wcst_import (e.g: INSPIRE metadata)
                    elements = xmlDoc.getElementsByTagName("gmlcov:Extension");
                }
                if (elements.length > 0) {
                    $scope.metadata = elements[0].innerHTML;

                    // Check if coverage metadata is XML / JSON format
                    for (let i = 0; i < $scope.metadata.length; i++) {
                        if ($scope.metadata[i] === "{") {
                            $scope.typeMetadata = "json";
                            break;
                        } else {
                            $scope.typeMetadata = "xml";
                            break;
                        }
                    }
                }

                // As coverage contains no metadata
                if ($scope.metadata == null) {
                    // To display empty in extra metadata dropdown
                    $scope.metadata = " ";
                    $("#btnUpdateCoverageMetadata").text("Insert metadata");
                } else {
                    // Coverage has existing metadata
                    $("#btnUpdateCoverageMetadata").text("Update metadata");
                }
            }

            $scope.describeCoverage = function () {          

                // Show the newly generated URL for DescribeCoverage
                $scope.updateGeneratedUrlForSelectedCisType();
                
                //Create describe coverage request
                let coverageIds:string[] = [];
                coverageIds.push($scope.selectedCoverageId);

                let describeCoverageRequest = new wcs.DescribeCoverage(coverageIds);
                $scope.coverageBBox = "";
                $scope.axes = [];                

                // Clear selected file to upload
                $("#coverageMetadataUploadFile").val("");
                $("#uploadFileName").html("");
                $("#btnUpdateCoverageMetadata").hide();

                           
                //Retrieve coverage description
                wcsService.getCoverageDescription(describeCoverageRequest)
                    .then(
                        (response:rasdaman.common.Response<wcs.CoverageDescription>)=> {
                            // //Success handler                            
                            $scope.coverageDescription = response.value;    
                            
                            if ($scope.selectedCisType == "CIS1.0") {
                                $scope.rawCoverageDescription = response.document.value;
                            }

                            $scope.parseCoverageMetadata();

                            let coverageExtent:any = webWorldWindService.getCoveragesExtentByCoverageId(webWorldWindService.wcsGetCapabilitiesWGS84CoverageExtents, $scope.selectedCoverageId);
                            if (coverageExtent == null) {
                                // coverage is not geo-referenced
                                $scope.hideWebWorldWindGlobe = true;
                            } else {
                                // coverage is referenced -> draw on globe
                                $scope.hideWebWorldWindGlobe = false;
                                webWorldWindService.showCoverageExtentOnGlobe(canvasId, $scope.selectedCoverageId, coverageExtent, true);
                            }                  
                        },
                        (...args:any[])=> {                            
                            $scope.coverageDescription = null ;

                            errorHandlingService.handleError(args);
                            $log.error(args);
                        })
                    .finally(() => {
                        $scope.wcsStateInformation.selectedCoverageDescription = $scope.coverageDescription;
                    });


                if ($scope.selectedCisType == "CIS1.1") {
                
                    // NOTE: now it always need to request twice if CIS1.1 is selected
                    wcsService.getCoverageDescriptionCis11($scope.generatedGETURL)
                        .then(
                            (response:string)=> {
                                // //Success handler                            
                                let xmlResult:string = response;
                                $scope.rawCoverageDescription = xmlResult;

                            },
                            (...args:any[])=> {                                                        

                                errorHandlingService.handleError(args);
                                $log.error(args);
                            })
                        .finally(() => {
                            $scope.wcsStateInformation.selectedCoverageDescription = $scope.coverageDescription;
                        });
                };


            };        
                
                

        }
    }

    interface WCSDescribeCoverageControllerScope extends WCSMainControllerScope {        
        // Not show the globe when coverage cannot reproject to EPSG:4326
        isCoverageDescriptionsDocumentOpen:boolean;
        hideWebWorldWindGlobe:boolean;

        coverageDescription:wcs.CoverageDescription;
        rawCoverageDescription:string;

        availableCoverageIds:string[];
        coverageCustomizedMetadatasDict:any;
        selectedCoverageId:string;
        newCoverageId:string;

        // Array of objects
        axes:any[];

        coverageBBox:string

        metadata:string;
        typeMetadata:string;

        generatedGETURL:string;


        avaiableCisTypes:any[];
        selectedCisType:string;

        isCoverageIdValid():boolean;
        describeCoverage():void;
        getAxisResolution(number, any):string;
        getAxisType(number, any):string;
        parseCoverageMetadata():void;

        adminUserLoggedIn:boolean;
        metadataFileToUpload:string;
        updateCoverageMetadata():void;

        updateGeneratedUrlForSelectedCisType():void;
    }
}
