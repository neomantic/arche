Feature: Discovering a way to find a resource
  In order to start finding a resource
  As an API client
  I want to a list of links to entry points to the available discoverable resources

Scenario Outline: A client receives a list of links to find resource entries points
  Given a discoverable resource exists with the following attributes:
    | link_relation_url | https://www.mydomain.com/alps/study |
    | href              | https://service.com/study           |
    | resource_name     | studies                             |
  And a discoverable resource exists with the following attributes:
    | link_relation_url | https://www.mydomain.com/alps/users |
    | href              | https://service.com/users           |
    | resource_name     | users                               |
  When I invoke the uniform interface method GET to "/" accepting "<Mime-Type>"
  Then I should get a status of 200
  And the resource representation should have exactly the following links:
  | link_relation          | href                                             |
  | studies                | https://service.com/study                        |
  | users                  | https://service.com/users                        |
  | discoverable_resources | http://example.org/discoverable_resources        |
  | profile                | http://example.org/alps/EntryPoints              |
  | self                   | http://example.org/                              |
  | type                   | http://example.org/alps/EntryPoints#entry_points |
  And the response should have the following header fields:
  | field         | field_contents       |
  | Cache-Control | max-age=600, private |
  | ETag          | anything             |
  | Location      | http://example.org/  |

 Examples:
  | Mime-Type                 |
  | application/hal+json      |
  | application/vnd.hale+json |
  | application/json          |

Scenario Outline: A Client receives a at least the discoverable resources entry point when no resources have been registered
  Given I invoke the uniform interface method GET to "/" accepting "<Mime-Type>"
  Then I should get a status of 200
  And the resource representation should have exactly the following links:
  | link_relation          | href                                             |
  | profile                | http://example.org/alps/EntryPoints              |
  | self                   | http://example.org/                              |
  | type                   | http://example.org/alps/EntryPoints#entry_points |
  | discoverable_resources | http://example.org/discoverable_resources        |
  And the response should have the following header fields:
  | field         | field_contents       |
  | Cache-Control | max-age=600, private |
  | ETag          | anything             |
  | Location      | http://example.org/  |

 Examples:
  | Mime-Type                 |
  | application/hal+json      |
  | application/vnd.hale+json |
  | application/json          |

Scenario: A client can receive an alps profile describing the link relations of entry points when there are none
  Given I invoke the uniform interface method GET to "/" accepting "application/hal+json"
  And I follow the link to the "profile" accepting "application/alps+json"
  Then I should get a status of 200
  And I should receive an "application/alps+json" with the following attributes:
  | attribute | value                                                                               |
  | version   | 1.0                                                                                 |
  | doc       | Describes the semantics, states and state transitions associated with Entry Points. |
  And the "application/alps+json" document should have the following links:
  | rel    | href                                |
  | self   | http://example.org/alps/EntryPoints |
  And the "application/alps+json" document should have a "entry_points" descriptor with the following properties:
  | property | value                                                               |
  | type     | semantic                                                            |
  | doc      | A collection of link relations to find resources of a specific type |
  And the "entry_points" descriptor should have exactly the following descriptors:
  | href                    |
  | #list                   |
  | #discoverable_resources |
  And the "application/alps+json" document should have a "list" descriptor with the following properties:
  | property | value                                            |
  | name     | self                                             |
  | type     | safe                                             |
  | doc      | Returns a list of entry points                   |
  | rt       | http://example.org/alps/EntryPoints#entry_points |
  | id       | list                                             |
  And the "application/alps+json" document should have a "discoverable_resources" descriptor with the following properties:
  | property | attribute                                                                           |
  | type     | safe                                                                                |
  | doc      | Returns a resource of the type 'discoverable_resources' as described by its profile |
  | rt       | http://example.org/alps/DiscoverableResources                                       |
  | name     | discoverable_resources                                                              |
  | id       | discoverable_resources                                                              |
  And the response should have the following header fields:
  | field         | field_contents                         |
  | Cache-Control | max-age=600, private                   |
  | ETag          | anything                               |
  | Location      | http://example.org/alps/EntryPoints    |
  | Accept        | application/alps+json,application/json |

Scenario: A client can receive an alps profile describing the link relations of entry points when there are some
  Given a discoverable resource exists with the following attributes:
    | link_relation_url | https://www.mydomain.com/alps/study |
    | href              | https://service.com/study           |
    | resource_name     | studies                             |
  And I invoke the uniform interface method GET to "/" accepting "application/hal+json"
  And I follow the link to the "profile" accepting "application/alps+json"
  Then I should get a status of 200
  And I should receive an "application/alps+json" with the following attributes:
  | attribute | value                                                                               |
  | version   | 1.0                                                                                 |
  | doc       | Describes the semantics, states and state transitions associated with Entry Points. |
  And the "application/alps+json" document should have the following links:
  | rel  | href                                |
  | self | http://example.org/alps/EntryPoints |
  And the "application/alps+json" document should have a "entry_points" descriptor with the following properties:
  | property | attribute                                                           |
  | id       | entry_points                                                        |
  | type     | semantic                                                            |
  | doc      | A collection of link relations to find resources of a specific type |
  And the "entry_points" descriptor should have exactly the following descriptors:
  | href                    |
  | #list                   |
  | #studies                |
  | #discoverable_resources |
  And the "application/alps+json" document should have a "list" descriptor with the following properties:
  | property | attribute                                        |
  | name     | self                                             |
  | type     | safe                                             |
  | doc      | Returns a list of entry points                   |
  | rt       | http://example.org/alps/EntryPoints#entry_points |
  And the "application/alps+json" document should have a "studies" descriptor with the following properties:
  | property | attribute                                                            |
  | type     | safe                                                                 |
  | doc      | Returns a resource of the type 'studies' as described by its profile |
  | rt       | https://www.mydomain.com/alps/study                                  |
  And the "studies" descriptor should have the following link:
  | rel     | href                                |
  | profile | https://www.mydomain.com/alps/study |
  And the response should have the following header fields:
  | field         | field_contents                         |
  | Cache-Control | max-age=600, private                   |
  | ETag          | anything                               |
  | Location      | http://example.org/alps/EntryPoints    |
  | Accept        | application/alps+json,application/json |
