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
 * Copyright 2003 - 2020 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;
import petascope.wcps.metadata.service.TempCoverageRegistry;
import petascope.wcps.metadata.service.WcpsCoverageMetadataTranslator;
import petascope.wcps.result.WcpsResult;

/**
 * Class to handle decode($1, "extraparameter") operator.
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DecodeCoverageHandler extends Handler {
    
    private org.slf4j.Logger log = LoggerFactory.getLogger(DecodeCoverageHandler.class);
    
    @Autowired
    private TempCoverageRegistry tempCoverageRegistry;
    @Autowired
    private WcpsCoverageMetadataTranslator wcpsCoverageMetadataTranslator;
    
    public DecodeCoverageHandler() {
        
    }
    
    public DecodeCoverageHandler create(Handler positionalParamaterHandler, Handler extraParamsHandler) {
        DecodeCoverageHandler result = new DecodeCoverageHandler();
        result.tempCoverageRegistry = tempCoverageRegistry;
        result.wcpsCoverageMetadataTranslator = wcpsCoverageMetadataTranslator;
        result.setChildren(Arrays.asList(positionalParamaterHandler, extraParamsHandler));
        
        return result;
    }
    
    @Override
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        String positionalParamater = ((WcpsResult)this.getFirstChild().handle(serviceRegistries)).getRasql();
        String extraParams = ((WcpsResult)this.getSecondChild().handle(serviceRegistries)).getRasql();
        
        WcpsResult result = this.handle(positionalParamater, extraParams);
        return result;
    }

    private WcpsResult handle(String positionalParamater, String extraParams) throws PetascopeException {
        
        // e.g: TEMP_COV_abc_2020011001
        WcpsCoverageMetadata metadata = null;
        String rasql = "";
        
        String tempCoverageId = this.tempCoverageRegistry.getTempCoverageId(positionalParamater);
        if (tempCoverageId != null) {
            metadata = this.wcpsCoverageMetadataTranslator.translate(tempCoverageId);
            // e.g: decode(<[0:0] 1c>, "GDAL", "{\"filePaths\":[\"mean_summer_airtemp.tif\"]}"))
            rasql = metadata.getRasdamanCollectionName();
        }
        
        WcpsResult wcpsResult = new WcpsResult(metadata, rasql);
        return wcpsResult;        
    }

}
