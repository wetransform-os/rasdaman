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
import jdk.vm.ci.meta.Local;
import liquibase.pro.packaged.D;
import org.rasdaman.admin.pyramid.service.PyramidService;
import org.rasdaman.domain.cis.AxisExtent;
import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.domain.cis.CoveragePyramid;
import org.rasdaman.domain.wms.Dimension;
import org.rasdaman.domain.wms.Layer;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.rasdaman.repository.service.WMSRepostioryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;

import java.util.List;

/**
 * Class to handle data migration
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class DataMigration11Handler extends AbstractDataMigrationHandler {

    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private WMSRepostioryService wmsRepostioryService;

    public DataMigration11Handler() {
        // NOTE: update this by one for new handler class
        this.migrationVersion = 11;
        this.handlerId = "2057c932-0aba-11ee-a4e1-c334c46b02d7";
    }

    @Override
    public void migrate() throws PetascopeException, SecoreException {

        for (Layer layer : this.wmsRepostioryService.readAllLocalLayersFromCache()) {
            if (layer.getDimensions() != null && layer.getDimensions().size() > 0) {
                for (Dimension dimension : layer.getDimensions()) {
                    String layerName = layer.getName();
                    Coverage basicCoverage = this.coverageRepositoryService.readCoverageBasicMetadataByIdFromLocalCache(layerName);
                    List<AxisExtent> axisExtents = basicCoverage.getEnvelope().getEnvelopeByAxis().getAxisExtents();
                    String units = Dimension.getUnits(dimension.getName(), axisExtents);
                    dimension.setUnits(units);
                }

                this.wmsRepostioryService.saveLayer(layer);
            }
        }

    }

}
