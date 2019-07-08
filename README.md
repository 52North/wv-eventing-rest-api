# 52°North Eventing REST API - Wupperverband Edition

This software is an extension to the [52°North Eventing REST API](https://github.com/52North/eventing-rest-api/).
It extends the REST interface with some additional endpoints and model specifics.

## Installation

First, you need to build the [52°North Eventing REST API](https://github.com/52North/eventing-rest-api/).

1. Clone the repository: `git clone https://github.com/52North/eventing-rest-api/`
1. Execute Maven: `cd eventing-rest-api && mvn clean install`

Now, this component can be build:

1. `mvn clean install` (in the root of this project)

A WAR file will be generated under the  `webapp/target` folder.

## Running

### PostgreSQL

The application relies on a specific PostgreSQL / PostGIS database structure.
To set up the structure, a set of SQL files are available in the `_docker` folder.
Execute this in the correct order (files are prefixed).

### Docker PostgreSQL

Alternatively, you can use a dockerized version of the database. This will automatically
set up the database structure. The following commands shall be run in the `_docker` subfolder.

```sh
docker run -e POSTGRES_USER=postgres -e POSTGRES_PASS=postgres -e POSTGRES_DB=sos -p 5432:5432 --name wv-test-data -v $(pwd)/1_prepare-db.sql:/docker-entrypoint-initdb.d/zz_1_prepare-db.sql -v $(pwd)/2_public.interval_text.sql:/docker-entrypoint-initdb.d/zz_2_public.interval_text.sql -v $(pwd)/3_public.wintersommerzeit.sql:/docker-entrypoint-initdb.d/zz_3_public.wintersommerzeit.sql -v $(pwd)/4_schema-data.sql:/docker-entrypoint-initdb.d/zz_4_schema-data.sql mdillon/postgis:10
```

A `docker-compose` file is also available which eases the setup:

`docker-compose up --build`

## License

Copyright 2019 52°North Initiative for Geospatial Open Source Software GmbH

Licensed under GNU General Public License, Version 2.0.
