/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    DistributionMetaClusterer.java
 *    Copyright (C) 2002 Richard Kirkby
 *
 */

package weka.clusterers;

import weka.core.*;
import weka.estimators.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Class for wrapping a Clusterer to make it return a distribution and density. Fits
 * normal distributions and discrete distributions within each cluster produced by
 * the wrapped clusterer.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.7 $
 */
public class DistributionMetaClusterer extends Clusterer 
  implements OptionHandler {

  /** holds training instances header information */
  private Instances m_theInstances;
  /** prior probabilities for the fitted clusters */
  private double [] m_priors;
  /** normal distributions fitted to each numeric attribute in each cluster */
  private double [][][] m_modelNormal;
  /** discrete distributions fitted to each discrete attribute in each cluster */
  private DiscreteEstimator [][] m_model;
  /** default minimum standard deviation */
  private double m_minStdDev = 1e-6;
  /** The clusterer being wrapped */
  private Clusterer m_wrappedClusterer = new weka.clusterers.SimpleKMeans();

  /**
   * Default constructor.
   * 
   */  
  public DistributionMetaClusterer() {

  }
   
  /**
   * Contructs a DistributionMetaClusterer wrapping a given Clusterer.
   * 
   * @param toWrap the clusterer to wrap around
   */    
  public DistributionMetaClusterer(Clusterer toWrap) {

    setClusterer(toWrap);
  }
  
  /**
   * Builds a clusterer for a set of instances.
   *
   * @param instances the instances to train the clusterer with
   * @exception Exception if the clusterer hasn't been set or something goes wrong
   */  
  public void buildClusterer(Instances data) throws Exception {
    m_theInstances = new Instances(data, 0);
    if (m_wrappedClusterer == null) {
      throw new Exception("No clusterer has been set");
    }
    m_wrappedClusterer.buildClusterer(data);
    m_model = 
       new DiscreteEstimator[m_wrappedClusterer.numberOfClusters()][data.numAttributes()];
    m_modelNormal = 
      new double[m_wrappedClusterer.numberOfClusters()][data.numAttributes()][2];
    m_priors = new double[m_wrappedClusterer.numberOfClusters()]; 
     for (int i = 0; i < m_wrappedClusterer.numberOfClusters(); i++) {
       for (int j = 0; j < data.numAttributes(); j++) {
	 if (data.attribute(j).isNominal()) {
	   m_model[i][j] = new DiscreteEstimator(data.attribute(j).numValues(),
						 true);
	 }
       }
     }
     
     Instance inst = null;
     // process data
     for (int i = 0; i < data.numInstances(); i++) {
       inst = data.instance(i);
       int cluster = m_wrappedClusterer.clusterInstance(inst);
       m_priors[cluster]++;
       for (int j = 0; j < data.numAttributes(); j++) {
	 if (!inst.isMissing(j)) {
	   if (data.attribute(j).isNominal()) {
	     m_model[cluster][j].addValue(inst.value(j),1.0);
	   } else {
	     m_modelNormal[cluster][j][0] += inst.value(j);
	     m_modelNormal[cluster][j][1] += (inst.value(j) * inst.value(j));
	   }
	 }
       }
     }
     
     // calculate mean and std deviation for numeric attributes
     for (int j = 0; j < data.numAttributes(); j++) {
       if (data.attribute(j).isNumeric()) {
	 for (int i = 0; i < m_wrappedClusterer.numberOfClusters(); i++) {	   
	   if (m_priors[i] > 0) {
	     // variance
	     m_modelNormal[i][j][1] = (m_modelNormal[i][j][1] - 
				       (m_modelNormal[i][j][0] *
					m_modelNormal[i][j][0] /
					m_priors[i])) /
	       m_priors[i];
	     
	     // std dev
	     m_modelNormal[i][j][1] = Math.sqrt(m_modelNormal[i][j][1]);
	     if (m_modelNormal[i][j][1] <= m_minStdDev 
		 || Double.isNaN(m_modelNormal[i][j][1])) {
	       m_modelNormal[i][j][1] = 
		 m_minStdDev;
	     }
	     
	     // mean
	     m_modelNormal[i][j][0] /= m_priors[i];
	   }
	 }
       }
     }
     
     Utils.normalize(m_priors);
  }
  
  /**
   * Computes the density for a given instance.
   * 
   * @param instance the instance to compute the density for
   * @return the density.
   * @exception Exception if the density could not be computed successfully
   */
  public double densityForInstance(Instance instance) throws Exception {

    return Utils.sum(weightsForInstance(instance));
  }

  /**
   * Returns the cluster probability distribution for an instance. Will simply have a
   * probability of 1 for the chosen cluster and 0 for the others.
   *
   * @param instance the instance to be clustered
   * @return the probability distribution
   */  
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double[] distribution = weightsForInstance(instance);
    Utils.normalize(distribution);
    return distribution;
  }

  /**
   * Returns the weights (indicating cluster membership) for a given instance
   * 
   * @param inst the instance to be assigned a cluster
   * @return an array of weights
   * @exception Exception if weights could not be computed
   */
  protected double[] weightsForInstance(Instance inst)
    throws Exception {

    int i, j;
    double prob;
    double[] wghts = new double[m_wrappedClusterer.numberOfClusters()];

    for (i = 0; i < m_wrappedClusterer.numberOfClusters(); i++) {
      if (m_priors[i] > 0) {
	prob = 1.0;
	
	for (j = 0; j < inst.numAttributes(); j++) {
	  if (!inst.isMissing(j)) {
	    if (inst.attribute(j).isNominal()) {
	    prob *= m_model[i][j].getProbability(inst.value(j));
	    }
	    else { // numeric attribute
	      prob *= normalDens(inst.value(j), 
				 m_modelNormal[i][j][0], 
				 m_modelNormal[i][j][1]);
	    }
	  }
	}

	wghts[i] = (prob * m_priors[i]);
      }
    }

    return  wghts;
  }

  /** Constant for normal distribution. */
  private static double m_normConst = Math.sqrt(2*Math.PI);

  /**
   * Density function of normal distribution.
   * @param x input value
   * @param mean mean of distribution
   * @param stdDev standard deviation of distribution
   */
  private double normalDens (double x, double mean, double stdDev) {
    double diff = x - mean;
    
    return  (1/(m_normConst*stdDev))*Math.exp(-(diff*diff/(2*stdDev*stdDev)));
  }
  
  /**
   * Returns the number of clusters.
   *
   * @return the number of clusters generated for a training dataset.
   * @exception Exception if number of clusters could not be returned successfully
   */
  public int numberOfClusters() throws Exception {

    return m_wrappedClusterer.numberOfClusters();
  }

  /**
   * Returns a description of the clusterer.
   *
   * @return a string containing a description of the clusterer
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    text.append("DistributionMetaClusterer: \n\nWrapped clusterer: " 
		+ m_wrappedClusterer.toString());

    text.append("\nFitted estimators: \n");
    
    for (int j = 0; j < m_priors.length; j++) {
      text.append("\nCluster: " + j + " Prior probability: " 
		  + Utils.doubleToString(m_priors[j], 4) + "\n\n");
      
      for (int i = 0; i < m_model[0].length; i++) {
        text.append("Attribute: " + m_theInstances.attribute(i).name() + "\n");
	
        if (m_theInstances.attribute(i).isNominal()) {
          if (m_model[j][i] != null) {
            text.append(m_model[j][i].toString());
          }
        }
        else {
          text.append("Normal Distribution. Mean = " 
		      + Utils.doubleToString(m_modelNormal[j][i][0], 4) 
		      + " StdDev = " 
		      + Utils.doubleToString(m_modelNormal[j][i][1], 4) 
		      + "\n");
        }
      }
    }

    return  text.toString();
  }

  /**
   * Sets the clusterer to wrap.
   *
   * @param toWrap the clusterer
   */
  public void setClusterer(Clusterer toWrap) {

    m_wrappedClusterer = toWrap;
  }

  /**
   * Gets the clusterer being wrapped.
   *
   * @return the clusterer
   */
  public Clusterer getClusterer() {

    return m_wrappedClusterer;
  }

  /**
   * Returns an enumeration describing the available options..
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(1);
    newVector.addElement(new Option(
				    "\tClusterer to wrap. (required)\n",
				    "W", 1,"-W <clusterer name>"));

    if ((m_wrappedClusterer != null) &&
	(m_wrappedClusterer instanceof OptionHandler)) {
      newVector.addElement(new Option(
				      "",
				      "", 0, "\nOptions specific to clusterer "
				      + m_wrappedClusterer.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_wrappedClusterer).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W clusterer name <br>
   * Clusterer to wrap. (required) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String wString = Utils.getOption('W', options);
    if (wString.length() != 0) {
      setClusterer(Clusterer.forName(wString,
				       Utils.partitionOptions(options)));

    } else {
      throw new Exception("A clusterer must be specified with the -W option.");
    }
  }

  /**
   * Gets the current settings of the clusterer.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {

    String [] clustererOptions = new String [0];
    if ((m_wrappedClusterer != null) &&
	(m_wrappedClusterer instanceof OptionHandler)) {
      clustererOptions = ((OptionHandler)m_wrappedClusterer).getOptions();
    }
    String [] options = new String [clustererOptions.length + 3];
    int current = 0;

    if (getClusterer() != null) {
      options[current++] = "-W";
      options[current++] = getClusterer().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(clustererOptions, 0, options, current, 
		     clustererOptions.length);
    current += clustererOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(ClusterEvaluation.
			 evaluateClusterer(new DistributionMetaClusterer(), 
					   argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
