(ns datomic.client-api-tutorial
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

(defn reset-test-db
  "Reset test database"
  [cfg db-name schema ident-genres test-data]
  (let [client (d/client cfg)
        _ (d/delete-database client {:db-name db-name})
        _ (d/create-database client {:db-name db-name})
        conn (d/connect client {:db-name db-name})
        schema-tx (d/transact conn {:tx-data schema})
        ident-genres-tx (d/transact conn {:tx-data ident-genres})
        test-data-tx (d/transact conn {:tx-data test-data})]
    {:test-data-tx test-data-tx :client client :conn conn}))

(defn init-test-db
  "Initialize test database"
  [cfg db-name]
  (let [client (d/client cfg)
        conn (d/connect client {:db-name db-name})]
    {:test-data-tx nil :client client :conn conn}))

(def system "datomic-tutorial")
(def db-name "movies")

(def cfg {:server-type :dev-local
          :system system})


(def movie-schema [{:db/ident :movie/uid
                    :db/valueType :db.type/keyword
                    :db/cardinality :db.cardinality/one
                    :db/unique :db.unique/identity
                    :db/doc "A UID"}

                   {:db/ident :movie/title
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one
                    :db/doc "The title of the movie"}

                   {:db/ident :movie/genre
                    :db/valueType :db.type/ref
                    :db/cardinality :db.cardinality/one
                    :db/doc "The genre of the movie"}

                   {:db/ident :movie/release-year
                    :db/valueType :db.type/long
                    :db/cardinality :db.cardinality/one
                    :db/doc "The year the movie was released in theaters"}])

;; Assertion - a single atomic fact
(defn make-idents [x] (mapv #(hash-map :db/ident %) x))
(def ident-genres (make-idents [:action :adventure :thriller :punk-dystopia]))
(def test-data [{:movie/uid :the-goonies
                 :movie/title "The Goonies"
                 :movie/genre :action
                 :movie/release-year 1985}
                {:movie/uid :commando
                 :movie/title "Commando"
                 :movie/genre :thriller
                 :movie/release-year 1985}
                {:movie/uid :repo_man
                 :movie/title "Repo Man"
                 :movie/genre :punk-dystopia
                 :movie/release-year 1984}])

;;Initialize or reset database when evaluating this namespace
;;(def db-conf (reset-test-db cfg db-name movie-schema ident-genres test-data))
(def db-conf (init-test-db cfg db-name))
;; List Databases in a System
(defn list-dbs [db-conf] (pp/pprint (d/list-databases (:client db-conf) {})))
;;(list-dbs db-conf)
;; Create a Database
(defn create-db [db-conf] (d/create-database (:client db-conf) {:db-name db-name}))
;; Delete a Database
(defn delete-db [db-conf] (d/delete-database (:client db-conf) {:db-name db-name}))
;;(delete-db db-conf)
;;(create-db db-conf)
;;(def client (:client db-conf))
(def conn (:conn db-conf))

;; Queries
(def titles '{:find [?e ?v] :where [[?e :movie/title ?v]]})
(def genres '{:find [?e ?v] :where [[?e :movie/genre ?id]
                                    [?id :db/ident ?v]]})
(def years '{:find [?e ?v] :where [[?e :movie/release-year ?v]]})
(def all-data '{:find [?e ?uid ?title ?genre ?year]
                :in [$]
                :where [[?e :movie/uid ?uid]
                        [?e :movie/title ?title]
                        [?e :movie/genre ?genre-id]
                        [?e :movie/release-year ?year]
                        [?genre-id :db/ident ?genre]]})
(def get-movie-by-title '{:find [?e ?title ?genre ?year]
                          :in [$ ?title]
                          :where [[?e :movie/title ?title]
                                  [?e :movie/genre ?genre-id]
                                  [?e :movie/release-year ?year]
                                  [?genre-id :db/ident ?genre]]})
(def get-movie-id-by-title '[:find ?e
                             :in $ ?title
                             :where [?e :movie/title ?title]])

;; Playground
(-> db-conf :test-data-tx :tempids)
(-> db-conf :test-data-tx :tempids)
(:test-data-tx db-conf)

(comment
 ;;(d/transact conn {:tx-data movie-schema})
 ;;(d/transact conn {:tx-data test-data})
 (d/transact conn {:tx-data [{:movie/uid :johnny
                              :movie/title "Johnny"
                              :movie/genre :thriller
                              :movie/release-year 1984}]})
 ;;(d/transact conn {:tx-data [[:db/retract 87960930222160 :movie/genre ""]]})
 ;;(d/transact conn {:tx-data [[:db/retractEntity [:movie/uid :johnny]]]})

 ;; Rename
 (d/transact conn {:tx-data [[:db/retract 79164837199949 :movie/title "Commando David"]
                             [:db/add 79164837199949 :movie/title "Commando"]]})
 )

(comment ;; Commando
 (let [pull-query-1 [:movie/title {:movie/genre [:db/ident]}]
       pull-query-2 [:movie/title :movie/genre]]
   (pp/pprint (d/pull (d/db conn) pull-query-1 (ffirst (d/q get-movie-id-by-title (d/db conn) "Commando"))))
   ;;                                   Since :movie/uid is unique
   (pp/pprint (d/pull (d/db conn) pull-query-2 [:movie/uid :commando]))))

(comment
 (do
   ;;(println "Titles") (pp/pprint (d/q {:query titles :args [(d/db conn)]}))
   ;;(println "Years") (pp/pprint (d/q {:query years :args [(d/db conn)]}))
   ;;(println "Genres") (pp/pprint (d/q {:query genres :args [(d/db conn)]}))
   (println "Commando") (pp/pprint (d/q get-movie-by-title (d/db conn) "Commando"))
   ;;(println "All data") (pp/pprint (d/q {:query all-data :args [(d/db conn)]}))
   ))

;; Raw index
(comment
 (d/datoms (d/db conn) {:index :eavt})
 (d/datoms (d/db conn) {:index :aevt})
 (d/datoms (d/db conn) {:index :avet})
 (d/datoms (d/db conn) {:index :vaet})
 (pp/pprint (d/datoms (d/db conn) {:index :eavt :components [:movie/title]}))
 (pp/pprint (d/datoms (d/db conn) {:index :aevt :components [:movie/title]}))
 (pp/pprint (d/datoms (d/db conn) {:index :avet :components [:movie/title]}))
 (pp/pprint (d/datoms (d/db conn) {:index :vaet :components [:movie/title]}))
 )

