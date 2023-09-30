(ns screwmake.util
  (:require
    [babashka.fs :as fs])) 

(defn get-source-files [dir ext]
  (let [files (map str (fs/list-dir dir))]
    (-> (filter #(and (not (fs/directory? %))
                      (= (fs/extension %) ext))
                files)
        (concat (map #(get-source-files % ext)
                     (filter fs/directory?
                             files)))
        flatten)))

(def root-files ["bb.edn", ".git", "flake.nix", "screwmake.json"])

(defn root?
  ([cwd] (root? cwd root-files))
  ([cwd root-files']
   (some fs/exists?
          (map #(fs/file cwd %)
               root-files'))))

(defn get-root
  ([] (get-root (fs/cwd)))
  ([cwd] (get-root cwd root-files))
  ([cwd root-files']
   (if (root? cwd root-files')
       cwd
       (get-root (fs/parent cwd)))))

(defn absnorm [path]
  (->> path
       fs/absolutize
       fs/normalize))

(defn fileify [& segs]
  (->> segs
       (apply fs/file)
       absnorm
       str))

(defn create-dirs [& dirs]
  (dorun
    (map fs/create-dirs
         dirs)))

(comment
  (map (comp println str)
       (get-source-files (absnorm "./src/")
                         "clj"))
  (str (get-root (absnorm "./src")))
  (create-dirs (map absnorm
                    ["./abc" "./def/ghj"])))
