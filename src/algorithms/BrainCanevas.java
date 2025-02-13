/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;

public class BrainCanevas extends Brain {
  public BrainCanevas() { super(); }

  public void activate() {
    //---PARTIE A MODIFIER/ECRIRE---//
    move();
  }
  public void step() {
    //---PARTIE A MODIFIER/ECRIRE---//
    if (detectFront().getObjectType()==IFrontSensorResult.Types.NOTHING) {
      move();
    }
  }
}
