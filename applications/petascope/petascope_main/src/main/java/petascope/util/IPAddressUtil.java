// -- rasdaman enterprise begin

/*
 *  Copyright 2003 - 2019 Peter Baumann / rasdaman GmbH. 
 *  For more information please see <http://www.rasdaman.org>
 *  or contact Peter Baumann via <baumann@rasdaman.com>.
 */

package petascope.util;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;

/**
 * 
 * Using this library GeoLite2 https://dev.maxmind.com/geoip/geoip2/geolite2/.
 * License:
 *  The GeoLite2 databases are distributed under the Creative Commons Attribution-ShareAlike 4.0 International License. 
 *  The attribution requirement may be met by including the following in all advertising and documentation mentioning features of or use of this database.
 *  This product includes GeoLite2 data created by MaxMind, available from <a href="https://www.maxmind.com">https://www.maxmind.com</a>.
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
public class IPAddressUtil {
    
    private static final Logger log = LoggerFactory.getLogger(IPAddressUtil.class);

    private static IPAddressUtil instance = null;
    private static DatabaseReader databaseReader;
    private static final String PATH_TO_GEOIP_DATABASE_FILE = "/statistic_collection/GeoLite2-Country_20190219/GeoLite2-Country.mmdb";
    
    // NOTE: if request comes from localhost, in java it is this IP
    public static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    public static final String LOCALHOST_IPV4 = "127.0.0.1";
    
    // In case, it cannot resolve country from IP address
    public static final String UNKNOWN_COUNTRY = "Unknown";    
    public static final String LOCALHOST = "Localhost";
    
    public static IPAddressUtil getInstance() throws PetascopeException {
        if (instance == null) {
            instance = new IPAddressUtil();
        }
        
        return instance;
    }
    
    private IPAddressUtil() throws PetascopeException {
        // Read GeoLite2 database file

        // NOTE: The file is added inside a jar file (as petascope-core) is a library, hence it cannot read the file directly via file path.
        InputStream inputStream = IPAddressUtil.class.getResourceAsStream(PATH_TO_GEOIP_DATABASE_FILE);
        
        if (inputStream == null) {
            throw new PetascopeException(ExceptionCode.RuntimeError, "Failed resolving internal resource '" + PATH_TO_GEOIP_DATABASE_FILE + "'");
        }

        try {
            databaseReader = new DatabaseReader.Builder(inputStream).build();
        } catch (IOException ex) {
            throw new PetascopeException(ExceptionCode.RuntimeError, 
                                        "Cannot build GeoIP database from GeoLite2-Country data file. Reason: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Return the country name by input IP address.
     */
    public String getCountryByIP(String ipAddress) throws PetascopeException {
        String country = LOCALHOST;
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex) {
            throw new PetascopeException(ExceptionCode.RuntimeError, 
                    "Cannot create InetAddress from IP address '" + ipAddress + "'. Reason: " + ex.getMessage(), ex);
        }
        
        // If IP address is not default localhost IP, then try to resolve its country
        if (!(inetAddress.getHostAddress().equals(LOCALHOST_IPV4) 
            || inetAddress.getHostAddress().equals(LOCALHOST_IPV6)
            || inetAddress.isAnyLocalAddress()
            || inetAddress.isSiteLocalAddress())) {
            
            CountryResponse response = null;
            
            try {
                response = databaseReader.country(inetAddress);
                country = response.getCountry().getName();
            } catch (Exception ex) {
                log.debug("Cannot get country name from IP address '" + inetAddress.getHostAddress() + "'. Reason: " + ex.getMessage());
                country = UNKNOWN_COUNTRY;
            }
        }
        
        return country;
    }
    
}


// -- rasdaman enterprise end