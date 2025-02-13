/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/RandomFire.java 2014-10-28 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;

public class RandomFire extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask;
  private double endTaskDirection;
  private int endTaskCounter;
  private boolean firstMove;

  //---CONSTRUCTORS---//
  public RandomFire() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    turnTask=true;
    moveTask=false;
    firstMove=true;
    endTaskDirection=(Math.random()-0.5)*0.5*Math.PI;
    turnRight=(endTaskDirection>0);
    endTaskDirection+=getHeading();
    if (turnRight) stepTurn(Parameters.Direction.RIGHT);
    else stepTurn(Parameters.Direction.LEFT);
    sendLogMessage("Turning point. Waza!");
  }
  public void step() {
    if (Math.random()<0.01) {
      fire(Math.random()*Math.PI*2);
      return;
    }
    if (turnTask) {
      if (isHeading(endTaskDirection)) {
        if (firstMove) {
          firstMove=false;
  	  turnTask=false;
          moveTask=true;
          endTaskCounter=400;
	  move();
          sendLogMessage("Moving a head. Waza!");
          return;
        }
	turnTask=false;
        moveTask=true;
        endTaskCounter=200;
	move();
        sendLogMessage("Moving a head. Waza!");
      } else {
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
      }
      return;
    }
    if (moveTask) {
      /*if (detectFront()!=NOTHING) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      }*/
      if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        move();
      }
      return;
    }
    return;
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
  }
}
