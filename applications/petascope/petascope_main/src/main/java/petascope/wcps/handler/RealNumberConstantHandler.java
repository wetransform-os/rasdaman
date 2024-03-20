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
import petascope.wcps.result.WcpsResult;

import java.util.List;

/**
 * Translator class for real numbers. The numbers in WCPS correspond to their
 * definition in rasql so no translation to a number format is done, the string
 * is passed upwards.
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RealNumberConstantHandler extends Handler {
    
    private String number;
    
    public RealNumberConstantHandler() {
        
    }
    
    public RealNumberConstantHandler create(String number) {
        RealNumberConstantHandler result = new RealNumberConstantHandler();
        result.number = number;
        return result;
    }
    
    public WcpsResult handle(List<Object> serviceRegistries) {
        WcpsResult wcpsResult = this.handle(this.number);
        return wcpsResult;
    }

    public String getNumber() {
        return number;
    }

    private WcpsResult handle(String number) {
        return new WcpsResult(null, number);
    }

}
