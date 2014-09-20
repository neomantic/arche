(ns step-definitions.http-response-steps
  (:use step-definitions.discoverable-resources-steps
        cucumber.runtime.clj
        clojure.test))

(Then #"^the response should have the following header fields:$" [table]
      (let [received-headers (last-response-headers)]
        (doseq [expected-headers (table-rows-map table)]
          (let [[field value] expected-headers]
            (is (not (nil? (get received-headers field)))
                (format "expected %s, but got: %s" field received-headers))
            (when (not (= value "anything"))
              (is (= value (get received-headers field))
                  (format "expected value %s for header %s; got %s"
                          value field (get received-headers field))))))))
