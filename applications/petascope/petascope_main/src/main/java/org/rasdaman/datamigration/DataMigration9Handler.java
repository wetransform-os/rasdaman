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
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;

/**
 * Add grid lower bound, grid upper bound and gridAxisOrder to AxisExtent
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class DataMigration9Handler extends AbstractDataMigrationHandler {
    
    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    
    private static final Logger log = LoggerFactory.getLogger(DataMigration9Handler.class);
    
    public DataMigration9Handler() {
        // NOTE: update this by one for new handler class
        this.migrationVersion = 9;
        this.handlerId = "c5bc277c-c958-11ed-9c47-33b7c058c89d";
    }

    @Override
    public void migrate() throws PetascopeException, Exception {
        
        for (String coverageId : this.coverageRepositoryService.readAllLocalCoverageIds()) {
            Coverage coverage = this.coverageRepositoryService.readCoverageFullMetadataByIdFromCache(coverageId);
            // Add grid lower bound and grid upper bound to AxisExtent when saving coverage
            this.coverageRepositoryService.save(coverage);
        }
        
    }
    
}
