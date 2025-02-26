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

package io.entgra.device.mgt.core.device.mgt.core.service;

import io.entgra.device.mgt.core.device.mgt.common.*;
import io.entgra.device.mgt.core.device.mgt.common.configuration.mgt.ConfigurationManagementException;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.MetadataManagementException;
import io.entgra.device.mgt.core.device.mgt.common.metadata.mgt.DeviceStatusManagementService;
import io.entgra.device.mgt.core.device.mgt.core.config.ui.UIConfigurationManager;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.entgra.device.mgt.core.device.mgt.common.Device;
import io.entgra.device.mgt.core.device.mgt.common.DeviceIdentifier;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.DeviceManagementException;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.TransactionManagementException;
import io.entgra.device.mgt.core.device.mgt.common.license.mgt.License;
import io.entgra.device.mgt.core.device.mgt.core.TestDeviceManagementService;
import io.entgra.device.mgt.core.device.mgt.core.authorization.DeviceAccessAuthorizationServiceImpl;
import io.entgra.device.mgt.core.device.mgt.core.common.BaseDeviceManagementTest;
import io.entgra.device.mgt.core.device.mgt.core.common.TestDataHolder;
import io.entgra.device.mgt.core.device.mgt.core.config.DeviceConfigurationManager;
import io.entgra.device.mgt.core.device.mgt.core.dao.DeviceDAO;
import io.entgra.device.mgt.core.device.mgt.core.dao.DeviceManagementDAOException;
import io.entgra.device.mgt.core.device.mgt.core.dao.DeviceManagementDAOFactory;
import io.entgra.device.mgt.core.device.mgt.core.device.details.mgt.dao.DeviceDetailsDAO;
import io.entgra.device.mgt.core.device.mgt.core.device.details.mgt.dao.DeviceDetailsMgtDAOException;
import io.entgra.device.mgt.core.device.mgt.core.device.details.mgt.impl.DeviceInformationManagerImpl;
import io.entgra.device.mgt.core.device.mgt.core.dto.DeviceType;
import io.entgra.device.mgt.core.device.mgt.core.internal.DeviceManagementDataHolder;
import io.entgra.device.mgt.core.device.mgt.core.internal.DeviceManagementServiceComponent;
import io.entgra.device.mgt.core.device.mgt.core.mock.MockConnection;
import io.entgra.device.mgt.core.device.mgt.core.mock.MockDataSource;
import io.entgra.device.mgt.core.device.mgt.core.mock.MockResultSet;
import io.entgra.device.mgt.core.device.mgt.core.mock.MockStatement;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.internal.RegistryDataHolder;
import org.wso2.carbon.registry.core.jdbc.realm.InMemoryRealmService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;

public class DeviceManagementProviderServiceTest extends BaseDeviceManagementTest {

    private static final Log log = LogFactory.getLog(DeviceManagementProviderServiceTest.class);
    public static final String DEVICE_ID = "9999";
    private static final String ALTERNATE_DEVICE_ID = "1128";
    private static final String DEVICE_TYPE = "RANDOM_DEVICE_TYPE";
    private final DeviceDetailsDAO deviceDetailsDAO = DeviceManagementDAOFactory.getDeviceDetailsDAO();
    private static final String MDM_CONFIG_LOCATION = "src" + File.separator + "test" + File.separator + "resources" +
            File.separator + "config" + File.separator + "operation" + File.separator + "mdm-ui-config.xml";

    DeviceManagementProviderService deviceMgtService;

    @BeforeClass
    public void init() throws Exception {
        DeviceConfigurationManager.getInstance().initConfig();
        log.info("Initializing");

        deviceMgtService = new DeviceManagementProviderServiceImpl();
        DeviceManagementServiceComponent.notifyStartupListeners();
        DeviceManagementDataHolder.getInstance().setDeviceManagementProvider(deviceMgtService);
        DeviceManagementDataHolder.getInstance().setRegistryService(getRegistryService());
        DeviceManagementDataHolder.getInstance().setDeviceAccessAuthorizationService(new DeviceAccessAuthorizationServiceImpl());
        DeviceManagementDataHolder.getInstance().setGroupManagementProviderService(new GroupManagementProviderServiceImpl());
        DeviceManagementDataHolder.getInstance().setDeviceInformationManager(new DeviceInformationManagerImpl());
        DeviceManagementDataHolder.getInstance().setDeviceTaskManagerService(null);
        deviceMgtService.registerDeviceType(new TestDeviceManagementService(DEVICE_TYPE,
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME));
        UIConfigurationManager.getInstance().initConfig(MDM_CONFIG_LOCATION);
    }

    private RegistryService getRegistryService() throws RegistryException {
        RealmService realmService = new InMemoryRealmService();
        RegistryDataHolder.getInstance().setRealmService(realmService);
        DeviceManagementDataHolder.getInstance().setRealmService(realmService);
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("carbon-home/repository/conf/registry.xml");
        RegistryContext context = RegistryContext.getBaseInstance(is, realmService);
        context.setSetup(true);
        return context.getEmbeddedRegistryService();
    }

    @Test
    public void testGetAvailableDeviceTypes() throws DeviceManagementException {
        List<DeviceType> deviceTypes = deviceMgtService.getDeviceTypes();
        if (!isMock()) {
            Assert.assertTrue(deviceTypes.size() > 0);
        }
    }

    @Test
    public void testGetAvailableDeviceType() throws DeviceManagementException {
        DeviceType deviceType = deviceMgtService.getDeviceType(DEVICE_TYPE);
        if (!isMock()) {
            Assert.assertTrue(deviceType.getName().equalsIgnoreCase(DEVICE_TYPE));
        }
    }

    @Test
    public void addLicense() throws DeviceManagementException {
        License license = new License();
        license.setLanguage("ENG");
        license.setName("RANDON_DEVICE_LICENSE");
        deviceMgtService.addLicense(DEVICE_TYPE, license);
    }

    @Test(expectedExceptions = DeviceManagementException.class)
    public void testNullDeviceEnrollment() throws DeviceManagementException {
        deviceMgtService.enrollDevice(null);
    }

    @Test
    public void testSuccessfulDeviceEnrollment() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        Device device = TestDataHolder.generateDummyDeviceData(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE));
        MockDataSource dataSource = null;
        if (isMock()) {
            Field datasourceField = DeviceManagementDAOFactory.class.getDeclaredField("dataSource");
            datasourceField.setAccessible(true);
            dataSource = (MockDataSource) getDataSource();
            dataSource.setConnection(new MockConnection(dataSource.getUrl()));

            MockConnection connection = new MockConnection(dataSource.getUrl());
            dataSource.setConnection(connection);
            MockStatement mockStatement = new MockStatement();
            MockResultSet resultSet = new MockResultSet();
            resultSet.addInteger(1);
            resultSet.addString(null);

            mockStatement.addResultSet(resultSet);
            connection.addMockStatement(mockStatement);

            datasourceField.set(datasourceField, dataSource);
        }
        try {
            boolean enrollmentStatus = deviceMgtService.enrollDevice(device);
            Assert.assertTrue(enrollmentStatus);
        } finally {
            if (dataSource != null) {
                dataSource.reset();
            }
        }
    }

    @Test(dependsOnMethods = "testSuccessfulDeviceEnrollment")
    public void testIsEnrolled() throws DeviceManagementException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId(DEVICE_ID);
        deviceIdentifier.setType(DEVICE_TYPE);
        boolean enrollmentStatus = deviceMgtService.isEnrolled(deviceIdentifier);
        if (!isMock()) {
            Assert.assertTrue(enrollmentStatus);
        }
    }

    @Test
    public void testIsEnrolledForNonExistingDevice() throws DeviceManagementException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId("34535235235235235");
        deviceIdentifier.setType(DEVICE_TYPE);
        boolean enrollmentStatus = deviceMgtService.isEnrolled(deviceIdentifier);
        Assert.assertFalse(enrollmentStatus);
    }

    @Test(expectedExceptions = DeviceManagementException.class)
    public void testIsEnrolledForNullDevice() throws DeviceManagementException {
        deviceMgtService.isEnrolled(null);
    }

    @Test
    public void testNonExistentDeviceType() throws DeviceManagementException {
        Device device = TestDataHolder.generateDummyDeviceData("abc");
        boolean enrollmentStatus = deviceMgtService.enrollDevice(device);
        Assert.assertFalse(enrollmentStatus);
    }


    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testReEnrollmentofSameDeviceUnderSameUser() throws DeviceManagementException, MetadataManagementException {
        if (!isMock()) {
            DeviceStatusManagementService deviceStatusManagementService = DeviceManagementDataHolder
                    .getInstance().getDeviceStatusManagementService();
            deviceStatusManagementService.addDefaultDeviceStatusFilterIfNotExist(MultitenantConstants.SUPER_TENANT_ID);
            Device device = TestDataHolder.generateDummyDeviceData(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE));
            boolean enrollment = deviceMgtService.enrollDevice(device);
            Assert.assertTrue(enrollment);
        }
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceUnderSameUser"})
    public void testReEnrollmentofSameDeviceWithOtherUser() throws DeviceManagementException {
        if (!isMock()) {
            EnrolmentInfo enrolmentInfo = new EnrolmentInfo();
            enrolmentInfo.setDateOfEnrolment(new Date().getTime());
            enrolmentInfo.setDateOfLastUpdate(new Date().getTime());
            enrolmentInfo.setOwner("user1");
            enrolmentInfo.setOwnership(EnrolmentInfo.OwnerShip.BYOD);
            enrolmentInfo.setStatus(EnrolmentInfo.Status.ACTIVE);

            Device alternateDevice = TestDataHolder.generateDummyDeviceData(DEVICE_ID, DEVICE_TYPE,
                    enrolmentInfo);
            Device retrievedDevice1 = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                    DEVICE_TYPE));

            deviceMgtService.enrollDevice(alternateDevice);
            Device retrievedDevice2 = deviceMgtService.getDevice(new DeviceIdentifier(alternateDevice
                    .getDeviceIdentifier(), alternateDevice.getType()));

            Assert.assertFalse(retrievedDevice1.getEnrolmentInfo().getOwner().equalsIgnoreCase
                    (retrievedDevice2.getEnrolmentInfo().getOwner()));
        }
    }


    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceWithOtherUser"})
    public void testDisenrollment() throws DeviceManagementException {
        if (!isMock()) {
            Device device = TestDataHolder.generateDummyDeviceData(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE));
            boolean disenrollmentStatus = deviceMgtService.removeDevice(new DeviceIdentifier
                    (device.getDeviceIdentifier(), device.getType()));
            log.info(disenrollmentStatus);
            Assert.assertTrue(disenrollmentStatus);
        }
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceWithOtherUser"}, expectedExceptions =
            DeviceManagementException.class)
    public void testDisenrollmentWithNullDeviceID() throws DeviceManagementException {
        deviceMgtService.disenrollDevice(null);
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceWithOtherUser"})
    public void testDisenrollmentWithNonExistentDT() throws DeviceManagementException {
        Device device = TestDataHolder.generateDummyDeviceData(new DeviceIdentifier(DEVICE_ID,
                "NON_EXISTENT_DT"));
        boolean result = deviceMgtService.disenrollDevice(new DeviceIdentifier(
                device.getDeviceIdentifier(), device.getType()));
        Assert.assertTrue(!result);
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceWithOtherUser"})
    public void testDisenrollmentWithNonExistentDevice() throws DeviceManagementException {
        Device device = TestDataHolder.generateDummyDeviceData(new DeviceIdentifier(ALTERNATE_DEVICE_ID,
                DEVICE_TYPE));
        boolean result = deviceMgtService.disenrollDevice(new DeviceIdentifier(
                device.getDeviceIdentifier(), device.getType()));
        Assert.assertTrue(!result);
    }

    @Test(dependsOnMethods = {"testDisenrollment"})
    public void testDisenrollAlreadyDisEnrolledDevice() throws DeviceManagementException {
        if (!isMock()) {
            Device device = TestDataHolder.generateDummyDeviceData(new DeviceIdentifier(DEVICE_ID,
                    DEVICE_TYPE));
            boolean result = deviceMgtService.removeDevice(new DeviceIdentifier(
                    device.getDeviceIdentifier(), device.getType()));
            Assert.assertTrue(result);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceCount() throws DeviceManagementException {
        int count = deviceMgtService.getDeviceCount();
        if (!isMock()) {
            Assert.assertTrue(count > 0);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceCountForUser() throws DeviceManagementException {
        int count = deviceMgtService.getDeviceCount(TestDataHolder.OWNER);
        if (!isMock()) {
            Assert.assertTrue(count > 0);
        }
    }

    @Test
    public void testGetDeviceCountForNonExistingUser() throws DeviceManagementException {
        int count = deviceMgtService.getDeviceCount("ABCD");
        Assert.assertEquals(count, 0);
    }

    @Test(expectedExceptions = DeviceManagementException.class)
    public void testGetDeviceCountForNullUser() throws DeviceManagementException {
        deviceMgtService.getDeviceCount((String) null);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testIsActive() throws DeviceManagementException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId(TestDataHolder.initialDeviceIdentifier);
        deviceIdentifier.setType(DEVICE_TYPE);
        Assert.assertTrue(deviceMgtService.isActive(deviceIdentifier));
    }

    @Test
    public void testIsActiveForNonExistingDevice() throws DeviceManagementException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId("34535235235235235");
        deviceIdentifier.setType("TEST_TYPE");
        Assert.assertFalse(deviceMgtService.isActive(deviceIdentifier));
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testSetActive() throws DeviceManagementException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId(TestDataHolder.initialDeviceIdentifier);
        deviceIdentifier.setType(DEVICE_TYPE);
        Assert.assertFalse(deviceMgtService.setActive(deviceIdentifier, true));
    }

    @Test
    public void testSetActiveForNonExistingDevice() throws DeviceManagementException {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId("34535235235235235");
        deviceIdentifier.setType("TEST_TYPE");
        Assert.assertFalse(deviceMgtService.setActive(deviceIdentifier, true));
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceEnrolledTenants() throws DeviceManagementException {
        List<Integer> tenants = deviceMgtService.getDeviceEnrolledTenants();
        if (!isMock()) {
            Assert.assertEquals(tenants.size(), 1);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDevice() throws DeviceManagementException, NoSuchFieldException, IllegalAccessException {
        MockDataSource dataSource = setDatasourceForGetDevice();
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE));
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(device.getDeviceIdentifier().equalsIgnoreCase(DEVICE_ID));
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceWithInfo() throws DeviceManagementException {
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE)
                , true);
        if (!isMock()) {
            Assert.assertTrue(device.getDeviceInfo() != null);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceByID() throws DeviceManagementException {
        Device device = deviceMgtService.getDevice(DEVICE_ID, true);
        if (!isMock()) {
            Assert.assertTrue(device.getDeviceIdentifier().equalsIgnoreCase(DEVICE_ID));
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetDeviceByIDWithNullID() throws DeviceManagementException {
        deviceMgtService.getDevice((String) null, true);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceByIDAndSinceDate() throws DeviceManagementException, DeviceDetailsMgtDAOException
            , TransactionManagementException {
        Device initialDevice = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE));
        addDeviceInformation(initialDevice);
        Device device = deviceMgtService.getDevice(DEVICE_ID, yesterday(), true);
        if (!isMock()) {
            Assert.assertTrue(device != null);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetDeviceByIDAndSinceDateWithNullID() throws DeviceManagementException, DeviceDetailsMgtDAOException
            , TransactionManagementException {
        Device initialDevice = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE));
        addDeviceInformation(initialDevice);
        deviceMgtService.getDevice((String)null, yesterday(), true);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetDeviceByIDAndSinceDateWithNullDate() throws DeviceManagementException,
                DeviceDetailsMgtDAOException, TransactionManagementException {
        Device initialDevice = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE));
        addDeviceInformation(initialDevice);
        deviceMgtService.getDevice(DEVICE_ID, (Date)null, true);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceTypeWithProps() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        MockDataSource dataSource = setDatasourceForGetDevice();
        Device device = deviceMgtService.getDeviceWithTypeProperties(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE));
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(!device.getProperties().isEmpty());
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviceWithOutInfo() throws DeviceManagementException {
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID, DEVICE_TYPE)
                , false);
        if (!isMock()) {
            Assert.assertTrue(device.getDeviceInfo() == null);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesOfRole() throws DeviceManagementException, NoSuchFieldException, IllegalAccessException {
        MockDataSource dataSource = setDatasourceForGetDevice();
        List<Device> devices = deviceMgtService.getAllDevicesOfRole("admin");
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(devices.size() > 0);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetAllDevicesOfRoleFailureFlow() throws DeviceManagementException, UserStoreException, NoSuchFieldException, IllegalAccessException {
        int tenantID = -1234;
        RealmService mockRealmService = Mockito.mock(RealmService.class, Mockito.CALLS_REAL_METHODS);

        Mockito.doThrow(new UserStoreException("Mocked Exception when obtaining Tenant Realm"))
                .when(mockRealmService).getTenantUserRealm(tenantID);
        RealmService currentRealm = DeviceManagementDataHolder.getInstance().getRealmService();
        DeviceManagementDataHolder.getInstance().setRealmService(mockRealmService);
        try {
            deviceMgtService.getAllDevicesOfRole("admin");
        } finally {
            DeviceManagementDataHolder.getInstance().setRealmService(currentRealm);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesOfRoleWithNonExistentRole() throws DeviceManagementException {
        List<Device> devices = deviceMgtService.getAllDevicesOfRole("non-existent-role");
        Assert.assertTrue(devices.size() == 0);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetAllDevicesOfRoleWithNullArgs() throws DeviceManagementException {
        deviceMgtService.getAllDevicesOfRole(null);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testDeviceByOwner() throws DeviceManagementException {
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE), "admin", true);
        if (!isMock()) {
            Assert.assertTrue(device != null);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testDeviceByOwnerAndNonExistentDeviceID() throws DeviceManagementException {
        String nonExistentDeviceID = "4455";
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(nonExistentDeviceID,
                DEVICE_TYPE), "admin", true);
        Assert.assertTrue(device == null);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testDeviceByOwnerWithNullDeviceID() throws DeviceManagementException {
        deviceMgtService.getDevice((DeviceIdentifier) null, "admin", true);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testDeviceByDate() throws DeviceManagementException, TransactionManagementException,
            DeviceDetailsMgtDAOException, NoSuchFieldException, IllegalAccessException {
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        Device initialDevice = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE));
        addDeviceInformation(initialDevice);
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE), yesterday());
        cleanupMockDatasource(dataSource);
        if (!isMock()) {
            Assert.assertTrue(device != null);
        }
    }

    private MockResultSet getMockGetDeviceResult() {
        MockResultSet resultSet = new MockResultSet();
        resultSet.addInteger(1);
        resultSet.addString("Test");
        resultSet.addString(null);
        resultSet.addString(DEVICE_TYPE);
        resultSet.addString(DEVICE_ID);
        resultSet.addInteger(0);
        resultSet.addString("admin");
        resultSet.addString("BYOD");
        resultSet.addTimestamp(new Timestamp(System.currentTimeMillis()));
        resultSet.addTimestamp(new Timestamp(System.currentTimeMillis()));
        resultSet.addString("ACTIVE");
        return resultSet;
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testUpdateDeviceInfo() throws DeviceManagementException,
            TransactionManagementException, DeviceDetailsMgtDAOException {
        if (!isMock()) {
            Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                    DEVICE_TYPE));

            boolean status = deviceMgtService.updateDeviceInfo(new DeviceIdentifier(DEVICE_ID,
                    DEVICE_TYPE), device);
            Assert.assertTrue(status);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testDeviceByDateWithNonExistentDevice() throws DeviceManagementException,
            TransactionManagementException, DeviceDetailsMgtDAOException {
        Device device = deviceMgtService.getDevice(new DeviceIdentifier(ALTERNATE_DEVICE_ID,
                DEVICE_TYPE), yesterday());
        Assert.assertTrue(device == null);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testDeviceByDateWithNullDeviceID() throws DeviceManagementException {
        deviceMgtService.getDevice((DeviceIdentifier) null, yesterday());
    }

    private void addDeviceInformation(Device initialDevice) throws TransactionManagementException, DeviceDetailsMgtDAOException {
        DeviceManagementDAOFactory.beginTransaction();

        //Device details table will be reffered when looking for last updated time
        //This dao entry is to mimic a device info operation
        deviceDetailsDAO.addDeviceInformation(initialDevice.getId(), initialDevice.getEnrolmentInfo().getId(),
                TestDataHolder
                .generateDummyDeviceInfo());

        DeviceManagementDAOFactory.closeConnection();
    }

    @Test(dependsOnMethods = {"testDeviceByDate"})
    public void testDeviceByDateAndOwner() throws DeviceManagementException {
        if (!isMock()) {
            Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                    DEVICE_TYPE), "admin", yesterday(), true);
            Assert.assertTrue(device != null);
        }
    }

    @Test
    public void testGetAvaliableDeviceTypes() throws DeviceManagementException {
        List<String> deviceTypes = deviceMgtService.getAvailableDeviceTypes();
        if (!isMock()) {
            Assert.assertTrue(!deviceTypes.isEmpty());
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevices() throws DeviceManagementException, NoSuchFieldException, IllegalAccessException {
        MockDataSource dataSource = setDatasourceForGetDevice();
        List<Device> devices = deviceMgtService.getAllDevices();
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(!devices.isEmpty());
    }

    private MockDataSource setDatasourceForGetDevice() throws IllegalAccessException, NoSuchFieldException {
        MockDataSource dataSource = null;
        if (isMock()) {
            Field datasourceField = DeviceManagementDAOFactory.class.getDeclaredField("dataSource");
            datasourceField.setAccessible(true);
            dataSource = (MockDataSource) getDataSource();

            //connection used for first get device operation.
            MockConnection connection = new MockConnection(dataSource.getUrl());
            dataSource.setConnection(connection);
            MockStatement mockStatement = new MockStatement();
            mockStatement.addResultSet(getMockGetDeviceResult());
            connection.addMockStatement(mockStatement);

            datasourceField.set(datasourceField, dataSource);
        }
        return dataSource;
    }

    private void cleanupMockDatasource(MockDataSource dataSource) {
        if (isMock()) {
            if (dataSource != null) {
                dataSource.reset();
            }
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesPaginated() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        request.setOwnerRole("admin");
        MockDataSource dataSource = setDatasourceForGetDevice();
        PaginationResult result = deviceMgtService.getAllDevices(request);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetAllDevicesWithNullRequest() throws DeviceManagementException {
        PaginationRequest request = null;
        deviceMgtService.getAllDevices(request);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesByName() throws DeviceManagementException, NoSuchFieldException, IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        request.setDeviceName(DEVICE_TYPE + "-" + DEVICE_ID);
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        PaginationResult result = deviceMgtService.getDevicesByName(request);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesByNameAndType() throws DeviceManagementException, NoSuchFieldException, IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        request.setDeviceName(DEVICE_TYPE + "-" + DEVICE_ID);
        request.setDeviceType(DEVICE_TYPE);
        MockDataSource dataSource = setDatasourceForGetDevice();
        List<Device> devices = deviceMgtService.getDevicesByNameAndType(request, true);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(!devices.isEmpty());
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesByStatus() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        List<String> statusList = new ArrayList<>(Collections.singletonList("ACTIVE"));
        request.setStatusList(statusList);
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        PaginationResult result = deviceMgtService.getDevicesByStatus(request, true);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);
    }

    private void setMockDeviceCount(MockConnection connection) {
        MockStatement statement = new MockStatement();
        connection.addMockStatement(statement);
        MockResultSet resultSet = new MockResultSet();
        resultSet.addInteger(1);
        statement.addResultSet(resultSet);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDevicesOfTypePaginated() throws DeviceManagementException {
        if (!isMock()) {
            PaginationRequest request = new PaginationRequest(0, 100);
            request.setDeviceType(DEVICE_TYPE);
            PaginationResult result = deviceMgtService.getDevicesByType(request);
            Assert.assertTrue(result.getRecordsTotal() > 0);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesWithInfo() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        MockDataSource dataSource = setDatasourceForGetDevice();
        List<Device> devices = deviceMgtService.getAllDevices(true);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(!devices.isEmpty());
        Assert.assertTrue(devices.get(0).getDeviceInfo() != null);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetAllDevicesWithInfoPaginated() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        PaginationResult result = deviceMgtService.getAllDevices(request, true);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetTenantedDevice() throws DeviceManagementException {
        SingletonMap deviceMap = deviceMgtService.getTenantedDevice(new
                DeviceIdentifier
                (DEVICE_ID, DEVICE_TYPE), false);
        if (!isMock()) {
            Assert.assertFalse(deviceMap.isEmpty());
        }
    }

    @Test
    public void testGetLicense() throws DeviceManagementException {
        License license = deviceMgtService.getLicense(DEVICE_TYPE, "ENG");
        Assert.assertTrue(license.getLanguage().equalsIgnoreCase("ENG"));
    }

    @Test(expectedExceptions = DeviceManagementException.class)
    public void testSendRegistrationEmailNoMetaInfo() throws ConfigurationManagementException, DeviceManagementException {
        deviceMgtService.sendRegistrationEmail(null);
        Assert.assertTrue(false);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetDeviesOfUser() throws DeviceManagementException {
        String username = null;
        deviceMgtService.getDevicesOfUser(username);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviesOfUserWhileUserNull() throws DeviceManagementException {
        if (!isMock()) {
            List<Device> devices = deviceMgtService.getDevicesOfUser("admin");
            Assert.assertTrue(!devices.isEmpty());
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDevieByStatus() throws DeviceManagementException {
        if (!isMock()) {
            Device device = deviceMgtService.getDevice(new DeviceIdentifier(DEVICE_ID,
                    DEVICE_TYPE), EnrolmentInfo.Status.ACTIVE);
            Assert.assertTrue(device != null);
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDevieByDate() throws DeviceManagementException {
        if (!isMock()) {
            List<Device> devices = deviceMgtService.getDevices(yesterday());
            Assert.assertTrue(!devices.isEmpty());
        }
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviesOfUserPaginated() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        request.setOwner("admin");
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        PaginationResult result = deviceMgtService.getDevicesOfUser(request, true);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);

    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testGetDeviesOfUserWhileNullOwnerPaginated() throws DeviceManagementException {
        PaginationRequest request = null;
        deviceMgtService.getDevicesOfUser(request, true);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviesByOwnership() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        request.setOwnership(EnrolmentInfo.OwnerShip.BYOD.toString());
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        PaginationResult result = deviceMgtService.getDevicesByOwnership(request);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);

    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testSetOwnership() throws DeviceManagementException {
        boolean status = deviceMgtService.setOwnership(new DeviceIdentifier(DEVICE_ID,
                DEVICE_TYPE), EnrolmentInfo.OwnerShip.COPE.toString());
        Assert.assertTrue(status);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testSetOwnershipNonExistentDT() throws DeviceManagementException {
        boolean status = deviceMgtService.setOwnership(new DeviceIdentifier(DEVICE_ID,
                "non-existent-dt"), EnrolmentInfo.OwnerShip.COPE.toString());
        Assert.assertFalse(status);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"}, expectedExceptions =
            DeviceManagementException.class)
    public void testSetOwnershipOfNullDevice() throws DeviceManagementException {
        deviceMgtService.setOwnership(null, EnrolmentInfo.OwnerShip.COPE.toString());
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviesByStatus() throws DeviceManagementException, NoSuchFieldException,
            IllegalAccessException {
        PaginationRequest request = new PaginationRequest(0, 100);
        List<String> statusList = new ArrayList<>(Collections.singletonList("ACTIVE"));
        request.setStatusList(statusList);
        MockDataSource dataSource = setDatasourceForGetDevice();
        if (dataSource != null) {
            setMockDeviceCount(dataSource.getConnection(0));
        }
        PaginationResult result = deviceMgtService.getDevicesByStatus(request);
        cleanupMockDatasource(dataSource);
        Assert.assertTrue(result.getRecordsTotal() > 0);
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceUnderSameUser"})
    public void testUpdateDevicesStatus() throws DeviceManagementException {
        if (!isMock()) {
            boolean status = deviceMgtService.setStatus("user1", EnrolmentInfo.Status.REMOVED);
            Assert.assertTrue(status);
        }
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceUnderSameUser"})
    public void testUpdateDevicesStatusWithDeviceID() throws DeviceManagementException {
        if (!isMock()) {
            Device device = TestDataHolder.generateDummyDeviceData("abc");
            try {
                int tenantId = TestDataHolder.SUPER_TENANT_ID;
                DeviceManagementDAOFactory.beginTransaction();
                DeviceDAO deviceDAO = DeviceManagementDAOFactory.getDeviceDAO();
                int deviceId = deviceDAO.addDevice(TestDataHolder.initialTestDeviceType.getId(), device, tenantId);
                device.setId(deviceId);
                int enrolmentId = deviceDAO.addEnrollment(device, tenantId);
                device.getEnrolmentInfo().setId(enrolmentId);
                DeviceManagementDAOFactory.commitTransaction();
            } catch (DeviceManagementDAOException e) {
                DeviceManagementDAOFactory.rollbackTransaction();
                String msg = "Error occurred while adding '" + device.getType() + "' device with the identifier '" +
                        device.getDeviceIdentifier() + "'";
                log.error(msg, e);
                Assert.fail(msg, e);
            } catch (TransactionManagementException e) {
                String msg = "Error occurred while initiating transaction";
                log.error(msg, e);
                Assert.fail(msg, e);
            } finally {
                DeviceManagementDAOFactory.closeConnection();
            }
            boolean status = deviceMgtService.setStatus(device, EnrolmentInfo.Status.ACTIVE);
            Assert.assertTrue(status);
        }
    }

    @Test(dependsOnMethods = {"testReEnrollmentofSameDeviceUnderSameUser"})
    public void testUpdateDevicesStatusOfNonExistingUser() throws DeviceManagementException {
        boolean status = deviceMgtService.setStatus("random-user", EnrolmentInfo.Status.REMOVED);
        Assert.assertFalse(status);
    }

    @Test(dependsOnMethods = {"testSuccessfulDeviceEnrollment"})
    public void testGetDeviesOfUserAndDeviceType() throws DeviceManagementException {
        if (!isMock()) {
            List<Device> devices = deviceMgtService.getDevicesOfUser("admin", DEVICE_TYPE, true);
            Assert.assertTrue(!devices.isEmpty() && devices.get(0).getType().equalsIgnoreCase
                    (DEVICE_TYPE) && devices.get(0).getDeviceInfo() != null);
        }
    }

    @Test
    public void testSendRegistrationEmailSuccessFlow() throws ConfigurationManagementException, DeviceManagementException {
        String recipient = "test-user@wso2.com";
        Properties props = new Properties();
        props.setProperty("first-name", "Test");
        props.setProperty("username", "User");
        props.setProperty("password", "!@#$$$%");

        EmailMetaInfo metaInfo = new EmailMetaInfo(recipient, props);
        deviceMgtService.sendRegistrationEmail(metaInfo);
        Assert.assertTrue(true);
    }

    @Test
    public void testSendEnrollmentInvitation() throws ConfigurationManagementException,
            DeviceManagementException {
        String recipient = "test-user@wso2.com";
        Properties props = new Properties();
        props.setProperty("first-name", "Test");
        props.setProperty("username", "User");
        props.setProperty("password", "!@#$$$%");

        EmailMetaInfo metaInfo = new EmailMetaInfo(recipient, props);

        deviceMgtService.sendEnrolmentInvitation("template-name", metaInfo);
        Assert.assertTrue(true);
    }

    private Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }
}
