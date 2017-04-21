(ns namespacefy.core-test
  (:require
    [clojure.test :refer :all]
    [namespacefy.core :refer [namespacefy unnamespacefy]]))

(deftest namespacefy-keyword
  (is (= (namespacefy :address {:ns :product.domain}) :product.domain/address))
  (is (= (namespacefy :address {:ns :product.domain}) :product.domain/address)))

(deftest unnamespacefy-keyword
  (is (= (unnamespacefy :product.domain.person/address) :address))
  (is (= (unnamespacefy :address) :address)))

(deftest namespacefy-with-same-regular-keywords
  (is (thrown? AssertionError (unnamespacefy {:product.domain.person/name "Seppo"
                                              :product.domain.task/name "Useless task"}))))

(deftest namespacefy-simple-map
  (is (= (namespacefy {:name "Seppo" :id 1}
                      {:ns :product.domain.person})
         {:product.domain.person/name "Seppo"
          :product.domain.person/id 1})))

(deftest namespacefy-nested-map
  (is (= (namespacefy {:name "Seppo"
                       :id 1
                       :address {:address "Pihlajakatu 23"
                                 :city "Helsinki"
                                 :country "Finland"}
                       :foo nil
                       :modified nil}
                      {:ns :product.domain.person
                       :except #{:foo}
                       :custom {:modified :our.ui/modified}
                       :inner {:address {:ns :product.domain.address}}})
         {:product.domain.person/name "Seppo"
          :product.domain.person/id 1
          :product.domain.person/address {:product.domain.address/address "Pihlajakatu 23"
                                          :product.domain.address/city "Helsinki"
                                          :product.domain.address/country "Finland"}
          :foo nil
          :our.ui/modified nil})))

(deftest namespacefy-deeply-nested-map
  (is (= (namespacefy {:name "Seppo"
                       :id 1
                       :address {:address "Pihlajakatu 23"
                                 :city "Helsinki"
                                 :country "Finland"
                                 :gps-location {:x 0
                                                :y 0}}}
                      {:ns :product.domain.person
                       :inner {:address {:ns :product.domain.address
                                         :inner {:gps-location {:ns :product.domain.gps}}}}})
         {:product.domain.person/name "Seppo"
          :product.domain.person/id 1
          :product.domain.person/address {:product.domain.address/address "Pihlajakatu 23"
                                          :product.domain.address/city "Helsinki"
                                          :product.domain.address/country "Finland"
                                          :product.domain.address/gps-location {:product.domain.gps/x 0
                                                                                :product.domain.gps/y 0}}}))

  (is (= (namespacefy {:name "Seppo"
                       :id 1
                       :address {:address "Pihlajakatu 23"
                                 :city "Helsinki"
                                 :country "Finland"
                                 :gps-location {:x 0
                                                :y 0}}
                       :foo nil
                       :modified nil}
                      {:ns :product.domain.person
                       :except #{:foo}
                       :custom {:modified :our.ui/modified}
                       :inner {:address {:ns :product.domain.address
                                         :inner {:gps-location {:ns :product.domain.gps}}}}})
         {:product.domain.person/name "Seppo"
          :product.domain.person/id 1
          :product.domain.person/address {:product.domain.address/address "Pihlajakatu 23"
                                          :product.domain.address/city "Helsinki"
                                          :product.domain.address/country "Finland"
                                          :product.domain.address/gps-location {:product.domain.gps/x 0
                                                                                :product.domain.gps/y 0}}
          :foo nil
          :our.ui/modified nil})))

(deftest namespacefy-vector-of-maps
  (is (= (namespacefy {:name "Seppo"
                       :id 1
                       :tasks [{:id 6 :description "Do something useful"}
                               {:id 7 :description "Do something useless"}]}
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/name "Seppo"
          :product.domain.person/id 1
          :product.domain.person/tasks [{:product.domain.task/id 6
                                         :product.domain.task/description "Do something useful"}
                                        {:product.domain.task/id 7
                                         :product.domain.task/description "Do something useless"}]})))

(deftest namespacefy-empty
  (is (= (namespacefy {} {:ns :product.domain.person}) {})))

(deftest unnamespacefy-simple-map
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :product.domain.person/id 1})
         {:name "Seppo" :id 1})))

(deftest unnamespacefy-nested-map
  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 1
                         :product.domain.player/tasks {:product.domain.task/id 6}})
         {:name "Seppo" :id 1 :tasks {:product.domain.task/id 6}}))

  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 666
                         :product.domain.player/tasks {:product.domain.task/id 6}}
                        {:except #{:product.domain.player/id}})
         {:name "Seppo" :product.domain.player/id 666 :tasks {:product.domain.task/id 6}}))

  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 666
                         :product.domain.player/task {:product.domain.task/id 6}}
                        {:recur? true :except #{:product.domain.player/id}})
         {:name "Seppo" :product.domain.player/id 666 :task {:id 6}})))

(deftest namespacefy-bad-data
  (is (thrown? AssertionError (namespacefy nil {:ns :product.domain.person})))
  (is (thrown? AssertionError (namespacefy 123 {:ns :product.domain.person})))
  (is (thrown? AssertionError (namespacefy {:name "Seppo"} {}))))

(deftest unnamespacefy-bad-data
  (is (thrown? AssertionError (unnamespacefy nil)))
  (is (thrown? AssertionError (unnamespacefy 123))))