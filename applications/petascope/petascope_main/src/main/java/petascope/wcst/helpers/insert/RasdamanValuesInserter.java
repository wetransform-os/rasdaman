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
 * Copyright 2003 - 2018 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcst.helpers.insert;

import org.slf4j.LoggerFactory;
import petascope.exceptions.PetascopeException;
import petascope.rasdaman.exceptions.RasdamanException;
import petascope.util.ras.RasUtil;


/**
 * Class for inserting data into a collection, starting from a values clause.
 *
 * @author <a href="mailto:merticariu@rasdaman.com">Vlad Merticariu</a>
 */
public class RasdamanValuesInserter extends AbstractRasdamanInserter {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RasdamanValuesInserter.class);

    String collectionName;
    String collectionType;
    String values;
    String tiling;

    /**
     * Class constructor.
     * @param collectionName: the collection name to be inserted.
     * @param collectionType: the collection type.
     * @param values: the values clause in rasdaman format.
     * @param tiling: the tiling clause in rasdaman format.
     */
    public RasdamanValuesInserter(String collectionName, String collectionType, String values, String tiling, String userName, String passWord) {
        this.collectionName = collectionName;
        this.collectionType = collectionType;
        this.values = values;
        this.tiling = tiling;
        
        this.username = userName;
        this.password = passWord;
    }

    @Override
    public void insert() throws RasdamanException, PetascopeException {
        try {
            //insert the values
            RasUtil.executeInsertStatement(collectionName, values, tiling, username, password);
        } catch (RasdamanException ex) {
            log.error("Rasdaman error when inserting into collection " + collectionName + ", the collection will be dropped. Error message: " + ex.getMessage());
            try {
                RasUtil.dropCollection(collectionName, username, password);
            } catch (RasdamanException ex1) {
                log.warn("Failed to drop collection: " + collectionName + ". Reason: " + ex1.getMessage());
            }

            throw ex;
        }
    }
}
