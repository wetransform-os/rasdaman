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
import com.rasdaman.admin.layer.service.AdminCreateOrUpdateLayerService;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.rasdaman.repository.service.WMSRepostioryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;

/**
 * Class to handle data migration version number 8.
 * It needs to update any local WMS layers from their associated coverages
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class DataMigration8Handler extends AbstractDataMigrationHandler {
    
    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private WMSRepostioryService wmsRepostioryService;
    @Autowired
    private AdminCreateOrUpdateLayerService createOrUpdateLayerService;
    
    private static final Logger log = LoggerFactory.getLogger(DataMigration8Handler.class);
    
    public DataMigration8Handler() {
        // NOTE: update this by one for new handler class
        this.migrationVersion = 8;
        this.handlerId = "8ca57fde-4fa4-11ed-a476-509a4cb4e064";
    }

    @Override
    public void migrate() throws PetascopeException, Exception {
        
        this.wmsRepostioryService.readAllLocalLayersFromCache();
        
        for (String coverageId : this.coverageRepositoryService.readAllLocalCoverageIds()) {
            if (this.wmsRepostioryService.isInLocalCache(coverageId)) {                
                this.coverageRepositoryService.readCoverageFullMetadataByIdFromCache(coverageId);
                // If this coverage has an associated WMS layer -> update
                this.createOrUpdateLayerService.save(coverageId, null);
            }
        }
        
    }
    
}

