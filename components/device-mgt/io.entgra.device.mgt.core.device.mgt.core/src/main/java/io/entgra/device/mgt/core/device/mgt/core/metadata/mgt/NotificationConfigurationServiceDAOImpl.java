/*
 * Copyright (c) 2018 - 2025, Entgra (Pvt) Ltd. (http://www.entgra.io) All Rights Reserved.
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

package io.entgra.device.mgt.core.device.mgt.core.metadata.mgt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.MetadataKeyAlreadyExistsException;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.MetadataManagementException;
import io.entgra.device.mgt.core.device.mgt.common.metadata.mgt.Metadata;
import io.entgra.device.mgt.core.device.mgt.common.metadata.mgt.MetadataManagementService;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.MetadataManagementDAOException;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.util.MetadataConstants;
import io.entgra.device.mgt.core.device.mgt.core.internal.DeviceManagementDataHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.NotificationConfig;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.NotificationConfigurationList;



public class NotificationConfigurationServiceDAOImpl {
    private static final Log log = LogFactory.getLog(NotificationConfigurationServiceDAOImpl.class);
    private static final Gson gson = new Gson();
    private static final NotificationConfig notificationConfig = new NotificationConfig();
    private static NotificationConfigurationList configurations = new NotificationConfigurationList();



MetadataManagementService metadataManagementService = DeviceManagementDataHolder.getInstance().getMetadataManagementService();


    public void addNotificationConfigContext(NotificationConfigurationList configurations) throws MetadataManagementException {
        Metadata configMetadata = constructNotificationConfigContext(configurations);
        try {
            metadataManagementService.createMetadata(configMetadata);
        } catch(MetadataManagementException e) {
            throw new MetadataManagementException("Error adding notification Configuration");
        }

    }

    /**
     * Constructs a Metadata object for Notification Configuration using GSON.
     *
     * @param configurations A list of NotificationConfigDTO objects containing notification configuration details.
     * @return A Metadata object containing the serialized notification configuration.
     */
    public Metadata constructNotificationConfigContext(NotificationConfigurationList configurations) throws MetadataManagementException {
        try {
            Metadata configMetadata = new Metadata();
            configMetadata.setMetaKey(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            configMetadata.setMetaValue(gson.toJson(configurations));
            return configMetadata;
        } catch (Exception e) {
            throw new MetadataManagementException("Error constructing Notification Config Context", e);
        }
    }

    /**
     * Deletes a specific notification configuration from the Metadata context for a given tenant.
     *
     * @param configID The unique identifier (operationCode) of the notification configuration to be deleted.
     * @throws MetadataManagementException If no configuration is found with the specified operationCode, or
     * if any error occurs during the database transaction or processing
     * This method retrieves the existing notification configuration context for the given tenant, removes the
     * configuration matching the provided operationCode, and updates the Metadata context with the remaining configurations.
     */

    public void deleteNotificationConfigContext(String configID) throws MetadataManagementException {
        try {
            Metadata existingMetadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (existingMetadata == null) {
                String message = "No notification configuration context found for tenant: ";
                throw new NoSuchElementException(message);
            }

            String metaValue = existingMetadata.getMetaValue();

            //  deserialize the metaValue to a list of NotificationConfigDTO
            Type listType = new TypeToken<NotificationConfig>() {}.getType();
            NotificationConfigurationList configurations = gson.fromJson(metaValue, listType);
            // Remove configuration with the given operationCode
            boolean isRemoved = configurations.getList().removeIf(config -> config.getId().equals(configID));
            if (!isRemoved) {
                String message = "No configuration found with operationCode: " + configID;
                log.error(message);
                throw new MetadataManagementDAOException(message);
            }

            // Serialize the updated list back to JSON
            existingMetadata.setMetaValue(gson.toJson(configurations));
            metadataManagementService.updateMetadata(existingMetadata);

        } catch (NoSuchElementException  e) {
            String msg = "No notification configuration context found for tenant: ";
            log.error(msg, e);
            throw new MetadataManagementException(msg, e);
        } catch (IllegalArgumentException e){
            String msg = "Invalid notification configuration context: " + configID;
            log.error(msg, e);
            throw new MetadataManagementException(msg,e);
        }
        catch (Exception e) {
            String message = "Unexpected error occurred while deleting notification configuration";
            log.error(message, e);
            throw new MetadataManagementException(message, e);
        }
    }
    /**
     * Updates an existing notification configuration or adds a new configuration to the Metadata context for a given tenant.
     *
     * @param updatedConfig The notification configuration to be updated or added.
     *                      If a configuration with the same operationCode exists, it will be updated; otherwise, it will be added as a new entry.
     * @throws MetadataManagementException If any error occurs during the database transaction or processing.
     *
     * This method retrieves the existing notification configuration context for the given tenant. If a configuration with the same
     * operationCode as the provided configuration exists, it updates that configuration with the new details. Otherwise, it appends
     * the provided configuration as a new entry. The updated configurations are then serialized and saved back to the Metadata context.
     */
    public void updateNotificationConfigContext(NotificationConfig updatedConfig) throws MetadataManagementException {
        try {
            Metadata existingMetadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            String metaValue = existingMetadata.getMetaValue();

            if (existingMetadata != null) {
                // Deserialize the existing configurations directly to a list
                Type listType = new TypeToken<List<NotificationConfig>>() {}.getType();
                //                List<NotificationConfigDTO> configurations;
                configurations = gson.fromJson(metaValue, listType);


                // Update or add the updatedConfig
                boolean isUpdated = false;
                for (int i = 0; i < configurations.size(); i++) {
                    if (configurations.get(i).getOperationCode().equals(updatedConfig.getOperationCode())) {
                        configurations.set(i, updatedConfig);
                        isUpdated = true;
                        break;
                    }
                }
                if (!isUpdated) {
                    configurations.add(updatedConfig);
                }

            } else {
                // If no existing metadata, create a new list
                NotificationConfigurationList configurations = new NotificationConfigurationList();
                configurations.add(updatedConfig);
            }

            // Serialize the updated configurations list to JSON
            String updatedMetaValue = gson.toJson(configurations);

            Metadata updatedMetadata = new Metadata();
            updatedMetadata.setMetaKey(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            updatedMetadata.setMetaValue(updatedMetaValue);

            // Update or create the metadata record
            if (existingMetadata != null) {
                metadataManagementService.updateMetadata(updatedMetadata);
            } else {
                metadataManagementService.createMetadata(updatedMetadata);
            }
        } catch (IOException e) {
            String message = "Error processing JSON while reading or updating configurations";
            log.error(message, e);
            throw new MetadataManagementException(message, e);
        } catch (MetadataManagementException e) {
            String msg = "Unexpected error while processing notification configuration context";
            log.error(msg, e);
            throw new MetadataManagementException(msg, e);
        }
    }



    public void deleteNotificationConfigurations() throws MetadataManagementException {
        try {
            metadataManagementService.deleteMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);

        } catch (NoSuchElementException e ) {
            log.error("No Meta Data found for Tenant ID");
            throw e;
        }catch (Exception e)
        {
            String message = "Unexpected error occurred while deleting notification configurations for tenant ID: ";
            log.error(message, e);
            throw new MetadataManagementException(message, e);
        }
    }

    public NotificationConfigurationList getNotificationConfigurations(){
        try {
            Metadata existingMetadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            NotificationConfigurationList configurations = new NotificationConfigurationList();

            if (existingMetadata == null) {
                log.warn("No notification configurations found for tenant");
                configurations.setList(Collections.emptyList());
                return configurations;
            }

            String metaValue = existingMetadata.getMetaValue();

            // Directly deserialize into a List of NotificationConfig
            Type listType = new TypeToken<NotificationConfig>() {}.getType();
            NotificationConfigurationList configurations = gson.fromJson(metaValue, listType);

        } catch (MetadataManagementDAOException e) {
            String message = "Unexpected error occurred while retrieving notification configurations for tenant ID: ";
            log.error(message, e);
            throw e;
        } catch (MetadataManagementException e) {
            throw new RuntimeException(e);
        }

    }




    public NotificationConfig getNotificationConfigByCode(String configID) {
        try {
            Metadata metaData = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (metaData == null) {
                log.error("No configurations found for tenant.");
                return null;
            }


            String metaValue = metaData.getMetaValue();

            // Directly deserialize into a List of NotificationConfig
            Type listType = new TypeToken<NotificationConfig>() {}.getType();
            NotificationConfigurationList configurations = gson.fromJson(metaValue, listType);


            // Search for the configuration with the given operationCode
            if (configurations != null) {
                for (NotificationConfig config : configurations.getList()) {
                    if (config.getOperationCode().equals(configID)) {
                        return config;
                    }
                }
            }

            log.warn("Configuration with operationCode '" + configID + "' not found for tenant.");
            return null;
        } catch (MetadataManagementDAOException e) {
            String message = "Error retrieving notification configuration by operationCode.";
            log.error(message, e);
            throw e;
        } catch (MetadataManagementException e) {
            throw new RuntimeException(e);
        }



    }


}





