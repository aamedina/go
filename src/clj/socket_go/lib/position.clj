(ns socket-go.lib.position
  (:require [clojure.math.numeric-tower :as math])
  (:use clojure.set)
  (:import java.util.BitSet
           java.util.Random
           java.math.BigInteger))

(defn opponent [color]
  (if (identical? :black color)
    :white
    :black))

(defn between? [n a b & {:keys [inclusive?]}]
  (let [lt (if inclusive? <= <)]
    (if (and (>= n a) (lt n b)) true false)))

(defn initial-liberties [pos dim]
  (let [liberties
        (vector (if ((comp not zero?) (mod pos dim)) (- pos 1) -1)
                (if ((comp not zero?) (mod (+ pos 1) dim)) (+ pos 1) -1)
                (- pos dim) (+ pos dim))]
    (into #{} (filter #(between? % 0 (math/expt dim 2)) liberties))))

(defn validate-color [color]  
  (case color
    :white true :black true :empty true
    false))

(defn position [pos dim]
  (hash-map :pos pos
            :color (atom :empty :validator validate-color)
            :liberties (atom (initial-liberties pos dim)) 
            :white-neighbors (atom (empty #{}))
            :black-neighbors (atom (empty #{}))
            :ko-fight? (atom false)))

(defn initial-positions [dim]
  (let [positions
        (into-array (map #(position % dim) (range (math/expt dim 2))))]
    positions))
