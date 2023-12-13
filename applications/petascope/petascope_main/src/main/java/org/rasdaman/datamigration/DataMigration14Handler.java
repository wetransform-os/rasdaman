// -- rasdaman enterprise begin

package org.rasdaman.datamigration;

import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.domain.cis.Field;
import org.rasdaman.domain.cis.Quantity;
import org.rasdaman.domain.cis.RangeType;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to handle data migration version number 14.
 * Update observation type for quantity
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class DataMigration14Handler extends AbstractDataMigrationHandler {

    @Autowired
    private CoverageRepositoryService coverageRepositoryService;

    public DataMigration14Handler() {
        // NOTE: update this by one for new handler class
        this.migrationVersion = 14;
        this.handlerId = "2c0a923a-8aa5-11ee-aa82-5f13d9deb9b7";
    }

    @Override
    public void migrate() throws PetascopeException, SecoreException {
        
        for (String coverageId : this.coverageRepositoryService.readAllLocalCoverageIds()) {
            
            Coverage localCoverage = this.coverageRepositoryService.readCoverageByIdFromDatabase(coverageId);
            for (Field field : localCoverage.getRangeType().getDataRecord().getFields()) {
                field.getQuantity().setObservationType(Quantity.ObservationType.NUMERICAL);
            }

            this.coverageRepositoryService.save(localCoverage);
        }
        
    }
    
}

// -- rasdaman enterprise end