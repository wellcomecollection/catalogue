package weco.pipeline.matcher.models

import weco.catalogue.internal_model.matcher.WorkNode

// This holds a collection of nodes in our graph database.
//
// Each node describes the directed edges for which it is the source, so
// this collection describes an entire graph -- a subgraph of the
// entire graph in our database.
//
case class WorkGraph(nodes: Set[WorkNode])
