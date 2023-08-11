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
package petascope.wcs2.handlers.kvp.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import static petascope.controller.AbstractController.getValuesByKeyAllowNull;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCSException;
import petascope.core.KVPSymbols;
import static petascope.core.KVPSymbols.KEY_INTERPOLATION;
import static petascope.core.gml.GMLGetCapabilitiesBuilder.INTERPOLATION_NEAR;
import static petascope.core.gml.GMLGetCapabilitiesBuilder.OGC_CITE_INTEPOLATION_NEAR;
import petascope.exceptions.PetascopeException;
import petascope.util.BigDecimalUtil;
import petascope.util.CrsUtil;
import petascope.util.ListUtil;
import petascope.util.StringUtil;
import petascope.wcps.exception.processing.ScaleValueLessThanZeroException;

/**
 * Service class for Scaling handler of GetCoverageKVP class
 *
 @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
public class KVPWCSGetCoverageScalingService {
    
    // NOTE: rasdaman only supports scale() by nearest neighbor up to now
    public static final Set<String> SUPPORTED_SCALING_AXIS_INTERPOLATIONS = new HashSet<>();
    public static final String NEAREST_INTERPOLATION = "nearest-neighbor";
    private static final String GDAL_NEAREST_INTERPOLATION = "near";
    
    static {
        SUPPORTED_SCALING_AXIS_INTERPOLATIONS.add(OGC_CITE_INTEPOLATION_NEAR);
        SUPPORTED_SCALING_AXIS_INTERPOLATIONS.add(INTERPOLATION_NEAR);
        SUPPORTED_SCALING_AXIS_INTERPOLATIONS.add(NEAREST_INTERPOLATION);
        SUPPORTED_SCALING_AXIS_INTERPOLATIONS.add(GDAL_NEAREST_INTERPOLATION);
    }

    public KVPWCSGetCoverageScalingService() {

    }

    /**
     * Depend on which type of scale to generate correct WCPS query e.g:
     * scalefactor=0.5 -> scalefactor(c, 0.5)
     *
     * @param queryContent
     * @param kvpParameters
     * @return
     * @throws petascope.exceptions.WCSException
     */
    public String handleScaleExtension(String queryContent, Map<String, String[]> kvpParameters) throws WCSException, PetascopeException {
        // Validate first as no mixing scale parameters (e.g: scalefactor=2&scaleaxes=Lat(0.5),Long(4.3)
        // but scaleaxes=Lat(0.5)&scaleaxes=Long(0.5) is ok
        Set<String> scaleParameters = new HashSet<>();
        for (Map.Entry<String, String[]> entry : kvpParameters.entrySet()) {
            if (entry.getKey().toLowerCase().contains(KVPSymbols.KEY_SCALE_PREFIX)) {
                scaleParameters.add(entry.getKey().toLowerCase());
            }
        }

        if (scaleParameters.size() > 1) {
            throw new WCSException(ExceptionCode.InvalidRequest, "Multiple scaling parameters in the request: must be unique.");
        }

        if (kvpParameters.containsKey(KVPSymbols.KEY_SCALEFACTOR)) {
            // e.g: scaleFactor=3 then all axes will upscaled by 3 (e.g: Lat axis has 100 pixels, then the ouput of Lat axis is 100 * 3 = 300)
            String scaleFactor = kvpParameters.get(KVPSymbols.KEY_SCALEFACTOR)[0];
            queryContent = KVPSymbols.KEY_SCALE_PREFIX + "( " + queryContent + ", { " + scaleFactor + " } )";

        } else if (kvpParameters.containsKey(KVPSymbols.KEY_SCALEAXES)) {
            // scaleAxes is the extension of scaleFactor
            // e.g: scaleAxes=Lat(5)&scaleAxes=Long(0.3) then (e.g: Lat axis has 100 pixels, then the output is 100 * 5 pixels, Long has 100 pixels, then the output is 100 * 0.3 pixels)
            String[] scaleAxesParams = kvpParameters.get(KVPSymbols.KEY_SCALEAXES);
            List<String> scaleAxes = new ArrayList<>();
            for (String scaleAxisParam : scaleAxesParams) {
                scaleAxes.add(scaleAxisParam);
            }
            queryContent = KVPSymbols.KEY_SCALE_PREFIX + "( " + queryContent + ", {" + ListUtil.join(scaleAxes, ", ") + "} )";
        } else if (kvpParameters.containsKey(KVPSymbols.KEY_SCALESIZE)) {
            // scaleSize is the extension of scaleExtent
            // e.g: scaleSize=Lat(5)&scaleSize=Long(3) then (e.g: Lat has 100 pixels, then the output is 5 pixels, Long has 100 pixels, then the output is 3 pixels)
            String[] scaleSizeParams = kvpParameters.get(KVPSymbols.KEY_SCALESIZE);
            List<String> scaleSizes = new ArrayList<>();
            for (String scaleSizeParam : scaleSizeParams) {
                // e.g. scaleSize=Lat(30),Long(500)
                String[] parts = scaleSizeParam.split(",");
                for (String part : parts) {
                    int index = part.indexOf("(");
                    String axisLabel = part.substring(0, index);
                    // e.g. (30): target is 30 grid pixels
                    int size = BigDecimalUtil.parseDecimalFromString(StringUtil.stripParentheses(part.substring(index))).intValue();

                    if (size <= 0) {
                        throw new ScaleValueLessThanZeroException(axisLabel, String.valueOf(size));
                    }

                    // e.g. Lat:"CRS:1"(20,30) then output of Lat is scaled to [25:30] grid domains
                    String gridCRSExtent = axisLabel + ":\"" + CrsUtil.GRID_CRS + "\"(0:" + (size - 1) + ")";
                    scaleSizes.add(gridCRSExtent);
                }
            }
            queryContent = KVPSymbols.KEY_SCALE_PREFIX + "( " + queryContent + ", {" + ListUtil.join(scaleSizes, ", ") + "} )";
        } else if (kvpParameters.containsKey(KVPSymbols.KEY_SCALEEXTENT)) {
            // e.g: scaleExtent=Lat(5:10)&scaleExtent=Long(3:30) then (e.g: Lat has 100 pixels, then the output is 5:10 pixels, Long has 100 pixels, then the output is 3:30 pixels)
            String[] scaleExtentParams = kvpParameters.get(KVPSymbols.KEY_SCALEEXTENT);
            List<String> scaleExtents = new ArrayList<>();
            for (String scaleExtentParam : scaleExtentParams) {
                // e.g. scaleExtent=Lat(10:20),Lon(15:20)
                String[] parts = scaleExtentParam.split(",");
                for (String part : parts) {
                    int index = part.indexOf("(");
                    String axisLabel = part.substring(0, index);
                    // e.g. (20,30): target is 11 grid pixels
                    String tuple = part.substring(index).replace(",", ":");

                    // e.g. Lat:"CRS:1"(20,30) then output of Lat is scaled to [25:30] grid domains
                    String gridCRSExtent = axisLabel + ":\"" + CrsUtil.GRID_CRS + "\"" + tuple;
                    scaleExtents.add(gridCRSExtent);
                }
            }
            queryContent = KVPSymbols.KEY_SCALE_PREFIX + "( " + queryContent + ", { " + ListUtil.join(scaleExtents, ", ") + "} )";
        }
        
        if (!scaleParameters.isEmpty()) {
            // NOTE: scale() in radaman only supports nearest neighbor
            String[] interpolations = getValuesByKeyAllowNull(kvpParameters, KEY_INTERPOLATION);
            if (interpolations != null) {
                String interpolation = interpolations[0];
                if (!this.SUPPORTED_SCALING_AXIS_INTERPOLATIONS.contains(interpolation)) {
                    throw new WCSException(ExceptionCode.InterpolationMethodNotSupported);
                }
            }
        }

        return queryContent;
    }
}
