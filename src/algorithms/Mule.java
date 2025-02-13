/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Mule.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;

public class Mule extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnLeftTask;
  private double endTaskDirection;

  //---CONSTRUCTORS---//
  public Mule() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    turnLeftTask=false;
    move();
    sendLogMessage("Moving a head. Waza!");
  }
  public void step() {
    if (turnLeftTask) {
      if (isHeading(endTaskDirection)) {
	turnLeftTask=false;
	move();
        sendLogMessage("Moving a head. Waza!");
      } else {
	stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
      }
      return;
    }
    if (detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot) {
      fire(getHeading());
      return;
    }
    if (!(detectFront().getObjectType()==IFrontSensorResult.Types.WALL || detectFront().getObjectType()==IFrontSensorResult.Types.Wreck)) {
      if (Math.random()<0.98) move(); //And what to do when blind blocked?
      else fire(getHeading());
      sendLogMessage("Moving a head. Waza!");
    } else {
      turnLeftTask=true;
      endTaskDirection=getHeading()+Parameters.LEFTTURNFULLANGLE;
      stepTurn(Parameters.Direction.LEFT);
      sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
    }
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<HEADINGPRECISION;
  }
}
