(ns socket-go.lib.board
  (:require [clojure.math.numeric-tower :as math]
            [clojure.core.reducers :as r])
  (:use clojure.set
        socket-go.lib.position
        socket-go.lib.hash
        [clojure.core.reducers :only [reducer folder fold]])
  (:import java.util.BitSet
           java.util.Random
           java.math.BigInteger))

(defn get-neighbors [color pos]
  (case color
    :black (:black-neighbors pos)
    :white (:white-neighbors pos)))

(defn nth-pos [board n]
  (nth (:positions board) n))

(defn neighbor-sets [board pos]
  (if (not= @(:color pos) :empty)
    (reduce
     (fn [neighbors next-pos]
       (conj neighbors @(get-neighbors @(:color pos) (nth-pos board next-pos))))
     [(conj @(get-neighbors @(:color pos) pos) (:pos pos))]
     @(get-neighbors @(:color pos) pos))
    (reduce
     (fn [liberties next-pos]
       (conj liberties @(:liberties (nth-pos board next-pos))))
     [(conj @(:liberties pos) (:pos pos))]
     @(:liberties pos))))

(defn disjoint-sets [sets]
  (->>
   (reduce
    (fn [disjoint-sets set]   
      (conj (difference disjoint-sets set) (intersection set disjoint-sets)))
    (reduce into #{} sets) sets)
   (remove empty?)))

(defn disjoint-neighbors [board pos]
  (->> (neighbor-sets board pos) (reduce union)))

(defn reduce-unit [board pos & {:keys [walk-empty?] :or {walk-empty? false}}]
  (let [pos (nth (:positions board) pos) color @(:color pos)]
    (println pos)
    (cond
     (and (not= color :empty) walk-empty?) #{}
     (and (= color :empty) (not walk-empty?)) #{}
     :else
     (reduce
      (fn [visited next-pos]
        (if (empty? next-pos)
          (reduced visited)
          (let [next-disjoint
                (disjoint-neighbors board (nth-pos board (first next-pos)))
                next-visited (into visited next-disjoint)]
            (recur next-visited
                   (into (disj next-pos (first next-pos))
                         (difference next-disjoint visited))))))
      #{} (hash-set #{(:pos pos)})))))

(defn group-liberties [board pos]
  (reduce (fn [liberties member]
            (into liberties
                  @(:liberties (nth (:positions board) member))))
          #{} (reduce-unit board pos)))

(defn empty-neighbors [board pos]
  (cond
   (= @(:color (nth-pos board pos)) :empty)
   (reduce (fn [neighbors member]
             (into neighbors
                   (union
                    @(:black-neighbors (nth-pos board member))
                    @(:white-neighbors (nth-pos board member)))))
           #{} (reduce-unit board pos :walk-empty? true))
   :else 
   (reduce (fn [neighbors member]
             (into neighbors
                   (union
                    @(get-neighbors @(:color (nth-pos board member))
                                    (nth-pos board member)))))
           #{} (reduce-unit board pos :walk-empty? true))))

(defn eye? [board pos]
  (let [pos (aget (:positions board) pos) color @(:color pos)]
    (when (and (identical? :empty color)
               (:white-neighbors pos))
      (cond (count @(:liberties pos)) false))))

(defn enemy-neighbors [color pos]
  (case color
    :black (:white-neighbors pos)
    :white (:black-neighbors pos)))

(defn group-enemies [board pos]
  (let [positions (map #(nth (:positions board) %) (reduce-unit board pos))]
    (reduce (fn [liberties member]
              (into liberties
                    @(enemy-neighbors @(:color member) member)))
            #{} positions)))

(defn enemy [color]
  (case color
    :black :white
    :white :black))

(defn rand-elem [coll]
  (first (shuffle coll)))

(defn enclosed-points [board]
  (let [empty-positions
        (difference @(:empty-positions board) @(:enclosed-positions board))
        enclosed
        (difference
         empty-positions
         (reduce-unit board (rand-elem empty-positions) :walk-empty? true))]
    (loop [enclosed-set enclosed enclosed-sets #{}]
      (cond (empty? enclosed-set)
            (do
              (swap! (:enclosed-positions board) union (into #{} enclosed-sets))
              enclosed-sets)
            :else
            (recur
             (difference enclosed-set (reduce-unit board (first enclosed-set)))
             (conj enclosed-sets (reduce-unit board (first enclosed-set))))))))

(defn color-positions [board color]
  (case color
    :black (:black-positions board)
    :white (:white-positions board)))

(defn adjacent-positions [board pos]
  (union @(:liberties pos) @(:black-neighbors pos) @(:white-neighbors pos)))

(defn enemy? [current-color other-pos]
  (identical? (enemy current-color) @(:color other-pos)))

(defn path? [board color first-pos second-pos]
  (if (= (reduce-unit board first-pos)
         (reduce-unit board second-pos))
    (loop [neighbors @(get-neighbors color (nth (:positions board) first-pos))]
      (cond (contains? neighbors second-pos) true
            (empty? neighbors) false
            :else
            (recur
             (union
              (into #{} (next neighbors))
              (difference
               @(get-neighbors
                 color (nth (:positions board) (first neighbors)))
               neighbors)))))
    false))

(defn cycle? [board unit]
  (let [start-pos (nth (:positions board) (first unit))
        color @(:color start-pos)
        start-pos-neighbors @(get-neighbors color start-pos)]
    (loop [node start-pos
           neighbors start-pos-neighbors
           visited #{start-pos}]
      (cond (and (contains? neighbors (:pos start-pos))
                 )
            (difference @(get-neighbors color start-pos) visited)))))

(defn unit-eyes [board pos]
  (let [unit (reduce-unit board pos)
        unit-liberties (map #(nth (:positions board) %)
                            (group-liberties board pos))
        pos (nth (:positions board) pos)
        color @(:color pos)]
    (filter #(and (<= (count @(:liberties %)) 2)
                  (empty? @(enemy-neighbors color %)))
            unit-liberties)))

(defn atari?
  ([group-liberties] (= (count group-liberties) 1))
  ([board pos] (= (count (group-liberties board pos)) 1)))

(def nearly-isolated? (comp atari? group-liberties))

(defn nearly-dead? [board color pos]
  (and (enemy? color pos)
       (nearly-isolated? board (:pos pos))))

(defn near-suicide? [board color pos]
  (and (= @(:color pos) :empty)
       (atari? @(:liberties pos))
       (zero? (count @(get-neighbors color pos)))))

(defn identify-captor [board color pos]
  (nth-pos board (first @(:liberties pos))))

(defn is-ko? [board color pos]
  (rotate (:hashes board))
  (toggle board (aget (:hashes board) 0) color pos)
  (if (near-suicide? board color pos)
    (let [captor (identify-captor board color pos)
          current-state (.clone ^BitSet (aget (:hashes board) 0))]
      (if (not (nil? captor))
        (do
          (toggle board current-state (enemy color) captor)
          (case color
            :black (ko? board current-state captor pos)
            :white (ko? board current-state pos captor))
          (cond (= current-state (aget (:hashes board) 1))
                (if @(:ko-fight? captor)
                  (do (aset (:hashes board) 0 (aget (:hashes board) 1))
                      (reset! (:ko-fight? captor) false) false)
                  (do (aset (:hashes board) 0 (aget (:hashes board) 1))
                      (reset! (:ko-fight? captor) true) true))
                :else
                (do (aset (:hashes board) 0 (aget (:hashes board) 1)) false)))

        (do (aset (:hashes board) 0 (aget (:hashes board) 1)) false)))
    (do (aset (:hashes board) 0 (aget (:hashes board) 1)) false)))

(defn suicide? [board color pos]
  (and (= 0 (count @(:liberties pos)))
       (->> @(enemy-neighbors color pos)
            (filter #(atari? board %))
            empty?)
       (if (pos? (count @(get-neighbors color pos)))
         (->> @(get-neighbors color pos)
              (filter #(atari? board %))
              (#(not (empty? %))))
         true)))

(defn legal-move? [board color pos]
  (and (number? (:pos pos))
       (contains? @(:empty-positions board) (:pos pos))
       (not (is-ko? board color pos))
       (not (suicide? board color pos))))

(def isolated? (comp empty? group-liberties))

(defn dead? [board pos]
  (isolated? board (:pos pos)))

(defn update-captured-neighbors [board color pos]
  (doseq [neighbor (adjacent-positions board pos)]
    (let [neighbor (nth-pos board neighbor)]
      (swap! (get-neighbors color neighbor) difference #{(:pos pos)})
      (swap! (:liberties neighbor) union #{(:pos pos)}))))

(defn remove-stone [board pos]
  (case @(:color pos)
    :black (swap! (:white-prisoners board) inc)
    :white (swap! (:black-prisoners board) inc))
  (update-captured-neighbors board @(:color pos) pos)
  (toggle board (aget (:hashes board) 0) @(:color pos) pos)
  (swap! (color-positions board @(:color pos)) difference #{(:pos pos)})
  (reset! (:color pos) :empty)
  (swap! (:empty-positions board) union #{(:pos pos)}))

(defn capture-unit [board unit]
  (doseq [pos unit]
    (remove-stone board (nth-pos board pos))))

(defn update-neighbors [board color pos]
  (let [captured-enemies
        (reduce
         (fn [captured neighbor]
           (let [neighbor (nth-pos board neighbor)]
             (swap! (get-neighbors color neighbor) union #{(:pos pos)})
             (swap! (:liberties neighbor) difference #{(:pos pos)})
             (if (and (enemy? color neighbor) (dead? board neighbor))
               (conj captured (reduce-unit board (:pos neighbor)))
               captured)))
         #{}
         (adjacent-positions board pos))]
    captured-enemies))

(defn gather-and-validate-units [board color]
  (let [units
        (reduce
         (fn [units pos]
           (if (contains? units pos)
             (-> units
                 (conj (reduce-unit board pos))
                 (difference (reduce-unit board pos)))
             units))
         @(color-positions board color)
         @(color-positions board color))
        captured-units
        (filter #(= (count (group-liberties board (first %))) 0) units)
        units (difference units captured-units)]
    (doseq [unit captured-units]
      (capture-unit board unit))
    units))


(defn add-stone [board color pos]
  (toggle board (aget (:hashes board) 0) color pos)
  (reset! (:color pos) color)
  (swap! (:empty-positions board) difference #{(:pos pos)})
  (swap! (color-positions board color) union #{(:pos pos)}))

(defn get-units [board color]
  (case color
    :black (:black-units board)
    :white (:white-units board)))

(defn move [board color pos]
  (if (= pos :pass)
    :pass
    (let [pos (nth-pos board pos)
          old-units
          (->> @(get-neighbors color pos)
               (reduce #(union %1 #{(reduce-unit board %2)}) #{})
               (map #(swap! (get-units board color) difference #{%}))
               doall)]
      (when (legal-move? board color pos)
        (add-stone board color pos)
        (let [player-units (get-units board color)
              enemy-units (get-units board (enemy color))
              new-unit (reduce-unit board (:pos pos))
              captured-units (update-neighbors board color pos)]
          (doseq [unit captured-units]
            (capture-unit board unit)
            (swap! enemy-units difference #{unit}))
          (swap! player-units union #{new-unit}))
        (swap! (:move-counter board) inc)
        {:color color :pos (:pos pos)}))))

(defn final-score [board]
  (let [enclosed-positions (enclosed-points board)
        territories
        (->>
         (map
          (fn [territory]
            (cond (every? #(= @(:color (nth-pos board %)) :black)
                          (empty-neighbors board (first territory)))
                  {:color :black :points (count territory)}
                  (every? #(= @(:color (nth-pos board %)) :white)
                          (empty-neighbors board (first territory)))
                  {:color :white :points (count territory)}))
          enclosed-positions)
         (remove nil?)
         (group-by :color))
        black-score
        (+ (count @(:black-positions board))
           (reduce + (map :points (:black territories))))
        white-score
        (+ (count @(:white-positions board))
           (reduce + (map :points (:white territories))))]
    (swap! (:black-score board) + black-score)
    (swap! (:white-score board) + white-score)
    (if (> black-score white-score) :black :white)))

(defn empty-board [dim]
  (hash-map
   :dim dim
   :positions (initial-positions dim)
   :move-counter (atom 0)
   :black-score (atom 0)
   :white-score (atom 0)
   :black-units (atom #{})
   :white-units (atom #{})
   :empty-positions (atom (into #{} (range 361)))
   :black-positions (atom #{})
   :white-positions (atom #{})
   :enclosed-positions (atom #{})
   :black-prisoners (atom 0)
   :white-prisoners (atom 0)
   :black-hashes
   (into-array BitSet (repeatedly 361 #(random-bitset (Random.) hash-bits)))
   :white-hashes
   (into-array BitSet (repeatedly 361 #(random-bitset (Random.) hash-bits)))
   :hashes
   (into-array BitSet [(empty-bitset hash-bits) (empty-bitset hash-bits)])))

(defn print-board [board]
  (let [dim (:dim board)
        cols (->> (range (inc dim))
                  (map #(char (+ (int \A) %)))
                  (remove #{\I})
                  (interpose " ")
                  (apply str))]
    (println "Black:" @(:black-score board))
    (println "White:" @(:white-score board))
    (println (str "    " cols "\n"))
    (doseq [y (reverse (range dim))]
      (print (format "%02d " (inc y)))
      (doseq [x (range dim)]
        (print
         (str " "
              (case @(:color (nth (:positions board) (+ x (* y 19))))
                :white "O"
                :black "#"
                :empty "."))))
      (println (format "  %02d" (inc y))))
    (println (str "\n    " cols "\n"))))

(defn two-passes? [moves board]
  (and (> (count moves) 2) (every? #{:pass} (take-last 2 moves))))

(defn prob-pass [n]
  (Math/abs (- 1.0 (/ 1 (Math/abs (- 1.0 (Math/exp (Math/log (/ n 2880)))))))))

(defn rand-move [board color]
  (let [m
        (rand-elem
         (into
          (into [] @(:empty-positions board))                 
          (repeat
           (Math/floor
            (* (prob-pass @(:move-counter board)) @(:move-counter board)))
           :pass)))]
    (move board color m)))

(defn reduce-game []
  (let [board (empty-board 19)]
    (reduce
     (fn [moves color]
       (if (two-passes? moves board)
         (reduced moves)
         (if-let [move (rand-move board color)]
           (conj moves move)
           (recur moves color))))
     []
     (flatten (repeat 60 [:black :white])))
    (final-score board)
    board))

(defn to-pos [board-pos]
  (let [letter (str (first board-pos))
        num (Integer/valueOf (apply str (next board-pos)))
        alpha "ABCDEFGHJKLMNOPQRST"]
    (+ num (* (.indexOf alpha letter) 19))))

(defn from-pos [pos]
  (let [letter (quot pos 19)
        num (mod pos 19)
        alpha "ABCDEFGHJKLMNOPQRST"]
    (str (.charAt alpha letter) num)))

(defn sample-game-statistics [n]
  (pmap (fn [game] (reduce-game)) (range n)))
