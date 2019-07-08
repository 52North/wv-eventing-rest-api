# Wupperverband Eventing API Test Plan

## Basic Data

### Groups

| id | name | admingroup for | global admingroup |
|----|------|------------|-------------------|
| 1 | sensorweb-opladen |  |  |
| 2 | sensorweb-opladen_admin | sensorweb-opladen |  |
| 3 | sensorweb-admins |  | yes |

### Users

| id | name | password | groups |
|----|------|----------|--------|
| 1 | sensorweb-admin1 | secret | sensorweb-admins |
| 2 | opladen-admin | secret | sensorweb-opladen, sensorweb-opladen_admin |
| 3 | opladen-user1 | secret | sensorweb-opladen |
| 4 | opladen-user2 | secret | sensorweb-opladen |

### Series

| id | category | phenomenon | procedure | feature_of_interest | unit | eventing_flag |
|---|---|---|---|---|---|-----|
| 1 | pegelstaende | Pegelstand | Einzelwert | Opladen | cm | 1 |
| 2 | pegelstaende | Pegelstand | Tagesmittelwert | Opladen | cm | 0 |

### Category

| id | category_id | name |
|---|---|---|
| 1 | pegelstaende_id | Pegelstaende |

### Phenomenon

| id | phenomenon_id | name |
|---|---|---|
| 1 | pegelstand_id | Pegelstand |

### Procedure

| id | procedure_id | name |
|---|---|---|
| 1 | einzelwert_id | Einzelwert |
| 2 | tagesmittelwert_id | Tagesmittelwert |

### Feature Of Interest

| id | feature_of_interest_id | name | reference_wv_id |
|---|---|---|---|
| 1 | opladen_id | Opladen | 71349 |

### Unit

| id | unit |
|---|--|
| 1 | cm |

## Subscription related Data

### Rules

| id | series | threshold | trend |
|----|------|------------|-------------------|
| 1 | 1 (Pegelstaende Opladen Einzelwerte) | 52.0 | 13 (steigt über den Grenzwert) |
| 2 | 1 (Pegelstaende Opladen Einzelwerte) | 52.0 | 21 (fällt unter den Grenzwert) |
| 3 | 1 (Pegelstaende Opladen Einzelwerte) | 102.0 | 13 (steigt über den Grenzwert) |
| 4 | 1 (Pegelstaende Opladen Einzelwerte) | 102.0 | 21 (fällt unter den Grenzwert) |

### Notifications

| id | series |
|----|------|
| 1 | 1 |
| 2 | 1 |

### Notification Rules (Grouping Rules as Notifications)

| notification | rule | notification level | is primary rule? |
|----|------|------------|-------------------|
| 1 | 1 | 1 (Warnung) | yes |
| 1 | 2 | 9 (Entwarnung) | no |

| notification | rule | notification level | is primary rule? |
|----|------|------------|-------------------|
| 2 | 3 | 1 (Warnung) | yes |
| 2 | 4 | 9 (Entwarnung) | no |

## Existing Subscriptions

| id | user | group | notification |
|----|------|------------|-------------------|
| 1 |  | 1 (sensorweb-opladen) | 1 |
| 2 | 3 (opladen-user1) |  | 2 |

## Existing Events

| id | time_stamp_created | series | event_type | observation_time | value | message | rule | notification |
|---|---|---|---|---|---|-----|---|-----|
| 1 | 2017-11-03T14:00:00Z | 1 (Pegelstaende Opladen Einzelwerte) | 4 (Abonnement) | 2017-11-03T13:45:00Z | 53.0 | Uebersteigung identifiziert! | 1 | 1 |
| 2 | 2017-11-03T15:00:00Z | 1 (Pegelstaende Opladen Einzelwerte) | 4 (Abonnement) | 2017-11-03T14:45:00Z | 51.0 | Fall unter Grenzwert identifiziert! | 2 | 1 |
| 3 | 2017-11-07T10:00:00Z | 1 (Pegelstaende Opladen Einzelwerte) | 4 (Abonnement) | 2017-11-07T11:45:00Z | 103.0 | Fall unter Grenzwert identifiziert! | 3 | 2 |

## Behaviour Testing

### User: opladen-user1

ID: 3

#### Retrieving Subscriptions

`HTTP GET` with `Authorization: Basic b3BsYWRlbi11c2VyMTpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/subscriptions

##### Expected Result:

List of Subscriptions with IDs: 1, 2

#### Retrieving Events

`HTTP GET` with `Authorization: Basic b3BsYWRlbi11c2VyMTpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/events

##### Expected Result:

List of events with IDs: 1, 2, 3

### User: opladen-user2

ID: 4

#### Retrieving Subscriptions

`HTTP GET` with `Authorization: Basic b3BsYWRlbi11c2VyMjpzZWNyZXQ=` (its a different one :-) ):

http://localhost:8080/wv-eventing-webapp/v1/subscriptions

##### Expected Result:

List of Subscriptions with IDs: 1

#### Retrieving Events

`HTTP GET` with `Authorization: Basic b3BsYWRlbi11c2VyMjpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/events

##### Expected Result:

List of events with IDs: 1, 2 (3 missing as it is a result of a `opladen-user1` subscription)

#### Deleting Subscriptions

`HTTP DELETE` with `Authorization: Basic b3BsYWRlbi11c2VyMjpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/subscriptions/1

##### Expected Result:

400 Bad Request with Error 'Subscription is not known: 1' (User cannot delete the group subscription)


### User: opladen-admin

ID: 2

#### Creating Subscriptions

`HTTP POST` with `Authorization: Basic b3BsYWRlbi11c2VyMjpzZWNyZXQ=` (its a different one :-) ):

http://localhost:8080/wv-eventing-webapp/v1/subscriptions

and payload:

```json
{  
   "publicationId":"1",
   "template":{  
      "id":"2",
      "parameters":{
      	"groupId": {
      		"value": 1
      	}
      }
   },
   "deliveryMethods":[{  
      "id":"wv-email",
      "parameters":{
      }
   }],
   "enabled":true,
   "endOfLife":"2018-06-19T13:22:08.248+02:00"
}
```

##### Expected Result:

200 OK (successful creation):

```json
{
    "id": 12345,
    "href": "http://localhost:8080/wv-eventing-webapp/v1/subscriptions/12345"
}
```
(id may defer)



#### Retrieving Subscriptions

`HTTP GET` with `Authorization: Basic b3BsYWRlbi1hZG1pbjpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/subscriptions

##### Expected Result:

List of Subscriptions with IDs: 1, 12345 (may defer, see above) and in
particular **not** 2 (as its a user subscription of a different user and
the requesting user is not a global admin).

#### Retrieving Events

`HTTP GET` with `Authorization: Basic b3BsYWRlbi1hZG1pbjpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/events

##### Expected Result:

List of events with IDs: 1, 2 (3 missing as it is a result of a `opladen-user1` subscription)

#### Deleting Subscriptions

`HTTP DELETE` with `Authorization: Basic b3BsYWRlbi11c2VyMjpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/subscriptions/1

##### Expected Result:

202 Accepted (successful removal)

#### Creating Templates

`HTTP POST` with `Authorization: Basic b3BsYWRlbi11c2VyMjpzZWNyZXQ=`:

http://localhost:8080/wv-eventing-webapp/v1/templates

and payload:

```json
{  
    "definition":{  
        "content":{
        	"publication":1,
        	"rules": [
        		{
		            "threshold":22.45,
		            "trend":11,
		            "primaryRule": true,
		            "level": 1
        		},
        		{
		            "threshold":22.45,
		            "trend":12,
		            "primaryRule": false,
		            "level": 9
        		}
    		]
        }
    }
}
```

##### Expected Result:

400 Bad Request with Error 'User is not allowed to create notifications'


### User: sensorweb-admin

ID: 3

This is a global administrator user.

#### Creating Templates

Templates are the basis for Notifications and the related Rule set.

`HTTP POST` with `Authorization: Basic c2Vuc29yd2ViLWFkbWluOnNlY3JldA==` (its a different one :-) ):

http://localhost:8080/wv-eventing-webapp/v1/templates

and payload:

```json
{  
    "definition":{  
        "content":{
        	"publication":1,
        	"rules": [
        		{
		            "threshold":22.45,
		            "trend":11,
		            "primaryRule": true,
		            "level": 1
        		},
        		{
		            "threshold":22.45,
		            "trend":12,
		            "primaryRule": false,
		            "level": 9
        		}
    		]
        }
    }
}
```

##### Expected Result:

200 OK (successful creation):

```json
{
    "id": 12345,
    "href": "http://localhost:8080/wv-eventing-webapp/v1/templates/12345"
}
```
(id may defer)
