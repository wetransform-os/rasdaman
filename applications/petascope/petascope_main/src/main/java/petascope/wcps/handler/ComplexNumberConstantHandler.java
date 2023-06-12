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

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.wcps.result.WcpsResult;

/**
 * Translator class for complex numbers.
 *
 * <code>
 *   (2,4)
 * </code> translates to  <code>
 *   complex(2,5)
 * </code>
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ComplexNumberConstantHandler extends Handler {
    
    public ComplexNumberConstantHandler() {
        
    }
    
    public ComplexNumberConstantHandler create(StringScalarHandler realPartHandler, StringScalarHandler imaginePartHandler) {
        ComplexNumberConstantHandler result = new ComplexNumberConstantHandler();
        result.setChildren(Arrays.asList(realPartHandler, imaginePartHandler));
        return result;
    }
    
    public WcpsResult handle(List<Object> serviceRegistries) throws PetascopeException {
        String re = ((WcpsResult)this.getFirstChild().handle(serviceRegistries)).getRasql();
        String im = ((WcpsResult)this.getSecondChild().handle(serviceRegistries)).getRasql();
        
        WcpsResult result = this.handle(re, im);
        return result;
    }

    private WcpsResult handle(String re, String im) {
        return new WcpsResult(null, TEMPLATE.replace("$re", re).replace("$im", im));
    }

    private final String TEMPLATE = "complex($re, $im)";

}
