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
 * Copyright 2003 - 2020 Peter Baumann /
 rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

 ///<reference path="../../../../assets/typings/tsd.d.ts"/>

module rasdaman.common {
    /**
     * Directive used to get the list of filtered rows from the smart table
     */
    export function getFilteredRows($window, $rootScope):angular.IDirective {
        return {
            require: '^stTable',            
            link: function(scope, element, attr, ctrl:any) {

                let rootScope = scope.$root;
                scope.$watch(function (rootScope) {
                    let obj:any[] = ctrl.getFilteredCollection();
                    let service = "";
                    
                    if (attr["stTable"] == "layers") {
                        service = "WMS";
                        $window.wmsGetCapabilitiesFilteredRows = obj;
                    } else {
                        service = "WCS";
                        $window.wcsGetCapabilitiesFilteredRows = obj;
                    }

                    if (obj.length == 0) {
                        if (service == "WCS") {
                            $window.wcsGetCapabilitiesFilteredRows = [];
                        } else if (service == "WMS") {
                            $window.wmsGetCapabilitiesFilteredRows = [];
                        }
                    }


                    // NOTE: Notify WCS and WMS GetCapabilities handler to recalculate the number of rows and sizes of filtered rows
                    $rootScope.$broadcast("filteredRowsEvent" + service, true);
                });
            }
        };
    }
}
