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
package org.n52.eventing.wv.dao.hibernate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.model.BaseEntity;
import org.n52.eventing.wv.model.WvUser;
import org.springframework.core.GenericTypeResolver;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 * @param <T> the model entity of this Dao inherting BaseEntity
 */
public class BaseHibernateDao<T extends BaseEntity> {

    private final Class<T> genericType;
    private final Session session;
    private String defaultLanguage = "de";

    public BaseHibernateDao(Session session) {
        this.session = session;
        this.genericType = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), BaseHibernateDao.class);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public Optional<T> retrieveById(int id) {
        return retrieveById(id, null);
    }

    public Optional<T> retrieveById(int id, Locale locale) {
        return retrieveByKey("id", Integer.toString(id), locale);
    }

    public QueryResult<T> retrieve(Pagination pagination) {
        return retrieve(pagination, null);
    }

    public QueryResult<T> retrieve(Pagination pagination, Locale locale) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.genericType);
        Root<T> root = criteriaQuery.from(this.genericType);
        criteriaQuery.select(root);
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get(orderField())));

        List<Predicate> criteria = customCriteria(criteriaBuilder, root);
        if (criteria != null && criteria.size() > 0) {
            Predicate[] arr = new Predicate[criteria.size()];
            criteriaQuery.where(criteriaBuilder.and(criteria.toArray(arr)));
        }

        Query<T> query = session.createQuery(criteriaQuery);
//        Set<Filter> filters = addUserGroupFilters(user, session);

        QueryResult<T> result = queryPagination(query, pagination, locale);

//        disableFilters(filters, session);
        return result;
    }

    protected Set<Filter> addUserGroupFilters(WvUser user, Session session) {
        if (user != null && !user.isAdmin()) {
            Set<Filter> filters = new LinkedHashSet<>();
//            Filter filterUser = session.enableFilter("filter_user");
//            filterUser.setParameter("param_user", ((Integer)user.getId()).longValue());
//            filters.add(filterUser);
            if (user.hasAdminGroups()) {
                Filter filterUserGroups = session.enableFilter("filter_user_groups");
                filterUserGroups.setParameter("param_user", ((Integer) user.getId()).longValue());
                filterUserGroups.setParameterList("param_groups", user.getAdminGroups().stream()
                        .map(g -> ((Integer) g.getId()).longValue()).collect(Collectors.toSet()));
                filters.add(filterUserGroups);
            }
        }
        return Collections.emptySet();

    }

    protected QueryResult<T> queryPagination(Query<T> query, Pagination pagination, Locale locale) {
        int totalHits = 0;
        if (pagination != null) {
            totalHits = query.list().size();
            query.setFirstResult(pagination.getOffset());
            query.setMaxResults(pagination.getLimit());
        }

        // only retrieved localed if it is not our default ("de")
        List<T> data;
        if (locale != null && !defaultLanguage.equalsIgnoreCase(locale.getLanguage())) {
            data = query.list().stream()
                    .map(item -> {
                        Optional<Localized<T>> result = retrieveLocaled(item, locale);
                        if (result.isPresent()) {
                            return localize(result.get(), item);
                        }
                        return item;
                    })
                    .collect(Collectors.toList());

        } else {
            data = query.list();
        }

        if (totalHits == 0) {
            totalHits = data.size();
        }

        return new QueryResult<>(data, totalHits);
    }

    protected void disableFilters(Collection<Filter> filters, Session session) {
       filters.forEach(f -> session.disableFilter(f.getName()));
    }

    protected Optional<Localized<T>> retrieveLocaled(T originalOpt, Locale locale) {
        return Optional.empty();
    }


    public boolean exists(int id) {
        return retrieveById(id).isPresent();
    }

    public boolean exists(String name) {
        return retrieveByName(name).isPresent();
    }

    public Optional<T> retrieveByName(String name) {
        return retrieveByKey("name", name);
    }

    public Optional<T> retrieveByName(String name, Locale locale) {
        return retrieveByKey("name", name, locale);
    }

    protected Optional<T> retrieveByKey(String key, String value) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(this.genericType);
        Root<T> root = query.from(this.genericType);

        List<Predicate> criteria = customCriteria(builder, root);
        if (criteria != null && criteria.size() > 0) {
            Predicate[] finalCriteria = new Predicate[criteria.size()+1];
            int i = 0;
            finalCriteria[i++] = builder.equal(root.get(key), value);
            for (Predicate c : criteria) {
                finalCriteria[i++] = c;
            }
            query.where(builder.and(finalCriteria));
        }
        else {
            query.where(builder.equal(root.get(key), value));
        }

        List<T> result = getSession().createQuery(query).list();
        return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
    }

    protected Optional<T> retrieveByKey(String key, String value, Locale locale) {
        if (locale == null) {
            return retrieveByKey("id", value);
        }

        Optional<T> result = retrieveByKey("id", value);
        if (result.isPresent()) {
            Optional<Localized<T>> localized = retrieveLocaled(result.get(), locale);

            if (localized.isPresent()) {
                return Optional.of(localize(localized.get(), result.get()));
            }

        }

        return result;
    }

    protected T localize(Localized<T> localed, T original) {
        return original;
    }

    public void store(T o) throws DatabaseException {
        store(o, false);
    }

    public void store(T o, boolean transactionInProgress) throws DatabaseException {
        if (!transactionInProgress) {
            Transaction trans = session.beginTransaction();
            session.save(o);
            trans.commit();
        }
        else {
            session.save(o);
        }
    }

    public void update(T o) throws DatabaseException {
        update(o, false);
    }

    public void update(T o, boolean transactionInProgress) throws DatabaseException {
        if (!transactionInProgress) {
            Transaction trans = session.beginTransaction();
            session.update(o);
            trans.commit();
        }
        else {
            session.update(o);
        }
    }

    public void remove(T o) throws DatabaseException {
        remove(o, false);
    }

    public void remove(T o, boolean transactionInProgress) throws DatabaseException {
        if (!transactionInProgress) {
            Transaction trans = session.beginTransaction();
            session.delete(o);
            trans.commit();
        }
        else {
            session.delete(o);
        }
    }

    protected Session getSession() {
        return session;
    }

    protected Class<T> getGenericType() {
        return genericType;
    }

    protected List<Predicate> customCriteria(CriteriaBuilder criteriaBuilder, Root<T> from) {
        return null;
    }

    protected String orderField() {
        return "id";
    }

}
