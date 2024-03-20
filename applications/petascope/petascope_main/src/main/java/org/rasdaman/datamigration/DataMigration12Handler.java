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
package org.rasdaman.datamigration;

import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.domain.cis.EnvelopeByAxis;
import org.rasdaman.domain.cis.RasdamanRangeSet;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;

import java.util.List;
import java.util.Map;

/**
 * Class to handle data migration version number 12
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class DataMigration12Handler extends AbstractDataMigrationHandler {

    @Autowired
    private CoverageRepositoryService coverageRepositoryService;

    public DataMigration12Handler() {
        // NOTE: update this by one for new handler class
        this.migrationVersion = 12;
        this.handlerId = "14ea23b0-4179-11ee-adb8-074906d78675";
    }

    @Override
    public void migrate() throws PetascopeException, SecoreException {

        List<Pair<Coverage, Boolean>> pairsList = this.coverageRepositoryService.readAllLocalCoveragesBasicMetatata();

        for (Pair<Coverage, Boolean> pair : pairsList) {
            final Coverage localCoverage = pair.fst;

            EnvelopeByAxis envelopeByAxis = localCoverage.getEnvelope().getEnvelopeByAxis();
            if (envelopeByAxis.getWgs84BBox() != null) {
                // Update existing coverage's wgs84 bbox as it is wrong for pyramid member coverages
                this.coverageRepositoryService.createCoverageExtent(localCoverage);

                // Then persist the WGS84 coverage extent to database
                this.coverageRepositoryService.saveWgs84BBox(envelopeByAxis);
            }
        }
    }

}