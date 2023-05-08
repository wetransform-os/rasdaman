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

package org.rasdaman.secore.util;


import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
public class HttpUtil {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HttpUtil.class);

    // If output is in one of this MIME and request contains Accept-Encoding: gzip header -> compress the result in gzip format
    public static final String COMPRESSION_GZIP_HEADER = "gzip";
    public static final String ACCEPT_ENCODING_HEADER_NAME = "Accept-Encoding";
    public static final String CONTENT_ENCODING_HEADER_NAME = "Content-Encoding";

    /**
     * Check if a request / response contains gzip header
     */
    public static boolean containGZIPHeaderValue(String headerValue) {
        return headerValue != null && headerValue.contains(COMPRESSION_GZIP_HEADER);
    }

    /**
     * Given an uncompressed byte[] -> compress it in GZIP format
     */
    public static final byte[] compressBytesArrayToGZIP(byte[] bytesArray) throws SecoreException {
        try (final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             final GZIPOutputStream zipStream = new GZIPOutputStream(byteStream)) {

            zipStream.write(bytesArray);
            zipStream.finish();

            byte[] compressedBytesArray = byteStream.toByteArray();
            return compressedBytesArray;

        } catch (Exception ex) {
            throw new SecoreException(ExceptionCode.IOConnectionError,
                "Failed to compress bytes array to GZIP format. Reason: " + ex.getMessage(), ex);
        }
    }
}
