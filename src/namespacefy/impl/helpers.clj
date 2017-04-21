(ns namespacefy.impl.helpers
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(declare namespacefy)

(defn namespacefy-keyword [namespace-as-keyword keyword-to-be-modified]
  (assert (keyword? namespace-as-keyword) "Namespace must be a keyword.")
  (assert (keyword? keyword-to-be-modified) "Can only namespacefy keywords.")
  (keyword (str (name namespace-as-keyword) "/" (name keyword-to-be-modified))))

(defn- namespacefy-map [map-x {:keys [ns except custom inner] :as options}]
  (let [except (or except #{})
        custom (or custom {})
        inner (or inner {})
        keys-to-be-modified (filter (comp not except) (keys map-x))
        original-keyword->namespaced-keyword (apply merge (map
                                                            #(-> {% (namespacefy-keyword ns %)})
                                                            keys-to-be-modified))
        namespacefied-inner-maps (apply merge (map
                                                #(-> {% (namespacefy (% map-x) (% inner))})
                                                (keys inner)))
        inner-keys-in-map-x (set (filter #((set (keys map-x)) %) (keys inner)))
        map-x-with-modified-inner-maps (merge map-x (select-keys namespacefied-inner-maps inner-keys-in-map-x))
        final-rename-logic (merge original-keyword->namespaced-keyword custom)]
    (set/rename-keys map-x-with-modified-inner-maps final-rename-logic)))

(defn namespacefy [data options]
  (cond (map? data)
        (namespacefy-map data options)

        (vector? data)
        (mapv #(namespacefy-map % options) data)))

(declare unnamespacefy)

(defn unnamespacefy-keyword [keyword-to-be-modified]
  (assert (keyword? keyword-to-be-modified) "Can only unnamespacefy keywords.")
  (keyword (name keyword-to-be-modified)))

(defn- validate-map-to-be-unnamespaced [map-x]
  (let [all-keywords (set (keys map-x))
        unnamespaced-keywords (set (map unnamespacefy-keyword all-keywords))]
    (when (not= (count all-keywords) (count unnamespaced-keywords))
      (throw (AssertionError. "Unnamespacing would result a map with more than one keyword with the same name.")))))

(defn- unnamespacefy-map
  [map-x {:keys [except recur?] :as options}]
  (validate-map-to-be-unnamespaced map-x)
  (let [except (or except #{})
        recur? (or recur? false)
        keys-to-be-modified (filter (comp not except) (keys map-x))
        original-keyword->unnamespaced-keyword (apply merge (map
                                                              #(-> {% (unnamespacefy-keyword %)})
                                                              keys-to-be-modified))
        keys-to-inner-maps (filter (fn [avain]
                                     (let [sisalto (avain map-x)]
                                       (or (map? sisalto) (vector? sisalto))))
                                   (keys map-x))
        unnamespacefied-inner-maps (apply merge (map
                                                  #(-> {% (unnamespacefy (% map-x))})
                                                  keys-to-inner-maps))
        map-x-with-modified-inner-maps (if recur?
                                         (merge map-x unnamespacefied-inner-maps)
                                         map-x)]
    (set/rename-keys map-x-with-modified-inner-maps original-keyword->unnamespaced-keyword)))

(defn unnamespacefy
  ([data] (unnamespacefy data {}))
  ([data options]
   (cond (map? data)
         (unnamespacefy-map data options)

         (vector? data)
         (mapv #(unnamespacefy-map % options) data))))