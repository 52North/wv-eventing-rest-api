/*
 * Copyright (C) 2016 - 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
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
import org.n52.eventing.wv.coding.GroupEncoder;
import org.n52.eventing.wv.coding.UserEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.springframework.web.bind.annotation.RestController;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.AccessRights;
import org.n52.eventing.wv.security.GroupPolicies;
import org.n52.eventing.wv.security.UserSecurityService;
import org.n52.eventing.wv.view.GroupView;
import org.n52.eventing.wv.view.UserView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@RestController
@RequestMapping(value = UrlSettings.API_V1_BASE+"/"+WvCustomResources.GROUPS_RESOURCE,
        produces = {"application/json"})
public class GroupController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(GroupController.class);

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

    @JsonView(value = org.n52.eventing.wv.view.Views.UserOverview.class)
    @RequestMapping("")
    public ResourceCollectionWithMetadata<GroupView> getGroups()
            throws IOException, URISyntaxException, InvalidPaginationException, ResourceNotFoundException {
        Map<String, String[]> query = context.getParameters();
        Pagination p = pageFactory.fromQuery(query);
        WvUser u;
        try {
            u = userService.resolveCurrentWvUser();
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new ResourceNotFoundException("me");
        } catch (ResourceNotFoundException e) {
            return new ResourceCollectionWithMetadata<>(new QueryResult<>(Collections.emptyList(), 0), p);
        }
        try (Session session = hdc.createSession()) {
            HibernateGroupDao dao = new HibernateGroupDao(session, groupPolicies);


            GroupEncoder encoder = new GroupEncoder();

            QueryResult<Group> result = dao.retrieve(p, u);
            QueryResult<GroupView> qr = new QueryResult<GroupView>(result.getResult().stream()
//                    .filter(g -> groupPolicies.isSensorWebGroup(g) )
//                    .filter(g -> accessRights.canSeeSubscriptionsOfGroup(u.get(), g) )
////                    .filter(g -> accessRights.withinAdminGroupNames(g.getName()) && accessRights.isInAdminGroup(u.get()) )
                    .map(g -> {
                        g.setGroupAdmin(accessRights.isGroupAdmin(u, g));
                        return encoder.encode(g);
                    })
                    .collect(Collectors.toList()), result.getTotalHits());

            return new ResourceCollectionWithMetadata<GroupView>(qr, p);
        }
    }

    @JsonView(value = org.n52.eventing.wv.view.Views.UserOverview.class)
    @RequestMapping(value = "/{item}", method = GET)
    public GroupView getGroup(@PathVariable("item") String id)
            throws IOException, URISyntaxException, ResourceNotFoundException {
        WvUser u;
        try {
            u = userService.resolveCurrentWvUser();
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new ResourceWithIdNotFoundException(id);
        }

        if (!accessRights.isInAdminGroup(u) && !accessRights.isGroupAdmin(u, Integer.parseInt(id))) {
            throw new ResourceWithIdNotFoundException(id);
        }

        try (Session session = hdc.createSession()) {
            HibernateGroupDao dao = new HibernateGroupDao(session, groupPolicies);

            Optional<Group> result = dao.retrieveById(Integer.parseInt(id));
            if (result.isPresent()) {
                if (!accessRights.canSeeSubscriptionsOfGroup(u, result.get())) {
                    throw new ResourceWithIdNotFoundException(id);
                }
                Group g = result.get();

                if (!groupPolicies.isSensorWebGroup(g)) {
                    throw new ResourceWithIdNotFoundException(id);
                }

                g.setGroupAdmin(accessRights.isGroupAdmin(u, g));

                return new GroupEncoder().encode(g);
            }
            else {
                throw new ResourceWithIdNotFoundException(id);
            }
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
            throw new NumberFormatException("invalid ID provided. IDs must be an integer");
        }
    }

    @RequestMapping("/{item}/users")
    public ResourceCollectionWithMetadata<UserView> getGroupUsers(@PathVariable("item") String id)
            throws IOException, URISyntaxException, ResourceNotFoundException, InvalidPaginationException {
        WvUser u;
        try {
            u = userService.resolveCurrentWvUser();
        } catch (NotAuthenticatedException ex) {
            LOG.warn(ex.getMessage());
            throw new ResourceWithIdNotFoundException(id);
        }

        if (!accessRights.isInAdminGroup(u) && !accessRights.isGroupAdmin(u, Integer.parseInt(id))) {
            throw new ResourceWithIdNotFoundException(id);
        }

        Map<String, String[]> query = context.getParameters();
        Pagination p = pageFactory.fromQuery(query);

        AtomicBoolean expanded = new AtomicBoolean(false);
        if (query.containsKey("expanded")) {
            expanded.set(Boolean.parseBoolean(query.get("expanded")[0]));
        }

        String baseUrl = context.getBaseApiUrl();

        try (Session session = hdc.createSession()) {
            HibernateUserDao dao = new HibernateUserDao(session, groupPolicies);
            HibernateGroupDao groupDao = new HibernateGroupDao(session, groupPolicies);

            Optional<Group> g = groupDao.retrieveById(Integer.parseInt(id));

            if (!g.isPresent()) {
                throw new ResourceWithIdNotFoundException(id);
            }

            if (!groupPolicies.isSensorWebGroup(g.get())) {
                throw new ResourceWithIdNotFoundException(id);
            }

            UserEncoder encoder = new UserEncoder(new Context(baseUrl, expanded.get()));

            QueryResult<WvUser> result = dao.retrieveByGroup(g.get(), p);
            QueryResult<UserView> qr = new QueryResult<>(result.getResult().stream()
                    .filter(gr -> accessRights.canSeeSubscriptionsOfUser(u, gr))
                    .map((WvUser wu) -> {
                        Hibernate.initialize(wu.getGroups());

                        wu.setGroups(wu.getGroups().stream()
                            .filter(gr -> groupPolicies.isSensorWebGroup(gr))
                            .filter(gr -> accessRights.canSeeSubscriptionsOfGroup(u, gr))
                            .filter(gr -> !gr.getName().endsWith(this.groupPolicies.getAdminSuffix()))
                            .map(gr -> {
                                gr.setGroupAdmin(accessRights.isGroupAdmin(wu, gr));
                                return gr;
                            }).collect(Collectors.toSet()));

                        wu.setAdmin(accessRights.isInAdminGroup(wu));
                        return encoder.encode(wu);
                    })
                    .collect(Collectors.toList()), result.getTotalHits());
            return new ResourceCollectionWithMetadata<UserView>(qr, p);
        }
        catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
            throw new NumberFormatException("invalid ID provided. IDs must be an integer");
        }
    }
}
