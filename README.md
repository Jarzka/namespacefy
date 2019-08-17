<p align="center">
  <img align="center" width="320" src="namespacefy_logo.png" alt="">
</p>

<p align="center">Namespacing Clojure(Script) data with ease.</p>

[![Clojars Project](https://img.shields.io/clojars/v/namespacefy.svg)](https://clojars.org/namespacefy)  
[![CircleCI](https://circleci.com/gh/Jarzka/namespacefy.svg?style=shield)](https://circleci.com/gh/Jarzka/namespacefy)

[API documentation](https://jarzka.github.io/namespacefy/docs/)

namespacefy is a small and simple Clojure(Script) library which aims to make it easy to keep map keys namespaced, no matter where your data comes from.

# Introduction

When data is fetched from a database or received from an external system, the output is often a map or a collection of maps with unnamespaced keywords. This is often the desired end result. However, it is often recommended to use namespaced keywords in Clojure to avoid name conflicts and to make the meaning of keywords more clear. This library aims to solve this problem by providing a simple helper function for keyword namespacing. When the namespacing is not needed anymore (for example you want to send your data into an external system as JSON), unnamespacing the map can be done easily with another simple function call.

# Installation

Add the following line into your Leiningen project:

```clj
[namespacefy "0.5.0"]
```

# Usage

## Require

```clj
(:require [namespacefy.core :refer [namespacefy unnamespacefy]])
```

## Namespacefy

You can namespacefy keywords, maps or collection types with a single **namespacefy** function:

```clojure
(def data {:name "Seppo"
           :id 1
           :tasks {:id 666 :time 5}
           :points 7
           :foobar nil})

(namespacefy data 
  {:ns :product.domain.player ; Base namespace for all keywords
   :except #{:foobar} ; Exceptions, do not namespacefy these keywords
   :custom {:points :product.domain.point/points} ; Namespacefy these keywords differently
   :inner {:tasks {:ns :product.domain.task}}}) ; How to handle keywords that contain collections or maps

;; We get the following output:
;; {:product.domain.player/name "Seppo"
;;  :product.domain.player/id 1
;;  :product.domain.player/tasks {:product.domain.task/id 666
;;                                :product.domain.task/time 5}
;;  :product.domain.point/points 7
;;  :foobar nil}"
```

## Unnamespacefy

To unnamespacefy the same data, use the **unnamespacefy** function. It also supports keywords, maps and collections. You can choose to unnamespacefy only the top level keywords, or deeply all maps and vector of maps with `:recur? true`. 

```clojure
(unnamespacefy data {:recur? true})

;; We get the following output:
;; {:name "Seppo"
;;  :id 1
;;  :tasks {:id 666 :time 5}
;;  :points 7
;;  :foobar nil}
```

Note that if unnamespacefying two keywords leads to the same result, you get an exception!

## Helpers

There are also some helper functions available for working effectively with the same keyword, regardless if it is namespaced or not. Generally speaking I do not recommend to mess with the same keyword in namespaced and unnamespaced form. However, these can be useful in situations in which you are changing your implementation from unnamespaced keywords to namespaced keywords.

```clojure
;; Get the specific key from a map, regardless if it is namespaced or not:
(get-un {:product.domain.player/name "Seppo"} :name) ; => "Seppo"
(get-un {:product.domain.task/name "The Task"} :name) ; => "The Task"
(get-un {:name "The Task"} :name) ; => "The Task"

;; Assoc the data to the given keyword which matches the corresponding namespaced or unnamespaced keyword.
(assoc-un {:product.domain.player/name "Seppo"} :name "Ismo")
;; => {:product.domain.player/name "Ismo"}
(assoc-un {:product.domain.task/name "The Task"} :name "The Task 123")
;; => {:product.domain.task/name "The Task 123"}
(assoc-un {:name "The Task"} :name "The Task 123")
;; => {:name "The Task 123"}
```

For more information on the available options, please read the [API documentation](https://jarzka.github.io/namespacefy/docs/).

# Changelog

Here: https://github.com/Jarzka/namespacefy/releases
