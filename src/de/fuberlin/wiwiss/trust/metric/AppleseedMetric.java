/*
 * AppleseedMetrik.java
 *
 * Created on 31. März 2005, 15:02
 */

package de.fuberlin.wiwiss.trust.metric;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.PrefixMapping;

import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.Quad;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphSetImpl;
import de.fuberlin.wiwiss.ng4j.triql.TriQLQuery;
import de.fuberlin.wiwiss.trust.EvaluationResult;
import de.fuberlin.wiwiss.trust.EXPL;
import de.fuberlin.wiwiss.trust.ExplanationPart;
import de.fuberlin.wiwiss.trust.MetricException;
import de.fuberlin.wiwiss.trust.metric.AssertedGraphs;
import de.fuberlin.wiwiss.trust.metric.vocab.MindswapTrust;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * <p>
 * This class implements the Appleseed metric introduced by Cai-Nicolas Ziegler
 * and Georg Lausen in their paper &quot;Spreading Actication Models of Trust
 * Propagation&quot" (2004). 
 * </p>
 * <p>
 * The metric has four required and five optional parameters. 
 * Required are:
 * <ul>
 *  <li>the source</li>
 *  <li>the sink</li>
 *  <li>number of the trusted nodes <i>t</i></li>
 *  <li>injection of trust <i>in</i></li>
 * </ul>  
 * The metric calculates a ranking of <i>M</i> nodes. It returns true if the sink is within 
 * the first <i>t</i> nodes. 
 * </p>
 * <p>
 * The optional Parameters and its default values:
 *  <table>
 *      <tr>
 *          <td>Parameter</td>
 *          <td>Default Value</td>
 *      </tr>
 *      <tr>
 *          <td>spreading factor <i>d</i></td>
 *          <td>0.85</td>
 *      </tr>
 *      <tr>
 *          <td>Threshold <i>T</i></td>
 *          <td>0.05</td>
 *      </tr>
 *      <tr>
 *          <td>Maximum numbers of Nodes <i>M</i></td>
 *          <td>200</td>
 *      </tr>
 *      <tr>
 *          <td>maximal path length <i>l</i></td>
 *          <td>6</td>
 *      </tr>
 *      <tr>
 *          <td>exponent <i>e</i></td>
 *          <td>200</td>
 *      </tr>
 *   </table>
 * For more details, please read the paper by Ziegler and Lausen.
 *
 * @author  Oliver Maresch (oliver-maresch@gmx.de)
 */
public final class AppleseedMetric extends Metric  implements de.fuberlin.wiwiss.trust.Metric {
    //TODO: generator for explanations 
    private List nodes = null;
    
    private Vector trustProperties;
        
    private Node source = null;

    private com.hp.hpl.jena.graph.Node sink = null;
    
    private float T = 0.05f;
    
    private float d = 0.85f;
    
    /** 
     * The power for the non-linear weight normalization.
     * (Default value 1 for linear normalization)
     */
    private float e = 1f;    
    
    private int l = 6;
    
    private int top;
    
    private float injection;
    
    private int iterations;
    
    /** Maximal number of nodes included in the calculation. Default is 0 (interpretated as infinity). */
    private int max_num = 0;
    
    /**
     * Contains Vectors of Vectors which contain Nodes. The Vector number i contains 
     * the nodes added to the analysed trust network in the iteration number i+1.
     */ 
    private Vector newNodesPerIteration;
    
    /**
     * Contains Floats which hold the maximum change of the trust values between the iterations.
     * The Float number i contains the maximal change over all analysed nodes between the iteration 
     * i and the interation i+1.
     */
    private Vector maximalDiffOfTrustPerIteration;
    
    /**
     * Collects all the source information and summerize them.
     */
    DataSourcesSummary sourceSummary = null;
 
    
    public AppleseedMetric(){
        super("http://www.wiwiss.fu-berlin.de/suhl/bizer/TPL/AppleseedMetric");
        nodes = null;
        trustProperties = MindswapTrust.getTrustProperties();
    }
    
    
    /**
     * Ranks begin with zero! Returns null if the node was not ranked.
     */
    public Integer getRankOf(com.hp.hpl.jena.graph.Node node){
        for(int i = 0; i < nodes.size() - 1; i++){
            if(((Node) nodes.get(i)).equals(node)){
                return new Integer(i);
            }
        }
        return null;
    }
    
    
    
    public int getIterations() {
        return iterations;
    }
    
    public int getNumberOfRankedNodes(){
        return nodes.size();
    }
    
    public void setup(NamedGraphSet sourceData) {
        super.setup(sourceData);
        nodes = new Vector();
        // set default values for optional 
        T = 0.05f;
        d = 0.85f;
        e = 1f;    
        l = 6;
        max_num = 0;  
        iterations = 0;
    }
    
    public de.fuberlin.wiwiss.trust.EvaluationResult calculateMetric(java.util.List arguments) throws MetricException {
        
        if(arguments == null || arguments.size() < 4){
            throw new MetricException("The Appleseed metric needs at least the node of the source, the node of the sink, the number of trusted nodes on top of the ranking and the trust value for the injection as parameters.");
        }
        
        // read required arguments
        
        // get source node
        {
            com.hp.hpl.jena.graph.Node tmp = (com.hp.hpl.jena.graph.Node) arguments.get(0);
            source = new SourceNode(tmp);
        }
        
        // get sink node
        {
            sink = (com.hp.hpl.jena.graph.Node) arguments.get(1);
        }
        
        // get the number of trusted nodes on top of the ranking
        try{
            com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(2);
            top = Integer.parseInt(tmp.getLiteral().getLexicalForm());
            if(top < 1) throw new MetricException("The third argument of the Appleseed metric contains the number of trusted nodes. Its value have to be a positive integer (excluding zero) value.");
        }catch(Exception e){
            throw new MetricException("The third argument of the Appleseed metric contains the number of trusted nodes. Its value have to be a positive integer (excluding zero) value.");
        }
        
        // get the injection
        try{
            com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(3);
            injection = Float.parseFloat(tmp.getLiteral().getLexicalForm());
            if(injection < 0f) throw new MetricException("The fourth argument of the Appleseed metric contains the trust injection for the source. Its value have to be a positive float value.");
        }catch(Exception e){
            throw new MetricException("The fourth argument of the Appleseed metric contains the trust injection for the source. Its value have to be a positive float value.");
        }
        
        // if a fifth argument is available, read the spreading factor d
        if(arguments.size() > 4){
            try{
                com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(4);
                d = Float.parseFloat(tmp.getLiteral().getLexicalForm());
                if(d > 1f || d < 0f) throw new MetricException("The optional fifth argument of the Appleseed metric contains the spreading factor. Its value have to be a float value in the range [0,1].");
            }catch(Exception e){
                throw new MetricException("The optional fifth argument of the Appleseed metric contains the spreading factor. Its value have to be a float value in the range [0,1].");
            }
        }
        
        // if a sixth argument is available, read the threshold
        if(arguments.size() > 5){
            try{
                com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(5);
                T = Float.parseFloat(tmp.getLiteral().getLexicalForm());
                if(T <= 0f) throw new MetricException("The optional sixth argument of the Appleseed metric contains the threshold. Its value have to be a float value above zero.");
            }catch(Exception e){
                throw new MetricException("The optional sixth argument of the Appleseed metric contains the threshold. Its value have to be a float value above zero.");
            }
        }
        
        // if a seventh argument is available, read the maximal number of nodes to unfold
        if(arguments.size() > 6){
            try{
                com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(6);
                max_num = Integer.parseInt(tmp.getLiteral().getLexicalForm());
                if(max_num < 0) throw new MetricException("The optional seventh argument of the Appleseed metric contains the maximal number of nodes to unfold. Its value have to be a positive interger value. Zero is interpretated as positive infinity.");
            }catch(Exception e){
                throw new MetricException("The optional seventh argument of the Appleseed metric contains the maximal number of nodes to unfold. Its value have to be a positive interger value. Zero is interpretated as positive infinity.");
            }
        }
        
        // if a eighth argument is available, read the maximal path length
        if(arguments.size() > 7){
            try{
                com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(7);
                l = Integer.parseInt(tmp.getLiteral().getLexicalForm());
                if(l < 0) throw new MetricException("The optional eighth argument of the Appleseed metric contains the maximal path length form the source. Its value have to be a positive interger value. Zero is interpretated as positive infinity.");
            }catch(Exception e){
                throw new MetricException("The optional eighth argument of the Appleseed metric contains the maximal path length form the source. Its value have to be a positive interger value. Zero is interpretated as positive infinity.");
            }
        }
        
        // if a ninth argument is available, read the exponent for non-linear weight normalisation
        if(arguments.size() > 8){
            try{
                com.hp.hpl.jena.graph.Node_Literal tmp = (com.hp.hpl.jena.graph.Node_Literal) arguments.get(8);
                e = Float.parseFloat(tmp.getLiteral().getLexicalForm());
                if(e <= 0f) throw new MetricException("The optional ninth argument of the Appleseed metric contains the exponent for non-linear weight nomalization. Its value have to be a positive float value above zero.");
            }catch(Exception e){
                throw new MetricException("The optional ninth argument of the Appleseed metric contains the exponent for non-linear weight nomalization. Its value have to be a positive float value above zero.");
            }
        }       
        
        // calculate metric
        boolean result = this.trust();
        
        return new MetricResult(result, explain(), explainRDF());
    }    
    
    private boolean trust(){
        // Explanation generation section begin -------------------------------------
        this.sourceSummary = new DataSourcesSummary();
        
        this.maximalDiffOfTrustPerIteration = new Vector();
        
        this.newNodesPerIteration = new Vector();
        // Explanation generation section end ---------------------------------------
        
        // the maximal difference of the nodes between the trust value of the 
        // current and the last trust value
        float max_diff = Float.MAX_VALUE;
        
        // add the source node, which initialize this calculation, to the trust network. 
        nodes.add(source);
        
        // inject initial trust
        source.inject(this.injection);
        
        // init number of other notes in the analysed trust network
        int num = 0;
        
        // counts the iterations
        iterations = 0;
        

        do {
            iterations++;
            
            // reset current number of nodes
            num = nodes.size();
            // reset maximal difference of trust
            max_diff = 0f;
            
            // make a working copy of the vector of nodes
            Vector current_nodes = new Vector(nodes);
            
            // Explanation generation section begin -------------------------------------
            
            Vector newNodes = new Vector();
            this.newNodesPerIteration.add(newNodes);
            
            // Explanation generation section end ---------------------------------------
            
            // for all nodes
            for(int i = 0; i < current_nodes.size(); i++){
                
                
                // the current node
                Node x = (Node) current_nodes.get(i);
                x.setupNextIteration();
                                
                
                // calculate the injections
                Vector edges = x.getEdges();
                for(int j = 0; j < edges.size(); j++){
                    
                    // current edge
                    Edge x2u = (Edge) edges.get(j);
                    // successor node
                    Node u = x2u.getDestination();
                    
                    // if the successor node is not in the current_nodes vector
                    if(!nodes.contains(u) && (max_num <= 0 || nodes.size() < max_num)){
                        // add node to nodes vector
                        nodes.add(u);
                        
                        // Explanation generation section begin -------------------------------------
                        newNodes.add(u);
                        // Explanation generation section end ---------------------------------------
                    }
                    // calculate injection in u by the weighted normalization
                    u.inject(x.calcInjection(x2u.getWeight()));
                }
             
                // update the maximal difference of trust
                max_diff = Math.max(max_diff, x.diffCurrentLastTrust());
            }
            
            // Explanation generation section begin -------------------------------------
            this.maximalDiffOfTrustPerIteration.add(new Float(max_diff));
            // Explanation generation section end ---------------------------------------

            
        // repeat while new nodes are found or the differences of trust between the current and the
        // last iteration are still greater than the threshold T
        } while(num < nodes.size() || (max_diff > T));
       
        java.util.Collections.sort(nodes);
        java.util.Collections.reverse(nodes);

        Integer rank = getRankOf(sink);
        
        if(rank == null){
            return false;
        } else if(rank.intValue() >= top){
            return false;
        } else {
            return true;
        }
    }
    
    
// Explanation generation section begin -------------------------------------
    
    protected ExplanationPart explain(){
        List text = new ArrayList();
        
        Integer rank = getRankOf(sink);
        
        if(rank == null){
            text.add(cl("The "));
            text.add(com.hp.hpl.jena.graph.Node.createURI(this.getURI()));
            text.add(cl(" could not find the sink "));
            text.add(sink);
            text.add(cl(" in the analysed local trust network of the source "));
            text.add(source.getJenaNode());
            text.add(("."));            
        }else { 
            int r = rank.intValue() + 1;
            if(r > top){
                text.add(cl("The "));
                text.add(com.hp.hpl.jena.graph.Node.createURI(this.getURI()));
                text.add(cl(" inferred, that the source "));
                text.add(source.getJenaNode());
                text.add(cl(" does not trust the sink "));
                text.add(sink);
                text.add(cl(". The sink got the rank number " + r + ", which is out of the top " + top + "."));
            }else{
                text.add(cl("The "));
                text.add(com.hp.hpl.jena.graph.Node.createURI(this.getURI()));
                text.add(cl(" inferred, that the source "));
                text.add(source.getJenaNode());
                text.add(cl(" trusts the sink "));
                text.add(sink);
                text.add(cl(". The sink got the rank number " + r + ", which is in the top " + top + "."));
            }
        }
        
        ExplanationPart explanation = new ExplanationPart(text);
        explanation.addPart(explainRanking());
        explanation.addPart(explainIterations());
        explanation.addPart(sourceSummary.summarize());
        return explanation;
    }
       
    private ExplanationPart explainIterations(){
        List text = new ArrayList();
        
        text.add(cl("The metric needed " + this.iterations + " to find new neighbors and to rank them.")); 
        
        ExplanationPart expl = new ExplanationPart(text);
        expl.setDetails(explainIterationDetails());
        
        return expl;
    }

    private ExplanationPart explainIterationDetails(){
        List text = new ArrayList();
        
        text.add(cl("The Appleseed metric iterates as long as new neighbors are found or the maximal change of the trust values of the neighbors differ more than the threshold of " + this.T + ". The gathering of new neigbors all new neighbors stops if all new neighbors are beyond the path length limit of " + this.l + " or the number of analysed neighbors exites the maximal number of nodes to analyse (" + this.max_num + ")."));
        text.add(cl("The iterations had the following bindings:"));
        
        ExplanationPart expl = new ExplanationPart(text);        

        for(int i = 0; i < this.iterations; i++){
            expl.addPart(explainIteration(i));
        }
        
        return expl;
    }
    
    private ExplanationPart explainIteration(int i){
        List text = new ArrayList();       
        
        if(i == iterations -1){
            text.add(cl("The iteration " + (i+1) + " did not satisfy one of the citeria for starting a new iteration. The iteration process stopped here."));
        }else{
            text.add(cl("The iteration " + (i+1) + " satisfy at least one of the criteria for starting a new iteration."));
        }
        
        ExplanationPart expl = new ExplanationPart(text);
        
        expl.addPart(explainMaxDiffOfIteration(i));
        expl.addPart(explainNewNodesOfIteration(i));
        
        return expl;
    }
    
    ExplanationPart explainMaxDiffOfIteration(int i){
        float maxDiff = ((Float) this.maximalDiffOfTrustPerIteration.get(i)).floatValue();
        List text = new ArrayList();

        text.add(cl("In iteration " + (i+1) + " the maximal change of a trust value of the analysed neighbors differed with " + maxDiff + ". "));
        if(maxDiff > T){
            text.add(cl("The maximal difference was greater than the threshold of " + this.T + ". "));
        }else{
            text.add(cl("The maximal difference was less/equal than the threshold of " + this.T + ". "));
        }

        ExplanationPart expl = new ExplanationPart(text);
        
        return expl;
    }
    
    
    ExplanationPart explainNewNodesOfIteration(int i){
        Vector newNodes = (Vector) this.newNodesPerIteration.get(i);
        List text = new ArrayList();

        if(this.nodes.size() > this.max_num){
            text.add(cl("The maximal number of neighbors to analyse (" + this.max_num + ") was reached. No new neighbors could be added. "));
        }else if(newNodes.size() <= 0){
            text.add(cl("No new neigbors found. "));
        }else{
            text.add(cl("" + newNodes.size() + " new neighbors were found: "));
            Iterator it = newNodes.iterator();
            while(it.hasNext()){
                Node n = (Node) it.next();
                text.add(n.getJenaNode());
                text.add(cl(", "));
            }
            text.add(cl("."));
        }

        ExplanationPart expl = new ExplanationPart(text);
        
        return expl;
    }

    private ExplanationPart explainRanking(){
        List text = new ArrayList();
        
        if( this.nodes.size() <= 0){
            text.add(cl("The ranking could not be computed. "));
            return new ExplanationPart(text);
        }
        text.add(cl("The metric ranked " + (this.nodes.size() - 1) + " direct and indirect neighbors."));
        text.add(cl("After the last iteration the ranking and the trust values were as follows: "));
        
        ExplanationPart expl = new ExplanationPart(text);
        
        Iterator nodes = this.nodes.iterator();
        int i = 1;
        while(nodes.hasNext()){
            Node n = (Node) nodes.next();
            List nt = new ArrayList();
            nt.add(cl("" + i + ". "));
            nt.add(n.getJenaNode());
            nt.add(cl(", " + n.getCurrentTrust()));
            expl.addPart(new ExplanationPart(nt));
            i++;
        }
        
        return expl;
    }
        
    
// Explanation generation section end ---------------------------------------
    
    /**
     * Represents a Node in the local trust network.
     */
    private class Node implements java.lang.Comparable {
        protected float in_current = 0;
        protected float in_last = 0;
        protected float trust_current = 0;
        protected float trust_last = 0;
        
        /** Sum of the weights of the node's successor nodes
         * (If exponential weight nomalization in used,
         * the sum contains the sum of weights raised to the 
         * given power.
         */
        protected float weightSum;
        
        protected Vector edges = null;
        
        protected com.hp.hpl.jena.graph.Node node = null;
        
        protected int depth;
        
        private Node(com.hp.hpl.jena.graph.Node node, int depth){
            this.node = node;
            this.depth = depth;
        }
        
        public boolean equals(Object obj){
            com.hp.hpl.jena.graph.Node n = null;
            try{
                n = ((Node) obj).node;
            }catch(ClassCastException e){
                try{
                    n = (com.hp.hpl.jena.graph.Node) obj;
                }catch(ClassCastException e2){
                    return false;
                }
            }
            return this.node.equals(n);
        }
        
        public float calcInjection(float weight){
            return d * this.getLastInjection() * (((float) Math.pow(weight, e))/this.getSumOfWeights());
        }
        
        public void setupNextIteration(){
            trust_last = trust_current;
            trust_current = trust_last + (1 - d) * in_current;
            in_last = in_current;
            in_current = 0;
            
        }
        
        public void inject(float in){
            in_current += in;
        }
        
        public com.hp.hpl.jena.graph.Node getJenaNode(){
            return node;
        }
        
        public float getLastInjection(){
            return in_last;
        }
        
        public float diffCurrentLastTrust(){
            return trust_current - trust_last;
        }
        
        public float getCurrentTrust(){
            return trust_current;
        }
        
        public float getSumOfWeights(){
            return weightSum;
        }
        
        protected void searchEdges(){
            edges = new Vector();
            
            // add backward trust edge
            edges.add(new Edge(source, 1));
            weightSum = 1f;
                       
            // if the depth of the node is smaller than the path length limit
            // search for edges
            if(depth < l || l <= 0){
                AssertedGraphs ag = new AssertedGraphs(node, getSourceData());
                NamedGraphSet graphs = ag.getGraphs();
                Map graph2Warrant = ag.getWarrantMap();
                
                // Explanation generation section begin -------------------------------------
                
                // add asserted graphs to used sources
                Iterator graphIt = graphs.listGraphs();
                while(graphIt.hasNext()){
                    com.hp.hpl.jena.graph.Node source = ((NamedGraph) graphIt.next()).getGraphName();
                    com.hp.hpl.jena.graph.Node warrant = (com.hp.hpl.jena.graph.Node) graph2Warrant.get(source);
                    sourceSummary.addDataSource(source, warrant, node);
                }
                // Explanation generation section end ---------------------------------------

                // for all trust properties ....
                for(int i = 0; i < trustProperties.size(); i++){
                    float weight = ((float)i)/10f;
                    // find the quads with the source as subjet and the trust property as predicate ...
                    Iterator n = graphs.findQuads(com.hp.hpl.jena.graph.Node.ANY, node, (com.hp.hpl.jena.graph.Node) trustProperties.get(i), com.hp.hpl.jena.graph.Node.ANY);
                    while(n.hasNext()){
                        Quad q = (Quad) n.next();
                        Node successor;
                        if(nodes.contains(q.getObject())){
                            successor = (Node) nodes.get(nodes.indexOf(q.getObject()));
                        } else {
                            successor = new Node(q.getObject(), depth + 1);
                        }
                        Edge edge = new Edge(successor, weight);
                        
                        // calculate the sum of weights
                        weightSum += (float) Math.pow(weight, e);
                        
                        edges.add(edge);
                    }
                }   
            }
        }
        
        public Vector getEdges(){
            if(edges == null){
                searchEdges();
            }
            return edges;
        }
        
        public int compareTo(Object obj) {
            Node n;
            try {
                n = (Node) obj;
            } catch (ClassCastException e2){
                return Integer.MAX_VALUE;
            }
            
            if(this.getCurrentTrust() < n.getCurrentTrust()){
                return -1;
            } else if ( this.getCurrentTrust() > n.getCurrentTrust()){
                return 1;
            } else{
                return 0;
            }
        }
        
    }
  
    /**
     * Represents the source in the local trust network.
     */ 
    private class SourceNode extends Node {
        
        public SourceNode(com.hp.hpl.jena.graph.Node node){
            super(node, 0);
        }
        
        public void setupNextIteration(){
            in_last = in_current;
            in_current = 0;
            trust_last = 0;
            trust_current = 0;
            
        }
        
        public float calcInjection(float weight){
            return this.getLastInjection() * (((float) Math.pow(weight, e))/this.getSumOfWeights());
        }
    
        public float diffCurrentLastTrust(){
            return 0;
        }

        
        // Does not add backward edge the the source
        protected void searchEdges(){
            edges = new Vector();
            
            weightSum = 0f;
                        
            // if the depth of the node is smaller than the path length limit
            // search for edges
            if(depth < l || l <= 0){
                AssertedGraphs ag = new AssertedGraphs(node, getSourceData());
                NamedGraphSet graphs = ag.getGraphs();
                Map graph2Warrant = ag.getWarrantMap();
                
                // Explanation generation section begin -------------------------------------
                
                // add asserted graphs to used sources
                Iterator graphIt = graphs.listGraphs();
                while(graphIt.hasNext()){
                    com.hp.hpl.jena.graph.Node source = ((NamedGraph) graphIt.next()).getGraphName();
                    com.hp.hpl.jena.graph.Node warrant = (com.hp.hpl.jena.graph.Node) graph2Warrant.get(source);
                    sourceSummary.addDataSource(source, warrant, node);
                }
                // Explanation generation section end ---------------------------------------

                // for all trust properties ....
                for(int i = 0; i < trustProperties.size(); i++){
                    float weight = ((float)i)/10f;
                    // find the quads with the source as subjet and the trust property as predicate ...
                    Iterator n = graphs.findQuads(com.hp.hpl.jena.graph.Node.ANY, node, (com.hp.hpl.jena.graph.Node) trustProperties.get(i), com.hp.hpl.jena.graph.Node.ANY);
                    while(n.hasNext()){
                        Quad q = (Quad) n.next();
                        Node successor;
                        if(nodes.contains(q.getObject())){
                            successor = (Node) nodes.get(nodes.indexOf(q.getObject()));
                        } else {
                            successor = new Node(q.getObject(), depth + 1);
                        }
                        Edge edge = new Edge(successor, weight);
                        
                        // calculate the sum of weights
                        weightSum += weight;
                        
                        edges.add(edge);
                    }
                }   
            }
        }
    }    
    
    /** 
     * Represents a edge to a nodes in the trust network. The start node of the
     * edge is the owner of the intence of this edge.
     */
    private class Edge{
        private Node dest = null;
        private float weight = 0;
        
        public Edge(Node dest, float weight){
            this.dest = dest;
            this.weight = weight;
        }
        
        public float getWeight(){
            return weight;
        }
        
        public Node getDestination(){
            return dest;
        }
        
    }
    
    /**
     * Sinple test methode
     */
    public static void main(String[] args) throws de.fuberlin.wiwiss.trust.MetricException {

        // get data source
        NamedGraphSet data = new NamedGraphSetImpl();
        data.read("file:/home/voodoo/Java/project/trustlayer/ng4j/doc/trustlayer/finTrustData.trig", "TRIG");
        
        // run metric
        java.util.List arguments = new java.util.LinkedList();
        com.hp.hpl.jena.graph.Node source = com.hp.hpl.jena.graph.Node.createURI("dadean7@lycos.de");
        arguments.add(0,source);
        com.hp.hpl.jena.graph.Node sink = com.hp.hpl.jena.graph.Node.createURI("http://www.reuters.com");
        arguments.add(1,sink);
        com.hp.hpl.jena.graph.Node top = com.hp.hpl.jena.graph.Node.createLiteral("20", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(2,top);
        com.hp.hpl.jena.graph.Node in = com.hp.hpl.jena.graph.Node.createLiteral("200", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(3,in);
        com.hp.hpl.jena.graph.Node d = com.hp.hpl.jena.graph.Node.createLiteral("0.85", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(4,d);
        com.hp.hpl.jena.graph.Node T = com.hp.hpl.jena.graph.Node.createLiteral("0.05", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(5,T);
        com.hp.hpl.jena.graph.Node M = com.hp.hpl.jena.graph.Node.createLiteral("200", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(6,M);
        com.hp.hpl.jena.graph.Node l = com.hp.hpl.jena.graph.Node.createLiteral("6", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(7,l);
        com.hp.hpl.jena.graph.Node e = com.hp.hpl.jena.graph.Node.createLiteral("1", null, com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDinteger);
        arguments.add(8,e);
        
        AppleseedMetric metric = new AppleseedMetric();
        metric.setup(data);
        EvaluationResult result = metric.calculateMetric(arguments);

        
        // print explanations of all triples
        ExplanationPart part = result.getTextExplanation();

        System.out.println("Explanation:");
        
        Model m = ModelFactory.createDefaultModel();
        Graph g = m.getGraph();
        part.writeAsRDF(g);
        m.setNsPrefixes(PrefixMapping.Standard);
        m.setNsPrefix("expl", EXPL.getURI());
        m.write(System.out, "N3");
        // get data source
        
        System.out.println("Question: Should the source <" + source.toString() + "> trust the sink <" + sink.toString() + ">?\nAnswer: " + (result.getResult()?"Yes.\n":"No.\n"));
        System.out.println("The algorithm used " + metric.getIterations() + " iterations to calculate the result.");
    }    
    
    protected Graph explainRDF() {
        return null;
    }
    
}

    