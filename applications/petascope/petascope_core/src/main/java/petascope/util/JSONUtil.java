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
 * Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
*/
package petascope.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;

/**
 * JSON ultilities
 * @author <a href="mailto:bphamhuu@jacobs-university.net">Bang Pham Huu</a>
 */
public class JSONUtil {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JSONUtil.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Escape quote " to \"
     */
    public static String escapeQuote(String text) {
        text = text.replace("\"", "\\\"");
        return text;
    }
    
    /**
     * Serialize an object to JSON string with indentation (human readable) and with null as well
     */
    public static String serializeObjectToJSONStringWithNull(Object obj) throws PetascopeException {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String result = serializeObjectToString(obj);
        
        return result;
    }    
    
    /**
     * Serialize an object to JSON string with indentation (human readable)
     */
    public static String serializeObjectToJSONString(Object obj) throws PetascopeException {
        if (obj instanceof String) {
            return obj.toString();
        }

        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String result = serializeObjectToString(obj);
        
        return result;
    }
    
    /**
     * Serialize an object to JSON string without indentation (e.g: rasql encode(extra_params_json_inline))
     */
    public static String serializeObjectToJSONStringNoIndentation(Object obj) throws PetascopeException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        String result = serializeObjectToString(obj);
        
        return result;
    }
    
    /**
     * Deserialize a JSON string to an object
     */
    public static Object deserialize(String json, Class c) throws PetascopeException {
        Object result = null;
        try {
            result = objectMapper
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                        .readValue(json, c);
        } catch (IOException ex) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, 
                    "Cannot deserialize JSON string to object '" + c.getName() + "'. Reason: " + ex.getMessage(), ex);
        }
        
        return result;
    }
    
    /**
     * Deserialize a JSON string to an object
     */
    public static Object deserializeWithUnknownProperties(String json, Class c) throws PetascopeException {
        Object result = null;
        try {
            result = objectMapper
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(json, c);
        } catch (IOException ex) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, 
                    "Cannot deserialize JSON string to object '" + c.getName() + "'. Reason: " + ex.getMessage(), ex);
        }
        
        return result;
    }
    
    /**
     * Serialize a JSON object to a string.
     */
    private static String serializeObjectToString(Object obj) throws PetascopeException {
        String json;
        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            throw new PetascopeException(ExceptionCode.RuntimeError, "Cannot serialize object to JSON string. Reason: " + ex.getMessage(), ex);
        }
        return json;
    }

    /**
     * Check if an input JSON string is valid
     * @param jsonInString
     * @return
     */
    public static boolean isJsonValid(String jsonInString) {
        if (StringUtils.isEmpty(jsonInString)) {
            return false;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonInString);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static void validate(String json, String schemaURL, SpecVersion.VersionFlag versionFlag) throws IOException, PetascopeException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag);
        InputStream inputStream = new URL(schemaURL).openStream();
        JsonSchema jsonSchema = factory.getSchema(inputStream);
        JsonNode jsonNode = objectMapper.readTree(json);
        Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);
        if (!errors.isEmpty()) {
            List<String> errorMessages = new ArrayList<>();

            for (ValidationMessage validationMessage : errors) {
                String errorMessage = validationMessage.getMessage();
                errorMessages.add(errorMessage);
            }
            throw new PetascopeException(ExceptionCode.InvalidRequest,
                    "JSON string is not valid from JSON schema at: " + schemaURL + ". Given errors: " + ListUtil.join(errorMessages, "\n"));
        }
    }
    
    /**
     * Clone an input object by serializing it to JSON string and deserializing it back to a new object
     */
    public static Object clone(Object inputObject) throws PetascopeException {
        if (inputObject == null) {
            return null;
        }
        
        String json = serializeObjectToJSONString(inputObject);
        Object result = null;
        
        try {
            result = objectMapper.readValue(json, inputObject.getClass());
        } catch (Exception ex) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, "Cannot clone object. Reason: " + ex.getMessage());
        }
        
        return result;
    }

    /**
     * Given a JSON string, returns the JsonNode content of a property
     * e.g. extract the content of the "process_graph" of the JSON example on
     * https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/store-custom-process
     */
    public static JsonNode getJsonNodeByPropertyName(String json, String propertyName) throws PetascopeException {
        JsonNode result = null;
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            result = rootNode.findPath(propertyName);
            if (result == null) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "JSON string does not contain the property: " + propertyName);
            }
        } catch (JsonProcessingException ex) {
            throw new PetascopeException(ExceptionCode.InternalComponentError,
                    "Cannot parse JSON string to extract the content of property name: " + propertyName + ". Reason: " + ex.getMessage());
        }

        return result;
    }

    /**
     * Given a JSON string, returns a parsed JsonNode object.
     */
    public static JsonNode getJsonNode(String json) throws PetascopeException {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new PetascopeException(ExceptionCode.InternalComponentError,
                    "Cannot parse JSON string. Reason: " + ex.getMessage());
        }
    }

    /**
     * Given a JSON string, then pretty print it to a better string
     */
    public static String prettyPrint(String json) throws PetascopeException {
        String result = null;
        try {
            result = objectMapper.readTree(json).toPrettyString();
        } catch (JsonProcessingException ex) {
            throw new PetascopeException(ExceptionCode.InternalComponentError,
                    "Cannot pretty print the input json string. Reason: " + ex.getMessage());
        }

        return result;
    }
    
    public static final String EMPTY_ROOT_NODE = "{}";
}
