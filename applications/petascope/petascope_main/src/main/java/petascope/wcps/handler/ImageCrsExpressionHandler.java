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
 * Copyright 2003 - 2018 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.util.CrsUtil;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;

/**
 * Translator class for imageCrs (CRS:1).  <code>
 * for c in (mr), d in (rgb) return imageCrs(c)
 * </code> translates to  <code>
 * Grid CRS (CRS:1)
 * </code>
 *
 * @author <a href="mailto:bphamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ImageCrsExpressionHandler extends Handler {
    
    public ImageCrsExpressionHandler() {
        
    }
    
    public ImageCrsExpressionHandler create(Handler coverageExpressionHandler) {
        ImageCrsExpressionHandler result = new ImageCrsExpressionHandler();
        result.setChildren(Arrays.asList(coverageExpressionHandler));
        return result;
    }

    public WcpsMetadataResult handle(List<Object> serviceRegistries) throws PetascopeException {
        WcpsResult coverageExpression = (WcpsResult) this.getFirstChild().handle(serviceRegistries);
        WcpsMetadataResult result = this.handle(coverageExpression);
        
        return result;
    }

    /**
     * Return the Rasql grid CRS of the coverage (CRS:1)
     */
    private WcpsMetadataResult handle(WcpsResult coverageExpression) {
        String imageCrsUri = CrsUtil.GRID_CRS;
        WcpsMetadataResult wcpsResult = new WcpsMetadataResult(coverageExpression.getMetadata(), imageCrsUri);
        return wcpsResult;
    }
}
