/*
 *    RandomizeFilter.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * This filter randomly shuffles the order of instances passed through it.
 * The random number generator is reset with the seed value whenever
 * inputFormat() is called. <p>
 *
 * Valid filter-specific options are:<p>
 *
 * -S num <br>
 * Specify the random number seed (default 42).<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class RandomizeFilter extends Filter implements OptionHandler {

  /** The random number seed */
  protected int m_Seed = 42;

  /** The current random number generator */
  protected Random m_Random;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSpecify the random number seed (default 42)",
              "S", 1, "-S <num>"));

    return newVector.elements();
  }


  /**
   * Parses a list of options for this object. Valid options are:<p>
   *
   * -S num <br>
   * Specify the random number seed (default 42).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setRandomSeed(Integer.parseInt(seedString));
    } else {
      setRandomSeed(42);
    }

    if (m_InputFormat != null) {
      inputFormat(m_InputFormat);
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [2];
    int current = 0;

    options[current++] = "-S"; options[current++] = "" + getRandomSeed();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  
  /**
   * Get the random number generator seed value.
   *
   * @return random number generator seed value.
   */
  public int getRandomSeed() {
    
    return m_Seed;
  }
  
  /**
   * Set the random number generator seed value.
   *
   * @param newRandomSeed value to use as the random number generator seed.
   */
  public void setRandomSeed(int newRandomSeed) {
    
    m_Seed = newRandomSeed;
  }
  
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   */
  public boolean inputFormat(Instances instanceInfo) {

    m_InputFormat = new Instances(instanceInfo, 0);
    setOutputFormat(m_InputFormat);
    m_Random = new Random(m_Seed);
    m_NewBatch = true;
    return true;
  }

  /**
   * Signify that this batch of input to the filter is finished. If
   * the filter requires all instances prior to filtering, output()
   * may now be called to retrieve the filtered instances. Any
   * subsequent instances filtered should be filtered based on setting
   * obtained from the first batch (unless the inputFormat has been
   * re-assigned or new options have been set). This 
   * implementation randomizes all the instances received in the batch.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined 
   */
  public boolean batchFinished() throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }

    m_InputFormat.randomize(m_Random);
    for (int i = 0; i < m_InputFormat.numInstances(); i++) {
      push(m_InputFormat.instance(i));
    }
    m_InputFormat = new Instances(m_InputFormat, 0);
    
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }


  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new RandomizeFilter(), argv);
      } else {
	Filter.filterFile(new RandomizeFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








