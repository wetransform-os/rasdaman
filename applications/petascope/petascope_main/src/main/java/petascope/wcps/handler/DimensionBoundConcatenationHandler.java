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
 * Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rasdaman.accesscontrol.service.AuthenticationService;
import org.apache.commons.lang3.StringUtils;
import org.rasdaman.config.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.util.BigDecimalUtil;
import petascope.util.ListUtil;
import petascope.util.StringUtil;
import petascope.util.TimeUtil;
import petascope.util.ras.RasUtil;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcps.subset_axis.model.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Handler for expression
 // coverageExpression  ( DOT coverageExpression ) *
 // e.g. ansi($month . "-01-" . "02")
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DimensionBoundConcatenationHandler extends Handler {

    @Autowired
    HttpServletRequest httpServletRequest;

    public DimensionBoundConcatenationHandler() {

    }

    public DimensionBoundConcatenationHandler create(List<Handler> handlers) {
        DimensionBoundConcatenationHandler result = new DimensionBoundConcatenationHandler();
        result.setChildren(handlers);
        result.httpServletRequest = this.httpServletRequest;

        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {

        List<String> resultTmps = new ArrayList<>();

        for (Handler childHandler : this.getChildren()) {
            VisitorResult visitorResult = childHandler.handle(serviceRegistries);
            String tmp = visitorResult.getResult();

            if (visitorResult instanceof ListStringResult) {
                // e.g. tmp = "2023-01", "2023-02", ...  from allmonths("2023")
                return visitorResult;
            } else {
                if (this.getChildren().size() > 1
                        && StringUtil.stripQuotes(tmp).toUpperCase().startsWith(TimeUtil.PERIODICITY_KEYWORD)) {
                    // e.g. "2015-01" . "P1M"
                    tmp = " " + tmp;
                }

                if (!StringUtil.startsWithQuote(tmp) && !(childHandler instanceof StringScalarHandler) && !BigDecimalUtil.isNumber(tmp)) {
                    try {
                        // e.g. 1 + 3
                        tmp = StringUtil.eval(tmp);
                    } catch (Exception ex) {
                        // Then try with rasql, e.g. ((long)(3 / 5))
                        String query = "SELECT " + tmp;
                        Pair<String, String> credentials = AuthenticationService.getRasUserCredentials(this.httpServletRequest);
                        tmp = RasUtil.executeQueryToReturnString(query, credentials.fst, credentials.snd);
                    }
                }
            }
            resultTmps.add(tmp);
        }

        String bound = ListUtil.join(resultTmps, "");
        VisitorResult result = new BoundResult(bound);
        return result;
    }


}
