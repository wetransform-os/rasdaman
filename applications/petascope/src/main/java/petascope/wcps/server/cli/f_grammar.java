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
 * Copyright 2003 - 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */


package petascope.wcps.server.cli;

import petascope.wcps.grammar.*;
import petascope.wcps.grammar.wcpsParser.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FileUtils;

/** Test the WCPS grammar parser (generated by ANTLR).
 * Input the path that contains an Abstract Syntax query.
 * Outputs the corresponding XML tree.
 *
 * @author Andrei Aiordachioaie
 */
public class f_grammar {

    private static String query;
    private static String path;

    public static void main(String[] args) throws RecognitionException, IOException {
        if (args.length != 1) {
            System.err.println("AbstractGrammarGen: no parameter to specify input file !");
            path = "test/test-tmp/29.test";
            query = FileUtils.readFileToString(new File(path));
        } else {
            path = args[0];
            query = FileUtils.readFileToString(new File(path));
        }

        System.out.println("Running with the following query: " + query);

        String xmlString = runQuery(query);
        System.out.println("Output XML: \n****************\n" + xmlString);

        System.exit(0);

    }

    public static String runQuery(String query) throws IOException, RecognitionException {
        InputStream stream = new ByteArrayInputStream(query.getBytes()); // defaults to ISO-1
        ANTLRInputStream inputStream = new ANTLRInputStream(stream);
//        wcpsLexer lexer = new wcpsLexer( inputStream );
        wcpsLexer lexer = new wcpsLexer(inputStream);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
//        wcpsParser parser = new wcpsParser(tokenStream);
        wcpsParser parser = new wcpsParser(tokenStream);

        wcpsRequest_return rrequest = parser.wcpsRequest();
        WCPSRequest request = rrequest.value;

        String result = request.toXML();
        return result;
    }
}
