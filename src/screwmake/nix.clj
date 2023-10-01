(ns screwmake.nix 
  (:require
    [babashka.process :as p]))

(defn in-shell
  "Run function with nix shell. `f` is a function which takes in a function `p`. Function `p` takes in a list of strings which get run in a nix shell with the `pkgs` included."
  [pkgs f]
  (f (fn [fst & args]
       (let [opts? (map? fst)
             args' (if opts?
                       args
                       (conj args fst))
             opts (merge (when opts? fst) {})]
         (apply p/shell
                opts
                (->> args'
                     (conj (vector "nix-shell" "-p" pkgs "--run"))
                     flatten))))))

(in-shell ["php"]
          (fn [sh]
            (-> (sh {:continue true
                     :out :string
                     :err :string} "which" "php") :err)))
