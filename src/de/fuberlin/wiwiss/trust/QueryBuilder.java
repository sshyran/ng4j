package de.fuberlin.wiwiss.trust;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.triql.GraphPattern;
import de.fuberlin.wiwiss.ng4j.triql.TriQLQuery;

/**
 * Service that builds a {@link TriQLQuery} from a find query pattern
 * and a {@link TrustPolicy}, and sets it up with an untrusted
 * {@link NamedGraphSet} as the data source.
 *
 * @version $Id: QueryBuilder.java,v 1.1 2005/10/04 00:03:44 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class QueryBuilder {
	private NamedGraphSet source;
	private Triple findMe;
	private TrustPolicy policy;
	private VariableBinding systemVariables;
	private TriQLQuery query;

	/**
	 * Sets up a new query builder.
	 * @param source The untrusted reporitory
	 * @param findMe The triple to be found
	 * @param policy The policy to be used
	 * @param systemVariables System variables like ?NOW and ?USER that
	 * 		are available in the policy
	 */
	public QueryBuilder(NamedGraphSet source, Triple findMe, TrustPolicy policy,
	        VariableBinding systemVariables) {
		this.source = source;
		this.findMe = findMe;
		this.policy = policy;
		this.systemVariables = systemVariables;
	}
	
	public TriQLQuery buildQuery() {
		this.query = new TriQLQuery();
		this.query.setSource(this.source);

		this.query.addResultVar(TrustPolicy.GRAPH.getName());

		addResultVarOrPrebind(TrustPolicy.SUBJ, this.findMe.getSubject());
		addResultVarOrPrebind(TrustPolicy.PRED, this.findMe.getPredicate());
		addResultVarOrPrebind(TrustPolicy.OBJ, this.findMe.getObject());
		
		GraphPattern findPattern = new GraphPattern(TrustPolicy.GRAPH);
		findPattern.addTriplePattern(new Triple(
		        TrustPolicy.SUBJ, TrustPolicy.PRED, TrustPolicy.OBJ));
		this.query.addGraphPattern(findPattern);

		for (Iterator iter = this.policy.getGraphPatterns().iterator(); iter.hasNext();) {
			GraphPattern pattern = (GraphPattern) iter.next();
			this.query.addGraphPattern(pattern);
			Iterator it = pattern.getAllVariables().iterator();
			while (it.hasNext()) {
                Node var = (Node) it.next();
                this.query.addResultVar(var.getName());
            }
		}

		Iterator it = this.policy.getPrefixMapping().getNsPrefixMap().keySet().iterator();
		while (it.hasNext()) {
            String prefix = (String) it.next();
            String uri = this.policy.getPrefixMapping().getNsPrefixURI(prefix);
            this.query.setPrefix(prefix, uri);
        }
		
		it = this.systemVariables.variableNames().iterator();
		while (it.hasNext()) {
		    String varName = (String) it.next();
		    this.query.prebindVariableValue(varName, this.systemVariables.value(varName));
		    this.query.addResultVar(varName);
		}
		return this.query;
	}
	
	private void addResultVarOrPrebind(Node var, Node value) {
		if (Node.ANY.equals(value)) {
		    this.query.addResultVar(var.getName());
		} else {
		    this.query.prebindVariableValue(var.getName(), value);
		}
	}
}