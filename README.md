## Running

Use docker-compose.yml to start postgres, maven to download dependencies (pom.xml). The Spring Boot application was developed and tested on Java SDK 14.

## Documentation

There is a swagger file:
```
src/main/resources/v1.yaml
```

## Sample requests 

Ready to run Idea http scripts:
```
src/test/kotlin/com/jacekgajek/ads/sample_requests.http
```

## Workflow

Availible fields: `clicks`, `impressions`, `campaign`, `dataSource`, `daily` (case insensitive)

1. Select fields which you want to use directly with `select` parameter
2. Group with `groupBy` parameter
3. Filter data with `filter` parameter (RSQL syntax)
4. Select native aggregations (`sum`, `max`, `min`, `count`, `avg`)
5. Add custom expression using selected values with `custom` parameter. Aggregations are accessible in format `[aggregation]_[field]`, for example `sum_clicks`. All used variables must be selected in previous steps.
6. Exclude unwanted fields which was used as intermediate values with `exclude` parameter

See `sample_requests.http` for examples.

## Deployment

At the time of writing, app is deployed on heroku on https://mighty-bayou-95598.herokuapp.com

Swagger UI:
https://mighty-bayou-95598.herokuapp.com/swagger-ui/

