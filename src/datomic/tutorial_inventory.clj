(ns datomic.tutorial-inventory
  (:require [datomic.client.api :as d]
            [clojure.pprint :as pp]))

;; https://docs.datomic.com/cloud/tutorial/assertion.html
;; https://docs.datomic.com/cloud/tutorial/read.html
;; https://docs.datomic.com/cloud/tutorial/accumulate.html
;; https://docs.datomic.com/cloud/tutorial/read-revisited.html

(defn make-idents [x] (mapv #(hash-map :db/ident %) x))

(def cfg {:server-type :dev-local
          :storage-dir :mem
          :system "datomic-tutorial"})

(def client (d/client cfg))
(def db-name "inventory")
(d/delete-database client {:db-name db-name})
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

;;--------------------------------------------------------------------------------
(def sizes [:small :medium :large :xlarge])
(def types [:shirt :pants :dress :hat])
(def colors [:red :green :blue :yellow])

(def schema-1
  [{:db/ident :inv/sku
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :inv/color
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :inv/size
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :inv/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def order-schema
  [{:db/ident :order/items
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}
   {:db/ident :item/id
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :item/count
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

;; --------------------------------------------------------------------------------
;; Aux functions
(defn create-sample-data
  [colors sizes types]
  "Create a vector of maps of all permutations of args"
  (->> (for [color colors size sizes type types]
         {:inv/color color
          :inv/size size
          :inv/type type})
       (map-indexed
        (fn [idx map]
          (assoc map :inv/sku (str "SKU-" idx))))
       vec))

(def sample-data (create-sample-data colors sizes types))
;;(pp/pprint sample-data)

(def add-order
  {:order/items
   [{:item/id [:inv/sku "SKU-5"]
     :item/count 10}
    {:item/id [:inv/sku "SKU-6"]
     :item/count 20}]})

;; --------------------------------------------------------------------------------
;; Transactions
(def sizes-tx (d/transact conn {:tx-data (make-idents sizes)}))
(def colors-tx (d/transact conn {:tx-data (make-idents colors)}))
(def types-tx (d/transact conn {:tx-data (make-idents types)}))
(def schema-tx (d/transact conn {:tx-data schema-1}))
(def order-schema-tx (d/transact conn {:tx-data order-schema}))
(def sample-data-tx (d/transact conn {:tx-data sample-data}))
(def add-order-tx (d/transact conn {:tx-data [add-order]}))

(comment
 (pp/pprint sizes-tx)
 (pp/pprint colors-tx)
 (pp/pprint types-tx)
 (pp/pprint schema-tx)
 (pp/pprint order-schema-tx)
 (pp/pprint sample-data-tx)
 (pp/pprint add-order-tx)
 )

;; --------------------------------------------------------------------------------
;; Read
(def db (d/db conn))

(d/pull (d/db conn)
        [{:inv/color [:db/ident]}
         {:inv/size [:db/ident]}
         {:inv/type [:db/ident]}]
        [:inv/sku "SKU-42"])

(d/pull (:db-before sample-data-tx)
        [{:inv/color [:db/ident]}
         {:inv/size [:db/ident]}
         {:inv/type [:db/ident]}]
        [:inv/sku "SKU-42"])

(d/pull (:db-after sample-data-tx)
        [{:inv/color [:db/ident]}
         {:inv/size [:db/ident]}
         {:inv/type [:db/ident]}]
        [:inv/sku "SKU-42"])

(d/q '[:find ?sku
       :where
       [?e :inv/sku "SKU-42"]
       [?e :inv/color ?color]
       [?e2 :inv/color ?color]
       [?e2 :inv/sku ?sku]]
     db)

(d/q '{:find [?sku]
       :where [[?e :inv/sku "SKU-42"]
               [?e :inv/color ?color]
               [?e2 :inv/color ?color]
               [?e2 :inv/sku ?sku]]}
     db)

;; --------------------------------------------------------------------------------
;; Accumulate
;; see order-schema and add-order

;; --------------------------------------------------------------------------------
;; Read Revisited: More Query

;; Parameterized Query
(comment
 (d/q '[:find ?sku
        :in $ ?inv
        :where
        [?item :item/id ?inv]
        [?order :order/items ?item]
        [?order :order/items ?other-item]
        [?other-item :item/id ?other-inv]
        [?other-inv :inv/sku ?sku]]
      db [:inv/sku "SKU-5"])

 (d/q '[:find ?order ?item
        :where
        [?order :order/items ?item
         ;;[?item :inv/sku ?item-x]
         ]]
      db)

 (d/q '[:find ?inv-id ?inv
        :where
        [?inv-id :inv/sku ?inv]]
      db))
