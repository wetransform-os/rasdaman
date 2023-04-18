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
package org.rasdaman;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import org.gdal.gdal.gdal;
import org.rasdaman.config.ConfigManager;
import org.rasdaman.migration.service.AbstractMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import petascope.controller.AbstractController;
import petascope.core.GeoTransform;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.PetascopeRuntimeException;
import petascope.util.CrsProjectionUtil;
import petascope.util.ras.TypeRegistry;
import petascope.wcs2.parsers.request.xml.XMLAbstractParser;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rasdaman.config.ConfigManager.STATIC_HTML_DIR_PATH;
import org.rasdaman.datamigration.DataMigrationService;
import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.rasdaman.repository.service.WMSRepostioryService;
import org.springframework.beans.factory.annotation.Autowired;
import petascope.core.Pair;
import petascope.util.CrsUtil;
import petascope.util.ras.RasUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.rasdaman.config.ConfigManager.KEY_PETASCOPE_CONF_DIR;
import org.rasdaman.repository.service.OWSMetadataRepostioryService;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * This class initializes the petascope properties and runs the application as jar file.
 *
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 * @author Dimitar Misev
 */
@SpringBootApplication(scanBasePackages = {"com.rasdaman", "org.rasdaman", "petascope"})
@EnableCaching

// NOTE: When the repository/entity/compontent package 
// is different to @SpringBootApplication (org.rasdaman for this main file),
// basePackages is required to be defined explicitly.
@EnableJpaRepositories(basePackages = {"com.rasdaman", "org.rasdaman"})
// Scan packages which contains Entities to create tables in database
@EntityScan(basePackages = {"com.rasdaman", "org.rasdaman", "petascope"}) 
@ComponentScan(basePackages = {"com.rasdaman", "org.rasdaman", "petascope"})
// NOTE: classpath is important when running as war package or it will have error: resource not found
@PropertySource({"classpath:application.properties"})
public class ApplicationMain extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(ApplicationMain.class);
    
    public static final String APPLICATION_PROPERTIES_FILE = "application.properties";
    // path to gdal native files (.so) which are needed for GDAL java to invoke.
    private static final String PREFIX_INPUT_PARAMETER = "--";    
    private static final String KEY_MIGRATE = "migrate";
    
    // When invoked from command line (e.g: java -jar rasdaman.war), the migration
    // is set with a command-line parameter --migrate which makes this option true.
    public static boolean MIGRATE = false;
    
    // Only used when running embedded petascope to set a custom directory containing petascope.properties
    private static String OPT_PETASCOPE_CONFDIR = "";
    
    private static Properties applicationProperties = null;
    
    public static boolean COVERAGES_CACHES_LOADED = false;
    
    // NOTE: this set is not updated when insert/delete local coverages, it only contains
    // the list of local collection from rasdaman when petascope starts
    private static Set<String> localRasdamanCollectionNames = new HashSet<>();
    
    /**
     * Check if petascope runs with embedded tomcat or external tomcat
     */
    public static boolean embedded = false;

    // Spring finds all the subclass of AbstractMigrationService and injects to the list
    @Resource
    List<AbstractMigrationService> migrationServices;
    
    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private WMSRepostioryService wmsRepostioryService;
    
    @Autowired
    private DataMigrationService dataMigrationService;
    
    @Autowired
    private OWSMetadataRepostioryService owsMetadataRepostioryService;

    @Autowired
    private ServletContext servletContext;



    private static ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(1);
    private static boolean initAfterPetascopeStarted = false;

    /**
     * Invoked when running Petascope (rasdaman.war) only in an external servlet container. 
     * It loads properties files for both the Spring Framework and ConfigManager.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() throws Exception {
        return init();
    }
    
    @Bean
    WebMvcConfigurer configurer () {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addResourceHandlers (ResourceHandlerRegistry registry) {
                if (!STATIC_HTML_DIR_PATH.isEmpty()) {
                    // valid demo folder path as resource, e.g: file:///var/www/html/Earthlook_RASDAMAN_PROJECTS/bigdatacube/
                    registry.addResourceHandler("/**")
                            .addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/public/", 
                                                  "file://" + STATIC_HTML_DIR_PATH + "/");
                }
            }
        };
    }

    /**
     * Invoked when running Petascope (rasdaman.war) only in an external servlet container. 
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        setRegisterErrorPageFilter(false);
        return builder.sources(ApplicationMain.class);
    }

    /**
     * Load properties files and init all necessary configurations.
     */
    private static PropertySourcesPlaceholderConfigurer init() throws Exception {
        // load application.properties
        applicationProperties = loadApplicationProperties();

        // load petascope.properties
        PropertySourcesPlaceholderConfigurer ret = loadPetascopeProperties(applicationProperties);
        initConfigurations(applicationProperties);

        return ret;
    }

    private static Properties loadApplicationProperties() throws Exception {
        String webappsDir = null;
        if (embedded == false) {
            // NOTE: for external system tomcat, it returns `/var/lib/tomcat9`
            // for external local tomcat, it returns `/home/rasdaman/pache-tomcat-8.5.34/bin`
            webappsDir = new FileSystemResource("").getFile().getCanonicalPath().replace("/bin", "") + "/webapps";

        }
        
        CrsUtil.loadInternalSecore(embedded, webappsDir);
        
        Properties properties = new Properties();
        properties.load(ApplicationMain.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES_FILE));
        return properties;
    }
    
    private static PropertySourcesPlaceholderConfigurer loadPetascopeProperties(Properties applicationProperties) throws Exception {
        PropertySourcesPlaceholderConfigurer ret = new PropertySourcesPlaceholderConfigurer();
        String petaPropsDir = OPT_PETASCOPE_CONFDIR;
        if (petaPropsDir.isEmpty()) {
            petaPropsDir = applicationProperties.getProperty(KEY_PETASCOPE_CONF_DIR);
        }
        File petaProps = new File(petaPropsDir, ConfigManager.PETASCOPE_PROPERTIES_FILE);
        ret.setLocation(new FileSystemResource(petaProps));
        return ret;
    }

    /**
     * Initialize all configurations for GDAL libraries, ConfigManager and OGC WCS XML Schema
     */
    private static void initConfigurations(Properties applicationProperties) throws Exception {
        if (OPT_PETASCOPE_CONFDIR.isEmpty()) {
            ConfigManager.init(applicationProperties.getProperty(KEY_PETASCOPE_CONF_DIR));
        } else {
            // properties file is configured from input parameters for embedded petascope
            ConfigManager.init(OPT_PETASCOPE_CONFDIR);
        }
        
        try {
            // Load all the type registry (set, mdd, base types) of rasdaman
            TypeRegistry.getInstance();
        } catch (Exception ex) {
            log.warn("Failed initializing type registry from rasdaman.", ex);
        }

        try {
            // Load the WCS Schema to validation if it is needed
            XMLAbstractParser.loadWcsSchema();
        } catch (Exception ex) {
            log.error("Failed loading the OGC WCS Schema for POST/SOAP request validation.", ex);
            AbstractController.startException = ex;
        }
    }
    
    private static void loadGdalLibrary() {
        
        // NOTE: to make sure that GDAL is loaded properly, do a simple CRS transformation here 
        // (if not, user has to restart Tomcat for JVM class loader does the load JNI again).
        try {
            gdal.AllRegister(); // should be done once on application startup
            // test projection to check if gdal-java library works
            String sourceCRS = "http://localhost:8080/def/crs/EPSG/0/4326";
            String targetCRS = "http://localhost:8080/def/crs/EPSG/0/3857";
            
            String wkt = CrsUtil.getWKT(sourceCRS);
            GeoTransform sourceGT = new GeoTransform(wkt, new BigDecimal("111.975"), new BigDecimal("-8.975"), 89, 71, new BigDecimal("0.5"), new BigDecimal("-0.5"));
            CrsProjectionUtil.getGeoTransformInTargetCRS(sourceGT, targetCRS);
        } catch (Error | Exception ex) {
            String errorMessage = "Transform test failed, probably due to a problem with adding GDAL native library "
                                + "to java library path; please restart Tomcat to fix this problem. Reason: " + ex;
            AbstractController.startException = new PetascopeException(ExceptionCode.InternalComponentError, errorMessage);
        }
    }
    
    // -----------------------------------------------------------------------------------

    /**
     * Invoked when all beans are created to check it should run the migration
     * or not.
     */
    @PostConstruct
    private void postInit() throws Exception {
        
        loadGdalLibrary();
        initTrustAllSSL();
        
        if (MIGRATE && AbstractController.startException == null) {
            log.info("Migrating petascopedb from JDBC URL '" + ConfigManager.SOURCE_DATASOURCE_URL + 
                    "' to JDBC URL '" + ConfigManager.PETASCOPE_DATASOURCE_URL + "'...");
            /*
             * NOTE: Hibernate is already connected when migration application starts,
             * so with the embedded database, if another connection tries to connect, it will return exception.
             */
            // Then, check what kind of migration should be done
            // NOTE: There are 2 types of migration:
            // + From legacy petascopedb prior version 9.5, it checks if petascopedb contains a 
            //   legacy table name then it migrates to new petascopedb version 9.5.
            // + From petascopedb after version 9.5 to a different database, it checks if both source JDBC, 
            //   target JDBC can be connected then it migrates entities to new database.
            for (AbstractMigrationService migrationService : migrationServices) {
                if (migrationService.isMigrating()) {
                    // A migration process is running, don't do anything else
                    log.error("A migration process is already running.");
                    System.exit(ExitCode.FAILURE.getExitCode());
                }
                
                try {
                    if (migrationService.canMigrate()) {
                        migrationService.migrate();
                        // Just do one migration
                        break;
                    }
                } catch (Exception ex) {
                    log.error("An error occured while migrating, aborting the migration process. Reason: " + ex.getMessage());
                    // Release the lock on Migration table so later can run migration again
                    migrationService.releaseLock();
                    System.exit(ExitCode.FAILURE.getExitCode());
                }
            }
            log.info("petascopedb migrated successfully.");
            System.exit(ExitCode.SUCCESS.getExitCode());
        }


        // ### data migration

        // Test if rasdaman is running first

        try {
            RasUtil.checkValidUserCredentials(ConfigManager.RASDAMAN_ADMIN_USER, ConfigManager.RASDAMAN_ADMIN_PASS);
        } catch(Exception ex) {
            String errorMessage = "Cannot check if rasdaman is running. Reason: " + ex.getMessage().trim()
                                + " Hint: make sure rasdaman is running and restart tomcat service afterwards.";
            log.error(errorMessage);
            AbstractController.startException = new PetascopeException(ExceptionCode.InternalComponentError, errorMessage);
            return;
        }

        CrsUtil.setInternalResolverCRSToQualifiedCRS();
        CrsUtil.currentWorkingResolverURL = ConfigManager.SECORE_URLS.get(0);

    }

    @EventListener(ApplicationReadyEvent.class)
    private void initAfterWebApplicationStarted(ApplicationReadyEvent event) throws Exception {
        // In case someone changed rasdaman.war to name.war and deployed it to external tomcat
        ConfigManager.CONTEXT_PATH = this.servletContext.getContextPath();

        final ApplicationMain currentClass = this;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (initAfterPetascopeStarted) {
                        // Everything was started, this service is stopped
                        schedulerExecutorService = Executors.newScheduledThreadPool(0);
                        return;
                    }

                    // Trigger checking working SECORE URL
                    CrsUtil.runTaskByInterval();

                    log.info("Running data migrations ...");

                    dataMigrationService.runMigration();

                    log.info("Checked data migrations.");

                    owsMetadataRepostioryService.read();


                    // load coverages / layers to caches in background thread
                    loadCoveragesLayersCaches(currentClass);

                    // then, check if local rasdaman collections of local coverages exist in RASBASE
                    checkLocalCollectionsOfCoveragesExist(currentClass);

                    initAfterPetascopeStarted = true;
                } catch (Exception ex) {
                    log.error("Failed to run services after petascope started. Reason: " + ex.getMessage(), ex);
                    throw new PetascopeRuntimeException(ex);
                }
            }
        };

        // NOTE: This schedule runs only one time and it checks when petascope actually started (especially when it is deployed in external tomcat)
        schedulerExecutorService.scheduleAtFixedRate(runnable,
                                                1,
                                                1, SECONDS);


    }


    
    /**
     * Run in a background thread to load coverages and layers to caches.
     * Log warnings if something don't work (later WCS / WMS Getcapabilities requests will retry to read from database to caches)
     */
    private void loadCoveragesLayersCaches(final ApplicationMain self) {

        Runnable runnable1 = new Runnable() {            
            public void run() {
                // ### 1. coverages
                
                try {
                    log.info("Loading coverages to caches ...");
                    coverageRepositoryService.readAllCoveragesBasicMetadataWhenPetascopeStarts();
                } catch (Exception ex) {
                    log.warn("Cannot load coverages to cache. Reason: " + ex.getMessage(), ex);
                }

                log.info("Loaded coverages to caches.");
                
                COVERAGES_CACHES_LOADED = true;
            }
        };
        
        Runnable runnable2 = new Runnable() {            
            public void run() {
        
                // ### 2. layers

                log.info("Loading layers to caches ...");
                
                try {
                    wmsRepostioryService.readAllLayers();
                } catch (PetascopeException ex) {
                    log.warn("Cannot load layers to cache. Reason: " + ex.getMessage(), ex);                    
                }
                
                log.info("Loaded layers to caches.");
            }
        };
        
        Thread thread1 = new Thread(runnable1);
        thread1.start();        
        
        Thread thread2 = new Thread(runnable2);
        thread2.start();
    }
    
    private void checkLocalCollectionsOfCoveragesExist(final ApplicationMain self) {
        Runnable runnable = new Runnable() {            
            public void run() {
                log.info("Checking that collections corresponding to the coverages in petascope exist in rasdaman...");
                
                List<Pair<Coverage, Boolean>> localCoveragesList = new ArrayList<>();
                try {
                    localCoveragesList = self.coverageRepositoryService.readAllLocalCoveragesBasicMetatata();
                } catch (PetascopeException ex) {
                    log.warn("Checking that collections corresponding to coverages exist in rasdaman failed, "
                            + "cannot read list of basic coverage metadata objects. Reason: " + ex.getMessage());
                }
                
                try {
                    localRasdamanCollectionNames = RasUtil.getLocalCollectionNames();
                } catch (Exception ex) {
                    log.warn("Checking that collections corresponding to coverages exist in rasdaman failed, "
                            + "cannot get list of local collections from rasdaman. Reason: " + ex.getMessage(), ex);
                }
                
                
                if (!localRasdamanCollectionNames.isEmpty()) {
                    // Only check if collection name exists for coverages, if rasdaman can be up for collecting collection names
                
                    for (Pair<Coverage, Boolean> pair : localCoveragesList) {
                        Coverage coverage = pair.fst;
                        String coverageId = coverage.getCoverageId();

                        String rasdamanCollectionName = coverage.getRasdamanRangeSet().getCollectionName();
                        if (!localRasdamanCollectionNames.contains(rasdamanCollectionName)) {
                            log.warn("Collection name '" + rasdamanCollectionName + "' of coverage '" + coverageId + "' does not exist in rasdaman; "
                                    + "it is best to delete this coverage and reimport it.");
                        }
                    }
                    
                }

                log.info("Done checking that collections corresponding to the coverages in petascope exist in rasdaman.");
            }
        };
        
        Thread thread = new Thread(runnable);
        thread.start();  
    }

    /**
     * Return the exit code to user
     */
    public enum ExitCode {
        SUCCESS(0),
        FAILURE(1);

        private final int value;

        private ExitCode(int value) {
            this.value = value;
        }

        public int getExitCode() {
            return this.value;
        }
    }
    
    /**
     * Check if input argument is added when running embedded ptascope
     */
    private static boolean matchInputArgumentKey(String arg, String key) {
        if (arg.startsWith(PREFIX_INPUT_PARAMETER + key)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if arg is key=value format.
     */
    private static void checkValidKeyEqualsValue(String arg) throws PetascopeException {
        if (!arg.contains("=")) {
            throw new PetascopeException(ExceptionCode.InvalidParameterValue, "Input parameter must be key=value, given: "  + arg);
        }
    }

    /**
     * Main method when running embedded petascope with optional input parameters,
     * e.g: java -jar rasdaman.war --petascope.confDir=/opt/rasdaman/new_etc
     */
    public static void main(String[] args) throws Exception {
        embedded = true;
                
        for (String arg : args) {
            if (matchInputArgumentKey(arg, KEY_PETASCOPE_CONF_DIR)) {
                checkValidKeyEqualsValue(arg);
                
                String value = arg.split("=")[1];
                OPT_PETASCOPE_CONFDIR = value;
            } else if (matchInputArgumentKey(arg, KEY_MIGRATE)) {
                MIGRATE = true;
            } else {
                throw new PetascopeException(ExceptionCode.NoApplicableCode, "Input parameter is not supported, given: " + arg);
            }
        }
        
        init();
        
        try {
            SpringApplication.run(ApplicationMain.class, args);
        } catch (Exception ex) {
            // NOTE: This class is private in Spring framework, cannot be imported here to compare by instanceof so must use class name to compare.
            if (!ex.getClass().getCanonicalName().equals("org.springframework.boot.devtools.restart.SilentExitExceptionHandler.SilentExitException")) {
                // There is a NULL error from Spring dev tools restarts Tomcat internally when a java file is saved (Compile on save) and this can be ignored in any cases.
                // NOTE: This error only happens when starting Petascope main application with mvn spring-boot:run for development.
                log.error("Error starting petascope with embedded Tomcat. Reason: " + ex.getMessage(), ex);
                System.exit(ExitCode.FAILURE.getExitCode());
            }
        }
    }
   
    /**
     * Make rasfed to trust any SSL certificate. 
     * NOTE: It has problem with connecting to HTTPS with certificate trusting.
     */
    private static void initTrustAllSSL() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException ex) {
            log.warn("Cannot initialize all SSL trusting. "
                    + "Rasfed may not be able to connect to remote petascope via HTTPS protocol.", ex);
        }

    }
}
