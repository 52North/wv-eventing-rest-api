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
package org.n52.eventing.wv.dao;

import java.util.Locale;
import java.util.Optional;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.model.BaseEntity;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 * @param <T> the generic entity implement BaseEntity
 */
public interface BaseDao<T extends BaseEntity> {

    Optional<T> retrieveById(int id);

    Optional<T> retrieveById(int id, Locale locale);

    QueryResult<T> retrieve(Pagination pagination) throws DatabaseException;

    QueryResult<T> retrieve(Pagination pagination, Locale locale) throws DatabaseException;

    void store(T r) throws DatabaseException;

    void store(T r, boolean transactionInProgress) throws DatabaseException;

    void update(T r) throws DatabaseException;

    void update(T r, boolean transactionInProgress) throws DatabaseException;

    void remove(T r) throws DatabaseException;

    void remove(T r, boolean transactionInProgress) throws DatabaseException;

    boolean exists(String name);

    Optional<T> retrieveByName(String name);

    Optional<T> retrieveByName(String name, Locale locale);

    void setDefaultLanguage(String defaultLanguage);

}
