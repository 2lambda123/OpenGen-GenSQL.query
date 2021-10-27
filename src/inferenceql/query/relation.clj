(ns inferenceql.query.relation
  "Functions for creating and manipulating relations."
  (:refer-clojure :exclude [empty sort transduce])
  (:require [clojure.spec.alpha :as s]
            [inferenceql.query.tuple :as tuple]
            [medley.core :as medley]))

;; These are not sequence functions, so the primary argument comes first.

(defn relation
  "Produces a relation from a sequence of maps."
  ([coll]
   (let [attributes (into []
                          (comp (mapcat keys)
                                (distinct))
                          coll)]
     (relation coll attributes)))
  ([coll attributes]
   (with-meta coll {::attributes attributes})))

(defn empty
  "Returns an empty relation with the specified attributes."
  [attrs]
  (with-meta [] {::attributes attrs}))

(defn relation?
  "Returns true if `x` is a relation."
  [x]
  (and (coll? x)
       (contains? (meta x)
                  ::attributes)))

(defn attributes
  "Returns the attributes of a relation."
  [rel]
  (-> rel meta ::attributes))

(defn tuples
  "Returns a sequence of tuples in relation `rel`."
  [rel]
  (map #(with-meta % {::tuple/attributes (attributes rel)})
       rel))

(defn project
  [rel attrs]
  (with-meta (sequence (map #(tuple/select-attrs % attrs))
                       (tuples rel))
    {::attributes attributes}))

(defn extended-project
  [rel coll]
  (let [f (fn [tuple]
            tuple
            (meta tuple)
            (->> (zipmap (map second coll)
                         (map #((first %) tuple)
                              coll))
                 (medley/remove-vals nil?)))]
    (with-meta (map f (tuples rel))
      {::attributes (mapv second coll)})))

(defn select
  [rel pred]
  (with-meta (filter pred (tuples rel))
    (meta rel)))

(defn limit
  [rel n]
  (with-meta (take n (tuples rel))
    (meta rel)))

(defn sort
  [rel attr order]
  (with-meta (->> (tuples rel)
                  (sort-by #(tuple/get % attr)
                           (case order
                             :ascending compare
                             :descending #(compare %2 %1))))
    (meta rel)))

(defn transduce
  [rel xf]
  (with-meta (into (empty rel)
                   xf
                   rel)
    (meta rel)))

(defn add-attribute
  [rel attr]
  (let [attributes (into []
                         (distinct)
                         (conj (attributes rel)
                               attr))]
    (relation rel attributes)))

(s/def ::attribute symbol?)
(s/def ::relation relation?)
