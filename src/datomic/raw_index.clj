(ns datomic.raw-index
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

;; Raw index
;; https://docs.datomic.com/cloud/query/raw-index-access.html

;; See tutorial-inventory for the full data set

(def cfg {:server-type :dev-local
          :system "datomic-tutorial"})
(def client (d/client cfg))
(def db-name "raw-index")
(d/delete-database client {:db-name db-name})
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(def schema-1
  [{:db/ident :inv/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :inv/color
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   ])

(defn create-sample-data
  []
  [{:inv/color "blue"
    :inv/name "adam"}
   #_{:inv/color "red"
    :inv/name "beatrice"}])

(def sample-data (create-sample-data))
;;(pp/pprint sample-data)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transactions
(def schema-tx (d/transact conn {:tx-data schema-1}))
;;(pp/pprint schema-tx)
(def sample-data-tx (d/transact conn {:tx-data sample-data}))
;;(pp/pprint sample-data-tx)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Raw indexes
(def db (d/db conn))

(d/q '{:find [?name]
       :where [[?e :inv/name "adam"]
               [?e :inv/color ?color]
               [?e2 :inv/color ?color]
               [?e2 :inv/name ?name]]}
     db)

(comment
 (d/datoms db {:index :eavt})
 (d/datoms db {:index :aevt})
 (d/datoms db {:index :avet})
 (d/datoms db {:index :vaet})
 )

(comment
 (pp/pprint (d/datoms db {:index :eavt :components [:inv/color]}))
 (pp/pprint (d/datoms db {:index :aevt :components [:inv/color]}))
 (pp/pprint (d/datoms db {:index :avet :components [:inv/color]}))
 (pp/pprint (d/datoms db {:index :vaet :components [:inv/color]}))
 )

(comment
 (pp/pprint (->> (d/datoms db {:index :eavt :components [:inv/color]}) (map (juxt :e :a :v))))
 (pp/pprint (->> (d/datoms db {:index :aevt :components [:inv/color]}) (map (juxt :e :a :v))))
 (pp/pprint (->> (d/datoms db {:index :avet :components [:inv/color]}) (map (juxt :e :a :v))))
 )

(comment
 (pp/pprint (d/datoms db {:index :eavt :components [:inv/color]}))
 (pp/pprint (d/datoms db {:index :eavt :components [:db/ident]}))
 (d/index-range db {:attrid :inv/color})
 )
