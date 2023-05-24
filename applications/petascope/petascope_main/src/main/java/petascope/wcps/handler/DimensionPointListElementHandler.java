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
 * Copyright 2003 - 2022 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.subset_axis.model.DimensionIntervalList;
import petascope.wcps.subset_axis.model.WcpsSubsetDimension;

/**
// dimensionPointElement (COMMA dimensionPointElement)*
// e.g: i(0), j(0) - List of Slicing points
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DimensionPointListElementHandler extends Handler {
    
    public DimensionPointListElementHandler() {
        
    }
    
    public DimensionPointListElementHandler create(List<Handler> childHandlers) {
        DimensionPointListElementHandler result = new DimensionPointListElementHandler();
        result.setChildren(childHandlers);
        
        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // dimensionPointElement (COMMA dimensionPointElement)*
        // e.g: i(0), j(0) - List of Slicing points
        List<WcpsSubsetDimension> intervalList = new ArrayList<>();
        for (Handler childHandler : this.getChildren()) {
            intervalList.add((WcpsSubsetDimension) childHandler.handle(serviceRegistries));
        }

        DimensionIntervalList dimensionIntervalList = new DimensionIntervalList(intervalList);
        return dimensionIntervalList;
    }

}
