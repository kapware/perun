# Perun

> In Slavic mythology, Perun (Cyrillic: Перýн) is the highest god of the pantheon and the god of sky, thunder, lightning, storms, rain, law, war, fertility and oak trees.

source: https://en.wikipedia.org/wiki/Perun

This is a showcase clojure project. The objective is to show how to consume REST api (https://openweathermap.org) and upload processed result to AWS S3.

## Prerequisites
* `clj` installed: https://clojure.org/guides/getting_started,
* AWS cli configured with enough permissions to create S3 objects in a bucket specified as `PERUN_S3_BUCKET` env variable,
* OpenWeather API key set as environment variable `OW_API_KEY`.

## Running locally
Following will download current weather and upload it to S3 bucket:
```
clj -Arun current
```

and this will download forecasts and upload it to S3 bucket:
```
clj -Arun forecasts
```

## Deploying
This is service is not meant for production, but if you still want to use it, build a jar (or native image), follow one of following excellent howtos:
https://github.com/clojure/tools.deps.alpha/wiki/Tools#build-tool-integration

or, if you're lazy:
```
clj -Ajar
```
and:
```
java -cp perun.jar clojure.main -m perun.core forecasts
```
