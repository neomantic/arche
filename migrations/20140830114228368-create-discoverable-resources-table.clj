;; migrations/20140830114228368-create-discoverable-resources-table.clj

(defn up []
  ["CREATE TABLE discoverable_resources(id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY, resource_name VARCHAR(255) NOT NULL, link_relation VARCHAR(255) NOT NULL, href VARCHAR(255) NOT NULL)"])

(defn down []
  ["DROP TABLE discoverable_resources"])