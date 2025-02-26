/*
 * Copyright (c) 2018 - 2023, Entgra (Pvt) Ltd. (http://www.entgra.io) All Rights Reserved.
 *
 * Entgra (Pvt) Ltd. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.entgra.device.mgt.core.device.mgt.extensions.device.type.template;

import io.entgra.device.mgt.core.device.mgt.common.*;
import io.entgra.device.mgt.core.device.mgt.common.configuration.mgt.PlatformConfiguration;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.DeviceManagementException;
import io.entgra.device.mgt.core.device.mgt.common.license.mgt.License;
import io.entgra.device.mgt.core.device.mgt.common.license.mgt.LicenseManagementException;
import io.entgra.device.mgt.core.device.mgt.common.license.mgt.LicenseManager;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.config.Feature;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.config.*;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.dao.DeviceDAODefinition;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.dao.DeviceTypePluginDAOManager;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.exception.DeviceTypeDeployerPayloadException;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.exception.DeviceTypeMgtPluginException;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.exception.DeviceTypePluginExtensionException;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.feature.ConfigurationBasedFeatureManager;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.util.DeviceTypePluginConstants;
import io.entgra.device.mgt.core.device.mgt.extensions.device.type.template.util.DeviceTypeUtils;
import io.entgra.device.mgt.core.device.mgt.extensions.license.mgt.meta.data.MetaRepositoryBasedLicenseManager;
import io.entgra.device.mgt.core.device.mgt.extensions.spi.DeviceTypePluginExtensionService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This holds the implementation of the device manager. From which an instance of it will be created using the
 * deployer file.
 */
public class DeviceTypeManager implements DeviceManager {

    private static final Log log = LogFactory.getLog(DeviceTypeManager.class);
    private final String deviceType;
    private final LicenseManager licenseManager;
    private final PlatformConfiguration defaultPlatformConfiguration;
    private final boolean requiredDeviceTypeAuthorization;

    private static final String PATH_MOBILE_PLUGIN_CONF_DIR =
            CarbonUtils.getEtcCarbonConfigDirPath() + File.separator + "device-mgt-plugin-configs" + File.separator
                    + "mobile";

    private FeatureManager featureManager;
    private boolean propertiesExist;
    private DeviceTypePluginDAOManager deviceTypePluginDAOManager;
    private DeviceTypePluginDAOManager propertyBasedDeviceTypePluginDAOManager = null;

    public DeviceTypeManager(DeviceTypeConfigIdentifier deviceTypeConfigIdentifier,
                             DeviceTypeConfiguration deviceTypeConfiguration) {
        deviceType = deviceTypeConfigIdentifier.getDeviceType();
        if (deviceTypeConfiguration.getFeatures() != null && deviceTypeConfiguration.getFeatures().
                getFeature() != null) {
            List<Feature> features = deviceTypeConfiguration.getFeatures().getFeature();
            featureManager = new ConfigurationBasedFeatureManager(features);
        }
        if (deviceTypeConfiguration.getDeviceAuthorizationConfig() != null) {
            requiredDeviceTypeAuthorization = deviceTypeConfiguration.getDeviceAuthorizationConfig().
                    isAuthorizationRequired();
        } else {
            requiredDeviceTypeAuthorization = true;
        }
        //add license to registry.
        this.licenseManager = new MetaRepositoryBasedLicenseManager();
        try {
            if (deviceTypeConfiguration.getLicense() != null) {
                License defaultLicense = new License();
                defaultLicense.setLanguage(deviceTypeConfiguration.getLicense().getLanguage());
                defaultLicense.setVersion(deviceTypeConfiguration.getLicense().getVersion());
                defaultLicense.setText(deviceTypeConfiguration.getLicense().getText());
                licenseManager.addLicense(deviceType, defaultLicense);
            }
        } catch (LicenseManagementException e) {
            String msg = "Error occurred while adding default license of device type: " + deviceType;
            throw new DeviceTypeDeployerPayloadException(msg, e);
        }

        // Loading default platform configuration
        try {
            defaultPlatformConfiguration = this.getDefaultConfiguration();
        } catch (DeviceManagementException e) {
            String msg =
                    "Error occurred while getting default platform configuration for the device type " + deviceType;
            throw new DeviceTypeDeployerPayloadException(msg, e);
        }

        DeviceDetails deviceDetails = deviceTypeConfiguration.getDeviceDetails();

        if (deviceDetails != null) {

            //Check whether device dao definition exist.
            String tableName = deviceTypeConfiguration.getDeviceDetails().getTableId();
            if (tableName != null && !tableName.isEmpty()) {
                DataSource dataSource = deviceTypeConfiguration.getDataSource();
                if (dataSource == null) {
                    throw new DeviceTypeDeployerPayloadException("Could not find the datasource related with the "
                            + "table id " + tableName + " for the device type " + deviceType);
                }
                TableConfig tableConfig = dataSource.getTableConfig();

                if (tableConfig == null) {
                    throw new DeviceTypeDeployerPayloadException("Could not find the table config with the "
                            + "table id " + tableName + " for the device type " + deviceType);
                }
                List<Table> tables = deviceTypeConfiguration.getDataSource().getTableConfig().getTable();
                Table deviceDefinitionTable = null;
                for (Table table : tables) {
                    if (tableName.equals(table.getName())) {
                        deviceDefinitionTable = table;
                        break;
                    }
                }
                if (deviceDefinitionTable == null) {
                    throw new DeviceTypeDeployerPayloadException("Could not find definition for table: " + tableName);
                }
                propertiesExist = true;

                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext()
                            .setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);

                    DeviceDAODefinition deviceDAODefinition = new DeviceDAODefinition(deviceDefinitionTable);
                    String datasourceName = deviceTypeConfiguration.getDataSource().getJndiConfig().getName();
                    if (datasourceName != null && !datasourceName.isEmpty()) {
                        String setupOption = System.getProperty("setup");
                        if (setupOption != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("-Dsetup is enabled. Device management repository schema initialization is about " +
                                        "to begin");
                            }
                            try {
                                DeviceTypeUtils.setupDeviceManagementSchema(datasourceName, deviceType,
                                        deviceDAODefinition.getDeviceTableName());
                            } catch (DeviceTypeMgtPluginException e) {
                                log.error("Exception occurred while initializing device management database schema", e);
                            }
                        }
                        deviceTypePluginDAOManager = new DeviceTypePluginDAOManager(datasourceName, deviceDAODefinition);
                        if (deviceDetails.getProperties() == null || deviceDetails.getProperties().getProperty() == null
                                || deviceDetails.getProperties().getProperty().size() == 0) {
                            Properties properties = new Properties();
                            List<String> propKeys = new ArrayList<>(deviceDAODefinition.getColumnNames());
                            propKeys.add("token");
                            properties.addProperties(propKeys);
                            deviceDetails.setProperties(properties);
                        }
                        propertyBasedDeviceTypePluginDAOManager = new DeviceTypePluginDAOManager(deviceType, deviceDetails);
                    } else {
                        throw new DeviceTypeDeployerPayloadException("Invalid datasource name.");
                    }
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            } else {
                if (deviceDetails.getProperties() != null && deviceDetails.getProperties().getProperty() != null
                        && deviceDetails.getProperties().getProperty().size() > 0) {
                    deviceTypePluginDAOManager = new DeviceTypePluginDAOManager(deviceType, deviceDetails);
                    propertiesExist = true;
                }
            }
        }
        setDeviceTypePluginManager();
    }

    /**
     * Set device type plugin DAO manager of each device type in a HashMap which can then be used via individual
     * device type plugin in working with its DAO components
     */
    private void setDeviceTypePluginManager() {
        if (StringUtils.isNotEmpty(deviceType)) {
            if (deviceTypePluginDAOManager != null) {
                DeviceTypePluginExtensionService deviceTypeManagerExtensionService =
                        new DeviceTypePluginExtensionServiceImpl();
                try {
                    deviceTypeManagerExtensionService.addPluginDAOManager(deviceType, deviceTypePluginDAOManager);
                } catch (DeviceTypePluginExtensionException e) {
                    String msg = "Error occurred while saving DeviceTypePluginDAOManager for device type: "
                            + deviceType;
                    log.error(msg);
                    throw new DeviceTypeDeployerPayloadException(msg);
                }
            } else {
                log.warn("Could not save DeviceTypePluginDAOManager for device type: " + deviceType +
                        " since DeviceTypePluginDAOManager is null.");
            }
        } else {
            String msg = "Could not save DeviceTypePluginDAOManager since device type is null or empty.";
            log.error(msg);
            throw new DeviceTypeDeployerPayloadException(msg);
        }
    }

    @Override
    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    @Override
    public boolean saveConfiguration(PlatformConfiguration tenantConfiguration)
            throws DeviceManagementException {
        if (tenantConfiguration == null) {
            throw new DeviceManagementException("Platform configuration is null. Cannot save the configuration");
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Persisting " + deviceType + " configurations in Registry");
            }
            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(PlatformConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(tenantConfiguration, writer);

            Resource resource = DeviceTypeUtils.getConfigurationRegistry().newResource();
            resource.setContent(writer.toString());
            resource.setMediaType(DeviceTypePluginConstants.MEDIA_TYPE_XML);
            DeviceTypeUtils.putRegistryResource(deviceType, resource);
            return true;
        } catch (DeviceTypeMgtPluginException e) {
            throw new DeviceManagementException(
                    "Error occurred while retrieving the Registry instance : " + e.getMessage(), e);
        } catch (RegistryException e) {
            throw new DeviceManagementException(
                    "Error occurred while persisting the Registry resource of " + deviceType + " Configuration : "
                            + e.getMessage(), e);
        } catch (JAXBException e) {
            throw new DeviceManagementException(
                    "Error occurred while parsing the " + deviceType + " configuration : " + e.getMessage(), e);
        }
    }

    @Override
    public PlatformConfiguration getConfiguration() throws DeviceManagementException {
        Resource resource;
        try {
            resource = DeviceTypeUtils.getRegistryResource(deviceType);
            if (resource != null) {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                XMLStreamReader reader = factory.createXMLStreamReader(
                        new StringReader(new String((byte[]) resource.getContent(), StandardCharsets.UTF_8)));

                JAXBContext context = JAXBContext.newInstance(PlatformConfiguration.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return (PlatformConfiguration) unmarshaller.unmarshal(reader);
            } else return defaultPlatformConfiguration;
        } catch (DeviceTypeMgtPluginException e) {
            throw new DeviceManagementException(
                    "Error occurred while retrieving the Registry instance : " + e.getMessage(), e);
        } catch (JAXBException | XMLStreamException e) {
            throw new DeviceManagementException(
                    "Error occurred while parsing the " + deviceType + " configuration : " + e.getMessage(), e);
        } catch (RegistryException e) {
            throw new DeviceManagementException(
                    "Error occurred while retrieving the Registry resource of " + deviceType + " Configuration : "
                            + e.getMessage(), e);
        }
    }

    @Override
    public boolean enrollDevice(Device device) throws DeviceManagementException {
        if (device == null) {
            throw new DeviceManagementException("Device is null. Cannot enroll the device.");
        }
        if (propertiesExist) {
            boolean status = false;
            boolean isEnrolled = this.isEnrolled(
                    new DeviceIdentifier(device.getDeviceIdentifier(), device.getType()));
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Enrolling a new device : " + device.getDeviceIdentifier());
                }
                if (isEnrolled) {
                    this.modifyEnrollment(device);
                } else {
                    deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                    status = deviceTypePluginDAOManager.getDeviceDAO().addDevice(device);
                    deviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
                }
            } catch (DeviceTypeMgtPluginException e) {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                String msg = "Error while enrolling the " + deviceType + " device : " + device.getDeviceIdentifier();
                throw new DeviceManagementException(msg, e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            if (propertyBasedDeviceTypePluginDAOManager != null && !isEnrolled && status) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding properties for new device : " + device.getDeviceIdentifier());
                    }
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                    status = propertyBasedDeviceTypePluginDAOManager.getDeviceDAO().addDevice(device);
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
                } catch (DeviceTypeMgtPluginException e) {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                    String msg = "Error while adding properties for " + deviceType + " device : " +
                            device.getDeviceIdentifier();
                    throw new DeviceManagementException(msg, e);
                } finally {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
                }
            }
            return status;
        }
        return true;
    }

    @Override
    public boolean modifyEnrollment(Device device) throws DeviceManagementException {
        if (propertiesExist) {
            boolean status;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Modifying properties for enrolling device : " + device.getDeviceIdentifier());
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                Device existingDevice = deviceTypePluginDAOManager.getDeviceDAO()
                        .getDevice(device.getDeviceIdentifier());
                if (existingDevice == null) {
                    status = deviceTypePluginDAOManager.getDeviceDAO().addDevice(device);
                } else {
                    status = deviceTypePluginDAOManager.getDeviceDAO().updateDevice(device);
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
            } catch (DeviceTypeMgtPluginException e) {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                String msg = "Error while updating the enrollment of the " + deviceType + " device : " +
                        device.getDeviceIdentifier();
                throw new DeviceManagementException(msg, e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            if (propertyBasedDeviceTypePluginDAOManager != null && status) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Updating properties for enrolling device : " + device.getDeviceIdentifier());
                    }
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                    status = propertyBasedDeviceTypePluginDAOManager.getDeviceDAO().updateDevice(device);
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
                } catch (DeviceTypeMgtPluginException e) {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                    String msg = "Error while updating properties for " + deviceType + " device : " +
                            device.getDeviceIdentifier();
                    throw new DeviceManagementException(msg, e);
                } finally {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
                }
            }
            return status;
        }
        return true;
    }

    @Override
    public boolean disenrollDevice(DeviceIdentifier deviceId) throws DeviceManagementException {
        //Here we don't have anything specific to do. Hence returning.
        return true;
    }

    @Override
    public boolean removeDevice(DeviceIdentifier deviceId) throws DeviceManagementException {
        //Here we don't have anything specific to do. Hence returning.
        return true;
    }

    @Override
    public boolean isEnrolled(DeviceIdentifier deviceId) throws DeviceManagementException {
        if (deviceId == null) {
            throw new DeviceManagementException("Cannot check the enrollment status of a null device");
        }
        if (propertiesExist) {
            boolean isEnrolled = false;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Checking the enrollment of Android device : " + deviceId.getId());
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                Device device =
                        deviceTypePluginDAOManager.getDeviceDAO().getDevice(deviceId.getId());
                if (device != null) {
                    isEnrolled = true;
                }
            } catch (DeviceTypeMgtPluginException e) {
                String msg = "Error while checking the enrollment status of " + deviceType + " device : " +
                        deviceId.getId();
                throw new DeviceManagementException(msg, e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            return isEnrolled;
        }
        return true;
    }

    @Override
    public boolean isActive(DeviceIdentifier deviceId) throws DeviceManagementException {
        return true;
    }

    @Override
    public boolean setActive(DeviceIdentifier deviceId, boolean status)
            throws DeviceManagementException {
        return true;
    }

    @Override
    public Device getDevice(DeviceIdentifier deviceId) throws DeviceManagementException {
        if (deviceId == null) {
            throw new DeviceManagementException("Cannot get the device. DeviceIdentifier is null");
        }
        if (propertiesExist) {
            Device device;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Getting the details of " + deviceType + " device : '" + deviceId.getId() + "'");
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                device = deviceTypePluginDAOManager.getDeviceDAO().getDevice(deviceId.getId());
            } catch (DeviceTypeMgtPluginException e) {
                throw new DeviceManagementException(
                        "Error occurred while fetching the " + deviceType + " device: '" + deviceId.getId() + "'", e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            return device;
        }
        return null;
    }

    @Override
    public boolean updateDeviceProperties(DeviceIdentifier deviceId, List<Device.Property> propertyList)
            throws DeviceManagementException {
        boolean status = false;
        if (propertiesExist) {
            Device updatedDevice = new Device();
            updatedDevice.setDeviceIdentifier(deviceId.getId());
            updatedDevice.setProperties(propertyList);
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Updating device properties of " + deviceType + " device : '"
                            + deviceId.getId() + "'");
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                status = deviceTypePluginDAOManager.getDeviceDAO().updateDevice(updatedDevice);
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
            } catch (DeviceTypeMgtPluginException e) {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                throw new DeviceManagementException(
                        "Error occurred while fetching the " + deviceType + " device: '" + deviceId.getId() + "'", e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            if (propertyBasedDeviceTypePluginDAOManager != null && status) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Updating device properties of " + deviceType + " device : '"
                                + deviceId.getId() + "'");
                    }
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                    status = propertyBasedDeviceTypePluginDAOManager.getDeviceDAO().updateDevice(updatedDevice);
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
                } catch (DeviceTypeMgtPluginException e) {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                    String msg = "Error while updating properties for " + deviceType + " device : " +
                            deviceId.getId();
                    throw new DeviceManagementException(msg, e);
                } finally {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
                }
            }
        }
        return status;
    }

    @Override
    public boolean setOwnership(DeviceIdentifier deviceId, String ownershipType)
            throws DeviceManagementException {
        return true;
    }

    @Override
    public boolean setStatus(DeviceIdentifier deviceIdentifier, String currentUser,
                             EnrolmentInfo.Status status) throws DeviceManagementException {
        return false;
    }

    @Override
    public License getLicense(String languageCode) throws LicenseManagementException {
        return licenseManager.getLicense(deviceType, languageCode);
    }

    @Override
    public void addLicense(License license) throws LicenseManagementException {
        licenseManager.addLicense(deviceType, license);
    }

    @Override
    public boolean requireDeviceAuthorization() {
        return requiredDeviceTypeAuthorization;
    }

    private PlatformConfiguration getDefaultConfiguration() throws DeviceManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Loading default " + deviceType + " platform configuration from " + deviceType +
                    "-default-platform-configuration.xml");
        }
        try {
            String platformConfigurationPath =
                    PATH_MOBILE_PLUGIN_CONF_DIR + File.separator + deviceType + "-default-platform-configuration.xml";
            File platformConfig = new File(platformConfigurationPath);

            if (platformConfig.exists()) {
                Document doc = DeviceTypeUtils.convertToDocument(platformConfig);
                JAXBContext context = JAXBContext.newInstance(PlatformConfiguration.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return (PlatformConfiguration) unmarshaller.unmarshal(doc);
            } else {
                log.warn(deviceType + "-default-platform-configuration.xml is not available, hence default " +
                        deviceType + "platform configuration cannot be loaded.");
            }
            return null;
        } catch (JAXBException e) {
            throw new DeviceManagementException(
                    "Error occurred while parsing the " + deviceType + " default platform configuration : " + e
                            .getMessage(), e);
        } catch (DeviceTypeMgtPluginException e) {
            throw new DeviceManagementException(
                    "Error occurred while parsing the " + deviceType + " default platform configuration : " + e
                            .getMessage(), e);
        }
    }

    @Override
    public boolean updateDeviceInfo(DeviceIdentifier deviceIdentifier, Device device)
            throws DeviceManagementException {
        if (propertiesExist) {
            boolean status;
            Device existingDevice = this.getDevice(deviceIdentifier);

            if (existingDevice == null) {
                return false;
            }
            existingDevice.setProperties(device.getProperties());
            try {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "updating the details of " + deviceType + " device : " + device.getDeviceIdentifier());
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                status = deviceTypePluginDAOManager.getDeviceDAO().updateDevice(existingDevice);
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
            } catch (DeviceTypeMgtPluginException e) {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                throw new DeviceManagementException(
                        "Error occurred while updating the " + deviceType + " device: '" +
                                device.getDeviceIdentifier() + "'", e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            return status;
        }
        return true;
    }

    @Override
    public List<Device> getAllDevices() throws DeviceManagementException {
        if (propertiesExist) {
            List<Device> devices = null;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Fetching the details of all " + deviceType + " devices");
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                devices = deviceTypePluginDAOManager.getDeviceDAO().getAllDevices();
            } catch (DeviceTypeMgtPluginException e) {
                throw new DeviceManagementException("Error occurred while fetching all " + deviceType + " devices", e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            return devices;
        }
        return null;
    }

    @Override
    public void deleteDevices(List<String> deviceIdentifierList) throws DeviceManagementException {
        if (propertiesExist) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Deleting the details of " + deviceType + " devices : " + deviceIdentifierList);
                }
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                if (deviceTypePluginDAOManager.getDeviceDAO().deleteDevices(deviceIdentifierList)) {
                    deviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
                } else {
                    deviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                    String msg = "Error occurred while deleting the " + deviceType + " devices: '" +
                            deviceIdentifierList;
                    log.error(msg);
                    throw new DeviceManagementException(msg);
                }
            } catch (DeviceTypeMgtPluginException e) {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                if (log.isDebugEnabled()) {
                    log.debug("Error occurred while deleting the " + deviceType + " devices: '" +
                            deviceIdentifierList + "'. Transaction rolled back");
                }
                String msg = "Error occurred while deleting the " + deviceType + " devices: '" +
                        deviceIdentifierList;
                log.error(msg, e);
                throw new DeviceManagementException(msg, e);
            } finally {
                deviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
            }
            if (propertyBasedDeviceTypePluginDAOManager != null) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Deleting the properties of " + deviceType + " devices : " + deviceIdentifierList);
                    }
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().beginTransaction();
                    if (propertyBasedDeviceTypePluginDAOManager.getDeviceDAO().deleteDevices(deviceIdentifierList)) {
                        propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().commitTransaction();
                    } else {
                        propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                        String msg = "Error occurred while deleting the properties of " + deviceType + " devices: '" +
                                deviceIdentifierList;
                        log.error(msg);
                        throw new DeviceManagementException(msg);
                    }
                } catch (DeviceTypeMgtPluginException e) {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().rollbackTransaction();
                    if (log.isDebugEnabled()) {
                        log.debug("Error occurred while deleting the properties of " + deviceType + " devices: '" +
                                deviceIdentifierList + "'. Transaction rolled back");
                    }
                    String msg = "Error occurred while deleting the properties of " + deviceType + " devices: '" +
                            deviceIdentifierList;
                    log.error(msg, e);
                    throw new DeviceManagementException(msg, e);
                } finally {
                    propertyBasedDeviceTypePluginDAOManager.getDeviceTypeDAOHandler().closeConnection();
                }
            }
        }
    }

}
