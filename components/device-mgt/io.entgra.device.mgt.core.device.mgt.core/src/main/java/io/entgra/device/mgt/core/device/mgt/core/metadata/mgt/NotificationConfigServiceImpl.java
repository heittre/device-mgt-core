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

package io.entgra.device.mgt.core.device.mgt.core.metadata.mgt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.MetadataManagementException;
import io.entgra.device.mgt.core.device.mgt.common.metadata.mgt.Metadata;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.MetadataManagementDAOException;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.util.MetadataConstants;
import io.entgra.device.mgt.core.device.mgt.core.dto.notification.mgt.NotificationConfigDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;


public class NotificationConfigServiceImpl {
    private static final Log log = LogFactory.getLog(NotificationConfigServiceImpl.class);

    MetadataManagementServiceImpl metadataManagementService = new MetadataManagementServiceImpl();

    public void addNotificationConfigContext(List<NotificationConfigDTO> configurations) throws MetadataManagementException, MetadataManagementDAOException {
        Metadata configMetadata = constructNotificationConfigContext(configurations);
        metadataManagementService.createMetadata(configMetadata);

    }

    /**
     * Constructs a Metadata object for Notification Configuration using ObjectMapper.
     *
     * @param configurations A list of NotificationConfigDTO objects containing notification configuration details.
     * @return A Metadata object containing the serialized notification configuration.
     */
    public Metadata constructNotificationConfigContext(List<NotificationConfigDTO> configurations) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Metadata configMetadata = new Metadata();
            configMetadata.setMetaKey(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            configMetadata.setMetaValue(objectMapper.writeValueAsString(configurations)); // Serialize the list directly
            return configMetadata;
        } catch (Exception e) {
            throw new RuntimeException("Error constructing Notification Config Context", e);
        }
    }

    /**
     * Deletes a specific notification configuration from the Metadata context for a given tenant.
     *
     * @param operationCode The unique identifier (operationCode) of the notification configuration to be deleted.
     * @throws MetadataManagementException If no configuration is found with the specified operationCode, or
     * if any error occurs during the database transaction or processing
     * This method retrieves the existing notification configuration context for the given tenant, removes the
     * configuration matching the provided operationCode, and updates the Metadata context with the remaining configurations.
     */

    public void deleteNotificationConfigContext(String operationCode) throws MetadataManagementDAOException {
        try {
            Metadata existingMetadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (existingMetadata == null) {
                String message = "No notification configuration context found for tenant: ";
                throw new NoSuchElementException(message);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String metaValue = existingMetadata.getMetaValue();

            // Directly deserialize the metaValue to a list of NotificationConfigDTO
            List<NotificationConfigDTO> configurations = objectMapper.readValue(metaValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class));

            // Remove configuration with the given operationCode
            boolean isRemoved = configurations.removeIf(config -> config.getOperationCode().equals(operationCode));
            if (!isRemoved) {
                String message = "No configuration found with operationCode: " + operationCode;
                log.error(message);
                throw new MetadataManagementDAOException(message);
            }

            // Serialize the updated list back to JSON
            existingMetadata.setMetaValue(objectMapper.writeValueAsString(configurations));
            metadataManagementService.updateMetadata(existingMetadata);

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (MetadataManagementDAOException e) {
            String message = "Error accessing database for metadata update";
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        } catch (Exception e) {
            String message = "Unexpected error occurred while deleting notification configuration";
            log.error(message, e);
            throw new RuntimeException(message, e);
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
    public void updateNotificationConfigContext(NotificationConfigDTO updatedConfig) throws MetadataManagementException {
        try {
            Metadata existingMetadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);

            ObjectMapper objectMapper = new ObjectMapper();
            List<NotificationConfigDTO> configurations;

            if (existingMetadata != null) {
                // Deserialize the existing configurations directly to a list
                configurations = objectMapper.readValue(
                        existingMetadata.getMetaValue(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
                );

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
                configurations = new ArrayList<>();
                configurations.add(updatedConfig);
            }

            // Serialize the updated configurations list to JSON
            String updatedMetaValue = objectMapper.writeValueAsString(configurations);

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
        } catch (Exception e) {
            String message = "Unexpected error while processing notification configuration context";
            log.error(message, e);
            throw new MetadataManagementException(message, e);
        }
    }



    public void deleteNotificationConfigurations() throws MetadataManagementDAOException {
        try {
            metadataManagementService.deleteMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.error("Error clearing configurations");
            throw e;
        } catch (Exception e) {
            String message = "Unexpected error occurred while deleting notification configurations for tenant ID: ";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public List<NotificationConfigDTO> getNotificationConfigurations() throws MetadataManagementDAOException {
        try {
            Metadata existingMetadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);

            if (existingMetadata == null) {
                log.warn("No notification configurations found for tenant");
                return Collections.emptyList();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            // Directly deserialize into a List of NotificationConfigDTO
            return objectMapper.readValue(
                    existingMetadata.getMetaValue(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
            );

        } catch (Exception e) {
            String message = "Unexpected error occurred while retrieving notification configurations for tenant ID: ";
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        }
    }




    public NotificationConfigDTO getNotificationConfigByCode(String operationCode) throws MetadataManagementDAOException {
        try {
            Metadata metadata = metadataManagementService.retrieveMetadata(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (metadata == null) {
                log.error("No configurations found for tenant.");
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            // Directly deserialize into a List of NotificationConfigDTO
            List<NotificationConfigDTO> configurations = objectMapper.readValue(
                    metadata.getMetaValue(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
            );

            // Search for the configuration with the given operationCode
            if (configurations != null) {
                for (NotificationConfigDTO config : configurations) {
                    if (config.getOperationCode().equals(operationCode)) {
                        return config;
                    }
                }
            }

            log.warn("Configuration with operationCode '" + operationCode + "' not found for tenant.");
            return null;
        } catch (Exception e) {
            String message = "Error retrieving notification configuration by operationCode.";
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        }
    }



}





