(ns datomic.fixing-bad-data
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

;; Based on Stuff Happens: Fixing Bad Data in Datomic
;; https://blog.datomic.com/2014/08/stuff-happens-fixing-bad-data-in-datomic.html

(def cfg {:server-type :dev-local
          :system "datomic-tutorial"})
(def client (d/client cfg))
(def db-name "bad-data")
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
(def schema-tx (d/transact conn {:tx-data schema-1}))
;;(pp/pprint schema-tx)

(def sample-data-tx (d/transact conn {:tx-data [{:inv/name "adam"
                                                 ;;:inv/color "blue"
                                                 }
                                                {:inv/name "david"
                                                 :inv/color "green"}]}))
;;(pp/pprint sample-data-tx)

;; Using :db/add to add John
(d/transact conn {:tx-data
                  [[:db/add "john" :inv/name "john"]
                   [:db/add "john" :inv/color "red"]]})

;; Get all
(d/q '{:find [?e ?name ?color]
       :in [$]
       :where [[?e :inv/name ?name]
               [(get-else $ ?e :inv/color "missing color value") ?color]]}
     (d/db conn))

;; Add missing color to adam
(comment
 (let [adam-id (->> (d/q '{:find [?e]
                           :in [$ ?name]
                           :where [[?e :inv/name ?name]]}
                         (d/db conn) "adam")
                    ffirst)]
   (d/transact conn {:tx-data
                     [[:db/add adam-id :inv/color "blue"]]})))


(def db (d/db conn))

(comment
 (d/datoms db {:index :eavt})
 (d/datoms db {:index :aevt})
 (d/datoms db {:index :avet})
 )

(comment
 (pp/pprint (d/datoms db {:index :eavt :components [:inv/name]}))
 (pp/pprint (d/datoms db {:index :aevt :components [:inv/name]}))
 (pp/pprint (d/datoms db {:index :avet :components [:inv/color]}))
 )

(comment
 (pp/pprint (->> (d/datoms db {:index :eavt :components [:inv/color]}) (map (juxt :e :a :v))))
 (pp/pprint (->> (d/datoms db {:index :aevt :components [:inv/color]}) (map (juxt :e :a :v))))
 (pp/pprint (->> (d/datoms db {:index :avet :components [:inv/color]}) (map (juxt :e :a :v))))
 )
