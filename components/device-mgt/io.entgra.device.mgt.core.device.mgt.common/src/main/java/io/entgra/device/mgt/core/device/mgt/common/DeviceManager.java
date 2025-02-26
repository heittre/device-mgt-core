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
package io.entgra.device.mgt.core.device.mgt.common;

import io.entgra.device.mgt.core.device.mgt.common.configuration.mgt.PlatformConfiguration;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.DeviceManagementException;
import io.entgra.device.mgt.core.device.mgt.common.license.mgt.License;
import io.entgra.device.mgt.core.device.mgt.common.license.mgt.LicenseManagementException;

import java.util.List;

/**
 * This represents the service provider interface that has to be implemented by any of new
 * device type plugin implementation intended to be managed through CDM.
 */
public interface DeviceManager {
    /**
     * Method to return feature manager implementation associated with a particular platform-specific plugin.
     *
     * @return Returns an instance of feature manager
     */
    FeatureManager getFeatureManager();

    /**
     * Method to save platform specific Configuration.
     * @param  configuration - A Platform configuration object which needs to save
     * @return Returns the status of the operation
     * @throws DeviceManagementException If something goes wrong while saving the configuration.
     */
    boolean saveConfiguration(PlatformConfiguration configuration) throws DeviceManagementException;

    /**
     * Method to get platform specific Configuration.
     *
     * @return Returns the platform specific tenant configurations
     * @throws DeviceManagementException If something goes wrong while fetching the configuration.
     */
    PlatformConfiguration getConfiguration() throws DeviceManagementException;

    /**
     * Method to enrolling a particular device of type mobile, IoT, etc within CDM.
     *
     * @param device Metadata corresponding to the device being enrolled
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while enrolling a device
     */
    boolean enrollDevice(Device device) throws DeviceManagementException;

    /**
     * Method to modify the metadata corresponding to device enrollment.
     *
     * @param device Modified device enrollment related metadata
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while modify the enrollment of a
     *                                   device
     */
    boolean modifyEnrollment(Device device) throws DeviceManagementException;

    /**
     * Method to disenroll a particular device from CDM.
     *
     * @param deviceId Fully qualified device identifier
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while disenrolling a device
     */
    boolean disenrollDevice(DeviceIdentifier deviceId) throws DeviceManagementException;

    /**
     * Method to delete multiple devices from CDM.
     * @param deviceIdentifiers Fully qualified device identifier list
     * @throws DeviceManagementException If some unusual behaviour is observed while deleting a device
     */
    void deleteDevices(List<String> deviceIdentifiers) throws DeviceManagementException;

    /**
     * Method to retrieve the status of the registration process of a particular device.
     *
     * @param deviceId Fully qualified device identifier
     * @return Status of enrollment
     * @throws DeviceManagementException If some unusual behaviour is observed while checking the enrollment
     *                                   status of the device
     */
    boolean isEnrolled(DeviceIdentifier deviceId) throws DeviceManagementException;

    /**
     * Method to retrieve the status of a particular device.
     *
     * @param deviceId Fully qualified device identifier
     * @return Returns if the device is active
     * @throws DeviceManagementException If some unusual behaviour is observed while checking the status
     *                                   of the device
     */
    boolean isActive(DeviceIdentifier deviceId) throws DeviceManagementException;

    /**
     * Method to set the status indicating whether a particular device registered within CDM is enabled at a given
     * moment.
     *
     * @param deviceId Fully qualified device identifier
     * @param status   Indicates whether the device is active
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while updating the active status
     *                                   of the device
     */
    boolean setActive(DeviceIdentifier deviceId, boolean status) throws DeviceManagementException;

    /**
     * Method to retrieve metadata of all devices registered within CDM corresponding to a particular device type.
     *
     * @return List of metadata corresponding to all devices registered within CDM
     * @throws DeviceManagementException If some unusual behaviour is observed while obtaining the enrolled device list
     */
    List<Device> getAllDevices() throws DeviceManagementException;

    /**
     * Method to retrieve metadata of a device corresponding to a particular type that carries a specific identifier.
     *
     * @param deviceId Fully qualified device identifier
     * @return Metadata corresponding to a particular device
     * @throws DeviceManagementException If some unusual behaviour is observed obtaining the device object
     */
    Device getDevice(DeviceIdentifier deviceId) throws DeviceManagementException;

    /**
     * Method to update device properties.
     *
     * @param deviceId identifier to identify the device
     * @param list device properties list
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while updating the device properties
     */
    boolean updateDeviceProperties(DeviceIdentifier deviceId,  List<Device.Property> list) throws DeviceManagementException;

    /**
     * Method to update device information.
     *
     * @param deviceIdentifier identifier to identify the device
     * @param device           Updated device information related data
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while updating the device info
     */
    boolean updateDeviceInfo(DeviceIdentifier deviceIdentifier, Device device) throws DeviceManagementException;

    /**
     * Method to set the ownership type of a particular device. i.e. BYOD, COPE.
     *
     * @param deviceId      Fully qualified device identifier
     * @param ownershipType Type of ownership
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while setting the ownership
     *                                   of the device
     */
    boolean setOwnership(DeviceIdentifier deviceId, String ownershipType) throws DeviceManagementException;

    boolean setStatus(DeviceIdentifier deviceId, String currentOwner,
                      EnrolmentInfo.Status status) throws DeviceManagementException;

    License getLicense(String languageCode) throws LicenseManagementException;

    void addLicense(License license) throws LicenseManagementException;

    /**
     * Method to enable to device-authentication for the device-platform.
     *
     * @return Returns boolean status to enable or disable device-authentication.
     */
    boolean requireDeviceAuthorization();

    /**
     * Method to remove a particular device from CDM.
     *
     * @param deviceId Fully qualified device identifier
     * @return A boolean indicating the status of the operation.
     * @throws DeviceManagementException If some unusual behaviour is observed while removing a device
     */
    boolean removeDevice(DeviceIdentifier deviceId) throws DeviceManagementException;

}
