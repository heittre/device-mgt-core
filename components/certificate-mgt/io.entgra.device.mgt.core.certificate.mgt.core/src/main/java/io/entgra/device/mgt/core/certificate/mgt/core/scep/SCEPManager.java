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

package io.entgra.device.mgt.core.certificate.mgt.core.scep;

import io.entgra.device.mgt.core.device.mgt.common.DeviceIdentifier;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.DeviceManagementException;
import org.wso2.carbon.user.api.UserStoreException;

public interface SCEPManager {
    /**
     * This method is used to retrieve a device of a given identifier wrapped by a
     * {@link TenantedDeviceWrapper} with it's tenant info
     *
     * @param deviceIdentifier device identifier
     * @return {@link TenantedDeviceWrapper} with a device's info and tenant info
     * @throws SCEPException will be thrown in case device is null or if a
     *                       {@link DeviceManagementException} or a {@link UserStoreException} is thrown
     */
    TenantedDeviceWrapper getValidatedDevice(DeviceIdentifier deviceIdentifier)
            throws SCEPException;
}
