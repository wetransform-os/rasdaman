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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

#include "mddtype.hh"
#include "reladminif/sqlitewrapper.hh"
#include <logging.hh>

void MDDType::insertInDb()
{
    SQLiteQuery::executeWithParams(
        "INSERT INTO RAS_MDDTYPES ( MDDTypeOId, MDDTypeName ) VALUES (%lld, '%s')",
        myOId.getCounter(), getName());
    DBObject::insertInDb();
}

void MDDType::readFromDb()
{
#ifdef RMANBENCHMARK
    DBObject::readTimer.resume();
#endif

    SQLiteQuery query(
        "SELECT MDDTypeName FROM RAS_MDDTYPES WHERE MDDTypeOId = %lld", myOId.getCounter());
    if (query.nextRow())
    {
        setName(query.nextColumnString());
    }
    else
    {
        LERROR << "mdd type " << myOId.getCounter() << " not found in RAS_MDDTYPES.";
        throw r_Ebase_dbms(SQLITE_NOTFOUND, "mdd type object not found in RAS_MDDTYPES.");
    }
    DBObject::readFromDb();

#ifdef RMANBENCHMARK
    DBObject::readTimer.pause();
#endif
}

void MDDType::deleteFromDb()
{
    SQLiteQuery::executeWithParams(
        "DELETE FROM RAS_MDDTYPES WHERE MDDTypeOId = %lld", myOId.getCounter());
    DBObject::deleteFromDb();
}
