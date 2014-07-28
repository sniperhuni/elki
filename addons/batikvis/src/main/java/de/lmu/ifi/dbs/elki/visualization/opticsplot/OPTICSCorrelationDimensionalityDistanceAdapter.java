package de.lmu.ifi.dbs.elki.visualization.opticsplot;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.CorrelationClusterOrderEntry;

/**
 * Adapter that will map a correlation distance to its dimensionality.
 * 
 * @author Erich Schubert
 */
public class OPTICSCorrelationDimensionalityDistanceAdapter implements OPTICSDistanceAdapter<CorrelationClusterOrderEntry<?>> {
  /**
   * Default constructor.
   */
  public OPTICSCorrelationDimensionalityDistanceAdapter() {
    super();
  }

  @Override
  public double getDoubleForEntry(CorrelationClusterOrderEntry<?> coe) {
    if(coe.getCorrelationValue() == Integer.MAX_VALUE) {
      return Double.POSITIVE_INFINITY;
    }
    return coe.getCorrelationValue();
  }

  @Override
  public boolean isInfinite(CorrelationClusterOrderEntry<?> coe) {
    return coe.getCorrelationValue() == Integer.MAX_VALUE;
  }
}