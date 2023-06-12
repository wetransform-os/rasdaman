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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.subset_axis.model.WKTCompoundPoints;
import petascope.wcps.subset_axis.model.WKTLineString;

/**
// Handle POLYGON LEFT_PARENTHESIS wktPointElementList RIGHT_PARENTHESIS
// e.g: POLYGON((20 30, 40 50), (60 70, 70 80))
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WKTLineStringHandler extends Handler {
    
    public WKTLineStringHandler() {
        
    }
    
    public WKTLineStringHandler create(Handler wktCompoundPointsHandler) {
        WKTLineStringHandler result = new WKTLineStringHandler();
        result.setChildren(Arrays.asList(wktCompoundPointsHandler));
        return result;
    }
    
    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        List<WKTCompoundPoints> wktCompoundPointsList = new ArrayList<>();
        
        WKTCompoundPoints wktCompoundPoints = (WKTCompoundPoints)this.getFirstChild().handle(serviceRegistries);
        wktCompoundPointsList.add(wktCompoundPoints);
        
        WKTLineString result = new WKTLineString(wktCompoundPointsList);
        return result;
    }
}
