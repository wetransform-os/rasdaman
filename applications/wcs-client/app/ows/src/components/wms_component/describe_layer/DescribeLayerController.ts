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

/// <reference path="../../../common/_common.ts"/>
/// <reference path="../../../models/wms_model/wms/_wms.ts"/>
///<reference path="../../../../assets/typings/tsd.d.ts"/>
/// <reference path="../../wms_component/settings/SettingsService.ts"/>
///<reference path="../../wms_component/WMSService.ts"/>
///<reference path="../../wcs_component/WCSService.ts"/>
///<reference path="../../../models/wms_model/wms/Capabilities.ts"/>
///<reference path="../../main/WMSMainController.ts"/>
///<reference path="../../web_world_wind/WebWorldWindService.ts"/>

module rasdaman {
    export class WMSDescribeLayerController {
        //Makes the controller work as a tab.
        private static selectedCoverageId:string;

        public static $inject = [
            "$scope",
            "$rootScope",
            "$log",            
            "rasdaman.WMSSettingsService",
            "rasdaman.WMSService",
            "rasdaman.WCSService",
            "Notification",
            "rasdaman.ErrorHandlingService",
            "rasdaman.WebWorldWindService"
        ];

        public constructor($scope:WMSDescribeLayerControllerScope,
                           $rootScope:angular.IRootScopeService,
                           $log:angular.ILogService,                           
                           settings:rasdaman.WMSSettingsService,
                           wmsService:rasdaman.WMSService,
                           wcsService:rasdaman.WCSService,
                           alertService:any,
                           errorHandlingService:rasdaman.ErrorHandlingService,
                           webWorldWindService:rasdaman.WebWorldWindService) {    
                               
            $scope.getMapRequestURL = null;                            
                               
            $scope.layerNames = [];
            $scope.layer = null;
            $scope.layers = [];   
            $scope.coverageBBox = "";
            
            $scope.displayWMSLayer = false;

            $scope.timeString = null;
            $scope.coverageDescription = null;

            let canvasId = "wmsCanvasDescribeLayer";

            let WCPS_QUERY_FRAGMENT = 0;
            let RASQL_QUERY_FRAGMENT = 1;            
          
            // When clicking on the layername from the table of GetCapabilities tab, it will change to DescribeLayer tab and load metadata for this selected layer.
            $rootScope.$watch("wmsSelectedLayerName", (layerName:string)=> {
                if (layerName != null) {
                    $scope.selectedLayerName = layerName;                
                    $scope.describeLayer();
                }
            });

            // Only allow to click on DescribeCoverage when layer name exists in the available list.
            $scope.isLayerNameValid = ():boolean => {                
                for (let i = 0; i < $scope.layers.length; i++) {
                    if ($scope.layers[i].name == $scope.selectedLayerName) {
                        return true;
                    }
                }                                    

                return false;
            };

            // NOTE: When DescribeCoverageController broadcasts message when a coverage id is renamed -> do some updatings
            $rootScope.$watch("renameCoverageId", (tupleObj:any) => {
                if (tupleObj != null) {
                    let oldCoverageId:string = tupleObj.oldCoverageId;
                    let newCoverageId:string = tupleObj.newCoverageId;

                    for (let i = 0; i < $scope.layerNames.length; i++) {
                        if ($scope.layerNames[i] == tupleObj.oldCoverageId) {
                            $scope.layerNames[i] = tupleObj.newCoverageId;
                            break;
                        }
                    }
                }
            });               
            
            // When GetCapabilities is requested, also update the available layers to be used in DescribeLayer controller.
            $scope.$watch("wmsStateInformation.serverCapabilities", (capabilities:wms.Capabilities)=> {                
                if (capabilities) {                                  
                    // NOTE: Clear the layers array first to get new valus from GetCapabilities
                    $scope.layers = [];  
                    $scope.layerNames = [];
                    $scope.display3DLayerNotification = false;
                    $scope.display4BandsExclamationMark = false;

                    capabilities.layers.forEach((layer:wms.Layer) => {                        
                        $scope.layerNames.push(layer.name);
                        $scope.layers.push(layer);                                                
                    });

                    // Describe the current selected layer
                    $scope.describeLayer();
                }
            });

            // When petascope admin user logged in, show insert/update/delete styles and insert/delete pyramid members features
            $rootScope.$watch("adminStateInformation.loggedIn", (newValue:boolean, oldValue:boolean)=> {
                if (newValue) {
                    // Admin logged in
                    $scope.adminUserLoggedIn = true;

                    $scope.hasInsertStyleRole = AdminService.hasRole($rootScope.adminStateInformation.roles, AdminService.PRIV_OWS_WMS_INSERT_STYLE);
                    $scope.hasUpdateStyleRole = AdminService.hasRole($rootScope.adminStateInformation.roles, AdminService.PRIV_OWS_WMS_UPDATE_STYLE);
                    $scope.hasDeleteStyleRole = AdminService.hasRole($rootScope.adminStateInformation.roles, AdminService.PRIV_OWS_WMS_DELETE_STYLE);

                    $scope.hasInsertCoverageRole = AdminService.hasRole($rootScope.adminStateInformation.roles, AdminService.PRIV_OWS_WCS_INSERT_COV);
                    $scope.hasDeleteCoverageRole = AdminService.hasRole($rootScope.adminStateInformation.roles, AdminService.PRIV_OWS_WCS_DELETE_COV);
                } else {
                    // Admin logged out
                    $scope.adminUserLoggedIn = false;
                }
            });
           
            // Describe the content (children elements of this selected layer) of a selected WMS layer
            $scope.describeLayer = function() {

                $scope.displayWMSLayer = false;
                $scope.selectedStyleName = "";
                $("#styleName").val("");
                $("#styleAbstract").val("");

                $("#overviewLegendImage").attr("src", "");

                for (let i = 0; i < $scope.layers.length; i++) {
                    if ($scope.layers[i].name == $scope.selectedLayerName) {                        

                        // Fetch the layer's metadata from the available layers
                        $scope.layer = $scope.layers[i];
                        $scope.isLayerDocumentOpen = true;

                        $scope.firstChangedSlider = [];
                        
                        // Fetch the coverageExtent by layerName to display on globe if possible
                        // as WMS layer name is as same as WCS coverageId
                        let coveragesExtents = [{"bbox": {"xmin": $scope.layer.coverageExtent.bbox.xmin,
                                                          "ymin": $scope.layer.coverageExtent.bbox.ymin,
                                                          "xmax": $scope.layer.coverageExtent.bbox.xmax,
                                                          "ymax": $scope.layer.coverageExtent.bbox.ymax}
                                               }];

                        // And load the layer as footprint on the globe
                        // Show coverage's extent on the globe                        
                        $scope.isCoverageDescriptionsHideGlobe = false;
                                                
                        // Check if coverage is 2D and has <= 4 bands then send a GetMap request to petascope and display result on globe
                        // Create describe coverage request
                        let coverageIds:string[] = [];
                        coverageIds.push($scope.layer.name);
                        let describeCoverageRequest = new wcs.DescribeCoverage(coverageIds);

                        let coverageExtent = $scope.layer.coverageExtent;
                        let bbox = "minLon=" + coverageExtent.bbox.xmin.toFixed(2) + ", minLat=" + coverageExtent.bbox.ymin.toFixed(2)
                                 +  ", maxLon=" + coverageExtent.bbox.xmax.toFixed(2) + ", maxLat=" + coverageExtent.bbox.ymax.toFixed(2);
                        $scope.coverageBBox = bbox;
                        
                        wcsService.getCoverageDescription(describeCoverageRequest)
                            .then(
                                (response:rasdaman.common.Response<wcs.CoverageDescription>)=> {
                                    //Success handler                                    
                                    $scope.coverageDescription = response.value;
                                    let dimensions = $scope.coverageDescription.boundedBy.envelope.srsDimension;

                                    addSliders(dimensions, coveragesExtents);                
                                    
                                    selectOptionsChange();
                                   
                                    // Then, load the footprint of layer on the globe
                                    webWorldWindService.showCoverageExtentOnGlobe(canvasId, $scope.layer.name, $scope.layer.coverageExtent);
                                },
                                (...args:any[])=> {                                    
                                    errorHandlingService.handleError(args);
                                    $log.error(args);
                                })

                        let listPyramidMembersRequest = new wms.ListPyramidMembers($scope.selectedLayerName);
                        
                        // get the pyramid members of this selected layer
                        wmsService.listPyramidMembersRequest(listPyramidMembersRequest).then(                            
                            (arrayData:[])=> {
                                let pyramidCoverageMembers = [];
                                arrayData.forEach((element:any) => {
                                    let coverageId = element["coverage"];
                                    let scaleFactors = element["scale"].join(",");
                                    let pyramidCoverageMember = new wms.PyramidCoverageMember(coverageId, scaleFactors);
                                    
                                    pyramidCoverageMembers.push(pyramidCoverageMember);
                                });

                                $scope.layers[i].pyramidCoverageMembers = pyramidCoverageMembers;
                            }, (...args:any[])=> {                                    
                                errorHandlingService.handleError(args);
                                $log.error(args);
                            });
                        

                        return;
                    }
                }                
                
            };

            /**
             * When sliders change, renew values to be displayed for WMS GetMap URL
             */
            function renewDisplayedWMSGetMapURL(url) {                
                let tmpURL = url + $scope.selectedStyleName;
                // Push the url to the view
                $( '#getMapRequestURL' ).text(tmpURL);
                $( '#getMapRequestURL' ).attr('href', tmpURL);
                $( '#secGetMap' ).attr('href', tmpURL);
            }

            /**
             * Add axis sliders for selected WMS layer             
             */
            function addSliders(dimensions, coveragesExtents) {

                for(let j = 0; j < dimensions; ++j) {
                    $scope.firstChangedSlider.push(false);
                }

                // Clear the content displayed in the info boxes of the sliders
                $("#sliders").empty();

                // Display a message to user about the last slice on non spatial axis is selected if layer is 3D+
                $scope.display3DLayerNotification = dimensions > 2 ? true : false;
                $scope.display4BandsExclamationMark = false;

                let showGetMapURL = false;
                let bands = $scope.coverageDescription.rangeType.dataRecord.fields.length;
                let bbox = coveragesExtents[0].bbox; 
                $scope.bboxLayer = bbox;  
                
                if (bands == 2 || bands > 4) {
                    $scope.display4BandsExclamationMark = true;
                }

                // As PNG can only support maximum 4 bands
                showGetMapURL = true;
                // send a getmap request in EPSG:4326 to server                                         
                let minLat = bbox.ymin;
                let minLong = bbox.xmin;
                let maxLat = bbox.ymax;
                let maxLong = bbox.xmax;
                
                $scope.timeString = null;

                // WMS 1.3 requires axes order by CRS (EPSG:4326 is lat, long order)
                let bboxStr = minLat + "," + minLong + "," + maxLat + "," + maxLong;   
                let urlDimensions = bboxStr;
                
                // Prepare the array to store the information for the 3D+ dimensions
                $scope.dimStr = [];
                for(let j = 0; j < 2; ++j){
                    $scope.dimStr.push('');
                }

                // Create the string used for the GetMap request in the 3D+ case
                for(let j = 2; j < dimensions; j++) {
                    if($scope.layer.layerDimensions[j].isTemporal == true) {
                        $scope.dimStr.push('&' + $scope.layer.layerDimensions[j].name + '="' + $scope.layer.layerDimensions[j].array[0] + '"');
                        $scope.timeString = $scope.layer.layerDimensions[j].array[0];
                    }
                    else {
                        $scope.dimStr.push('&' + $scope.layer.layerDimensions[j].name + '=' + $scope.layer.layerDimensions[j].array[0]);
                    }
                }
                for(let j = 2; j < dimensions; j++) {
                    urlDimensions += $scope.dimStr[j];
                }

                let getMapRequest = new wms.GetMap($scope.layer.name, urlDimensions, 800, 600, $scope.selectedStyleName);
                let url = settings.wmsFullEndpoint + "&" + getMapRequest.toKVP();
                $scope.getMapRequestURL = url;

                $( '#getMapRequestURL' ).text($scope.getMapRequestURL);
                // Then, let webworldwind shows the result of GetMap on the globe
                // Default layer is not shown
                webWorldWindService.loadGetMapResultOnGlobe(canvasId, $scope.selectedLayerName, null, $scope.bboxLayer, $scope.displayWMSLayer,
                                                                $scope.timeString, $scope.dimStr);
                

                if (!showGetMapURL) {
                    // Coverage cannot show GetMap on globe
                    $scope.getMapRequestURL = null;
                }  


                // Initialise auxbBox that can be modified in WebWorldWindService and dosen't change the initial values of the bbox
                let auxbBox = {
                    xmin:Number,
                    xmax:Number,
                    ymin:Number,
                    ymax:Number
                };
                auxbBox.xmax = $scope.bboxLayer.xmax;
                auxbBox.xmin = $scope.bboxLayer.xmin;
                auxbBox.ymax = $scope.bboxLayer.ymax;
                auxbBox.ymin = $scope.bboxLayer.ymin;

                let stepSize = 0.01;
                let numberStepsLat = ($scope.bboxLayer.ymax - $scope.bboxLayer.ymin) / stepSize;
                let numberStepsLong = ($scope.bboxLayer.xmax - $scope.bboxLayer.xmin) / stepSize;

                let stepLat = ($scope.bboxLayer.ymax - $scope.bboxLayer.ymin) / numberStepsLat;
                let stepLong = ($scope.bboxLayer.xmax - $scope.bboxLayer.xmin) / numberStepsLong;

                // Latitude slider
                $("#latSlider").slider({
                    max: numberStepsLat,
                    range: true,
                    values: [0, numberStepsLat],
                    slide: function(event, slider) {
                        // Get max/min values of the lat bbox
                        let sliderMin = slider.values[0];
                        let sliderMax = slider.values[1];

                        // Set the slider as changed, compute what means one step on the slider
                        $scope.firstChangedSlider[1] = true;
                                         
                        // Compute the new values of the lat bbox, setted using the sliders
                        minLat = bbox.ymin;
                        maxLat = bbox.ymax;
                        minLat += stepLat * sliderMin;
                        maxLat -= stepLat * (numberStepsLat - sliderMax);

                        // Update auxbBox, push the change to the bboxLayer
                        auxbBox.ymin = minLat;
                        auxbBox.ymax = maxLat;
                        $scope.bboxLayer = auxbBox;

                        // Update the lat info tooltip of the sliders
                        let tooltip = minLat + ':' + maxLat;                        
                        $("#latSlider").attr('data-original-title', tooltip);
                        $("#latSlider").tooltip('show');
                    
                        // Update the GetMap url
                        let bboxStr = 'bbox=' + minLat + "," + minLong + "," + maxLat + "," + maxLong;
                        let pos1 = url.indexOf('&bbox=');
                        let pos2 = url.indexOf('&', pos1 + 1);
                        url = url.substr(0, pos1 + 1) + bboxStr + url.substr(pos2, url.length - pos2);
                        $scope.getMapRequestURL = url;
                        
                        renewDisplayedWMSGetMapURL(url);

                        // Load the changed footprint of the layer on the globe
                        webWorldWindService.loadGetMapResultOnGlobe(canvasId, $scope.selectedLayerName, 
                                                    $scope.selectedStyleName, auxbBox, $scope.displayWMSLayer, $scope.timeString, $scope.dimStr);
                    }
                });

                $("#latSlider").tooltip();
                $("#latSlider").attr('data-original-title', $scope.bboxLayer.ymin + ':' + $scope.bboxLayer.ymax);

                // If the lat slider hasn't yet been moved set it to the initial position
                if ($scope.firstChangedSlider[1] == false) {
                    $("#latSlider").slider('values', [0, numberStepsLat]);
                }
                
                $("#longSlider").slider({
                    max: numberStepsLong,
                    range: true,
                    values: [0, numberStepsLong],
                    slide: function(event, slider) {
                        // Get max/min values of the long bbox
                        let sliderMin = slider.values[0];
                        let sliderMax = slider.values[1];

                        // Set the slider as changed, compute what means one step on the slider
                        $scope.firstChangedSlider[2] = true;
                                                
                        // Compute the new values of the long bbox, setted using the sliders
                        minLong = bbox.xmin;
                        maxLong = bbox.xmax;
                        minLong += stepLong * sliderMin;
                        maxLong -= stepLong * (numberStepsLong - sliderMax)

                        // Update auxbBox, push the change to the bboxLayer
                        auxbBox.xmin = minLong;
                        auxbBox.xmax = maxLong;
                        $scope.bboxLayer = auxbBox;

                        // Update the long info tooltip of the sliders
                        let tooltip = minLong + ':' + maxLong;                        
                        $("#longSlider").attr('data-original-title', tooltip);
                        $("#longSlider").tooltip('show');

                        // Update the GetMap url
                        let bboxStr = 'bbox=' + minLat + "," + minLong + "," + maxLat + "," + maxLong;
                        let pos1 = url.indexOf('&bbox=');
                        let pos2 = url.indexOf('&', pos1 + 1);
                        url = url.substr(0, pos1 + 1) + bboxStr + url.substr(pos2, url.length - pos2);
                        $scope.getMapRequestURL = url;

                        renewDisplayedWMSGetMapURL(url);
                        
                        // Load the changed footprint of the layer on the globe
                        webWorldWindService.loadGetMapResultOnGlobe(canvasId, $scope.selectedLayerName, 
                                                $scope.selectedStyleName, auxbBox, $scope.displayWMSLayer, $scope.timeString, $scope.dimStr);
                    }
                });

                $("#longSlider").tooltip();
                $("#longSlider").attr('data-original-title', $scope.bboxLayer.xmin + ':' + $scope.bboxLayer.xmax);

                // If the long slider hasn't yet been moved set it to the initial position
                if ($scope.firstChangedSlider[2] == false) {
                    $("#longSlider").slider('values', [0, numberStepsLong]);
                }

                let sufixSlider = "d";

                for (let j = 2; j < dimensions; j++) {
                    // Create for each dimension the view components for its corresponding slider 
                    $("<div />", { class:"containerSliders", id:"containerSlider"+j+sufixSlider})
                        .appendTo( $("#sliders"));

                    $("<label />", { class:"sliderLabel", id:"label"+j+sufixSlider})
                        .appendTo( $("#containerSlider"+j+sufixSlider));
                    $("#label"+j+sufixSlider).text($scope.layer.layerDimensions[j].name + ':');

                    $("<div />", { class:"slider", id:"slider"+j+sufixSlider})
                        .appendTo( $("#containerSlider"+j+sufixSlider));

                    let sliderId = "#slider" + j + sufixSlider;
                  
                    // Controler of the slider
                    $( function() {
                        $(sliderId).slider({
                            // Set for each dimension the number of steps on its corresponding the slider
                            max: $scope.layer.layerDimensions[j].array.length - 1,
                            // Initialisations for each slider
                            create: function(event, slider) {
                                // Define the variables such that they can be seen inside the slider code
                                this.sliderObj = $scope.layer.layerDimensions[j];          
                                this.sliderObj.index = j;

                                let sizeSlider = $scope.layer.layerDimensions[j].array.length - 1;
                                
                                // Add the index lines below the slider
                                for (let it = 1; it < sizeSlider; ++it) {
                                    $("<label>|</label>").css('left', (it/sizeSlider*100)+'%')
                                        .appendTo($(sliderId));
                                }
                                
                            },

                            slide: function(event, slider) {
                                // Set the slider as changed
                                $scope.firstChangedSlider[this.sliderPos] = true;

                                // Update the GetMap url
                                if (this.sliderObj.isTemporal == true) {
                                    $scope.dimStr[this.sliderObj.index] = this.sliderObj.name + '="' + this.sliderObj.array[slider.value] + '"';
                                    $scope.timeString = this.sliderObj.array[slider.value];
                                } else {
                                    $scope.dimStr[this.sliderObj.index] = this.sliderObj.name + '=' + this.sliderObj.array[slider.value];
                                }

                                let pos1 = url.indexOf('&' + this.sliderObj.name + '=');
                                let pos2 = url.indexOf('&', pos1 + 1);
                                url = url.substr(0, pos1 + 1) + $scope.dimStr[this.sliderObj.index] + url.substr(pos2, url.length - pos2);
                                $scope.getMapRequestURL = url;
                                
                                // Update the dimenitional info tooltip of the slider
                                let tooltip = this.sliderObj.array[slider.value];                                
                                $(sliderId).attr('data-original-title', tooltip);
                                $(sliderId).tooltip('show');

                                renewDisplayedWMSGetMapURL(url);

                                // Load the changed footprint of the layer on the globe
                                webWorldWindService.loadGetMapResultOnGlobe(canvasId, $scope.selectedLayerName, 
                                                                            $scope.selectedStyleName, auxbBox, $scope.displayWMSLayer, $scope.timeString, $scope.dimStr);
                            }
                        });
                    } );

                    $(sliderId).tooltip();
                    $(sliderId).attr('data-original-title', $scope.layer.layerDimensions[j].array[0]);
                    

                    // If the i-th dimentional slider hasn't yet been moved set it to the initial position
                    if ($scope.firstChangedSlider[j] == false) {
                        $(sliderId).slider('value', 0);
                    }
                }
            }

            $scope.isLayerDocumentOpen = false;
            
            // Load/Unload WMSLayer on WebWorldWind globe from the checkbox user selected
            $scope.showWMSLayerOnGlobe = (styleName:string)=> {
                $scope.selectedStyleName = styleName;
                $scope.displayWMSLayer = true;          

                renewDisplayedWMSGetMapURL($scope.getMapRequestURL);
                webWorldWindService.loadGetMapResultOnGlobe(canvasId, $scope.selectedLayerName, 
                                    styleName, $scope.bboxLayer, true, $scope.timeString, $scope.dimStr);
            }

            $scope.hideWMSLayerOnGlobe = ()=> {                
                $scope.displayWMSLayer = false;          
                webWorldWindService.loadGetMapResultOnGlobe(canvasId, $scope.selectedLayerName, 
                                    $scope.selectedStyleName, $scope.bboxLayer, false, $scope.timeString, $scope.dimStr);
            }

            // ********** Layer's downscaled coverages management **************

            // Create a pyramid member coverage as downscaled level coverage of this selected layer
            $scope.createPyramidMember = () => {                
                let pyramidMemberCoverageId = $("#pyramidMemberCoverageIdValue").val().trim();
                let scaleFactors = $("#scaleFactorsValue").val().trim();                

                // validation first
                for (let i = 0; i < $scope.layer.pyramidCoverageMembers.length; i++) {
                    let pyramidMemberCoverage = $scope.layer.pyramidCoverageMembers[i];
                    if (pyramidMemberCoverage.coverageId == pyramidMemberCoverageId) {
                        alertService.error("Coverage pyramid member: <b>" + pyramidMemberCoverageId + "</b> already exists in the pyramid of layer: <b>" + $scope.selectedLayerName + "</b>");
                        return;
                    } else if (pyramidMemberCoverage.scaleFactors == scaleFactors) {
                        alertService.error("Scale factors: <b>" + scaleFactors + "</b> already exists in coverage pyramid member: <b>" + pyramidMemberCoverage.coverageId + "</b> of layer: <b>" + $scope.selectedLayerName + "</b>");
                        return;
                    }
                }

                let createPyramidMember = new wms.CreatePyramidMember($scope.layer.name, scaleFactors, pyramidMemberCoverageId);
                wmsService.createPyramidMemberRequest(createPyramidMember).then(
                    (...args:any[])=> {
                        alertService.success("Successfully created pyramid member coverage: <b>" + pyramidMemberCoverageId 
                                           + "</b> with scalefactors: <b>" + scaleFactors + "</b> of layer:  <b>" + $scope.layer.name + "</b>.");
                        // reload WMS GetCapabilities
                        // NOTE: This is required, because WCS GetCapabilties Controller needs to get the WGS84 BBox and the coverageSizeInBytes of the newly created pyramid member coverage
                        // but it doesn't have these information here here, so it needs to do full request WCS GetCapabilities (!)
                        $rootScope.wmsCreatedPyramidMemberCoverage = true;
                        
                        $scope.layer.pyramidCoverageMembers.push(new wms.PyramidCoverageMember(pyramidMemberCoverageId, scaleFactors));                       
                    }, (...args:any[])=> {
                        errorHandlingService.handleError(args);                            
                    }).finally(function () {                        
                });

            }

            // Remove a pyramid member coverage from the base coverage (selected layer)
            $scope.removePyramidMember = (pyramidMemberCoverageId:string) => {
                // Then, send the delete layer's downscaled collection level request to server
                let removePyramidMemberRequest = new wms.RemovePyramidMember($scope.layer.name, pyramidMemberCoverageId);
                wmsService.removePyramidMemberRequest(removePyramidMemberRequest).then(
                    (...args:any[])=> {
                        alertService.success("Successfully removed pyramid member: <b>" + pyramidMemberCoverageId 
                                + "</b> from layer: <b>" + $scope.layer.name + "</b>");

                        // reload WMS GetCapabilities (no need it, as pyramid member coverage is not deleted (!))
                        // $scope.wmsStateInformation.reloadServerCapabilities = true;

                        for (let i = 0; i < $scope.layer.pyramidCoverageMembers.length; i++) {
                            let pyramidMemberCoverage = $scope.layer.pyramidCoverageMembers[i];
                            if (pyramidMemberCoverage.coverageId == pyramidMemberCoverageId) {
                                // remove the pyramid member coverage in the pyramid list of the selected layer
                                $scope.layer.pyramidCoverageMembers.splice(i, 1);
                                break;
                            }
                        }

                    }, (...args:any[])=> {
                        errorHandlingService.handleError(args);                            
                    }).finally(function () {                        
                });                                
            }


            // ********** Layer's styles management **************

            // Show/hide query/table color definitions if not needed
            function selectOptionsChange() {
                
                $("#styleQueryType").val("none").change();

                $("#styleQueryType").change(function() {
                    if (this.value !== "none") {
                        $("#divStyleQuery").show();
                    } else {
                        $("#divStyleQuery").hide();
                    }
                });

                $("#styleColorTableType").val("none").change();               

                $("#styleColorTableType").change(function() {                    
                    if (this.value !== "none") {
                        $("#divStyleColorTableDefinition").show();
                    } else {
                        $("#divStyleColorTableDefinition").hide();
                    }
                });

                $("#colorTableDefinitionStyleFileInput").change(function() {                 
                    const reader = new FileReader();
                    reader.onload = function fileReadCompleted() {
                        $("#styleColorTableDefinition").val(reader.result as string);
                    };
                    reader.readAsText(this.files[0]);
                });

                /**
                 * Select a legend image from a local file
                 */
                $("#legendImageFileInput").change(function() {                    
                    let selectedfile = this.files;
                    if (selectedfile.length > 0) {
                        let imageFile = selectedfile[0];
                        let fileReader = new FileReader();
                        fileReader.onload = function(fileLoadedEvent) {
                            let srcData:any = fileLoadedEvent.target.result;
                            // put the base64 to textarea for submit
                            $("#hiddenLegendBase64Textarea").val(srcData);

                            $("#overviewLegendImage").attr("src", srcData);

                        }
                        fileReader.readAsDataURL(imageFile);
                    }
                });

            }
            
            $scope.isStyleNameValid = (styleName:string)=> {                
                for (let i = 0; i < $scope.layer.styles.length; ++i) {
                    if ($scope.layer.styles[i].name == styleName) {
                        return true;
                    }
                }                                    

                return false;
            };

            // Set this style as default style of the selected layer
            $scope.setDefaultStyle = (styleName:string) => {
                $scope.describeStyleToUpdate(styleName);

                if ($scope.layer.styles.length == 1) {
                    return;
                } else if ($scope.layer.styles.length > 1) {
                    // Send request to petascope to set this style as the default of layer
                    $scope.defaultStyleName = styleName;

                    // If admin doesn't set defaultStyle via checkbox, but via the radio button in the table of styles, 
                    // then this style is set to default and the checkbox defaultStyle button is checked
                    if ($scope.defaultStyleName === styleName) {
                        $("#defaultStyle").prop("checked", true);
                    }

                    $scope.updateStyle();
                }
            }

            // Display the selected style's metadata to the form for updating
            $scope.describeStyleToUpdate = (styleName:string) => {
                for (let i = 0; i < $scope.layer.styles.length; i++) {
                    let styleObj = $scope.layer.styles[i];
                    if (styleObj.name == styleName) {
                        $("#styleName").val(styleObj.name);                        
                        $("#styleAbstract").val(styleObj.abstract);

                        if (styleObj.defaultStyle == true) {
                            $("#defaultStyle").prop("checked", true);
                        } else {
                            $("#defaultStyle").prop("checked", false);
                        }

                        let styleQueryType = styleObj.queryType;
                        if (styleQueryType === "") {
                            styleQueryType = "none";
                        }
                        $("#styleQueryType").val(styleQueryType);
                        $("#styleQuery").val(styleObj.query);
                        
                        let colorTableType = styleObj.colorTableType;
                        if (colorTableType === "") {
                            colorTableType = "none";
                        }
                        $("#styleColorTableType").val(colorTableType);
                        $("#styleColorTableDefinition").val(styleObj.colorTableDefinition);

                        // Show/hide query/color table definition divs
                        $("#styleQueryType").change();
                        $("#styleColorTableType").change();
                        
                        
                        if (styleObj.legendGraphicURL != null) {
                            // suffix date to avoid cache image in web browser
                            $("#overviewLegendImage").attr("src", styleObj.legendGraphicURL + "&" + new Date().getTime());
                        } else {
                            if (styleObj.legendGraphicBase64String == null) {
                                $("#overviewLegendImage").attr("src", "");
                            } else {
                                $("#overviewLegendImage").attr("src", styleObj.legendGraphicBase64String);
                            }
                        }                        

                        break;
                    }
                }
            }

            // validate the style's data before insert/update to database
            $scope.validateStyle = ()=> {
                let styleName = $("#styleName").val();
                let styleAbstract = $("#styleAbstract").val();
                let styleQueryType = $("#styleQueryType").val();
                let styleQuery = $("#styleQuery").val();
                let styleColorTableType = $("#styleColorTableType").val();
                let styleColorTableDefintion = $("#styleColorTableDefinition").val();
                

                if (styleName.trim() === "") {
                    alertService.error("Style name cannot be empty.");
                    return;
                } else if (styleAbstract.trim() === "") {
                    alertService.error("Style abstract cannot be empty.");
                    return;
                }

                if (styleQueryType == "none" && styleColorTableType == "none") {
                    alertService.error("A style must contain at least a query fragment or a color table definition.");
                    return;
                }

                if (styleQuery.trim() === "" && styleColorTableDefintion.trim() === "") {
                    alertService.error("Style query or color table definition must have value.");
                    return;
                }                  
                
                return true;
            }

            /**
             * Make a radio button on smart table to set a style as default checked
             * @param radioButtonId 
             */
            $scope.checkDefaultStyleCheckBox = (radioButtonId:string) => {
                // NOTE: it is not possible to update the radio button in the smart table via ng-model
                // so use jquery instead
                $('input:radio[name="defaultStyleRadioButtonGroup"]').prop('checked', false);
                $("#defaultStyleRadioButton_" + radioButtonId).prop("checked", true);
            }

            // update WMS style to database
            $scope.updateStyle = () => {
                // first validate the style's data
                if ($scope.validateStyle()) {
                    let styleName = $("#styleName").val();
                    let styleAbstract = $("#styleAbstract").val();
                    let styleQueryType = $("#styleQueryType").val();
                    let styleQuery = $("#styleQuery").val();
                    let styleColorTableType = $("#styleColorTableType").val();
                    let styleColorTableDefintion = $("#styleColorTableDefinition").val();

                    // If admin changes in defaultStyle checkbox to true, then this style is set to default
                    let defaultStyle = $("#defaultStyle").prop("checked");                    

                    // Check if style of current layer exists
                    if (!$scope.isStyleNameValid(styleName)) {
                        alertService.error("Style name: <b>" + styleName + "</b> does not exist to update.");
                        return;
                    }

                    let legendGraphicBase64 = null;
                    let base64String = $("#hiddenLegendBase64Textarea").val();
                    if (base64String !== "") {
                        legendGraphicBase64 = base64String;
                    }

                    // Then, send the update layer's style request to server
                    let updateLayerStyle = new wms.UpdateLayerStyle($scope.layer.name, styleName, styleAbstract, styleQueryType, 
                                                                    styleQuery, styleColorTableType, styleColorTableDefintion, defaultStyle, legendGraphicBase64);
                    wmsService.updateLayerStyleRequest(updateLayerStyle).then(
                        (...args:any[])=> {
                            alertService.success("Successfully update style with name: <b>" + styleName + "</b> of layer: <b>" + $scope.layer.name + "</b>");                          
                            
                            // reload WMS GetCapabilites (not needed)
                            // $scope.wmsStateInformation.reloadServerCapabilities = true;
                            // $scope.describeStyleToUpdate(styleName);
                            
                            // update locally in the client

                            if (defaultStyle == true) {
                                // unmark all previous styles 
                                for (let i = 0; i < $scope.layer.styles.length; i++) {
                                    $scope.layer.styles[i].defaultStyle = false;
                                }
                            }

                            for (let i = 0; i < $scope.layer.styles.length; i++) {
                                let style:wms.Style = $scope.layer.styles[i];
                                if (style.name == styleName) {
                                    style.abstract = styleAbstract;
                                    style.queryType = styleQueryType;
                                    style.query = styleQuery;
                                    style.colorTableType = styleColorTableType;
                                    style.colorTableDefinition = styleColorTableDefintion;
                                    style.defaultStyle = defaultStyle;
                                    if (legendGraphicBase64 != "") {
                                        style.legendGraphicBase64String = legendGraphicBase64;
                                    }
                                    break;
                                }
                            }

                            $scope.checkDefaultStyleCheckBox(styleName);

                        }, (...args:any[])=> {
                            errorHandlingService.handleError(args);                            
                        }).finally(function () {                        
                    });
                }                
            }

            // insert WMS style to database
            $scope.insertStyle = () => {
                // first validate the style's data
                if ($scope.validateStyle()) {
                    let styleName = $("#styleName").val();
                    let styleAbstract = $("#styleAbstract").val();
                    let styleQueryType = $("#styleQueryType").val();
                    let styleQuery = $("#styleQuery").val();
                    let styleColorTableType = $("#styleColorTableType").val();
                    let styleColorTableDefintion = $("#styleColorTableDefinition").val();
                    let defaultStyle = $("#defaultStyle").prop("checked");

                    // Check if style of current layer exists
                    if ($scope.isStyleNameValid(styleName)) {
                        alertService.error("Style name: <b>" + styleName + "</b> already exists, cannot insert the same name.");
                        return;
                    }

                    let legendGraphicBase64 = null;
                    let base64String = $("#hiddenLegendBase64Textarea").val();
                    if (base64String !== "") {
                        legendGraphicBase64 = base64String;
                    }

                    // Then, send the insert layer's style request to server
                    let insertLayerStyle = new wms.InsertLayerStyle($scope.layer.name, styleName, styleAbstract, styleQueryType, styleQuery, styleColorTableType, styleColorTableDefintion, defaultStyle, legendGraphicBase64);
                    wmsService.insertLayerStyleRequest(insertLayerStyle).then(
                        (...args:any[])=> {
                            alertService.success("Successfully insert style with name: <b>" + styleName + "</b> of layer: <b>" + $scope.layer.name + "</b>");

                            // reload WMS GetCapabilites (not needed)
                            // $scope.wmsStateInformation.reloadServerCapabilities = true;
                            // $scope.describeStyleToUpdate(styleName);


                            // Update locally in the client

                            let newStyle:wms.Style = new wms.Style(styleName, styleAbstract, styleQueryType, styleQuery, styleColorTableType, styleColorTableDefintion, defaultStyle, null, legendGraphicBase64);
                            if (defaultStyle == true) {
                                // unmark all previous styles 
                                for (let i = 0; i < $scope.layer.styles.length; i++) {
                                    $scope.layer.styles[i].defaultStyle = false;
                                }
                            }
                            $scope.layer.styles.push(newStyle);

                            $scope.checkDefaultStyleCheckBox(styleName);                            
                            
                        }, (...args:any[])=> {
                            errorHandlingService.handleError(args);
                        }).finally(function () {                        
                    });
                }                
            }

            // delete WMS style from database
            $scope.deleteStyle = (styleName:string)=> {                
                // Then, send the delete layer's style request to server
                let deleteLayerStyle = new wms.DeleteLayerStyle($scope.layer.name, styleName);                    
                wmsService.deleteLayerStyleRequest(deleteLayerStyle).then(
                    (...args:any[])=> {
                        alertService.success("Successfully delete style with name <b>" + styleName + "</b> of layer with name <b>" + $scope.layer.name + "</b>");
                        // reload WMS GetCapabilities (not needed)
                        // $scope.wmsStateInformation.reloadServerCapabilities = true;

                        for (let i = 0; i < $scope.layer.styles.length; i++) {
                            let style:wms.Style = $scope.layer.styles[i];
                            if (style.name == styleName) {                                
                                if (style.defaultStyle == true) {
                                    // this deleted style is default -> set it to the first style
                                    $scope.layer.styles[0].defaultStyle = true;
                                }

                                $scope.layer.styles.splice(i, 1);
                                break;
                            }
                        }
                    }, (...args:any[])=> {
                        errorHandlingService.handleError(args);                            
                    }).finally(function () {                        
                });                                
            }
        }
    }

    interface WMSDescribeLayerControllerScope extends WMSMainControllerScope {           
        isLayerDocumentOpen:boolean;
        // Only with 2D coverage (bands <=4) can show GetMap
        getMapRequestURL:string;        
        bboxLayer:any;
        displayWMSLayer:boolean;
        display4BandsNotification:boolean;
        display4BandsExclamationMark:boolean;

        timeString:string;

        firstChangedSlider:boolean[];

        display3DLayerNotification:boolean;

        // Show the WMSLayer on WebWorldWind globe (default doesn't show)
        showWMSLayerOnGlobe(styleName:string):void;
        // Hide the WMSLayer on WebWorldWind globe
        hideWMSLayerOnGlobe():void;

        // Model of text box to search layer by name
        layerNames:string[];
        // Contain all the layers's metadata
        layers:wms.Layer[];        
        // Selected layer to describe
        layer:wms.Layer;
        coverageBBox:string;
        selectedLayerName:string;       
        selectedStyleName:string; 
        describeLayer():void;
	    deleteStyle(styleName:string):void;
	    isStyleNameValid(styleName:string):boolean;
	    isCoverageDescriptionsHideGlobe:boolean;
	    isLayerNameValid():boolean;
	    validateStyle():boolean;
	    insertStyle():void;
	    updateStyle():void;
        describeStyleToUpdate(styleName:string):void;

        defaultStyleName:string;
        setDefaultStyle(styleName:string):void;
        
        coverageDescription:wcs.CoverageDescription;

        hasInsertStyleRole: boolean;
        hasUpdateStyleRole: boolean;
        hasDeleteStyleRole: boolean;

        hasInsertCoverageRole: boolean;
        hasDeleteCoverageRole: boolean;
        
        dimStr: any[];
    }
}
