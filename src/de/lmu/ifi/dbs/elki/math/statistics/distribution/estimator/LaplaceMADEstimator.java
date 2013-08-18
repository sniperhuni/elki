package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LaplaceDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Laplace distribution parameters using Median and MAD.
 * 
 * Reference:
 * <p>
 * D. J. Olive<br />
 * Applied Robust Statistics<br />
 * Preprint of an upcoming book, University of Minnesota
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ExponentialDistribution
 */
@Reference(title = "Applied Robust Statistics", authors = "D. J. Olive", booktitle = "Applied Robust Statistics", url="http://lagrange.math.siu.edu/Olive/preprints.htm")
public class LaplaceMADEstimator extends AbstractMADEstimator<LaplaceDistribution> {
  /**
   * Static instance.
   */
  public static final LaplaceMADEstimator STATIC = new LaplaceMADEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LaplaceMADEstimator() {
    // Do not instantiate
  }

  @Override
  public LaplaceDistribution estimateFromMedianMAD(double median, double mad) {
    final double location = median;
    final double scale = 1.443 * mad;
    return new LaplaceDistribution(1. / scale, location);
  }

  @Override
  public Class<? super LaplaceDistribution> getDistributionClass() {
    return LaplaceDistribution.class;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LaplaceMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
