// -- rasdaman enterprise begin

/*
 *  Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *  For more information please see <http://www.rasdaman.org>
 *  or contact Peter Baumann via <baumann@rasdaman.com>.
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


// -- rasdaman enterprise end