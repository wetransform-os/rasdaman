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
 * Copyright 2003 - 2021 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package com.rasdaman.admin.layer.style.service;

import com.rasdaman.admin.service.AbstractAdminService;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import nu.xom.Builder;
import org.rasdaman.config.ConfigManager;
import org.rasdaman.domain.wms.Layer;
import org.rasdaman.domain.wms.LegendURL;
import org.rasdaman.domain.wms.Style;
import org.rasdaman.repository.service.WMSRepostioryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.controller.AbstractController;
import petascope.controller.PetascopeController;
import static petascope.core.KVPSymbols.KEY_COVERAGE_ID;
import static petascope.core.KVPSymbols.KEY_NEW_STYLE_ID;
import static petascope.core.KVPSymbols.KEY_STYLE_ID;
import static petascope.core.KVPSymbols.KEY_WMS_ABSTRACT;
import static petascope.core.KVPSymbols.KEY_WMS_COLOR_TABLE_DEFINITION;
import static petascope.core.KVPSymbols.KEY_WMS_COLOR_TABLE_TYPE;
import static petascope.core.KVPSymbols.KEY_WMS_DEFAULT_STYLE;
import static petascope.core.KVPSymbols.KEY_WMS_LEGEND_GRAPHIC;
import static petascope.core.KVPSymbols.KEY_WMS_RASQL_TRANSFORM_FRAGMENT;
import static petascope.core.KVPSymbols.KEY_WMS_TITLE;
import static petascope.core.KVPSymbols.KEY_WMS_WCPS_QUERY_FRAGMENT;
import petascope.core.response.Response;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.JSONUtil;
import petascope.util.MIMEUtil;
import petascope.util.SetUtil;
import petascope.util.XMLUtil;
import petascope.wms.handlers.service.WMSGetMapCachingService;
import petascope.exceptions.WMSException;

/**
 * Service to create or update a style of a layer
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class AdminCreateOrUpdateStyleService extends AbstractAdminService {
    
    private static Logger log = LoggerFactory.getLogger(AdminCreateOrUpdateStyleService.class);
    
    private static Set<String> VALID_PARAMETERS = SetUtil.createLowercaseHashSet(KEY_COVERAGE_ID, KEY_STYLE_ID, KEY_NEW_STYLE_ID,
                                                                                KEY_WMS_TITLE, KEY_WMS_ABSTRACT,
                                                                                KEY_WMS_WCPS_QUERY_FRAGMENT, KEY_WMS_RASQL_TRANSFORM_FRAGMENT,
                                                                                KEY_WMS_COLOR_TABLE_TYPE, KEY_WMS_COLOR_TABLE_DEFINITION,
                                                                                KEY_WMS_DEFAULT_STYLE, KEY_WMS_LEGEND_GRAPHIC
                                                                                );
    
    @Autowired
    private WMSRepostioryService wmsRepostioryService;
    @Autowired
    private WMSGetMapCachingService wmsGetMapCachingService;
    
    public AdminCreateOrUpdateStyleService() {

    }
    
    private void validate(Map<String, String[]> kvpParameters) throws PetascopeException {
        this.validateRequiredParameters(kvpParameters, VALID_PARAMETERS);
    }
    
    /**
     * Handle add a new style to an existing layer
     */
    public Response handleAdd(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException, Exception {
        String layerName = AbstractController.getValueByKey(kvpParameters, KEY_COVERAGE_ID);
        String styleName = AbstractController.getValueByKey(kvpParameters, KEY_STYLE_ID);
        
        Layer layer = this.wmsRepostioryService.readLayerByNameFromLocalCache(layerName);
        if (layer == null) {
            throw new WMSException(ExceptionCode.NoSuchLayer, "Layer '" + layerName + "' does not exist in local database.");
        } else if (layer.hasStyle(styleName)) {
            throw new WMSException(ExceptionCode.InvalidRequest, "Style '" + styleName + "' already exists in layer '" + layerName + "'.");
        }
        
        return this.handle(httpServletRequest, kvpParameters);
    }
    
    /**
     * Handle update a new style to an existing layer
     */
    public Response handleUpdate(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws PetascopeException, Exception {
        String layerName = AbstractController.getValueByKey(kvpParameters, KEY_COVERAGE_ID);
        String styleName = AbstractController.getValueByKey(kvpParameters, KEY_STYLE_ID);
        String newStyleName = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_NEW_STYLE_ID);
        
        Layer layer = this.wmsRepostioryService.readLayerByNameFromLocalCache(layerName);
        if (layer == null) {
            throw new WMSException(ExceptionCode.NoSuchLayer, "Layer '" + layerName + "' does not exist in local database.");
        } else if (!layer.hasStyle(styleName)) {
            throw new WMSException(ExceptionCode.InvalidRequest, "Style '" + styleName + "' does not exist in layer '" + layerName + "'.");
        } else if (newStyleName != null && layer.getStyle(newStyleName) != null) {
            throw new WMSException(ExceptionCode.InvalidRequest, "New style '" + newStyleName + "' already exists in layer '" + layerName + "'.");
        }
        
        return this.handle(httpServletRequest, kvpParameters);
    }
    
    @Override
    public Response handle(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws Exception {
        
        this.validate(kvpParameters);
        
        // Check if layer does exist in database
        String layerName = AbstractController.getValueByKey(kvpParameters, KEY_COVERAGE_ID);
        String styleName = AbstractController.getValueByKey(kvpParameters, KEY_STYLE_ID);
        
        String newStyleName = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_NEW_STYLE_ID);
        
        boolean styleExist = false;
        Layer layer = this.wmsRepostioryService.readLayerByNameFromLocalCache(layerName);
        if (layer.getStyle(styleName) != null) {
            styleExist = true;
        }
        
        // NOTE: if title param does not exist, title is as same as name
        String styleTitle = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_TITLE);
        if (styleTitle == null) {
            styleTitle = styleName;
        }
        String styleAbstract = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_ABSTRACT);
        if (styleAbstract == null) {
            styleAbstract = "";
        }
        
        layer = this.wmsRepostioryService.readLayerByNameFromDatabase(layerName);
        
        Style style = null;
        if (!styleExist) {
            // create a new style to insert
            style = new Style();
            layer.getStyles().add(style);
        } else {
            // use the existing style to update
            style = layer.getStyle(styleName);
        }

        style.setName(styleName);
        
        // Rename an existing style by a new name via styleId request parameter
        if (styleExist && newStyleName != null) {
            style.setName(newStyleName);
            if (style.getTitle().equals(styleName)) {
                style.setTitle(newStyleName);
            }
        }
        
        style.setTitle(styleTitle);
        style.setStyleAbstract(styleAbstract);
        
        // NOTE: A style must have a value for wcpsQueryFragment or rasqlTransformFragment
        String wcpsQueryFragment = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_WCPS_QUERY_FRAGMENT);
        String rasqlQueryFragment = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_RASQL_TRANSFORM_FRAGMENT);
        
        if (wcpsQueryFragment != null) {
            style.setWcpsQueryFragment(wcpsQueryFragment);
            style.setRasqlQueryFragment(null);
        } else if (rasqlQueryFragment != null) {
            style.setRasqlQueryFragment(rasqlQueryFragment);
            style.setWcpsQueryFragment(null);
        }
        
        String colorTableType = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_COLOR_TABLE_TYPE);
        if (colorTableType != null) {
            // e.g: GDAL
            if (colorTableType.equals("none")) {
                style.setColorTableType(null);
                style.setColorTableDefinition(null);
            } else {
                byte colorTableTypeCode = org.rasdaman.domain.wms.Style.ColorTableType.getTypeCode(colorTableType);
                style.setColorTableType(colorTableTypeCode);
            }
        }
        
        String colorTableDefinition = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_COLOR_TABLE_DEFINITION);
        if (colorTableDefinition != null) {
            if (colorTableType.equalsIgnoreCase(Style.ColorTableType.SLD.toString())
                && XMLUtil.isXmlString(colorTableDefinition)) {
                // SLD XML
                try {
                    new Builder().build(new StringReader(colorTableDefinition)).getRootElement();
                } catch (Exception ex) {
                    throw new WMSException(ExceptionCode.InvalidRequest, 
                                                 "The provided SLD text is not valid XML format for style '" + styleName + "' of layer '" + layerName + "'"
                                                + ". Reason: " + XMLUtil.enquoteCDATA(ex.getMessage()), ex);
                }
            } else if (colorTableType.equalsIgnoreCase(Style.ColorTableType.ColorMap.toString())
                    && !JSONUtil.isJsonValid(colorTableDefinition)) {
                // ColorMap JSON
                throw new WMSException(ExceptionCode.InvalidRequest,
                        "The provided color table definition text is not valid JSON format for style '" + styleName + "' of layer '" + layerName + "'.");
            }
            style.setColorTableDefinition(colorTableDefinition);
        }
        
        String legendGraphicBase64 = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_LEGEND_GRAPHIC);
        if (legendGraphicBase64 != null) {
            if (legendGraphicBase64.trim().isEmpty()) {
                // LEGENDGRAPHIC=& -> remove the existing legendgraphic
                style.setLegendURL(null);
            } else {
                // get MIME from the base64 (e.g. image/png)
                if (!(legendGraphicBase64.startsWith("data:image/png") || legendGraphicBase64.startsWith("data:image/jpeg"))) {
                    throw new WMSException(ExceptionCode.InvalidRequest, 
                                 "Parameter " + KEY_WMS_LEGEND_GRAPHIC + " requires PNG/JPEG image encoded in base64 string."); 
                }

                // data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAATEAAAB+CA....
                String format = legendGraphicBase64.split(";")[0].split("data:")[1];
                String onlineResourceURL = "service=WMS&request=GetLegendGraphic&format=" + format + "&layer=" + layerName + "&style=" + styleName;
                LegendURL legendURL = new LegendURL(format, legendGraphicBase64, onlineResourceURL);
                if (style.getLegendURL() != null) {
                    // updating existing object
                    style.getLegendURL().setFormat(format);
                    style.getLegendURL().setLegendGraphicBase64(legendGraphicBase64);
                    style.getLegendURL().setOnlineResourceURL(onlineResourceURL);
                } else {
                    // create new object
                    style.setLegendURL(legendURL);
                }
            }
        }
              
        String defaultStyleTmp = AbstractController.getValueByKeyAllowNull(kvpParameters, KEY_WMS_DEFAULT_STYLE);
        if (defaultStyleTmp != null) {
            boolean defaultStyle = Boolean.valueOf(defaultStyleTmp);
            if (defaultStyle == true) {
                // admin explicitly sets this style as default style, then other styles must not be default style
                for (Style styleTmp : layer.getStyles()) {
                    if (styleTmp.getName().equals(style.getName())) {
                        styleTmp.setDefaultStyle(true);
                    } else {
                        styleTmp.setDefaultStyle(false);
                    }
                }
                
                // Then remove the GetMap request which contains layers and no style from cache as there is a new style set to default
                this.wmsGetMapCachingService.removeStyleGetMapInCache(layerName, null);
            } else {
                // style is unset as default style, then the first style is set as default style
                if (layer.isDefaultStyle(style)) {
                    style.setDefaultStyle(false);
                    layer.getStyles().get(0).setDefaultStyle(true);
                    
                    // Then remove the GetMap request which contains layers and no style from cache as there is a new style set to default
                    this.wmsGetMapCachingService.removeStyleGetMapInCache(layerName, null);
                }
            }
        }
        
        // Then update the layer with the new updated/added style to database.
        this.wmsRepostioryService.saveLayer(layer);
        log.info("WMS Style '" + style.getName() + "' is persisted in database.");

        if (styleExist) {
            // Remove all the cached GetMap responses from cache as style is updated
            this.wmsGetMapCachingService.removeStyleGetMapInCache(layerName, styleName);
        }
        
        Response result = new Response(Arrays.asList("".getBytes()), MIMEUtil.MIME_TEXT);
        return result;
    }
    
}
