# KVStore

This is a naive attempt at writing a simple, distributed key value store. My main goal in writing this is to gain intuition about what it takes to build such a system.

## System requirements
* Basic CRUD operations. Get / Insert / Delete
* HTTP interface
* Strongly consistent (as consistent as it can be)
* Distributed and fault tolerant
* Persistence

## Design

I drew heavy inspiration from MongoDB's and Kafka's master / slave setup. There will be one primary and many slaves. All reads / writes will go to the primary. The slaves will then replicate the operations on the primary.

### Basic CRUD operations. Get / Insert / Delete

There is a simple interface for key / value operations. I've added an in-memory hash implementation for this.

### HTTP interface

User can query via the HTTP interface from any of the nodes. A proxy will figure out which is the primary and forward the requests to the primary. If there is no currently elected primary, the request will fail.

### Strongly consistent (as consistent as it can be)

All reads / writes will go to the primary. This is to avoid presenting an inconsistent view of the world. We can relax this by allowing the proxy to read from any of the members in the replica set.

### Distributed and fault tolerant

`ClusterCoordinator` (cluster singleton) handles leader election / split brain resolution [TODO]. I use the cluster singleton to have a consistent view of the entire cluster.

When the primary goes down, `ClusterCoordinator` will elect a new primary amongst the in-sync secondaries (caught up to the primary). Secondaries can join and leave as they wish.

### Persistence [TODO]

## Tech

Akka