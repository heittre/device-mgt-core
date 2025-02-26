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

package io.entgra.device.mgt.core.application.mgt.core.impl;

import io.entgra.device.mgt.core.application.mgt.common.ReleaseVersionInfo;
import io.entgra.device.mgt.core.application.mgt.common.exception.FileDownloaderServiceException;
import io.entgra.device.mgt.core.application.mgt.core.exception.BadRequestException;
import io.entgra.device.mgt.core.application.mgt.core.dao.*;
import io.entgra.device.mgt.core.application.mgt.core.exception.*;
import io.entgra.device.mgt.core.application.mgt.core.dao.SPApplicationDAO;
import io.entgra.device.mgt.core.application.mgt.core.util.ApplicationManagementUtil;
import io.entgra.device.mgt.core.application.mgt.common.ApplicationArtifact;
import io.entgra.device.mgt.core.application.mgt.common.ApplicationInstaller;
import io.entgra.device.mgt.core.application.mgt.common.ApplicationList;
import io.entgra.device.mgt.core.application.mgt.common.ApplicationSubscriptionType;
import io.entgra.device.mgt.core.application.mgt.common.ApplicationType;
import io.entgra.device.mgt.core.application.mgt.common.DeviceTypes;
import io.entgra.device.mgt.core.application.mgt.common.Filter;
import io.entgra.device.mgt.core.application.mgt.common.LifecycleChanger;
import io.entgra.device.mgt.core.application.mgt.common.LifecycleState;
import io.entgra.device.mgt.core.application.mgt.common.Pagination;
import io.entgra.device.mgt.core.application.mgt.common.config.RatingConfiguration;
import io.entgra.device.mgt.core.application.mgt.common.dto.ApplicationDTO;
import io.entgra.device.mgt.core.application.mgt.common.dto.ApplicationReleaseDTO;
import io.entgra.device.mgt.core.application.mgt.common.dto.CategoryDTO;
import io.entgra.device.mgt.core.application.mgt.common.dto.DeviceSubscriptionDTO;
import io.entgra.device.mgt.core.application.mgt.common.dto.TagDTO;
import io.entgra.device.mgt.core.application.mgt.common.exception.ApplicationManagementException;
import io.entgra.device.mgt.core.application.mgt.common.exception.ApplicationStorageManagementException;
import io.entgra.device.mgt.core.application.mgt.common.exception.DBConnectionException;
import io.entgra.device.mgt.core.application.mgt.common.exception.LifecycleManagementException;
import io.entgra.device.mgt.core.application.mgt.common.exception.RequestValidatingException;
import io.entgra.device.mgt.core.application.mgt.common.exception.ResourceManagementException;
import io.entgra.device.mgt.core.application.mgt.common.exception.TransactionManagementException;
import io.entgra.device.mgt.core.application.mgt.common.response.Application;
import io.entgra.device.mgt.core.application.mgt.common.response.ApplicationRelease;
import io.entgra.device.mgt.core.application.mgt.common.response.Category;
import io.entgra.device.mgt.core.application.mgt.common.response.Tag;
import io.entgra.device.mgt.core.application.mgt.common.services.ApplicationManager;
import io.entgra.device.mgt.core.application.mgt.common.services.ApplicationStorageManager;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.ApplicationUpdateWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.ApplicationWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.CustomAppReleaseWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.CustomAppWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.EntAppReleaseWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.PublicAppReleaseWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.PublicAppWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.WebAppReleaseWrapper;
import io.entgra.device.mgt.core.application.mgt.common.wrapper.WebAppWrapper;
import io.entgra.device.mgt.core.application.mgt.core.config.ConfigurationManager;
import io.entgra.device.mgt.core.application.mgt.core.dao.common.ApplicationManagementDAOFactory;
import io.entgra.device.mgt.core.application.mgt.core.exception.ApplicationManagementDAOException;
import io.entgra.device.mgt.core.application.mgt.core.exception.ForbiddenException;
import io.entgra.device.mgt.core.application.mgt.core.exception.LifeCycleManagementDAOException;
import io.entgra.device.mgt.core.application.mgt.core.exception.NotFoundException;
import io.entgra.device.mgt.core.application.mgt.core.exception.VisibilityManagementDAOException;
import io.entgra.device.mgt.core.application.mgt.core.internal.DataHolder;
import io.entgra.device.mgt.core.application.mgt.core.lifecycle.LifecycleStateManager;
import io.entgra.device.mgt.core.application.mgt.core.util.APIUtil;
import io.entgra.device.mgt.core.application.mgt.core.util.ConnectionManagerUtil;
import io.entgra.device.mgt.core.application.mgt.core.util.Constants;
import io.entgra.device.mgt.core.device.mgt.common.Base64File;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.DeviceManagementException;
import io.entgra.device.mgt.core.device.mgt.common.PaginationRequest;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.MetadataManagementException;
import io.entgra.device.mgt.core.device.mgt.common.metadata.mgt.Metadata;
import io.entgra.device.mgt.core.device.mgt.core.common.exception.StorageManagementException;
import io.entgra.device.mgt.core.device.mgt.core.dto.DeviceType;
import io.entgra.device.mgt.core.device.mgt.core.service.DeviceManagementProviderService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default Concrete implementation of Application Management related implementations.
 */
public class ApplicationManagerImpl implements ApplicationManager {

    private static final Log log = LogFactory.getLog(ApplicationManagerImpl.class);
    private VisibilityDAO visibilityDAO;
    private ApplicationDAO applicationDAO;
    private ApplicationReleaseDAO applicationReleaseDAO;
    private LifecycleStateDAO lifecycleStateDAO;
    private SubscriptionDAO subscriptionDAO;
    private LifecycleStateManager lifecycleStateManager;
    private SPApplicationDAO spApplicationDAO;
    private VppApplicationDAO vppApplicationDAO;
    private ReviewDAO reviewDAO;

    public ApplicationManagerImpl() {
        initDataAccessObjects();
        lifecycleStateManager = DataHolder.getInstance().getLifecycleStateManager();
    }

    private void initDataAccessObjects() {
        this.visibilityDAO = ApplicationManagementDAOFactory.getVisibilityDAO();
        this.applicationDAO = ApplicationManagementDAOFactory.getApplicationDAO();
        this.lifecycleStateDAO = ApplicationManagementDAOFactory.getLifecycleStateDAO();
        this.applicationReleaseDAO = ApplicationManagementDAOFactory.getApplicationReleaseDAO();
        this.subscriptionDAO = ApplicationManagementDAOFactory.getSubscriptionDAO();
        this.spApplicationDAO = ApplicationManagementDAOFactory.getSPApplicationDAO();
        this.vppApplicationDAO = ApplicationManagementDAOFactory.getVppApplicationDAO();
        this.reviewDAO = ApplicationManagementDAOFactory.getCommentDAO();
    }

    @Override
    public <T> Application createApplication(T app, boolean isPublished) throws ApplicationManagementException {
        return createApplicationBasedOnRemoteStatus(app, isPublished);
    }

    /**
     * Create the application based on the release wrapper's remote status. If the remote status is true, then
     * the application creation will take place asynchronously.
     * @param app Application release wrapper
     * @param isPublished Publish status
     * @return {@link Application}
     * @throws ApplicationManagementException Throws when error occurred while application creation
     */
    @SuppressWarnings("unchecked")
    private <T> Application createApplicationBasedOnRemoteStatus(T app, boolean isPublished) throws ApplicationManagementException {
        if (ApplicationManagementUtil.getRemoteStatus(app)) {
            List<?> releaseWrappers = ApplicationManagementUtil.deriveApplicationWithoutRelease(app);
            Application createdApplication = triggerApplicationCreation(app, isPublished);
            if (createdApplication == null) {
                throw new ApplicationManagementException("Null retrieved for created application.");
            }
            try {
                if (releaseWrappers != null && !releaseWrappers.isEmpty()) {
                    if (app instanceof ApplicationWrapper) {
                        ((ApplicationWrapper) app).setEntAppReleaseWrappers((List<EntAppReleaseWrapper>) releaseWrappers);
                        createApplicationReleaseBasedOnRemoteStatus(createdApplication.getId(),
                                ((ApplicationWrapper) app).getEntAppReleaseWrappers().get(0), isPublished);
                    } else if (app instanceof CustomAppWrapper) {
                        ((CustomAppWrapper) app).setCustomAppReleaseWrappers((List<CustomAppReleaseWrapper>) releaseWrappers);
                        createApplicationReleaseBasedOnRemoteStatus(createdApplication.getId(),
                                ((CustomAppWrapper) app).getCustomAppReleaseWrappers().get(0), isPublished);
                    } else {
                        throw new ApplicationManagementException("Unsupported release wrapper received");
                    }
                }
                return createdApplication;
            } catch (ResourceManagementException e) {
                throw new ApplicationManagementException("Error encountered while creating deploying artifact", e);
            }
        }
        return triggerApplicationCreation(app, isPublished);
    }

    /**
     * Trigger the application creation process
     * @param app Application release wrapper
     * @param isPublished Publish status
     * @return {@link Application}
     * @throws ApplicationManagementException Throws when error occurred while creating the application
     */
    private <T> Application triggerApplicationCreation(T app, boolean isPublished) throws ApplicationManagementException {
        ApplicationDTO applicationDTO = uploadReleaseArtifactIfExist(app);
        try {
            ConnectionManagerUtil.beginDBTransaction();
            Application application = addAppDataIntoDB(applicationDTO, isPublished);
            ConnectionManagerUtil.commitDBTransaction();
            return application;
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting database connection.";
            log.error(msg, e);
            ApplicationManagementUtil.deleteArtifactIfExist(applicationDTO);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while disabling AutoCommit.";
            log.error(msg, e);
            ApplicationManagementUtil.deleteArtifactIfExist(applicationDTO);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            ApplicationManagementUtil.deleteArtifactIfExist(applicationDTO);
            String msg = "Error occurred while adding application with the name " + applicationDTO.getName() + " to database ";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * Create application release based on remote status. If the remote status is true, then the
     * application release creation will take place asynchronously.
     * @param appId Application id
     * @param releaseWrapper Release wrapper
     * @param isPublished Publish status
     * @return {@link Application}
     * @throws ApplicationManagementException Throws when error occurred while deploying the release
     * @throws ResourceManagementException Throws when error occurred while deploying the release
     */
    private <T> ApplicationRelease createApplicationReleaseBasedOnRemoteStatus(int appId, T releaseWrapper, boolean isPublished)
            throws ApplicationManagementException, ResourceManagementException {
        if (ApplicationManagementUtil.getRemoteStatusFromWrapper(releaseWrapper)) {
            triggerReleaseAsynchronously(appId, releaseWrapper, isPublished);
        } else {
            if (releaseWrapper instanceof EntAppReleaseWrapper) {
                return triggerEntAppRelease(appId, (EntAppReleaseWrapper) releaseWrapper, isPublished);
            }

            if (releaseWrapper instanceof CustomAppReleaseWrapper) {
                return triggerCustomAppRelease(appId, (CustomAppReleaseWrapper) releaseWrapper, isPublished);
            }

            throw new ApplicationManagementException("Unsupported release wrapper received");
        }
        return new ApplicationRelease();
    }

    /**
     * Trigger release creation asynchronously
     * @param appId Application id
     * @param releaseWrapper Release wrapper
     * @param isPublished Publish status
     */
    private <T> void triggerReleaseAsynchronously(int appId, T releaseWrapper, boolean isPublished) {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        new Thread(() -> {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId, true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(username);
                if (releaseWrapper instanceof EntAppReleaseWrapper &&
                        ((EntAppReleaseWrapper) releaseWrapper).isRemoteStatus()) {
                    triggerEntAppRelease(appId, (EntAppReleaseWrapper) releaseWrapper, isPublished);
                }else if (releaseWrapper instanceof CustomAppReleaseWrapper &&
                        ((CustomAppReleaseWrapper) releaseWrapper).isRemoteStatus()) {
                    triggerCustomAppRelease(appId, (CustomAppReleaseWrapper) releaseWrapper, isPublished);
                } else {
                    throw new ApplicationManagementException("Unsupported release wrapper received");
                }
            } catch (ApplicationManagementException | ResourceManagementException e) {
                log.error("Error encountered while deploying remote application release", e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }).start();
    }

    /**
     * Trigger enterprise application creation
     * @param appId Application id
     * @param releaseWrapper Release wrapper
     * @param isPublished Publish status
     * @return {@link ApplicationRelease}
     * @throws ApplicationManagementException Throws when error encountered while creating enterprise application
     */
    private ApplicationRelease triggerEntAppRelease(int appId, EntAppReleaseWrapper releaseWrapper, boolean isPublished)
            throws ApplicationManagementException{
        ApplicationManager applicationManager = APIUtil.getApplicationManager();
        try {
            ApplicationArtifact artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                    releaseWrapper.getArtifactLink(), releaseWrapper.getBannerLink());
            ApplicationDTO applicationDTO = applicationManager.getApplication(appId);
            DeviceType deviceType = APIUtil.getDeviceTypeData(applicationDTO.getDeviceTypeId());
            ApplicationReleaseDTO releaseDTO = APIUtil.releaseWrapperToReleaseDTO(releaseWrapper);
            releaseDTO = uploadEntAppReleaseArtifacts(releaseDTO, artifact, deviceType.getName(), true);
            try {
                return createRelease(applicationDTO, releaseDTO, ApplicationType.ENTERPRISE, isPublished);
            } catch (ApplicationManagementException e) {
                String msg = "Error occurred while creating ent app release for application with the name: " + applicationDTO.getName();
                log.error(msg, e);
                deleteApplicationArtifacts(Collections.singletonList(releaseDTO.getAppHashValue()));
                throw new ApplicationManagementException(msg, e);
            }
        } catch (MalformedURLException e) {
            String msg = "Malformed URL link received as a downloadable link";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (FileDownloaderServiceException e) {
            String msg = "Error encountered while downloading application release artifacts for app id " + appId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public ApplicationRelease createEntAppRelease(int appId, EntAppReleaseWrapper releaseWrapper, boolean isPublished)
            throws ApplicationManagementException {
        try {
            return createApplicationReleaseBasedOnRemoteStatus(appId, releaseWrapper, isPublished);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred while creating enterprise app release for the app id " + appId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public ApplicationRelease createWebAppRelease(int appId, WebAppReleaseWrapper releaseWrapper, boolean isPublished)
            throws ApplicationManagementException, ResourceManagementException {
        ApplicationManager applicationManager = APIUtil.getApplicationManager();
        try {
            ApplicationDTO applicationDTO = applicationManager.getApplication(appId);
            ApplicationArtifact artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                    null, releaseWrapper.getBannerLink());
            ApplicationReleaseDTO releaseDTO = APIUtil.releaseWrapperToReleaseDTO(releaseWrapper);
            releaseDTO = uploadWebAppReleaseArtifacts(releaseDTO, artifact);
            try {
                return createRelease(applicationDTO, releaseDTO, ApplicationType.WEB_CLIP, isPublished);
            } catch (ApplicationManagementException e) {
                String msg = "Error occurred while creating web app release for application with the name: " + applicationDTO.getName();
                log.error(msg, e);
                deleteApplicationArtifacts(Collections.singletonList(releaseDTO.getAppHashValue()));
                throw new ApplicationManagementException(msg, e);
            }
        } catch (MalformedURLException e) {
            String msg = "Malformed URL link received as a downloadable link";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (FileDownloaderServiceException e) {
            String msg = "Error encountered while downloading application release artifacts";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public ApplicationRelease createPubAppRelease(int appId, PublicAppReleaseWrapper releaseWrapper, boolean isPublished) throws
            ResourceManagementException, ApplicationManagementException {
        ApplicationManager applicationManager = APIUtil.getApplicationManager();
        try {
            ApplicationDTO applicationDTO = applicationManager.getApplication(appId);
            DeviceType deviceType = APIUtil.getDeviceTypeData(applicationDTO.getDeviceTypeId());
            ApplicationArtifact artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                    null, releaseWrapper.getBannerLink());
            ApplicationReleaseDTO releaseDTO = APIUtil.releaseWrapperToReleaseDTO(releaseWrapper);
            releaseDTO = uploadPubAppReleaseArtifacts(releaseDTO, artifact, deviceType.getName());
            try {
                return createRelease(applicationDTO, releaseDTO, ApplicationType.PUBLIC, isPublished);
            } catch (ApplicationManagementException e) {
                String msg = "Error occurred while creating ent public release for application with the name: " + applicationDTO.getName();
                log.error(msg, e);
                deleteApplicationArtifacts(Collections.singletonList(releaseDTO.getAppHashValue()));
                throw new ApplicationManagementException(msg, e);
            }
        } catch (MalformedURLException e) {
            String msg = "Malformed URL link received as a downloadable link";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (FileDownloaderServiceException e) {
            String msg = "Error encountered while downloading application release artifacts";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public ApplicationRelease createCustomAppRelease(int appId, CustomAppReleaseWrapper releaseWrapper, boolean isPublished)
            throws ApplicationManagementException {
        try {
            return createApplicationReleaseBasedOnRemoteStatus(appId, releaseWrapper, isPublished);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred while creating enterprise app release";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    private ApplicationRelease triggerCustomAppRelease(int appId, CustomAppReleaseWrapper releaseWrapper, boolean isPublished)
            throws ResourceManagementException, ApplicationManagementException {
        ApplicationManager applicationManager = APIUtil.getApplicationManager();
        try {
            ApplicationDTO applicationDTO = applicationManager.getApplication(appId);
            DeviceType deviceType = APIUtil.getDeviceTypeData(applicationDTO.getDeviceTypeId());
            ApplicationArtifact artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                    releaseWrapper.getArtifactLink(), releaseWrapper.getBannerLink());
            ApplicationReleaseDTO releaseDTO = APIUtil.releaseWrapperToReleaseDTO(releaseWrapper);
            releaseDTO = uploadCustomAppReleaseArtifacts(releaseDTO, artifact, deviceType.getName());
            try {
                return createRelease(applicationDTO, releaseDTO, ApplicationType.CUSTOM, isPublished);
            } catch (ApplicationManagementException e) {
                String msg = "Error occurred while creating custom app release for application with the name: " + applicationDTO.getName();
                log.error(msg, e);
                deleteApplicationArtifacts(Collections.singletonList(releaseDTO.getAppHashValue()));
                throw new ApplicationManagementException(msg, e);
            }
        } catch (MalformedURLException e) {
            String msg = "Malformed URL link received as a downloadable link";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (FileDownloaderServiceException e) {
            String msg = "Error encountered while downloading application release artifacts";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public <T> ApplicationDTO uploadReleaseArtifactIfExist(T app) throws ApplicationManagementException {
        if (ApplicationManagementUtil.isReleaseAvailable(app)) {
            return uploadReleaseArtifact(app);
        }
        return APIUtil.convertToAppDTO(app);
    }

    /**
     * Upload release artifacts depending on the application wrapper type
     *
     * @param app Application wrapper bean
     * @param <T> Application Wrapper class
     * @return ApplicationDTO that is constructed after uploading the artifacts
     * @throws ApplicationManagementException if any error occurred while uploading artifacts
     */
    private <T> ApplicationDTO uploadReleaseArtifact(T app)
            throws ApplicationManagementException {
        ApplicationArtifact artifact;
        ApplicationDTO applicationDTO = APIUtil.convertToAppDTO(app);
        ApplicationReleaseDTO releaseDTO = applicationDTO.getApplicationReleaseDTOs().get(0);
        if (log.isDebugEnabled()) {
            log.debug("Ent. Application create request is received. Application name: " + applicationDTO.getName());
        }
        try {
            if (app instanceof ApplicationWrapper) {
                ApplicationWrapper wrapper = (ApplicationWrapper) app;
                EntAppReleaseWrapper releaseWrapper = wrapper.getEntAppReleaseWrappers().get(0);
                artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                        releaseWrapper.getArtifactLink(), releaseWrapper.getBannerLink());
                releaseDTO = uploadEntAppReleaseArtifacts(releaseDTO,
                        artifact, wrapper.getDeviceType(), false);
            } else if (app instanceof PublicAppWrapper) {
                PublicAppWrapper wrapper = (PublicAppWrapper) app;
                PublicAppReleaseWrapper releaseWrapper = wrapper.getPublicAppReleaseWrappers().get(0);
                artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                        null, releaseWrapper.getBannerLink());
                releaseDTO = uploadPubAppReleaseArtifacts(releaseDTO, artifact, wrapper.getDeviceType());
            } else if (app instanceof WebAppWrapper) {
                WebAppWrapper wrapper = (WebAppWrapper) app;
                WebAppReleaseWrapper releaseWrapper = wrapper.getWebAppReleaseWrappers().get(0);
                artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                        null, releaseWrapper.getBannerLink());
                releaseDTO = uploadWebAppReleaseArtifacts(releaseDTO, artifact);
            } else if (app instanceof CustomAppWrapper) {
                CustomAppWrapper wrapper = (CustomAppWrapper) app;
                CustomAppReleaseWrapper releaseWrapper = wrapper.getCustomAppReleaseWrappers().get(0);
                artifact = ApplicationManagementUtil.constructApplicationArtifact(releaseWrapper.getIconLink(), releaseWrapper.getScreenshotLinks(),
                        releaseWrapper.getArtifactLink(), releaseWrapper.getBannerLink());
                try {
                    releaseDTO = uploadCustomAppReleaseArtifacts(releaseDTO, artifact, wrapper.getDeviceType());
                } catch (ResourceManagementException e) {
                    String msg = "Error Occurred when uploading artifacts of the web clip: " + wrapper.getName();
                    log.error(msg);
                    throw new ApplicationManagementException(msg, e);
                }
            } else {
                String msg = "Invalid payload found with the request. Hence verify the request payload object.";
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
        } catch (ResourceManagementException e) {
            String msg = "Error Occurred when uploading artifacts of the web clip: " + applicationDTO.getName();
            log.error(msg);
            throw new ApplicationManagementException(msg, e);
        } catch (MalformedURLException e) {
            String msg = "Malformed URL link received as a downloadable link";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (FileDownloaderServiceException e) {
            String msg = "Error encountered while downloading application release artifacts";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        // TODO: artifact URLs are not working for Windows AppX installations https://roadmap.entgra.net/issues/11010
        //ApplicationManagementUtil.addInstallerPathToMetadata(releaseDTO);
        applicationDTO.getApplicationReleaseDTOs().clear();
        applicationDTO.getApplicationReleaseDTOs().add(releaseDTO);
        return applicationDTO;
    }

    @Override
    public void addAppToFavourites(int appId) throws ApplicationManagementException {
        validateAddAppToFavouritesRequest(appId);
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            ConnectionManagerUtil.beginDBTransaction();
            applicationDAO.addAppToFavourite(appId, userName, tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while staring transaction to add applicationId: "
                    + appId + " to favourites";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while adding application id " + appId + " to favourites ";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while adding application with the id: " + appId + " to favourites ";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void removeAppFromFavourites(int appId) throws ApplicationManagementException {
        validateRemoveAppFromFavouritesRequest(appId);
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            ConnectionManagerUtil.beginDBTransaction();
            applicationDAO.removeAppFromFavourite(appId, userName, tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while staring transaction to remove applicationId: "
                    + appId + " from favourites";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while removing application id " + appId + " from favourites ";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while removing application with the id: " + appId + " from favourites ";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public boolean isFavouriteApp(int appId) throws ApplicationManagementException{
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            ConnectionManagerUtil.openDBConnection();
            return applicationDAO.isFavouriteApp(appId, userName, tenantId);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting DB connection to check is app with the id " + appId
                    + " is a favourite app";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while checking app with the id " + appId + " is a favourite app.";
            log.error(msg);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }

    }

    /**
     * Use to check if the requested application id is valid before removing from favourites
     *
     * @param appId ID of the application
     * @throws ApplicationManagementException if ID is not valid or errors while validating
     */
    private void validateRemoveAppFromFavouritesRequest(int appId) throws ApplicationManagementException {
        if (!isFavouriteApp(appId)) {
            String msg = "Provided application is not a favourite app in order remove from favourites";
            throw new BadRequestException(msg);
        }
    }

    /**
     * Use to check if the requested application id is valid before adding to favourites
     *
     * @param appId ID of the application
     * @throws ApplicationManagementException if ID is not valid or errors while validating
     */
    private void validateAddAppToFavouritesRequest(int appId) throws ApplicationManagementException {
        try {
            getApplication(appId);
        } catch (NotFoundException e) {
            String msg = "Requested application does not exists for add to favourites.";
            throw new BadRequestException(msg);
        }
        if (isFavouriteApp(appId)) {
            String msg = "Requested application is already in favourites list.";
            throw new BadRequestException(msg);
        }
    }

    /**
     * Upload enterprise application release artifact into file system.
     *
     * @param releaseDTO Application Release
     * @param applicationArtifact Application Release artifacts
     * @param deviceTypeName Device Type name
     * @param isNewRelease New Release or Not
     * @return {@link ApplicationReleaseDTO}
     * @throws ApplicationManagementException if error occurred while uploading artifacts into file system.
     */
    private ApplicationReleaseDTO uploadEntAppReleaseArtifacts(ApplicationReleaseDTO releaseDTO,
                                                              ApplicationArtifact applicationArtifact, String deviceTypeName, boolean isNewRelease)
            throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ApplicationReleaseDTO applicationReleaseDTO = addApplicationReleaseArtifacts(deviceTypeName, releaseDTO,
                    applicationArtifact, isNewRelease);
            return addImageArtifacts(applicationReleaseDTO, applicationArtifact, tenantId);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred while uploading application release artifacts.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * Use to upload/save web app release artifacts (I.E icon)
     *
     * @param releaseDTO {@link ApplicationReleaseDTO}
     * @param applicationArtifact {@link ApplicationArtifact}
     * @return constructed {@link ApplicationReleaseDTO} with upload details
     * @throws ResourceManagementException if error occurred while uploading
     */
    private ApplicationReleaseDTO uploadWebAppReleaseArtifacts(ApplicationReleaseDTO releaseDTO, ApplicationArtifact applicationArtifact)
            throws ResourceManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        releaseDTO.setUuid(UUID.randomUUID().toString());
        releaseDTO.setAppHashValue(DigestUtils.md5Hex(releaseDTO.getInstallerName()));
        //uploading application artifacts
        return addImageArtifacts(releaseDTO, applicationArtifact, tenantId);
    }

    /**
     * Use to upload/save public app release artifacts (I.E icon)
     *
     * @param releaseDTO {@link ApplicationReleaseDTO}
     * @param applicationArtifact {@link ApplicationArtifact}
     * @param deviceType Device Type name
     * @return constructed {@link ApplicationReleaseDTO} with upload details
     * @throws ResourceManagementException if error occurred while uploading
     */
    private ApplicationReleaseDTO uploadPubAppReleaseArtifacts(ApplicationReleaseDTO releaseDTO, ApplicationArtifact applicationArtifact,
                                                              String deviceType)
            throws ResourceManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String appInstallerUrl = getPublicAppStorePath(deviceType) + releaseDTO.getPackageName();
        releaseDTO.setInstallerName(appInstallerUrl);
        releaseDTO.setUuid(UUID.randomUUID().toString());
        releaseDTO.setAppHashValue(DigestUtils.md5Hex(appInstallerUrl));
        //uploading application artifacts
        return addImageArtifacts(releaseDTO, applicationArtifact, tenantId);
    }

    public void validatePublicAppReleasePackageName(String packageName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            List<ApplicationReleaseDTO> exitingPubAppReleases = applicationReleaseDAO
                    .getReleaseByPackages(Collections.singletonList(packageName), tenantId);
            if (!exitingPubAppReleases.isEmpty()){
                String msg = "Public app release exists for package name " + packageName
                        + ". Hence you can't add new public app for package name "
                        + packageName;
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error Occurred when fetching release: " + packageName;
            log.error(msg);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * Use to upload/save public app release artifacts (I.E icon)
     *
     * @param releaseDTO {@link ApplicationReleaseDTO}
     * @param applicationArtifact {@link ApplicationArtifact}
     * @param deviceType Device Type name
     * @return constructed {@link ApplicationReleaseDTO} with upload details
     * @throws ResourceManagementException if error occurred while uploading
     */
    private ApplicationReleaseDTO uploadCustomAppReleaseArtifacts(ApplicationReleaseDTO releaseDTO, ApplicationArtifact applicationArtifact,
                                                                  String deviceType)
            throws ResourceManagementException, ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();
        try {
            String md5OfApp = applicationStorageManager.
                    getMD5(Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())));
            validateReleaseBinaryFileHash(md5OfApp);
            releaseDTO.setUuid(UUID.randomUUID().toString());
            releaseDTO.setAppHashValue(md5OfApp);
            releaseDTO.setInstallerName(applicationArtifact.getInstallerName());

            applicationStorageManager.uploadReleaseArtifact(releaseDTO, deviceType,
                    Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())), tenantId);
        } catch (IOException e) {
            String msg = "Error occurred when uploading release artifact into the server";
            log.error(msg);
            throw new ApplicationManagementException(msg, e);
        } catch (StorageManagementException e) {
            String msg = "Error occurred while md5sum value retrieving process: application UUID "
                    + releaseDTO.getUuid();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return addImageArtifacts(releaseDTO, applicationArtifact, tenantId);
    }

    public void validateReleaseBinaryFileHash(String hash)
            throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            if (this.applicationReleaseDAO.verifyReleaseExistenceByHash(hash, tenantId)) {
                String msg = "Application release already exists";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while checking if release already exists";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    public String getPublicAppStorePath(String deviceType) {
        if (DeviceTypes.ANDROID.toString().equalsIgnoreCase(deviceType)) {
            return Constants.GOOGLE_PLAY_STORE_URL;
        } else if (DeviceTypes.IOS.toString().equalsIgnoreCase(deviceType)) {
            return Constants.APPLE_STORE_URL;
        } else if (DeviceTypes.WINDOWS.toString().equalsIgnoreCase(deviceType)) {
            return Constants.MICROSOFT_STORE_URL;
        } else {
            throw new IllegalArgumentException("No such device with the name " + deviceType);
        }
    }

    /**
     * Helps to byte content of release binary file of application artifact
     * This method can be useful when uploading application release binary file or when generating md5hex of release binary
     *
     * @param artifact {@link ApplicationArtifact}
     * @return byte content of application release binary file
     * @throws ApplicationManagementException if error occurred while getting byte content
     */
    private byte[] getByteContentOfApp(ApplicationArtifact artifact) throws ApplicationManagementException{
        try {
            return IOUtils.toByteArray(artifact.getInstallerStream());
        } catch (IOException e) {
            String msg = "Error occurred while getting byte content of app binary artifact";
            log.error(msg);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * Useful to generate md5hex string of application release
     *
     * @param applicationArtifact {@link ApplicationArtifact}
     * @param content byte array content of application release binary file
     * @return Generated md5hex string
     * @throws ApplicationManagementException if any error occurred while generating md5hex string
     */
    private String generateMD5OfApp(ApplicationArtifact applicationArtifact, byte[] content) throws ApplicationManagementException {
        try {
            ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();
            String md5OfApp = applicationStorageManager.getMD5(new ByteArrayInputStream(content));
            if (md5OfApp == null) {
                String msg = "Error occurred while generating md5sum value of " + applicationArtifact.getInstallerName();
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
            return md5OfApp;
        } catch(StorageManagementException e) {
            String msg = "Error occurred while generating md5sum value of " + applicationArtifact.getInstallerName();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * Delete Application release artifacts
     *
     * @param directoryPaths Directory paths
     * @throws ApplicationManagementException if error occurred while deleting application release artifacts.
     */
    @Override
    public void deleteApplicationArtifacts(List<String> directoryPaths) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();
        try {
            applicationStorageManager.deleteAllApplicationReleaseArtifacts(directoryPaths, tenantId);
        } catch (ApplicationStorageManagementException e) {
            String msg = "Error occurred when deleting application artifacts. directory paths: ." + directoryPaths
                    .toString();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * To add Application release artifacts
     *
     * @param deviceType Device Type
     * @param applicationReleaseDTO Application Release
     * @param applicationArtifact Application release artifacts
     * @param isNewRelease Is new release or Not
     * @return {@link ApplicationReleaseDTO}
     * @throws ResourceManagementException if error occurred while handling application release artifacts.
     * @throws ApplicationManagementException if error occurred while handling application release data.
     */
    private ApplicationReleaseDTO addApplicationReleaseArtifacts(String deviceType,
                                                                 ApplicationReleaseDTO applicationReleaseDTO, ApplicationArtifact applicationArtifact, boolean isNewRelease)
            throws ResourceManagementException, ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();

        String uuid = UUID.randomUUID().toString();
        applicationReleaseDTO.setUuid(uuid);
        // The application executable artifacts such as apks are uploaded.
        try {
            applicationReleaseDTO.setInstallerName(applicationArtifact.getInstallerName());
            if (!DeviceTypes.WINDOWS.toString().equalsIgnoreCase(deviceType)) {
                ApplicationInstaller applicationInstaller = applicationStorageManager
                        .getAppInstallerData(Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())), deviceType);
                applicationReleaseDTO.setVersion(applicationInstaller.getVersion());
                applicationReleaseDTO.setPackageName(applicationInstaller.getPackageName());
            } else {
                String windowsInstallerName = applicationArtifact.getInstallerName();
                String extension = windowsInstallerName.substring(windowsInstallerName.lastIndexOf(".") + 1);
                if (!extension.equalsIgnoreCase(Constants.MSI) &&
                        !extension.equalsIgnoreCase(Constants.APPX)) {
                    String msg = "Application Type doesn't match with supporting application types of " +
                            deviceType + "platform which are APPX and MSI";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
            }

            String packageName = applicationReleaseDTO.getPackageName();
            try {
                ConnectionManagerUtil.openDBConnection();
                if (!isNewRelease && applicationReleaseDAO
                        .isActiveReleaseExisitForPackageName(packageName, tenantId,
                                lifecycleStateManager.getEndState())) {
                    String msg = "Application release is already exist for the package name: " + packageName
                            + ". Either you can delete all application releases for package " + packageName + " or "
                            + "you can add this app release as an new application release, under the existing "
                            + "application.";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                String md5OfApp = applicationStorageManager.
                        getMD5(Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())));
                if (md5OfApp == null) {
                    String msg = "Error occurred while md5sum value retrieving process: application UUID "
                            + applicationReleaseDTO.getUuid();
                    log.error(msg);
                    throw new ApplicationStorageManagementException(msg);
                }
                if (this.applicationReleaseDAO.verifyReleaseExistenceByHash(md5OfApp, tenantId)) {
                    String msg =
                            "Application release exists for the uploaded binary file. Device Type: " + deviceType;
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                applicationReleaseDTO.setAppHashValue(md5OfApp);
                applicationStorageManager
                        .uploadReleaseArtifact(applicationReleaseDTO, deviceType,
                                Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())), tenantId);
            } catch (StorageManagementException e) {
                String msg = "Error occurred while md5sum value retrieving process: application UUID "
                        + applicationReleaseDTO.getUuid();
                log.error(msg, e);
                throw new ApplicationStorageManagementException(msg, e);
            } catch (DBConnectionException e) {
                String msg = "Error occurred when getting database connection for verifying app release data.";
                log.error(msg, e);
                throw new ApplicationManagementException(msg, e);
            } catch (ApplicationManagementDAOException e) {
                String msg =
                        "Error occurred when executing the query for verifying application release existence for "
                                + "the package.";
                log.error(msg, e);
                throw new ApplicationManagementException(msg, e);
            } finally {
                ConnectionManagerUtil.closeDBConnection();
            }
        } catch (IOException e) {
            String msg = "Error occurred when getting file input stream. Installer name: " + applicationArtifact
                    .getInstallerName();
            log.error(msg, e);
            throw new ApplicationStorageManagementException(msg, e);
        }
        return applicationReleaseDTO;
    }

    /**
     * This method could be used to update enterprise application release artifacts.
     *
     * @param deviceType Device Type
     * @param applicationReleaseDTO Application Release
     * @param applicationArtifact Application release artifacts
     * @return {@link ApplicationReleaseDTO}
     * @throws ResourceManagementException if error occurred while handling application release artifacts.
     * @throws ApplicationManagementException if error occurred while handling application release data.
     */
    private ApplicationReleaseDTO updateEntAppReleaseArtifact(String deviceType,
                                                              ApplicationReleaseDTO applicationReleaseDTO, ApplicationArtifact applicationArtifact)
            throws ResourceManagementException, ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();

        // The application executable artifacts such as apks are uploaded.
        try {
            String md5OfApp = applicationStorageManager.getMD5(Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())));
            if (md5OfApp == null) {
                String msg = "Error occurred while retrieving md5sum value from the binary file for application "
                        + "release UUID " + applicationReleaseDTO.getUuid();
                log.error(msg);
                throw new ApplicationStorageManagementException(msg);
            }

            if (!applicationReleaseDTO.getAppHashValue().equals(md5OfApp)) {
                applicationReleaseDTO.setInstallerName(applicationArtifact.getInstallerName());
                ApplicationInstaller applicationInstaller = applicationStorageManager
                        .getAppInstallerData(Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())), deviceType);
                String packageName = applicationInstaller.getPackageName();

                try {
                    ConnectionManagerUtil.getDBConnection();
                    if (this.applicationReleaseDAO.verifyReleaseExistenceByHash(md5OfApp, tenantId)) {
                        String msg = "Same binary file is in the server. Hence you can't add same file into the "
                                + "server. Device Type: " + deviceType + " and package name: " + packageName;
                        log.error(msg);
                        throw new BadRequestException(msg);
                    }
                    if (applicationReleaseDTO.getPackageName() == null) {
                        String msg = "Found null value for application release package name for application "
                                + "release which has UUID: " + applicationReleaseDTO.getUuid();
                        log.error(msg);
                        throw new ApplicationManagementException(msg);
                    }
                    if (!applicationReleaseDTO.getPackageName().equals(packageName)) {
                        String msg = "Package name of the new artifact does not match with the package name of "
                                + "the exiting application release. Package name of the existing app release "
                                + applicationReleaseDTO.getPackageName() + " and package name of the new "
                                + "application release " + packageName;
                        log.error(msg);
                        throw new BadRequestException(msg);
                    }

                    applicationReleaseDTO.setVersion(applicationInstaller.getVersion());
                    applicationReleaseDTO.setPackageName(packageName);
                    String deletingAppHashValue = applicationReleaseDTO.getAppHashValue();
                    applicationReleaseDTO.setAppHashValue(md5OfApp);
                    applicationStorageManager.uploadReleaseArtifact(applicationReleaseDTO, deviceType,
                            Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())),
                            tenantId);
                    applicationStorageManager.copyImageArtifactsAndDeleteInstaller(deletingAppHashValue,
                            applicationReleaseDTO, tenantId);
                } catch (DBConnectionException e) {
                    String msg = "Error occurred when getting database connection for verifying application "
                            + "release existing for new app hash value.";
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } catch (ApplicationManagementDAOException e) {
                    String msg = "Error occurred when executing the query for verifying application release "
                            + "existence for the new app hash value.";
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } finally {
                    ConnectionManagerUtil.closeDBConnection();
                }
            }
        } catch (StorageManagementException e) {
            String msg = "Error occurred while retrieving md5sum value from the binary file for application "
                    + "release UUID " + applicationReleaseDTO.getUuid();
            log.error(msg, e);
            throw new ApplicationStorageManagementException(msg, e);
        } catch (IOException e) {
            String msg = "Error occurred when getting file input stream. Installer name: " + applicationArtifact
                    .getInstallerName();
            log.error(msg, e);
            throw new ApplicationStorageManagementException(msg, e);
        }
        return applicationReleaseDTO;
    }

    /**
     * Add image artifacts into file system
     *
     * @param applicationReleaseDTO Application Release
     * @param applicationArtifact Image artifacts
     * @param tenantId Tenant Id
     * @return {@link ApplicationReleaseDTO}
     * @throws ResourceManagementException if error occurred while uploading image artifacts into file system.
     */
    private ApplicationReleaseDTO addImageArtifacts(ApplicationReleaseDTO applicationReleaseDTO,
                                                    ApplicationArtifact applicationArtifact, int tenantId) throws ResourceManagementException {
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();

        applicationReleaseDTO.setIconName(ApplicationManagementUtil.sanitizeName
                (applicationArtifact.getIconName(), Constants.ICON_NAME));
        applicationReleaseDTO.setBannerName(applicationArtifact.getBannerName());

        Map<String, InputStream> screenshots = applicationArtifact.getScreenshots();
        List<String> screenshotNames = new ArrayList<>(screenshots.keySet());

        int counter = 1;
        for (String scName : screenshotNames) {
            if (counter == 1) {
                applicationReleaseDTO.setScreenshotName1(ApplicationManagementUtil.sanitizeName
                        (scName, Constants.SCREENSHOT_NAME + counter));
            } else if (counter == 2) {
                applicationReleaseDTO.setScreenshotName2(ApplicationManagementUtil.sanitizeName
                        (scName, Constants.SCREENSHOT_NAME + counter));
            } else if (counter == 3) {
                applicationReleaseDTO.setScreenshotName3(ApplicationManagementUtil.sanitizeName
                        (scName, Constants.SCREENSHOT_NAME + counter));
            }
            counter++;
        }

        // Upload images
        applicationReleaseDTO = applicationStorageManager
                .uploadImageArtifacts(applicationReleaseDTO, applicationArtifact.getIconStream(),
                        applicationArtifact.getBannerStream(), new ArrayList<>(screenshots.values()), tenantId);
        return applicationReleaseDTO;
    }

    /**
     * Update Image artifacts of Application RApplication Release
     *
     * @param applicationArtifact Application release Artifacts
     * @param tenantId Tenant Id
     * @return {@link ApplicationReleaseDTO}
     * @throws ResourceManagementException if error occurred while uploading application release artifacts into the file system.
     */
    private ApplicationReleaseDTO updateImageArtifacts(ApplicationReleaseDTO applicationReleaseDTO,
                                                       ApplicationArtifact applicationArtifact, int tenantId) throws ResourceManagementException{
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();

        if (!StringUtils.isEmpty(applicationArtifact.getIconName())) {
            applicationStorageManager
                    .deleteAppReleaseArtifact(applicationReleaseDTO.getAppHashValue(), Constants.ICON_ARTIFACT,
                            applicationReleaseDTO.getIconName(), tenantId);
            applicationReleaseDTO.setIconName(ApplicationManagementUtil.sanitizeName
                    (applicationArtifact.getIconName(), Constants.ICON_NAME));
        }
        if (!StringUtils.isEmpty(applicationArtifact.getBannerName())){
            applicationStorageManager
                    .deleteAppReleaseArtifact(applicationReleaseDTO.getAppHashValue(), Constants.BANNER_ARTIFACT,
                            applicationReleaseDTO.getBannerName(), tenantId);
            applicationReleaseDTO.setBannerName(applicationArtifact.getBannerName());
        }

        Map<String, InputStream> screenshots = applicationArtifact.getScreenshots();
        List<InputStream> screenshotStreams = new ArrayList<>();

        if (screenshots != null){
            List<String> screenshotNames = new ArrayList<>(screenshots.keySet());
            screenshotStreams = new ArrayList<>(screenshots.values());

            int counter = 1;
            for (String scName : screenshotNames) {
                String folderPath = Constants.SCREENSHOT_ARTIFACT + counter;
                if (counter == 1) {
                    applicationStorageManager
                            .deleteAppReleaseArtifact(applicationReleaseDTO.getAppHashValue(), folderPath,
                                    applicationReleaseDTO.getScreenshotName1(), tenantId);
                    applicationReleaseDTO.setScreenshotName1(ApplicationManagementUtil.sanitizeName
                            (scName, Constants.SCREENSHOT_NAME + counter));
                } else if (counter == 2) {
                    applicationStorageManager
                            .deleteAppReleaseArtifact(applicationReleaseDTO.getAppHashValue(), folderPath,
                                    applicationReleaseDTO.getScreenshotName2(), tenantId);
                    applicationReleaseDTO.setScreenshotName2(ApplicationManagementUtil.sanitizeName
                            (scName, Constants.SCREENSHOT_NAME + counter));
                } else if (counter == 3) {
                    applicationStorageManager
                            .deleteAppReleaseArtifact(applicationReleaseDTO.getAppHashValue(), folderPath,
                                    applicationReleaseDTO.getScreenshotName3(), tenantId);
                    applicationReleaseDTO.setScreenshotName3(ApplicationManagementUtil.sanitizeName
                            (scName, Constants.SCREENSHOT_NAME + counter));
                }
                counter++;
            }
        }

        // Upload images
        applicationReleaseDTO = applicationStorageManager
                .uploadImageArtifacts(applicationReleaseDTO, applicationArtifact.getIconStream(),
                        applicationArtifact.getBannerStream(), screenshotStreams, tenantId);
        return applicationReleaseDTO;
    }

    @Override
    public ApplicationList getFavouriteApplications(Filter filter) throws ApplicationManagementException {
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        filter.setFavouredBy(userName);
        return getApplications(filter);
    }

    @Override
    public ApplicationList getApplications(Filter filter) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        ApplicationList applicationList = new ApplicationList();
        List<Application> applications = new ArrayList<>();
        DeviceType deviceType;

        if (StringUtils.isNotBlank(filter.getDeviceType())) {
            deviceType = APIUtil.getDeviceTypeData(filter.getDeviceType());
        } else {
            deviceType = new DeviceType();
            deviceType.setId(-1);
        }

        try {
            ConnectionManagerUtil.openDBConnection();
            validateFilter(filter);
            List<ApplicationDTO> appDTOs = applicationDAO.getApplications(filter, deviceType.getId(), tenantId);
            for (ApplicationDTO applicationDTO : appDTOs) {
                if (lifecycleStateManager.getEndState().equals(applicationDTO.getStatus())) {
                    continue;
                }

                //Set application categories, tags and unrestricted roles to the application DTO.
                applicationDTO
                        .setUnrestrictedRoles(visibilityDAO.getUnrestrictedRoles(applicationDTO.getId(), tenantId));
                setApplicationProperties(applicationDTO);

                if (isFilteringApp(applicationDTO, filter)) {
                    boolean isHideableApp = isHideableApp(applicationDTO.getApplicationReleaseDTOs());
                    boolean isDeletableApp = isDeletableApp(applicationDTO.getApplicationReleaseDTOs());

                    List<ApplicationReleaseDTO> filteredApplicationReleaseDTOs = new ArrayList<>();
                    for (ApplicationReleaseDTO applicationReleaseDTO : applicationDTO.getApplicationReleaseDTOs()) {
                        if (StringUtils.isNotEmpty(filter.getVersion()) && !filter.getVersion()
                                .equals(applicationReleaseDTO.getVersion())) {
                            continue;
                        }
                        if (StringUtils.isNotEmpty(filter.getAppReleaseState()) && !filter.getAppReleaseState()
                                .equals(applicationReleaseDTO.getCurrentState())) {
                            continue;
                        }
                        if (StringUtils.isNotEmpty(filter.getAppReleaseType()) && !filter.getAppReleaseType()
                                .equals(applicationReleaseDTO.getReleaseType())) {
                            continue;
                        }
                        filteredApplicationReleaseDTOs.add(applicationReleaseDTO);
                    }

                    applicationDTO.setApplicationReleaseDTOs(filteredApplicationReleaseDTOs);
                    Application application = APIUtil.appDtoToAppResponse(applicationDTO);
                    application.setDeletableApp(isDeletableApp);
                    application.setHideableApp(isHideableApp);
                    applications.add(application);
                }
            }
            Pagination pagination = new Pagination();
            pagination.setCount(applicationDAO.getApplicationCount(filter, deviceType.getId(), tenantId));
            pagination.setSize(applications.size());
            pagination.setOffset(filter.getOffset());
            pagination.setLimit(filter.getLimit());

            applicationList.setApplications(applications);
            applicationList.setPagination(pagination);
            return applicationList;
        } catch (DBConnectionException e) {
            String msg = "Error occurred when getting database connection to get applications by filtering from "
                    + "requested filter.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg =
                    "DAO exception while getting applications of tenant " + tenantId + ". Filter: " + filter.toString();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * To check whether the application is filtering app or not
     *
     * @param applicationDTO Application DTO object
     * @param filter Filter
     * @return false if application doesn't satisfy filters, otherwise returns true.
     * @throws ApplicationManagementException if error occurred while checking whether user has app unrestricted roles
     * or filtering roles.
     */
    private boolean isFilteringApp(ApplicationDTO applicationDTO, Filter filter) throws ApplicationManagementException {
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        List<String> filteringTags = filter.getTags();
        List<String> filteringCategories = filter.getCategories();
        List<String> filteringUnrestrictedRoles = filter.getUnrestrictedRoles();

        List<String> appUnrestrictedRoles = applicationDTO.getUnrestrictedRoles();
        List<String> appCategoryList = applicationDTO.getAppCategories();
        List<String> appTagList = applicationDTO.getTags();
        try {
            if (!appUnrestrictedRoles.isEmpty() && !hasUserRole(appUnrestrictedRoles, userName)) {
                return false;
            }
            if (filteringUnrestrictedRoles != null && !filteringUnrestrictedRoles.isEmpty() && !hasAppUnrestrictedRole(
                    appUnrestrictedRoles, filteringUnrestrictedRoles, userName)) {
                return false;
            }
            if (filteringCategories != null && !filteringCategories.isEmpty() && filteringCategories.stream()
                    .noneMatch(appCategoryList::contains)) {
                return false;
            }
            if (filteringTags != null && !filteringTags.isEmpty() && filteringTags.stream()
                    .noneMatch(appTagList::contains)) {
                return false;
            }
        } catch (UserStoreException e) {
            String msg = "User-store exception while checking whether the user " + userName
                    + " has permission to view application";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return true;
    }
    /**
     * To check whether the application is whether hideable or not
     *
     * @param applicationReleaseDTOs Application releases
     * @return true if application releases are in hideable state (i.e Retired), otherwise returns false
     * @throws ApplicationManagementException if error occurred while getting application release end state.
     */
    @Override
    public boolean isHideableApp(List<ApplicationReleaseDTO> applicationReleaseDTOs)
            throws ApplicationManagementException {
        try {
            for (ApplicationReleaseDTO applicationReleaseDTO : applicationReleaseDTOs) {
                if (!lifecycleStateManager.getEndState().equals(applicationReleaseDTO.getCurrentState())) {
                    return false;
                }
            }
        } catch (LifecycleManagementException e) {
            String msg = "Error occurred while testing application is whether hideable app or not.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return true;
    }

    /**
     * To check whether the application is whether deletable or not
     *
     * @param applicationReleaseDTOs Application releases
     * @return true if application releases are in deletable state (i.e Created or Rejected), otherwise returns false
     * @throws ApplicationManagementException if error occurred while checking whether the application release is in
     * deletable state or not.
     */
    @Override
    public boolean isDeletableApp(List<ApplicationReleaseDTO> applicationReleaseDTOs)
            throws ApplicationManagementException {
        try {
            for (ApplicationReleaseDTO applicationReleaseDTO : applicationReleaseDTOs) {
                if (!lifecycleStateManager.isDeletableState(applicationReleaseDTO.getCurrentState())) {
                    return false;
                }
            }
        } catch (LifecycleManagementException e) {
            String msg = "Error occurred while testing application is whether deletable app or not.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return true;
    }

    @Override
    public List<Application> getApplications(List<String> packageNames) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            ConnectionManagerUtil.openDBConnection();
            List<ApplicationDTO> appDTOs = applicationDAO.getAppWithRelatedReleases(packageNames, tenantId);
            List<ApplicationDTO> filteredApplications = new ArrayList<>();
            for (ApplicationDTO applicationDTO : appDTOs) {
                if (!lifecycleStateManager.getEndState().equals(applicationDTO.getStatus())) {
                    //Set application categories, tags and unrestricted roles to the application DTO.
                    applicationDTO
                            .setUnrestrictedRoles(visibilityDAO.getUnrestrictedRoles(applicationDTO.getId(), tenantId));
                    applicationDTO.setAppCategories(applicationDAO.getAppCategories(applicationDTO.getId(), tenantId));
                    applicationDTO.setTags(applicationDAO.getAppTags(applicationDTO.getId(), tenantId));
                    if (applicationDTO.getUnrestrictedRoles().isEmpty() || hasUserRole(
                            applicationDTO.getUnrestrictedRoles(), userName)) {
                        filteredApplications.add(applicationDTO);
                    }
                }

                List<ApplicationReleaseDTO> filteredApplicationReleaseDTOs = new ArrayList<>();
                for (ApplicationReleaseDTO applicationReleaseDTO : applicationDTO.getApplicationReleaseDTOs()) {
                    if (!applicationReleaseDTO.getCurrentState().equals(lifecycleStateManager.getEndState())) {
                        filteredApplicationReleaseDTOs.add(applicationReleaseDTO);
                    }
                }
                applicationDTO.setApplicationReleaseDTOs(filteredApplicationReleaseDTOs);
            }

            List<Application> applications = new ArrayList<>();
            for (ApplicationDTO appDTO : filteredApplications) {
                applications.add(APIUtil.appDtoToAppResponse(appDTO));
            }
            return applications;
        } catch (DBConnectionException e) {
            String msg = "Error occurred when getting database connection to get applications by filtering from "
                    + "requested filter.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (UserStoreException e) {
            String msg = "User-store exception while checking whether the user " + userName + " of tenant " + tenantId
                    + " has the publisher permission";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "DAO exception while getting applications for the user " + userName + " of tenant " + tenantId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * Check whether at least one filtering role is in app unrestricted roles.
     *
     * @param appUnrestrictedRoles Application unrestricted roles
     * @param filteringUnrestrictedRoles Filtering roles
     * @param userName Username
     * @return True if one filtering unrestricted role is associated with application unrestricted roles.
     * @throws BadRequestException if user doesn't have assigned at least one filtering role
     * @throws UserStoreException if error occurred when checking whether user has assigned at least one filtering role.
     */
    private boolean hasAppUnrestrictedRole(List<String> appUnrestrictedRoles, List<String> filteringUnrestrictedRoles,
                                           String userName) throws BadRequestException, UserStoreException {
        if (!hasUserRole(filteringUnrestrictedRoles, userName)) {
            String msg =
                    "At least one filtering role is not assigned for the user: " + userName + ". Hence user " + userName
                            + " Can't filter applications by giving these unrestricted role list";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        if (!appUnrestrictedRoles.isEmpty()) {
            for (String role : filteringUnrestrictedRoles) {
                if (appUnrestrictedRoles.contains(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Application addAppDataIntoDB(ApplicationDTO applicationDTO, boolean isPublished) throws
            ApplicationManagementException  {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationReleaseDTO applicationReleaseDTO = null;
        if (applicationDTO.getApplicationReleaseDTOs().size() > 0) {
            applicationReleaseDTO = applicationDTO.getApplicationReleaseDTOs().get(0);
        }
        try {
            // Insert to application table
            int appId = this.applicationDAO.createApplication(applicationDTO, tenantId);
            if (appId == -1) {
                String msg = "Application data storing is Failed.";
                log.error(msg);
                throw new ApplicationManagementDAOException(msg);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("New ApplicationDTO entry added to AP_APP table. App Id:" + appId);
                }
                //add application categories

                List<Integer> categoryIds = applicationDAO.getCategoryIdsForCategoryNames(applicationDTO.getAppCategories(), tenantId);
                this.applicationDAO.addCategoryMapping(categoryIds, appId, tenantId);

                //adding application unrestricted roles
                if (applicationDTO.getUnrestrictedRoles() != null && !applicationDTO.getUnrestrictedRoles().isEmpty()) {
                    this.visibilityDAO.addUnrestrictedRoles(applicationDTO.getUnrestrictedRoles(), appId, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug("New restricted roles to app ID mapping added to AP_UNRESTRICTED_ROLE table."
                                + " App Id:" + appId);
                    }
                }

                //adding application tags
                if (applicationDTO.getTags() != null && !applicationDTO.getTags().isEmpty()) {
                    List<TagDTO> registeredTags = applicationDAO.getAllTags(tenantId);
                    List<String> registeredTagNames = new ArrayList<>();
                    List<Integer> tagIds = new ArrayList<>();

                    for (TagDTO tagDTO : registeredTags) {
                        registeredTagNames.add(tagDTO.getTagName());
                    }
                    List<String> newTags = getDifference(applicationDTO.getTags(), registeredTagNames);
                    if (!newTags.isEmpty()) {
                        this.applicationDAO.addTags(newTags, tenantId);
                        if (log.isDebugEnabled()) {
                            log.debug("New tags entry added to AP_APP_TAG table. App Id:" + appId);
                        }
                        tagIds = this.applicationDAO.getTagIdsForTagNames(applicationDTO.getTags(), tenantId);
                    } else {
                        for (TagDTO tagDTO : registeredTags) {
                            for (String tagName : applicationDTO.getTags()) {
                                if (tagName.equals(tagDTO.getTagName())) {
                                    tagIds.add(tagDTO.getId());
                                    break;
                                }
                            }
                        }
                    }
                    this.applicationDAO.addTagMapping(tagIds, appId, tenantId);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Creating a new release. App Id:" + appId);
                }
                List<ApplicationReleaseDTO> applicationReleaseEntities = new ArrayList<>();
                if (applicationReleaseDTO != null) {
                    String lifeCycleState = lifecycleStateManager.getInitialState();
                    String[] publishStates= {"IN-REVIEW", "APPROVED", "PUBLISHED"};

                    applicationReleaseDTO.setCurrentState(lifeCycleState);
                    applicationReleaseDTO = this.applicationReleaseDAO.createRelease(applicationReleaseDTO, appId, tenantId);
                    LifecycleState lifecycleState = getLifecycleStateInstance(lifeCycleState, lifeCycleState);
                    this.lifecycleStateDAO.addLifecycleState(lifecycleState, applicationReleaseDTO.getId(), tenantId);
                    if(isPublished){
                        for (String state: publishStates) {
                            LifecycleChanger lifecycleChanger = new LifecycleChanger();
                            lifecycleChanger.setAction(state);
                            lifecycleChanger.setReason("Updated to " + state);
                            this.changeLifecycleState(applicationReleaseDTO, lifecycleChanger);
                        }
                    }
                    if (Constants.ENTERPRISE_APP_TYPE.equals(applicationDTO.getType()) || Constants.PUBLIC_APP_TYPE.equals(applicationDTO.getType())) {
                        persistAppIconInfo(applicationReleaseDTO);
                    }
                    applicationReleaseEntities.add(applicationReleaseDTO);
                }
                applicationDTO.setId(appId);
                applicationDTO.setApplicationReleaseDTOs(applicationReleaseEntities);
                return APIUtil.appDtoToAppResponse(applicationDTO);
            }
        } catch (LifeCycleManagementDAOException e) {
            String msg =
                    "Error occurred while adding lifecycle state. application name: " + applicationDTO.getName() + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while adding application or application release. application name: "
                    + applicationDTO.getName() + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifecycleManagementException e) {
            String msg =
                    "Error occurred when getting initial lifecycle state. application name: " + applicationDTO.getName()
                            + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (VisibilityManagementDAOException e) {
            String msg = "Error occurred while adding unrestricted roles. application name: " + applicationDTO.getName()
                    + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * Persist application icon information when creating an application
     *
     * @param applicationReleaseDTO {@link ApplicationReleaseDTO}
     * @throws ApplicationManagementException if error occurred while persisting application icon information
     */
    private void persistAppIconInfo(ApplicationReleaseDTO applicationReleaseDTO)
            throws ApplicationManagementException {
        try {
            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
            String iconPath = APIUtil.createAppIconPath(applicationReleaseDTO, tenantId);
            DataHolder.getInstance().getDeviceManagementService().saveApplicationIcon(iconPath,
                    String.valueOf(applicationReleaseDTO.getPackageName()), applicationReleaseDTO.getVersion());
        } catch (ApplicationManagementException e) {
            String msg = "Error occurred while creating iconPath. Application package name : " + applicationReleaseDTO.getPackageName();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while saving application icon info. Application package name : " + applicationReleaseDTO.getPackageName();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public <T> ApplicationRelease createRelease(ApplicationDTO applicationDTO, ApplicationReleaseDTO applicationReleaseDTO,
                                                ApplicationType type, boolean isPublished)
            throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        if (log.isDebugEnabled()) {
            log.debug("Application release creating request is received for the application id: " + applicationDTO.getId());
        }

        if (!type.toString().equals(applicationDTO.getType())) {
            String msg = "It is possible to add new application release for " + type
                    + " app type. But you are requesting to add new application release for " + applicationDTO.getType()
                    + " app type.";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (!type.equals(ApplicationType.ENTERPRISE)) {
            int applicationReleaseCount = applicationDTO.getApplicationReleaseDTOs().size();
            if (applicationReleaseCount > 0) {
                String msg = "Application type of " + applicationDTO.getType() + " can only have one release";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        }

        try {
            ConnectionManagerUtil.beginDBTransaction();
            String lifeCycleState = lifecycleStateManager.getInitialState();
            String[] publishStates = {"IN-REVIEW", "APPROVED", "PUBLISHED"};

            applicationReleaseDTO.setCurrentState(lifeCycleState);
            LifecycleState lifecycleState = getLifecycleStateInstance(lifeCycleState, lifeCycleState);
            applicationReleaseDTO = this.applicationReleaseDAO
                    .createRelease(applicationReleaseDTO, applicationDTO.getId(), tenantId);
            this.lifecycleStateDAO
                    .addLifecycleState(lifecycleState, applicationReleaseDTO.getId(), tenantId);
            if(isPublished){
                for (String state: publishStates) {
                    LifecycleChanger lifecycleChanger = new LifecycleChanger();
                    lifecycleChanger.setAction(state);
                    lifecycleChanger.setReason("Updated to " + state);
                    this.changeLifecycleState(applicationReleaseDTO, lifecycleChanger);
                }
            }
            ApplicationRelease applicationRelease = APIUtil.releaseDtoToRelease(applicationReleaseDTO);
            ConnectionManagerUtil.commitDBTransaction();
            return applicationRelease;
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while staring application release creating transaction for application Id: "
                    + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while adding application release into IoTS app management ApplicationDTO id of"
                    + " the application release: " + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifeCycleManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while adding new application release lifecycle state to the application"
                    + " release: " + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while adding new application release for application " + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public ApplicationDTO getApplication(int applicationId) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            ApplicationDTO applicationDTO = this.applicationDAO.getApplication(applicationId, tenantId);
            if (applicationDTO == null) {
                String msg = "Couldn't find application for the application Id: " + applicationId;
                log.error(msg);
                throw new NotFoundException(msg);
            }
            return applicationDTO;
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection for getting application for the app ID"
                    + " " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting application data for application ID: " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public  ApplicationList getSubscribedAppsOfDevice(int deviceId, PaginationRequest request) throws ApplicationManagementException {
        ApplicationList applicationList = new ApplicationList();
        List<Application> applications = new ArrayList<>();
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            List<ApplicationDTO>  applicationDTOS = this.applicationDAO.getSubscribedAppsOfDevice(deviceId, tenantId, request);
            for (ApplicationDTO applicationDTO: applicationDTOS) {
                applicationDTO.setTags(this.applicationDAO.getAppTags(applicationDTO.getId(), tenantId));
                applicationDTO.setAppCategories(this.applicationDAO.getAppCategories(applicationDTO.getId(), tenantId));
                applications.add(APIUtil.appDtoToAppResponse(applicationDTO));
            }

            List<ApplicationDTO>  totalApplications = this.applicationDAO.getSubscribedAppsOfDevice(deviceId, tenantId, null);
            Pagination pagination = new Pagination();
            pagination.setCount(totalApplications.size());
            pagination.setSize(applications.size());
            pagination.setOffset(request.getStartIndex());
            pagination.setLimit(request.getRowCount());
            applicationList.setApplications(applications);
            applicationList.setPagination(pagination);
            return applicationList;
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting installed apps of device with device id: "
                    + deviceId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "DB Connection error occurred while getting  installed apps of device with device id: " + deviceId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * Check whether given OS range is valid or invalid
     *
     * @param osRange OS range
     * @param deviceTypeName Device Type
     * @return true if invalid OS range, Otherwise returns false
     * @throws ApplicationManagementException if error occurred while getting device type version for lower OS version
     * and higher OS version
     */
    private boolean isInvalidOsVersionRange(String osRange, String deviceTypeName)
            throws ApplicationManagementException {
        String lowestSupportingOsVersion;
        String highestSupportingOsVersion = null;
        String[] supportedOsVersionValues = osRange.split("-");
        lowestSupportingOsVersion = supportedOsVersionValues[0].trim();
        if (!"ALL".equals(supportedOsVersionValues[1].trim())) {
            highestSupportingOsVersion = supportedOsVersionValues[1].trim();
        }

        try {
            DeviceManagementProviderService deviceManagementProviderService = DataHolder.getInstance()
                    .getDeviceManagementService();
            return deviceManagementProviderService.getDeviceTypeVersion(deviceTypeName, lowestSupportingOsVersion)
                    == null || (highestSupportingOsVersion != null
                    && deviceManagementProviderService.getDeviceTypeVersion(deviceTypeName, highestSupportingOsVersion)
                    == null);
        } catch (DeviceManagementException e) {
            String msg =
                    "Error occurred while getting supported device type versions for device type : " + deviceTypeName;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public Application getApplicationById(int appId, String state) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        ApplicationDTO applicationDTO = getApplication(appId);
        try {
            ConnectionManagerUtil.openDBConnection();
            List<String> unrestrictedRoles = this.visibilityDAO.getUnrestrictedRoles(appId, tenantId);
            if (!unrestrictedRoles.isEmpty() && !hasUserRole(unrestrictedRoles, userName)) {
                String msg = "You are trying to access visibility restricted application and you are not assigned"
                        + " required roles to view this application,";
                log.error(msg);
                throw new ForbiddenException(msg);
            }
            if (lifecycleStateManager.getEndState().equals(applicationDTO.getStatus())) {
                return null;
            }

            List<ApplicationReleaseDTO> filteredApplicationReleaseDTOs = new ArrayList<>();
            AtomicBoolean isDeletableApp = new AtomicBoolean(true);
            AtomicBoolean isHideableApp = new AtomicBoolean(true);
            for (ApplicationReleaseDTO applicationReleaseDTO : applicationDTO.getApplicationReleaseDTOs()) {
                if (!applicationReleaseDTO.getCurrentState().equals(lifecycleStateManager.getEndState())) {
                    if (isHideableApp.get()) {
                        isHideableApp.set(false);
                    }
                    if (isDeletableApp.get() && !lifecycleStateManager
                            .isDeletableState(applicationReleaseDTO.getCurrentState())) {
                        isDeletableApp.set(false);
                    }
                    if (state == null || state.equals(applicationReleaseDTO.getCurrentState())) {
                        filteredApplicationReleaseDTOs.add(applicationReleaseDTO);
                    }
                }
            }

            applicationDTO.setApplicationReleaseDTOs(filteredApplicationReleaseDTOs);
            applicationDTO.setTags(this.applicationDAO.getAppTags(appId, tenantId));
            applicationDTO.setAppCategories(this.applicationDAO.getAppCategories(appId, tenantId));

            Application application = APIUtil.appDtoToAppResponse(applicationDTO);
            application.setHideableApp(isHideableApp.get());
            application.setDeletableApp(isDeletableApp.get());
            return application;
        } catch (DBConnectionException e) {
            String msg =
                    "Error occurred while obtaining the database connection to get application for application ID: "
                            + appId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifecycleManagementException e) {
            String msg = "Error occurred when getting the last state of the application lifecycle flow";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (UserStoreException e) {
            String msg = "User-store exception while getting application with the application id " + appId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting, either application tags or application categories";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public Application getApplicationByUuid(String releaseUuid) throws ApplicationManagementException{
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();

        try {
            ConnectionManagerUtil.openDBConnection();
            ApplicationDTO applicationDTO = applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            if (applicationDTO == null) {
                String msg = "Couldn't found an application for application release UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }

            ApplicationReleaseDTO applicationReleaseDTO = applicationDTO.getApplicationReleaseDTOs().get(0);
            if (lifecycleStateManager.isEndState(applicationReleaseDTO.getCurrentState())) {
                return null;
            }

            List<String> unrestrictedRoles = this.visibilityDAO.getUnrestrictedRoles(applicationDTO.getId(), tenantId);
            if (!unrestrictedRoles.isEmpty() && !hasUserRole(unrestrictedRoles, userName)) {
                String msg = "You are trying to access visibility restricted application. You don't have required "
                        + "roles to view this application,";
                log.error(msg);
                throw new ForbiddenException(msg);
            }

            applicationDTO.setTags(this.applicationDAO.getAppTags(applicationDTO.getId(), tenantId));
            applicationDTO.setAppCategories(this.applicationDAO.getAppCategories(applicationDTO.getId(), tenantId));

            Application application = APIUtil.appDtoToAppResponse(applicationDTO);
            if (lifecycleStateManager.isDeletableState(applicationReleaseDTO.getCurrentState())) {
                ApplicationDTO entireApplication = applicationDAO.getApplication(applicationDTO.getId(), tenantId);
                application.setDeletableApp(isDeletableApp(entireApplication.getApplicationReleaseDTOs()));
            }
            return application;
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection to get application for application "
                    + "release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (UserStoreException e) {
            String msg = "User-store exception occurred while getting application for application release UUID "
                    + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting dta which are related to Application.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public Application getApplicationByUuid(String releaseUuid, String state) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        boolean isVisibleApp = false;

        try {
            ConnectionManagerUtil.openDBConnection();
            ApplicationDTO applicationDTO = applicationDAO.getApplication(releaseUuid, tenantId);

            if (applicationDTO == null) {
                String msg = "Couldn't found an application for application release UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }

            List<ApplicationReleaseDTO> filteredApplicationReleaseDTOs = new ArrayList<>();
            for (ApplicationReleaseDTO applicationReleaseDTO : applicationDTO.getApplicationReleaseDTOs()) {
                if (!applicationReleaseDTO.getCurrentState().equals(lifecycleStateManager.getEndState()) && (
                        state == null || applicationReleaseDTO.getCurrentState().equals(state))) {
                    filteredApplicationReleaseDTOs.add(applicationReleaseDTO);
                }
            }
            if (state != null && filteredApplicationReleaseDTOs.isEmpty()) {
                return null;
            }
            applicationDTO.setApplicationReleaseDTOs(filteredApplicationReleaseDTOs);
            setApplicationProperties(applicationDTO);

            List<String> unrestrictedRoles = this.visibilityDAO.getUnrestrictedRoles(applicationDTO.getId(), tenantId);
            if (!unrestrictedRoles.isEmpty()) {
                if (hasUserRole(unrestrictedRoles, userName)) {
                    isVisibleApp = true;
                }
            } else {
                isVisibleApp = true;
            }

            if (!isVisibleApp) {
                String msg = "You are trying to access visibility restricted application. You don't have required "
                        + "roles to view this application,";
                log.error(msg);
                throw new ForbiddenException(msg);
            }
            return APIUtil.appDtoToAppResponse(applicationDTO);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection to get application for application "
                    + "release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifecycleManagementException e) {
            String msg = "Error occurred when getting the last state of the application lifecycle flow";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (UserStoreException e) {
            String msg = "User-store exception while getting application with the application release UUID " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting, application data.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * This is useful to set application properties that are mapped in other database tables (I.E: tags and categories)
     */
    private void setApplicationProperties(ApplicationDTO applicationDTO) throws ApplicationManagementDAOException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        applicationDTO.setTags(applicationDAO.getAppTags(applicationDTO.getId(), tenantId));
        applicationDTO.setAppCategories(applicationDAO.getAppCategories(applicationDTO.getId(), tenantId));
        applicationDTO.setFavourite(applicationDAO.isFavouriteApp(applicationDTO.getId(), userName, tenantId));
    }

    /**
     * Check whether at least one role is assigned to the given user.
     *
     * @param unrestrictedRoleList unrestricted role list
     * @param userName Username
     * @return true at least one unrestricted role has assigned to given user, otherwise returns false.
     * @throws UserStoreException If it is unable to load {@link UserRealm} from {@link CarbonContext}
     */
    private boolean hasUserRole(Collection<String> unrestrictedRoleList, String userName) throws UserStoreException {
        String[] roleList = getRolesOfUser(userName);
        for (String unrestrictedRole : unrestrictedRoleList) {
            for (String role : roleList) {
                if (unrestrictedRole.equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether valid unrestricted role list or not
     *
     * @return true or false
     * @throws UserStoreException If it is unable to load {@link UserRealm} from {@link CarbonContext}
     */
    private boolean isValidRestrictedRole(Collection<String> unrestrictedRoleList) throws UserStoreException {
        //todo check role by role
        UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();
        if (userRealm != null) {
            List<String> roleList = new ArrayList<>(Arrays.asList(userRealm.getUserStoreManager().getRoleNames()));
            return roleList.containsAll(unrestrictedRoleList);
        } else {
            String msg = "User realm is not initiated.";
            log.error(msg);
            throw new UserStoreException(msg);
        }
    }

    /**
     * Check whether valid metaData value or not
     *
     * @return true or false
     * @throws MetadataManagementException If it is unable to load metaData
     */
    private boolean isUserAbleToViewAllRoles() throws MetadataManagementException {
        List<Metadata> allMetadata;
        allMetadata = APIUtil.getMetadataManagementService().retrieveAllMetadata();
        if (allMetadata != null && !allMetadata.isEmpty()) {
            for (Metadata metadata : allMetadata) {
                if (Constants.SHOW_ALL_ROLES.equals(metadata.getMetaKey())) {
                    String metaValue = metadata.getMetaValue();
                    if (metaValue != null) {
                        JSONObject jsonObject;
                        jsonObject = new JSONObject(metaValue);
                        return jsonObject.getBoolean(Constants.IS_USER_ABLE_TO_VIEW_ALL_ROLES);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get assigned role list of the given user.
     *
     * @param userName Username
     * @return List of roles
     * @throws UserStoreException If it is unable to load {@link UserRealm} from {@link CarbonContext}
     */
    private String[] getRolesOfUser(String userName) throws UserStoreException {
        UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();
        String[] roleList;
        if (userRealm != null) {
            userRealm.getUserStoreManager().getRoleNames();
            roleList = userRealm.getUserStoreManager().getRoleListOfUser(userName);
        } else {
            String msg = "User realm is not initiated. Logged in user: " + userName;
            log.error(msg);
            throw new UserStoreException(msg);
        }
        return roleList;
    }

    @Override
    public void deleteApplication(int applicationId) throws ApplicationManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Request is received to delete applications which are related with the application id "
                    + applicationId);
        }
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationDTO applicationDTO = getApplication(applicationId);
        deleteApplication(applicationDTO, tenantId);
    }

    /**
     * Delete the entire app data and the each app release data and artifacts.
     *
     * @param applicationDTO ApplicationDTO object
     * @param tenantId Tenant Id
     * @throws ApplicationManagementException if error occurred while deleting application data or app relase data.
     */
    private void deleteApplication(ApplicationDTO applicationDTO, int tenantId) throws ApplicationManagementException {
        List<Integer> deletingAppReleaseIds = new ArrayList<>();
        List<String> deletingAppHashVals = new ArrayList<>();
        try {
            ConnectionManagerUtil.beginDBTransaction();
            for (ApplicationReleaseDTO applicationReleaseDTO : applicationDTO.getApplicationReleaseDTOs()) {
                if (!lifecycleStateManager.isDeletableState(applicationReleaseDTO.getCurrentState())){
                    String msg = "Application release which has application release UUID: " +
                            applicationReleaseDTO.getUuid() + " is not in a deletable state. Therefore Application "
                            + "deletion is not permitted. In order to delete the application, all application releases "
                            + "of the application has to be in a deletable state.";
                    log.error(msg);
                    throw new ForbiddenException(msg);
                }
                if (!subscriptionDAO.getDeviceSubscriptions(applicationReleaseDTO.getId(), tenantId, null, null).isEmpty()) {
                    String msg = "Application release which has UUID: " + applicationReleaseDTO.getUuid()
                            + " either subscribed to device/s or it had subscribed to device/s. Therefore you are not "
                            + "permitted to delete the application release.";
                    log.error(msg);
                    throw new ForbiddenException(msg);
                }
                deletingAppHashVals.add(applicationReleaseDTO.getAppHashValue());
                deletingAppReleaseIds.add(applicationReleaseDTO.getId());
            }
            this.lifecycleStateDAO.deleteLifecycleStates(deletingAppReleaseIds);
            this.applicationReleaseDAO.deleteReleases(deletingAppReleaseIds);
            this.applicationDAO.deleteApplicationTags(applicationDTO.getId(), tenantId);
            this.applicationDAO.deleteAppCategories(applicationDTO.getId(), tenantId);
            this.visibilityDAO.deleteAppUnrestrictedRoles(applicationDTO.getId(), tenantId);
            this.spApplicationDAO.deleteApplicationFromServiceProviders(applicationDTO.getId(), tenantId);
            this.applicationDAO.deleteApplication(applicationDTO.getId(), tenantId);
            APIUtil.getApplicationStorageManager().deleteAllApplicationReleaseArtifacts(deletingAppHashVals, tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to delete application which has "
                    + "application ID: " + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when deleting application which has application ID: "
                    + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when getting application data for application id: " + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationStorageManagementException e) {
            String msg = "Error occurred when deleting application artifacts in the file system. Application id: "
                    + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifeCycleManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while deleting life-cycle state data of application releases of the application"
                    + " which has application ID: " + applicationDTO.getId();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void retireApplication(int applicationId) throws ApplicationManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Request is received to delete applications which are related with the application id "
                    + applicationId);
        }
        ApplicationDTO applicationDTO = getApplication(applicationId);
        try {
            ConnectionManagerUtil.beginDBTransaction();
            List<ApplicationReleaseDTO> applicationReleaseDTOs = applicationDTO.getApplicationReleaseDTOs();
            List<ApplicationReleaseDTO> activeApplicationReleaseDTOs = new ArrayList<>();
            for (ApplicationReleaseDTO applicationReleaseDTO : applicationReleaseDTOs) {
                if (!applicationReleaseDTO.getCurrentState().equals(lifecycleStateManager.getEndState())) {
                    activeApplicationReleaseDTOs.add(applicationReleaseDTO);
                }
            }
            if (!activeApplicationReleaseDTOs.isEmpty()) {
                String msg = "There are application releases which are not in the " + lifecycleStateManager
                        .getEndState() + " state. Hence you are not allowed to delete the application";
                log.error(msg);
                throw new ForbiddenException(msg);
            }
            this.applicationDAO.retireApplication(applicationId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to retire an application which has "
                    + "application ID:" + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when retiring application which has application ID: "
                    + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting application data for application id: " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void deleteApplicationRelease(String releaseUuid) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();
        ApplicationDTO applicationDTO;
        try {
            ConnectionManagerUtil.openDBConnection();
            applicationDTO = this.applicationDAO.getApplication(releaseUuid, tenantId);
            if (applicationDTO == null) {
                String msg = "Couldn't find an application which has application release UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to get application which has "
                    + "application release of UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg =
                    "Error occurred when getting application data which has application release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }

        for (ApplicationReleaseDTO applicationReleaseDTO : applicationDTO.getApplicationReleaseDTOs()) {
            if (releaseUuid.equals(applicationReleaseDTO.getUuid())) {
                if (!lifecycleStateManager.isDeletableState(applicationReleaseDTO.getCurrentState())) {
                    String msg =
                            "Application state is not in the deletable state. Therefore you are not permitted to "
                                    + "delete the application release.";
                    log.error(msg);
                    throw new ForbiddenException(msg);
                }
                try {
                    ConnectionManagerUtil.beginDBTransaction();
                    List<DeviceSubscriptionDTO> deviceSubscriptionDTOS = subscriptionDAO
                            .getDeviceSubscriptions(applicationReleaseDTO.getId(), tenantId, null, null);
                    if (!deviceSubscriptionDTOS.isEmpty()) {
                        String msg = "Application release which has UUID: " + applicationReleaseDTO.getUuid()
                                + " either subscribed to device/s or it had subscribed to device/s. Therefore you "
                                + "are not permitted to delete the application release.";
                        log.error(msg);
                        throw new ForbiddenException(msg);
                    }
                    lifecycleStateDAO.deleteLifecycleStateByReleaseId(applicationReleaseDTO.getId());
                    applicationReleaseDAO.deleteRelease(applicationReleaseDTO.getId());
                    applicationStorageManager.deleteAllApplicationReleaseArtifacts(
                            Collections.singletonList(applicationReleaseDTO.getAppHashValue()), tenantId);
                    ConnectionManagerUtil.commitDBTransaction();
                } catch (DBConnectionException e) {
                    String msg = "Error occurred while observing the database connection to delete application "
                            + "release which has the UUID:" + releaseUuid;
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } catch (TransactionManagementException e) {
                    String msg = "Database access error is occurred when deleting application release which has "
                            + "the UUID: " + releaseUuid;
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } catch (ApplicationManagementDAOException e) {
                    ConnectionManagerUtil.rollbackDBTransaction();
                    String msg = "Error occurred while verifying whether application release has an subscription or "
                            + "not. Application release UUID: " + releaseUuid;
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } catch (ApplicationStorageManagementException e) {
                    String msg = "Error occurred when deleting the application release artifact from the file "
                            + "system. Application release UUID: " + releaseUuid;
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } catch (LifeCycleManagementDAOException e) {
                    ConnectionManagerUtil.rollbackDBTransaction();
                    String msg = "Error occurred when deleting lifecycle data for application release UUID: "
                            + releaseUuid;
                    log.error(msg, e);
                    throw new ApplicationManagementException(msg, e);
                } finally {
                    ConnectionManagerUtil.closeDBConnection();
                }
                break;
            }
        }
        try {
            deleteAppIconInfo(applicationDTO);
        } catch (ApplicationManagementException e) {
            String msg = "Error occurred while deleting application icon info. Application package name: " + applicationDTO.getPackageName();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public void updateApplicationImageArtifact(String uuid, ApplicationArtifact applicationArtifact)
            throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationReleaseDTO applicationReleaseDTO;
        try {
            ConnectionManagerUtil.beginDBTransaction();
            applicationReleaseDTO = this.applicationReleaseDAO.getReleaseByUUID(uuid, tenantId);
            if (applicationReleaseDTO == null) {
                String msg = "Application release image artifact uploading is failed. Doesn't exist a application "
                        + "release for application ID: application UUID: " + uuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }

            applicationReleaseDTO = this.applicationReleaseDAO
                    .updateRelease(updateImageArtifacts(applicationReleaseDTO, applicationArtifact, tenantId), tenantId);
            if (applicationReleaseDTO == null) {
                ConnectionManagerUtil.rollbackDBTransaction();
                String msg = "Application release updating count is 0 for application release UUID: " + uuid;
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg =
                    "Error occurred when getting DB connection to update image artifacts of the application release "
                            + "which has  uuid " + uuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating application release image artifacts which has "
                    + "UUID: " + uuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg =
                    "Error occurred while getting application release data for updating image artifacts of the application release uuid "
                            + uuid + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ResourceManagementException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while updating image artifacts of the application release uuid " + uuid + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg , e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void updateApplicationArtifact(String deviceType, String releaseUuid,
                                          ApplicationArtifact applicationArtifact) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        boolean isValidDeviceType = false;
        List<DeviceType> deviceTypes;
        try {
            deviceTypes = DataHolder.getInstance().getDeviceManagementService().getDeviceTypes();
            for (DeviceType dt : deviceTypes) {
                if (dt.getName().equals(deviceType)) {
                    isValidDeviceType = true;
                    break;
                }
            }
            if (!isValidDeviceType) {
                String msg = "Invalid request to update application release artifact, invalid application type: "
                        + deviceType + " for application release uuid: " + releaseUuid;
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while getting supported device types in IoTS";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }

        try {
            ConnectionManagerUtil.beginDBTransaction();
            ApplicationDTO applicationDTO = this.applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            if (applicationDTO == null) {
                String msg = "Couldn't found an application which has application release for UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }
            if (!ApplicationType.ENTERPRISE.toString().equals(applicationDTO.getType())) {
                String msg = "If Application type is " + applicationDTO.getType() + ", then you don't have application "
                        + "release artifact to update for application release UUID: " + releaseUuid;
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }

            ApplicationReleaseDTO applicationReleaseDTO = applicationDTO.getApplicationReleaseDTOs().get(0);
            if (!lifecycleStateManager.isUpdatableState(applicationReleaseDTO.getCurrentState())) {
                String msg = "Application release in " + applicationReleaseDTO.getCurrentState()
                        + " state. Therefore you are not allowed to update the application release. Hence, "
                        + "please move application release from " + applicationReleaseDTO.getCurrentState()
                        + " to updatable state.";
                log.error(msg);
                throw new ForbiddenException(msg);
            }

            applicationReleaseDTO = updateEntAppReleaseArtifact(deviceType,applicationReleaseDTO
                    , applicationArtifact);
            applicationReleaseDTO = this.applicationReleaseDAO.updateRelease(applicationReleaseDTO, tenantId);
            if (applicationReleaseDTO == null) {
                ConnectionManagerUtil.rollbackDBTransaction();
                throw new ApplicationManagementException(
                        "ApplicationDTO release updating count is 0.  ApplicationDTO release UUID: " + releaseUuid);

            }
            ConnectionManagerUtil.commitDBTransaction();
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while getting/updating APPM DB for updating application Installer.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while starting the transaction to update application release artifact which has "
                    + "application uuid " + releaseUuid + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred when getting DB connection to update application release artifact of the "
                    + "application release uuid " + releaseUuid + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationStorageManagementException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "In order to update the artifact, couldn't find it in the system";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ResourceManagementException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when updating application installer.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<LifecycleState> getLifecycleStateChangeFlow(String releaseUuid) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            ApplicationReleaseDTO applicationReleaseDTO = this.applicationReleaseDAO
                    .getReleaseByUUID(releaseUuid, tenantId);
            if (applicationReleaseDTO == null) {
                String msg = "Couldn't found an application release for application release UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }
            return this.lifecycleStateDAO.getLifecycleStates(applicationReleaseDTO.getId(), tenantId);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection to get lifecycle state change flow for "
                    + "application release which has UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifeCycleManagementDAOException e) {
            String msg = "Failed to get lifecycle state for application release uuid " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg =
                    "Error occurred while getting application release for application release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public ApplicationRelease changeLifecycleState(String releaseUuid, LifecycleChanger lifecycleChanger)
            throws ApplicationManagementException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (lifecycleChanger == null || StringUtils.isEmpty(lifecycleChanger.getAction())) {
            String msg = "The Action is null or empty. Please verify the request.";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        try {
            ConnectionManagerUtil.beginDBTransaction();
            ApplicationReleaseDTO applicationReleaseDTO = this.applicationReleaseDAO
                    .getReleaseByUUID(releaseUuid, tenantId);

            if (applicationReleaseDTO == null) {
                String msg = "Couldn't found an application release for the UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }
            if (lifecycleStateManager
                    .isValidStateChange(applicationReleaseDTO.getCurrentState(), lifecycleChanger.getAction(), userName,
                            tenantId)) {
                if (lifecycleStateManager.isInstallableState(lifecycleChanger.getAction()) && applicationReleaseDAO
                        .hasExistInstallableAppRelease(applicationReleaseDTO.getUuid(),
                                lifecycleStateManager.getInstallableState(), tenantId)) {
                    String msg = "Installable application release is already registered for the application. "
                            + "Therefore it is not permitted to change the lifecycle state from "
                            + applicationReleaseDTO.getCurrentState() + " to " + lifecycleChanger.getAction();
                    log.error(msg);
                    throw new ForbiddenException(msg);
                }
                LifecycleState lifecycleState = new LifecycleState();
                lifecycleState.setCurrentState(lifecycleChanger.getAction());
                lifecycleState.setPreviousState(applicationReleaseDTO.getCurrentState());
                lifecycleState.setUpdatedBy(userName);
                lifecycleState.setReasonForChange(lifecycleChanger.getReason());
                applicationReleaseDTO.setCurrentState(lifecycleChanger.getAction());
                if (this.applicationReleaseDAO.updateRelease(applicationReleaseDTO, tenantId) == null) {
                    String msg = "Application release updating is failed/.";
                    log.error(msg);
                    throw new ApplicationManagementException(msg);
                }
                this.lifecycleStateDAO.addLifecycleState(lifecycleState, applicationReleaseDTO.getId(), tenantId);
                ConnectionManagerUtil.commitDBTransaction();
                return APIUtil.releaseDtoToRelease(applicationReleaseDTO);
            } else {
                String msg = "Invalid lifecycle state transition from '" + applicationReleaseDTO.getCurrentState() + "'"
                        + " to '" + lifecycleChanger.getAction() + "'";
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to change lifecycle state of the "
                    + "application release which has UUID:" + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when changing lifecycle state of application release which "
                    + "has UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }catch (LifeCycleManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Failed to add lifecycle state for Application release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when accessing application release data of application release which has the "
                    + "application release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    public ApplicationRelease changeLifecycleState(ApplicationReleaseDTO applicationReleaseDTO, LifecycleChanger lifecycleChanger) throws ApplicationManagementException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (lifecycleChanger == null || StringUtils.isEmpty(lifecycleChanger.getAction())) {
            String msg = "The Action is null or empty. Please verify the request.";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        try{
            if (lifecycleStateManager
                    .isValidStateChange(applicationReleaseDTO.getCurrentState(), lifecycleChanger.getAction(), userName,
                            tenantId)) {
                if (lifecycleStateManager.isInstallableState(lifecycleChanger.getAction()) && applicationReleaseDAO
                        .hasExistInstallableAppRelease(applicationReleaseDTO.getUuid(),
                                lifecycleStateManager.getInstallableState(), tenantId)) {
                    String msg = "Installable application release is already registered for the application. "
                            + "Therefore it is not permitted to change the lifecycle state from "
                            + applicationReleaseDTO.getCurrentState() + " to " + lifecycleChanger.getAction();
                    log.error(msg);
                    throw new ForbiddenException(msg);
                }
                LifecycleState lifecycleState = new LifecycleState();
                lifecycleState.setCurrentState(lifecycleChanger.getAction());
                lifecycleState.setPreviousState(applicationReleaseDTO.getCurrentState());
                lifecycleState.setUpdatedBy(userName);
                lifecycleState.setReasonForChange(lifecycleChanger.getReason());
                applicationReleaseDTO.setCurrentState(lifecycleChanger.getAction());
                if (this.applicationReleaseDAO.updateRelease(applicationReleaseDTO, tenantId) == null) {
                    String msg = "Application release updating is failed/.";
                    log.error(msg);
                    throw new ApplicationManagementException(msg);
                }
                this.lifecycleStateDAO.addLifecycleState(lifecycleState, applicationReleaseDTO.getId(), tenantId);
                return APIUtil.releaseDtoToRelease(applicationReleaseDTO);
            } else {
                String msg = "Invalid lifecycle state transition from '" + applicationReleaseDTO.getCurrentState() + "'"
                        + " to '" + lifecycleChanger.getAction() + "'";
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when accessing application release data of application release which has the "
                    + "application release UUID: " + applicationReleaseDTO.getUuid();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifeCycleManagementDAOException e) {
            String msg = "Failed to add lifecycle state for Application release UUID: " + applicationReleaseDTO.getUuid();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }


    @Override
    public void addApplicationCategories(List<String> categories) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.beginDBTransaction();
            List<CategoryDTO> existingCategories = applicationDAO.getAllCategories(tenantId);
            List<String> existingCategoryNames = existingCategories.stream().map(CategoryDTO::getCategoryName)
                    .collect(Collectors.toList());
            if(!existingCategoryNames.containsAll(categories)){
                List<String> newCategories = getDifference(categories, existingCategoryNames);
                applicationDAO.addCategories(newCategories, tenantId);
            }
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to add application categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when adding application categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when getting existing categories or when inserting new application categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public boolean isExistingAppName(String appName, String deviceTypeName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        DeviceManagementProviderService deviceManagementProviderService = DataHolder.getInstance()
                .getDeviceManagementService();
        try {
            int deviceTypeId;
            if (!deviceTypeName.equals(Constants.ALL)) {
                DeviceType deviceType = deviceManagementProviderService.getDeviceType(deviceTypeName);
                if (deviceType == null) {
                    String msg = "Device type doesn't exist. Hence check the application name existence with valid "
                            + "device type name.";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                deviceTypeId = deviceType.getId();
            } else {
                //For web-clips device type = 'ALL'
                deviceTypeId = 0;
            }
            try {
                ConnectionManagerUtil.openDBConnection();
                if (applicationDAO.isExistingAppName(appName, deviceTypeId, tenantId)) {
                    return true;
                }
            } catch (DBConnectionException e) {
                String msg = "Error occurred while getting DB connection to check the existence of application with "
                        + "name: " + appName + " and the device type: " + deviceTypeName;
                log.error(msg, e);
                throw new ApplicationManagementException(msg, e);
            } catch (ApplicationManagementDAOException e) {
                String msg = "Error occurred while checking the existence of application with " + "name: " + appName
                        + "and the  device type: " + deviceTypeName + " in the database";
                log.error(msg);
                throw new ApplicationManagementException(msg, e);
            } finally {
                ConnectionManagerUtil.closeDBConnection();
            }
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while getting the device type data for device type: " + deviceTypeName;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return false;
    }

    @Override
    public Application updateApplication(int applicationId, ApplicationUpdateWrapper applicationUpdateWrapper)
            throws ApplicationManagementException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        ApplicationDTO applicationDTO = getApplication(applicationId);
        String sanitizedName = "";
        if (!StringUtils.isEmpty(applicationUpdateWrapper.getName())) {
            sanitizedName = ApplicationManagementUtil.sanitizeName(applicationUpdateWrapper.getName(),
                    Constants.ApplicationProperties.NAME );
        }
        try {
            ConnectionManagerUtil.beginDBTransaction();
            if (!StringUtils.isEmpty(sanitizedName) && !applicationDTO.getName()
                    .equals(sanitizedName)) {
                if (applicationDAO
                        .isExistingAppName(sanitizedName.trim(), applicationDTO.getDeviceTypeId(),
                                tenantId)) {
                    String msg = "Already an application registered with same name " + sanitizedName
                            + ". Hence you can't update the application name from " + applicationDTO.getName() + " to "
                            + sanitizedName;
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                applicationDTO.setName(sanitizedName);
            }
            if (!StringUtils.isEmpty(applicationUpdateWrapper.getSubMethod()) && !applicationDTO.getSubType()
                    .equals(applicationUpdateWrapper.getSubMethod())) {
                if (!ApplicationSubscriptionType.PAID.toString().equals(applicationUpdateWrapper.getSubMethod())
                        && !ApplicationSubscriptionType.FREE.toString().equals(applicationUpdateWrapper.getSubMethod())) {
                    String msg = "Invalid application subscription type is found with application updating request "
                            + applicationUpdateWrapper.getSubMethod();
                    log.error(msg);
                    throw new BadRequestException(msg);

                } else if (ApplicationSubscriptionType.FREE.toString().equals(applicationUpdateWrapper.getSubMethod())
                        && !StringUtils.isEmpty(applicationUpdateWrapper.getPaymentCurrency())) {
                    String msg = "If you are going to change paid app as Free app, "
                            + "currency attribute in the application updating payload should be null or \"\"";
                    log.error(msg);
                    throw new ApplicationManagementException(msg);
                } else if (ApplicationSubscriptionType.PAID.toString().equals(applicationUpdateWrapper.getSubMethod())
                        && StringUtils.isEmpty(applicationUpdateWrapper.getPaymentCurrency()) ){
                    String msg = "If you are going to change Free app as paid app, currency attribute in the application"
                            + " payload should not be null or \"\"";
                    log.error(msg);
                    throw new ApplicationManagementException(msg);
                }
                applicationDTO.setSubType(applicationUpdateWrapper.getSubMethod());
                applicationDTO.setPaymentCurrency(applicationUpdateWrapper.getPaymentCurrency());
            }

            if (!StringUtils.isEmpty(applicationUpdateWrapper.getDescription())){
                applicationDTO.setDescription(applicationUpdateWrapper.getDescription());
            }

            List<String> appUnrestrictedRoles = this.visibilityDAO.getUnrestrictedRoles(applicationId, tenantId);
            List<String> appCategories = this.applicationDAO.getAppCategories(applicationId, tenantId);
            List<String> appTags = this.applicationDAO.getAppTags(applicationId, tenantId);

            boolean isExistingAppRestricted = !appUnrestrictedRoles.isEmpty();
            boolean isUpdatingAppRestricted = false;
            if (applicationUpdateWrapper.getUnrestrictedRoles() != null && !applicationUpdateWrapper
                    .getUnrestrictedRoles().isEmpty()) {
                isUpdatingAppRestricted = true;
            }

            if (isExistingAppRestricted && !isUpdatingAppRestricted) {
                visibilityDAO.deleteUnrestrictedRoles(appUnrestrictedRoles, applicationId, tenantId);
                appUnrestrictedRoles.clear();
            } else if (isUpdatingAppRestricted) {
                if (!hasUserRole(applicationUpdateWrapper.getUnrestrictedRoles(), userName)) {
                    String msg =
                            "You are trying to restrict the visibility of visible application.But you are trying to "
                                    + "restrict the visibility to roles that there isn't at least one role is assigned "
                                    + "to user: " + userName + ". Therefore, it is not allowed and you should have "
                                    + "added at least one role that assigned to " + userName + " user into "
                                    + "restricting role set.";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }

                if (!isExistingAppRestricted) {
                    visibilityDAO.addUnrestrictedRoles(applicationUpdateWrapper.getUnrestrictedRoles(), applicationId,
                            tenantId);
                    appUnrestrictedRoles = applicationUpdateWrapper.getUnrestrictedRoles();
                } else {
                    List<String> addingRoleList = getDifference(applicationUpdateWrapper.getUnrestrictedRoles(),
                            appUnrestrictedRoles);
                    List<String> removingRoleList = getDifference(appUnrestrictedRoles,
                            applicationUpdateWrapper.getUnrestrictedRoles());
                    if (!addingRoleList.isEmpty()) {
                        visibilityDAO.addUnrestrictedRoles(addingRoleList, applicationId, tenantId);
                        appUnrestrictedRoles.addAll(addingRoleList);
                    }
                    if (!removingRoleList.isEmpty()) {
                        visibilityDAO.deleteUnrestrictedRoles(removingRoleList, applicationId, tenantId);
                        appUnrestrictedRoles.removeAll(removingRoleList);
                    }
                }
            }

            List<String> updatingAppCategries = applicationUpdateWrapper.getCategories();
            if (updatingAppCategries != null){
                List<CategoryDTO> allCategories = this.applicationDAO.getAllCategories(tenantId);
                List<String> allCategoryName = allCategories.stream().map(CategoryDTO::getCategoryName)
                        .collect(Collectors.toList());

                if (!getDifference(updatingAppCategries, allCategoryName).isEmpty()){
                    String msg = "Application update request contains invalid category names. Hence please verify the "
                            + "request payload";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }

                List<String> addingAppCategories = getDifference(updatingAppCategries, appCategories);
                List<String> removingAppCategories = getDifference(appCategories, updatingAppCategries);
                if (!addingAppCategories.isEmpty()) {
                    List<Integer> categoryIds = this.applicationDAO
                            .getCategoryIdsForCategoryNames(addingAppCategories, tenantId);
                    this.applicationDAO.addCategoryMapping(categoryIds, applicationId, tenantId);
                    appCategories.addAll(addingAppCategories);
                }
                if (!removingAppCategories.isEmpty()) {
                    List<Integer> categoryIds = this.applicationDAO
                            .getCategoryIdsForCategoryNames(removingAppCategories, tenantId);
                    this.applicationDAO.deleteAppCategories(categoryIds, applicationId, tenantId);
                    appCategories.removeAll(removingAppCategories);
                }
            }

            List<String> updatingAppTags = applicationUpdateWrapper.getTags();
            if (updatingAppTags!= null){
                List<String> addingTagList = getDifference(updatingAppTags, appTags);
                List<String> removingTagList = getDifference(appTags, updatingAppTags);
                if (!addingTagList.isEmpty()) {
                    List<TagDTO> allTags = this.applicationDAO.getAllTags(tenantId);
                    List<String> newTags = addingTagList.stream().filter(updatingTagName -> allTags.stream()
                            .noneMatch(tag -> tag.getTagName().equals(updatingTagName))).collect(Collectors.toList());
                    if (!newTags.isEmpty()){
                        this.applicationDAO.addTags(newTags, tenantId);
                    }
                    List<Integer> addingTagIds = this.applicationDAO.getTagIdsForTagNames(addingTagList, tenantId);
                    this.applicationDAO.addTagMapping(addingTagIds, applicationId, tenantId);
                    appTags.addAll(addingTagList);
                }
                if (!removingTagList.isEmpty()) {
                    List<Integer> removingTagIds = this.applicationDAO.getTagIdsForTagNames(removingTagList, tenantId);
                    this.applicationDAO.deleteApplicationTags(removingTagIds, applicationId, tenantId);
                    appTags.removeAll(removingTagList);
                }
            }
            if (!applicationDAO.updateApplication(applicationDTO, tenantId)){
                ConnectionManagerUtil.rollbackDBTransaction();
                String msg = "Any application is not updated for the application ID: " + applicationId;
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }

            applicationDTO.setUnrestrictedRoles(appUnrestrictedRoles);
            applicationDTO.setAppCategories(appCategories);
            applicationDTO.setTags(appTags);
            ConnectionManagerUtil.commitDBTransaction();
            return APIUtil.appDtoToAppResponse(applicationDTO);
        } catch (UserStoreException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while checking whether logged in user is ADMIN or not when updating "
                    + "application of application id: " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while updating the application, application id: " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (VisibilityManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while updating the visibility restriction of the application. Application id: "
                    + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while starting database transaction for application updating. Application id: "
                    + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting database connection for application updating. Application id: "
                    + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<Tag> getRegisteredTags() throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            List<TagDTO> tags = applicationDAO.getAllTags(tenantId);
            List<Integer> mappedTagIds = applicationDAO.getDistinctTagIdsInTagMapping();
            List<Tag> responseTagList = new ArrayList<>();
            tags.forEach(tag -> {
                Tag responseTag = new Tag();
                if (!mappedTagIds.contains(tag.getId())) {
                    responseTag.setTagDeletable(true);
                }
                responseTag.setTagName(tag.getTagName());
                responseTagList.add(responseTag);
            });
            return responseTagList;
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection to get registered tags";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting registered tags from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<Category> getRegisteredCategories() throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            List<CategoryDTO> categories = applicationDAO.getAllCategories(tenantId);
            List<Integer> mappedCategoryIds = applicationDAO.getDistinctCategoryIdsInCategoryMapping();
            List<Category> responseCategoryList = new ArrayList<>();
            categories.forEach(category -> {
                Category responseCategory = new Category();
                if (!mappedCategoryIds.contains(category.getId())) {
                    responseCategory.setCategoryDeletable(true);
                }
                responseCategory.setCategoryName(category.getCategoryName());
                responseCategoryList.add(responseCategory);
            });
            return responseCategoryList;
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection to get registered categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting registered tags from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void deleteApplicationTag(int appId, String tagName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ApplicationDTO applicationDTO = getApplication(appId);
            ConnectionManagerUtil.beginDBTransaction();
            TagDTO tag = applicationDAO.getTagForTagName(tagName, tenantId);
            if (tag == null){
                String msg = "Couldn't found a tag for tag name " + tagName + ".";
                log.error(msg);
                throw new NotFoundException(msg);
            }
            if (applicationDAO.hasTagMapping(tag.getId(), applicationDTO.getId(), tenantId)){
                applicationDAO.deleteApplicationTag(tag.getId(), applicationDTO.getId(), tenantId);
                ConnectionManagerUtil.commitDBTransaction();
            } else {
                String msg = "Tag " + tagName + " is not an application tag. Application name: " + applicationDTO.getName();
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to delete application tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when deleting application tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting tag Id or deleting tag mapping from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void deleteTag(String tagName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.beginDBTransaction();
            TagDTO tag = applicationDAO.getTagForTagName(tagName, tenantId);
            if (tag == null){
                String msg = "Couldn't found a tag for tag name " + tagName + ".";
                log.error(msg);
                throw new NotFoundException(msg);
            }
            if (applicationDAO.hasTagMapping(tag.getId(), tenantId)){
                applicationDAO.deleteTagMapping(tag.getId(), tenantId);
            }
            applicationDAO.deleteTag(tag.getId(), tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to delete registered tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when deleting registered tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting tag Id or deleting the tag from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void deleteUnusedTag(String tagName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.beginDBTransaction();
            TagDTO tag = applicationDAO.getTagForTagName(tagName, tenantId);
            if (tag == null){
                String msg = "Couldn't found a tag for tag name " + tagName + ".";
                log.error(msg);
                throw new NotFoundException(msg);
            }
            if (applicationDAO.hasTagMapping(tag.getId(), tenantId)){
                String msg =
                        "Tag " + tagName + " is used for applications. Hence it is not permitted to delete the tag "
                                + tagName;
                log.error(msg);
                throw new ForbiddenException(msg);
            }
            applicationDAO.deleteTag(tag.getId(), tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to delete unused tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when deleting unused tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting tag Ids or deleting the tag from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void updateTag(String oldTagName, String newTagName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        if (StringUtils.isEmpty(oldTagName) || StringUtils.isEmpty(newTagName)) {
            String msg = "Either old tag name or new tag name contains empty/null value. Hence please verify the "
                    + "request.";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        try {
            ConnectionManagerUtil.beginDBTransaction();
            if (applicationDAO.getTagForTagName(newTagName, tenantId) != null){
                String msg =
                        "You are trying to modify tag name into existing tag. Therefore you can't modify tag name from "
                                + oldTagName + " to new tag name " + newTagName;
                log.error(msg);
                throw new BadRequestException(msg);
            }
            TagDTO tag = applicationDAO.getTagForTagName(oldTagName, tenantId);
            if (tag == null){
                String msg = "Couldn't found a tag for tag name " + oldTagName + ".";
                log.error(msg);
                throw new NotFoundException(msg);
            }
            tag.setTagName(newTagName);
            applicationDAO.updateTag(tag, tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to update application tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating application tag.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting tag Ids or deleting the tag from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<String> addTags(List<String> tags) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            if (tags != null && !tags.isEmpty()) {
                ConnectionManagerUtil.beginDBTransaction();
                List<TagDTO> registeredTags = applicationDAO.getAllTags(tenantId);
                List<String> registeredTagNames = registeredTags.stream().map(TagDTO::getTagName)
                        .collect(Collectors.toList());

                List<String> newTags = getDifference(tags, registeredTagNames);
                if (!newTags.isEmpty()) {
                    this.applicationDAO.addTags(newTags, tenantId);
                    ConnectionManagerUtil.commitDBTransaction();
                    if (log.isDebugEnabled()) {
                        log.debug("New tags are added to the AP_APP_TAG table.");
                    }
                }
                return Stream.concat(registeredTagNames.stream(), newTags.stream()).collect(Collectors.toList());
            } else{
                String msg = "Tag list is either null of empty. In order to add new tags, tag list should be a list of "
                        + "Stings. Therefore please verify the payload.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to add tags.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when adding tags.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred either getting registered tags or adding new tags.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<String> addApplicationTags(int appId, List<String> tags) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ApplicationDTO applicationDTO = getApplication(appId);
            if (tags != null && !tags.isEmpty()) {
                ConnectionManagerUtil.beginDBTransaction();
                List<TagDTO> registeredTags = applicationDAO.getAllTags(tenantId);
                List<String> registeredTagNames = registeredTags.stream().map(TagDTO::getTagName)
                        .collect(Collectors.toList());

                List<String> newTags = getDifference(tags, registeredTagNames);
                if (!newTags.isEmpty()) {
                    this.applicationDAO.addTags(newTags, tenantId);
                    if (log.isDebugEnabled()) {
                        log.debug("New tags entries are added to AP_APP_TAG table. App Id:" + applicationDTO.getId());
                    }
                }

                List<String> applicationTags = this.applicationDAO.getAppTags(applicationDTO.getId(), tenantId);
                List<String> newApplicationTags = getDifference(tags, applicationTags);
                if (!newApplicationTags.isEmpty()) {
                    List<Integer> newTagIds = this.applicationDAO.getTagIdsForTagNames(newApplicationTags, tenantId);
                    this.applicationDAO.addTagMapping(newTagIds, applicationDTO.getId(), tenantId);
                    ConnectionManagerUtil.commitDBTransaction();
                }
                return Stream.concat(applicationTags.stream(), newApplicationTags.stream())
                        .collect(Collectors.toList());
            } else {
                String msg = "Tag list is either null or empty. In order to add new tags for application which has "
                        + "application name: " + applicationDTO.getName() +", tag list should be a list of Stings. Therefore please "
                        + "verify the payload.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to add application tags.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when adding application tags.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while accessing application tags. Application ID: " + appId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<String> addCategories(List<String> categories) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            if (categories != null && !categories.isEmpty()) {
                ConnectionManagerUtil.beginDBTransaction();
                List<CategoryDTO> registeredCategories = applicationDAO.getAllCategories(tenantId);
                List<String> registeredCategoryNames = registeredCategories.stream().map(CategoryDTO::getCategoryName)
                        .collect(Collectors.toList());

                List<String> newCategories = getDifference(categories, registeredCategoryNames);
                if (!newCategories.isEmpty()) {
                    this.applicationDAO.addCategories(newCategories, tenantId);
                    ConnectionManagerUtil.commitDBTransaction();
                    if (log.isDebugEnabled()) {
                        log.debug("New categories are added to the AP_APP_TAG table.");
                    }
                }
                return Stream.concat(registeredCategoryNames.stream(), newCategories.stream())
                        .collect(Collectors.toList());
            } else{
                String msg = "Category list is either null of empty. In order to add new categories, category list "
                        + "should be a list of Stings. Therefore please verify the payload.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to add categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when adding categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred either getting registered categories or adding new categories.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void deleteCategory(String categoryName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.beginDBTransaction();
            CategoryDTO category = applicationDAO.getCategoryForCategoryName(categoryName, tenantId);
            if (category == null){
                String msg = "Couldn't found a category for category name " + categoryName + ".";
                log.error(msg);
                throw new NotFoundException(msg);
            }
            if (applicationDAO.hasCategoryMapping(category.getId(), tenantId)){
                String msg = "Category " + category.getCategoryName() + " is used by some applications. Therefore it "
                        + "is not permitted to delete the application category.";
                log.error(msg);
                throw new ForbiddenException(msg);
            }
            applicationDAO.deleteCategory(category.getId(), tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to delete category.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when deleting category.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting category Id or deleting the category from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void updateCategory(String oldCategoryName, String newCategoryName) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        if (StringUtils.isEmpty(oldCategoryName) || StringUtils.isEmpty(newCategoryName)) {
            String msg = "Either old category name or new category name contains empty/null value. Hence please verify the "
                    + "request.";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        try {
            ConnectionManagerUtil.beginDBTransaction();
            CategoryDTO category = applicationDAO.getCategoryForCategoryName(oldCategoryName, tenantId);
            if (category == null){
                String msg = "Couldn't found a category for category name " + oldCategoryName + ".";
                log.error(msg);
                throw new NotFoundException(msg);
            }
            category.setCategoryName(newCategoryName);
            applicationDAO.updateCategory(category, tenantId);
            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to update category.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating category.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred when getting tag Ids or deleting the category from the system.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public String getInstallableLifecycleState() throws ApplicationManagementException {
        if (lifecycleStateManager == null) {
            String msg = "Application lifecycle manager is not initialed. Please contact the administrator.";
            log.error(msg);
            throw new ApplicationManagementException(msg);
        }
        return lifecycleStateManager.getInstallableState();
    }

    /**
     * This method can be used to validate {@link Filter} object.
     *
     * @param filter {@link Filter}
     * @throws BadRequestException if filter object contains incompatible data.
     */
    private void validateFilter(Filter filter) throws BadRequestException {
        if (filter == null) {
            String msg = "Filter validation is failed, Filter shouldn't be null, hence please verify the request payload";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        String appType = filter.getAppType();

        if (!StringUtils.isEmpty(appType)) {
            boolean isValidAppType = false;
            for (ApplicationType applicationType : ApplicationType.values()) {
                if (applicationType.toString().equalsIgnoreCase(appType) || Constants.ALL.equalsIgnoreCase(appType)) {
                    isValidAppType = true;
                    break;
                }
            }
            if (!isValidAppType) {
                String msg =
                        "Filter validation is failed, Invalid application type is found in filter. Application Type: "
                                + appType + " Please verify the request payload";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        }

        RatingConfiguration ratingConfiguration = ConfigurationManager.getInstance().getConfiguration()
                .getRatingConfiguration();

        int defaultMinRating = ratingConfiguration.getMinRatingValue();
        int defaultMaxRating = ratingConfiguration.getMaxRatingValue();
        int filteringMinRating = filter.getMinimumRating();

        if (filteringMinRating != 0 && (filteringMinRating < defaultMinRating || filteringMinRating > defaultMaxRating))
        {
            String msg = "Filter validation is failed, Minimum rating value: " + filteringMinRating
                    + " is not in the range of default minimum rating value " + defaultMaxRating
                    + " and default maximum rating " + defaultMaxRating;
            log.error(msg);
            throw new BadRequestException(msg);
        }

        String appReleaseState = filter.getAppReleaseState();
        if (!StringUtils.isEmpty(appReleaseState) && !lifecycleStateManager.isStateExist(appReleaseState)) {
            String msg = "Filter validation is failed, Requesting to filter by invalid app release state: "
                    + appReleaseState;
            log.error(msg);
            throw new BadRequestException(msg);
        }
    }

    /**
     * This method can be used to get difference of two lists.
     *
     * @param list1 List of objects
     * @param list2 List of object
     * @param <T> Object type
     * @return return list of values which are not in the list2 but in the list1
     */
    private <T> List<T> getDifference(List<T> list1, Collection<T> list2) {
        List<T> list = new ArrayList<>();
        for (T t : list1) {
            if (!list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * By invoking the method, it returns Lifecycle State Instance.
     * @param currentState Current state of the lifecycle
     * @param previousState Previouse state of the Lifecycle
     * @return {@link LifecycleState}
     */
    private LifecycleState getLifecycleStateInstance(String currentState, String previousState) {
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        LifecycleState lifecycleState = new LifecycleState();
        lifecycleState.setCurrentState(currentState);
        lifecycleState.setPreviousState(previousState);
        lifecycleState.setUpdatedBy(userName);
        return lifecycleState;
    }

    @Override
    public ApplicationRelease updateEntAppRelease(String releaseUuid, EntAppReleaseWrapper entAppReleaseWrapper) throws ApplicationManagementException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ApplicationArtifact applicationArtifact = ApplicationManagementUtil.
                    constructApplicationArtifact(entAppReleaseWrapper.getIconLink(), entAppReleaseWrapper.getScreenshotLinks(),
                            entAppReleaseWrapper.getArtifactLink(), entAppReleaseWrapper.getBannerLink());
            ConnectionManagerUtil.beginDBTransaction();
            ApplicationDTO applicationDTO = this.applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            DeviceType deviceTypeObj = APIUtil.getDeviceTypeData(applicationDTO.getDeviceTypeId());
            AtomicReference<ApplicationReleaseDTO> applicationReleaseDTO = new AtomicReference<>(
                    applicationDTO.getApplicationReleaseDTOs().get(0));
            validateAppReleaseUpdating(entAppReleaseWrapper, applicationDTO, applicationArtifact,
                    ApplicationType.ENTERPRISE.toString());
            applicationReleaseDTO.get().setPrice(entAppReleaseWrapper.getPrice());
            applicationReleaseDTO.get().setIsSharedWithAllTenants(applicationReleaseDTO.get().getIsSharedWithAllTenants());
            if (!StringUtils.isEmpty(entAppReleaseWrapper.getSupportedOsVersions())) {
                applicationReleaseDTO.get().setSupportedOsVersions(entAppReleaseWrapper.getSupportedOsVersions());
            }
            if (!StringUtils.isEmpty(entAppReleaseWrapper.getDescription())) {
                applicationReleaseDTO.get().setDescription(entAppReleaseWrapper.getDescription());
            }
            if (!StringUtils.isEmpty(entAppReleaseWrapper.getReleaseType())) {
                applicationReleaseDTO.get().setReleaseType(entAppReleaseWrapper.getReleaseType());
            }
            if (!StringUtils.isEmpty(entAppReleaseWrapper.getMetaData())) {
                applicationReleaseDTO.get().setMetaData(entAppReleaseWrapper.getMetaData());
            }

            //If the application device type is WINDOWS, it is allowed to modify version number and package name.
            if (DeviceTypes.WINDOWS.toString().equalsIgnoreCase(deviceTypeObj.getName())) {
                if (!StringUtils.isEmpty(entAppReleaseWrapper.getVersion())) {
                    applicationReleaseDTO.get().setVersion(entAppReleaseWrapper.getVersion());
                }
                if (!StringUtils.isEmpty(entAppReleaseWrapper.getPackageName())) {
                    applicationReleaseDTO.get().setPackageName(entAppReleaseWrapper.getPackageName());
                }
            }

            if (!StringUtils.isEmpty(applicationArtifact.getInstallerName())
                    && applicationArtifact.getInstallerStream() != null) {
                applicationReleaseDTO
                        .set(updateEntAppReleaseArtifact(deviceTypeObj.getName(), applicationReleaseDTO.get(),
                                applicationArtifact));
            }
            applicationReleaseDTO.set(updateImageArtifacts(applicationReleaseDTO.get(), applicationArtifact, tenantId));
            boolean updateStatus = applicationReleaseDAO.updateRelease(applicationReleaseDTO.get(), tenantId) != null;
            if (!updateStatus) {
                ConnectionManagerUtil.rollbackDBTransaction();
                return null;
            }
            ConnectionManagerUtil.commitDBTransaction();
            return APIUtil.releaseDtoToRelease(applicationReleaseDTO.get());
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to update enterprise app release which "
                    + "has release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating enterprise app release which has release UUID: "
                    + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when updating Ent Application release of UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred when updating application release artifact in the file system. Ent App release "
                    + "UUID:" + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (MalformedURLException e) {
            throw new ApplicationManagementException("Malformed downloadable URL received for the Public app " +
                    "release UUID: " + releaseUuid);
        } catch (FileDownloaderServiceException e) {
            throw new ApplicationManagementException("Error encountered while downloading artifact for the Public app " +
                    "release UUID: " + releaseUuid);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public ApplicationRelease updatePubAppRelease(String releaseUuid, PublicAppReleaseWrapper publicAppReleaseWrapper) throws ApplicationManagementException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ApplicationArtifact applicationArtifact = ApplicationManagementUtil.
                    constructApplicationArtifact(publicAppReleaseWrapper.getIconLink(), publicAppReleaseWrapper.getScreenshotLinks(),
                            null, publicAppReleaseWrapper.getBannerLink());
            ConnectionManagerUtil.beginDBTransaction();
            ApplicationDTO applicationDTO = this.applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            validateAppReleaseUpdating(publicAppReleaseWrapper, applicationDTO, applicationArtifact,
                    ApplicationType.PUBLIC.toString());
            AtomicReference<ApplicationReleaseDTO> applicationReleaseDTO = new AtomicReference<>(
                    applicationDTO.getApplicationReleaseDTOs().get(0));

            applicationReleaseDTO.get().setPrice(publicAppReleaseWrapper.getPrice());
            applicationReleaseDTO.get().setIsSharedWithAllTenants(applicationReleaseDTO.get().getIsSharedWithAllTenants());

            if (!StringUtils.isEmpty(publicAppReleaseWrapper.getPackageName())) {
                applicationReleaseDTO.get().setVersion(publicAppReleaseWrapper.getVersion());
            }
            if (!StringUtils.isEmpty(publicAppReleaseWrapper.getVersion())) {
                applicationReleaseDTO.get().setVersion(publicAppReleaseWrapper.getVersion());
            }
            if (!StringUtils.isEmpty(publicAppReleaseWrapper.getSupportedOsVersions())) {
                applicationReleaseDTO.get().setSupportedOsVersions(publicAppReleaseWrapper.getSupportedOsVersions());
            }
            if (!StringUtils.isEmpty(publicAppReleaseWrapper.getDescription())) {
                applicationReleaseDTO.get().setDescription(publicAppReleaseWrapper.getDescription());
            }
            if (!StringUtils.isEmpty(publicAppReleaseWrapper.getReleaseType())) {
                applicationReleaseDTO.get().setReleaseType(publicAppReleaseWrapper.getReleaseType());
            }
            if (!StringUtils.isEmpty(publicAppReleaseWrapper.getMetaData())) {
                applicationReleaseDTO.get().setMetaData(publicAppReleaseWrapper.getMetaData());
            }

            applicationReleaseDTO.set(updateImageArtifacts(applicationReleaseDTO.get(), applicationArtifact, tenantId));

            boolean updateStatus = applicationReleaseDAO.updateRelease(applicationReleaseDTO.get(), tenantId) != null;
            if (!updateStatus) {
                ConnectionManagerUtil.rollbackDBTransaction();
                return null;
            }
            ConnectionManagerUtil.commitDBTransaction();
            return APIUtil.releaseDtoToRelease(applicationReleaseDTO.get());
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to update public app release which "
                    + "has release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating public app release which has release UUID:."
                    + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when updating public app release of UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred when updating public app release artifact in the file system. Public app "
                    + "release UUID:" + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (MalformedURLException e) {
            throw new ApplicationManagementException("Malformed downloadable URL received for the Public app " +
                    "release UUID: " + releaseUuid);
        } catch (FileDownloaderServiceException e) {
            throw new ApplicationManagementException("Error encountered while downloading artifact for the Public app " +
                    "release UUID: " + releaseUuid);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public ApplicationRelease updateWebAppRelease(String releaseUuid, WebAppReleaseWrapper webAppReleaseWrapper) throws ApplicationManagementException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ApplicationArtifact applicationArtifact = ApplicationManagementUtil.
                    constructApplicationArtifact(webAppReleaseWrapper.getIconLink(), webAppReleaseWrapper.getScreenshotLinks(),
                            null, webAppReleaseWrapper.getBannerLink());
            ConnectionManagerUtil.beginDBTransaction();
            ApplicationDTO applicationDTO = this.applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            validateAppReleaseUpdating(webAppReleaseWrapper, applicationDTO, applicationArtifact,
                    ApplicationType.WEB_CLIP.toString());
            AtomicReference<ApplicationReleaseDTO> applicationReleaseDTO = new AtomicReference<>(
                    applicationDTO.getApplicationReleaseDTOs().get(0));

            applicationReleaseDTO.get().setPrice(webAppReleaseWrapper.getPrice());
            applicationReleaseDTO.get().setIsSharedWithAllTenants(applicationReleaseDTO.get().getIsSharedWithAllTenants());

            if (!StringUtils.isEmpty(webAppReleaseWrapper.getVersion())) {
                applicationReleaseDTO.get().setVersion(webAppReleaseWrapper.getVersion());
            }
            if (!StringUtils.isEmpty(webAppReleaseWrapper.getUrl())) {
                applicationReleaseDTO.get().setInstallerName(webAppReleaseWrapper.getUrl());
            }
            if (!StringUtils.isEmpty(webAppReleaseWrapper.getDescription())) {
                applicationReleaseDTO.get().setDescription(webAppReleaseWrapper.getDescription());
            }
            if (!StringUtils.isEmpty(webAppReleaseWrapper.getReleaseType())) {
                applicationReleaseDTO.get().setReleaseType(webAppReleaseWrapper.getReleaseType());
            }
            if (!StringUtils.isEmpty(webAppReleaseWrapper.getMetaData())) {
                applicationReleaseDTO.get().setMetaData(webAppReleaseWrapper.getMetaData());
            }

            applicationReleaseDTO.set(updateImageArtifacts(applicationReleaseDTO.get(), applicationArtifact, tenantId));
            boolean updateStatus = applicationReleaseDAO.updateRelease(applicationReleaseDTO.get(), tenantId) != null;
            if (!updateStatus) {
                ConnectionManagerUtil.rollbackDBTransaction();
                return null;
            }
            ConnectionManagerUtil.commitDBTransaction();
            return APIUtil.releaseDtoToRelease(applicationReleaseDTO.get());
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to update web app release which "
                    + "has release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating web app release which has release UUID:."
                    + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when updating web app release for web app Release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred when updating web app release artifact in the file system. Web app "
                    + "release UUID:" + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (MalformedURLException e) {
            throw new ApplicationManagementException("Malformed downloadable URL received for the Public app " +
                    "release UUID: " + releaseUuid);
        } catch (FileDownloaderServiceException e) {
            throw new ApplicationManagementException("Error encountered while downloading artifact for the Public app " +
                    "release UUID: " + releaseUuid);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public ApplicationRelease updateCustomAppRelease(String releaseUuid, CustomAppReleaseWrapper customAppReleaseWrapper)
            throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        ApplicationStorageManager applicationStorageManager = APIUtil.getApplicationStorageManager();
        try {
            ApplicationArtifact applicationArtifact = ApplicationManagementUtil.
                    constructApplicationArtifact(customAppReleaseWrapper.getIconLink(),
                            customAppReleaseWrapper.getScreenshotLinks(), customAppReleaseWrapper.getArtifactLink(),
                            customAppReleaseWrapper.getBannerLink());
            ConnectionManagerUtil.beginDBTransaction();
            ApplicationDTO applicationDTO = this.applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            AtomicReference<ApplicationReleaseDTO> applicationReleaseDTO = new AtomicReference<>(
                    applicationDTO.getApplicationReleaseDTOs().get(0));
            validateAppReleaseUpdating(customAppReleaseWrapper, applicationDTO, applicationArtifact,
                    ApplicationType.CUSTOM.toString());
            applicationReleaseDTO.get().setPrice(customAppReleaseWrapper.getPrice());
            applicationReleaseDTO.get()
                    .setIsSharedWithAllTenants(applicationReleaseDTO.get().getIsSharedWithAllTenants());
            if (!StringUtils.isEmpty(customAppReleaseWrapper.getPackageName())) {
                applicationReleaseDTO.get().setVersion(customAppReleaseWrapper.getVersion());
            }
            if (!StringUtils.isEmpty(customAppReleaseWrapper.getVersion())) {
                applicationReleaseDTO.get().setVersion(customAppReleaseWrapper.getVersion());
            }
            if (!StringUtils.isEmpty(customAppReleaseWrapper.getDescription())) {
                applicationReleaseDTO.get().setDescription(customAppReleaseWrapper.getDescription());
            }
            if (!StringUtils.isEmpty(customAppReleaseWrapper.getReleaseType())) {
                applicationReleaseDTO.get().setReleaseType(customAppReleaseWrapper.getReleaseType());
            }
            if (!StringUtils.isEmpty(customAppReleaseWrapper.getMetaData())) {
                applicationReleaseDTO.get().setMetaData(customAppReleaseWrapper.getMetaData());
            }

            if (!StringUtils.isEmpty(applicationArtifact.getInstallerName())
                    && applicationArtifact.getInstallerStream() != null) {
                DeviceType deviceTypeObj = APIUtil.getDeviceTypeData(applicationDTO.getDeviceTypeId());
                // The application executable artifacts such as deb are uploaded.
                try {
                    String md5OfApp = applicationStorageManager.getMD5(
                            Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())));
                    if (md5OfApp == null) {
                        String msg = "Error occurred while retrieving md5sum value from the binary file for "
                                + "application release UUID " + applicationReleaseDTO.get().getUuid();
                        log.error(msg);
                        throw new ApplicationStorageManagementException(msg);
                    }

                    if (!applicationReleaseDTO.get().getAppHashValue().equals(md5OfApp)) {
                        try {
                            ConnectionManagerUtil.getDBConnection();
                            if (this.applicationReleaseDAO.verifyReleaseExistenceByHash(md5OfApp, tenantId)) {
                                String msg =
                                        "Same binary file is in the server. Hence you can't add same file into the "
                                                + "server. Device Type: " + deviceTypeObj.getName()
                                                + " and package name: " + applicationDTO.getApplicationReleaseDTOs()
                                                .get(0).getPackageName();
                                log.error(msg);
                                throw new BadRequestException(msg);
                            }

                            applicationReleaseDTO.get().setInstallerName(applicationArtifact.getInstallerName());
                            String deletingAppHashValue = applicationReleaseDTO.get().getAppHashValue();
                            applicationReleaseDTO.get().setAppHashValue(md5OfApp);
                            applicationStorageManager.
                                    uploadReleaseArtifact(applicationReleaseDTO.get(), deviceTypeObj.getName(),
                                            Files.newInputStream(Paths.get(applicationArtifact.getInstallerPath())), tenantId);
                            applicationStorageManager.copyImageArtifactsAndDeleteInstaller(deletingAppHashValue,
                                    applicationReleaseDTO.get(), tenantId);
                        } catch (DBConnectionException e) {
                            String msg = "Error occurred when getting database connection for verifying application"
                                    + " release existing for new app hash value.";
                            log.error(msg, e);
                            throw new ApplicationManagementException(msg, e);
                        } catch (ApplicationManagementDAOException e) {
                            String msg =
                                    "Error occurred when executing the query for verifying application release "
                                            + "existence for the new app hash value.";
                            log.error(msg, e);
                            throw new ApplicationManagementException(msg, e);
                        } finally {
                            ConnectionManagerUtil.closeDBConnection();
                        }
                    }
                } catch (StorageManagementException e) {
                    String msg = "Error occurred while retrieving md5sum value from the binary file for "
                            + "application release UUID " + applicationReleaseDTO.get().getUuid();
                    log.error(msg, e);
                    throw new ApplicationStorageManagementException(msg, e);
                } catch (IOException e) {
                    String msg = "Error occurred when getting byte array of binary file. Installer name: "
                            + applicationArtifact.getInstallerName();
                    log.error(msg, e);
                    throw new ApplicationStorageManagementException(msg, e);
                }
            }
            applicationReleaseDTO.set(updateImageArtifacts(applicationReleaseDTO.get(), applicationArtifact, tenantId));
            boolean updateStatus = applicationReleaseDAO.updateRelease(applicationReleaseDTO.get(), tenantId) != null;
            if (!updateStatus) {
                ConnectionManagerUtil.rollbackDBTransaction();
                return null;
            }
            ConnectionManagerUtil.commitDBTransaction();
            return APIUtil.releaseDtoToRelease(applicationReleaseDTO.get());
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting the database connection to update enterprise app release which "
                    + "has release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Database access error is occurred when updating enterprise app release which has release "
                    + "UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred when updating Ent Application release of UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ResourceManagementException e) {
            String msg = "Error occurred when updating application release artifact in the file system. Ent App release "
                    + "UUID:" + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (MalformedURLException e) {
            throw new ApplicationManagementException("Malformed downloadable URL received for the Public app " +
                    "release UUID: " + releaseUuid);
        } catch (FileDownloaderServiceException e) {
            throw new ApplicationManagementException("Error encountered while downloading artifact for the Public app " +
                    "release UUID: " + releaseUuid);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    /**
     * To validate app release updating request
     *
     * @param param either {@link EntAppReleaseWrapper} or {@link PublicAppReleaseWrapper}
     *              or {@link WebAppReleaseWrapper} or {@link CustomAppReleaseWrapper}
     * @param applicationDTO Existing application {@link ApplicationDTO}
     * @param applicationArtifact Application release artifacts which contains either icon or screenshots or artifact
     * @param appType Application Type
     * @throws ApplicationManagementException if invalid application release updating payload received.
     */
    private <T> void validateAppReleaseUpdating(T param, ApplicationDTO applicationDTO,
                                                ApplicationArtifact applicationArtifact, String appType)
            throws ApplicationManagementException {
        if (applicationDTO == null) {
            String msg = "Couldn't found an application for requested UUID.";
            log.error(msg);
            throw new NotFoundException(msg);
        }
        if (!appType.equals(applicationDTO.getType())) {
            String msg = "You trying to perform " + appType + " app release update on " + applicationDTO.getType()
                    + " app release.";
            log.error(msg);
            throw new ForbiddenException(msg);
        }

        boolean requireToCheckUpdatability = false;
        double price = 0.0;
        String supportedOsVersions = null;

        if (param instanceof EntAppReleaseWrapper) {
            EntAppReleaseWrapper entAppReleaseWrapper = (EntAppReleaseWrapper) param;
            if (!StringUtils.isEmpty(applicationArtifact.getInstallerName())
                    && applicationArtifact.getInstallerStream() != null) {
                requireToCheckUpdatability = true;
            }
            price = entAppReleaseWrapper.getPrice();
            supportedOsVersions = entAppReleaseWrapper.getSupportedOsVersions();
        } else if (param instanceof PublicAppReleaseWrapper) {
            PublicAppReleaseWrapper publicAppReleaseWrapper = (PublicAppReleaseWrapper) param;
            if (!StringUtils.isBlank(publicAppReleaseWrapper.getPackageName())) {
                requireToCheckUpdatability = true;
            }
            price = publicAppReleaseWrapper.getPrice();
            supportedOsVersions = publicAppReleaseWrapper.getSupportedOsVersions();
        } else if (param instanceof WebAppReleaseWrapper) {
            WebAppReleaseWrapper webAppReleaseWrapper = (WebAppReleaseWrapper) param;
            if (!StringUtils.isBlank(webAppReleaseWrapper.getUrl())) {
                requireToCheckUpdatability = true;
            }
        } else if (param instanceof CustomAppReleaseWrapper && !StringUtils
                .isEmpty(applicationArtifact.getInstallerName()) && applicationArtifact.getInstallerStream() != null) {
            requireToCheckUpdatability = true;
        }

        ApplicationReleaseDTO applicationReleaseDTO = applicationDTO.getApplicationReleaseDTOs().get(0);
        if (requireToCheckUpdatability && !lifecycleStateManager
                .isUpdatableState(applicationReleaseDTO.getCurrentState())) {
            String msg = "Application release in " + applicationReleaseDTO.getCurrentState()
                    + " state. Therefore you are not allowed to update the application release. Hence, "
                    + "please move application release from " + applicationReleaseDTO.getCurrentState()
                    + " to updatable state.";
            log.error(msg);
            throw new ForbiddenException(msg);
        }

        String applicationSubType = applicationDTO.getSubType();
        if (price < 0.0 || (price == 0.0 && ApplicationSubscriptionType.PAID.toString().equals(applicationSubType)) || (
                price > 0.0 && ApplicationSubscriptionType.FREE.toString().equals(applicationSubType))) {
            String msg =
                    "Invalid app release payload for updating application release. Application " + "price is " + price
                            + " for " + applicationSubType + " application.";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (ApplicationType.ENTERPRISE.toString().equals(appType) || ApplicationType.PUBLIC.toString()
                .equals(appType)) {
            DeviceType deviceTypeObj = APIUtil.getDeviceTypeData(applicationDTO.getDeviceTypeId());
            if (!StringUtils.isEmpty(supportedOsVersions) && isInvalidOsVersionRange(supportedOsVersions,
                    deviceTypeObj.getName())) {
                String msg = "You are trying to update application release which has invalid or unsupported OS "
                        + "versions in the supportedOsVersions section. Hence, please re-evaluate the request payload.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        }
    }

    @Override
    public <T> void validateAppCreatingRequest(T param)
            throws ApplicationManagementException, RequestValidatingException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        int deviceTypeId = -1;
        String appName;
        int appNameLength = 20;
        List<String> appCategories;
        List<String> unrestrictedRoles;

        if (param instanceof ApplicationWrapper) {
            ApplicationWrapper applicationWrapper = (ApplicationWrapper) param;
            appName = applicationWrapper.getName();
            if (StringUtils.isEmpty(appName)) {
                String msg = "Application name cannot be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appName.length() > appNameLength) {
                String msg = "Application name must be less than or equal to 20 characters in length.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            appCategories = applicationWrapper.getCategories();
            if (appCategories == null) {
                String msg = "Application category can't be null.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appCategories.isEmpty()) {
                String msg = "Application category can't be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(applicationWrapper.getDeviceType())) {
                String msg = "Device type can't be empty for the application.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            DeviceType deviceType = APIUtil.getDeviceTypeData(applicationWrapper.getDeviceType());
            deviceTypeId = deviceType.getId();
            unrestrictedRoles = applicationWrapper.getUnrestrictedRoles();
            List<EntAppReleaseWrapper> releaseWrappers = applicationWrapper.getEntAppReleaseWrappers();
            if (!releaseWrappers.isEmpty()) {
                EntAppReleaseWrapper releaseWrapper = releaseWrappers.get(0);
                validateEntAppReleaseCreatingRequest(releaseWrapper, applicationWrapper.getDeviceType());
            }
        } else if (param instanceof WebAppWrapper) {
            WebAppWrapper webAppWrapper = (WebAppWrapper) param;
            appName = webAppWrapper.getName();
            if (StringUtils.isEmpty(appName)) {
                String msg = "Web Clip name cannot be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appName.length() > appNameLength) {
                String msg = "Application name must be less than or equal to 20 characters in length.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            appCategories = webAppWrapper.getCategories();
            if (appCategories == null) {
                String msg = "Web Clip category can't be null.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appCategories.isEmpty()) {
                String msg = "Web clip category can't be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(webAppWrapper.getType()) || (!ApplicationType.WEB_CLIP.toString()
                    .equals(webAppWrapper.getType()) && !ApplicationType.WEB_APP.toString()
                    .equals(webAppWrapper.getType()))) {
                String msg = "Web app wrapper contains invalid application type with the request. Hence please verify "
                        + "the request payload..";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            unrestrictedRoles = webAppWrapper.getUnrestrictedRoles();
            List<WebAppReleaseWrapper> releaseWrappers = webAppWrapper.getWebAppReleaseWrappers();
            if(!releaseWrappers.isEmpty()) {
                WebAppReleaseWrapper releaseWrapper = releaseWrappers.get(0);
                validateWebAppReleaseCreatingRequest(releaseWrapper);
            }
        } else if (param instanceof PublicAppWrapper) {
            PublicAppWrapper publicAppWrapper = (PublicAppWrapper) param;
            appName = publicAppWrapper.getName();
            if (StringUtils.isEmpty(appName)) {
                String msg = "Application name cannot be empty for public app.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appName.length() > appNameLength) {
                String msg = "Application name must be less than or equal to 20 characters in length.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            appCategories = publicAppWrapper.getCategories();
            if (appCategories == null) {
                String msg = "Application category can't be null.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appCategories.isEmpty()) {
                String msg = "Application category can't be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(publicAppWrapper.getDeviceType())) {
                String msg = "Device type can't be empty for the public application.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            DeviceType deviceType = APIUtil.getDeviceTypeData(publicAppWrapper.getDeviceType());
            deviceTypeId = deviceType.getId();
            unrestrictedRoles = publicAppWrapper.getUnrestrictedRoles();
            List<PublicAppReleaseWrapper> releaseWrappers = publicAppWrapper.getPublicAppReleaseWrappers();
            if(!releaseWrappers.isEmpty()) {
                PublicAppReleaseWrapper releaseWrapper = releaseWrappers.get(0);
                validatePublicAppReleaseCreatingRequest(releaseWrapper, publicAppWrapper.getDeviceType());
            }
        } else if (param instanceof CustomAppWrapper) {
            CustomAppWrapper customAppWrapper = (CustomAppWrapper) param;
            appName = customAppWrapper.getName();
            if (StringUtils.isEmpty(appName)) {
                String msg = "Application name cannot be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appName.length() > appNameLength) {
                String msg = "Application name must be less than or equal to 20 characters in length.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            appCategories = customAppWrapper.getCategories();
            if (appCategories == null) {
                String msg = "Application category can't be null.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (appCategories.isEmpty()) {
                String msg = "Application category can't be empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(customAppWrapper.getDeviceType())) {
                String msg = "Device type can't be empty for the application.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            DeviceType deviceType = APIUtil.getDeviceTypeData(customAppWrapper.getDeviceType());
            deviceTypeId = deviceType.getId();
            unrestrictedRoles = customAppWrapper.getUnrestrictedRoles();
            List<CustomAppReleaseWrapper> releaseWrappers = customAppWrapper.getCustomAppReleaseWrappers();
            if(!releaseWrappers.isEmpty()) {
                CustomAppReleaseWrapper releaseWrapper = releaseWrappers.get(0);
                validateCustomAppReleaseCreatingRequest(releaseWrapper, customAppWrapper.getDeviceType());
            }
        } else {
            String msg = "Invalid payload found with the request. Hence verify the request payload object.";
            log.error(msg);
            throw new ApplicationManagementException(msg);
        }

        try {
            ConnectionManagerUtil.openDBConnection();
            if (unrestrictedRoles != null && !unrestrictedRoles.isEmpty()) {
                if (!isValidRestrictedRole(unrestrictedRoles)) {
                    String msg = "Unrestricted role list contain role/roles which are not in the user store.";
                    log.error(msg);
                    throw new ApplicationManagementException(msg);
                }
                if (!isUserAbleToViewAllRoles()) {
                    if (!hasUserRole(unrestrictedRoles, userName)) {
                        String msg = "You are trying to restrict the visibility of the application for a role set, but "
                                + "in order to perform the action at least one role should be assigned to user: "
                                + userName;
                        log.error(msg);
                        throw new BadRequestException(msg);
                    }
                }
            }

            Filter filter = new Filter();
            filter.setFullMatch(true);
            filter.setAppName(appName);
            filter.setOffset(0);
            filter.setLimit(1);
            List<ApplicationDTO> applicationList = applicationDAO.getApplications(filter, deviceTypeId, tenantId);
            if (!applicationList.isEmpty()) {
                String msg =
                        "Already an application registered with same name - " + applicationList.get(0).getName() + ".";
                log.error(msg);
                throw new BadRequestException(msg);
            }

            List<CategoryDTO> registeredCategories = this.applicationDAO.getAllCategories(tenantId);

            if (registeredCategories.isEmpty()) {
                String msg = "Registered application category set is empty. Since it is mandatory to add application "
                        + "category when adding new application, registered application category list shouldn't be null.";
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
            for (String cat : appCategories) {
                boolean isValidCategory = false;
                for (CategoryDTO obj : registeredCategories) {
                    if (cat.equals(obj.getCategoryName())) {
                        isValidCategory = true;
                        break;
                    }
                }
                if (!isValidCategory) {
                    String msg = "Application Creating request contains invalid categories. Hence please verify the "
                            + "application creating payload.";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
            }
        } catch (DBConnectionException e) {
            String msg = "Error occurred while getting database connection.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg =
                    "Error occurred while getting data which is related to web clip. web clip name: " + appName + ".";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (UserStoreException e) {
            String msg = "Error occurred when validating the unrestricted roles given for the web clip";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (MetadataManagementException e) {
            String msg = "Error occurred while retrieving metadata list";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public <T> void validateReleaseCreatingRequest(T param, String deviceType) throws ApplicationManagementException {
        if (param == null) {
            String msg = "In order to validate release creating request param shouldn't be null.";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (param instanceof EntAppReleaseWrapper) {
            AtomicReference<EntAppReleaseWrapper> entAppReleaseWrapper = new AtomicReference<>(
                    (EntAppReleaseWrapper) param);
            if (StringUtils.isEmpty(entAppReleaseWrapper.get().getSupportedOsVersions())) {
                String msg = "Supported OS Version shouldn't be null or empty.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (isInvalidOsVersionRange(entAppReleaseWrapper.get().getSupportedOsVersions(), deviceType)) {
                String msg = "You are trying to create application which has an application release contains invalid or "
                        + "unsupported OS versions in the supportedOsVersions section. Hence, please re-evaluate the "
                        + "request payload.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            //Validating the version number and the packageName of the Windows new applications releases
            if (DeviceTypes.WINDOWS.toString().equalsIgnoreCase(deviceType)) {
                if (entAppReleaseWrapper.get().getVersion() == null || entAppReleaseWrapper.get().getPackageName() == null) {
                    String msg = "Application Version number or/and PackageName..both are required only when the app type is " +
                            deviceType + " platform type";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
            }
        } else if (param instanceof WebAppReleaseWrapper) {
            WebAppReleaseWrapper webAppReleaseWrapper = (WebAppReleaseWrapper) param;
            UrlValidator urlValidator = new UrlValidator();
            if (StringUtils.isEmpty(webAppReleaseWrapper.getVersion())) {
                String msg = "Version shouldn't be empty or null for the WEB CLIP release creating request.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(webAppReleaseWrapper.getUrl())) {
                String msg = "URL shouldn't be null for the application release creating request for application type "
                        + "WEB_CLIP";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (!urlValidator.isValid(webAppReleaseWrapper.getUrl())) {
                String msg = "Request payload contains an invalid Web Clip URL.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } else if (param instanceof PublicAppReleaseWrapper) {
            PublicAppReleaseWrapper publicAppReleaseWrapper = (PublicAppReleaseWrapper) param;
            if (StringUtils.isEmpty(publicAppReleaseWrapper.getSupportedOsVersions())) {
                String msg = "Supported OS Version shouldn't be null or empty for public app release creating request.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(publicAppReleaseWrapper.getVersion())) {
                String msg = "Version shouldn't be empty or null for the Public App release creating request.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(publicAppReleaseWrapper.getPackageName())) {
                String msg = "Package name shouldn't be empty or null for the Public App release creating request.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (isInvalidOsVersionRange(publicAppReleaseWrapper.getSupportedOsVersions(), deviceType)) {
                String msg = "You are trying to create application which has an application release contains invalid or "
                        + "unsupported OS versions in the supportedOsVersions section. Hence, please re-evaluate the "
                        + "request payload.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } else if (param instanceof CustomAppReleaseWrapper) {
            CustomAppReleaseWrapper customAppReleaseWrapper = (CustomAppReleaseWrapper) param;
            if (StringUtils.isEmpty(customAppReleaseWrapper.getVersion())) {
                String msg = "Version shouldn't be empty or null for the custom App release creating request.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (StringUtils.isEmpty(customAppReleaseWrapper.getPackageName())) {
                String msg = "Package name shouldn't be empty or null for the custom App release creating request.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } else {
            String msg = "Invalid payload found with the release creating request. Hence verify the release creating "
                    + "request payload object.";
            log.error(msg);
            throw new ApplicationManagementException(msg);
        }
    }

    @Override
    public void validateEntAppReleaseCreatingRequest(EntAppReleaseWrapper releaseWrapper, String deviceType)
            throws RequestValidatingException, ApplicationManagementException {
        validateReleaseCreatingRequest(releaseWrapper, deviceType);
    }

    @Override
    public void validateCustomAppReleaseCreatingRequest(CustomAppReleaseWrapper releaseWrapper, String deviceType)
            throws RequestValidatingException, ApplicationManagementException {
        validateReleaseCreatingRequest(releaseWrapper, deviceType);
    }

    @Override
    public void validateWebAppReleaseCreatingRequest(WebAppReleaseWrapper releaseWrapper)
            throws RequestValidatingException, ApplicationManagementException {
        validateReleaseCreatingRequest(releaseWrapper, Constants.ANY);
    }

    @Override
    public void validatePublicAppReleaseCreatingRequest(PublicAppReleaseWrapper releaseWrapper, String deviceType)
            throws RequestValidatingException, ApplicationManagementException {
        validateReleaseCreatingRequest(releaseWrapper, deviceType);
        validatePublicAppReleasePackageName(releaseWrapper.getPackageName());
    }

    @Override
    public void validateImageArtifacts(Base64File iconFile, List<Base64File> screenshots)
            throws RequestValidatingException {
        if (iconFile == null) {
            String msg = "Icon file is not found with the application release creating request.";
            log.error(msg);
            throw new RequestValidatingException(msg);
        }
        if (screenshots == null || screenshots.isEmpty()) {
            String msg = "Screenshots are not found with the application release creating request.";
            log.error(msg);
            throw new RequestValidatingException(msg);
        }
    }

    @Override
    public void validateBase64File(Base64File file) throws RequestValidatingException {
        if (StringUtils.isEmpty(file.getBase64String())) {
            throw new RequestValidatingException("Base64File in the payload doesn't contain base64 string");
        }
        if (StringUtils.isEmpty(file.getName())) {
            throw new RequestValidatingException("Base64File in the payload doesn't contain file name");
        }
    }

    @Override
    public void validateImageArtifacts(Attachment iconFile, Attachment bannerFile,
                                       List<Attachment> attachmentList) throws RequestValidatingException {
        if (iconFile == null) {
            String msg = "Icon file is not found with the application release creating request.";
            log.error(msg);
            throw new RequestValidatingException(msg);
        }
        if (attachmentList == null || attachmentList.isEmpty()) {
            String msg = "Screenshots are not found with the application release creating request.";
            log.error(msg);
            throw new RequestValidatingException(msg);
        }
    }

    @Override
    public void validateBinaryArtifact(Base64File binaryFile) throws RequestValidatingException {
        if (binaryFile == null) {
            String msg = "Binary file is not found with the application release creating request for ENTERPRISE app "
                    + "creating request.";
            log.error(msg);
            throw new RequestValidatingException(msg);
        }
    }

    @Override
    public void validateBinaryArtifact(Attachment binaryFile) throws RequestValidatingException {
        if (binaryFile == null) {
            String msg = "Binary file is not found with the application release creating request for ENTERPRISE app "
                    + "creating request.";
            log.error(msg);
            throw new RequestValidatingException(msg);
        }
    }

    @Override
    public boolean checkSubDeviceIdsForOperations(int operationId, int deviceId) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            ConnectionManagerUtil.openDBConnection();
            List<Integer> deviceSubIds = subscriptionDAO.getDeviceSubIdsForOperation(operationId, deviceId, tenantId);
            if (deviceSubIds.isEmpty()) {
                return false;
            }
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting the device sub ids for the operations";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
        return true;
    }

    @Override
    public void updateSubStatus(int deviceId, List<Integer> operationIds, String status) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            ConnectionManagerUtil.beginDBTransaction();
            for (int operationId : operationIds) {
                List<Integer> deviceSubIds = subscriptionDAO.getDeviceSubIdsForOperation(operationId, deviceId, tenantId);
                if (!subscriptionDAO.updateDeviceSubStatus(deviceId, deviceSubIds, status, tenantId)){
                    ConnectionManagerUtil.rollbackDBTransaction();
                    String msg = "Didn't update an any app subscription of device for operation Id: " + operationId;
                    log.error(msg);
                    throw new ApplicationManagementException(msg);
                }
            }
            ConnectionManagerUtil.commitDBTransaction();
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while updating app subscription status of the device.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to update aoo subscription status of "
                    + "device.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while executing database transaction";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void updateSubsStatus(int deviceId, int operationId, String status) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            ConnectionManagerUtil.beginDBTransaction();
            List<Integer> deviceSubIds = subscriptionDAO.getDeviceSubIdsForOperation(operationId, deviceId, tenantId);
            if (!subscriptionDAO.updateDeviceSubStatus(deviceId, deviceSubIds, status, tenantId)){
                ConnectionManagerUtil.rollbackDBTransaction();
                String msg = "Didn't update an any app subscription of device for operation Id: " + operationId;
                log.error(msg);
                throw new ApplicationManagementException(msg);
            }
            ConnectionManagerUtil.commitDBTransaction();
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while updating app subscription status of the device.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to update aoo subscription status of "
                    + "device.";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (TransactionManagementException e) {
            String msg = "Error occurred while executing database transaction";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public String getPlistArtifact(String releaseUuid) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            ApplicationDTO applicationDTO = this.applicationDAO.getAppWithRelatedRelease(releaseUuid, tenantId);
            if (applicationDTO == null) {
                String msg = "Couldn't find application for the release UUID: " + releaseUuid;
                log.error(msg);
                throw new NotFoundException(msg);
            }
            ApplicationReleaseDTO applicationReleaseDTO = applicationDTO.getApplicationReleaseDTOs().get(0);

            String artifactDownloadURL = APIUtil.getArtifactDownloadBaseURL()
                    + tenantId + Constants.FORWARD_SLASH + applicationReleaseDTO.getUuid()
                    + Constants.FORWARD_SLASH + Constants.APP_ARTIFACT + Constants.FORWARD_SLASH +
                    applicationReleaseDTO.getInstallerName();
            String plistContent = "&lt;!DOCTYPE plist PUBLIC &quot;-//Apple//DTDPLIST1.0//EN&quot; &quot;" +
                    "http://www.apple.com/DTDs/PropertyList-1.0.dtd&quot;&gt;&lt;plist version=&quot;" +
                    "1.0&quot;&gt;&lt;dict&gt;&lt;key&gt;items&lt;/key&gt;&lt;array&gt;&lt;dict&gt;&lt;" +
                    "key&gt;assets&lt;/key&gt;&lt;array&gt;&lt;dict&gt;&lt;key&gt;kind&lt;/key&gt;&lt;" +
                    "string&gt;software-package&lt;/string&gt;&lt;key&gt;url&lt;/key&gt;&lt;string&gt;" +
                    "$downloadURL&lt;/string&gt;&lt;/dict&gt;&lt;/array&gt;&lt;key&gt;metadata&lt;" +
                    "/key&gt;&lt;dict&gt;&lt;key&gt;bundle-identifier&lt;/key&gt;&lt;string&gt;" +
                    "$packageName&lt;/string&gt;&lt;key&gt;bundle-version&lt;/key&gt;&lt;string&gt;" +
                    "$bundleVersion&lt;/string&gt;&lt;key&gt;kind&lt;/key&gt;&lt;string&gt;" +
                    "software&lt;/string&gt;&lt;key&gt;title&lt;/key&gt;&lt;string&gt;$appName&lt;" +
                    "/string&gt;&lt;/dict&gt;&lt;/dict&gt;&lt;/array&gt;&lt;/dict&gt;&lt;/plist&gt;";
            plistContent = plistContent.replace("$downloadURL", artifactDownloadURL)
                    .replace("$packageName", applicationReleaseDTO.getPackageName())
                    .replace("$bundleVersion", applicationReleaseDTO.getVersion())
                    .replace("$appName", applicationDTO.getName());
            return StringEscapeUtils.unescapeXml(plistContent);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection for getting application for the release UUID: "
                    + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting application data for release UUID: " + releaseUuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public List<ApplicationReleaseDTO> getReleaseByPackageNames(List<String> packageIds) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
        try {
            ConnectionManagerUtil.openDBConnection();
            return this.applicationReleaseDAO.getReleaseByPackages(packageIds, tenantId);
        } catch (DBConnectionException e) {
            String msg = "Error occurred while obtaining the database connection for getting application for the " +
                    "packages";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting application data for packages";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void updateAppIconInfo(ApplicationRelease applicationRelease, String oldPackageName) throws ApplicationManagementException {
        try {
            DataHolder.getInstance().getDeviceManagementService().updateApplicationIcon(applicationRelease.getIconPath(),
                    oldPackageName, applicationRelease.getPackageName(), applicationRelease.getVersion());
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while updating application icon info. Application package name: " + oldPackageName;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * Delete application icon information when deleting an application
     *
     * @param applicationDTO {@link ApplicationDTO}
     * @throws ApplicationManagementException if error occurred while deleting application icon information
     */
    private void deleteAppIconInfo(ApplicationDTO applicationDTO) throws ApplicationManagementException {
        try {
            DataHolder.getInstance().getDeviceManagementService().deleteApplicationIcon(applicationDTO.getPackageName());
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while deleting application icon info. Application package name: " + applicationDTO.getPackageName();
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public void deleteApplicationDataOfTenant(int tenantId) throws ApplicationManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Request is received to delete application related data of tenant with ID: " + tenantId);
        }
        try {
            ConnectionManagerUtil.beginDBTransaction();

            vppApplicationDAO.deleteAssociationByTenant(tenantId);
            vppApplicationDAO.deleteVppUserByTenant(tenantId);
            vppApplicationDAO.deleteAssetsByTenant(tenantId);
            reviewDAO.deleteReviewsByTenant(tenantId);
            subscriptionDAO.deleteOperationMappingByTenant(tenantId);
            subscriptionDAO.deleteDeviceSubscriptionByTenant(tenantId);
            subscriptionDAO.deleteGroupSubscriptionByTenant(tenantId);
            subscriptionDAO.deleteRoleSubscriptionByTenant(tenantId);
            subscriptionDAO.deleteUserSubscriptionByTenant(tenantId);
            applicationDAO.deleteAppFavouritesByTenant(tenantId);
            applicationDAO.deleteApplicationTagsMappingByTenant(tenantId);
            applicationDAO.deleteApplicationTagsByTenant(tenantId);
            applicationDAO.deleteApplicationCategoryMappingByTenant(tenantId);
            applicationDAO.deleteApplicationCategoriesByTenant(tenantId);
            subscriptionDAO.deleteScheduledSubscriptionByTenant(tenantId);
            lifecycleStateDAO.deleteAppLifecycleStatesByTenant(tenantId);
            applicationReleaseDAO.deleteReleasesByTenant(tenantId);
            visibilityDAO.deleteAppUnrestrictedRolesByTenant(tenantId);
            spApplicationDAO.deleteSPApplicationMappingByTenant(tenantId);
            spApplicationDAO.deleteIdentityServerByTenant(tenantId);
            applicationDAO.deleteApplicationsByTenant(tenantId);

            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to delete applications for tenant with ID: "
                    + tenantId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Database access error is occurred when getting applications for tenant with ID: " + tenantId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifeCycleManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while deleting life-cycle state data of application releases of the tenant"
                    + " of ID: " + tenantId ;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ReviewManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while deleting reviews of application releases of the applications"
                    + " of tenant ID: " + tenantId ;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }

    @Override
    public void deleteApplicationDataByTenantId(int tenantId) throws ApplicationManagementException {
        try {
            ConnectionManagerUtil.beginDBTransaction();
            vppApplicationDAO.deleteAssociationByTenant(tenantId);
            vppApplicationDAO.deleteVppUserByTenant(tenantId);
            vppApplicationDAO.deleteAssetsByTenant(tenantId);
            reviewDAO.deleteReviewsByTenant(tenantId);
            subscriptionDAO.deleteOperationMappingByTenant(tenantId);
            subscriptionDAO.deleteDeviceSubscriptionByTenant(tenantId);
            subscriptionDAO.deleteGroupSubscriptionByTenant(tenantId);
            subscriptionDAO.deleteRoleSubscriptionByTenant(tenantId);
            subscriptionDAO.deleteUserSubscriptionByTenant(tenantId);
            applicationDAO.deleteAppFavouritesByTenant(tenantId);
            applicationDAO.deleteApplicationTagsMappingByTenant(tenantId);
            applicationDAO.deleteApplicationTagsByTenant(tenantId);
            applicationDAO.deleteApplicationCategoryMappingByTenant(tenantId);
            applicationDAO.deleteApplicationCategoriesByTenant(tenantId);
            subscriptionDAO.deleteScheduledSubscriptionByTenant(tenantId);
            lifecycleStateDAO.deleteAppLifecycleStatesByTenant(tenantId);
            applicationReleaseDAO.deleteReleasesByTenant(tenantId);
            visibilityDAO.deleteAppUnrestrictedRolesByTenant(tenantId);
            spApplicationDAO.deleteSPApplicationMappingByTenant(tenantId);
            spApplicationDAO.deleteIdentityServerByTenant(tenantId);
            applicationDAO.deleteApplicationsByTenant(tenantId);

            ConnectionManagerUtil.commitDBTransaction();
        } catch (DBConnectionException e) {
            String msg = "Error occurred while observing the database connection to delete applications for tenant with " +
                    "tenant ID: " + tenantId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ApplicationManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Database access error is occurred when getting applications for tenant with tenant Id: "
                    + tenantId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (LifeCycleManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while deleting life-cycle state data of application releases of the tenant"
                    + " of id: " + tenantId ;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (ReviewManagementDAOException e) {
            ConnectionManagerUtil.rollbackDBTransaction();
            String msg = "Error occurred while deleting reviews of application releases of the applications"
                    + " of tenant of id: " + tenantId ;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    @Override
    public void deleteApplicationArtifactsByTenantId(int tenantId) throws ApplicationManagementException {
        try {
            DataHolder.getInstance().getApplicationStorageManager().deleteAppFolderOfTenant(tenantId);
        } catch (ApplicationStorageManagementException e) {
            String msg = "Error deleting app artifacts of tenant of Id: " + tenantId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    /**
     * Retrieve {@link ReleaseVersionInfo} for a given package name
     * @param uuid UUID of the application release
     * @return List of {@link ReleaseVersionInfo}
     * @throws ApplicationManagementException throws when error encountered while retrieving data
     */
    @Override
    public List<ReleaseVersionInfo> getApplicationReleaseVersions(String uuid) throws ApplicationManagementException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            ConnectionManagerUtil.openDBConnection();
            return applicationDAO.getApplicationReleaseVersions(uuid, tenantId);
        } catch (ApplicationManagementDAOException e) {
            String msg = "Error occurred while getting available application releases for uuid : " + uuid;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } finally {
            ConnectionManagerUtil.closeDBConnection();
        }
    }
}
