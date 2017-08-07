(ns namespacefy.core-test
  (:require
    [clojure.test :refer :all]
    [namespacefy.core :refer [namespacefy unnamespacefy
                              get-un assoc-un]]))

;; -- Namespacefy

(deftest namespacefy-keyword
  (is (= (namespacefy :address {:ns :product.domain}) :product.domain/address))
  (is (= (namespacefy :foo {:ns :domain}) :domain/foo)))

(deftest namespacefy-with-same-regular-keywords
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.person/name "Seppo"
                                                        :product.domain.task/name "Useless task"}))))

(deftest namespacefy-map
  (is (= (namespacefy {:name "Seppo" :id 1}
                      {:ns :product.domain.person})
         {:product.domain.person/name "Seppo"
          :product.domain.person/id 1})))

(deftest namespacefy-coll
  (is (= (namespacefy '({:name "Seppo" :id 1})
                      {:ns :product.domain.person})
         '({:product.domain.person/name "Seppo"
            :product.domain.person/id 1}))))

(deftest namespacefy-set
  (is (= (namespacefy #{{:name "Seppo" :id 1}}
                      {:ns :product.domain.person})
         #{{:product.domain.person/name "Seppo"
            :product.domain.person/id 1}})))

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

(deftest namespacefy-coll-of-maps
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
                                         :product.domain.task/description "Do something useless"}]}))

  (is (= (namespacefy {:tasks '({:id 6 :description "Do something useful"} {:id 7 :description "Do something useless"})}
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/tasks '({:product.domain.task/id 6
                                          :product.domain.task/description "Do something useful"}
                                          {:product.domain.task/id 7
                                           :product.domain.task/description "Do something useless"})}))

  (is (= (namespacefy {:tasks (map #(-> {:id %}) [1 2 3])} ;; Test lazy sequence
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/tasks [{:product.domain.task/id 1}
                                        {:product.domain.task/id 2}
                                        {:product.domain.task/id 3}]}))

  (is (= (namespacefy {:tasks #{{:id 6 :description "Do something useful"}
                                {:id 7 :description "Do something useless"}}}
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/tasks #{{:product.domain.task/description "Do something useful"
                                          :product.domain.task/id 6}
                                         {:product.domain.task/description "Do something useless"
                                          :product.domain.task/id 7}}})))

(deftest namespacefy-coll-things
  ;; Coll of colls
  (is (= (namespacefy {:tasks [[{:id 6 :description "Do something useful"}
                                {:id 7 :description "Do something useless"}]
                               [{:id 6 :description "Do something useful"}
                                {:id 7 :description "Do something useless"}]]}
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/tasks [[{:product.domain.task/id 6
                                          :product.domain.task/description "Do something useful"}
                                         {:product.domain.task/id 7
                                          :product.domain.task/description "Do something useless"}]
                                        [{:product.domain.task/id 6
                                          :product.domain.task/description "Do something useful"}
                                         {:product.domain.task/id 7
                                          :product.domain.task/description "Do something useless"}]]}))
  ;; Coll of keywords should not do anything
  (is (= (namespacefy {:tasks [:one :two :three]}
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/tasks [:one :two :three]}))

  ;; Coll of nils should not do anything
  (is (= (namespacefy {:tasks [nil nil nil]}
                      {:ns :product.domain.person
                       :inner {:tasks {:ns :product.domain.task}}})
         {:product.domain.person/tasks [nil nil nil]}))

  ;; Coll of multiple types
  (let [object (new Object)]
    (is (= (namespacefy {:tasks [nil :keyword object]}
                        {:ns :product.domain.person
                         :inner {:tasks {:ns :product.domain.task}}})
           {:product.domain.person/tasks [nil :keyword object]}))))

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
         {:name "Seppo" :id 1}))
  (is (= (unnamespacefy {:name "Seppo"
                         :id 1})
         {:name "Seppo" :id 1}))
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :id 1})
         {:name "Seppo" :id 1})))

(deftest unnamespacefy-coll
  (is (= (unnamespacefy '({:product.domain.person/name "Seppo"
                         :product.domain.person/id 1}))
         '({:name "Seppo" :id 1}))))

(deftest unnamespacefy-set
  (is (= (unnamespacefy #{{:product.domain.person/name "Seppo"
                           :product.domain.person/id 1}})
         #{{:name "Seppo" :id 1}})))

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
         {:name "Seppo" :product.domain.player/id 666 :task {:id 6}}))

  (is (= (unnamespacefy {:stuff {:product.domain.player/name "Seppo"
                                 :product.domain.task/name "Important task"}})
         {:stuff {:product.domain.player/name "Seppo"
                  :product.domain.task/name "Important task"}})
      "No conflicts occur since recur is not used"))

(deftest unnamespacefy-coll-of-maps
  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 1
                         :product.domain.player/tasks [{:product.domain.task/id 6}
                                                       {:product.domain.task/id 7}]}
                        {:recur? true})
         {:name "Seppo" :id 1 :tasks [{:id 6}
                                      {:id 7}]}))

  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 1
                         :product.domain.player/tasks '({:product.domain.task/id 6}
                                                         {:product.domain.task/id 7})}
                        {:recur? true})
         {:name "Seppo" :id 1 :tasks '({:id 6}
                                        {:id 7})}))

  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 1
                         :product.domain.player/tasks #{{:product.domain.task/id 6}
                                                        {:product.domain.task/id 7}}}
                        {:recur? true})
         {:name "Seppo" :id 1 :tasks #{{:id 6}
                                       {:id 7}}}))

  (is (= (unnamespacefy {:product.domain.player/name "Seppo"
                         :product.domain.player/id 1
                         :product.domain.player/tasks (map #(-> {:product.domain.task/id %}) [6 7])}
                        {:recur? true})
         {:name "Seppo" :id 1 :tasks [{:id 6}
                                      {:id 7}]})))

(deftest unnamespacefy-coll-of-things
  ;; Coll of colls
  (is (= (unnamespacefy {:product.domain.player/tasks [[{:product.domain.task/id 6}
                                                        {:product.domain.task/id 7}]
                                                       [{:product.domain.task/id 6}
                                                        {:product.domain.task/id 7}]]}
                        {:recur? true})
         {:tasks [[{:id 6}
                   {:id 7}]
                  [{:id 6}
                   {:id 7}]]}))

  ;; Coll of keywords should not do anything
  (is (= (unnamespacefy {:product.domain.player/tasks [:one :two :three]}
                        {:recur? true})
         {:tasks [:one :two :three]}))

  ;; Coll of nils
  (is (= (unnamespacefy {:product.domain.player/tasks [nil nil nil]}
                        {:recur? true})
         {:tasks [nil nil nil]}))

  ;; Coll of multiple types
  (let [object (new Object)]
    (is (= (unnamespacefy {:product.domain.player/tasks [nil object :omg]}
                          {:recur? true})
           {:tasks [nil object :omg]}))))

(deftest unnamespacefy-keyword
  (is (= (unnamespacefy :product.domain.person/address) :address))
  (is (= (unnamespacefy :address) :address)))

(deftest unnamespacefy-resolving-conflicts
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :product.domain.task/name "Important task"}
                        {:custom {:product.domain.person/name :person-name
                                  :product.domain.task/name :task-name}})
         {:person-name "Seppo"
          :task-name "Important task"}))
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :name "foo"}
                        {:custom {:product.domain.person/name :person-name}})
         {:person-name "Seppo"
          :name "foo"}))
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :product.domain.task/name "Important task"
                         :product.domain.person/score 666}
                        {:custom {:product.domain.person/name :person-name
                                  :product.domain.task/name :task-name}})
         {:person-name "Seppo"
          :task-name "Important task"
          :score 666}))
  (is (= (unnamespacefy {:product.domain.person/name "Seppo"
                         :product.domain.task/name "Important task"
                         :name "foo"}
                        {:custom {:product.domain.person/name :person-name
                                  :product.domain.task/name :task-name}})
         {:person-name "Seppo"
          :task-name "Important task"
          :name "foo"})))

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
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.player/name "Seppo"
                                                        :product.domain.task/name "Important task"})))
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.player/name "Seppo"
                                                        :name "foo"})))
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.player/name "Seppo"
                                                        :product.domain.task/name "Important task"}
                                                       {:custom nil})))
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.player/name "Seppo"
                                                        :product.domain.task/name "Important task"
                                                        :product.domain.player/id 1
                                                        :product.domain.task/id 2}
                                                       ;; Resolve :name conflict, but there is still :id
                                                       {:custom {:product.domain.player/name :person-name
                                                                 :product.domain.task/name :task-name}})))
  (is (thrown? IllegalArgumentException (unnamespacefy {:product.domain.player/name "Seppo"
                                                        :product.domain.task/name "Important task"}
                                                       {:custom 123})))
  (is (thrown? IllegalArgumentException (unnamespacefy {:stuff {:product.domain.player/name "Seppo"
                                                                :product.domain.task/name "Important task"}}
                                                       {:recur? true})))
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