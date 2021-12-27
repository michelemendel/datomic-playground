(ns datomic.misc
  (:require [instaparse.core :as insta]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def gremlin-parser
  (insta/parser (clojure.java.io/resource "gremlin.ebnf")))


(def as-and-bs
  (insta/parser
   "S = AB*
    AB = A B
    A = 'a'+
    B = 'b'+"))

(comment
 (insta/parse as-and-bs "aabbbaab")

 (let [pd (as-and-bs "aabbbaab" :total true)]
   (cond-> pd (insta/failure? pd) insta/get-failure))

 (let [pd (as-and-bs "aabbbaab" :total true)]
   (insta/transform {:A (fn [a b] [:FIRST a a])} pd)))

;;(insta/visualize (as-and-bs "aabbbaab"))














(let [q "g.V()
.hasLabel(\"Objective\")
.not(inE('Strategy Is Addressed By'))"
      q "//The script calculates the sum of 'Total Direct Cost' for all Live applications
       //PT

       g.V().hasLabel('Application')
       .has('lifecycle_phase','Live')
       .values('total_direct_cost')
       .sum()
       .math('_/1000000') // to show the figure in Millions
       //.math('ceil _')"

      q "def deno = g.V().hasLabel(\"Application\")\r
       .has('tags','HR')\r
       .has('rationalization_assessment','Retire')\r
       .count().next()\r
       \r
       def nume = g.V().hasLabel(\"Application\")\r
       .has('tags','HR')\r
       .has('rationalization_assessment','Retire')\r
       .has('lifecycle_phase','Live')\r
       .count().next()\r
       \r
       def percr = (nume / deno)*100\r
       def fina = Math.round(percr)\r
       \r
       //.math('_ / nume')\r
       //math('_*100')"]

  (-> q
      (str/replace #"\"" "'")
      (str/replace #"\s+" " ")
      (str/replace #"\s\." ".")
      ))
