(ns namespacefy.core-test
  (:require
    [clojure.test :refer :all]
    [namespacefy.core :refer [namespacefy unnamespacefy
                              get-un assoc-un]]))

;; -- Namespacefy

(deftest namespacefy-keyword
  (is (= (namespacefy :address {:ns :product.domain}) :product.domain/address))
  (is (= (namespacefy :address {:ns :product.domain}) :product.domain/address)))

(deftest namespacefy-with-same-regular-keywords
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.person/name "Seppo"
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

(deftest namespacefy-nil
  (is (nil? (namespacefy nil {:ns :product.domain.person})))
  ;; Inner value is nil
  (is (= (namespacefy {:name "Seppo"
                       :address nil}
                      {:ns :product.domain.person
                       :inner {:address {:ns :product.domain.address}}})
         {:product.domain.person/name "Seppo"
          :product.domain.person/address nil})))

(deftest namespacefy-empty
  (is (= (namespacefy {} {:ns :product.domain.person}) {})))

(deftest namespacefy-bad-data
  (is (thrown? IllegalArgumentException (namespacefy \k {:ns :product.domain.person})))
  (is (thrown? IllegalArgumentException (namespacefy 123 {:ns :product.domain.person})))
  (is (thrown? IllegalArgumentException (namespacefy {:name "Seppo"} {}))))

;; -- Unnamespacefy

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

(deftest unnamespacefy-keyword
  (is (= (unnamespacefy :product.domain.person/address) :address))
  (is (= (unnamespacefy :address) :address)))

(deftest unnamespacefy-nil
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :product.domain.person/address nil})
         {:name "Seppo"
          :address nil}))
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :address nil})
         {:name "Seppo"
          :address nil}))
  (is (nil? (unnamespacefy nil))))

(deftest unnamespacefy-bad-data
  (is (thrown? IllegalArgumentException (unnamespacefy 123)))
  (is (thrown? IllegalArgumentException (unnamespacefy "hello")))
  (is (thrown? IllegalArgumentException (unnamespacefy \a))))

;; -- get-un

(deftest get-un-works
  ;; Basic tests
  (is (= (get-un {:product.domain.player/name "Player"} :name) "Player"))
  (is (= (get-un {:product.domain.player/name "The Task"} :name) "The Task"))
  (is (= (get-un {:name "The Task"} :name) "The Task"))

  ;; Key is not present in the map -> nil
  (is (= (get-un {:name "The Task"} :id) nil))
  (is (= (get-un {:product.domain.player/name "Player"} :id) nil))
  (is (= (get-un {:name "The Task"} "name") nil))
  (is (= (get-un {:name "The Task"} 1) nil))

  ;; If the map is nil, should always return nil
  (is (= (get-un nil 1) nil))
  (is (= (get-un nil "name") nil))
  (is (= (get-un nil :name) nil)))

(deftest get-un-works-correctly-with-bad-data
  ;; Unable to resolve the correct key
  (is (thrown? IllegalArgumentException (get-un {:product.domain.player/name "Player"
                                                 :product.domain.task/name "The Task"}
                                                :name)))
  (is (thrown? IllegalArgumentException (get-un {:product.domain.player/name "Player"
                                                 :product.domain.task/name "The Task"}
                                                123)))

  ;; Map is not a map or the keys are not keywords
  (is (thrown? IllegalArgumentException (get-un 123 :name)))
  (is (thrown? IllegalArgumentException (get-un {"1" "hello"} :name)))
  (is (thrown? IllegalArgumentException (get-un {"1" "hello"} "1")))
  (is (thrown? IllegalArgumentException (get-un {1 "hello"} :name)))
  (is (thrown? IllegalArgumentException (get-un {1 "hello"} 1)))
  (is (thrown? IllegalArgumentException (get-un 123 123))))

;; -- assoc-un

(deftest assoc-un-works
  ;; Basic tests
  (is (= (assoc-un {:product.domain.player/name "Player"} :name "Player Zero")
         {:product.domain.player/name "Player Zero"}))
  (is (= (assoc-un {:product.domain.task/name "The Task"} :name "The Task 123")
         {:product.domain.task/name "The Task 123"}))
  (is (= (assoc-un {:name "Player"} :name "Player Zero")
         {:name "Player Zero"}))

  ;; Map is nil or empty
  (is (= (assoc-un nil :name "Player Zero")
         nil))
  (is (= (assoc-un nil 1 :a)
         nil))
  (is (= (assoc-un {} 1 :a)
         {}))

  ;; Key is not present in the map
  (is (= (assoc-un {:foo :bar} :name "Seppo")
         {:foo :bar}))
  (is (= (assoc-un {:foo :bar} nil "Seppo")
         {:foo :bar}))
  (is (= (assoc-un {:foo :bar} {} {})
         {:foo :bar})))

(deftest assoc-un-works-correctly-with-bad-data
  ;; Unable to resolve the correct key
  (is (thrown? IllegalArgumentException (assoc-un {:product.domain.task/name "The Task"
                                                   :product.domain.player/name "Seppo"}
                                                  :name "Which one!?")))

  ;; Map is not a map or the keys are not keywords
  (is (thrown? IllegalArgumentException (assoc-un 123 123 "Player Zero")))
  (is (thrown? IllegalArgumentException (assoc-un [] 123 "Player Zero")))
  (is (thrown? IllegalArgumentException (assoc-un {"name" "Player"} "name" "Player Zero")))
  (is (thrown? IllegalArgumentException (assoc-un "Hello" 123 "Player Zero"))))