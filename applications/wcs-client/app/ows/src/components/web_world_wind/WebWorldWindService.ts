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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

///<reference path="../../../assets/typings/tsd.d.ts"/>
///<reference path="../../_all.ts"/>
///<reference path="../login_component/CredentialService.ts"/>
///<reference path="../wms_component/settings/SettingsService.ts"/>


module rasdaman {
    //Declare the WorldWind object so that typescript does not complain.
    declare let WorldWind:any;
    // NOTE: remember to register Service, Controller, Directive classes to app/src/app.ts
    // or it will have this error: $injector:unpr
    // https://docs.angularjs.org/error/$injector/unpr?p0=rasdaman.WebWorldWindServiceProvider%20%3C-%20rasdaman.WebWorldWindService
    export class WebWorldWindService {          
        
        public static $inject = [
            "$rootScope",                       
            "rasdaman.WMSSettingsService",
            "rasdaman.CredentialService"
        ];


        // Array of object for each WebWorldWind in each canvas (WCS GetCapabilities, DescribeCoverage, GetCoverage, WMS GetCapabilities, WMS DescribeLayer)        
        private webWorldWindModels: WebWorldWindModel[] = [];  

        private wmsSetting:WMSSettingsService = null;
        private authorizationToken:string = "";

        private SURFACE_POLYGONS_LAYER = "SURFACE_POLYGONS_LAYER";

        // Collects all WGS84 bboxes from geo-referenced coverages to be used in other WCS controllers
        public wcsGetCapabilitiesWGS84CoveageExtents:any[] = [];
        // Collects all WGS84 bboxes from geo-referenced coverages to be used in other WMS controllers
        public wmsGetCapabilitiesWGS84CoveageExtents:any[] = [];

        public constructor($rootScope:angular.IRootScopeService,                           
                           wmsSetting:rasdaman.WMSSettingsService,
                           credentialService:rasdaman.CredentialService) {            
            this.wmsSetting = wmsSetting;            
            this.authorizationToken = credentialService.getAuthorizationHeader(this.wmsSetting.wmsEndpoint)["Authorization"];            
        }

        /**
         Given a list of coverage extents / or layer extents and a coverage Id (layer name) -> return this coverage Id's extent in WGS84 CRS
         */
        public getCoveragesExtentByCoverageId(coverageExtents:any[], coverageId:string) {
            for (let i = 0; i < coverageExtents.length; i++) {
                let tmp = coverageExtents[i];
                if (tmp.coverageId === coverageId) {
                    return tmp;
                }
            }

            // CoverageExtent does not exist which means coverage cannot reproject to EPSG:4326
            return null;            
        }

        /**
         * Return a WebWorldWindModel object based on a canvas Id         
         */
        private getWebWorldWindModelByCanvasId(canvasId: string): WebWorldWindModel {            
            for (let i = 0; i < this.webWorldWindModels.length; i++) {
                if (this.webWorldWindModels[i].canvasId == canvasId) {
                    return this.webWorldWindModels[i];
                }
            }
            return null;
        }

        /**
         * A canvas (wwd) has only 1 surface polygons layer to draw polygons on it.
         */
        private getSurfacePolygonsLayer(canvasId:string):any {
            let webWorldWindModel:WebWorldWindModel = this.getWebWorldWindModelByCanvasId(canvasId);            
            for (let i = 0; i < webWorldWindModel.wwd.layers.length; i++) {
                let layer:any = webWorldWindModel.wwd.layers[i];
                if (layer.displayName == this.SURFACE_POLYGONS_LAYER) {
                    return layer;
                }
            }

            return null;            
        }

        /**
         * When a coverage is renamed, then find the polygon in surface image layer if exists with old coverage id, 
         * then update it to new coverage id         
         */
        public updateSurfacePolygonCoverageId(canvasId:string, oldCoverageId:string, newCoverageId:string):void {
            let polygonsLayer:any = this.getSurfacePolygonsLayer(canvasId);
            for (let i = 0; i < polygonsLayer.renderables.length; i++) {
                let polygonObj = polygonsLayer.renderables[i];
                if (polygonObj.coverageId == oldCoverageId) {
                    polygonObj.coverageId = newCoverageId;
                    return;
                }
            }
        }
        
        // Init the WebWorldWind on the canvasId HTML element
        public initWebWorldWind(canvasId: string) {
            let tmp:WebWorldWindModel = this.getWebWorldWindModelByCanvasId(canvasId);
            if (tmp != null) {
                // WebWorldWindModel already initialized, don't add it one more time
                return null;
            }

            // Create a WorldWindow for the canvas.                
            let wwd = new WorldWind.WorldWindow(canvasId);
            
            let wmsLayer = null;
            
            let layers = [
                {layer: new WorldWind.BMNGOneImageLayer(), enabled: true},
                {layer: new WorldWind.BingAerialWithLabelsLayer(null), enabled: true},
                {layer: new WorldWind.CompassLayer(), enabled: true},
                {layer: new WorldWind.CoordinatesDisplayLayer(wwd), enabled: true},
                {layer: new WorldWind.ViewControlsLayer(wwd), enabled: true}
            ];

            // Bing layers
            for (let i = 0; i < layers.length; i++) {
                layers[i].layer.enabled = layers[i].enabled;
                wwd.addLayer(layers[i].layer);
            }

            // Coverage's extent as a text when hovering mouse over
            let textLayer = new WorldWind.RenderableLayer("Screen Text");
            wwd.addLayer(textLayer);

            // Create a layer to hold the polygons.
            let surfaceImageLayer = new WorldWind.RenderableLayer(this.SURFACE_POLYGONS_LAYER);            
            // A layer contains a list of polygons (each polygon is a coverage's extent in WGS84 CRS)
            wwd.addLayer(surfaceImageLayer);

            // Listen for mouse moves and highlight the placemarks that the cursor rolls over.
            wwd.addEventListener("mousemove", (o) => {
                // Clear the displayed screen text
                textLayer.removeAllRenderables();
                let pickPoint = wwd.canvasCoordinates(o.clientX, o.clientY);                    
                let pickList = wwd.pick(pickPoint);
                if (pickList.objects.length > 0) {

                    let polygonObjs = this.getSurfacePolygonsLayer(canvasId).renderables;

                    for (let p = 0; p < pickList.objects.length; p++) {
                        let pickedObject = pickList.objects[p];

                        if (pickedObject.position != null) {

                            let lat = pickedObject.position.latitude;
                            let lon = pickedObject.position.longitude;

                            let intersectedCoverageIds = "";                           

                            let count = 0;
                            for (let i = 0; i < polygonObjs.length; i++) {
                                let coverageExtent = polygonObjs[i].coverageExtent;
                                let bbox = coverageExtent.bbox;
                                
                                
                                if (lon >= bbox.xmin && lon <= bbox.xmax
                                    && lat >= bbox.ymin && lat <= bbox.ymax) {
                                    if (count < 8) {
                                        let text = coverageExtent.coverageId 
                                            + " - with bbox: minLon=" + bbox.xmin.toFixed(2) + ", minLat=" + bbox.ymin.toFixed(2) 
                                            + ", maxLon=" + bbox.xmax.toFixed(2) + ", maxLat=" + bbox.ymax.toFixed(2);
                                        intersectedCoverageIds += text + " \n";
                                    }

                                    count += 1;
                                }

                            }

                            if (count >= 8) {
                                intersectedCoverageIds = " There are total: " + count + " intersecting objects. \n " 
                                                    + intersectedCoverageIds 
                                                    + " and more objects ... \n" ;
                            }        
                            
                            if (intersectedCoverageIds != "") {
                                let screenText = new WorldWind.ScreenText(
                                                        new WorldWind.Offset(WorldWind.OFFSET_FRACTION, 0.5, WorldWind.OFFSET_FRACTION, 0.5), 
                                                                            intersectedCoverageIds);
                                let textAttributes = new WorldWind.TextAttributes(null);
                                textAttributes.color = WorldWind.Color.YELLOW;
                                screenText.attributes = textAttributes;
                                
                                textLayer.addRenderable(screenText);
                            }

                            break;
                        }
                        
                    }
                }
            });

            // Now set up to handle highlighting.
            let highlightController = new WorldWind.HighlightController(wwd);  

            // Create a new WebWorldWindModel and add to the array
            let webWorldWindModel: WebWorldWindModel = {
                canvasId: canvasId,
                wwd: wwd,
                wmsLayer: wmsLayer
            }

            this.webWorldWindModels.push(webWorldWindModel);

            // Then return the WebWorldWindModel object to be used later
            return webWorldWindModel;
        }

        /**
         * Based on a coverageExtent, create a WebWorldWind polygon         
         */
        private createPolygonObj(coverageId:string, coverageExtent:any): any {
            let polygonAttributes = new WorldWind.ShapeAttributes(null);
            polygonAttributes.drawInterior = true;
            polygonAttributes.drawOutline = true;
            polygonAttributes.outlineColor = WorldWind.Color.BLUE;
            polygonAttributes.interiorColor = new WorldWind.Color(0, 1, 1, 0.1);
            polygonAttributes.applyLighting = true;

            // Create and assign the polygon's highlight attributes.
            let highlightAttributes = new WorldWind.ShapeAttributes(polygonAttributes);
            highlightAttributes.outlineColor = WorldWind.Color.RED;
            highlightAttributes.interiorColor = new WorldWind.Color(1, 1, 1, 0.1);        
                      
            let bbox = coverageExtent.bbox;
            // NOTE: by default, coverage extent is shown on globe
            coverageExtent.show = true;

            let xmin = bbox.xmin.toFixed(5);
            if (xmin < -180) {
                xmin = -180;
            }
            let ymin = bbox.ymin.toFixed(5);
            if (ymin < -90) {
                ymin = 90;
            }
            let xmax = bbox.xmax.toFixed(5);
            if (xmax > 180) {
                xmax = 180;
            }
            let ymax = bbox.ymax.toFixed(5);
            if (ymax > 90) {
                ymax = 90;
            }

            let boundaries:any[] = [];
            boundaries[0] = []; // outer boundary
            boundaries[0].push(new WorldWind.Location(ymin, xmin));
            boundaries[0].push(new WorldWind.Location(ymin, xmax));
            boundaries[0].push(new WorldWind.Location(ymax, xmax));
            boundaries[0].push(new WorldWind.Location(ymax, xmin));                                       

            let polygonObj = new WorldWind.SurfacePolygon(boundaries, polygonAttributes);
            // a made-up property to know this polygon belongs to a coverageId
            polygonObj.coverageId = coverageId;
            polygonObj.highlightAttributes = highlightAttributes;

            polygonObj.coverageExtent = coverageExtent;

            return polygonObj;
                      
        }

        /**
         * Used for WCS DescribeCoverage / GetCoverage and WCS DescribeLayer which only draws one polygon on a separate WebWorldWind
         */
        public showCoverageExtentOnGlobe(canvasId: string, coverageId:string, coverageExtent:any) {
            let webWorldWindModel:WebWorldWindModel = this.getWebWorldWindModelByCanvasId(canvasId);
            if (webWorldWindModel == null) {
                this.initWebWorldWind(canvasId);
            }

            let polygonsLayer = this.getSurfacePolygonsLayer(canvasId);
            // Then, remove all drawn polygons layers when described previous layers
            polygonsLayer.removeAllRenderables();

            // create a new polygon object for the polygon layer
            // Add the polygon to surface layer
            let polygonObj = this.createPolygonObj(coverageId, coverageExtent);
            polygonsLayer.addRenderable(polygonObj);   

            this.gotoCoverageExtentCenter(canvasId, coverageExtent);
        }

        /**
         * Show / hide a coverage extent of a coverage Id / layer name on the WebWorldWind globe.
         * NOTE: used only for WCS / WMS GetCapabilities via checkboxes
         * @param canvasId 
         * @param coverageId 
         * @param coverageExtent
         */
        public showHideCoverageExtentOnGlobe(canvasId: string, coverageId:string, coverageExtent:any) {     
            let webWorldWindModel:WebWorldWindModel = this.getWebWorldWindModelByCanvasId(canvasId);
            if (webWorldWindModel == null) {
                this.initWebWorldWind(canvasId);
            }            
            
            let polygonsLayer = this.getSurfacePolygonsLayer(canvasId);
            let foundIndex = -1;

            for (let i = 0; i < polygonsLayer.renderables.length; i++) {
                let polygonObj = polygonsLayer.renderables[i];
                if (polygonObj.coverageId == coverageId) {
                    // polygon already exists -> remove it
                    foundIndex = i;
                    break;
                }
            }
            
            if (foundIndex != -1) {
                // Remove the drawn polygon on surface layer
                polygonsLayer.renderables.splice(foundIndex, 1);                
                // In this case, consider it is used to remove the polygon from surface layers and just redraw WebWorldWind
                this.getWebWorldWindModelByCanvasId(canvasId).wwd.redraw();
            } else {           
                if (coverageExtent != null) {
                    // create a new polygon object for the new polygon layer
                    // Add the polygon to surface layer
                    let polygonObj = this.createPolygonObj(coverageId, coverageExtent);
                    polygonsLayer.addRenderable(polygonObj);                        

                    // then zoom to this coverage's extent                
                    // look at the showed/hided coverage extent's center and redraw webworldwind
                    this.gotoCoverageExtentCenter(canvasId, coverageExtent);            
                }
            }

        }


        // Go to the center of the first coverage extent of the input array on Globe
        public gotoCoverageExtentCenter(canvasId: string, coverageExtent: any) {
            let webWorldWindModel = this.getWebWorldWindModelByCanvasId(canvasId);
            let xcenter = (coverageExtent.bbox.xmin + coverageExtent.bbox.xmax) / 2;
            let ycenter = (coverageExtent.bbox.ymin + coverageExtent.bbox.ymax) / 2;
            let wwd = webWorldWindModel.wwd;            

            // NOTE: using wwd.goTo() will make the Globe hang up
            wwd.navigator.lookAtLocation = new WorldWind.Location(ycenter, xcenter);            
            wwd.redraw();                                                                   
        }        

        // When a coverage extent is showed/hided from user, update the show property to know coverageExtent is showed/hided
        private updateCoverageExtentShowProperty(coveragesExtentsArray:any, coverageId:string, value:boolean) {
            for (let i = 0; i < coveragesExtentsArray.length; i++) {
                if (coveragesExtentsArray[i].coverageId == coverageId) {
                    coveragesExtentsArray[i].show = value;
                    return;
                }                
            }
        }

        // ****************** WMS

        /**
         * Show the result of a GetMap request for a layer (coverage) with bands <=4 and dimensions = 2
         * @param canvasId 
         * @param coveragesExtentsArray 
         * @param getMapRequestURL 
         */


        private oldLayerName : string = '';

        public loadGetMapResultOnGlobe(canvasId: string, layerName: string, styleName: string, bbox: any, displayLayer: boolean, timeMoment: any, nonXYAxes: any) {

            // It uses the same canvasId for DescribeLayer
            let webWorldWindModel = this.getWebWorldWindModelByCanvasId(canvasId);

             // Init the WebWorldWindModel for the canvasId if it does not exist
            if (webWorldWindModel == null) {
                webWorldWindModel = this.initWebWorldWind(canvasId);
            }                        

            // NOTE: max bbox of EPSG:4326 which WebWorldWind supports is [-90,90,-180,180]
            let ymin = Math.max(-90, bbox.ymin);
            let ymax = Math.min(90, bbox.ymax);
            let xmin = Math.max(-180, bbox.xmin);
            let xmax = Math.min(180, bbox.xmax);

            let wwd = webWorldWindModel.wwd;
            let config = {
                    title: "WMS layer overview",
                    version: WMSSettingsService.version,
                    service: this.wmsSetting.wmsEndpoint,
                    layerNames: layerName,
                    sector: new WorldWind.Sector(ymin, ymax, xmin, xmax),
                    levelZeroDelta: new WorldWind.Location(36, 36),
                    numLevels: 15,
                    format: "image/png",
                    styleNames: styleName,
                    size: 256
                };
            
            // Prepare the property timeString to be passed to the WmsLayer consructor 
            let timeString;
            if(timeMoment != null) {
                timeString = '"' + timeMoment + '"';
            }
            else {
                timeString = null;
            }

            // Zoom at distance 1 km (to avoid loading full big coverage which causes server terminated due to not enough RAM)
            if(this.oldLayerName != layerName) {
                // default set zoom to 30 km
                wwd.navigator.range = 3000 * 1000;
                this.oldLayerName = layerName;
            }

            // Remove the rendered surface image layer and replace it with new layer
            wwd.removeLayer(webWorldWindModel.wmsLayer);
            let wmsLayer = new BAWmsLayer(config, timeString, this.authorizationToken, nonXYAxes);                        
            webWorldWindModel.wmsLayer = wmsLayer;     
            if (displayLayer) {
                // Should this Layer be displayed
                wwd.addLayer(wmsLayer);
            }

            // Reloads the WMS layer
            // https://forum.worldwindcentral.com/forum/web-world-wind/web-world-wind-help/17505-how-to-refresh-an-individual-wms-layer
            wmsLayer.refresh();
            wwd.redraw();
        }
    }  

    interface WebWorldWindModel {
        canvasId: string,
        wwd: any,
        wmsLayer: any,
    }
    
    export class BAWmsLayer extends WorldWind.WmsLayer {

        private authorizationHeader:string = "";
        // e.g. ["isobaric=25000", "height=3500"]
        private nonXYAxes: any;

        public constructor(config:{}, timeString:string, authorizationHeader:string, nonXYAxes: any) {
            super(config, timeString);
            this.authorizationHeader = authorizationHeader;
            this.nonXYAxes = nonXYAxes;  
        }

        // Inspire from https://github.com/NASAWorldWind/WebWorldWind/blob/5f1afa8a30c11a5de7d86cb246c93da72e9c125e/src/layer/TiledImageLayer.js#L471
        retrieveTileImage(dc, tile, suppressRedraw) {
           if (this.currentRetrievals.indexOf(tile.imagePath) < 0) {
               if (this.currentRetrievals.length > this.retrievalQueueSize) {
                   return;
               }

               if (this.absentResourceList.isResourceAbsent(tile.imagePath)) {
                   return;
               }
               let url = this.resourceUrlForTile(tile, this.retrievalImageFormat),
                   image = new Image(),
                   imagePath = tile.imagePath,
                   cache = dc.gpuResourceCache,
                   canvas = dc.currentGlContext.canvas,
                   layer = this;

                for (let key in this.nonXYAxes) {
                    url += "&" + this.nonXYAxes[key];
                }

                if (!url) {
                   this.currentTilesInvalid = true;
                   return;
               }
               image.onload = function () {
                   let texture = layer.createTexture(dc, tile, image);
                   layer.removeFromCurrentRetrievals(imagePath);
                   if (texture) {
                       cache.putResource(imagePath, texture, texture.size);
                       layer.currentTilesInvalid = true;
                       layer.absentResourceList.unmarkResourceAbsent(imagePath);
                       if (!suppressRedraw) {
                           // Send an event to request a redraw.
                           let e = document.createEvent('Event');
                           e.initEvent(WorldWind.REDRAW_EVENT_TYPE, true, true);
                           canvas.dispatchEvent(e);
                       }
                   }
               };
               image.onerror = function () {
                   layer.removeFromCurrentRetrievals(imagePath);
                   layer.absentResourceList.markResourceAbsent(imagePath);
               };
               this.currentRetrievals.push(imagePath);
               image.crossOrigin = this.crossOrigin;

               let xhr = new XMLHttpRequest();
               xhr.responseType = "arraybuffer";               
               xhr.onload = function () {
                       let blb = new Blob([xhr.response], { type: 'image/png' });
                       let url = (window.URL).createObjectURL(blb);
                       image.src = url;
               };
               xhr.open("GET", url, true);
               //here goes the authentication header
               xhr.setRequestHeader("Authorization", this.authorizationHeader);
               xhr.send()
           }
       };
   }
}
