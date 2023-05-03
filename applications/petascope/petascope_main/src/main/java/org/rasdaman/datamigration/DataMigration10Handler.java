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
import org.rasdaman.admin.pyramid.service.PyramidService;
import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.domain.cis.CoveragePyramid;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;

import java.util.List;

/**
 * Class to handle data migration version number 10
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class DataMigration10Handler extends AbstractDataMigrationHandler {

    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private PyramidService pyramidService;

    public DataMigration10Handler() {
        // NOTE: update this by one for new handler class
        this.migrationVersion = 10;
        this.handlerId = "64540588-e97c-11ed-95eb-2b0dfa145e1a";
    }

    @Override
    public void migrate() throws PetascopeException {
        
        for (String coverageId : this.coverageRepositoryService.readAllLocalCoverageIds()) {
            Coverage baseCoverage = this.coverageRepositoryService.readCoverageBasicMetadataByIdFromLocalCache(coverageId);

            if (baseCoverage.getPyramid() != null && !baseCoverage.getPyramid().isEmpty()) {
                // sort the pyramid by scale factors then persist it
                baseCoverage = this.coverageRepositoryService.readCoverageFullMetadataByIdFromCache(coverageId);
                List<CoveragePyramid> sortedPyramid = this.pyramidService.sortByScaleFactors(baseCoverage.getPyramid());
                baseCoverage.setPyramid(sortedPyramid);

                this.coverageRepositoryService.save(baseCoverage);
            }
        }
        
    }
    
}