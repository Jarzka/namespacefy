(ns namespacefy.core
  (:require [namespacefy.impl.helpers :as helpers]))

(defn namespacefy
  "The data can be a map, a vector of maps or a keyword. In all cases, it adds a namespace
   to the keywords.

   Options is a map with the following keywords:
   :ns        Keyword, which provides the default namespace to be used (if exceptions are not provided).
              If data is a keyword, this is the only option which is taken into account.
   :except    Set of keywords. The name of the keywords are not modified.
   :custom    Map of keywords present in the given data. These keywords are named differently.
   :inner     Map, which provides options for namespacefying inner maps and vectors.

  Example:
  
  (def data {:name \"player1\" :id 1 :tasks {:id 666 :time 5} :points 7 :foobar nil})

  (namespacefy data {:ns :product.domain.player
                     :except #{:foobar}
                     :custom {:points :product.domain.point/points}
                     :inner {:tasks {:ns :product.domain.task}}})
  => {:product.domain.player/name \"player1\"
      :product.domain.player/ip 1
      :product.domain.player/tasks {:product.domain.task/id 666
                                    :product.domain.task/time 5}
      :product.domain.point/points 7
      :foobar nil}"
  [data options]
  (helpers/namespacefy data options))

(defn unnamespacefy
  "Converts namespaced keywords to regular keywords. The data can be a map, vector of maps or a
   keyword.

  Options is a map with the following keywords:
  except        Set of keywords. The name of the keywords are not modified.
  :recur?       Unnamespacefy all keywords recursively (all nested maps and vector of maps are touched)"
  ([data] (unnamespacefy data {}))
  ([data options]
   (helpers/unnamespacefy data options)))

(defn get-un
  "Returns the key which matches to the given keyword after unnamespacefying.
   The keys of the given map should be keywords and there should be only one
   possible match, otherwise an exception is thrown.

   This helper can be useful if you want to build a function which supports multiple
   namespaced keywords that contain the same regular part. For example, getting the name of both
   :product.domain.player/name and :product.domain.task/name is easy with get-un."
  [map key]
  (helpers/get-un map key))

(defn assoc-un
  "Assocs the data to the given keyword which matches the corresponding namespaced or
   unnamespaced keyword. The keys of the given map should be keywords and
   there should be only one possible match, otherwise an exception is thrown.

   If the key is not present in the map, returns the original map."
  [map key data]
  (helpers/assoc-un map key data))