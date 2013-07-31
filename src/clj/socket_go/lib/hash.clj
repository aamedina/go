(ns socket-go.lib.hash
  (:require [clojure.math.numeric-tower :as math])
  (:use clojure.set)  
  (:import java.util.BitSet
           java.util.Random
           java.math.BigInteger))

(def hash-bits 128)

(defn empty-bitset [size]
  (BitSet. size))

(defn random-bitset [^Random rng ^long size]
  (let [bigint (BigInteger. size rng)
        bitset (BitSet. size)]
    (dotimes [i size]
      (.set bitset i (.testBit bigint i)))
    bitset))

(defn move-hash [board color pos]
  (case color
    :black (aget (:black-hashes board) pos)
    :white (aget (:white-hashes board) pos)))

(defn rotate [hashes]
  (aset hashes 1 (.clone ^BitSet (aget hashes 0)))  
  nil)

(defn toggle [board current-state color pos]
  (.xor ^BitSet current-state
        (move-hash board color (:pos pos)))
  nil)

;; (let [pos (nth (:positions board) pos)]
    
;;     ;; (println color (:pos pos) current-state)
;;     )

(defn ko? [board current-state white-stone black-stone]
  (toggle board current-state :white white-stone)
  (toggle board current-state :black black-stone))

(defn zobrist-hash []
  (into-array BitSet [(empty-bitset hash-bits) (empty-bitset hash-bits)]))
