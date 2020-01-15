(ns perun.core
  (:gen-class)
  (:require [environ.core             :as environ]
            [org.httpkit.client       :as http-kit]
            [cheshire.core            :as cheshire]
            [perun.flatten            :as flatten]
            [clojure.data.csv         :as csv]
            [clojure.java.io          :as io]
            [cognitect.aws.client.api :as aws]))

(defonce api-key (environ/env :ow-api-key))
(defonce output-s3-bucket (environ/env :perun-s3-bucket))
(defonce cities [{:city "Athens" :country "gr"}
                 {:city "Paris"  :country "fr"}
                 {:city "London" :country "uk"}
                 {:city "Madrid" :country "es"}
                 {:city "Moscow" :country "ru"}
                 {:city "Rome"   :country "it"}])


(defn parse-response [normalize-fn promise]
  (-> promise
      deref
      :body
      (cheshire/parse-string true)
      normalize-fn))


(defn write-csv [row-data path ]
  (let [columns (keys (first row-data))
        headers (map name columns)
        rows    (mapv #(mapv % columns) row-data)]
    (with-open [file (io/writer path)]
      (csv/write-csv file (cons headers rows)))))


(defn download-csv [{:keys [cities output url-fn parse-response-fn]}]
  (let [promises (doall (map #(http-kit/get (url-fn %)) cities))
        results  (doall (map parse-response-fn promises))]
    (-> (apply concat results)
        (write-csv output))))


;; Current weather
(defn current-weather-url [{:keys [city country]}]
  (str "https://api.openweathermap.org/data/2.5/weather?appid=" api-key "&q=" city "," country))


(defn denormalize-current-weather [current-weather-response]
  (-> current-weather-response
      (update :weather first)
      (flatten/flatten-map)
      (vector)))

(def parse-current-weather-response (partial parse-response denormalize-current-weather))

(comment
  (download-csv {:cities cities
                 :url-fn current-weather-url
                 :parse-response-fn parse-current-weather-response
                 :output "/tmp/foo.csv"
                 }) 
  )

;; Forecasts
(defn forecast-url [{:keys [city country]}]
  (str "https://api.openweathermap.org/data/2.5/forecast?appid=" api-key "&q=" city "," country))


(defn denormalize-forecast [{:keys [list] :as forecast-response}]
  (let [denormalized-city (flatten/flatten-map (select-keys forecast-response [:city]))]
    (map (fn [weather]
           (-> weather
               (update :weather first)
               (flatten/flatten-map)
               (merge denormalized-city)
               ))
         list)))

(def parse-forecast-response (partial parse-response denormalize-forecast))


(comment
  (download-csv {:cities cities
                 :url-fn forecast-url
                 :parse-response-fn parse-forecast-response
                 :output "/tmp/foo.csv"
                 }) 

)

;; S3 upload
(defn uuid [] (str (java.util.UUID/randomUUID)))

(def s3 (aws/client {:api :s3}))

(defn upload-to-s3 [{:keys [prefix file]}]
  (aws/invoke s3 {:op :PutObject :request {:Bucket output-s3-bucket :Key (str prefix "_" (uuid) ".csv")
                                           :Body (slurp file)}}))

(comment
  (aws/invoke s3 {:op :ListBuckets})  

  (aws/invoke s3 {:op :PutObject :request {:Bucket output-s3-bucket :Key (str (uuid) ".csv")
                                           :Body (slurp "/tmp/foo.csv")}}) 

  (uuid)
  )


(defn upload-current []
  (let [temp-file "/tmp/current.csv"]
    (download-csv {:cities cities
                   :url-fn current-weather-url
                   :parse-response-fn parse-current-weather-response
                   :output temp-file}) 
    (upload-to-s3 {:prefix "current" :file temp-file})))


(defn upload-forecasts []
  (let [temp-file "/tmp/forecast.csv"]
    (download-csv {:cities cities
                   :url-fn forecast-url
                   :parse-response-fn parse-forecast-response
                   :output temp-file
                   })
    (upload-to-s3 {:prefix "forecast" :file temp-file})))


(defn -main [mode & args]
  (cond (= "current" mode) (upload-current)
        (= "forecasts" mode) (upload-forecasts)
      :default (println "Usage: [current|forecasts]")))


(comment
  (-main "current") 
  (-main "forecasts") 
  )

