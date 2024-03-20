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
 * Copyright 2003 - 2019 Peter Baumann /
 rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
///<reference path="../../../assets/typings/tsd.d.ts"/>
///<reference path="../../_all.ts"/>

module rasdaman {
    export class CredentialService {
        // Dictionary of url and username, password for each url
        public credentialsDict: {};

        public constructor() {              
            this.credentialsDict = JSON.parse(window.localStorage.getItem("credentials"));
            if (this.credentialsDict == null) {
                this.credentialsDict = {};
            }
        }

        // Insert or update new credential to the dictionary
        public persitCredential(endpoint:string, credential:login.Credential) {
            this.credentialsDict[endpoint] = credential;
            window.localStorage.setItem("credentials", JSON.stringify(this.credentialsDict));
        }

        // Destroy stored credentials
        public clearStorage() {
            this.credentialsDict = {};
            window.localStorage.setItem("credentials", JSON.stringify(this.credentialsDict));
        }

        public hasStoredCredentials() {
            return Object.keys(this.credentialsDict).length > 0;            
        }

        // Return the stored credentials
        public getAuthorizationHeader(petascopeEndPoint:string) {
            var result = {};

            if (this.hasStoredCredentials) {
                var credential = this.credentialsDict[petascopeEndPoint];                 
                if (credential != null && credential["username"] != null) {
                    var username = credential["username"];
                    var password = credential["password"];
                    
                    // NOTE: petauser was petascope admin user and since v10+, 
                    // it has no use anymore in petascope (user doesn't exist in rasdaman)
                    //  and user must login with different user in admin's tab
                    if (username == "petauser") {
                        this.clearStorage();
                        return result;
                    }
                    
                    result["Authorization"] = this.getEncodedBasicAuthencationString(username, password);
                }
            }

            return result;
        }

        // // Create a basic authentication header from username and password
        public createBasicAuthenticationHeader(username:string, password:string):{} {
            var headers = {};
            headers["Authorization"] = this.getEncodedBasicAuthencationString(username, password);

            return headers;
        }

        // Get an encoded basic authentication string from username and password
        private getEncodedBasicAuthencationString(username:string, password:string):string {
            var result = "Basic " + btoa(username + ":" + password);
            return result;
        }
    }
}
