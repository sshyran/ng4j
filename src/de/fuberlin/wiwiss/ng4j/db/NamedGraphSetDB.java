// $Id: NamedGraphSetDB.java,v 1.2 2004/11/02 02:26:13 cyganiak Exp $
package de.fuberlin.wiwiss.ng4j.db;

import java.sql.Connection;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphModel;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.Quad;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetIO;

/**
 * <p>A {@link NamedGraphSet} implementation backed by a relational database.</p>
 *
 * <p>The real work is done by a {@link QuadDB} instance. This class provides a
 * NamedGraphSet view onto the QuadDB.</p>
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NamedGraphSetDB extends NamedGraphSetIO implements NamedGraphSet {
	private final static String DEFAULT_TABLE_PREFIX = "ng4j";
	private QuadDB db;

	/**
	 * Creates a new NamedGraphSet. The necessary tables will be created
	 * automatically if they don't exist.
	 * @param db A QuadDB instance which provides persistence for this NamedGraphSet.
	 */
	public NamedGraphSetDB(QuadDB db) {
		this.db = db;
		if (!this.db.tablesExist()) {
			this.db.createTables();
		}
	}

	/**
	 * Creates a persistent NamedGraphSet from a database connection. The necessary
	 * tables will be created automatically if they don't exist.
	 * @param connection A connection to an SQL database
	 */
	public NamedGraphSetDB(Connection connection) {
		this(connection, DEFAULT_TABLE_PREFIX);
	}

	/**
	 * Creates a persistent NamedGraphSet from a database connection using a table prefix.
	 * The necessary tables will be created if they don't exist. The table prefix
	 * allows storing multiple NamedGraphSets in a single database.
	 * @param connection A connection to an SQL database
	 * @param tablePrefix a prefix for all tables used by the new NamedGraphSet
	 */
	public NamedGraphSetDB(Connection connection, String tablePrefix) {
		this(new QuadDB(connection, tablePrefix));
	}

	/**
	 * Drops the persistent NamedGraphSet from the database.
	 * @param connection A connection to an SQL database
	 */
	public static void delete(Connection connection) {
		NamedGraphSetDB.delete(connection, DEFAULT_TABLE_PREFIX);
	}
	
	/**
	 * Drops a persistent NamedGraphSet from a database. Must be used to delete
	 * persistent NamedGraphSets that were created with a table prefix.
	 * @param connection A connection to an SQL database
	 * @param tablePrefix The prefix of the tables used by the NamedGraphSet
	 */
	public static void delete(Connection connection, String tablePrefix) {
		QuadDB db = new QuadDB(connection, tablePrefix);
		if (db.tablesExist()) {
			db.deleteTables();
		}
		db.close();
	}

	public void addGraph(NamedGraph graph) {
		createGraph(graph.getGraphName());
		Iterator it = graph.find(Node.ANY, Node.ANY, Node.ANY);
		while (it.hasNext()) {
			Triple triple = (Triple) it.next();
			this.db.insert(graph.getGraphName(), triple.getSubject(), triple.getPredicate(), triple.getObject());
		}
	}

	public void removeGraph(Node graphName) {
		this.db.delete(graphName, Node.ANY, Node.ANY, Node.ANY);
		this.db.deleteGraphName(graphName);
	}

	public void removeGraph(String graphNameURI) {
		removeGraph(Node.createURI(graphNameURI));
	}

	public boolean containsGraph(Node graphName) {
		return this.db.containsGraphName(graphName);
	}

	public boolean containsGraph(String graphNameURI) {
		return this.db.containsGraphName(Node.createURI(graphNameURI));
	}

	public NamedGraph getGraph(Node graphName) {
		if (!this.db.containsGraphName(graphName)) {
			return null;
		}
		return new NamedGraphDB(this.db, graphName);
	}

	public NamedGraph getGraph(String graphNameURI) {
		return getGraph(Node.createURI(graphNameURI));
	}

	public NamedGraph createGraph(Node graphName) {
		if (this.db.containsGraphName(graphName)) {
			this.db.delete(graphName, Node.ANY, Node.ANY, Node.ANY);
		} else {
			this.db.insertGraphName(graphName);
		}
		return getGraph(graphName);
	}

	public NamedGraph createGraph(String graphNameURI) {
		return createGraph(Node.createURI(graphNameURI));
	}

	public Iterator listGraphs() {
		final Iterator graphNames = this.db.listGraphNames();
		return new Iterator() {
			private Node current = null;
			public boolean hasNext() {
				return graphNames.hasNext();
			}
			public Object next() {
				this.current = (Node) graphNames.next();
				return new NamedGraphDB(getDB(), this.current);
			}
			public void remove() {
				if (this.current == null) {
					throw new IllegalStateException("next() was not called, or current element is already deleted");
				}
				removeGraph(this.current);
				this.current = null;
			}
		};
	}

	public long countGraphs() {
		return this.db.countGraphNames();
	}

	public boolean isEmpty() {
		return this.db.countGraphNames() == 0;
	}

	public void addQuad(Quad quad) {
		if (!this.db.containsGraphName(quad.getGraphName())) {
			this.db.insertGraphName(quad.getGraphName());
		}
		this.db.insert(quad.getGraphName(), quad.getSubject(), quad.getPredicate(), quad.getObject());
	}

	public boolean containsQuad(Quad pattern) {
		return this.db.find(pattern.getGraphName(), pattern.getSubject(), pattern.getPredicate(), pattern.getObject()).hasNext();
	}

	public void removeQuad(Quad pattern) {
		this.db.delete(pattern.getGraphName(), pattern.getSubject(), pattern.getPredicate(), pattern.getObject());
	}

	public int countQuads() {
		return this.db.count();
	}

	public Iterator findQuads(Quad pattern) {
		return findQuads(pattern.getGraphName(), pattern.getSubject(),
				pattern.getPredicate(), pattern.getObject());
	}

	public Iterator findQuads(Node graphName, Node subject, Node predicate,
			Node object) {
		return this.db.find(graphName, subject, predicate, object);
	}

	public Graph asJenaGraph(final Node defaultGraphForAdding) {
		if (!containsGraph(defaultGraphForAdding)) {
			createGraph(defaultGraphForAdding);
		}
		return new NamedGraphDB(this.db, Node.ANY) {
			public void performAdd(Triple t) {
				getDB().insert(defaultGraphForAdding,
						t.getSubject(), t.getPredicate(), t.getObject());
			}
		};
	}

	public NamedGraphModel asJenaModel(String defaultGraphForAdding) {
		return new NamedGraphModel(this, defaultGraphForAdding);
	}

	public void close() {
		this.db.close();
	}
	
	QuadDB getDB() {
		return this.db;
	}
}

/*
 *  (c)   Copyright 2004 Christian Bizer (chris@bizer.de)
 *   All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */