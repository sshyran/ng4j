// $Id: NamedGraphDB.java,v 1.1 2004/11/02 02:00:23 cyganiak Exp $
package de.fuberlin.wiwiss.ng4j.db;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.Quad;

/**
 * A database-backed Named Graph implementation which is
 * handed out by {@link NamedGraphSetDB#getGraph(Node)}.
 * The real work is done by a {@link QuadDB} instance. This class
 * provides a NamedGraph view onto the QuadDB interface for a
 * fixed graph name.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NamedGraphDB extends GraphBase implements NamedGraph {
	private QuadDB db;
	private Node graphName;
	
	public NamedGraphDB(QuadDB db, Node graphName) {
		this.db = db;
		this.graphName = graphName;
	}

	public Node getGraphName() {
		return this.graphName;
	}

	public void performDelete(Triple t) {
		this.db.delete(this.graphName, t.getSubject(), t.getPredicate(), t.getObject());
	}

	public ExtendedIterator find(TripleMatch m) {
		final Iterator quadIt = this.db.find(
				this.graphName,
				m.getMatchSubject(),
				m.getMatchPredicate(),
				m.getMatchObject());
		return new NiceIterator() {
			public boolean hasNext() {
				return quadIt.hasNext();
			}
			public Object next() {
				return ((Quad) quadIt.next()).getTriple();
			}
			public void remove() {
				quadIt.remove();
			}
		};
	}

	public void performAdd(Triple t) {
		this.db.insert(this.graphName, t.getSubject(), t.getPredicate(), t.getObject());
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