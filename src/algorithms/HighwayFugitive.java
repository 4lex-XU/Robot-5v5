/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/HighwayFugitive.java 2014-10-28 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class HighwayFugitive extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask,highway,back;
  private double endTaskDirection,lastShot;
  private int endTaskCounter;
  private boolean firstMove;

  //---CONSTRUCTORS---//
  public HighwayFugitive() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    turnTask=true;
    moveTask=false;
    firstMove=true;
    highway=false;
    back=false;
    endTaskDirection=(Math.random()-0.5)*0.5*Math.PI;
    turnRight=(endTaskDirection>0);
    endTaskDirection+=getHeading();
    lastShot=Math.random()*Math.PI*2;
    if (turnRight) stepTurn(Parameters.Direction.RIGHT);
    else stepTurn(Parameters.Direction.LEFT);
    sendLogMessage("Turning point. Waza!");
  }
  public void step() {
    if (Math.random()<0.01) {
      fire(Math.random()*Math.PI*2);
      return;
    }
    ArrayList<IRadarResult> radarResults = detectRadar();
    if (highway) {
      if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        highway=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        if (Math.random()<0.1) {
          for (IRadarResult r : radarResults) {
            if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
              fire(r.getObjectDirection());
              lastShot=r.getObjectDirection();
              return;
            }
          }
          fire(lastShot);
          return;
        } else {
          if (back) moveBack(); else move();
        }
      }
      return;
    }
    if (radarResults.size()!=0){
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
          highway=true;
          back=(Math.cos(getHeading()-r.getObjectDirection())>0);
          endTaskCounter=400;
          fire(r.getObjectDirection());
          lastShot=r.getObjectDirection();
          return;
        }
      }
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
          fire(r.getObjectDirection());
          return;
        }
      }
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
        endTaskCounter=100;
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
