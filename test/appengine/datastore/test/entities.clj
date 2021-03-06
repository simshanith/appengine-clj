(ns appengine.datastore.test.entities
  (:import (com.google.appengine.api.datastore Entity Key GeoPt))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.datastore.protocols
        appengine.test
        appengine.utils
        clojure.test
        [clojure.contrib.string :only (join lower-case)]
        [clojure.contrib.seq :only (includes?)]))

(refer-private 'appengine.datastore.entities)

(defentity Person ()
  ((name)))

(defentity Continent ()
  ((iso-3166-alpha-2 :key lower-case)
   (location :serialize GeoPt)
   (name)))

;; (with-local-datastore
;;   ;; (country-key
;;   ;;  (continent-key {:iso-3166-alpha-2 "eu"})
;;   ;;  {:iso-3166-alpha-2 "de"})
;;   ;; (person-key {})
;;   ;; (continent-key {:iso-3166-alpha-2 "eu"})
;;   ;; (country-key nil nil)
;;   ;; (person {})
;;   ;; (println (country (continent {:iso-3166-alpha-2 "eu" :name "Europe"})
;;   ;;           {:iso-3166-alpha-2 "de" :name "Germany"}))
;;   )

(defentity Country (Continent)
  ((iso-3166-alpha-2 :key lower-case)
   (location :serialize GeoPt)
   (name)))

(defentity Region (Country)
  ((country :key lower-case)
   (location :serialize GeoPt)
   (name :key lower-case)))

(defn europe-seq []
  [:iso-3166-alpha-2 "eu"
   :key (make-key (entity-kind-name Continent) "eu")
   :kind (entity-kind-name Continent)
   :location {:latitude (float 54.52) :longitude (float 15.25)}
   :name "Europe"])

(defn europe-array-map []
  (apply array-map (europe-seq)) )

(defn europe-hash-map []
  (apply hash-map (europe-seq)) )

(defn europe-entity []
  (let [{:keys [key name iso-3166-alpha-2 location]} (europe-hash-map)]
    (doto (Entity. key)
      (.setProperty "name" name)
      (.setProperty "iso-3166-alpha-2" iso-3166-alpha-2)
      (.setProperty "location" (GeoPt. (:latitude location) (:longitude location))))))

(defn europe-record []
  (continent
   {:iso-3166-alpha-2 "eu"
    :key (make-key (entity-kind-name Continent) "eu")
    :kind (entity-kind-name Continent)
    :location {:latitude (float 54.52) :longitude (float 15.25)}
    :name "Europe"}))

(defn make-germany []
  (country
   (europe-record)
   {:iso-3166-alpha-2 "de"
    :location {:latitude (float 51.16) :longitude (float 10.45)}
    :name "Germany"}))

(def continent-specification
     ['(iso-3166-alpha-2 :key #'lower-case)
      '(location :serialize GeoPt)
      '(name)])

(def region-specification
     ['(country :key #'lower-case)
      '(location :serialize GeoPt)
      '(name :key #'lower-case)])

(datastore-test test-entity?
  (is (not (entity? nil)))
  (is (not (entity? "")))
  (is (entity? (Entity. "person"))))

(deftest test-entity-fn-doc
  (are [record name]
    (is (= (entity-fn-doc record) name))
    Continent "Make a continent."
    'CountryFlag "Make a country flag."))

(deftest test-entity-fn-sym
  (are [record name]
    (is (= (entity-fn-sym record) name))
    Continent 'continent
    'CountryFlag 'country-flag))

(deftest test-entity-p-fn-doc
  (are [record name]
    (is (= (entity-p-fn-doc record) name))
    Continent "Returns true if arg is a continent, false otherwise."
    'CountryFlag "Returns true if arg is a country flag, false otherwise."))

(deftest test-entity-p-fn-sym
  (are [record name]
    (is (= (entity-p-fn-sym record) name))
    Continent 'continent?
    'CountryFlag 'country-flag?))

(deftest test-entity-kind-name
  (are [entity kind]
    (is (= (entity-kind-name entity) kind))
    nil nil
    Continent "continent"
    "Continent" "continent"))

(deftest test-entity-protocol-name
  (are [entity expected]
    (is (= (entity-protocol-name entity) expected))
    nil nil
    "" nil
    Continent "ContinentProtocol"
    'Continent "ContinentProtocol"
    "Continent" "ContinentProtocol"))

(deftest test-extract-key
  (let [key-fns [[:iso-3166-alpha-2 true] [:name lower-case]]]
    (is (= (extract-key nil nil)
           (extract-key nil [])
           (extract-key {} nil)
           (extract-key {} [])
           (extract-key nil key-fns)
           (extract-key {} key-fns)
           (extract-key {:iso-3166-alpha-2 "de"} key-fns)
           (extract-key {:name "Berlin"} key-fns)
           nil))
    (is (= (extract-key {:iso-3166-alpha-2 "de" :name "Berlin"} key-fns)
           "de-berlin"))))

(deftest test-extract-key-fns
  (is (= (extract-key-fns continent-specification)
         [[:iso-3166-alpha-2 '#'lower-case]]))
  (is (= (extract-key-fns region-specification)
         [[:country '#'lower-case] [:name '#'lower-case]])))

(deftest test-extract-option
  (is (= (extract-option continent-specification :key)
         {:iso-3166-alpha-2 '(var lower-case)}))
  (is (= (extract-option continent-specification :serialize)
         {:location 'GeoPt})))

(deftest test-extract-properties
  (let [properties (extract-properties continent-specification)]      
    (is (= (:iso-3166-alpha-2 properties) {:key '#'lower-case}))
    (is (= (:location properties) {:serialize 'GeoPt}))
    (is (= (:name properties) {}))))

(deftest test-extract-serializer
  (is (= (extract-serializer continent-specification)
         {:location 'GeoPt})))

(deftest test-extract-values
  (let [key-fns [[:iso-3166-alpha-2 true] [:name lower-case]]]
    (is (empty? (extract-values nil nil)))
    (is (= (extract-values nil key-fns)
           (extract-values {} key-fns)
           [nil nil]))
    (is (= (extract-values {:iso-3166-alpha-2 "de" :name "Berlin"} key-fns)
           ["de" "berlin"]))))

(deftest test-key-fn-doc
  (are [record name]
    (is (= (key-fn-doc record) name))
    Continent "Make a continent key."
    'CountryFlag "Make a country flag key."))

(deftest test-key-fn-sym
  (are [record name]
    (is (= (key-fn-sym record) name))
    Continent 'continent-key
    'CountryFlag 'country-flag-key))

(deftest test-key-name-fn-doc
  (are [record name]
    (is (= (key-name-fn-doc record) name))
    'Continent "Extract the continent key name."
    'CountryFlag "Extract the country flag key name."))

(deftest test-key-name-fn-sym
  (are [record name]
    (is (= (key-name-fn-sym record) name))
    Continent 'continent-key-name
    'CountryFlag 'country-flag-key-name))

(deftest test-find-entities-fn-sym
  (are [record name]
    (is (= (find-entities-fn-sym record) name))
    Continent 'find-continents
    'CountryFlag 'find-country-flags))

(deftest test-find-entities-property-fn-sym
  (are [record property name]
    (is (= (find-entities-by-property-fn-sym record property) name))
    Continent 'iso-3166-alpha-2 'find-continents-by-iso-3166-alpha-2
    'CountryFlag 'iso-3166-alpha-2 'find-country-flags-by-iso-3166-alpha-2))

(datastore-test test-make-blank-entity-with-named-key
  (let [key (make-key "continent" "eu")
        entity (make-blank-entity key)]
    (is (= (.getKey entity) key))
    (is (= (.getKind entity) "continent"))
    (is (empty? (.getProperties entity)))
    (is (isa? (class entity) Entity))
    (is (nil? (.getParent entity)))))

(datastore-test test-make-blank-entity-with-numbered-key 
  (let [key (make-key "continent" 1)
        entity (make-blank-entity key)]
    (is (= (.getKey entity) key))
    (is (= (.getKind entity) "continent"))
    (is (empty? (.getProperties entity)))
    (is (isa? (class entity) Entity))
    (is (nil? (.getParent entity)))))

(datastore-test test-make-blank-entity-with-kind
  (let [entity (make-blank-entity "continent")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "continent"))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (= (.getId key) 0))
      (is (= (.getKind key) "continent"))
      (is (isa? (class key) Key))
      (is (nil? (.getName key)))
      (is (nil? (.getParent key)))
      (is (not (.isComplete key))))))

(datastore-test test-make-blank-entity-with-parent-kind-and-named-key
  (let [parent-key (make-key "continent" "eu")
        entity (make-blank-entity parent-key "country" "de")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "country"))
    (is (= (.getParent entity) parent-key))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (.isComplete key))
      (is (= (.getId key) 0))
      (is (= (.getKind key) "country"))
      (is (= (.getName key) "de"))
      (is (= (.getParent key) parent-key))
      (is (isa? (class key) Key)))))

(datastore-test test-make-blank-entity-with-parent-kind-and-numbered-key
  (let [parent-key (make-key "continent" "eu")
        entity (make-blank-entity parent-key "country" 1)]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "country"))
    (is (= (.getParent entity) parent-key))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (.isComplete key))
      (is (= (.getId key) 1))
      (is (= (.getKind key) "country"))
      (is (= (.getParent key) parent-key))
      (is (isa? (class key) Key))
      (is (nil? (.getName key))))))

;; ENTITY KEY NAME

(datastore-test test-person-key-name
  (is (= (person-key-name {})
         (person-key-name {:name "Bob"})
         nil)))

(deftest test-continent-key-name
  (is (= (continent-key-name {})
         (continent-key-name {:name "Europe"})
         nil))
  (is (= (continent-key-name {:iso-3166-alpha-2 "eu"}) "eu")))

(deftest test-country-key-name
  (is (= (country-key-name {})
         (country-key-name {:name "Germany"})
         nil))
  (is (= (country-key-name {:iso-3166-alpha-2 "de"}) "de")))

(deftest test-region-key-name
  (is (= (region-key-name {})
         (region-key-name {:latitude 1 :longitude 2})
         nil))
  (is (= (region-key-name {:country "de" :name "Berlin"}) "de-berlin")))


;; ENTITY KEY

(datastore-test test-person-key
  (is (= (person-key {})
         (person-key {:name "Bob"})
         nil)))

(datastore-test test-continent-key
  (is (= (continent-key {})
         (continent-key {:name "Europe"})
         nil))
  (let [key (continent-key {:iso-3166-alpha-2 "eu"})]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) (entity-kind-name Continent)))
    (is (= (.getId key) 0))
    (is (= (.getName key) "eu"))))

(datastore-test test-country-key
  (let [continent-key (make-key "continent" "eu")]
    (is (= (country-key continent-key {})
           (country-key continent-key {:name "Europe"})
           nil))
    (let [country-key (country-key continent-key {:iso-3166-alpha-2 "de"})]
      (is (key? country-key))
      (is (.isComplete country-key))    
      (is (= (.getParent country-key) continent-key))    
      (is (= (.getKind country-key) "country"))
      (is (= (.getId country-key) 0))
      (is (= (.getName country-key) "de")))))

(datastore-test test-region-key
  (let [continent-key (make-key "continent" "eu")
        country-key (country-key continent-key {:iso-3166-alpha-2 "de"})]
    (is (= (region-key country-key {})
           (region-key country-key {:name "Europe"})
           nil))
    (let [region-key (region-key country-key {:country "de" :name "Berlin"})]
      (is (key? region-key))
      (is (.isComplete region-key))    
      (is (= (.getParent region-key) country-key))    
      (is (= (.getParent (.getParent region-key)) continent-key))    
      (is (= (.getKind region-key) "region"))
      (is (= (.getId region-key) 0))
      (is (= (.getName region-key) "de-berlin")))))

;; ENTITY

(datastore-test test-person
  (let [person (person {:name "Bob"})]
    (is (nil? (:key person)))
    (is (= (:kind person) "person"))
    (is (= (:name person) "Bob"))))

(datastore-test test-continent
  (let [location {:latitude 54.52 :longitude 15.25}
        continent (continent {:iso-3166-alpha-2 "eu" :name "Europe" :location location})]
    (let [key (:key continent)]
      (is (key? key))
      (is (.isComplete key))    
      (is (nil? (.getParent key)))    
      (is (= (.getKind key) (entity-kind-name Continent)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (is (= (:iso-3166-alpha-2 continent) "eu"))
    (is (= (:kind continent) (.getKind (:key continent))))
    (is (= (:location continent) location))
    (is (= (:name continent) "Europe"))))

(datastore-test test-country
  (let [location {:latitude 51.16 :longitude 10.45}
        country (country (europe-record) {:iso-3166-alpha-2 "de" :name "Germany" :location location})]
    (let [key (:key country)]
      (is (key? key))
      (is (.isComplete key))    
      (is (= (.getParent key) (:key (europe-record))))    
      (is (= (.getKind key) (entity-kind-name Country)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de")))
    (is (= (:iso-3166-alpha-2 country) "de"))
    (is (= (:kind country) (.getKind (:key country))))
    (is (= (:location country) location))
    (is (= (:name country) "Germany"))))

(datastore-test test-region
  (let [location {:latitude 52.52 :longitude 13.41}
        region (region (make-germany) {:country "de" :name "Berlin" :location location})]
    (let [key (:key region)]
      (is (key? key))
      (is (.isComplete key))    
      (is (= (.getParent key) (:key (make-germany))))    
      (is (= (.getKind key) (entity-kind-name Region)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de-berlin")))
    (is (= (:country region) "de"))
    (is (= (:kind region) (.getKind (:key region))))
    (is (= (:location region) location))
    (is (= (:name region) "Berlin"))))

;; ENTITY-P

(datastore-test test-continent?
  (is (not (continent? nil)))
  (is (not (continent? "")))
  (is (not (continent? (make-germany))))
  (is (continent? (europe-array-map)))
  (is (continent? (europe-entity)))
  (is (continent? (europe-hash-map)))
  (is (continent? (europe-record))))

;; DESERIALIZE

(datastore-test test-deserialize-entity
  (let [entity (Entity. (make-key "color" "red")) map (deserialize-entity entity)]
    (is (map? map))
    (is (= (count (keys map)) (+ 2 (count (.getProperties entity)))))
    (is (= (:key map) (.getKey entity)))
    (is (= (:kind map) (.getKind entity)))))

(datastore-test test-deserialize-entity-with-continent
  (let [entity (europe-entity) record (deserialize-entity entity)]
    (is (continent? record))
    (is (isa? (class record) Continent))
    (is (= (count (keys record)) (+ 2 (count (.getProperties entity)))))
    (is (= (:key record) (.getKey entity)))
    (is (= (:kind record) (.getKind entity)))
    (is (= (:iso-3166-alpha-2 record) (.getProperty entity "iso-3166-alpha-2")))
    (is (= (:name record) (.getProperty entity "name"))) 
    (is (= (:location record) (deserialize (.getProperty entity "location"))))))

;; SERIALIZE

(datastore-test test-serialize-entity-with-person
  (let [entity (serialize-entity {:kind "person" :name "Roman"})]
    (is (entity? entity))
    (is (= (.getKind entity) "person"))
    (is (= (.getProperty entity "name") "Roman"))
    (let [key (.getKey entity)]
      (is (= (.getId key) 0))
      (is (= (.getKind key) "person"))
      (is (isa? (class key) Key))
      (is (nil? (.getName key)))
      (is (nil? (.getParent key)))
      (is (not (.isComplete key))))))

(datastore-test test-serialize-entity-with-color
  (let [entity (serialize-entity {:kind "color" :key (make-key "color" "red")})]
    (is (entity? entity))
    (is (= (.getKind entity) "color"))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (.isComplete key))
      (is (= (.getId key) 0))
      (is (= (.getKind key) "color"))
      (is (= (.getName key) "red"))
      (is (isa? (class key) Key))
      (is (nil? (.getParent key))))))

(datastore-test test-serialize-entity-with-continent
  (is (= (serialize-entity (europe-array-map)) (europe-entity)))
  (is (= (serialize-entity (europe-hash-map)) (europe-entity)))
  (is (= (serialize-entity (europe-record)) (europe-entity))))

(datastore-test test-serialize  
  (are [record]
    (let [entity (serialize record)]
      (is (entity? entity))
      (are [property-name property-value]
        (is (= (.getProperty entity (stringify property-name)) property-value))
        :name "Europe"
        :iso-3166-alpha-2 "eu")
      (let [key (.getKey entity)]
        (is (key? key))
        (is (.isComplete key))
        (is (nil? (.getParent key)))
        (is (= (.getId key) 0))
        (is (= (.getName key) "eu"))))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

;; FIND ENTITIES

(datastore-test test-find-continents
  (let [europe (save-entity (europe-record))
        continents (find-continents)]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (= (first continents) europe))))

(datastore-test test-find-continents-by-iso-3166-alpha-2
  (let [europe (save-entity (europe-record))
        continents (find-continents-by-iso-3166-alpha-2 (:iso-3166-alpha-2 europe))]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (includes? continents europe))))

(datastore-test test-find-continents-by-name
  (let [europe (save-entity (europe-record))
        continents (find-continents-by-name (:name europe))]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (includes? continents europe))))

(datastore-test test-find-continents-by-location
  (let [europe (save-entity (europe-record))
        continents (find-continents-by-location (:location europe))]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (includes? continents europe))))

;; CRUD

(datastore-test test-create-entity  
  (are [object]
    (do
      (is (nil? (find-entity object)))
      (let [record (create-entity object)]
        (is (continent? record))        
        (is (= (find-entity record) record))
        (is (thrown? Exception (create-entity object)))
        (is (delete-entity record))))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-delete-entity  
  (are [record]
    (do
      (create-entity record)
      (is (not (nil? (find-entity record))))
      (is (delete-entity record))
      (is (nil? (find-entity record)))
      (is (delete-entity record)))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-save-entity
  (are [object]        
    (let [entity (save-entity object)]
      (is (continent? entity))        
      (is (= (find-entity entity) entity))
      (is (map? (save-entity object)))
      (is (delete-entity entity)))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-find-entity
  (are [object]        
    (let [entity (save-entity object)]
      (is (continent? (find-entity entity)))
      (is (= (find-entity entity) entity))
      (is (delete-entity entity))
      (is (nil? (find-entity entity))))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-update-entity
  (let [updates {:name "Asia"}]
    (are [object key-vals]
      (do
        (is (delete-entity object))
        (let [entity (update-entity object key-vals)]
          (doseq [[key value] key-vals]
            (is (= (key entity) value)))
          (is (map? entity))        
          (is (= (select-keys entity (keys key-vals)) key-vals))        
          (is (= (update-entity object key-vals) entity))))
      (europe-array-map) updates
      (europe-entity) updates
      (europe-hash-map) updates
      (europe-record) updates)))

(datastore-test test-update-entity-with-location
  (let [updates {:name "Asia" :location {:latitude 1 :longitude 2}}]
    (are [object key-vals]
      (do
        (is (delete-entity object))
        (let [entity (update-entity object key-vals)]
          (doseq [[key value] key-vals]
            (is (= (key entity) value)))
          (is (map? entity))        
          (is (= (select-keys entity (keys key-vals)) key-vals))        
          (is (= (update-entity object key-vals) entity))))
      (europe-array-map) updates
      (europe-entity) updates
      (europe-hash-map) updates
      (europe-record) updates)))
