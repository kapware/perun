(ns perun.flatten)

;; source: https://gist.github.com/sudodoki/023d5f08c2f847b072b652687fdb27f2
(defn get-key
  [prefix key]
  (if (nil? prefix)
    key
    (str prefix "_" key)))


(defn flatten-map-kvs
  ([map] (flatten-map-kvs map nil))
  ([map prefix]
   (reduce
    (fn [memo [k v]]
      (if (map? v)
        (concat memo (flatten-map-kvs v (get-key prefix (name k))))
        (conj memo [(get-key prefix (name k)) v])))
    [] map)))


(defn flatten-map
  [m]
  (into {} (flatten-map-kvs m)))
