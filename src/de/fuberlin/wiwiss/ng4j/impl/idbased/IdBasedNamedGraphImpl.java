package de.fuberlin.wiwiss.ng4j.impl.idbased;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.ng4j.impl.NamedGraphImpl;


/**
 * A named graph that is based on a main memory implementation of RDF graphs
 * and that is particularily well suited for the Semantic Web client.
 *
 * @author Olaf Hartig
 */
public class IdBasedNamedGraphImpl extends NamedGraphImpl
                                   implements IdBasedGraph
{
	// initialization

	public IdBasedNamedGraphImpl ( Node graphName, IdBasedGraphMem graph )
	{
		super( graphName, graph );
	}

	public IdBasedNamedGraphImpl ( String graphNameURI, IdBasedGraphMem graph )
	{
		super( graphNameURI, graph );
	}


	// implementation of the IdBasedGraph interface

	/**
	 * Calls the corresponding method of the embedded graph ({@link IdBasedGraphMem#getNode}).
	 * 
	 * @see de.fuberlin.wiwiss.ng4j.impl.idbased.IdBasedGraph#getNode(int)
	 */
	final public Node getNode ( int id )
	{
		return ( (IdBasedGraphMem) graph ).getNode( id );
	}

	/**
	 * Calls the corresponding method of the embedded graph ({@link IdBasedGraphMem#getId}).
	 * 
	 * @see de.fuberlin.wiwiss.ng4j.impl.idbased.IdBasedGraph#getId(com.hp.hpl.jena.graph.Node)
	 */
	final public int getId ( Node n )
	{
		return ( (IdBasedGraphMem) graph ).getId( n );
	}

	/**
	 * Calls the corresponding method of the embedded graph ({@link IdBasedGraphMem#contains}).
	 * 
	 * @see de.fuberlin.wiwiss.ng4j.impl.idbased.IdBasedGraph#contains(int, int, int)
	 */
	final public boolean contains ( int sId, int pId, int oId )
	{
		return ( (IdBasedGraphMem) graph ).contains( sId, pId, oId );
	}

	/**
	 * Calls the corresponding method of the embedded graph ({@link IdBasedGraphMem#find}).
	 * 
	 * @see de.fuberlin.wiwiss.ng4j.impl.idbased.IdBasedGraph#find(int, int, int)
	 */
	final public Iterator<EncodedTriple> find ( int sId, int pId, int oId )
	{
		return ( (IdBasedGraphMem) graph ).find( sId, pId, oId );
	}

}

/*
 * (c) Copyright 2009 Christian Bizer (chris@bizer.de)
 * All rights reserved.
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