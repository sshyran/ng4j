// $Id: SemanticWebClient.java,v 1.4 2006/09/28 14:51:03 tgauss Exp $
package de.fuberlin.wiwiss.ng4j.semwebclient;

import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import java.util.Iterator;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.Node;

/**
 * <p>
 * The SematicWebClient interface enables applications to access the Semantic
 * Web.
 * </p>
 * 
 * <p>
 * The Semantic Web is represended as a single, global RDF graph. Applications
 * can use the find(TripleMatch pattern) method to retrieve information from the
 * Semantic Web.
 * </p>
 * 
 * <p>
 * During the execution of a find() query, information about all resources that
 * appear in the triple pattern and in the query results is dynamically loaded
 * from the Semantic Web by:
 * <ul>
 * <li>dereferencing the URI of each resource using the HTTP protocol,
 * <li>following all known rdf:seeAlso links for each resource.
 * </ul>
 * </p>
 * 
 * <p>
 * Internally, retrieved information is represented as a set of named graphs,
 * which allows applications to keep track of information provenance.
 * </p>
 * 
 * <p>
 * If the result of dereferencing a URI is a RDF graph, the graph is added as a
 * named graph (named with the retrieval URI) to graph set. <BR>
 * 
 * Within each graphset there is a graph http://localhost/provenanceInformation,
 * which contains provenance information for each retrieved graph. Each graph is
 * described with a swp:sourceURL and a swp:retrievalTimestamp property.
 * 
 * @author Chris Bizer (chris@bizer.de)
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface SemanticWebClient extends NamedGraphSet {

	/**
	 * Finds Triples that match a triple pattern. The argument may contain
	 * wildcards ({@link Node#ANY}).
	 * 
	 * The implementation of the find method uses multithreading, thus the first
	 * triples can already be used while there is still information being
	 * retrieved in the background.
	 * 
	 * Returned triples have a sourceGraphs() method which returns an iterator
	 * over all graphs names in which contain the triple. The returned iterator
	 * has a retrievalFinished() method which returns true when the retrieval
	 * process for all URIs and see:Also links is finished.
	 * 
	 * @param pattern
	 * @return
	 */
	public Iterator find(TripleMatch pattern);

	/**
	 * Finds Triples that match a triple pattern. The argument may contain
	 * wildcards ({@link Node#ANY}).
	 * 
	 * The implementation of the find method uses multithreading, thus the first
	 * triples can already be used while there is still information being
	 * retrieved in the background.
	 * 
	 * Returned triples have a sourceGraphs() method which returns an iterator
	 * over all graphs names in which contain the triple. The returned iterator
	 * has a retrievalFinished() method which returns true when the retrieval
	 * process for all URIs and see:Also links is finished.
	 * 
	 * @param pattern
	 * @return
	 */
	public void find(TripleMatch pattern, TripleListener listener);

	/**
	 * Adds a remote graph to the graphset. The graph is retrieved by
	 * dereferencing the URI.
	 * 
	 * @param seconds
	 */
	public void addRemoteGraph(String URI);

	/**
	 * Reloads a remote graph. The current graph is replaced by the new graph.
	 * 
	 * @param seconds
	 */
	public void reloadRemoteGraph(String URI);

	/**
	 * Sets a configuration option. Like Timeout or MaxRetrievalSize
	 */
	public void setConfig(String option, String value);

	/**
	 * Get a configuration option. Like Timeout or MaxRetrievalSize MaxThreads
	 */
	public String getConfig(String option);

	/**
	 * Returns an iterator over all successfully dereferenced URIs.
	 * 
	 * @return
	 */
	public Iterator successfullyDereferencedURIs();

	/**
	 * Returns an iterator over all URIs that couldn't be dereferenced. The
	 * elements of the list are pairs (URI, errorcode) describing the problem
	 * that appeared in the retrieval process.
	 * 
	 * @return
	 */
	public Iterator unsuccessfullyDereferencedURIs();

}

/*
 * (c) Copyright 2006 Christian Bizer (chris@bizer.de) All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */