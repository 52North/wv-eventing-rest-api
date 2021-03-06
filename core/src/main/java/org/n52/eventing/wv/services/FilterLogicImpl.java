/*
 * Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.eventing.wv.services;

import java.util.Map;
import java.util.Optional;
import org.hibernate.Session;
import org.n52.eventing.rest.parameters.ParameterInstance;
import org.n52.eventing.rest.subscriptions.FilterLogic;
import org.n52.eventing.rest.subscriptions.InvalidSubscriptionException;
import org.n52.eventing.rest.model.Subscription;
import org.n52.eventing.rest.model.TemplateDefinition;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.SubscriptionDao;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import org.n52.eventing.wv.dao.hibernate.HibernateNotificationDao;
import org.n52.eventing.wv.dao.hibernate.HibernateSubscriptionDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.i18n.I18nProvider;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.Notification;
import org.n52.eventing.wv.model.WvSubscription;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.security.GroupPolicies;
import org.n52.eventing.wv.security.UserSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class FilterLogicImpl extends BaseService implements FilterLogic {

    private static final Logger LOG = LoggerFactory.getLogger(FilterLogicImpl.class);

    @Autowired
    private I18nProvider i18n;

    @Autowired
    HibernateDatabaseConnection hibernateConnection;

    @Autowired
    UserSecurityService userSecurityService;

    @Autowired
    AccessRights accessRights;

    @Autowired
    GroupPolicies groupPolicies;

    @Override
    public String internalSubscribe(Subscription s, TemplateDefinition template) throws InvalidSubscriptionException {
        WvUser user;
        try {
            user = super.resolveUser(accessRights);
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new InvalidSubscriptionException("User is not authenticated");
        }

        try (Session session = hibernateConnection.createSession()) {
            Map<String, ParameterInstance> params = s.getNotificationInstance().getParameters();

            WvUser subUser = null;
            if (params != null && params.containsKey(WvSubscriptionTemplateFactory.USER_PARAMETER)) {
                Integer intVal = (Integer) params.get(WvSubscriptionTemplateFactory.USER_PARAMETER).getValue();
                if (intVal != null) {
                    Optional<WvUser> targetUser = new HibernateUserDao(session, groupPolicies).retrieveById(intVal);

                    if (!targetUser.isPresent() || !accessRights.canManageSubscriptionsForUser(user, targetUser.get())) {
                        throw new InvalidSubscriptionException("Not allowed to set the subscription for the targeted user");
                    }

                    subUser = targetUser.get();
                }
            }

            Group subGroup = null;
            if (params != null && params.containsKey(WvSubscriptionTemplateFactory.GROUP_PARAMETER)) {
                Integer intVal = (Integer) params.get(WvSubscriptionTemplateFactory.GROUP_PARAMETER).getValue();
                if (intVal != null) {
                    Optional<Group> targetGroup = new HibernateGroupDao(session, groupPolicies).retrieveById(intVal);

                    if (!targetGroup.isPresent() || !accessRights.canManageSubscriptionsForGroup(user, targetGroup.get())) {
                        throw new InvalidSubscriptionException("Not allowed to set the subscription for the targeted group or group not available");
                    }

                    subGroup = targetGroup.get();
                }
            }

            if (subUser == null && subGroup == null) {
                //use the logged in user for this subscription
                subUser = user;
            }

            int templateId = super.parseId(template.getId());

            HibernateNotificationDao notifDao = new HibernateNotificationDao(session);
            Optional<Notification> notif = notifDao.retrieveById(templateId);

            if (!notif.isPresent()) {
                throw new InvalidSubscriptionException("The target rule is not available: "+templateId);
            }

            WvSubscription subscription = new WvSubscription(notif.get());
            subscription.setGroup(subGroup);
            subscription.setUser(subUser);

            SubscriptionDao subDao = new HibernateSubscriptionDao(session);

            if (subDao.hasEntity(subscription)) {
                throw new InvalidSubscriptionException(i18n.getString("subscription.alreadyPresent"));
            }

            subDao.store(subscription);

            return Integer.toString(subscription.getId());
        } catch (DatabaseException ex) {
            LOG.warn(ex.getMessage());
            LOG.debug(ex.getMessage(), ex);
            throw new InvalidSubscriptionException("Could not store subscription", ex);
        }
    }

    @Override
    public void remove(String id) {
        LOG.debug("No operation required, everything will be done by "+SubscriptionsServiceImpl.class.getSimpleName());
    }

}
