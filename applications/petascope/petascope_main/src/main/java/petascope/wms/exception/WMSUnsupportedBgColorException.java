
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

package petascope.wms.exception;

import org.jetbrains.annotations.NotNull;
import petascope.core.KVPSymbols;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WMSException;


public class WMSUnsupportedBgColorException extends WMSException {


    public WMSUnsupportedBgColorException(@NotNull String bgcolorValue) {
        super(ExceptionCode.InvalidFormat, BACKGROUND_COLOR_ERROR_MESSAGE.replace("$value", bgcolorValue));
    }

    public WMSUnsupportedBgColorException(Integer numberOfBands) {
        super(ExceptionCode.InvalidFormat, NUMBER_OF_BANDS_ERROR_MESSAGE.replace("$numberOfBands", numberOfBands.toString()));
    }

    private static final String BACKGROUND_COLOR_ERROR_MESSAGE = "Invalid value for '" + KVPSymbols.KEY_WMS_BGCOLOR + "' parameter. Given '$value'." +
        " Hint: valid value is hexadecimal red-green-blue colour with format: 0xRRGGBB.";

    private static final String NUMBER_OF_BANDS_ERROR_MESSAGE = "Requesting layer has total: $numberOfBands bands. " +
                                                              "Hint: " + KVPSymbols.KEY_WMS_BGCOLOR + " requesting parameter can be applied only on 1, 3 or 4 bands layer.";
}
