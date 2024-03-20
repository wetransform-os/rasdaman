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
package com.rasdaman.admin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response result for /rasdaman/admin/authisactive request
 */
public class AuthIsActiveResult {

    private boolean basicAuthenticationEnabled;
    private String rasdamanUser;

    public AuthIsActiveResult(boolean basicAuthenticationEnabled, String rasdamanUser) {
        this.basicAuthenticationEnabled = basicAuthenticationEnabled;
        this.rasdamanUser = rasdamanUser;
    }

    @JsonProperty("basic_authentication_header_enabled")
    public boolean isBasicAuthenticationEnabled() {
        return basicAuthenticationEnabled;
    }

    @JsonProperty("rasdaman_user")
    public String getRasdamanUser() {
        return rasdamanUser;
    }
}
