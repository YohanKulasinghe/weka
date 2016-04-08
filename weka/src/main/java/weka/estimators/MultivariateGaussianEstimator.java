/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    MultivariateNormalEstimator.java
 *    Copyright (C) 2013 University of Waikato
 */

package weka.estimators;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;

import java.io.Serializable;

/**
 * Implementation of Multivariate Distribution Estimation using Normal
 * Distribution.
 * 
 * @author Uday Kamath, PhD, George Mason University
 * @author Eibe Frank, University of Waikato
 * @version $Revision$
 * 
 */
public class MultivariateGaussianEstimator implements MultivariateEstimator, Serializable {

  /** Mean vector */
  protected Vector mean;

  /** Inverse of covariance matrix */
  protected Matrix covarianceInverse;

  /** Factor to make density integrate to one (log of this factor) */
  protected double lnconstant;

  /** Ridge parameter to add to diagonal of covariance matrix */
  protected double m_Ridge = 1e-6;

  /**
   * Log of twice the number pi: log(2*pi).
   */
  public static final double Log2PI = Math.log(2 * Math.PI);

  /**
   * Returns string summarizing the estimator.
   */
  public String toString() {

    StringBuffer sb = new StringBuffer();
    sb.append("Natural logarithm of normalizing factor: " + lnconstant + "\n\n");
    sb.append("Mean vector:\n\n" + mean + "\n");
    sb.append("Inverse of covariance matrix:\n\n" + covarianceInverse + "\n");
    return sb.toString();
  }

  /**
   * Returns the log of the density value for the given vector.
   * 
   * @param valuePassed input vector
   * @return log density based on given distribution
   */
  @Override
  public double logDensity(double[] valuePassed) {

    // calculate mean subtractions
    Vector subtractedMean = new DenseVector(mean.size());
    for (int i = 0; i < valuePassed.length; i++) {
      subtractedMean.set(i, valuePassed[i] - mean.get(i));
    }

    return lnconstant -
            0.5 * subtractedMean.dot(covarianceInverse.mult(subtractedMean, new DenseVector(subtractedMean.size())));
  }

  /**
   * Generates the estimator based on the given observations and weight vector.
   * Equal weights are assumed if the weight vector is null.
   */
  @Override
  public void estimate(double[][] observations, double[] weights) {

    if (weights == null) {
      weights = new double[observations.length];
      for (int i = 0; i < weights.length; i++) {
        weights[i] = 1.0;
      }
    }

    mean = weightedMean(observations, weights);
    Matrix cov = weightedCovariance(observations, weights, mean);

    // Compute inverse of covariance matrix
    DenseCholesky chol = new DenseCholesky(observations[0].length, true).factor((UpperSPDDenseMatrix)cov);
    covarianceInverse = chol.solve(Matrices.identity(observations[0].length));
    covarianceInverse = new UpperSPDDenseMatrix(covarianceInverse); // Convert from DenseMatrix

    double logDeterminant = 0;
    for (int i = 0; i < observations[0].length; i++) {
      logDeterminant += Math.log(chol.getU().get(i, i));
    }
    logDeterminant *= 2;
    lnconstant = -(Log2PI * observations[0].length + logDeterminant) * 0.5;
  }

  /**
   * Computes the mean vector
   * @param matrix the data (assumed to contain at least one row)
   * @param weights the observation weights
   * @return the weighted mean
   */
  private Vector weightedMean(double[][] matrix, double[] weights) {

    int rows = matrix.length;
    int cols = matrix[0].length;

    Vector mean = new DenseVector(cols);

    // for each row
    double sumOfWeights = 0;
    for (int i = 0; i < rows; i++) {
      double[] row = matrix[i];
      double w = weights[i];

      // for each column
      for (int j = 0; j < cols; j++) {
        mean.add(j, row[j] * w);
      }
      sumOfWeights += w;
    }

    mean.scale(1.0 / sumOfWeights);

    return mean;
  }

  /**
   * Computes the estimate of the covariance matrix.
   *
   * @param matrix A multi-dimensional array containing the matrix values (assumed to contain at least one row).
   * @param weights The observation weights.
   * @param mean The values' mean vector.
   * @return The covariance matrix.
   */
  private Matrix weightedCovariance(double[][] matrix, double[] weights,  Vector mean) {

    int rows = matrix.length;
    int cols = matrix[0].length;

    if (mean.size() != cols) {
      throw new IllegalArgumentException("Length of the mean vector must match matrix.");
    }

    Matrix covT = new DenseMatrix(cols, cols);
    for (int i = 0; i < cols; i++) {
      for (int j = i; j < cols; j++) {
        double s = 0.0;
        double sumOfWeights = 0;
        for (int k = 0; k < rows; k++) {
          s += weights[k] * (matrix[k][j] - mean.get(j)) * (matrix[k][i] - mean.get(i));
          sumOfWeights += weights[k];
        }
        s /= sumOfWeights;
        covT.set(i, j, s);
        covT.set(j, i, s);
        if (i == j) {
          covT.add(i, j, m_Ridge);
        }
      }
    }

    return new UpperSPDDenseMatrix(covT);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "The value of the ridge parameter.";
  }

  /**
   * Get the value of Ridge.
   *
   * @return Value of Ridge.
   */
  public double getRidge() {

    return m_Ridge;
  }

  /**
   * Set the value of Ridge.
   *
   * @param newRidge Value to assign to Ridge.
   */
  public void setRidge(double newRidge) {

    m_Ridge = newRidge;
  }

  /**
   * Main method for testing this class.
   * @param args command-line parameters
   */
  public static void main(String[] args) {

    double[][] dataset1 = new double[4][1];
    dataset1[0][0] = 0.49;
    dataset1[1][0] = 0.46;
    dataset1[2][0] = 0.51;
    dataset1[3][0] = 0.55;

    MultivariateEstimator mv1 = new MultivariateGaussianEstimator();
    mv1.estimate(dataset1, new double[]{0.7, 0.2, 0.05, 0.05});
    //mv1.estimate(dataset1, null);

    System.err.println(mv1);

    double integral1 = 0;
    int numVals = 1000;
    for (int i = 0; i < numVals; i++) {
      double[] point = new double[1];
      point[0] = (i + 0.5) * (1.0 / numVals);
      double logdens = mv1.logDensity(point);
      if (!Double.isNaN(logdens)) {
        integral1 += Math.exp(logdens) * (1.0 / numVals);
      }
    }
    System.err.println("Approximate integral: " + integral1);

    double[][] dataset = new double[4][3];
    dataset[0][0] = 0.49;
    dataset[0][1] = 0.51;
    dataset[0][2] = 0.53;
    dataset[1][0] = 0.46;
    dataset[1][1] = 0.47;
    dataset[1][2] = 0.52;
    dataset[2][0] = 0.51;
    dataset[2][1] = 0.49;
    dataset[2][2] = 0.47;
    dataset[3][0] = 0.55;
    dataset[3][1] = 0.52;
    dataset[3][2] = 0.54;

    MultivariateEstimator mv = new MultivariateGaussianEstimator();
    mv.estimate(dataset, new double[]{2, 0.2, 0.05, 0.05});
    //mv.estimate(dataset, null);

    System.err.println(mv);

    double integral = 0;
    int numVals2 = 200;
    for (int i = 0; i < numVals2; i++) {
      for (int j = 0; j < numVals2; j++) {
        for (int k = 0; k < numVals2; k++) {
          double[] point = new double[3];
          point[0] = (i + 0.5) * (1.0 / numVals2);
          point[1] = (j + 0.5) * (1.0 / numVals2);
          point[2] = (k + 0.5) * (1.0 / numVals2);
          double logdens = mv.logDensity(point);
          if (!Double.isNaN(logdens)) {
            integral += Math.exp(logdens) / (numVals2 * numVals2 * numVals2);
          }
        }
      }
    }
    System.err.println("Approximate integral: " + integral);

    double[][] dataset3 = new double[5][3];
    dataset3[0][0] = 0.49;
    dataset3[0][1] = 0.51;
    dataset3[0][2] = 0.53;
    dataset3[4][0] = 0.49;
    dataset3[4][1] = 0.51;
    dataset3[4][2] = 0.53;
    dataset3[1][0] = 0.46;
    dataset3[1][1] = 0.47;
    dataset3[1][2] = 0.52;
    dataset3[2][0] = 0.51;
    dataset3[2][1] = 0.49;
    dataset3[2][2] = 0.47;
    dataset3[3][0] = 0.55;
    dataset3[3][1] = 0.52;
    dataset3[3][2] = 0.54;

    MultivariateEstimator mv3 = new MultivariateGaussianEstimator();
    mv3.estimate(dataset3, new double[]{1, 0.2, 0.05, 0.05, 1});
    //mv3.estimate(dataset3, null);

    System.err.println(mv3);

    double integral3 = 0;
    int numVals3 = 200;
    for (int i = 0; i < numVals3; i++) {
      for (int j = 0; j < numVals3; j++) {
        for (int k = 0; k < numVals3; k++) {
          double[] point = new double[3];
          point[0] = (i + 0.5) * (1.0 / numVals3);
          point[1] = (j + 0.5) * (1.0 / numVals3);
          point[2] = (k + 0.5) * (1.0 / numVals3);
          double logdens = mv.logDensity(point);
          if (!Double.isNaN(logdens)) {
            integral3 += Math.exp(logdens) / (numVals3 * numVals3 * numVals3);
          }
        }
      }
    }
    System.err.println("Approximate integral: " + integral3); }
}
