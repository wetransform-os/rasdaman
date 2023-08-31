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
package petascope.openeo.handlers.model;

/**
 * Object to represent bounding-box of load_collection process:
 * 
    "west": {
          "description": "West (lower left corner, coordinate axis 1).",
          "type": "number"
      },
      "south": {
          "description": "South (lower left corner, coordinate axis 2).",
          "type": "number"
      },
      "east": {
          "description": "East (upper right corner, coordinate axis 1).",
          "type": "number"
      },
      "north": {
          "description": "North (upper right corner, coordinate axis 2).",
          "type": "number"
      },
      "base": {
          "description": "Base (optional, lower left corner, coordinate axis 3).",
          "type": ["number", "null"],
          "default": null
      },
      "height": {
          "description": "Height (optional, upper right corner, coordinate axis 3).",
          "type": ["number", "null"],
          "default": null
      },
      "crs": {
          "description": "Coordinate reference system of the extent, specified as as [EPSG code](http://www.epsg-registry.org/). Defaults to `4326` (EPSG code 4326) unless the client explicitly requests a different coordinate reference system.",
          "anyOf": [{"title": "EPSG Code", "type": "integer", "subtype": "epsg-code","minimum": 1000, "examples": [3857]}],
          "default": 4326
      }
 */
public class ParameterBoundingBox {

    public String west;
    public String south;
    public String east;
    public String north;
    public String base;
    public String height;
    public String crs;
    
    public ParameterBoundingBox(String west, String south, String east, String north, String base, String height, String crs) {
        this.west = west;
        this.south = south;
        this.east = east;
        this.north = north;
        this.base = base;
        this.height = height;
        this.crs = crs;
    }
}
