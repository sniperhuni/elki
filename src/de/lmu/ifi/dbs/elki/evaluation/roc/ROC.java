package de.lmu.ifi.dbs.elki.evaluation.roc;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

/**
 * Compute ROC (Receiver Operating Characteristics) curves.
 * 
 * A ROC curve compares the true positive rate (y-axis) and false positive rate
 * (x-axis).
 * 
 * It was first used in radio signal detection, but has since found widespread
 * use in information retrieval, in particular for evaluating binary
 * classification problems.
 * 
 * ROC curves are particularly useful to evaluate a ranking of objects with
 * respect to a binary classification problem: a random sampling will
 * approximately achieve a ROC value of 0.5, while a perfect separation will
 * achieve 1.0 (all positives first) or 0.0 (all negatives first). In most use
 * cases, a score significantly below 0.5 indicates that the algorithm result
 * has been used the wrong way, and should be used backwards.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses SimpleAdapter
 * @apiviz.uses DistanceResultAdapter
 * @apiviz.uses OutlierScoreAdapter
 */
// TODO: add lazy Iterator<> based results that do not require full
// materialization
public class ROC {
  /**
   * Compute a ROC curve given a set of positive IDs and a sorted list of
   * (comparable, ID)s, where the comparable object is used to decided when two
   * objects are interchangeable.
   * 
   * @param <C> Reference type
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei List of neighbors along with some comparable object to detect
   *        'same positions'.
   * @return area under curve
   */
  public static <C extends Comparable<? super C>, T> List<DoubleDoublePair> materializeROC(int size, Set<? super T> ids, Iterator<? extends PairInterface<C, T>> nei) {
    final double DELTA = 0.01 / (size * size);

    int postot = ids.size();
    int negtot = size - postot;
    int poscnt = 0;
    int negcnt = 0;
    ArrayList<DoubleDoublePair> res = new ArrayList<DoubleDoublePair>(postot + 2);

    // start in bottom left
    res.add(new DoubleDoublePair(0.0, 0.0));

    PairInterface<C, T> prev = null;
    while(nei.hasNext()) {
      // Previous positive rate - y axis
      double curpos = ((double) poscnt) / postot;
      // Previous negative rate - x axis
      double curneg = ((double) negcnt) / negtot;

      // Analyze next point
      PairInterface<C, T> cur = nei.next();
      // positive or negative match?
      if(ids.contains(cur.getSecond())) {
        poscnt += 1;
      }
      else {
        negcnt += 1;
      }
      // defer calculation for ties
      if((prev != null) && (prev.getFirst().compareTo(cur.getFirst()) == 0)) {
        continue;
      }
      // simplify curve when possible:
      if(res.size() >= 2) {
        DoubleDoublePair last1 = res.get(res.size() - 2);
        DoubleDoublePair last2 = res.get(res.size() - 1);
        // vertical simplification
        if((last1.first == last2.first) && (last2.first == curneg)) {
          res.remove(res.size() - 1);
        }
        // horizontal simplification
        else if((last1.second == last2.second) && (last2.second == curpos)) {
          res.remove(res.size() - 1);
        }
        // diagonal simplification
        // TODO: Make a test.
        else if(Math.abs((last2.first - last1.first) - (curneg - last2.first)) < DELTA && Math.abs((last2.second - last1.second) - (curpos - last2.second)) < DELTA) {
          res.remove(res.size() - 1);
        }
      }
      // Add a new point (for the previous entry!)
      res.add(new DoubleDoublePair(curneg, curpos));
      prev = cur;
    }
    // ensure we end up in the top right corner.
    // Since we didn't add a point for the last entry yet, this likely is needed.
    {
      DoubleDoublePair last = res.get(res.size() - 1);
      if(last.first < 1.0 || last.second < 1.0) {
        res.add(new DoubleDoublePair(1.0, 1.0));
      }
    }
    return res;
  }

  /**
   * This adapter can be used for an arbitrary collection of Integers, and uses
   * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
   * 
   * Note that of course, no id should occur more than once.
   * 
   * The ROC values would be incorrect then anyway!
   * 
   * @author Erich Schubert
   */
  public static class SimpleAdapter implements Iterator<DBIDPair> {
    /**
     * Original Iterator
     */
    private Iterator<DBID> iter;

    /**
     * Constructor
     * 
     * @param iter Iterator for object IDs
     */
    public SimpleAdapter(Iterator<DBID> iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return this.iter.hasNext();
    }

    @Override
    public DBIDPair next() {
      DBID id = this.iter.next();
      return DBIDUtil.newPair(id, id);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * This adapter can be used for an arbitrary collection of Integers, and uses
   * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
   * 
   * Note that of course, no id should occur more than once.
   * 
   * The ROC values would be incorrect then anyway!
   * 
   * @author Erich Schubert
   * @param <D> Distance type
   */
  public static class DistanceResultAdapter<D extends Distance<D>> implements Iterator<Pair<D, DBID>> {
    /**
     * Original Iterator
     */
    private Iterator<DistanceResultPair<D>> iter;

    /**
     * Constructor
     * 
     * @param iter Iterator for distance results
     */
    public DistanceResultAdapter(Iterator<DistanceResultPair<D>> iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return this.iter.hasNext();
    }

    @Override
    public Pair<D, DBID> next() {
      DistanceResultPair<D> d = this.iter.next();
      return new Pair<D, DBID>(d.getDistance(), d.getDBID());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * This adapter can be used for an arbitrary collection of Integers, and uses
   * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
   * 
   * Note that of course, no id should occur more than once.
   * 
   * The ROC values would be incorrect then anyway!
   * 
   * @author Erich Schubert
   */
  public static class OutlierScoreAdapter implements Iterator<DoubleObjPair<DBID>> {
    /**
     * Original Iterator
     */
    private Iterator<DBID> iter;

    /**
     * Outlier score
     */
    private Relation<Double> scores;

    /**
     * Constructor.
     * 
     * @param o Result
     */
    public OutlierScoreAdapter(OutlierResult o) {
      super();
      this.iter = o.getOrdering().iter(o.getScores().getDBIDs());
      this.scores = o.getScores();
    }

    @Override
    public boolean hasNext() {
      return this.iter.hasNext();
    }

    @Override
    public DoubleObjPair<DBID> next() {
      DBID id = this.iter.next();
      return new DoubleObjPair<DBID>(scores.get(id), id);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * compute the Area Under Curve (difference to y axis) for an arbitrary
   * polygon
   * 
   * @param curve Iterable list of points (x,y)
   * @return area und curve
   */
  public static double computeAUC(Iterable<DoubleDoublePair> curve) {
    double result = 0.0;
    Iterator<DoubleDoublePair> iter = curve.iterator();
    // it doesn't make sense to speak about the "area under a curve" when there
    // is no curve.
    if(!iter.hasNext()) {
      return Double.NaN;
    }
    // starting point
    DoubleDoublePair prev = iter.next();
    // check there is at least a second point
    if(!iter.hasNext()) {
      return Double.NaN;
    }
    while(iter.hasNext()) {
      DoubleDoublePair next = iter.next();
      // width * height at half way.
      double width = next.first - prev.first;
      double meanheight = (next.second + prev.second) / 2;
      result += width * meanheight;
      prev = next;
    }
    return result;
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param <D> Distance type
   * @param size Database size
   * @param clus Cluster object
   * @param nei Query result
   * @return area under curve
   */
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, Cluster<?> clus, List<DistanceResultPair<D>> nei) {
    // TODO: ensure the collection has efficient "contains".
    return ROC.computeROCAUCDistanceResult(size, clus.getIDs(), nei);
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param <D> Distance type
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, DBIDs ids, List<DistanceResultPair<D>> nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    List<DoubleDoublePair> roc = materializeROC(size, DBIDUtil.ensureSet(ids), new DistanceResultAdapter<D>(nei.iterator()));
    return computeAUC(roc);
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static double computeROCAUCSimple(int size, DBIDs ids, DBIDs nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    List<DoubleDoublePair> roc = materializeROC(size, DBIDUtil.ensureSet(ids), new SimpleAdapter(nei.iterator()));
    return computeAUC(roc);
  }
}
