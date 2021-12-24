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

///<reference path="../../../common/_common.ts"/>

module ows {
    // Used only for result of WCS GetCapabilities (not coverage's real metadata)
    export class CustomizedMetadata {
        public hostname:String;
        public petascopeEndPoint:String;

        // Convert value of element coverageSizeInBytes to a human-readable value (e.g: 1000 -> 1KB)
        // "N/A" if size doesn't exist from GetCapabilities result
        public coverageSize:String;
        public localCoverageSizeInBytes:number = 0;
        public remoteCoverageSizeInBytes:number = 0;
        public isBlackedList:boolean;

        public constructor(source:rasdaman.common.ISerializedObject) {
            rasdaman.common.ArgumentValidator.isNotNull(source, "source");

            this.parseCoverageLocation(source);
            this.parseCoverageSizeInBytes(source);
            this.parseBlackListed(source);
        }

        /**
         * If rasdaman:blackListed exists, then get it as true or false
         * 
         * <rasdaman:blackListed>true</rasdaman:blackListed>
         */
        private parseBlackListed(source:rasdaman.common.ISerializedObject):void {
            let childElement = "rasdaman:blackListed";
            if (source.doesElementExist(childElement)) {
                let blackListedElement = source.getChildAsSerializedObject(childElement);
                this.isBlackedList = blackListedElement.getValueAsBool();
            } else {
                this.isBlackedList = null;
            }
        }

        /**
         If in customized metadata it exists, then, get hostname and endpoint from location element.

        <rasdaman:location>
            <rasdaman:hostname>locahost</rasdaman:hostname>
            <rasdaman:endpoint>http://localhost:8080/rasdaman/ows</rasdaman:endpoint>
        </rasdaman:location>
          */        
        private parseCoverageLocation(source:rasdaman.common.ISerializedObject):void {
            let childElement = "rasdaman:location";
            if (source.doesElementExist(childElement)) {
                let locationElement = source.getChildAsSerializedObject(childElement);
                this.hostname = locationElement.getChildAsSerializedObject("rasdaman:hostname").getValueAsString();
                this.petascopeEndPoint = locationElement.getChildAsSerializedObject("rasdaman:endpoint").getValueAsString();
            }
        }

        /**
         * If rasdaman:sizeInbytes exists, then parse it and convert to human-readable value.
         * 
         * <rasdaman:sizeInBytes>6912</rasdaman:sizeInBytes>
         */
        private parseCoverageSizeInBytes(source:rasdaman.common.ISerializedObject):void {
            let childElement = "rasdaman:sizeInBytes";

            if (source.doesElementExist(childElement)) {
                let sizeInBytesElement = source.getChildAsSerializedObject(childElement);
                let sizeInBytes = sizeInBytesElement.getValueAsString();

                this.coverageSize = CustomizedMetadata.convertNumberOfBytesToHumanReadable(sizeInBytes);

                if (this.hostname === undefined) {
                    this.localCoverageSizeInBytes = sizeInBytesElement.getValueAsNumber();
                } else {
                    this.remoteCoverageSizeInBytes = sizeInBytesElement.getValueAsNumber();
                }
            } else {
                // if <sizeInBytes> element not exist                
                this.coverageSize = "N/A";
            }
        }

        /**
         * Convert a number of bytes to human readable string.
         * e.g: 1000 -> 1 KB         
         */
        public static convertNumberOfBytesToHumanReadable(numberOfBytes):String {
            if (numberOfBytes == 0) {
                return "0 B";
            }
            const k = 1000;            
            const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
            let i = Math.floor(Math.log(numberOfBytes) / Math.log(k));
            let result = parseFloat((numberOfBytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];           

            return result;
        }
    }
}
