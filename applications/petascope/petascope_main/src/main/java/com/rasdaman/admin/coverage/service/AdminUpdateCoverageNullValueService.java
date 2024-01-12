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
 * Copyright 2003 - 2024 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package com.rasdaman.admin.coverage.service;

import com.rasdaman.accesscontrol.service.AuthenticationService;
import com.rasdaman.admin.service.AbstractAdminService;
import org.rasdaman.domain.cis.*;
import org.rasdaman.repository.service.CoveragePyramidRepositoryService;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.rasdaman.repository.service.WMSRepostioryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.controller.AbstractController;
import petascope.controller.PetascopeController;
import petascope.core.KVPSymbols;
import petascope.core.Pair;
import petascope.core.response.Response;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.MIMEUtil;
import petascope.util.SetUtil;
import petascope.util.StringUtil;
import petascope.util.XMLUtil;
import petascope.util.ras.RasUtil;
import petascope.util.ras.TypeRegistry;
import petascope.util.ras.TypeResolverUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static petascope.core.KVPSymbols.*;

/**
 * Service to handle update coverage's (and its corresponding rasdaman collection) null values
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class AdminUpdateCoverageNullValueService extends AbstractAdminService {

    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private PetascopeController petascopeController;

    private static Logger log = LoggerFactory.getLogger(AdminUpdateCoverageNullValueService.class);

    private static Set<String> VALID_PARAMETERS = SetUtil.createLowercaseHashSet(KEY_COVERAGE_ID, KVPSymbols.KEY_NULL_VALUES);

    public AdminUpdateCoverageNullValueService() {

    }

    private void validate(Map<String, String[]> kvpParameters) throws PetascopeException {
        this.validateRequiredParameters(kvpParameters, VALID_PARAMETERS);
    }

    @Override
    public Response handle(HttpServletRequest httpServletRequest, Map<String, String[]> kvpParameters) throws Exception {
        this.validate(kvpParameters);

        String coverageId = AbstractController.getValueByKey(kvpParameters, KEY_COVERAGE_ID);

        // e.g. [25, 35, 35] for all bands or { [5], [35:35], [6] } for 3 bands (null value per band is different)
        String nullValues = AbstractController.getValueByKey(kvpParameters, KEY_NULL_VALUES);

        // Then, change the current null values of the coverage and save it to database
        // e.g. element1 is 23:35,35:40,56, element2 is 35 (different null values per band)
        List<String> parsedNullValuesTmp = StringUtil.extractStringsBetweenSquareBrackets(nullValues);

        Coverage coverage = this.coverageRepositoryService.readCoverageByIdFromDatabase(coverageId);
        Pair<String, String> credentials = AuthenticationService.getBasicAuthCredentialsOrRasguest(this.httpServletRequest);


        // First to create a new rasdaman set type
        String mddArrayType = coverage.getRasdamanRangeSet().getMddType();
        String newRasdamanSetType = StringUtil.addDateTimeSuffix(coverageId);
        String query = "CREATE TYPE " + newRasdamanSetType + " AS SET (" + mddArrayType + " NULL VALUES " + nullValues + " )";
        RasUtil.executeRasqlQuery(query, credentials.fst, credentials.snd, true);

        // Then, change the set type of the rasdaman collection
        query = "ALTER collection " + coverage.getRasdamanRangeSet().getCollectionName() + " SET TYPE " + newRasdamanSetType;
        RasUtil.executeRasqlQuery(query, credentials.fst, credentials.snd, true);

        // Then, update caches
        TypeRegistry.setTypeDefinitions.put(newRasdamanSetType, mddArrayType);
        TypeRegistry.setTypeNullValues.put(newRasdamanSetType, parsedNullValuesTmp);


        int i = 0;
        for (Field field : coverage.getRangeType().getDataRecord().getFields()) {
            List<NilValue> newNilValues = new ArrayList<>();

            if (parsedNullValuesTmp.size() == 1) {
                // all bands have the same null values
                // e.g. [23, 35:55, 65]
                addNilValuesToInputList(newNilValues, parsedNullValuesTmp.get(0));
                field.getQuantity().setNilValues(newNilValues);
            } else {
                // each band has different null value
                // e.g. { [23, 35:55, 65], [35:36], ...} then band1 has: [23, 35:55, 65] as null values
                if (i < parsedNullValuesTmp.size()) {
                    addNilValuesToInputList(newNilValues, parsedNullValuesTmp.get(i));
                }
                field.getQuantity().setNilValues(newNilValues);
            }

            i++;
        }

        // then persist the new null values to database
        coverageRepositoryService.save(coverage);
        
        Response result = new Response(Arrays.asList("".getBytes()), MIMEUtil.MIME_TEXT);
        return result;
    }

    private static void addNilValuesToInputList(List<NilValue> nilValues, String parsedNullValuesTmp) {
        String[] tmps = parsedNullValuesTmp.split(", ");
        for (String tmp : tmps) {
            nilValues.add(new NilValue(tmp.strip()));
        }
    }

}
