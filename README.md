# Project structure

* Implementation in `app/controllers` 
    * `JsonGenerator` first service for JSON data generation
    * `CSVConverter` second service for CSV data conversion 


# Running the app
The both services will be started using command

```
sbt run
```

# Accessing endpoints
## First service
### Generate JSON endpoint
```
curl -v localhost:9000/generate/json/7
```

## Second service
### First endpoint
Last part url defined number of rows
```
curl -v localhost:9000/csv/endpoint1/4
```

### Second endpoint
List of columns given as post parameter with 'Content-Type: text/plain'
```
curl -v -d '_type,_id,key,name,fullName,iata_airport_code,country,location_id,inEurope,countryCode,coreCountry,distance,latitude,longitude,type,id' -H 'Content-Type: text/plain' -X POST localhost:9000/csv/endpoint2/5
```

### Third endpoint
List of columns given as post parameter with 'Content-Type: text/plain'
```
curl -v -d 'latitude+longitude,sqrt(distance),abs(latitude),round(distance),distance' -H 'Content-Type: text/plain' -X POST localhost:9000/csv/endpoint3/3
```


# Current limitations

* For simplification and time reduction there is no validation and error handling of columns parameters - it demonstrates 'happy path' implementation of functionality
* Due to time constraints third endpoint implementation is limited
  * supported operators: *+/- with two parameters and functions: sqrt, abs and round for one parameter. 
  * operations only supported for Double type columns: latitude, longitude, distance - this is due the limitations of used galliaproject library

