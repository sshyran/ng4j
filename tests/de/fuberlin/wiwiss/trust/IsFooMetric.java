package de.fuberlin.wiwiss.trust;

import java.util.Collections;
import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.trust.ExplanationPart;
import de.fuberlin.wiwiss.trust.Metric;
import de.fuberlin.wiwiss.trust.MetricException;
import de.fuberlin.wiwiss.trust.MetricResult;

/**
 * Metric that returns true if the first argument is the literal node "foo".
 *  
 * @version $Id: IsFooMetric.java,v 1.1 2005/02/18 01:44:59 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class IsFooMetric implements Metric {

    public void setup(NamedGraphSet sourceData) {
        // do nothing
    }

    public String getURI() {
        return "http://example.org/metrics#IsFoo";
    }

    public MetricResult calculateMetric(final List arguments) throws MetricException {
        if (arguments.size() != 1) {
            throw new MetricException();
        }
        return new MetricResult() {
            public boolean getResult() {
                return ((Node) arguments.get(0)).equals(Node.createLiteral("foo"));
            }
            public ExplanationPart getTextExplanation() {
                return new ExplanationPart(Collections.singletonList(Node.createLiteral("foo")));
            }
            public Graph getGraphExplanation() {
                return null;
            }
        };
    }
}