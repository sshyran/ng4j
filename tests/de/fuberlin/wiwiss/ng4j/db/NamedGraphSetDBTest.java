// $Id: NamedGraphSetDBTest.java,v 1.1 2004/12/12 17:30:29 cyganiak Exp $
package de.fuberlin.wiwiss.ng4j.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.mem.GraphMem;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.NamedGraphSetTest;
import de.fuberlin.wiwiss.ng4j.Quad;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphImpl;
import de.fuberlin.wiwiss.ng4j.triql.TriQLQuery;

/**
 * Tests {@link NamedGraphSetDB} by reusing the common NamedGraphSetTest
 * tests and adding some additional tests.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NamedGraphSetDBTest extends NamedGraphSetTest {

	public NamedGraphSet createNamedGraphSet() throws Exception {
		return DBConnectionHelper.createNamedGraphSetDB();
	}

	public void tearDown() throws Exception {
		super.tearDown();
		DBConnectionHelper.deleteNamedGraphSetTables();
	}
	
	public void testIsEmpty() {
		assertTrue(this.set.isEmpty());
		this.set.addQuad(new Quad(node1, node1, node1, node1));
		assertFalse(this.set.isEmpty());
	}
	
	public void testAddGraph() {
		NamedGraph ng = new NamedGraphImpl(node1, new GraphMem());
		assertFalse(this.set.containsGraph(node1));
		assertEquals(0, this.set.countGraphs());
		this.set.addGraph(ng);
		assertEquals(1, this.set.countGraphs());
		assertTrue(this.set.containsGraph(node1));
		assertFalse(this.set.containsGraph(node2));
		assertTrue(this.set.getGraph(node1).isEmpty());
		assertNull(this.set.getGraph(node2));
		Iterator it = this.set.listGraphs();
		assertTrue(it.hasNext());
		assertEquals(node1, ((NamedGraph) it.next()).getGraphName());
		assertFalse(it.hasNext());
		assertFalse(this.set.isEmpty());
		it.remove();
		assertTrue(this.set.isEmpty());
	}
	
	public void testTriQL() {
		List l = new ArrayList();
		l.add(new Quad(node1, foo, bar, baz));
		l.add(new Quad(node1, foo, foo, foo));
		l.add(new Quad(node2, foo, foo, foo));
		Iterator it = l.iterator();
		while (it.hasNext()) {
			Quad q = (Quad) it.next();
			this.set.addQuad(q);
		}
		it = TriQLQuery.exec(this.set, "SELECT * WHERE ?graph (?s ?p ?o)");
		List actual = new ArrayList();
		assertTrue(it.hasNext());
		actual.add(it.next());
		assertTrue(it.hasNext());
		actual.add(it.next());
		assertTrue(it.hasNext());
		actual.add(it.next());
		assertFalse(it.hasNext());
	}
}