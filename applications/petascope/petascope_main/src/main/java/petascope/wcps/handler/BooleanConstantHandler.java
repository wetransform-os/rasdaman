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
package petascope.wcps.handler;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsResult;

import java.util.List;

/**
 * Class to translate a boolean constant, e.g. true or false
 * <p/>
 * <code>
 * true
 * </code> translates to  <code>
 * true
 * </code>
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class BooleanConstantHandler extends Handler {
    
    private String value;
    
    public BooleanConstantHandler() {
        
    }
    
    public BooleanConstantHandler create(String value) {
        BooleanConstantHandler result = new BooleanConstantHandler();
        result.value = value;
        
        return result;
    }
    
    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        VisitorResult result = this.handle(this.value);
        return result;
    }

    private WcpsResult handle(String booleanValue) {
        return new WcpsResult(null, booleanValue);
    }


}
