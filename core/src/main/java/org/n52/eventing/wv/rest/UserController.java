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
package org.n52.eventing.wv.rest;

import com.fasterxml.jackson.annotation.JsonView;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.n52.eventing.rest.InvalidPaginationException;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.PaginationFactory;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.ResourceCollectionWithMetadata;
import org.n52.eventing.rest.UrlSettings;
import org.n52.eventing.rest.binding.BaseController;
import org.n52.eventing.rest.binding.exception.ResourceNotFoundException;
import org.n52.eventing.rest.binding.exception.concrete.ResourceWithIdNotFoundException;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.wv.coding.Context;
import org.n52.eventing.wv.coding.UserEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.springframework.web.bind.annotation.RestController;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.security.GroupPolicies;
import org.n52.eventing.wv.security.UserSecurityService;
import org.n52.eventing.wv.view.UserView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@RestController
@RequestMapping(value = UrlSettings.API_V1_BASE+"/"+WvCustomResources.USERS_RESOURCE,
        produces = {"application/json"})
public class UserController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private HibernateDatabaseConnection hdc;

    @Autowired
    private AccessRights accessRights;

    @Autowired
    private UserSecurityService userService;

    @Autowired
    private GroupPolicies groupPolicies;

    @Autowired
    private RequestContext context;

    @Autowired
    private PaginationFactory pageFactory;

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    @RequestMapping("")
    public ResourceCollectionWithMetadata<UserView> getUsers()
            throws IOException, URISyntaxException, InvalidPaginationException {
        Map<String, String[]> query = context.getParameters();
        Pagination p = pageFactory.fromQuery(query);

        AtomicBoolean expanded = new AtomicBoolean(false);
        if (query.containsKey("expanded")) {
            expanded.set(Boolean.parseBoolean(query.get("expanded")[0]));
        }

        String baseUrl = context.getBaseApiUrl();

        try (Session session = hdc.createSession()) {
            HibernateUserDao dao = new HibernateUserDao(session, groupPolicies);
            WvUser u;
            try {
                u = userService.resolveCurrentWvUser();
            } catch (NotAuthenticatedException ex) {
                LOG.warn(ex.getMessage());
                return new ResourceCollectionWithMetadata<>(new QueryResult<>(Collections.emptyList(), 0), p);
            } catch (ResourceNotFoundException e) {
                return new ResourceCollectionWithMetadata<>(new QueryResult<>(Collections.emptyList(), 0), p);
            }

            UserEncoder encoder = new UserEncoder(new Context(baseUrl, expanded.get(), false));

            QueryResult<WvUser> result = dao.retrieve(p, accessRights.enhanceUser(u));
            QueryResult<UserView> qr = new QueryResult<>(result.getResult().stream()
//                    .filter(g -> accessRights.canSeeSubscriptionsOfUser(u.get(), g))
                    .map((WvUser wu) -> {
                        return createUserView(wu, encoder);
                    })
                    .collect(Collectors.toList()), result.getTotalHits());

            return new ResourceCollectionWithMetadata<>(qr, p);
        }
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    @RequestMapping(value = "/{item}", method = GET)
    public UserView getUser(@PathVariable("item") String id)
            throws IOException, URISyntaxException, ResourceNotFoundException, InvalidPaginationException {
        Map<String, String[]> query = context.getParameters();

        AtomicBoolean expanded = new AtomicBoolean(true);
        if (query.containsKey("expanded")) {
            expanded.set(Boolean.parseBoolean(query.get("expanded")[0]));
        }

        String baseUrl = context.getBaseApiUrl();

        try (Session session = hdc.createSession()) {
            HibernateUserDao dao = new HibernateUserDao(session, groupPolicies);
            WvUser u;
            try {
                u = userService.resolveCurrentWvUser();
            } catch (NotAuthenticatedException ex) {
                LOG.warn(ex.getMessage());
                throw new ResourceWithIdNotFoundException(id);
            }

            Optional<WvUser> result = dao.retrieveById(Integer.parseInt(id));
            if (result.isPresent()) {
                if (!accessRights.canSeeSubscriptionsOfUser(u, result.get())) {
                    throw new  ResourceWithIdNotFoundException(id);
                }

                WvUser resultUser = result.get();

                Hibernate.initialize(resultUser.getGroups());
                return createUserView(result.get(), new UserEncoder(new Context(baseUrl, expanded.get(), false)));
            }
            else {
                throw new  ResourceWithIdNotFoundException(id);
            }
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
            throw new NumberFormatException("invalid ID provided. IDs must be an integer");
        }
    }

    @JsonView(org.n52.eventing.wv.view.Views.UserOverview.class)
    @RequestMapping(value = "/me", method = GET)
    public UserView getCurrentUser()
            throws IOException, URISyntaxException, InvalidPaginationException, ResourceNotFoundException {
        WvUser u;
        try {
            u = userService.resolveCurrentWvUser();
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new ResourceWithIdNotFoundException("me");
        } catch (ResourceWithIdNotFoundException ex) {
            throw new ResourceWithIdNotFoundException("me");

        }

        return getUser(Integer.toString(u.getId()));
    }

    private UserView createUserView(WvUser result, UserEncoder encoder) {
        result.setGroups(result.getGroups().stream()
                .filter(g -> groupPolicies.isSensorWebGroup(g))
                .filter(g -> !g.getName().endsWith(this.groupPolicies.getAdminSuffix()) || accessRights.withinAdminGroupNames(g.getName()) )
                .map(g -> {
                    g.setGroupAdmin(accessRights.isGroupAdmin(result, g));
                    return g;
                }).collect(Collectors.toSet()));

        result.setAdmin(accessRights.isInAdminGroup(result));
        return encoder.encode(result);
    }

}
