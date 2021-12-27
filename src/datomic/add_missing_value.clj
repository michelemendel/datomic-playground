(ns datomic.add_missing_value
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

(def cfg {:server-type :dev-local
          :system "datomic-tutorial"})
(def client (d/client cfg))
(def db-name "missing")
(d/delete-database client {:db-name db-name})
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(defn print-tx
  [tx]
  tx
  #_(->> (:tx-data tx) next (map (juxt :e :a :v :tx :added))))

(def schema-1
  [{:db/ident :inv/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :inv/color
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   ])
(def schema-tx (d/transact conn {:tx-data schema-1}))

(def sample-data-tx (d/transact conn {:tx-data [{:inv/name "adam"}
                                                {:inv/name "david" :inv/color "green"}]}))

;; Using :db/add to add John
#_(def john-tx (d/transact conn {:tx-data
                                 [[:db/add "john" :inv/name "john"]
                                  [:db/add "john" :inv/color "red"]]}))

(defn get-id
  [val]
  (->> (d/q '{:find [?e ?val]
              :in [$ ?val]
              :where [[?e :inv/name ?val]]}
            (d/db conn) val)
       ffirst))

(defn set-color
  [name color]
  (d/transact conn {:tx-data
                    [[:db/add (get-id name) :inv/color color]]}))

;; Add Adam's missing color
(def tx1 (set-color "adam" "blue"))
;; Change Adam's color
(def tx2 (set-color "adam" "black"))

(print-tx sample-data-tx)
(print-tx tx1)
(print-tx tx2)

;; tx-range (log API)
(d/tx-range conn {:start 7 :end nil})
;; as-of (time filters)
(d/pull (d/as-of (d/db conn) 8) '[*] (get-id "adam"))
;; history (time filters)
(d/history (d/db conn))
;; index-pull
()

(d/q '{:find [?e ?name ?color]
       :in [$]
       :where [[?e :inv/name ?name]
               [(get-else $ ?e :inv/color "missing color value") ?color]]}
     (d/db conn)
     ;;(d/as-of (d/db conn) 8)
     ;;(d/history (d/db conn))
     )

(d/q '{:find
       ;;[?e-str ?a-str ?v]
       [?e ?e-name ?a ?a-name ?v #_?inst]
       :in [$]
       :where [[?e ?a ?v ?tx true]
               ;;[?added :db/ident ?op]
               [?tx :db/txInstant ?inst]
               [(get-else $ ?e :db/ident "-") ?e-name]
               ;;(not [?a :db/ident :db/doc])
               [?a :db/ident ?a-name]
               [(vector :e ?e ?e-name) ?e-str]
               [(vector :a ?a ?a-name) ?a-str]]}
     ;;(d/db conn)
     ;;(d/as-of (d/db conn) 8)
     (d/history (d/db conn))
     )

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




;;--------------------------------------------------------------------------------
;; Slack
;; https://clojurians.slack.com/archives/C03RZMDSH/p1639503640104800
;; Don't know what he wanted to show with this

(let [db (d/db conn)
      history-db (d/history db)
      a->a-name (->> (d/q '[:find ?a-name ?a
                             :where [?a :db/ident ?a-name]]
                           db)
                     (into {}))]
  #_(pp/pprint (->> (take 1 [[1 2 3] [:a :b :c]]) #_(take 1 (d/datoms history-db {:index :eavt}))
                    (map #(get % 1))))
  (pp/pprint (->> a->a-name (take 2) (into {})))

  #_(map (fn [d] (->> d ((juxt :e :a :v))))
       (take 10 (d/datoms history-db {:index :eavt})))

  #_(map (fn [d] (update d 1 a->a-name))
         (->> (d/datoms history-db {:index :eavt})
              (take 1)))
  )

#_(let [db (d/db conn)
      history-db (d/history db)
      a->a-name (->> (d/q '[:find ?a ?a-name
                            :where [?a :db/ident ?a-name]]
                          db)
                     (into {}))]
  (->> a->a-name
       (map (fn [d] (update d 1 (seq (d/datoms history-db {:index :eavt})))))))



