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
package petascope.exceptions;

/**
 * This exception is used to write an warning to client when it has requested some rare requests which need to be notified
 */
public class WarningException extends PetascopeException {

    private String warningMessage;

    public WarningException(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public String getXMLString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rasdaman:warning xmlns:rasdaman=\"http://www.rasdaman.org\">\n" + this.getWarningMessage() + "\n</rasdaman:warning>";
    }
}
