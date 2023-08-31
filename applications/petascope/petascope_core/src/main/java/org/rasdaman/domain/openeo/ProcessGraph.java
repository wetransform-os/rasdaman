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
package org.rasdaman.domain.openeo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Table to store the user-defined process graphs
 * https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes
 */
@Entity
@Table(name = ProcessGraph.TABLE_NAME)
public class ProcessGraph implements Serializable {

    public static final String TABLE_NAME = "openeo_process_graph";
    public static final String COLUMN_ID = TABLE_NAME + "_id";

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = COLUMN_ID)
    private long id;


    @Column(name = "process_graph_id")
    // Submitted by user at PUT /process_graphs/{process_graph_id}
    private String processGraphId;

    @Column(name = "namespace")
    // users can submit duplicated names for the process graphs, in this case it will use namespace
    private String namespace = "backend";

    @Column(name = "username")
    // Submitted by user at PUT /process_graphs/{process_graph_id}
    private String username;

    @Column(name = "process_graph_content")
    // JSON content submitted by user at PUT /process_graphs/{process_graph_id}
    @Lob
    // NOTE: As this could be long text, so varchar(255) is not enough
    private String content;


    public ProcessGraph() {

    }

    public ProcessGraph(String processGraphId, String username, String content) {
        this.processGraphId = processGraphId;
        this.username = username;
        this.content = content;
    }

    public String getProcessGraphId() {
        return processGraphId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }



    @Override
    public int hashCode() {
        return Objects.hash(processGraphId, namespace);
    }
}
