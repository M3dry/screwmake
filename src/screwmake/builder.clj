(ns screwmake.builder)

(defprotocol Builder
  (run [config cmd args]))

(defmulti make-config
  (fn [{builder :builder} _ _]
    builder))

(defmethod make-config nil [_ name _]
  (println (str "Invalid builder for project `" name "`"))
  (System/exit 1))

(comment
  (make-config {:builder "C"
                :config {:cc "clangd"
                         :cflags ["-Wall", "-g"]
                         :dirs {:out "./build"
                                :src "./app"}
                         :target "foobaz"}}
              "foobar"
              "/home/m3/projects/foobar"))
