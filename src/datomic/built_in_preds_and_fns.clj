(ns datomic.built-in-preds-and-fns
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

;;https://docs.datomic.com/cloud/query/query-data-reference.html

(def cfg {:server-type :dev-local
          :system "datomic-tutorial"})
(def client (d/client cfg))
(def db-name "built-in")
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Built in predicates and functions
(def db (d/db conn))

;; get-some
(d/q '{:find [?name ?color]
       :in [$ [?name ...]]
       :where [[?e :inv/name ?name]
               [(get-else $ ?e :inv/color "missing color value") ?color]]}
     db ["adam" "david"])
;; Or
(d/q '{:find [?name ?color]
       :in [$]
       :where [[?e :inv/name ?name]
               [(get-else $ ?e :inv/color "missing color value") ?color]]}
     db)

;; missing
(d/q '{:find [?name ?color]
       :in [$]
       :where [[?e :inv/name ?name]
               [(missing? $ ?e :inv/color) ?color]]}
     db)

;; get-some
(d/q '{:find [?e ?attr ?name ?val]
       :in [$]
       :where [[?e :inv/name ?name]
               [(get-some $ ?e :inv/color :inv/name) [?attr ?val]]]}
     db)

;; ground
(d/q '{:find [?name ?val]
       :in [$]
       :where [[?e :inv/name ?name]
               [(ground [:a :b]) [?val ...]]]}
     db)

;; tuple
(d/q '{:find [?names]
       :in [$ [?a ?b]]
       :where [[(tuple $ ?a ?b) ?names]]}
     db ["adam" "david"])
