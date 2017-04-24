(ns namespacefy.impl.helpers
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(declare namespacefy)
(declare unnamespacefy)

(defn- throw-exception [message]
  #?(:cljs (throw (js/Error. message))
     :clj  (throw (IllegalArgumentException. message))))


(defn- namespacefy-keyword [data {:keys [ns] :as options}]
  (keyword (str (name ns) "/" (name data))))

(defn- unnamespacefy-keyword [keyword-to-be-modified]
  (keyword (name keyword-to-be-modified)))

(defn- validate-map-to-be-unnamespacefyed [map-x]
  (when-not (map? map-x) (throw-exception "Argument must be a map"))
  (let [all-keywords (set (keys map-x))
        unnamespaced-keywords (set (map unnamespacefy-keyword all-keywords))]
    (when (not= (count all-keywords) (count unnamespaced-keywords))
      (throw-exception "Unnamespacing would result a map with more than one keyword with the same name."))))

(defn- validate-map-to-be-namespacefyed [map-x options]
  (when-not (map? map-x) (throw-exception "Argument must be a map"))
  (when-not (:ns options) (throw-exception "Must provide default namespace")))

(defn- original-keys->namespaced-keys [original-keys options]
  (apply merge (map
                 #(-> {% (namespacefy-keyword % options)})
                 original-keys)))

(defn- namespacefy-map [map-x {:keys [ns except custom inner] :as options}]
  (validate-map-to-be-namespacefyed map-x options)
  (let [except (or except #{})
        custom (or custom {})
        inner (or inner {})
        keys-to-be-modified (filter (comp not except) (keys map-x))
        original-keyword->namespaced-keyword (original-keys->namespaced-keys keys-to-be-modified
                                                                             options)
        namespacefied-inner-maps (apply merge (map
                                                #(-> {% (namespacefy (% map-x) (% inner))})
                                                (keys inner)))
        inner-keys-in-map-x (set (filter #((set (keys map-x)) %) (keys inner)))
        map-x-with-modified-inner-maps (merge map-x (select-keys namespacefied-inner-maps inner-keys-in-map-x))
        final-rename-logic (merge original-keyword->namespaced-keyword custom)]
    (set/rename-keys map-x-with-modified-inner-maps final-rename-logic)))

(defn namespacefy [data options]
  (cond
    (keyword? data)
    (namespacefy-keyword data options)

    (map? data)
    (namespacefy-map data options)

    (vector? data)
    (mapv #(namespacefy-map % options) data)

    :default
    (throw-exception "Unsupported data, cannot namespacefy.")))

(defn- original-keys>unnamespaced-keys [original-keys]
  (apply merge (map
                 #(-> {% (unnamespacefy-keyword %)})
                 original-keys)))

(defn- unnamespacefy-map
  [map-x {:keys [except recur?] :as options}]
  (validate-map-to-be-unnamespacefyed map-x)
  (let [except (or except #{})
        recur? (or recur? false)
        keys-to-be-modified (filter (comp not except) (keys map-x))
        original-keyword->unnamespaced-keyword (original-keys>unnamespaced-keys keys-to-be-modified)
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
   (cond
     (keyword? data)
     (unnamespacefy-keyword data)

     (map? data)
     (unnamespacefy-map data options)

     (vector? data)
     (mapv #(unnamespacefy-map % options) data)

     :default
     (throw-exception "Unsupported data, cannot unnamespacefy."))))

(defn get-un [map-x key]
  (when map-x
    (validate-map-to-be-unnamespacefyed map-x)
    (when-not (keyword? key) (throw-exception "Key must be a key"))
    (let [all-keys (keys map-x)
          best-match (first (filter #(= (unnamespacefy %) key) all-keys))]
      (best-match map-x))))

(defn assoc-un [map-x key data]
  (if (nil? map-x)
    (assoc map-x key data)
    (do
      (validate-map-to-be-unnamespacefyed map-x)
      (when-not (keyword? key) (throw-exception "Key must be a key"))
      (let [all-keys (keys map-x)
            best-match (first (filter #(= (unnamespacefy %) key) all-keys))]
        (assoc map-x best-match data)))))